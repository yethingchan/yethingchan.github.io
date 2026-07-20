# 02-高级结果映射 · 多对多与 `discriminator` 鉴别器

> 前置：[[MyBatis Mapper XML/进阶专题/02-高级结果映射/00-索引]]

## 多对多：两个一对多 + 中间表

用户 ↔ 角色是典型多对多（中间表 `sys_user_role`）。**MyBatis 没有 `manyToMany` 标签**，用"一对多 + 中间实体"建模：

```xml
<resultMap id="userWithRoles" type="SysUser">
  <id property="userId" column="user_id"/>
  <!-- 第一层：用户 → 中间关系 -->
  <collection property="userRoles" ofType="SysUserRole" resultMap="urMap"/>
</resultMap>

<resultMap id="urMap" type="SysUserRole">
  <id property="id" column="ur_id"/>
  <result property="userId" column="user_id"/>
  <!-- 第二层：关系 → 角色 -->
  <association property="role" javaType="SysRole">
    <id property="roleId" column="role_id"/>
    <result property="roleName" column="role_name"/>
  </association>
</resultMap>

<select id="selectUserRoles" resultMap="userWithRoles">
  SELECT ur.id ur_id, u.user_id, r.role_id, r.role_name
  FROM sys_user u
  JOIN sys_user_role ur ON u.user_id = ur.user_id
  JOIN sys_role r ON ur.role_id = r.role_id
  WHERE u.user_id = #{id}
</select>
```
- 多对多 = 用户(1) → 关系(N) → 角色(1)，所以 `collection` 套 `association`。
- 若只想要角色名列表，可简化：直接 `collection ofType="SysRole"` 把 role 列映射进去（跳过中间对象）。

## `discriminator` 鉴别器：按某列选不同子集映射

场景：一张 `biz_content` 表存多种业务类型，不同 `type` 列对应不同字段集合。

```xml
<resultMap id="contentMap" type="BizContent">
  <id property="id" column="id"/>
  <result property="type" column="type"/>
  <discriminator column="type" javaType="string">
    <case value="TEXT" resultMap="textMap"/>
    <case value="IMAGE" resultMap="imageMap"/>
    <case value="VIDEO" resultMap="videoMap"/>
  </discriminator>
</resultMap>

<resultMap id="textMap" type="BizContent" extends="contentMap">
  <result property="textContent" column="text_content"/>
</resultMap>
<resultMap id="imageMap" type="BizContent" extends="contentMap">
  <result property="imageUrl" column="image_url"/>
</resultMap>
<resultMap id="videoMap" type="BizContent" extends="contentMap">
  <result property="videoUrl" column="video_url"/>
  <result property="duration" column="duration"/>
</resultMap>
```
- `discriminator column="type"` 读 `type` 的值，匹配 `<case>` 选用对应 `resultMap`。
- 子 `resultMap` 用 `extends` 继承父的字段，只补自己特有的。
- 类似 Java 的 `switch(type)`，但作用于**结果映射阶段**。

## 与继承映射的关系

`discriminator` 常用于"表继承"：`biz_content` 是父类表，各 type 是子类字段。MyBatis 不强制要求 `type` 是某个 Java 父类的子类，只要字段能映射到同一个 `resultType` 即可。

## 易错点

1. `discriminator` 的 `column` 必须是**已映射的列**（先有 `<result property="type" column="type"/>`）。
2. `case` 的 `resultMap` 必须 `extends` 父，否则父的字段在子类映射里丢失。
3. 多对多忘了写内层 `<id>` 同样会去重失败。

下一步：[[MyBatis Mapper XML/进阶专题/02-高级结果映射/04-构造器映射与列前缀继承复用]]
