---
title: UpdateWrapper 详解
---

# 06 UpdateWrapper 详解

> 上接：[[MyBatisPlus/05-QueryWrapper详解]]
> `UpdateWrapper` 用于**"按条件批量改，又不传完整实体"**的场景。核心是 `.set()` 指定要改的列。

## 6.1 基本：set + eq

```java
UpdateWrapper<User> uw = Wrappers.update();
uw.set("status", "1")          // SET status = '1'
  .set("remark", "批量停用")
  .eq("status", "0");        // WHERE status = '0'
userMapper.update(null, uw);    // 第一参传 null（用 wrapper 提供值）
// SQL: UPDATE sys_user SET status='1', remark='批量停用' WHERE status='0'
```
> **第一参必须传 `null`**（或空实体），因为值都放在 `UpdateWrapper` 的 `set` 里了。传实体则会把实体非 null 字段也加进 SET。

## 6.2 setSql（手写片段）

想写函数 / 自增 / 复杂表达式时用 `setSql`（注意防注入，别拼用户输入）：
```java
UpdateWrapper<User> uw = Wrappers.update();
uw.setSql("login_num = login_num + 1")   // 自增1（不依赖 Java 值）
  .eq("user_id", 1L);
userMapper.update(null, uw);
// UPDATE sys_user SET login_num = login_num + 1 WHERE user_id = 1
```

## 6.3 set 与实体混合（部分来自实体）

```java
User partial = new User(); partial.setRemark("超龄停用");  // 实体只给 remark
UpdateWrapper<User> uw = Wrappers.update();
uw.set("status", "1")               // 额外用 set 加 status
  .gt("age", 60);                 // WHERE age > 60
userMapper.update(partial, uw);
// SET remark='超龄停用', status='1' WHERE age > 60
```
> 实体提供的非 null 字段 + `set` 指定的字段，**合并**进 SET。

## 6.4 和 updateById 怎么选

| 场景 | 用 |
|------|----|
| 知道主键 ID，改若干字段 | `updateById(entity)`（最清晰） |
| 不知道 ID，按条件批量改 | `update(null, UpdateWrapper)` |
| 改的值要用函数（自增/NOW()） | `UpdateWrapper.setSql(...)` |

## 6.5 ⚠️ 别漏 WHERE（全表更新灾难）

```java
UpdateWrapper<User> uw = Wrappers.update();
uw.set("status","1");
userMapper.update(null, uw);   // ❌ 没有 eq/where → 全表 status='1'！
```
> 配合 [[MyBatisPlus/03-BaseMapper的CRUD]] 的 `BlockAttackInnerInterceptor` 兜底，但**写完 Wrapper 先打印 SQL 核对 WHERE** 才是好习惯。

## 验证清单

- [ ] `update(null, UpdateWrapper.set("status","1").eq("age",20))` 只改 age=20 的行。
- [ ] `setSql("login_num = login_num + 1")` 自增生效，库里值 +1 而非被 Java 覆盖。
- [ ] 漏写 WHERE 时，`BlockAttackInnerInterceptor` 拦截抛异常（而不是全表更新）。

> 下一步：[[MyBatisPlus/07-LambdaWrapper与列安全]] 用方法引用替代字符串列名，从根上防"列名写错"。
