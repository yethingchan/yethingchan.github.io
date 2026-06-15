## 相关链接

- [[00-部署方案总览]]
- [[02-跨平台部署指南]]
- [[04-配置管理与环境隔离]]
- [[05-监控与故障排查]]
- [[架构总览]]
- [[通信协议总览]]

## Docker容器化部署

Docker容器化是IoTGateway在生产环境中推荐的部署方式之一。通过容器化，可以实现环境一致性、快速部署和便捷的版本管理。本章从Dockerfile源码分析入手，详细讲解容器化部署的完整流程。

## Dockerfile解析

IoTGateway项目根目录下的`Dockerfile`内容如下：

```dockerfile
FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS base
WORKDIR /app
# 将同级目录下的 app 文件夹拷贝到镜像内的 /app 目录
COPY app /app
# 暴露需要的端口
EXPOSE 518
EXPOSE 1888
EXPOSE 503

# 以 base 为基础构建最终镜像
FROM base AS final
# 设置时区为上海
ENV TZ=Asia/Shanghai
# 设置容器启动命令，启动 IoTGateway.dll
ENTRYPOINT ["dotnet", "IoTGateway.dll"]
```

### 逐行解析

| 指令 | 说明 |
|------|------|
| `FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS base` | 基础镜像，包含.NET 8.0运行时（约220MB） |
| `WORKDIR /app` | 设置工作目录为`/app` |
| `COPY app /app` | 将预编译的发布文件拷贝到镜像中 |
| `EXPOSE 518` | 声明HTTP端口 |
| `EXPOSE 1888` | 声明MQTT TCP端口 |
| `EXPOSE 503` | 声明预留端口 |
| `FROM base AS final` | 基于base阶段创建最终镜像 |
| `ENV TZ=Asia/Shanghai` | 设置容器时区为上海 |
| `ENTRYPOINT ["dotnet", "IoTGateway.dll"]` | 容器启动命令 |

### 镜像结构说明

这个Dockerfile采用了一种简化的构建方式——它不包含编译阶段（非多阶段构建），而是假设`app/`目录中已经存在预编译好的发布文件。构建流程为：

```
开发机器                              Docker镜像
┌─────────────┐    dotnet publish    ┌──────────┐
│ 源代码       │ ──────────────────→ │ app/     │
│ .csproj     │                      │ 发布文件  │
└─────────────┘                      └────┬─────┘
                                          │ docker build
                                          ▼
                                     ┌──────────┐
                                     │ 最终镜像  │
                                     │ aspnet:8 │
                                     │ + 应用   │
                                     └──────────┘
```

## 准备发布文件

在构建Docker镜像之前，需要先发布应用程序：

```bash
# 1. 发布为Linux x64版本
dotnet publish IoTGateway/IoTGateway.csproj \
  -c Release \
  -r linux-x64 \
  --self-contained false \
  -o ./app

# 2. 或者发布为自包含版本（目标机器无需.NET运行时）
dotnet publish IoTGateway/IoTGateway.csproj \
  -c Release \
  -r linux-x64 \
  --self-contained true \
  -p:PublishSingleFile=true \
  -o ./app
```

**两种发布方式对比：**

| 特性 | 依赖框架（推荐） | 自包含 |
|------|-----------------|--------|
| 基础镜像 | `aspnet:8.0` | `aspnet:8.0`或更小的`runtime-deps` |
| 镜像大小 | ~300MB | ~450MB |
| 启动速度 | 较快 | 稍慢 |
| 运行时更新 | 跟随基础镜像更新 | 需要重新发布 |

## 构建Docker镜像

```bash
# 在项目根目录执行（Dockerfile所在目录）
docker build -t iotgateway:latest .

# 指定版本号
docker build -t iotgateway:1.0.0 .

# 查看构建结果
docker images | grep iotgateway
```

## 运行容器

### 基础运行

```bash
docker run -d \
  --name iotgateway \
  -p 518:518 \
  -p 1888:1888 \
  iotgateway:latest
```

### 生产环境运行（推荐）

```bash
docker run -d \
  --name iotgateway \
  --restart always \
  -p 518:518 \
  -p 1888:1888 \
  -v /opt/iotgateway/data:/app/data \
  -v /opt/iotgateway/logs:/app/logs \
  -v /opt/iotgateway/files:/app/files \
  -v /opt/iotgateway/Plugins:/app/Plugins \
  -e TZ=Asia/Shanghai \
  iotgateway:latest
```

**参数说明：**

| 参数 | 说明 |
|------|------|
| `-d` | 后台运行 |
| `--name` | 容器名称 |
| `--restart always` | 容器退出后自动重启 |
| `-p 518:518` | 端口映射（HTTP） |
| `-p 1888:1888` | 端口映射（MQTT） |
| `-v 宿主机路径:容器路径` | 卷挂载 |
| `-e TZ=...` | 环境变量 |

## 数据持久化

容器化部署中，数据持久化是关键问题。以下是需要挂载的卷：

```bash
# 创建持久化目录
mkdir -p /opt/iotgateway/{data,logs,files,Plugins}

# 如果已有数据需要迁移，将SQLite数据库拷贝到data目录
cp iotgateway.db /opt/iotgateway/data/
```

**卷挂载映射表：**

| 容器路径 | 宿主机路径 | 内容 | 说明 |
|---------|-----------|------|------|
| `/app/data` | `/opt/iotgateway/data` | SQLite数据库 | 核心业务数据 |
| `/app/logs` | `/opt/iotgateway/logs` | 日志文件 | 运维排障 |
| `/app/files` | `/opt/iotgateway/files` | 上传文件 | 导入导出文件 |
| `/app/Plugins` | `/opt/iotgateway/Plugins` | 驱动插件 | 自定义驱动 |
| `/app/appsettings.json` | `/opt/iotgateway/appsettings.json` | 配置文件 | 可选，外部化配置 |

## Docker Compose部署

对于需要同时部署多个服务（如IoTGateway + MQTT Broker + 数据库）的场景，推荐使用docker-compose：

```yaml
# docker-compose.yml
version: '3.8'

services:
  iotgateway:
    image: iotgateway:latest
    container_name: iotgateway
    restart: always
    ports:
      - "518:518"       # HTTP
      - "1888:1888"     # MQTT
    volumes:
      - ./data:/app/data
      - ./logs:/app/logs
      - ./files:/app/files
      - ./Plugins:/app/Plugins
      - ./appsettings.json:/app/appsettings.json:ro
    environment:
      - TZ=Asia/Shanghai
    networks:
      - iot-network

  # 可选：外部MQTT Broker（如果需要与IoTGateway分开部署）
  # mqtt-broker:
  #   image: eclipse-mosquitto:2
  #   ports:
  #     - "1883:1883"
  #   volumes:
  #     - ./mosquitto/config:/mosquitto/config
  #     - ./mosquitto/data:/mosquitto/data

networks:
  iot-network:
    driver: bridge
```

### 启动和管理

```bash
# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f iotgateway

# 停止服务
docker-compose down

# 更新镜像并重启
docker-compose pull
docker-compose up -d
```

## 多实例部署

通过docker-compose可以部署多个IoTGateway实例（例如不同工厂使用不同的网关）：

```yaml
version: '3.8'

services:
  gateway-factory-a:
    image: iotgateway:latest
    container_name: gateway-factory-a
    restart: always
    ports:
      - "5181:518"
      - "18881:1888"
    volumes:
      - ./factory-a/data:/app/data
      - ./factory-a/logs:/app/logs

  gateway-factory-b:
    image: iotgateway:latest
    container_name: gateway-factory-b
    restart: always
    ports:
      - "5182:518"
      - "18882:1888"
    volumes:
      - ./factory-b/data:/app/data
      - ./factory-b/logs:/app/logs
```

## 容器管理常用命令

```bash
# 查看运行中的容器
docker ps

# 查看容器日志（最近100行，持续跟踪）
docker logs --tail 100 -f iotgateway

# 进入容器内部调试
docker exec -it iotgateway /bin/bash

# 查看容器资源占用
docker stats iotgateway

# 重启容器
docker restart iotgateway

# 停止并删除容器（数据卷不受影响）
docker stop iotgateway && docker rm iotgateway

# 查看容器详细信息
docker inspect iotgateway
```

## 镜像优化建议

### 使用.slim镜像

```dockerfile
# 使用slim版本可以减小约50MB体积
FROM mcr.microsoft.com/dotnet/aspnet:8.0-jammy-slim AS base
```

### 多阶段构建（完整编译型Dockerfile）

如果需要从源码直接在Docker中编译：

```dockerfile
# 构建阶段
FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src
COPY . .
RUN dotnet restore
RUN dotnet publish IoTGateway/IoTGateway.csproj -c Release -o /app/publish

# 运行阶段
FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS final
WORKDIR /app
COPY --from=build /app/publish .
ENV TZ=Asia/Shanghai
EXPOSE 518 1888
ENTRYPOINT ["dotnet", "IoTGateway.dll"]
```

### 使用.dockerignore

创建`.dockerignore`文件减小构建上下文：

```
**/bin/
**/obj/
**/.vs/
**/node_modules/
*.user
*.suo
.git/
doc/
images/
```

## 安全加固

### 非root用户运行

```dockerfile
FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS final
WORKDIR /app
COPY app /app

# 创建非root用户
RUN adduser --disabled-password --gecos '' appuser
USER appuser

ENV TZ=Asia/Shanghai
EXPOSE 518 1888
ENTRYPOINT ["dotnet", "IoTGateway.dll"]
```

### 只读文件系统

```bash
docker run -d \
  --name iotgateway \
  --read-only \
  --tmpfs /tmp \
  -v /opt/iotgateway/data:/app/data \
  -v /opt/iotgateway/logs:/app/logs \
  -p 518:518 -p 1888:1888 \
  iotgateway:latest
```

## 备份与恢复

```bash
# 备份数据库
docker exec iotgateway cp /app/data/iotgateway.db /app/data/iotgateway.db.bak
docker cp iotgateway:/app/data/iotgateway.db.bak ./backup/

# 恢复数据库
docker cp ./backup/iotgateway.db iotgateway:/app/data/iotgateway.db
docker restart iotgateway

# 定期备份脚本（加入crontab）
#!/bin/bash
BACKUP_DIR="/opt/iotgateway/backup"
DATE=$(date +%Y%m%d_%H%M%S)
docker exec iotgateway cp /app/data/iotgateway.db /app/data/iotgateway.db.bak
docker cp iotgateway:/app/data/iotgateway.db.bak ${BACKUP_DIR}/iotgateway_${DATE}.db
# 保留最近30天的备份
find ${BACKUP_DIR} -name "iotgateway_*.db" -mtime +30 -delete
```

## 小结

Docker容器化部署为IoTGateway提供了环境一致性和快速部署的能力。通过合理的卷挂载策略保证数据持久化，通过docker-compose简化多服务编排，通过安全加固满足工业环境的安全要求。对于需要在多条产线或多个工厂部署IoT网关的场景，容器化方案显著降低了运维复杂度。

---

上一篇: [[00-部署方案总览]] | 下一篇: [[02-跨平台部署指南]]
