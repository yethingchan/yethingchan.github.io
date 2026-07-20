<template>
  <div>
    <el-form :inline="true" :model="query" class="search-form">
      <el-form-item label="标题"><el-input v-model="query.title" placeholder="公告标题" clearable /></el-form-item>
      <el-form-item label="类型">
        <el-select v-model="query.type" clearable style="width:120px">
          <el-option v-for="t in typeOptions" :key="t.dictValue" :label="t.dictLabel" :value="t.dictValue" />
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" :icon="Search" @click="load">查询</el-button></el-form-item>
    </el-form>

    <div class="page-tool-bar">
      <div><el-button type="primary" :icon="Plus" v-hasPermi="'business:notice:add'" @click="openAdd">新增</el-button></div>
    </div>

    <el-table :data="rows" border stripe>
      <el-table-column prop="noticeId" label="编号" width="80" />
      <el-table-column prop="noticeTitle" label="标题" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }"><el-tag>{{ typeLabel(row.type) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === '0' ? 'success' : 'danger'">{{ row.status === '0' ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="Edit" v-hasPermi="'business:notice:edit'" @click="openEdit(row)">修改</el-button>
          <el-button link type="danger" :icon="Delete" v-hasPermi="'business:notice:remove'" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page.pageNum" v-model:page-size="page.pageSize"
        :total="total" :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next" @current-change="load" @size-change="load" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="noticeTitle"><el-input v-model="form.noticeTitle" /></el-form-item>
        <el-form-item label="类型" prop="noticeType">
          <el-select v-model="form.noticeType" style="width:100%">
            <el-option v-for="t in typeOptions" :key="t.dictValue" :label="t.dictLabel" :value="t.dictValue" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容"><el-input v-model="form.noticeContent" type="textarea" :rows="4" /></el-form-item>
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
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { listNotice, addNotice, editNotice, removeNotices } from '@/api/business/notice'
import { dictDataByType } from '@/api/system/dict'
import { ElMessage, ElMessageBox } from 'element-plus'

const rows = ref([])
const total = ref(0)
const page = reactive({ pageNum: 1, pageSize: 10 })
const query = reactive({ title: '', type: '' })
const typeOptions = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()
const form = reactive({ noticeId: undefined, noticeTitle: '', noticeType: '', noticeContent: '', status: '0' })
const rules = { noticeTitle: [{ required: true, message: '请输入标题', trigger: 'blur' }] }

function load() {
  listNotice({ ...page, ...query }).then(res => { rows.value = res.rows || []; total.value = res.total || 0 })
}
function typeLabel(type) {
  const t = typeOptions.value.find(o => o.dictValue === type)
  return t ? t.dictLabel : (type || '-')
}
function openAdd() {
  dialogTitle.value = '新增公告'
  Object.assign(form, { noticeId: undefined, noticeTitle: '', noticeType: typeOptions.value[0] ? typeOptions.value[0].dictValue : '', noticeContent: '', status: '0' })
  dialogVisible.value = true
}
function openEdit(row) {
  dialogTitle.value = '修改公告'
  Object.assign(form, row)
  dialogVisible.value = true
}
function submit() {
  formRef.value.validate(valid => {
    if (!valid) return
    submitting.value = true
    const p = form.noticeId ? editNotice(form) : addNotice(form)
    p.then(() => { ElMessage.success('操作成功'); dialogVisible.value = false; load() })
      .finally(() => submitting.value = false)
  })
}
function remove(row) {
  ElMessageBox.confirm('确认删除公告「' + row.noticeTitle + '」？', '提示', { type: 'warning' })
    .then(() => removeNotices([row.noticeId]).then(() => { ElMessage.success('删除成功'); load() }))
    .catch(() => {})
}
onMounted(() => {
  load()
  dictDataByType('sys_notice_type').then(res => { typeOptions.value = res.data || [] })
})
</script>
