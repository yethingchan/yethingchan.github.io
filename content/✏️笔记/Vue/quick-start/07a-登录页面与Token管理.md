# 第七章（上）：登录页面与 Token 管理

> 本章目标：实现完整的登录认证体系，包括登录页面、Token 管理、路由守卫拦截、动态菜单权限、按钮级权限指令。这是后台管理系统最核心的安全模块，所有业务功能都建立在登录与权限之上。

---

## 本章概览

| 模块 | 文件路径 | 核心职责 |
|------|----------|----------|
| 登录页面 | `src/views/login/index.vue` | 登录表单、校验、记住密码、加载状态 |
| 用户接口 | `src/api/user.js` | login / getUserInfo / logout |
| 用户状态 | `src/store/modules/user.js` | Token、角色、登录/登出逻辑 |
| 路由守卫 | `src/permission.js` | 登录拦截、动态路由注册 |
| 权限路由 | `src/store/modules/permission.js` | 根据角色过滤动态路由 |
| Token 管理 | `src/utils/auth.js` | Token 读写与清除 |
| 请求封装 | `src/utils/request.js` | Axios 拦截器、Token 自动附加 |
| 权限指令 | `src/directives/permission.js` | v-permission 按钮级权限 |
| 权限 Hook | `src/hooks/usePermission.js` | 权限判断组合式函数 |

> **前置依赖**：本章依赖第二章的工具类封装和第三章的路由与布局体系。如果尚未完成这两章，请先创建对应的文件。

---

## 一、登录页面 `src/views/login/index.vue`

### 1.1 完整代码

```vue
<!-- src/views/login/index.vue -->
<template>
  <div class="login-container">
    <!-- 背景装饰 -->
    <div class="login-bg">
      <div class="bg-shape bg-shape-1"></div>
      <div class="bg-shape bg-shape-2"></div>
      <div class="bg-shape bg-shape-3"></div>
    </div>

    <!-- 登录卡片 -->
    <div class="login-card">
      <!-- 系统标题 -->
      <div class="login-header">
        <h2 class="login-title">Vue Admin</h2>
        <p class="login-subtitle">后台管理系统</p>
      </div>

      <!-- 登录表单 -->
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        size="large"
      >
        <!-- 用户名 -->
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            clearable
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <!-- 密码 -->
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
            clearable
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <!-- 记住密码 -->
        <el-form-item>
          <div class="login-options">
            <el-checkbox v-model="loginForm.rememberMe">
              记住密码
            </el-checkbox>
          </div>
        </el-form-item>

        <!-- 登录按钮 -->
        <el-form-item>
          <el-button
            type="primary"
            class="login-btn"
            :loading="loginLoading"
            @click="handleLogin"
          >
            {{ loginLoading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 底部信息 -->
      <div class="login-footer">
        <span>默认账号：admin / 123456</span>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 登录页面
 *
 * 职责：
 * 1. 用户名/密码输入与表单校验
 * 2. 记住密码功能（localStorage）
 * 3. 调用登录接口，存储 Token
 * 4. 跳转到 redirect 指定的页面或首页
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// ==================== 表单相关 ====================

/** 登录表单引用（用于调用 validate / resetFields） */
const loginFormRef = ref(null)

/** 登录加载状态 */
const loginLoading = ref(false)

/** 登录表单数据 */
const loginForm = reactive({
  username: '',
  password: '',
  rememberMe: false
})

/** 表单校验规则 */
const loginRules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度为 2~20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6~20 个字符', trigger: 'blur' }
  ]
})

// ==================== 记住密码 ====================

/** 记住密码在 localStorage 中的键名 */
const REMEMBER_KEY = 'login_remember'

/**
 * 从 localStorage 读取记住的用户名和密码
 */
const loadRememberedCredentials = () => {
  const remembered = localStorage.getItem(REMEMBER_KEY)
  if (remembered) {
    try {
      const { username, password } = JSON.parse(remembered)
      loginForm.username = username || ''
      loginForm.password = password || ''
      loginForm.rememberMe = true
    } catch (e) {
      // 数据格式异常，忽略
      localStorage.removeItem(REMEMBER_KEY)
    }
  }
}

/**
 * 保存或清除记住的凭据
 * @param {string} username
 * @param {string} password
 */
const handleRememberCredentials = (username, password) => {
  if (loginForm.rememberMe) {
    localStorage.setItem(REMEMBER_KEY, JSON.stringify({ username, password }))
  } else {
    localStorage.removeItem(REMEMBER_KEY)
  }
}

// ==================== 登录逻辑 ====================

/**
 * 处理登录
 * 流程：表单校验 → 调用 login API → 存储 Token → 获取用户信息 →
 *       存储用户角色 → 生成动态路由 → 跳转首页
 */
const handleLogin = () => {
  loginFormRef.value.validate(async (valid) => {
    if (!valid) return

    loginLoading.value = true

    try {
      // 1. 调用登录接口
      await userStore.login({
        username: loginForm.username,
        password: loginForm.password
      })

      // 2. 处理记住密码
      handleRememberCredentials(loginForm.username, loginForm.password)

      // 3. 获取用户信息（触发路由守卫中的动态路由生成）
      await userStore.getUserInfo()

      // 4. 跳转到 redirect 指定的页面或首页
      const redirect = route.query.redirect || '/'
      router.push(redirect)

      ElMessage.success('登录成功')
    } catch (error) {
      ElMessage.error(error.message || '登录失败，请检查用户名和密码')
    } finally {
      loginLoading.value = false
    }
  })
}

// ==================== 生命周期 ====================

onMounted(() => {
  // 页面加载时读取记住的凭据
  loadRememberedCredentials()
})
</script>

<style lang="scss" scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

// ==================== 背景装饰 ====================
.login-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;

  .bg-shape {
    position: absolute;
    border-radius: 50%;
    opacity: 0.1;
    background-color: #fff;
  }

  .bg-shape-1 {
    width: 400px;
    height: 400px;
    top: -100px;
    right: -100px;
    animation: float 6s ease-in-out infinite;
  }

  .bg-shape-2 {
    width: 300px;
    height: 300px;
    bottom: -80px;
    left: -80px;
    animation: float 8s ease-in-out infinite reverse;
  }

  .bg-shape-3 {
    width: 200px;
    height: 200px;
    top: 50%;
    left: 10%;
    animation: float 10s ease-in-out infinite;
  }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

// ==================== 登录卡片 ====================
.login-card {
  width: 420px;
  padding: 40px 36px 24px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(10px);
  position: relative;
  z-index: 1;
}

// ==================== 标题区域 ====================
.login-header {
  text-align: center;
  margin-bottom: 32px;

  .login-title {
    font-size: 28px;
    font-weight: 700;
    color: #303133;
    margin: 0 0 8px;
    letter-spacing: 2px;
  }

  .login-subtitle {
    font-size: 14px;
    color: #909399;
    margin: 0;
  }
}

// ==================== 表单 ====================
.login-form {
  .el-form-item {
    margin-bottom: 22px;
  }

  // 记住密码行
  .login-options {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;

    :deep(.el-checkbox__label) {
      font-size: 14px;
      color: #606266;
    }
  }

  // 登录按钮
  .login-btn {
    width: 100%;
    height: 44px;
    font-size: 16px;
    font-weight: 600;
    letter-spacing: 4px;
    border-radius: 8px;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
    }

    &:active {
      transform: translateY(0);
    }
  }
}

// ==================== 底部信息 ====================
.login-footer {
  text-align: center;
  margin-top: 16px;

  span {
    font-size: 12px;
    color: #c0c4cc;
  }
}

// ==================== 响应式 ====================
@media screen and (max-width: 480px) {
  .login-card {
    width: 90%;
    padding: 32px 24px 20px;
    margin: 0 16px;

    .login-header .login-title {
      font-size: 24px;
    }
  }
}
</style>
```

### 1.2 关键要点

**1. 表单校验**

`el-form` 的 `rules` 定义在 `loginRules` 中，通过 `loginFormRef.value.validate()` 手动触发校验。校验通过后才会执行登录逻辑。校验规则说明：

| 字段 | 规则 | 说明 |
|------|------|------|
| `username` | `required` | 必填，不能为空 |
| `username` | `min: 2, max: 20` | 长度 2~20 个字符 |
| `password` | `required` | 必填，不能为空 |
| `password` | `min: 6, max: 20` | 长度 6~20 个字符 |

**2. 记住密码**

使用 `localStorage` 存储用户名和密码（JSON 格式）。页面加载时自动读取填充，登录成功后根据勾选状态决定是否保存。**注意**：生产环境中密码不应明文存储，建议加密后再存储，或仅记住用户名。

**3. 登录按钮加载状态**

`loginLoading` 控制按钮的 `loading` 属性，防止用户重复点击。`finally` 块确保无论成功还是失败都会关闭 loading。

**4. 路由跳转**

登录成功后读取 `route.query.redirect`，跳转到用户原来想访问的页面。如果没有 redirect 参数，默认跳转首页 `/`。

---

## 二、用户 API 接口 `src/api/user.js`

> **与第3章的关系**：该文件在第3章（路由与布局体系）中已首次创建。本章在此完整列出是为了展示登录认证流程的完整性。如果你已经在第3章创建过此文件，无需重复创建，代码内容一致。

### 2.1 完整代码

```js
// src/api/user.js
import request from '@/utils/request'

/**
 * 用户登录
 *
 * @param {Object} data - 登录参数
 * @param {string} data.username - 用户名
 * @param {string} data.password - 密码
 * @returns {Promise<{code: number, message: string, data: {token: string}}>}
 *
 * 接口说明：
 * - POST 请求
 * - 请求体：{ username: 'admin', password: '123456' }
 * - 成功响应：{ code: 200, data: { token: 'eyJhbGciOi...' } }
 * - 失败响应：{ code: 401, message: '用户名或密码错误' }
 */
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

/**
 * 获取当前登录用户信息
 *
 * @returns {Promise<{code: number, data: {name: string, avatar: string, roles: string[]}}>}
 *
 * 接口说明：
 * - GET 请求
 * - 需要在请求头中携带 Token（Authorization: Bearer xxx）
 * - 成功响应：
 *   {
 *     code: 200,
 *     data: {
 *       name: '系统管理员',
 *       avatar: 'https://xxx.com/avatar.png',
 *       roles: ['admin']
 *     }
 *   }
 * - roles 字段是用户角色列表，用于权限判断
 * - 常见角色：admin（超级管理员）、editor（编辑者）、viewer（查看者）
 *
 * 错误场景：
 * - Token 过期或无效 → 返回 401，前端会自动跳转登录页
 * - Token 未携带 → 返回 401
 */
export function getUserInfo() {
  return request({
    url: '/auth/userinfo',
    method: 'get'
  })
}

/**
 * 退出登录
 *
 * @returns {Promise<{code: number, message: string}>}
 *
 * 接口说明：
 * - POST 请求
 * - 服务端清除该用户的登录状态（如 JWT 黑名单、Session 销毁）
 * - 成功响应：{ code: 200, message: '退出成功' }
 * - 即使接口调用失败，前端也会清除本地 Token
 */
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}
```

### 2.2 接口汇总

| 接口 | 方法 | 路径 | 参数 | 返回值 | 说明 |
|------|------|------|------|--------|------|
| 登录 | POST | `/auth/login` | `{ username, password }` | `{ token }` | 返回认证 Token |
| 用户信息 | GET | `/auth/userinfo` | 无（Header 带 Token） | `{ name, avatar, roles }` | 返回当前用户信息与角色 |
| 退出 | POST | `/auth/logout` | 无 | `{ code, message }` | 注销当前会话 |

---

## 三、登录流程（完整流程图）

### 3.1 流程图

```
用户输入用户名和密码
        │
        ▼
  ┌─────────────┐
  │  点击登录按钮  │
  └──────┬──────┘
         │
         ▼
  ┌──────────────┐     校验失败
  │  el-form 表单  │──────────→ 显示校验错误提示
  │    校验        │             (用户名/密码不能为空)
  └──────┬───────┘
         │ 校验通过
         ▼
  ┌──────────────┐     失败
  │  调用 login   │──────────→ 提示"用户名或密码错误"
  │    API        │
  └──────┬───────┘
         │ 成功，返回 token
         ▼
  ┌──────────────┐
  │  setToken()   │  ←── 将 token 存入 localStorage
  │  存储到本地    │  ←── userStore.token = data.token
  └──────┬───────┘
         │
         ▼
  ┌──────────────┐     失败（Token 无效）
  │ 获取用户信息   │──────────→ 清除 Token → 跳转登录页
  │ getUserInfo() │
  └──────┬───────┘
         │ 成功，返回 roles / name / avatar
         ▼
  ┌──────────────┐
  │ 存储用户信息   │  ←── userStore.roles = data.roles
  │ 到 Pinia      │  ←── userStore.name = data.name
  │               │  ←── userStore.avatar = data.avatar
  └──────┬───────┘
         │
         ▼
  ┌──────────────┐
  │ 根据角色过滤   │  ←── permissionStore.generateRoutes(roles)
  │ 动态路由       │  ←── admin → 全部路由
  │               │  ←── editor → 过滤后的路由
  └──────┬───────┘
         │
         ▼
  ┌──────────────┐
  │ router.addRoute │  ←── 动态添加过滤后的路由到 router
  │ 注册动态路由    │
  └──────┬───────┘
         │
         ▼
  ┌──────────────┐
  │  跳转到首页   │  ←── 如果有 redirect 则跳转到目标页面
  │  或目标页面    │  ←── router.push(redirect || '/')
  └──────────────┘
```

### 3.2 各步骤对应代码

| 步骤 | 文件 | 关键代码 |
|------|------|----------|
| 表单校验 | `login/index.vue` | `loginFormRef.value.validate()` |
| 调用 login API | `store/modules/user.js` | `userStore.login(loginForm)` |
| 存储 Token | `store/modules/user.js` | `setToken(data.token)` |
| 获取用户信息 | `store/modules/user.js` | `userStore.getUserInfo()` |
| 生成动态路由 | `store/modules/permission.js` | `permissionStore.generateRoutes(roles)` |
| 注册路由 | `permission.js` | `router.addRoute(route)` |
| 跳转页面 | `login/index.vue` | `router.push(redirect \|\| '/')` |

---

## 四、Token 管理机制

> **与第2章、第3章的关系**：`auth.js` 在第2章（工具类封装）中已完整实现，第3章也列出了同一文件。本章在此再次列出是为了 Token 管理章节的完整性。如果你已经在第2章创建过此文件，无需重复创建。

### 4.1 Token 存储工具 `src/utils/auth.js`

```js
// src/utils/auth.js
import { getStorage, setStorage, removeStorage } from '@/utils/storage'

/** Token 在 localStorage 中存储的键名 */
const TOKEN_KEY = 'access_token'

/** Token 默认过期时间：7 天（单位：秒） */
const TOKEN_EXPIRE = 7 * 24 * 60 * 60

/**
 * 获取 Token
 * @returns {string|null}
 */
export function getToken() {
  return getStorage(TOKEN_KEY)
}

/**
 * 设置 Token
 * @param {string} token  - Token 字符串
 * @param {number} expire - 过期时间（秒），默认 7 天
 */
export function setToken(token, expire = TOKEN_EXPIRE) {
  setStorage(TOKEN_KEY, token, expire || null)
}

/**
 * 移除 Token
 */
export function removeToken() {
  removeStorage(TOKEN_KEY)
}
```

> 代码详见第二章工具类封装。此处仅做说明。

### 4.2 Token 自动附加到请求头

Token 的自动附加在 Axios 请求拦截器中完成（`src/utils/request.js`）：

```js
// src/utils/request.js - 请求拦截器部分
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      // 自动将 Token 附加到请求头，格式为 Bearer Token
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)
```

**工作原理**：每次发起 HTTP 请求时，拦截器自动从 localStorage 中读取 Token，并以 `Authorization: Bearer <token>` 格式注入请求头。业务代码无需关心 Token 的传递。

### 4.3 Token 过期处理（401 处理）

当 Token 过期时，服务端返回 401 状态码。响应拦截器捕获后执行以下逻辑：

```js
// src/utils/request.js - 响应拦截器部分（401 处理）
function handleTokenExpired() {
  // 1. 清除本地 Token
  removeToken()

  // 2. 弹出提示框
  ElMessageBox.confirm(
    '登录状态已过期，请重新登录',
    '提示',
    {
      confirmButtonText: '重新登录',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    // 3. 跳转到登录页
    window.location.href = `${window.location.origin}/#/login`
  }).catch(() => {
    // 用户点击取消
  })
}

// HTTP 错误处理
function handleHttpError(status, message) {
  if (status === 401) {
    handleTokenExpired()
    return
  }
  // ...其他错误码处理
}
```

### 4.4 退出登录清除流程

退出登录时需要清理所有认证相关的状态，由 `userStore.resetState()` 统一处理：

```js
// src/store/modules/user.js - resetState 方法
resetState() {
  // 1. 清空 store 中的 Token
  this.token = ''
  // 2. 清空用户角色
  this.roles = []
  // 3. 清空用户信息
  this.name = ''
  this.avatar = ''
  // 4. 清除 localStorage 中的 Token
  removeToken()
}
```

**完整的退出登录流程**：

```
用户点击退出
    │
    ▼
ElMessageBox.confirm（二次确认）
    │
    ├── 取消 → 不做任何操作
    │
    └── 确定
        │
        ▼
    调用 logout API（通知服务端销毁会话）
        │
        ▼ （无论成功与否）
    resetState()
        │
        ├── token = ''
        ├── roles = []
        ├── name = ''
        ├── avatar = ''
        └── removeToken()
            │
            ▼
    跳转到登录页
    router.push('/login?redirect=当前路径')
```

### 4.5 Token 生命周期管理汇总

| 场景 | 操作 | 触发位置 |
|------|------|----------|
| 登录成功 | `setToken(token)` | `userStore.login()` |
| 每次请求 | `getToken()` + 注入 Header | `request.js` 请求拦截器 |
| 页面刷新 | `getToken()` 恢复登录状态 | `user.js` store 初始化 |
| Token 过期(401) | `removeToken()` + 跳转登录 | `request.js` 响应拦截器 |
| 退出登录 | `removeToken()` + 清空状态 | `userStore.resetState()` |
| 路由守卫检查 | `getToken()` 判断是否登录 | `permission.js` |

---

> **上一章**：[第6章 树形与多级列表](./06-树形与多级列表.md)
>
> **下一篇**：[第7章（下）菜单权限与按钮权限](./07b-菜单权限与按钮权限.md)
>
> **章节总览**：[第7章 登录认证与权限](./07-登录认证与权限.md)
