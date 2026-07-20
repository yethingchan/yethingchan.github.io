# 第六章（上）：el-tree 与树形表格

> 本章目标：掌握 Element Plus 中 el-tree 与 el-table 树形数据的完整用法，包括基础展示、自定义节点、树形表格、懒加载、数据结构转换工具函数，以及综合的部门管理页面实战。

---

## 一、el-tree 基础用法

### 1.1 基本树形展示

`el-tree` 是 Element Plus 提供的树形控件，核心作用是将 **嵌套的树形数据** 渲染为可展开/折叠的树结构。

树形数据的标准格式是一个嵌套数组，每个节点包含 `id`、`label`、`children`：

```js
const treeData = [
  {
    id: 1,
    label: '技术部',
    children: [
      { id: 11, label: '前端组', children: [{ id: 111, label: 'Vue 小组' }] },
      { id: 12, label: '后端组' }
    ]
  },
  {
    id: 2,
    label: '产品部',
    children: [
      { id: 21, label: '产品设计组' },
      { id: 22, label: '用户研究组' }
    ]
  }
]
```

**关键属性说明：**

| 属性 | 说明 | 示例 |
|------|------|------|
| `data` | 树形数据数组 | `:data="treeData"` |
| `node-key` | 每个节点唯一标识的字段名 | `node-key="id"` |
| `default-expand-all` | 是否默认展开所有节点 | 布尔值 |
| `default-expanded-keys` | 默认展开的节点 key 数组 | `[1, 2]` |
| `highlight-current` | 是否高亮当前选中节点 | 布尔值 |
| `props` | 映射字段名（默认 id/label/children） | `:props="{ label: 'name' }"` |
| `show-checkbox` | 是否显示复选框 | 布尔值 |

### 1.2 完整基础示例

```vue
<!-- src/views/tree/BasicTree.vue -->
<template>
  <div class="basic-tree-demo">
    <h3>el-tree 基础用法</h3>

    <div class="tree-container">
      <!-- 默认展开全部 + 高亮当前 -->
      <el-tree
        :data="treeData"
        node-key="id"
        default-expand-all
        highlight-current
        :default-expanded-keys="[1, 2]"
        :props="defaultProps"
        @node-click="handleNodeClick"
      />
    </div>

    <div class="info-panel">
      <p v-if="selectedNode">
        <strong>选中节点：</strong>{{ selectedNode.label }}
      </p>
      <p v-else style="color: #999;">点击节点查看信息</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

// 树形数据
const treeData = ref([
  {
    id: 1,
    label: '技术部',
    children: [
      {
        id: 11,
        label: '前端组',
        children: [
          { id: 111, label: 'Vue 小组' },
          { id: 112, label: 'React 小组' }
        ]
      },
      {
        id: 12,
        label: '后端组',
        children: [
          { id: 121, label: 'Java 小组' },
          { id: 122, label: 'Go 小组' }
        ]
      },
      { id: 13, label: '测试组' }
    ]
  },
  {
    id: 2,
    label: '产品部',
    children: [
      { id: 21, label: '产品设计组' },
      { id: 22, label: '用户研究组' }
    ]
  },
  {
    id: 3,
    label: '运营部',
    children: [
      { id: 31, label: '内容运营' },
      { id: 32, label: '活动运营' }
    ]
  }
])

// 字段映射配置（如果数据字段名不是默认的 label/children）
const defaultProps = {
  children: 'children',
  label: 'label'
}

// 当前选中的节点
const selectedNode = ref(null)

// 节点点击事件
const handleNodeClick = (data, node) => {
  selectedNode.value = data
  console.log('点击的节点数据：', data)
  console.log('节点对象：', node)
}
</script>

<style scoped>
.basic-tree-demo {
  display: flex;
  gap: 40px;
  padding: 20px;
}

.tree-container {
  width: 280px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 12px;
}

.info-panel {
  flex: 1;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 4px;
}
</style>
```

### 1.3 复选框与获取选中

```vue
<!-- src/views/tree/CheckboxTree.vue -->
<template>
  <div class="checkbox-tree-demo">
    <h3>带复选框的树</h3>

    <el-tree
      ref="treeRef"
      :data="treeData"
      node-key="id"
      show-checkbox
      default-expand-all
      :default-checked-keys="[11, 21]"
      @check-change="handleCheckChange"
    />

    <div class="actions">
      <el-button @click="getCheckedNodes">获取选中节点</el-button>
      <el-button @click="getCheckedKeys">获取选中 Key</el-button>
      <el-button @click="setCheckedKeys">设置选中 [11, 22]</el-button>
    </div>

    <div class="result">
      <p><strong>当前选中的节点：</strong></p>
      <pre>{{ checkedResult }}</pre>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const treeRef = ref(null)

const treeData = ref([
  {
    id: 1,
    label: '权限管理',
    children: [
      { id: 11, label: '用户列表' },
      { id: 12, label: '角色管理' },
      { id: 13, label: '菜单管理' }
    ]
  },
  {
    id: 2,
    label: '系统设置',
    children: [
      { id: 21, label: '基本配置' },
      { id: 22, label: '日志管理' }
    ]
  }
])

const checkedResult = ref([])

// 勾选状态变化时的回调
const handleCheckChange = (data, checked, indeterminate) => {
  console.log('节点数据：', data)
  console.log('是否选中：', checked)
  console.log('是否半选：', indeterminate)
}

// 获取选中节点（完整数据对象）
const getCheckedNodes = () => {
  const nodes = treeRef.value.getCheckedNodes()
  checkedResult.value = nodes
  console.log('选中节点：', nodes)
}

// 获取选中节点的 key 数组
const getCheckedKeys = () => {
  const keys = treeRef.value.getCheckedKeys()
  console.log('选中 Key：', keys)
  checkedResult.value = keys
}

// 通过 key 设置选中状态
const setCheckedKeys = () => {
  treeRef.value.setCheckedKeys([11, 22])
}
</script>

<style scoped>
.checkbox-tree-demo {
  padding: 20px;
}

.actions {
  margin: 16px 0;
}

.result pre {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  font-size: 13px;
}
</style>
```

---

## 二、自定义节点内容

### 2.1 作用域插槽

`el-tree` 通过 `#default` 插槽允许自定义每个节点的渲染内容，插槽参数包含：

| 参数 | 说明 |
|------|------|
| `node` | 节点对象（包含 level、isLeaf、expanded 等属性和方法） |
| `data` | 该节点对应的数据对象 |

### 2.2 完整示例：组织架构树

每个节点显示 **名称 + 人数标记 + 编辑/删除按钮**：

```vue
<!-- src/views/tree/CustomNodeTree.vue -->
<template>
  <div class="custom-node-tree">
    <h3>自定义节点 - 组织架构树</h3>

    <div class="tree-wrapper">
      <el-input
        v-model="filterText"
        placeholder="输入关键字过滤"
        clearable
        style="margin-bottom: 12px;"
      />

      <el-tree
        ref="treeRef"
        :data="orgData"
        node-key="id"
        default-expand-all
        highlight-current
        :expand-on-click-node="false"
        :filter-node-method="filterNode"
        @node-click="handleNodeClick"
      >
        <!-- 自定义节点内容 -->
        <template #default="{ node, data }">
          <div class="custom-node">
            <!-- 节点图标 -->
            <el-icon class="node-icon" :color="getNodeColor(data)">
              <Folder v-if="data.children && data.children.length" />
              <Document v-else />
            </el-icon>

            <!-- 节点名称 -->
            <span class="node-label" :class="{ 'is-leaf': !data.children?.length }">
              {{ data.label }}
            </span>

            <!-- 人数标记 -->
            <el-tag
              v-if="data.count"
              size="small"
              type="info"
              class="node-count"
            >
              {{ data.count }}人
            </el-tag>

            <!-- 操作按钮（hover 时显示） -->
            <span class="node-actions" @click.stop>
              <el-button
                type="primary"
                link
                size="small"
                @click.stop="handleEdit(data)"
              >
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button
                type="primary"
                link
                size="small"
                @click.stop="handleAdd(data)"
              >
                <el-icon><Plus /></el-icon>
              </el-button>
              <el-popconfirm
                title="确定要删除该节点吗？"
                @confirm="handleDelete(node, data)"
              >
                <template #reference>
                  <el-button
                    type="danger"
                    link
                    size="small"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </template>
              </el-popconfirm>
            </span>
          </div>
        </template>
      </el-tree>
    </div>

    <!-- 右侧详情面板 -->
    <div class="detail-panel">
      <template v-if="currentNode">
        <h4>{{ currentNode.label }}</h4>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="节点 ID">{{ currentNode.id }}</el-descriptions-item>
          <el-descriptions-item label="层级深度">{{ currentLevel }}</el-descriptions-item>
          <el-descriptions-item label="人数">{{ currentNode.count || 0 }}</el-descriptions-item>
          <el-descriptions-item label="子节点数">
            {{ currentNode.children?.length || 0 }}
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <el-empty v-else description="点击左侧节点查看详情" />
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Folder, Document, Edit, Plus, Delete } from '@element-plus/icons-vue'

const treeRef = ref(null)
const filterText = ref('')
const currentNode = ref(null)
const currentLevel = ref(0)

// 组织架构数据
const orgData = ref([
  {
    id: 1,
    label: '总公司',
    count: 120,
    children: [
      {
        id: 11,
        label: '技术中心',
        count: 45,
        children: [
          { id: 111, label: '前端开发部', count: 15 },
          { id: 112, label: '后端开发部', count: 18 },
          { id: 113, label: '测试部', count: 12 }
        ]
      },
      {
        id: 12,
        label: '产品中心',
        count: 25,
        children: [
          { id: 121, label: '产品设计部', count: 10 },
          { id: 122, label: '用户研究部', count: 8 },
          { id: 123, label: '数据分析部', count: 7 }
        ]
      },
      {
        id: 13,
        label: '运营中心',
        count: 30,
        children: [
          { id: 131, label: '内容运营部', count: 12 },
          { id: 132, label: '市场推广部', count: 18 }
        ]
      },
      { id: 14, label: '行政人事部', count: 20 }
    ]
  }
])

// 根据是否有子节点返回不同图标颜色
const getNodeColor = (data) => {
  return data.children && data.children.length > 0 ? '#409eff' : '#67c23a'
}

// 过滤方法
const filterNode = (value, data) => {
  if (!value) return true
  return data.label.includes(value)
}

// 监听过滤文本，实时过滤树
watch(filterText, (val) => {
  treeRef.value.filter(val)
})

// 点击节点
const handleNodeClick = (data, node) => {
  currentNode.value = data
  currentLevel.value = node.level
}

// 编辑节点
const handleEdit = (data) => {
  ElMessage.info(`编辑节点：${data.label}`)
}

// 新增子节点
const handleAdd = (data) => {
  const newNode = {
    id: Date.now(),
    label: '新增部门',
    count: 0,
    children: []
  }
  if (!data.children) {
    data.children = []
  }
  data.children.push(newNode)
  ElMessage.success('已添加子节点')
}

// 删除节点
const handleDelete = (node, data) => {
  // 从父节点的 children 中移除
  const parent = node.parent
  const children = parent.data.children || parent.data
  const index = children.findIndex((item) => item.id === data.id)
  if (index !== -1) {
    children.splice(index, 1)
  }
  ElMessage.success(`已删除：${data.label}`)
}
</script>

<style scoped>
.custom-node-tree {
  display: flex;
  gap: 24px;
  padding: 20px;
}

.tree-wrapper {
  width: 360px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 16px;
}

.detail-panel {
  flex: 1;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 4px;
  min-height: 300px;
}

/* 自定义节点样式 */
.custom-node {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  font-size: 14px;
  padding-right: 8px;
}

.node-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.node-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-label.is-leaf {
  color: #606266;
}

.node-count {
  flex-shrink: 0;
}

/* 操作按钮默认隐藏，hover 时显示 */
.node-actions {
  display: none;
  flex-shrink: 0;
  margin-left: auto;
}

.custom-node:hover .node-actions {
  display: flex;
  align-items: center;
}
</style>
```

---

## 三、多级折叠列表（树形表格）

### 3.1 el-table 树形数据

`el-table` 原生支持树形数据展示，通过以下两个属性配合使用：

| 属性 | 说明 |
|------|------|
| `row-key` | 行数据的 key，用于树形展开（通常为 `id`） |
| `tree-props` | 树形结构配置，指定子节点字段名 |

核心配置：

```vue
<el-table
  :data="tableData"
  row-key="id"
  :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
/>
```

### 3.2 完整树形表格示例

```vue
<!-- src/views/tree/TreeTable.vue -->
<template>
  <div class="tree-table-demo">
    <h3>树形表格 - 多级菜单管理</h3>

    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button type="primary" @click="toggleExpand(true)">
        <el-icon><ArrowDown /></el-icon> 展开全部
      </el-button>
      <el-button @click="toggleExpand(false)">
        <el-icon><ArrowUp /></el-icon> 折叠全部
      </el-button>
      <el-button type="success" @click="handleAddRoot">
        <el-icon><Plus /></el-icon> 新增根菜单
      </el-button>
    </div>

    <!-- 树形表格 -->
    <el-table
      ref="tableRef"
      :data="menuData"
      row-key="id"
      :tree-props="{ children: 'children' }"
      border
      default-expand-all
      stripe
      :expand-row-keys="expandedKeys"
      @expand-change="handleExpandChange"
    >
      <!-- 菜单名称列（显示图标 + 名称） -->
      <el-table-column prop="name" label="菜单名称" min-width="240">
        <template #default="{ row }">
          <div class="menu-name">
            <el-icon v-if="row.icon" style="margin-right: 6px;">
              <component :is="row.icon" />
            </el-icon>
            <span>{{ row.name }}</span>
            <el-tag v-if="row.type === 'dir'" size="small" type="info" style="margin-left: 8px;">
              目录
            </el-tag>
            <el-tag v-else-if="row.type === 'menu'" size="small" type="success" style="margin-left: 8px;">
              菜单
            </el-tag>
            <el-tag v-else size="small" style="margin-left: 8px;">
              按钮
            </el-tag>
          </div>
        </template>
      </el-table-column>

      <!-- 排序号 -->
      <el-table-column prop="sort" label="排序" width="80" align="center" />

      <!-- 路由路径 -->
      <el-table-column prop="path" label="路由路径" min-width="160" />

      <!-- 权限标识 -->
      <el-table-column prop="permission" label="权限标识" min-width="180" />

      <!-- 状态 -->
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>

      <!-- 创建时间 -->
      <el-table-column prop="createTime" label="创建时间" width="180" />

      <!-- 操作列 -->
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleAddChild(row)">
            新增
          </el-button>
          <el-button type="primary" link size="small" @click="handleEdit(row)">
            编辑
          </el-button>
          <el-popconfirm
            title="确定删除该菜单吗？"
            @confirm="handleDelete(row)"
          >
            <template #reference>
              <el-button type="danger" link size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowDown, ArrowUp, Plus } from '@element-plus/icons-vue'

const tableRef = ref(null)
const expandedKeys = ref([])

// 多级菜单数据
const menuData = ref([
  {
    id: 1,
    name: '系统管理',
    icon: 'Setting',
    type: 'dir',
    path: '/system',
    permission: '',
    sort: 1,
    status: 1,
    createTime: '2025-01-01 00:00:00',
    children: [
      {
        id: 11,
        name: '用户管理',
        icon: 'User',
        type: 'menu',
        path: '/system/user',
        permission: 'system:user:list',
        sort: 1,
        status: 1,
        createTime: '2025-01-01 00:00:00',
        children: [
          { id: 111, name: '用户新增', type: 'btn', path: '', permission: 'system:user:add', sort: 1, status: 1, createTime: '2025-01-01 00:00:00' },
          { id: 112, name: '用户编辑', type: 'btn', path: '', permission: 'system:user:edit', sort: 2, status: 1, createTime: '2025-01-01 00:00:00' },
          { id: 113, name: '用户删除', type: 'btn', path: '', permission: 'system:user:delete', sort: 3, status: 1, createTime: '2025-01-01 00:00:00' }
        ]
      },
      {
        id: 12,
        name: '角色管理',
        icon: 'Lock',
        type: 'menu',
        path: '/system/role',
        permission: 'system:role:list',
        sort: 2,
        status: 1,
        createTime: '2025-01-01 00:00:00',
        children: [
          { id: 121, name: '角色新增', type: 'btn', path: '', permission: 'system:role:add', sort: 1, status: 1, createTime: '2025-01-01 00:00:00' },
          { id: 122, name: '角色编辑', type: 'btn', path: '', permission: 'system:role:edit', sort: 2, status: 1, createTime: '2025-01-01 00:00:00' }
        ]
      },
      {
        id: 13,
        name: '菜单管理',
        icon: 'Menu',
        type: 'menu',
        path: '/system/menu',
        permission: 'system:menu:list',
        sort: 3,
        status: 1,
        createTime: '2025-01-01 00:00:00'
      }
    ]
  },
  {
    id: 2,
    name: '监控中心',
    icon: 'Monitor',
    type: 'dir',
    path: '/monitor',
    permission: '',
    sort: 2,
    status: 1,
    createTime: '2025-01-01 00:00:00',
    children: [
      {
        id: 21,
        name: '在线用户',
        icon: 'UserFilled',
        type: 'menu',
        path: '/monitor/online',
        permission: 'monitor:online:list',
        sort: 1,
        status: 1,
        createTime: '2025-01-01 00:00:00'
      },
      {
        id: 22,
        name: '操作日志',
        icon: 'Notebook',
        type: 'menu',
        path: '/monitor/log',
        permission: 'monitor:log:list',
        sort: 2,
        status: 1,
        createTime: '2025-01-01 00:00:00'
      }
    ]
  },
  {
    id: 3,
    name: '开发工具',
    icon: 'Opportunity',
    type: 'dir',
    path: '/tool',
    permission: '',
    sort: 3,
    status: 0,
    createTime: '2025-01-01 00:00:00',
    children: [
      {
        id: 31,
        name: '代码生成',
        icon: 'MagicStick',
        type: 'menu',
        path: '/tool/gen',
        permission: 'tool:gen:list',
        sort: 1,
        status: 0,
        createTime: '2025-01-01 00:00:00'
      }
    ]
  }
])

// 展开/折叠全部
// 思路：递归收集所有有子节点的行 key，通过 toggleRowExpansion 控制展开状态
const toggleExpand = (expand) => {
  const allKeys = []
  const collectKeys = (nodes) => {
    nodes.forEach((node) => {
      if (node.children && node.children.length > 0) {
        allKeys.push(node.id)
        collectKeys(node.children)
      }
    })
  }
  collectKeys(menuData.value)

  // 遍历表格数据行，调用 toggleRowExpansion
  toggleRows(menuData.value, expand)
}

const toggleRows = (rows, expand) => {
  rows.forEach((row) => {
    tableRef.value?.toggleRowExpansion(row, expand)
    if (row.children && row.children.length) {
      toggleRows(row.children, expand)
    }
  })
}

// 展开变化事件
const handleExpandChange = (row, expanded) => {
  if (expanded) {
    expandedKeys.value.push(row.id)
  } else {
    const index = expandedKeys.value.indexOf(row.id)
    if (index > -1) {
      expandedKeys.value.splice(index, 1)
    }
  }
}

// 新增根菜单
const handleAddRoot = () => {
  ElMessage.info('新增根菜单（弹窗表单略）')
}

// 新增子菜单
const handleAddChild = (row) => {
  ElMessage.info(`在「${row.name}」下新增子菜单（弹窗表单略）`)
}

// 编辑
const handleEdit = (row) => {
  ElMessage.info(`编辑菜单：${row.name}（弹窗表单略）`)
}

// 删除（递归查找并移除）
const handleDelete = (row) => {
  const removeById = (list, id) => {
    for (let i = 0; i < list.length; i++) {
      if (list[i].id === id) {
        list.splice(i, 1)
        return true
      }
      if (list[i].children && removeById(list[i].children, id)) {
        return true
      }
    }
    return false
  }
  removeById(menuData.value, row.id)
  ElMessage.success(`已删除：${row.name}`)
}
</script>

<style scoped>
.tree-table-demo {
  padding: 20px;
}

.toolbar {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
}

.menu-name {
  display: flex;
  align-items: center;
}
</style>
```

---

## 四、el-tree 的懒加载

### 4.1 懒加载原理

当树形数据层级很深或数据量很大时，一次性加载全部数据会导致性能问题。`el-tree` 提供了 **懒加载（Lazy Load）** 机制：

| 属性/事件 | 说明 |
|-----------|------|
| `lazy` | 开启懒加载模式 |
| `load` | 加载子节点的回调函数 `load(node, resolve)` |

懒加载的工作流程：

```
1. el-tree 渲染顶层节点
2. 用户展开某个节点
3. 触发 load(node, resolve) 回调
4. 在 load 中发起异步请求获取子节点数据
5. 调用 resolve(data) 将数据传给 el-tree 渲染
```

### 4.2 完整懒加载示例

```vue
<!-- src/views/tree/LazyTree.vue -->
<template>
  <div class="lazy-tree-demo">
    <h3>el-tree 懒加载</h3>
    <p class="desc">点击展开节点时，动态加载子节点数据（模拟异步请求）</p>

    <div class="tree-wrapper">
      <el-tree
        ref="treeRef"
        :props="treeProps"
        :load="loadNode"
        lazy
        node-key="id"
        highlight-current
        :expand-on-click-node="false"
        @node-click="handleNodeClick"
      >
        <!-- 自定义节点：显示加载状态 -->
        <template #default="{ node, data }">
          <div class="custom-lazy-node">
            <el-icon class="node-icon" :style="{ color: node.isLeaf ? '#67c23a' : '#409eff' }">
              <Document v-if="node.isLeaf" />
              <Folder v-else />
            </el-icon>
            <span class="node-label">{{ data.label }}</span>

            <!-- 加载中状态 -->
            <el-icon v-if="node.loading" class="loading-icon is-loading">
              <Loading />
            </el-icon>

            <!-- 叶子节点标记 -->
            <el-tag v-if="node.isLeaf" size="small" type="success" class="leaf-tag">
              叶子
            </el-tag>
          </div>
        </template>
      </el-tree>
    </div>

    <!-- 信息面板 -->
    <div class="info-panel">
      <h4>节点信息</h4>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="名称">{{ selectedNode?.label || '-' }}</el-descriptions-item>
        <el-descriptions-item label="ID">{{ selectedNode?.id || '-' }}</el-descriptions-item>
        <el-descriptions-item label="层级">
          {{ selectedNode ? selectedNode._level : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="是否叶子节点">
          {{ selectedNode ? (selectedNode._isLeaf ? '是' : '否') : '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Document, Folder, Loading } from '@element-plus/icons-vue'

const treeRef = ref(null)
const selectedNode = ref(null)

// 懒加载模式下，props 必须设置 isLeaf 字段
// el-tree 通过 isLeaf 判断该节点是否为叶子节点（没有展开箭头）
const treeProps = {
  label: 'label',
  children: 'children',
  isLeaf: 'isLeaf'
}

/**
 * 模拟异步获取子节点数据
 * @param {number} parentId - 父节点 ID
 * @returns {Promise<Array>} 子节点数组
 */
const fetchChildren = (parentId) => {
  return new Promise((resolve) => {
    // 模拟网络延迟 500~1500ms
    setTimeout(() => {
      resolve(generateMockData(parentId))
    }, 500 + Math.random() * 1000)
  })
}

/**
 * 根据父节点 ID 生成模拟数据
 * 第一层有 3 个节点，第二层有 2~4 个节点，第三层为叶子节点
 */
const generateMockData = (parentId) => {
  if (parentId === 0) {
    // 根节点数据
    return [
      { id: 1, label: '华东区域', isLeaf: false },
      { id: 2, label: '华南区域', isLeaf: false },
      { id: 3, label: '华北区域', isLeaf: false },
      { id: 4, label: '西部区域', isLeaf: false }
    ]
  }

  // 非根节点：根据 ID 位数判断层级
  // ID 为 1 位数(1-9) → 第二层（省份），ID 为 2 位数(10-99) → 第三层（城市/叶子）
  const idStr = String(parentId)
  if (idStr.length >= 2) {
    // 第三层及以下为叶子节点（各城市的分公司）
    const cities = ['上海', '杭州', '南京', '苏州', '广州', '深圳', '北京', '天津', '成都', '重庆', '武汉', '长沙']
    const count = 2 + Math.floor(Math.random() * 3)
    const result = []
    for (let i = 0; i < count; i++) {
      result.push({
        id: parentId * 10 + i + 1,
        label: cities[Math.floor(Math.random() * cities.length)] + '分公司',
        isLeaf: true
      })
    }
    return result
  }

  // 第二层：区域下的省份（ID 为 1 位数时）
  const provinceMap = {
    1: ['上海市', '江苏省', '浙江省', '安徽省'],
    2: ['广东省', '福建省', '广西壮族自治区'],
    3: ['北京市', '天津市', '河北省', '山西省'],
    4: ['四川省', '重庆市', '云南省', '贵州省']
  }
  const provinces = provinceMap[parentId] || ['默认省份A', '默认省份B', '默认省份C']
  return provinces.map((name, index) => ({
    id: parentId * 10 + index + 1,
    label: name,
    isLeaf: false
  }))

/**
 * load 回调 —— 懒加载的核心
 * @param {Object} node - 当前节点对象
 * @param {Function} resolve - 回调函数，传入子节点数据
 *
 * 注意：
 * 1. resolve 必须被调用，否则节点会一直处于 loading 状态
 * 2. 即使没有子节点，也要调用 resolve([])
 * 3. node.loading 属性可以判断当前是否正在加载
 */
const loadNode = (node, resolve) => {
  // node.level === 0 表示根节点（最顶层虚拟节点）
  if (node.level === 0) {
    return resolve(generateMockData(0))
  }

  // 非根节点：根据父节点 ID 异步加载
  const parentId = node.data.id
  fetchChildren(parentId).then((data) => {
    resolve(data)
  })
}

// 节点点击
const handleNodeClick = (data, node) => {
  selectedNode.value = {
    ...data,
    _level: node.level,
    _isLeaf: node.isLeaf
  }
}
</script>

<style scoped>
.lazy-tree-demo {
  display: flex;
  gap: 24px;
  padding: 20px;
}

.desc {
  color: #909399;
  font-size: 13px;
  margin-bottom: 16px;
}

.tree-wrapper {
  width: 320px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 16px;
}

.info-panel {
  flex: 1;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 4px;
}

.custom-lazy-node {
  display: flex;
  align-items: center;
  gap: 6px;
}

.node-icon {
  font-size: 16px;
}

.loading-icon {
  color: #409eff;
  margin-left: 4px;
}

.leaf-tag {
  margin-left: 8px;
}
</style>
```

### 4.3 懒加载注意事项

1. **`isLeaf` 必须声明**：懒加载模式下，el-tree 无法预知节点是否有子节点，必须通过 `isLeaf` 字段告知。

2. **`resolve` 必须调用**：无论成功还是失败，都必须调用 `resolve`，否则节点会一直处于 loading 状态。失败时传入空数组 `resolve([])`。

3. **重新加载子节点**：可通过 `node.childNodes` 清空并重新调用 `resolve` 来实现刷新。

```js
// 刷新某节点的子节点
const refreshNode = (node) => {
  node.loaded = false
  node.loading = false
  // 清除子节点 DOM
  node.childNodes = []
  // 重新触发 load
  loadData(node)
}
```

---

> **上一章**：[第5章 表格与分页](./05-表格与分页.md)
>
> **下一篇**：[第6章（下）树工具函数与部门管理实战](./06b-树工具函数与部门管理实战.md)
>
> **章节总览**：[第6章 树形与多级列表](./06-树形与多级列表.md)
