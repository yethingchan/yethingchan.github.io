# 07-企业实战 · 高并发扣减防超卖（呼应 WMS）

> 前置：[[MySQL/进阶专题/07-企业级SQL与架构实战/01-窗口函数与递归CTE深度]] ｜ 锁：[[MySQL/进阶专题/01-事务与锁深度/03-锁的底层record-gap-nextkey意向锁]]

## 场景（本仓库 WMS 库存）

```
扣库存：库存 100，并发 200 个"扣 1"请求 → 不能扣成 -100（超卖）
```

## 方案 1：数据库乐观锁（版本号 / CAS）

```sql
-- 表加 version 列
UPDATE wms_stock SET qty = qty - 1, version = version + 1
WHERE sku_id = ? AND version = #{oldVersion} AND qty >= 1;
```
- `WHERE version = oldVersion`：并发时只有一个能成功（其他拿到的 oldVersion 已变 → 影响行数 0）。
- 应用判断 `affectedRows == 0` → 重试或报"库存不足"。
- **优点**：无长锁，并发高。
- **缺点**：高冲突下大量重试；要重试逻辑。

> 本仓库 MyBatis-Plus 的 `@Version` + `OptimisticLockerInnerInterceptor` 就是自动拼这串（见 [[MyBatisPlus/12-逻辑删除与乐观锁]]）。

## 方案 2：单条 SQL 原子扣减（最推荐）

```sql
UPDATE wms_stock SET qty = qty - #{n}
WHERE sku_id = ? AND qty >= #{n};   -- 数据库原子判断，不超卖
```
- `qty >= n` 在**同一条 UPDATE 里**保证不扣成负。
- 应用判断 `affectedRows == 1` 成功，0 则"库存不足"。
- **不需要 version 列、不需要重试**，单 SQL 原子 → 最稳。
- 配合 `WHERE sku_id=?`（走主键/唯一索引）→ **行锁**，并发互不干扰。

## 方案 3：Redis 预扣 + 异步落库（超高并发）

```
1) 先用 Lua 脚本在 Redis 原子预扣（INCRBY 负数 + 判断 >=0）
2) 预扣成功 → 异步消息落库（UPDATE 库存表）
3) 落库失败 → 回补 Redis 预扣
```
- 扛住瞬时洪峰（Redis 单线程原子）。
- 最终一致性（落库可能延迟，见 [[MySQL/进阶专题/03-分库分表与分布式事务/05-分布式事务Seata与最终一致]]）。
- 本仓库若做秒杀/大促类，走这条。

## ⚠️ 致命坑：先 SELECT 再 UPDATE（超卖根源）

```java
// ❌ 错误写法
int q = stockMapper.selectQty(sku);   // 查到 100
if (q >= 1) stockMapper.updateQty(sku, q - 1);  // 并发下两个请求都查到 100 → 都扣 → 超卖
```
- 查和改**不是原子** → 并发下都读到旧值 → 都通过判断 → 超卖。
- 永远用**方案 2 的单条原子 UPDATE**，别"先查后改"。

## ⚠️ 另一个坑：扣减没加 `qty >= n` 判断

```sql
UPDATE wms_stock SET qty = qty - 1 WHERE sku_id = ?;   -- ❌ 没判断，扣成负
```
- 必须 `AND qty >= 1`（方案 2）。

## 事务与锁的注意

- 扣减 SQL 走 `WHERE sku_id`（唯一索引）→ **行锁**，不同 sku 不互斥。
- 别在扣减事务里干别的事（RPC/大循环）→ 持锁久 → 死锁/阻塞（见 [[MySQL/进阶专题/01-事务与锁深度/05-隔离级别实现与长事务危害]]）。
- 扣减 + 写流水：**同一事务**，但事务要短。

## 完整落地（本仓库风格）

```java
@Transactional   // 短事务
public boolean deduct(Long skuId, int n) {
    int rows = stockMapper.deductAtomic(skuId, n);  // UPDATE ... SET qty=qty-? WHERE sku=? AND qty>=?
    if (rows == 1) {
        stockLogMapper.insert(...);  // 写流水
        return true;
    }
    return false;   // 库存不足
}
```

## 结论

- **超卖根源**："先 SELECT 再 UPDATE"非原子 → 并发都读旧值。
- **最稳方案**：单条 `UPDATE ... SET qty=qty-n WHERE sku=? AND qty>=n`（行锁 + 原子判断）。
- 高冲突用**乐观锁 version**（CAS + 重试）。
- 超高并发用 **Redis 预扣 + 异步落库**（最终一致）。
- 扣减事务要短，别夹 RPC。

下一步：[[MySQL/进阶专题/07-企业级SQL与架构实战/03-缓存与数据库一致性(Canal)]]
