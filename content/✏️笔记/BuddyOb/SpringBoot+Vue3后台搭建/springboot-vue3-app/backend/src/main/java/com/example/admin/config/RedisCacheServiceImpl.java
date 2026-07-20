package com.example.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存实现（仅在 mysql profile 生效）。
 * 用 StringRedisTemplate + JSON 序列化，规避 JDK 二进制乱码。
 */
@Service
@Profile("mysql")
public class RedisCacheServiceImpl implements CacheService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public long incr(String key, long delta) {
        Long v = redisTemplate.opsForValue().increment(key, delta);
        return v == null ? 0L : v;
    }

    @Override
    public void expire(String key, long seconds) {
        redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
    }

    @Override
    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    @Override
    public void deletePattern(String pattern) {
        Set<String> ks = redisTemplate.keys(pattern);
        if (ks != null && !ks.isEmpty()) {
            redisTemplate.delete(ks);
        }
    }
}
