# 03-缓存机制深度 · 二级缓存原理与 `Cache` 接口

> 前置：[[MyBatis Mapper XML/进阶专题/03-缓存机制深度/00-索引]]

## 二级缓存是什么

二级缓存作用域 = **Mapper 的 `namespace`**，存于 `Configuration` 的 `caches` 映射里，**跨 `SqlSession` 共享**。进程内（默认 `PerpetualCache`）。

## 怎么开启

两步：

### 1) 全局开关（默认 true）

```yaml
mybatis:
  configuration:
    cache-enabled: true   # 默认就是 true
```

### 2) Mapper 上声明

XML 方式：
```xml
<mapper namespace="com.x.mapper.SysDictMapper">
  <cache/>            <!-- 开二级缓存 -->
  <select id="selectAll" useCache="true" flushCache="false"> ... </select>
</mapper>
```
注解方式：
```java
@CacheNamespace   // 等价于 <cache/>
public interface SysDictMapper { ... }
```

## `<cache>` 的属性

```xml
<cache
  eviction="LRU"
  flushInterval="60000"
  size="1024"
  readOnly="true"/>
```
| 属性 | 含义 | 默认 |
|------|------|------|
| `eviction` | 淘汰策略：LRU / FIFO / SOFT / WEAK | LRU |
| `flushInterval` | 刷新间隔（ms），0 表示不过期 | 0 |
| `size` | 最多缓存对象数 | 1024 |
| `readOnly` | true=返回只读引用（省序列化，但别改它）；false=返回副本（序列化） | false |

## 读写流程（关键：事务提交才进缓存）

二级缓存的写入**不在查询时立即发生**，而是在 **`SqlSession` 提交后**由 `TransactionalCacheManager` 把"待提交缓存"刷入真正的 `Cache`：

```
查询：
  hit? → 返回
  miss? → 查库 → 放入 TransactionalCache（待提交区），不立刻进二级

提交（commit）：
  TransactionalCache 把数据刷进二级 Cache
回滚（rollback）：
  丢弃待提交区数据（防脏数据进缓存）
```

> 这就是为什么**未提交事务里的查询不会污染二级缓存**。

## 二级缓存的"串味"问题

二级缓存按 `namespace` 隔离。但若 `A Mapper` 查了 `sys_user`，`B Mapper` 更新了 `sys_user`，**A 的缓存不会自动失效**（因为归属 A 的 namespace）。→ **多表关联/跨 Mapper 写同一张表时，二级缓存极易脏读**。

解决：用 `<cache-ref namespace="..."/>` 把多个 Mapper 的缓存共享同一块，或用 `flushCache="true"` 让写操作清缓存（但那样缓存价值大降）。

## 序列化要求

`readOnly=false`（默认）时，缓存对象必须**可序列化**（`implements Serializable`），因为二级缓存可能跨调用返回副本（序列化/反序列化）。实体类没实现 `Serializable` → 报 `NotSerializableException`。

## 结论与建议

- 二级缓存适合：**只读/极少写**的单表（如字典、配置、常量）。
- 不适合：**强一致、多表关联、高并发写**的业务表。
- 企业实践：**关掉 Mapper 二级缓存，统一用 Redis 业务层缓存**（见 03），更可控。

下一步：[[MyBatis Mapper XML/进阶专题/03-缓存机制深度/03-Redis二级缓存集成(接本仓库)]]
