# 04 · 响应式系统：ref 与 reactive

**响应式是 Vue 的核心。** 数据变 → 视图自动变。Vue 3 用 `Proxy` 实现，提供了两个入口：`ref` 和 `reactive`。

## 1. `ref`：任意类型的值

```vue
<script setup>
import { ref } from 'vue'
const count = ref(0)          // 用 ref 包裹
console.log(count.value)      // 脚本里访问要 .value
count.value++                 // 修改要 .value
</script>

<template>
  <p>{{ count }}</p>          <!-- 模板里不用 .value，自动解包 -->
</template>
```

- `ref` 可以包**基本类型**（数字、字符串、布尔）和**对象**。
- 模板中直接 `{{ count }}`，Vue 自动解包。
- 脚本逻辑里要用 `count.value`。

## 2. `reactive`：对象/数组

```vue
<script setup>
import { reactive } from 'vue'
const user = reactive({ name: 'admin', age: 18 })
user.age = 19                // 直接改属性，无需 .value
</script>
```

- `reactive` 只接受**对象类型**（Object / Array / Map 等）。
- 修改属性直接 `user.age = 19`，无需 `.value`。
- ⚠️ **不要解构 `reactive` 对象**，否则会丢失响应性：
  ```js
  const { name } = user   // ❌ name 不再是响应式的
  // 正确做法：用 toRefs
  import { toRefs } from 'vue'
  const { name } = toRefs(user)  // ✅
  ```

## 3. 该用哪个？

| 场景 | 推荐 |
|------|------|
| 基本类型（计数、开关、ID） | `ref` |
| 表单对象、多个字段聚合 | `reactive` 或 `ref({...})` |
| 在 Pinia / 组合函数里返回 | 通常用 `ref` |

> 本项目的 `user.js` store 里 `roles`、`permissions` 用的是普通数组 state（Pinia 内部已做响应式），而组件里常用 `ref` 存列表数据。

## 4. 响应式的本质（理解即可）

```js
const count = ref(0)
// Vue 在"读取"count 时收集依赖（哪些视图在用）
// 在"写入"count.value 时触发更新（通知那些视图重渲染）
```

这就是"依赖收集 + 派发更新"，无需你手动 `innerHTML`。

## 小结

- `ref(值)`：任意类型，脚本里用 `.value`，模板里自动解包。
- `reactive(对象)`：对象/数组，直接改属性；别解构。
- 响应式 = 数据变，视图自动变。

下一篇：[计算属性 computed](./05-计算属性computed.md)
