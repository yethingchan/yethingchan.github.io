# 13 · Linux 下安装 MySQL

后端纯 MySQL，必须先装库、建库、导 SQL。

## 安装（Ubuntu/Debian）

```bash
sudo apt update
sudo apt install mysql-server -y
sudo systemctl enable --now mysql
```

CentOS：
```bash
sudo yum install mysql-server -y
sudo systemctl enable --now mysqld
```

## 安全初始化（Ubuntu 首次）

```bash
sudo mysql_secure_installation
# 按提示设 root 密码、删匿名用户、禁远程 root
```

> 本项目 `application.yml` 默认 `root/root`。若你设了别的密码，记得改 `application.yml` 的 `spring.datasource.password`。

## 建库 + 导 SQL（关键）

```bash
mysql -u root -p
```

```sql
CREATE DATABASE ruoyi DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE ruoyi;
source /opt/admin/backend/schema-mysql.sql;
source /opt/admin/backend/data.sql;
```

> 文件路径按你上传 jar 时是否带上 `src/main/resources` 来定。若 `schema-mysql.sql` 不在服务器，先从开发机传上去：
> ```bash
> scp backend/src/main/resources/schema-mysql.sql user@ip:/opt/admin/backend/
> ```

## 允许后端连接

- 后端和 MySQL 同机：用 `localhost`/`127.0.0.1` 即可。
- 不同机：MySQL 需授权远程用户，并开放 3306（见 17 篇防火墙），但**生产不建议把 3306 暴露公网**。

```sql
CREATE USER 'ruoyi'@'%' IDENTIFIED BY '强密码';
GRANT ALL ON ruoyi.* TO 'ruoyi'@'%';
FLUSH PRIVILEGES;
```

## 验证

```bash
mysql -u root -p -e "USE ruoyi; SHOW TABLES;"
# 应看到 10 张表
```

## 小结

Linux MySQL：装 → 安全初始化 → 建 `ruoyi` 库 → `source` 两个 SQL → 调 `application.yml` 账号。库建好后端才能起。

下一篇：[Linux 下 Nginx 部署前端](./14-Linux下Nginx部署前端.md)
