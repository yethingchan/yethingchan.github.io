# 01-事务与锁深度 · redo 日志与崩溃恢复（WAL / 两阶段提交）

> 前置：[[MySQL/进阶专题/01-事务与锁深度/01-MVCC原理undo版本链与ReadView]]

## 为什么需要 redo log

InnoDB 改数据是先改**内存（Buffer Pool）** 的，不会每次都刷盘（随机写太慢）。但若断电，内存里已提交的事务就丢了。redo log 解决"**已提交的事务不丢**"。

## WAL（Write-Ahead Logging）

"先写日志，再改数据"。事务提交时，**先保证 redo log 落盘**，内存数据页晚点刷盘也无妨——崩溃后用 redo 重放恢复。

## redo log 结构

- **物理逻辑日志**：记"某个表空间、某页、偏移量、改了什么值"（不是 SQL，也不是整页）。
- 固定大小、**循环写**（write pos 追 checkPoint，满了停写）。
- `ib_logfile0/1`，参数 `innodb_log_file_size` / `innodb_log_files_in_group`。

## 两阶段提交（2PC，保证 redo 与 binlog 一致）

一个事务提交涉及两份日志：**redo（InnoDB）** 和 **binlog（Server 层，主从复制用）**。必须一致，否则主从数据对不上。

```
事务提交：
  1) prepare 阶段：redo 写盘（状态=PREPARE），fsync
  2) binlog 写盘：binlog 写盘，fsync
  3) commit 阶段：redo 写盘（状态=COMMIT）
```
崩溃恢复时：
- 若 redo 是 `COMMIT` 状态 → 直接提交。
- 若 redo 是 `PREPARE` 状态 → 拿 XID 去 **binlog 找对应事件**：
  - binlog 有完整提交记录 → 补提交（redo 标记 COMMIT）。
  - binlog 没有（binlog 没写盘）→ 回滚事务。

> 这样保证：**要么 redo 和 binlog 都有，要么都没有**。主从不会因为崩溃而数据不一致。

## redo 写入的三种时机（双 1 配置）

| 参数 | 含义 | 推荐 |
|------|------|------|
| `innodb_flush_log_at_trx_commit` | redo 何时 fsync | **1**（每次提交都落盘，不丢） |
| `sync_binlog` | binlog 何时 fsync | **1**（每次提交落盘） |
| 两者都=1 | "双 1" | 数据安全，性能略降 |

- `=0`：每秒刷，崩溃可能丢 1 秒。
- `=2`：写 OS 缓存不 fsync，宕机不丢（OS 没崩），断电丢。

## 组提交（group commit）

高并发下，多个事务的 redo/fsync 可以**合并成一次磁盘 IO**（redo log 组提交 + binlog 组提交），大幅提升 TPS。所以"双 1"在现代 MySQL 下性能远好于直觉。

## crash recovery 流程（重启时）

```
1. 扫描 redo，按 LSN 重放所有已提交+PREPARE 的改动到内存页
2. 对 PREPARE 状态的事务，用 XID 查 binlog 决定提交/回滚
3. 内存脏页由后台刷盘线程（page cleaner）慢慢写回
```
> undo log 也参与：未提交事务用 undo 回滚（见 01 版本链）。

## 与 undo 的分工

| 日志 | 用途 | 方向 |
|------|------|------|
| **redo** | 崩溃后**重放已提交**的改动（前滚） | 保证持久性 D |
| **undo** | 回滚**未提交**的改动 / 支撑 MVCC 版本链 | 保证原子性 A + 一致性读 |

## 结论

- redo = "已提交不丢"的基石，WAL + 循环写 + fsync。
- 2PC 让 redo 与 binlog 一致，主从不会因崩溃错乱。
- "双 1"最安全，组提交抵消性能损耗。

下一步：[[MySQL/进阶专题/01-事务与锁深度/03-锁的底层record-gap-nextkey意向锁]]
