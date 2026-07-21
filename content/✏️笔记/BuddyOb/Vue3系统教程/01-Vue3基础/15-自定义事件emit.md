# 15 · 自定义事件 emit

`emit` 是**子组件向父组件通信**的通道，用来"通知父组件发生了什么事"。

## 声明与触发

```vue
<!-- Child.vue -->
<script setup>
const emit = defineEmits(['submit', 'update:count'])
function handleClick() {
  emit('submit', { id: 1 })          // 可带载荷
  emit('update:count', 10)
}
</script>
<template>
  <button @click="handleClick">提交</button>
</template>
```

## 父组件监听

```vue
<Child @submit="onSubmit" @update:count="onCountChange" />
```

```js
function onSubmit(payload) { console.log(payload.id) }
function onCountChange(val) { count.value = val }
```

## 重要模式：v-model 在组件上（双向绑定）

子组件用 `update:modelValue` 事件配合父组件的 `v-model`：

```vue
<!-- Child.vue -->
<script setup>
const props = defineProps(['modelValue'])
const emit = defineEmits(['update:modelValue'])
function onInput(e) { emit('update:modelValue', e.target.value) }
</script>
<template>
  <input :value="props.modelValue" @input="onInput" />
</template>
```

```vue
<!-- Parent.vue -->
<Child v-model="name" />
```

> Element Plus 的 `el-input`、`el-dialog`（`v-model="visible"`）都是这个原理。

## 本项目中的例子

弹窗子组件通知父组件关闭/刷新：

```vue
<!-- 父：views/system/user/index.vue -->
<el-dialog v-model="open" title="新增用户" @closed="getList">
  ...
</el-dialog>
```

删除确认后通知父组件刷新列表：

```js
// 子组件（操作列按钮在父页面内时，直接调用父方法）
function handleDelete(row) {
  ElMessageBox.confirm('确认删除？').then(() => {
    delUser(row.id).then(() => { ElMessage.success('删除成功'); getList() })
  })
}
```

## 小结

- `emit` = 子通知父，可带数据。
- 声明 `defineEmits`，触发 `emit('事件名', 数据)`。
- `update:modelValue` + `v-model` 实现组件双向绑定（Element Plus 弹窗/输入都用）。

下一篇：[插槽 slot](./16-插槽slot.md)
