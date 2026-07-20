# 01-动态SQL高级 · `trim` 精确控制前缀后缀

> 前置：[[MyBatis Mapper XML/进阶专题/01-动态SQL高级/00-索引]] ｜ 基础：[[MyBatis Mapper XML/05-动态SQL]]

## 为什么需要 `trim`

`where` 和 `set` 其实是 `trim` 的语法糖：
- `<where>` = `<trim prefix="WHERE" prefixOverrides="AND |OR ">`
- `<set>` = `<trim prefix="SET" suffixOverrides=",">`

但当条件不止"去掉首 AND"这么简单时，就要手写 `trim`。

## `trim` 四属性

| 属性 | 作用 |
|------|------|
| `prefix` | 若内部有内容，在整段前加此字符串 |
| `suffix` | 若内部有内容，在整段后加此字符串 |
| `prefixOverrides` | 若整段以这些字符串开头，则**删掉**它们（可写多个，用 `\|` 分隔） |
| `suffixOverrides` | 若整段以这些字符串结尾，则**删掉**它们 |

## 例 1：手写 `where`（等价于 `<where>`）

```xml
<select id="selectUser" resultType="SysUser">
  SELECT * FROM sys_user
  <trim prefix="WHERE" prefixOverrides="AND |OR ">
    <if test="userName != null"> AND user_name = #{userName} </if>
    <if test="status != null"> AND status = #{status} </if>
  </trim>
</select>
```
- 当 `userName` 有值时，内部首字符是 `AND user_name...`，`prefixOverrides="AND "` 把它删掉，再 `prefix="WHERE"` 补上 → `WHERE user_name = ?`。
- 当两个条件都没有 → 内部为空，`trim` **不会输出任何东西**（包括 `WHERE`），避免 `SELECT * FROM sys_user WHERE` 这种语法错。

## 例 2：手写 `set`（等价于 `<set>`）

```xml
<update id="updateUser">
  UPDATE sys_user
  <trim prefix="SET" suffixOverrides=",">
    <if test="userName != null">user_name = #{userName},</if>
    <if test="email != null">email = #{email},</if>
  </trim>
  WHERE user_id = #{userId}
</update>
```
最后一个有值的字段后面会带逗号，`suffixOverrides=","` 把它删掉，避免 `SET email = ?,` 这种语法错。

## 例 3：`trim` 干 `where`/`set` 干不了的事

需求：可选排序与分页，且要同时处理前缀 `ORDER BY` 与后缀 `LIMIT`：

```xml
<select id="list" resultType="SysUser">
  SELECT * FROM sys_user
  <trim prefix="WHERE" prefixOverrides="AND ">
    <if test="deptId != null">AND dept_id = #{deptId}</if>
  </trim>
  <trim prefix="ORDER BY" prefixOverrides=",">
    <if test="orderBy != null">, ${orderBy}</if>
  </trim>
  <trim prefix="" suffixOverrides=" ">
    <if test="pageSize != null">LIMIT #{offset}, #{pageSize} </if>
  </trim>
</select>
```
> 注意：`${orderBy}` 是**用户可控的排序列名**，必须白名单校验列名，否则是 SQL 注入点（见 [[MyBatis Mapper XML/进阶专题/06-企业实战与疑难排查/04-诡异Bug排查与Locations坑根源]]）。

## 关键结论

- `trim` = `where`/`set` 的通用形态，能处理任意前后缀增删。
- `Overrides` 用的是**字符串前缀/后缀匹配**，多个候选用 `|` 分隔（注意每个候选前后留空格）。
- 内部为空时 `trim` 完全不输出——这是它比"手写 `WHERE 1=1`"优雅的地方。

下一步：[[MyBatis Mapper XML/进阶专题/01-动态SQL高级/02-choose-when-otherwise与嵌套分支]]
