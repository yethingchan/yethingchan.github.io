# 第八章：完整 CRUD 实战 -- 用户管理页面

> **本章是整个教程最核心的章节。** 我们将从零开始，一步步搭建一个完整的"用户管理"页面，涵盖后台管理系统中最常见的所有功能：搜索筛选、表格展示、分页、新增/编辑弹窗、删除（单条 + 批量）、状态切换、导出。学完本章，你就掌握了后台管理系统 80% 以上的页面开发能力。

---

## 本章概览

| 步骤 | 内容 | 依赖的前置章节 |
|------|------|----------------|
| 步骤1 | 创建 API 接口文件 | 第2章（request.js） |
| 步骤2 | 创建页面骨架 | 第3章（路由）、第4章（布局） |
| 步骤3 | 实现搜索功能 | 第5章（useTable） |
| 步骤4 | 实现表格（列配置、状态开关、操作列） | 第5章（Table 组件） |
| 步骤5 | 实现分页 | 第5章（Pagination 组件） |
| 步骤6 | 实现新增/编辑弹窗（表单 + 校验） | Element Plus |
| 步骤7 | 实现删除功能（单条 + 批量） | Element Plus |
| 步骤8 | 实现导出功能 | 第2章（request.js） |
| 步骤9 | 组合所有功能 -- 完整可运行代码 | 全部 |

**最终效果：**

```
┌─────────────────────────────────────────────────────────────┐
│  用户管理                                                     │
├─────────────────────────────────────────────────────────────┤
│  搜索栏：[用户名] [手机号] [状态▼]  [搜索] [重置]             │
│  工具栏：[+ 新增] [批量删除] [导出]                            │
├───┬────┬──────┬────────┬────┬────┬──────┬──────────────────┤
│ ☐ │ ID │ 用户名│  昵称   │手机│部门│ 状态 │    操作           │
├───┼────┼──────┼────────┼────┼────┼──────┼──────────────────┤
│ ☐ │ 1  │admin │ 管理员  │138 │技术部│ ●启用│ 编辑 删除 重置  │
│ ☐ │ 2  │zhang │ 张三   │139 │产品部│ ○停用│ 编辑 删除 重置  │
├───┴────┴──────┴────────┴────┴────┴──────┴──────────────────┤
│                       < 1 2 3 ... > 共 86 条                  │
└─────────────────────────────────────────────────────────────┘
```

---

## 步骤1：创建 API 接口文件

> 每个业务模块都有对应的 API 文件，统一管理该模块的所有接口调用。这是前后端分离开发的标准做法。

**文件路径：** `src/api/system/user.js`

```js
/**
 * @file src/api/system/user.js
 * @description 用户管理模块 -- API 接口定义
 *
 * 按功能分类组织：
 *   - 查询：getUserList、getUserDetail
 *   - 新增/编辑：createUser、updateUser
 *   - 删除：deleteUser
 *   - 导出：exportUser
 *   - 其他：resetUserPassword、changeUserStatus
 */
import request from '@/utils/request'

// ==================== 查询相关 ====================

/**
 * 分页查询用户列表
 * @param {Object} params - 查询参数
 * @param {number} params.pageNum     - 页码（从1开始）
 * @param {number} params.pageSize   - 每页条数
 * @param {string} [params.username]  - 用户名（模糊搜索）
 * @param {string} [params.phone]     - 手机号（模糊搜索）
 * @param {number} [params.status]    - 状态：1-启用 0-停用
 * @returns {Promise} { code: 200, data: { list: Array, total: number } }
 *
 * @example 返回值示例：
 * {
 *   code: 200,
 *   data: {
 *     list: [
 *       {
 *         id: 1,
 *         username: 'admin',
 *         nickname: '管理员',
 *         phone: '13800138000',
 *         email: 'admin@example.com',
 *         deptName: '技术部',
 *         roleNames: ['管理员'],
 *         status: 1,
 *         createTime: '2025-01-01 10:00:00'
 *       }
 *     ],
 *     total: 86
 *   }
 * }
 */
export function getUserList(params) {
  return request.get('/system/user/list', { params })
}

/**
 * 获取用户详情
 * @param {number} id - 用户ID
 * @returns {Promise} { code: 200, data: { id, username, nickname, ... } }
 */
export function getUserDetail(id) {
  return request.get(`/system/user/${id}`)
}

// ==================== 新增/编辑相关 ====================

/**
 * 新增用户
 * @param {Object} data - 用户信息
 * @param {string} data.username - 用户名（必填）
 * @param {string} data.nickname - 昵称（必填）
 * @param {string} data.password - 密码（新增时必填）
 * @param {string} data.phone    - 手机号
 * @param {string} data.email    - 邮箱
 * @param {number} data.status   - 状态：1-启用 0-停用
 * @param {number} data.deptId   - 部门ID
 * @param {Array}  data.roleIds  - 角色ID数组
 * @returns {Promise} { code: 200, message: '新增成功' }
 */
export function createUser(data) {
  return request.post('/system/user', data)
}

/**
 * 编辑用户
 * @param {number} id   - 用户ID
 * @param {Object} data - 修改的用户信息（同 createUser，但 password 非必填）
 * @returns {Promise} { code: 200, message: '修改成功' }
 */
export function updateUser(id, data) {
  return request.put(`/system/user/${id}`, data)
}

// ==================== 删除相关 ====================

/**
 * 删除用户（单条）
 * @param {number} id - 用户ID
 * @returns {Promise} { code: 200, message: '删除成功' }
 */
export function deleteUser(id) {
  return request.del(`/system/user/${id}`)
}

// ==================== 状态与密码相关 ====================

/**
 * 修改用户状态
 * @param {number} id     - 用户ID
 * @param {number} status - 目标状态：1-启用 0-停用
 * @returns {Promise} { code: 200, message: '操作成功' }
 */
export function changeUserStatus(id, status) {
  return request.put(`/system/user/changeStatus`, { id, status })
}

/**
 * 重置用户密码
 * @param {number} id       - 用户ID
 * @param {string} password - 新密码
 * @returns {Promise} { code: 200, message: '密码重置成功' }
 */
export function resetUserPassword(id, password) {
  return request.put(`/system/user/resetPwd`, { id, password })
}

// ==================== 导出相关 ====================

/**
 * 导出用户列表
 * @param {Object} params - 查询参数（同 getUserList，用于按条件筛选导出）
 * @returns {Promise} 返回二进制流（Blob），需要前端处理文件下载
 */
export function exportUser(params) {
  return request.get('/system/user/export', {
    params,
    responseType: 'blob', // 重要：告诉 axios 响应类型是二进制流
  })
}

// ==================== 辅助数据接口 ====================

/**
 * 获取角色列表（用于弹窗中的角色下拉选择）
 * @returns {Promise} { code: 200, data: [{ id, name }] }
 */
export function getRoleOptions() {
  return request.get('/system/role/options')
}

/**
 * 获取部门树（用于弹窗中的部门下拉选择）
 * @returns {Promise} { code: 200, data: [{ id, label, children }] }
 */
export function getDeptTree() {
  return request.get('/system/dept/tree')
}
```

> **要点说明：**
> - 接口文件按照"查询 -> 新增编辑 -> 删除 -> 状态密码 -> 导出 -> 辅助数据"的顺序组织，结构清晰。
> - 每个接口函数都有完整的 JSDoc 注释，包含参数说明和返回值示例。
> - `exportUser` 接口需要设置 `responseType: 'blob'`，因为后端返回的是文件流，而不是 JSON。
> - `getRoleOptions` 和 `getDeptTree` 是辅助接口，用于弹窗表单中的下拉选择数据来源。

---

## 步骤2：创建页面骨架

> 先搭建页面的四个核心区域：搜索栏、操作按钮区、表格区、分页区。这些是后台管理页面的标准布局。

**文件路径：** `src/views/system/user/index.vue`

```vue
<template>
  <div class="page-container">
    <!-- ==================== 页面标题 ==================== -->
    <div class="page-header">
      <h2>用户管理</h2>
    </div>

    <!-- ==================== 搜索栏 ==================== -->
    <div class="search-bar">
      <el-form :inline="true" :model="searchForm" ref="searchFormRef">
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="searchForm.username"
            placeholder="请输入用户名"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="searchForm.phone"
            placeholder="请输入手机号"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="searchForm.status" placeholder="用户状态" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- ==================== 操作按钮区 ==================== -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增
        </el-button>
        <el-button type="danger" :disabled="selection.length === 0" @click="handleBatchDelete">
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
        <el-button type="warning" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </div>
      <div class="toolbar-right">
        <el-tooltip content="刷新">
          <el-button circle @click="refresh">
            <el-icon><Refresh /></el-icon>
          </el-button>
        </el-tooltip>
      </div>
    </div>

    <!-- ==================== 表格区 ==================== -->
    <Table
      :columns="columns"
      :data="tableData"
      :loading="loading"
      stripe
      @selection-change="handleSelectionChange"
    >
      <!-- 状态列：使用 switch 开关 -->
      <template #status="{ row }">
        <el-switch
          :model-value="row.status === 1"
          @change="(val) => handleStatusChange(row, val)"
          inline-prompt
          active-text="启用"
          inactive-text="停用"
        />
      </template>

      <!-- 操作列 -->
      <template #action="{ row }">
        <el-button type="primary" link size="small" @click="handleEdit(row)">
          <el-icon><Edit /></el-icon> 编辑
        </el-button>
        <el-button type="danger" link size="small" @click="handleDelete(row)">
          <el-icon><Delete /></el-icon> 删除
        </el-button>
        <el-button type="warning" link size="small" @click="handleResetPwd(row)">
          <el-icon><Key /></el-icon> 重置密码
        </el-button>
      </template>
    </Table>

    <!-- ==================== 分页区 ==================== -->
    <Pagination
      v-model:current-page="pagination.currentPage"
      v-model:page-size="pagination.pageSize"
      :total="pagination.total"
      :page-sizes="pageSizesConfig"
      @current-change="handleCurrentChange"
      @size-change="handleSizeChange"
    />
  </div>
</template>

<script setup>
// 这里先只放骨架代码，后续步骤逐步填充
import { reactive } from 'vue'
import {
  Search, Refresh, Plus, Delete, Download, Edit, Key,
} from '@element-plus/icons-vue'
import Table from '@/components/Table/index.vue'
import Pagination from '@/components/Pagination/index.vue'
import { useTable } from '@/hooks/useTable'
import { getUserList } from '@/api/system/user'

// 搜索表单
const searchForm = reactive({
  username: '',
  phone: '',
  status: '',
})

// 使用 useTable 组合式函数
const {
  tableData,
  loading,
  selection,
  pagination,
  pageSizesConfig,
  handleSearch,
  handleReset,
  handleSelectionChange,
  handleCurrentChange,
  handleSizeChange,
  refresh,
} = useTable(getUserList, {
  searchForm,
})

// 列配置
const columns = [
  { type: 'selection', width: 55 },
  { type: 'index', label: '序号', width: 70 },
  { prop: 'username', label: '用户名', width: 120 },
  { prop: 'nickname', label: '昵称', width: 120 },
  { prop: 'phone', label: '手机号', width: 140 },
  { prop: 'deptName', label: '部门', width: 120 },
  { prop: 'status', label: '状态', width: 100, align: 'center', slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 180, sortable: true },
  { label: '操作', width: 220, fixed: 'right', align: 'center', slot: 'action' },
]

// 占位方法（后续步骤实现）
const handleAdd = () => {}
const handleEdit = (row) => {}
const handleDelete = (row) => {}
const handleBatchDelete = () => {}
const handleStatusChange = (row, val) => {}
const handleResetPwd = (row) => {}
const handleExport = () => {}
</script>

<style scoped>
.page-container {
  padding: 20px;
}
.page-header {
  margin-bottom: 16px;
}
.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}
.search-bar {
  margin-bottom: 16px;
  padding: 18px 20px 0;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.toolbar-left {
  display: flex;
  gap: 12px;
}
</style>
```

> **要点说明：**
> - 页面分为四个核心区域，每个区域用注释分隔，一目了然。
> - 搜索栏使用 `el-form :inline="true"` 实现行内布局。
> - 操作按钮区分为左侧（新增/批量删除/导出）和右侧（刷新按钮）。
> - 表格使用第5章封装的 `Table` 组件，通过 `columns` 配置驱动列渲染。
> - 分页使用第5章封装的 `Pagination` 组件，通过 `v-model` 双向绑定页码和页数。
> - 此时的占位方法是空的，后续步骤逐步填充。

---

## 步骤3：实现搜索功能

> 搜索功能已经通过 `useTable` 组合式函数自动处理了。这一步我们重点讲解搜索的工作原理。

### 3.1 搜索的工作原理

```
用户输入搜索条件 → 点击"搜索"按钮
       ↓
handleSearch()        ← useTable 提供
       ↓
pagination.currentPage = 1    ← 重置到第一页
       ↓
fetchData()            ← useTable 内部方法
       ↓
合并分页参数 + 搜索条件 → 构建请求参数
       ↓
调用 API（getUserList）
       ↓
更新 tableData 和 pagination.total
       ↓
表格自动刷新
```

### 3.2 useTable 如何处理搜索参数

回顾 `useTable` 的核心代码（`src/hooks/useTable.js`）：

```js
const fetchData = async () => {
  loading.value = true
  try {
    // 1. 构建分页参数
    let params = {
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize,
    }

    // 2. 合并搜索条件（自动过滤掉空值）
    if (searchFormRef) {
      const searchParams = {}
      Object.keys(searchFormRef).forEach((key) => {
        const val = searchFormRef[key]
        if (val !== '' && val !== null && val !== undefined) {
          searchParams[key] = val
        }
      })
      params = { ...params, ...searchParams }
    }

    // 3. 调用 API
    const res = await apiFn(params)

    // 4. 处理响应
    // ...
  }
}
```

> **要点：** `useTable` 会自动过滤掉空值字段（`''`、`null`、`undefined`），所以当用户不填某个搜索条件时，该字段不会发送给后端。

### 3.3 重置功能的工作原理

```js
// useTable 内部的 handleReset
const handleReset = () => {
  // 1. 清空搜索表单的所有字段
  if (searchFormRef) {
    Object.keys(searchFormRef).forEach((key) => {
      searchFormRef[key] = ''
    })
  }
  // 2. 重置到第一页，恢复默认每页条数
  pagination.currentPage = 1
  pagination.pageSize = defaultPageSize
  // 3. 重新加载数据
  fetchData()
}
```

### 3.4 搜索栏增强细节

以下是搜索栏中一些实用的增强写法：

```vue
<!-- 回车搜索：输入框绑定 @keyup.enter -->
<el-input
  v-model="searchForm.username"
  placeholder="请输入用户名"
  clearable
  @keyup.enter="handleSearch"
/>

<!-- 状态选择器：clearable 允许清空选择 -->
<el-select v-model="searchForm.status" placeholder="用户状态" clearable>
  <el-option label="启用" :value="1" />
  <el-option label="停用" :value="0" />
</el-select>

<!-- 重置按钮：调用 useTable 提供的 handleReset -->
<el-button @click="handleReset">
  <el-icon><Refresh /></el-icon>
  重置
</el-button>
```

> **实用技巧：**
> - 输入框加上 `@keyup.enter` 让用户按回车即可搜索。
> - 选择器加上 `clearable` 让用户可以清空已选条件。
> - 重置操作会同时清空表单 + 重置页码 + 重新请求。

---

## 步骤4：实现表格

> 表格是后台页面的核心展示区域。本步骤实现列配置、状态开关、操作列等常见表格功能。

### 4.1 列配置详解

```js
const columns = [
  // 多选列：用于批量操作
  { type: 'selection', width: 55 },

  // 序号列：自动编号（1, 2, 3...）
  { type: 'index', label: '序号', width: 70 },

  // 普通列：直接显示字段值
  { prop: 'username', label: '用户名', width: 120 },
  { prop: 'nickname', label: '昵称', width: 120 },
  { prop: 'phone', label: '手机号', width: 140 },
  { prop: 'deptName', label: '部门', width: 120 },

  // 自定义插槽列：状态列使用 switch 开关
  { prop: 'status', label: '状态', width: 100, align: 'center', slot: 'status' },

  // 可排序列：点击表头排序
  { prop: 'createTime', label: '创建时间', width: 180, sortable: true },

  // 操作列：固定在右侧
  { label: '操作', width: 220, fixed: 'right', align: 'center', slot: 'action' },
]
```

### 4.2 状态列 -- Switch 开关

状态列使用 `el-switch` 组件，用户可以直接在表格中切换用户状态，调用后端 API 实时生效：

```vue
<!-- 状态列：使用 switch 开关 -->
<template #status="{ row }">
  <el-switch
    :model-value="row.status === 1"
    @change="(val) => handleStatusChange(row, val)"
    inline-prompt
    active-text="启用"
    inactive-text="停用"
  />
</template>
```

**对应的处理函数：**

```js
import { changeUserStatus } from '@/api/system/user'
import { ElMessage } from 'element-plus'

/**
 * 切换用户状态
 * @param {Object} row - 当前行数据
 * @param {boolean} val - 开关新值（true=启用, false=停用）
 */
const handleStatusChange = async (row, val) => {
  const newStatus = val ? 1 : 0
  const statusText = val ? '启用' : '停用'

  try {
    // 调用 API 更新状态
    await changeUserStatus(row.id, newStatus)

    // 更新本地数据（不刷新整个表格，避免闪烁）
    row.status = newStatus

    ElMessage.success(`已${statusText}用户「${row.username}」`)
  } catch (error) {
    // 失败时不修改状态（switch 会自动回弹）
    ElMessage.error(`操作失败：${error.message || '请稍后重试'}`)
  }
}
```

> **要点说明：**
> - 使用 `:model-value` 而非 `v-model`，因为状态更新需要在 API 调用成功后才生效。如果使用 `v-model`，开关会立即变化，API 失败时再回弹，体验不好。
> - `inline-prompt` 让开关上显示文字（"启用"/"停用"），更直观。
> - API 调用成功后手动更新 `row.status`，失败时 switch 自动回弹到原状态。

### 4.3 操作列 -- 编辑、删除、重置密码

```vue
<template #action="{ row }">
  <el-button type="primary" link size="small" @click="handleEdit(row)">
    <el-icon><Edit /></el-icon> 编辑
  </el-button>
  <el-button type="danger" link size="small" @click="handleDelete(row)">
    <el-icon><Delete /></el-icon> 删除
  </el-button>
  <el-button type="warning" link size="small" @click="handleResetPwd(row)">
    <el-icon><Key /></el-icon> 重置密码
  </el-button>
</template>
```

> **要点说明：**
> - 使用 `link` 类型的按钮，使操作列更紧凑、不占空间。
> - 每个按钮使用不同颜色区分操作类型（primary=编辑、danger=删除、warning=重置密码）。
> - 通过 `slot: 'action'` 使用插槽自定义渲染。

---

## 步骤5：实现分页

> 分页已经通过 `useTable` + `Pagination` 组件自动处理了。本步骤讲解分页的数据绑定和事件处理。

### 5.1 分页模板

```vue
<Pagination
  v-model:current-page="pagination.currentPage"
  v-model:page-size="pagination.pageSize"
  :total="pagination.total"
  :page-sizes="pageSizesConfig"
  @current-change="handleCurrentChange"
  @size-change="handleSizeChange"
/>
```

### 5.2 数据绑定关系

```
useTable 返回的 pagination（reactive）
  ├── currentPage  ←→  Pagination v-model:current-page（双向绑定）
  ├── pageSize     ←→  Pagination v-model:page-size   （双向绑定）
  └── total        →   Pagination :total               （单向传递）

useTable 返回的方法
  ├── handleCurrentChange(page)  → 页码变化时触发
  │     └── pagination.currentPage = page
  │     └── fetchData()
  │
  └── handleSizeChange(size)     → 每页条数变化时触发
        └── pagination.pageSize = size
        └── pagination.currentPage = 1   ← 切回第一页
        └── fetchData()
```

### 5.3 分页工作流程

```
用户点击第3页
    ↓
Pagination 触发 @current-change 事件
    ↓
handleCurrentChange(3)           ← useTable 提供
    ↓
pagination.currentPage = 3
    ↓
fetchData()
    ↓
API 请求：{ pageNum: 3, pageSize: 10, ...searchForm }
    ↓
响应：{ list: [...], total: 86 }
    ↓
更新 tableData 和 pagination.total
    ↓
表格和分页组件自动更新
```

> **注意：** 当用户切换"每页条数"时，页码会自动重置为 1。这是标准做法，因为切换页大小后当前页可能已经不存在了。

---

## 步骤6：实现新增/编辑弹窗

> 新增和编辑共用同一个弹窗，通过 `dialogTitle` 和 `dialogMode` 区分当前是新增还是编辑模式。这是后台系统最常见的弹窗模式。

### 6.1 弹窗模板代码

在 `index.vue` 的 `<template>` 中，分页组件之后添加弹窗：

```vue
    <!-- ==================== 新增/编辑弹窗 ==================== -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :close-on-click-modal="false"
      :close-on-press-escape="true"
      @closed="handleDialogClosed"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="formData.username"
            placeholder="请输入用户名"
            :disabled="dialogMode === 'edit'"
            maxlength="20"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="昵称" prop="nickname">
          <el-input
            v-model="formData.nickname"
            placeholder="请输入昵称"
            maxlength="20"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="formData.phone"
            placeholder="请输入手机号"
            maxlength="11"
          />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="formData.email"
            placeholder="请输入邮箱"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password" v-if="dialogMode === 'add'">
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="请输入密码"
            show-password
            maxlength="20"
          />
        </el-form-item>

        <el-form-item label="角色" prop="roleIds">
          <el-select
            v-model="formData.roleIds"
            multiple
            placeholder="请选择角色"
            style="width: 100%"
          >
            <el-option
              v-for="item in roleOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="部门" prop="deptId">
          <el-tree-select
            v-model="formData.deptId"
            :data="deptTreeData"
            :props="{ label: 'label', value: 'id', children: 'children' }"
            placeholder="请选择部门"
            check-strictly
            :render-after-expand="false"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
```

### 6.2 弹窗相关逻辑代码

在 `<script setup>` 中添加：

```js
import { ref, reactive, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getUserList,
  createUser,
  updateUser,
  deleteUser,
  changeUserStatus,
  resetUserPassword,
  exportUser,
  getRoleOptions,
  getDeptTree,
} from '@/api/system/user'

// ==================== 弹窗状态 ====================

/** 弹窗是否可见 */
const dialogVisible = ref(false)

/** 弹窗模式：'add' 新增 | 'edit' 编辑 */
const dialogMode = ref('add')

/** 弹窗标题（根据模式动态变化） */
const dialogTitle = ref('新增用户')

/** 提交按钮加载状态 */
const submitLoading = ref(false)

// ==================== 表单数据 ====================

/** 表单引用（用于校验） */
const formRef = ref(null)

/** 表单数据 */
const formData = reactive({
  id: undefined,
  username: '',
  nickname: '',
  phone: '',
  email: '',
  password: '',
  roleIds: [],
  deptId: undefined,
  status: 1,
})

// ==================== 表单校验规则 ====================

/** 校验规则 */
const formRules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度为 2-20 个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' },
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度为 2-20 个字符', trigger: 'blur' },
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 个字符', trigger: 'blur' },
    {
      pattern: /^(?=.*[a-zA-Z])(?=.*\d).+$/,
      message: '密码必须包含字母和数字',
      trigger: 'blur',
    },
  ],
  roleIds: [
    { required: true, type: 'array', message: '请选择角色', trigger: 'change' },
  ],
})

// ==================== 下拉选项数据 ====================

/** 角色选项列表 */
const roleOptions = ref([])

/** 部门树数据 */
const deptTreeData = ref([])

/** 加载角色选项 */
const loadRoleOptions = async () => {
  try {
    const res = await getRoleOptions()
    roleOptions.value = res.data || []
  } catch (error) {
    console.error('加载角色选项失败:', error)
  }
}

/** 加载部门树 */
const loadDeptTree = async () => {
  try {
    const res = await getDeptTree()
    deptTreeData.value = res.data || []
  } catch (error) {
    console.error('加载部门树失败:', error)
  }
}

// ==================== 弹窗操作 ====================

/**
 * 重置表单数据到初始状态
 */
const resetForm = () => {
  formData.id = undefined
  formData.username = ''
  formData.nickname = ''
  formData.phone = ''
  formData.email = ''
  formData.password = ''
  formData.roleIds = []
  formData.deptId = undefined
  formData.status = 1

  // 清除校验状态（重要！否则残留上一次的校验错误）
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

/**
 * 打开新增弹窗
 */
const handleAdd = () => {
  dialogMode.value = 'add'
  dialogTitle.value = '新增用户'
  resetForm()
  dialogVisible.value = true

  // 加载下拉选项数据
  loadRoleOptions()
  loadDeptTree()
}

/**
 * 打开编辑弹窗
 * @param {Object} row - 当前行的用户数据
 */
const handleEdit = async (row) => {
  dialogMode.value = 'edit'
  dialogTitle.value = '编辑用户'
  resetForm()

  // 加载下拉选项数据
  await Promise.all([loadRoleOptions(), loadDeptTree()])

  // 如果有详情接口，调用详情接口获取最新数据
  // const res = await getUserDetail(row.id)
  // Object.assign(formData, res.data)

  // 这里直接用行数据回填（简单场景够用）
  Object.assign(formData, {
    id: row.id,
    username: row.username,
    nickname: row.nickname,
    phone: row.phone,
    email: row.email,
    roleIds: row.roleIds || [],
    deptId: row.deptId,
    status: row.status,
  })

  dialogVisible.value = true
}

/**
 * 弹窗关闭后的回调（清理表单状态）
 */
const handleDialogClosed = () => {
  resetForm()
}

/**
 * 提交表单（新增或编辑）
 */
const handleSubmit = async () => {
  // 1. 先进行表单校验
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (dialogMode.value === 'add') {
      // 新增模式
      await createUser(formData)
      ElMessage.success('新增成功')
    } else {
      // 编辑模式
      const { id, ...data } = formData
      await updateUser(id, data)
      ElMessage.success('修改成功')
    }

    dialogVisible.value = false
    refresh() // 刷新表格数据
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}
```

### 6.3 新增与编辑的区别处理

| 对比项 | 新增模式 | 编辑模式 |
|--------|---------|---------|
| 弹窗标题 | "新增用户" | "编辑用户" |
| 密码字段 | 显示（必填） | 隐藏 |
| 用户名 | 可编辑 | 禁用（不可修改） |
| 提交接口 | `createUser(formData)` | `updateUser(id, data)` |
| 提交后 | 刷新当前页 | 刷新当前页 |

> **要点说明：**
> - 新增和编辑共用同一个 `el-dialog`，通过 `dialogMode` 变量控制行为差异。
> - `v-if="dialogMode === 'add'"` 控制密码字段仅在新增时显示。
> - `:disabled="dialogMode === 'edit'"` 让用户名在编辑时不可修改（用户名通常不允许修改）。
> - `handleDialogClosed` 在弹窗关闭动画结束后清空表单，避免下次打开时残留旧数据。
> - `resetForm` 中使用 `nextTick` + `clearValidate` 清除校验状态，否则上一次的校验错误提示不会消失。

---

## 步骤7：实现删除功能

> 删除功能包含单条删除和批量删除两种。删除前必须进行二次确认，防止误操作。

### 7.1 单条删除

```js
/**
 * 单条删除
 * @param {Object} row - 当前行数据
 */
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除用户「${row.username}」吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
    .then(async () => {
      try {
        await deleteUser(row.id)
        ElMessage.success('删除成功')
        refresh() // 刷新当前页
      } catch (error) {
        console.error('删除失败:', error)
      }
    })
    .catch(() => {
      // 用户点击了"取消"，什么都不做
    })
}
```

### 7.2 批量删除

```js
/**
 * 批量删除
 * 需要先选中至少一条数据
 */
const handleBatchDelete = () => {
  const ids = selection.value.map((row) => row.id)

  ElMessageBox.confirm(
    `确定要删除选中的 ${ids.length} 个用户吗？`,
    '批量删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
    .then(async () => {
      try {
        // 批量删除接口（传入 ID 数组）
        // 方式1：数组作为请求体
        await request.post('/system/user/batchDelete', { ids })
        // 方式2：数组作为 URL 参数（逗号分隔）
        // await deleteUser(ids.join(','))

        ElMessage.success(`成功删除 ${ids.length} 个用户`)
        refresh()
      } catch (error) {
        console.error('批量删除失败:', error)
      }
    })
    .catch(() => {
      // 用户点击了"取消"
    })
}
```

### 7.3 删除的注意事项

```
场景1：删除当前页最后一条数据
    ↓
数据刷新后当前页变空
    ↓
应该自动跳转到上一页
    ↓
解决方案：删除成功后判断是否需要回退页码
```

**增强版删除（处理页码回退）：**

```js
/**
 * 删除后智能刷新
 * 如果当前页已经没有数据了，自动回退到上一页
 */
const smartRefresh = () => {
  if (
    tableData.value.length === 1 &&
    pagination.currentPage > 1
  ) {
    pagination.currentPage -= 1
  }
  refresh()
}

// 在删除成功后使用 smartRefresh 替代 refresh
// await deleteUser(row.id)
// ElMessage.success('删除成功')
// smartRefresh()
```

> **要点说明：**
> - `ElMessageBox.confirm` 弹出确认框，是删除操作的标准做法。
> - `.then()` 处理确认，`.catch()` 处理取消（点击取消或按 ESC 时触发）。
> - 批量删除的 `ids` 从 `selection.value`（useTable 维护的多选数据）中获取。
> - 工具栏中的批量删除按钮通过 `:disabled="selection.length === 0"` 控制可用状态。

---

## 步骤8：实现导出功能

> 导出功能需要将后端返回的文件流（Blob）处理成文件下载。

### 8.1 导出函数

```js
/**
 * 导出用户列表
 * 将搜索条件作为导出参数，导出当前筛选结果
 */
const handleExport = () => {
  ElMessageBox.confirm('确定要导出当前筛选的用户数据吗？', '导出确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info',
  })
    .then(async () => {
      try {
        // 构建导出参数（使用当前搜索条件）
        const params = {
          username: searchForm.username || undefined,
          phone: searchForm.phone || undefined,
          status: searchForm.status !== '' ? searchForm.status : undefined,
        }

        // 调用导出接口（返回 Blob）
        const res = await exportUser(params)

        // 处理文件下载
        downloadBlob(res, `用户列表_${formatDate(new Date())}.xlsx`)

        ElMessage.success('导出成功')
      } catch (error) {
        console.error('导出失败:', error)
      }
    })
    .catch(() => {})
}

/**
 * 将 Blob 数据下载为文件
 * @param {Blob} blob     - 二进制数据
 * @param {string} filename - 文件名
 */
const downloadBlob = (blob, filename) => {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url) // 释放内存
}
```

### 8.2 导出工作流程

```
用户点击"导出"按钮
    ↓
弹出确认框
    ↓
确认后调用 exportUser API（responseType: 'blob'）
    ↓
后端返回二进制流（Excel 文件）
    ↓
前端使用 Blob + URL.createObjectURL 处理
    ↓
创建隐藏的 <a> 标签触发下载
    ↓
释放 URL 对象内存
    ↓
提示"导出成功"
```

### 8.3 导出接口的特殊处理

> `request.js` 中需要处理 Blob 响应的特殊情况。因为 Blob 响应不会走 JSON 解析逻辑，但错误响应仍然是 JSON 格式。

如果第2章的 `request.js` 响应拦截器没有处理 Blob 错误，可以单独在导出函数中处理：

```js
const handleExport = async () => {
  try {
    const res = await exportUser(params)

    // 检查是否是错误响应（Blob 类型需要特殊判断）
    if (res.type === 'application/json') {
      // 后端返回的是 JSON（错误信息），不是文件
      const text = await res.text()
      const errorData = JSON.parse(text)
      ElMessage.error(errorData.message || '导出失败')
      return
    }

    downloadBlob(res, filename)
  } catch (error) {
    ElMessage.error('导出失败')
  }
}
```

> **要点说明：**
> - 导出接口必须在 `request.js` 配置中设置 `responseType: 'blob'`，否则返回的数据会是乱码。
> - `downloadBlob` 函数是通用的文件下载工具，可以放到 `src/utils/index.js` 中复用。
> - 导出时使用当前的搜索条件作为参数，确保导出的数据和用户看到的一致。
> - 使用 `URL.createObjectURL` + `URL.revokeObjectURL` 管理内存，防止内存泄漏。

---

## 步骤9：完整可运行代码

> 下面是用户管理页面的完整代码，将上述所有功能整合到一起。每个区域都有清晰的注释标记，方便阅读和维护。

**文件路径：** `src/views/system/user/index.vue`

```vue
<template>
  <div class="page-container">

    <!-- ==================== 页面标题 ==================== -->
    <div class="page-header">
      <h2>用户管理</h2>
    </div>

    <!-- ==================== 搜索栏 ==================== -->
    <div class="search-bar">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="searchForm.username"
            placeholder="请输入用户名"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="searchForm.phone"
            placeholder="请输入手机号"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="searchForm.status" placeholder="用户状态" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- ==================== 操作按钮区 ==================== -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增
        </el-button>
        <el-button
          type="danger"
          :disabled="selection.length === 0"
          @click="handleBatchDelete"
        >
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
        <el-button type="warning" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </div>
      <div class="toolbar-right">
        <el-tooltip content="刷新">
          <el-button circle @click="refresh">
            <el-icon><Refresh /></el-icon>
          </el-button>
        </el-tooltip>
      </div>
    </div>

    <!-- ==================== 表格区 ==================== -->
    <Table
      :columns="columns"
      :data="tableData"
      :loading="loading"
      stripe
      @selection-change="handleSelectionChange"
    >
      <!-- 状态列：switch 开关 -->
      <template #status="{ row }">
        <el-switch
          :model-value="row.status === 1"
          @change="(val) => handleStatusChange(row, val)"
          inline-prompt
          active-text="启用"
          inactive-text="停用"
        />
      </template>

      <!-- 操作列 -->
      <template #action="{ row }">
        <el-button type="primary" link size="small" @click="handleEdit(row)">
          <el-icon><Edit /></el-icon> 编辑
        </el-button>
        <el-button type="danger" link size="small" @click="handleDelete(row)">
          <el-icon><Delete /></el-icon> 删除
        </el-button>
        <el-button type="warning" link size="small" @click="handleResetPwd(row)">
          <el-icon><Key /></el-icon> 重置密码
        </el-button>
      </template>
    </Table>

    <!-- ==================== 分页区 ==================== -->
    <Pagination
      v-model:current-page="pagination.currentPage"
      v-model:page-size="pagination.pageSize"
      :total="pagination.total"
      :page-sizes="pageSizesConfig"
      @current-change="handleCurrentChange"
      @size-change="handleSizeChange"
    />

    <!-- ==================== 新增/编辑弹窗 ==================== -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="80px"
      >
        <!-- 用户名（新增可编辑，编辑禁用） -->
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="formData.username"
            placeholder="请输入用户名"
            :disabled="dialogMode === 'edit'"
            maxlength="20"
            show-word-limit
          />
        </el-form-item>

        <!-- 昵称 -->
        <el-form-item label="昵称" prop="nickname">
          <el-input
            v-model="formData.nickname"
            placeholder="请输入昵称"
            maxlength="20"
            show-word-limit
          />
        </el-form-item>

        <!-- 手机号 -->
        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="formData.phone"
            placeholder="请输入手机号"
            maxlength="11"
          />
        </el-form-item>

        <!-- 邮箱 -->
        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="formData.email"
            placeholder="请输入邮箱"
          />
        </el-form-item>

        <!-- 密码（仅新增时显示） -->
        <el-form-item label="密码" prop="password" v-if="dialogMode === 'add'">
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="请输入密码"
            show-password
            maxlength="20"
          />
        </el-form-item>

        <!-- 角色选择（多选） -->
        <el-form-item label="角色" prop="roleIds">
          <el-select
            v-model="formData.roleIds"
            multiple
            placeholder="请选择角色"
            style="width: 100%"
          >
            <el-option
              v-for="item in roleOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <!-- 部门选择（树形下拉） -->
        <el-form-item label="部门" prop="deptId">
          <el-tree-select
            v-model="formData.deptId"
            :data="deptTreeData"
            :props="{ label: 'label', value: 'id', children: 'children' }"
            placeholder="请选择部门"
            check-strictly
            :render-after-expand="false"
            style="width: 100%"
          />
        </el-form-item>

        <!-- 状态（单选） -->
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <!-- 弹窗底部按钮 -->
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- ==================== 重置密码弹窗 ==================== -->
    <el-dialog
      v-model="resetPwdDialogVisible"
      title="重置密码"
      width="450px"
      :close-on-click-modal="false"
      @closed="resetPwdForm.password = ''"
    >
      <el-form ref="resetPwdFormRef" :model="resetPwdForm" :rules="resetPwdRules">
        <el-form-item label="用户" label-width="60px">
          <span>{{ resetPwdForm.username }}</span>
        </el-form-item>
        <el-form-item label="新密码" prop="password" label-width="60px">
          <el-input
            v-model="resetPwdForm.password"
            type="password"
            placeholder="请输入新密码"
            show-password
            maxlength="20"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="resetPwdDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="resetPwdLoading"
          @click="handleResetPwdSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
/**
 * @file src/views/system/user/index.vue
 * @description 用户管理页面 -- 完整 CRUD 实现
 *
 * 功能清单：
 *   1. 分页查询（useTable + 搜索条件）
 *   2. 新增用户（弹窗 + 表单校验）
 *   3. 编辑用户（弹窗 + 数据回填）
 *   4. 删除用户（单条 + 批量 + 二次确认）
 *   5. 状态切换（el-switch）
 *   6. 重置密码（独立弹窗）
 *   7. 导出用户列表（Blob 文件下载）
 */

import { ref, reactive, nextTick } from 'vue'
import {
  Search, Refresh, Plus, Delete, Download, Edit, Key,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Table from '@/components/Table/index.vue'
import Pagination from '@/components/Pagination/index.vue'
import { useTable } from '@/hooks/useTable'
import {
  getUserList,
  createUser,
  updateUser,
  deleteUser,
  changeUserStatus,
  resetUserPassword,
  exportUser,
  getRoleOptions,
  getDeptTree,
} from '@/api/system/user'

// ================================================================
//  一、搜索与分页（使用 useTable 组合式函数）
// ================================================================

/** 搜索表单 */
const searchForm = reactive({
  username: '',
  phone: '',
  status: '',
})

/** useTable -- 自动管理分页、加载、搜索、重置、多选 */
const {
  tableData,        // 表格数据
  loading,          // 加载状态
  selection,        // 多选行数据
  pagination,       // 分页信息 { currentPage, pageSize, total }
  pageSizesConfig,  // 每页条数选项
  handleSearch,     // 搜索（重置页码 + 刷新）
  handleReset,      // 重置（清空表单 + 重置页码 + 刷新）
  handleSelectionChange, // 多选变化
  handleCurrentChange,   // 页码变化
  handleSizeChange,      // 每页条数变化
  refresh,          // 刷新当前页
} = useTable(getUserList, {
  searchForm,
})

// ================================================================
//  二、表格列配置
// ================================================================

const columns = [
  { type: 'selection', width: 55 },
  { type: 'index', label: '序号', width: 70 },
  { prop: 'username', label: '用户名', width: 120 },
  { prop: 'nickname', label: '昵称', width: 120 },
  { prop: 'phone', label: '手机号', width: 140 },
  { prop: 'deptName', label: '部门', width: 120 },
  {
    prop: 'status',
    label: '状态',
    width: 100,
    align: 'center',
    slot: 'status',
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 180,
    sortable: true,
  },
  {
    label: '操作',
    width: 220,
    fixed: 'right',
    align: 'center',
    slot: 'action',
  },
]

// ================================================================
//  三、状态切换
// ================================================================

/**
 * 切换用户启用/停用状态
 * @param {Object}  row - 当前行数据
 * @param {boolean} val - switch 新值（true=启用, false=停用）
 */
const handleStatusChange = async (row, val) => {
  const newStatus = val ? 1 : 0
  const statusText = val ? '启用' : '停用'

  try {
    await changeUserStatus(row.id, newStatus)
    row.status = newStatus // 本地更新
    ElMessage.success(`已${statusText}用户「${row.username}」`)
  } catch (error) {
    ElMessage.error(`操作失败：${error.message || '请稍后重试'}`)
  }
}

// ================================================================
//  四、新增/编辑弹窗
// ================================================================

/** 弹窗是否可见 */
const dialogVisible = ref(false)

/** 弹窗模式：'add' | 'edit' */
const dialogMode = ref('add')

/** 弹窗标题 */
const dialogTitle = ref('新增用户')

/** 提交按钮加载状态 */
const submitLoading = ref(false)

/** 表单引用 */
const formRef = ref(null)

/** 表单数据 */
const formData = reactive({
  id: undefined,
  username: '',
  nickname: '',
  phone: '',
  email: '',
  password: '',
  roleIds: [],
  deptId: undefined,
  status: 1,
})

/** 表单校验规则 */
const formRules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度为 2-20 个字符', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_]+$/,
      message: '用户名只能包含字母、数字和下划线',
      trigger: 'blur',
    },
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度为 2-20 个字符', trigger: 'blur' },
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 个字符', trigger: 'blur' },
    {
      pattern: /^(?=.*[a-zA-Z])(?=.*\d).+$/,
      message: '密码必须包含字母和数字',
      trigger: 'blur',
    },
  ],
  roleIds: [
    { required: true, type: 'array', message: '请选择角色', trigger: 'change' },
  ],
})

/** 角色下拉选项 */
const roleOptions = ref([])

/** 部门树数据 */
const deptTreeData = ref([])

/** 加载角色选项 */
const loadRoleOptions = async () => {
  try {
    const res = await getRoleOptions()
    roleOptions.value = res.data || []
  } catch (error) {
    console.error('加载角色选项失败:', error)
  }
}

/** 加载部门树 */
const loadDeptTree = async () => {
  try {
    const res = await getDeptTree()
    deptTreeData.value = res.data || []
  } catch (error) {
    console.error('加载部门树失败:', error)
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.id = undefined
  formData.username = ''
  formData.nickname = ''
  formData.phone = ''
  formData.email = ''
  formData.password = ''
  formData.roleIds = []
  formData.deptId = undefined
  formData.status = 1

  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

/** 打开新增弹窗 */
const handleAdd = () => {
  dialogMode.value = 'add'
  dialogTitle.value = '新增用户'
  resetForm()
  dialogVisible.value = true
  loadRoleOptions()
  loadDeptTree()
}

/** 打开编辑弹窗 */
const handleEdit = async (row) => {
  dialogMode.value = 'edit'
  dialogTitle.value = '编辑用户'
  resetForm()

  // 先加载下拉选项，再回填数据
  await Promise.all([loadRoleOptions(), loadDeptTree()])

  // 用行数据回填（生产环境建议调用 getUserDetail 获取最新数据）
  Object.assign(formData, {
    id: row.id,
    username: row.username,
    nickname: row.nickname,
    phone: row.phone,
    email: row.email,
    roleIds: row.roleIds || [],
    deptId: row.deptId,
    status: row.status,
  })

  dialogVisible.value = true
}

/** 弹窗关闭回调 */
const handleDialogClosed = () => {
  resetForm()
}

/** 提交表单 */
const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (dialogMode.value === 'add') {
      await createUser(formData)
      ElMessage.success('新增成功')
    } else {
      const { id, ...data } = formData
      await updateUser(id, data)
      ElMessage.success('修改成功')
    }

    dialogVisible.value = false
    refresh()
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

// ================================================================
//  五、删除功能（单条 + 批量）
// ================================================================

/**
 * 删除后智能刷新
 * 如果当前页只有一条数据且不是第一页，自动回退到上一页
 */
const smartRefresh = () => {
  if (tableData.value.length === 1 && pagination.currentPage > 1) {
    pagination.currentPage -= 1
  }
  refresh()
}

/** 单条删除 */
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除用户「${row.username}」吗？`,
    '删除确认',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  )
    .then(async () => {
      try {
        await deleteUser(row.id)
        ElMessage.success('删除成功')
        smartRefresh()
      } catch (error) {
        console.error('删除失败:', error)
      }
    })
    .catch(() => {})
}

/** 批量删除 */
const handleBatchDelete = () => {
  const ids = selection.value.map((row) => row.id)

  ElMessageBox.confirm(
    `确定要删除选中的 ${ids.length} 个用户吗？`,
    '批量删除确认',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  )
    .then(async () => {
      try {
        await deleteUser(ids.join(','))
        ElMessage.success(`成功删除 ${ids.length} 个用户`)
        smartRefresh()
      } catch (error) {
        console.error('批量删除失败:', error)
      }
    })
    .catch(() => {})
}

// ================================================================
//  六、重置密码
// ================================================================

/** 重置密码弹窗可见状态 */
const resetPwdDialogVisible = ref(false)

/** 重置密码加载状态 */
const resetPwdLoading = ref(false)

/** 重置密码表单引用 */
const resetPwdFormRef = ref(null)

/** 重置密码表单数据 */
const resetPwdForm = reactive({
  id: undefined,
  username: '',
  password: '',
})

/** 重置密码校验规则 */
const resetPwdRules = reactive({
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 个字符', trigger: 'blur' },
    {
      pattern: /^(?=.*[a-zA-Z])(?=.*\d).+$/,
      message: '密码必须包含字母和数字',
      trigger: 'blur',
    },
  ],
})

/** 打开重置密码弹窗 */
const handleResetPwd = (row) => {
  resetPwdForm.id = row.id
  resetPwdForm.username = row.username
  resetPwdForm.password = ''
  resetPwdDialogVisible.value = true
}

/** 提交重置密码 */
const handleResetPwdSubmit = async () => {
  const valid = await resetPwdFormRef.value?.validate().catch(() => false)
  if (!valid) return

  resetPwdLoading.value = true
  try {
    await resetUserPassword(resetPwdForm.id, resetPwdForm.password)
    ElMessage.success(`已重置用户「${resetPwdForm.username}」的密码`)
    resetPwdDialogVisible.value = false
  } catch (error) {
    console.error('重置密码失败:', error)
  } finally {
    resetPwdLoading.value = false
  }
}

// ================================================================
//  七、导出功能
// ================================================================

/**
 * 将 Blob 数据下载为文件
 * @param {Blob}   blob     - 二进制数据
 * @param {string} filename - 文件名
 */
const downloadBlob = (blob, filename) => {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

/** 导出用户列表 */
const handleExport = () => {
  ElMessageBox.confirm(
    '确定要导出当前筛选的用户数据吗？',
    '导出确认',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
  )
    .then(async () => {
      try {
        const params = {
          username: searchForm.username || undefined,
          phone: searchForm.phone || undefined,
          status: searchForm.status !== '' ? searchForm.status : undefined,
        }

        const res = await exportUser(params)

        // 处理可能的错误响应（后端返回 JSON 而非文件）
        if (res.type === 'application/json') {
          const text = await res.text()
          const errorData = JSON.parse(text)
          ElMessage.error(errorData.message || '导出失败')
          return
        }

        // 生成文件名
        const now = new Date()
        const dateStr = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`
        downloadBlob(res, `用户列表_${dateStr}.xlsx`)

        ElMessage.success('导出成功')
      } catch (error) {
        console.error('导出失败:', error)
        ElMessage.error('导出失败，请稍后重试')
      }
    })
    .catch(() => {})
}
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.page-header {
  margin-bottom: 16px;
}
.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.search-bar {
  margin-bottom: 16px;
  padding: 18px 20px 0;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.toolbar-left {
  display: flex;
  gap: 12px;
}
.toolbar-right {
  display: flex;
  gap: 8px;
}
</style>
```

---

## 模拟数据版本（无需后端即可运行）

> 如果你还没有后端接口，可以使用以下模拟 API 文件替代真实接口。直接替换 `src/api/system/user.js` 即可。

**文件路径：** `src/api/system/user.js`（模拟版本）

```js
/**
 * @file src/api/system/user.js
 * @description 用户管理模块 -- 模拟接口（无需后端）
 */

// ==================== 模拟数据 ====================

const mockUsers = Array.from({ length: 86 }, (_, i) => ({
  id: i + 1,
  username: `user${String(i + 1).padStart(3, '0')}`,
  nickname: ['张三', '李四', '王五', '赵六', '孙七', '周八', '吴九', '郑十'][i % 8],
  phone: `1${[3, 5, 7, 8, 9][i % 5]}${String(80000000 + i).slice(0, 8)}`,
  email: `user${String(i + 1).padStart(3, '0')}@example.com`,
  deptId: (i % 5) + 1,
  deptName: ['技术部', '产品部', '市场部', '人事部', '财务部'][i % 5],
  roleIds: [(i % 3) + 1],
  roleNames: ['管理员', '编辑', '用户'][i % 3],
  status: i % 5 === 0 ? 0 : 1,
  createTime: `2025-${String((i % 12) + 1).padStart(2, '0')}-${String((i % 28) + 1).padStart(2, '0')} ${String((i % 12) + 8).padStart(2, '0')}:${String(i % 60).padStart(2, '0')}:00`,
}))

const mockRoles = [
  { id: 1, name: '管理员' },
  { id: 2, name: '编辑' },
  { id: 3, name: '用户' },
]

const mockDeptTree = [
  {
    id: 1,
    label: '总公司',
    children: [
      { id: 2, label: '技术部' },
      { id: 3, label: '产品部' },
      { id: 4, label: '市场部' },
      { id: 5, label: '人事部' },
      { id: 6, label: '财务部' },
    ],
  },
]

let nextId = 87

// ==================== 工具函数 ====================

const delay = (ms = 300) => new Promise((resolve) => setTimeout(resolve, ms))

// ==================== 接口实现 ====================

/** 分页查询用户列表 */
export async function getUserList(params) {
  await delay()

  const { pageNum = 1, pageSize = 10, username, phone, status } = params
  let filtered = [...mockUsers]

  if (username) {
    filtered = filtered.filter((item) => item.username.includes(username))
  }
  if (phone) {
    filtered = filtered.filter((item) => item.phone.includes(phone))
  }
  if (status !== undefined && status !== '' && status !== null) {
    filtered = filtered.filter((item) => item.status === Number(status))
  }

  const total = filtered.length
  const start = (pageNum - 1) * pageSize
  const list = filtered.slice(start, start + pageSize)

  return { data: { list, total } }
}

/** 获取用户详情 */
export async function getUserDetail(id) {
  await delay(100)
  const user = mockUsers.find((item) => item.id === id)
  return { data: user || null }
}

/** 新增用户 */
export async function createUser(data) {
  await delay(200)
  const newUser = {
    ...data,
    id: nextId++,
    deptName: mockDeptTree[0].children.find((d) => d.id === data.deptId)?.label || '',
    roleNames: data.roleIds?.map((rid) => mockRoles.find((r) => r.id === rid)?.name).filter(Boolean) || [],
    createTime: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-'),
  }
  mockUsers.unshift(newUser)
  return { data: null }
}

/** 编辑用户 */
export async function updateUser(id, data) {
  await delay(200)
  const index = mockUsers.findIndex((item) => item.id === id)
  if (index !== -1) {
    Object.assign(mockUsers[index], data, {
      deptName: mockDeptTree[0].children.find((d) => d.id === data.deptId)?.label || '',
      roleNames: data.roleIds?.map((rid) => mockRoles.find((r) => r.id === rid)?.name).filter(Boolean) || [],
    })
  }
  return { data: null }
}

/** 删除用户 */
export async function deleteUser(id) {
  await delay(200)
  if (typeof id === 'string') {
    // 批量删除（逗号分隔的 ID 字符串）
    const ids = id.split(',').map(Number)
    ids.forEach((delId) => {
      const index = mockUsers.findIndex((item) => item.id === delId)
      if (index !== -1) mockUsers.splice(index, 1)
    })
  } else {
    // 单条删除
    const index = mockUsers.findIndex((item) => item.id === id)
    if (index !== -1) mockUsers.splice(index, 1)
  }
  return { data: null }
}

/** 修改用户状态 */
export async function changeUserStatus(id, status) {
  await delay(100)
  const user = mockUsers.find((item) => item.id === id)
  if (user) user.status = status
  return { data: null }
}

/** 重置用户密码 */
export async function resetUserPassword(id, password) {
  await delay(200)
  console.log(`[模拟] 重置用户 ${id} 的密码为: ${password}`)
  return { data: null }
}

/** 导出用户列表 */
export async function exportUser(params) {
  await delay(500)
  // 模拟返回一个简单的文本 Blob（实际项目中是 Excel 文件）
  const content = 'ID,用户名,昵称,手机号,状态\n' + mockUsers
    .map((u) => `${u.id},${u.username},${u.nickname},${u.phone},${u.status === 1 ? '启用' : '停用'}`)
    .join('\n')
  return new Blob([content], { type: 'text/csv;charset=utf-8;' })
}

/** 获取角色选项 */
export async function getRoleOptions() {
  await delay(100)
  return { data: mockRoles }
}

/** 获取部门树 */
export async function getDeptTree() {
  await delay(100)
  return { data: mockDeptTree }
}
```

---

## 路由配置

> 最后别忘了在路由文件中注册用户管理页面。

**文件路径：** `src/router/index.js`（在对应的路由模块中添加）

```js
{
  path: '/system/user',
  name: 'UserManage',
  component: () => import('@/views/system/user/index.vue'),
  meta: {
    title: '用户管理',
    icon: 'User',
  },
}
```

---

## 文件结构总览

完成本章后，涉及的所有文件：

```
src/
├── api/
│   └── system/
│       └── user.js              ← 步骤1：API 接口定义
├── components/
│   ├── Table/
│   │   └── index.vue            ← 第5章封装的表格组件（复用）
│   └── Pagination/
│       └── index.vue            ← 第5章封装的分页组件（复用）
├── hooks/
│   └── useTable.js              ← 第5章封装的组合式函数（复用）
├── views/
│   └── system/
│       └── user/
│           └── index.vue         ← 步骤2-9：完整 CRUD 页面
└── router/
    └── index.js                 ← 路由配置（注册页面）
```

---

## 本章要点总结

### 代码组织原则

| 原则 | 说明 |
|------|------|
| **API 层分离** | 所有接口调用集中在 `src/api/` 目录，页面不直接写请求逻辑 |
| **useTable 复用** | 分页、搜索、重置等通用逻辑交给组合式函数，页面只关注业务 |
| **组件封装** | Table 和 Pagination 组件统一渲染逻辑，页面通过配置驱动 |
| **弹窗复用** | 新增和编辑共用同一个弹窗，通过 mode 区分行为差异 |
| **注释分隔** | 每个功能区域用注释标记，长文件也能快速定位 |

### 功能清单

| 功能 | 实现方式 | 关键技术点 |
|------|---------|-----------|
| 分页查询 | `useTable` 组合式函数 | 自动管理 pageNum/pageSize/total |
| 搜索筛选 | `useTable` + `searchForm` | 自动过滤空值、回车搜索 |
| 重置 | `useTable.handleReset` | 清空表单 + 重置页码 |
| 表格展示 | `Table` 组件 + `columns` 配置 | 配置驱动、插槽自定义 |
| 状态切换 | `el-switch` + API | `:model-value` 避免双向绑定闪烁 |
| 新增/编辑 | `el-dialog` + `el-form` | 共用弹窗、模式切换、表单校验 |
| 删除 | `ElMessageBox.confirm` | 单条 + 批量 + 智能页码回退 |
| 重置密码 | 独立弹窗 | 表单校验 |
| 导出 | `responseType: 'blob'` + `downloadBlob` | Blob 文件下载 |

### 举一反三

学完本章后，你可以用完全相同的模式搭建其他管理页面，只需修改以下内容：

1. **API 文件**：替换为对应模块的接口（如 `src/api/system/role.js`）
2. **搜索表单**：替换为对应模块的搜索字段（如角色名称、权限标识）
3. **列配置**：替换为对应模块的字段（如角色名、权限字符、排序）
4. **弹窗表单**：替换为对应模块的编辑字段（如角色名、权限字符、菜单权限）
5. **路由配置**：注册新的页面路径

```
用户管理 → 角色管理 → 菜单管理 → 部门管理 → 字典管理
  同样的模式，不同的字段
```

> **这就是后台管理系统页面开发的"万能模板"。** 掌握了这一个页面的完整写法，所有类似的管理页面都能举一反三。
