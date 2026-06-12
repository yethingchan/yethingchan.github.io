# 07 - 接口文档与API管理

## 一、概述

若依框架集成了Springdoc（OpenAPI 3）替代了旧版的Swagger 2，用于自动生成API接口文档。通过配置Springdoc，系统自动扫描Controller层的接口定义，生成标准化的API文档，支持在线调试、分组展示、JWT认证等功能。

---

## 二、查看与剖析点

### 2.1 核心文件清单

| 文件路径 | 作用 |
|---------|------|
| `ruoyi-admin/.../web/core/config/SwaggerConfig.java` | Springdoc/OpenAPI配置 |
| `ruoyi-admin/src/main/resources/application.yml` | Springdoc配置项 |
| `ruoyi-framework/.../config/ResourcesConfig.java` | 静态资源映射（Swagger UI） |
| `ruoyi-framework/.../config/SecurityConfig.java` | Security放行Swagger相关路径 |

### 2.2 访问路径

| 路径 | 说明 |
|------|------|
| `/swagger-ui.html` | Swagger UI界面 |
| `/v3/api-docs` | OpenAPI JSON文档 |
| `/v3/api-docs.yaml` | OpenAPI YAML文档 |

---

## 三、源码关键片段引用

### 3.1 SwaggerConfig - OpenAPI配置

> 源码位置：`ruoyi-admin/src/main/java/com/ruoyi/web/core/config/SwaggerConfig.java`

```java
@Configuration
public class SwaggerConfig
{
    @Autowired
    private RuoYiConfig ruoyiConfig;

    /**
     * 自定义的 OpenAPI 对象
     */
    @Bean
    public OpenAPI customOpenApi()
    {
        return new OpenAPI().components(new Components()
            // 设置认证的请求头
            .addSecuritySchemes("apikey", securityScheme()))
            .addSecurityItem(new SecurityRequirement().addList("apikey"))
            .info(getApiInfo());
    }

    @Bean
    public SecurityScheme securityScheme()
    {
        return new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .name("Authorization")
            .in(SecurityScheme.In.HEADER)
            .scheme("Bearer");
    }

    /**
     * 添加摘要信息
     */
    public Info getApiInfo()
    {
        return new Info()
            .title("标题：若依管理系统_接口文档")
            .description("描述：用于管理集团旗下公司的人员信息,具体包括XXX,XXX模块...")
            .contact(new Contact().name(ruoyiConfig.getName()))
            .version("版本号:" + ruoyiConfig.getVersion());
    }
}
```

**剖析要点：**
- 使用Springdoc的`OpenAPI`对象配置API文档基本信息
- `SecurityScheme`配置JWT认证方式：类型为APIKEY，Header名称为Authorization，前缀为Bearer
- `SecurityRequirement`全局添加认证要求，所有接口默认需要Token
- 文档标题、描述、作者、版本从`RuoYiConfig`中动态获取

### 3.2 application.yml - Springdoc配置

> 源码位置：`ruoyi-admin/src/main/resources/application.yml`

```yaml
# Springdoc配置
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    tags-sorter: alpha
  group-configs:
    - group: 'default'
      display-name: '测试模块'
      paths-to-match: '/**'
      packages-to-scan: com.ruoyi.web.controller.tool
```

**剖析要点：**
- `api-docs.path`：OpenAPI JSON文档路径
- `swagger-ui.enabled`：是否启用Swagger UI
- `swagger-ui.path`：Swagger UI访问路径
- `tags-sorter: alpha`：接口按字母顺序排序
- `group-configs`：分组配置，当前仅配置了一个"测试模块"分组
- `packages-to-scan`：指定扫描的Controller包路径

### 3.3 ResourcesConfig - 静态资源映射

> 源码位置：`ruoyi-framework/src/main/java/com/ruoyi/framework/config/ResourcesConfig.java`

```java
@Override
public void addResourceHandlers(ResourceHandlerRegistry registry)
{
    /** 本地文件上传路径 */
    registry.addResourceHandler(Constants.RESOURCE_PREFIX + "/**")
            .addResourceLocations("file:" + RuoYiConfig.getProfile() + "/");

    /** swagger配置 */
    registry.addResourceHandler("/swagger-ui/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/")
            .setCacheControl(CacheControl.maxAge(5, TimeUnit.HOURS).cachePublic());
}
```

**剖析要点：**
- Swagger UI的静态资源映射到`springfox-swagger-ui`的classpath路径
- 缓存策略：5小时，减少重复请求
- 注意：虽然使用了Springdoc，但静态资源路径仍引用`springfox-swagger-ui`，这是Springdoc对Swagger UI的兼容处理

### 3.4 SecurityConfig - Swagger路径放行

> 源码位置：`ruoyi-framework/src/main/java/com/ruoyi/framework/config/SecurityConfig.java`

```java
.authorizeHttpRequests((requests) -> {
    // ...
    .requestMatchers("/swagger-ui.html", "/v3/api-docs/**", "/swagger-ui/**", "/druid/**").permitAll()
    // ...
})
```

**剖析要点：**
- Swagger相关路径设为`permitAll()`，无需认证即可访问API文档
- 同时放行了Druid监控路径（`/druid/**`）

---

## 四、接口文档使用流程

```
开发阶段：
  1. 在Controller中编写接口方法
  2. Springdoc自动扫描生成API文档
  3. 访问 /swagger-ui.html 查看文档

调试阶段：
  1. 点击"Authorize"按钮
  2. 输入JWT Token（Bearer {token}）
  3. 选择接口发送请求调试

生产环境：
  1. 设置 swagger-ui.enabled = false 关闭Swagger UI
  2. 或通过Spring Profile控制（仅dev环境启用）
```

---

## 五、细节留神

1. **生产环境安全**：Swagger UI默认开启，生产环境务必关闭（`swagger-ui.enabled=false`），避免暴露接口信息。
2. **分组配置**：默认只有一个"测试模块"分组，实际项目应根据业务模块配置多个分组（如用户管理、系统监控等）。
3. **JWT认证**：在Swagger UI中调试接口时，需要先通过登录接口获取Token，然后在Authorize中配置。
4. **静态资源路径**：ResourcesConfig中引用了`springfox-swagger-ui`路径，如果升级Springdoc版本可能需要调整。
5. **接口描述**：Controller方法上应添加完整的注解描述（@ApiOperation、@ApiParam等），否则文档可读性差。

---

## 六、提问方向

1. **若依从Swagger 2迁移到Springdoc（OpenAPI 3），两者在注解和配置上有哪些主要差异？迁移时需要注意什么？**

2. **当前Swagger分组只配置了"测试模块"，如何根据业务模块（如系统管理、系统监控、开发工具）配置多个分组？**

3. **在生产环境中，除了关闭`swagger-ui.enabled`，还有哪些安全措施可以防止API文档泄露？**

4. **SwaggerConfig中SecurityScheme配置的JWT认证方式为APIKEY类型而非HTTP Bearer类型，这两种方式在Swagger UI中的表现有什么区别？**

5. **如果项目使用了统一返回体（AjaxResult），如何在Swagger文档中正确展示响应结构，而不是展示AjaxResult的外层包装？**

6. **Springdoc如何与若依的`@Anonymous`注解配合，在文档中标注哪些接口不需要认证？**
