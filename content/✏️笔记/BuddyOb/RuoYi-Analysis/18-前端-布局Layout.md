# 18 · 前端-布局 Layout

> 对应清单：进阶第 18 条（Layout 组合）。
> `src/layout/index.vue` 是登录后所有页面的"外壳"：左侧菜单 + 顶部栏 + 标签栏 + 内容区 + 设置抽屉。

## 一、layout/index.vue —— 外壳（真实）

```vue
<template>
  <div :class="classObj" class="app-wrapper" :style="{'--current-color': theme, ...}">
    <div v-if="device==='mobile'&&sidebar.opened" class="drawer-bg" @click="handleClickOutside"/>
    <sidebar v-if="!sidebar.hide" class="sidebar-container"/>      <!-- ① 左侧菜单 -->
    <div :class="{hasTagsView:needTagsView,sidebarHide:sidebar.hide}" class="main-container">
      <div :class="{'fixed-header':fixedHeader}">
        <navbar @setLayout="setLayout"/>                              <!-- ② 顶部栏 -->
        <tags-view v-if="needTagsView"/>                              <!-- ③ 标签栏 -->
      </div>
      <app-main/>                                                  <!-- ④ 内容区（router-view） -->
      <settings ref="settingRef"/>                                     <!-- ⑤ 设置抽屉 -->
    </div>
  </div>
</template>
<script>
import { AppMain, Navbar, Settings, Sidebar, TagsView } from './components'
import ResizeMixin from './mixin/ResizeHandler'
import { mapState } from 'vuex'
export default {
  name: 'Layout',
  components: { AppMain, Navbar, Settings, Sidebar, TagsView },
  mixins: [ResizeMixin],
  computed: {
    ...mapState({                                  // ⑥ 从 Vuex 取布局状态
      theme: s => s.settings.theme,
      sidebar: s => s.app.sidebar,
      device: s => s.app.device,
      needTagsView: s => s.settings.tagsView,
      fixedHeader: s => s.settings.fixedHeader
    })
  },
  methods: {
    handleClickOutside() { this.$store.dispatch('app/closeSideBar', { withoutAnimation: false }) },
    setLayout() { this.$refs.settingRef.openSetting() }   // ⑦ 顶栏齿轮开设置
  }
}
</script>
```

**逐行解释（理解它，就理解了"页面外壳"）：**
1. **`sidebar`**：左侧菜单，数据来自 `store/permission.sidebarRouters`（就是 [[14-前端-路由与权限守卫]] 里 `GenerateRoutes` 生成的菜单路由）。菜单项 = 可访问的路由。
2. **`navbar`**：顶部栏——汉堡按钮（开合侧栏）、面包屑、全屏、尺寸、主题、用户下拉（个人中心/改密码/退出）。
3. **`tags-view`**：顶部的"页签"（你开过的页面变成可点标签），数据在 `store/tagsView`。可关/可刷新/可右键关其它。
4. **`app-main`**：核心 —— 里面就一个 `<router-view/>`，**当前路由的页面渲染在这**。
5. **`settings`**：右下角"设置"抽屉（齿轮触发 `setLayout`），改主题色/标签栏/固定头/Logo 等，写 `store/settings`（见 [[20-前端-字典主题与工具函数]]）。
6. ⑥ **`mapState`**：布局随 Vuex 状态变——侧栏开合、是否手机端、要不要标签栏，全从 `app`/`settings` module 读。
7. ⑦ 顶栏齿轮 → 打开设置抽屉，**改的设置实时反映到整个外壳**（因为都绑 Vuex）。

## 二、Layout 的子组件（知道干什么）

| 子组件 | 路径 | 职责 |
|---|---|---|
| `Sidebar/index` | `layout/components/Sidebar` | 递归渲染 `sidebarRouters` 成可折叠菜单；含 Logo（`settings.sidebarLogo`） |
| `Sidebar/Logo` | — | 左上角项目名+图标 |
| `Sidebar/Link` | — | 菜单项是外链时开新窗口，内链走 router |
| `Navbar` | `layout/components/Navbar` | 汉堡/面包屑/全屏/尺寸/主题/用户下拉 |
| `TagsView` | `layout/components/TagsView` | 页签栏（含 `ScrollPane` 横向滚动、`ContextMenu` 右键） |
| `AppMain` | `layout/components/AppMain` | `<router-view>` + `<keep-alive>`（按 `meta.noCache` 决定缓存） |
| `Settings` | `layout/components/Settings` | 布局设置抽屉（主题色/导航模式/标签栏/固定头/Logo…） |
| `mixin/ResizeHandler` | `layout/mixin` | 监听窗口尺寸，窄屏自动切"手机模式"（侧栏变抽屉） |
| `components/TopBar` `TopNav` | — | `settings.navType=2/3`（混合/顶部导航）时启用 |
| `components/InnerLink` | — | 菜单 `link` 外链用 `<iframe>` 内嵌（对应后端 `RouterVo.meta.link`） |

## 三、为什么"改一个设置全站变"

```
Settings 抽屉改 theme
   → commit('SET_THEME') 写 store/settings
   → layout/index.vue 的 ...mapState(theme) 重算
   → :style="{'--current-color': theme}" 改 CSS 变量
   → 所有用该变量的组件（菜单/按钮/标签）瞬间换色
```
- 这是 **CSS 变量 + Vuex** 的组合：主题色只是个 CSS 自定义属性，谁引用谁变。

## 四、和路由/权限的关系（串联回顾）

```
登录 → permission.js 守卫 GetInfo → store/permission.GenerateRoutes
   → sidebarRouters = 后端菜单转的路由
   → Layout 的 <sidebar/> 渲染这些路由成菜单
   → 点菜单 → router 跳 → <app-main/> 的 <router-view/> 显示对应视图
```
> 整个前台导航，从源头（后端菜单）到末端（`<router-view/>` 显示页面）是一条链。学完本章，你理解了"登录后看到的那个后台界面"是怎么拼出来的。下一章进"视图与 API"——业务页面本身。
