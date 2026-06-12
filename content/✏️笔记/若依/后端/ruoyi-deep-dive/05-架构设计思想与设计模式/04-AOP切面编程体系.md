# 04 - AOP切面编程体系

> 本文档全面分析 RuoYi-Vue 项目中 4 个核心 AOP 切面的设计、执行顺序、注解驱动机制和实际应用场景。

---

## 一、AOP 切面总览

### 1.1 四大切面一览

| 切面类 | 关联注解 | 通知类型 | 执行顺序 | 功能 |
|--------|----------|----------|----------|------|
| `DataSourceAspect` | `@DataSource` | `@Around` | 1（最高） | 多数据源切换 |
| `DataScopeAspect` | `@DataScope` | `@Before` | 2 | 数据权限过滤 |
| `RateLimiterAspect` | `@RateLimiter` | `@Before` | 3 | 接口限流 |
| `LogAspect` | `@Log` | `@Before` + `@AfterReturning` + `@AfterThrowing` | 4 | 操作日志记录 |

### 1.2 文件位置

```
ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/
    ├── DataSourceAspect.java      -- 多数据源切换
    ├── DataScopeAspect.java       -- 数据权限过滤
    ├── RateLimiterAspect.java    -- 接口限流
    └── LogAspect.java             -- 操作日志记录
```

### 1.3 注解定义位置

```
ruoyi-common/src/main/java/com/ruoyi/common/annotation/
    ├── DataSource.java           -- 数据源切换注解
    ├── DataScope.java            -- 数据权限过滤注解
    ├── RateLimiter.java          -- 限流注解
    └── Log.java                  -- 操作日志注解
```

---

## 二、DataSourceAspect -- 多数据源切换

### 2.1 设计思路

通过 `@DataSource` 注解标记需要切换数据源的方法或类，`DataSourceAspect` 在方法执行前切换到指定数据源，执行后恢复默认数据源。

### 2.2 查看&剖析点

- 注解定义：`ruoyi-common/.../annotation/DataSource.java`
- 切面实现：`ruoyi-framework/.../aspectj/DataSourceAspect.java`
- 数据源上下文：`ruoyi-framework/.../datasource/DynamicDataSourceContextHolder.java`
- 动态数据源：`ruoyi-framework/.../datasource/DynamicDataSource.java`
- 数据源配置：`ruoyi-framework/.../config/DruidConfig.java`

### 2.3 源码关键片段引用

**注解定义**：

```java
// 文件：ruoyi-common/src/main/java/com/ruoyi/common/annotation/DataSource.java
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DataSource
{
    public DataSourceType value() default DataSourceType.MASTER;
}
```

**切面实现 -- 使用 @Around 环绕通知**：

```java
// 文件：ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/DataSourceAspect.java
@Aspect
@Order(1)  // 最高优先级，确保数据源在其他切面之前切换
@Component
public class DataSourceAspect
{
    // 切入点：方法上的 @DataSource 注解 或 类上的 @DataSource 注解
    @Pointcut("@annotation(com.ruoyi.common.annotation.DataSource)"
            + "|| @within(com.ruoyi.common.annotation.DataSource)")
    public void dsPointCut() {}

    @Around("dsPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable
    {
        DataSource dataSource = getDataSource(point);
        if (StringUtils.isNotNull(dataSource))
        {
            // 切换数据源
            DynamicDataSourceContextHolder.setDataSourceType(dataSource.value().name());
        }
        try
        {
            return point.proceed();  // 执行目标方法
        }
        finally
        {
            // 恢复默认数据源（无论成功还是异常都会执行）
            DynamicDataSourceContextHolder.clearDataSourceType();
        }
    }

    // 优先获取方法上的注解，其次获取类上的注解
    public DataSource getDataSource(ProceedingJoinPoint point)
    {
        MethodSignature signature = (MethodSignature) point.getSignature();
        DataSource dataSource = AnnotationUtils.findAnnotation(signature.getMethod(), DataSource.class);
        if (Objects.nonNull(dataSource)) {
            return dataSource;
        }
        return AnnotationUtils.findAnnotation(signature.getDeclaringType(), DataSource.class);
    }
}
```

**数据源上下文 -- ThreadLocal 保证线程安全**：

```java
// 文件：ruoyi-framework/src/main/java/com/ruoyi/framework/datasource/DynamicDataSourceContextHolder.java
public class DynamicDataSourceContextHolder
{
    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();

    public static void setDataSourceType(String dsType) {
        log.info("切换到{}数据源", dsType);
        CONTEXT_HOLDER.set(dsType);
    }

    public static String getDataSourceType() {
        return CONTEXT_HOLDER.get();
    }

    public static void clearDataSourceType() {
        CONTEXT_HOLDER.remove();
    }
}
```

**动态数据源 -- Spring 的 AbstractRoutingDataSource**：

```java
// 文件：ruoyi-framework/src/main/java/com/ruoyi/framework/datasource/DynamicDataSource.java
public class DynamicDataSource extends AbstractRoutingDataSource
{
    public DynamicDataSource(DataSource defaultTargetDataSource, Map<Object, Object> targetDataSources)
    {
        super.setDefaultTargetDataSource(defaultTargetDataSource);
        super.setTargetDataSources(targetDataSources);
        super.afterPropertiesSet();
    }

    @Override
    protected Object determineCurrentLookupKey()
    {
        return DynamicDataSourceContextHolder.getDataSourceType();
    }
}
```

### 2.4 执行流程

```
Service方法调用
    |
    v
DataSourceAspect.around()
    |-- getDataSource() 获取注解中的数据源类型
    |-- DynamicDataSourceContextHolder.setDataSourceType("SLAVE")  // 切换到从库
    |-- point.proceed()  // 执行目标方法
    |       |
    |       v
    |   DynamicDataSource.determineCurrentLookupKey()  // MyBatis获取连接时调用
    |       |-- return "SLAVE"
    |       |-- 返回从库连接
    |       |
    |       v
    |   执行SQL（从库）
    |
    |-- DynamicDataSourceContextHolder.clearDataSourceType()  // 恢复默认
```

---

## 三、DataScopeAspect -- 数据权限过滤

### 3.1 设计思路

通过 `@DataScope` 注解标记需要进行数据权限过滤的方法，切面在方法执行前根据当前用户的角色数据权限范围，动态拼接 SQL 条件到 `BaseEntity.params` 中。

### 3.2 查看&剖析点

- 注解定义：`ruoyi-common/.../annotation/DataScope.java`
- 切面实现：`ruoyi-framework/.../aspectj/DataScopeAspect.java`
- 实体基类：`ruoyi-common/.../core/domain/BaseEntity.java`（`params` 字段）

### 3.3 源码关键片段引用

**注解定义**：

```java
// 文件：ruoyi-common/src/main/java/com/ruoyi/common/annotation/DataScope.java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope
{
    public String userAlias() default "";     // 用户表别名
    public String deptAlias() default "";     // 部门表别名
    public String userField() default "user_id";  // 用户字段名
    public String deptField() default "dept_id";  // 部门字段名
    public String permission() default "";      // 权限字符
}
```

**切面实现 -- 使用 @Before 前置通知**：

```java
// 文件：ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/DataScopeAspect.java
@Aspect
@Component
public class DataScopeAspect
{
    public static final String DATA_SCOPE = "dataScope";

    @Before("@annotation(controllerDataScope)")
    public void doBefore(JoinPoint point, DataScope controllerDataScope) throws Throwable
    {
        clearDataScope(point);      // 防SQL注入：先清空
        handleDataScope(point, controllerDataScope);  // 拼接SQL
    }

    protected void handleDataScope(final JoinPoint joinPoint, DataScope controllerDataScope)
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (StringUtils.isNotNull(loginUser))
        {
            SysUser currentUser = loginUser.getUser();
            // 超级管理员不过滤数据
            if (StringUtils.isNotNull(currentUser) && !currentUser.isAdmin())
            {
                dataScopeFilter(joinPoint, currentUser,
                    controllerDataScope.userAlias(),
                    controllerDataScope.deptAlias(),
                    controllerDataScope.userField(),
                    controllerDataScope.deptField(),
                    permission);
            }
        }
    }

    public static void dataScopeFilter(JoinPoint joinPoint, SysUser user,
            String userAlias, String deptAlias, String userField, String deptField, String permission)
    {
        StringBuilder sqlString = new StringBuilder();
        List<String> conditions = new ArrayList<String>();

        for (SysRole role : user.getRoles())
        {
            String dataScope = role.getDataScope();
            // 根据不同权限范围拼接不同SQL
            if (Constants.Dept.DATA_SCOPE_ALL.equals(dataScope)) {
                sqlString = new StringBuilder();  // 全部数据，不拼接条件
                break;
            }
            else if (Constants.Dept.DATA_SCOPE_CUSTOM.equals(dataScope)) {
                sqlString.append(StringUtils.format(
                    " OR {}.{} IN ( SELECT dept_id FROM sys_role_dept WHERE role_id = {} ) ",
                    deptAlias, deptField, role.getRoleId()));
            }
            else if (Constants.Dept.DATA_SCOPE_DEPT.equals(dataScope)) {
                sqlString.append(StringUtils.format(" OR {}.{} = {} ", deptAlias, deptField, user.getDeptId()));
            }
            else if (Constants.Dept.DATA_SCOPE_DEPT_AND_CHILD.equals(dataScope)) {
                sqlString.append(StringUtils.format(
                    " OR {}.{} IN ( SELECT dept_id FROM sys_dept WHERE dept_id = {} or find_in_set( {} , ancestors ) )",
                    deptAlias, deptField, user.getDeptId(), user.getDeptId()));
            }
            else if (Constants.Dept.DATA_SCOPE_SELF.equals(dataScope)) {
                sqlString.append(StringUtils.format(" OR {}.{} = {} ", userAlias, userField, user.getUserId()));
            }
            conditions.add(dataScope);
        }

        // 将SQL片段放入BaseEntity.params
        if (StringUtils.isNotBlank(sqlString.toString()))
        {
            Object params = joinPoint.getArgs()[0];
            if (params instanceof BaseEntity)
            {
                BaseEntity baseEntity = (BaseEntity) params;
                baseEntity.getParams().put(DATA_SCOPE, " AND (" + sqlString.substring(4) + ")");
            }
        }
    }
}
```

### 3.4 实际应用场景

```java
// Service 中的典型用法
@DataScope(deptAlias = "d", userAlias = "u")
public List<SysUser> selectUserList(SysUser user)
{
    return userMapper.selectUserList(user);
}

// Mapper XML 中引用
<select id="selectUserList" parameterType="SysUser" resultMap="SysUserResult">
    select u.*, d.dept_name from sys_user u
    left join sys_dept d on u.dept_id = d.dept_id
    where u.del_flag = '0'
    ${params.dataScope}  <!-- 数据权限SQL片段 -->
</select>
```

---

## 四、RateLimiterAspect -- 接口限流

### 4.1 设计思路

通过 `@RateLimiter` 注解标记需要限流的接口，切面在方法执行前通过 Redis + Lua 脚本实现分布式限流。

### 4.2 查看&剖析点

- 注解定义：`ruoyi-common/.../annotation/RateLimiter.java`
- 切面实现：`ruoyi-framework/.../aspectj/RateLimiterAspect.java`
- 限流脚本：Redis Lua 脚本（通过 `RedisScript<Long>` 加载）

### 4.3 源码关键片段引用

**注解定义**：

```java
// 文件：ruoyi-common/src/main/java/com/ruoyi/common/annotation/RateLimiter.java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter
{
    public String key() default CacheConstants.RATE_LIMIT_KEY;  // 限流key前缀
    public int time() default 60;    // 限流时间窗口（秒）
    public int count() default 100; // 时间窗口内允许的请求数
    public LimitType limitType() default LimitType.DEFAULT;  // 限流类型（全局/IP）
}
```

**切面实现 -- 使用 @Before 前置通知**：

```java
// 文件：ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/RateLimiterAspect.java
@Aspect
@Component
public class RateLimiterAspect
{
    private RedisTemplate<Object, Object> redisTemplate;
    private RedisScript<Long> limitScript;  // Lua限流脚本

    @Before("@annotation(rateLimiter)")
    public void doBefore(JoinPoint point, RateLimiter rateLimiter) throws Throwable
    {
        int time = rateLimiter.time();
        int count = rateLimiter.count();

        // 构建限流key：前缀 + IP(可选) + 类名 + 方法名
        String combineKey = getCombineKey(rateLimiter, point);
        List<Object> keys = Collections.singletonList(combineKey);

        try
        {
            // 执行Redis Lua脚本进行限流判断
            Long number = redisTemplate.execute(limitScript, keys, count, time);
            if (StringUtils.isNull(number) || number.intValue() > count)
            {
                throw new ServiceException("访问过于频繁，请稍候再试");
            }
        }
        catch (ServiceException e) {
            throw e;  // 限流异常直接抛出
        }
        catch (Exception e) {
            throw new RuntimeException("服务器限流异常，请稍候再试");
        }
    }

    public String getCombineKey(RateLimiter rateLimiter, JoinPoint point)
    {
        StringBuffer stringBuffer = new StringBuffer(rateLimiter.key());
        if (rateLimiter.limitType() == LimitType.IP) {
            stringBuffer.append(IpUtils.getIpAddr()).append("-");  // IP级别限流
        }
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = method.getDeclaringClass();
        stringBuffer.append(targetClass.getName()).append("-").append(method.getName());
        return stringBuffer.toString();
    }
}
```

### 4.4 限流算法

RuoYi-Vue 使用 **Redis + Lua 脚本** 实现滑动窗口限流，Lua 脚本保证原子性：

```lua
-- 限流Lua脚本（典型实现）
local key = KEYS[1]
local count = tonumber(ARGV[1])
local time = tonumber(ARGV[2])
local current = redis.call('INCR', key)
if current == 1 then
    redis.call('EXPIRE', key, time)
end
if current > count then
    return current
end
return current
```

---

## 五、LogAspect -- 操作日志记录

### 5.1 设计思路

通过 `@Log` 注解标记需要记录日志的方法，切面在方法执行后（成功或异常）异步记录操作日志到数据库。

### 5.2 查看&剖析点

- 注解定义：`ruoyi-common/.../annotation/Log.java`
- 切面实现：`ruoyi-framework/.../aspectj/LogAspect.java`
- 异步工厂：`ruoyi-framework/.../manager/factory/AsyncFactory.java`
- 异步管理：`ruoyi-framework/.../manager/AsyncManager.java`

### 5.3 源码关键片段引用

**注解定义**：

```java
// 文件：ruoyi-common/src/main/java/com/ruoyi/common/annotation/Log.java
@Target({ ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log
{
    public String title() default "";                            // 模块名称
    public BusinessType businessType() default BusinessType.OTHER; // 操作类型
    public OperatorType operatorType() default OperatorType.MANAGE; // 操作人类别
    public boolean isSaveRequestData() default true;              // 是否保存请求参数
    public boolean isSaveResponseData() default true;             // 是否保存响应参数
    public String[] excludeParamNames() default {};               // 排除的参数名
}
```

**切面实现 -- 三种通知组合**：

```java
// 文件：ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/LogAspect.java
@Aspect
@Component
public class LogAspect
{
    public static final String[] EXCLUDE_PROPERTIES = { "password", "oldPassword", "newPassword", "confirmPassword" };
    private static final ThreadLocal<Long> TIME_THREADLOCAL = new NamedThreadLocal<>("Cost Time");

    // 通知1：方法执行前 -- 记录开始时间
    @Before(value = "@annotation(controllerLog)")
    public void doBefore(JoinPoint joinPoint, Log controllerLog)
    {
        TIME_THREADLOCAL.set(System.currentTimeMillis());
    }

    // 通知2：方法正常返回后 -- 记录操作日志
    @AfterReturning(pointcut = "@annotation(controllerLog)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Log controllerLog, Object jsonResult)
    {
        handleLog(joinPoint, controllerLog, null, jsonResult);
    }

    // 通知3：方法抛出异常后 -- 记录异常日志
    @AfterThrowing(value = "@annotation(controllerLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Log controllerLog, Exception e)
    {
        handleLog(joinPoint, controllerLog, e, null);
    }

    protected void handleLog(final JoinPoint joinPoint, Log controllerLog, final Exception e, Object jsonResult)
    {
        try
        {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            SysOperLog operLog = new SysOperLog();
            operLog.setStatus(BusinessStatus.SUCCESS.ordinal());
            operLog.setOperIp(IpUtils.getIpAddr());
            operLog.setOperUrl(StringUtils.substring(ServletUtils.getRequest().getRequestURI(), 0, 255));
            if (loginUser != null) {
                operLog.setOperName(loginUser.getUsername());
            }
            if (e != null) {
                operLog.setStatus(BusinessStatus.FAIL.ordinal());
                operLog.setErrorMsg(StringUtils.substring(
                    Convert.toStr(e.getMessage(), ExceptionUtil.getExceptionMessage(e)), 0, 2000));
            }
            operLog.setMethod(joinPoint.getTarget().getClass().getName()
                + "." + joinPoint.getSignature().getName() + "()");
            operLog.setRequestMethod(ServletUtils.getRequest().getMethod());
            getControllerMethodDescription(joinPoint, controllerLog, operLog, jsonResult);
            operLog.setCostTime(System.currentTimeMillis() - TIME_THREADLOCAL.get());
            // 异步保存到数据库
            AsyncManager.me().execute(AsyncFactory.recordOper(operLog));
        }
        catch (Exception exp) {
            log.error("异常信息:{}", exp.getMessage());
        }
        finally {
            TIME_THREADLOCAL.remove();  // 清理ThreadLocal防止内存泄漏
        }
    }
}
```

### 5.4 日志记录流程

```
Controller方法执行
    |
    v
@Before: 记录开始时间到 ThreadLocal
    |
    v
Controller方法执行（业务逻辑）
    |
    ├── 成功 ──> @AfterReturning: handleLog(null, jsonResult)
    |
    └── 异常 ──> @AfterThrowing: handleLog(e, null)
                    |
                    v
                AsyncManager.me().execute(AsyncFactory.recordOper(operLog))
                    |
                    v
                异步线程池执行
                    |
                    v
                查询IP归属地 -> ISysOperLogService.insertOperlog(operLog)
```

---

## 六、切面执行顺序详解

### 6.1 顺序控制机制

```java
// DataSourceAspect 使用 @Order(1) 确保最高优先级
@Aspect
@Order(1)
@Component
public class DataSourceAspect { ... }

// 其他切面没有指定 @Order，使用默认顺序
// 在同一个 JoinPoint 上，@Before 通知的执行顺序由 @Order 决定
// @After/@AfterReturning/@AfterThrowing 的执行顺序与 @Before 相反
```

### 6.2 多切面组合场景

当一个方法同时标注了多个注解时：

```java
// 假设一个方法同时有多个注解
@DataSource(DataSourceType.SLAVE)   // 切换到从库
@DataScope(deptAlias = "d")         // 数据权限过滤
@Log(title = "用户查询")              // 记录操作日志
public List<SysUser> selectUserList(SysUser user) { ... }
```

**执行顺序**：

```
1. DataSourceAspect @Around 开始
    └── 切换数据源到 SLAVE

2. DataScopeAspect @Before
    └── 拼接数据权限SQL

3. RateLimiterAspect @Before（如果有）
    └── 限流检查

4. LogAspect @Before
    └── 记录开始时间

5. 目标方法执行
    └── userMapper.selectUserList(user)
        └── 使用SLAVE数据源 + 数据权限SQL

6. LogAspect @AfterReturning / @AfterThrowing
    └── 异步记录操作日志

7. DataSourceAspect @Around 结束
    └── 恢复默认数据源
```

### 6.3 顺序设计原因

- **DataSourceAspect 必须最先执行**：数据源切换必须在 SQL 执行之前完成，且必须在 finally 中恢复
- **DataScopeAspect 在数据源之后**：数据权限SQL拼接依赖数据源已切换到正确的库
- **RateLimiterAspect 在业务之前**：限流检查应该在业务逻辑之前进行
- **LogAspect 最后**：日志记录需要获取方法执行的耗时和结果

---

## 七、注解驱动机制

### 7.1 注解 -> 切面 -> 业务 的连接方式

```
┌──────────┐     ┌──────────────┐     ┌──────────────┐
│  @Log    │────>│  LogAspect   │────>│ AsyncFactory │
│  注解    │     │  切面        │     │ 异步记录日志  │
└──────────┘     └──────────────┘     └──────────────┘
       │                │                     │
  标记在Controller    通过@AfterReturning    通过SpringUtils
  方法上              /@AfterThrowing       .getBean获取Service
  指定title/type     捕获方法结果/异常     保存到数据库
```

### 7.2 注解属性与切面行为的映射

| 注解 | 属性 | 切面行为 |
|------|------|----------|
| `@Log` | `title` | 设置日志模块名称 |
| `@Log` | `businessType` | 设置操作类型（INSERT/UPDATE/DELETE/EXPORT等） |
| `@Log` | `isSaveRequestData` | 是否记录请求参数 |
| `@Log` | `excludeParamNames` | 排除敏感参数 |
| `@DataSource` | `value` | 指定数据源类型（MASTER/SLAVE） |
| `@DataScope` | `deptAlias` | 指定部门表SQL别名 |
| `@DataScope` | `userAlias` | 指定用户表SQL别名 |
| `@RateLimiter` | `time` | 限流时间窗口 |
| `@RateLimiter` | `count` | 窗口内允许请求数 |
| `@RateLimiter` | `limitType` | 限流维度（全局/IP） |

---

## 细节留神

1. **DataSourceAspect 的 `@Order(1)`** 至关重要。如果没有设置最高优先级，数据源切换可能在其他切面之后执行，导致 SQL 在错误的数据源上执行。
2. **DataScopeAspect 使用 `${params.dataScope}`** 而非 `#{params.dataScope}`。`${}` 是 MyBatis 的字符串替换，直接将 SQL 片段嵌入到 SQL 语句中。虽然 `clearDataScope()` 方法做了防护，但使用 `${}` 本身就有 SQL 注入风险。
3. **RateLimiterAspect 的 Redis Lua 脚本** 通过 `RedisScript<Long>` 注入，确保了限流判断的原子性。如果 Redis 不可用，限流功能将直接抛出异常，需要考虑降级策略。
4. **LogAspect 使用 ThreadLocal 记录开始时间**，在 finally 块中清理。如果 `handleLog` 方法内部抛出异常被捕获，finally 仍然会执行清理，避免内存泄漏。
5. **四个切面可以组合使用**，但需要注意执行顺序。特别是 `@DataSource` 和 `@DataScope` 同时使用时，数据源切换必须在数据权限 SQL 拼接之前完成。
6. **LogAspect 的异步记录**通过 `AsyncManager.me().execute()` 实现，日志记录不会阻塞业务方法的返回。但如果异步线程池满了，新任务会被拒绝执行。

---

## 提问方向

1. **DataSourceAspect**：如果在一个事务中需要同时操作主库和从库（如主库写入、从库读取），当前的 `@DataSource` 注解能否满足？如果不能，应该如何设计多数据源事务方案？
2. **DataScopeAspect**：`${params.dataScope}` 使用字符串替换存在 SQL 注入风险。虽然 `clearDataScope()` 做了防护，但如果攻击者能在 `params` 中注入其他键值对呢？如何从根本上解决这个问题？
3. **RateLimiterAspect**：当前的限流是基于 Redis 的，如果 Redis 出现网络分区或宕机，限流功能会如何表现？如何设计一个优雅降级的限流方案？
4. **LogAspect**：`@AfterThrowing` 只能捕获方法抛出的异常。如果异常被全局异常处理器 `GlobalExceptionHandler` 捕获并返回了统一响应，`@AfterThrowing` 还能触发吗？为什么？
5. **执行顺序**：如果自定义了一个新的切面（如 `@Encrypt` 数据加密切面），应该如何设置 `@Order` 确保它在正确的位置执行？请描述完整的顺序设计思路。
6. **注解驱动**：`@DataScope` 注解的 `permission` 属性用于"多个角色匹配符合要求的权限"。请详细解释这个属性的使用场景，以及在 `DataScopeAspect` 中是如何利用这个属性的？
7. **综合设计**：如果要新增一个 `@Encrypt` 切面实现字段级加密（如手机号入库前加密、查询后解密），应该使用哪种通知类型？如何与现有的四个切面协调执行顺序？
