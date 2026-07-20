# 04-批量与性能优化 · `foreach` 批量插入与 MySQL 批处理

> 前置：[[MyBatis Mapper XML/进阶专题/04-批量与性能优化/00-索引]]

## 三种批量插入写法对比

### 写法 1：单条循环（最慢，别用）

```java
for (User u : list) userMapper.insert(u);   // N 次网络往返 + N 次提交
```

### 写法 2：`foreach` 多值行（推荐，SQL 层批量）

```xml
<insert id="batchInsert">
  INSERT INTO sys_user (user_name, email, status)
  VALUES
  <foreach collection="list" item="u" separator=",">
    (#{u.userName}, #{u.email}, #{u.status})
  </foreach>
</insert>
```
→ 生成 `INSERT ... VALUES (?,?,?),(?,?,?)...` **一条 SQL**，网络往返 1 次。

### 写法 3：`ExecutorType.BATCH`（JDBC 层批量，见 02）

## MySQL 批处理的关键开关：`rewriteBatchedStatements`

即使写成多条 `INSERT`（写法 1 的循环），只要 JDBC 开启批处理，MySQL Connector/J 也能合并。但**默认没开**！

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/ruoyi?rewriteBatchedStatements=true
```
- 不开启：每条 `INSERT` 单独发 → 慢。
- 开启后：Connector/J 把多条同结构 `INSERT` **在服务端拼成一条多值 `INSERT`**，吞吐提升 **5~10 倍**。
- 本仓库 `application.yml` 的 MySQL URL 应确认带此参数（见 [[MyBatis Mapper XML/进阶专题/06-企业实战与疑难排查/01-多数据源与Mapper分包扫描]]）。

## 批量大小与上限

- MySQL `max_allowed_packet`（默认 4MB~64MB）限制单条 SQL 长度。一次拼 10,000 行可能超包 → **分批**。
- 经验值：每批 **500~1000 行** 一个 `INSERT`，循环提交。

```java
int batch = 500;
for (int i = 0; i < list.size(); i += batch) {
    List<User> sub = list.subList(i, Math.min(i + batch, list.size()));
    userMapper.batchInsert(sub);   // foreach 拼一条
}
```

## 批量更新（按主键 case-when）

```xml
<update id="batchUpdate">
  UPDATE sys_user
  <foreach collection="list" item="u" separator=" ">
    WHEN #{u.userId} THEN #{u.status}
  </foreach>
  WHERE user_id IN
  <foreach collection="list" item="u" open="(" close=")" separator=",">
    #{u.userId}
  </foreach>
</update>
```
> 注意：MySQL 不支持 `UPDATE ... SET col = CASE WHEN ... END` 之外更优雅的语法，上面写法需拼 `CASE` 或拆成多条。更稳的是**写法 3 BATCH** 或 `foreach` 逐条 `UPDATE` 配合批处理。

## `useGeneratedKeys` 与批量的冲突

`foreach` 多值插入时，**`useGeneratedKeys` 只能拿到最后一条的 id**（MySQL 一次多值插入只返回首 id + 影响行数）。若需要每条自增 id：
```xml
<insert id="batchInsert" useGeneratedKeys="true" keyProperty="id">
  INSERT ... VALUES
  <foreach ...> (...) </foreach>
</insert>
```
→ 返回 `id` 只有部分可用。**需要逐条 id 时改用单条循环 + BATCH 执行器**。

## 结论

- 默认首选 **`foreach` 多值行 + `rewriteBatchedStatements=true`**，一条 SQL 搞定。
- 超大数据量**分批 500~1000 行**。
- 需要每条自增 id → BATCH 执行器逐条（见 02）。

下一步：[[MyBatis Mapper XML/进阶专题/04-批量与性能优化/02-ExecutorType.BATCH手动批处理]]
