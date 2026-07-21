# 11 · UserStore 状态设计

本项目 `src/store/modules/user.js`（已核对）是登录态的唯一真相来源。

## state

```js
state: () => ({
  token: localStorage.getItem('admin_token') || '',
  userInfo: {},
  roles: [],
  permissions: [],
  routers: [],
  menus: []
})
```

- `token`：初始从 `localStorage` 读，刷新不丢登录。
- `userInfo / roles / permissions`：来自 `/getInfo`。
- `routers`：后端原始菜单树；`menus`：转成 vue-router 后的路由（供侧边栏渲染）。

## actions

```js
login(data) {
  return new Promise((resolve, reject) => {
    loginApi(data).then(res => {
      setToken(res.data.token)         // 写 localStorage
      this.token = res.data.token       // 写 state
      resolve()
    }).catch(reject)
  })
},
getInfo() {
  return getInfoApi().then(res => {
    this.userInfo = res.data.user
    this.roles = res.data.roles
    this.permissions = res.data.permissions
    return res.data
  })
},
getRouters() {
  return getRoutersApi().then(res => {
    this.routers = res.data
    return res.data
  })
},
setMenus(menus) { this.menus = menus },
logout() {
  return new Promise(resolve => {
    logoutApi().then(() => {}).catch(() => {})
    this.token = ''
    this.roles = []; this.permissions = []
    this.routers = []; this.menus = []; this.userInfo = {}
    removeToken()
    resolve()
  })
}
```

## 后端契约（对接时务必对齐）

| action | 调的接口 | 期望返回 |
|--------|----------|----------|
| `login` | `/login` | `{ data: { token: 'xxx' } }` |
| `getInfo` | `/getInfo` | `{ data: { user, roles, permissions } }` |
| `getRouters` | `/getRouters` | `{ data: [菜单树] }` |
| `logout` | `/logout` | 任意（成败都清态） |

> ⚠️ 如果后端 `/getInfo` 不返回 `permissions` 或 `roles`，前端按钮权限与动态菜单会全部失效。

## 在组件里用

```vue
<script setup>
import { useUserStore } from '@/store/modules/user'
const userStore = useUserStore()
console.log(userStore.userInfo.userName)
</script>
```

## 小结

`user` store 管 token/用户/角色/权限/菜单。登录写 token，进首页拉 `getInfo`+`getRouters`，退出清空。后端返回字段名必须对齐。

下一篇：[Axios 请求封装 request.js](./12-Axios请求封装request.js.md)
