# 07 · 主键自增 `useGeneratedKeys` 与 `<selectKey>`

> 上接：[[00-总览与目录]] ｜ 前置：[[01-文件结构与四大标签]]

INSERT 后想把数据库生成的主键回填到入参对象，有两种方式。

## 方式一：useGeneratedKeys（MySQL 自增列）

```xml
<insert id="insertUser" parameterType="SysUser"
        useGeneratedKeys="true" keyProperty="id" keyColumn="user_id">
    insert into sys_user(user_name, status)
    values(#{userName}, #{status})
</insert>
```

- `useGeneratedKeys="true"`：使用 JDBC 的 `getGeneratedKeys`。
- `keyProperty`：回填到 Java 对象的哪个属性（如 `id`）。
- `keyColumn`：对应的数据库列名（多列主键或列名不一致时指定）。

> 执行后 `user.getId()` 就能拿到自增主键值。

## 方式二：`<selectKey>`（序列 / UUID / 非自增）

```xml
<insert id="insertUser">
    <selectKey keyProperty="id" resultType="Long" order="BEFORE">
        select seq_user.nextval from dual
    </selectKey>
    insert into sys_user(user_id, user_name) values(#{id}, #{userName})
</insert>
```

- `order="BEFORE"`：插入**前**取主键（序列类，如 Oracle）。
- `order="AFTER"`：插入**后**取主键（自增类，等价于 `useGeneratedKeys`）。
- `keyProperty`：回填到哪个属性。
- `resultType`：主键类型（用别名或全限定名）。

### UUID 示例

```xml
<insert id="insertUser">
    <selectKey keyProperty="id" resultType="String" order="BEFORE">
        select replace(uuid(), '-', '') from dual
    </selectKey>
    insert into sys_user(user_id, user_name) values(#{id}, #{userName})
</insert>
```

## 速记

| 主键来源 | 写法 |
|----------|------|
| MySQL 自增列 | `useGeneratedKeys="true" keyProperty="id"` |
| Oracle 序列 / 需先取号 | `<selectKey order="BEFORE">` |
| UUID | `<selectKey order="BEFORE">` + `uuid()` |
| 自增但想显式写 | `<selectKey order="AFTER">` |

> 关联阅读：返回值/参数映射基础见 [[02-parameterType与resultType]]。
