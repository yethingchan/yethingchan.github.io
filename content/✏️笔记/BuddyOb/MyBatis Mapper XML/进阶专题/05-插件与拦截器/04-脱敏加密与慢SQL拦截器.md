# 05-插件与拦截器 · 脱敏加密与慢 SQL 拦截器

> 前置：[[MyBatis Mapper XML/进阶专题/05-插件与拦截器/01-Interceptor四大拦截点原理]]

## 一、结果脱敏（在 `ResultSetHandler` / `TypeHandler`）

手机号、身份证落库/返回前脱敏。两种层面：

### 方案 A：`TypeHandler` 字段级脱敏（推荐，最干净）

```java
public class MaskPhoneTypeHandler extends BaseTypeHandler<String> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String phone, JdbcType t) throws SQLException {
        ps.setString(i, phone);                 // 入库存原文
    }
    @Override
    public String getNullableResult(ResultSet rs, String col) throws SQLException {
        String v = rs.getString(col);
        return v == null ? null : v.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");  // 138****1234
    }
    // 另两个 getNullableResult(ResultSet,int)/(CallableStatement,...) 同逻辑
}
```
XML 里指定：
```xml
<result column="phone" property="phone" typeHandler="com.x.MaskPhoneTypeHandler"/>
```
- **入库用原文，出库脱敏**，满足"查得到但看不到全号"。
- 比在拦截器里改 ResultSet 更精准（只作用于特定字段）。

### 方案 B：拦截器批量脱敏（适合统一规则）

在 `ResultSetHandler.handleResultSets` 后遍历结果对象，反射把带 `@Sensitive` 注解的字段脱敏。灵活但性能略差。

## 二、参数加密（`ParameterHandler`）

落库前对敏感字段 AES 加密：

```java
@Intercepts({@Signature(type = ParameterHandler.class, method = "setParameters",
            args = {PreparedStatement.class})})
public class EncryptInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation inv) throws Throwable {
        ParameterHandler ph = (ParameterHandler) inv.getTarget();
        // 读 MappedStatement 判断是不是敏感表，再对参数 AES 加密
        // 注意：直接拿 parameterObject 反射改字段值即可
        return inv.proceed();
    }
}
```
- 配合 `TypeHandler` 更优雅（加密放 `setNonNullParameter`，解密放 `getNullableResult`）。
- 推荐用 **`TypeHandler` 做加解密**，拦截器留给"统一横切"场景。

## 三、慢 SQL 日志拦截器（`StatementHandler`）

```java
@Intercepts({@Signature(type = StatementHandler.class, method = "query",
            args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})})
@Signature(type = StatementHandler.class, method = "update", args = {MappedStatement.class, Object.class})
public class SlowSqlInterceptor implements Interceptor {
    private static final long THRESHOLD = 1000; // ms
    @Override
    public Object intercept(Invocation inv) throws Throwable {
        long start = System.nanoTime();
        try { return inv.proceed(); }
        finally {
            long cost = (System.nanoTime() - start) / 1_000_000;
            MappedStatement ms = (MappedStatement) inv.getArgs()[0];
            if (cost > THRESHOLD) {
                log.warn("慢SQL {}ms | {} | {}", cost, ms.getId(), getSql(inv));
            }
        }
    }
}
```
- 在 `finally` 里统计耗时，超过阈值告警。
- 接本仓库日志体系：RuoYi 有 `AsyncManager` + `ScheduledExecutorService` 异步记日志（见 [[RuoYi-Analysis/06-后端-AOP与拦截器]]）。

## 四、防全表误操作 `BlockAttackInnerInterceptor`

MyBatis-Plus 提供，拦截**无 WHERE 的 `DELETE`/`UPDATE`**：

```java
interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
```
- `UPDATE/DELETE` 没有 `WHERE` 条件 → 直接抛异常，防止 `DELETE FROM sys_user` 删全表。
- 本仓库若在 MP 体系下应开启（基础教程 [[MyBatisPlus/03-BaseMapper的CRUD]] 已提及）。

## 五、SQL 注入关键词拦截（安全兜底）

在 `StatementHandler.prepare` 前，对**最终 SQL 串**做黑名单扫描：

```java
private static final String[] BAD = {"' OR ","-- ","/*","UNION SELECT","xp_cmdshell"};
private void checkSql(String sql) {
    for (String b : BAD) if (sql.toUpperCase().contains(b))
        throw new RuntimeException("疑似 SQL 注入: " + b);
}
```
> 这只是**兜底**，真正防注入靠 `#{}` 占位（见 [[MyBatis Mapper XML/进阶专题/06-企业实战与疑难排查/04-诡异Bug排查与Locations坑根源]]）。

## 结论

- 字段级脱敏/加密 → 首选 **`TypeHandler`**（入库原文出库脱敏 / 入库加密出库解密）。
- 慢 SQL / 防全表 / 注入扫描 → `Interceptor` 横切。
- 拦截器链顺序要设计好（如分页在加密之后，否则密文分页）。

下一步回到：[[MyBatis Mapper XML/进阶专题/05-插件与拦截器/00-索引]] ｜ 进入 [[MyBatis Mapper XML/进阶专题/06-企业实战与疑难排查/00-索引]]
