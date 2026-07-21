# 05 · 计算属性 computed

当模板里需要**基于已有数据推导出新值**时，用计算属性，而不是把复杂逻辑写进模板。

## 基本用法

```vue
<script setup>
import { ref, computed } from 'vue'
const firstName = ref('张')
const lastName = ref('三')
// 全名 = 派生值
const fullName = computed(() => firstName.value + lastName.value)
</script>

<template>
  <p>{{ fullName }}</p>   <!-- 像普通变量一样用 -->
</template>
```

- `computed` 返回的是一个**只读的响应式 ref**。
- 依赖（`firstName`/`lastName`）变化时，`fullName` 自动重算。

## 计算属性 vs 方法

```vue
<p>{{ fullName }}</p>        <!-- computed：有缓存 -->
<p>{{ getFullName() }}</p>   <!-- 方法：每次渲染都调用 -->
```

| 对比 | computed | method |
|------|----------|--------|
| 缓存 | ✅ 依赖不变不重算 | ❌ 每次都执行 |
| 适合 | 派生数据 | 事件处理、无缓存需求 |

> 列表过滤、格式化日期、拼接名称，优先用 `computed`。

## 可写的计算属性（set）

```js
const fullName = computed({
  get: () => firstName.value + lastName.value,
  set: (val) => {
    [firstName.value, lastName.value] = val.split(' ')
  }
})
fullName.value = '李 四'   // 触发 set
```

## 本项目中的例子

侧边栏在 `dashboard` 前拼接动态菜单：

```js
const menuList = computed(() => [dashboardRoute, ...userStore.menus])
```

表格里根据状态字段算"是否禁用"：

```js
const disabledRows = computed(() =>
  tableData.value.filter(r => r.status === '0')
)
```

## 小结

- `computed` 用于**派生数据**，带缓存、效率高。
- 模板里当普通变量用；脚本里用 `.value`。
- 不要在计算属性里做异步/副作用（那该用 `watch` 或 `watchEffect`）。

下一篇：[侦听器 watch](./06-侦听器watch.md)
