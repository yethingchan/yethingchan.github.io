# Vue / React / TypeScript 学习路线

> 对应 Java 全栈路线思维导图「前端与工程」第二叶。现代前端框架 + 类型系统。

## 一、学习阶段

### 入门：能写组件
- TypeScript：类型、接口、泛型、与 JS 互操作
- Vue3：Composition API、响应式、组件通信、Pinia
- React：Hooks（useState/useEffect）、组件、状态管理

### 进阶：能写应用
- 路由：Vue Router / React Router
- 状态：Pinia / Redux / Zustand
- 工程化：Vite、ESLint、环境配置
- 网络：Axios/fetch、Token 管理、拦截器

### 高级：能交付
- 组件封装（slot/props 设计）
- 性能：虚拟列表、懒加载、Memo
- 与后端联调：接口约定、Mock、错误统一处理
- UI 库：Element Plus / Ant Design

## 二、关键要点与常见坑
- TS 不是可选，大项目它能救命（类型即文档）
- Vue 响应式：ref vs reactive 区别、解构丢失响应
- React 依赖数组写错导致死循环/不更新
- 跨域开发用代理，上线用网关/Nginx

## 三、实战
- 入门：用 Vue3+TS 写个人博客前台
- 进阶：React+TS 写带登录鉴权的管理后台

## 四、衔接
- 前置《HTML 与 CSS 与 JS 学习路线》
- 联动《Spring 与 SpringBoot》（后端接口、JWT）

## 五、资源
- Vue/React 官方文档；TypeScript 官方文档；《TypeScript 入门到实践》

## 六、心法
1. 二选一深耕（Vue 或 React），别都浅尝。
2. TS 越早上手越省 bug。
3. 全栈前端目标：能独立交付页面并和后端联调，不追求精通到源码。
