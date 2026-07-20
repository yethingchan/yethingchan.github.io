# 06-企业实战与疑难排查 · 多数据源与 Mapper 分包扫描

> 前置：[[MyBatis Mapper XML/进阶专题/06-企业实战与疑难排查/00-索引]]

## 场景

主库（业务） + 从库（报表/只读） + 第三方库（遗留系统）。每库一套 `SqlSessionFactory` + `MappedStatement`。

## 核心原则：分包隔离

```
com.x.mapper.biz   → 主库 SqlSessionFactory-A
com.x.mapper.report→ 从库 SqlSessionFactory-B
```

## 配置（Spring Boot，多 `SqlSessionFactory`）

```java
@Configuration
@MapperScan(basePackages = "com.x.mapper.biz", sqlSessionFactoryRef = "bizSqlSessionFactory")
public class BizDataSourceConfig {

    @Bean @ConfigurationProperties("spring.datasource.biz")
    public DataSource bizDataSource() { return DruidDataSourceBuilder.create().build(); }

    @Bean
    public SqlSessionFactory bizSqlSessionFactory(@Qualifier("bizDataSource") DataSource ds) throws Exception {
        MybatisSqlSessionFactoryBean fb = new MybatisSqlSessionFactoryBean();
        fb.setDataSource(ds);
        fb.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/biz/**/*Mapper.xml"));
        fb.setTypeAliasesPackage("com.x.biz.domain");   // 该库的实体包
        return fb.getObject();
    }

    @Bean
    public DataSourceTransactionManager bizTxManager(@Qualifier("bizDataSource") DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }
}
```
- `@MapperScan` 把**不同包**的 Mapper 接口绑定到不同 `SqlSessionFactory`。
- 每个库独立的 `DataSourceTransactionManager`。
- 从库**只读**可设 `readOnly=true` 连接（中间件层）。

## 本仓库的"隐式多模块"

你的工程虽是单库，但 `ruoyi-*` 各模块 + `test-module` 也是**多 Mapper 包**共存：
- 主工程：`@MapperScan("com.ruoyi.**.mapper")`（基础包）
- 你的模块：额外 `@MapperScan("cn.yething.test.mapper")`（见 [[RuoYi-Analysis/02-后端-工程与启动]]）

> 它们共用**同一个** `SqlSessionFactory`（单数据源），只是扫描路径不同。一旦你将来接第二个库，就升级成上面的"分包隔离"模式。

## 事务管理器选择

```java
@Transactional("bizTxManager")   // 显式指定用哪个事务管理器
public void doBiz() { ... }
```
- 有多个 `DataSourceTransactionManager` 时，`@Transactional` 必须用 `value/transactionManager` 指定，否则取**主**（primary）那个。

## 常见坑

1. **`@Primary` 冲突**：多个 `DataSource`/`SqlSessionFactory` 必须有一个 `@Primary`，否则 Spring 不知道注入哪个。
2. **`typeAliasesPackage` 漏包** → 别名找不到（正是本仓库 `Locations` 的根因，见 04）。
3. **`mapperLocations` 路径错** → `Invalid bound statement (not found)`（XML 没被扫到）。
4. **跨库事务**：两个库没法用同一个本地事务，需要 **JTA / Seata**（见 [[MySQL/进阶专题/03-分库分表与分布式事务/05-分布式事务Seata与最终一致]]）。

## 结论

- 多库 = 多 `SqlSessionFactory` + `@MapperScan` 按包分流 + 各自事务管理器。
- 单库多模块（如本仓库）共享一个 factory，靠 `@MapperScan` 路径覆盖即可。
- 跨库一致性用分布式事务，不是本地 `@Transactional`。

下一步：[[MyBatis Mapper XML/进阶专题/06-企业实战与疑难排查/02-注解XML混合与复杂报表SQL]]
