# 02 · SQL 分类与基础查询

> 进阶路线：[[00-总览与学习路线]] → L1 基础 ｜ 上接：[[01-数据库与MySQL概述]]

## 1. SQL 语句分类

| 分类 | 全称 | 作用 | 典型语句 |
|------|------|------|----------|
| DDL | Data Definition | 定义结构（库/表/索引） | `CREATE` `ALTER` `DROP` `TRUNCATE` |
| DML | Data Manipulation | 操作数据 | `INSERT` `UPDATE` `DELETE` |
| DQL | Data Query | 查询 | `SELECT` |
| DCL | Data Control | 权限控制 | `GRANT` `REVOKE` |
| TCL | Transaction | 事务控制 | `BEGIN` `COMMIT` `ROLLBACK` `SAVEPOINT` |

## 2. SELECT 基础语法

```sql
SELECT [DISTINCT] 列1, 列2, 函数()
FROM 表
[WHERE 条件]
[GROUP BY 列]
[HAVING 分组后条件]
[ORDER BY 列 [ASC|DESC]]
[LIMIT 偏移, 条数];
```

### SELECT 的**执行顺序**（极易错，务必记牢）

```
1. FROM        -- 先确定从哪张表
2. WHERE       -- 行级过滤（不能用聚合函数）
3. GROUP BY    -- 分组
4. HAVING      -- 组级过滤（可用聚合函数）
5. SELECT      -- 选择列、计算
6. ORDER BY    -- 排序（能用 SELECT 别名）
7. LIMIT       -- 截取
```

> 因此 `WHERE` 里**不能**用 `SELECT` 里定义的别名，`ORDER BY` 可以。

## 3. 基础查询示例

```sql
-- 查全部列（生产慎用 SELECT *）
SELECT * FROM sys_user;

-- 指定列 + 别名 + 去重
SELECT DISTINCT dept_id, user_name AS name FROM sys_user;

-- 条件：比较/范围/集合/空值
SELECT * FROM sys_user
WHERE status = 0
  AND create_time >= '2026-01-01'
  AND dept_id IN (10, 20)
  AND user_name LIKE '张%'      -- % 任意多字符, _ 单个字符
  AND email IS NOT NULL;

-- 排序 + 分页（LIMIT 偏移, 数量）
SELECT * FROM sys_user ORDER BY create_time DESC LIMIT 0, 10;
```

## 4. 常用运算符

| 类型 | 示例 |
|------|------|
| 比较 | `=`, `!=`/`<>` , `>`, `<`, `>=`, `<=` |
| 范围 | `BETWEEN 1 AND 10`, `IN (...)`, `NOT IN` |
| 模糊 | `LIKE '张%'`, `LIKE '%中%'` |
| 空值 | `IS NULL`, `IS NOT NULL`（注意不是 `= NULL`） |
| 逻辑 | `AND`, `OR`, `NOT`, 优先级 `NOT > AND > OR` |

## 5. INSERT / UPDATE / DELETE

```sql
INSERT INTO sys_user(user_name, dept_id) VALUES ('张三', 10);

-- 批量插入（推荐，一次网络往返）
INSERT INTO sys_user(user_name, dept_id) VALUES
  ('李四', 10), ('王五', 20);

UPDATE sys_user SET status = 1 WHERE id = 100;
DELETE FROM sys_user WHERE id = 100;
```

> ⚠️ `DELETE` / `UPDATE` **必须带 WHERE**，否则全表操作（生产事故高发）。
> `TRUNCATE` 清空表比 `DELETE` 快（不记 undo、无法回滚、重置自增），属 DDL。

## 6. 学习检查点

- [ ] 能默写 SELECT 7 步执行顺序
- [ ] 知道 `WHERE` 不能用别名的原因
- [ ] 会写 IN / LIKE / BETWEEN / IS NULL
- [ ] 知道 DELETE 忘加 WHERE 的后果

> 下一篇：[[03-数据类型与字符集]]（怎么选对字段类型，直接影响性能与空间）
