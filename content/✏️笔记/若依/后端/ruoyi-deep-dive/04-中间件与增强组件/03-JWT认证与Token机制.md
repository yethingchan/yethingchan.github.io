# 03 - JWT认证与Token机制

## 一、概述

若依框架采用JWT（JSON Web Token）作为认证凭证，结合Redis缓存实现了一套完整的Token管理机制。JWT负责生成和验证令牌的合法性，Redis负责存储用户登录状态和实现Token的自动续期、主动失效等功能。这种"JWT + Redis"的双层设计既保证了无状态认证的便利性，又具备了有状态管理的控制力。

---

## 二、查看与剖析点

### 2.1 核心文件清单

| 文件路径 | 作用 |
|---------|------|
| `ruoyi-framework/.../web/service/TokenService.java` | Token核心服务：创建/验证/刷新/删除/权限刷新 |
| `ruoyi-common/.../core/domain/model/LoginUser.java` | 登录用户模型，存储在Redis中的用户信息 |
| `ruoyi-common/.../constant/Constants.java` | Token相关常量定义 |
| `ruoyi-common/.../constant/CacheConstants.java` | Token缓存Key前缀 |
| `ruoyi-admin/.../resources/application.yml` | Token配置（header/secret/expireTime） |
| `ruoyi-framework/.../security/filter/JwtAuthenticationTokenFilter.java` | Token验证过滤器 |

### 2.2 Token生命周期

```
登录成功
  |
TokenService.createToken()
  |-- 生成UUID作为Token标识
  |-- 设置用户代理信息(IP/浏览器/OS/地址)
  |-- 构建JWT Claims(uuid + username)
  |-- 使用HS512签名生成JWT字符串
  |-- 将LoginUser缓存到Redis(key=login_tokens:uuid, 30分钟)
  |
返回JWT给前端
  |
前端每次请求携带Authorization: Bearer {jwt}
  |
JwtAuthenticationTokenFilter.doFilterInternal()
  |-- 从Header中提取JWT
  |-- 解析JWT获取uuid
  |-- 从Redis获取LoginUser
  |-- verifyToken()检查有效期，不足20分钟自动续期
  |-- 设置SecurityContext
  |
退出登录
  |-- 从Redis删除LoginUser缓存
  |-- Token立即失效
```

---

## 三、源码关键片段引用

### 3.1 Token配置

> 源码位置：`ruoyi-admin/src/main/resources/application.yml`

```yaml
# token配置
token:
  # 令牌自定义标识
  header: Authorization
  # 令牌密钥
  secret: abcdefghijklmnopqrstuvwxyz
  # 令牌有效期（默认30分钟）
  expireTime: 30
```

### 3.2 Token相关常量

> 源码位置：`ruoyi-common/src/main/java/com/ruoyi/common/constant/Constants.java`

```java
/** 令牌 */
public static final String TOKEN = "token";
/** 令牌前缀 */
public static final String TOKEN_PREFIX = "Bearer ";
/** 令牌前缀 */
public static final String LOGIN_USER_KEY = "login_user_key";
/** 用户ID */
public static final String JWT_USERID = "userid";
/** 用户名称 */
public static final String JWT_USERNAME = Claims.SUBJECT;
/** 用户头像 */
public static final String JWT_AVATAR = "avatar";
/** 创建时间 */
public static final String JWT_CREATED = "created";
/** 用户权限 */
public static final String JWT_AUTHORITIES = "authorities";
```

**剖析要点：**
- `TOKEN_PREFIX = "Bearer "`：JWT标准前缀，前端请求Header格式为`Authorization: Bearer {token}`
- `LOGIN_USER_KEY`：JWT Claims中存储的UUID键名，用于关联Redis中的LoginUser
- JWT中仅存储uuid和username，不存储权限等敏感信息

### 3.3 TokenService - Token创建

> 源码位置：`ruoyi-framework/src/main/java/com/ruoyi/framework/web/service/TokenService.java`

```java
@Component
public class TokenService
{
    @Value("${token.header}")
    private String header;

    @Value("${token.secret}")
    private String secret;

    @Value("${token.expireTime}")
    private int expireTime;

    protected static final long MILLIS_SECOND = 1000;
    protected static final long MILLIS_MINUTE = 60 * MILLIS_SECOND;
    private static final Long MILLIS_MINUTE_TWENTY = 20 * 60 * 1000L;

    @Autowired
    private RedisCache redisCache;

    /**
     * 创建令牌
     */
    public String createToken(LoginUser loginUser)
    {
        String token = IdUtils.fastUUID();
        loginUser.setToken(token);
        setUserAgent(loginUser);
        refreshToken(loginUser);

        Map<String, Object> claims = new HashMap<>();
        claims.put(Constants.LOGIN_USER_KEY, token);
        claims.put(Constants.JWT_USERNAME, loginUser.getUsername());
        return createToken(claims);
    }

    /**
     * 从数据声明生成令牌
     */
    private String createToken(Map<String, Object> claims)
    {
        String token = Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, secret).compact();
        return token;
    }
}
```

**剖析要点：**
- Token创建流程：生成UUID -> 设置用户代理 -> 刷新Redis缓存 -> 构建JWT Claims -> HS512签名
- JWT中仅包含uuid和username，不包含权限、角色等，减少JWT体积
- 使用`IdUtils.fastUUID()`生成32位UUID作为Token标识
- `signWith(SignatureAlgorithm.HS512, secret)`使用HS512算法签名

### 3.4 TokenService - Token验证与续期

```java
/**
 * 验证令牌有效期，相差不足20分钟，自动刷新缓存
 */
public void verifyToken(LoginUser loginUser)
{
    long expireTime = loginUser.getExpireTime();
    long currentTime = System.currentTimeMillis();
    if (expireTime - currentTime <= MILLIS_MINUTE_TWENTY)
    {
        refreshToken(loginUser);
    }
}

/**
 * 刷新令牌有效期
 */
public void refreshToken(LoginUser loginUser)
{
    loginUser.setLoginTime(System.currentTimeMillis());
    loginUser.setExpireTime(loginUser.getLoginTime() + expireTime * MILLIS_MINUTE);
    // 根据uuid将loginUser缓存
    String userKey = getTokenKey(loginUser.getToken());
    redisCache.setCacheObject(userKey, loginUser, expireTime, TimeUnit.MINUTES);
}

private String getTokenKey(String uuid)
{
    return CacheConstants.LOGIN_TOKEN_KEY + uuid;
}
```

**剖析要点：**
- 续期阈值：剩余有效期不足20分钟时自动续期
- 续期操作：重置登录时间和过期时间，重新写入Redis（30分钟TTL）
- 效果：活跃用户Token永不过期，非活跃用户30分钟后自动失效
- `expireTime * MILLIS_MINUTE`：将配置的分钟数转换为毫秒

### 3.5 TokenService - 获取登录用户

```java
/**
 * 获取用户身份信息
 */
public LoginUser getLoginUser(HttpServletRequest request)
{
    // 获取请求携带的令牌
    String token = getToken(request);
    if (StringUtils.isNotEmpty(token))
    {
        try
        {
            Claims claims = parseToken(token);
            // 解析对应的权限以及用户信息
            String uuid = (String) claims.get(Constants.LOGIN_USER_KEY);
            String userKey = getTokenKey(uuid);
            LoginUser user = redisCache.getCacheObject(userKey);
            return user;
        }
        catch (Exception e)
        {
            log.error("获取用户信息异常'{}'", e.getMessage());
        }
    }
    return null;
}

/**
 * 获取请求token
 */
private String getToken(HttpServletRequest request)
{
    String token = request.getHeader(header);
    if (StringUtils.isNotEmpty(token) && token.startsWith(Constants.TOKEN_PREFIX))
    {
        token = token.replace(Constants.TOKEN_PREFIX, "");
    }
    return token;
}

/**
 * 从令牌中获取数据声明
 */
private Claims parseToken(String token)
{
    return Jwts.parser()
            .setSigningKey(secret)
            .parseClaimsJws(token)
            .getBody();
}
```

**剖析要点：**
- 从请求Header中提取Token，去除`Bearer `前缀
- 解析JWT获取uuid，再从Redis获取完整的LoginUser对象
- JWT解析失败（Token篡改/过期/格式错误）时返回null，不抛出异常
- 每次请求都需要访问Redis获取LoginUser，这是"JWT + Redis"方案的代价

### 3.6 TokenService - 删除与权限刷新

```java
/**
 * 删除用户身份信息
 */
public void delLoginUser(String token)
{
    if (StringUtils.isNotEmpty(token))
    {
        String userKey = getTokenKey(token);
        redisCache.deleteObject(userKey);
    }
}

/**
 * 角色权限变更后，刷新所有持有该角色的在线用户权限
 */
public void refreshPermissionByRoleId(Long roleId, SysPermissionService permissionService)
{
    // 扫描所有在线 token
    String pattern = CacheConstants.LOGIN_TOKEN_KEY + "*";
    Collection<String> keys = redisCache.keys(pattern);
    if (keys == null || keys.isEmpty())
    {
        return;
    }
    for (String key : keys)
    {
        LoginUser loginUser = redisCache.getCacheObject(key);
        if (loginUser == null || loginUser.getUser() == null || loginUser.getUser().isAdmin())
        {
            continue; // 管理员拥有所有权限，跳过
        }
        // 判断该用户是否拥有此角色
        boolean hasRole = loginUser.getUser().getRoles() != null
                && loginUser.getUser().getRoles().stream().anyMatch(r -> roleId.equals(r.getRoleId()));
        if (!hasRole)
        {
            continue;
        }
        // 刷新权限缓存
        loginUser.setPermissions(permissionService.getMenuPermission(loginUser.getUser()));
        refreshToken(loginUser);
        log.info("角色[{}]权限变更，已刷新在线用户[{}]的权限缓存", roleId, loginUser.getUsername());
    }
}
```

**剖析要点：**
- `delLoginUser`：删除Redis中的LoginUser，Token立即失效（用于退出登录和踢下线）
- `refreshPermissionByRoleId`：角色权限变更后，遍历所有在线用户，刷新拥有该角色的用户权限
- 管理员（admin）跳过权限刷新，因为管理员拥有所有权限
- 使用`keys(login_tokens:*)`扫描所有在线Token，低频操作可接受

### 3.7 LoginUser - 登录用户模型

> 源码位置：`ruoyi-common/src/main/java/com/ruoyi/common/core/domain/model/LoginUser.java`

LoginUser实现了`UserDetails`接口，包含以下核心字段：
- `userId`：用户ID
- `deptId`：部门ID
- `token`：UUID标识
- `loginTime`：登录时间
- `expireTime`：过期时间
- `ipaddr`：登录IP
- `loginLocation`：登录地点
- `browser`：浏览器
- `os`：操作系统
- `user`：SysUser完整对象
- `permissions`：菜单权限集合（Set<String>）

---

## 四、JWT + Redis 双层设计分析

### 为什么不只用JWT？

| 方案 | 优点 | 缺点 |
|------|------|------|
| 纯JWT | 无状态、不依赖存储 | 无法主动失效、无法续期、Token体积大 |
| 纯Session | 服务端可控 | 有状态、不支持水平扩展 |
| JWT + Redis（若依方案） | 可主动失效、可续期、JWT体积小 | 每次请求需查Redis |

### 若依的设计取舍

- JWT仅存储uuid和username，体积小，解析快
- 完整的LoginUser存储在Redis中，支持主动失效和续期
- 每次请求需查Redis获取LoginUser，但Redis查询性能极高（亚毫秒级）

---

## 五、细节留神

1. **JWT无过期时间**：若依的JWT本身没有设置`exp`过期时间，过期控制完全依赖Redis的TTL。这意味着JWT字符串本身永远有效，但Redis中的LoginUser会过期。
2. **密钥安全性**：默认密钥为`abcdefghijklmnopqrstuvwxyz`，生产环境必须更换为足够复杂的密钥。
3. **Token续期阈值**：20分钟的续期阈值是硬编码的常量（`MILLIS_MINUTE_TWENTY`），不可配置。
4. **权限刷新性能**：`refreshPermissionByRoleId`使用`keys`命令扫描所有在线Token，在线用户量大时可能影响Redis性能。
5. **JWT解析异常处理**：`getLoginUser`中JWT解析失败只记录日志返回null，不会抛出异常，这意味着无效Token的请求会以"未认证"身份继续执行过滤器链。

---

## 六、提问方向

1. **若依的JWT没有设置`exp`过期时间，过期控制完全依赖Redis。如果有人获取了JWT字符串，在Redis缓存过期前是否可以一直使用？这种设计有什么安全隐患？**

2. **Token续期阈值20分钟是硬编码的，如果需要让用户自定义这个值，应该如何改造？**

3. **`refreshPermissionByRoleId`方法使用`keys`命令扫描所有在线用户，在万人在线的场景下如何优化？**

4. **如果Redis宕机，所有在线用户的Token会怎样？系统是完全不可用还是部分功能可用？如何设计Redis降级方案？**

5. **若依的JWT使用HS512对称加密，密钥同时用于签名和验证。如果改用RS256非对称加密（公钥验证、私钥签名），应该如何改造？有什么优势？**

6. **当前Token续期是在每次请求时被动触发的，如果用户持续活跃但操作频率很低（如30分钟内只操作一次），Token会不会过期？为什么？**
