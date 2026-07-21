# 14 · 接口调试与 Mock 数据

## 1. 用浏览器 Network 看请求

F12 → Network → 筛选 `/api` → 看请求头（Authorization）、响应体（code/msg/data）。

## 2. 用 Postman / Apifox / curl 直接测后端

```bash
# 登录拿 token
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# 带 token 查列表
curl http://localhost:8080/system/user/list?pageNum=1\&pageSize=10 \
  -H "Authorization: Bearer <token>"
```

> 注意：直接打后端 8080 时**不带 `/api` 前缀**（因为 `/api` 是前端加的，后端路由没有它）。这是最常见的困惑点。

## 3. 前端联调前 Mock

后端没好时，临时让 `request.js` 返回假数据：

```js
// 临时（仅开发）
if (config.url.includes('/system/user/list')) {
  return Promise.resolve({ code: 200, msg: 'ok', data: { rows: [], total: 0 } })
}
```

或更规范：用 Vite 插件 `vite-plugin-mock` 在 `vite.config.js` 配置 mock 接口。

## 4. 后端没启动怎么办

开发态若没起后端，`/api` 代理会连不上 → Network 报 `(failed) net::ERR_CONNECTION_REFUSED`。先确认后端 `mvn spring-boot:run` 已起在 8080。

## 5. 验证前后端契约一致性

- 字段名：后端 `AjaxResult{code,msg,data}`，前端只认这三个。
- 列表：`rows`/`total`。
- 权限串：三处一致（见 08 篇）。

## 小结

调试 = Network 看请求 + curl/Postman 直测后端（注意不带 `/api`）+ 必要时 Mock。先保证后端 8080 在跑。

---

## 本分册完结

读完 **03-与SpringBoot对接** 14 篇，你已掌握前后端的全部契约。下一步把系统真正跑上服务器 → **[04-部署](./../04-部署/README.md)**。
