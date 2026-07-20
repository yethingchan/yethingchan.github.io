<template>
  <div>
    <el-form :inline="true" :model="query" class="search-form">
      <el-form-item label="用户名"><el-input v-model="query.userName" placeholder="用户名" clearable /></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="状态" clearable style="width:120px">
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
      <div>
        <el-button type="primary" :icon="Plus" v-hasPermi="'system:user:add'" @click="openAdd">新增</el-button>
      </div>
    </div>

    <el-table :data="rows" border stripe>
      <el-table-column prop="userId" label="编号" width="80" />
      <el-table-column prop="userName" label="用户名" />
      <el-table-column prop="nickName" label="昵称" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="phonenumber" label="手机号" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === '0' ? 'success' : 'danger'">{{ row.status === '0' ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="Edit" v-hasPermi="'system:user:edit'" @click="openEdit(row)">修改</el-button>
          <el-button link type="danger" :icon="Delete" v-hasPermi="'system:user:remove'" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="userName">
          <el-input v-model="form.userName" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="昵称"><el-input v-model="form.nickName" /></el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="form.password" type="password" placeholder="默认 123456" />
        </el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phonenumber" /></el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.sex" style="width:100%">
            <el-option label="男" value="0" />
            <el-option label="女" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple style="width:100%">
            <el-option v-for="r in roleOptions" :key="r.roleId" :label="r.roleName" :value="r.roleId" />
          </el-select>
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
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { listUser, getUser, addUser, updateUser, deleteUsers } from '@/api/system/user'
import { listRole } from '@/api/system/role'
import { ElMessage, ElMessageBox } from 'element-plus'

const rows = ref([])
const total = ref(0)
const page = reactive({ pageNum: 1, pageSize: 10 })
const query = reactive({ userName: '', status: '' })
const roleOptions = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  userId: undefined, userName: '', nickName: '', password: '',
  email: '', phonenumber: '', sex: '0', status: '0', roleIds: []
})
const rules = { userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }] }

function load() {
  listUser({ ...page, ...query }).then(res => {
    rows.value = res.rows || []
    total.value = res.total || 0
  })
}
function reset() { query.userName = ''; query.status = ''; load() }

function openAdd() {
  isEdit.value = false
  dialogTitle.value = '新增用户'
  Object.assign(form, {
    userId: undefined, userName: '', nickName: '', password: '',
    email: '', phonenumber: '', sex: '0', status: '0', roleIds: []
  })
  dialogVisible.value = true
}
function openEdit(row) {
  isEdit.value = true
  dialogTitle.value = '修改用户'
  getUser(row.userId).then(res => {
    const d = res.data
    Object.assign(form, d.user, { roleIds: d.roleIds || [] })
    form.userId = d.user.userId
    form.password = ''
    dialogVisible.value = true
  })
}
function submit() {
  formRef.value.validate(valid => {
    if (!valid) return
    submitting.value = true
    const payload = { user: { ...form }, roleIds: form.roleIds }
    const p = isEdit.value ? updateUser(payload) : addUser(payload)
    p.then(() => { ElMessage.success('操作成功'); dialogVisible.value = false; load() })
      .finally(() => submitting.value = false)
  })
}
function remove(row) {
  ElMessageBox.confirm('确认删除用户「' + row.userName + '」？', '提示', { type: 'warning' })
    .then(() => deleteUsers([row.userId]).then(() => { ElMessage.success('删除成功'); load() }))
    .catch(() => {})
}

onMounted(() => {
  load()
  listRole({ pageNum: 1, pageSize: 100 }).then(res => { roleOptions.value = res.rows || [] })
})
</script>
