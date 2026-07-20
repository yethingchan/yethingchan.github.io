package com.example.admin.config;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地缓存实现（默认 profile 生效）：ConcurrentHashMap + TTL。
 * 无需 Redis 即可启动，满足"开箱即跑"。
 */
@Service
@Primary
@Profile("!mysql")
public class LocalCacheServiceImpl implements CacheService {

    private static final class Value {
        Object v;
        long expireAt; // 0 = 永不过期

        Value(Object v, long expireAt) {
            this.v = v;
            this.expireAt = expireAt;
        }
    }

    private final Map<String, Value> store = new ConcurrentHashMap<>();
    private final Map<String, Long> counters = new ConcurrentHashMap<>();

    private boolean expired(Value val) {
        return val.expireAt > 0 && System.currentTimeMillis() > val.expireAt;
    }

    @Override
    public void set(String key, Object value) {
        store.put(key, new Value(value, 0L));
    }

    @Override
    public Object get(String key) {
        Value val = store.get(key);
        if (val == null) {
            return null;
        }
        if (expired(val)) {
            store.remove(key);
            return null;
        }
        return val.v;
    }

    @Override
    public void delete(String key) {
        store.remove(key);
        counters.remove(key);
    }

    @Override
    public boolean hasKey(String key) {
        return get(key) != null;
    }

    @Override
    public long incr(String key, long delta) {
        long next = counters.getOrDefault(key, 0L) + delta;
        counters.put(key, next);
        return next;
    }

    @Override
    public void expire(String key, long seconds) {
        Value val = store.get(key);
        if (val != null) {
            val.expireAt = System.currentTimeMillis() + seconds * 1000L;
        }
    }

    @Override
    public Set<String> keys(String pattern) {
        return store.keySet();
    }

    @Override
    public void deletePattern(String pattern) {
        Iterator<String> it = store.keySet().iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
    }
}
