---
title: Token 鉴权强化与防重复提交
---

# 10-3 Token 鉴权强化 + 防重复提交

> 上接：[[SpringBoot+Vue3后台搭建/10-安全防护/02-XSS与SQL注入过滤]]
> 基础 JWT 在 [[../03-后端基础框架/03-SpringSecurity与JWT鉴权]] 已通。但"能登录"≠"安全"：token 可能被盗、被重放、被用来连点刷数据。本章补三道锁。

## 3.1 Token 防盗用：绑定 IP + UA

思路：登录时把"签发 IP + User-Agent"和 token 一起存 Redis；过滤器每次校验，**环境变了就作废**（提示重新登录）。

```java
// LoginUserService 存登录用户时带上环境指纹
public LoginUser buildLoginUser(SysUser u, List<String> perms, HttpServletRequest req) {
    LoginUser lu = new LoginUser(u, perms);
    lu.setLoginIp(getClientIp(req));
    lu.setUserAgent(req.getHeader("User-Agent"));
    return lu;
}

// JwtAuthenticationTokenFilter 里（3.3 章的 doFilterInternal）追加校验
LoginUser lu = loginUserService.getLoginUser(username);
if (lu != null) {
    String curIp = getClientIp(req);
    String curUa = req.getHeader("User-Agent");
    if (!Objects.equals(lu.getLoginIp(), curIp)
            || !Objects.equals(lu.getUserAgent(), curUa)) {
        // 环境突变：极可能是 token 被盗，直接作废并拒绝
        redisCache.deleteObject("login:" + token);
        SecurityContextHolder.clearContext();
        return; // 不放行 → 后续 401
    }
    // ... 正常塞 Authentication
}
```

> 权衡：绑定太严（如手机切 WiFi IP 变）会误踢正常用户。生产常用"**UA 必校验 + IP 段/城市变更才校验**"，或只做"异地登录提醒"而非直接踢。按业务敏感度选档。

## 3.2 单点登录（踢掉旧会话）

很多后台要求"同一账号只能一处登录"。做法：Redis 用 **Hash/Set 记某用户的所有 token**，新登录先清旧：

```java
// 登录成功后
String newToken = jwtUtils.createToken(...);
// 清掉该用户旧 token（实现单点）
String userKey = "login:tokens:" + loginUser.getUserId();
redisCache.getCacheSet(userKey).forEach(t -> redisCache.deleteObject("login:" + t));
redisCache.deleteObject(userKey);
// 存新 token
redisCache.setCacheObject("login:" + newToken, loginUser, 7200, TimeUnit.SECONDS);
redisCache.setCacheSet(userKey, newToken);
```

> "允许同账号多端登录"则去掉清旧逻辑即可。两种都要在"在线用户"页能手动踢人（[[../04-权限管理模块/06-在线用户与会话监控]]）。

## 3.3 防重放（进阶，可选）

token 有效期长怕被盗用重放，可加 `jti`（唯一 ID）+ 短期 `nonce`：

```java
// 生成时塞 jti
String jti = UUID.randomUUID().toString();
Jwts.builder().id(jti).subject(username)... // 0.12 用 .id()
// 校验时把 jti 进 Redis 黑名单即可主动作废某个 token（退出登录用）
```

退出登录接口：删 `login:token` + 把 `jti` 加 Redis 黑名单，过滤器查黑名单就拒。

## 3.4 防重复提交：完整注册（接 03-4）

[[../03-后端基础框架/04-跨域CORS与XSS防护]] 的 4.3 写了 `@RepeatSubmit` 注解和拦截器，但**没注册到 Spring**。补上 `WebMvcConfigurer`：

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Resource private RepeatSubmitInterceptor repeatSubmitInterceptor;
    @Resource private IpBlacklistInterceptor ipBlacklistInterceptor; // 来自 10-1

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(ipBlacklistInterceptor).addPathPatterns("/**");      // 最前
        registry.addInterceptor(repeatSubmitInterceptor).addPathPatterns("/**")
                .excludePathPatterns("/login", "/captchaImage", "/error");        // 放行登录
    }
}
```

**分布式版**（多实例部署，单机 Redis 锁不够）：用 `SET key val NX EX` 原子加锁：

```java
// 在 RepeatSubmitInterceptor.preHandle 里
String key = "repeat:" + userId + ":" + uri + ":" + repeatToken;
// SET NX EX：不存在才设，存在则拿不到 → 命中重复
Boolean ok = redisTemplate.opsForValue()
    .setIfAbsent(key, "1", Duration.ofSeconds(anno.interval()));
if (Boolean.FALSE.equals(ok)) throw new BusinessException(anno.message());
```

> `setIfAbsent` 是 Redis 的 `SET NX`，**原子操作**，比"先 get 再 set"在并发下安全。微服务多节点时，所有节点共用同一 Redis，锁就全局生效。

## 验证清单

- [ ] 复制 token 到另一浏览器（UA 不同）→ 访问被拒，提示重新登录。
- [ ] 同账号在新设备登录 → 旧设备再访问被踢（单点）。
- [ ] 同一接口连点两次（带相同 `X-Repeat-Token`）→ 第二次 `BusinessException("不允许重复提交")`。
- [ ] 多节点部署下，重复提交仍被全局拦住（Redis 锁生效）。
- [ ] 点退出登录后，旧 token 立即失效（jti 进黑名单）。

> 下一步：[[../10-安全防护/04-文件上传拦截与接口限流]] 把"上传"和"刷接口"两道最后防线补齐。
