# 07 · Windows 下部署 Spring Boot 后端

## 1. 打包

```bash
cd backend
mvn clean package -DskipTests
# 产物：target/admin-1.0.0.jar
```

## 2. 检查数据库

后端**纯 MySQL**，启动前必须：
```sql
CREATE DATABASE ruoyi DEFAULT CHARSET utf8mb4;
USE ruoyi;
source schema-mysql.sql;
source data.sql;
```
并确认 `application.yml` 的 `spring.datasource` 的 `url/username/password` 与你的 MySQL 一致（默认 `root/root`、库 `ruoyi`）。

## 3. 运行 jar

```bat
java -jar D:\deploy\backend\admin.jar
```

- 默认端口 8080（写在 `application.yml` 或 `pom.xml` 的 `spring-boot.run.arguments`）。
- 前台运行会占住命令行；生产应输出日志并后台运行（见 09 篇服务化）。

## 4. 验证后端

```bash
curl http://localhost:8080/captcha        # 应该返回 JSON（验证码接口）
curl http://localhost:8080/login -X POST -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"123456\"}"
```

## 5. 指定端口/ profile（如需）

```bat
java -jar admin.jar --server.port=8080
```

> 本项目已无 `mysql` profile（Redis 已移除），默认就是 MySQL，无需加 `-Dspring.profiles.active`。

## 6. 日志

- Spring Boot 默认输出到控制台；生产建议配 `logging.file.name`（在 `application.yml`）：
  ```yaml
  logging:
    file:
      name: D:/deploy/logs/admin.log
  ```

## 小结

后端部署 = 打包 jar + 确保 MySQL 已建库导 SQL + `java -jar`。验证 `/login` 能返回 token。

下一篇：[Windows 下 Nginx 配置详解](./08-Windows下Nginx配置详解.md)
