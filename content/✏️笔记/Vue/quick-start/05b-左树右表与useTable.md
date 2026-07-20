# 第五章（下）：左树右表布局与 useTable 组合式函数

## 五、左树右表布局

### 5.1 设计思路

"左树右表"是后台管理系统中非常经典的布局模式，常见场景：

| 场景 | 左侧树 | 右侧表格 |
|------|--------|----------|
| 部门管理 | 部门树 | 该部门下的人员列表 |
| 分类管理 | 分类树 | 该分类下的商品列表 |
| 菜单管理 | 菜单树 | 该菜单下的子菜单列表 |
| 组织架构 | 组织树 | 该组织下的员工列表 |

**核心交互：** 点击左侧树的某个节点 → 右侧表格自动刷新，显示该节点下的数据。

**目录结构：** `src/views/tree-table/TreeTable.vue`

### 5.2 完整代码

```vue
<template>
  <div class="tree-table-container">
    <div class="left-tree">
      <div class="tree-header">
        <span>部门列表</span>
        <el-button type="primary" link @click="toggleExpandAll">
          {{ isExpandAll ? '折叠全部' : '展开全部' }}
        </el-button>
      </div>
      <el-input
        v-model="treeFilterText"
        placeholder="搜索部门"
        clearable
        class="tree-search"
      />
      <el-tree
        ref="treeRef"
        :data="treeData"
        :props="treeProps"
        :expand-on-click-node="false"
        :highlight-current="true"
        :default-expand-all="isExpandAll"
        :filter-node-method="filterNode"
        node-key="id"
        @node-click="handleNodeClick"
      >
        <template #default="{ node, data }">
          <span class="custom-tree-node">
            <span class="node-label">{{ node.label }}</span>
            <span class="node-count">({{ data.count }})</span>
          </span>
        </template>
      </el-tree>
    </div>

    <div class="right-table">
      <div class="table-header">
        <h3>{{ currentNodeLabel }} - 人员列表</h3>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增
        </el-button>
      </div>

      <Table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        stripe
      >
        <template #status="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '在职' : '离职' }}
          </el-tag>
        </template>
        <template #action="{ row }">
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </Table>

      <Pagination
        v-model:current-page="pagination.currentPage"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        @change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import Table from '@/components/Table/index.vue'
import Pagination from '@/components/Pagination/index.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// ========== 左侧树 ==========
const treeRef = ref(null)
const treeFilterText = ref('')
const isExpandAll = ref(true)
const currentNodeId = ref(1)
const currentNodeLabel = ref('总公司')

const treeProps = {
  children: 'children',
  label: 'label',
}

const treeData = ref([
  {
    id: 1,
    label: '总公司',
    count: 15,
    children: [
      {
        id: 2,
        label: '技术部',
        count: 6,
        children: [
          { id: 5, label: '前端组', count: 3 },
          { id: 6, label: '后端组', count: 3 },
        ],
      },
      {
        id: 3,
        label: '产品部',
        count: 4,
        children: [
          { id: 7, label: '产品一组', count: 2 },
          { id: 8, label: '产品二组', count: 2 },
        ],
      },
      {
        id: 4,
        label: '市场部',
        count: 5,
      },
    ],
  },
])

// 树节点过滤
watch(treeFilterText, (val) => {
  treeRef.value?.filter(val)
})

const filterNode = (value, data) => {
  if (!value) return true
  return data.label.includes(value)
}

// 展开/折叠全部
const toggleExpandAll = () => {
  const nodes = treeRef.value?.store._getAllNodes()
  isExpandAll.value = !isExpandAll.value
  nodes?.forEach((node) => {
    node.expanded = isExpandAll.value
  })
}

// 树节点点击
const handleNodeClick = (data) => {
  currentNodeId.value = data.id
  currentNodeLabel.value = data.label
  pagination.currentPage = 1
  fetchData()
}

// ========== 右侧表格 ==========
const loading = ref(false)
const tableData = ref([])
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0,
})

const columns = [
  { type: 'index', label: '序号', width: 70 },
  { prop: 'name', label: '姓名', width: 100 },
  { prop: 'department', label: '部门', width: 120 },
  { prop: 'position', label: '职位', width: 120 },
  { prop: 'phone', label: '手机号', width: 140 },
  { prop: 'email', label: '邮箱', showOverflowTooltip: true },
  { prop: 'status', label: '状态', width: 80, align: 'center', slot: 'status' },
  { label: '操作', width: 160, fixed: 'right', align: 'center', slot: 'action' },
]

// ========== 模拟数据 ==========
const staffData = {
  1: [
    { id: 1, name: '王总', department: '总公司', position: '总经理', phone: '13800000001', email: 'wangzong@example.com', status: 1 },
    { id: 2, name: '李副总', department: '总公司', position: '副总经理', phone: '13800000002', email: 'lifuzong@example.com', status: 1 },
    { id: 3, name: '张主任', department: '技术部', position: '技术总监', phone: '13800000003', email: 'zhangzr@example.com', status: 1 },
    { id: 4, name: '刘经理', department: '产品部', position: '产品经理', phone: '13800000004', email: 'liujl@example.com', status: 1 },
    { id: 5, name: '陈经理', department: '市场部', position: '市场经理', phone: '13800000005', email: 'chenjl@example.com', status: 1 },
  ],
  2: [
    { id: 6, name: '张三', department: '技术部', position: '前端工程师', phone: '13800000006', email: 'zhangsan@example.com', status: 1 },
    { id: 7, name: '李四', department: '技术部', position: '后端工程师', phone: '13800000007', email: 'lisi@example.com', status: 1 },
    { id: 8, name: '王五', department: '技术部', position: '测试工程师', phone: '13800000008', email: 'wangwu@example.com', status: 0 },
  ],
  3: [
    { id: 9, name: '赵六', department: '产品部', position: '产品经理', phone: '13800000009', email: 'zhaoliu@example.com', status: 1 },
    { id: 10, name: '孙七', department: '产品部', position: '产品助理', phone: '13800000010', email: 'sunqi@example.com', status: 1 },
  ],
  4: [
    { id: 11, name: '周八', department: '市场部', position: '市场专员', phone: '13800000011', email: 'zhouba@example.com', status: 1 },
    { id: 12, name: '吴九', department: '市场部', position: '市场专员', phone: '13800000012', email: 'wujiu@example.com', status: 1 },
  ],
  5: [
    { id: 13, name: '郑十', department: '前端组', position: '前端开发', phone: '13800000013', email: 'zhengshi@example.com', status: 1 },
    { id: 14, name: '钱一', department: '前端组', position: '前端开发', phone: '13800000014', email: 'qianyi@example.com', status: 1 },
  ],
  6: [
    { id: 15, name: '冯二', department: '后端组', position: '后端开发', phone: '13800000015', email: 'fenger@example.com', status: 1 },
  ],
}

// ========== 数据加载 ==========
const fetchData = () => {
  loading.value = true
  // 实际项目中根据 currentNodeId 请求后端接口
  // const res = await getStaffByDept(currentNodeId.value, { pageNum, pageSize })
  const data = staffData[currentNodeId.value] || []
  pagination.total = data.length
  const start = (pagination.currentPage - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  tableData.value = data.slice(start, end)
  setTimeout(() => {
    loading.value = false
  }, 200)
}

const handlePageChange = () => {
  fetchData()
}

const handleAdd = () => {
  ElMessage.info(`在「${currentNodeLabel.value}」下新增人员`)
}

const handleEdit = (row) => {
  ElMessage.info(`编辑：${row.name}`)
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除「${row.name}」？`, '提示', {
    type: 'warning',
  }).then(() => {
    tableData.value = tableData.value.filter(item => item.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}
</script>

<style scoped>
.tree-table-container {
  display: flex;
  gap: 16px;
  height: calc(100vh - 120px);
}

/* 左侧树 */
.left-tree {
  width: 280px;
  min-width: 280px;
  padding: 16px;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.tree-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 600;
}

.tree-search {
  margin-bottom: 12px;
}

.left-tree .el-tree {
  flex: 1;
  overflow: auto;
}

.custom-tree-node {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.node-label {
  font-size: 14px;
}

.node-count {
  font-size: 12px;
  color: #909399;
}

/* 右侧表格 */
.right-table {
  flex: 1;
  padding: 16px;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.table-header h3 {
  margin: 0;
  font-size: 16px;
}

.right-table :deep(.el-table) {
  flex: 1;
}

.right-table :deep(.pagination-container) {
  padding: 12px 0 0;
}
</style>
```

### 5.3 关键要点

1. **布局结构：** 使用 `flex` 布局，左侧固定宽度，右侧自适应。外层容器设置 `height: calc(100vh - 120px)` 撑满视口，左侧树和右侧表格各自内部滚动。
2. **树节点点击联动：** `@node-click` 回调中更新 `currentNodeId`，重置分页到第一页，然后调用 `fetchData()` 刷新右侧数据。
3. **树节点搜索：** `el-tree` 的 `filter-node-method` 配合 `watch` 监听搜索关键词，实时过滤树节点。
4. **父子节点数据关系：** 实际项目中，父节点（如"总公司"）可能需要聚合所有子部门的数据，也可能只显示直接下级。这取决于业务需求，通常在接口层面处理。

---

## 六、useTable 组合式函数

### 6.1 设计思路

在实际开发中，每个表格页面的逻辑高度相似：分页参数管理、数据加载、搜索/重置、多选管理。我们将这些通用逻辑抽取为 `useTable` 组合式函数，减少每个页面的重复代码。

**目录结构：**

```
src/hooks/
└── useTable.js
```

### 6.2 完整代码

**文件：** `src/hooks/useTable.js`

```js
import { ref, reactive, onMounted } from 'vue'

/**
 * 通用表格组合式函数
 * @param {Function} apiFn     - 获取数据的 API 函数（必须返回 Promise）
 * @param {Object}   options   - 配置选项
 * @param {Object}   options.searchForm    - 搜索表单对象（reactive）
 * @param {Function} options.beforeFetch  - 请求前的参数处理函数（可选）
 * @param {Function} options.afterFetch   - 请求后的数据处理函数（可选）
 * @param {Boolean}  options.immediate     - 是否在 onMounted 时立即加载（默认 true）
 * @param {Number}   options.defaultPageSize - 默认每页条数（默认 10）
 * @param {Array}    options.pageSizes     - 每页条数选项（默认 [10, 20, 50, 100]）
 */
export function useTable(apiFn, options = {}) {
  const {
    searchForm: searchFormRef,
    beforeFetch,
    afterFetch,
    immediate = true,
    defaultPageSize = 10,
    pageSizes = [10, 20, 50, 100],
  } = options

  // ========== 分页状态 ==========
  const pagination = reactive({
    currentPage: 1,
    pageSize: defaultPageSize,
    total: 0,
  })

  const pageSizesConfig = pageSizes

  // ========== 表格状态 ==========
  const tableData = ref([])
  const loading = ref(false)
  const selection = ref([]) // 多选行

  // ========== 核心：加载数据 ==========
  const fetchData = async () => {
    loading.value = true
    try {
      // 构建请求参数
      let params = {
        pageNum: pagination.currentPage,
        pageSize: pagination.pageSize,
      }

      // 合并搜索条件
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

      // 请求前钩子（可修改参数）
      if (beforeFetch) {
        params = beforeFetch(params)
      }

      // 调用 API
      const res = await apiFn(params)

      // 处理响应数据（兼容不同的后端返回格式）
      let list = []
      let total = 0

      if (Array.isArray(res)) {
        // 响应直接是数组
        list = res
        total = res.length
      } else if (res.data) {
        // 响应是对象 { data: { list, total } } 或 { data: { records, total } }
        list = res.data.list || res.data.records || res.data.data || []
        total = res.data.total || list.length
      } else if (res.list || res.records) {
        list = res.list || res.records
        total = res.total || list.length
      }

      // 请求后钩子（可加工数据）
      if (afterFetch) {
        list = afterFetch(list)
      }

      tableData.value = list
      pagination.total = total
    } catch (error) {
      console.error('useTable fetchData error:', error)
      tableData.value = []
      pagination.total = 0
    } finally {
      loading.value = false
    }
  }

  // ========== 分页事件 ==========
  const handleCurrentChange = (page) => {
    pagination.currentPage = page
    fetchData()
  }

  const handleSizeChange = (size) => {
    pagination.pageSize = size
    pagination.currentPage = 1
    fetchData()
  }

  // ========== 搜索与重置 ==========
  const handleSearch = () => {
    pagination.currentPage = 1
    fetchData()
  }

  const handleReset = () => {
    // 重置搜索表单
    if (searchFormRef) {
      Object.keys(searchFormRef).forEach((key) => {
        searchFormRef[key] = ''
      })
    }
    pagination.currentPage = 1
    pagination.pageSize = defaultPageSize
    fetchData()
  }

  // ========== 多选事件 ==========
  const handleSelectionChange = (val) => {
    selection.value = val
  }

  // ========== 手动刷新（保持当前页码） ==========
  const refresh = () => {
    fetchData()
  }

  // ========== 初始化 ==========
  if (immediate) {
    onMounted(() => {
      fetchData()
    })
  }

  // ========== 返回 ==========
  return {
    // 状态
    tableData,
    loading,
    selection,
    pagination,
    pageSizesConfig,
    // 方法
    fetchData,
    handleCurrentChange,
    handleSizeChange,
    handleSearch,
    handleReset,
    handleSelectionChange,
    refresh,
  }
}
```

### 6.3 使用示例

**场景：用户管理页面**

**目录结构：** `src/views/system/user/UserList.vue`

```vue
<template>
  <div class="page-container">
    <h2>用户管理</h2>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="searchForm.phone" placeholder="请输入手机号" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable>
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

    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon> 新增
      </el-button>
      <el-button type="danger" :disabled="selection.length === 0" @click="handleBatchDelete">
        批量删除
      </el-button>
    </div>

    <!-- 表格 -->
    <Table
      :columns="columns"
      :data="tableData"
      :loading="loading"
      stripe
      @selection-change="handleSelectionChange"
    >
      <template #status="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
          {{ row.status === 1 ? '启用' : '停用' }}
        </el-tag>
      </template>
      <template #action="{ row }">
        <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
        <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
      </template>
    </Table>

    <!-- 分页 -->
    <Pagination
      v-model:current-page="pagination.currentPage"
      v-model:page-size="pagination.pageSize"
      :total="pagination.total"
      :page-sizes="pageSizesConfig"
      @change="handleCurrentChange"
    />
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import Table from '@/components/Table/index.vue'
import Pagination from '@/components/Pagination/index.vue'
import { useTable } from '@/hooks/useTable'
import { getUserList, deleteUser, batchDeleteUser } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'

// 搜索表单
const searchForm = reactive({
  username: '',
  phone: '',
  status: '',
})

// 使用 useTable 组合式函数 —— 一行代码搞定分页 + 数据加载 + 搜索重置
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
  { prop: 'id', label: 'ID', width: 80, align: 'center' },
  { prop: 'username', label: '用户名', width: 120 },
  { prop: 'phone', label: '手机号', width: 140 },
  { prop: 'email', label: '邮箱', showOverflowTooltip: true },
  { prop: 'roleName', label: '角色', width: 120 },
  { prop: 'status', label: '状态', width: 80, align: 'center', slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 180, sortable: true },
  { label: '操作', width: 160, fixed: 'right', align: 'center', slot: 'action' },
]

// 新增
const handleAdd = () => {
  ElMessage.info('打开新增弹窗')
}

// 编辑
const handleEdit = (row) => {
  ElMessage.info(`编辑用户：${row.username}`)
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除用户「${row.username}」？`, '提示', {
    type: 'warning',
  }).then(async () => {
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    refresh() // 刷新当前页
  }).catch(() => {})
}

// 批量删除
const handleBatchDelete = () => {
  const ids = selection.value.map(row => row.id)
  ElMessageBox.confirm(`确定删除选中的 ${ids.length} 个用户？`, '提示', {
    type: 'warning',
  }).then(async () => {
    await batchDeleteUser(ids)
    ElMessage.success('批量删除成功')
    refresh()
  }).catch(() => {})
}
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
.toolbar {
  margin-bottom: 16px;
  display: flex;
  gap: 12px;
}
</style>
```

### 6.4 模拟 API 接口（配合演示）

**文件：** `src/api/user.js`（模拟版本，实际项目中替换为真实接口）

```js
// 模拟用户列表接口
// 实际项目中替换为：
// import request from '@/utils/request'
// export const getUserList = (params) => request.get('/api/users', { params })

const mockUsers = Array.from({ length: 86 }, (_, i) => ({
  id: i + 1,
  username: `user${String(i + 1).padStart(3, '0')}`,
  phone: `138${String(10000000 + i).slice(0, 8)}`,
  email: `user${String(i + 1).padStart(3, '0')}@example.com`,
  roleName: ['管理员', '编辑', '用户'][i % 3],
  status: i % 3 === 0 ? 0 : 1,
  createTime: `2025-${String((i % 12) + 1).padStart(2, '0')}-${String((i % 28) + 1).padStart(2, '0')} ${String((i % 12) + 8).padStart(2, '0')}:00:00`,
}))

export function getUserList(params) {
  return new Promise((resolve) => {
    setTimeout(() => {
      const { pageNum = 1, pageSize = 10, username, phone, status } = params

      let filtered = [...mockUsers]
      if (username) {
        filtered = filtered.filter(item => item.username.includes(username))
      }
      if (phone) {
        filtered = filtered.filter(item => item.phone.includes(phone))
      }
      if (status !== undefined && status !== '' && status !== null) {
        filtered = filtered.filter(item => item.status === Number(status))
      }

      const total = filtered.length
      const start = (pageNum - 1) * pageSize
      const list = filtered.slice(start, start + pageSize)

      resolve({
        data: { list, total },
      })
    }, 300)
  })
}

export function deleteUser(id) {
  return new Promise((resolve) => {
    setTimeout(() => resolve({ data: null }), 200)
  })
}

export function batchDeleteUser(ids) {
  return new Promise((resolve) => {
    setTimeout(() => resolve({ data: null }), 200)
  })
}
```

### 6.5 useTable 高级用法

**传入 `beforeFetch` 钩子，在请求前加工参数：**

```js
const { tableData, loading, pagination, ... } = useTable(getUserList, {
  searchForm,
  // 请求前处理参数（例如添加额外筛选条件）
  beforeFetch: (params) => {
    // 将日期范围拆分为开始和结束时间
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
      delete params.dateRange // 后端不需要原始日期范围
    }
    return params
  },
  // 请求后处理数据（例如状态码转文字）
  afterFetch: (list) => {
    return list.map(item => ({
      ...item,
      statusText: item.status === 1 ? '启用' : '停用',
    }))
  },
})
```

**不自动加载（手动控制加载时机，例如左树右表场景）：**

```js
// 左树右表场景中，右侧表格不自动加载
// 需要等左侧树选中节点后再加载
const { tableData, loading, pagination, fetchData, ...rest } = useTable(
  getStaffByDept,
  {
    immediate: false, // 不自动加载
  }
)

// 左侧树节点点击时调用
const handleNodeClick = (node) => {
  currentDeptId.value = node.id
  pagination.currentPage = 1
  // 手动调用 fetchData 并传入额外参数
  fetchData({ deptId: node.id })
}
```

> 注意：上面的用法需要 `useTable` 支持传入额外参数。如果需要这种用法，可以在 `useTable` 中增加一个 `fetchData(params)` 的重载支持：

```js
// 在 useTable 中增加参数重载
const fetchData = async (extraParams = {}) => {
  loading.value = true
  try {
    let params = {
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize,
      ...extraParams, // 合并额外参数
    }
    // ... 后续逻辑不变
  } finally {
    loading.value = false
  }
}
```

### 6.6 useTable 功能总结

| 功能 | 说明 |
|------|------|
| `tableData` | 表格数据（ref） |
| `loading` | 加载状态（ref） |
| `selection` | 当前选中行（ref） |
| `pagination` | 分页参数 `{ currentPage, pageSize, total }`（reactive） |
| `pageSizesConfig` | 每页条数选项（ref） |
| `fetchData()` | 手动刷新数据 |
| `handleSearch()` | 搜索（重置页码 + 刷新） |
| `handleReset()` | 重置搜索条件（清空表单 + 重置页码 + 刷新） |
| `handleSelectionChange()` | 多选变化回调 |
| `handleCurrentChange()` | 页码变化回调 |
| `handleSizeChange()` | 每页条数变化回调 |
| `refresh()` | 刷新当前页（保持页码不变） |

---

## 七、本章总结

### 7.1 知识点回顾

| 模块 | 核心内容 |
|------|----------|
| el-table 基础 | 列定义、常用属性、formatter、自定义插槽、多选/序号/操作列 |
| 分页联动 | el-pagination 属性、分页事件、搜索重置时重置页码 |
| 通用 Table 组件 | columns 配置驱动、插槽透传、属性透传 |
| 通用 Pagination 组件 | v-model 双向绑定、autoHide 自动隐藏 |
| 左树右表 | flex 布局、树节点联动表格、搜索过滤 |
| useTable | 分页/加载/搜索/重置/多选一体化封装 |

### 7.2 最佳实践清单

1. **columns 配置驱动**：避免在模板中写大量重复的 `el-table-column`，用配置数组 + 通用组件替代。
2. **搜索时重置页码**：无论搜索还是切换 pageSize，都要将 `currentPage` 重置为 `1`。
3. **使用 v-loading**：数据加载时给用户明确反馈。
4. **操作列固定右侧**：数据列较多时，将操作列 `fixed="right"` 确保始终可见。
5. **showOverflowTooltip**：内容较长的列开启此属性，避免撑破布局。
6. **useTable 减少重复**：每个表格页面使用 `useTable` 组合式函数，将重复逻辑收敛到一处。
7. **左树右表 flex 布局**：外层 flex + 内部 overflow:auto，确保树和表格各自独立滚动。

---

> **上一篇**：[第5章（上）el-table 基础与通用组件](./05a-el-table基础与通用组件.md)
>
> **下一章**：[第6章 树形与多级列表](./06-树形与多级列表.md)
>
> **章节总览**：[第5章 表格与分页](./05-表格与分页.md)
