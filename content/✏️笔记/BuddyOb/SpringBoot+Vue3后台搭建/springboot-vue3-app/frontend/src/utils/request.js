import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from '@/utils/auth'

// 统一门面：baseURL=/api 走 Vite 代理到后端
const service = axios.create({
  baseURL: '/api',
  timeout: 10000,
  // 数组参数序列化为重复 key（ids=1&ids=2），对齐后端 @RequestParam List<Long>
  paramsSerializer: (params) => {
    const parts = []
    Object.entries(params || {}).forEach(([k, v]) => {
      if (v === undefined || v === null) return
      if (Array.isArray(v)) {
        v.forEach((item) => parts.push(`${k}=${encodeURIComponent(item)}`))
      } else {
        parts.push(`${k}=${encodeURIComponent(v)}`)
      }
    })
    return parts.join('&')
  }
})

service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端 AjaxResult.code === 200 才成功
    if (res.code === 200) {
      return res
    }
    ElMessage.error(res.msg || '操作失败')
    // 401 未登录 / 403 无权限：清 token 回登录页
    if (res.code === 401 || res.code === 403) {
      removeToken()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(new Error(res.msg || 'Error'))
  },
  (error) => {
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default service
