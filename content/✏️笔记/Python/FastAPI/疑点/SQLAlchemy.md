# SQLAlchemy 完整通俗讲解，结合你刚才的FastAPI项目
## 一、字面拆分
1. **SQL**：就是我们写的数据库查询语言（`SELECT INSERT UPDATE DELETE`）
2. **Alchemy**：本意「炼金术」，引申为**转化、封装工具**
合起来：**把原生SQL语句，翻译成Python对象操作的工具**。

## 二、核心定义
SQLAlchemy 是 Python 最主流的 **ORM 数据库框架**
### ORM 是什么（重点）
ORM = Object Relational Mapping 对象关系映射
作用：
不用手写 `select * from tb_skill where id=1` 这种原生SQL，
直接用 Python 类、对象、方法操作数据库表。

- MySQL 里的**一张表** → Python 里一个 `class`（你的 models/skill.py 的 Skill）
- MySQL 表里的**一行数据** → Python 的一个对象实例
- MySQL 的**字段** → 类里面的属性

## 三、举对比例子，一眼看懂区别
### 方式1：原生SQL（不用SQLAlchemy）
```python
# 手写SQL字符串，容易写错、注入攻击、拼接麻烦
sql = "INSERT INTO tb_skill(skill_name,price) VALUES ('喷漆',200)"
cursor.execute(sql)
```

### 方式2：SQLAlchemy ORM（你项目里在用）
```python
# 完全纯Python语法，看不见SQL
new_skill = Skill(skill_name="喷漆", price=200)
db.add(new_skill)
await db.commit()
```
SQLAlchemy 底层自动帮你翻译成上面那条SQL，你不用管。

## 四、SQLAlchemy 两大组成部分（你代码里全用到了）
### 1. Core 核心层：SQL语句构造器
用来拼接查询、更新、删除语句
```python
# 你crud里的代码
stmt = select(Skill).where(Skill.id == sid)
# SQLAlchemy自动生成：SELECT * FROM tb_skill WHERE id = ?
```

### 2. ORM 层（你项目主要在用）
把数据表映射成Python类，实现面向对象操作数据库
对应你：
- `database.py` 里的 `Base = declarative_base()` 基类
- `models/skill.py` 继承Base的Skill数据表类

## 五、结合你项目代码逐行对应解释
### 1. Base = declarative_base()
```python
from sqlalchemy.orm import declarative_base
Base = declarative_base()
```
作用：创建ORM父类，所有数据表模型必须继承它，SQLAlchemy才能识别这是一张数据库表。

### 2. models/skill.py 表映射
```python
class Skill(Base):
    __tablename__ = "tb_skill"
    id = Column(Integer, primary_key=True)
    skill_name = Column(String(50))
```
- `__tablename__`：告诉ORM，这个类对应MySQL哪张真实表
- `Column`：代表数据库字段，`Integer/String` 对应MySQL字段类型
- 整个 `Skill` 类 = 映射MySQL `tb_skill` 整张表

### 3. crud 里查询
```python
stmt = select(Skill).where(Skill.id == sid)
res = await db.execute(stmt)
skill_obj = res.scalar_one_or_none()
```
- `select(Skill)`：ORM语法，等价 `SELECT * FROM tb_skill`
- `.where()`：等价 `WHERE id = 变量`
- `skill_obj`：查询出来的一行数据，是一个Skill对象，直接 `skill_obj.skill_name` 取值，不用解析元组。

## 六、SQLAlchemy 给你带来的好处（适配你的FastAPI后台）
1. **不用手写SQL字符串**，避免拼写错误、SQL注入漏洞；
2. 面向对象写代码，和你的Pydantic模型、FastAPI风格统一，全Python；
3. 兼容 MySQL / PostgreSQL / SQLite 等数据库，换库几乎不用改业务代码；
4. 自带事务：`commit()` 提交、出错自动回滚；
5. 支持异步模式（`asyncmy` + AsyncSession），适配FastAPI高并发；
6. 自动类型匹配，不用手动转换数据库返回的数字、字符串。

## 七、极简总结
SQLAlchemy = Python操作MySQL的翻译官
你写Python类/对象 → 它自动翻译成SQL发给MySQL；
MySQL返回数据 → 它自动包装成Python对象给你使用。
你整个项目的 models、database、crud 全部依赖它实现数据库交互。