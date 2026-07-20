# 03-缓存机制深度 · Redis 二级缓存集成（接本仓库）

> 前置：[[MyBatis Mapper XML/进阶专题/03-缓存机制深度/02-二级缓存原理与Cache接口]]
> 本仓库已有 `com.ruoyi.common.core.redis.RedisCache`（封装 `RedisTemplate` 的 set/get/delete/keys/expire）。

## 思路

MyBatis 二级缓存的本质是 `org.apache.ibatis.cache.Cache` 接口。只要**自己实现这个接口，把数据存进 Redis**，就得到"跨进程、可过期、集中式"的二级缓存。

## `Cache` 接口要实现的 7 个方法

```java
public interface Cache {
    String getId();                                  // namespace
    void putObject(Object key, Object value);         // 写入
    Object getObject(Object key);                     // 读取
    Object removeObject(Object key);                  // 移除（LRU 淘汰时调）
    void clear();                                    // 清空（增删改触发）
    int getSize();                                   // 大小
    default ReadWriteLock getReadWriteLock() {...}    // 读写锁，默认无锁
}
```

## 基于本仓库 `RedisCache` 的实现

```java
package com.ruoyi.common.core.redis;

import org.apache.ibatis.cache.Cache;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RedisMybatisCache implements Cache {

    private final String id;                 // = Mapper namespace
    private static final String PREFIX = "mybatis_cache:";
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // MyBatis 会调用此构造器，传入 namespace 作为 id
    public RedisMybatisCache(String id) {
        this.id = id;
    }

    @Override public String getId() { return id; }

    private RedisCache redis() {
        // 从 Spring 容器取本仓库的 RedisCache 单例
        return SpringUtils.getBean(RedisCache.class);
    }

    @Override
    public void putObject(Object key, Object value) {
        // 序列化用 RedisCache 的 JSON 方式；TTL 30 分钟防雪崩
        redis().setCacheObject(PREFIX + id + ":" + key, value, 30, TimeUnit.MINUTES);
    }

    @Override
    public Object getObject(Object key) {
        return redis().getCacheObject(PREFIX + id + ":" + key);
    }

    @Override
    public Object removeObject(Object key) {
        redis().deleteObject(PREFIX + id + ":" + key);
        return null;
    }

    @Override
    public void clear() {
        // 清整个 namespace：用 keys 扫前缀（生产建议用 SCAN，见下）
        Collection<String> keys = redis().keys(PREFIX + id + ":*");
        if (keys != null && !keys.isEmpty()) redis().deleteObject(keys);
    }

    @Override public int getSize() { return redis().keys(PREFIX + id + ":*").size(); }
    @Override public ReadWriteLock getReadWriteLock() { return lock; }
}
```
> `SpringUtils.getBean` 是本仓库已有的取 Bean 工具；`RedisCache` 的 `setCacheObject/getCacheObject/deleteObject/keys` 方法已存在。

## 在 Mapper 上启用

```xml
<cache type="com.ruoyi.common.core.redis.RedisMybatisCache" eviction="LRU" flushInterval="0" size="1024"/>
```
或注解：
```java
@CacheNamespace(implementation = RedisMybatisCache.class)
public interface SysDictDataMapper { ... }
```

## 生产级注意点

1. **`clear()` 用 `keys` 会阻塞 Redis** → 生产换成 `SCAN` 游标删除，或维护一个"本 namespace 的 key 集合"单独存（每次 put 往集合里加 key，clear 时遍历删）。
2. **序列化**：value 必须是可 JSON 序列化的 POJO；加 `@JsonIgnore` 排除懒加载代理/大字段。
3. **TTL**：`putObject` 设随机 TTL（如 30±10 分钟），避免大量 key 同时过期造成雪崩（见 04）。
4. **事务一致性**：二级缓存写发生在 `commit` 后，若 Redis 写入失败要兜底（catch 后清本地待提交区）。
5. **只读场景**：只读数据设 `readOnly=true` 省去序列化开销，但返回的引用**不能改**（改了相当于改了缓存）。

## 更现实的替代

大多数项目**不把 MyBatis 二级缓存接 Redis**，而是：
- 业务层用 `RedisCache` 手动缓存（如 RuoYi 的字典缓存 `DictUtils`）；
- 或用 MyBatis-Plus 的 `RedisCache` 集成；
- 或用 Spring Cache（`@Cacheable`）+ Redis。

> 本仓库 RuoYi 的字典/配置/菜单用的是**业务层 `RedisCache` 手动缓存**（`Constants.SYS_DICT_KEY` 等），而非 Mapper 二级缓存——更直观可控。

下一步：[[MyBatis Mapper XML/进阶专题/03-缓存机制深度/04-缓存三大问题与事务脏读]]
