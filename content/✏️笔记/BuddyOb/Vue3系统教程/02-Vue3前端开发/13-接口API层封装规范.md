# 13 · 接口 API 层封装规范

本项目约定：**每个后端模块对应一个 `src/api/*.js` 文件，只写请求函数**。

## 标准写法

`src/api/system/user.js`：

```js
import request from '@/utils/request'

// 列表（GET，参数走 query）
export function listUser(query) {
  return request({ url: '/system/user/list', method: 'get', params: query })
}
// 详情（路径参数）
export function getUser(id) {
  return request({ url: `/system/user/${id}`, method: 'get' })
}
// 新增（POST，body）
export function addUser(data) {
  return request({ url: '/system/user', method: 'post', data })
}
// 修改
export function updateUser(data) {
  return request({ url: '/system/user', method: 'put', data })
}
// 删除（路径参数）
export function delUser(id) {
  return request({ url: `/system/user/${id}`, method: 'delete' })
}
// 批量删除（数组参数）
export function delUsers(ids) {
  return request({ url: '/system/user/' + ids.join(','), method: 'delete' })
}
```

## 约定总结

| 操作 | method | 参数位置 | URL 风格 |
|------|--------|----------|----------|
| 列表 | GET | `params` | `/module/list` |
| 详情 | GET | 路径 | `/module/{id}` |
| 新增 | POST | `data` | `/module` |
| 修改 | PUT | `data` | `/module` |
| 删除 | DELETE | 路径 | `/module/{id}` |

- GET 用 `params`（拼到 URL 查询串）；POST/PUT 用 `data`（放请求体）。
- 函数名 `listXxx / getXxx / addXxx / updateXxx / delXxx`，见名知意。

## 在页面里调用

```vue
<script setup>
import { listUser } from '@/api/system/user'
const tableData = ref([])
function getList() {
  listUser(queryParams).then(res => {
    tableData.value = res.data.rows      // 数据在 res.data
  })
}
</script>
```

## 本项目已有的 api 文件

```
src/api/login.js
src/api/system/{user,role,menu,dict,config}.js
src/api/business/notice.js
src/api/monitor/operlog.js
```

## 小结

API 层 = 每个模块一个 js，只封装请求函数（REST 风格）。页面 `import` 后调用，数据在 `res.data`。

下一篇：[Element Plus 组件库引入](./14-Element-Plus组件库引入.md)
