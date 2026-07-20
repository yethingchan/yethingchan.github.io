# 02-高级结果映射 · `collection` 一对多与嵌套查询 vs 结果

> 前置：[[MyBatis Mapper XML/进阶专题/02-高级结果映射/00-索引]]

## 场景：角色 → 多个菜单（一对多）

### 写法 A：嵌套结果（JOIN + `resultMap` 去重）

```xml
<resultMap id="roleWithMenus" type="SysRole">
  <id property="roleId" column="role_id"/>
  <result property="roleName" column="role_name"/>
  <collection property="menus" ofType="SysMenu">
    <id property="menuId" column="menu_id"/>
    <result property="menuName" column="menu_name"/>
  </collection>
</resultMap>

<select id="selectRoleMenus" resultMap="roleWithMenus">
  SELECT r.role_id, r.role_name, m.menu_id, m.menu_name
  FROM sys_role r
  LEFT JOIN sys_role_menu rm ON r.role_id = rm.role_id
  LEFT JOIN sys_menu m ON rm.menu_id = m.menu_id
  WHERE r.role_id = #{id}
</select>
```
- **关键**：`collection` 内**必须有 `<id>`**（这里是 `menu_id`）。
- MyBatis 用 `role_id`（外层 id）+ `menu_id`（内层 id）做**行去重**：同一角色的多行被折叠成一个 `SysRole` 对象，`menus` 列表收集所有菜单行。
- 若忘了写内层 `<id>` → 无法去重，可能重复或错位。

### 写法 B：嵌套查询（N+1 风险）

```xml
<resultMap id="roleLazyMenus" type="SysRole">
  <id property="roleId" column="role_id"/>
  <collection property="menus" ofType="SysMenu" column="role_id"
              select="com.x.mapper.SysMenuMapper.selectByRole"
              fetchType="lazy"/>
</resultMap>
```
- 先查角色（1 条），再对每个角色发菜单查询（N 条）→ **N+1 问题**。
- `fetchType="lazy"` 可缓解（不遍历就不查），但一旦遍历 N 个角色就发 N 条 SQL。

## 两种写法取舍

| 维度 | 嵌套结果（JOIN） | 嵌套查询（select） |
|------|------------------|---------------------|
| SQL 条数 | 1 条 | 1+N 条（N+1） |
| 结果集 | 行被笛卡尔积放大 | 干净 |
| 延迟加载 | 不支持 | 支持 |
| 适用 | 关联必用、数据量可控 | 关联偶尔用、且能 lazy |
| 内存 | JOIN 大表可能膨胀 | 按需 |

> 企业里**绝大多数情况用"嵌套结果 + 写好 `<id>`"**，避免 N+1。N+1 是线上慢查询头号原因（见 [[MyBatis Mapper XML/进阶专题/04-批量与性能优化/04-性能陷阱N+1与类型开销]]）。

## `column` 传多参给子查询

```xml
<collection property="orders" ofType="Order"
  column="{uid=userId, tenant=tenantId}"
  select="com.x.OrderMapper.selectByUserTenant" fetchType="lazy"/>
```
子查询 `selectByUserTenant` 用 `@Param("uid")`、`@Param("tenant")` 接收。

## 多列聚合到一个 `ofType` 集合的技巧

如果 `collection` 的列名与外层冲突（都叫 `id`），用 `columnPrefix` 区分（见 04）。

下一步：[[MyBatis Mapper XML/进阶专题/02-高级结果映射/03-多对多与discriminator鉴别器]]
