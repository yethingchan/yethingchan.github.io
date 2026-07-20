# 04-高可用复制 · MGR 组复制（Paxos）

> 前置：[[MySQL/进阶专题/04-高可用复制与集群/02-GTID与半同步无损复制]]

## MGR 是什么

**MySQL Group Replication（组复制）**：多个 MySQL 节点组成一个**复制组**，基于 **Paxos（具体是 XCom）** 协议实现**多数派一致**。是 MySQL 官方的高可用方案（替代手写 MHA）。

## 两种模式

| 模式 | 说明 | 读写 |
|------|------|------|
| **单主（Single-Primary，默认）** | 组内只有一个主（读写），其余是从（只读） | 主写、从读 |
| **多主（Multi-Primary）** | 所有节点都可写 | 多点写，冲突检测 |

> 多数用**单主**：自动选主、主挂自动切到新主，业务无感。

## Paxos 多数派（核心）

```
3 节点组：写事务要 ≥ 2 个节点同意（多数派）才提交
5 节点组：要 ≥ 3 个同意
允许 ≤ (N-1)/2 个节点挂掉，集群仍可用（如 3 节点挂 1 个 OK）
```
- 基于**共识**，不依赖"主从延迟"那套。
- 比异步/半同步更**强一致**（多数派确认）。

## 冲突检测（多主模式关键）

多主下两个节点同时改**同一行** → MGR 用**冲突检测**：
```
每个写事务带"改了哪些行的 GTID 版本"
提交时若有别的节点已改了同一行（版本冲突）
→ 后提交的事务被**回滚**（报错给客户端重试）
```
- 单主模式基本无冲突（只有一个写点）。
- 多主模式要避免"热点行多节点并发改"（如全局计数器）。

## 单主模式自动选主

```
主节点挂 → 剩余节点重新共识选新主 → 新主自动置为读写
业务通过 VIP / 中间件重连新主
```
- 选主基于 **UUID 顺序 / 权重**（`group_replication_member_weight`）。
- 配合 **MySQL Router**（官方轻量代理）自动把写流量导到当前主。

## 配置要点（单主 3 节点）

```ini
# 所有节点
server_id = 1/2/3
gtid_mode = ON
enforce_gtid_consistency = ON
binlog_format = ROW
transaction_write_set_extraction = XXHASH64
plugin_load_add = 'group_replication.so'
group_replication_group_name = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
group_replication_local_address = 'node1:33061'     # 内部通信端口
group_replication_group_seeds = 'node1:33061,node2:33061,node3:33061'
group_replication_single_primary_mode = ON           # 单主
group_replication_bootstrap_group = OFF             # 只有首节点首次启动置 ON
```

## MGR vs 传统主从

| 维度 | 传统主从 + 半同步 | MGR |
|------|-------------------|-----|
| 一致性 | 半同步（多数派不强） | **Paxos 多数派** |
| 故障切换 | 需外部工具（MHA 等） | **内置自动选主** |
| 脑裂 | 需外部仲裁 | 多数派防脑裂 |
| 多写 | 不支持 | 支持（多主） |
| 复杂度 | 低 | 中（配置多、网络要求高） |

## 适用与注意

- 节点间**网络要好**（Paxos 对延迟/抖动敏感），异地多活慎重。
- 单主模式下基本无冲突，最稳。
- 不是"替代备份"——MGR 防的是"挂"，不防"误删"（误删照样同步，见 [[MySQL/进阶专题/05-备份恢复与容灾]]）。

## 结论

- MGR = 官方组复制，Paxos 多数派，强一致 + 自动选主。
- 默认**单主**最稳（自动切主、业务无感）。
- 多主有冲突检测，避开热点行并发。
- 比传统主从 + 外部切换更省心，但网络要求高。

下一步：[[MySQL/进阶专题/04-高可用复制与集群/04-读写分离中间件]]
