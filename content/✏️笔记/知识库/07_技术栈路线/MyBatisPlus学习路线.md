# MyBatis 与 MyBatisPlus 学习路线

> 对应 Java 全栈路线思维导图「数据与框架」第三叶。持久层主流 ORM，增删改查效率神器。

## 一、学习阶段

### 入门：能跑 CRUD
- JDBC 回顾：连接/Statement/结果集（理解 MyBatis 解决了什么）
- MyBatis：SqlSession、mapper.xml、接口绑定
- 基础映射：resultType/resultMap、参数 #{} 与 ${} 区别

### 进阶：动态 SQL 与映射
- 动态 SQL：if / choose / foreach / trim / where / set
- 关联映射：一对一、一对多（association / collection）
- 缓存：一级（SqlSession）/ 二级（Mapper）
- 逆向工程 / 代码生成

### 高级：MyBatisPlus 提效
- MP 通用 CRUD（BaseMapper）、条件构造器 Wrapper
- 分页插件、逻辑删除、自动填充、乐观锁插件
- 代码生成器（AutoGenerator）
- 多数据源、自定义 SQL 混用

## 二、关键要点与常见坑
- `#{}` 预编译防注入，`${}` 拼接有注入风险（只用于排序/表名）
- 一级缓存 SqlSession 级，多线程下注意
- MP Wrapper 别写太复杂，复杂逻辑回 mapper.xml
- 逻辑删除字段别和唯一索引冲突

## 三、实战
- 入门：MyBatis 手写用户表 CRUD
- 进阶：MP 搭后台管理，用代码生成器出整套单表接口

## 四、衔接
- 联动《MySQL 与 Redis 学习路线》（SQL 与索引）
- 联动《Spring 与 SpringBoot 学习路线》（整合 starter）

## 五、资源
- MyBatis 官方文档；MyBatisPlus 官方文档

## 六、心法
1. MyBatis 给你 SQL 控制权，MP 给你 CRUD 速度，两者互补。
2. 复杂报表回到 XML，别用 Wrapper 硬凑。
3. 生成器是体力活解放，但生成的代码要自己读一遍。
