# 08 · Windows 下 Nginx 配置详解

深入 Nginx 配置的每个细节（Windows 与 Linux 配置语法一致，仅路径不同）。

## 完整配置模板（Windows 路径）

```nginx
worker_processes  1;

events {
    worker_connections 1024;
}

http {
    include       mime.types;
    default_type  application/octet-stream;
    sendfile      on;
    keepalive_timeout 65;
    gzip on;                       # 开启压缩，省流量
    gzip_types text/css application/javascript application/json image/svg+xml;

    server {
        listen       80;
        server_name  localhost;     # 有域名填域名

        # 前端静态资源
        location / {
            root   D:/deploy/frontend/dist;
            index  index.html;
            try_files $uri $uri/ /index.html;
        }

        # 后端接口反代
        location /api/ {
            proxy_pass http://127.0.0.1:8080/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_connect_timeout 60s;
            proxy_read_timeout 60s;
        }
    }
}
```

## 关键指令解释

| 指令 | 作用 |
|------|------|
| `root` | 静态文件根目录（dist） |
| `try_files $uri $uri/ /index.html` | history 模式防刷新 404 |
| `location /api/` | 匹配以 `/api/` 开头的请求 |
| `proxy_pass http://127.0.0.1:8080/` | 转发到后端；**末尾 `/` 会剥离 `/api`** |
| `proxy_set_header` | 把真实客户端信息传给后端（日志/IP 正确） |
| `gzip on` | 压缩静态资源，提速 |

## ⚠️ 两个最常见错误

1. **`proxy_pass` 末尾漏 `/`**：写成 `proxy_pass http://127.0.0.1:8080`（无斜杠），Nginx 会把 `/api/login` 原样转发成 `http://127.0.0.1:8080/api/login`，后端无此路由 → 404。
   - ✅ 正确：`proxy_pass http://127.0.0.1:8080/;`（带斜杠剥离 `/api`）。
2. **漏 `try_files`**：刷新 `/system/user` 直接 404。

## 多前端共存（子路径）

若前端挂在 `/admin/` 下，需让 Vite `base: '/admin/'`（改 `vite.config.js`）并：

```nginx
location /admin/ {
    alias D:/deploy/frontend/dist/;
    try_files $uri $uri/ /admin/index.html;
}
```

## 重载配置

```bat
nginx -s reload
```

## 小结

Nginx 配置核心：`root` + `try_files`（前端）、`location /api/` + `proxy_pass .../`（后端）。末尾斜杠和 try_files 是两大雷区。

下一篇：[Windows 服务化部署](./09-Windows服务化部署.md)
