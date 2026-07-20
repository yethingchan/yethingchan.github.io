# 06 · `<foreach>` 的 `collection` 取值规则（重点）

> 上接：[[00-总览与目录]] ｜ 前置：[[05-动态SQL]] ｜ 与 `parameterType` 的关系：[[02-parameterType与resultType]]

`<foreach>` 里 `collection` 的值**不是随便写的**，取决于 Mapper 接口方法怎么定义参数。这是最容易写错的地方。

## 核心结论

> **`collection` 的取值由「参数是不是数组/集合 + 有没有 `@Param` 命名」决定，与 `parameterType` 写没写、写什么都无关。**

所以即便 XML 里**省略了 `parameterType`**，`collection="array"` 依然有效——MyBatis 在运行时看到实际传进来的是数组，就用默认名 `"array"` 去取。

## 取值对照表

| Mapper 接口方法定义 | `collection` 应写 |
|----------------------|-------------------|
| `deleteByIds(Long[] ids)`（单个数组，无 `@Param`） | `"array"` |
| `deleteByIds(List<Long> ids)`（单个 List，无 `@Param`） | `"list"` |
| `deleteByIds(@Param("ids") Long[] ids)`（有 `@Param`） | `"ids"`（即注解里的名字） |
| `save(@Param("dept") Dept d, @Param("users") List<User> us)` | 遍历用户写 `"users"` |

## 示例

### 情形 A：单个数组，无 @Param → `collection="array"`

```java
// 接口
int deleteByIds(Long[] ids);
```
```xml
<delete id="deleteByIds">
    delete from sys_user where user_id in
    <foreach collection="array" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</delete>
```

### 情形 B：有 @Param → `collection="ids"`（注解名）

```java
// 接口
int deleteByIds(@Param("ids") Long[] ids);
```
```xml
<delete id="deleteByIds">
    delete from sys_user where user_id in
    <foreach collection="ids" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</delete>
```

### 情形 C：单个 List → `collection="list"`

```java
int insertBatch(List<SysUser> list);
```
```xml
<insert id="insertBatch">
    insert into sys_user(user_name) values
    <foreach collection="list" item="u" separator=",">
        (#{u.userName})
    </foreach>
</insert>
```

## 常见误区（务必分清）

1. **误区**：「我写了 `parameterType="Long[]"` 就能用 `collection` 了。」
   **事实**：MyBatis 默认别名里**没有 `Long[]`**（只有 `long[]` 这种基本类型数组）。写成 `parameterType="Long[]"` 反而会报 `Could not resolve type alias 'Long[]'`。数组参数直接**省略 `parameterType`** 即可。
2. **误区**：「`collection` 的值要看 `parameterType`。」
   **事实**：`collection` 看的是「参数形态 + `@Param` 命名」，见上方对照表。
3. **误区**：「去掉 `parameterType` 后 `collection="array"` 就失效了。」
   **事实**：不会。运行时 MyBatis 仍知道实际传入的是数组，默认名就是 `"array"`。

## 速记

- 数组、没命名 → `array`
- List、没命名 → `list`
- 用了 `@Param("x")` → `x`
- 数组别写 `Long[]` 当 `parameterType`（没有这个别名），直接省略
