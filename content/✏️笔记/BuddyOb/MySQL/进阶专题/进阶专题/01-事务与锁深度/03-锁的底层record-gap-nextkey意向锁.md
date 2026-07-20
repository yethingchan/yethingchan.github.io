# 01-事务与锁深度 · 锁的底层（record / gap / next-key / 意向锁）

> 前置：[[MySQL/进阶专题/01-事务与锁深度/01-MVCC原理undo版本链与ReadView]]

## InnoDB 锁的层级

```
表级锁
  ├─ 表锁（LOCK TABLES，少用）
  └─ 意向锁（IS/IX，自动，表级，协调行锁）
行级锁（InnoDB 核心）
  ├─ 记录锁 Record Lock（锁单行）
  ├─ 间隙锁 Gap Lock（锁两记录之间空隙）
  └─ 临键锁 Next-Key Lock = Record + Gap（默认，防幻读）
```

## 1) 记录锁（Record Lock）

锁**索引记录本身**。
```sql
SELECT * FROM user WHERE id = 5 FOR UPDATE;  -- 锁 id=5 这行
```
- 只在**命中索引**时才是行锁。
- 若 `WHERE` 列**无索引** → 退化成**全表记录锁**（等于锁全表！经典坑）。

## 2) 间隙锁（Gap Lock）

锁**索引记录之间的"空隙"**，不含记录本身。
```sql
-- 表有 id: 1, 5, 10
SELECT * FROM t WHERE id BETWEEN 5 AND 10 FOR UPDATE;
-- 锁住 (1,5)、(5,10)、(10,+∞) 这些间隙 → 不能插入 6/7/8/9/11...
```
- 目的：防止**插入**让"当前读"看到新行（幻读）。
- Gap 锁之间**不冲突**（两个事务都能加同一个 gap 锁，因为都是"防插入"）。

## 3) 临键锁（Next-Key Lock）—— 默认

`Next-Key = Record Lock + Gap Lock`，锁"当前记录 + 前面空隙"，**左开右闭 `(prev, cur]`**。
- **RR 隔离级别默认用 next-key 锁**。
- 例：id 有 1,5,10，`WHERE id>5` 当前读会锁 `(5,10]`、`(10,+∞)`。
- 这样既能锁住已有行，又能堵住间隙插入 → **防幻读**。

> RC 隔离级别下**只有 Record Lock，没有 Gap/Next-Key**（所以 RC 有幻读风险，需自己加锁）。

## 4) 意向锁（Intention Lock，表级）

- 事务要给某行加**行锁**前，先自动在**表上加意向锁**（`IX` 或 `IS`）。
- 作用：让"表锁"能快速知道"表里有没有行被锁"，不用逐行扫。
- `IX` 与 `IX`/`IS` **兼容**，但与**表级 S/X 锁冲突**。
- 你一般感知不到它，但 `SHOW ENGINE INNODB STATUS` 里能看到。

## 加锁规则速记（RR 下，官方简化版）

1. 命中索引 → 锁扫描到的索引记录（next-key），**未命中**的记录在语句结束释放（semi-consistent 例外）。
2. 等值查询（`=`）：
   - 命中唯一索引 → 退化为 **Record Lock**（只锁那一行，不锁间隙）。
   - 命中普通索引 → **next-key**（锁记录 + 间隙）。
3. 范围查询（`>`/`<`/`BETWEEN`）→ **next-key**，直到扫描终止条件。
4. **无索引** → 全表每行加 next-key（等于锁全表，极其危险）。

## 当前读加什么锁

| 语句 | 加锁 |
|------|------|
| `SELECT ... FOR UPDATE` | 当前读 + 加 X 锁（next-key/record） |
| `SELECT ... LOCK IN SHARE MODE` | 当前读 + 加 S 锁 |
| `UPDATE/DELETE/INSERT` | 当前读 + 加 X 锁，INSERT 还有插入意向锁 |

## 例：死锁最常见的来源

```
事务A: UPDATE t SET v=1 WHERE id=5;   -- 锁 id=5
事务B: UPDATE t SET v=2 WHERE id=10;  -- 锁 id=10
事务A: UPDATE t SET v=3 WHERE id=10;   -- 等 B 释放 id=10
事务B: UPDATE t SET v=4 WHERE id=5;    -- 等 A 释放 id=5 → 互等 → 死锁
```
→ 详见 04。

## 结论

- 行锁三兄弟：Record（行）/ Gap（隙）/ Next-Key（行+隙，默认）。
- RR 默认 next-key → 防幻读。
- 无索引 WHERE → 锁全表（大坑）。
- 意向锁是表级协调器，你几乎无感但存在。

下一步：[[MySQL/进阶专题/01-事务与锁深度/04-死锁原理与排查]]
