---
title: 多租户与 JSON 类型处理器
---

# 14 多租户隔离与 JSON 类型处理器

> 上接：[[MyBatis-Plus/13-代码生成器AutoGenerator]]
> 两个企业高频进阶点：**多租户**（SaaS 一张表给所有客户用，按 `tenant_id` 隔离）和 **JSON 字段**（MySQL 的 JSON 列直接映射成 Java 对象）。

## 14.1 多租户隔离（TenantLineInnerInterceptor）

每张表加 `tenant_id` 列，MP **自动**给所有 SQL 拼 `AND tenant_id = 当前租户`，你业务代码无感。

### 拦截器配置
```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor i = new MybatisPlusInterceptor();
    i.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
        @Override public Expression getTenantId() {
            // 从登录上下文取当前租户（本教程 SecurityUtils，见 03 章）
            return new LongValue(SecurityUtils.getTenantId());
        }
        @Override public String getTenantIdColumn() { return "tenant_id"; }
        @Override public boolean ignoreTable(String tableName) {
            // 这些表不隔离（字典/参数等公共表）
            return "sys_dict_data".equals(tableName) || "sys_config".equals(tableName);
        }
    }));
    i.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    return i;
}
```

### 效果
```java
userService.list();   // 你啥都没写
// 实际：SELECT * FROM sys_user WHERE tenant_id = 7   ← 自动加！
// 插入也自动带 tenant_id；租户 A 永远看不到租户 B 的数据
```
> **注意**：手写 XML/注解 SQL **不会** 自动加租户条件，得自己写 `AND tenant_id = #{tenantId}`。只有 MP 的 Wrapper/BaseMapper 才自动。

## 14.2 JSON 类型处理器（JacksonTypeHandler）

MySQL 的 `JSON` 列，用 MP 直接映射成 `List`/`Map`/自定义对象，省得自己序列化。

### 实体
```java
@Data
@TableName(value = "sys_user", autoResultMap = true)   // ★ JSON 字段必须 autoResultMap=true
public class SysUser {
    private Long userId;
    // ext 是 MySQL JSON 列，映射成 Map
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> ext;

    // 或映射成对象列表
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> roles;
}
```

### 存取自动序列化
```java
SysUser u = new SysUser();
u.setExt(Map.of("theme", "dark", "lang", "zh"));  // Java 对象
userService.save(u);
// 入库：ext 列存成 JSON 字符串 '{"theme":"dark","lang":"zh"}'

SysUser got = userService.getById(u.getUserId());
got.getExt().get("theme");   // 直接是 Map，自动反序列化
```
> 必须 `@TableName(autoResultMap = true)` + `@TableField(typeHandler = JacksonTypeHandler.class)` 两者齐备，JSON 字段才正确读写。**手写 XML 时也得在 resultMap 里显式配 `typeHandler`**。

## 14.3 选型速览

| 需求 | 方案 |
|------|------|
| SaaS 多客户共用表 | `TenantLineInnerInterceptor` + 公共表 `ignoreTable` |
| 列存 JSON（配置/扩展属性） | `JacksonTypeHandler` + `autoResultMap=true` |
| 列存枚举 | `@EnumValue`（见 [[MyBatis-Plus/02-实体注解与表映射]] 2.5） |

## 验证清单

- [ ] 配了租户拦截器后，`list()` 自动带 `WHERE tenant_id = ?`，租户间数据隔离。
- [ ] 公共表（字典等）在 `ignoreTable` 里被排除，不被加租户条件。
- [ ] `@TableField(typeHandler=JacksonTypeHandler.class)` + `autoResultMap=true` 后，JSON 列读写自动序列化/反序列化。
- [ ] 手写 XML 的 JSON 字段在 `resultMap` 显式配了 `typeHandler`（否则查出来是 null）。

> 下一步（收官）：[[MyBatis-Plus/15-企业级实战与避坑]] 把生产最该注意的坑一次性列清。
