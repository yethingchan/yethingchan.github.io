import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/store/modules/user'
import { generateRoutes } from '@/utils/route'
import { ElMessage } from 'element-plus'

// 常量路由（无需登录即可访问 / 布局壳）
export const constantRoutes = [
  {
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    hidden: true
  },
  {
    path: '/404',
    component: () => import('@/views/error/404.vue'),
    hidden: true
  },
  {
    path: '/',
    component: () => import('@/views/layout/index.vue'),
    redirect: '/index',
    children: [
      {
        path: 'index',
        name: 'Index',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'HomeFilled', affix: true }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/404', hidden: true }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 })
})

// 前端路由守卫：未登录跳登录页；已登录但没拿到用户信息则拉取并动态挂载路由
router.beforeEach(async (to, from, next) => {
  const token = getToken()
  if (!token) {
    if (to.path === '/login') next()
    else next('/login')
    return
  }
  if (to.path === '/login') {
    next('/')
    return
  }

  const userStore = useUserStore()
  if (!userStore.roles || userStore.roles.length === 0) {
    try {
      await userStore.getInfo()
      const rawRouters = await userStore.getRouters()
      const asyncRoutes = generateRoutes(rawRouters)
      userStore.setMenus(asyncRoutes)
      asyncRoutes.forEach((r) => router.addRoute(r))
      // 重新进入目标路由，确保动态路由已生效
      next({ ...to, replace: true })
    } catch (e) {
      await userStore.logout()
      ElMessage.error('获取用户信息失败，请重新登录')
      next('/login')
    }
  } else {
    next()
  }
})

export default router
