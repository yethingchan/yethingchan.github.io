# 01-事务与锁深度 · MVCC 原理（undo 版本链 + ReadView）

> 前置：[[MySQL/进阶专题/01-事务与锁深度/00-索引]]

## MVCC 是什么

**多版本并发控制（Multi-Version Concurrency Control）**：每行数据保留多个历史版本（在 undo log 里），读请求按自己的"快照"取对应版本——**读不加锁，读写不阻塞**。这是 MySQL "读性能好"的根本原因。

## 隐藏列（每行都有）

InnoDB 每行隐含三列：
- `DB_TRX_ID`：最近一次修改这行的事务 id。
- `DB_ROLL_PTR`：回滚指针，指向 undo log 里**上一个版本**。
- `DB_ROW_ID`：行 id（无主键时做聚簇索引）。

## undo 版本链

```
事务10: UPDATE salary=1000  (trx_id=10, roll_ptr → 旧版本)
事务20: UPDATE salary=2000  (trx_id=20, roll_ptr → 版本10)
事务30: UPDATE salary=3000  (trx_id=30, roll_ptr → 版本20)

最新行: salary=3000, trx_id=30, roll_ptr → [20:2000] → [10:1000] → [初始:500]
            ↑ undo 版本链（都在 undo log 里）
```

## ReadView（读视图）

一个事务**第一次快照读**时生成 ReadView，记录：
- `m_ids`：当前**活跃（未提交）** 的事务 id 列表。
- `min_trx_id` / `max_trx_id`：活跃事务的最小/最大 id。
- `creator_trx_id`：自己的事务 id。

**可见性判断规则**（从最新版本沿链往下找，找到第一个"对我可见"的版本）：
- 版本 `trx_id == creator_trx_id` → 自己改的，可见。
- 版本 `trx_id < min_trx_id` → 已提交（在我之前就结束了），可见。
- 版本 `trx_id` 在 `[min, max]` 且**不在** `m_ids`（已提交）→ 可见。
- 版本 `trx_id` 在 `m_ids`（还活跃未提交）→ 不可见，沿 `roll_ptr` 找上一版。
- 版本 `trx_id > max_trx_id` → 在我之后开启，不可见，继续找。

## 快照读 vs 当前读

| 类型 | 语句 | 行为 |
|------|------|------|
| 快照读 | `SELECT ...`（普通） | 走 MVCC，读快照，不加锁 |
| 当前读 | `SELECT ... FOR UPDATE` / `LOCK IN SHARE MODE` | 读**最新已提交**版本，加锁 |
| 当前读 | `INSERT/UPDATE/DELETE` | 改最新版本，加锁 |

> RR 级别下，**快照读**复用事务开始时的 ReadView（整个事务看到同一快照 → 防幻读）；RC 级别下**每次**快照读都生成新 ReadView（所以 RC 有不可重复读）。

## 例：为什么 RR 下"读不加锁也不脏读、可重复读"

```
T1 (trx=30, RR): SELECT salary  → 看到 1000
T2 (trx=40):      UPDATE salary=2000; COMMIT;
T1 再 SELECT salary  → 仍看到 1000（复用同一 ReadView，沿 undo 链取旧版）
```
T1 全程不加锁，但看到的始终是开始时的快照——这就是 MVCC 的威力。

## 为什么"快照读"看不到别人刚插入的（防幻读）

RR 下 T1 的 ReadView 在第一次读时生成，之后插入的新行 `trx_id` 必然 > `max_trx_id` → 沿规则判定"不可见"。所以 T1 整个事务都看不到新插入的行 = 防幻读。

> 注意：RR 防幻读**只对快照读有效**。当前读（`FOR UPDATE`）靠 **next-key 锁** 防幻读（见 03）。

## 结论

- MVCC = undo 版本链 + ReadView 可见性规则。
- 快照读走 MVCC，读不加锁，靠"版本链"拿到一致性视图。
- RR 防幻读 = 快照读复用 ReadView + 当前读加 next-key 锁。

下一步：[[MySQL/进阶专题/01-事务与锁深度/02-redo日志与崩溃恢复两阶段提交]]
