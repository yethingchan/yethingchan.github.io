---
title: QueryWrapper 详解（★重点）
---

# 05 QueryWrapper 详解（★ 本库重点）

> 上接：[[MyBatisPlus/04-IServer与ServiceImpl]]
> **Wrapper（条件构造器）是 MP 的灵魂**。本章把所有查询条件方法一次讲透，并附"速查表"。更新看 [[MyBatisPlus/06-UpdateWrapper详解]]，类型安全列看 [[MyBatisPlus/07-LambdaWrapper与列安全]]。

## 5.1 三种拿到 QueryWrapper 的方式

```java
// ① new
QueryWrapper<User> w = new QueryWrapper<>();
w.eq("status","0");

// ② Wrappers 工具（推荐，省 new）
QueryWrapper<User> w = Wrappers.query();          // 泛型可从上下文推断
QueryWrapper<User> w2 = Wrappers.<User>query();   // 显式泛型

// ③ 链式（IService 提供，见 04 章 4.4）
List<User> list = userService.lambdaQuery().eq(User::getStatus,"0").list();
```

## 5.2 比较条件（最常用）

| 方法 | SQL 效果 | 示例 |
|------|-----------|------|
| `eq("col", v)` | `col = v` | `eq("status","0")` |
| `ne("col", v)` | `col <> v` | `ne("status","1")` |
| `gt("col", v)` | `col > v` | `gt("age", 18)` |
| `ge("col", v)` | `col >= v` | `ge("age", 18)` |
| `lt("col", v)` | `col < v` | `lt("age", 60)` |
| `le("col", v)` | `col <= v` | `le("age", 60)` |
| `between("col", a, b)` | `col BETWEEN a AND b` | `between("age",18,60)` |
| `notBetween(...)` | `col NOT BETWEEN ...` | — |

```java
QueryWrapper<User> w = Wrappers.query();
w.eq("status", "0")          // status = '0'
 .gt("age", 18)              // AND age > 18
 .le("age", 60);             // AND age <= 60
List<User> list = userMapper.selectList(w);
```

## 5.3 模糊 / 空值

| 方法 | SQL | 说明 |
|------|-----|------|
| `like("col", "张")` | `col LIKE '%张%'` | 两端模糊 |
| `likeLeft("col", "三")` | `col LIKE '%三'` | 右模糊（%在左） |
| `likeRight("col", "张")` | `col LIKE '张%'` | 左模糊（%在右） |
| `notLike(...)` | `col NOT LIKE ...` | — |
| `isNull("col")` | `col IS NULL` | — |
| `isNotNull("col")` | `col IS NOT NULL` | — |

```java
w.like("user_name", "张")        // user_name LIKE '%张%'
 .isNotNull("email");            // AND email IS NOT NULL
```

## 5.4 IN / NOT IN

```java
w.in("dept_id", Arrays.asList(1L,2L,3L));   // dept_id IN (1,2,3)
w.notIn("status", "1","2");                    // status NOT IN ('1','2')
w.inSql("dept_id", "SELECT id FROM sys_dept WHERE status='0'");  // dept_id IN (子查询)
w.notInSql("user_id", "SELECT user_id FROM sys_role_exclude");
```
> `inSql` / `notInSql` 第二参是**子查询字符串**，注意若拼用户输入有注入风险（用 [[MyBatisPlus/08-动态条件与自定义SQL]] 的 `apply` 占位更安全）。

## 5.5 排序 / 分组 / 聚合

```java
// 排序
w.orderByAsc("age");                  // ORDER BY age ASC
w.orderByDesc("create_time");         // ORDER BY create_time DESC
w.orderBy(true, false, "age", "id");// orderBy(是否生效, 是否ASC, 列...)

// 分组 + 聚合（配合 select 取聚合列）
QueryWrapper<User> w2 = Wrappers.query();
w2.select("dept_id, COUNT(*) AS cnt, MAX(age) AS maxAge")
  .groupBy("dept_id")
  .having("COUNT(*) > {0}", 5);     // HAVING 用 {0} 占位防注入
List<Map<String,Object>> rows = userMapper.selectMaps(w2);
```
> `selectMaps` 返回 `List<Map>`（聚合结果没对应实体时用它）。`having` **必须用 `{0}` 占位 + 参数**，别直接拼字符串。

## 5.6 逻辑连接：or / and / nested（括号）

```java
// (status='0' OR status='1') AND age > 18
w.and(i -> i.eq("status","0").or().eq("status","1"))
 .gt("age", 18);

// status='0' AND (age < 18 OR age > 60)   ← nested 加括号
w.eq("status","0")
 .nested(i -> i.lt("age",18).or().gt("age",60));

// 全局 OR：默认 AND，.or() 切换为 OR（慎用，易写错）
w.eq("status","0").or().eq("status","1");  // status='0' OR status='1'
```
> **新手最易错**：`eq(a).or().eq(b)` 是 `a OR b`；要 `(a OR b) AND c` 必须 `and(i->i.eq(a).or().eq(b)).gt(c)`。拿不准就把条件拆进 `and()`/`nested()` 加括号，SQL 打印出来核对。

## 5.7 apply（拼函数 / 防注入）

```java
// 查"今天创建"——直接拼函数，用 {0} 占位传参（安全！）
w.apply("DATE(create_time) = {0}", LocalDate.now());
// 生成：DATE(create_time) = ?  参数自动绑定

// 区间日期
w.apply("create_time BETWEEN {0} AND {1}", start, end);
```
> **永远用 `{0},{1}...` 占位 + 参数**，不要用字符串拼接用户输入进 `apply`——那是 SQL 注入口。

## 5.8 last（强行追加 SQL 片段）

```java
w.last("LIMIT 1");        // 末尾追加，无视分页（只取一条时用）
w.last("FOR UPDATE");     // 加行锁（悲观锁场景）
```
> `last` 是直接字符串拼接，**不防注入**，只放你自己的常量片段，绝不接用户输入。

## 5.9 select（指定查询列 / 排除列）

```java
QueryWrapper<User> w = Wrappers.query();
w.select("user_id", "user_name", "age");   // 只查这三列（其余为 null）
// 排除：select(User.class, i -> !i.getColumn().equals("password"))  // 不查密码列
List<User> list = userMapper.selectList(w);
```

## 5.10 速查表（背下来）

```
比较：  eq  ne  gt  ge  lt  le  between  notBetween
模糊：  like  likeLeft  likeRight  notLike
空值：  isNull  isNotNull
集合：  in  notIn  inSql  notInSql
排序：  orderByAsc  orderByDesc  orderBy(cond,asc,cols)
分组：  groupBy  having
逻辑：  or()  and(Consumer)  nested(Consumer)
拼接：  apply("{0}",param)  last("SQL")  exists  notExists  select
```
> 全部方法都带 **`boolean condition` 重载**（如 `eq(boolean, col, val)`），`condition=false` 时该条件**整条跳过**——这就是 [[MyBatisPlus/08-动态条件与自定义SQL]] 的"动态条件"基础。

## 验证清单

- [ ] 写出 `(status='0' OR status='1') AND age BETWEEN 18 AND 60` 的 Wrapper（用 `and(...)` + `between`）。
- [ ] `like` / `likeLeft` / `likeRight` 三者的 `%` 位置能说清。
- [ ] `having` 和 `apply` 都用 `{0}` 占位传参，不拼字符串。
- [ ] `nested(...)` 能在 SQL 里正确加出括号。
- [ ] `select("user_id","user_name")` 后查出来的 `age` 为 null（列没查）。

> 下一步：[[MyBatisPlus/06-UpdateWrapper详解]] 看"更新"怎么用 Wrapper 不带实体。
