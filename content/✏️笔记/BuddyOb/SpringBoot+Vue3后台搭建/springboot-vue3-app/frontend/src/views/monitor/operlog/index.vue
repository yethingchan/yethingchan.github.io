<template>
  <div>
    <el-form :inline="true" :model="query" class="search-form">
      <el-form-item label="操作名"><el-input v-model="query.title" placeholder="操作模块" clearable /></el-form-item>
      <el-form-item label="操作人"><el-input v-model="query.operName" clearable /></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable style="width:120px">
          <el-option label="正常" :value="0" />
          <el-option label="异常" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" :icon="Search" @click="load">查询</el-button></el-form-item>
    </el-form>

    <div class="page-tool-bar">
      <div>
        <el-button type="danger" :icon="Delete" v-hasPermi="'monitor:operlog:remove'" :disabled="!selected.length" @click="batchRemove">批量删除</el-button>
        <el-button type="warning" :icon="Delete" v-hasPermi="'monitor:operlog:remove'" @click="clean">清空日志</el-button>
      </div>
    </div>

    <el-table :data="rows" border stripe @selection-change="onSelect">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="operId" label="编号" width="80" />
      <el-table-column prop="title" label="操作模块" />
      <el-table-column prop="operName" label="操作人" width="120" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'">{{ row.status === 0 ? '正常' : '异常' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="operIp" label="IP" width="130" />
      <el-table-column prop="operTime" label="操作时间" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" :icon="Delete" v-hasPermi="'monitor:operlog:remove'" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page.pageNum" v-model:page-size="page.pageSize"
        :total="total" :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next" @current-change="load" @size-change="load" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Delete } from '@element-plus/icons-vue'
import { listOperlog, removeOperlogs, cleanOperlog } from '@/api/monitor/operlog'
import { ElMessage, ElMessageBox } from 'element-plus'

const rows = ref([])
const total = ref(0)
const page = reactive({ pageNum: 1, pageSize: 10 })
const query = reactive({ title: '', operName: '', status: '' })
const selected = ref([])

function load() {
  listOperlog({ ...page, ...query }).then(res => { rows.value = res.rows || []; total.value = res.total || 0 })
}
function onSelect(val) { selected.value = val.map(r => r.operId) }
function remove(row) {
  ElMessageBox.confirm('确认删除该日志？', '提示', { type: 'warning' })
    .then(() => removeOperlogs([row.operId]).then(() => { ElMessage.success('删除成功'); load() }))
    .catch(() => {})
}
function batchRemove() {
  ElMessageBox.confirm('确认批量删除选中日志？', '提示', { type: 'warning' })
    .then(() => removeOperlogs(selected.value).then(() => { ElMessage.success('删除成功'); load() }))
    .catch(() => {})
}
function clean() {
  ElMessageBox.confirm('确认清空所有操作日志？', '提示', { type: 'warning' })
    .then(() => cleanOperlog().then(() => { ElMessage.success('已清空'); load() }))
    .catch(() => {})
}
onMounted(load)
</script>
