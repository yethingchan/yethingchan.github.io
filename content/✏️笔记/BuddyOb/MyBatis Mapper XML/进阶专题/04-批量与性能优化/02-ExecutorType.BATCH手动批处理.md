# 04-批量与性能优化 · `ExecutorType.BATCH` 手动批处理

> 前置：[[MyBatis Mapper XML/进阶专题/04-批量与性能优化/00-索引]]

## 原理：`BATCH` 执行器把 SQL 攒成 `addBatch()`

普通 `SIMPLE` 执行器：每条语句立即执行。
`BATCH` 执行器：相同 SQL 结构的语句**不立即执行**，先 `PreparedStatement.addBatch()`，攒一批后 `executeBatch()` 一次发往数据库。

## 用法（关键：开 BATCH 的 SqlSession）

```java
@Autowired SqlSessionTemplate sqlSessionTemplate;   // 本仓库 MyBatis-Spring 注入
// 或注入 SqlSessionFactory

public void batchInsert(List<User> list) {
    // 开一个 BATCH 模式的 SqlSession，且关闭自动提交（手动控制事务）
    try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH, false)) {
        UserMapper mapper = session.getMapper(UserMapper.class);
        int i = 0;
        for (User u : list) {
            mapper.insert(u);          // 不立即执行，进入 batch 缓冲区
            if (++i % 1000 == 0) {  // 每 1000 条 flush 一次
                session.flushStatements();
            }
        }
        session.flushStatements();    // 把剩余 flush
        session.commit();             // 提交事务
    }
}
```

## 为什么比 `foreach` 多值行更好

| 维度 | foreach 多值行 | BATCH 执行器 |
|------|----------------|---------------|
| 单条 SQL 长度 | 超长（易超 `max_allowed_packet`） | 短（反复执行同一 Prepared） |
| 获取自增 id | 只能拿末条 | **每条都能拿**（flush 后回填） |
| 内存 | 拼巨大 SQL 串 | 轻量 |
| 适用 | 中小批量、不需要逐条 id | 大数据量、需逐条 id |

> 本仓库 `sqlSessionTemplate` 默认是 `SIMPLE`；要 BATCH 必须**自己 `openSession(BATCH)`**。

## `flushStatements` 与自增 id 回填

`flushStatements()` 返回 `List<BatchResult>`，MyBatis 会把自增 id 回填到传入的实体对象（`keyProperty`）：

```java
for (User u : list) mapper.insert(u);
session.flushStatements();
// 此刻 list 里每个 u.getId() 已被回填
```

## 事务边界（重要）

- `openSession(BATCH, false)` 的 `false` = 不自动提交。
- **必须 `commit()`**，否则 BATCH 缓冲的语句不落库（回滚）。
- 超大列表建议**每批一个事务**（如每 5000 条 commit 一次），避免单事务过长锁表。

## 与 Spring `@Transactional` 的关系

在 `@Transactional` 内用 `sqlSessionTemplate.openSession(BATCH)` 会**另开一个 Session**，不受外层事务管理（可能挂起）。一般批量任务**独立方法 + 自己控制 Session + 自己 commit**，不依赖 Spring 声明式事务。

## 性能调优点

1. `flush` 间隔：太小（如每 10 条）频繁网络往返；太大（如每 10 万）内存涨。经验 **500~5000**。
2. JVM 堆：BATCH 会缓存未 flush 的语句，超大列表注意内存。
3. 配合 `rewriteBatchedStatements=true` 效果叠加（Connector/J 再把同构语句合并）。

## 结论

- 需要**逐条自增 id**、或**单条 SQL 会超长** → 用 `ExecutorType.BATCH` + 定时 `flushStatements()` + 手动 `commit()`。
- 简单中小批量、不关心 id → `foreach` 多值行更省事。

下一步：[[MyBatis Mapper XML/进阶专题/04-批量与性能优化/03-流式查询与大结果集]]
