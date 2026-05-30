---
title: "快捷命令：Vue VSCode Snippets"
description: ""
date: "2026-05-31"
tags: []
share: true
cover: https://public.ysjf.com/mediastorm/material/material/%E8%87%AA%E7%84%B6%E9%A3%8E%E5%85%89_%E4%B8%9C%E5%8C%97_5_%E5%85%A8%E6%99%AF.jpg
---

# Vue/Nuxt 快捷指令对照表
| 快捷键前缀                                   | 生成指令描述                                                                            | 所属分类        |
| --------------------------------------- | --------------------------------------------------------------------------------- | ----------- |
| vdata                                   | Vue Component Data                                                                | Vue 基础语法    |
| vmethod                                 | vue method                                                                        | Vue 基础语法    |
| vcomputed                               | computed value                                                                    | Vue 基础语法    |
| vbeforecreate                           | beforeCreate lifecycle method                                                     | Vue 生命周期    |
| vcreated                                | created lifecycle method                                                          | Vue 生命周期    |
| vbeforemount                            | beforeMount lifecycle method                                                      | Vue 生命周期    |
| vmounted                                | mounted lifecycle method                                                          | Vue 生命周期    |
| vbeforeupdate                           | beforeUpdate lifecycle method                                                     | Vue 生命周期    |
| vupdated                                | updated lifecycle method                                                          | Vue 生命周期    |
| vbeforedestroy                          | beforeDestroy lifecycle method                                                    | Vue 生命周期    |
| vdestroyed                              | destroyed lifecycle method                                                        | Vue 生命周期    |
| vwatcher                                | vue watcher                                                                       | Vue 监听器     |
| vwatcher-options                        | vue watcher with options                                                          | Vue 监听器     |
| vprops                                  | Vue Props with Default                                                            | Vue 组件传参    |
| vimport                                 | Import one component into another                                                 | Vue 组件导入    |
| vcomponents                             | Import one component into another, within export statement                        | Vue 组件导入    |
| vimport-export                          | import a component and include it in export default                               | Vue 组件导入    |
| vimport-dynamic                         | Import component that should be lazy loaded                                       | Vue 组件导入    |
| vmapstate                               | map getters inside a vue component                                                | Vuex 辅助函数   |
| vmapgetters                             | mapgetters inside a vue component                                                 | Vuex 辅助函数   |
| vmapmutations                           | mapmutations inside a vue component                                               | Vuex 辅助函数   |
| vmapactions                             | mapactions inside a vue component                                                 | Vuex 辅助函数   |
| vfilter                                 | vue filter                                                                        | Vue 过滤器     |
| vmixin                                  | vue mixin                                                                         | Vue 混入      |
| vmixin-use                              | vue use mixin                                                                     | Vue 混入      |
| vc-direct                               | vue custom directive                                                              | Vue 自定义指令   |
| vimport-lib                             | import a library                                                                  | Vue 第三方库导入  |
| vimport-gsap                            | import gsap library                                                               | Vue 第三方库导入  |
| vanimhook-js                            | transition component js hooks                                                     | Vue 过渡动画    |
| vcommit                                 | commit to vuex store in methods for mutation                                      | Vuex 操作     |
| vdispatch                               | dispatch to vuex store in methods for action                                      | Vuex 操作     |
| vtest                                   | unit test component                                                               | Vue 单元测试    |
| vconfig                                 | vue.config.js                                                                     | Vue 配置      |
| v3reactive                              | Vue Composition api - reactive                                                    | Vue3 组合式API |
| v3computed                              | Vue Composition api - computed                                                    | Vue3 组合式API |
| v3watch                                 | Vue Composition api - watcher single source                                       | Vue3 组合式API |
| v3watch-array                           | Vue Composition api - watch as array                                              | Vue3 组合式API |
| v3watcheffect                           | Vue Composition api - watchEffect                                                 | Vue3 组合式API |
| v3ref                                   | Vue Ref                                                                           | Vue3 组合式API |
| v3onmounted                             | Vue Mounted Lifecycle hook                                                        | Vue3 生命周期   |
| v3onbeforemount                         | Vue onBeforeMount Lifecycle hook                                                  | Vue3 生命周期   |
| v3onbeforeupdate                        | Vue onBeforeUpdate Lifecycle hook                                                 | Vue3 生命周期   |
| v3onupdated                             | Vue onUpdated Lifecycle hook                                                      | Vue3 生命周期   |
| v3onerrorcaptured                       | Vue onErrorCaptured Lifecycle hook                                                | Vue3 生命周期   |
| v3onunmounted                           | (destroyed) Vue onUnmounted Lifecycle hook                                        | Vue3 生命周期   |
| v3onbeforeunmount                       | (beforeDestroy) Vue onBeforeUnmount Lifecycle hook                                | Vue3 生命周期   |
| vplugin                                 | Import a plugin to main.js or plugins file                                        | Vue 插件      |
| v3reactive-setup                        | Vue Composition API Script with Reactive                                          | Vue3 组合式API |
| v3useinoptions                          | Use Composition API within Options API                                            | Vue3 组合式API |
| vstore                                  | Base for Vuex store                                                               | Vuex 核心     |
| vgetter                                 | vuex getter                                                                       | Vuex 核心     |
| vmutation                               | vuex mutation                                                                     | Vuex 核心     |
| vaction                                 | vuex action                                                                       | Vuex 核心     |
| vstore-import                           | import vuex store into main.js                                                    | Vuex 导入     |
| vmodule                                 | vuex module                                                                       | Vuex 模块化    |
| vstore2                                 | vuex store 2                                                                      | Vuex 核心     |
| vfor                                    | vfor statement (HTML/Pug)                                                         | Vue 模板语法    |
| vmodel                                  | v-model directive (HTML/Pug)                                                      | Vue 模板语法    |
| vmodel-num                              | v-model directive number input (HTML/Pug)                                         | Vue 模板语法    |
| von                                     | v-on click handler with arguments (HTML/Pug)                                      | Vue 模板语法    |
| vel-props                               | component element with props (HTML/Pug)                                           | Vue 模板语法    |
| vslot-named                             | named slot                                                                        | Vue 模板语法    |
| vsrc                                    | image source binding (HTML/Pug)                                                   | Vue 模板语法    |
| vstyle                                  | vue inline style binding (HTML/Pug)                                               | Vue 模板语法    |
| vstyle-obj                              | vue inline style binding, objects (HTML/Pug)                                      | Vue 模板语法    |
| vclass                                  | vue class binding (HTML/Pug)                                                      | Vue 模板语法    |
| vclass-obj                              | vue class binding (HTML/Pug)                                                      | Vue 模板语法    |
| vclass-obj-mult                         | vue multiple conditional class bindings (HTML/Pug)                                | Vue 模板语法    |
| vemit-child                             | Vue Emit from Child Component (HTML/Pug)                                          | Vue 组件通信    |
| vemit-parent                            | Vue Emit to Parent Component (HTML/Pug)                                           | Vue 组件通信    |
| vanim                                   | transition component js hooks (HTML/Pug)                                          | Vue 过渡动画    |
| vnuxtl                                  | nuxt routing link (HTML/Pug)                                                      | Nuxt 路由     |
| vroutename                              | Named routing link                                                                | Vue 路由      |
| vroutenameparam                         | Named routing link w/ params                                                      | Vue 路由      |
| vroutepath                              | Path routing link                                                                 | Vue 路由      |
| nasyncdata                              | Nuxt asyncData                                                                    | Nuxt 数据请求   |
| nasyncdataaxios                         | Nuxt asyncData with Axios module                                                  | Nuxt 数据请求   |
| nfetch                                  | Nuxt Fetch                                                                        | Nuxt 数据请求   |
| nfetchaxios                             | Nuxt Fetch with Axios module                                                      | Nuxt 数据请求   |
| nparam                                  | Nuxt Route Params                                                                 | Nuxt 路由     |
| nhead                                   | Nuxt Head                                                                         | Nuxt 页面配置   |
| nfont                                   | link to include fonts in a nuxt project, in nuxt-config                           | Nuxt 配置     |
| ncss                                    | link to css assets such as normalize                                              | Nuxt 配置     |
| gitignore                               | gitignore file                                                                    | 通用配置        |
| vbase                                   | Base for Vue File with SCSS                                                       | Vue 单文件组件   |
| vbase-sass                              | Base for Vue File with PostCSS (SASS)                                             | Vue 单文件组件   |
| vbase-less                              | Base for Vue File with PostCSS (LESS)                                             | Vue 单文件组件   |
| vbase-pcss                              | Base for Vue File with PostCSS                                                    | Vue 单文件组件   |
| vbase-css                               | Base for Vue File with CSS                                                        | Vue 单文件组件   |
| vbase-styl                              | Base for Vue File with Stylus                                                     | Vue 单文件组件   |
| vbase-ts                                | Base for Vue File with Typescript                                                 | Vue 单文件组件   |
| vbase-ns                                | Base for Vue File with no styles                                                  | Vue 单文件组件   |
| vbase-3                                 | Base for Vue File Composition API with SCSS                                       | Vue3 单文件组件  |
| vbase-3-setup                           | Base for Vue File Setup Composition API with SCSS                                 | Vue3 单文件组件  |
| vbase-3-reactive                        | Base for Vue File Composition API with SCSS                                       | Vue3 单文件组件  |
| vbase-3-ts                              | Base for Vue File Composition API - Typescript                                    | Vue3 单文件组件  |
| vbase-3-ts-setup                        | Base for Vue File Setup Composition API - Typescript                              | Vue3 单文件组件  |
| vbase-ts-class                          | Base for Vue File with Class based Typescript format                              | Vue 单文件组件   |
| vbase-3-script-setup / vbase-3-ss       | Base for Vue Single File Component Script Setup (Composition API)                 | Vue3 单文件组件  |
| vbase-3-script-setup-ts / vbase-3-ss-ts | Base for Vue Single File Component Script Setup with TypeScript (Composition API) | Vue3 单文件组件  |
| vrouter                                 | Base for Vue Router                                                               | Vue 路由配置    |
| vscrollbehavior                         | Vue Router scrollBehavior                                                         | Vue 路由配置    |
| vbeforeeach                             | Vue Router global guards beforeEach                                               | Vue 路由守卫    |
| vbeforeresolve                          | Vue Router global guards beforeResolve                                            | Vue 路由守卫    |
| vaftereach                              | Vue Router global guards afterEach                                                | Vue 路由守卫    |
| vbeforeenter                            | Vue Router per-route guard beforeEnter                                            | Vue 路由守卫    |
| vbeforerouteenter                       | Vue Router component guards beforeRouteEnter                                      | Vue 路由守卫    |
| vbeforerouteupdate                      | Vue Router component guards beforeRouteUpdate                                     | Vue 路由守卫    |
| vbeforerouteleave                       | Vue Router component guards beforeRouteLeave                                      | Vue 路由守卫    |
| vroute-named                            | Vue Router route with per route code-splitting                                    | Vue 路由配置    |

### 补充说明
1. 部分快捷键（如 `vfor`/`vmodel`/`von` 等）同时支持 HTML 模板和 Pug 模板语法，表格中已标注 `(HTML/Pug)`
2. Vue3 相关指令均标注「Vue3 组合式API/生命周期/单文件组件」分类
3. 相同功能但不同语法的快捷键（如 `vbase` 系列）按样式预处理器/语法规范做了细分
4. 所有 Vuex 相关指令统一归类到「Vuex 核心/辅助函数/操作/模块化/导入」分类下
5. Nuxt 专属指令单独归类到「Nuxt 数据请求/路由/配置」分类
