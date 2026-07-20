# 02-索引底层 · EXPLAIN 逐列精讲与 key_len

> 前置：[[MySQL/进阶专题/02-索引底层与优化器执行计划/03-统计信息与直方图optimizer-trace]]

## 怎么看

```sql
EXPLAIN SELECT * FROM user WHERE age=20 AND name='张';
-- 或 8.0 树状：
EXPLAIN FORMAT=TREE SELECT ...;
-- 或 JSON（最详细）：
EXPLAIN FORMAT=JSON SELECT ...;
```

## 逐列精讲（传统格式）

| 列 | 含义 | 怎么判读 |
|------|------|-----------|
| `id` | 查询序号 | 越大越先执行；相同 id 从上往下 |
| `select_type` | 查询类型 | `SIMPLE` 简单 / `PRIMARY` 主 / `SUBQUERY` / `DERIVED` 派生表 |
| `table` | 表名 | 可能是 `<derivedN>`（派生表） |
| `partitions` | 命中分区 | 用了分区表才显示 |
| `type` | **访问类型（最重要）** | 见下，从好到坏 |
| `possible_keys` | 可能用到的索引 | 空 ≠ 不查，可能全扫 |
| `key` | **实际用的索引** | `NULL` = 没走索引（全表扫） |
| `key_len` | **索引使用的字节数** | 判断是否"用到联合索引的哪些列" |
| `ref` | 与索引比较的列/常量 | `const` / `func` / 列名 |
| `rows` | **估算扫描行数** | 越小越好（估算值） |
| `filtered` | 过滤后保留百分比 | 100 = 没额外过滤 |
| `Extra` | **额外信息** | 见下，关键提示 |

## `type` 从好到坏（面试必背）

```
system > const > eq_ref > ref > range > index > ALL
```
- `const`：主键/唯一索引等值，`WHERE id=1`，最多一行。
- `eq_ref`：JOIN 被驱动表用**主键/唯一索引**关联，一行对一行。
- `ref`：普通索引等值，`WHERE age=20`，可能多行。
- `range`：索引范围，`>, <, BETWEEN, IN, LIKE '张%'`。
- `index`：全索引扫描（扫整棵二级索引，比 ALL 好点因为索引小）。
- `ALL`：**全表扫描**，最差，必须优化（除非表极小）。

> 经验：**线上查询至少到 `range`，最好 `ref`/`const`**。`ALL` 出现在大表 = 事故前兆。

## `key_len` 怎么算（判断是否用到联合索引的哪些列）

`key_len` = 实际用到的索引列**字节长度之和**（含 NULL 标记 1 字节、变长 2 字节）。

常用类型长度：
| 类型 | 字节 |
|------|------|
| `TINYINT` | 1 |
| `SMALLINT` | 2 |
| `INT` | 4 |
| `BIGINT` | 8 |
| `DATETIME`(5.6+) | 5（或 8，看版本） |
| `DATE` | 3 |
| `CHAR(N)` utf8mb4 | 4×N（+1 若允许 NULL，+2 变长） |
| `VARCHAR(N)` utf8mb4 | 4×N + 2（变长）+ 1（可 NULL） |

例：`INDEX(a INT, b VARCHAR(10))`，都 NOT NULL：`key_len = 4 + (4*10+2) = 46`。
- 若 `WHERE a=1` → `key_len=4`（只用了 a）。
- 若 `WHERE a=1 AND b='x'` → `key_len=46`（用了 a,b 全部）。

> 看 `key_len` 能确认"联合索引到底用没用全"，比猜准。

## `Extra` 关键值

| Extra | 含义 | 好坏 |
|------|------|------|
| `Using index` | 覆盖索引，不回表 | ✅ 最好 |
| `Using index condition` | ICP 生效 | ✅ 好 |
| `Using where` | Server 层还过滤 | ⚠ 普通 |
| `Using filesort` | **额外排序**（无索引排序） | ❌ 大表慢 |
| `Using temporary` | **用临时表**（GROUP BY/DISTINCT 没索引） | ❌ 慢 |
| `Using join buffer` | JOIN 没用上索引，用缓冲 | ❌ |
| `Select tables optimized away` | 聚合被索引优化掉 | ✅ |

> `Using filesort` + `Using temporary` 是**两大性能毒药**，通常靠加索引（排序/分组列进索引）消除。

## 实际诊断流程

```
1. EXPLAIN 看 type 是不是 ALL → 大表全扫要改
2. 看 key 是不是 NULL → 没走索引
3. 看 key_len → 联合索引用没用全
4. 看 rows → 估算扫多少行
5. 看 Extra → 有没有 filesort/temporary/覆盖
6. 配合 optimizer_trace 看为什么没选期望索引
```

下一步：[[MySQL/进阶专题/02-索引底层与优化器执行计划/05-索引失效全场景]]
