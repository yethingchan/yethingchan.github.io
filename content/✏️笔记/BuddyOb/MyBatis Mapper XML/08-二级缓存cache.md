# 08 · 二级缓存 `<cache>`

> 上接：[[00-总览与目录]] ｜ 前置：[[01-文件结构与四大标签]]

## 开启本 namespace 的二级缓存

```xml
<cache
    eviction="LRU"
    flushInterval="60000"
    size="512"
    readOnly="true"/>
```

- 默认**关闭**，写上 `<cache/>` 即开启本 namespace 的二级缓存。
- `eviction`：`LRU`（默认）/ `FIFO` / `SOFT` / `WEAK`。
- `flushInterval`：刷新间隔（毫秒）。
- `size`：缓存对象最大数量。
- `readOnly`：`true` 返回只读引用（更快，但对象不可修改）；`false` 返回拷贝。

## 单条语句覆盖

```xml
<select id="selectUser" resultType="SysUser" useCache="false">
    ...
</select>

<insert id="insertUser" flushCache="true">
    ...
</insert>
```

- `useCache="false"`：该查询不读/不写二级缓存。
- `flushCache="true"`：该语句执行前清空缓存。

## 注意事项

- 被缓存的对象必须实现 `java.io.Serializable`。
- 二级缓存跨 SqlSession 共享，按 `namespace` 隔离；不同 namespace 的缓存互不干扰。
- 多表关联查询的缓存一致性需谨慎（任一表更新都应考虑失效）。

## 速记

| 想做 | 做法 |
|--------|------|
| 开启本文件缓存 | 写 `<cache/>` |
| 某查询不走缓存 | `useCache="false"` |
| 某写操作清空缓存 | `flushCache="true"` |
| 缓存对象 | 必须 `implements Serializable` |
