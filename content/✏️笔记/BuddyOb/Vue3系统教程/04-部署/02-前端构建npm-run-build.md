# 02 · 前端构建 npm run build

## 构建命令

```bash
cd frontend
npm install      # 首次或依赖变了才需要
npm run build    # 产物输出到 frontend/dist/
```

> 本项目 `package.json` 的 `build` = `vite build`，无需额外参数。

## 构建前检查

1. **依赖已装**：`node_modules` 存在；换机器先 `npm install`。
2. **代理/前缀正确**：本项目 `baseURL='/api'` 硬编码，生产靠 Nginx 反代，**无需改代码**。
3. **后端地址**：若想直连后端（不用 Nginx 反代），需加 `.env.production` 改 `request.js`（见 02 分册 23 篇）。本教程用 Nginx 反代方案，零改代码。

## 构建过程发生了什么

- Vite 用 Rollup 打包：合并、压缩、Tree-shaking。
- 各 `views/**/*.vue` 被拆成独立 chunk（懒加载）。
- 输出 `dist/index.html` + `dist/assets/*.js|css`。

## 验证构建产物

```bash
npm run preview    # 本地起 4173 预览 dist
```

或直接在浏览器打开 `dist/index.html`（因 `base` 为空，可直接 file 打开看静态结构，但 API 调用会因跨域/无后端失败，仅验证打包成功）。

## ⚠️ 常见坑

- `dist/` 是 7-19 旧构建：源码改过必须**重新 build**，否则部署的是旧代码。
- 磁盘满/内存不足导致 build 失败：清理或加 `--max-old-space-size`。
- Node 版本过低：本项目用 Node 22，低版本可能报错。

## 小结

`npm run build` 生成 `dist/`（静态资源）。生产靠 Nginx 托管 + 反代，前端零改代码。

下一篇：[产物 dist 目录说明](./03-产物dist目录说明.md)
