# FastAPI + MySQL 完整增删改查教程（异步SQLAlchemy，适配你的vue-fastapi-admin项目）
## 前置依赖安装
先在你的 `zerorunner` 虚拟环境安装包
```bash
pip install fastapi uvicorn sqlalchemy asyncmy pydantic python-dotenv
```
- `asyncmy`：MySQL异步驱动（项目用的 `mysql+asyncmy`）
- `sqlalchemy`：ORM数据库框架，不用手写原生SQL
- `python-dotenv`：读取.env配置

---
## 步骤1：项目分层结构（和你现有backend架构一致）
```
backend/
├── .env                # 数据库连接地址
├── config.py           # pydantic读取环境变量
├── database.py         # 数据库连接、会话、引擎
├── schemas/
│   └── user.py         # Pydantic模型（入参/出参）
├── models/
│   └── user.py         # SQLAlchemy数据库表实体
├── crud/
│   └── user.py         # 增删改查业务逻辑
├── main.py             # 接口路由
```

## 步骤2：配置.env 数据库连接
```env
# .env
MYSQL_DATABASE_URI=mysql+asyncmy://root:123456@localhost:3306/zerorunner?charset=UTF8MB4
```

## 步骤3：config.py 读取数据库配置（Pydantic BaseSettings）
```python
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    MYSQL_DATABASE_URI: str

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"

config = Settings()
```

## 步骤4：database.py 数据库全局连接（异步核心）
```python
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession
from sqlalchemy.orm import declarative_base, sessionmaker
from config import config

# 1. 创建异步数据库引擎
engine = create_async_engine(config.MYSQL_DATABASE_URI, echo=False)

# 2. 创建异步会话工厂
AsyncSessionLocal = sessionmaker(
    bind=engine, class_=AsyncSession, expire_on_commit=False
)

# 3. ORM表基类，所有数据库模型继承这个
Base = declarative_base()

# 4. 依赖函数：接口自动获取数据库会话
async def get_db() -> AsyncSession:
    async with AsyncSessionLocal() as session:
        yield session
```

## 步骤5：models/user.py 数据库表实体（SQLAlchemy）
定义MySQL里真实存在的表结构
```python
from sqlalchemy import Column, Integer, String
from database import Base

class User(Base):
    __tablename__ = "sys_user"  # 数据库表名

    id = Column(Integer, primary_key=True, autoincrement=True, index=True)
    username = Column(String(50), unique=True, nullable=False, index=True)
    password = Column(String(100), nullable=False)
    age = Column(Integer, nullable=True)
```

## 步骤6：schemas/user.py Pydantic模型（数据校验、前后端交互）
分3类模型：创建入参、更新入参、返回前端模型
```python
from pydantic import BaseModel, Field
from typing import Optional

# 创建用户接口接收参数
class UserCreate(BaseModel):
    username: str = Field(min_length=3, max_length=20)
    password: str = Field(min_length=6)
    age: Optional[int] = Field(None, gt=0, lt=120)

# 更新用户接口接收参数
class UserUpdate(BaseModel):
    username: Optional[str] = None
    password: Optional[str] = None
    age: Optional[int] = None

# 查询返回给前端的模型（隐藏password）
class UserResp(BaseModel):
    id: int
    username: str
    age: Optional[int]

    # 关键：from_attributes = True 支持ORM数据库对象直接转模型
    class Config:
        from_attributes = True
```

## 步骤7：crud/user.py 封装增删改查核心逻辑（业务层）
所有数据库操作统一写在这里，接口只调用方法，不写SQL
```python
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, delete, update
from models.user import User
from schemas.user import UserCreate, UserUpdate

# 1. 新增用户 CREATE
async def create_user(db: AsyncSession, user_info: UserCreate):
    # 把Pydantic对象转字典，构建数据库实体
    db_user = User(**user_info.model_dump())
    db.add(db_user)
    await db.commit()   # 提交事务
    await db.refresh(db_user)  # 刷新获取自增id
    return db_user

# 2. 根据id查询单个用户 READ
async def get_user_by_id(db: AsyncSession, user_id: int):
    stmt = select(User).where(User.id == user_id)
    result = await db.execute(stmt)
    return result.scalar_one_or_none()

# 3. 查询全部用户 READ
async def get_all_users(db: AsyncSession):
    stmt = select(User)
    result = await db.execute(stmt)
    return result.scalars().all()

# 4. 更新用户 UPDATE
async def update_user(db: AsyncSession, user_id: int, update_data: UserUpdate):
    # 过滤掉None空值，只更新传入的字段
    update_dict = update_data.model_dump(exclude_unset=True)
    stmt = update(User).where(User.id == user_id).values(**update_dict)
    await db.execute(stmt)
    await db.commit()
    # 返回更新后的数据
    return await get_user_by_id(db, user_id)

# 5. 删除用户 DELETE
async def delete_user(db: AsyncSession, user_id: int):
    stmt = delete(User).where(User.id == user_id)
    await db.execute(stmt)
    await db.commit()
    return True
```

## 步骤8：main.py 编写接口路由，调用CRUD
```python
from fastapi import FastAPI, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from database import get_db
from crud.user import create_user, get_user_by_id, get_all_users, update_user, delete_user
from schemas.user import UserCreate, UserUpdate, UserResp

app = FastAPI(title="MySQL CRUD Demo")

# 1. 新增用户 POST
@app.post("/user/add", response_model=UserResp)
async def add_user(user: UserCreate, db: AsyncSession = Depends(get_db)):
    return await create_user(db=db, user_info=user)

# 2. 根据ID查询用户 GET
@app.get("/user/{user_id}", response_model=UserResp)
async def query_user(user_id: int, db: AsyncSession = Depends(get_db)):
    db_user = await get_user_by_id(db, user_id)
    if not db_user:
        raise HTTPException(status_code=404, detail="用户不存在")
    return db_user

# 3. 查询所有用户 GET
@app.get("/user/list", response_model=list[UserResp])
async def list_user(db: AsyncSession = Depends(get_db)):
    return await get_all_users(db)

# 4. 更新用户 PUT
@app.put("/user/{user_id}", response_model=UserResp)
async def modify_user(user_id: int, user: UserUpdate, db: AsyncSession = Depends(get_db)):
    db_user = await get_user_by_id(db, user_id)
    if not db_user:
        raise HTTPException(status_code=404, detail="用户不存在")
    return await update_user(db, user_id, user)

# 5. 删除用户 DELETE
@app.delete("/user/{user_id}")
async def remove_user(user_id: int, db: AsyncSession = Depends(get_db)):
    db_user = await get_user_by_id(db, user_id)
    if not db_user:
        raise HTTPException(status_code=404, detail="用户不存在")
    await delete_user(db, user_id)
    return {"msg": "删除成功"}
```

## 步骤9：启动服务 & 测试接口
### 启动命令
```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```
### 在线调试页面
浏览器打开：`http://127.0.0.1:8000/docs`
可以可视化测试 新增/查询/修改/删除 全部接口。

---
# 核心知识点拆解（对应你之前不懂的Pydantic）
1. **schemas 里的Pydantic模型作用**
   - `UserCreate`：校验前端POST传参，用户名长度、密码长度非法直接报错；
   - `UserResp`：过滤数据库密码字段，不会返回敏感信息给前端；
   - `from_attributes = True`：让SQLAlchemy数据库对象直接转为Pydantic模型，不用手动赋值。

2. **CRUD层职责**
   只写数据库操作逻辑，接口只负责接收参数、抛异常，实现分层解耦，大型项目易维护。

3. **异步关键点**
   所有数据库操作必须加 `await`，使用 `asyncmy` + `AsyncSession`，和你项目里 `mysql+asyncmy` 完全匹配，不会阻塞接口。

# 常见踩坑
1. 忘记 `await db.commit()`：数据不会真正存入MySQL；
2. Pydantic模型没加 `from_attributes = True`：数据库对象转模型报错；
3. `.env` 地址写错、MySQL未启动、数据库不存在，Pydantic直接抛出配置缺失报错；
4. 同步驱动 `pymysql` 和异步 `asyncmy` 不能混用，URI格式必须对应。


