# 02 · `parameterType` 与 `resultType`

> 上接：[[00-总览与目录]] ｜ 上一节：[[01-文件结构与四大标签]]

## 3. `parameterType` 详解

- **作用**：声明「这条语句整体接收的入参类型是什么」。
- **可省略**：不写时，MyBatis 在真正调用方法时，根据传进来的实际参数自行推断类型。
- **与 `collection` 无关**：`<foreach>` 里的 `collection` 取值取决于「参数本身是不是数组/集合、以及有没有 `@Param` 命名」，与 `parameterType` 写没写、写什么都无关（详见 [[06-foreach的collection规则]]）。
- **数组不能用 `Long[]` 当别名**：MyBatis 内置别名只覆盖了基本类型数组（如 `long[]`），没有 `Long[]`。数组参数建议**直接省略 `parameterType`**，靠 `collection="array"` 遍历。

```xml
<!-- 推荐：省略 parameterType，运行时自动推断 -->
<delete id="deleteByIds">
    delete from t where id in
    <foreach collection="array" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</delete>
```

## 4. `resultType` 与 `resultMap` 的区别

- `resultType`：**自动映射**。要求数据库列名与 Java 属性名能对应（下划线转驼峰需开启 `mapUnderscoreToCamelCase`，或列名正好匹配）。适合简单对象。
- `resultMap`：**手动映射**。当列名与属性名不一致、或有关联对象（`association`）、集合（`collection`）时使用。详见 [[03-resultMap详细]]。

```xml
<!-- resultType：列名能直接对应属性名 -->
<select id="selectName" resultType="String">
    select user_name from sys_user where id = #{id}
</select>

<!-- 简单对象也能用 resultType（前提是列名匹配属性） -->
<select id="selectUser" resultType="com.ruoyi.system.domain.SysUser">
    select user_id, user_name from sys_user where user_id = #{id}
</select>
```

### 类型别名 vs 全限定类名

`resultType` / `parameterType` 既可以用全限定类名（如 `com.ruoyi.system.domain.SysUser`），也可以用**类型别名**（如 `SysUser`，前提是别名已注册）。

> 本仓库别名由 `application.yml` 的 `mybatis.typeAliasesPackage` 扫描 `*.domain` 包自动注册，**简单类名即别名**。
> 例：`cn.yething.test.domain.Locations` → 别名 `Locations`，XML 里可直接写 `resultType="Locations"`。
> 若别名找不到会报 `Could not resolve type alias 'Xxx'`，见 [[10-常见坑速查]]。

## 速记

| 想做的事 | 用 |
|----------|-----|
| 列名能直接对上属性 | `resultType` |
| 列名对不上 / 有关联对象 | `resultMap` |
| 数组/集合参数 | 省略 `parameterType`，用 `collection`（见 [[06-foreach的collection规则]]） |
