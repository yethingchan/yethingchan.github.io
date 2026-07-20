# 06 · 后端-AOP 与拦截器

> 对应清单：进阶第 8 条（操作日志）、高级第 16 条（数据权限）、高级第 21 条（防重复提交）、高级第 19 条（限流）。
> AOP 把"横切逻辑"（权限 SQL、日志、限流）从业务代码里抽出来织入。本文精讲两个切面，其余列职责。

## 一、DataScopeAspect —— 数据权限（高级第 16 条，RuoYi 最秀机制）

**问题**：管理员看全公司，部门经理只看自己部门，普通员工只看自己。如果每条 SQL 都手写 `AND dept_id = ?`，要写到死。RuoYi 用**注解 + 切面**解决。

先在被拦截的方法上标注解：
```java
@DataScope(deptAlias = "d")   // deptAlias：SQL 里部门表的别名
public List<SysUser> selectUserList(SysUser user) { ... }
```

切面核心：
```java
@Aspect
@Component
public class DataScopeAspect {
    public static final String DATA_SCOPE = "dataScope";

    @Before("@annotation(controllerDataScope)")     // ① 在标了 @DataScope 的方法前执行
    public void doBefore(JoinPoint point, DataScope controllerDataScope) {
        clearDataScope(point);                          // ② 先清空旧的，防注入
        handleDataScope(point, controllerDataScope);
    }

    protected void handleDataScope(JoinPoint joinPoint, DataScope ds) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser != null && !loginUser.getUser().isAdmin()) {  // ③ 管理员不过滤
            dataScopeFilter(joinPoint, loginUser.getUser(),
                ds.userAlias(), ds.deptAlias(), ds.userField(), ds.deptField(),
                StringUtils.defaultIfEmpty(ds.permission(), PermissionContextHolder.getContext()));
        }
    }

    // ④ 按角色的数据范围，拼出 SQL 片段
    public static void dataScopeFilter(JoinPoint joinPoint, SysUser user,
            String userAlias, String deptAlias, String userField, String deptField, String permission) {
        StringBuilder sqlString = new StringBuilder();
        for (SysRole role : user.getRoles()) {
            String dataScope = role.getDataScope();
            if (DATA_SCOPE_ALL.equals(dataScope)) {            // 全部
                sqlString = new StringBuilder(); break;
            } else if (DATA_SCOPE_CUSTOM.equals(dataScope)) {    // 自定义部门
                sqlString.append(format(" OR {}.{} IN ( SELECT dept_id FROM sys_role_dept WHERE role_id = {} ) ",
                        deptAlias, deptField, role.getRoleId()));
            } else if (DATA_SCOPE_DEPT.equals(dataScope)) {     // 仅本部门
                sqlString.append(format(" OR {}.{} = {} ", deptAlias, deptField, user.getDeptId()));
            } else if (DATA_SCOPE_DEPT_AND_CHILD.equals(dataScope)) { // 本部门及下属
                sqlString.append(format(" OR {}.{} IN ( SELECT dept_id FROM sys_dept WHERE dept_id = {} or find_in_set({}, ancestors) )",
                        deptAlias, deptField, user.getDeptId(), user.getDeptId()));
            } else if (DATA_SCOPE_SELF.equals(dataScope)) {     // 仅本人
                sqlString.append(format(" OR {}.{} = {} ", userAlias, userField, user.getUserId()));
            }
        }
        if (isNotBlank(sqlString.toString())) {
            Object params = joinPoint.getArgs()[0];            // ⑤ 拿到第一个入参（BaseEntity）
            if (params instanceof BaseEntity) {
                BaseEntity base = (BaseEntity) params;
                // ⑥ 拼好的 SQL 塞进 params.dataScope（去掉开头的 " OR "）
                base.getParams().put(DATA_SCOPE, " AND (" + sqlString.substring(4) + ")");
            }
        }
    }

    private void clearDataScope(JoinPoint joinPoint) {       // ⑦ 防注入：先置空
        Object params = joinPoint.getArgs()[0];
        if (params instanceof BaseEntity)
            ((BaseEntity) params).getParams().put(DATA_SCOPE, "");
    }
}
```

**逐行解释（这是理解 RuoYi 数据权限的全部）：**
1. `@Before("@annotation(...)")`：Spring AOP，**每当有方法标了 `@DataScope` 就先跑这段**。
2. `clearDataScope`：先把 `params.dataScope` 清空——**防止攻击者用同名参数注入恶意 SQL**（SQL 注入防护的"先清后写"）。
3. 管理员（`isAdmin`）直接跳过，看全部。
4. 遍历用户所有角色，按 `role.getDataScope()`（存在 `sys_role.data_scope`：1=全部/2=自定义/3=本部门/4=本部门及下属/5=仅本人）拼 `OR` 片段。多个角色取并集。
5. 切面拿到方法的**第一个入参**（约定必须是 `BaseEntity` 子类，如 `SysUser`），这就是 `[[04-后端-通用核心层]]` 里那个带 `params` 的基类。
6. 把拼好的 `AND (d.dept_id = 105)` 之类塞进 `params.dataScope`。
7. Mapper XML 里写 `<where> ... ${params.dataScope} </where>`——`${}` 直接拼接（因为片段是框架自己生成的可信串，且已先清空防注入）。

> **整条链路**：`@DataScope(deptAlias="d")` → 切面拼 SQL → 存 `BaseEntity.params.dataScope` → XML 用 `${params.dataScope}` 拼上 → 查出来的就是"你该看的数据"。新增业务表想带数据权限，只要实体 `extends BaseEntity` + 方法标 `@DataScope` + XML 加 `${params.dataScope}` 三件套即可。

## 二、LogAspect —— 操作日志（进阶第 8 条）

```java
@Aspect
@Component
public class LogAspect {
    public static final String[] EXCLUDE_PROPERTIES = { "password","oldPassword","newPassword","confirmPassword" };
    private static final ThreadLocal<Long> TIME_THREADLOCAL = new NamedThreadLocal<>("Cost Time");
    private static final int PARAM_MAX_LENGTH = 2000;

    @Before("@annotation(controllerLog)")                       // ① 方法前：记开始时间
    public void doBefore(JoinPoint jp, Log controllerLog) { TIME_THREADLOCAL.set(System.currentTimeMillis()); }

    @AfterReturning(pointcut="@annotation(controllerLog)", returning="jsonResult")  // ② 正常返回后
    public void doAfterReturning(JoinPoint jp, Log controllerLog, Object jsonResult) {
        handleLog(jp, controllerLog, null, jsonResult);
    }

    @AfterThrowing(value="@annotation(controllerLog)", throwing="e")  // ③ 抛异常后
    public void doAfterThrowing(JoinPoint jp, Log controllerLog, Exception e) {
        handleLog(jp, controllerLog, e, null);
    }

    protected void handleLog(JoinPoint jp, Log controllerLog, Exception e, Object jsonResult) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysOperLog operLog = new SysOperLog();
        operLog.setStatus(e == null ? BusinessStatus.SUCCESS.ordinal() : BusinessStatus.FAIL.ordinal());
        operLog.setOperName(loginUser.getUsername());
        operLog.setDeptName(loginUser.getDept().getDeptName());
        operLog.setOperIp(IpUtils.getIpAddr());
        operLog.setOperLocation(AddressUtils.getRealAddressByIP(...));
        operLog.setOperUrl(ServletUtils.getRequest().getRequestURI());
        operLog.setTitle(controllerLog.title());                    // ④ 读 @Log(title="用户管理")
        operLog.setBusinessType(controllerLog.businessType().ordinal()); // ⑤ 读 @Log(businessType=INSERT)
        // ... 取入参（过滤 password 字段）、序列化、算耗时 ...
        // ⑥ 异步入库
        AsyncManager.me().execute(AsyncFactory.recordOper(operLog));
    }
}
```

**解释：**
- 用法：**在 Controller 方法上写 `@Log(title = "用户管理", businessType = BusinessType.INSERT)`** 就自动记一条操作日志到 `sys_oper_log`。无需手写入库代码。
- ①②③ 是 AOP 的三种织入点：`@Before` 记时、`@AfterReturning` 成功、`@AfterThrowing` 失败。
- ④⑤ 读注解上的 `title` / `businessType`，所以日志标题和类型来自你标的注解，**注解即配置**。
- `EXCLUDE_PROPERTIES`：**密码等敏感字段绝不写进日志**（安全）。
- ⑥ `AsyncManager` 异步落库，不拖慢接口（线程池见 [[03-后端-配置层]]）。

## 三、其它切面 / 拦截器（知道职责即可）

| 类 | 类型 | 职责 | 清单 |
|---|---|---|---|
| `DataSourceAspect` | `@Aspect` | 配合 `@DataSource("slave")` 注解，切换多数据源（主库写/从库读） | （若启用多库） |
| `RateLimiterAspect` | `@Aspect` | 配合 `@RateLimiter` 注解做接口限流（Redis 令牌/计数） | 高级第 19 条 |
| `RepeatSubmitInterceptor` + `impl/SameUrlDataInterceptor` | `HandlerInterceptor` | 在 `ResourcesConfig` 注册，防表单重复提交（同 URL+参数短时间重复） | 高级第 21 条 |
| `RepeatableFilter` + `RepeatedlyRequestWrapper` | `Filter` | 让 request 的 body 可重复读（防重提交时要读两次 body） | — |
| `RefererFilter` | `Filter` | 防盗链（yml `referer.enabled`），只允许白名单域名引用资源 | — |
| `PropertyPreExcludeFilter` | `Filter` | Jackson 序列化时排除指定敏感属性（配合 `@JsonIgnore` 的另一种手段） | — |

> **限流 `@RateLimiter` 典型用法**（写在 Controller 上）：
> ```java
> @RateLimiter(count = 5, time = 60, limitType = LimitType.IP)
> @PostMapping("/test")
> public AjaxResult test() { ... }   // 同一 IP 60 秒内最多调 5 次
> ```
> **防重复提交**：前端 `request.js` 也有一层（见 [[16-前端-请求封装与权限工具]]），后端这层是"兜底"，用 `SameUrlDataInterceptor` 比 `url+参数+时间窗`。

## 四、AsyncManager —— 让 AOP 不阻塞

`LogAspect`、`SysLoginService` 都调 `AsyncManager.me().execute(AsyncFactory.recordXxx(...))`。它内部持有一个 `ThreadPoolExecutor`（来自 `ThreadPoolConfig`），把"写日志/记登录"这种**慢活丢给后台线程**，主接口立刻返回。这是 RuoYi 高并发下接口不被日志拖垮的关键。

> 学完本章，你掌握了 RuoYi 的两个"魔法"：**数据权限（注解→切面→SQL 注入 `params.dataScope`）** 和 **操作日志（注解即配置，AOP 自动落库）**，以及限流/防重提交的横切做法。下一章进业务层，看用户/角色/菜单这些实体和 Service 怎么组织。
