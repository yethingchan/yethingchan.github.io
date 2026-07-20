import request from '@/utils/request'

export function listDictType(params) {
  return request({ url: '/system/dict/type/list', method: 'get', params })
}

export function dictTypeOptions() {
  return request({ url: '/system/dict/type/optionselect', method: 'get' })
}

export function getDictType(id) {
  return request({ url: '/system/dict/type/' + id, method: 'get' })
}

export function addDictType(data) {
  return request({ url: '/system/dict/type', method: 'post', data })
}

export function editDictType(data) {
  return request({ url: '/system/dict/type', method: 'put', data })
}

export function removeDictTypes(ids) {
  return request({ url: '/system/dict/type', method: 'delete', params: { ids } })
}

export function listDictData(params) {
  return request({ url: '/system/dict/data/list', method: 'get', params })
}

export function dictDataByType(dictType) {
  return request({ url: '/system/dict/data/type', method: 'get', params: { dictType } })
}

export function addDictData(data) {
  return request({ url: '/system/dict/data', method: 'post', data })
}

export function editDictData(data) {
  return request({ url: '/system/dict/data', method: 'put', data })
}

export function removeDictData(ids) {
  return request({ url: '/system/dict/data', method: 'delete', params: { ids } })
}
