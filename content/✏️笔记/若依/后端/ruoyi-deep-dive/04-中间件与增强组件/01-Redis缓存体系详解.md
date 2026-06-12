# 01 - Redis缓存体系详解

## 一、概述

若依框架的Redis缓存体系是整个系统的核心基础设施之一，承担着用户认证Token管理、验证码存储、系统配置缓存、字典数据缓存、防重复提交、接口限流、密码错误计数等多种关键职责。理解Redis缓存的设计与使用方式，是掌握若依框架运行机制的重要一环。

---

## 二、查看与剖析点

### 2.1 核心文件清单

| 文件路径 | 作用 |
|---------|------|
| `ruoyi-framework/.../config/RedisConfig.java` | Redis连接配置、序列化器配置、限流Lua脚本注册 |
| `ruoyi-framework/.../config/FastJson2JsonRedisSerializer.java` | FastJson2序列化器，autoType白名单控制 |
| `ruoyi-common/.../core/redis/RedisCache.java` | Redis工具类，封装所有Redis操作 |
| `ruoyi-common/.../constant/CacheConstants.java` | 缓存Key常量定义 |
| `ruoyi-common/.../constant/Constants.java` | 全局常量，含JSON_WHITELIST_STR |
| `ruoyi-framework/.../aspectj/RateLimiterAspect.java` | 限流切面，使用Lua脚本 |
| `ruoyi-admin/.../resources/application.yml` | Redis连接池配置(Lettuce) |

### 2.2 架构层次

```
application.yml (连接池配置)
       |
RedisConfig (序列化配置 + Lua脚本注册)
       |
RedisCache (统一操作封装)
       |
    各业务模块调用 (Token/验证码/配置/字典/限流/防重提交/密码错误)
```

---

## 三、源码关键片段引用

### 3.1 RedisConfig - 序列化配置

> 源码位置：`ruoyi-framework/src/main/java/com/ruoyi/framework/config/RedisConfig.java`

```java
@Configuration
@EnableCaching
public class RedisConfig extends CachingConfigurerSupport
{
    @Bean
    @SuppressWarnings(value = { "unchecked", "rawtypes" })
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory connectionFactory)
    {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        FastJson2JsonRedisSerializer serializer = new FastJson2JsonRedisSerializer(Object.class);

        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);

        // Hash的key也采用StringRedisSerializer的序列化方式
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
}
```

**剖析要点：**
- Key使用`StringRedisSerializer`，保证Key的可读性（Redis客户端可直接查看）
- Value使用`FastJson2JsonRedisSerializer`，支持对象序列化，保留类型信息（`WriteClassName`）
- Hash结构的Key和Value分别配置序列化器
- 继承`CachingConfigurerSupport`以支持Spring Cache注解（`@Cacheable`等）

### 3.2 FastJson2JsonRedisSerializer - autoType安全控制

> 源码位置：`ruoyi-framework/src/main/java/com/ruoyi/framework/config/FastJson2JsonRedisSerializer.java`

```java
public class FastJson2JsonRedisSerializer<T> implements RedisSerializer<T>
{
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    static final Filter AUTO_TYPE_FILTER = JSONReader.autoTypeFilter(Constants.JSON_WHITELIST_STR);

    @Override
    public byte[] serialize(T t) throws SerializationException
    {
        if (t == null)
        {
            return new byte[0];
        }
        return JSON.toJSONString(t, JSONWriter.Feature.WriteClassName).getBytes(DEFAULT_CHARSET);
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException
    {
        if (bytes == null || bytes.length <= 0)
        {
            return null;
        }
        String str = new String(bytes, DEFAULT_CHARSET);
        return JSON.parseObject(str, clazz, AUTO_TYPE_FILTER);
    }
}
```

**剖析要点：**
- 序列化时使用`WriteClassName`写入`@type`类型信息，反序列化时才能还原为具体对象类型
- `AUTO_TYPE_FILTER`使用`Constants.JSON_WHITELIST_STR`白名单，仅允许`com.ruoyi`包下的类进行autoType解析
- 这是FastJson反序列化漏洞的防护措施，防止恶意类注入

### 3.3 Constants中的白名单配置

> 源码位置：`ruoyi-common/src/main/java/com/ruoyi/common/constant/Constants.java`

```java
/**
 * 自动识别json对象白名单配置（仅允许解析的包名，范围越小越安全）
 */
public static final String[] JSON_WHITELIST_STR = { "com.ruoyi" };
```

**剖析要点：**
- 白名单范围仅限`com.ruoyi`，范围越小越安全
- 如果项目引入了第三方库的对象也需要缓存，需要扩展此白名单

### 3.4 CacheConstants - 缓存Key常量

> 源码位置：`ruoyi-common/src/main/java/com/ruoyi/common/constant/CacheConstants.java`

```java
public class CacheConstants
{
    /** 登录用户 redis key */
    public static final String LOGIN_TOKEN_KEY = "login_tokens:";
    /** 验证码 redis key */
    public static final String CAPTCHA_CODE_KEY = "captcha_codes:";
    /** 参数管理 cache key */
    public static final String SYS_CONFIG_KEY = "sys_config:";
    /** 字典管理 cache key */
    public static final String SYS_DICT_KEY = "sys_dict:";
    /** 防重提交 redis key */
    public static final String REPEAT_SUBMIT_KEY = "repeat_submit:";
    /** 限流 redis key */
    public static final String RATE_LIMIT_KEY = "rate_limit:";
    /** 登录账户密码错误次数 redis key */
    public static final String PWD_ERR_CNT_KEY = "pwd_err_cnt:";
}
```

**剖析要点：**
- 所有Key都以冒号`:`结尾，形成Redis命名空间的层级结构
- 每种业务场景有独立的Key前缀，便于分类管理和批量操作
- `LOGIN_TOKEN_KEY`后接UUID，实现一用户一Token

### 3.5 RedisCache - 核心工具类

> 源码位置：`ruoyi-common/src/main/java/com/ruoyi/common/core/redis/RedisCache.java`

```java
@SuppressWarnings(value = { "unchecked", "rawtypes" })
@Component
public class RedisCache
{
    @Autowired
    public RedisTemplate redisTemplate;

    // ========== 基本对象操作 ==========
    public <T> void setCacheObject(final String key, final T value)
    {
        redisTemplate.opsForValue().set(key, value);
    }

    public <T> void setCacheObject(final String key, final T value, final Integer timeout, final TimeUnit timeUnit)
    {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    public <T> T getCacheObject(final String key)
    {
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        return operation.get(key);
    }

    // ========== List操作 ==========
    public <T> long setCacheList(final String key, final List<T> dataList)
    {
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
        return count == null ? 0 : count;
    }

    public <T> List<T> getCacheList(final String key)
    {
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    // ========== Set操作 ==========
    public <T> BoundSetOperations<String, T> setCacheSet(final String key, final Set<T> dataSet)
    {
        BoundSetOperations<String, T> setOperation = redisTemplate.boundSetOps(key);
        Iterator<T> it = dataSet.iterator();
        while (it.hasNext())
        {
            setOperation.add(it.next());
        }
        return setOperation;
    }

    // ========== Map/Hash操作 ==========
    public <T> void setCacheMap(final String key, final Map<String, T> dataMap)
    {
        if (dataMap != null) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }

    public <T> Map<String, T> getCacheMap(final String key)
    {
        return redisTemplate.opsForHash().entries(key);
    }

    public <T> void setCacheMapValue(final String key, final String hKey, final T value)
    {
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    // ========== 删除操作 ==========
    public boolean deleteObject(final String key)
    {
        return redisTemplate.delete(key);
    }

    // ========== 模糊查询 ==========
    public Collection<String> keys(final String pattern)
    {
        return redisTemplate.keys(pattern);
    }
}
```

**剖析要点：**
- 封装了RedisTemplate的5种数据结构操作：String、List、Set、Hash、模糊查询
- `setCacheObject`提供带超时和不带超时两个重载版本
- `keys`方法使用通配符模式匹配，如`login_tokens:*`可获取所有在线用户Token
- 注意：`keys`命令在生产环境大数据量下有性能问题，若依中仅用于权限刷新等低频操作

### 3.6 限流Lua脚本

> 源码位置：`ruoyi-framework/src/main/java/com/ruoyi/framework/config/RedisConfig.java`

```java
@Bean
public DefaultRedisScript<Long> limitScript()
{
    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
    redisScript.setScriptText(limitScriptText());
    redisScript.setResultType(Long.class);
    return redisScript;
}

private String limitScriptText()
{
    return "local key = KEYS[1]\n" +
            "local count = tonumber(ARGV[1])\n" +
            "local time = tonumber(ARGV[2])\n" +
            "local current = redis.call('get', key);\n" +
            "if current and tonumber(current) > count then\n" +
            "    return tonumber(current);\n" +
            "end\n" +
            "current = redis.call('incr', key)\n" +
            "if tonumber(current) == 1 then\n" +
            "    redis.call('expire', key, time)\n" +
            "end\n" +
            "return tonumber(current);";
}
```

**Lua脚本逻辑分析：**
1. 获取当前key的计数值
2. 如果已存在且超过count限制，直接返回当前值（触发限流）
3. 否则执行`INCR`原子递增
4. 如果是第一次访问（值为1），设置过期时间
5. 返回当前计数值

**剖析要点：**
- 使用Lua脚本保证`INCR`+`EXPIRE`的原子性，避免并发问题
- 实现的是固定窗口限流（非滑动窗口），在窗口边界可能存在瞬间2倍流量
- `ARGV[1]`是允许的次数上限，`ARGV[2]`是时间窗口（秒）

### 3.7 RateLimiterAspect - 限流切面

> 源码位置：`ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/RateLimiterAspect.java`

```java
@Before("@annotation(rateLimiter)")
public void doBefore(JoinPoint point, RateLimiter rateLimiter) throws Throwable
{
    int time = rateLimiter.time();
    int count = rateLimiter.count();

    String combineKey = getCombineKey(rateLimiter, point);
    List<Object> keys = Collections.singletonList(combineKey);
    try
    {
        Long number = redisTemplate.execute(limitScript, keys, count, time);
        if (StringUtils.isNull(number) || number.intValue() > count)
        {
            throw new ServiceException("访问过于频繁，请稍候再试");
        }
        log.info("限制请求'{}',当前请求'{}',缓存key'{}'", count, number.intValue(), combineKey);
    }
    // ...
}

public String getCombineKey(RateLimiter rateLimiter, JoinPoint point)
{
    StringBuffer stringBuffer = new StringBuffer(rateLimiter.key());
    if (rateLimiter.limitType() == LimitType.IP)
    {
        stringBuffer.append(IpUtils.getIpAddr()).append("-");
    }
    MethodSignature signature = (MethodSignature) point.getSignature();
    Method method = signature.getMethod();
    Class<?> targetClass = method.getDeclaringClass();
    stringBuffer.append(targetClass.getName()).append("-").append(method.getName());
    return stringBuffer.toString();
}
```

**剖析要点：**
- 限流Key由`前缀 + IP(可选) + 类名 + 方法名`组成
- 支持按IP限流（`LimitType.IP`）和按方法全局限流（`LimitType.DEFAULT`）
- 通过`@RateLimiter`注解声明式使用，默认60秒100次

### 3.8 Redis连接池配置

> 源码位置：`ruoyi-admin/src/main/resources/application.yml`

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      password:
      timeout: 10s
      lettuce:
        pool:
          min-idle: 0
          max-idle: 8
          max-active: 8
          max-wait: -1ms
```

**剖析要点：**
- 使用Lettuce连接池（Spring Boot 2.x默认），而非Jedis
- `max-active=8`：最大连接数8个
- `max-idle=8`：最大空闲连接8个
- `timeout=10s`：连接超时10秒
- `max-wait=-1ms`：连接等待无限制（不推荐生产环境使用）

---

## 四、Redis使用场景汇总

| 场景 | Key格式 | Value类型 | 过期策略 | 所在模块 |
|------|---------|----------|---------|---------|
| 登录Token | `login_tokens:{uuid}` | LoginUser对象 | 30分钟(自动续期) | TokenService |
| 验证码 | `captcha_codes:{uuid}` | String(验证码答案) | 2分钟 | CaptchaController |
| 系统配置 | `sys_config:{configKey}` | String(配置值) | 永久(手动清理) | SysConfigServiceImpl |
| 字典数据 | `sys_dict:{dictType}` | List(字典数据列表) | 永久(手动清理) | SysDictDataServiceImpl |
| 防重复提交 | `repeat_submit:{key}` | String | 注解配置时间 | SameUrlDataInterceptor |
| 接口限流 | `rate_limit:{key}` | Long(计数器) | 注解配置时间 | RateLimiterAspect |
| 密码错误次数 | `pwd_err_cnt:{username}` | Integer(错误次数) | 10分钟 | SysPasswordService |

---

## 五、细节留神

1. **autoType安全**：FastJson2的autoType白名单仅包含`com.ruoyi`，如果需要缓存第三方对象，务必扩展白名单，否则反序列化会失败。
2. **keys命令性能**：`RedisCache.keys()`方法在生产环境大数据量下可能阻塞Redis，若依中仅用于权限刷新（扫描所有在线Token），频率极低。
3. **Token续期机制**：Token有效期30分钟，但每次请求都会检查剩余时间，不足20分钟自动刷新，实现"活跃用户永不过期"的效果。
4. **限流脚本边界问题**：当前Lua脚本实现的是固定窗口限流，在窗口切换的瞬间可能允许2倍于限制的请求通过。如需更精确的限流，应改用滑动窗口或令牌桶算法。
5. **序列化兼容性**：使用FastJson2的`WriteClassName`写入类型信息，如果修改了类的包名或字段结构，已缓存的旧数据可能反序列化失败。
6. **Redis连接池大小**：默认max-active=8，在高并发场景下可能成为瓶颈，需要根据实际并发量调整。

---

## 六、提问方向

1. **若依为什么选择FastJson2作为Redis序列化方案，而不是Jackson或JDK自带序列化？各自的优缺点是什么？**

2. **autoType白名单设置为`com.ruoyi`，如果项目中引入了MyBatis-Plus的Page对象也需要缓存，应该如何安全地扩展白名单？**

3. **限流Lua脚本实现的是固定窗口限流，如何改造为滑动窗口限流（使用Redis的Sorted Set）？请写出改造后的Lua脚本。**

4. **`RedisCache.keys()`方法在大数据量下有性能风险，若依中权限刷新功能使用了`login_tokens:*`模式扫描所有在线用户，如何优化这个场景？**

5. **如果Redis服务不可用，系统的哪些功能会受到影响？若依框架中是否有Redis降级或容错机制？如果没有，你会如何设计？**

6. **Token自动续期机制中，20分钟的阈值是如何确定的？如果将这个值调大或调小，会对系统安全性和用户体验产生什么影响？**

7. **当前Redis连接池max-active=8，假设系统有1000个并发请求需要访问Redis，会出现什么情况？如何合理配置连接池参数？**
