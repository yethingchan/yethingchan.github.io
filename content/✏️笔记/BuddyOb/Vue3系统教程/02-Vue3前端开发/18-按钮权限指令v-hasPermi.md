# 18 · 按钮权限指令 v-hasPermi

本项目用**自定义指令** `v-hasPermi` 控制按钮显隐，比 `v-if` 更彻底（无权限直接移除 DOM）。

## 用法

```vue
<el-button v-hasPermi="'system:user:add'">新增</el-button>
<el-button v-hasPermi="'system:user:edit'">编辑</el-button>
```

无 `system:user:add` 权限时，这个按钮**根本不渲染**。

## 实现（src/directive/hasPermi.js）

```js
import { hasPermi } from '@/utils/permission'
export default {
  mounted(el, binding) {
    const { value } = binding
    if (value && !hasPermi(value)) {
      if (el.parentNode) el.parentNode.removeChild(el)  // 直接移除节点
    }
  }
}
```

注册（`src/directive/index.js`）：

```js
import hasPermi from './hasPermi'
export default {
  install(app) { app.directive('hasPermi', hasPermi) }
}
```

## 权限判断（src/utils/permission.js）

```js
import { useUserStore } from '@/store/modules/user'
export function hasPermi(permission) {
  const userStore = useUserStore()
  if (!userStore.permissions) return false
  return userStore.permissions.includes(permission)
}
```

- 权限串是字符串数组（如 `['system:user:add', 'system:user:edit']`），来自 `/getInfo` 的 `permissions` 字段。
- 指令在 `mounted` 时判断；没有就 `removeChild`。

## ⚠️ 三处必须一致

前端 `v-hasPermi` 的权限串 = 后端 `@PreAuthorize("hasAuthority('system:user:add')")` = 数据库 `sys_menu` 的 `perms` 字段。三者对不上，按钮要么不显示、要么点了被后端拦截。

## 小结

`v-hasPermi="'xxx:yyy:zzz'"` 无权限移除按钮；权限串来自 `userStore.permissions`，必须与后端一致。

下一篇：[布局组件 Layout 解析](./19-布局组件Layout解析.md)
