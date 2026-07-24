---
title: 代码生成器 AutoGenerator
---

# 13 代码生成器（FastAutoGenerator）

> 上接：[[MyBatisPlus/12-逻辑删除与乐观锁]]
> 一张表 → Entity / Mapper / Service / Controller 一堆文件，手写累死。**MP 官方生成器**连库读表结构，一键吐全套。本 Spring 教程 06 章的"代码生成器"是手写读 `information_schema`（讲原理），这里给**官方标准做法**（生产用）。

## 13.1 依赖

```xml
<dependency>
  <groupId>com.baomidou</groupId>
  <artifactId>mybatis-plus-generator</artifactId>
  <version>3.5.7</version>
</dependency>
<dependency>
  <groupId>org.freemarker</groupId>
  <artifactId>freemarker</artifactId>   <!-- 模板引擎（Velocity 已弃用，用 FreeMarker） -->
</dependency>
```

## 13.2 一键生成（3.5.x 的 FastAutoGenerator）

```java
public class CodeGenerator {
    public static void main(String[] args) {
        FastAutoGenerator.create(
                "jdbc:mysql://localhost:3306/ruoyi?useSSL=false&serverTimezone=Asia/Shanghai",
                "root", "123456")
            // ① 全局
            .globalConfig(b -> b
                .author("james")
                .outputDir(System.getProperty("user.dir") + "/src/main/java")
                .commentDate("yyyy-MM-dd")
                .enableSwagger()          // 加 Swagger 注解
                .fileOverride())           // 覆盖已存在文件
            // ② 包结构
            .packageConfig(p -> p
                .parent("com.example.admin")
                .entity("modules.wms.domain")
                .mapper("modules.wms.mapper")
                .service("modules.wms.service")
                .serviceImpl("modules.wms.service.impl")
                .controller("modules.wms.controller")
                .xml("mapper"))            // Mapper XML 输出位置
            // ③ 策略
            .strategyConfig(s -> s
                .addInclude("wms_material", "wms_location", "wms_inventory") // 要生成的表
                .entityBuilder()
                    .enableLombok()                 // @Data
                    .enableTableFieldAnnotation()     // 加 @TableField
                    .logicDeleteColumnName("del_flag")// 自动识别逻辑删除字段
                    .versionColumnName("version")     // 乐观锁
                    .naming(NamingStrategy.underline_to_camel)
                .controllerBuilder()
                    .enableRestStyle()              // @RestController + 映射
                .mapperBuilder()
                    .enableMapperAnnotation())      // @Mapper
            .execute();   // 执行！
    }
}
```

## 13.3 生成出来有什么

对每张表产出：
```
com.example.admin.modules.wms
  ├─ domain/WmsMaterial.java     (@TableName/@TableId/@TableField/@TableLogic)
  ├─ mapper/WmsMaterialMapper.java (extends BaseMapper<WmsMaterial>)
  ├─ service/IWmsMaterialService.java (extends IService<...>)
  ├─ service/impl/WmsMaterialServiceImpl.java
  ├─ controller/WmsMaterialController.java (基础 CRUD 接口)
  └─ resources/mapper/WmsMaterialMapper.xml (空骨架)
```
> 生成后**直接能跑 CRUD**（MP 接口已就绪）。你再按业务往 Controller/Service 补逻辑即可——这正是 [[../SpringBoot+Vue3后台搭建/06-系统工具/01-代码生成器|06-系统工具·代码生成器]] 那章"代码生成"的最终形态。

## 13.4 模板可定制

```java
.strategyConfig(s -> s
    .templateBuilder()
        .entity("/templates/my-entity.java.ftl")  // 用你自己的 .ftl 模板
    .controller("/templates/my-controller.java.ftl"))
```
> 想生成"带 `@PreAuthorize` 权限串 + Swagger 注释 + 统一返回 `AjaxResult`"的 Controller？写个 `.ftl` 模板放 `resources/templates/`，生成器按它吐——这是企业"统一代码风格"的杀手锏。

## 验证清单

- [ ] 跑 `main`，`wms_*` 三张表的全套 Entity/Mapper/Service/Controller 生成到对应包。
- [ ] 生成的 Entity 带 `@TableLogic`/`@Version`（按策略自动识别）。
- [ ] 生成的 Controller 是 `@RestController` 且基础 CRUD 接口齐全。
- [ ] 自定义 `.ftl` 模板后，生成的代码带上了项目约定的注解/返回体。

> 下一步：[[MyBatisPlus/14-多租户与JSON类型处理器]] 讲 SaaS 多租户隔离和 JSON 字段映射。
