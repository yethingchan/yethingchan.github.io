import request from '@/utils/request'

export function listOperlog(params) {
  return request({ url: '/monitor/operlog/list', method: 'get', params })
}

export function removeOperlogs(ids) {
  return request({ url: '/monitor/operlog', method: 'delete', params: { ids } })
}

export function cleanOperlog() {
  return request({ url: '/monitor/operlog/clean', method: 'delete' })
}
