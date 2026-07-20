# 04-高可用复制 · 主从复制原理（binlog 三格式）

> 前置：[[MySQL/进阶专题/04-高可用复制与集群/00-索引]]

## 复制的整体链路

```
主库 Master
  │  事务提交 → 写 binlog
  ▼
  dump 线程（主库）── 推 binlog 给从库
  ▼
  IO 线程（从库）── 收 binlog → 写 relay log（中继日志）
  ▼
  SQL 线程（从库）── 读 relay log → 回放（重执行 SQL/事件）
```
- **主库 dump 线程**：1 个从库 1 个 dump 线程，推送 binlog。
- **从库 IO 线程**：拉取 → 写 `relay log`。
- **从库 SQL 线程**：回放 relay log → 应用到从库数据。

## binlog 三种格式（核心差异）

| 格式 | 记录内容 | 优点 | 缺点 | 适用 |
|------|---------|------|------|------|
| **STATEMENT** | 原始 SQL 语句 | 日志小 | **不安全**：`NOW()/UUID()/触发器` 在主从结果不一致 | 老版本 |
| **ROW** | 每行**改前/改后**的值 | **最安全**（主从绝对一致） | 日志大（批量改百万行=百万事件） | **8.0 默认** |
| **MIXED** | 能 STATEMENT 就用它，不安全时转 ROW | 折中 | 行为不透明 | 过渡 |

> 现在**几乎都 ROW**。代价是 binlog 大（批量 UPDATE/DELETE 大表要小心，见 [[MySQL/进阶专题/05-备份恢复与容灾/03-PITR时间点恢复与闪回]]）。

## ROW 格式下看 binlog

```bash
mysqlbinlog --verbose --base64-output=DECODE-ROWS binlog.000001
# 看到的是：
# UPDATE `db`.`t` WHERE @1=5 @2='old' SET @1=5 @2='new'
```

## 主从延迟（replication lag）来源

```
从库落后主库 N 秒 = SQL 线程回放赶不上主库写入
```
常见原因：
1. **从库硬件/配置弱**于主库（SQL 单线程回放是瓶颈）。
2. **大事务**：主库一个事务几秒提交，从库要几秒回放 → 整个延迟。
3. **从库有读压力**占满了回放资源。
4. 老版本**单 SQL 线程**（5.6 前）→ 5.7+ 有**多线程复制（MTS）**：按库/逻辑时钟并行回放。

## 多线程复制 MTS（5.7+）

```
slave_parallel_workers = 8          # 并行回放线程数
slave_parallel_type = LOGICAL_CLOCK  # 同事务依赖的并行
```
- 按"事务依赖"并行回放，大幅降低延迟。
- 本仓库 MySQL 8 默认支持，生产应开。

## 延迟监控

```sql
SHOW REPLICA STATUS\G    -- 8.0 用 REPLICA，旧版 SLAVE
-- 关注：
--   Seconds_Behind_Source  (延迟秒数)
--   Relay_Log_Pos / Exec_Source_Log_Pos (回放进度)
```

## 主从不一致的典型场景

- 主库用 `STATEMENT` + `UUID()` → 从库值不同。
- 从库被**直接写入**（绕开复制）→ 主从漂移（禁止从库写！）。
- 非幂等事件回放失败 → SQL 线程停（需 `sql_replica_skip_counter` 或 GTID 跳过）。

## 结论

- 复制链路：binlog → dump 推 → IO 写 relay → SQL 回放。
- binlog 用 **ROW**（安全），日志大会影响备份/恢复。
- 延迟主因：大事务 + 单线程回放 → 开 MTS 并行。
- 从库**只读**，禁直写，否则漂移。

下一步：[[MySQL/进阶专题/04-高可用复制与集群/02-GTID与半同步无损复制]]
