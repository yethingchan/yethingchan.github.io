import request from '@/utils/request'

export function listUser(params) {
  return request({ url: '/system/user/list', method: 'get', params })
}

export function getUser(id) {
  return request({ url: '/system/user/' + id, method: 'get' })
}

export function addUser(data) {
  return request({ url: '/system/user', method: 'post', data })
}

export function updateUser(data) {
  return request({ url: '/system/user', method: 'put', data })
}

export function deleteUsers(ids) {
  return request({ url: '/system/user', method: 'delete', params: { ids } })
}
