# 06 · 侦听器 watch

当需要在**数据变化时执行副作用**（发请求、操作 DOM、写 localStorage）时，用 `watch`。

## 基本用法

```vue
<script setup>
import { ref, watch } from 'vue'
const count = ref(0)
watch(count, (newVal, oldVal) => {
  console.log(`count 从 ${oldVal} 变成 ${newVal}`)
})
</script>
```

- 第一个参数是"要侦听的数据源"（ref / reactive / getter 函数）。
- 回调收到 `(新值, 旧值)`。

## 侦听多个源

```js
watch([foo, bar], ([newFoo, newBar]) => { /* ... */ })
```

## 侦听 reactive 对象的某个属性

```js
watch(() => user.age, (newAge) => { /* ... */ })
```

## 深度侦听（对象内部变化）

```js
watch(form, () => { /* ... */ }, { deep: true })
```

> 注意：`deep: true` 会遍历整个对象，大对象有性能成本。

## 立即执行：`immediate`

```js
watch(keyword, fetchList, { immediate: true })  // 挂载即先跑一次
```

## `watch` vs `watchEffect`

```js
import { watchEffect } from 'vue'
watchEffect(() => {
  // 自动收集依赖：函数里用到的响应式数据都会侦听
  console.log(count.value)
})
```

| 对比 | watch | watchEffect |
|------|-------|-------------|
| 依赖 | 显式指定 | 自动收集 |
| 旧值 | 有 | 无 |
| 立即执行 | 需 `immediate` | 默认立即 |

## 本项目中的例子

搜索关键字变化自动拉列表：

```js
watch(() => queryParams.keyword, () => { handleQuery() })
```

## 小结

- 数据变化要"做事"（请求/副作用）→ 用 `watch` 或 `watchEffect`。
- 派生展示数据 → 用 `computed`（上篇）。
- 别在 `watch` 里改它自己侦听的数据，容易死循环。

下一篇：[组合式 API 与选项式 API](./07-组合式API与选项式API.md)
