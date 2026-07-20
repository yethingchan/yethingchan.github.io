# 01-动态SQL高级 · `choose-when-otherwise` 与嵌套分支

> 前置：[[MyBatis Mapper XML/进阶专题/01-动态SQL高级/00-索引]]

## `choose` 是"多选一"，`if` 是"多选多"

- `if`：每个条件独立判断，命中几个拼几个。
- `choose`：像 `switch`，**从上往下，命中第一个 `when` 就停**，`otherwise` 是 `default`。

## 例 1：基础 choose（按优先级匹配查询条件）

```xml
<select id="selectByPriority" resultType="SysUser">
  SELECT * FROM sys_user
  <where>
    <choose>
      <when test="userId != null"> user_id = #{userId} </when>
      <when test="phone != null">  phone = #{phone} </when>
      <when test="email != null"> email = #{email} </when>
      <otherwise> status = '0' </otherwise>
    </choose>
  </where>
</select>
```
传了 `userId` 就只用 `userId`，**不会**再拼 `phone`/`email`。这正是 `choose` 与 `if` 的本质区别。

## 例 2：`choose` 与 `if` 混用（组合条件）

`choose` 分支内部仍可含 `if`：

```xml
<select id="search" resultType="SysUser">
  SELECT * FROM sys_user
  <where>
    <if test="tenantId != null"> AND tenant_id = #{tenantId} </if>
    <choose>
      <when test="roleKey != null and roleKey == 'admin'">
        AND user_id IN (SELECT user_id FROM sys_user_role)
      </when>
      <when test="deptId != null">
        AND dept_id = #{deptId}
      </when>
      <otherwise>
        AND del_flag = '0'
      </otherwise>
    </choose>
  </where>
</select>
```
这里 `tenantId` 是**必带**的跨租户条件（`if`），而 `roleKey`/`deptId` 是**二选一**的可选条件（`choose`）。

## 例 3：多级嵌套 choose（按状态走不同子查询）

```xml
<select id="listByState" resultType="OrderVO">
  SELECT * FROM wms_order
  <where>
    <choose>
      <when test="state == 'WAIT'">
        AND create_time &gt;= #{start}
        <choose>
          <when test="urgent == 1"> AND priority = 'HIGH' </when>
          <otherwise> AND priority != 'HIGH' </otherwise>
        </choose>
      </when>
      <when test="state == 'DONE'">
        AND finish_time IS NOT NULL
      </when>
      <otherwise>
        AND state != 'CANCEL'
      </otherwise>
    </choose>
  </where>
</select>
```
嵌套 `choose` 常用于"外层按大状态分流，内层再按子条件细分"。可读性比一长串 `if` 强得多。

## 易错点

1. **`when` 的 `test` 是 OGNL**，字符串比较用 `== 'admin'`（单引号），不是 `eq`。
2. **`choose` 没有 `else if` 语法**，多分支就是多个 `<when>`。
3. 每个 `when` 命中后**直接跳出**，后面的 `when` 不再求值——若需要"都拼上"，应改用 `if`。
4. `otherwise` 可省略；省略后且都不命中，则 `choose` 不输出任何内容（`where` 会智能去掉自身）。

下一步：[[MyBatis Mapper XML/进阶专题/01-动态SQL高级/03-foreach深层用法与嵌套集合]]
