# Pydantic 数据模型完整详解（结合你 FastAPI 后台项目实战）
## 一、什么是 Pydantic？
Pydantic 是 Python 主流**数据校验、类型转换、数据建模库**，核心依靠 Python 自带的**类型注解**做自动校验。
在 FastAPI 项目里，所有入参、出参、配置文件（你的 `config.py Configs`）全部基于 Pydantic 模型实现。

### 核心两大作用
1. **自动校验数据**：判断参数类型、长度、范围、格式（手机号、邮箱、URL），非法数据直接抛出清晰错误；
2. **自动类型转换**：前端传字符串数字，自动转 int；空字符串自动转 None，不用手动写 if 判断；
3. **数据序列化/反序列化**：对象 ↔ JSON 互相转换，专门适配前后端接口交互。

## 二、Pydantic 模型基础结构
### 1. 基础定义
所有模型必须继承 `pydantic.BaseModel`，类内写字段+类型注解。
```python
from pydantic import BaseModel

# 定义一个用户数据模型
class User(BaseModel):
    id: int
    username: str
    age: int | None = None  # 可选字段，默认值None
```

### 2. 实例化 & 自动校验
```python
# 正常数据，自动生成对象
user = User(id=1, username="admin", age=20)
print(user.id)  # 1
print(user.username)  # admin

# 错误数据，直接抛校验异常（不用手动判断）
user = User(id="不是数字", username="admin")
# 抛出 ValidationError：id 必须是int类型
```

### 3. 转 JSON/字典（接口返回必备）
```python
# 模型转字典
user_dict = user.model_dump()
# 模型转json字符串
user_json = user.model_dump_json()
```

## 三、三大核心使用场景（对应你的 vue-fastapi-admin 项目）
### 场景1：接口请求体校验（前端传参）
后端接口接收前端 POST JSON，用 Pydantic 自动校验，不用手动写一堆判断：
```python
from fastapi import FastAPI
from pydantic import BaseModel, EmailStr

app = FastAPI()

# 登录请求模型
class LoginForm(BaseModel):
    username: str
    password: str
    email: EmailStr | None = None  # 自带邮箱格式校验

@app.post("/login")
def login(form: LoginForm):
    # form 已经自动校验完成，无需手动判断
    return {"msg": "登录成功", "user": form.model_dump()}
```
前端传 `email=abc` 会直接报错：`value is not a valid email address`。

### 场景2：接口返回响应模型（过滤敏感字段）
数据库表有 `password` 密码字段，不能返回给前端，用 Pydantic 响应模型过滤：
```python
# 数据库原始模型（含密码）
class UserDB(BaseModel):
    id: int
    username: str
    password: str
    age: int

# 返回给前端的模型，隐藏password
class UserResp(BaseModel):
    id: int
    username: str
    age: int | None

# 数据库查询对象转前端响应
db_user = UserDB(id=1, username="admin", password="123456", age=20)
resp = UserResp.model_validate(db_user)
print(resp.model_dump())
# 输出：{"id":1,"username":"admin","age":20} 无password
```

### 场景3：环境配置读取（你项目里的 config.py）
你的 `Configs()` 就是标准 Pydantic 模型，自动读取 `.env` 文件、校验必填项：
```python
from pydantic_settings import BaseSettings

class Configs(BaseSettings):
    # 自动从.env读取，缺失直接抛异常（你之前的5个字段缺失报错就是这里）
    REDIS_URI: str
    MYSQL_DATABASE_URI: str
    CELERY_BROKER_URL: str

    class Config:
        env_file = ".env"  # 指定读取.env文件
```
.env 缺少字段时，Pydantic 直接抛出 `Field required` 校验错误，就是你之前启动报错的根源。

## 四、常用高级校验功能（项目高频使用）
### 1. 字段约束：长度、大小、正则
```python
from pydantic import BaseModel, Field

class UserCreate(BaseModel):
    # 用户名：长度3~20
    username: str = Field(min_length=3, max_length=20, description="账号名称")
    # 密码：最少6位
    password: str = Field(min_length=6)
    # 年龄：1~120
    age: int = Field(gt=0, lt=120)
```

### 2. 可选字段、默认值
```python
class Demo(BaseModel):
    a: str  # 必填，不传直接报错
    b: str | None = None  # 可选，不传为None
    c: int = 10  # 可选，不传默认10
```

### 3. 嵌套模型（复杂分层数据，多级JSON）
前端传嵌套JSON，用模型嵌套解析：
```python
class Address(BaseModel):
    province: str
    city: str

class User(BaseModel):
    name: str
    address: Address  # 嵌套模型
```
接收JSON：
```json
{
  "name": "张三",
  "address": {
    "province": "江苏",
    "city": "南京"
  }
}
```

### 4. ORM 模式（对接SQLAlchemy数据库）
数据库 ORM 对象直接转 Pydantic 模型，不用手动赋值：
```python
class UserResp(BaseModel):
    id: int
    username: str

    class Config:
        from_attributes = True  # 开启ORM兼容，支持数据库对象直接解析
```

## 五、和普通 Python 类的核心区别
| 普通Class | Pydantic BaseModel |
|------|------|
| 无自动校验，需要手动写if判断类型 | 自动校验类型、长度、格式，非法数据直接抛错 |
| JSON序列化需要手动写转换函数 | 内置 `model_dump()` 一键转字典/JSON |
| 无法自动读取环境变量、配置文件 | 搭配 `BaseSettings` 自动读取.env、环境变量 |
| 不支持嵌套数据自动解析 | 原生支持嵌套模型、列表、字典校验 |
| FastAPI不会自动解析请求体 | FastAPI原生识别Pydantic，自动解析POST参数 |

## 六、结合你项目的实操总结
1. `schemas/` 文件夹里所有文件，全部是 Pydantic 模型：
   - `user.py`：登录、新增用户、返回用户信息模型；
   - 区分**入参模型（Create/Update）** 和 **出参模型（Resp）**；
2. `config.py` 的 `Configs` 继承 `BaseSettings`，属于Pydantic衍生模型，专门读取.env；
3. 接口函数参数写 `form: XXXModel`，FastAPI 会自动把前端JSON转成模型对象，非法参数直接返回友好报错，不用手动校验；
4. 数据库查询出来的 SQLAlchemy 对象，通过 `model_validate()` 转为响应模型，过滤密码、敏感字段，安全返回前端。

## 七、新手高频踩坑点
1. Pydantic v2 方法名变化：
   - 旧版 `dict()` → 新版 `model_dump()`
   - 旧版 `json()` → 新版 `model_dump_json()`
   - `orm_mode = True` → `from_attributes = True`
2. 字段类型写错，不会隐式转换，严格校验；
3. `.env` 缺失字段直接启动报错，就是Pydantic的必填校验；
4. 数据库ORM对象无法赋值模型：忘记加 `from_attributes = True`。