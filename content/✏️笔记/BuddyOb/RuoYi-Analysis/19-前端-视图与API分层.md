# 19 · 前端-视图与 API 分层

> 对应清单：入门第 10 条（api/*.js）、进阶第 17 条（通用组件在视图里用）、高级第 23 条（字典在视图里用）。
> 视图（views）是"页面"，api 是"页面调接口的地方"。本文以一个真实用户管理页为标本。

## 一、视图目录（56 个 .vue）

```
src/views/
├─ index.vue                 # 首页（dashboard，含 ECharts）
├─ login/index.vue          # 登录页（调 user.Login + SysLoginService）
├─ register/index.vue     # 注册
├─ lock/  error/          # 锁屏 / 401·404
├─ system/
│   ├─ user/index.vue     # ★ 用户管理（下面标本）
│   ├─ user/profile/      # 个人中心
│   ├─ role/  dept/  menu/  post/  dict/  config/  notice/
├─ monitor/
│   ├─ server/  cache/  online/  logininfor/  operlog/  job/
└─ tool/
    ├─ build/  gen/  swagger/    # 表单构建 / 代码生成 / 接口文档
```

## 二、api/system/user.js —— 页面的"接口层"（真实）

```js
import request from '@/utils/request'
export function listUser(query) {                 // ① 列表
  return request({ url: '/system/user/list', method: 'get', params: query })
}
export function getUser(userId) {                    // ② 详情
  return request({ url: '/system/user/' + parseStrEmpty(userId), method: 'get' })
}
export function addUser(data) {                       // ③ 新增
  return request({ url: '/system/user', method: 'post', data })
}
export function updateUser(data) {                    // ④ 修改
  return request({ url: '/system/user', method: 'put', data })
}
export function delUser(userId) {                      // ⑤ 删除
  return request({ url: '/system/user/' + userId, method: 'delete' })
}
export function resetUserPwd(userId, password) {     // ⑥ 重置密码
  return request({ url: '/system/user/resetPwd', method: 'put',
    data: { userId, password } })
}
export function deptTreeSelect() {                     // ⑦ 部门下拉树
  return request({ url: '/system/user/deptTree', method: 'get' })
}
```

**解释：** 一个函数 = 一次 `request()` 调用，**只写 URL + method + 参数**，不含拦截/拼装逻辑（那些在 [[16-前端-请求封装与权限工具]] 的 `request.js`）。视图里 `import { listUser } from '@/api/system/user'` 即用。

## 三、views/system/user/index.vue —— 标本页面（节选，真实）

```vue
<template>
  <div class="app-container tree-sidebar-manage-wrap">
    <!-- ① 查询表单 -->
    <el-form :model="queryParams" :inline="true" v-show="showSearch">
      <el-form-item label="用户名称" prop="userName">
        <el-input v-model="queryParams.userName" placeholder="请输入用户名称"/>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="用户状态" clearable>
          <el-option v-for="dict in dict.type.sys_normal_disable"
            :key="dict.value" :label="dict.label" :value="dict.value"/>  <!-- ② 字典下拉 -->
        </el-select>
      </el-form-item>
    </el-form>

    <!-- ③ 按钮区：v-hasPermi 控制显隐 -->
    <el-row :gutter="10" class="mb8">
      <el-button v-hasPermi="['system:user:add']" @click="handleAdd">新增</el-button>
      <el-button v-hasPermi="['system:user:edit']" :disabled="single" @click="handleUpdate">修改</el-button>
      <el-button v-hasPermi="['system:user:remove']" :disabled="multiple" @click="handleDelete">删除</el-button>
      <el-button v-hasPermi="['system:user:export']" @click="handleExport">导出</el-button>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" :columns="columns"/>
    </el-row>

    <!-- ④ 表格 -->
    <el-table v-loading="loading" :data="userList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50"/>
      <el-table-column label="用户名称" prop="userName"/>
      <el-table-column label="状态" align="center">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.status" active-value="0" inactive-value="1"
            @change="handleStatusChange(scope.row)"/>   <!-- ⑤ 状态开关 -->
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template slot-scope="scope" v-if="scope.row.userId !== 1">  <!-- ⑥ 超管不可动 -->
          <el-button v-hasPermi="['system:user:edit']" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button v-hasPermi="['system:user:remove']" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- ⑦ 分页 -->
    <pagination v-show="total > 0" :total="total"
      :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
  </div>
</template>
```

**逐段解释（一个标准 CRUD 页的骨架）：**
1. **查询表单**：`queryParams` 收集条件，`v-show="showSearch"` 可被 `RightToolbar` 显隐。
2. ② **`dict.type.sys_normal_disable`**：字典数据（"正常/停用"），由 `DictData` 插件注入（见 [[20-前端-字典主题与工具函数]]）。`v-for` 渲染成下拉项——**不用手写下拉选项**。
3. ③ **`v-hasPermi` 按钮**：没 `system:user:add` 权限，这个"新增"按钮 DOM 直接被删（见 [[17-前端-权限指令与通用组件]]）。
4. ④ **`el-table :data="userList"`**：数据来自 `getList()` 拉的 `res.rows`。`@selection-change` 收集勾选，决定"修改/删除"按钮的 `single`/`multiple` 禁用态。
5. ⑤ **`el-switch` 改状态**：`@change` 调 `changeUserStatus` → `put /system/user/changeStatus` → 后端即时改库。
6. ⑥ **`scope.row.userId !== 1`**：超管（id=1）不可被改/删——前端一道保险（后端 `@PreAuthorize` 也会拦）。
7. ⑦ **`<pagination/>`**：翻页改 `queryParams.pageNum` → `@pagination="getList"` 重新拉——和后端 `PageHelper` 分页闭环（见 [[17-前端-权限指令与通用组件]]）。

**脚本里 `getList` 长这样（典型）：**
```js
getList() {
  this.loading = true
  listUser(this.queryParams).then(res => {   // 调 api
    this.userList = res.rows; this.total = res.total; this.loading = false
  })
}
```

## 四、登录页与全局的协作（串联）

```
login/index.vue
  ├─ 用户输入 + 点登录 → this.$store.dispatch('user/Login', {username,password,code,uuid})
  │     （user/Login 见 [[15-前端-Vuex状态管理]]，内部调 api/login → 后端 SysLoginService）
  ├─ 登录成功存入 cookie token → 跳首页
  └─ 之后每次跳转触发 permission.js 守卫（[[14-前端-路由与权限守卫]]）
        → GetInfo 拉权限 → GenerateRoutes 加菜单路由 → Layout 渲染
```

## 五、为什么"后端加张表，前端几乎零改动"

1. 后端用代码生成器（[[09-后端-代码生成器]]）吐出 `Xxx.vue` + `xxx.js(api)` + 菜单 SQL；
2. 菜单 SQL 插入 `sys_menu` → 登录后 `getRouters` 把它变路由（[[14-前端-路由与权限守卫]]）；
3. 生成的 `Xxx.vue` 就是第三节那种"表单+按钮+表格+分页"骨架，改字段即可；
4. 生成的 `xxx.js` 就是第二节那种 REST 函数。

> 学完本章，你看到一个 RuoYi 页面，能立刻拆出"表单/按钮/表格/分页各对应什么、数据从哪个 api 来、权限字符串和后端哪条对得上"。最后一章讲字典、主题、工具函数这些"润色与复用"件。
