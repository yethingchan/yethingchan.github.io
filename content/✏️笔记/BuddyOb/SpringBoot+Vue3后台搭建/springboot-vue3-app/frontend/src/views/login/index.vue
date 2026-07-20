<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 class="login-title">SpringBoot + Vue3 企业后台</h2>
      <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin">
        <el-form-item prop="userName">
          <el-input v-model="form.userName" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="密码" :prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">登 录</el-button>
      </el-form>
      <div class="login-tip">默认账号：admin / 123456</div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/modules/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ userName: 'admin', password: '123456' })
const rules = {
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function handleLogin() {
  formRef.value.validate((valid) => {
    if (!valid) return
    loading.value = true
    userStore
      .login(form)
      .then(() => router.push('/'))
      .catch(() => {})
      .finally(() => {
        loading.value = false
      })
  })
}
</script>

<style scoped>
.login-container {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f2d3d, #3a5068);
}
.login-card {
  width: 380px;
  padding: 10px 20px;
}
.login-title {
  text-align: center;
  margin: 10px 0 24px;
  color: #303133;
}
.login-btn {
  width: 100%;
}
.login-tip {
  text-align: center;
  color: #909399;
  font-size: 13px;
  margin-top: 12px;
}
</style>
