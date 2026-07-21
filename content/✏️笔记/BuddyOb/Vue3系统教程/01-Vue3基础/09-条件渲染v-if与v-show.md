# 09 · 条件渲染：v-if 与 v-show

控制元素"显不显示"有两个指令，行为不同。

## v-if：真正地创建/销毁

```vue
<h1 v-if="isAdmin">管理员面板</h1>
<p v-else-if="isOp">操作员面板</p>
<p v-else>游客面板</p>
```

- 条件为 `false` 时，元素**不会出现在 DOM 中**（destroyed）。
- 切换开销大（频繁切换不划算），但初始为 false 时性能好。

## v-show：切 display

```vue
<div v-show="showPanel">内容</div>
```

- 元素**始终在 DOM 中**，只是 `display: none`。
- 切换开销小，适合频繁显示/隐藏。

## 对比

| 对比 | v-if | v-show |
|------|------|--------|
| DOM | 真删/真建 | 保留，改 `display` |
| 切换成本 | 高 | 低 |
| 适用 | 不常切换、权限控制 | 频繁切换（如 tab、弹层） |
| 可与 v-else | ✅ | ❌ |

## 本项目中的例子

权限指令 `v-hasPermi` 本质是比 `v-if` 更彻底——**无权限直接移除 DOM 节点**：

```vue
<el-button v-hasPermi="'system:user:add'">新增</el-button>
```

`v-hasPermi` 的实现（`directive/hasPermi.js`）在无权限时 `parentNode.removeChild(el)`，比 `v-show` 更省。

## 模板上的 v-if 与 key

用 `v-if` 切换同一位置的不同组件时，加 `key` 可强制重建：

```vue
<component :is="view" :key="view" />
```

## 小结

- 权限/一次性显示 → `v-if`（彻底移除）。
- 频繁切换 → `v-show`（留 DOM）。
- 本项目按钮权限用 `v-hasPermi`（移除节点），比两者都彻底。

下一篇：[列表渲染 v-for](./10-列表渲染v-for.md)
