# 12 · Axios 请求封装 request.js（核心）

`src/utils/request.js` 是**所有 HTTP 请求的统一出口**。理解它就理解了前后端怎么对话。

## 完整代码（已核对）

```js
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from '@/utils/auth'

const service = axios.create({
  baseURL: '/api',
  timeout: 10000,
  paramsSerializer: (params) => {
    const parts = []
    Object.entries(params || {}).forEach(([k, v]) => {
      if (v === undefined || v === null) return
      if (Array.isArray(v)) {
        v.forEach(item => parts.push(`${k}=${encodeURIComponent(item)}`))
      } else {
        parts.push(`${k}=${encodeURIComponent(v)}`)
      }
    })
    return parts.join('&')
  }
})

// 请求拦截：自动带 token
service.interceptors.request.use(config => {
  const token = getToken()
  if (token) config.headers['Authorization'] = 'Bearer ' + token
  return config
})

// 响应拦截：统一处理 code
service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) return res          // 成功：返回 res（业务里取 res.data）
    ElMessage.error(res.msg || '操作失败')
    if (res.code === 401 || res.code === 403) {
      removeToken()
      if (window.location.pathname !== '/login') window.location.href = '/login'
    }
    return Promise.reject(new Error(res.msg || 'Error'))
  },
  error => {
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default service
```

## 关键点

### `baseURL: '/api'`
所有请求前缀 `/api`。开发态由 Vite 代理剥离转 8080；生产态由 Nginx 反代。**本项目没有 `.env`，这里硬编码。**

### 请求拦截
有 token 就加 `Authorization: Bearer <token>`。后端用这个校验身份。

### `paramsSerializer`
数组参数序列化成 `ids=1&ids=2`，对应后端 `@RequestParam List<Long> ids`。否则默认会变成 `ids[]=1`（后端收不到）。

### 响应拦截
- 后端返回 `{ code, msg, data }`。**`code === 200` 才算成功**，返回 `res`（注意返回的是整个 `res`，业务取 `res.data`）。
- `code === 401 || 403`：弹错 + 清 token + `window.location.href = '/login'` 硬刷新回登录页。
- 其它非 200：弹错 + reject。

### ⚠️ 业务里怎么取数据
```js
const res = await listUser(query)   // res 是响应拦截返回的 res
const rows = res.data.rows           // 真正的数据在 res.data 里
```

## 小结

`request.js` 统一做三件事：加 `/api` 前缀、请求带 token、响应按 `code===200` 判成功并统一报错/跳登录。所有 api 文件都 `import` 它。

下一篇：[接口 API 层封装规范](./13-接口API层封装规范.md)
