# 05 · Windows 环境准备

在 Windows 上部署需要准备三样：Node（构建前端用）、JDK（跑后端 jar）、MySQL（数据库）。Nginx 见 04 篇。

## 1. Node.js（仅构建前端需要）

- 下载 LTS：https://nodejs.org （本项目用 Node 22，装 LTS 即可）。
- 安装后验证：`node -v`、`npm -v`。
- 只需在**构建机器**上装（开发机即可），生产 Windows 服务器若只跑 jar 和 Nginx，可不装 Node（直接拷 `dist/`）。

## 2. JDK（跑后端 jar）

- 本项目 Spring Boot 3 需要 **JDK 17+**。
- 下载：Oracle JDK 或 Eclipse Temurin（https://adoptium.net）。
- 安装后配置 `JAVA_HOME` 并加入 `PATH`：
  ```
  系统属性 → 高级 → 环境变量
  JAVA_HOME = C:\Program Files\Java\jdk-17
  Path 追加 %JAVA_HOME%\bin
  ```
- 验证：`java -version`。

## 3. MySQL（数据库）

- 下载 MySQL 8.0 Installer：https://dev.mysql.com/downloads/installer/
- 安装时设 root 密码（本项目默认 `root/root`，若不同需改 `application.yml` 的 `spring.datasource`）。
- 启动 MySQL 服务（安装时勾选开机自启）。
- 建库 + 导 SQL（关键！）：
  ```sql
  CREATE DATABASE ruoyi DEFAULT CHARSET utf8mb4;
  USE ruoyi;
  source backend/src/main/resources/schema-mysql.sql;
  source backend/src/main/resources/data.sql;
  ```

## 4. 构建后端 jar

```bash
cd backend
mvn clean package -DskipTests     # 产物：target/admin-*.jar
```

> 需要 Maven；若没有，用项目自带的 `mvnw`（Maven Wrapper）。

## 目录规划（建议）

```
D:\deploy\
  ├─ frontend/dist/        # 前端构建产物
  ├─ backend/admin.jar     # 后端 jar
  ├─ nginx/                # Nginx 解压目录
  └─ logs/                 # 日志
```

## 小结

Windows 部署三件套：Node（构建）、JDK 17+（跑 jar）、MySQL（建库导 SQL）。后端打成 jar，前端打成 dist。

下一篇：[Windows 下部署前端（Nginx）](./06-Windows下部署前端Nginx.md)
