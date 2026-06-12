# 05 - MyBatis与数据持久层（MyBatisConfig、PageHelper、动态数据源）

## 一、查看&剖析点

| 剖析维度 | 关键文件路径 |
|---------|------------|
| MyBatisConfig | `ruoyi-framework/src/main/java/com/ruoyi/framework/config/MyBatisConfig.java` |
| DruidConfig | `ruoyi-framework/src/main/java/com/ruoyi/framework/config/DruidConfig.java` |
| DynamicDataSource | `ruoyi-framework/src/main/java/com/ruoyi/framework/datasource/DynamicDataSource.java` |
| DynamicDataSourceContextHolder | `ruoyi-framework/src/main/java/com/ruoyi/framework/datasource/DynamicDataSourceContextHolder.java` |
| DataSourceAspect | `ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/DataSourceAspect.java` |
| application.yml | `ruoyi-admin/src/main/resources/application.yml` |
| application-druid.yml | `ruoyi-admin/src/main/resources/application-druid.yml` |
| mybatis-config.xml | `ruoyi-admin/src/main/resources/mybatis/mybatis-config.xml` |
| Mapper XML示例 | `ruoyi-system/src/main/resources/mapper/system/SysUserMapper.xml` |

---

## 二、核心设计思想

RuoYi-Vue 的数据持久层采用 **"MyBatis + PageHelper + Druid + 动态数据源"** 的组合方案：

- **MyBatis**：ORM 框架，负责 SQL 映射和对象关系转换
- **PageHelper**：MyBatis 分页插件，通过拦截器实现物理分页
- **Druid**：阿里巴巴数据库连接池，提供监控和性能优化
- **动态数据源**：基于 `AbstractRoutingDataSource` 实现主从切换

---

## 三、源码关键片段引用

### 3.1 MyBatisConfig -- MyBatis核心配置

```java
// 文件：ruoyi-framework/src/main/java/com/ruoyi/framework/config/MyBatisConfig.java
@Configuration
public class MyBatisConfig
{
    @Autowired
    private Environment env;

    static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";

    /**
     * 支持通配符扫描包（如 com.ruoyi.**.domain）
     */
    public static String setTypeAliasesPackage(String typeAliasesPackage)
    {
        ResourcePatternResolver resolver = (ResourcePatternResolver) new PathMatchingResourcePatternResolver();
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resolver);
        List<String> allResult = new ArrayList<String>();
        try
        {
            for (String aliasesPackage : typeAliasesPackage.split(","))
            {
                List<String> result = new ArrayList<String>();
                aliasesPackage = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                        + ClassUtils.convertClassNameToResourcePath(aliasesPackage.trim()) + "/" + DEFAULT_RESOURCE_PATTERN;
                Resource[] resources = resolver.getResources(aliasesPackage);
                if (resources != null && resources.length > 0)
                {
                    MetadataReader metadataReader = null;
                    for (Resource resource : resources)
                    {
                        if (resource.isReadable())
                        {
                            metadataReader = metadataReaderFactory.getMetadataReader(resource);
                            try
                            {
                                result.add(Class.forName(
                                    metadataReader.getClassMetadata().getClassName()
                                ).getPackage().getName());
                            }
                            catch (ClassNotFoundException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                // 去重
                if (result.size() > 0)
                {
                    HashSet<String> hashResult = new HashSet<String>(result);
                    allResult.addAll(hashResult);
                }
            }
            if (allResult.size() > 0)
            {
                typeAliasesPackage = String.join(",", allResult.toArray(new String[0]));
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return typeAliasesPackage;
    }

    /**
     * 解析Mapper XML文件位置（支持通配符）
     */
    public Resource[] resolveMapperLocations(String[] mapperLocations)
    {
        ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
        List<Resource> resources = new ArrayList<Resource>();
        if (mapperLocations != null)
        {
            for (String mapperLocation : mapperLocations)
            {
                try
                {
                    Resource[] mappers = resourceResolver.getResources(mapperLocation);
                    resources.addAll(Arrays.asList(mappers));
                }
                catch (IOException e)
                {
                    // ignore
                }
            }
        }
        return resources.toArray(new Resource[resources.size()]);
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception
    {
        String typeAliasesPackage = env.getProperty("mybatis.typeAliasesPackage");
        String mapperLocations = env.getProperty("mybatis.mapperLocations");
        String configLocation = env.getProperty("mybatis.configLocation");
        typeAliasesPackage = setTypeAliasesPackage(typeAliasesPackage);
        VFS.addImplClass(SpringBootVFS.class);

        final SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setTypeAliasesPackage(typeAliasesPackage);
        sessionFactory.setMapperLocations(resolveMapperLocations(
            StringUtils.split(mapperLocations, ",")));
        sessionFactory.setConfigLocation(new DefaultResourceLoader().getResource(configLocation));
        return sessionFactory.getObject();
    }
}
```

**设计要点**：
- `setTypeAliasesPackage` 支持通配符包扫描（如 `com.ruoyi.**.domain`），将通配符展开为具体的包名
- `resolveMapperLocations` 支持多个 Mapper XML 路径，使用 `PathMatchingResourcePatternResolver` 解析通配符
- 手动创建 `SqlSessionFactory`，而非依赖 MyBatis-Spring-Boot-Starter 的自动配置
- `VFS.addImplClass(SpringBootVFS.class)` 解决 Spring Boot 打包后 MyBatis 无法读取类路径资源的问题

### 3.2 DruidConfig -- 数据源配置

```java
// 文件：ruoyi-framework/src/main/java/com/ruoyi/framework/config/DruidConfig.java
@Configuration
public class DruidConfig
{
    // 主数据源
    @Bean
    @ConfigurationProperties("spring.datasource.druid.master")
    public DataSource masterDataSource(DruidProperties druidProperties)
    {
        DruidDataSource dataSource = DruidDataSourceBuilder.create().build();
        return druidProperties.dataSource(dataSource);
    }

    // 从数据源（条件加载）
    @Bean
    @ConfigurationProperties("spring.datasource.druid.slave")
    @ConditionalOnProperty(prefix = "spring.datasource.druid.slave",
                           name = "enabled", havingValue = "true")
    public DataSource slaveDataSource(DruidProperties druidProperties)
    {
        DruidDataSource dataSource = DruidDataSourceBuilder.create().build();
        return druidProperties.dataSource(dataSource);
    }

    // 动态数据源
    @Bean(name = "dynamicDataSource")
    @Primary
    public DynamicDataSource dataSource(DataSource masterDataSource)
    {
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceType.MASTER.name(), masterDataSource);
        setDataSource(targetDataSources, DataSourceType.SLAVE.name(), "slaveDataSource");
        return new DynamicDataSource(masterDataSource, targetDataSources);
    }
}
```

**设计要点**：
- 主数据源通过 `@ConfigurationProperties` 绑定 `spring.datasource.druid.master` 配置
- 从数据源通过 `@ConditionalOnProperty` 条件加载，只有配置 `enabled=true` 才创建
- `DynamicDataSource` 标记为 `@Primary`，作为默认数据源注入
- `setDataSource` 方法通过 `SpringUtils.getBean` 尝试获取从数据源 Bean，获取不到时静默忽略

### 3.3 DynamicDataSource -- 动态路由数据源

```java
// 文件：ruoyi-framework/src/main/java/com/ruoyi/framework/datasource/DynamicDataSource.java
public class DynamicDataSource extends AbstractRoutingDataSource
{
    public DynamicDataSource(DataSource defaultTargetDataSource, Map<Object, Object> targetDataSources)
    {
        super.setDefaultTargetDataSource(defaultTargetDataSource);
        super.setTargetDataSources(targetDataSources);
        super.afterPropertiesSet();
    }

    @Override
    protected Object determineCurrentLookupKey()
    {
        return DynamicDataSourceContextHolder.getDataSourceType();
    }
}
```

**设计要点**：
- 继承 Spring 的 `AbstractRoutingDataSource`，核心方法是 `determineCurrentLookupKey()`
- 每次获取数据库连接时，Spring 会调用 `determineCurrentLookupKey()` 获取当前数据源的 key
- key 来自 `DynamicDataSourceContextHolder`（ThreadLocal 存储）

### 3.4 DynamicDataSourceContextHolder -- 数据源上下文

```java
// 文件：ruoyi-framework/src/main/java/com/ruoyi/framework/datasource/DynamicDataSourceContextHolder.java
public class DynamicDataSourceContextHolder
{
    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();

    public static void setDataSourceType(String dsType)
    {
        log.info("切换到{}数据源", dsType);
        CONTEXT_HOLDER.set(dsType);
    }

    public static String getDataSourceType()
    {
        return CONTEXT_HOLDER.get();
    }

    public static void clearDataSourceType()
    {
        CONTEXT_HOLDER.remove();
    }
}
```

**设计要点**：
- 使用 `ThreadLocal` 存储当前线程的数据源类型
- `setDataSourceType` 切换数据源，`clearDataSourceType` 清理（防止内存泄漏）
- 注意：`ThreadLocal` 在线程池环境下需要特别注意清理

### 3.5 DataSourceAspect -- 数据源切换切面

```java
// 文件：ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/DataSourceAspect.java
@Aspect
@Order(1)  // 最高优先级，确保在数据操作之前切换数据源
@Component
public class DataSourceAspect
{
    @Pointcut("@annotation(com.ruoyi.common.annotation.DataSource)"
            + "|| @within(com.ruoyi.common.annotation.DataSource)")
    public void dsPointCut() {}

    @Around("dsPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable
    {
        DataSource dataSource = getDataSource(point);
        if (StringUtils.isNotNull(dataSource))
        {
            DynamicDataSourceContextHolder.setDataSourceType(dataSource.value().name());
        }
        try
        {
            return point.proceed();
        }
        finally
        {
            DynamicDataSourceContextHolder.clearDataSourceType();
        }
    }

    /**
     * 获取数据源注解（方法优先，类次之）
     */
    public DataSource getDataSource(ProceedingJoinPoint point)
    {
        MethodSignature signature = (MethodSignature) point.getSignature();
        DataSource dataSource = AnnotationUtils.findAnnotation(signature.getMethod(), DataSource.class);
        if (Objects.nonNull(dataSource))
        {
            return dataSource;
        }
        return AnnotationUtils.findAnnotation(signature.getDeclaringType(), DataSource.class);
    }
}
```

**设计要点**：
- `@Order(1)` 确保在所有其他切面之前执行
- 支持方法级别和类级别的 `@DataSource` 注解，**方法优先于类**
- `finally` 块中清理 ThreadLocal，防止线程复用时数据源错乱
- `@within` 表示类级别的注解也会被拦截

### 3.6 application.yml 中的 MyBatis 配置

```yaml
# 文件：ruoyi-admin/src/main/resources/application.yml
mybatis:
  typeAliasesPackage: com.ruoyi.**.domain
  mapperLocations: classpath*:mapper/**/*Mapper.xml
  configLocation: classpath:mybatis/mybatis-config.xml
```

### 3.7 mybatis-config.xml 全局配置

```xml
<!-- 文件：ruoyi-admin/src/main/resources/mybatis/mybatis-config.xml -->
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration PUBLIC "-//mybatis.org//DTD Config 3.0//EN" "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <settings>
        <setting name="cacheEnabled" value="false" />
        <setting name="lazyLoadingEnabled" value="true" />
        <setting name="multipleResultSetsEnabled" value="true" />
        <setting name="useColumnLabel" value="true" />
        <setting name="useGeneratedKeys" value="true" />
        <setting name="defaultExecutorType" value="REUSE" />
        <setting name="mapUnderscoreToCamelCase" value="true" />
    </settings>
</configuration>
```

**关键配置说明**：
- `cacheEnabled=false`：关闭 MyBatis 二级缓存（使用 Redis 替代）
- `mapUnderscoreToCamelCase=true`：数据库下划线命名自动转 Java 驼峰命名
- `useGeneratedKeys=true`：支持主键回填

### 3.8 Mapper XML 示例

```xml
<!-- 文件：ruoyi-system/src/main/resources/mapper/system/SysUserMapper.xml -->
<mapper namespace="com.ruoyi.system.mapper.SysUserMapper">
    <resultMap type="SysUser" id="SysUserResult">
        <id     property="userId"     column="user_id"     />
        <result property="deptId"     column="dept_id"     />
        <result property="userName"   column="user_name"   />
        <result property="nickName"   column="nick_name"   />
        <!-- ... -->
        <association property="dept" javaType="SysDept" resultMap="SysDeptResult" />
        <collection property="roles" javaType="java.util.List" resultMap="SysRoleResult" />
    </resultMap>

    <select id="selectUserList" parameterType="SysUser" resultMap="SysUserResult">
        select u.user_id, u.dept_id, u.user_name, u.nick_name, u.email, u.phonenumber, u.status,
            u.create_by, u.create_time, d.dept_name, d.leader
        from sys_user u
        left join sys_dept d on u.dept_id = d.dept_id
        where u.del_flag = '0'
        <if test="userName != null and userName != ''">
            AND u.user_name like concat('%', #{userName}, '%')
        </if>
        <if test="status != null and status != ''">
            AND u.status = #{status}
        </if>
        <!-- 数据权限过滤 -->
        ${params.dataScope}
    </select>
</mapper>
```

**设计要点**：
- 使用 `<resultMap>` 手动映射，支持关联查询（`association`/`collection`）
- `${params.dataScope}` 是数据权限过滤的 SQL 片段，由 `DataScopeAspect` 动态注入
- 使用 `concat('%', #{userName}, '%')` 而非 `'%' + #{userName} + '%'`，避免 SQL 注入

### 3.9 PageHelper 分页机制

```java
// BaseController 中的分页方法
protected void startPage()
{
    PageUtils.startPage();
}

protected TableDataInfo getDataTable(List<?> list)
{
    TableDataInfo rspData = new TableDataInfo();
    rspData.setCode(HttpStatus.SUCCESS);
    rspData.setMsg("查询成功");
    rspData.setRows(list);
    rspData.setTotal(new PageInfo(list).getTotal());
    return rspData;
}
```

**分页使用流程**：
```java
// Controller 中
startPage();  // 设置分页参数（从请求中获取 pageNum 和 pageSize）
List<SysUser> list = userService.selectUserList(user);  // 执行查询（PageHelper自动拦截）
return getDataTable(list);  // 封装分页结果
```

---

## 四、动态数据源切换流程

```
1. Service 方法标注 @DataSource(DataSourceType.SLAVE)
    |
    v
2. DataSourceAspect 拦截（@Order(1) 最高优先级）
    |
    v
3. 读取注解的 value()，设置到 ThreadLocal
    DynamicDataSourceContextHolder.setDataSourceType("SLAVE")
    |
    v
4. 执行 Service 方法中的数据库操作
    |
    v
5. DynamicDataSource.determineCurrentLookupKey() 从 ThreadLocal 获取 "SLAVE"
    |
    v
6. AbstractRoutingDataSource 根据 key 路由到从数据源
    |
    v
7. finally 清理 ThreadLocal
    DynamicDataSourceContextHolder.clearDataSourceType()
```

---

## 五、细节留神

1. **MyBatisConfig 手动创建 SqlSessionFactory**：这是因为需要支持通配符包扫描（`com.ruoyi.**.domain`），MyBatis-Spring-Boot-Starter 的自动配置不支持这种通配符。

2. **cacheEnabled=false**：关闭了 MyBatis 二级缓存，使用 Redis 作为分布式缓存替代。这是一个正确的选择，因为 MyBatis 二级缓存在分布式环境下有数据一致性问题。

3. **从数据源条件加载**：`@ConditionalOnProperty` 意味着如果不需要主从切换，从数据源不会被创建，减少资源消耗。

4. **DataSourceAspect 的 @Order(1)**：确保数据源切换在所有其他切面（如 @Log、@DataScope）之前执行，否则其他切面中的数据库操作可能使用错误的数据源。

5. **${params.dataScope} 使用 $ 而非 #**：这是数据权限过滤的 SQL 片段注入，使用 `${}` 是因为需要注入完整的 SQL 片段（包含 WHERE 条件），不能用 `#{}` 参数化。但这也意味着需要确保 dataScope 的值是安全的。

6. **PageHelper 的线程变量清理**：`BaseController.clearPage()` 方法用于清理 PageHelper 的线程变量，但实际代码中很少调用。

---

## 六、提问方向

1. **MyBatisConfig 手动创建 SqlSessionFactory 而非使用自动配置，这种做法在 Spring Boot 3.x 中是否仍然必要？有没有更优雅的方式支持通配符包扫描？**

2. **动态数据源使用 ThreadLocal 存储数据源类型，在线程池环境下可能存在什么问题？你会如何设计更安全的线程上下文传递机制？**

3. **${params.dataScope} 使用 `${}` 注入 SQL 片段，虽然内容由 DataScopeAspect 控制，但这种做法是否存在安全风险？你会如何改进？**

4. **PageHelper 分页在复杂查询（多表关联、子查询）中可能出现性能问题。你会如何优化分页查询？**

5. **如果需要支持三个以上的数据源（如主库、从库1、从库2），当前的动态数据源设计需要做哪些改造？**

6. **Druid 连接池的监控功能（StatFilter、WallFilter）在生产环境中应该开启还是关闭？如何配置合理的连接池参数？**

7. **MyBatis 的 `useGeneratedKeys=true` 在批量插入时如何使用？如果表有自增主键但不需要回填，关闭这个配置是否有性能提升？**

8. **在读写分离场景下，如果在同一个事务中既有读操作又有写操作，如何确保一致性？你会如何设计事务与数据源切换的协调机制？**
