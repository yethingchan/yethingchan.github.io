# 02 · 创建第一个 Vue 应用

本篇用最小例子让你跑通一个 Vue 3 应用。本项目用 **Vite** 作为构建工具（见 02 分册），这里先理解核心流程。

## 一个单文件组件（SFC）的结构

Vue 3 推荐使用 `.vue` 单文件组件，包含三部分：

```vue
<template>
  <!-- 视图：HTML 模板 -->
  <h1>{{ title }}</h1>
  <button @click="count++">点击了 {{ count }} 次</button>
</template>

<script setup>
// 逻辑：组合式 API
import { ref } from 'vue'
const title = '我的第一个 Vue 应用'
const count = ref(0)
</script>

<style scoped>
/* 样式：scoped 表示只作用于当前组件 */
h1 { color: #409eff; }
</style>
```

- `<template>`：模板，`{{ }}` 是插值。
- `<script setup>`：组合式 API 的语法糖，写的顶层变量/函数自动暴露给模板。
- `<style scoped>`：样式局部作用域，避免污染其它组件。

## 应用入口（对比本项目 main.js）

创建应用并挂载到 `#app`：

```js
import { createApp } from 'vue'
import App from './App.vue'

createApp(App).mount('#app')
```

本项目 `src/main.js` 比这多了几步（注册 Pinia、router、Element Plus、权限指令），本质一样：

```js
const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.use(directive)        // v-hasPermi 指令
app.mount('#app')
```

## 用 Vite 创建项目（与本项目一致）

```bash
npm create vite@latest my-app -- --template vue
cd my-app
npm install
npm run dev      # 启动开发服务器（本项目端口 3000）
```

> 本项目的 `package.json` 只有 `dev / build / preview` 三条脚本，技术栈正是 Vue3 + Vite。

## 小结

- `.vue` = template + script + style 三块。
- `createApp(App).mount('#app')` 是启动入口。
- Vite 是开发/构建工具，提供热更新和打包。

下一篇：[模板语法与插值](./03-模板语法与插值.md)
