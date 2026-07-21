# 04 · Nginx 基础与安装

Nginx 是部署的核心：托管前端静态文件 + 反代 `/api` 到后端。

## Nginx 能做什么（本项目用到的）

1. **静态文件服务**：把 `dist/` 当静态资源返回（快、支持 gzip、缓存）。
2. **反向代理**：把 `/api/*` 转发到后端 8080。
3. **HTTPS 终结**：在 Nginx 上配 SSL，后端仍用 HTTP（内网）。
4. **负载均衡**（多实例后端时）。

## Windows 安装

1. 下载：https://nginx.org/en/download.html （Stable 版，如 `nginx/Windows-1.26.x`）
2. 解压到如 `D:\APP\nginx\`。
3. 启动：双击 `nginx.exe`，或命令行 `start nginx`。
4. 验证：浏览器开 `http://localhost` → 看到 Nginx 欢迎页。
5. 常用命令（在 nginx 目录下）：
   ```bat
   nginx -s stop      # 停止
   nginx -s reload    # 重载配置（改了 conf 不用重启）
   nginx -t           # 测试配置文件语法
   ```

## Linux 安装（Ubuntu/Debian）

```bash
sudo apt update
sudo apt install nginx -y
sudo systemctl enable nginx
sudo systemctl start nginx
```

CentOS：
```bash
sudo yum install epel-release -y
sudo yum install nginx -y
sudo systemctl enable --now nginx
```

## 配置文件位置

- Windows：`conf/nginx.conf`（在解压目录内）。
- Linux：`/etc/nginx/nginx.conf`（主），站点放 `/etc/nginx/conf.d/*.conf`。

## 小结

Nginx = 静态托管 + 反代 + HTTPS。Windows 解压即用，Linux `apt/yum install`。改配置后 `nginx -s reload` 或 `systemctl reload nginx`。

下一篇：[Windows 环境准备](./05-Windows环境准备.md)
