# 10 · Pinia 状态管理入门

Pinia 是 Vue 官方推荐的状态库，用来存**跨组件共享**的数据（本项目里就是"登录用户"）。

## 为什么需要它

- 登录态 `token`、当前用户、菜单、权限，很多组件都要用。
- 用 `props` 一层层传太繁琐；用 Pinia 集中管理，任何组件 `useUserStore()` 直接取。

## 定义 store

```js
import { defineStore } from 'pinia'

export const useCounterStore = defineStore('counter', {
  state: () => ({ count: 0 }),
  getters: {
    double: (state) => state.count * 2
  },
  actions: {
    inc() { this.count++ }
  }
})
```

- `state`：响应式数据（类似 `data`）。
- `getters`：派生（类似 `computed`）。
- `actions`：方法（可异步，类似 `methods`）。

## 使用 store

```vue
<script setup>
import { useCounterStore } from '@/store/modules/counter'
const store = useCounterStore()
store.inc()              // 调 action
console.log(store.count) // 读 state
</script>
```

## 本项目里的 store

`src/store/modules/user.js` 是唯一 store，存：

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

- 页面里 `const userStore = useUserStore()` 就能拿到当前用户、菜单、权限。
- 侧边栏读 `userStore.menus` 渲染菜单；按钮读 `userStore.permissions` 判权限。

## 小结

Pinia = 全局状态中心。本项目只用 `user` 一个 store 管理登录态与菜单权限。任何组件 `useUserStore()` 即用。

下一篇：[UserStore 状态设计](./11-UserStore状态设计.md)
