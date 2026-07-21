# 04 · 部署（Windows → Linux）

本分册把系统真正跑上服务器。覆盖：本地 Windows 部署、Linux 生产部署、Nginx、JDK/MySQL、systemd、Docker、HTTPS、CI/CD。

> 前置事实：
> - 前端构建 `npm run build` → `dist/`（静态文件）。
> - 后端 `mvn package` → `target/*.jar`（Spring Boot 可执行 jar，内嵌 Tomcat，端口 8080）。
> - 后端**纯 MySQL**，启动前需建库并 `source schema-mysql.sql + data.sql`。
> - 前端**没有 `.env`**，`/api` 前缀靠 **Nginx 反代**到后端。
> - **本项目尚未内置** Dockerfile / nginx.conf / 部署脚本——本分册给出可直接落到工程的模板。

## 本分册目录

1. [部署架构总览](./01-部署架构总览.md)
2. [前端构建 npm run build](./02-前端构建npm-run-build.md)
3. [产物 dist 目录说明](./03-产物dist目录说明.md)
4. [Nginx 基础与安装](./04-Nginx基础与安装.md)
5. [Windows 环境准备](./05-Windows环境准备.md)
6. [Windows 下部署前端（Nginx）](./06-Windows下部署前端Nginx.md)
7. [Windows 下部署 Spring Boot 后端](./07-Windows下部署SpringBoot后端.md)
8. [Windows 下 Nginx 配置详解](./08-Windows下Nginx配置详解.md)
9. [Windows 服务化部署](./09-Windows服务化部署.md)
10. [Linux 环境准备](./10-Linux环境准备.md)
11. [Linux 下安装 Node 构建前端](./11-Linux下安装Node构建前端.md)
12. [Linux 下安装 JDK 运行 jar](./12-Linux下安装JDK运行jar.md)
13. [Linux 下安装 MySQL](./13-Linux下安装MySQL.md)
14. [Linux 下 Nginx 部署前端](./14-Linux下Nginx部署前端.md)
15. [Linux 下 Nginx 配置详解](./15-Linux下Nginx配置详解.md)
16. [Linux 下 systemd 托管后端](./16-Linux下systemd托管后端.md)
17. [Linux 防火墙与端口开放](./17-Linux防火墙与端口开放.md)
18. [域名与 HTTPS 配置](./18-域名与HTTPS配置.md)
19. [Docker 容器化部署](./19-Docker容器化部署.md)
20. [反向代理与负载均衡](./20-反向代理与负载均衡.md)
21. [部署排错与日志查看](./21-部署排错与日志查看.md)
22. [持续集成与自动化部署](./22-持续集成与自动化部署.md)

## 部署总览一句话

```
前端 dist/  →  Nginx 静态托管 + /api 反代
后端 jar    →  java -jar 或 systemd / Docker
MySQL       →  独立服务，后端连它
```

下一篇：[部署架构总览](./01-部署架构总览.md)
