# 06-企业实战与疑难排查 · 诡异 Bug 排查与 `Locations` 坑根源

> 前置：[[MyBatis Mapper XML/进阶专题/06-企业实战与疑难排查/00-索引]] ｜ 本仓库真实复盘：[[RuoYi-Analysis/11-后端-你的test-module与集成]]

这一篇把你**最初踩的真实坑**讲透，并扩展更多"看不懂报错"的排查。

## 🔴 你的 `Locations` 报错根因

报错原文：
```
Could not resolve type alias 'Locations'
```
发生在 `LocationsMapper.xml` 里写了 `resultType="Locations"`（或 `parameterType="Locations"`）。

### 为什么报这个错

`Locations` 是**类型别名（type alias）**，不是全限定类名。MyBatis 解析 XML 时：
1. 先去**已注册的别名表**找 `Locations`。
2. 别名表来自 `mybatis.typeAliasesPackage` 扫描到的所有 `*.domain` 类的**简单类名**。
3. 你的 `Locations` 实体在 `cn.yething.test.domain`，但当时 `typeAliasesPackage` 只配了 `com.ruoyi.**.domain` → **没扫到 `cn.yething` 包** → 别名 `Locations` 不存在 → 报错。

### 修复（本仓库已改好）

`ruoyi-admin/src/main/resources/application.yml`：
```yaml
mybatis:
  typeAliasesPackage: com.ruoyi.**.domain,cn.yething.**.domain
```
加了 `cn.yething.**.domain` 后，`Locations` 被扫到，别名注册成功，报错消失。

> 这也解释了为什么`test-module` 能并入主工程：`@ComponentScan({"cn.yething","com.ruoyi"})` + `@MapperScan("cn.yething.test.mapper")` + 上面的 `typeAliasesPackage` 三处都要覆盖到 `cn.yething`。

## `#{}` vs `${}`：永远的混淆源

| 写法 | 行为 | 安全 | 用途 |
|------|------|------|------|
| `#{name}` | 占位符 → `?` → 预编译设参 | **安全**（防注入） | 值（where 条件、set 值） |
| `${name}` | 字符串直接替换 | **危险**（注入） | 表名/列名/排序（必须白名单） |

### 经典错误 1：用 `${}` 拼值

```xml
WHERE user_name = '${userName}'   <!-- 用户输入 ' OR '1'='1 → 注入 -->
```
→ 必须改用 `#{userName}`。

### 经典错误 2：用 `#{}` 拼列名/表名（报错）

```xml
ORDER BY #{orderBy}   <!-- 生成 ORDER BY 'user_name' → 语法/逻辑错 -->
```
`#{}` 会加引号，`ORDER BY` 后面跟字符串字面量不报错但排序失效。列名/排序只能用 `${orderBy}`（**列名必须后端白名单**）。

## 参数找不到：`Parameter 'list' not found`

```xml
<foreach collection="list" ...>   <!-- 但方法参数是 @Param("ids") -->
```
- 单参 `List` → `collection="list"`。
- 但用了 `@Param("ids")` → 必须用 `collection="ids"`。
- 你的 `deleteLocationsByIds(long[] ids)` 用 `collection="array"`（数组）正确；若改用 `@Param` 则要改 `collection`。

> 回顾你当时的另一处整改：原 XML 误写了 `parameterType="String"`（数组怎么可能是 String），改成 `parameterType="Long[]"` 仍会错（MyBatis 默认别名无 `Long[]`），最终**直接删掉 `parameterType`**、保留 `collection="array"` 才对。`parameterType` 与 `collection` 是**独立**属性——删 `parameterType` 不影响 `collection="array"`。

## `Invalid bound statement (not found)`

```
Invalid bound statement (not found): com.x.mapper.UserMapper.selectX
```
排查四连：
1. XML 的 `namespace` = 接口**全限定名**？
2. `<select id="selectX">` 的 id = **方法名**？
3. XML 文件是否被 `mapperLocations` 扫到（路径/后缀 `*.xml`）？
4. 编译后 `target/classes/mapper/...Mapper.xml` 是否存在（Maven 没拷贝 resources）？

## `TooManyResultsException`

```
Expected one result (or null) to be returned by selectOne(), but found: 3
```
- `selectOne`/`selectById` 查到多行 → 检查条件是否漏了唯一键，或方法该用 `selectList`。

## `BindingException: Parameter 'xxx' not found`

- 多参数方法**没加 `@Param`**，XML 里用 `#{xxx}` 找不到。
- 解法：方法参数加 `@Param("xxx")`，或 XML 用 `#{param1}`/`#{arg0}`。

## `Type handler was null` / 类型不匹配

- 列类型与 Java 属性类型对不上（如 `BIGINT` 映射到 `String` 但没 TypeHandler）。
- 解法：加自定义 `TypeHandler`（见 [[MyBatis Mapper XML/进阶专题/05-插件与拦截器/04-脱敏加密与慢SQL拦截器]]）。

## 排查套路总结

```
报错含 "alias"        → typeAliasesPackage 漏包（你的 Locations）
报错含 "not found"    → namespace/id 错 或 XML 没扫到
报错含 "Parameter"    → @Param 或 collection 名错
报错含 "TooMany"      → selectOne 查到多行
SQL 慢/全表扫         → 隐式转换/函数包裹/前缀模糊（见 04-04）
```

下一步回到总览：[[MyBatis Mapper XML/进阶专题/00-进阶专题总览]] ｜ 回到基础 [[MyBatis Mapper XML/00-总览与目录]]
