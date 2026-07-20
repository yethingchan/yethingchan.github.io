import request from '@/utils/request'

export function listConfig(params) {
  return request({ url: '/system/config/list', method: 'get', params })
}

export function getConfig(id) {
  return request({ url: '/system/config/' + id, method: 'get' })
}

export function addConfig(data) {
  return request({ url: '/system/config', method: 'post', data })
}

export function editConfig(data) {
  return request({ url: '/system/config', method: 'put', data })
}

export function removeConfigs(ids) {
  return request({ url: '/system/config', method: 'delete', params: { ids } })
}
