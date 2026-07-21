# 06 · App.vue 根组件

`src/App.vue` 是整个应用的最外层壳，通常只放一个路由出口。

```vue
<template>
  <router-view />
</template>

<script setup>
// 根组件一般很轻：只负责渲染当前路由对应的页面
</script>

<style>
/* 全局基础样式，或留空交给 styles/index.css */
</style>
```

## 为什么这么简单

- 真正的布局（侧边栏、顶栏）在 `src/views/layout/index.vue`，由路由 `/` 指向。
- `App.vue` 只提供 `<router-view />`，URL 变化时这里渲染对应页面组件。
- 全局 Reset CSS、主题色在 `src/styles/index.css`（`main.js` 已引入）。

## 与布局的关系

```
App.vue
  └─ <router-view/>  （渲染匹配到的路由组件）
       ├─ /login        → views/login/index.vue
       └─ /             → views/layout/index.vue
                           └─ 内部又有一个 <router-view/> → 业务页面
```

注意：本项目有**两层** `<router-view/>`——外层在 App，内层在 Layout。Layout 是"带菜单的壳"，里面再放业务页。

## 小结

`App.vue` 是根壳，只放 `<router-view/>`；布局与菜单在 `views/layout`，全局样式在 `styles/index.css`。

下一篇：[vue-router 路由基础](./07-vue-router路由基础.md)
