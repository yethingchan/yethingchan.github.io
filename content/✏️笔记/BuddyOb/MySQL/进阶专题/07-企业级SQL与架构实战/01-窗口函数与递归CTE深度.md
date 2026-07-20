---
title: 窗口函数与递归 CTE 深度
---

# 01 窗口函数与递归 CTE 深度

> 前置：[[MySQL/进阶专题/07-企业级SQL与架构实战/00-索引]] ｜ 基础篇：[[MySQL/06-函数分组与窗口函数]]

窗口函数（Window Function）和递归 CTE 是"写对分析 SQL"的两大杀器。基础篇只演示了语法，这里讲清**语义边界、性能坑、以及它们如何替换掉又慢又难维护的关联子查询**。

## 一、窗口函数：不改变行数，只给每行"开窗算一遍"

普通聚合 `GROUP BY` 会把多行压成一行；窗口函数 `OVER (...)` **保留每一行**，同时算出它在某个"窗口"内的聚合/排名结果。

```sql
-- 每个部门内按薪资排名（行数不变，每人一行 + 一个 rank 列）
SELECT
  emp_id, dept_id, salary,
  ROW_NUMBER() OVER (PARTITION BY dept_id ORDER BY salary DESC) AS rn,
  RANK()       OVER (PARTITION BY dept_id ORDER BY salary DESC) AS rk,
  DENSE_RANK() OVER (PARTITION BY dept_id ORDER BY salary DESC) AS drk,
  SUM(salary)  OVER (PARTITION BY dept_id)                     AS dept_total,
  AVG(salary)  OVER (PARTITION BY dept_id ORDER BY emp_id
                     ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS running_avg
FROM emp;
```

### 1.1 四个排名函数的区别（面试高频）

| 函数 | 并列处理 | 示例（分数 100,100,90） |
|------|----------|--------------------------|
| `ROW_NUMBER()` | 强行给唯一序号，并列也 1,2,3 | 1,2,3 |
| `RANK()` | 并列同号，跳过后续序号 | 1,1,3 |
| `DENSE_RANK()` | 并列同号，不跳号 | 1,1,2 |
| `NTILE(n)` | 把有序行均分成 n 个桶 | 桶号 1~n |

> 取"每个部门工资最高的前 3 人"用 `ROW_NUMBER()` + 外层 `WHERE rn <= 3`，比 `GROUP BY` + 子查询简洁且通常更快。

### 1.2 窗口框架（Window Frame）——最容易被忽略的语义

`OVER()` 里的窗口范围由 `ROWS` / `RANGE` + 边界决定，默认是 `RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW`（到当前行止）。

```sql
-- 移动平均：当前行及前 2 行
AVG(price) OVER (ORDER BY dt
  ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) AS ma3

-- 累计求和到当前行
SUM(amount) OVER (ORDER BY dt
  ROWS UNBOUNDED PRECEDING) AS cum_sum
```

- `ROWS`：按**物理行偏移**计。
- `RANGE`：按**排序列的值区间**计（同值都算进窗口），处理并列更语义化但要小心结果行数。

### 1.3 取前一名/后一名：`LAG` / `LEAD`（同比环比神器）

```sql
-- 当月销量 vs 上月销量
SELECT month, sales,
  LAG(sales, 1)  OVER (ORDER BY month) AS prev_month,
  LEAD(sales, 1) OVER (ORDER BY month) AS next_month,
  ROUND((sales - LAG(sales,1) OVER (ORDER BY month))
         / LAG(sales,1) OVER (ORDER BY month) * 100, 2) AS mom_pct
FROM monthly_sales;
```

> 这套"环比/同比"以前要靠 `JOIN` 自己上个月，现在一个 `LAG` 搞定。**但注意**：窗口里 `ORDER BY` 的列必须有唯一性保证，否则 `LAG/LEAD` 取到的是"排序后相邻行"，若排序键有重复，结果不确定。配合唯一键或 `ORDER BY dt, id` 更安全。

### 1.4 性能要点

- 窗口函数仍可能走 `filesort`（`EXPLAIN` 的 `Extra` 出现 `Using temporary; Using filesort`），`PARTITION BY` + `ORDER BY` 的列最好能命中索引。
- 多窗口复用同一分区排序时，MySQL 8.0 会尝试复用排序结果，但别指望——复杂查询下仍是多次排序。
- 不要在 `WHERE` 里引用窗口函数的结果（窗口在 `WHERE` 之后执行）；要过滤排名后结果，用**子查询/CTE 包一层再 `WHERE rn<=3`**。

## 二、递归 CTE：用 `WITH RECURSIVE` 处理树与层级

基础篇提过 CTE，递归版是"非递归锚点 + UNION ALL 递归体"：

```sql
-- 查询某个部门下的整棵组织树
WITH RECURSIVE dept_tree AS (
  -- 锚点：起点部门
  SELECT id, parent_id, name, 1 AS level
  FROM dept
  WHERE id = 10
  UNION ALL
  -- 递归：连接子节点
  SELECT d.id, d.parent_id, d.name, t.level + 1
  FROM dept d
  INNER JOIN dept_tree t ON d.parent_id = t.id
)
SELECT * FROM dept_tree;
```

### 2.1 三大经典用法

1. **树形/层级展开**（组织、类目、评论楼中楼）—— 如上。
2. **生成连续序列**（补日期缺口、造测试数据）：
   ```sql
   WITH RECURSIVE seq AS (
     SELECT 1 AS n
     UNION ALL
     SELECT n + 1 FROM seq WHERE n < 100
   )
   SELECT n FROM seq;
   ```
3. **路径/闭环检测**：递归时把访问过的节点拼成 `path`，检测 `path LIKE '%id%'` 防环（MySQL 8.0 递归 CTE **默认限制递归深度** `cte_max_recursion_depth=1000`，环会触发报错而非死循环）。

### 2.2 递归 CTE 的坑

- **必须写终止条件**（递归体的 `WHERE n < 100` / 连不上就自然结束），否则超过 `cte_max_recursion_depth` 直接报错。
- 递归体里用 `INNER JOIN` 而非 `LEFT JOIN`，否则会无限产生 NULL 行。
- 大数据树（上万节点）递归 CTE 是逐层展开，性能不如"物化路径"`path` 字段（如 `01/03/10/`）+ 前缀索引查询。真有海量层级，考虑在应用层用闭包表（closure table）。

## 三、与本仓库/进阶其他篇的衔接

- 递归 CTE 替代的"自连接树查询"，常出现在 [[RuoYi-Analysis/00-总览]] 里的部门/菜单树；RuoYi 前端用递归组件渲染，后端也可用本篇 SQL 直接返回拍平树。
- 窗口函数的"分组 TopN"思路，和 [[MySQL/进阶专题/02-索引底层与优化器执行计划/02-覆盖索引ICP与索引合并MRR]] 的覆盖索引配合，能让 `PARTITION BY` + `ORDER BY` 完全走索引、不回表。
- 大结果集的窗口计算若拖慢接口，参考 [[MySQL/进阶专题/07-企业级SQL与架构实战/04-冷热分离归档与读写分离一致性]] 做物化/预计算。

## 小结

- 排名用 `ROW_NUMBER/RANK/DENSE_RANK`；移动聚合/累计用 `ROWS/RANGE` 框架；环比用 `LAG/LEAD`。
- 窗口不改变行数，过滤排名结果要"外套一层子查询"。
- 树/层级/序列用 `WITH RECURSIVE`，记得终止条件与深度上限。
- 这俩是"把复杂关联子查询拍成线性 SQL"的利器，先想清楚语义再写。