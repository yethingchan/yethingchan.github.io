import request from '@/utils/request'

export function listMenu(params) {
  return request({ url: '/system/menu/list', method: 'get', params })
}

// 角色赋权时的菜单树
export function menuTree() {
  return request({ url: '/system/menu/tree', method: 'get' })
}

export function addMenu(data) {
  return request({ url: '/system/menu', method: 'post', data })
}

export function updateMenu(data) {
  return request({ url: '/system/menu', method: 'put', data })
}

export function deleteMenu(id) {
  return request({ url: '/system/menu/' + id, method: 'delete' })
}
