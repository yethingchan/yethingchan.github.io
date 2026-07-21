# 05 · 入口文件 main.js 解析

本项目 `frontend/src/main.js`（已核对）：

```js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import directive from './directive'
import './styles/index.css'

const app = createApp(App)

app.use(createPinia())   // 1. 状态管理
app.use(router)          // 2. 路由
app.use(ElementPlus)     // 3. Element Plus（全量）
app.use(directive)       // 4. 自定义指令（v-hasPermi）

// 5. 全局注册所有图标组件
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')        // 6. 挂载到 index.html 的 #app
```

## 逐行说明

1. `createPinia()` + `app.use`：启用 Pinia，之后 `useUserStore()` 才可用。
2. `app.use(router)`：启用路由，`<router-view/>` 才能渲染页面。
3. `app.use(ElementPlus)`：**全量引入** Element Plus + 它的 CSS。
4. `app.use(directive)`：注册 `v-hasPermi` 权限指令（见 `src/directive/index.js`）。
5. 把 Element Plus 所有图标注册成全局组件，模板里可直接 `<Edit/>`、`<Delete/>`。
6. `mount('#app')`：把根组件挂到 `index.html` 的 `<div id="app">`。

## 注意

- **路由守卫写在 `router/index.js` 的 `beforeEach`**，只要 `import router` 就已生效，无需在 main.js 额外调用。
- 没有按需引入 Element Plus（全量），首包稍大但简单。生产可对体积敏感时改按需。

## 小结

`main.js` 是"装配中心"：挂 Pinia、router、Element Plus、权限指令、全局图标，最后 `mount('#app')`。

下一篇：[App.vue 根组件](./06-App.vue根组件.md)
