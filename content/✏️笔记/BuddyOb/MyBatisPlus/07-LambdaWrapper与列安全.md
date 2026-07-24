---
title: LambdaWrapper 与列安全
---

# 07 LambdaWrapper 与列安全

> 上接：[[MyBatisPlus/06-UpdateWrapper详解]]
> 前面 `QueryWrapper.eq("user_name", ...)` 的**列名是字符串**——拼错字母编译不报错，运行才炸。Lambda 版用**方法引用**取列名，编译期就帮你对。

## 7.1 四种 Wrapper 关系

| 类 | 列写法 | 用途 |
|----|---------|------|
| `QueryWrapper<T>` | 字符串 `"user_name"` | 查询，列名手写 |
| `UpdateWrapper<T>` | 字符串 + `set` | 更新，列名手写 |
| **`LambdaQueryWrapper<T>`** | 方法引用 `User::getUserName` | 查询，**列名编译期校验** |
| **`LambdaUpdateWrapper<T>`** | 方法引用 + `set` | 更新，列名编译期校验 |

## 7.2 怎么拿到 Lambda 版

```java
// ① new
LambdaQueryWrapper<User> lw = new LambdaQueryWrapper<>();

// ② Wrappers 工具（推荐）
LambdaQueryWrapper<User> lw = Wrappers.lambdaQuery(User.class);
LambdaUpdateWrapper<User> luw = Wrappers.lambdaUpdate(User.class);
// 若类型可推断：Wrappers.lambdaQuery() / lambdaUpdate()

// ③ IService 链式（最常用，见 [[MyBatisPlus/04-IServer与ServiceImpl]] 4.4）
userService.lambdaQuery().eq(User::getStatus,"0").list();
userService.lambdaUpdate().eq(User::getAge,20).set(User::getStatus,"1").update();
```

## 7.3 对比：字符串 vs 方法引用

```java
// ❌ 字符串：user_nme 拼错，编译通过，运行报 "Unknown column"
QueryWrapper<User> w = Wrappers.query();
w.eq("user_nme", "张三");

// ✅ Lambda：写错方法名（getUesrName）编译直接红，且重构字段名会自动跟着改
LambdaQueryWrapper<User> lw = Wrappers.lambdaQuery(User.class);
lw.eq(User::getUserName, "张三")     // 列名由 getUserName 反推，绝不会拼错
  .gt(User::getAge, 18)
  .orderByDesc(User::getCreateTime);
List<User> list = userMapper.selectList(lw);
```

## 7.4 更新也用 Lambda

```java
LambdaUpdateWrapper<User> luw = Wrappers.lambdaUpdate(User.class);
luw.eq(User::getStatus, "0")
   .set(User::getStatus, "1")
   .set(User::getRemark, "批量停用");
userMapper.update(null, luw);

// 或链式
userService.lambdaUpdate()
   .eq(User::getAge, 20)
   .set(User::getStatus, "1")
   .update();
```

## 7.5 什么时候仍用字符串版

| 情况 | 用 |
|------|----|
| 列对应实体字段 | **Lambda**（优先，安全） |
| 聚合 `COUNT(*)`、`DATE(create_time)` 等**非实体列** | 字符串 `QueryWrapper.select("dept_id, COUNT(*) cnt")` |
| 用 `apply("DATE(create_time) = {0}", ...)` 拼函数 | 字符串（Lambda 没这能力） |
| 动态列名（从配置/前端传列名） | 字符串（需自己校验白名单！） |

> 经验法则：**列是实体字段 → 一律 Lambda**；只有聚合/函数/动态列才用字符串，且动态列名必须过白名单防注入。

## 验证清单

- [ ] `LambdaQueryWrapper` 用 `User::getUserName` 写错方法名时**编译报错**（而非运行期）。
- [ ] 把实体字段 `userName` 改名，Lambda 调用处编译自动报错提示同步改（字符串版不会）。
- [ ] 聚合/函数场景正确退回字符串 `QueryWrapper.select("dept_id, COUNT(*) cnt")`。
- [ ] 链式 `userService.lambdaUpdate().eq(...).set(...).update()` 跑通。

> 下一步：[[MyBatisPlus/08-动态条件与自定义SQL]] 讲 condition 参数（动态拼条件）、nested/or/and 组合、以及 Wrapper 和手写 SQL 怎么共存。
