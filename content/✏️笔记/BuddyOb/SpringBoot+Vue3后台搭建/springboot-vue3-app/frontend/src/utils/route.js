import Layout from '@/views/layout/index.vue'

// Vite 编译期收集所有 views 下的 .vue，用于动态路由懒加载
// key 形如 '../views/system/user/index.vue'
const modules = import.meta.glob('../views/**/*.vue')

function loadView(component) {
  const key = `../views/${component}.vue`
  return modules[key] || (() => import('@/views/error/404.vue'))
}

// 把一个后端 RouterVO 转成 vue-router 的 RouteRecord
function toRoute(vo, isRoot) {
  const route = {
    path: isRoot ? '/' + vo.path : vo.path,
    name: vo.name,
    meta: vo.meta || {}
  }
  if (vo.component === 'Layout') {
    // 目录（M）：用布局壳组件
    route.component = Layout
  } else if (vo.component) {
    // 菜单（C）：按后端返回的 component 字符串懒加载视图
    route.component = loadView(vo.component)
  }
  if (vo.children && vo.children.length) {
    route.children = vo.children.map((c) => toRoute(c, false))
  }
  return route
}

export function generateRoutes(rawRouters) {
  if (!Array.isArray(rawRouters)) return []
  return rawRouters.map((vo) => toRoute(vo, true))
}
