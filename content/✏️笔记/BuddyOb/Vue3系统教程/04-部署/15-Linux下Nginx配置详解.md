# 15 · Linux 下 Nginx 配置详解

与 Windows 配置语法一致，仅路径用 Linux 风格。这里补充 Linux 特有细节。

## 配置结构（Linux）

```
/etc/nginx/
├── nginx.conf          # 主配置（含 http 块、引入 conf.d）
└── conf.d/
    └── admin.conf      # 我们的站点（推荐单站点一个文件）
```

主配置一般已包含 `include /etc/nginx/conf.d/*.conf;`，所以只需在 `conf.d/` 放站点文件。

## 完整站点模板

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端
    location / {
        root /opt/admin/frontend;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 后端反代
    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 可选：静态资源缓存
    location ~* \.(js|css|png|jpg|svg|woff2)$ {
        root /opt/admin/frontend;
        expires 7d;
        add_header Cache-Control "public";
    }
}
```

## 日志

默认 `/var/log/nginx/access.log` 和 `error.log`。排查 404/502 看 error.log：

```bash
sudo tail -f /var/log/nginx/error.log
```

## 多站点

每个站点一个 `conf.d/xxx.conf`，用不同 `server_name` 或 `listen` 端口区分。

## 重载 vs 重启

```bash
sudo nginx -t && sudo systemctl reload nginx   # 改配置用 reload（不中断）
sudo systemctl restart nginx                   # 升级 Nginx 才用 restart
```

## 小结

Linux Nginx 配法与 Windows 同，站点放 `conf.d/`。配完 `nginx -t` + `reload`。日志在 `/var/log/nginx/`。

下一篇：[Linux 下 systemd 托管后端](./16-Linux下systemd托管后端.md)
