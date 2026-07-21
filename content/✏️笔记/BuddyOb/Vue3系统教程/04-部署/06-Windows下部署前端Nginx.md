# 06 · Windows 下部署前端（Nginx）

## 步骤

1. 前端构建（在开发机或构建机）：
   ```bash
   cd frontend && npm run build
   ```
2. 把 `dist/` 整个目录拷到 Windows 服务器，如 `D:\deploy\frontend\dist\`。
3. 配置 Nginx（编辑 `conf/nginx.conf` 或新建 `conf/conf.d/admin.conf`），见 08 篇。
4. 重载 Nginx：`nginx -s reload`。
5. 浏览器访问 `http://服务器IP` → 看到登录页。

## 最简 Nginx 配置（先跑起来）

```nginx
worker_processes  1;
events { worker_connections 1024; }
http {
    include       mime.types;
    default_type  application/octet-stream;
    sendfile      on;

    server {
        listen       80;
        server_name  localhost;

        location / {
            root   D:/deploy/frontend/dist;
            index  index.html;
            try_files $uri $uri/ /index.html;   # history 模式必备
        }

        location /api/ {
            proxy_pass http://127.0.0.1:8080/;   # 末尾 / 剥离 /api
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
    }
}
```

> `try_files` 是关键：Vue 用 history 模式，刷新子路由（如 `/system/user`）时 Nginx 要回退到 `index.html`，否则 404。

## 验证

- 打开 `http://localhost` → 登录页。
- 登录后 Network 里 `/api/*` 请求状态 200。
- 刷新 `/system/user` 不 404。

## 小结

前端部署 = 拷 `dist/` + Nginx 配 `root` + `try_files` + `/api` 反代。配好 `reload` 即可。

下一篇：[Windows 下部署 Spring Boot 后端](./07-Windows下部署SpringBoot后端.md)
