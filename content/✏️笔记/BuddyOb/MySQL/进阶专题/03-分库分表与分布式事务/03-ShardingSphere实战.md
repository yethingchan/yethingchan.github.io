# 03-分库分表 · ShardingSphere 实战

> 前置：[[MySQL/进阶专题/03-分库分表与分布式事务/02-全局唯一ID方案]]

## ShardingSphere 是什么

Apache ShardingSphere（Java 生态主流）以**透明中间件**形式存在：
- **Sharding-JDBC**（最常用）： jar 包，拦截 JDBC，**应用内**改写 SQL、路由分片、合并结果。对业务代码几乎无侵入（改配置即可）。
- Sharding-Proxy：独立代理（像 MyCat），跨语言。

## 核心概念

| 概念 | 作用 |
|------|------|
| 逻辑表 `t_order` | 代码里写的表名 |
| 真实表 `t_order_0..3` | 物理分片 |
| 分片键 `order_id` | 路由依据 |
| 分片算法 | 算落到哪个真实表 |
| **绑定表 binding** | 两表用同一分片键 → JOIN 不出片 |
| **广播表 broadcast** | 每个分片都全量存一份（如字典表） |

## 配置示例（Sharding-JDBC，`application.yml`）

```yaml
spring:
  shardingsphere:
    datasource:
      names: ds0, ds1
      ds0: { ... jdbc-url: jdbc:mysql://db0/order }   # 两个分库
      ds1: { ... jdbc-url: jdbc:mysql://db1/order }
    rules:
      sharding:
        tables:
          t_order:
            actual-data-nodes: ds$->{0..1}.t_order_$->{0..3}   # 2库 × 4表 = 8片
            database-strategy:
              standard:
                sharding-column: user_id
                sharding-algorithm-name: db-inline
            table-strategy:
              standard:
                sharding-column: order_id
                sharding-algorithm-name: tbl-inline
        sharding-algorithms:
          db-inline:   { type: INLINE, props: { algorithm-expression: ds$->{user_id % 2} } }
          tbl-inline:  { type: INLINE, props: { algorithm-expression: t_order_$->{order_id % 4} } }
        binding-tables: t_order, t_order_item     # 同 user_id 分片 → JOIN 不出片
        broadcast-tables: t_dict                        # 字典表每片全量
```

## 代码层几乎不变

```java
// 业务代码照写逻辑表名
orderMapper.insert(order);             // 实际被路由到 t_order_2
List<Order> list = orderMapper.selectByUserId(uid);  // 单分片命中（user_id 分片键）
```
- Sharding-JDBC 在 JDBC 层**拦截并改写 SQL** → `t_order` 变成 `t_order_2`，并按 `user_id` 路由到对应库。
- 本仓库若未来接入，MyBatis 的 Mapper 仍写逻辑表名，零改 SQL。

## 绑定表（避免跨片 JOIN）

`order` 和 `order_item` **都用 `order_id` 分片** → 同一个 order 的两类数据落在同一片 → JOIN 在片内完成，**不出片**。

```sql
SELECT * FROM t_order o JOIN t_order_item i ON o.order_id = i.order_id
WHERE o.order_id = 123;
-- 两个表都按 order_id 分到同一片 → 单分片内 JOIN，快
```
不配绑定表 → 笛卡尔乘积式跨片 JOIN（每片 order × 每片 item），灾难。

## 广播表（字典/配置）

`t_dict` 这种"小且全分片都要"的表，配成广播 → 每个分片存全量，JOIN 时不用跨片拉。写操作会广播到所有分片。

## 读写分离（同套配置）

```yaml
rules:
  readwrite-splitting:
    data-sources:
      myds:
        write-data-source-name: ds-master
        read-data-source-names: ds-slave0, ds-slave1
        load-balancer-name: round-robin
```
> 与分片可叠加：先分片，分片内再读写分离。

## 强制路由（Hint）

某些查询无法从 SQL 提取分片键时，用 Hint 编程指定：
```java
try (HintManager hm = HintManager.getInstance()) {
    hm.setDatabaseShardingValue("ds0");   // 强制走 ds0
    orderMapper.selectAll();
}
```

## 结论

- Sharding-JDBC = 应用内 JDBC 拦截，**改配置不改代码**。
- 分片键 + 分片算法决定路由；**绑定表**让同键 JOIN 不出片；**广播表**解决小表全片共享。
- 与读写分离可叠加；Hint 兜底无法从 SQL 取键的场景。

下一步：[[MySQL/进阶专题/03-分库分表与分布式事务/04-跨分片查询与JOIN难题]]
