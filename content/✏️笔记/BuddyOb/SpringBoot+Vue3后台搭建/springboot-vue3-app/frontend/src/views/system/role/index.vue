<template>
  <div>
    <el-form :inline="true" :model="query" class="search-form">
      <el-form-item label="角色名"><el-input v-model="query.roleName" placeholder="角色名" clearable /></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable style="width:120px">
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="load">查询</el-button>
        <el-button :icon="Refresh" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="page-tool-bar">
      <div><el-button type="primary" :icon="Plus" v-hasPermi="'system:role:add'" @click="openAdd">新增</el-button></div>
    </div>

    <el-table :data="rows" border stripe>
      <el-table-column prop="roleId" label="编号" width="80" />
      <el-table-column prop="roleName" label="角色名" />
      <el-table-column prop="roleKey" label="权限字符" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === '0' ? 'success' : 'danger'">{{ row.status === '0' ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="Edit" v-hasPermi="'system:role:edit'" @click="openEdit(row)">修改</el-button>
          <el-button link type="primary" :icon="Menu" v-hasPermi="'system:role:edit'" @click="openMenu(row)">菜单权限</el-button>
          <el-button link type="danger" :icon="Delete" v-hasPermi="'system:role:remove'" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page.pageNum"
        v-model:page-size="page.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="load"
        @size-change="load"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名" prop="roleName"><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="权限字符" prop="roleKey"><el-input v-model="form.roleKey" placeholder="如 admin" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="menuDialog" title="分配菜单权限" width="500px">
      <el-tree
        ref="treeRef"
        :data="menuTreeData"
        :props="{ label: (d) => d.meta && d.meta.title, children: 'children' }"
        show-checkbox
        node-key="menuId"
        :default-checked-keys="checkedKeys"
      />
      <template #footer>
        <el-button @click="menuDialog = false">取消</el-button>
        <el-button type="primary" :loading="menuSubmitting" @click="saveMenu">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { Search, Refresh, Plus, Edit, Delete, Menu } from '@element-plus/icons-vue'
import { listRole, getRole, addRole, updateRole, deleteRole, saveRoleMenus } from '@/api/system/role'
import { menuTree } from '@/api/system/menu'
import { ElMessage, ElMessageBox } from 'element-plus'

const rows = ref([])
const total = ref(0)
const page = reactive({ pageNum: 1, pageSize: 10 })
const query = reactive({ roleName: '', status: '' })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()
const form = reactive({ roleId: undefined, roleName: '', roleKey: '', status: '0', remark: '' })
const rules = {
  roleName: [{ required: true, message: '请输入角色名', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入权限字符', trigger: 'blur' }]
}

const menuDialog = ref(false)
const menuSubmitting = ref(false)
const treeRef = ref()
const menuTreeData = ref([])
const checkedKeys = ref([])
let currentRoleId = null

function load() {
  listRole({ ...page, ...query }).then(res => {
    rows.value = res.rows || []
    total.value = res.total || 0
  })
}
function reset() { query.roleName = ''; query.status = ''; load() }

function openAdd() {
  dialogTitle.value = '新增角色'
  Object.assign(form, { roleId: undefined, roleName: '', roleKey: '', status: '0', remark: '' })
  dialogVisible.value = true
}
function openEdit(row) {
  dialogTitle.value = '修改角色'
  getRole(row.roleId).then(res => {
    Object.assign(form, res.data.role)
    dialogVisible.value = true
  })
}
function submit() {
  formRef.value.validate(valid => {
    if (!valid) return
    submitting.value = true
    const p = form.roleId ? updateRole(form) : addRole(form)
    p.then(() => { ElMessage.success('操作成功'); dialogVisible.value = false; load() })
      .finally(() => submitting.value = false)
  })
}
function remove(row) {
  ElMessageBox.confirm('确认删除角色「' + row.roleName + '」？', '提示', { type: 'warning' })
    .then(() => deleteRole(row.roleId).then(() => { ElMessage.success('删除成功'); load() }))
    .catch(() => {})
}

async function openMenu(row) {
  currentRoleId = row.roleId
  const res = await getRole(row.roleId)
  checkedKeys.value = res.data.menuIds || []
  menuDialog.value = true
  await nextTick()
  treeRef.value && treeRef.value.setCheckedKeys(checkedKeys.value)
}
function saveMenu() {
  const keys = treeRef.value.getCheckedKeys()
  menuSubmitting.value = true
  saveRoleMenus({ role: { roleId: currentRoleId }, menuIds: keys })
    .then(() => { ElMessage.success('分配成功'); menuDialog.value = false })
    .finally(() => menuSubmitting.value = false)
}

onMounted(() => {
  load()
  menuTree().then(res => { menuTreeData.value = res.data || [] })
})
</script>
