# 02 · HTTP 请求与响应格式

## 请求格式

- Method：`GET / POST / PUT / DELETE`（REST 风格）。
- Header：`Content-Type: application/json`，`Authorization: Bearer <token>`。
- Body（POST/PUT）：JSON。
- Query（GET）：URL 参数。

前端 `request.js` 统一处理了 `baseURL=/api` 和 token 注入，所以业务代码只写：

```js
request({ url: '/system/user/list', method: 'get', params: query })  // GET
request({ url: '/system/user', method: 'post', data: form })         // POST
```

## 响应格式（统一）

后端所有接口返回 `AjaxResult`：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": { "rows": [...], "total": 50 }
}
```

| 字段 | 含义 |
|------|------|
| `code` | 业务状态码（200 成功，其它失败） |
| `msg` | 提示信息 |
| `data` | 业务数据（可为对象/数组/null） |

## 前端如何消费

```js
const res = await listUser(query)   // res 是响应拦截返回的 res（即 {code,msg,data}）
const rows = res.data.rows
```

⚠️ 注意：响应拦截器 `return res`（整个对象），**数据在 `res.data` 里**，别漏一层。

## 状态码 vs HTTP 状态码

| 层 | 含义 |
|----|------|
| HTTP 状态码 | 200/404/500（网络/服务器层） |
| 业务 `code` | 200/401/403/500（业务层，在响应体里） |

本项目用**业务 `code`** 判断成功与否；HTTP 状态码通常恒为 200（即使业务失败），错误靠 `code` 体现。

## 小结

请求 = JSON + Bearer token；响应 = `{code,msg,data}`，前端取 `res.data`。业务成功看 `code===200`。

下一篇：[统一返回结果 AjaxResult](./03-统一返回结果AjaxResult.md)
