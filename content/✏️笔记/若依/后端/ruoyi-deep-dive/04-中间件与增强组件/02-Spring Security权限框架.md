# 02 - Spring Security权限框架

## 一、概述

若依框架基于Spring Security构建了完整的权限认证体系，采用无状态（STATELESS）JWT Token认证方式，实现了CSRF禁用、Session禁用、JWT过滤器链、CORS跨域支持、BCrypt密码加密、方法级权限控制等核心安全功能。整个权限框架的设计思路是"配置集中化 + 过滤器链式处理 + 注解式权限控制"。

---

## 二、查看与剖析点

### 2.1 核心文件清单

| 文件路径 | 作用 |
|---------|------|
| `ruoyi-framework/.../config/SecurityConfig.java` | Security核心配置，过滤器链、Session策略、权限注解开关 |
| `ruoyi-framework/.../security/filter/JwtAuthenticationTokenFilter.java` | JWT Token验证过滤器 |
| `ruoyi-framework/.../security/handle/AuthenticationEntryPointImpl.java` | 认证失败处理器(401) |
| `ruoyi-framework/.../security/handle/LogoutSuccessHandlerImpl.java` | 退出成功处理器 |
| `ruoyi-framework/.../web/service/UserDetailsServiceImpl.java` | 用户认证服务 |
| `ruoyi-framework/.../web/service/SysLoginService.java` | 登录业务逻辑 |
| `ruoyi-framework/.../web/service/SysPasswordService.java` | 密码校验与错误锁定 |
| `ruoyi-framework/.../web/service/PermissionService.java` | 自定义权限Bean(ss) |
| `ruoyi-framework/.../web/service/SysPermissionService.java` | 角色/菜单权限获取 |
| `ruoyi-framework/.../config/properties/PermitAllUrlProperties.java` | @Anonymous注解URL自动收集 |
| `ruoyi-framework/.../security/context/AuthenticationContextHolder.java` | 认证上下文 holder |

### 2.2 过滤器链执行顺序

```
HTTP Request
    |
CorsFilter (跨域处理)
    |
LogoutFilter (处理/logout请求)
    |
JwtAuthenticationTokenFilter (JWT Token验证)
    |
UsernamePasswordAuthenticationFilter (Spring Security默认)
    |
AuthorizationManager (权限校验)
    |
AuthenticationEntryPointImpl (认证失败返回401)
    |
HTTP Response
```

---

## 三、源码关键片段引用

### 3.1 SecurityConfig - 核心配置

> 源码位置：`ruoyi-framework/src/main/java/com/ruoyi/framework/config/SecurityConfig.java`

```java
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@Configuration
public class SecurityConfig
{
    @Autowired
    private AuthenticationEntryPointImpl unauthorizedHandler;
    @Autowired
    private LogoutSuccessHandlerImpl logoutSuccessHandler;
    @Autowired
    private JwtAuthenticationTokenFilter authenticationTokenFilter;
    @Autowired
    private CorsFilter corsFilter;
    @Autowired
    private PermitAllUrlProperties permitAllUrl;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception
    {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception
    {
        return httpSecurity
            // CSRF禁用，因为不使用session
            .csrf(csrf -> csrf.disable())
            // 禁用HTTP响应标头
            .headers((headersCustomizer) -> {
                headersCustomizer.cacheControl(cache -> cache.disable()).frameOptions(options -> options.sameOrigin());
            })
            // 认证失败处理类
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            // 基于token，所以不需要session
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 注解标记允许匿名访问的url
            .authorizeHttpRequests((requests) -> {
                permitAllUrl.getUrls().forEach(url -> requests.requestMatchers(url).permitAll());
                requests.requestMatchers("/login", "/register", "/captchaImage").permitAll()
                    .requestMatchers(HttpMethod.GET, "/", "/*.html", "/**.html", "/**.css", "/**.js", "/profile/**").permitAll()
                    .requestMatchers("/swagger-ui.html", "/v3/api-docs/**", "/swagger-ui/**", "/druid/**").permitAll()
                    .anyRequest().authenticated();
            })
            .logout(logout -> logout.logoutUrl("/logout").logoutSuccessHandler(logoutSuccessHandler))
            // 添加JWT filter
            .addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
            // 添加CORS filter
            .addFilterBefore(corsFilter, JwtAuthenticationTokenFilter.class)
            .addFilterBefore(corsFilter, LogoutFilter.class)
            .build();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder()
    {
        return new BCryptPasswordEncoder();
    }
}
```

**剖析要点：**
- `@EnableMethodSecurity(prePostEnabled=true, securedEnabled=true)`：开启方法级权限控制，支持`@PreAuthorize`、`@PostAuthorize`、`@Secured`注解
- `csrf.disable()`：禁用CSRF，因为使用JWT Token而非Cookie/Session
- `STATELESS`：不创建Session，每次请求都独立认证
- `permitAllUrl.getUrls()`：动态收集所有`@Anonymous`注解标记的URL
- 过滤器顺序：CorsFilter -> JwtAuthenticationTokenFilter -> UsernamePasswordAuthenticationFilter
- `BCryptPasswordEncoder`：BCrypt强散列哈希加密

### 3.2 JwtAuthenticationTokenFilter - Token验证过滤器

> 源码位置：`ruoyi-framework/src/main/java/com/ruoyi/framework/security/filter/JwtAuthenticationTokenFilter.java`

```java
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter
{
    @Autowired
    private TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNull(SecurityUtils.getAuthentication()))
        {
            tokenService.verifyToken(loginUser);
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }
        chain.doFilter(request, response);
    }
}
```

**剖析要点：**
- 继承`OncePerRequestFilter`，保证每个请求只执行一次过滤逻辑
- 先从请求中获取LoginUser，如果存在且SecurityContext中尚未设置认证信息
- 调用`verifyToken`检查Token有效期，不足20分钟自动续期
- 构建`UsernamePasswordAuthenticationToken`并设置到`SecurityContextHolder`
- 注意：`loginUser.getAuthorities()`提供了用户的权限集合，供后续权限校验使用

### 3.3 AuthenticationEntryPointImpl - 认证失败处理

> 源码位置：`ruoyi-framework/src/main/java/com/ruoyi/framework/security/handle/AuthenticationEntryPointImpl.java`

```java
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint, Serializable
{
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException
    {
        int code = HttpStatus.UNAUTHORIZED;
        String msg = StringUtils.format("请求访问：{}，认证失败，无法访问系统资源", request.getRequestURI());
        ServletUtils.renderString(response, JSON.toJSONString(AjaxResult.error(code, msg)));
    }
}
```

**剖析要点：**
- 当用户未认证（未携带Token或Token无效）访问受保护资源时触发
- 返回401状态码的JSON响应，而非Spring Security默认的302重定向到登录页
- 前端根据401状态码跳转到登录页面

### 3.4 LogoutSuccessHandlerImpl - 退出处理

> 源码位置：`ruoyi-framework/src/main/java/com/ruoyi/framework/security/handle/LogoutSuccessHandlerImpl.java`

```java
@Configuration
public class LogoutSuccessHandlerImpl implements LogoutSuccessHandler
{
    @Autowired
    private TokenService tokenService;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException
    {
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser))
        {
            String userName = loginUser.getUsername();
            // 删除用户缓存记录
            tokenService.delLoginUser(loginUser.getToken());
            // 记录用户退出日志
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(userName, Constants.LOGOUT, MessageUtils.message("user.logout.success")));
        }
        ServletUtils.renderString(response, JSON.toJSONString(AjaxResult.success(MessageUtils.message("user.logout.success"))));
    }
}
```

**剖析要点：**
- 退出流程：获取当前用户 -> 删除Redis中的LoginUser缓存 -> 异步记录退出日志 -> 返回成功JSON
- 使用`AsyncManager`异步记录日志，不阻塞退出响应
- 删除Redis缓存后，该Token立即失效，实现真正的"踢下线"

### 3.5 UserDetailsServiceImpl - 用户认证服务

> 源码位置：`ruoyi-framework/src/main/java/com/ruoyi/framework/web/service/UserDetailsServiceImpl.java`

```java
@Service
public class UserDetailsServiceImpl implements UserDetailsService
{
    @Autowired
    private ISysUserService userService;
    @Autowired
    private SysPasswordService passwordService;
    @Autowired
    private SysPermissionService permissionService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException
    {
        SysUser user = userService.selectUserByUserName(username);
        if (StringUtils.isNull(user))
        {
            log.info("登录用户：{} 不存在.", username);
            throw new ServiceException(MessageUtils.message("user.not.exists"));
        }
        else if (UserStatus.DELETED.getCode().equals(user.getDelFlag()))
        {
            log.info("登录用户：{} 已被删除.", username);
            throw new ServiceException(MessageUtils.message("user.password.delete"));
        }
        else if (UserStatus.DISABLE.getCode().equals(user.getStatus()))
        {
            log.info("登录用户：{} 已被停用.", username);
            throw new ServiceException(MessageUtils.message("user.blocked"));
        }

        passwordService.validate(user);
        return createLoginUser(user);
    }

    public UserDetails createLoginUser(SysUser user)
    {
        return new LoginUser(user.getUserId(), user.getDeptId(), user, permissionService.getMenuPermission(user));
    }
}
```

**剖析要点：**
- 实现Spring Security的`UserDetailsService`接口，是认证的核心入口
- 校验顺序：用户是否存在 -> 是否已删除 -> 是否已停用 -> 密码是否正确（含错误次数校验）
- `createLoginUser`将SysUser包装为LoginUser，同时加载用户的菜单权限集合
- 异常信息通过`MessageUtils.message()`国际化处理

### 3.6 SysLoginService - 登录业务逻辑

> 源码位置：`ruoyi-framework/src/main/java/com/ruoyi/framework/web/service/SysLoginService.java`

```java
public String login(String username, String password, String code, String uuid)
{
    // 验证码校验
    validateCaptcha(username, code, uuid);
    // 登录前置校验
    loginPreCheck(username, password);
    // 用户验证
    Authentication authentication = null;
    try
    {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
        AuthenticationContextHolder.setContext(authenticationToken);
        // 该方法会去调用UserDetailsServiceImpl.loadUserByUsername
        authentication = authenticationManager.authenticate(authenticationToken);
    }
    catch (Exception e)
    {
        if (e instanceof BadCredentialsException)
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();
        }
        else
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, e.getMessage()));
            throw new ServiceException(e.getMessage());
        }
    }
    finally
    {
        AuthenticationContextHolder.clearContext();
    }
    AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success")));
    LoginUser loginUser = (LoginUser) authentication.getPrincipal();
    recordLoginInfo(loginUser.getUserId());
    // 生成token
    return tokenService.createToken(loginUser);
}
```

**登录前置校验逻辑：**

```java
public void loginPreCheck(String username, String password)
{
    // 用户名或密码为空
    if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password))
    {
        throw new UserNotExistsException();
    }
    // 密码长度校验
    if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
            || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
    {
        throw new UserPasswordNotMatchException();
    }
    // 用户名长度校验
    if (username.length() < UserConstants.USERNAME_MIN_LENGTH
            || username.length() > UserConstants.USERNAME_MAX_LENGTH)
    {
        throw new UserPasswordNotMatchException();
    }
    // IP黑名单校验
    String blackStr = configService.selectConfigByKey("sys.login.blackIPList");
    if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr()))
    {
        throw new BlackListException();
    }
}
```

**剖析要点：**
- 登录流程：验证码校验 -> 前置校验(非空/长度/IP黑名单) -> Spring Security认证 -> 记录日志 -> 生成Token
- `AuthenticationContextHolder`使用ThreadLocal存储认证上下文，在finally中清理，防止内存泄漏
- 每个校验步骤失败都会异步记录登录失败日志
- `authenticationManager.authenticate()`会触发`UserDetailsServiceImpl.loadUserByUsername()`

### 3.7 SysPasswordService - 密码校验与锁定

> 源码位置：`ruoyi-framework/src/main/java/com/ruoyi/framework/web/service/SysPasswordService.java`

```java
@Component
public class SysPasswordService
{
    @Autowired
    private RedisCache redisCache;
    @Value(value = "${user.password.maxRetryCount}")
    private int maxRetryCount;
    @Value(value = "${user.password.lockTime}")
    private int lockTime;

    private String getCacheKey(String username)
    {
        return CacheConstants.PWD_ERR_CNT_KEY + username;
    }

    public void validate(SysUser user)
    {
        Authentication usernamePasswordAuthenticationToken = AuthenticationContextHolder.getContext();
        String username = usernamePasswordAuthenticationToken.getName();
        String password = usernamePasswordAuthenticationToken.getCredentials().toString();

        Integer retryCount = redisCache.getCacheObject(getCacheKey(username));
        if (retryCount == null)
        {
            retryCount = 0;
        }
        if (retryCount >= Integer.valueOf(maxRetryCount).intValue())
        {
            throw new UserPasswordRetryLimitExceedException(maxRetryCount, lockTime);
        }
        if (!matches(user, password))
        {
            retryCount = retryCount + 1;
            redisCache.setCacheObject(getCacheKey(username), retryCount, lockTime, TimeUnit.MINUTES);
            throw new UserPasswordNotMatchException();
        }
        else
        {
            clearLoginRecordCache(username);
        }
    }
}
```

**剖析要点：**
- 密码错误次数存储在Redis中，Key为`pwd_err_cnt:{username}`
- 默认最大重试5次，锁定10分钟（通过`application.yml`配置）
- 密码正确后清除错误计数缓存
- 使用BCrypt进行密码匹配：`SecurityUtils.matchesPassword(rawPassword, user.getPassword())`

### 3.8 PermissionService - 自定义权限Bean

> 源码位置：`ruoyi-framework/src/main/java/com/ruoyi/framework/web/service/PermissionService.java`

```java
@Service("ss")
public class PermissionService
{
    public boolean hasPermi(String permission) { ... }
    public boolean lacksPermi(String permission) { ... }
    public boolean hasAnyPermi(String permissions) { ... }
    public boolean hasRole(String role) { ... }
    public boolean lacksRole(String role) { ... }
    public boolean hasAnyRoles(String roles) { ... }

    private boolean hasPermissions(Set<String> permissions, String permission)
    {
        return permissions.contains(Constants.ALL_PERMISSION) || permissions.contains(StringUtils.trim(permission));
    }
}
```

**使用方式（在Controller中）：**

```java
@PreAuthorize("@ss.hasPermi('system:user:list')")
@GetMapping("/list")
public AjaxResult list(SysUser user) { ... }

@PreAuthorize("@ss.hasRole('admin')")
@PostMapping("/add")
public AjaxResult add(@RequestBody SysUser user) { ... }
```

**剖析要点：**
- Bean名称为`ss`（取自SpringSecurity首字母），在SpEL表达式中通过`@ss`引用
- `ALL_PERMISSION = "*:*:*"`，管理员拥有此权限，直接通过所有权限校验
- `hasAnyPermi`支持逗号分隔的多权限判断，任一匹配即通过

### 3.9 PermitAllUrlProperties - 匿名URL自动收集

> 源码位置：`ruoyi-framework/src/main/java/com/ruoyi/framework/config/properties/PermitAllUrlProperties.java`

```java
@Configuration
public class PermitAllUrlProperties implements InitializingBean, ApplicationContextAware
{
    private static final Pattern PATTERN = Pattern.compile("\\{(.*?)\\}");
    private ApplicationContext applicationContext;
    private List<String> urls = new ArrayList<>();

    @Override
    public void afterPropertiesSet()
    {
        RequestMappingHandlerMapping mapping = applicationContext.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> map = mapping.getHandlerMethods();

        map.keySet().forEach(info -> {
            HandlerMethod handlerMethod = map.get(info);
            // 获取方法上边的注解 替代path variable 为 *
            Anonymous method = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Anonymous.class);
            Optional.ofNullable(method).ifPresent(anonymous -> Objects.requireNonNull(info.getPathPatternsCondition().getPatternValues())
                    .forEach(url -> urls.add(RegExUtils.replaceAll(url, PATTERN, ASTERISK))));
            // 获取类上边的注解
            Anonymous controller = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), Anonymous.class);
            Optional.ofNullable(controller).ifPresent(anonymous -> Objects.requireNonNull(info.getPathPatternsCondition().getPatternValues())
                    .forEach(url -> urls.add(RegExUtils.replaceAll(url, PATTERN, ASTERISK))));
        });
    }
}
```

**剖析要点：**
- 实现`InitializingBean`，在Bean初始化完成后扫描所有Controller方法
- 自动收集标注了`@Anonymous`注解的URL，将其设为`permitAll()`
- 将路径变量`{xxx}`替换为`*`通配符，如`/api/{id}`变为`/api/*`
- 支持方法级别和类级别的`@Anonymous`注解

---

## 四、细节留神

1. **SecurityContext清理**：`AuthenticationContextHolder`使用ThreadLocal存储认证信息，在`finally`块中清理，防止线程池复用时导致认证信息泄漏。
2. **过滤器顺序**：CorsFilter必须在JwtAuthenticationTokenFilter之前，否则跨域预检请求（OPTIONS）会被JWT过滤器拦截。
3. **密码错误锁定**：密码错误次数存在Redis中，如果Redis重启，错误计数会丢失。这是有意设计还是需要改进？
4. **@Anonymous vs permitAll**：`@Anonymous`注解用于标记不需要认证的接口，比在SecurityConfig中硬编码URL更灵活、更易维护。
5. **权限缓存时机**：用户权限在登录时加载并缓存到LoginUser中（Redis），角色权限变更后需要主动刷新在线用户的权限缓存。

---

## 五、提问方向

1. **若依为什么选择禁用CSRF和Session？在什么场景下这种选择可能带来安全风险？**

2. **`AuthenticationContextHolder`使用ThreadLocal存储认证上下文，在异步线程（如@Async方法）中能否正常获取认证信息？如何解决？**

3. **密码错误锁定机制依赖Redis存储错误次数，如果Redis宕机重启，锁定机制会失效。如何设计一个更可靠的密码错误锁定方案？**

4. **`@Anonymous`注解的URL自动收集机制在应用启动时执行，如果使用了动态路由或自定义RequestMapping，能否被正确收集？**

5. **`PermissionService`的Bean名称为`ss`，如果项目中存在其他名为`ss`的Bean，会如何处理？这种命名方式有什么潜在问题？**

6. **SecurityConfig中配置了`.anyRequest().authenticated()`，如果新增了一个接口忘记配置权限，默认行为是什么？如何设计一个"默认拒绝"的安全策略？**

7. **JWT过滤器中`SecurityContextHolder.getContext().setAuthentication()`设置的是线程级别的认证信息，在Tomcat线程池复用场景下，如果过滤器链中某个环节抛出异常未清理SecurityContext，会导致什么问题？**
