# 04-高可用复制 · GTID 与半同步/无损复制

> 前置：[[MySQL/进阶专题/04-高可用复制与集群/01-主从复制原理binlog三格式]]

## GTID（全局事务 ID）

传统复制靠"binlog 文件名 + 位置（pos）"定位同步点，切换从库时要手动找 pos，易错。GTID 用**全局唯一事务 ID** 取代：

```
GTID = source_id:transaction_id
例：3E11FA47-71CA-11E1-9E33-C80AA942956:23
      （server_uuid : 该实例第 23 个事务）
```
- 每个事务一个 GTID，**全局唯一**，主从都认。
- 从库记录"已执行的 GTID 集合"（`gtid_executed`），新主库看这个集合就知道"从哪续"。

## GTID 复制的好处

```
传统切换：
  主挂 → 找数据最新的从 → 查它的 binlog pos → CHANGE MASTER 指向新主 → 易错、要算 pos

GTID 切换：
  主挂 → CHANGE REPLICATION SOURCE 到新主 → 自动按 gtid_executed 续传
  → 不用算 pos，自动跳过已执行的
```
- **自动定位、自动跳重**，故障切换工具（MHA/Orchestrator）依赖它。

## 半同步复制（Semi-Sync）

默认异步复制：主库提交即返回客户端，**不等从库** → 主挂可能丢从库还没收到的数据。

半同步：主库提交后**至少等一个从库 ACK（收到 relay log）** 才返回成功。

```
主提交 → 写 binlog → 等 ≥1 从库 ACK（binlog 已到从的 relay）
       → 才向客户端返回成功
```

配置：
```sql
-- 主从都装插件
INSTALL PLUGIN rpl_semi_sync_master SONAME 'semisync_master.so';
INSTALL PLUGIN rpl_semi_sync_replica SONAME 'semisync_slave.so';
SET GLOBAL rpl_semi_sync_master_enabled = 1;
SET GLOBAL rpl_semi_sync_replica_enabled = 1;
```

## 半同步 vs 无损复制（after-sync）

| 模式 | 行为 | 数据安全性 |
|------|------|-----------|
| **传统半同步（after-commit，旧）** | 主库**已提交**（事务可见）才等 ACK → 若此时主挂、从没收到 → 主上可见的事务在从上不存在 → **幻读/丢** | 有窗口 |
| **无损复制（after-sync，5.7+ 默认）** | 主库**写 binlog 后、提交前**等 ACK → 从没收到就回滚主事务 → 主从一致 | **无损** |

> 现在默认是 **after-sync（无损）**，配置 `rpl_semi_sync_master_wait_point = AFTER_SYNC`。

## 半同步的代价与退化

- **延迟增加**：每次提交多等一次网络 RTT（从库 ACK）。
- **超时退化**：从库 ACK 超时（默认 10s）→ **自动退化成异步**，不再阻塞（保可用，丢安全性）。
- 折中：用 **一主两从 + 至少一从半同步** → 一个从挂了另一个还能 ACK。

## 与"双 1"的关系

半同步保证"binlog 到从的 relay"；`sync_binlog=1` + `innodb_flush_log_at_trx_commit=1` 保证"主库 binlog/redo 落盘"（见 [[MySQL/进阶专题/01-事务与锁深度/02-redo日志与崩溃恢复两阶段提交]]）。二者配合 = 主从都不丢。

## 结论

- GTID 用全局事务 ID 取代 pos → **切换自动续传、不丢重**。
- 半同步：主提交**至少等一个从 ACK**，防主挂丢数据。
- **无损（after-sync）** 是默认，比旧半同步更安全。
- 代价是提交多一次 RTT；超时退化异步（保可用）。
- 配双 1 + 半同步 = 主从强一致。

下一步：[[MySQL/进阶专题/04-高可用复制与集群/03-MGR组复制Paxos]]
