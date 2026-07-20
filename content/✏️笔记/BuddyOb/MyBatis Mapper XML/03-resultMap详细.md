# 03 · `resultMap` 详细

> 上接：[[00-总览与目录]] ｜ 前置：[[02-parameterType与resultType]]

当列名与 Java 属性名不一致，或需要映射关联对象/集合时，用 `resultMap` 代替 `resultType`。

## 基础结构

```xml
<resultMap id="SysUserResult" type="SysUser" autoMapping="true">
    <!-- 主键映射（可帮助提升性能，标记为主键） -->
    <id     property="userId"   column="user_id"/>
    <!-- 普通列映射 -->
    <result property="userName" column="user_name"/>
    <result property="createTime" column="create_time"/>
</resultMap>

<select id="selectUser" resultMap="SysUserResult">
    select user_id, user_name, create_time from sys_user where user_id = #{id}
</select>
```

- `id`：本 `resultMap` 的名字，被 `resultMap="..."` 引用。
- `type`：结果对象的类型（全限定名或别名）。
- `autoMapping="true"`：未显式列出的列仍按名字自动映射（默认遵循全局 `autoMappingBehavior`）。

## 子标签速查

| 子标签 | 说明 |
|--------|------|
| `<id>` | 主键列（可优化缓存/性能）。 |
| `<result>` | 普通列 → 属性。 |
| `<association>` | 一对一/多对一对象。`javaType` 指定类型，`resultMap` 可复用另一个映射。 |
| `<collection>` | 一对多集合。`ofType` 指定集合元素的类型。 |
| `<constructor>` | 用构造器而非 setter 注入。 |
| `<discriminator>` | 按列值分支选择不同 `resultMap`。 |

## association（一对一 / 多对一）

```xml
<resultMap id="SysUserResult" type="SysUser">
    <id     property="userId"   column="user_id"/>
    <result property="userName" column="user_name"/>
    <!-- 关联部门对象 -->
    <association property="dept" javaType="SysDept" resultMap="SysDeptResult"/>
</resultMap>

<resultMap id="SysDeptResult" type="SysDept">
    <id     property="deptId"   column="dept_id"/>
    <result property="deptName" column="dept_name"/>
</resultMap>
```

## collection（一对多）

```xml
<resultMap id="SysUserResult" type="SysUser">
    <id     property="userId"   column="user_id"/>
    <result property="userName" column="user_name"/>
    <!-- 关联角色集合 -->
    <collection property="roles" ofType="SysRole" resultMap="SysRoleResult"/>
</resultMap>
```

> 多表 join 时列名可能冲突，可在 `<association>/<collection>` 上加 `columnPrefix="d_"` 给关联表列加前缀。

## constructor（构造器注入）

```xml
<resultMap id="SysUserResult" type="SysUser">
    <constructor>
        <idArg column="user_id"   javaType="Long"/>
        <arg   column="user_name" javaType="String"/>
    </constructor>
</resultMap>
```

## discriminator（鉴别器，按列值选映射）

```xml
<resultMap id="SysUserResult" type="SysUser">
    <id     property="userId"   column="user_id"/>
    <result property="userName" column="user_name"/>
    <discriminator javaType="Integer" column="user_type">
        <case value="1" resultMap="SysUserAdminResult"/>
        <case value="2" resultMap="SysUserNormalResult"/>
    </discriminator>
</resultMap>
```

## 速记

- 列名能对上 → 直接用 `resultType`。
- 列名对不上 / 要关联 → `resultMap` + `<id>/<result>`。
- 一个对象嵌一个对象 → `<association>`。
- 一个对象嵌一组对象 → `<collection>`。
