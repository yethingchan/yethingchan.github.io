# 03 · 统一返回结果 AjaxResult

后端用 `AjaxResult` 包装所有响应（本项目约定，对应 RuoYi 的 `AjaxResult`）。

## 结构

```java
public class AjaxResult {
    private int code;
    private String msg;
    private Object data;
    // 构造 + 静态工厂
    public static AjaxResult success(Object data) { ... }   // code=200
    public static AjaxResult error(String msg) { ... }      // code=500
    public static AjaxResult error(int code, String msg) { ... }
}
```

## 常用写法

```java
// 成功带数据
return AjaxResult.success(userList);
// 成功带提示
return AjaxResult.success("操作成功");
// 失败
return AjaxResult.error("参数错误");
// 自定义 code
return AjaxResult.error(500, "服务器异常");
```

## 分页专用（TableDataInfo）

列表接口返回分页结构：

```java
public class TableDataInfo {
    private List<?> rows;   // 当前页数据
    private long total;     // 总条数
}
// 返回
return getDataTable(list);   // 自动包成 { code:200, rows, total }
```

前端对应取 `res.data.rows` / `res.data.total`。

## 为什么需要统一返回

- 前端响应拦截器**只认 `code===200`**，结构不统一就会误判失败。
- 所有接口同构，前端不用为每个接口写不同解析逻辑。

## ⚠️ 对接踩坑

- 后端若直接 `return userList`（裸 List），前端 `res.code` 是 undefined → 被当成失败。
- 务必用 `AjaxResult.success(...)` 包裹。

## 小结

所有接口返回 `AjaxResult{code,msg,data}`；列表用 `TableDataInfo{rows,total}`。后端务必统一包裹，否则前端解析失败。

下一篇：[JWT 认证与 Token 机制](./04-JWT认证与Token机制.md)
