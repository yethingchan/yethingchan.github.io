# K8s / CI-CD / 监控 学习路线

> 对应 Java 全栈路线思维导图「架构与实战」第三叶。把服务稳稳送上生产并盯着它。

## 一、学习阶段

### 入门：能部署
- Linux 基础：常用命令、权限、网络排查
- Docker：镜像/容器/docker-compose（见对应路线）
- K8s 概念：Pod / Deployment / Service / Ingress

### 进阶：能编排
- K8s：yaml 编写、副本/滚动更新、ConfigMap/Secret
- CI/CD：Jenkins / GitHub Actions / GitLab CI 流水线
- 监控：Prometheus + Grafana 指标、告警

### 高级：能运维生产
- 日志：ELK（Elasticsearch+Logstash+Kibana）
- 链路追踪：SkyWalking
- 灰度/回滚/自愈（探针 liveness/readiness）
- 安全：镜像扫描、最小权限

## 二、关键要点与常见坑
- K8s 配置用 ConfigMap，别写死进镜像
- 探针配错会导致频繁重启或流量打进未就绪实例
- CI 流水线要能一键回滚，不能只前进
- 监控没告警 = 摆设，告警太多 = 狼来了

## 三、实战
- 入门：把 SpringBoot 打镜像，docker-compose 起一套
- 进阶：上 K8s 部署 + GitHub Actions 自动构建 + Prometheus 监控

## 四、衔接
- 前置《Maven 与 Git 与 Docker》《Spring 与 SpringBoot》
- 联动《高并发与高可用》（监控支撑调优）

## 五、资源
- Kubernetes 官方文档（kubernetes.io）；《凤凰架构》

## 六、心法
1. 部署不是终点，监控才是——看不见的系统会悄悄死。
2. 回滚能力比发布能力更重要。
3. 一切皆配置（IaC），环境一致才不出"我机器上能跑"。
