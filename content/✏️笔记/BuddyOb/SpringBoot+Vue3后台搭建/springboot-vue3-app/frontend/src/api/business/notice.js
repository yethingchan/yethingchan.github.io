import request from '@/utils/request'

export function listNotice(params) {
  return request({ url: '/business/notice/list', method: 'get', params })
}

export function getNotice(id) {
  return request({ url: '/business/notice/' + id, method: 'get' })
}

export function addNotice(data) {
  return request({ url: '/business/notice', method: 'post', data })
}

export function editNotice(data) {
  return request({ url: '/business/notice', method: 'put', data })
}

export function removeNotices(ids) {
  return request({ url: '/business/notice', method: 'delete', params: { ids } })
}
