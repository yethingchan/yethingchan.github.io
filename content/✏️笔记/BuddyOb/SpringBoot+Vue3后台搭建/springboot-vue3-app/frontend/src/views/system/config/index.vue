<template>
  <div>
    <el-form :inline="true" :model="query" class="search-form">
      <el-form-item label="参数名"><el-input v-model="query.configName" placeholder="参数名" clearable /></el-form-item>
      <el-form-item label="参数键"><el-input v-model="query.configKey" placeholder="参数键" clearable /></el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="load">查询</el-button>
        <el-button :icon="Refresh" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="page-tool-bar">
      <div><el-button type="primary" :icon="Plus" v-hasPermi="'system:config:add'" @click="openAdd">新增</el-button></div>
    </div>

    <el-table :data="rows" border stripe>
      <el-table-column prop="configId" label="编号" width="80" />
      <el-table-column prop="configName" label="参数名" />
      <el-table-column prop="configKey" label="参数键" />
      <el-table-column prop="configValue" label="参数值" />
      <el-table-column label="内置" width="90">
        <template #default="{ row }"><el-tag :type="row.configType === 'Y' ? 'warning' : 'info'">{{ row.configType === 'Y' ? '是' : '否' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="Edit" v-hasPermi="'system:config:edit'" @click="openEdit(row)">修改</el-button>
          <el-button link type="danger" :icon="Delete" v-hasPermi="'system:config:remove'" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page.pageNum" v-model:page-size="page.pageSize"
        :total="total" :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next" @current-change="load" @size-change="load" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="参数名" prop="configName"><el-input v-model="form.configName" /></el-form-item>
        <el-form-item label="参数键" prop="configKey"><el-input v-model="form.configKey" /></el-form-item>
        <el-form-item label="参数值"><el-input v-model="form.configValue" /></el-form-item>
        <el-form-item label="是否内置">
          <el-radio-group v-model="form.configType">
            <el-radio value="Y">是</el-radio>
            <el-radio value="N">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
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
import { listConfig, getConfig, addConfig, editConfig, removeConfigs } from '@/api/system/config'
import { ElMessage, ElMessageBox } from 'element-plus'

const rows = ref([])
const total = ref(0)
const page = reactive({ pageNum: 1, pageSize: 10 })
const query = reactive({ configName: '', configKey: '' })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()
const form = reactive({ configId: undefined, configName: '', configKey: '', configValue: '', configType: 'N', remark: '' })
const rules = {
  configName: [{ required: true, message: '请输入参数名', trigger: 'blur' }],
  configKey: [{ required: true, message: '请输入参数键', trigger: 'blur' }]
}

function load() {
  listConfig({ ...page, ...query }).then(res => { rows.value = res.rows || []; total.value = res.total || 0 })
}
function reset() { query.configName = ''; query.configKey = ''; load() }
function openAdd() {
  dialogTitle.value = '新增参数'
  Object.assign(form, { configId: undefined, configName: '', configKey: '', configValue: '', configType: 'N', remark: '' })
  dialogVisible.value = true
}
function openEdit(row) {
  dialogTitle.value = '修改参数'
  getConfig(row.configId).then(res => { Object.assign(form, res.data); dialogVisible.value = true })
}
function submit() {
  formRef.value.validate(valid => {
    if (!valid) return
    submitting.value = true
    const p = form.configId ? editConfig(form) : addConfig(form)
    p.then(() => { ElMessage.success('操作成功'); dialogVisible.value = false; load() })
      .finally(() => submitting.value = false)
  })
}
function remove(row) {
  ElMessageBox.confirm('确认删除参数「' + row.configName + '」？', '提示', { type: 'warning' })
    .then(() => removeConfigs([row.configId]).then(() => { ElMessage.success('删除成功'); load() }))
    .catch(() => {})
}
onMounted(load)
</script>
