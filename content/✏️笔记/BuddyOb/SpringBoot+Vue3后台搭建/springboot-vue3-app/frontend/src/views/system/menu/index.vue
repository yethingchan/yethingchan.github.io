<template>
  <div>
    <div class="page-tool-bar">
      <div><el-button type="primary" :icon="Plus" v-hasPermi="'system:menu:add'" @click="openAdd">新增菜单</el-button></div>
    </div>

    <el-table :data="rows" border stripe row-key="menuId">
      <el-table-column prop="menuId" label="编号" width="80" />
      <el-table-column prop="menuName" label="菜单名称" />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="row.menuType === 'M' ? 'success' : row.menuType === 'C' ? 'warning' : 'info'">
            {{ row.menuType === 'M' ? '目录' : row.menuType === 'C' ? '菜单' : '按钮' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="orderNum" label="排序" width="70" />
      <el-table-column prop="path" label="路由地址" />
      <el-table-column prop="component" label="组件路径" />
      <el-table-column prop="perms" label="权限标识" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === '0' ? 'success' : 'danger'">{{ row.status === '0' ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="Edit" v-hasPermi="'system:menu:edit'" @click="openEdit(row)">修改</el-button>
          <el-button link type="danger" :icon="Delete" v-hasPermi="'system:menu:remove'" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="上级菜单">
          <el-select v-model="form.parentId" style="width:100%">
            <el-option label="根目录" :value="0" />
            <el-option v-for="m in parentOptions" :key="m.menuId" :label="m.menuName" :value="m.menuId" />
          </el-select>
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio value="M">目录</el-radio>
            <el-radio value="C">菜单</el-radio>
            <el-radio value="F">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName"><el-input v-model="form.menuName" /></el-form-item>
        <el-form-item label="路由地址"><el-input v-model="form.path" placeholder="如 user" /></el-form-item>
        <el-form-item label="组件路径" v-if="form.menuType === 'C'"><el-input v-model="form.component" placeholder="如 system/user/index" /></el-form-item>
        <el-form-item label="权限标识" v-if="form.menuType === 'F'"><el-input v-model="form.perms" placeholder="如 system:user:add" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.orderNum" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { listMenu, addMenu, updateMenu, deleteMenu } from '@/api/system/menu'
import { ElMessage, ElMessageBox } from 'element-plus'

const rows = ref([])
const parentOptions = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  menuId: undefined, parentId: 0, menuName: '', menuType: 'M',
  path: '', component: '', perms: '', icon: '#', orderNum: 1, status: '0'
})
const rules = { menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }] }

function load() {
  listMenu({}).then(res => {
    rows.value = res.data || []
    parentOptions.value = (res.data || []).filter(m => m.menuType === 'M')
  })
}
function openAdd() {
  dialogTitle.value = '新增菜单'
  Object.assign(form, {
    menuId: undefined, parentId: 0, menuName: '', menuType: 'M',
    path: '', component: '', perms: '', icon: '#', orderNum: 1, status: '0'
  })
  dialogVisible.value = true
}
function openEdit(row) {
  dialogTitle.value = '修改菜单'
  Object.assign(form, row)
  dialogVisible.value = true
}
function submit() {
  formRef.value.validate(valid => {
    if (!valid) return
    submitting.value = true
    const payload = { ...form }
    const p = form.menuId ? updateMenu(payload) : addMenu(payload)
    p.then(() => { ElMessage.success('操作成功'); dialogVisible.value = false; load() })
      .finally(() => submitting.value = false)
  })
}
function remove(row) {
  ElMessageBox.confirm('确认删除菜单「' + row.menuName + '」？', '提示', { type: 'warning' })
    .then(() => deleteMenu(row.menuId).then(() => { ElMessage.success('删除成功'); load() }))
    .catch(() => {})
}
onMounted(load)
</script>
