# 19 · 过渡与动画 Transition

Vue 提供 `<Transition>` 组件，给元素进出场加动画。

## 基本用法

```vue
<Transition name="fade">
  <p v-if="show">我会淡入淡出</p>
</Transition>
```

对应 CSS（以 `fade` 为前缀）：

```css
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
```

## 类名生命周期

```
进入：v-enter-from → v-enter-active → v-enter-to
离开：v-leave-from → v-leave-active → v-leave-to
```

## 内置 name 钩子

- 列表用 `<TransitionGroup>`（配合 `v-for` + `:key`）。
- 可监听 JS 钩子：`@before-enter` / `@enter` / `@leave`。

## 本项目中的例子

弹窗（`el-dialog`）自带过渡；菜单展开、标签页切换用 Element Plus 内置动画。表格行新增也可加：

```vue
<TransitionGroup name="list" tag="div">
  <div v-for="item in list" :key="item.id">{{ item.name }}</div>
</TransitionGroup>
```

```css
.list-enter-active, .list-leave-active { transition: all 0.3s; }
.list-enter-from, .list-leave-to { opacity: 0; transform: translateX(-20px); }
```

## 小结

- `<Transition>` 给单个元素进出场加动画，靠 CSS class 钩子。
- 列表用 `<TransitionGroup>` + `:key`。
- Element Plus 组件多数自带动画，无需手写。

下一篇：[Teleport 与片段](./20-Teleport与片段.md)
