# FastAPI + MySQL 极简单文件Demo（纯演示，无分层、无.env、开箱即用）
## 1. 安装依赖
```bash
pip install fastapi uvicorn sqlalchemy pymysql
```
这里用同步`pymysql`，代码更简单，适合Demo演示

## 2. 完整main.py（复制直接运行）
```python
from fastapi import FastAPI, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy import create_engine, Column, Integer, String, select, update, delete
from sqlalchemy.orm import declarative_base, sessionmaker, Session

# ===================== 1. 数据库连接（直接写死，Demo简化） =====================
# 修改为你本地MySQL账号密码库名
DB_URL = "mysql+pymysql://root:123456@127.0.0.1:3306/demo_db?charset=utf8mb4"
engine = create_engine(DB_URL)
SessionLocal = sessionmaker(bind=engine)
Base = declarative_base()

# 获取数据库会话
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# ===================== 2. 数据库表定义 =====================
class User(Base):
    __tablename__ = "user"
    id = Column(Integer, primary_key=True, autoincrement=True)
    name = Column(String(30), nullable=False)
    age = Column(Integer)

# 自动创建表（第一次运行执行一次）
Base.metadata.create_all(bind=engine)

# ===================== 3. Pydantic 校验模型 =====================
# 新增/更新入参
class UserForm(BaseModel):
    name: str
    age: int | None = None

# 返回数据模型
class UserResp(BaseModel):
    id: int
    name: str
    age: int | None
    class Config:
        from_attributes = True

# ===================== 4. 接口 增删改查 =====================
app = FastAPI(title="MySQL Demo CRUD")

# 新增
@app.post("/add", response_model=UserResp)
def add_user(form: UserForm, db: Session = Depends(get_db)):
    user = User(name=form.name, age=form.age)
    db.add(user)
    db.commit()
    db.refresh(user)
    return user

# 查询单条
@app.get("/get/{uid}", response_model=UserResp)
def get_user(uid: int, db: Session = Depends(get_db)):
    user = db.scalar(select(User).where(User.id == uid))
    if not user:
        raise HTTPException(status_code=404, detail="用户不存在")
    return user

# 查询全部
@app.get("/list", response_model=list[UserResp])
def list_user(db: Session = Depends(get_db)):
    return db.scalars(select(User)).all()

# 修改
@app.put("/update/{uid}", response_model=UserResp)
def update_user(uid: int, form: UserForm, db: Session = Depends(get_db)):
    user = db.scalar(select(User).where(User.id == uid))
    if not user:
        raise HTTPException(status_code=404, detail="用户不存在")
    stmt = update(User).where(User.id == uid).values(name=form.name, age=form.age)
    db.execute(stmt)
    db.commit()
    return db.scalar(select(User).where(User.id == uid))

# 删除
@app.delete("/del/{uid}")
def del_user(uid: int, db: Session = Depends(get_db)):
    user = db.scalar(select(User).where(User.id == uid))
    if not user:
        raise HTTPException(status_code=404, detail="用户不存在")
    stmt = delete(User).where(User.id == uid)
    db.execute(stmt)
    db.commit()
    return {"msg": "删除成功"}
```

## 3. 使用步骤
1. 本地MySQL新建数据库 `demo_db`
```sql
CREATE DATABASE demo_db DEFAULT CHARACTER SET utf8mb4;
```
2. 修改代码里 `DB_URL` 的账号、密码、库名匹配你本地MySQL
3. 启动服务
```bash
uvicorn main:app --reload
```
4. 打开接口文档调试：http://127.0.0.1:8000/docs

## Demo简化说明（对比企业项目）
1. 无.env配置：数据库地址直接写在代码里，仅演示用，生产禁止
2. 同步SQLAlchemy：去掉复杂`async`/await，新手更容易看懂
3. 全部代码单文件：不用拆分crud、schemas、database文件夹
4. 极简Pydantic：只做基础参数校验、数据返回，无复杂字段限制
5. 自动建表：启动代码自动生成`user`表，不用手动执行SQL脚本

## 关键概念极简解释
1. `User`类（继承Base）：对应MySQL真实数据表
2. `UserForm`：前端提交数据的校验模板（Pydantic）
3. `UserResp`：控制返回给前端的数据，`from_attributes=True` 用来把数据库对象转JSON
4. `get_db()`：依赖注入，每个接口自动拿到数据库连接，用完自动关闭
5. 增删改查逻辑全部写在接口内，省去单独CRUD文件，适合小Demo