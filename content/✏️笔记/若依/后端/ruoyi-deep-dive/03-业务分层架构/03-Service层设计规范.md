# 03 - Service 层设计规范

> 本文档深入剖析 RuoYi-Vue 项目 Service 层的设计规范，包括接口与实现分离、事务管理、数据权限、典型 Service 代码分析。

---

## 一、Service 层在架构中的定位

Service 层是 RuoYi-Vue 三层架构的核心，承载所有业务逻辑。它上承 Controller 层的调用，下接 Mapper 层的数据访问，是整个应用的"大脑"。

```
                    Controller 层
                        │ 调用接口
                        ▼
              ┌─────────────────────┐
              │  ISysUserService     │  ← 接口定义（契约）
              └─────────┬───────────┘
                        │ 实现
                        ▼
              ┌─────────────────────┐
              │ SysUserServiceImpl  │  ← 实现类（业务逻辑）
              │   - @Service        │
              │   - @Transactional  │
              │   - @DataScope      │
              │   - 业务校验        │
              │   - 多Mapper协调    │
              └─────────┬───────────┘
                        │ 调用
                        ▼
              ┌─────────────────────┐
              │ SysUserMapper       │  ← 数据访问
              │ SysUserRoleMapper   │
              │ SysUserPostMapper   │
              └─────────────────────┘
```

---

## 二、接口与实现分离设计

### 2.1 设计模式

RuoYi-Vue 的 Service 层严格遵循**接口-实现分离**模式：

| 组件 | 命名规范 | 示例 | 位置 |
|------|----------|------|------|
| 接口 | `I` + 业务名 + `Service` | `ISysUserService` | `service/` |
| 实现 | 业务名 + `ServiceImpl` | `SysUserServiceImpl` | `service/impl/` |

### 2.2 接口定义示例

```java
// 文件：ruoyi-system/src/main/java/com/ruoyi/system/service/ISysUserService.java

public interface ISysUserService
{
    public List<SysUser> selectUserList(SysUser user);
    public SysUser selectUserByUserName(String userName);
    public SysUser selectUserById(Long userId);
    public boolean checkUserNameUnique(SysUser user);
    public boolean checkPhoneUnique(SysUser user);
    public boolean checkEmailUnique(SysUser user);
    public void checkUserAllowed(SysUser user);
    public void checkUserDataScope(Long userId);
    public int insertUser(SysUser user);
    public int updateUser(SysUser user);
    public int deleteUserByIds(Long[] userIds);
    public int resetPwd(SysUser user);
    public String importUser(List<SysUser> userList, Boolean isUpdateSupport, String operName);
    // ... 更多方法
}
```

### 2.3 实现类示例

```java
// 文件：ruoyi-system/src/main/java/com/ruoyi/system/service/impl/SysUserServiceImpl.java

@Service
public class SysUserServiceImpl implements ISysUserService
{
    @Autowired private SysUserMapper userMapper;
    @Autowired private SysRoleMapper roleMapper;
    @Autowired private SysPostMapper postMapper;
    @Autowired private SysUserRoleMapper userRoleMapper;
    @Autowired private SysUserPostMapper userPostMapper;
    @Autowired private ISysConfigService configService;
    @Autowired private ISysDeptService deptService;
    @Autowired protected Validator validator;

    // 实现接口方法...
}
```

### 2.4 接口分离的好处

1. **面向接口编程**：Controller 只依赖接口 `ISysUserService`，不依赖具体实现
2. **便于单元测试**：可以轻松 Mock 接口进行测试
3. **便于替换实现**：如果需要更换实现（如引入缓存层），只需新增实现类
4. **契约清晰**：接口定义了 Service 提供的所有能力，一目了然

---

## 三、事务管理

### 3.1 @Transactional 使用规范

RuoYi-Vue 在 Service 层使用 Spring 的 `@Transactional` 注解管理事务，遵循以下规范：

**需要添加事务的场景：**
- 涉及多表操作的方法（如新增用户时同时插入用户-角色关联、用户-岗位关联）
- 涉及先删后增的方法（如修改用户时先删除旧关联再插入新关联）
- 批量操作方法

**不需要事务的场景：**
- 单表查询操作
- 单表更新操作（如修改状态、重置密码）

### 3.2 典型事务方法分析

```java
// 文件：SysUserServiceImpl.java

// 新增用户 - 需要事务：涉及3张表
@Override
@Transactional
public int insertUser(SysUser user) {
    int rows = userMapper.insertUser(user);      // 1. 插入用户主表
    insertUserPost(user);                        // 2. 插入用户-岗位关联表
    insertUserRole(user);                        // 3. 插入用户-角色关联表
    return rows;
}

// 修改用户 - 需要事务：涉及3张表，先删后增
@Override
@Transactional
public int updateUser(SysUser user) {
    Long userId = user.getUserId();
    userRoleMapper.deleteUserRoleByUserId(userId);   // 1. 删除旧角色关联
    insertUserRole(user);                              // 2. 插入新角色关联
    userPostMapper.deleteUserPostByUserId(userId);    // 3. 删除旧岗位关联
    insertUserPost(user);                              // 4. 插入新岗位关联
    return userMapper.updateUser(user);                // 5. 更新用户主表
}

// 批量删除 - 需要事务：涉及3张表
@Override
@Transactional
public int deleteUserByIds(Long[] userIds) {
    for (Long userId : userIds) {
        checkUserAllowed(new SysUser(userId));    // 校验是否允许操作
        checkUserDataScope(userId);                // 校验数据权限
    }
    userRoleMapper.deleteUserRole(userIds);       // 删除角色关联
    userPostMapper.deleteUserPost(userIds);       // 删除岗位关联
    return userMapper.deleteUserByIds(userIds);    // 逻辑删除用户
}

// 查询用户列表 - 不需要事务
@Override
@DataScope(deptAlias = "d", userAlias = "u")
public List<SysUser> selectUserList(SysUser user) {
    return userMapper.selectUserList(user);
}

// 重置密码 - 不需要事务（单表操作）
@Override
public int resetPwd(SysUser user) {
    return userMapper.resetUserPwd(user.getUserId(), user.getPassword());
}
```

### 3.3 事务传播机制

RuoYi-Vue 默认使用 `@Transactional` 的默认传播行为 `REQUIRED`：
- 如果当前有事务，则加入当前事务
- 如果当前没有事务，则新建一个事务

这意味着如果 Controller 调用 ServiceA，ServiceA 内部又调用 ServiceB，它们会在同一个事务中执行。

---

## 四、数据权限设计

### 4.1 @DataScope 注解

```java
// 文件：ruoyi-common/src/main/java/com/ruoyi/common/annotation/DataScope.java

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {
    /** 部门表的别名 */
    public String deptAlias() default "";
    /** 用户表的别名 */
    public String userAlias() default "";
    /** 部门ID字段名 */
    public String deptField() default "dept_id";
    /** 用户ID字段名 */
    public String userField() default "user_id";
    /** 权限字符 */
    public String permission() default "";
}
```

### 4.2 DataScopeAspect 切面实现

```java
// 文件：ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/DataScopeAspect.java

@Aspect
@Component
public class DataScopeAspect {

    @Before("@annotation(controllerDataScope)")
    public void doBefore(JoinPoint point, DataScope controllerDataScope) throws Throwable {
        clearDataScope(point);           // 先清空，防止SQL注入
        handleDataScope(point, controllerDataScope);
    }

    protected void handleDataScope(final JoinPoint joinPoint, DataScope controllerDataScope) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (StringUtils.isNotNull(loginUser)) {
            SysUser currentUser = loginUser.getUser();
            // 超级管理员不过滤数据
            if (StringUtils.isNotNull(currentUser) && !currentUser.isAdmin()) {
                dataScopeFilter(joinPoint, currentUser, ...);
            }
        }
    }
}
```

### 4.3 数据权限 SQL 拼接逻辑

DataScopeAspect 根据用户角色的 `dataScope` 字段值，动态拼接不同的 SQL 条件：

| dataScope 值 | 含义 | 拼接的 SQL |
|-------------|------|-----------|
| `1` (DATA_SCOPE_ALL) | 全部数据权限 | 不拼接任何条件 |
| `2` (DATA_SCOPE_CUSTOM) | 自定义数据权限 | `OR d.dept_id IN (SELECT dept_id FROM sys_role_dept WHERE role_id = ?)` |
| `3` (DATA_SCOPE_DEPT) | 本部门数据权限 | `OR d.dept_id = 当前用户部门ID` |
| `4` (DATA_SCOPE_DEPT_AND_CHILD) | 本部门及以下数据权限 | `OR d.dept_id IN (SELECT dept_id FROM sys_dept WHERE dept_id = ? or find_in_set(?, ancestors))` |
| `5` (DATA_SCOPE_SELF) | 仅本人数据权限 | `OR u.user_id = 当前用户ID` |

### 4.4 数据权限注入流程

```
1. Controller 调用 userService.selectUserList(user)
2. Spring AOP 拦截到 @DataScope 注解
3. DataScopeAspect.doBefore() 执行
4. clearDataScope() 清空 params.dataScope（防注入）
5. handleDataScope() 获取当前用户的角色列表
6. 遍历角色，根据 dataScope 拼接 SQL
7. 将 SQL 片段设置到 user.getParams().put("dataScope", sqlString)
8. Mapper XML 中的 ${params.dataScope} 被替换为拼接的 SQL
9. MyBatis 执行完整的 SQL（包含数据权限条件）
```

### 4.5 数据权限使用示例

```java
// 用户列表查询 - 同时指定部门别名和用户别名
@Override
@DataScope(deptAlias = "d", userAlias = "u")
public List<SysUser> selectUserList(SysUser user) {
    return userMapper.selectUserList(user);
}

// 角色列表查询 - 只指定部门别名
@Override
@DataScope(deptAlias = "d")
public List<SysRole> selectRoleList(SysRole role) {
    return roleMapper.selectRoleList(role);
}
```

---

## 五、业务校验设计

### 5.1 校验类型清单

RuoYi-Vue 的 Service 层包含多种校验逻辑：

| 校验类型 | 方法命名 | 典型实现 | 异常类型 |
|----------|----------|----------|----------|
| 操作权限校验 | `checkXxxAllowed` | 不允许操作超级管理员 | ServiceException |
| 数据权限校验 | `checkXxxDataScope` | 没有权限访问数据 | ServiceException |
| 唯一性校验 | `checkXxxUnique` | 名称/编码已存在 | 返回 boolean |
| 状态校验 | `validate` | 密码错误次数超限 | ServiceException |

### 5.2 操作权限校验

```java
// 文件：SysUserServiceImpl.java

@Override
public void checkUserAllowed(SysUser user) {
    if (StringUtils.isNotNull(user.getUserId()) && user.isAdmin()) {
        throw new ServiceException("不允许操作超级管理员用户");
    }
}
```

### 5.3 数据权限校验（通过 AOP 代理间接校验）

```java
// 文件：SysUserServiceImpl.java

@Override
public void checkUserDataScope(Long userId) {
    if (!SecurityUtils.isAdmin()) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        // 注意：通过 AOP 代理调用，确保 @DataScope 切面生效
        List<SysUser> users = SpringUtils.getAopProxy(this).selectUserList(user);
        if (StringUtils.isEmpty(users)) {
            throw new ServiceException("没有权限访问用户数据！");
        }
    }
}
```

**关键细节：** `SpringUtils.getAopProxy(this)` 获取的是当前对象的 AOP 代理，而不是 this 本身。这是因为 `@DataScope` 注解的切面需要通过代理对象才能生效，直接调用 `this.selectUserList()` 不会触发切面。

### 5.4 唯一性校验

```java
// 文件：SysUserServiceImpl.java

@Override
public boolean checkUserNameUnique(SysUser user) {
    Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
    SysUser info = userMapper.checkUserNameUnique(user.getUserName());
    if (StringUtils.isNotNull(info) && info.getUserId().longValue() != userId.longValue()) {
        return UserConstants.NOT_UNIQUE;  // false
    }
    return UserConstants.UNIQUE;  // true
}
```

**设计要点：** 新增时 userId 为 null，使用 -1L 作为默认值；修改时使用当前用户的 userId，排除自身。

---

## 六、典型 Service 完整代码分析

### 6.1 SysUserServiceImpl 依赖关系

```
SysUserServiceImpl
  ├── SysUserMapper          (用户主表 CRUD)
  ├── SysRoleMapper          (查询用户角色)
  ├── SysPostMapper          (查询用户岗位)
  ├── SysUserRoleMapper      (用户-角色关联)
  ├── SysUserPostMapper      (用户-岗位关联)
  ├── ISysConfigService      (读取系统配置)
  ├── ISysDeptService        (部门数据权限校验)
  └── Validator              (JSR-303 校验器)
```

### 6.2 导入用户方法分析

```java
// 文件：SysUserServiceImpl.java

@Override
public String importUser(List<SysUser> userList, Boolean isUpdateSupport, String operName) {
    if (StringUtils.isNull(userList) || userList.size() == 0) {
        throw new ServiceException("导入用户数据不能为空！");
    }
    int successNum = 0;
    int failureNum = 0;
    StringBuilder successMsg = new StringBuilder();
    StringBuilder failureMsg = new StringBuilder();

    for (SysUser user : userList) {
        try {
            SysUser u = userMapper.selectUserByUserName(user.getUserName());
            if (StringUtils.isNull(u)) {
                // 新用户：校验 → 加密密码 → 插入
                BeanValidators.validateWithException(validator, user);
                deptService.checkDeptDataScope(user.getDeptId());
                String password = configService.selectConfigByKey("sys.user.initPassword");
                user.setPassword(SecurityUtils.encryptPassword(password));
                user.setCreateBy(operName);
                userMapper.insertUser(user);
                successNum++;
                successMsg.append("<br/>" + successNum + "、账号 " + user.getUserName() + " 导入成功");
            } else if (isUpdateSupport) {
                // 已存在且允许更新：校验 → 更新
                BeanValidators.validateWithException(validator, user);
                checkUserAllowed(u);
                checkUserDataScope(u.getUserId());
                deptService.checkDeptDataScope(user.getDeptId());
                user.setUserId(u.getUserId());
                user.setUpdateBy(operName);
                userMapper.updateUser(user);
                successNum++;
                successMsg.append("<br/>" + successNum + "、账号 " + user.getUserName() + " 更新成功");
            } else {
                // 已存在且不允许更新
                failureNum++;
                failureMsg.append("<br/>" + failureNum + "、账号 " + user.getUserName() + " 已存在");
            }
        } catch (Exception e) {
            failureNum++;
            failureMsg.append("<br/>" + failureNum + "、账号 " + user.getUserName() + " 导入失败：" + e.getMessage());
        }
    }

    if (failureNum > 0) {
        failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
        throw new ServiceException(failureMsg.toString());
    } else {
        successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
    }
    return successMsg.toString();
}
```

**设计要点：**
- 逐条处理，一条失败不影响其他条
- 使用 HTML 格式化消息（`<br/>`），前端可直接渲染
- 有失败则抛异常，全部成功则返回成功消息
- 导入新用户使用系统配置的初始密码

---

## 七、Service 层设计模式总结

### 7.1 模板方法模式

Service 层的增删改查方法遵循固定的模板：

```
查询方法模板：
  @DataScope → mapper.selectXxx()

新增方法模板：
  @Transactional → mapper.insertXxx() → insertXxxXxx()

修改方法模板：
  @Transactional → mapper.deleteXxxXxx() → insertXxxXxx() → mapper.updateXxx()

删除方法模板：
  @Transactional → checkXxxAllowed() → checkXxxDataScope() → mapper.deleteXxxXxx() → mapper.deleteXxxByIds()
```

### 7.2 策略模式（数据权限）

通过 `@DataScope` 注解的参数和角色配置，实现不同级别的数据权限策略，无需修改业务代码。

### 7.3 代理模式（AOP 代理）

`SpringUtils.getAopProxy(this)` 确保 AOP 切面在 Service 内部调用时也能生效。

---

## 八、查看与剖析点

1. **查看 ISysUserService 接口的所有方法定义**，理解 Service 层对外提供的完整能力
   - 文件：`ruoyi-system/src/main/java/com/ruoyi/system/service/ISysUserService.java`

2. **查看 SysRoleServiceImpl 的数据权限校验方法**，对比与 SysUserServiceImpl 的异同
   - 文件：`ruoyi-system/src/main/java/com/ruoyi/system/service/impl/SysRoleServiceImpl.java`

3. **查看 SpringUtils.getAopProxy() 的实现**，理解 AOP 代理的获取方式
   - 文件：`ruoyi-common/src/main/java/com/ruoyi/common/utils/spring/SpringUtils.java`

4. **查看 @DataScope 注解的定义**，理解各参数的含义
   - 文件：`ruoyi-common/src/main/java/com/ruoyi/common/annotation/DataScope.java`

5. **查看 UserConstants 中的常量定义**，理解唯一性校验的返回值约定
   - 文件：`ruoyi-common/src/main/java/com/ruoyi/common/constant/UserConstants.java`

---

## 九、细节留神

1. **importUser 方法没有 @Transactional 注解**：导入方法逐条处理，每条独立提交，一条失败不影响其他条。如果加了 @Transactional，一条失败会导致全部回滚
2. **checkUserDataScope 使用 AOP 代理调用**：`SpringUtils.getAopProxy(this).selectUserList(user)` 而非 `this.selectUserList(user)`，这是因为 @DataScope 是 AOP 切面，直接内部调用不会触发切面
3. **insertUserRole 和 insertUserPost 是 public 方法但不在接口中**：它们是内部辅助方法，仅供 ServiceImpl 内部使用，不对外暴露
4. **Service 实现类注入了 Validator**：`@Autowired protected Validator validator`，用于在导入等场景中手动触发 JSR-303 校验（`BeanValidators.validateWithException`）
5. **SysUserServiceImpl 注入了 ISysDeptService**：用于在新增/修改/导入用户时校验部门的数据权限，体现了 Service 之间的交叉校验

---

## 十、提问方向

1. **SysUserServiceImpl 的 `insertUser` 方法使用了 `@Transactional`，如果 `insertUserPost` 方法抛出异常，`userMapper.insertUser` 的插入会回滚吗？为什么？如果不想回滚应该怎么做？**

2. **`checkUserDataScope` 方法中使用了 `SpringUtils.getAopProxy(this).selectUserList(user)` 来间接调用自身方法，请解释为什么不能直接用 `this.selectUserList(user)`？如果不用 AOP 代理，有什么替代方案可以实现同样的数据权限校验效果？**

3. **`importUser` 方法没有使用 `@Transactional`，这意味着每条用户的导入是独立事务。如果第 50 条导入失败，前 49 条已经提交了。这种设计是否合理？在什么场景下应该使用事务？**

4. **SysUserServiceImpl 注入了 8 个依赖（5个 Mapper + 3个 Service），这是否违反了"单一职责原则"？如果用户管理的业务越来越复杂，应该如何重构来降低耦合度？**

5. **数据权限的 5 种级别（全部/自定义/本部门/本部门及以下/仅本人）是如何决定的？如果一个用户有多个角色，每个角色的数据权限不同，最终如何拼接 SQL？**

6. **RuoYi-Vue 的 Service 层校验逻辑（如 checkUserAllowed、checkUserDataScope）放在 Service 层而非 Controller 层，这样做有什么好处？如果放在 Controller 层会有什么问题？**

7. **`insertUserRole` 和 `insertUserPost` 方法是 public 但不在接口中定义，这种设计是否合理？如果其他 Service 需要调用这些方法，应该如何处理？**
