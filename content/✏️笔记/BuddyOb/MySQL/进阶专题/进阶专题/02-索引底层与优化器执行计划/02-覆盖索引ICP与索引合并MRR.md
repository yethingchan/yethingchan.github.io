# 02-索引底层 · 覆盖索引 / ICP / 索引合并 / MRR

> 前置：[[MySQL/进阶专题/02-索引底层与优化器执行计划/01-B+树页结构与回表]]

## 1) 覆盖索引（Covering Index）—— 免回表

若**所有 `SELECT` 列都在索引里**，二级索引就能直接返回，不用回表：

```sql
-- INDEX idx_age_name (age, name)
SELECT age, name FROM user WHERE age = 20;
-- 索引叶子已有 (age, name, id)，age/name 都在 → 覆盖，不回表
```
EXPLAIN 的 `Extra` 会显示 **`Using index`**（覆盖索引提示）。

**设计技巧**：把高频查询的"条件列 + 返回列"做成**联合覆盖索引**：
```
WHERE status = ? AND create_time > ?  查  order_no, amount
→ INDEX (status, create_time, order_no, amount)   -- 覆盖
```

## 2) 索引下推 ICP（Index Condition Pushdown）

**5.6+ 默认开**。把 `WHERE` 里**能用的条件"下推"到存储引擎层（扫索引时就过滤）**，而不是把记录全读到 Server 层再过滤。

```sql
-- INDEX idx_age_name (age, name)
SELECT * FROM user WHERE age = 20 AND name LIKE '张%';
```
- **无 ICP**：用 `age=20` 扫索引，拿到所有 age=20 的 id，**回表**，Server 层再过滤 `name LIKE '张%'`。
- **有 ICP**：在**扫 `age` 索引时**就顺便用 `name LIKE '张%'` 过滤 → 少回表。

EXPLAIN `Extra` 显示 **`Using index condition`**。大表效果显著。

## 3) 索引合并 Index Merge

优化器对**多个单列索引**做合并（5.0+）：
- `Index Intersection`（交集）：`WHERE a=? AND b=?` 分别走 `idx_a`/`idx_b`，取交集。
- `Index Union`（并集）：`WHERE a=? OR b=?` 分别走两个索引，取并集。
- `Index Sort-Union`：先排序再并。

```sql
-- idx_a(a), idx_b(b)
SELECT * FROM t WHERE a=1 OR b=2;   -- 可能 Index Union
```
**注意**：Index Merge 一般说明**缺一个 `(a,b)` 联合索引**。联合索引通常比 Merge 快（少一次索引扫描）。优先建联合索引而非靠 Merge。

## 4) MRR（Multi-Range Read）

回表时，按主键顺序**批量、排序**地回表，把"随机 IO"转成"顺序 IO"：

```
普通回表：id=3, 9, 1, 7 → 随机访问聚簇页（乱序）
MRR：先排成 1,3,7,9 → 顺序访问（磁盘预读友好）
```
`optimizer_switch='mrr=on'` 默认开。对"二级索引范围查 + 大量回表"效果明显。

## 5) BKA（Batched Key Access）

JOIN 时把驱动表的一批 join key **批量**送给被驱动表索引，类似 MRR 思路，减少随机 IO。需 `batched_key_access=on` + MRR 开。

## 对照表

| 技术 | 解决 | Extra 显示 |
|------|------|-----------|
| 覆盖索引 | 免回表 | `Using index` |
| ICP | 索引层先过滤，少回表 | `Using index condition` |
| Index Merge | 多单列索引合并 | `Using union(...)` / `intersect` |
| MRR | 回表随机 IO 转顺序 | `Using MRR` |
| BKA | JOIN 批量 key 访问 | `Using BKA` |

## 结论

- 覆盖索引 = 最好的索引（不回表），按"条件+返回列"建联合。
- ICP 默认开，让索引层多干点过滤活。
- Index Merge 是"没建对联合索引"的补丁，优先改联合索引。
- MRR/BKA 把随机回表 IO 转顺序，大结果集受益。

下一步：[[MySQL/进阶专题/02-索引底层与优化器执行计划/03-统计信息与直方图optimizer-trace]]
