# 19 · 布局组件 Layout 解析

布局壳在 `src/views/layout/index.vue`（注意：不是 `src/layout`），是整个后台框架。

## 结构

```
Layout (views/layout/index.vue)
 ├─ 顶部栏（Header）
 │    ├─ 折叠按钮
 │    ├─ 面包屑
 │    └─ 右侧：用户名 + 下拉（个人中心/退出登录）
 ├─ 左侧：侧边栏（sidebarItem 递归渲染菜单）
 └─ 主区：<router-view/>  ← 业务页面渲染在这里
```

## 关键逻辑

```vue
<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/store/modules/user'
import SidebarItem from './sidebarItem.vue'

const userStore = useUserStore()
// 菜单数据来自动态路由：dashboard + 后端下发的 menus
const dashboard = { path: 'index', meta: { title: '首页', icon: 'HomeFilled' } }
const menuList = computed(() => [dashboard, ...userStore.menus])

function logout() {
  userStore.logout().then(() => router.push('/login'))
}
</script>
```

- 菜单列表 = `[dashboard, ...userStore.menus]`。
- 顶栏显示 `userStore.userInfo.userName`。
- 退出登录调 `userStore.logout()` 后跳 `/login`。

## 样式约定（内联）

- 侧边栏宽度 210px，主题色 `#304156`。
- 这些都写在组件 `<style>` 里（项目没有 `settings.js` 集中管理）。

## 两层 router-view

- `App.vue` 的 `<router-view/>` 渲染到 `/` → Layout。
- Layout 内部又有一个 `<router-view/>`，渲染业务页面（如用户管理）。

## 小结

Layout 是带菜单的壳，菜单来自 `userStore.menus`，业务页嵌在它的 `<router-view/>` 里。退出登录走 `userStore.logout()`。

下一篇：[侧边栏菜单渲染](./20-侧边栏菜单渲染.md)
