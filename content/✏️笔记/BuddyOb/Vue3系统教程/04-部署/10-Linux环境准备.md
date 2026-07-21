# 10 · Linux 环境准备

Linux 生产部署是主流。以 **Ubuntu 22.04 / CentOS 7+** 为例。需要：Node（构建）、JDK 17+、MySQL、Nginx。

## 连接服务器

```bash
ssh user@服务器IP
```

## 创建部署目录

```bash
sudo mkdir -p /opt/admin/{frontend,backend,logs}
sudo chown -R $USER:$USER /opt/admin
```

## 安装清单

| 组件 | 用途 | 安装方式 |
|------|------|----------|
| JDK 17 | 跑后端 jar | `apt install openjdk-17-jdk` |
| Node 22 | 构建前端（或不传 dist 直接拷） | nvm 或官方二进制 |
| MySQL 8 | 数据库 | `apt install mysql-server` |
| Nginx | 静态+反代 | `apt install nginx` |

## 用 nvm 装 Node（可选，仅构建时需要）

```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.0/install.sh | bash
source ~/.bashrc
nvm install 22
```

## 用户与权限建议

- 不建议用 root 跑服务。创建专用用户 `deploy`：
  ```bash
  sudo useradd -m deploy
  ```
- 后端 jar 用 `deploy` 用户经 systemd 运行（见 16 篇）。

## 上传文件

- 前端：`scp -r dist/* user@ip:/opt/admin/frontend/`
- 后端：`scp target/admin.jar user@ip:/opt/admin/backend/`
- 或用 Git + CI 自动拉取（见 22 篇）。

## 小结

Linux 准备：建 `/opt/admin` 目录、装 JDK/Node/MySQL/Nginx、建议专用部署用户。前端 dist 和后端 jar 传上去。

下一篇：[Linux 下安装 Node 构建前端](./11-Linux下安装Node构建前端.md)
