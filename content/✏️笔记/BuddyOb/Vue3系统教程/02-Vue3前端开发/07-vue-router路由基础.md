# 07 · vue-router 路由基础

路由 = URL 与页面组件的映射。本项目用 **vue-router 4**。

## 基本写法

```js
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', component: () => import('@/views/Home.vue') },
  { path: '/user', component: () => import('@/views/User.vue') }
]

const router = createRouter({
  history: createWebHistory(),   // HTML5 history 模式（URL 无 #）
  routes
})
```

## 两种加载方式

```js
// 静态导入（进首包）
import Home from '@/views/Home.vue'

// 动态导入（懒加载，按需请求，本项目用这种）
component: () => import('@/views/Home.vue')
```

> 本项目除登录/404/布局壳外，业务页都用 `() => import(...)` 懒加载，减小首包。

## 路由模式：history vs hash

| 模式 | URL 样子 | 部署注意 |
|------|----------|----------|
| `createWebHistory` | `/user` | 需要服务器 `try_files` 兜底（本项目用的） |
| `createWebHash` | `/#/user` | 无需服务器配置，但 URL 带 `#` |

本项目用 `createWebHistory()`，**部署时 Nginx 必须配 `try_files $uri $uri/ /index.html`**，否则刷新子路由 404。

## 嵌套路由

```js
{
  path: '/',
  component: Layout,
  children: [
    { path: 'user', component: UserPage }  // 访问 /user
  ]
}
```

子路由渲染在 Layout 的 `<router-view/>` 里。

## 编程式导航

```js
import { useRouter } from 'vue-router'
const router = useRouter()
router.push('/user')          // 跳转
router.push({ name: 'User' }) // 按名字跳
router.replace('/login')      // 替换（无历史）
```

## 本项目中的用法

路由实例在 `src/router/index.js`，含 `constantRoutes`（登录/404/布局壳）与动态 `addRoute`（见下篇）。

## 小结

- 路由 = URL ↔ 组件映射。
- 本项目用 history 模式 + 懒加载；部署需 Nginx 兜底。
- 跳转用 `router.push`。

下一篇：[静态路由与动态路由](./08-静态路由与动态路由.md)
