# 02 - Controller 层设计规范

> 本文档深入剖析 RuoYi-Vue 项目 Controller 层的设计规范，包括 BaseController 基类、注解使用规范、参数校验、返回值封装和典型代码分析。

---

## 一、Controller 层在架构中的定位

Controller 层是整个后端应用的"门面"，是前端与后端交互的入口。在 RuoYi-Vue 的三层架构中，Controller 层遵循**"薄控制器"**设计原则：

```
Controller 层职责边界：
  [接收请求] → [权限校验] → [参数校验] → [调用Service] → [封装返回]
       |            |            |              |             |
    不做业务     注解驱动      @Validated     委托Service   统一格式
    逻辑处理     声明式       JSR-303        不直接操作DB   AjaxResult
```

**核心原则：Controller 层不包含任何业务逻辑，所有业务逻辑下沉到 Service 层。**

---

## 二、BaseController 基类详解

所有 Controller 都继承自 `BaseController`，它封装了 Controller 层的通用能力。

### 2.1 BaseController 完整源码

```java
// 文件：ruoyi-common/src/main/java/com/ruoyi/common/core/controller/BaseController.java

public class BaseController
{
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** 日期格式自动转换 */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Date.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(DateUtils.parseDate(text));
            }
        });
    }

    /** 设置请求分页数据 */
    protected void startPage() {
        PageUtils.startPage();
    }

    /** 设置请求排序数据 */
    protected void startOrderBy() {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        if (StringUtils.isNotEmpty(pageDomain.getOrderBy())) {
            String orderBy = SqlUtil.escapeOrderBySql(pageDomain.getOrderBy());
            PageHelper.orderBy(orderBy);
        }
    }

    /** 清理分页的线程变量 */
    protected void clearPage() {
        PageUtils.clearPage();
    }

    /** 响应请求分页数据 */
    protected TableDataInfo getDataTable(List<?> list) {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setMsg("查询成功");
        rspData.setRows(list);
        rspData.setTotal(new PageInfo(list).getTotal());
        return rspData;
    }

    /** 返回成功/失败/警告 */
    public AjaxResult success() { return AjaxResult.success(); }
    public AjaxResult error() { return AjaxResult.error(); }
    public AjaxResult success(String message) { return AjaxResult.success(message); }
    public AjaxResult success(Object data) { return AjaxResult.success(data); }
    public AjaxResult error(String message) { return AjaxResult.error(message); }
    public AjaxResult warn(String message) { return AjaxResult.warn(message); }

    /** 根据影响行数返回操作结果 */
    protected AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error();
    }
    protected AjaxResult toAjax(boolean result) {
        return result ? success() : error();
    }

    /** 获取当前登录用户信息 */
    public LoginUser getLoginUser() { return SecurityUtils.getLoginUser(); }
    public Long getUserId() { return getLoginUser().getUserId(); }
    public Long getDeptId() { return getLoginUser().getDeptId(); }
    public String getUsername() { return getLoginUser().getUsername(); }
}
```

### 2.2 BaseController 提供的能力清单

| 方法 | 功能 | 使用场景 |
|------|------|----------|
| `startPage()` | 启动 PageHelper 分页 | 所有分页查询接口 |
| `startOrderBy()` | 设置排序参数 | 需要自定义排序的查询 |
| `getDataTable(list)` | 封装分页返回数据 | 分页查询接口的返回 |
| `toAjax(rows)` | 根据影响行数返回成功/失败 | 增删改操作 |
| `toAjax(result)` | 根据布尔值返回成功/失败 | 布尔类型操作 |
| `success()` / `error()` / `warn()` | 返回操作结果 | 通用响应 |
| `getLoginUser()` | 获取当前登录用户 | 需要操作人信息 |
| `getUserId()` / `getUsername()` | 获取当前用户ID/名称 | 设置 createBy/updateBy |
| `initBinder()` | 日期格式自动转换 | 接收日期参数时自动生效 |

### 2.3 为什么 BaseController 放在 ruoyi-common 模块？

BaseController 位于 `ruoyi-common` 模块而非 `ruoyi-admin` 模块，这样设计的原因：
- `ruoyi-generator` 和 `ruoyi-quartz` 模块的 Controller 也可以继承它
- 避免循环依赖（如果放在 admin 模块，其他模块无法依赖它）
- 体现了"通用能力下沉"的设计思想

---

## 三、注解使用规范

### 3.1 类级别注解

```java
@RestController                          // 标记为 REST 控制器，自动添加 @ResponseBody
@RequestMapping("/system/user")          // 定义模块的基础路径
public class SysUserController extends BaseController
```

### 3.2 权限控制注解 @PreAuthorize

RuoYi-Vue 使用 Spring Security 的 `@PreAuthorize` 注解进行方法级权限控制，通过自定义的 `@ss` (PermissionService) Bean 进行权限判断：

```java
// 权限字符串格式：模块:功能:操作
@PreAuthorize("@ss.hasPermi('system:user:list')")     // 查询权限
@PreAuthorize("@ss.hasPermi('system:user:add')")      // 新增权限
@PreAuthorize("@ss.hasPermi('system:user:edit')")     // 修改权限
@PreAuthorize("@ss.hasPermi('system:user:remove')")   // 删除权限
@PreAuthorize("@ss.hasPermi('system:user:export')")   // 导出权限
@PreAuthorize("@ss.hasPermi('system:user:import')")   // 导入权限
@PreAuthorize("@ss.hasRole('admin')")                // 角色判断
```

**权限字符串命名规范：** `模块名:业务名:操作类型`

| 操作类型 | 权限后缀 | HTTP 方法 |
|----------|----------|-----------|
| 查询列表 | `:list` | GET |
| 查询详情 | `:query` | GET |
| 新增 | `:add` | POST |
| 修改 | `:edit` | PUT |
| 删除 | `:remove` | DELETE |
| 导出 | `:export` | POST |
| 导入 | `:import` | POST |
| 授权 | `:edit` | PUT |

### 3.3 操作日志注解 @Log

```java
// 文件：ruoyi-common/src/main/java/com/ruoyi/common/annotation/Log.java
@Log(title = "用户管理", businessType = BusinessType.INSERT)
@Log(title = "用户管理", businessType = BusinessType.UPDATE)
@Log(title = "用户管理", businessType = BusinessType.DELETE)
@Log(title = "用户管理", businessType = BusinessType.EXPORT)
@Log(title = "用户管理", businessType = BusinessType.IMPORT)
@Log(title = "用户管理", businessType = BusinessType.GRANT)
```

**BusinessType 枚举值：**

| 值 | 含义 | 典型场景 |
|----|------|----------|
| OTHER | 其他 | 登录、登出 |
| INSERT | 新增 | 新增用户/角色 |
| UPDATE | 修改 | 修改用户信息 |
| DELETE | 删除 | 删除用户 |
| GRANT | 授权 | 分配角色 |
| EXPORT | 导出 | 导出 Excel |
| IMPORT | 导入 | 导入 Excel |
| GENCODE | 生成代码 | 代码生成 |

**LogAspect 切面处理流程：**

```java
// 文件：ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/LogAspect.java

@Before(value = "@annotation(controllerLog)")
public void doBefore(JoinPoint joinPoint, Log controllerLog) {
    TIME_THREADLOCAL.set(System.currentTimeMillis());  // 记录开始时间
}

@AfterReturning(pointcut = "@annotation(controllerLog)", returning = "jsonResult")
public void doAfterReturning(JoinPoint joinPoint, Log controllerLog, Object jsonResult) {
    handleLog(joinPoint, controllerLog, null, jsonResult);  // 正常返回时记录日志
}

@AfterThrowing(value = "@annotation(controllerLog)", throwing = "e")
public void doAfterThrowing(JoinPoint joinPoint, Log controllerLog, Exception e) {
    handleLog(joinPoint, controllerLog, e, null);  // 异常时也记录日志
}
```

### 3.4 限流注解 @RateLimiter

```java
@RateLimiter(count = 10, limitType = LimitType.IP)  // 限制每个IP 10次/分钟
```

### 3.5 防重复提交注解 @RepeatSubmit

```java
@RepeatSubmit  // 默认间隔5秒内不允许重复提交
```

### 3.6 数据权限注解 @DataScope

虽然 `@DataScope` 主要用在 Service 层，但理解它的存在对 Controller 层设计很重要：

```java
@DataScope(deptAlias = "d", userAlias = "u")  // 声明表别名，AOP切面会自动拼接SQL
```

---

## 四、参数校验规范

### 4.1 @Validated 注解触发 JSR-303 校验

```java
// 文件：SysUserController.java

@PostMapping
public AjaxResult add(@Validated @RequestBody SysUser user) {
    // @Validated 触发 SysUser 类上的 JSR-303 注解校验
    // 如 @NotBlank、@Size、@Email 等
}
```

### 4.2 手动业务校验

除了 JSR-303 自动校验，Controller 层还会进行手动业务校验：

```java
// 文件：SysUserController.java - 新增用户

@PostMapping
public AjaxResult add(@Validated @RequestBody SysUser user) {
    // 1. 数据权限校验
    deptService.checkDeptDataScope(user.getDeptId());
    roleService.checkRoleDataScope(user.getRoleIds());
    // 2. 唯一性校验
    if (!userService.checkUserNameUnique(user)) {
        return error("新增用户'" + user.getUserName() + "'失败，登录账号已存在");
    }
    if (!userService.checkPhoneUnique(user)) {
        return error("新增用户'" + user.getUserName() + "'失败，手机号码已存在");
    }
    if (!userService.checkEmailUnique(user)) {
        return error("新增用户'" + user.getUserName() + "'失败，邮箱账号已存在");
    }
    // 3. 设置操作人
    user.setCreateBy(getUsername());
    // 4. 密码加密
    user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
    return toAjax(userService.insertUser(user));
}
```

### 4.3 校验顺序规范

RuoYi-Vue 的 Controller 层校验遵循固定顺序：

```
1. @PreAuthorize 权限校验（Spring Security 自动处理）
2. @Validated JSR-303 校验（Spring MVC 自动处理）
3. 数据权限校验（手动调用 checkXxxDataScope）
4. 唯一性校验（手动调用 checkXxxUnique）
5. 操作权限校验（手动调用 checkXxxAllowed）
6. 设置操作人信息（getUsername()）
7. 调用 Service 处理业务
```

---

## 五、返回值封装规范

### 5.1 AjaxResult - 统一响应格式

```java
// 文件：ruoyi-common/src/main/java/com/ruoyi/common/core/domain/AjaxResult.java

public class AjaxResult extends HashMap<String, Object> {
    public static final String CODE_TAG = "code";   // 状态码
    public static final String MSG_TAG = "msg";     // 返回消息
    public static final String DATA_TAG = "data";   // 数据对象
}
```

**AjaxResult 响应格式：**

```json
// 成功响应
{
    "code": 200,
    "msg": "操作成功",
    "data": { ... }
}

// 失败响应
{
    "code": 500,
    "msg": "新增用户'admin'失败，登录账号已存在"
}

// 查询详情响应（可包含多个数据）
{
    "code": 200,
    "msg": "操作成功",
    "data": { "user": {...}, "roles": [...], "posts": [...] },
    "postIds": [1, 2],
    "roleIds": [1, 2]
}
```

### 5.2 TableDataInfo - 分页响应格式

```java
// 分页查询返回
{
    "code": 200,
    "msg": "查询成功",
    "rows": [ ... ],      // 数据列表
    "total": 100          // 总记录数
}
```

### 5.3 返回值使用规范

| 场景 | 返回类型 | 封装方式 |
|------|----------|----------|
| 分页查询 | `TableDataInfo` | `getDataTable(list)` |
| 单条查询 | `AjaxResult` | `success(data)` |
| 新增/修改/删除 | `AjaxResult` | `toAjax(rows)` |
| 文件导出 | `void` | 直接写入 HttpServletResponse |
| 文件下载 | `void` | 直接写入 HttpServletResponse |

---

## 六、RESTful API 设计规范

### 6.1 URL 命名规范

```
基础路径：/模块名/业务名
  /system/user          - 用户管理
  /system/role          - 角色管理
  /system/menu          - 菜单管理
  /system/dept          - 部门管理
  /tool/gen             - 代码生成
  /monitor/online        - 在线用户
```

### 6.2 HTTP 方法与操作对应

```java
@GetMapping("/list")                    // 分页查询列表
@GetMapping(value = { "/", "/{userId}" }) // 查询详情（支持无参和有参）
@PostMapping                            // 新增
@PutMapping                             // 修改
@DeleteMapping("/{userIds}")            // 批量删除
@PostMapping("/export")                 // 导出
@PostMapping("/importData")            // 导入
@PutMapping("/resetPwd")                // 重置密码
@PutMapping("/changeStatus")            // 修改状态
```

### 6.3 典型 Controller 完整代码分析

以 `SysUserController` 为例，展示完整的 Controller 设计模式：

```java
// 文件：ruoyi-admin/src/main/java/com/ruoyi/web/controller/system/SysUserController.java

@RestController                           // 1. REST控制器
@RequestMapping("/system/user")           // 2. 模块路径
public class SysUserController extends BaseController  // 3. 继承基类
{
    @Autowired private ISysUserService userService;     // 4. 注入Service接口
    @Autowired private ISysRoleService roleService;
    @Autowired private ISysDeptService deptService;
    @Autowired private ISysPostService postService;

    // 5. 分页查询 - 标准模式
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysUser user) {
        startPage();                              // 启动分页
        List<SysUser> list = userService.selectUserList(user);  // 调用Service
        return getDataTable(list);                // 封装分页数据
    }

    // 6. 导出 - 日志+权限+流式输出
    @Log(title = "用户管理", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('system:user:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysUser user) {
        List<SysUser> list = userService.selectUserList(user);
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        util.exportExcel(response, list, "用户数据");
    }

    // 7. 导入 - 文件上传+解析+批量处理
    @Log(title = "用户管理", businessType = BusinessType.IMPORT)
    @PreAuthorize("@ss.hasPermi('system:user:import')")
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        List<SysUser> userList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = userService.importUser(userList, updateSupport, operName);
        return success(message);
    }

    // 8. 查询详情 - 多数据聚合返回
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping(value = { "/", "/{userId}" })
    public AjaxResult getInfo(@PathVariable(value = "userId", required = false) Long userId) {
        AjaxResult ajax = AjaxResult.success();
        if (StringUtils.isNotNull(userId)) {
            userService.checkUserDataScope(userId);  // 数据权限校验
            SysUser sysUser = userService.selectUserById(userId);
            ajax.put(AjaxResult.DATA_TAG, sysUser);
            ajax.put("postIds", postService.selectPostListByUserId(userId));
            ajax.put("roleIds", sysUser.getRoles().stream()
                .map(SysRole::getRoleId).collect(Collectors.toList()));
        }
        List<SysRole> roles = roleService.selectRoleAll();
        ajax.put("roles", SecurityUtils.isAdmin(userId)
            ? roles : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        ajax.put("posts", postService.selectPostAll());
        return ajax;
    }

    // 9. 新增 - 完整校验链
    @PreAuthorize("@ss.hasPermi('system:user:add')")
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysUser user) {
        deptService.checkDeptDataScope(user.getDeptId());
        roleService.checkRoleDataScope(user.getRoleIds());
        if (!userService.checkUserNameUnique(user)) {
            return error("新增用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        // ... 其他唯一性校验
        user.setCreateBy(getUsername());
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        return toAjax(userService.insertUser(user));
    }

    // 10. 修改 - 先校验再修改
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysUser user) {
        userService.checkUserAllowed(user);         // 操作权限校验
        userService.checkUserDataScope(user.getUserId());  // 数据权限校验
        // ... 唯一性校验
        user.setUpdateBy(getUsername());
        return toAjax(userService.updateUser(user));
    }

    // 11. 删除 - 防止删除自己
    @PreAuthorize("@ss.hasPermi('system:user:remove')")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userIds}")
    public AjaxResult remove(@PathVariable Long[] userIds) {
        if (ArrayUtils.contains(userIds, getUserId())) {
            return error("当前用户不能删除");
        }
        return toAjax(userService.deleteUserByIds(userIds));
    }
}
```

---

## 七、Controller 层请求处理流程图

```
前端 HTTP 请求
    │
    ▼
┌─────────────────────────────────────────┐
│  CorsFilter (跨域处理)                   │
└────────────────┬────────────────────────┘
                 ▼
┌─────────────────────────────────────────┐
│  JwtAuthenticationTokenFilter            │
│  (解析Token → 获取LoginUser → 设置SecurityContext) │
└────────────────┬────────────────────────┘
                 ▼
┌─────────────────────────────────────────┐
│  Spring Security 权限过滤器链             │
│  (认证状态检查)                           │
└────────────────┬────────────────────────┘
                 ▼
┌─────────────────────────────────────────┐
│  DispatcherServlet 路由到 Controller      │
└────────────────┬────────────────────────┘
                 ▼
┌─────────────────────────────────────────┐
│  @PreAuthorize 权限校验                   │
│  (@ss.hasPermi 检查权限标识)              │
└────────────────┬────────────────────────┘
                 ▼
┌─────────────────────────────────────────┐
│  @Validated 参数校验                      │
│  (JSR-303 注解校验)                       │
└────────────────┬────────────────────────┘
                 ▼
┌─────────────────────────────────────────┐
│  LogAspect.doBefore()                    │
│  (记录请求开始时间)                        │
└────────────────┬────────────────────────┘
                 ▼
┌─────────────────────────────────────────┐
│  Controller 方法体执行                     │
│  (业务校验 → 调用Service → 封装返回)       │
└────────────────┬────────────────────────┘
                 ▼
┌─────────────────────────────────────────┐
│  LogAspect.doAfterReturning()            │
│  (异步记录操作日志)                        │
└────────────────┬────────────────────────┘
                 ▼
┌─────────────────────────────────────────┐
│  返回 JSON 响应                           │
└─────────────────────────────────────────┘
```

---

## 八、查看与剖析点

1. **查看所有 Controller 的目录结构**，理解模块划分方式
   - 路径：`ruoyi-admin/src/main/java/com/ruoyi/web/controller/`
   - 子包：`system/`、`monitor/`、`tool/`、`common/`

2. **查看 LogAspect 的完整实现**，理解操作日志如何通过 AOP 自动记录
   - 文件：`ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/LogAspect.java`

3. **查看 @Log 注解的定义**，理解注解属性设计
   - 文件：`ruoyi-common/src/main/java/com/ruoyi/common/annotation/Log.java`

4. **查看 PermissionService 的实现**，理解 `@ss.hasPermi()` 的底层逻辑
   - 文件：`ruoyi-framework/src/main/java/com/ruoyi/framework/web/service/SysPermissionService.java`

5. **查看 TableDataInfo 和 PageDomain 的实现**，理解分页参数如何从前端传递到后端
   - 文件：`ruoyi-common/src/main/java/com/ruoyi/common/core/page/TableDataInfo.java`
   - 文件：`ruoyi-common/src/main/java/com/ruoyi/common/core/page/PageDomain.java`

---

## 九、细节留神

1. **getInfo 接口支持无参和有参两种调用方式**：`@GetMapping(value = { "/", "/{userId}" })`，当 userId 为 null 时返回新增表单所需的全部选项数据（角色列表、岗位列表），有值时返回用户详情
2. **导出接口使用 POST 方法而非 GET**：因为导出可能需要传递复杂的查询条件，POST 的 request body 更适合
3. **删除接口接收数组参数**：`@PathVariable Long[] userIds`，支持批量删除，用逗号分隔 ID
4. **密码加密在 Controller 层完成**：`SecurityUtils.encryptPassword(user.getPassword())` 在调用 Service 前就完成了加密，Service 层存储的是已加密的密码
5. **LogAspect 使用 ThreadLocal 记录耗时**：`TIME_THREADLOCAL` 在 doBefore 中设置，在 doAfterReturning/doAfterThrowing 中读取和清理，确保线程安全

---

## 十、提问方向

1. **BaseController 的 `startPage()` 方法调用了 `PageUtils.startPage()`，这个方法内部做了什么？为什么它必须在 Service 调用之前执行？如果忘记调用 startPage() 会发生什么？**

2. **RuoYi-Vue 的 `@PreAuthorize("@ss.hasPermi('system:user:list')")` 中，`@ss` 是什么？它是如何注册到 Spring 容器中的？`hasPermi` 方法的具体实现逻辑是什么？**

3. **LogAspect 的 `@AfterThrowing` 能捕获所有异常吗？如果 Controller 方法内部 try-catch 了异常，LogAspect 还能记录到异常日志吗？这种设计有什么潜在问题？**

4. **`toAjax(int rows)` 方法通过判断 `rows > 0` 来决定返回成功还是失败，但如果 update 操作实际没有修改任何数据（rows=0），这是否一定意味着失败？在什么场景下 rows=0 不应该被视为错误？**

5. **SysUserController 的 `getInfo` 方法中，管理员能看到所有角色（包括 admin 角色），非管理员看不到 admin 角色，这种过滤逻辑放在 Controller 层是否合适？如果放在 Service 层会有什么不同？**

6. **导出接口 `export` 方法没有返回值（void），直接将数据写入 HttpServletResponse，这种流式输出的方式与返回 AjaxResult 相比有什么优势和劣势？在什么场景下应该选择哪种方式？**

7. **如果需要给所有 Controller 接口添加一个统一的请求日志（记录请求URL、参数、耗时），你会如何实现？与现有的 LogAspect 有什么区别？如何避免重复记录？**
