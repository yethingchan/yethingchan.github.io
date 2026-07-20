# 03-缓存机制深度 · 一级缓存 `SqlSession` 生命周期

> 前置：[[MyBatis Mapper XML/进阶专题/03-缓存机制深度/00-索引]]

## 一级缓存是什么

一级缓存（local cache）存在 **`BaseExecutor` 的 `localCache`（`PerpetualCache`）** 里，作用域 = **一个 `SqlSession`**。同一个 Session 里，相同 SQL + 相同参数，第二次查询**直接走内存，不发 SQL**。

```java
try (SqlSession session = sqlSessionFactory.openSession()) {
    UserMapper m = session.getMapper(UserMapper.class);
    User u1 = m.selectById(1);  // 发 SQL
    User u2 = m.selectById(1);  // 不发 SQL，取 localCache
    System.out.println(u1 == u2); // true（同一对象引用）
}
```

## 一级缓存的 key

不是"SQL 字符串"，而是 `CacheKey`，由这些共同决定：
- `statementId`（namespace + id）
- 分页 `rowBounds`
- 最终 SQL（含 `#{}` 占位后的参数值）
- `environment`（数据源 id）

只要有一个不同 → key 不同 → 缓存不命中。

## 何时失效（清空 localCache）

| 操作 | 是否清空 |
|------|---------|
| 同 Session 内 `update/insert/delete` | **清空**（无论是不是同一张表） |
| `session.clearCache()` | 手动清空 |
| `session.commit()` / `rollback()` | 清空 |
| 查询加了 `flushCache=true` | 每次先清再查 |

> 关键陷阱：**同 Session 内做了写操作，之前的一级缓存立刻全清**。这意味着"先查后改再查"的第二次查询会重新发 SQL——这不是 bug，是一级缓存的保守正确性保证。

## 一级缓存与 Spring 的关系（重要）

在 Spring（MyBatis-Spring）环境里，`SqlSession` 由 `SqlSessionTemplate` **每次操作都从 `SqlSessionHolder` 取**，且**默认每次 Mapper 调用都开/关一个 SqlSession**（除非在事务内）。

```java
@Transactional
public void test() {
    userMapper.selectById(1);  // 事务内：复用同一个 SqlSession
    userMapper.selectById(1);  // 一级缓存命中，不发 SQL
}
```
```java
public void test() {                 // 无事务
    userMapper.selectById(1);  // SqlSession A（关）
    userMapper.selectById(1);  // SqlSession B（新开）→ 一级缓存不共享！仍发 SQL
}
```
- **有 `@Transactional`**：整个方法共用一个 `SqlSession`，一级缓存生效。
- **无事务**：每次 Mapper 调用各自开/关 Session，一级缓存几乎不共享。

## 为什么一级缓存"看起来没用"

因为 Spring 默认无事务时每次新建 Session，一级缓存跨不了调用。所以它主要服务于：**同一个事务内多次查同一数据**（避免重复查），以及 **Batch/嵌套调用**。

## 关闭一级缓存

```yaml
mybatis:
  configuration:
    local-cache-scope: statement   # 默认 SESSION；设为 STATEMENT 则每次语句后清缓存
```

## 结论

- 一级缓存 = `SqlSession` 级，默认开、不可关（只能缩到 statement 级）。
- 它防的是"同一事务内的重复查询"，不是跨请求的缓存。
- 跨请求共享必须靠二级缓存或外部缓存（Redis）。

下一步：[[MyBatis Mapper XML/进阶专题/03-缓存机制深度/02-二级缓存原理与Cache接口]]
