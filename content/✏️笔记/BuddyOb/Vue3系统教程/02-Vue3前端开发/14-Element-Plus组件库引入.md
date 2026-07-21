# 14 · Element Plus 组件库引入

Element Plus 是 Vue 3 的成熟 UI 库，本项目用它构建所有界面。

## 引入方式（本项目：全量）

`src/main.js`：

```js
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
app.use(ElementPlus)
```

全量引入后，模板里可直接用 `<el-button>`、`<el-table>` 等。

## 图标

```js
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
for (const [key, c] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, c)   // 全局注册，模板用 <Edit/> <Delete/> <Search/>
}
```

## 常用组件速查

| 组件 | 用途 |
|------|------|
| `el-container/el-aside/el-header/el-main` | 布局 |
| `el-menu/el-sub-menu/el-menu-item` | 菜单 |
| `el-table/el-table-column` | 表格 |
| `el-form/el-form-item` | 表单 |
| `el-input/el-select/el-radio/el-date-picker` | 输入 |
| `el-dialog` | 弹窗 |
| `el-button` | 按钮 |
| `el-pagination` | 分页 |
| `el-tag/el-switch/el-upload` | 标签/开关/上传 |
| `ElMessage/ElMessageBox` | 消息/确认框（JS 调用） |

## JS 消息（非组件）

```js
import { ElMessage, ElMessageBox } from 'element-plus'
ElMessage.success('操作成功')
ElMessageBox.confirm('确认删除？', '提示').then(() => { /* 确定 */ })
```

## 主题定制（可选）

全量引入后可改主题色（需 sass）。本项目未做，保持默认蓝 `#409EFF`。

## 小结

Element Plus 在 main.js 全量引入 + 全局注册图标。业务页直接用 `el-*` 组件和 `ElMessage`。

下一篇：[表格组件标准用法](./15-表格组件标准用法.md)
