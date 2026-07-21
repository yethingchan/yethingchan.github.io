# 22 · 图标与 SVG 使用

本项目图标来自 **@element-plus/icons-vue**，在 main.js 已全局注册。

## 在模板直接使用

```vue
<el-button><el-icon><Edit /></el-icon>编辑</el-button>
<!-- 简写（全局注册后可直接用组件名） -->
<Edit />
<Delete />
<Search />
```

## 菜单图标（动态）

菜单图标名来自后端菜单 `meta.icon` 字段，用动态组件渲染：

```vue
<el-icon v-if="item.meta?.icon">
  <component :is="item.meta.icon" />
</el-icon>
```

> 所以后端 `sys_menu` 的 `icon` 字段必须填 **Element Plus 图标名**（如 `User`、`Setting`、`HomeFilled`），否则显示空白。

## 常用图标名

| 用途 | 图标名 |
|------|--------|
| 首页 | `HomeFilled` |
| 系统管理 | `Setting` |
| 用户 | `User` |
| 角色 | `Role` / `Avatar` |
| 菜单 | `Menu` |
| 字典 | `Notebook` |
| 编辑 | `Edit` |
| 删除 | `Delete` |
| 搜索 | `Search` |
| 新增 | `Plus` |

完整列表见 Element Plus 官方 Icons 页。

## 自定义 SVG（进阶）

如需自己的 SVG，放 `src/assets/` 并用 `vite-svg-loader` 或 `<img src>` 引入。本项目未使用自定义 SVG。

## 小结

图标 = Element Plus 图标库，全局注册后模板直接用 `<IconName/>`；菜单图标由后端 `meta.icon` 动态渲染。

下一篇：[环境变量与多环境配置](./23-环境变量与多环境配置.md)
