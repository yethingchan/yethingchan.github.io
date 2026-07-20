# 15 · 前端-Vuex 状态管理

> 对应清单：进阶第 15 条（user/permission 协作）。
> 9 个 store module 是"全局共享内存"。本文以 `user` 为标本，其余列职责。

## 一、store/user.js —— 当前用户（真实）

```js
const user = {
  state: {
    token: getToken(),     // ① 初始从 cookie 读
    id: '', name: '', nickName: '', avatar: '',
    roles: [],          // ② 角色数组
    permissions: []       // ③ 权限字符串数组（和后端 LoginUser.permissions 对应）
  },
  mutations: {                    // 同步改 state
    SET_TOKEN: (s, t) => { s.token = t },
    SET_ROLES: (s, r) => { s.roles = r },
    SET_PERMISSIONS: (s, p) => { s.permissions = p }
  },
  actions: {                      // 异步（调 api）
    Login({ commit }, userInfo) {
      const username = userInfo.username.trim()
      const password = userInfo.password
      return new Promise((resolve, reject) => {
        login(username, password, userInfo.code, userInfo.uuid).then(res => {
          setToken(res.token)              // ④ 存 cookie
          commit('SET_TOKEN', res.token)
          store.dispatch('lock/unlockScreen')
          resolve()
        }).catch(reject)
      })
    },
    GetInfo({ commit, state }) {            // ⑤ 拉用户信息 + 权限
      return new Promise((resolve, reject) => {
        getInfo().then(res => {
          const user = res.user
          if (res.roles && res.roles.length > 0) {
            commit('SET_ROLES', res.roles)
            commit('SET_PERMISSIONS', res.permissions)  // ⑥ 权限存进来
          } else commit('SET_ROLES', ['ROLE_DEFAULT'])
          commit('SET_ID', user.userId); commit('SET_NAME', user.userName)
          commit('SET_AVATAR', user.avatar)
          cache.session.set('pwrChrtype', res.pwdChrtype)   // ⑦ 密码策略标记
          if (res.isDefaultModifyPwd) MessageBox.confirm('您的密码还是初始密码...')  // ⑧ 初始密码提示
          resolve(res)
        }).catch(reject)
      })
    },
    LogOut({ commit, state }) {
      return new Promise(resolve => {
        logout(state.token).then(() => {
          commit('SET_TOKEN', ''); commit('SET_ROLES', []); commit('SET_PERMISSIONS', [])
          removeToken(); resolve()
        })
      })
    }
  }
}
export default user
```

**逐行解释（这是"我是谁"的全生命周期）：**
1. **`token` 初始从 cookie 读**：`getToken()`（`plugins/auth.js`，封装 `js-cookie`）。刷新页面 token 不丢。
2. ③ **`permissions` 是前端权限判断的"真相源"**：`GetInfo` 把它从后端 `res.permissions` 存进来，随后 `v-hasPermi` / `hasPermiOr` 都读它。
3. ④ **登录成功**：`setToken(res.token)` 写 cookie，`commit('SET_TOKEN')` 写 state。之后 `request.js` 拦截器从 cookie 取 token 加请求头。
4. ⑤⑥ **`GetInfo` 是守卫里 `store.dispatch('GetInfo')` 调的**：拿回 `user/roles/permissions`，其中 `permissions` 决定**能看哪些菜单、哪些按钮**（见 [[14-前端-路由与权限守卫]]）。
5. ⑦⑧ **密码策略 UX**：后端返回 `isDefaultModifyPwd`/`isPasswordExpired`，前端弹窗逼用户改密码——安全细节。

## 二、其它 store module（知道存什么）

| module | 关键 state | 谁用 |
|---|---|---|
| `app` | `sidebar`(开合/是否手机)/`device`/`size` | Layout 侧栏开合、`cache`/移动端判断 |
| `permission` | `routes`/`addRoutes`/`sidebarRouters`/`topbarRouters` | [[14-前端-路由与权限守卫]] 的 `GenerateRoutes` 写这里，Sidebar 读 `sidebarRouters` 渲染菜单 |
| `settings` | `title`/`sideTheme`/`tagsView`/`fixedHeader`/`sidebarLogo`… | `settings.js` 的同名配置，布局设置抽屉改的就是它（见 [[18-前端-布局Layout]] / [[20-前端-字典主题与工具函数]]） |
| `tagsView` | 打开过的页签数组、缓存的 `visitedViews` | 顶部标签栏（可关闭/刷新/右键） |
| `dict` | 已加载的字典缓存 `dicts` | `getDicts('sys_normal_disable')` 写入，下拉/标签读（见 [[20-前端-字典主题与工具函数]]） |
| `lock` | `isLock`/`password` | 锁屏功能（守卫里 `isLock` 拦截跳 `/lock`） |

## 三、组件里怎么用

```js
// 读
this.$store.getters.permissions        // 权限数组
this.$store.getters.roles
this.$store.state.app.sidebar.opened
// 改（调 action）
this.$store.dispatch('user/LogOut')    // 退出
this.$store.dispatch('app/toggleSideBar') // 侧栏开合
this.$store.commit('SET_XXX', val)    // 直接改（少用，优先 action）
```

> Vue2 写法：`this.$store`。配合 `mapGetters`/`mapState` 可在组件 `computed` 里解构。Vuex 的**单一数据源**让"登录态、权限、菜单、布局"在任意组件都能取、改了全局响应。

## 四、和后端如何对应

| 前端 Vuex | 后端来源 |
|---|---|
| `user.token` | `TokenService.createToken` 发的 UUID-token |
| `user.permissions` | `SysPermissionService.getMenuPermission` 算的 `Set<String>` |
| `permission.sidebarRouters` | `SysMenuServiceImpl.buildMenus` 转的 `RouterVo` |
| `dict.dicts` | `sys_dict_type` / `sys_dict_data` 表 |

> 学完本章，你理解了"前端全局状态机"：**token/权限/路由/布局/字典都躺在 Vuex 里**，守卫拉一次、全应用共享。下一章看这些状态怎么被"请求"带着跑到后端。
