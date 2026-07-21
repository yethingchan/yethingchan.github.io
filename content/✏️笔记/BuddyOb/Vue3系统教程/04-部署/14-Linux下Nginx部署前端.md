# 14 · Linux 下 Nginx 部署前端

## 上传 dist

```bash
# 本地开发机
cd frontend && npm run build
scp -r dist/* user@ip:/opt/admin/frontend/
```

## 写站点配置

在 `/etc/nginx/conf.d/admin.conf`（Linux 推荐放 conf.d）：

```nginx
server {
    listen 80;
    server_name your-domain.com;   # 或服务器 IP

    location / {
        root /opt/admin/frontend;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## 测试并重载

```bash
sudo nginx -t            # 语法检查
sudo systemctl reload nginx
```

## 权限注意

Nginx 进程（www-data/nginx 用户）要能读 `/opt/admin/frontend`：

```bash
sudo chmod -R 755 /opt/admin/frontend
sudo chown -R www-data:www-data /opt/admin/frontend   # 若 Nginx 用 www-data
```

## 验证

浏览器开 `http://服务器IP` → 登录页；刷新子路由不 404；登录后 `/api` 请求 200。

## 小结

Linux 前端部署：传 `dist/` → 写 `conf.d/admin.conf`（`root`+`try_files`+`/api` 反代）→ `nginx -t` → `reload`。

下一篇：[Linux 下 Nginx 配置详解](./15-Linux下Nginx配置详解.md)
