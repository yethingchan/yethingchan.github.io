# Maven / Git / Docker 学习路线

> 对应 Java 全栈路线思维导图「前端与工程」第三叶。工程化三件套：构建 / 版本 / 容器。

## 一、学习阶段

### 入门：能构建与提交
- Maven：坐标、依赖、生命周期（clean/compile/package）、多模块
- Git：init/add/commit/push/pull、分支、.gitignore
- Docker：镜像/容器概念、docker run、dockerfile 基础

### 进阶：懂协作与封装
- Maven：依赖传递/冲突排除、profile、私服（Nexus）
- Git：分支模型（Git Flow）、rebase、merge 冲突解决、stash
- Docker：docker-compose、卷挂载、镜像优化（多阶段构建）

### 高级：能标准化交付
- Maven 私服与 release 流程
- Git 保护分支、Code Review 流程
- Docker 镜像安全扫描、CI 集成（见 K8s/CI-CD 路线）

## 二、关键要点与常见坑
- Maven 依赖冲突：mvn dependency:tree 查，exclusion 排
- Git 别在 main 上直接开发，开特性分支
- Docker 镜像别把源码/密钥打进去，用 .dockerignore
- 容器里程序用前台进程跑，别加 nohup 后台

## 三、实战
- 入门：Maven 多模块项目 + Git 提交到远程
- 进阶：把 SpringBoot 打成镜像并 docker-compose 起 MySQL+Redis

## 四、衔接
- 联动《Spring 与 SpringBoot》《K8s 与 CI-CD 与监控》
- Git 协作呼应《沟通力》（Code Review 沟通）

## 五、资源
- Maven/Git/Docker 官方文档；《Git 权威指南》

## 六、心法
1. 这三样是"工程素养"，不会就显业余。
2. 镜像越小越好，构建越快、攻击面越小。
3. 分支模型定清楚，团队协作少一半冲突。
