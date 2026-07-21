# 04 · JWT 认证与 Token 机制

本项目用 **JWT（JSON Web Token）** 做无状态登录认证。

## 流程

```
1. 登录：POST /login {username,password}
   → 后端校验密码（BCrypt）
   → 签发 JWT（含用户名/过期时间）
   → 返回 { code:200, data:{ token: "eyJ..." } }

2. 后续请求：Header 带 Authorization: Bearer <token>
   → 后端拦截器校验 JWT 签名+过期
   → 通过则放行，否则返回 code=401

3. 过期/无效：前端响应拦截清 token + 跳 /login
```

## Token 怎么存

- 前端：`localStorage['admin_token']`（`utils/auth.js`）。
- 每次请求由 `request.js` 请求拦截器加 `Authorization: Bearer <token>`。

## 后端侧（本项目约定）

- 登录接口 `/login` 校验成功后返回 token。
- 有一个 JWT 拦截器/过滤器，对除 `/login`、`/captcha` 外的接口校验 token。
- `SecurityConfig` 放行白名单：`/login`、`/captcha`、`/error`、`/favicon.ico`（注意：已无 `/h2-console`）。

## 前端如何配合

```js
// request.js 请求拦截
if (token) config.headers['Authorization'] = 'Bearer ' + token
// 响应拦截
if (res.code === 401) { removeToken(); location.href = '/login' }
```

## JWT 的特点

| 优点 | 注意 |
|------|------|
| 无状态，后端不存 session | 无法主动吊销（除非加黑名单/短期过期） |
| 适合分布式/多实例 | 密钥泄露风险，需 HTTPS |
| 自带过期时间 | 过期前端重新登录 |

## 小结

JWT = 登录发 token、请求带 `Bearer`、后端校验。前端存 localStorage，401 清 token 回登录。

下一篇：[登录接口对接流程](./05-登录接口对接流程.md)
