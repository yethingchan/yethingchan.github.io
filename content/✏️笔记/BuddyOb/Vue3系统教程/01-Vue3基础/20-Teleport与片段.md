# 20 · Teleport 与片段

## Teleport：把内容"传送"到别处

弹窗、Toast 常需要脱离当前 DOM 层级（避免被 `overflow:hidden` 裁剪）：

```vue
<Teleport to="body">
  <div class="modal" v-if="open">我是弹窗，渲染在 body 下</div>
</Teleport>
```

- `to` 可以是选择器（`body`、`#app`）或 DOM 节点。
- 逻辑仍在当前组件，只是 DOM 挂到别处。

## 本项目中的例子

Element Plus 的 `el-dialog` 内部默认用 `Teleport` 把对话框挂到 `body`，所以弹窗能覆盖全屏、不被布局裁剪。

## 片段（Fragment）：多根节点

Vue 3 模板支持多个根节点，无需包裹 div：

```vue
<template>
  <li>第一项</li>
  <li>第二项</li>
</template>
```

Vue 2 必须单一根节点，Vue 3 放宽了，写组件更自然。

## 小结

- `Teleport` 把 DOM 渲染到指定位置（弹窗/Toast 必备）。
- Vue 3 支持多根节点（Fragment），组件模板更简洁。

---

## 本分册完结

恭喜读完 **01-Vue3基础** 全部 20 篇！你已经掌握 Vue 3 的核心：响应式、组件、指令、插槽、动态组件、动画。

接下来建议进入 **[02-Vue3前端开发](./../02-Vue3前端开发/README.md)**，看看本项目如何把这些能力组织成一个真实工程（Vite、路由、Pinia、Element Plus 的真实代码范式）。
