# 12 · Linux 下安装 JDK 运行 jar

后端是 Spring Boot 可执行 jar，需 **JDK 17+**。

## 安装（Ubuntu/Debian）

```bash
sudo apt update
sudo apt install openjdk-17-jdk -y
java -version     # 验证：显示 17.x
```

CentOS/RHEL：
```bash
sudo yum install java-17-openjdk-devel -y
```

## 运行 jar

```bash
cd /opt/admin/backend
java -jar admin.jar
```

- 前台运行（调试用）。生产请用 systemd（见 16 篇）后台托管。
- 端口默认 8080（来自 `application.yml`）。
- 指定内存（大项目）：
  ```bash
  java -Xms512m -Xmx1024m -jar admin.jar
  ```

## 验证后端起来了

```bash
curl http://localhost:8080/captcha
# 或看日志
journalctl -u admin-backend -f    # 若用 systemd
```

## 常见问题

- `java: command not found`：JDK 没装或 PATH 没配。
- 端口被占：`lsof -i:8080` 看谁占了，或换端口 `--server.port=8081`。
- 连不上 MySQL：检查 `application.yml` 的 `url/username/password` 和 MySQL 是否在跑。

## 小结

Linux 跑后端：`apt install openjdk-17-jdk` → `java -jar admin.jar`。生产用 systemd 托管（下篇）。

下一篇：[Linux 下安装 MySQL](./13-Linux下安装MySQL.md)
