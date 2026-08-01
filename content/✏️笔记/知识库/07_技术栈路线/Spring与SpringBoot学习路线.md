# Spring 与 SpringBoot 学习路线

> 对应 Java 全栈路线思维导图「数据与框架」第二叶。Java 后端事实标准，必须吃透。

## 一、学习阶段

### 入门：能跑一个 Boot 项目
- Spring 基础：IoC / DI、容器、Bean
- Spring MVC：请求映射、参数绑定、统一返回、全局异常
- Spring Boot：起步依赖、配置文件、跑通 CRUD

### 进阶：懂原理
- Spring：Bean 生命周期、作用域、循环依赖解决、AOP（动态代理 JDK/CGLIB）
- Boot 自动配置：@EnableAutoConfiguration、spring.factories / SPI
- 事务：@Transactional 全属性、传播 7 种、隔离 4 种（深度见下）
- 常用 starter：web / data-jpa / validation / actuator

### 高级：能定制与排障
- 自定义 starter、条件注解 @Conditional
- 启动流程源码、Bean 加载顺序
- 事务失效 10 大场景（自调用、非 public、异常被吞…）
- 整合 Redis/ES/MQ 等

## 二、关键要点与常见坑
- 循环依赖：构造器注入无解，字段/setter 可（三级缓存）
- 事务自调用失效：代理对象内部调用不走代理
- @Transactional 默认只回滚 RuntimeException
- 配置优先级：命令行 > 配置文件 > 默认值

## 三、实战
- 入门：Boot + MyBatis 写用户管理（增删改查+分页）
- 进阶：Boot + Redis + 事务，做带缓存的订单服务

## 四、衔接
- 深度权威参考《SpringBoot 事务知识库》（01_技术底座）
- 联动《MyBatisPlus 学习路线》《MySQL 与 Redis 学习路线》

## 五、资源
- 《Spring 实战》《Spring Boot 实战》；《SpringBoot 事务知识库》

## 六、心法
1. Spring 不是"会用注解"，是"懂它为什么这么设计"。
2. 自动配置是约定优于配置，读懂 starter 才不被黑盒困住。
3. 事务是后端生命线，失效场景逐条背熟。
