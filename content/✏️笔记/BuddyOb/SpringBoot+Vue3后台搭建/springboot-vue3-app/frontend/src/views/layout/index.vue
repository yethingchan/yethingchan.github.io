<template>
  <el-container class="app-wrapper">
    <el-aside width="210px" class="sidebar">
      <div class="logo">企业后台</div>
      <el-scrollbar>
        <el-menu
          :default-active="activeMenu"
          router
          class="sidebar-menu"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409eff"
        >
          <sidebar-item v-for="m in menuList" :key="m.path" :item="m" base-path="" />
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container>
      <el-header class="navbar">
        <div class="navbar-title">SpringBoot + Vue3 企业级后台管理系统</div>
        <el-dropdown @command="handleCommand">
          <span class="navbar-user">
            {{ userStore.userInfo.userName || '用户' }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import SidebarItem from './sidebarItem.vue'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

const menuList = computed(() => {
  const dashboard = { path: '/index', meta: { title: '首页', icon: 'HomeFilled' } }
  return [dashboard, ...(userStore.menus || [])]
})

function handleCommand(cmd) {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
      .then(() => {
        userStore.logout().then(() => router.push('/login'))
      })
      .catch(() => {})
  }
}
</script>

<style scoped>
.app-wrapper {
  height: 100%;
}
.sidebar {
  background: #304156;
  overflow: hidden;
}
.logo {
  height: 50px;
  line-height: 50px;
  text-align: center;
  color: #fff;
  font-weight: 600;
  background: #2b3a4d;
}
.sidebar-menu {
  border-right: none;
}
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
}
.navbar-title {
  font-weight: 600;
}
.navbar-user {
  cursor: pointer;
  outline: none;
}
.app-main {
  background: #f0f2f5;
  padding: 16px;
}
</style>
