# 10 · 列表渲染 v-for

后台管理系统的表格、菜单、下拉项，全靠 `v-for` 渲染数组/对象。

## 渲染数组

```vue
<li v-for="(item, index) in items" :key="item.id">
  {{ index }} - {{ item.name }}
</li>
```

- `item`：当前项；`index`：下标（可选）。
- **必须加 `:key`**（唯一稳定标识），帮助 Vue 高效复用 DOM。

## key 的重要性

```vue
<!-- ❌ 用 index 当 key 可能导致状态错乱 -->
<li v-for="(item, i) in items" :key="i">

<!-- ✅ 用稳定 id -->
<li v-for="item in items" :key="item.id">
```

> 本项目所有表格行、菜单项都用 `:key="item.id"` 或 `:key="item.path"`，这是硬性规范。

## 渲染对象

```vue
<li v-for="(value, key) in user" :key="key">
  {{ key }}: {{ value }}
</li>
```

## 配合 template 渲染多节点

```vue
<template v-for="item in items" :key="item.id">
  <h3>{{ item.title }}</h3>
  <p>{{ item.desc }}</p>
</template>
```

## 本项目中的例子

菜单递归渲染（`sidebarItem.vue` 用 `v-for` 遍历 `item.children`）：

```vue
<template v-for="child in item.children" :key="child.path">
  <sidebar-item :item="child" />
</template>
```

表格行（`element-plus` 的 `el-table` 内部也是 `v-for`）：

```vue
<el-table :data="tableData" style="width:100%">
  <el-table-column prop="name" label="名称" />
  <el-table-column prop="status" label="状态" />
</el-table>
```

## 小结

- `v-for="(item, index) in list"`，务必 `:key` 用稳定唯一值。
- 表格、菜单、列表全靠它。
- 不要和 `v-if` 用在同一元素（优先级冲突），需要过滤先用 `computed`。

下一篇：[事件处理](./11-事件处理.md)
