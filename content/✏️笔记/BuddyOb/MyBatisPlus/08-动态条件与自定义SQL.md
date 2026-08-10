---
title: 08-动态条件与自定义SQL
---

# 08 动态条件与自定义 SQL

> 上接：[[07-LambdaWrapper与列安全]]
> 真实查询都是"用户填了啥才拼啥"。本章讲**动态条件**（condition 参数）和 **Wrapper + 手写 SQL 共存**（复杂联表时）。

## 8.1 condition 参数（动态拼接核心）

每个条件方法都有 `boolean condition` 重载：**`condition=false` 时整条条件跳过**。

```java
// 前端查询对象（可能部分为空）
UserQuery q = ...;   // q.userName / q.status / q.startAge 可能 null

LambdaQueryWrapper<User> lw = Wrappers.lambdaQuery(User.class);
lw.eq(q.getUserName() != null, User::getUserName, q.getUserName())  // 有值才拼
  .eq(q.getStatus() != null, User::getStatus, q.getStatus())      // 空则跳过
  .ge(q.getStartAge() != null, User::getAge, q.getStartAge())
  .le(q.getEndAge()   != null, User::getAge, q.getEndAge());
List<User> list = userMapper.selectList(lw);
```
> 这就是本 Spring 教程里 `selectUserPage` 多条件筛选的写法（见 [[../SpringBoot+Vue3后台搭建/04-权限管理模块/01-用户管理]] 1.3）。**比 XML 里写一堆 `<if test="...">` 清爽太多**。

## 8.2 字符串版同样有 condition

```java
QueryWrapper<User> w = Wrappers.query();
w.eq(Strings.isNotBlank(q.getUserName()), "user_name", q.getUserName())
 .between(q.getStart() != null && q.getEnd() != null, "create_time", q.getStart(), q.getEnd());
```

## 8.3 组合：nested / and / or 也支持 condition

```java
lw.and(q.getVip() != null,
      i -> i.eq(User::getStatus,"0").or().eq(User::getStatus,"1"));
// 仅当 q.vip != null 时，才追加 AND (status='0' OR status='1')
```

## 8.4 Wrapper 与手写 SQL 共存（复杂联表）

MP 的 `customSqlSegment` 让你**手写 FROM/JOIN，WHERE 仍交给 Wrapper**：

### 注解方式
```java
public interface UserMapper extends BaseMapper<User> {
    @Select("SELECT u.*, d.dept_name FROM sys_user u " +
             "LEFT JOIN sys_dept d ON u.dept_id = d.dept_id ${ew.customSqlSegment}")
    List<User> selectUserJoinDept(@Param("ew") Wrapper<User> wrapper);
}
// 调用
LambdaQueryWrapper<User> lw = Wrappers.lambdaQuery(User.class)
    .eq(User::getStatus,"0").like(User::getUserName,"张");
List<User> list = userMapper.selectUserJoinDept(lw);
// 生成 WHERE status='0' AND user_name LIKE '%张%'，JOIN 是你手写的
```
> **必须**用 `${ew.customSqlSegment}`（不是 `#{}`），并让形参名是 `ew` 且加 `@Param("ew")`。MP 把 Wrapper 拼好的 WHERE（含 AND/OR/参数）原样注入。

### XML 方式
```xml
<select id="selectUserJoinDept" resultType="com.example.admin.domain.User">
  SELECT u.*, d.dept_name FROM sys_user u
  LEFT JOIN sys_dept d ON u.dept_id = d.dept_id
  ${ew.customSqlSegment}
</select>
```
> ⚠️ `${ew.customSqlSegment}` 是**唯一**允许 `${}` 的地方（MP 内部已参数化，安全）；其余任何用户输入**禁止**用 `${}` 拼接。

## 8.5 apply 拼函数（仍参数化）

```java
lw.apply(q.getDate() != null, "DATE(create_time) = {0}", q.getDate());
// condition 为假 → 不拼；为真 → DATE(create_time) = ? 参数绑定
```

## 8.6 常见错误

| 错误 | 后果 | 正确 |
|------|------|------|
| 动态条件忘了 condition | 空值拼进 SQL（`LIKE '%%'` 还好，但 `eq(null)` 会变 `= null` 全不中） | 每个条件加 `xxx != null` 判断 |
| 手写 SQL 用 `${}` 拼用户输入 | **SQL 注入** | 用 Wrapper 的 `apply("{0}", param)` |
| `customSqlSegment` 形参名不是 `ew` | Wrapper 不生效 | `@Param("ew") Wrapper<T> wrapper` |

## 验证清单

- [ ] 只填"姓名"时，生成的 SQL **只有** `LIKE '%张%'`，没有 `status = null` 这种废条件。
- [ ] 手写 JOIN + `customSqlSegment`，Wrapper 的 where 正确追加在 JOIN 后。
- [ ] `apply("DATE(create_time)={0}", date)` 的 `date` 是参数绑定（打印 SQL 显示为 `?`），不是字符串拼接。
- [ ] 全仓 grep `${`，除 `customSqlSegment` 外无任何 `${}` 拼用户输入。

> 下一步进 L3 高级特性：[[09-分页插件]]。
