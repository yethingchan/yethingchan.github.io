# 11 · Linux 下安装 Node 构建前端

两种选择：① 在 Linux 上 `npm run build`；② 在开发机构建好，只传 `dist/`。推荐 ②（服务器更干净），但这里讲 ① 以备需要。

## 方案 A：Linux 上构建（nvm）

```bash
# 安装 nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.0/install.sh | bash
source ~/.bashrc
nvm install 22
nvm use 22

# 拉代码并构建
cd /opt/admin/frontend-src
npm install
npm run build
# 产物在 dist/，拷到 Nginx 目录
cp -r dist/* /opt/admin/frontend/
```

## 方案 B：开发机构建后传 dist（推荐）

```bash
# 本地
cd frontend && npm run build
# 传到服务器
scp -r dist/* user@ip:/opt/admin/frontend/
```

服务器**无需 Node**，Nginx 直接托管静态文件。更轻、更安全。

## 构建注意

- 若用方案 A，确保服务器内存足够（Node 构建约需 1G+）。
- 锁定 Node 版本与本地一致，避免行为差异。
- `.env`：本项目无 `.env`，无需处理；若未来加了，构建时 `NODE_ENV=production` 会自动读 `.env.production`。

## 验证构建产物

```bash
ls /opt/admin/frontend/
# 应有 index.html 和 assets/
```

## 小结

推荐方案 B：本地构建传 `dist/`，服务器不装 Node。需要服务器端构建时用 nvm 装 Node 22 再 `npm run build`。

下一篇：[Linux 下安装 JDK 运行 jar](./12-Linux下安装JDK运行jar.md)
