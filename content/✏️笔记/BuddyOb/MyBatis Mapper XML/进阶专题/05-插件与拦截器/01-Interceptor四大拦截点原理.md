# 05-插件与拦截器 · `Interceptor` 四大拦截点原理

> 前置：[[MyBatis Mapper XML/进阶专题/05-插件与拦截器/00-索引]]

## 四个可拦截的组件

| 拦截点（type） | 主要方法 | 能干什么 |
|------|------|------|
| `Executor` | `update` / `query` | 分页、缓存、事务日志、全表防护 |
| `ParameterHandler` | `setParameters` | 参数加密/脱敏、类型转换 |
| `StatementHandler` | `prepare` / `parameterize` / `batch` | **改写 SQL**（数据权限、多租户）、SQL 日志 |
| `ResultSetHandler` | `handleResultSets` | 结果脱敏、结果包装 |

> 改写 SQL 主要在 `StatementHandler.prepare`（拿到 `BoundSql` 改 SQL 串）和 `Executor.query`（改 `MappedStatement`）。

## 一个最小拦截器骨架

```java
@Intercepts({
  @Signature(
    type = StatementHandler.class,
    method = "prepare",
    args = {Connection.class, Integer.class}   // 注意 MyBatis 3.5+ prepare 签名
  )
})
public class MyPlugin implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler sh = (StatementHandler) invocation.getTarget();
        BoundSql boundSql = sh.getBoundSql();
        String sql = boundSql.getSql();
        // 这里可以改写 sql、加注释、记日志...
        System.out.println("原始 SQL: " + sql);
        return invocation.proceed();   // 放行到下一环/真实方法
    }

    @Override
    public Object plugin(Object target) {
        // 只有匹配 @Signature 的 target 才被代理
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties props) {
        // 读取 <plugin interceptor="..."><property .../></plugin> 的配置
    }
}
```

## `@Signature` 的 `args` 必须精确匹配方法签名

这是最容易错的点。`StatementHandler.prepare` 在 3.5.x 的签名是 `prepare(Connection, Integer)`（第二个参数是 `Integer` timeout），**不是旧版的 `prepare(Connection)`**。写错 `args` → 拦截器不生效且无报错。

## `Plugin.wrap` 原理

`Plugin` 实现 `InvocationHandler`，用 JDK 动态代理：

```
Plugin.wrap(target, interceptor)
  → Proxy.newProxyInstance(..., new Plugin(target, interceptor, signatureMap))
```

- 代理只实现 `@Signature` 里声明的接口方法。
- 调用被拦截方法时 → 进入 `Plugin.invoke` → 匹配到 `intercept` → 调你的逻辑 → `invocation.proceed()` 调真实方法。
- 没在 `@Signature` 里的方法 → 直接调真实方法，不进拦截器。

## 多个拦截器的顺序

`Configuration` 里按 **`plugins` 声明顺序**形成代理链。先声明的在外层，后声明的在内层：

```
调用 → 插件A代理 → 插件B代理 → 真实对象
```
顺序影响 SQL 改写结果（如分页插件应在数据权限之后/之前要设计好）。

## 注册（Spring Boot）

```java
@Bean
public ConfigurationCustomizer myPluginCustomizer(MyPlugin p) {
    return config -> config.addInterceptor(p);
}
// 或通过 MybatisSqlSessionFactoryBean.setPlugins(...)
```
本仓库 RuoYi 的 `MyBatisConfig` 通过 `sqlSessionFactory` 设置拦截器（见 [[RuoYi-Analysis/03-后端-配置层]]）。

## 结论

- 拦 SQL 改写在 `StatementHandler.prepare` 拿 `BoundSql`。
- `@Signature.args` 要**精确匹配**真实方法签名（版本差异坑）。
- `Plugin.wrap` = JDK 动态代理；多个插件按声明顺序套娃。

下一步：[[MyBatis Mapper XML/进阶专题/05-插件与拦截器/02-分页插件原理对比]]
