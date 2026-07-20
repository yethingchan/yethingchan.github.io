---
title: 高并发扣减防超卖（WMS）
---

# 02 高并发扣减防超卖（WMS）

> 前置：[[MySQL/进阶专题/07-企业级SQL与架构实战/00-索引]] ｜ 事务与锁基础：[[MySQL/进阶专题/01-事务与锁深度/03-锁的底层record-gap-nextkey意向锁]]

库存扣减是电商/仓储（WMS）系统里**最容易出资损**的场景。一个"先查后扣"的朴素写法，在高并发下必然超卖。本篇讲清原子扣减的几种正确姿势。

## 一、错误示范：先 SELECT 再 UPDATE（必超卖）

```java
// ❌ 两个线程同时读到 stock=1，都判断通过，都扣成 -1（超卖）
Integer stock = mapper.selectStock(sku);   // 读
if (stock >= n) {
    mapper.updateStock(sku, stock - n);     // 写（非原子）
}
```

并发下，线程 A、B 同时 `SELECT` 拿到 `stock=1`，都通过判断，结果库存变成 `-1`。**读和写在两个事务里，中间有窗口可被穿插。**

## 二、正确姿势一：单条原子 UPDATE（首选）

把"判断 + 扣减"压进**一条** SQL，利用 InnoDB 行锁 + 条件，让数据库保证原子性：

```sql
UPDATE wms_stock
SET stock = stock - #{n},
    update_time = NOW()
WHERE sku = #{sku}
  AND stock >= #{n};        -- 关键：条件里校验余量
```

应用层看**受影响行数**：

```java
int rows = mapper.deduct(sku, n);   // 返回受影响的行数
if (rows == 0) {
    throw new BizException("库存不足");   // 0 行 = 扣减失败（余量不够）
}
// rows == 1 才代表扣减成功
```

为什么对：

- `WHERE sku=?` 命中**聚簇索引行锁**，并发扣同一 SKU 时，InnoDB 让它们**串行**执行这条 UPDATE。
- `stock >= n` 在加锁后、修改前再次校验，不足则 0 行，天然防超卖。
- 整件事在一个语句 + 一个事务里完成，没有"读—写"窗口。

> 呼应 [[MySQL/进阶专题/01-事务与锁深度/05-隔离级别实现与长事务危害]]：这条语句本身很短，行锁持有时间极短，不会放大锁竞争。务必**别在外面套一个长事务**去查这查那。

## 三、正确姿势二：乐观锁 version（适合"带版本"的领域）

```sql
UPDATE wms_stock
SET stock = stock - #{n}, version = version + 1
WHERE sku = #{sku}
  AND version = #{oldVersion}
  AND stock >= #{n};
```

- 并发冲突时 `version` 已变，`WHERE version=oldVersion` 命中 0 行，应用层重试或报错。
- 优点：避免长事务行锁；缺点：高冲突下重试开销大。库存强扣减场景**不如姿势一直接**（姿势一不依赖版本重读）。

## 四、正确姿势三：Redis 预扣 + 异步落库（超高并发）

当 QPS 极高、DB 扛不住时，把"扣减"前置到 Redis（单线程 + Lua 原子）：

```lua
-- deduct.lua：原子预扣，返回剩余量；不足返回 -1
local stock = tonumber(redis.call('GET', KEYS[1]))
local n = tonumber(ARGV[1])
if stock < n then return -1 end
return redis.call('DECRBY', KEYS[1], n)
```

- 请求先打 Redis 预扣，成功再异步（MQ）把扣减消息落 MySQL 做持久账。
- 关键点：
  1. **Redis 与 DB 的最终一致**靠可靠消息 + 幂等消费（见 [[MySQL/进阶专题/07-企业级SQL与架构实战/03-缓存与数据库一致性(Canal)]]）。
  2. 用 `DECRBY` 而非 `GET`+`SET`，保证原子。
  3. 防止 Redis 宕机丢扣减：Redis 开启 AOF 持久化，或 DB 作为"最终真相源"在消费时**再做一次 `stock >= n` 的原子校验**（双重保险，杜绝 Redis 单点故障导致的超卖）。
- 这就回到姿势一：即便 Redis 放行了，DB 落账时仍用 `WHERE stock >= n` 兜底。

## 五、避坑清单

- ❌ 绝对不要用 `SELECT ... FOR UPDATE` 先锁再判断再扣——在长事务里锁住行，并发直接掉到串行，且容易死锁（见 [[MySQL/进阶专题/01-事务与锁深度/04-死锁原理与排查]]）。除非你明确要"锁住行做多步复杂处理"。
- ❌ 不要相信应用层 `if (stock >= n)` 判断，那只是"读时"的快照。
- ✅ 扣减 SQL 一定带 `AND stock >= n`，靠受影响行数判断成败。
- ✅ 扣减涉及的 `sku` 字段必须有索引（最好是聚簇/唯一索引），否则行锁会退化成间隙/表锁，影响面爆炸。
- ✅ 配合 [[MySQL/进阶专题/06-参数调优与运维实战/02-连接线程与wait_timeout]] 的连接池与超时设置，避免扣减事务卡住占连接。

## 六、与本仓库的衔接

- RuoYi 的库存/订单业务若落地本模式，扣减 Mapper 写在 `cn.yething` 的 domain/mapper 下，XML 参考 [[MyBatis Mapper XML/进阶专题/04-批量与性能优化/01-foreach批量插入与MySQL批处理]] 的批处理思路做批量出库。
- 分布式多库时，单条原子 UPDATE 仍只在"单分片"内有效；跨 SKU 的分布式扣减要上 [[MySQL/进阶专题/03-分库分表与分布式事务/05-分布式事务Seata与最终一致]] 的 TCC/Seata AT。

## 小结

- 防超卖的核心：**判断和扣减必须原子**，交给数据库单条 `UPDATE ... WHERE stock >= n`。
- 看受影响行数（0=失败）而非自己读的值。
- 超高并发：Redis 预扣（Lua 原子）+ DB 异步落账（二次 `stock >= n` 兜底）+ 幂等消息。
- 这层 SQL 是 WMS 资损的第一道防线，务必单测并发 + 压测验证。