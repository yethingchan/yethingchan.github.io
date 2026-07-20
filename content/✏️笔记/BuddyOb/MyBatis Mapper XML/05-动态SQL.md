# 05 · 动态 SQL

> 上接：[[00-总览与目录]] ｜ 前置：[[01-文件结构与四大标签]] ｜ 批量遍历专章：[[06-foreach的collection规则]]

根据入参动态拼 SQL。所有判断表达式都是 **OGNL**，`test` 里直接写属性名（来自入参对象的 getter）。

## 7.1 `<if>` 条件判断

```xml
<if test="userName != null and userName != ''">
    and user_name = #{userName}
</if>
```
- 字符串非空判断套路：`xxx != null and xxx != ''`。
- 数值非空判断：`xxx != null`（不用判空串）。

## 7.2 `<choose>/<when>/<otherwise>` —— 多选一（类似 switch）

```xml
<choose>
    <when test="type == 1"> and user_type = 1 </when>
    <when test="type == 2"> and user_type = 2 </when>
    <otherwise> and user_type = 0 </otherwise>
</choose>
```

## 7.3 `<where>` —— 智能 WHERE

自动在必要时加 `WHERE`，并**去掉开头多余的 `and` / `or`**。

```xml
<where>
    <if test="userName != null"> and user_name = #{userName} </if>
    <if test="status != null"> and status = #{status} </if>
</where>
```

## 7.4 `<set>` —— 智能 SET（用于 update）

自动加 `SET` 并**去掉结尾多余的逗号**。

```xml
<update id="updateUser">
    update sys_user
    <set>
        <if test="userName != null">user_name = #{userName},</if>
        <if test="status != null">status = #{status},</if>
    </set>
    where user_id = #{userId}
</update>
```

## 7.5 `<trim>` —— 通用裁剪（where/set 的底层）

```xml
<!-- 等价于 <where> -->
<trim prefix="WHERE" prefixOverrides="AND |OR ">
    ...
</trim>

<!-- 等价于 <set> -->
<trim prefix="SET" suffixOverrides=",">
    ...
</trim>
```

| 属性 | 说明 |
|------|------|
| `prefix` | 整体前缀（如 `WHERE` / `SET` / `(`）。 |
| `suffix` | 整体后缀（如 `)`）。 |
| `prefixOverrides` | 去掉内容**开头**匹配的字符（如 `AND `）。 |
| `suffixOverrides` | 去掉内容**结尾**匹配的字符（如 `,`）。 |

## 7.6 `<foreach>` —— 遍历集合/数组

> ⚠️ 重点单独成章，规则易错：[[06-foreach的collection规则]]

```xml
<foreach collection="array" item="id" index="i"
         open="(" separator="," close=")">
    #{id}
</foreach>
```

| 属性 | 说明 |
|------|------|
| `collection` | 要遍历的对象（取值规则见 [[06-foreach的collection规则]]）。 |
| `item` | 当前元素的变量名，循环体内用 `#{item名}` 引用。 |
| `index` | 当前索引/键的变量名（List 为下标，Map 为 key）。 |
| `open` / `close` | 整体前后包裹的字符串（如括号）。 |
| `separator` | 元素之间的分隔符。 |

## 7.7 `<bind>` —— 绑定变量（常用于模糊查询，避免数据库方言差异）

```xml
<bind name="nameLike" value="'%' + userName + '%'"/>
select * from sys_user where user_name like #{nameLike}
```

## 速记

| 想要 | 用 |
|------|-----|
| 有则拼、无则不拼的单个条件 | `<if>` |
| 多选一（互斥分支） | `<choose>/<when>/<otherwise>` |
| 动态 WHERE（自动处理 and） | `<where>` |
| 动态 SET（自动处理逗号） | `<set>` |
| 自定义的拼接/裁剪 | `<trim>` |
| 遍历数组/集合（in 查询、批量） | `<foreach>` |
| 复杂模糊匹配表达式 | `<bind>` |
