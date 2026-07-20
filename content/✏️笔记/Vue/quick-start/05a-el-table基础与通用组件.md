# 第五章（上）：el-table 基础与通用组件封装

> 本章目标：掌握 Element Plus 中 el-table 的完整用法、分页联动机制、通用表格/分页组件封装、左树右表经典布局，以及 useTable 组合式函数的设计与使用。表格是后台管理系统中最核心的组件，本章是整个教程中最重要的章节之一。

---

## 一、基础表格 el-table

### 1.1 基本用法

`el-table` 是 Element Plus 中最常用的数据展示组件，配合 `el-table-column` 定义列。

**目录结构：** `src/views/table/BasicTable.vue`

```vue
<template>
  <div class="page-container">
    <h2>基础表格</h2>

    <!-- 最基本的表格 -->
    <el-table :data="tableData" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="age" label="年龄" width="80" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="role" label="角色" width="100" />
    </el-table>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const tableData = ref([
  { id: 1, name: '张三', age: 28, email: 'zhangsan@example.com', role: '管理员' },
  { id: 2, name: '李四', age: 32, email: 'lisi@example.com', role: '编辑' },
  { id: 3, name: '王五', age: 25, email: 'wangwu@example.com', role: '用户' },
  { id: 4, name: '赵六', age: 30, email: 'zhaoliu@example.com', role: '编辑' },
  { id: 5, name: '孙七', age: 27, email: 'sunqi@example.com', role: '用户' },
])
</script>

<style scoped>
.page-container {
  padding: 20px;
}
h2 {
  margin-bottom: 16px;
}
</style>
```

### 1.2 常用表格属性

| 属性 | 说明 | 类型 | 默认值 |
|------|------|------|--------|
| `data` | 显示的数据 | `array` | `[]` |
| `border` | 是否带有纵向边框 | `boolean` | `false` |
| `stripe` | 是否为斑马纹 | `boolean` | `false` |
| `height` | 固定表头的高度（像素） | `string / number` | — |
| `max-height` | 表格最大高度，超出后滚动 | `string / number` | — |
| `show-header` | 是否显示表头 | `boolean` | `true` |
| `highlight-current-row` | 是否高亮当前行 | `boolean` | `false` |
| `empty-text` | 空数据时的提示文本 | `string` | `'暂无数据'` |
| `row-key` | 行数据的 Key，用于优化渲染 | `function(row) / string` | — |
| `default-sort` | 默认排序 | `{ prop, order }` | — |
| `tooltip-effect` | tooltip 的主题 | `string` | `'dark'` |

```vue
<template>
  <!-- 斑马纹 + 边框 + 固定高度 + 高亮当前行 -->
  <el-table
    :data="tableData"
    stripe
    border
    height="400"
    highlight-current-row
    tooltip-effect="dark"
    :row-key="(row) => row.id"
  >
    <el-table-column prop="id" label="ID" width="80" />
    <el-table-column prop="name" label="姓名" width="120" />
    <el-table-column prop="email" label="邮箱" />
  </el-table>
</template>
```

### 1.3 列属性详解

| 属性 | 说明 | 类型 | 示例 |
|------|------|------|------|
| `prop` | 对应数据字段名 | `string` | `'name'` |
| `label` | 列标题 | `string` | `'姓名'` |
| `width` | 列宽度 | `string / number` | `120` |
| `min-width` | 列最小宽度（自适应） | `string / number` | `150` |
| `sortable` | 是否可排序 | `boolean / string` | `true` / `'custom'` |
| `fixed` | 列是否固定 | `boolean / string` | `'left'` / `'right'` |
| `formatter` | 格式化内容 | `function(row, col, val)` | 见下方示例 |
| `show-overflow-tooltip` | 超出时显示 tooltip | `boolean` | `true` |
| `align` | 对齐方式 | `string` | `'center'` / `'left'` / `'right'` |
| `header-align` | 表头对齐方式 | `string` | `'center'` |

```vue
<template>
  <el-table :data="tableData" border>
    <el-table-column
      prop="id"
      label="ID"
      width="80"
      align="center"
      fixed="left"
    />
    <el-table-column
      prop="name"
      label="姓名"
      width="120"
      show-overflow-tooltip
    />
    <el-table-column
      prop="status"
      label="状态"
      width="100"
      align="center"
      :formatter="statusFormatter"
    />
    <el-table-column
      prop="createTime"
      label="创建时间"
      width="180"
      sortable
      :formatter="timeFormatter"
    />
    <el-table-column
      prop="salary"
      label="薪资"
      width="120"
      align="right"
      :formatter="moneyFormatter"
    />
    <el-table-column
      prop="address"
      label="地址"
      min-width="200"
      show-overflow-tooltip
    />
  </el-table>
</template>

<script setup>
import { ref } from 'vue'

const tableData = ref([
  {
    id: 1,
    name: '张三',
    status: 1,
    createTime: '2025-06-15 09:30:00',
    salary: 15000,
    address: '北京市朝阳区建国路88号某某大厦12层',
  },
  {
    id: 2,
    name: '李四',
    status: 0,
    createTime: '2025-07-01 14:20:00',
    salary: 12000,
    address: '上海市浦东新区陆家嘴环路1000号',
  },
])

// 状态格式化：1 → 启用，0 → 停用
const statusFormatter = (row, column, cellValue) => {
  return cellValue === 1 ? '启用' : '停用'
}

// 时间格式化
const timeFormatter = (row, column, cellValue) => {
  if (!cellValue) return '-'
  return cellValue.replace('T', ' ')
}

// 金额格式化
const moneyFormatter = (row, column, cellValue) => {
  if (cellValue == null) return '-'
  return `¥ ${cellValue.toLocaleString()}`
}
</script>
```

### 1.4 自定义列内容（template #default）

`formatter` 只能处理简单的文本转换，当列中需要显示按钮、标签、图片等复杂内容时，需要使用作用域插槽。

```vue
<template>
  <el-table :data="tableData" border>
    <el-table-column prop="id" label="ID" width="80" align="center" />

    <el-table-column prop="name" label="姓名" width="120" />

    <!-- 自定义状态列：用 el-tag 显示不同颜色 -->
    <el-table-column prop="status" label="状态" width="100" align="center">
      <template #default="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'danger'">
          {{ row.status === 1 ? '启用' : '停用' }}
        </el-tag>
      </template>
    </el-table-column>

    <!-- 自定义头像列：显示图片 -->
    <el-table-column label="头像" width="80" align="center">
      <template #default="{ row }">
        <el-avatar :size="32" :src="row.avatar" />
      </template>
    </el-table-column>

    <!-- 自定义操作列 -->
    <el-table-column label="操作" width="200" align="center" fixed="right">
      <template #default="{ row }">
        <el-button type="primary" link @click="handleEdit(row)">
          编辑
        </el-button>
        <el-button type="danger" link @click="handleDelete(row)">
          删除
        </el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([
  { id: 1, name: '张三', status: 1, avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png' },
  { id: 2, name: '李四', status: 0, avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png' },
  { id: 3, name: '王五', status: 1, avatar: 'https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png' },
])

const handleEdit = (row) => {
  ElMessage.info(`编辑用户：${row.name}`)
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除用户「${row.name}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    tableData.value = tableData.value.filter(item => item.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}
</script>
```

> **要点说明：** 作用域插槽通过 `#default="{ row }"` 解构出当前行的数据对象 `row`，`row` 中包含了该行所有字段的值。

### 1.5 多选列与序号列

```vue
<template>
  <el-table
    :data="tableData"
    border
    stripe
    @selection-change="handleSelectionChange"
  >
    <!-- 多选列 -->
    <el-table-column type="selection" width="55" align="center" />

    <!-- 序号列 -->
    <el-table-column type="index" label="序号" width="70" align="center" />

    <el-table-column prop="id" label="ID" width="80" align="center" />
    <el-table-column prop="name" label="姓名" width="120" />
    <el-table-column prop="role" label="角色" width="120" />
    <el-table-column prop="email" label="邮箱" />

    <!-- 操作列固定在右侧 -->
    <el-table-column label="操作" width="200" align="center" fixed="right">
      <template #default="{ row }">
        <el-button type="primary" link @click="handleView(row)">查看</el-button>
        <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
        <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
      </template>
    </el-table-column>
  </el-table>

  <!-- 批量操作区域 -->
  <div class="batch-bar" v-if="selectedRows.length > 0">
    <span>已选择 {{ selectedRows.length }} 项</span>
    <el-button type="danger" @click="handleBatchDelete">批量删除</el-button>
    <el-button type="primary" @click="handleBatchExport">批量导出</el-button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([
  { id: 1, name: '张三', role: '管理员', email: 'zhangsan@example.com' },
  { id: 2, name: '李四', role: '编辑', email: 'lisi@example.com' },
  { id: 3, name: '王五', role: '用户', email: 'wangwu@example.com' },
  { id: 4, name: '赵六', role: '编辑', email: 'zhaoliu@example.com' },
  { id: 5, name: '孙七', role: '用户', email: 'sunqi@example.com' },
])

const selectedRows = ref([])

// 多选变化回调
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleView = (row) => ElMessage.info(`查看：${row.name}`)
const handleEdit = (row) => ElMessage.info(`编辑：${row.name}`)

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除「${row.name}」？`, '提示', {
    type: 'warning',
  }).then(() => {
    tableData.value = tableData.value.filter(item => item.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

const handleBatchDelete = () => {
  const ids = selectedRows.value.map(row => row.id)
  ElMessageBox.confirm(`确定删除选中的 ${ids.length} 条数据？`, '提示', {
    type: 'warning',
  }).then(() => {
    tableData.value = tableData.value.filter(item => !ids.includes(item.id))
    selectedRows.value = []
    ElMessage.success('批量删除成功')
  }).catch(() => {})
}

const handleBatchExport = () => {
  ElMessage.success(`导出 ${selectedRows.value.length} 条数据`)
}
</script>

<style scoped>
.batch-bar {
  margin-top: 12px;
  padding: 8px 16px;
  background: #ecf5ff;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.batch-bar span {
  font-size: 14px;
  color: #409eff;
}
</style>
```

> **要点说明：**
> - `type="selection"` 渲染为多选复选框列，配合 `@selection-change` 事件获取所有选中行。
> - `type="index"` 渲染为序号列，从 1 开始自增。如果分页场景下需要全局序号，可用 `:index="indexMethod"` 自定义计算。
> - `fixed="right"` 将操作列固定在表格右侧，横向滚动时始终可见。

---

## 二、表格 + 分页联动

### 2.1 el-pagination 完整属性

| 属性 | 说明 | 类型 | 默认值 |
|------|------|------|--------|
| `total` | 总条目数 | `number` | `0` |
| `page-size` | 每页条数 | `number` | `10` |
| `current-page` | 当前页码 | `number` | `1` |
| `page-sizes` | 每页条数选择器的选项 | `number[]` | `[10, 20, 30, 40, 50]` |
| `layout` | 组件布局（子组件名用逗号分隔） | `string` | `'prev, pager, next, jumper, ->, total'` |
| `background` | 是否为分页按钮添加背景色 | `boolean` | `false` |
| `small` | 是否使用小型分页样式 | `boolean` | `false` |
| `hide-on-single-page` | 只有一页时是否隐藏 | `boolean` | `false` |

**layout 子组件说明：**

| 名称 | 说明 |
|------|------|
| `total` | 显示总条数 |
| `sizes` | 每页条数选择器 |
| `prev` | 上一页按钮 |
| `pager` | 页码列表 |
| `next` | 下一页按钮 |
| `jumper` | 跳转输入框 |
| `->` | 后面的元素右对齐 |

### 2.2 完整分页联动示例

**目录结构：** `src/views/table/TableWithPagination.vue`

```vue
<template>
  <div class="page-container">
    <h2>表格 + 分页联动</h2>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.role" placeholder="请选择角色" clearable>
            <el-option label="管理员" value="admin" />
            <el-option label="编辑" value="editor" />
            <el-option label="用户" value="user" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <el-table
      :data="tableData"
      border
      stripe
      v-loading="loading"
      element-loading-text="加载中..."
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column type="index" label="序号" width="70" align="center" />
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="name" label="姓名" width="120" show-overflow-tooltip />
      <el-table-column prop="role" label="角色" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="roleTagType(row.role)">{{ row.role }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="180" sortable />
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleView(row)">查看</el-button>
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.currentPage"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        :background="true"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// ========== 搜索表单 ==========
const searchForm = reactive({
  name: '',
  role: '',
  status: '',
})

// ========== 分页参数 ==========
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0,
})

// ========== 表格数据 ==========
const tableData = ref([])
const loading = ref(false)

// ========== 模拟全部数据 ==========
const allData = ref([])
const generateMockData = () => {
  const roles = ['管理员', '编辑', '用户']
  const data = []
  for (let i = 1; i <= 86; i++) {
    data.push({
      id: i,
      name: `用户${String(i).padStart(3, '0')}`,
      role: roles[i % 3],
      status: i % 2 === 0 ? 1 : 0,
      email: `user${String(i).padStart(3, '0')}@example.com`,
      createTime: `2025-0${(i % 9) + 1}-${String((i % 28) + 1).padStart(2, '0')} 09:30:00`,
    })
  }
  return data
}

// ========== 数据加载（核心方法） ==========
const fetchData = () => {
  loading.value = true

  // 模拟前端过滤
  let filtered = [...allData.value]
  if (searchForm.name) {
    filtered = filtered.filter(item => item.name.includes(searchForm.name))
  }
  if (searchForm.role) {
    filtered = filtered.filter(item => item.role === searchForm.role)
  }
  if (searchForm.status !== '' && searchForm.status !== null) {
    filtered = filtered.filter(item => item.status === searchForm.status)
  }

  // 更新总数
  pagination.total = filtered.length

  // 前端分页切片
  const start = (pagination.currentPage - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  tableData.value = filtered.slice(start, end)

  // 模拟异步延迟
  setTimeout(() => {
    loading.value = false
  }, 300)
}

// ========== 分页事件处理 ==========
const handlePageChange = (page) => {
  pagination.currentPage = page
  fetchData()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  // 切换每页条数后，回到第一页
  pagination.currentPage = 1
  fetchData()
}

// ========== 搜索与重置 ==========
const handleSearch = () => {
  pagination.currentPage = 1  // 搜索时回到第一页
  fetchData()
}

const handleReset = () => {
  searchForm.name = ''
  searchForm.role = ''
  searchForm.status = ''
  pagination.currentPage = 1
  pagination.pageSize = 10
  fetchData()
}

// ========== 行操作 ==========
const roleTagType = (role) => {
  const map = { '管理员': 'danger', '编辑': 'warning', '用户': 'info' }
  return map[role] || ''
}

const handleView = (row) => ElMessage.info(`查看：${row.name}`)
const handleEdit = (row) => ElMessage.info(`编辑：${row.name}`)

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除「${row.name}」？`, '提示', {
    type: 'warning',
  }).then(() => {
    allData.value = allData.value.filter(item => item.id !== row.id)
    fetchData()
    ElMessage.success('删除成功')
  }).catch(() => {})
}

// ========== 初始化 ==========
onMounted(() => {
  allData.value = generateMockData()
  fetchData()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
}
h2 {
  margin-bottom: 16px;
}
.search-bar {
  margin-bottom: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 4px;
}
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
```

### 2.3 分页联动要点总结

**分页联动的核心流程：**

```
用户操作（翻页/切换条数/搜索/重置）
    ↓
修改分页参数（currentPage / pageSize）
    ↓
调用 fetchData() 重新请求数据
    ↓
后端返回新的 total + 当前页数据
    ↓
更新表格数据和分页总数
```

**关键注意事项：**

1. **搜索时重置页码：** 每次搜索条件变化后，必须将 `currentPage` 重置为 `1`，否则可能出现当前页超出新数据范围导致表格为空的问题。
2. **切换 pageSize 时重置页码：** 每页条数变化后，总页数可能减少，当前页可能不存在，需要回到第一页。
3. **v-model 双向绑定：** `v-model:current-page` 和 `v-model:page-size` 实现双向绑定，也可以不使用 v-model，改为在事件中手动更新。两种方式都可以，推荐使用 v-model 方式更简洁。
4. **后端分页 vs 前端分页：** 实际项目中分页由后端处理。前端将 `currentPage` 和 `pageSize` 传给后端接口，后端返回 `total` 和当前页的数据列表。上面的示例为了演示方便使用了前端分页。

---

## 三、封装通用表格组件

### 3.1 设计思路

后台系统中大量页面都包含表格，每次都写重复的 `el-table` 模板非常冗余。我们将表格封装为通用组件，通过 `columns` 配置数组来声明式地定义列。

**目录结构：**

```
src/components/Table/
└── index.vue
```

### 3.2 columns 配置设计

```js
// columns 数组中的每一项对应一列
const columns = [
  {
    type: 'index',       // 特殊类型：'selection' | 'index' | 'expand'
    label: '序号',
    width: 70,
    align: 'center',
  },
  {
    prop: 'name',        // 普通列
    label: '姓名',
    width: 120,
    sortable: true,      // 是否可排序：true | 'custom'
    showOverflowTooltip: true,
  },
  {
    prop: 'status',
    label: '状态',
    width: 100,
    slot: 'status',       // 使用插槽自定义渲染
    align: 'center',
  },
  {
    label: '操作',        // 操作列（无 prop）
    slot: 'action',       // 使用插槽自定义渲染
    width: 200,
    fixed: 'right',
    align: 'center',
  },
]
```

### 3.3 完整组件代码

**文件：** `src/components/Table/index.vue`

```vue
<template>
  <el-table
    v-bind="$attrs"
    :data="data"
    :border="border"
    :stripe="stripe"
    :row-key="rowKey"
    v-loading="loading"
    element-loading-text="加载中..."
    @selection-change="handleSelectionChange"
    @sort-change="handleSortChange"
  >
    <!-- 遍历 columns 生成列 -->
    <template v-for="col in columns" :key="col.prop || col.type || col.label">
      <!-- 序号列 -->
      <el-table-column
        v-if="col.type === 'index'"
        type="index"
        :label="col.label || '序号'"
        :width="col.width || 70"
        :align="col.align || 'center'"
        :index="col.indexMethod"
      />

      <!-- 多选列 -->
      <el-table-column
        v-else-if="col.type === 'selection'"
        type="selection"
        :width="col.width || 55"
        :align="col.align || 'center'"
        :selectable="col.selectable"
      />

      <!-- 展开列 -->
      <el-table-column
        v-else-if="col.type === 'expand'"
        type="expand"
        :width="col.width"
      >
        <template #default="scope">
          <slot :name="col.slot || 'expand'" v-bind="scope" />
        </template>
      </el-table-column>

      <!-- 自定义插槽列 -->
      <el-table-column
        v-else-if="col.slot"
        :prop="col.prop"
        :label="col.label"
        :width="col.width"
        :min-width="col.minWidth"
        :sortable="col.sortable"
        :fixed="col.fixed"
        :align="col.align"
        :header-align="col.headerAlign"
        :show-overflow-tooltip="col.showOverflowTooltip !== false"
      >
        <template #default="scope">
          <slot :name="col.slot" v-bind="scope" />
        </template>
      </el-table-column>

      <!-- 普通列（支持 formatter） -->
      <el-table-column
        v-else
        :prop="col.prop"
        :label="col.label"
        :width="col.width"
        :min-width="col.minWidth"
        :sortable="col.sortable"
        :fixed="col.fixed"
        :formatter="col.formatter"
        :align="col.align"
        :header-align="col.headerAlign"
        :show-overflow-tooltip="col.showOverflowTooltip !== false"
      />
    </template>

    <!-- 默认插槽：用于放置 append 内容等 -->
    <template #append v-if="$slots.append">
      <slot name="append" />
    </template>

    <!-- 空数据插槽 -->
    <template #empty v-if="$slots.empty">
      <slot name="empty" />
    </template>
  </el-table>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'

defineProps({
  // 列配置数组
  columns: {
    type: Array,
    required: true,
    default: () => [],
  },
  // 表格数据
  data: {
    type: Array,
    default: () => [],
  },
  // 加载状态
  loading: {
    type: Boolean,
    default: false,
  },
  // 是否带边框
  border: {
    type: Boolean,
    default: true,
  },
  // 是否斑马纹
  stripe: {
    type: Boolean,
    default: false,
  },
  // 行数据的 Key
  rowKey: {
    type: [String, Function],
    default: '',
  },
})

const emit = defineEmits(['selection-change', 'sort-change'])

const handleSelectionChange = (selection) => {
  emit('selection-change', selection)
}

const handleSortChange = ({ column, prop, order }) => {
  emit('sort-change', { column, prop, order })
}
</script>
```

### 3.4 使用示例

```vue
<template>
  <div class="page-container">
    <h2>通用表格组件使用</h2>

    <Table
      :columns="columns"
      :data="tableData"
      :loading="loading"
      stripe
      @selection-change="handleSelectionChange"
    >
      <!-- 状态列插槽 -->
      <template #status="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'danger'">
          {{ row.status === 1 ? '启用' : '停用' }}
        </el-tag>
      </template>

      <!-- 操作列插槽 -->
      <template #action="{ row }">
        <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
        <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
      </template>
    </Table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import Table from '@/components/Table/index.vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)

// 列配置 —— 声明式定义，简洁清晰
const columns = [
  { type: 'selection', width: 55 },
  { type: 'index', label: '序号', width: 70 },
  { prop: 'id', label: 'ID', width: 80, align: 'center' },
  { prop: 'name', label: '姓名', width: 120 },
  { prop: 'role', label: '角色', width: 120 },
  { prop: 'status', label: '状态', width: 100, align: 'center', slot: 'status' },
  { prop: 'email', label: '邮箱', showOverflowTooltip: true },
  { label: '操作', width: 180, fixed: 'right', align: 'center', slot: 'action' },
]

const tableData = ref([
  { id: 1, name: '张三', role: '管理员', status: 1, email: 'zhangsan@example.com' },
  { id: 2, name: '李四', role: '编辑', status: 0, email: 'lisi@example.com' },
  { id: 3, name: '王五', role: '用户', status: 1, email: 'wangwu@example.com' },
])

const handleSelectionChange = (selection) => {
  console.log('选中的行：', selection)
}

const handleEdit = (row) => ElMessage.info(`编辑：${row.name}`)
const handleDelete = (row) => ElMessage.info(`删除：${row.name}`)
</script>
```

> **要点说明：**
> - 组件通过 `v-bind="$attrs"` 透传所有未声明的属性到 `el-table`，这样 `height`、`highlight-current-row`、`default-sort` 等属性都能直接使用。
> - `columns` 配置了 `slot` 的列，会在组件内部渲染为具名插槽，使用时通过 `<template #slotName="{ row }">` 自定义内容。
> - `border` 默认为 `true`（后台系统表格通常有边框更清晰），可通过 `:border="false"` 覆盖。

---

## 四、封装通用分页组件

### 4.1 设计思路

分页组件需要：
- 支持双向绑定 `currentPage` 和 `pageSize`
- 自动在数据量少于第一页条数时隐藏
- 样式统一（右对齐 + 适当间距）

**目录结构：**

```
src/components/Pagination/
└── index.vue
```

### 4.2 完整组件代码

**文件：** `src/components/Pagination/index.vue`

```vue
<template>
  <div v-if="show" class="pagination-container">
    <el-pagination
      v-model:current-page="innerCurrentPage"
      v-model:page-size="innerPageSize"
      :page-sizes="innerPageSizes"
      :total="total"
      :background="background"
      :small="small"
      :layout="layout"
      @current-change="handleCurrentChange"
      @size-change="handleSizeChange"
    />
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'

const props = defineProps({
  // 当前页码（支持 v-model）
  currentPage: {
    type: Number,
    default: 1,
  },
  // 每页条数（支持 v-model）
  pageSize: {
    type: Number,
    default: 10,
  },
  // 总条数
  total: {
    type: Number,
    required: true,
    default: 0,
  },
  // 每页条数选项
  pageSizes: {
    type: Array,
    default: () => [10, 20, 50, 100],
  },
  // 布局
  layout: {
    type: String,
    default: 'total, sizes, prev, pager, next, jumper',
  },
  // 是否有背景色
  background: {
    type: Boolean,
    default: true,
  },
  // 小型样式
  small: {
    type: Boolean,
    default: false,
  },
  // 是否在不需要分页时自动隐藏
  autoHide: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['update:currentPage', 'update:pageSize', 'change'])

// 内部状态（用于 v-model 绑定）
const innerCurrentPage = computed({
  get: () => props.currentPage,
  set: (val) => emit('update:currentPage', val),
})

const innerPageSize = computed({
  get: () => props.pageSize,
  set: (val) => emit('update:pageSize', val),
})

const innerPageSizes = computed(() => props.pageSizes)

// 是否显示分页（总条数 <= 第一页条数时自动隐藏）
const show = computed(() => {
  if (!props.autoHide) return true
  return props.total > props.pageSizes[0]
})

// 页码变化
const handleCurrentChange = (page) => {
  emit('update:currentPage', page)
  emit('change', { page, pageSize: props.pageSize })
}

// 每页条数变化
const handleSizeChange = (size) => {
  emit('update:pageSize', size)
  emit('update:currentPage', 1) // 切换条数时回到第一页
  emit('change', { page: 1, pageSize: size })
}
</script>

<style scoped>
.pagination-container {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0 0;
}
</style>
```

### 4.3 使用示例

```vue
<template>
  <div class="page-container">
    <!-- 表格 -->
    <Table :columns="columns" :data="tableData" :loading="loading" stripe />

    <!-- 分页：v-model 双向绑定 -->
    <Pagination
      v-model:current-page="pagination.currentPage"
      v-model:page-size="pagination.pageSize"
      :total="pagination.total"
      :page-sizes="[10, 20, 50, 100]"
      @change="handlePageChange"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import Table from '@/components/Table/index.vue'
import Pagination from '@/components/Pagination/index.vue'

const loading = ref(false)
const tableData = ref([])
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0,
})

const columns = [
  { type: 'index', label: '序号', width: 70 },
  { prop: 'id', label: 'ID', width: 80, align: 'center' },
  { prop: 'name', label: '姓名', width: 120 },
  { prop: 'email', label: '邮箱' },
]

const fetchData = async () => {
  loading.value = true
  try {
    // 实际项目中调用后端接口
    // const res = await getUserList({
    //   pageNum: pagination.currentPage,
    //   pageSize: pagination.pageSize,
    // })
    // tableData.value = res.data.list
    // pagination.total = res.data.total

    // 模拟数据
    const allData = Array.from({ length: 86 }, (_, i) => ({
      id: i + 1,
      name: `用户${String(i + 1).padStart(3, '0')}`,
      email: `user${String(i + 1).padStart(3, '0')}@example.com`,
    }))
    pagination.total = allData.length
    const start = (pagination.currentPage - 1) * pagination.pageSize
    const end = start + pagination.pageSize
    tableData.value = allData.slice(start, end)
  } finally {
    loading.value = false
  }
}

const handlePageChange = ({ page, pageSize }) => {
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>
```

> **要点说明：**
> - 使用 `v-model:current-page` 和 `v-model:page-size` 实现双向绑定，父组件直接维护分页参数。
> - `autoHide` 属性默认为 `true`，当 `total` 小于等于 `pageSizes[0]`（默认 10）时自动隐藏分页组件。
> - `@change` 事件统一返回 `{ page, pageSize }`，父组件只需监听此事件即可触发数据刷新。

---

> **上一章**：[第4章 核心布局组件](./04-核心布局组件.md)
>
> **下一篇**：[第5章（下）左树右表与 useTable](./05b-左树右表与useTable.md)
>
> **章节总览**：[第5章 表格与分页](./05-表格与分页.md)
