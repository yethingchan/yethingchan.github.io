# 14 · Props：父子组件通信

`props` 是**父组件向子组件传数据**的通道，子组件只读不写。

## 声明 Props

```vue
<!-- Child.vue -->
<script setup>
const props = defineProps({
  title: String,
  count: { type: Number, default: 0 },
  list: { type: Array, required: true }
})
console.log(props.title)
</script>
```

也可简写类型：

```js
const props = defineProps(['title', 'count'])
```

## 父组件传值

```vue
<Child :title="pageTitle" :count="total" :list="rows" />
```

- 静态值可不加 `:`：`<Child title="固定标题" />`。
- 动态值加 `:`：`<Child :count="total" />`。

## ⚠️ Props 是单向数据流

```
父 ──(props)──▶ 子
```

- 子组件**不应修改 props**（会报警告）。
- 想改，应该在父组件改，或通过 `emit` 通知父组件改。
- 若需基于 props 派生，用 `computed`：
  ```js
  const upperTitle = computed(() => props.title.toUpperCase())
  ```

## 对象/数组作为 props

- 引用类型，子组件改内部属性虽不报错但**违背单向流**，应尽量避免。
- 推荐模式：子组件 `emit('update', newValue)`，父组件更新后传回。

## 本项目中的例子

侧边栏递归（`sidebarItem.vue`）通过 `props.item` 接收菜单节点：

```vue
<script setup>
const props = defineProps({ item: { type: Object, required: true } })
</script>
<template>
  <el-sub-menu v-if="props.item.children?.length" :index="props.item.path">
    <template #title>{{ props.item.meta.title }}</template>
    <sidebar-item
      v-for="child in props.item.children"
      :key="child.path"
      :item="child"
    />
  </el-sub-menu>
</template>
```

## 小结

- `props` = 父传子，只读。
- 声明用 `defineProps`，传值用 `:属性`。
- 单向数据流：子不改 props，要改就 `emit` 让父改。

下一篇：[自定义事件 emit](./15-自定义事件emit.md)
