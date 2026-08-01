# MQ / RPC / Nacos 学习路线

> 对应 Java 全栈路线思维导图「前端与工程」第四叶。分布式通信三件套：异步 / 调用 / 注册。

## 一、学习阶段

### 入门：能通消息与调用
- 消息队列：Kafka / RabbitMQ 基础概念、生产消费
- RPC：Dubbo / gRPC 入门（接口定义、调用）
- 注册中心：Nacos 注册发现 + 配置管理

### 进阶：懂机制
- Kafka：分区、消费者组、offset、Exactly-Once、重复/丢失
- RabbitMQ：交换机/队列/路由、死信队列
- Dubbo：SPI、负载均衡、容错
- gRPC：Protobuf、流式

### 高级：能设计通信架构
- 消息幂等、重试、补偿
- 服务注册与健康检查、灰度
- MQ 削峰填谷、异步解耦场景选型
- 与 SpringCloud 集成（OpenFeign/Dubbo）

## 二、关键要点与常见坑
- 消息重复：消费端做幂等（唯一键/去重表）
- Kafka 分区数决定并行度，别随意改
- RPC 超时/重试要配，否则雪崩
- Nacos 配置改了要能回滚、有环境隔离

## 三、实战
- 入门：RabbitMQ 做异步发邮件；Dubbo 两个服务互调
- 进阶：Kafka 做订单事件总线，下游库存/积分消费

## 四、衔接
- 前置《Spring 与 SpringBoot》《SpringCloud》
- 联动《微服务与分布式》《高并发与高可用》

## 五、资源
- Kafka/RabbitMQ/Dubbo/Nacos 官方文档；《RabbitMQ 实战》

## 六、心法
1. MQ 解耦削峰，RPC 同步调用，选型看场景。
2. 分布式通信第一要务是"消息不丢、不重、有序可证"。
3. 注册中心是微服务的通讯录，挂了全公司失联。
