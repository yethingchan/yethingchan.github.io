# 03 · package.json 详解

本项目 `frontend/package.json` 真实内容（已核对）：

```json
{
  "name": "admin-frontend",
  "version": "1.0.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "@element-plus/icons-vue": "^2.3.1",
    "axios": "^1.7.7",
    "element-plus": "^2.8.4",
    "pinia": "^2.2.4",
    "vue": "^3.5.10",
    "vue-router": "^4.4.5"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.1.4",
    "vite": "^5.4.8"
  }
}
```

## 关键点

### `"type": "module"`
- `.js` 文件按 **ESM** 解析，因此 `vite.config.js` 里能写 `import`。
- 若写成 CommonJS（`require`）会报错。

### scripts

| 命令 | 等价 | 说明 |
|------|------|------|
| `npm run dev` | `vite` | 开发服务器，端口 3000 |
| `npm run build` | `vite build` | 生产构建 → `dist/` |
| `npm run preview` | `vite preview` | 预览 `dist/`，端口 4173 |

> 没有 `lint` / `test` 脚本（纯教学项目，未集成）。

### 依赖分层
- `dependencies`：运行时需要（Vue、Element Plus、Pinia、axios、vue-router）。
- `devDependencies`：仅开发需要（Vite、plugin-vue）。

## 常用操作

```bash
npm install           # 安装所有依赖
npm install axios@1   # 加一个运行依赖
npm install -D vite@5 # 加一个开发依赖
npm run build         # 构建
```

## 小结

`package.json` 是工程清单：`type:module` 定模块规范，`scripts` 定三条命令，`dependencies`/`devDependencies` 分层管理依赖。

下一篇：[Vite 配置详解](./04-Vite配置详解.md)
