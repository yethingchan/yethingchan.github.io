<template>
  <div>
    <el-card>
      <h2>欢迎使用企业级后台管理系统</h2>
      <p class="tip">技术栈：SpringBoot 3 + Spring Security 6 + JWT + MyBatis-Plus + Vue 3 + Element Plus</p>
    </el-card>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-title">当前用户</div>
          <div class="stat-value">{{ userStore.userInfo.userName || '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-title">角色</div>
          <div class="stat-value">{{ (userStore.roles || []).join('，') || '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-title">权限数量</div>
          <div class="stat-value">{{ (userStore.permissions || []).length }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { useUserStore } from '@/store/modules/user'
const userStore = useUserStore()
</script>

<style scoped>
.tip {
  color: #909399;
}
.stat-title {
  color: #909399;
  font-size: 14px;
}
.stat-value {
  margin-top: 8px;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}
</style>
