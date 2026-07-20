---
title: 后端 Spring Boot 初始化
---

# 01-1 后端 Spring Boot 初始化

> 上接：[[SpringBoot+Vue3后台搭建/01-工程初始化/00-索引与项目结构]]

## 1.1 pom.xml（核心依赖）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version>      <!-- 父依赖：统一版本，不用每个都写 version -->
    <relativePath/>
  </parent>
  <groupId>com.example</groupId>
  <artifactId>admin</artifactId>
  <version>1.0.0</version>
  <properties>
    <java.version>17</java.version>
  </properties>

  <dependencies>
    <!-- Web：内嵌 Tomcat + Spring MVC -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 安全：Spring Security 6 -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- JWT：jjwt 0.12 新 API -->
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-api</artifactId>
      <version>0.12.5</version>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-impl</artifactId>
      <version>0.12.5</version>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-jackson</artifactId>
      <version>0.12.5</version>
      <scope>runtime</scope>
    </dependency>

    <!-- MyBatis-Plus：在 mybatis 之上增强 -->
    <dependency>
      <groupId>com.baomidou</groupId>
      <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
      <version>3.5.7</version>
    </dependency>

    <!-- MySQL 驱动 -->
    <dependency>
      <groupId>com.mysql</groupId>
      <artifactId>mysql-connector-j</artifactId>
      <scope>runtime</scope>
    </dependency>

    <!-- Druid 连接池（Spring Boot 3 专用 starter） -->
    <dependency>
      <groupId>com.alibaba</groupId>
      <artifactId>druid-spring-boot-3-starter</artifactId>
      <version>1.2.23</version>
    </dependency>

    <!-- Redis -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- 参数校验 -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Swagger 文档（springdoc，兼容 Spring Boot 3） -->
    <dependency>
      <groupId>org.springdoc</groupId>
      <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
      <version>2.5.0</version>
    </dependency>

    <!-- Quartz 定时任务 -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-quartz</artifactId>
    </dependency>

    <!-- Excel -->
    <dependency>
      <groupId>com.alibaba</groupId>
      <artifactId>easyexcel</artifactId>
      <version>3.3.4</version>
    </dependency>

    <!-- 工具集 -->
    <dependency>
      <groupId>cn.hutool</groupId>
      <artifactId>hutool-all</artifactId>
      <version>5.8.27</version>
    </dependency>
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <optional>true</optional>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
          <excludes><exclude><groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId></exclude></excludes>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

**逐段解释**
- `spring-boot-starter-parent`：父 POM，**统一管理所有 starter 版本**，所以下面多数依赖不写 `<version>`。
- `mybatis-plus-spring-boot3-starter`：注意是 **`-spring-boot3-`** 变体，配 Spring Boot 3。用错 starter 会 NoClassDefFound。
- `druid-spring-boot-3-starter`：同样要 3 专用版。
- `jjwt` 三个包：`api`（编译用）+ `impl`+`jackson`（运行期 JSON 序列化，必须 runtime 引入，否则运行时报 `Unable to find an implementation`）。
- `easyexcel`/`hutool`/`quartz`/`springdoc`：后续工具/监控/文档章节用。

## 1.2 启动类 AdminApplication.java

```java
package com.example.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.example.admin.modules.**.mapper") // 扫描所有 Mapper 接口
@SpringBootApplication
public class AdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
```

**解释**
- `@MapperScan`：MyBatis-Plus 的 Mapper 接口要被扫描成 Spring Bean。这里用 `**` 通配 `modules` 下所有子包的 `mapper`。
- `@SpringBootApplication`：开启自动装配 + 组件扫描（默认扫 `com.example.admin` 及其子包）。

## 1.3 一个最小可跑接口（先验证工程）

```java
package com.example.admin.common.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @GetMapping("/hello")
    public String hello() {
        return "hello admin";
    }
}
```

启动后访问 `http://localhost:8080/hello` 应返回 `hello admin`。

## 1.4 application.yml（主配置骨架）

```yaml
server:
  port: 8080

spring:
  application:
    name: ruoyi-admin
  datasource:
    druid:                       # Druid 专属前缀
      url: jdbc:mysql://localhost:3306/admin?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
      username: root
      password: root
      driver-class-name: com.mysql.cj.jdbc.Driver
  data-source:
    type: com.alibaba.druid.pool.DruidDataSource
  redis:
    host: localhost
    port: 6379
    database: 0
    password: ""
    lettuce:
      pool:
        max-active: 16
        max-idle: 8
        min-idle: 1

# MyBatis-Plus 配置（详细在 [[../03-后端基础框架/02-MyBatisPlus与Redis配置]]）
mybatis-plus:
  mapper-locations: classpath*:mapper/**/*Mapper.xml
  type-aliases-package: com.example.admin.modules.**.domain
  global-config:
    db-config:
      id-type: auto               # 主键自增
  configuration:
    map-underscore-to-camel-case: true   # 下划线转驼峰
```

> 这里只放骨架。完整的安全/MP/Redis/Swagger 配置在 [[../03-后端基础框架]] 各章展开。

## 1.5 验证清单
- [ ] `mvn spring-boot:run` 或 IDE 启动，控制台出现 Tomcat started。
- [ ] 浏览器打开 `http://localhost:8080/hello` 返回字符串。

> 下一步：[[../01-工程初始化/02-前端Vue3初始化]] 把前端空壳也起起来。
