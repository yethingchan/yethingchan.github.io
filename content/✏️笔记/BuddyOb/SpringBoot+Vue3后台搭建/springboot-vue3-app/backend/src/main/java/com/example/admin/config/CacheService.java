package com.example.admin.config;

import java.util.Set;

/**
 * 缓存抽象：默认用本地 ConcurrentHashMap（零依赖），
 * mysql profile 下切换为 Redis 实现（RedisCacheServiceImpl）。
 */
public interface CacheService {

    void set(String key, Object value);

    Object get(String key);

    void delete(String key);

    boolean hasKey(String key);

    long incr(String key, long delta);

    void expire(String key, long seconds);

    Set<String> keys(String pattern);

    void deletePattern(String pattern);
}
