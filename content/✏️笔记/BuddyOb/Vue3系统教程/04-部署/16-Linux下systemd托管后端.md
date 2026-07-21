# 16 · Linux 下 systemd 托管后端

生产用 **systemd** 把 `java -jar` 变成系统服务：开机自启、崩溃自启、统一看日志。

## 建服务文件

```bash
sudo vim /etc/systemd/system/admin-backend.service
```

```ini
[Unit]
Description=Admin Backend (Spring Boot)
After=network.target mysql.service

[Service]
User=deploy
WorkingDirectory=/opt/admin/backend
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -jar /opt/admin/backend/admin.jar
SuccessExitStatus=143
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

- `After=mysql.service`：确保 MySQL 先起。
- `Restart=always`：崩溃 5 秒后自动重启。
- `User=deploy`：用专用用户跑，不用 root（先 `useradd -m deploy`，并把 jar 目录属主给它）。

## 启用与启停

```bash
sudo systemctl daemon-reload
sudo systemctl enable admin-backend      # 开机自启
sudo systemctl start admin-backend
sudo systemctl status admin-backend      # 看状态
```

## 看日志

```bash
sudo journalctl -u admin-backend -f       # 实时日志
sudo journalctl -u admin-backend --since "10 min ago"
```

## 改配置后

```bash
sudo systemctl restart admin-backend
```

## 验证

```bash
curl http://localhost:8080/captcha
```

## 小结

systemd 服务文件放 `/etc/systemd/system/admin-backend.service`，`enable` 开机自启、`restart` 生效、`journalctl` 看日志。崩溃自动拉起。

下一篇：[Linux 防火墙与端口开放](./17-Linux防火墙与端口开放.md)
