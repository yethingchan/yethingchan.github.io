# 06-企业实战与疑难排查 · 注解 XML 混合与复杂报表 SQL

> 前置：[[MyBatis Mapper XML/进阶专题/06-企业实战与疑难排查/00-索引]]

## 注解 vs XML 怎么选

| 场景 | 用 |
|------|-----|
| 简单 CRUD、单表 | 注解（`@Select`/`@Insert`）+ MyBatis-Plus |
| 复杂动态 SQL、长 SQL、多表 JION | **XML**（可读、可复用片段、好调试） |

> 经验：**动态 SQL 超过 3 个 `if` 就用 XML**。注解里塞 `<script>` 可读性差。

## 注解里写动态 SQL（`<script>`）

```java
@Select({
  "<script>",
  "SELECT * FROM sys_user",
  "<where>",
  "  <if test='userName != null'> AND user_name = #{userName} </if>",
  "  <if test='status != null'> AND status = #{status} </if>",
  "</where>",
  "</script>"
})
List<SysUser> list(@Param("userName") String userName, @Param("status") String status);
```
- 必须用 `<script>` 包裹，否则动态标签不解析。
- 每句要加逗号（Java 字符串数组元素）。
- 复杂时**还是 XML 香**。

## 复杂报表 SQL 实战

### 1) 行转列（动态列）

需求：把"每月销售额"转成"1月/2月/.../12月"列。

```xml
<select id="salesPivot" resultType="map">
  SELECT
    product,
    SUM(CASE WHEN MONTH(order_date)=1 THEN amount ELSE 0 END) AS m1,
    SUM(CASE WHEN MONTH(order_date)=2 THEN amount ELSE 0 END) AS m2,
    ... 12 列 ...
  FROM wms_order
  GROUP BY product
</select>
```
- 列数固定 → 直接 CASE。
- 列数**动态**（如任意季度）→ 只能 Java 拼 SQL（用 `${}` 拼列名，**必须白名单校验**）。

### 2) 递归 CTE（树形，如部门/菜单）

```xml
<select id="deptTree" resultType="SysDept">
  WITH RECURSIVE cte AS (
    SELECT * FROM sys_dept WHERE parent_id = 0
    UNION ALL
    SELECT d.* FROM sys_dept d JOIN cte ON d.parent_id = cte.dept_id
  )
  SELECT * FROM cte
</select>
```
- MySQL 8 支持 `WITH RECURSIVE`。
- 比"程序里递归查库"性能好得多（一次 SQL 出整棵树）。

### 3) 动态 IN + 分页 + 排序（报表常见组合）

```xml
<select id="report" resultType="ReportVO">
  SELECT u.user_name, COUNT(o.id) cnt
  FROM sys_user u LEFT JOIN wms_order o ON u.user_id = o.user_id
  <where>
    <if test="deptId != null"> AND u.dept_id = #{deptId} </if>
    <if test="statusList != null and statusList.size > 0">
      AND o.status IN
      <foreach collection="statusList" item="s" open="(" close=")" separator=","> #{s} </foreach>
    </if>
  </where>
  GROUP BY u.user_name
  <choose>
    <when test="orderBy != null"> ORDER BY ${orderBy} </when>
    <otherwise> ORDER BY cnt DESC </otherwise>
  </choose>
  LIMIT #{offset}, #{pageSize}
</select>
```
- `${orderBy}` 是列名占位，**必须白名单**（见 04 注入）。
- 报表导出用 Cursor/ResultHandler 流式（见 [[MyBatis Mapper XML/进阶专题/04-批量与性能优化/03-流式查询与大结果集]]）。

## 注解和 XML 能共存吗

能。同一 Mapper 接口可以：
- 方法 A 用注解 `@Select`。
- 方法 B 对应 XML `<select id="B">`。

但要满足：XML 的 `namespace` = 接口全限定名，且**接口方法和 XML 语句 id 不重复**（否则 `MappedStatement` 重复注册报错）。

## 结论

- 简单用注解，复杂/动态用 XML。
- 报表三板斧：CASE 行转列、CTE 递归树、动态 IN+分页+排序。
- 动态列名用 `${}` 必须白名单。

下一步：[[MyBatis Mapper XML/进阶专题/06-企业实战与疑难排查/03-Spring事务集成坑]]
