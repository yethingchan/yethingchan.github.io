# 07-企业实战 · 窗口函数与递归 CTE 深度

> 前置：[[MySQL/进阶专题/07-企业级SQL与架构实战/00-索引]] ｜ 基础：[[MySQL/06-函数分组与窗口函数]]

## 窗口函数回顾

窗口函数 = "分组内计算，但**不折叠行**"（和 `GROUP BY` 不同，每行都保留，附带一个窗口计算结果）。

```
SELECT user_id, order_date, amount,
       SUM(amount) OVER (PARTITION BY user_id)        AS user_total,   -- 每人总额
       RANK()     OVER (PARTITION BY user_id ORDER BY amount DESC) AS rk  -- 每人内排名
FROM wms_order;
```

## 1) 排名三兄弟（最常用）

| 函数 | 相同值 | 排名跳号 |
|------|---------|---------|
| `RANK()` | 同分同号 | 跳（1,1,3） |
| `DENSE_RANK()` | 同分同号 | 不跳（1,1,2） |
| `ROW_NUMBER()` | 同分任取 | 不跳（1,2,3） |

> "取每人金额最高的 1 笔" → `ROW_NUMBER()` + 子查询取 `rn=1`。
```sql
SELECT * FROM (
  SELECT *, ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY amount DESC) rn
  FROM wms_order
) t WHERE rn = 1;
```

## 2) 累计 / 滑动窗口

```sql
-- 累计：从分区首行到当前
SUM(amount) OVER (PARTITION BY user_id ORDER BY order_date
     ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS cum;

-- 近 3 行滑动平均（含当前）
AVG(amount) OVER (ORDER BY order_date
     ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) AS ma3;
```
- `ROWS` 按**行数**；`RANGE` 按**值范围**（同值算一组）。
- 同比/环比、累计毛利、移动均线全靠它。

## 3) 同比/环比（LAG/LEAD）

```sql
SELECT month, amount,
       LAG(amount, 1)  OVER (ORDER BY month) AS prev_month,   -- 上一期
       amount - LAG(amount,1) OVER (ORDER BY month) AS mom_diff
FROM monthly_sales;
```
- `LAG(col, n)` 往前 n 行；`LEAD(col, n)` 往后 n 行。
- 替代"自连接取上期"，可读且快。

## 4) 递归 CTE（树形，部门/菜单/地区）

```sql
WITH RECURSIVE cte AS (
  -- 锚点：根
  SELECT dept_id, dept_name, parent_id, 0 AS depth
  FROM sys_dept WHERE parent_id = 0
  UNION ALL
  -- 递归：join 自己
  SELECT d.dept_id, d.dept_name, d.parent_id, c.depth + 1
  FROM sys_dept d JOIN cte c ON d.parent_id = c.dept_id
)
SELECT * FROM cte;
```
- 一次 SQL 出整棵树（替代"程序递归查库"）。
- 防环：加 `depth < 10` 之类上限，避免脏数据成环死循环。
- 本仓库菜单/部门树后端常用（见 [[RuoYi-Analysis/07-后端-系统业务层]] 的 `buildDeptTree`）。

## 5) 分页 + 窗口（避免深分页）

深分页 `LIMIT 1000000, 10` 慢（见 [[MySQL/进阶专题/02-索引底层与优化器执行计划/05-索引失效全场景]]）。用**游标分页 + 窗口**：

```sql
-- 上一页最大 id 已知 → 只取接下来 10 行
SELECT * FROM wms_order WHERE id < ? ORDER BY id DESC LIMIT 10;
```
> 窗口函数本身**不能消深分页**，但能替代"先查全量再排序取段"。真要深翻页请用 ES（见 [[MySQL/进阶专题/03-分库分表与分布式事务/04-跨分片查询与JOIN难题]]）。

## 结论

- 窗口函数**不折叠行**，每行带窗口计算结果。
- 排名用 `ROW_NUMBER()` 取 TopN、`RANK/DENSE_RANK` 排名。
- 累计/滑动用 `ROWS/RANGE BETWEEN`；同比用 `LAG/LEAD`。
- 树形用**递归 CTE**，防环加 depth 上限。
- 深翻页用游标分页，别靠 `LIMIT offset`。

下一步：[[MySQL/进阶专题/07-企业级SQL与架构实战/02-高并发扣减防超卖(WMS)]]
