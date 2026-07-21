# 02 · Vue 3 前端开发（本项目工程）

本分册基于**本项目的真实前端代码**讲解。读完你会知道每个文件在哪、干嘛用、怎么改。

> 关键技术事实（已核对 `package.json` / `vite.config.js` / `src/`）：
> - 技术栈：Vue 3.5 + Vite 5.4 + Element Plus 2.8 + Pinia 2.2 + vue-router 4.4，**纯 JavaScript（无 TS）**。
> - 启动 `npm run dev` → 端口 **3000**；构建 `npm run build` → 产物 `dist/`。
> - **没有 `.env` 文件**，`baseURL` 硬编码 `src/utils/request.js` 的 `/api`。
> - 开发态代理：`vite.config.js` 的 `proxy` 把 `/api` → `http://localhost:8080` 并剥离前缀。
> - 权限指令 `v-hasPermi`、动态路由 `/getRouters`、登录态 `localStorage['admin_token']`。

## 本分册目录

1. [项目目录结构详解](./01-项目目录结构详解.md)
2. [技术栈概览](./02-技术栈概览.md)
3. [package.json 详解](./03-package.json详解.md)
4. [Vite 配置详解](./04-Vite配置详解.md)
5. [入口文件 main.js 解析](./05-入口文件main.js解析.md)
6. [App.vue 根组件](./06-App.vue根组件.md)
7. [vue-router 路由基础](./07-vue-router路由基础.md)
8. [静态路由与动态路由](./08-静态路由与动态路由.md)
9. [路由守卫与登录拦截](./09-路由守卫与登录拦截.md)
10. [Pinia 状态管理入门](./10-Pinia状态管理入门.md)
11. [UserStore 状态设计](./11-UserStore状态设计.md)
12. [Axios 请求封装 request.js](./12-Axios请求封装request.js.md)
13. [接口 API 层封装规范](./13-接口API层封装规范.md)
14. [Element Plus 组件库引入](./14-Element-Plus组件库引入.md)
15. [表格组件标准用法](./15-表格组件标准用法.md)
16. [表单与弹窗标准用法](./16-表单与弹窗标准用法.md)
17. [分页与查询标准用法](./17-分页与查询标准用法.md)
18. [按钮权限指令 v-hasPermi](./18-按钮权限指令v-hasPermi.md)
19. [布局组件 Layout 解析](./19-布局组件Layout解析.md)
20. [侧边栏菜单渲染](./20-侧边栏菜单渲染.md)
21. [登录页面与流程](./21-登录页面与流程.md)
22. [图标与 SVG 使用](./22-图标与SVG使用.md)
23. [环境变量与多环境配置](./23-环境变量与多环境配置.md)
24. [代码规范与目录约定](./24-代码规范与目录约定.md)
25. [前端开发调试技巧](./25-前端开发调试技巧.md)

## 学习建议

- 先读 1~6 建立"工程全景"，再读 7~13 理解"路由/状态/请求"三大支柱。
- 14~18 是写业务页面最常用的组件与权限范式。
- 19~22 理解布局与登录串联。
- 23~25 是工程化与提效。

读完本分册后进入 [03-与SpringBoot对接](./../03-与SpringBoot对接/README.md)。
