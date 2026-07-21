# 17 · Provide 与 Inject 依赖注入

当组件层级很深（祖→父→子→孙），用 `props` 一层层传很麻烦。`provide/inject` 让**祖先直接给任意后代传数据**。

## 祖先 provide

```vue
<!-- Ancestor.vue -->
<script setup>
import { provide, ref } from 'vue'
const theme = ref('dark')
provide('theme', theme)        // key + value（可以是响应式的）
</script>
```

## 后代 inject

```vue
<!-- DeepChild.vue -->
<script setup>
import { inject } from 'vue'
const theme = inject('theme', 'light')   // 第二个参数是默认值
</script>
<template><p>当前主题：{{ theme }}</p></template>
```

## 修改：提供方法

```vue
// 祖先
provide('changeTheme', (t) => { theme.value = t })
// 后代
const changeTheme = inject('changeTheme')
changeTheme('light')
```

## 与 Pinia 的关系

- `provide/inject`：适合**局部、组件树内**的共享（如主题、表单上下文）。
- `Pinia`：适合**全局、跨模块**的状态（如用户、token、菜单）。

> 本项目用 **Pinia（user store）** 管理全局登录态和菜单，而不是 provide/inject，因为登录态是整个应用都要用的。

## 小结

- `provide(key, value)` + `inject(key, 默认值)`。
- 解决"深层传值"问题，跨越多级组件。
- 全局状态优先用 Pinia；局部树内共享可用 provide/inject。

下一篇：[动态组件与异步组件](./18-动态组件与异步组件.md)
