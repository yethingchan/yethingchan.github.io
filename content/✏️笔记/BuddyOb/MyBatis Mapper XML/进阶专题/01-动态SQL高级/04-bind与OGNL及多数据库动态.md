# 01-动态SQL高级 · `bind` 与 OGNL 及多数据库动态

> 前置：[[MyBatis Mapper XML/进阶专题/01-动态SQL高级/00-索引]]

## `bind`：在 SQL 外先计算一个变量

`bind` 把一个 OGNL 表达式的计算结果绑定成一个变量，再在 SQL 里用 `#{}` 引用。常用于**模糊查询拼接**与**跨数据库兼容**。

### 例 1：模糊查询（推荐写法）

```xml
<select id="likeName" resultType="SysUser">
  <bind name="pattern" value="'%' + userName + '%'" />
  SELECT * FROM sys_user
  WHERE user_name LIKE #{pattern}
</select>
```
- `value` 是 OGNL 字符串拼接：`'%' + userName + '%'`。
- 之后任何地方都能用 `#{pattern}`，**且用 `#{}` 占位，安全**。
- 对比错误写法 `WHERE user_name LIKE '%${userName}%'`——`${}` 直接替换，用户输入 `' OR '1'='1` 就注入。

### 例 2：`bind` 统一日期格式化

```xml
<bind name="dayStart" value="date.substring(0,10) + ' 00:00:00'" />
```

## OGNL 在 `test` 里的能力

`test` 属性里就是 OGNL 表达式，可用：

```xml
<if test="list != null and list.size > 0"> ... </if>
<if test="status == 1 or status == 2"> ... </if>
<if test="user.name != null and user.name != ''"> ... </if>
<if test="!ids.isEmpty()"> ... </if>
```
- 判空：集合用 `list != null and list.size > 0`；字符串用 `name != null and name != ''`。
- 取对象属性：`user.name`（OGNL 自动按 getter 解析）。
- 方法调用：`.size`、`.isEmpty()`、字符串 `.contains()` 等都可用。

> 注意 `>` `<` 在 XML 里要转义成 `&gt;` `&lt;`（基础篇 [[MyBatis Mapper XML/09-特殊字符与CDATA]]）。

## 多数据库动态：`databaseIdProvider`

不同库语法不同（分页：`LIMIT` vs `ROWNUM` vs `TOP`）。MyBatis 支持按 `databaseId` 选择片段。

### 配置（Spring Boot）

```java
@Bean
public DatabaseIdProvider databaseIdProvider() {
    VendorDatabaseIdProvider p = new VendorDatabaseIdProvider();
    Properties props = new Properties();
    props.setProperty("MySQL", "mysql");
    props.setProperty("Oracle", "oracle");
    p.setProperties(props);
    return p;
}
```

### Mapper 里用 `_databaseId`

```xml
<select id="page" resultType="Order">
  SELECT * FROM wms_order
  <where>
    <if test="_databaseId == 'mysql'"> 1=1 </if>
    <if test="_databaseId == 'oracle'"> ROWNUM &lt;= #{limit} </if>
  </where>
  <if test="_databaseId == 'mysql'">
    LIMIT #{offset}, #{limit}
  </if>
</select>
```
`_databaseId` 是 MyBatis 自动注入的隐式变量，等于当前库对应的 provider key。

## 例：结合 `bind` + `_databaseId` 做分页方言

```xml
<select id="page" resultType="Order">
  <bind name="off" value="(pageNum - 1) * pageSize" />
  SELECT * FROM wms_order
  <if test="_databaseId == 'mysql'"> LIMIT #{off}, #{pageSize} </if>
  <if test="_databaseId == 'oracle'">
    WHERE ROWNUM &lt;= #{pageNum * pageSize}
  </if>
</select>
```

## 关键点

- `bind` 能让你**用 `#{}` 安全地引用拼接结果**，是替代 `${}` 注入的最佳手段。
- `_databaseId` 是多数据源/多库兼容的官方方案，比在 Java 里 `if (dbType.equals(...))` 优雅。
- OGNL 表达式能力很强，但**只应出现在 `test`/`bind value` 里**；拼进 SQL 正文的部分一律 `#{}`。

下一步回到：[[MyBatis Mapper XML/进阶专题/01-动态SQL高级/00-索引]] ｜ 或进入 [[MyBatis Mapper XML/进阶专题/02-高级结果映射/00-索引]]
