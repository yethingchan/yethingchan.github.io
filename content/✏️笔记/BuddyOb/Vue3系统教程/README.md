# Vue 3 系统教程（面向 Spring Boot 后台管理系统）

本教程专为**本项目的后端（Spring Boot 3 + MyBatis-Plus + 纯 MySQL）**配套编写，目标是让你从零掌握：

1. **Vue 3 本身**——框架原理、响应式、组件、路由、状态管理。
2. **本项目的 Vue 3 前端开发**——目录结构、`vite.config.js`、`request.js`、动态路由、权限指令等真实代码范式。
3. **与 Spring Boot 对接**——统一返回、JWT、动态菜单、权限、CORS、分页、文件、字典。
4. **部署**——从 Windows 本地部署到 Linux 生产部署，再到 Docker 与 CI/CD。
5. **实战案例**——新增模块全流程、各管理模块拆解、常见问题。

> 技术栈（已核对项目实际）：Vue 3.5 + Vite 5.4 + Element Plus 2.8 + Pinia 2.2 + vue-router 4.4，纯 JavaScript（无 TypeScript）。

---

## 📚 目录导航

| 分册 | 内容 | 篇数 |
|------|------|------|
| [01-Vue3基础](./01-Vue3基础/README.md) | Vue 3 框架本身的系统讲解 | 20 |
| [02-Vue3前端开发](./02-Vue3前端开发/README.md) | 本项目前端工程的结构与开发范式 | 25 |
| [03-与SpringBoot对接](./03-与SpringBoot对接/README.md) | 前后端分离下的接口、鉴权、权限、部署契约 | 14 |
| [04-部署](./04-部署/README.md) | Windows / Linux / Docker / CI-CD 部署 | 22 |
| [05-实战案例](./05-实战案例/README.md) | 新增模块、模块拆解、排错 FAQ | 8 |

---

## 🧭 建议学习路线

```
完全新手：  01（Vue3基础）  →  02（前端开发）  →  03（对接）  →  04（部署）  →  05（实战）
只做前端：  02  →  03  →  05
只做部署：  04（按需跳读）
排查对接问题：03 + 05-08（FAQ）
```

## ⚠️ 本项目几个关键事实（写进教程前的提醒）

- 前端**没有 `.env` 文件**，`baseURL` 硬编码为 `/api`（见 `src/utils/request.js`）。
- 开发态靠 `vite.config.js` 的 `proxy` 把 `/api` 转发到后端 `http://localhost:8080` 并**剥离 `/api` 前缀**。
- 生产态前端靠 **Nginx 反代**把 `/api` 转到后端；本项目**尚未内置** Dockerfile / nginx.conf / 部署脚本，教程 04 分册会给出可直接落地的模板。
- 动态路由来自后端 `/getRouters`；权限来自 `/getInfo` 返回的 `permissions`；统一返回结构为 `{ code, msg, data }`，`code===200` 为成功。
- 路由使用 `createWebHistory()`（history 模式），部署时 Nginx 必须配 `try_files` 防刷新 404。

---

## 📌 配套命令速查

```bash
# 前端
npm install      # 安装依赖
npm run dev      # 开发服务器，端口 3000
npm run build    # 生产构建，产物在 dist/
npm run preview  # 本地预览 dist，端口 4173

# 后端（纯 MySQL，需先建库并 source 两个 SQL）
mvn spring-boot:run
```

开始阅读 → [01-Vue3基础](./01-Vue3基础/README.md)
