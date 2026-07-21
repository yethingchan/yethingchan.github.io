# 16 · 插槽 slot

插槽让父组件向子组件的**指定位置插入内容**，实现布局/结构复用。

## 默认插槽

```vue
<!-- Card.vue -->
<template>
  <div class="card">
    <slot>默认内容（父没传时显示）</slot>
  </div>
</template>
```

```vue
<Card>这是卡片里的内容</Card>
```

## 具名插槽

```vue
<!-- Card.vue -->
<template>
  <div class="card">
    <header><slot name="header" /></header>
    <main><slot /></main>
    <footer><slot name="footer" /></footer>
  </div>
</template>
```

```vue
<Card>
  <template #header>标题</template>
  正文
  <template #footer>底部按钮</template>
</Card>
```

> `#header` 是 `v-slot:header` 的简写。

## 作用域插槽（子传数据给插槽）

```vue
<!-- List.vue -->
<template>
  <slot :item="item" :index="i" />
</template>
```

```vue
<List v-slot="{ item, index }">
  <p>{{ index }} - {{ item.name }}</p>
</List>
```

## 本项目中的例子

Element Plus 大量使用插槽：

```vue
<el-table :data="tableData">
  <el-table-column label="操作">
    <template #default="scope">
      <el-button @click="handleEdit(scope.row)">编辑</el-button>
      <el-button @click="handleDelete(scope.row)">删除</el-button>
    </template>
  </el-table-column>
</el-table>
```

`scope.row` 就是作用域插槽把"当前行数据"回传给父模板。

## 小结

- 默认插槽：`<slot>` 放内容。
- 具名插槽：`<slot name="x">` + `<template #x>`。
- 作用域插槽：子组件通过 `slot` 把数据传给父模板（`scope.row`）。

下一篇：[Provide 与 Inject 依赖注入](./17-Provide与Inject依赖注入.md)
