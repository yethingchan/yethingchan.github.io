# 06-参数调优 · 连接线程与 `wait_timeout`

> 前置：[[MySQL/进阶专题/06-参数调优与运维实战/01-Buffer-Pool调优与预热]]

## 连接模型

```
应用连接池（Hikari/Druid）
  └─ N 个 TCP 连接 ──> MySQL 服务端连接线程
                            └─ 每个连接一个"线程"（或线程池复用）
```
> 本仓库用 Druid（见 [[RuoYi-Analysis/03-后端-配置层]] 的 Druid 配置）。连接数 = 应用连接池大小。

## `max_connections` —— 连接数上限

```
max_connections = 1000      # 默认 151
```
- 超过 → 新连接报 `Too many connections`（雪崩信号）。
- 设多大？≈ **应用连接池总和 + 管理预留**。别盲目设几万（每个连接占内存 ~几 MB + 线程开销）。
- 临时救急：`SET GLOBAL max_connections=2000`（重启失效，改配置文件才永久）。

## `wait_timeout` / `interactive_timeout` —— 闲置连接回收

```
wait_timeout = 28800          # 非交互连接闲置 N 秒后断开（默认 8 小时！）
interactive_timeout = 28800    # 交互（mysql 客户端）连接
```
- **默认 8 小时太长**：应用连接池里"假死"连接、网络断了没感知的连接会一直占着 → 慢慢吃满 `max_connections`。
- 推荐设短（如 `300`~`600` 秒），配合**连接池的探活/回收**：
  ```
  Druid: test-while-idle / validation-query / min-evictable-idle-time-millis
  ```
- 效果：闲置连接被 MySQL 主动断开 → 不会堆积 → 连接数可控。

## 连接池的关键参数（应用侧，比服务端更常踩）

| 参数 | 作用 | 坑 |
|------|------|-----|
| `max-active` / `maximum-pool-size` | 连接池上限 | 太大 → 压垮 MySQL；太小 → 应用等连接 |
| `max-wait` | 拿不到连接等多久 | 太长 → 请求线程堆积；太短 → 频繁报错 |
| `min-idle` | 最小空闲连接 | 保活，避免冷建连 |
| `test-while-idle` | 借出前探活 | 防拿到"已断的连接" |
| `max-lifetime` | 连接最大寿命 | **要 < MySQL wait_timeout**，否则拿到被服务端断开的"死连接" |

> 经典坑：`max-lifetime` > `wait_timeout` → 连接池以为连接活着，MySQL 已断开 → 报 `MySQL server has gone away` / `Broken pipe`。**让连接的寿命比服务端超时短**。

## 线程池（thread pool，企业版特性）

社区版 MySQL 默认**每连接一线程**，高并发下线程上下文切换开销大。

- **企业版**有 `thread_handling=pool-of-threads` 线程池（限并发执行线程数）。
- 社区版替代：靠**连接池限流** + 控制 `max_connections`，避免无脑建连。

## 连接雪崩的成因与防护

```
雪崩链条：
  慢查询/大事务 → 占住连接不释放
  → 应用连接池耗尽 → 新请求等连接（max-wait）
  → 等不到 → 报错/线程堆积
  → 更多重试 → 更慢 → 全崩
```
防护：
1. **控制连接池上限**，别无脑放大。
2. **`wait_timeout` 缩短** + 探活，及时回收死连接。
3. **慢查询治理**（见 [[MySQL/12-慢查询分析与调优]]）—— 根因在慢 SQL 占连接。
4. **限流/熔断** 在应用层挡住重试风暴。

## 监控连接

```sql
SHOW GLOBAL STATUS LIKE 'Threads_connected';   -- 当前连接数
SHOW GLOBAL STATUS LIKE 'Threads_running';     -- 正在跑的（比 connected 关键）
SHOW PROCESSLIST;                                  -- 看谁占着连接、State 是什么
SHOW GLOBAL STATUS LIKE 'Aborted_connects';  -- 失败连接（可能被攻击/配置错）
```
> `Threads_running` 持续高 = 有慢查询/锁在跑，比 `Threads_connected` 更能说明问题。

## 结论

- `max_connections` 按"连接池总和 + 预留"设，别盲目几万。
- `wait_timeout` 缩短（300~600s）+ 连接池探活，防闲置连接吃满。
- **连接池 `max-lifetime` < MySQL `wait_timeout`**，防死连接报错。
- 雪崩根因多在慢 SQL 占连接，治本靠慢查询治理。

下一步：[[MySQL/进阶专题/06-参数调优与运维实战/03-大表DDL-gh-ost与instant]]
