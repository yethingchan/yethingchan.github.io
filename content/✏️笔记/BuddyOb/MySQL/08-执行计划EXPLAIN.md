# 08 · 执行计划 EXPLAIN

> 进阶路线：[[00-总览与学习路线]] → L3 性能原理 ｜ 前置：[[07-索引原理B+Tree]]

优化前先「看计划」。`EXPLAIN` 告诉你 MySQL 打算怎么执行这条 SQL。

## 1. 怎么用

```sql
EXPLAIN SELECT * FROM sys_user WHERE dept_id = 10 AND status = 0;
-- 更详细（8.0）：
EXPLAIN ANALYZE SELECT ...;   -- 实际执行+耗时
EXPLAIN FORMAT=JSON SELECT ...;  -- 树状详细
```

## 2. 重点看哪些列

| 列 | 看什么 |
|----|--------|
| `type` | **访问类型**，性能关键（见下） |
| `key` | **实际用到的索引**；`NULL`=没用索引 ⚠️ |
| `key_len` | 用到索引的长度，判断组合索引命中了前几列 |
| `rows` | 预估扫描行数（越少越好） |
| `Extra` | 额外信息（见下，常有坑） |
| `possible_keys` | 可能用到的索引 |

## 3. type 访问类型（从好到坏）

```
system > const > eq_ref > ref > range > index > ALL
  完美   主键/唯一 联表 eq   普通索引 范围    全索引 全表
```
| type | 含义 | 评价 |
|------|------|------|
| `const` | 主键/唯一索引等值 | ✅ 最快 |
| `eq_ref` | 联表时主键/唯一匹配 | ✅ |
| `ref` | 普通索引等值 | ✅ 好 |
| `range` | 索引范围（`>`, `IN`, `BETWEEN`） | ✅ 可接受 |
| `index` | 全索引扫描（没回表但扫了整索引） | ⚠️ |
| `ALL` | **全表扫描** | ❌ 必须优化 |

> 目标：至少到 `range`，最好 `ref` 及以上。**出现 `ALL` 且无必要 = 慢查询元凶**。

## 4. Extra 常见值（坑位）

| Extra | 含义 | 处理 |
|-------|------|------|
| `Using index` | **覆盖索引**，不用回表 | ✅ 极好 |
| `Using where` | 服务层再过滤 | 正常 |
| `Using index condition` | 索引下推 ICP | ✅ 好（8.0 默认） |
| `Using filesort` | **文件排序**，无可用索引 | ❌ 加排序索引 |
| `Using temporary` | **用临时表**（常因 GROUP BY/DISTINCT 无索引） | ❌ 优化 |
| `Select tables optimized away` | 优化器已优化掉 | ✅ |

## 5. 实战解读示例

```
type=ref, key=idx_dept_status, rows=3, Extra=Using index
→ 命中组合索引、覆盖索引、只扫3行，完美。

type=ALL, key=NULL, rows=500000, Extra=Using filesort
→ 全表扫描 + 文件排序，500万行必慢，必须加索引。
```

## 6. 学习检查点

- [ ] 会跑 `EXPLAIN` 并能定位 `type=ALL`
- [ ] 知道 `key=NULL` 表示没用索引
- [ ] 看到 `Using filesort` / `Using temporary` 知道要优化
- [ ] 知道 `Using index` = 覆盖索引是好现象

> 下一篇：[[09-索引设计与优化实战]]（怎么建索引、哪些写法会让索引失效）
