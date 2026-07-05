本教程将完整介绍如何将 Quartz 博客部署到 GitHub Pages，并配置 GitHub Actions 实现内容更新后的自动部署。我们以本项目的实际配置为例，逐步讲解每一个环节。

## GitHub Pages 部署完整步骤

### 第一步：创建 GitHub 仓库

如果你的 GitHub 用户名是 `yethingchan`，需要创建一个名为 `yethingchan.github.io` 的仓库：

1. 登录 GitHub，点击右上角的 "New repository"
2. Repository name 填写 `yethingchan.github.io`（必须与用户名完全匹配）
3. 选择 Public（公开仓库，GitHub Pages 免费版仅支持公开仓库）
4. 勾选 "Add a README file"
5. 点击 "Create repository"

如果你的博客不部署在用户名仓库上，而是部署在 `username/blog` 这样的项目仓库中，则 base URL 需要调整为 `/blog`。本教程以前者为例。

### 第二步：配置 baseUrl

在 `quartz.config.ts` 中设置 `baseUrl`：

```typescript
// quartz.config.ts
configuration: {
  pageTitle: "Odyssey",
  pageTitleSuffix: " | MD文件库",
  baseUrl: "yethingchan.github.io",
  // ...
}
```

`baseUrl` 用于生成 `CNAME` 文件、`sitemap.xml`、RSS feed 和 robots.txt 中的绝对 URL。注意这里只填写域名，不需要加 `https://` 前缀。

`baseUrl` 在 `quartz/cfg.ts` 的 `GlobalConfiguration` 接口中定义为可选字段：

```typescript
/** Base URL to use for CNAME files, sitemaps, and RSS feeds that require an absolute URL.
 *   Quartz will avoid using this as much as possible and use relative URLs most of the time
 */
baseUrl?: string
```

这意味着 Quartz 大部分情况下使用相对路径，仅在需要绝对 URL 的场景（如 sitemap、RSS）才引用 `baseUrl`。

### 第三步：设置 GitHub Pages 源

本项目的部署方式使用 GitHub Actions 直接部署，不需要手动设置 Pages 源。但如果你需要手动配置：

1. 进入仓库的 Settings > Pages
2. Source 选择 "GitHub Actions"（推荐，与我们的 CI/CD 工作流配合）
3. 不要选择 "Deploy from a branch"，因为 GitHub Pages 的传统部署方式需要 `gh-pages` 分支，而我们使用 Actions 直接部署更简洁

## GitHub Actions CI/CD 配置

### deploy.yaml 工作流分析

本项目的部署工作流位于 `.github/workflows/deploy.yaml`，完整内容如下：

```yaml
name: Deploy Quartz to GitHub Pages

on:
  push:
    branches:
      - main

permissions:
  contents: read
  pages: write
  id-token: write

concurrency:
  group: pages
  cancel-in-progress: false

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: 22
          cache: npm

      - name: Install dependencies
        run: npm ci

      - name: Build Quartz
        run: npx quartz build

      - name: Upload artifact
        uses: actions/upload-pages-artifact@v3
        with:
          path: public

  deploy:
    needs: build
    runs-on: ubuntu-latest
    environment:
      name: github-pages
    steps:
      - name: Deploy to GitHub Pages
        id: deployment
        uses: actions/deploy-pages@v4
```

### 工作流逐行解读

**触发条件**：当有代码推送到 `main` 分支时自动触发。这意味着每次你将新的 Markdown 内容或配置变更 push 到 main 分支，都会自动触发构建和部署。

**权限声明**：

```yaml
permissions:
  contents: read    # 读取仓库代码
  pages: write      # 写入 GitHub Pages
  id-token: write   # 身份令牌（用于部署认证）
```

**并发控制**：

```yaml
concurrency:
  group: pages
  cancel-in-progress: false
```

`cancel-in-progress: false` 表示如果上一次部署还在进行中，新的部署会排队等待而非取消。这可以避免竞态条件，确保部署的稳定性。

**Job 1: build（构建阶段）**

1. **Checkout**：`actions/checkout@v4` 检出代码，`fetch-depth: 0` 拉取完整 git 历史（`CreatedModifiedDate` 插件需要 git 历史来确定文件创建时间）
2. **Setup Node.js**：`actions/setup-node@v4` 安装 Node.js 22，并启用 npm 缓存
3. **Install dependencies**：`npm ci` 根据锁文件精确安装依赖（比 `npm install` 更严格、更快）
4. **Build**：`npx quartz build` 执行全量构建，生成 `public/` 目录
5. **Upload artifact**：将 `public/` 目录上传为构建产物，供 deploy 阶段使用

**Job 2: deploy（部署阶段）**

依赖 build job 完成后，使用 `actions/deploy-pages@v4` 将构建产物部署到 GitHub Pages。

### 构建缓存优化

工作流中已经配置了 npm 缓存：

```yaml
- name: Setup Node.js
  uses: actions/setup-node@v4
  with:
    node-version: 22
    cache: npm
```

`cache: npm` 告诉 setup-node 插件缓存 `node_modules`。基于 `package-lock.json` 的哈希值来判断缓存是否有效，通常能将依赖安装时间从 30-60 秒缩短到 5-10 秒。

### 自动部署的注意事项

1. **首次部署**：首次推送代码后，需要在仓库 Settings > Pages 中手动将 Source 切换为 "GitHub Actions"，否则 Actions 部署会失败
2. **构建时间**：GitHub Actions 免费版每月有 2000 分钟的额度。每次构建大约需要 1-2 分钟（含依赖安装和构建），对于博客项目绰绰有余
3. **部署延迟**：GitHub Pages 部署后通常需要 1-3 分钟才能在全球 CDN 节点生效
4. **构建失败通知**：建议在仓库 Settings > Notifications 中开启 Actions 通知，以便在构建失败时及时收到邮件提醒

## 自定义域名配置

如果你想使用自定义域名（如 `blog.example.com`）替代 `yethingchan.github.io`，需要进行以下配置：

### 1. 添加 CNAME 插件

在 `quartz.config.ts` 的 emitters 中添加 CNAME 插件：

```typescript
emitters: [
  // ...其他 emitters
  Plugin.CNAME(),  // 自动在 public/ 目录中生成 CNAME 文件
]
```

本项目已经包含此插件。

### 2. 配置 DNS 解析

在你的域名服务商处添加以下 DNS 记录：

| 类型 | 名称 | 值 |
|------|------|-----|
| CNAME | `blog` 或 `@` | `yethingchan.github.io` |

如果你使用根域名（`example.com`），需要添加 A 记录指向 GitHub Pages 的 IP：
- `185.199.108.153`
- `185.199.109.153`
- `185.199.110.153`
- `185.199.111.153`

### 3. 在 GitHub 设置中配置域名

进入仓库 Settings > Pages > Custom domain，填入你的域名并勾选 "Enforce HTTPS"。

### 4. 更新 baseUrl

如果使用自定义域名，记得更新 `quartz.config.ts` 中的 `baseUrl`：

```typescript
baseUrl: "blog.example.com",
```

同时更新 `quartz/static/robots.txt` 中的 Sitemap URL：

```
User-agent: *
Allow: /
Sitemap: https://blog.example.com/sitemap.xml
```

## 部署后验证

部署完成后（通常在 push 后 2-5 分钟），需要进行以下验证：

### 检查所有页面是否正常

1. 访问 `https://yethingchan.github.io/`，确认首页正常加载
2. 点击侧边栏的 Explorer，展开文件夹树，随机访问几个页面
3. 检查中文路径的页面是否能正常访问（如 `https://yethingchan.github.io/笔记/C sharp/async await`）
4. 检查静态资源是否加载完成（字体、图标、背景图片等）

### 检查 SPA 路由是否工作

本项目启用了 SPA 模式（`enableSPA: true`）：

1. 在首页点击任意文章链接，观察页面是否无刷新地切换内容
2. 使用浏览器的前进/后退按钮，确认 SPA 路由正常工作
3. 直接在地址栏输入一个子页面的完整 URL 并回车，确认能正常加载（刷新 SPA 路由的关键测试）

### 检查搜索功能

1. 点击左侧搜索栏，输入关键词
2. 确认搜索结果能正确显示
3. 确认搜索结果的链接能正确跳转

### 检查暗色模式

1. 点击左侧工具栏的暗色模式切换按钮
2. 确认页面颜色方案切换为暗色主题
3. 确认自定义样式（背景渐变、文章卡片、代码块）在暗色模式下正常显示
4. 刷新页面后确认主题偏好被正确保存

## 常见部署问题

### Base URL 配置错误

**症状**：页面能打开，但 CSS/JS 全部 404，页面显示为纯文本无样式。

**原因**：`baseUrl` 配置不正确。

**排查方法**：
- 如果部署在用户名仓库（`username.github.io`），`baseUrl` 只需要填域名，不加 `https://`
- 如果部署在项目仓库（`username/repo`），`baseUrl` 需要填 `username.github.io/repo`
- 检查 `quartz.config.ts` 中 `baseUrl` 是否有拼写错误

```typescript
// 正确
baseUrl: "yethingchan.github.io"

// 错误
baseUrl: "https://yethingchan.github.io"  // 不要加协议前缀
baseUrl: "yethingchan.github.io/"        // 不要加尾部斜杠
```

### CSS/JS 路径不正确

**症状**：部分样式丢失，或者 JavaScript 功能异常。

**排查方法**：
1. 打开浏览器开发者工具（F12），查看 Console 和 Network 面板
2. 筛选红色（失败）的请求，查看具体是哪些资源 404
3. 检查 `quartz/styles/custom.scss` 中引用的静态资源路径是否正确（如字体文件路径）

本项目的字体引用在 `quartz/styles/custom.scss` 中：

```scss
@font-face {
  font-family: "LXGWWenKaiScreenR";
  src: url("/static/fonts/LXGWWenKaiScreen.woff2") format("woff2");
  font-display: swap;
}
```

确保字体文件 `quartz/static/fonts/LXGWWenKaiScreen.woff2` 存在，构建后会被复制到 `public/static/fonts/` 目录。

### 404 页面配置

**症状**：访问不存在的路径时显示 GitHub 默认的 404 页面，而非 Quartz 自定义的 404 页面。

**排查方法**：
1. 确认 `quartz.config.ts` 的 emitters 中包含 `Plugin.NotFoundPage()`
2. 确认构建产物 `public/404.html` 文件存在
3. 如果使用自定义域名，可能需要在 DNS 配置中额外添加一条通配符记录
4. 在 GitHub Pages 设置中确认 Source 是 "GitHub Actions" 而非某个分支

### 部署未生效

**症状**：push 代码后网站没有更新。

**排查方法**：
1. 进入仓库的 Actions 标签页，检查最近的 workflow run 状态
2. 如果 run 失败，点击查看日志定位错误
3. 如果 run 成功但页面未更新，等待 2-3 分钟让 CDN 缓存刷新
4. 尝试强制刷新浏览器（Ctrl+Shift+R）

### 内容更新自动部署流程总结

整个自动部署流程如下：

```
本地编辑 Markdown → git add & commit → git push origin main
        ↓
GitHub Actions 触发 → checkout 代码 → 安装 Node.js 22
        ↓
npm ci 安装依赖 → npx quartz build 全量构建
        ↓
上传 public/ 构建产物 → 部署到 GitHub Pages
        ↓
CDN 分发生效 → 网站更新完成（约 2-5 分钟）
```

如果你需要更快的部署体验，也可以使用本项目中已有的 `.github/workflows/uslessflows/` 目录下的预览工作流，它们可以在 Pull Request 中生成预览部署。
