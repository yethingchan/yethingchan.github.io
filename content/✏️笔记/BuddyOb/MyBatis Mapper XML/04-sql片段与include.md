# 04 · `<sql>` 片段复用与 `<include>`

> 上接：[[00-总览与目录]] ｜ 相关：[[05-动态SQL]]

把重复的 SQL 片段抽出来复用，避免到处复制。

## 定义与引用

```xml
<!-- 定义可复用 SQL 片段 -->
<sql id="selectUserVo">
    select user_id, user_name, create_time
    from sys_user
</sql>

<!-- 引用 -->
<select id="selectUserList" resultMap="SysUserResult">
    <include refid="selectUserVo"/>
    <where>
        <if test="userName != null and userName != ''">
            and user_name like concat('%', #{userName}, '%')
        </if>
    </where>
</select>
```

## include 传参（片段里用 `${}` 取值）

```xml
<sql id="cols">
    ${prefix}user_id, ${prefix}user_name
</sql>

<select id="selectUser" resultMap="SysUserResult">
    select
    <include refid="cols">
        <property name="prefix" value="u."/>
    </include>
    from sys_user u
</select>
```

> ⚠️ **注入风险**：`<include>` 传的属性在片段里用 `${prefix}` 取值，是**字符串拼接（非预编译）**。
> 只用于列名/表名等**不能参数化**的位置，且值必须是**你代码里写死的常量**，绝不能来自用户输入，否则有 SQL 注入风险。
> 真正的值（用户输入）一律用 `#{}` 预编译。

## 速记

| 场景 | 做法 |
|------|------|
| 重复 select 列 / 表名 | 抽成 `<sql id="...">`，用 `<include refid="...">` 引用 |
| 片段需要不同前缀（多表 join） | `<include>` 内 `<property>` 传参，片段里 `${名}` 取值 |
| 用户数据 | 永远 `#{}`，不要放进 `${}` |
