<template>
  <el-tabs v-model="activeTab">
    <el-tab-pane label="字典类型" name="type">
      <div class="page-tool-bar">
        <div><el-button type="primary" :icon="Plus" v-hasPermi="'system:dict:add'" @click="openTypeAdd">新增类型</el-button></div>
      </div>
      <el-table :data="typeRows" border stripe>
        <el-table-column prop="dictId" label="编号" width="80" />
        <el-table-column prop="dictName" label="字典名称" />
        <el-table-column prop="dictType" label="字典类型" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="row.status === '0' ? 'success' : 'danger'">{{ row.status === '0' ? '正常' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" v-hasPermi="'system:dict:edit'" @click="openTypeEdit(row)">修改</el-button>
            <el-button link type="danger" :icon="Delete" v-hasPermi="'system:dict:remove'" @click="removeType(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="typeDialog" :title="typeTitle" width="480px">
        <el-form ref="typeRef" :model="typeForm" :rules="typeRules" label-width="90px">
          <el-form-item label="字典名称" prop="dictName"><el-input v-model="typeForm.dictName" /></el-form-item>
          <el-form-item label="字典类型" prop="dictType"><el-input v-model="typeForm.dictType" placeholder="如 sys_user_sex" /></el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="typeForm.status">
              <el-radio value="0">正常</el-radio>
              <el-radio value="1">停用</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="备注"><el-input v-model="typeForm.remark" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="typeDialog = false">取消</el-button>
          <el-button type="primary" :loading="typeSubmit" @click="submitType">确定</el-button>
        </template>
      </el-dialog>
    </el-tab-pane>

    <el-tab-pane label="字典数据" name="data">
      <el-form :inline="true" :model="dataQuery" class="search-form">
        <el-form-item label="字典类型">
          <el-select v-model="dataQuery.dictType" placeholder="选择类型" clearable style="width:200px" @change="loadData">
            <el-option v-for="t in typeRows" :key="t.dictId" :label="t.dictName" :value="t.dictType" />
          </el-select>
        </el-form-item>
      </el-form>
      <div class="page-tool-bar">
        <div>
          <el-button type="primary" :icon="Plus" :disabled="!dataQuery.dictType" v-hasPermi="'system:dict:add'" @click="openDataAdd">新增数据</el-button>
        </div>
      </div>
      <el-table :data="dataRows" border stripe>
        <el-table-column prop="dictCode" label="编号" width="80" />
        <el-table-column prop="dictLabel" label="字典标签" />
        <el-table-column prop="dictValue" label="字典键值" />
        <el-table-column prop="dictSort" label="排序" width="70" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="row.status === '0' ? 'success' : 'danger'">{{ row.status === '0' ? '正常' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" v-hasPermi="'system:dict:edit'" @click="openDataEdit(row)">修改</el-button>
            <el-button link type="danger" :icon="Delete" v-hasPermi="'system:dict:remove'" @click="removeData(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="dataDialog" :title="dataTitle" width="480px">
        <el-form ref="dataRef" :model="dataForm" :rules="dataRules" label-width="90px">
          <el-form-item label="字典类型"><el-input :model-value="dataForm.dictType" disabled /></el-form-item>
          <el-form-item label="标签" prop="dictLabel"><el-input v-model="dataForm.dictLabel" /></el-form-item>
          <el-form-item label="键值" prop="dictValue"><el-input v-model="dataForm.dictValue" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="dataForm.dictSort" :min="0" /></el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="dataForm.status">
              <el-radio value="0">正常</el-radio>
              <el-radio value="1">停用</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dataDialog = false">取消</el-button>
          <el-button type="primary" :loading="dataSubmit" @click="submitData">确定</el-button>
        </template>
      </el-dialog>
    </el-tab-pane>
  </el-tabs>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import {
  listDictType, addDictType, editDictType, removeDictTypes,
  listDictData, addDictData, editDictData, removeDictData
} from '@/api/system/dict'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('type')
const typeRows = ref([])
const dataRows = ref([])
const dataQuery = reactive({ dictType: '' })

const typeDialog = ref(false)
const typeTitle = ref('')
const typeSubmit = ref(false)
const typeRef = ref()
const typeForm = reactive({ dictId: undefined, dictName: '', dictType: '', status: '0', remark: '' })
const typeRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: [{ required: true, message: '请输入字典类型', trigger: 'blur' }]
}

const dataDialog = ref(false)
const dataTitle = ref('')
const dataSubmit = ref(false)
const dataRef = ref()
const dataForm = reactive({ dictCode: undefined, dictType: '', dictLabel: '', dictValue: '', dictSort: 1, status: '0' })
const dataRules = {
  dictLabel: [{ required: true, message: '请输入标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入键值', trigger: 'blur' }]
}

function loadType() {
  listDictType({ pageNum: 1, pageSize: 100 }).then(res => { typeRows.value = res.rows || [] })
}
function openTypeAdd() {
  typeTitle.value = '新增类型'
  Object.assign(typeForm, { dictId: undefined, dictName: '', dictType: '', status: '0', remark: '' })
  typeDialog.value = true
}
function openTypeEdit(row) { typeTitle.value = '修改类型'; Object.assign(typeForm, row); typeDialog.value = true }
function submitType() {
  typeRef.value.validate(valid => {
    if (!valid) return
    typeSubmit.value = true
    const p = typeForm.dictId ? editDictType(typeForm) : addDictType(typeForm)
    p.then(() => { ElMessage.success('操作成功'); typeDialog.value = false; loadType() })
      .finally(() => typeSubmit.value = false)
  })
}
function removeType(row) {
  ElMessageBox.confirm('确认删除字典类型「' + row.dictName + '」？', '提示', { type: 'warning' })
    .then(() => removeDictTypes([row.dictId]).then(() => { ElMessage.success('删除成功'); loadType() }))
    .catch(() => {})
}

function loadData() {
  if (!dataQuery.dictType) return
  listDictData({ pageNum: 1, pageSize: 100, dictType: dataQuery.dictType })
    .then(res => { dataRows.value = res.rows || [] })
}
function openDataAdd() {
  dataTitle.value = '新增数据'
  Object.assign(dataForm, { dictCode: undefined, dictType: dataQuery.dictType, dictLabel: '', dictValue: '', dictSort: 1, status: '0' })
  dataDialog.value = true
}
function openDataEdit(row) { dataTitle.value = '修改数据'; Object.assign(dataForm, row); dataDialog.value = true }
function submitData() {
  dataRef.value.validate(valid => {
    if (!valid) return
    dataSubmit.value = true
    const p = dataForm.dictCode ? editDictData(dataForm) : addDictData(dataForm)
    p.then(() => { ElMessage.success('操作成功'); dataDialog.value = false; loadData() })
      .finally(() => dataSubmit.value = false)
  })
}
function removeData(row) {
  ElMessageBox.confirm('确认删除字典数据？', '提示', { type: 'warning' })
    .then(() => removeDictData([row.dictCode]).then(() => { ElMessage.success('删除成功'); loadData() }))
    .catch(() => {})
}

onMounted(loadType)
</script>
