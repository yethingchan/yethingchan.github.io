# FastAPI 标准分层架构完整拆解（适配你：MySQL CRUD 后端接口项目）
## 一、完整目录分层（企业通用、小型项目也能照搬）
```
backend/
├── main.py                # 项目入口、总路由聚合、服务启动
├── config.py              # 全局配置（数据库地址、JWT、Redis等，Pydantic读取.env）
├── .env                   # 环境变量（mysql地址、账号密码）
├── database.py            # 数据库连接引擎、会话、依赖函数（全局DB连接）
├── models/                # 数据库实体层（SQLAlchemy，映射MySQL真实表）
│   └── skill.py           # 技能表数据库模型
├── schemas/               # Pydantic数据模型层（参数校验、前后端数据转换）
│   └── skill.py           # 新增/修改入参、返回前端模型
├── crud/                  # 数据库操作层（纯增删改查逻辑，只操作DB）
│   └── skill.py           # select/insert/update/delete 封装
├── api/                   # 接口路由层（接收前端请求，调用crud，返回数据）
│   └── skill.py           # 技能相关所有接口：新增/查询/修改/删除
└── utils/                 # 工具包（可选，异常、加密、日志等）
```

# 二、每一层作用、互相调用关系（从上到下数据流）
## 层级调用顺序（固定单向流向，禁止反向调用）
**前端请求 → main.py总路由 → api路由层 → crud数据库操作层 → models数据表 → MySQL**
数据回传反向：MySQL → models → crud → api → schemas过滤 → 返回前端

## 1. 第一层：main.py 项目入口（总指挥）
### 职责
1. 创建FastAPI实例；
2. 加载所有api路由、注册路由前缀；
3. 全局中间件、跨域、全局异常统一处理；
4. 启动服务入口。
### 如何引用其他层
只导入 `api` 下的路由，不碰crud、models、schemas。
### 示例代码片段
```python
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
# 只导入api路由
from api.skill import router as skill_router

app = FastAPI()
# 跨域
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_credentials=True, allow_methods=["*"], allow_headers=["*"])

# 注册技能接口路由，统一前缀 /skill
app.include_router(skill_router, prefix="/skill", tags=["技能管理"])
```

## 2. 第二层：api/xxx.py 路由接口层（对接前端）
### 职责
1. 定义接口地址（POST/GET/PUT/DELETE）；
2. 接收前端参数，用Pydantic（schemas）校验；
3. 通过Depends拿到数据库会话db；
4. **调用crud层的增删改查方法**；
5. 处理业务判断（如数据不存在抛出404）；
6. 指定返回模型，过滤敏感数据。
### 引用规则
只能导入：schemas、crud、database的get_db，**不能直接操作数据库、不能写SQL**。
### 调用流程示例（新增技能接口）
```python
# api/skill.py
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from database import get_db
# 导入校验模型
from schemas.skill import SkillCreate, SkillResp
# 导入数据库操作方法
from crud.skill import create_skill, get_skill_by_id

router = APIRouter()

# 新增技能接口 POST /skill/add
@router.post("/add", response_model=SkillResp)
async def add_skill(
    form: SkillCreate,  # schemas校验前端传参
    db: AsyncSession = Depends(get_db)
):
    # 直接调用crud层封装好的新增方法，不用写SQL
    new_skill = await create_skill(db=db, data=form)
    return new_skill

# 根据ID查询 GET /skill/1
@router.get("/{skill_id}", response_model=SkillResp)
async def get_skill(skill_id: int, db: AsyncSession = Depends(get_db)):
    skill = await get_skill_by_id(db, skill_id)
    if not skill:
        raise HTTPException(status_code=404, detail="技能不存在")
    return skill
```

## 3. 第三层：crud/xxx.py 数据库操作层（纯CRUD封装）
### 核心职责
只写数据库读写逻辑，封装所有SQLAlchemy查询、新增、修改、删除；
不处理接口报错、不接收前端请求，只接收db会话和数据模型。
### 引用规则
只能导入：models（数据表）、schemas（入参模型），**不导入api、main**。
### 示例代码 crud/skill.py
```python
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, insert, update, delete
from models.skill import Skill  # 数据表实体
from schemas.skill import SkillCreate, SkillUpdate

# 1. 新增
async def create_skill(db: AsyncSession, data: SkillCreate):
    # Pydantic模型转字典，构建数据库对象
    db_obj = Skill(**data.model_dump())
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    return db_obj

# 2. 根据ID查询
async def get_skill_by_id(db: AsyncSession, sid: int):
    stmt = select(Skill).where(Skill.id == sid)
    res = await db.execute(stmt)
    return res.scalar_one_or_none()

# 3. 修改
async def update_skill(db: AsyncSession, sid: int, update_data: SkillUpdate):
    data_dict = update_data.model_dump(exclude_unset=True)
    stmt = update(Skill).where(Skill.id == sid).values(**data_dict)
    await db.execute(stmt)
    await db.commit()
    return await get_skill_by_id(db, sid)

# 4. 删除
async def delete_skill(db: AsyncSession, sid: int):
    stmt = delete(Skill).where(Skill.id == sid)
    await db.execute(stmt)
    await db.commit()
    return True
```

## 4. 第四层：schemas/xxx.py Pydantic 数据模型层（数据校验转换）
### 职责
1. 校验前端传入参数（长度、数字、必填项）；
2. 定义接口返回给前端的字段，隐藏数据库敏感字段；
3. 开启from_attributes，让数据库models对象直接转JSON。
### 被谁引用
只被 api路由层、crud层导入，不引用其他层。
### schemas/skill.py 示例
```python
from pydantic import BaseModel, Field
from typing import Optional

# 新增技能 前端传入参数校验
class SkillCreate(BaseModel):
    skill_name: str = Field(min_length=2, max_length=50, description="技能名称")
    price: float = Field(gt=0, description="技能价格")

# 修改技能 可选参数
class SkillUpdate(BaseModel):
    skill_name: Optional[str] = None
    price: Optional[float] = None

# 返回给前端的数据模型（控制返回字段）
class SkillResp(BaseModel):
    id: int
    skill_name: str
    price: float

    # 关键：支持SQLAlchemy models对象自动解析
    class Config:
        from_attributes = True
```

## 5. 第五层：models/xxx.py SQLAlchemy 数据库实体层（映射MySQL表）
### 职责
一对一映射MySQL里真实的数据表，定义字段名、类型、主键、外键；
### 仅被crud层导入，上层api、main完全不接触
### models/skill.py 示例
```python
from sqlalchemy import Column, Integer, String, Float
from database import Base

class Skill(Base):
    __tablename__ = "tb_skill"  # mysql真实表名
    id = Column(Integer, primary_key=True, autoincrement=True, comment="主键")
    skill_name = Column(String(50), nullable=False, comment="技能名称")
    price = Column(Float, nullable=False, comment="技能价格")
```

## 6. 公共底层：database.py 全局数据库连接
### 职责
1. 读取config里的mysql连接地址，创建异步数据库引擎；
2. 创建数据库会话工厂；
3. 提供依赖函数 `get_db()`，每个接口自动注入数据库连接；
### 被谁引用
所有api路由层通过 `Depends(get_db)` 调用，全局唯一连接管理。

## 7. 公共底层：config.py + .env 配置层
### 职责
用Pydantic BaseSettings读取.env里的数据库地址、账号密码；
### 仅被database.py导入，全局统一配置。

# 三、完整数据流串联演示（新增技能接口完整流转）
1. 前端发送POST请求 `/skill/add`，携带JSON：
```json
{"skill_name": "喷漆", "price": 200}
```
2. `api/skill.py` 接口接收，`SkillCreate`（schemas）自动校验参数是否合法；
3. 接口调用 `crud.create_skill(db, form)`，把校验好的数据传给CRUD；
4. CRUD导入 `models.Skill` 数据表模型，将数据转为数据库实体；
5. 通过异步session执行insert，提交MySQL；
6. MySQL插入成功后，返回数据库对象；
7. 逐层回传到api接口，用 `SkillResp` 模型过滤字段；
8. FastAPI自动转JSON返回给前端。

# 四、分层核心规则（避免代码混乱）
1. **单向调用，不反向导入**
   main → api → crud → models
   禁止：crud导入api、models导入crud、api直接操作models
2. 分层职责隔离
   - api：只管接收请求、响应、简单判断；
   - crud：只做数据库读写，纯数据操作；
   - schemas：只做数据校验、序列化；
   - models：只定义数据表结构；
3. 数据库会话统一通过 `get_db()` 依赖注入，不手动创建连接；
4. 所有参数校验交给Pydantic schemas，不写if判断参数合法性；
5. 所有SQL操作封装在crud，接口里看不到任何SQL语句。

# 五、小型Demo简化方案（不想拆太多文件）
如果只是练习，可合并简化，但分层逻辑不变：
1. 合并 models + crud 到一个db.py；
2. 合并 schemas 到 main.py；
3. api路由直接写在main；
**但分层逻辑不变：请求校验→CRUD操作→数据表映射，只是文件合并，逻辑分层仍遵守。**

[[文件分层代码解释]]