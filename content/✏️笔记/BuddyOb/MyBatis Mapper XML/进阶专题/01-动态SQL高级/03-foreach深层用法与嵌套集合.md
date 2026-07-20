# 01-动态SQL高级 · `foreach` 深层用法与嵌套集合

> 前置：[[MyBatis Mapper XML/进阶专题/01-动态SQL高级/00-索引]] ｜ 基础：[[MyBatis Mapper XML/06-foreach的collection规则]]

## `foreach` 属性回顾

| 属性 | 含义 |
|------|------|
| `collection` | 要遍历的对象（见下方取值规则） |
| `item` | 当前元素变量名 |
| `index` | 当前下标（`List`/`数组` 为下标；`Map` 为 key） |
| `open`/`close` | 包裹整个片段的首尾字符串 |
| `separator` | 每轮之间的分隔符 |

## 取值规则（基础篇已讲，复习关键）

- 单参 `List` → `collection="list"`
- 单参数组 → `collection="array"`（本仓库 `LocationsMapper.deleteLocationsByIds` 用此，见 [[MyBatis Mapper XML/进阶专题/06-企业实战与疑难排查/04-诡异Bug排查与Locations坑根源]]）
- `@Param("ids")` → `collection="ids"`
- `Map` 参数 → `collection="mapKey"` 或遍历 entry

## 例 1：批量 IN（最常见）

```xml
<select id="selectByIds" resultType="SysUser">
  SELECT * FROM sys_user
  WHERE user_id IN
  <foreach collection="ids" item="id" open="(" close=")" separator=",">
    #{id}
  </foreach>
</select>
```
传入 `ids=[1,2,3]` → `WHERE user_id IN (?, ?, ?)`，参数逐个安全注入。

## 例 2：批量插入（多值行）

```xml
<insert id="batchInsert">
  INSERT INTO sys_log (user_name, ip, status) VALUES
  <foreach collection="list" item="item" separator=",">
    (#{item.userName}, #{item.ip}, #{item.status})
  </foreach>
</insert>
```
遍历 `List<SysLog>`，每个对象取属性。**注意** `item` 是对象，用 `#{item.userName}` 取字段。性能细节见 [[MyBatis Mapper XML/进阶专题/04-批量与性能优化/01-foreach批量插入与MySQL批处理]]。

## 例 3：嵌套集合（List 里套对象，对象里又有 List）

需求：批量插入"每个订单的多条明细"——即双重 `foreach`：

```xml
<insert id="batchInsertOrders">
  INSERT INTO wms_order_item (order_id, sku, qty) VALUES
  <foreach collection="orders" item="order" separator=",">
    <foreach collection="order.items" item="it" separator=",">
      (#{order.id}, #{it.sku}, #{it.qty})
    </foreach>
  </foreach>
</insert>
```
外层遍历 `orders`，内层 `collection="order.items"` 遍历每个订单的明细列表 `List<OrderItem>`。这种"多对多展开"是 `foreach` 嵌套的精髓。

## 例 4：`Map` 遍历（动态列更新）

```xml
<update id="dynamicSet">
  UPDATE sys_user
  <set>
    <foreach collection="fieldMap" index="col" item="val" separator=",">
      ${col} = #{val}
    </foreach>
  </set>
  WHERE user_id = #{userId}
</update>
```
`fieldMap` 为 `Map<String,Object>`，`index` 是列名、`item` 是值。列名用 `${col}`（**必须是白名单列名**，否则注入）；值用 `#{val}`。

## 例 5：`open/close` 与 `separator` 组合做 `(a,b),(c,d)`

```xml
<foreach collection="pairs" item="p" open="" close="" separator=",">
  (#{p.k}, #{p.v})
</foreach>
```

## 避坑

1. **`collection` 值写错** → `Parameter 'list' not found`，是最常见的 foreach 报错（详见 [[MyBatis Mapper XML/进阶专题/06-企业实战与疑难排查/04-诡异Bug排查与Locations坑根源]]）。
2. **IN 列表为空** → `IN ()` 语法错。务必在 Java 层判空，或用 `WHERE 1=1 AND (${empty ? '1=1' : 'id IN (...)'}`) ` 防御。
3. **`separator` 忘写** → 所有值挤一起报语法错。
4. 大列表 `IN` 超过 MySQL `max_allowed_packet` 或优化器阈值（约 1000~2000）→ 改 `JOIN` 临时表或分批。

下一步：[[MyBatis Mapper XML/进阶专题/01-动态SQL高级/04-bind与OGNL及多数据库动态]]
