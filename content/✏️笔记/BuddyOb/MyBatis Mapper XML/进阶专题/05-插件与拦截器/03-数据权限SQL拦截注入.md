# 05-插件与拦截器 · 数据权限 SQL 拦截注入

> 前置：[[MyBatis Mapper XML/进阶专题/05-插件与拦截器/02-分页插件原理对比]] ｜ RuoYi 真实实现：[[RuoYi-Analysis/06-后端-AOP与拦截器]]

## 数据权限是什么

"部门经理只能看自己部门的单据，总监能看所有子部门"。传统做法是**每个 SQL 手写 `AND dept_id IN (...)`**，重复且易漏。

优雅做法：**在 SQL 层自动注入数据范围片段**——这就是 RuoYi `DataScopeAspect` 的本质（它用 AOP 而非 MyBatis 拦截器，但二者思想一致）。本篇用 MyBatis 拦截器演示同一思路。

## 思路

1. 自定义注解 `@DataScope(deptAlias="d")` 标在 Mapper 方法上。
2. 拦截器读注解 + 当前登录人的部门/角色。
3. 改写 `BoundSql`，在 `WHERE` 后注入 `AND d.dept_id IN (当前人可见部门)`。

## 注解

```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {
    String deptAlias() default "";   // 部门表别名，如 "d"
    String userAlias() default "";
}
```

## 拦截器（核心：改 BoundSql）

MyBatis 的 `BoundSql` 是不可变的，要改写 SQL 得**反射替换它的 `sql` 字段**（或用 MP 的 `TenantLineInnerInterceptor` 继承更优雅，见下）：

```java
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare",
            args = {Connection.class, Integer.class})})
public class DataScopeInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation inv) throws Throwable {
        StatementHandler sh = (StatementHandler) inv.getTarget();
        MetaObject mo = SystemMetaObject.forObject(sh);
        MappedStatement ms = (MappedStatement) mo.getValue("delegate.mappedStatement");

        DataScope anno = getAnnotation(ms);     // 从 ms.getId() 解析方法上的注解
        if (anno == null) return inv.proceed(); // 没标注解 → 不处理

        BoundSql boundSql = sh.getBoundSql();
        String sql = boundSql.getSql();

        // 当前登录人可见部门（从 TokenService / SecurityContext 取）
        String deptIds = currentUserDeptScope();   // 如 "10,11,12"

        String scope = " AND " + anno.deptAlias() + ".dept_id IN (" + deptIds + ") ";
        // 注入到第一个 WHERE 后（简化：找不到 WHERE 则拼到末尾）
        String newSql = injectAfterWhere(sql, scope);

        // 反射替换 BoundSql 的 sql 字段
        setFieldValue(boundSql, "sql", newSql);
        return inv.proceed();
    }
    // plugin() / setProperties() 略，同 01
}
```

## 为什么 RuoYi 用 AOP 而不是拦截器

RuoYi 的 `DataScopeAspect` 是**在 Service 层、调用 Mapper 之前**，把部门条件 `set` 进 `BaseEntity.params` 的 `dataScope` 字段，然后在 **Mapper XML 里写 `<if test="params.dataScope != null"> ${params.dataScope} </if>`** 拼接。

- 好处：SQL 改写逻辑在 XML 里**可见、可调试**，不像拦截器那样"黑盒"。
- 代价：每个需要数据权限的 XML 都要写那段 `<if>`（RuoYi 用公共 SQL 片段复用）。

> 两种方式本质都是"按当前人动态拼 SQL 片段"。拦截器更自动，AOP+XML 更可控可见。**本仓库选了后者**。

## 更现代的做法：MP `TenantLineInnerInterceptor`

多租户/数据权限若用 MyBatis-Plus，直接继承它：

```java
public class DeptScopeLine implements TenantLineInnerInterceptor {
    @Override public Expression getTenantId() {
        return new LongValue(currentDeptScope()); // 返回可见部门集合需自定义
    }
    @Override public String getTenantIdColumn() { return "dept_id"; }
    @Override public boolean doTableFilter(String tableName) {
        return !needScope(tableName);   // 白名单表才注入
    }
}
```
- 自动在**所有匹配表**的 SQL 注入 `dept_id` 条件。
- 配套 `@InterceptorIgnore` 可跳过特定方法。

## 安全红线

- 注入的 `deptIds` **必须是后端算好的可信集合**，绝不可来自前端参数（否则越权）。
- 用 `#{}` 还是 `${}`？这里只能 `${}`（因为要拼 SQL 结构），所以 `deptIds` 必须是**数字白名单**，不能含任何用户输入。

## 结论

- 数据权限 = "按当前登录人，自动给 SQL 加数据范围片段"。
- 实现可选：MyBatis 拦截器（黑盒自动） / RuoYi 式 AOP+XML（可见可控） / MP TenantLine（生态融合）。
- 注入值必须后端可信，避免越权。

下一步：[[MyBatis Mapper XML/进阶专题/05-插件与拦截器/04-脱敏加密与慢SQL拦截器]]
