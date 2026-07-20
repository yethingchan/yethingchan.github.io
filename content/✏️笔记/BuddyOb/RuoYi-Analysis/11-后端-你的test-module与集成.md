# 11 · 后端-你的 test-module 与集成

> 对应清单：高级第 17 条（test-module 接入主工程）。
> 本章把你最初的那个报错（`Could not resolve type alias 'Locations'`）彻底讲透，并说明一个**私有模块**如何干净地并入 RuoYi 主工程。

## 一、test-module 长什么样（来自真实文件清单）

```
test-module/
└─ src/main/java/cn/yething/test/
   ├─ controller/LocationsController.java   // 你的接口
   ├─ domain/Locations.java            // 实体（在 cn.yething.test.domain）
   ├─ mapper/LocationsMapper.java     // Mapper 接口
   ├─ service/ILocationsService.java
   └─ service/impl/LocationsServiceImpl.java
```

**关键点**：包是 `cn.yething.test`，**不在 `com.ruoyi` 下**。RuoYi 默认只扫 `com.ruoyi`，所以不加配置，这个模块对 Spring/MyBatis 完全"隐形"。

## 二、三处改动，把它接进来

### ① 启动类扫包（RuoYiApplication.java）

```java
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@ComponentScan(basePackages = {"cn.yething","com.ruoyi"})   // ← 两个包都扫
@MapperScan("cn.yething.test.mapper")                        // ← 只扫你的 Mapper
public class RuoYiApplication { ... }
```

- `@ComponentScan({"cn.yething","com.ruoyi"})`：让 `LocationsController`/`LocationsServiceImpl` 等被 Spring 实例化。
- `@MapperScan("cn.yething.test.mapper")`：你的 Mapper 接口由这里接管生成代理。（主工程的 Mapper 由 `MyBatisConfig` 的 `mapperLocations` 扫 XML 兜底。）

### ② MyBatis 类型别名覆盖你的包（application.yml）

```yaml
mybatis:
  typeAliasesPackage: com.ruoyi.**.domain,cn.yething.**.domain   # ← 必须含 cn.yething
  mapperLocations: classpath*:mapper/**/*Mapper.xml
```

**这正是你最初 `Could not resolve type alias 'Locations'` 的根因与修复**：
- XML 里写 `resultType="Locations"`，MyBatis 要去"类型别名包"里找 `Locations` 类；
- 当时 `typeAliasesPackage` **只有 `com.ruoyi.**.domain`**，找不到 `cn.yething.test.domain.Locations` → 报错；
- 加上 `cn.yething.**.domain` 后，`MyBatisConfig.setTypeAliasesPackage` 把 `**` 扫描成真实包名，`Locations` 被认出。

### ③ Mapper XML 的写法注意（你遇到过的另一个坑）

```xml
<delete id="deleteLocationsByIds">
  delete from locations where id in
  <foreach collection="array" item="id" open="(" separator="," close=")">
    #{id}
  </foreach>
</delete>
```

- `collection="array"` 表示入参是 `Long[]`（MyBatis 对数组默认 `array`、对 List 默认 `list`）。
- **`parameterType` 和 `collection` 是独立属性**：前面你去掉 `parameterType="Long[]"`（因为 MyBatis 没有 `Long[]` 这个内建别名，写了反而解析失败），但 `collection="array"` 照常工作——它不依赖 `parameterType`，由"入参实际是数组"决定。
- 等价更稳的写法：`void deleteLocationsByIds(@Param("ids") Long[] ids)` + `collection="ids"`。

## 三、它如何融入整个分层（呼应 [[01-后端总览与分层架构]]）

```
浏览器
  → JwtAuthenticationTokenFilter（认 token，不管你哪个包）
  → SecurityConfig（permitAll 之外的都要登录）
  → LocationsController（你写的，@PreAuthorize 同上机制）
       → ILocationsService / impl（业务）
            → LocationsMapper（MyBatis 代理，XML 在 classpath*:mapper/**）
                 → MySQL
Redis（token/权限）、DataScopeAspect、LogAspect 对所有包一视同仁
```

**结论**：`cn.yething.test` 和 `com.ruoyi` 在运行时**没有任何区别待遇**——鉴权、AOP、Redis、MyBatis 全工程共用。你要做的只是"让 Spring 扫到、让 MyBatis 认类、让 XML 能被加载"这三件配置活。

## 四、想再加一个私有模块，照抄即可

1. 新建 Maven 模块，包名 `cn.yething.xxx`；
2. 在根 `pom.xml` 的 `<modules>` 加 `<module>xxx-module</module>`；
3. 实体放 `cn.yething.xxx.domain`（已被 `typeAliasesPackage` 覆盖，无需再改）；
4. Mapper 接口放 `cn.yething.xxx.mapper`，XML 放 `resources/mapper/xxx/`（`classpath*:mapper/**` 自动扫）；
5. 若 Mapper 接口也想被 `@MapperScan` 直接扫，把它加进 `RuoYiApplication` 的 `@MapperScan`（可写 `@MapperScan({"cn.yething.test.mapper","cn.yething.xxx.mapper"})`）。

> 至此，后端 7 章 + test-module 全部讲完。你最初那个 `Locations` 报错，本质是"新模块没被纳入 RuoYi 的扫描与别名体系"——现在你从原理到配置都清楚了。接下来进入前端，看 Vue2 这半边怎么和这套后端默契配合。
