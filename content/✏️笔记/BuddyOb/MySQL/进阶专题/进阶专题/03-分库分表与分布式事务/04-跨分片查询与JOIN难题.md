# 03-分库分表 · 跨分片查询与 JOIN 难题

> 前置：[[MySQL/进阶专题/03-分库分表与分布式事务/03-ShardingSphere实战]]

## 分片后"爽"的查询 vs "痛"的查询

```
✅ WHERE user_id = ?        → 单分片命中，快（user_id 是分片键）
❌ WHERE status = 'PAID'     → 不知 user_id，要扫全部 N 个分片
❌ 跨分片 JOIN / 跨分片分页 / 跨分片排序 → 中间件要合并
```

## 1) 跨分片分页（深分页爆炸）

```sql
SELECT * FROM t_order ORDER BY create_time DESC LIMIT 1000000, 10;
```
分片环境下：
- 中间件要在**每个分片**都查 `LIMIT 1000000, 10`（每片 100 万行后取 10）。
- 再把各片结果**拉到中间件内存**做全局排序，取全局第 100 万~100万+10。
- = N 倍 IO + 巨量内存 → **极慢**。

**解法**：
- **避免深度分页**：前端"下一页"用**游标/上一页最大 id**（`WHERE id < ? ORDER BY id DESC LIMIT 10`），每片只查 10 行。
- **冗余维度表**：把"按时间流水分页"的数据异构同步到一张按时间分片的表，或用 ES/ClickHouse 承接这类查询。
- **二次查询法**：先各片取 `LIMIT 10 offset=?` 的排序键，全局排序后回原片精确取。

## 2) 跨分片排序 / 聚合

```sql
SELECT user_id, SUM(amount) FROM t_order GROUP BY user_id;
```
- 每个分片先本地 `GROUP BY` + `SUM`。
- 中间件把各片结果**再聚合**（`MERGE` 引擎）。
- 小结果集 OK；大结果集同样内存压力。

## 3) 跨分片 JOIN（未配绑定表时）

```
t_order(按 order_id 分) JOIN t_user(按 user_id 分)
→ 两表分片键不同 → 数据不在同一片 → 笛卡尔乘积式跨片 JOIN
```
**解法**：
- 配**绑定表**（同分片键）→ JOIN 不出片（见 03）。
- 冗余字段：订单表冗余 `user_name` 等，避免 JOIN。
- 异构同步：把需要 JOIN 的数据同步到同一分片或 ES。
- 应用层分两次查 + 内存组装（小数据量可接受）。

## 4) 分布式 COUNT / MAX

```sql
SELECT COUNT(*) FROM t_order;      -- 每片 COUNT，中间件求和
SELECT MAX(amount) FROM t_order;  -- 每片 MAX，中间件取最大
```
- COUNT/MAX/MIN 可下推各片再合并，相对便宜。
- `AVG` 不能直接合并（要 `SUM/COUNT` 才能合并）。

## 5) 异构数据（分片 + 其他存储）

很多公司**主库分片扛交易**，同时把数据**异步同步到 ES / ClickHouse / Hive** 承接：
- 复杂检索 → ES。
- OLAP 报表/聚合 → ClickHouse。
- 离线分析 → Hive。
- 应用按查询类型路由到不同存储（**CQRS 思路**）。

## 设计时的"反跨片"纪律

1. **90% 查询能带分片键** → 选对分片键（见 01）。
2. 跨片查询**提前用冗余/异构**化解，别等到线上爆。
3. 禁用 `SELECT *` + 深分页；用游标分页。
4. JOIN 只在绑定表内发生，或冗余字段。

## 结论

- 分片的"代价"集中在**不带分片键的查询、跨片 JOIN、深分页、全局排序聚合**。
- 游标分页替代 `LIMIT offset`；绑定表让同键 JOIN 不出片。
- 复杂检索/报表用 ES/ClickHouse 异构承接（CQRS）。

下一步：[[MySQL/进阶专题/03-分库分表与分布式事务/05-分布式事务Seata与最终一致]]
