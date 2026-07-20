# 02-索引底层 · 统计信息 / 直方图 / optimizer trace

> 前置：[[MySQL/进阶专题/02-索引底层与优化器执行计划/02-覆盖索引ICP与索引合并MRR]]

## 优化器怎么"选"索引

优化器不试遍所有索引，而是基于**统计信息估算每个计划的 cost**，选 cost 最小的。统计信息**不准** → 选错索引 → 慢查询。

## 统计信息（Cardinality）

```sql
SHOW INDEX FROM user;        -- 看每个索引的 Cardinality（基数/唯一值数）
SHOW TABLE STATUS;           -- Rows 估算
```
- `Cardinality` 越高（接近行数）→ 选择性越好 → 优化器越爱用。
- 统计是**采样估算**，不是精确值（`innodb_stats_persistent_sample_pages` 控制采样页）。

## 什么时候统计信息会"过期"

- 大量增删改后，统计没更新。
- 大表 `DELETE` 后行数骤减但统计还旧。

**手动更新**：
```sql
ANALYZE TABLE user;     -- 重新采样统计信息（轻量）
```

## 直方图（Histogram，8.0 新）

当某列**数据分布极不均匀**（如 `status` 99% 是 0，1% 是 1），光靠 Cardinality 不够，优化器不知道"查 status=1 只命中 1%"。直方图补这个：

```sql
ANALYZE TABLE user UPDATE HISTOGRAM ON status WITH 100 BUCKETS;
-- 查看
SELECT * FROM information_schema.column_statistics
WHERE table_name='user' AND column_name='status';
```
- 优化器用直方图更准地估算 `WHERE status=1` 的行数占比 → 选对索引/执行方式。
- 适合**低基数列但分布倾斜**（如省市、状态、类型）。

## optimizer trace —— 看优化器"怎么想的"

想知道"为什么没走我期望的索引"，开 trace：

```sql
SET optimizer_trace="enabled=on";
SELECT * FROM user WHERE age=20 AND name='张';
SELECT * FROM information_schema.OPTIMIZER_TRACE\G
SET optimizer_trace="enabled=off";
```
重点看 `TRACE` 里的：
- `rows_estimated`：各计划估算行数。
- `considered_execution_plans`：每个候选计划的 cost 对比。
- `reconsidering_access_paths_for_index_ordering`：是否因 ORDER BY 改计划。

> 常见现象：优化器估算"走索引要回表 50% 行"，认为不如全表扫 → 于是 `type=ALL`。这时要么**加覆盖索引**消除回表，要么 `FORCE INDEX`（谨慎）。

## 强制索引（谨慎用）

```sql
SELECT * FROM user FORCE INDEX(idx_age) WHERE age=20;
-- 或 USE INDEX / IGNORE INDEX
```
- 仅当统计信息迟迟不准、业务明确知道该走哪个时用。
- 长期依赖 `FORCE INDEX` 是**技术债**（数据分布变了它不会自动纠偏）。优先 `ANALYZE TABLE` + 建覆盖索引。

## 统计信息参数

| 参数 | 作用 |
|------|------|
| `innodb_stats_persistent` | 统计持久化（默认 ON，重启不丢） |
| `innodb_stats_persistent_sample_pages` | 采样页数，越大越准越慢 |
| `innodb_stats_auto_recalc` | 表改动超 10% 自动重算（默认 ON） |

## 结论

- 优化器按 cost 选索引，cost 来自**统计信息估算** → 统计过期会选错。
- `ANALYZE TABLE` 重算统计；倾斜列用**直方图**补分布。
- `OPTIMIZER_TRACE` 看"为什么选错"；`FORCE INDEX` 是最后手段。

下一步：[[MySQL/进阶专题/02-索引底层与优化器执行计划/04-EXPLAIN逐列精讲与key_len]]
