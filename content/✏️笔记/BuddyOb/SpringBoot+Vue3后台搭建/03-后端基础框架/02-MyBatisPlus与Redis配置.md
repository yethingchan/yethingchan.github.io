---
title: MyBatis-Plus 与 Redis 配置
---

# 03-2 MyBatis-Plus 与 Redis 配置

> 上接：[[SpringBoot+Vue3后台搭建/03-后端基础框架/01-统一返回与全局异常]]

## 2.1 MyBatis-Plus 配置类（分页 + 自动填充 + 逻辑删）

```java
package com.example.admin.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.LocalDateTime;

@Configuration
public class MybatisPlusConfig {

    /** 插件链：分页是核心，可叠加乐观锁/数据权限等（见 [[../04-权限管理模块/05-数据权限实现]]） */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页拦截器：让 selectPage 自动拼 LIMIT，并把 total 算好
        interceptor.addInnerInterceptor(
            new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /** 自动填充 create_time / update_time，不用每次手动 set */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override public void insertFill(MetaObject meta) {
                strictInsertFill(meta, "createTime", LocalDateTime.class, LocalDateTime.now());
                strictInsertFill(meta, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
            @Override public void updateFill(MetaObject meta) {
                strictUpdateFill(meta, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
```
**讲解**
- `PaginationInnerInterceptor` 是 MP 分页的**灵魂**：你写 `userMapper.selectPage(page, queryWrapper)`，它自动在 SQL 后加 `LIMIT`，并额外查一次 `count` 填 `page.total`。**前端分页全靠它**。
- `MetaObjectHandler`：实体字段加 `@TableField(fill=FieldFill.INSERT)` 才生效（见下方 DTO 示例）。
- 逻辑删除：在 `application.yml` 里加 `mybatis-plus.global-config.db-config.logic-delete-field: delFlag` + `logic-not-delete-value: 0` + `logic-delete-value: 2`，之后 `removeById` 自动变 `UPDATE ... SET del_flag=2`。**对应 [[../02-数据库与RBAC/01-RBAC核心表SQL]] 的 del_flag 设计**。

## 2.2 实体示例（自动填充 + 逻辑删）

```java
package com.example.admin.modules.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)   // 自增主键（对应 DB 的 AUTO_INCREMENT）
    private Long userId;
    private String userName;
    private String nickName;
    private String password;

    @TableField(fill = FieldFill.INSERT)   // 插入自动填
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic                    // 逻辑删除字段
    private String delFlag;
}
```

## 2.3 Mapper 接口（几乎不用写 SQL）

```java
package com.example.admin.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.admin.modules.system.domain.SysUser;

public interface SysUserMapper extends BaseMapper<SysUser> {
    // BaseMapper 已提供：selectById / insert / updateById / deleteById
    // / selectPage / selectList / selectCount ... 增删改查全自动
}
```
**讲解**：继承 `BaseMapper<SysUser>`，**0 行 SQL 搞定 CRUD**。复杂查询用 `QueryWrapper`（见 [[../04-权限管理模块/01-用户管理]] 的分页筛选）。

## 2.4 Redis 配置（JSON 序列化，杜绝乱码）

```java
package com.example.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> tpl = new RedisTemplate<>();
        tpl.setConnectionFactory(factory);
        // Key 用 String 序列化；Value 用 JSON（可读性+跨语言）
        tpl.setKeySerializer(new StringRedisSerializer());
        tpl.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        tpl.setHashKeySerializer(new StringRedisSerializer());
        tpl.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return tpl;
    }
}
```
**讲解**：默认 `JdkSerializationRedisSerializer` 会把对象存成二进制乱码，Redis 客户端里根本看不懂。**改成 JSON** 后，Redis 里是 `{...}` 明文，运维排查、跨语言读取都方便。

## 2.5 缓存工具类（业务层直接调）

```java
package com.example.admin.common.utils;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import jakarta.annotation.Resource;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class RedisCache {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public <T> void setCacheObject(String key, T value) {
        redisTemplate.opsForValue().set(key, value);
    }
    public <T> void setCacheObject(String key, T value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }
    @SuppressWarnings("unchecked")
    public <T> T getCacheObject(String key) {
        return key == null ? null : (T) redisTemplate.opsForValue().get(key);
    }
    public void expire(String key, long timeout, TimeUnit unit) {
        redisTemplate.expire(key, timeout, unit);
    }
    public void deleteObject(String key) { redisTemplate.delete(key); }
    public Set<String> keys(String pattern) { return redisTemplate.keys(pattern); }
    // ... Hash / List / 递增 等封装略
}
```
**讲解**：封装一层避免业务里到处 `opsForValue()`。菜单/字典/参数缓存全靠它（见 [[../05-字典与基础数据]]、[[../07-日志与监控/02-监控与缓存备份]]）。

## 2.6 application.yml 补充（MP 逻辑删）

```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: delFlag     # 实体里的逻辑删字段名
      logic-not-delete-value: '0'        # 未删
      logic-delete-value: '2'           # 已删
```
> 注意：`del_flag` 列在 DB 是 CHAR(1)，这里值写字符串 `'0'/'2'` 与表设计一致。

## 验证清单
- [ ] `userMapper.selectPage(new Page<>(1,10), null)` 返回 total 正确。
- [ ] 插入实体后 `create_time` 自动有值（不用手动 set）。
- [ ] `removeById(1)` 执行的是 `UPDATE SET del_flag=2` 而非 DELETE。
- [ ] `redisTemplate.opsForValue().set("k", user)` 后，redis-cli 里 `GET k` 是 JSON。

> 下一步：[[../03-后端基础框架/03-SpringSecurity与JWT鉴权]] 把登录与鉴权接上。
