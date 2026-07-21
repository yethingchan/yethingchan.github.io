# 03 · 产物 dist 目录说明

## 目录结构

```
dist/
├── index.html              # 入口，引用下面的 assets
└── assets/
    ├── index-[hash].css    # 入口样式（含 Element Plus）
    ├── index-[hash].js     # 入口 JS（Vue/Element Plus/路由/store）
    ├── 404-[hash].js       # 404 页
    ├── dashboard-[hash].js # 首页（懒加载块）
    ├── user-[hash].js      # 用户管理页（懒加载块）
    ├── dict-[hash].js      # 字典页
    └── ...                  # 其它视图分包
```

## 关键点

- **文件名带 hash**（`[hash]`）：内容变了 hash 就变，便于浏览器缓存（强缓存 + 内容变更即失效）。
- `index.html` 用相对路径引用 `assets/`（本项目 `base` 默认空），所以 `dist/` 可放任意子目录或根目录。
- 懒加载块：每个业务页独立 chunk，首屏只加载 `index-*.js`，进哪个菜单才加载对应块，**首屏更快**。

## 部署时怎么用

- 把整个 `dist/` 目录拷贝到 Nginx 的 `html/` 根（或子目录）。
- **不要只传 `index.html`**——必须连 `assets/` 一起，否则白屏（找不到 JS/CSS）。

## 更新部署

- 重新 `npm run build`，用新的 `dist/` **整体覆盖**旧的（旧的 hash 文件可删，避免堆积）。
- 因为 hash 变化，`index.html` 会引用新文件，用户自动拿到新版。

## 小结

`dist/` = `index.html` + `assets/`（带 hash）。整体拷贝到 Nginx，不可只传 html。

下一篇：[Nginx 基础与安装](./04-Nginx基础与安装.md)
