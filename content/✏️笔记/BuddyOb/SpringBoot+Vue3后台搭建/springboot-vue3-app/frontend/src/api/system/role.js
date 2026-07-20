import request from '@/utils/request'

export function listRole(params) {
  return request({ url: '/system/role/list', method: 'get', params })
}

export function getRole(id) {
  return request({ url: '/system/role/' + id, method: 'get' })
}

export function addRole(data) {
  return request({ url: '/system/role', method: 'post', data })
}

export function updateRole(data) {
  return request({ url: '/system/role', method: 'put', data })
}

export function deleteRole(id) {
  return request({ url: '/system/role/' + id, method: 'delete' })
}

// 角色菜单授权：body = { role: { roleId }, menuIds: [...] }
export function saveRoleMenus(data) {
  return request({ url: '/system/role/menu', method: 'post', data })
}
