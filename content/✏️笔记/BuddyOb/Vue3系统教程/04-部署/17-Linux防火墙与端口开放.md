# 17 · Linux 防火墙与端口开放

只暴露 80/443 给用户，后端 8080 和 MySQL 3306 藏内网。

## firewalld（CentOS/RHEL）

```bash
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --permanent --add-port=8080/tcp   # 仅调试临时开放，生产建议关
sudo firewall-cmd --reload
sudo firewall-cmd --list-all
```

## ufw（Ubuntu/Debian）

```bash
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
sudo ufw status
```

## 安全原则

| 端口 | 是否对外 | 说明 |
|------|----------|------|
| 80/443 | ✅ 开 | 用户访问 Nginx |
| 8080 | ❌ 关 | 仅 Nginx 本机反代访问（127.0.0.1） |
| 3306 | ❌ 关 | 仅本机/内网，绝不暴露公网 |

> 即使 8080 开着，因为 Nginx 反代用的是 `127.0.0.1:8080`（本机回环），外部也访问不到——但保险起见还是把 8080 从防火墙关掉。

## 云服务器额外注意

- 阿里云/腾讯云等还有**安全组**，要在控制台也放开 80/443。
- 安全组 + 系统防火墙**两层都要配**。

## 验证端口监听

```bash
sudo ss -tlnp | grep -E ':80|:443|:8080'
```

## 小结

防火墙只放 80/443；8080/3306 仅内网。云服务器还要配安全组。两层都要开。

下一篇：[域名与 HTTPS 配置](./18-域名与HTTPS配置.md)
