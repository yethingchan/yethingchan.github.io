# 第十章：Mock 数据与后端对接

在前端开发过程中，后端接口往往不能同步就绪。如果前端必须等到后端开发完毕才能开始工作，整个项目周期将被大幅拉长。**Mock 数据**（模拟数据）就是解决这一问题的关键手段——它让前端可以在没有真实后端的情况下，使用"假的"但结构真实的数据进行开发、调试和测试。

本章将系统讲解：

- 为什么需要 Mock 数据，以及常见的 Mock 方案
- 如何使用 `vite-plugin-mock` 搭建完整的 Mock 服务
- 编写贴近真实场景的 Mock 接口（登录、CRUD、分页、树形结构）
- 从 Mock 平滑过渡到真实后端联调
- 跨域问题的成因与解决方案
- 前后端 API 接口规范建议

---

## 一、为什么需要 Mock 数据

### 1.1 前后端并行开发

在现代前后端分离的架构中，前端和后端通常是**并行开发**的。产品需求确定后，双方约定好接口文档（URL、请求参数、返回格式），然后各自独立开发。

如果没有 Mock 数据，前端开发者将面临以下困境：

- 后端接口没写好，页面拿不到数据，无法渲染
- 后端接口出了 Bug，前端开发也被阻塞
- 本地无法启动完整环境，每次调试都要连测试服务器

有了 Mock 数据，前端只需要按照约定好的接口格式，在本地模拟一份"假数据"，就可以独立完成页面开发、交互调试和边界测试。

### 1.2 快速原型验证

在项目初期或做 Demo 演示时，搭建一整套后端服务成本很高。Mock 数据可以让你在**几分钟内**拥有完整的增删改查能力，快速验证产品设计是否合理。

### 1.3 常见 Mock 方案对比

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| **Mock.js** | 轻量、随机数据生成能力强 | 需要手动拦截 Ajax，不够优雅 | 老项目、jQuery 项目 |
| **json-server** | 零代码、启动快 | 只支持 RESTful，无法模拟复杂逻辑 | 简单原型、Demo |
| **vite-plugin-mock** | 与 Vite 深度集成，支持热更新 | 仅适用于 Vite 项目 | Vite + Vue 3 项目 |
| **Apifox / YApi** | 在线协作、自动生成 Mock | 依赖网络，免费额度有限 | 团队协作、大型项目 |

本教程选择 **vite-plugin-mock**，因为它与我们的 Vite 项目无缝集成，配置简单，支持请求模拟、延迟、状态码控制等高级功能。

---

## 二、使用 vite-plugin-mock

### 2.1 安装依赖

```bash
npm install vite-plugin-mock mockjs -D
```

- `vite-plugin-mock`：Vite 的 Mock 插件，负责拦截请求并返回 Mock 数据
- `mockjs`：数据生成库，可以生成随机的姓名、手机号、日期等仿真数据

### 2.2 配置 vite.config.js

在项目根目录的 `vite.config.js` 中添加 Mock 插件：

```js
// vite.config.js
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { viteMockServe } from 'vite-plugin-mock'
import path from 'path'

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd())

  return {
    plugins: [
      vue(),
      // Mock 插件配置
      viteMockServe({
        // Mock 文件所在目录（相对于项目根目录）
        mockPath: 'mock',
        // 是否启用 Mock（仅在开发环境启用）
        enable: mode === 'development',
        // 是否在控制台输出 Mock 请求日志
        logger: true,
        // 是否监听 Mock 文件变化并热更新
        watchFiles: true,
      }),
    ],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src'),
      },
    },
    server: {
      port: 3000,
      open: true,
    },
  }
})
```

> **提示**：`enable: mode === 'development'` 确保 Mock 只在开发环境生效。打包上线时，Mock 代码不会被包含进产物。

### 2.3 创建 Mock 目录结构

在项目根目录下创建 `mock/` 文件夹，按照业务模块组织 Mock 文件：

```
项目根目录/
├── mock/
│   ├── user.js              # 用户登录/信息相关 Mock
│   └── system/
│       ├── user.js          # 系统管理 - 用户管理 CRUD
│       ├── dept.js          # 系统管理 - 部门管理（树形）
│       └── role.js          # 系统管理 - 角色管理
├── src/
├── vite.config.js
└── package.json
```

> **约定**：Mock 文件的命名和路径尽量与接口路径保持一致，方便查找和维护。例如 `/api/system/user/list` 对应的 Mock 文件就是 `mock/system/user.js`。

### 2.4 Mock 文件基本格式

每个 Mock 文件需要**默认导出一个数组**，数组中的每个对象代表一个 Mock 接口：

```js
// mock/demo.js
export default [
  {
    url: '/api/demo/hello',    // 请求路径
    method: 'get',             // 请求方法
    response: () => {          // 响应数据
      return {
        code: 200,
        data: 'Hello Mock!',
        message: 'success',
      }
    },
  },
  {
    url: '/api/demo/list',
    method: 'post',
    // 可以接收请求参数
    response: ({ body }) => {
      const { pageNum, pageSize } = body
      return {
        code: 200,
        data: {
          list: [],
          total: 0,
        },
        message: 'success',
      }
    },
  },
]
```

### 2.5 认识 Mock.Random

`mockjs` 提供了强大的 `Mock.Random` 工具，可以生成各种仿真数据：

```js
import { Random } from 'mockjs'

// 姓名
Random.cname()           // "张三"、"李四"

// 手机号
Random.string('number', 11)  // "13812345678"
// 更好的写法（符合真实手机号格式）
'1' + [3, 5, 7, 8, 9][Math.floor(Math.random() * 5)] + Random.string('number', 9)

// 邮箱
Random.email()           // "zhangsan@example.com"

// 日期
Random.date('yyyy-MM-dd')      // "2024-03-15"
Random.datetime('yyyy-MM-dd HH:mm:ss')  // "2024-03-15 14:30:00"

// ID
Random.id()              // 随机的 18 位 ID 字符串

// 图片占位
Random.image('200x100')  // 生成占位图 URL

// 自然数
Random.natural(1, 100)   // 1~100 之间的随机整数

// 从数组中随机取值
Random.pick(['启用', '禁用'])  // "启用" 或 "禁用"
```

> **注意**：Mock.Random 生成的是随机数据，每次请求结果可能不同。如果需要固定的测试数据，建议手动构造。

---

## 三、Mock 数据完整示例

下面给出完整的 Mock 文件示例，覆盖登录、用户 CRUD、部门树等核心接口。

### 3.1 用户登录 Mock

```js
// mock/user.js
import { Random } from 'mockjs'

export default [
  // 登录接口
  {
    url: '/api/auth/login',
    method: 'post',
    response: ({ body }) => {
      const { username, password } = body

      // 模拟用户数据库
      const users = {
        admin: { password: '123456', role: 'admin' },
        editor: { password: '123456', role: 'editor' },
        user: { password: '123456', role: 'user' },
      }

      const user = users[username]

      if (!user || user.password !== password) {
        return {
          code: 401,
          data: null,
          message: '用户名或密码错误',
        }
      }

      // 生成模拟 token
      const token = `token_${username}_${Date.now()}`

      return {
        code: 200,
        data: {
          token,
          username,
          role: user.role,
        },
        message: '登录成功',
      }
    },
  },

  // 获取用户信息
  {
    url: '/api/auth/info',
    method: 'get',
    response: ({ headers }) => {
      // 从 token 中解析用户信息
      const token = headers.authorization || ''
      const username = token.replace('Bearer token_', '').split('_')[0]

      const userInfo = {
        admin: {
          userId: 1,
          username: 'admin',
          nickname: '超级管理员',
          avatar: 'https://github.com/shengxinjing.png',
          roles: ['admin'],
          permissions: ['*'],
        },
        editor: {
          userId: 2,
          username: 'editor',
          nickname: '编辑员',
          avatar: 'https://github.com/shengxinjing.png',
          roles: ['editor'],
          permissions: ['system:user:query', 'system:user:add', 'system:user:edit'],
        },
        user: {
          userId: 3,
          username: 'user',
          nickname: '普通用户',
          avatar: 'https://github.com/shengxinjing.png',
          roles: ['user'],
          permissions: ['system:user:query'],
        },
      }

      const info = userInfo[username]

      if (!info) {
        return {
          code: 401,
          data: null,
          message: 'Token 无效或已过期',
        }
      }

      return {
        code: 200,
        data: info,
        message: 'success',
      }
    },
  },

  // 退出登录
  {
    url: '/api/auth/logout',
    method: 'post',
    response: () => {
      return {
        code: 200,
        data: null,
        message: '退出成功',
      }
    },
  },
]
```

### 3.2 用户管理 CRUD Mock

```js
// mock/system/user.js
import { Random } from 'mockjs'

// 模拟用户数据库（内存中存储）
const userList = []
for (let i = 1; i <= 86; i++) {
  userList.push({
    id: i,
    username: `user${String(i).padStart(3, '0')}`,
    nickname: Random.cname(),
    phone: '1' + [3, 5, 7, 8, 9][i % 5] + Random.string('number', 9),
    email: Random.email(),
    deptId: Random.pick([1, 2, 3, 4, 5]),
    deptName: Random.pick(['研发部', '产品部', '市场部', '人事部', '财务部']),
    status: Random.pick([0, 1]),  // 0-启用 1-禁用
    remark: Random.paragraph(),
    createTime: Random.date('yyyy-MM-dd HH:mm:ss'),
  })
}

export default [
  // 获取用户列表（分页 + 搜索）
  {
    url: '/api/system/user/list',
    method: 'get',
    response: ({ query }) => {
      const {
        pageNum = 1,
        pageSize = 10,
        username,
        phone,
        status,
        deptId,
      } = query

      // 过滤数据
      let filteredList = [...userList]

      if (username) {
        filteredList = filteredList.filter((item) =>
          item.username.includes(username) || item.nickname.includes(username)
        )
      }
      if (phone) {
        filteredList = filteredList.filter((item) => item.phone.includes(phone))
      }
      if (status !== undefined && status !== '' && status !== null) {
        filteredList = filteredList.filter((item) => item.status === Number(status))
      }
      if (deptId) {
        filteredList = filteredList.filter((item) => item.deptId === Number(deptId))
      }

      // 分页
      const total = filteredList.length
      const start = (Number(pageNum) - 1) * Number(pageSize)
      const end = start + Number(pageSize)
      const list = filteredList.slice(start, end)

      return {
        code: 200,
        data: {
          list,
          total,
        },
        message: 'success',
      }
    },
  },

  // 新增用户
  {
    url: '/api/system/user',
    method: 'post',
    response: ({ body }) => {
      const { username, nickname, phone, email, deptId, deptName, status, remark } = body

      // 检查用户名是否重复
      if (userList.find((item) => item.username === username)) {
        return {
          code: 500,
          data: null,
          message: '用户名已存在',
        }
      }

      const newUser = {
        id: userList.length > 0 ? Math.max(...userList.map((u) => u.id)) + 1 : 1,
        username,
        nickname,
        phone,
        email,
        deptId,
        deptName,
        status: status ?? 0,
        remark: remark || '',
        createTime: new Date().toISOString().replace('T', ' ').slice(0, 19),
      }

      userList.unshift(newUser)

      return {
        code: 200,
        data: newUser,
        message: '新增成功',
      }
    },
  },

  // 修改用户
  {
    url: '/api/system/user',
    method: 'put',
    response: ({ body }) => {
      const { id, ...updateData } = body
      const index = userList.findIndex((item) => item.id === id)

      if (index === -1) {
        return {
          code: 404,
          data: null,
          message: '用户不存在',
        }
      }

      userList[index] = { ...userList[index], ...updateData }

      return {
        code: 200,
        data: userList[index],
        message: '修改成功',
      }
    },
  },

  // 删除用户（支持批量删除）
  {
    url: '/api/system/user/:id',
    method: 'delete',
    response: ({ query }) => {
      const { id } = query
      const ids = id.split(',').map(Number)

      const deletedCount = ids.filter((delId) => {
        const index = userList.findIndex((item) => item.id === delId)
        if (index !== -1) {
          userList.splice(index, 1)
          return true
        }
        return false
      }).length

      return {
        code: 200,
        data: { deleted: deletedCount },
        message: `成功删除 ${deletedCount} 条记录`,
      }
    },
  },

  // 修改用户状态
  {
    url: '/api/system/user/changeStatus',
    method: 'put',
    response: ({ body }) => {
      const { id, status } = body
      const user = userList.find((item) => item.id === id)

      if (!user) {
        return {
          code: 404,
          data: null,
          message: '用户不存在',
        }
      }

      user.status = status

      return {
        code: 200,
        data: null,
        message: '状态修改成功',
      }
    },
  },

  // 导出用户（模拟）
  {
    url: '/api/system/user/export',
    method: 'post',
    response: ({ body }) => {
      return {
        code: 200,
        data: {
          url: '/mock-export/users.xlsx',
          filename: `用户数据_${new Date().toLocaleDateString()}.xlsx`,
        },
        message: '导出成功',
      }
    },
  },
]
```

### 3.3 部门树 Mock

```js
// mock/system/dept.js
import { Random } from 'mockjs'

// 部门树数据
const deptTree = [
  {
    id: 1,
    parentId: 0,
    deptName: '总公司',
    orderNum: 0,
    leader: Random.cname(),
    phone: '15888888888',
    email: Random.email(),
    status: 0,
    children: [
      {
        id: 2,
        parentId: 1,
        deptName: '研发部',
        orderNum: 1,
        leader: Random.cname(),
        phone: '15888888801',
        email: Random.email(),
        status: 0,
        children: [
          {
            id: 6,
            parentId: 2,
            deptName: '前端组',
            orderNum: 1,
            leader: Random.cname(),
            phone: '15888888806',
            email: Random.email(),
            status: 0,
            children: [],
          },
          {
            id: 7,
            parentId: 2,
            deptName: '后端组',
            orderNum: 2,
            leader: Random.cname(),
            phone: '15888888807',
            email: Random.email(),
            status: 0,
            children: [],
          },
        ],
      },
      {
        id: 3,
        parentId: 1,
        deptName: '产品部',
        orderNum: 2,
        leader: Random.cname(),
        phone: '15888888802',
        email: Random.email(),
        status: 0,
        children: [],
      },
      {
        id: 4,
        parentId: 1,
        deptName: '市场部',
        orderNum: 3,
        leader: Random.cname(),
        phone: '15888888803',
        email: Random.email(),
        status: 0,
        children: [
          {
            id: 8,
            parentId: 4,
            deptName: '销售一组',
            orderNum: 1,
            leader: Random.cname(),
            phone: '15888888808',
            email: Random.email(),
            status: 0,
            children: [],
          },
          {
            id: 9,
            parentId: 4,
            deptName: '销售二组',
            orderNum: 2,
            leader: Random.cname(),
            phone: '15888888809',
            email: Random.email(),
            status: 1,
            children: [],
          },
        ],
      },
      {
        id: 5,
        parentId: 1,
        deptName: '人事部',
        orderNum: 4,
        leader: Random.cname(),
        phone: '15888888804',
        email: Random.email(),
        status: 0,
        children: [],
      },
    ],
  },
]

// 扁平化树形数据（用于列表展示）
function flattenTree(tree, result = []) {
  tree.forEach((node) => {
    result.push(node)
    if (node.children && node.children.length > 0) {
      flattenTree(node.children, result)
    }
  })
  return result
}

export default [
  // 获取部门树
  {
    url: '/api/system/dept/tree',
    method: 'get',
    response: () => {
      return {
        code: 200,
        data: deptTree,
        message: 'success',
      }
    },
  },

  // 获取部门列表（扁平化）
  {
    url: '/api/system/dept/list',
    method: 'get',
    response: ({ query }) => {
      const { deptName, status } = query

      let list = flattenTree(JSON.parse(JSON.stringify(deptTree)))

      if (deptName) {
        list = list.filter((item) => item.deptName.includes(deptName))
      }
      if (status !== undefined && status !== '' && status !== null) {
        list = list.filter((item) => item.status === Number(status))
      }

      return {
        code: 200,
        data: list,
        message: 'success',
      }
    },
  },

  // 新增部门
  {
    url: '/api/system/dept',
    method: 'post',
    response: ({ body }) => {
      const { parentId, deptName, orderNum, leader, phone, email, status } = body

      const newDept = {
        id: Date.now(),
        parentId,
        deptName,
        orderNum: orderNum || 0,
        leader: leader || '',
        phone: phone || '',
        email: email || '',
        status: status ?? 0,
        children: [],
      }

      // 在树中找到父节点并添加
      const flatList = flattenTree(deptTree)
      const parent = flatList.find((item) => item.id === parentId)
      if (parent) {
        if (!parent.children) parent.children = []
        parent.children.push(newDept)
      }

      return {
        code: 200,
        data: newDept,
        message: '新增成功',
      }
    },
  },

  // 修改部门
  {
    url: '/api/system/dept',
    method: 'put',
    response: ({ body }) => {
      const { id, ...updateData } = body

      const flatList = flattenTree(deptTree)
      const dept = flatList.find((item) => item.id === id)

      if (!dept) {
        return {
          code: 404,
          data: null,
          message: '部门不存在',
        }
      }

      Object.assign(dept, updateData)

      return {
        code: 200,
        data: dept,
        message: '修改成功',
      }
    },
  },

  // 删除部门
  {
    url: '/api/system/dept/:id',
    method: 'delete',
    response: ({ query }) => {
      const { id } = query
      const deptId = Number(id)

      const flatList = flattenTree(deptTree)
      const dept = flatList.find((item) => item.id === deptId)

      if (!dept) {
        return {
          code: 404,
          data: null,
          message: '部门不存在',
        }
      }

      // 检查是否有子部门
      if (dept.children && dept.children.length > 0) {
        return {
          code: 500,
          data: null,
          message: '存在子部门，不允许删除',
        }
      }

      // 从父节点中移除
      const parent = flatList.find((item) => item.id === dept.parentId)
      if (parent && parent.children) {
        parent.children = parent.children.filter((child) => child.id !== deptId)
      }

      return {
        code: 200,
        data: null,
        message: '删除成功',
      }
    },
  },
]
```

---

## 四、前后端联调

当后端接口开发完成后，我们需要将前端从 Mock 数据切换到真实后端 API。这个过程需要谨慎处理，确保平滑过渡。

### 4.1 关闭 Mock 数据

在 `vite.config.js` 中关闭 Mock 插件：

```js
// vite.config.js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { viteMockServe } from 'vite-plugin-mock'

export default defineConfig(({ command, mode }) => {
  return {
    plugins: [
      vue(),
      viteMockServe({
        mockPath: 'mock',
        enable: false,  // 关闭 Mock
        logger: true,
      }),
    ],
    // ... 其他配置
  }
})
```

**更优雅的方式**：通过环境变量控制，避免频繁修改配置文件：

```js
// vite.config.js
export default defineConfig(({ command, mode }) => {
  const isMock = process.env.VITE_USE_MOCK === 'true'

  return {
    plugins: [
      vue(),
      viteMockServe({
        mockPath: 'mock',
        enable: isMock,
        logger: true,
      }),
    ],
  }
})
```

在 `.env.development` 中控制：

```bash
# 是否启用 Mock（true-启用，false-禁用）
VITE_USE_MOCK=false
```

### 4.2 配置代理服务器

开发环境下，前端和后端通常运行在不同的端口，会产生跨域问题。通过 Vite 的 `server.proxy` 配置代理：

```js
// vite.config.js
export default defineConfig(({ command, mode }) => {
  return {
    plugins: [
      // ...
    ],
    server: {
      port: 3000,
      open: true,
      // 代理配置
      proxy: {
        '/api': {
          target: 'http://localhost:8080',  // 后端服务器地址
          changeOrigin: true,               // 允许跨域
          // rewrite: (path) => path.replace(/^\/api/, ''),  // 如果后端接口没有 /api 前缀，需要重写
        },
      },
    },
  }
})
```

**代理配置说明**：

- `target`：后端服务器的真实地址
- `changeOrigin: true`：修改请求头中的 `Origin` 字段，让后端认为请求来自同源
- `rewrite`：如果后端接口路径不以 `/api` 开头，需要重写路径。例如后端接口是 `/user/list`，则需要将 `/api/user/list` 重写为 `/user/list`

**使用环境变量管理后端地址**：

```js
// vite.config.js
export default defineConfig(({ command, mode }) => {
  const apiBaseURL = process.env.VITE_API_BASE_URL || 'http://localhost:8080'

  return {
    server: {
      proxy: {
        '/api': {
          target: apiBaseURL,
          changeOrigin: true,
        },
      },
    },
  }
})
```

```bash
# .env.development
VITE_API_BASE_URL=http://localhost:8080

# .env.production
VITE_API_BASE_URL=https://api.example.com
```

### 4.3 处理接口差异

真实后端的接口可能与 Mock 的接口存在差异，需要在代码中处理：

**常见差异及解决方案**：

1. **响应格式不一致**

```js
// Mock 格式
{ code: 200, data: { list: [], total: 0 }, message: 'success' }

// 后端格式（可能是）
{ code: 0, result: { records: [], count: 0 }, msg: 'ok' }
```

在 axios 拦截器中统一转换：

```js
// src/utils/request.js
service.interceptors.response.use(
  (response) => {
    const res = response.data
    
    // 统一转换为前端标准格式
    if (res.code === 0) {
      res.code = 200
      res.data = {
        list: res.result?.records || [],
        total: res.result?.count || 0,
      }
      res.message = res.msg
    }
    
    return res
  },
  // ...
)
```

2. **时间格式不一致**

```js
// 后端返回时间戳
{ createTime: 1710500000000 }

// 前端需要格式化
import dayjs from 'dayjs'

service.interceptors.response.use((response) => {
  const res = response.data
  
  // 递归格式化时间字段
  function formatTime(obj) {
    if (!obj || typeof obj !== 'object') return obj
    
    Object.keys(obj).forEach((key) => {
      if (key.includes('Time') && typeof obj[key] === 'number') {
        obj[key] = dayjs(obj[key]).format('YYYY-MM-DD HH:mm:ss')
      } else if (typeof obj[key] === 'object') {
        formatTime(obj[key])
      }
    })
    return obj
  }
  
  return formatTime(res)
})
```

3. **字段名不一致**

```js
// 后端使用下划线命名
{ user_name: '张三', dept_id: 1 }

// 前端使用驼峰命名
{ userName: '张三', deptId: 1 }
```

使用 `lodash` 的 `camelCase` 转换：

```js
import { camelCase } from 'lodash-es'

function toCamelCase(obj) {
  if (Array.isArray(obj)) {
    return obj.map(toCamelCase)
  }
  if (obj !== null && typeof obj === 'object') {
    return Object.keys(obj).reduce((acc, key) => {
      acc[camelCase(key)] = toCamelCase(obj[key])
      return acc
    }, {})
  }
  return obj
}

service.interceptors.response.use((response) => {
  response.data = toCamelCase(response.data)
  return response.data
})
```

### 4.4 联调测试清单

切换到真实后端后，按以下清单逐项检查：

- [ ] 登录接口：用户名密码验证、Token 生成
- [ ] 用户信息接口：根据 Token 返回用户信息
- [ ] 列表接口：分页参数、搜索过滤、排序
- [ ] 新增接口：表单验证、重复校验
- [ ] 修改接口：数据更新、乐观锁
- [ ] 删除接口：单条删除、批量删除
- [ ] 状态修改：启用/禁用切换
- [ ] 文件上传：图片上传、Excel 导入导出
- [ ] 权限控制：不同角色看到不同的数据

---

## 五、跨域处理方案

### 5.1 什么是跨域？

**跨域**（Cross-Origin）是指浏览器出于安全考虑，限制不同源（协议、域名、端口任一不同）之间的资源访问。

例如：
- 前端运行在 `http://localhost:3000`
- 后端运行在 `http://localhost:8080`
- 端口不同，属于跨域

浏览器会阻止跨域请求，并在控制台报错：

```
Access to XMLHttpRequest at 'http://localhost:8080/api/user/list' 
from origin 'http://localhost:3000' has been blocked by CORS policy: 
No 'Access-Control-Allow-Origin' header is present on the requested resource.
```

### 5.2 开发环境：Vite 代理

开发环境下，最简单的方式就是使用 Vite 的代理配置（前面已经讲过）：

```js
// vite.config.js
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
}
```

**原理**：浏览器请求 `http://localhost:3000/api/user/list`，Vite 开发服务器收到请求后转发给 `http://localhost:8080/api/user/list`，绕过了浏览器的跨域限制。

### 5.3 生产环境：Nginx 反向代理

生产环境下，通常使用 Nginx 作为反向代理服务器：

```nginx
server {
    listen 80;
    server_name www.example.com;

    # 前端静态文件
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;  # SPA 路由支持
    }

    # API 代理
    location /api/ {
        proxy_pass http://backend-server:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

**原理**：用户访问 `http://www.example.com/api/user/list`，Nginx 将请求转发给后端服务器 `http://backend-server:8080/api/user/list`，对浏览器来说是同源的。

### 5.4 后端 CORS 配置

如果后端支持 CORS，可以在后端配置允许跨域：

**Java (Spring Boot)**：

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("http://localhost:3000", "https://www.example.com")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

**Node.js (Express)**：

```js
const cors = require('cors')

app.use(cors({
  origin: ['http://localhost:3000', 'https://www.example.com'],
  methods: ['GET', 'POST', 'PUT', 'DELETE'],
  credentials: true,
}))
```

### 5.5 跨域问题排查

如果遇到跨域问题，按以下步骤排查：

1. **检查浏览器控制台错误信息**
   - `No 'Access-Control-Allow-Origin' header`：后端未配置 CORS
   - `Request header field authorization is not allowed`：自定义请求头未被允许

2. **检查网络请求**
   - 打开 DevTools → Network
   - 查看请求是否发出，响应状态码是什么
   - 查看 `OPTIONS` 预检请求是否成功

3. **检查代理配置**
   - 确认 `vite.config.js` 中的代理配置正确
   - 确认后端服务器地址和端口正确
   - 重启 Vite 开发服务器

4. **检查后端配置**
   - 确认后端服务已启动
   - 确认后端 CORS 配置正确
   - 查看后端日志是否有报错

---

## 六、API 接口规范建议

前后端协作时，制定统一的接口规范可以大大提高开发效率。

### 6.1 RESTful API 规范

推荐使用 RESTful 风格的 API 设计：

```
GET    /api/system/user/list     # 获取用户列表
GET    /api/system/user/:id      # 获取用户详情
POST   /api/system/user          # 新增用户
PUT    /api/system/user          # 修改用户
DELETE /api/system/user/:id      # 删除用户
```

### 6.2 统一响应格式

所有接口返回统一的格式：

```js
// 成功响应
{
  code: 200,           // 状态码：200-成功，其他-失败
  data: { ... },       // 数据
  message: 'success',  // 提示信息
}

// 失败响应
{
  code: 500,           // 错误码
  data: null,          // 数据为 null
  message: '用户名已存在',  // 错误信息
}
```

**常用状态码**：

| 状态码 | 含义 | 说明 |
|--------|------|------|
| 200 | 成功 | 请求成功 |
| 400 | 参数错误 | 请求参数不合法 |
| 401 | 未授权 | 未登录或 Token 过期 |
| 403 | 禁止访问 | 无权限 |
| 404 | 资源不存在 | 请求的资源不存在 |
| 500 | 服务器错误 | 服务器内部错误 |

### 6.3 分页规范

**请求参数**：

```js
{
  pageNum: 1,      // 页码（从 1 开始）
  pageSize: 10,    // 每页条数
}
```

**响应格式**：

```js
{
  code: 200,
  data: {
    list: [...],   // 数据列表
    total: 86,     // 总记录数
  },
  message: 'success',
}
```

### 6.4 搜索过滤规范

使用 query 参数传递搜索条件：

```js
// 请求
GET /api/system/user/list?pageNum=1&pageSize=10&username=张&status=0

// 后端接收
{
  pageNum: '1',
  pageSize: '10',
  username: '张',
  status: '0',
}
```

### 6.5 批量操作规范

批量删除使用逗号分隔的 ID：

```js
// 请求
DELETE /api/system/user/1,2,3

// 或使用 POST
POST /api/system/user/batchDelete
{
  ids: [1, 2, 3]
}
```

### 6.6 Token 传递规范

使用 `Authorization` 请求头传递 Token：

```js
// 请求头
Authorization: Bearer token_admin_1710500000000

// axios 拦截器自动添加
service.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})
```

---

## 七、本章小结

本章我们学习了：

1. **为什么需要 Mock 数据**：前后端并行开发、快速原型验证、独立测试调试
2. **使用 vite-plugin-mock**：安装配置、Mock 文件结构、Mock.Random 数据生成
3. **完整 Mock 示例**：登录认证、用户 CRUD（分页、搜索、新增、修改、删除、状态修改、导出）、部门树管理
4. **前后端联调**：关闭 Mock、配置代理、处理接口差异、联调测试清单
5. **跨域处理方案**：开发环境 Vite 代理、生产环境 Nginx 反向代理、后端 CORS 配置
6. **API 接口规范**：RESTful 风格、统一响应格式、分页规范、Token 传递

**核心要点**：

- Mock 数据让前端开发不再依赖后端，提高开发效率
- Mock 文件的响应格式必须与真实后端保持一致
- 通过环境变量控制 Mock 的开关，方便切换
- 开发环境使用 Vite 代理解决跨域，生产环境使用 Nginx 反向代理
- 制定统一的接口规范，前后端协作更高效

下一章我们将学习如何将项目打包部署上线，涵盖 Vite 构建配置、Nginx 部署、Docker 容器化等内容。

---

> **上一章**：[第9章 其他搭建顺序方案](./09-其他搭建顺序方案.md)
>
> **下一章**：[第11章 打包部署与上线](./11-打包部署与上线.md) —— Vite 构建配置、Nginx 部署、Docker 容器化
>
> **教程总览**：[教程总览与学习路线](./00-教程总览与学习路线.md)
