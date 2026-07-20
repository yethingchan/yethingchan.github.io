---
title: Spring Security + JWT 鉴权
---

# 03-3 Spring Security 6 + JWT 鉴权

> 上接：[[SpringBoot+Vue3后台搭建/03-后端基础框架/02-MyBatisPlus与Redis配置]]

## 3.0 认证授权整体流程

```
前端 /login {username,password}
  → LoginController 用 AuthenticationManager 认证（比对 BCrypt 密码）
  → 认证成功 → JwtUtils 发 token（含用户名+角色）
  → token 存 Redis（key=login:token）
  → 返回前端，前端存 cookie/localStorage

此后每次请求 Header: Authorization: Bearer xxxx
  → JwtAuthenticationTokenFilter 解析 token
  → 从 Redis 取 LoginUser 塞进 SecurityContext
  → @PreAuthorize("hasPermi('system:user:list')") 据此放行
```

## 3.1 SecurityConfig（Spring Boot 3 / Security 6 新写法）

```java
package com.example.admin.config;

import com.example.admin.security.JwtAuthenticationTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.Customizer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** 核心：安全过滤链（Security 6 用 lambda DSL，不再继承 WebSecurityConfigurerAdapter） */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                         JwtAuthenticationTokenFilter jwtFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())                  // 前后端分离用 token，关闭 CSRF
            .cors(Customizer.withDefaults())                 // 启用 CORS（用 [[../01-工程初始化/03-前后端联调与跨域]] 的 CorsFilter）
            .sessionManagement(sm -> sm.sessionCreationPolicy(
                org.springframework.security.config.annotation.web.builders.SessionCreationPolicy.STATELESS)) // 无状态：不建 HttpSession
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/captchaImage", "/swagger-ui/**",
                                 "/v3/api-docs/**", "/doc.html", "/error").permitAll()
                .anyRequest().authenticated())               // 其余全部要登录
            .addFilterBefore(jwtFilter,
                UsernamePasswordAuthenticationFilter.class)    // 把 JWT 过滤器插在用户名密码过滤器之前
            .formLogin(form -> form.disable())                // 自己写登录接口，禁用默认表单
            .httpBasic(basic -> basic.disable());
        return http.build();
    }

    /** 暴露 AuthenticationManager（LoginController 用它做认证） */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    /** BCrypt 密码编码器（登录比对 + 注册加密都用它） */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```
**逐行解释**
- `csrf.disable()`：token 方案下 CSRF token 没意义，关掉避免干扰。
- `sessionManagement → STATELESS`：**关键**。前后端分离不用 Session，每次靠 token 自证，服务端不存会话。
- `addFilterBefore(jwtFilter, ...)`：让 JWT 过滤器先跑，把 token 解析成 `Authentication` 塞进 Context，后面的 `@PreAuthorize` 才能拿到权限。
- `permitAll()` 白名单：登录、验证码、Swagger 文档、error 必须放行，否则连登录都进不去。

## 3.2 JwtUtils（jjwt 0.12 新 API）

```java
package com.example.admin.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtils {
    @Value("${token.secret:abcdefghijklmnopqrstuvwxyz0123456789}")
    private String secret;          // 生产用 32+ 字节随机串，放配置中心
    @Value("${token.expire:7200}")
    private long expireSeconds;     // 2 小时

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 生成 token：载荷里放用户名 + 角色列表 */
    public String createToken(String username, List<String> roles) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .subject(username)
            .claim("roles", roles)
            .issuedAt(new Date(now))
            .expiration(new Date(now + expireSeconds * 1000))
            .signWith(key(), Jwts.SIG.HS256)    // 0.12 用 Jwts.SIG 而非 SignatureAlgorithm
            .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser().verifyWith(key()).build()
                   .parseSignedClaims(token).getPayload();
    }
    public String getUsername(String token) { return parseToken(token).getSubject(); }
    public boolean validate(String token) {
        try { parseToken(token); return true; }
        catch (JwtException | IllegalArgumentException e) { return false; }
    }
}
```
**讲解**：jjwt **0.12 改了 API**——`signWith` 用 `Jwts.SIG.HS256`，`parser()` 用 `verifyWith(key)`。老教程的 `SignatureAlgorithm.HS256`/`setSigningKey(String)` 已废弃会报错，照新版写。

## 3.3 JwtAuthenticationTokenFilter（每次请求校验）

```java
package com.example.admin.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.example.admin.common.utils.RedisCache;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final RedisCache redisCache;
    // 真实项目这里调 UserDetailsServiceImpl 或直接从 Redis 取 LoginUser
    private final com.example.admin.security.service.LoginUserService loginUserService;

    public JwtAuthenticationTokenFilter(JwtUtils jwtUtils, RedisCache redisCache,
                                         com.example.admin.security.service.LoginUserService s) {
        this.jwtUtils = jwtUtils; this.redisCache = redisCache; this.loginUserService = s;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp,
                                    FilterChain chain) throws ServletException, IOException {
        String token = parseBearer(req);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String username = jwtUtils.getUsername(token);            // 1) 取用户名
                var loginUser = loginUserService.getLoginUser(username); // 2) 取用户（Redis 或查库）
                if (jwtUtils.validate(token)) {                        // 3) 校验未过期/签名对
                    var auth = new UsernamePasswordAuthenticationToken(
                        loginUser, null, loginUser.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(auth); // 4) 塞进 Context
                }
            } catch (Exception ignored) { /* 无效 token：不放行，留到后面 401 */ }
        }
        chain.doFilter(req, resp);   // 无论是否验证，都放行给后续过滤器（最终由 authorizeHttpRequests 判 401）
    }

    private String parseBearer(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        return (h != null && h.startsWith("Bearer ")) ? h.substring(7) : null;
    }
}
```
**讲解**：这就是"无状态登录"的本质——**每次请求自带身份，过滤器把 token 翻译成 Security 的 Authentication**。之后 `@PreAuthorize`、Controller 里 `SecurityUtils.getLoginUser()` 才能用。

## 3.4 登录接口（发 token）

```java
@RestController
public class LoginController {
    @Resource private AuthenticationManager authenticationManager;
    @Resource private JwtUtils jwtUtils;
    @Resource private RedisCache redisCache;
    @Resource private LoginUserService loginUserService;

    @PostMapping("/login")
    public AjaxResult login(@Valid @RequestBody LoginBody body) {
        // 1) Security 认证（内部调 UserDetailsServiceImpl 比对 BCrypt 密码）
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(body.getUsername(), body.getPassword()));
        LoginUser loginUser = (LoginUser) auth.getPrincipal();

        // 2) 发 token（载荷含角色）
        String token = jwtUtils.createToken(loginUser.getUsername(),
            loginUser.getAuthorities().stream().map(a -> a.getAuthority()).toList());

        // 3) 用户对象存 Redis（key 含 token），供过滤器/在线用户用
        redisCache.setCacheObject("login:" + token, loginUser, 7200, TimeUnit.SECONDS);

        // 4) 返回（AjaxResult 是 HashMap，单独 put 再 return）
        AjaxResult res = AjaxResult.success("登录成功");
        res.put("token", token);
        return res;
    }
}
```
**注意**：`AjaxResult.success(...).put(...)` 会返回 put 的**旧值**（null）而非对象——必须像上面先接变量再 `put` 再 `return`。这是新手高频坑。

## 3.5 UserDetailsServiceImpl（认证时查用户+权限）

```java
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Resource private SysUserMapper userMapper;
    @Resource private SysRoleMapper roleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) {
        SysUser u = userMapper.selectOne(
            Wrappers.<SysUser>query().eq("user_name", username));
        if (u == null) throw new UsernameNotFoundException("用户不存在");
        // 查该用户所有权限串（多角色合并去重）
        List<String> perms = roleMapper.selectPermsByUserId(u.getUserId());
        return new LoginUser(u, perms.stream().map(SimpleGrantedAuthority::new).toList());
    }
}
```
> `selectPermsByUserId` 是多表 JOIN（`sys_user_role`↔`sys_role_menu`↔`sys_menu.perms`），见 [[../04-权限管理模块/02-角色与菜单权限分配]]。

## 3.6 application.yml 补充

```yaml
token:
  secret: "2f0b7c8e1d4a6f9b3c5e7a9d1b3f5c7e9a1b3c5d7e9f1a3b5c7d9e1f3a5b7" # 32+ 字节随机
  expire: 7200
```

## 验证清单
- [ ] 调 `/login` 拿到 `{code:200, token:"eyJ..."}`。
- [ ] 不带 token 访问 `/system/user/list` → 401。
- [ ] 带 `Authorization: Bearer xxx` → 正常返回（过滤器生效）。
- [ ] token 过期后访问 → 401（JwtUtils.validate 拦截）。

> 下一步：[[../03-后端基础框架/04-跨域CORS与XSS防护]] 补 XSS/防重提交。
