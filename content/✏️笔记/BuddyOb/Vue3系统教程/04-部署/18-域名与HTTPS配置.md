# 18 · 域名与 HTTPS 配置

生产必须用 HTTPS（加密 + 浏览器信任）。在 Nginx 上终结 SSL，后端仍走 HTTP 内网。

## 1. 域名解析

- 在域名服务商把 `A 记录` 指向服务器公网 IP。
- 等 DNS 生效（`ping your-domain.com` 通）。

## 2. 申请证书（Let's Encrypt 免费）

```bash
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d your-domain.com
```

certbot 会自动改 Nginx 配置加 SSL，并设自动续期。

## 3. 手动配 SSL（已有证书时）

```nginx
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$host$request_uri;     # HTTP 全跳 HTTPS
}

server {
    listen 443 ssl;
    server_name your-domain.com;

    ssl_certificate     /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;

    location / {
        root /opt/admin/frontend;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto https;
    }
}
```

> 注意反代里 `X-Forwarded-Proto https`，让后端知道原始协议是 https（生成绝对链接/重定向时用）。

## 4. 自动续期

```bash
sudo certbot renew --dry-run    # 测试
# 实际续期由 certbot 的 systemd timer 自动跑
```

## 验证

浏览器开 `https://your-domain.com` → 地址栏小锁；刷新子路由正常。

## 小结

HTTPS = 域名解析 + Let's Encrypt 证书 + Nginx 443 + HTTP 跳转。证书自动续期。后端不感知 SSL。

下一篇：[Docker 容器化部署](./19-Docker容器化部署.md)
