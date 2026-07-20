# 10 · 事务 ACID 与隔离级别

> 进阶路线：[[00-总览与学习路线]] → L3 性能原理 ｜ 前置：[[04-表设计与约束]]

事务保证「一组操作要么全成、要么全败」。并发下要懂隔离级别。

## 1. ACID

| 特性 | 含义 |
|------|------|
| **A**tomicity 原子性 | 事务内操作要么全做，要么全不做（靠 undo log 回滚） |
| **C**onsistency 一致性 | 事务前后数据都满足约束（业务层也参与） |
| **I**solation 隔离性 | 并发事务互不干扰（靠锁 + MVCC） |
| **D**urability 持久性 | 提交后数据不丢（靠 redo log + 刷盘） |

## 2. 并发三大问题

| 问题 | 现象 |
|------|------|
| **脏读** | 读到别的事务**未提交**的数据（它可能回滚） |
| **不可重复读** | 同一事务内两次读同一行，**被别的事务改了**（重点在 UPDATE） |
| **幻读** | 同一事务内两次查询，**多了/少了行**（重点在 INSERT/DELETE） |

## 3. 四种隔离级别（MySQL）

| 级别 | 脏读 | 不可重复读 | 幻读 |
|------|------|-----------|------|
| `READ UNCOMMITTED` 读未提交 | ❌ 有 | ❌ 有 | ❌ 有 |
| `READ COMMITTED` 读已提交 (Oracle默认) | ✅ | ❌ 有 | ❌ 有 |
| `REPEATABLE READ` 可重复读 (**MySQL默认**) | ✅ | ✅ | ⚠️ 基本防住(InnoDB) |
| `SERIALIZABLE` 串行化 | ✅ | ✅ | ✅ |

```sql
SELECT @@transaction_isolation;   -- 看当前级别
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
```

> **MySQL InnoDB 在 `REPEATABLE READ` 下靠 MVCC + Next-Key Lock 基本杜绝幻读**，这是它敢把这个当默认级别的原因。

## 4. 事务控制语句

```sql
BEGIN;                       -- 或 START TRANSACTION
UPDATE account SET bal=bal-100 WHERE id=1;
UPDATE account SET bal=bal+100 WHERE id=2;
COMMIT;                      -- 成功提交
-- ROLLBACK;                 -- 失败回滚
```

## 5. MVCC 一句话原理（面试常考）

- 每行有隐藏的 `trx_id`（最后修改事务ID）和 `roll_ptr`（指向 undo log 旧版本）。
- 事务开启时生成 **ReadView**，只读「自己开始前已提交」的版本 → 实现「可重复读」且**读不加锁、读写不阻塞**。
- 版本链 + ReadView 决定每行对当前事务「可见否」。

## 6. 大事务的危害（企业级经验）

- 锁持有久 → 阻塞别人、易死锁。
- 回滚慢（undo 大）。
- 占连接、拖垮池。
> **原则：事务越小越短越好**，把非 DB 操作（RPC、发消息）挪到事务外。

## 7. 学习检查点

- [ ] 背下 ACID 与各自靠什么实现
- [ ] 区分脏读/不可重复读/幻读
- [ ] 知道 MySQL 默认隔离级别与原因
- [ ] 知道大事务的危害

> 下一篇：[[11-锁机制与并发控制]]（隔离级别底下，锁在怎么干活）
