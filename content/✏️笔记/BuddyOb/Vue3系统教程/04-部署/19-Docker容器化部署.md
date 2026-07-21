# 19 · Docker 容器化部署

把前端和后端打包成镜像，用 Docker 跑，环境一致、迁移方便。（本项目尚未内置 Dockerfile，本节给出可直接落地的模板。）

## 前端 Dockerfile（frontend/Dockerfile）

```dockerfile
# 构建阶段
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# 运行阶段：nginx 托管 dist
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

配套 `frontend/nginx.conf`（容器内）：

```nginx
server {
    listen 80;
    location / { root /usr/share/nginx/html; try_files $uri $uri/ /index.html; }
    location /api/ { proxy_pass http://backend:8080/; proxy_set_header Host $host; }
}
```

> 容器内反代用服务名 `backend`（docker-compose 里定义），不是 127.0.0.1。

## 后端 Dockerfile（backend/Dockerfile）

```dockerfile
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/admin.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## docker-compose.yml（编排）

```yaml
version: '3.8'
services:
  backend:
    build: ./backend
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/ruoyi
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=root
    depends_on:
      - mysql
    networks: [app-net]

  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on: [backend]
    networks: [app-net]

  mysql:
    image: mysql:8
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: ruoyi
    volumes:
      - mysql-data:/var/lib/mysql
    networks: [app-net]

networks:
  app-net:
volumes:
  mysql-data:
```

## 运行

```bash
docker compose up -d --build
```

## 注意

- 首次需把 `schema-mysql.sql`/`data.sql` 导入 mysql 容器（挂载 init 脚本或手动 `docker exec` 导入）。
- 生产可用 `mysql` 服务 `volumes` 持久化数据。
- 镜像里后端连 `mysql:3306`（compose 服务名解析）。

## 小结

Docker = 前端（node 构建→nginx 托管）+ 后端（jdk 跑 jar）+ mysql，用 compose 编排。环境一致、一键起。

下一篇：[反向代理与负载均衡](./20-反向代理与负载均衡.md)
