---
title: Swagger 接口文档 + Quartz 定时任务
---

# 06-3 Swagger 接口文档 + Quartz 定时任务

> 上接：[[SpringBoot+Vue3后台搭建/06-系统工具/02-文件上传与验证码]]

## 3.1 Swagger 在线接口文档（springdoc）

```java
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
          .info(new Info().title("后台管理系统 API")
                       .version("1.0").description("Spring Boot + Vue3"));
    }
    /** 让文档需要 Bearer 令牌（调试时填 token 即可） */
    @Bean
    public OperationCustomizer bearerAuth() {
        return (operation, handler) -> {
            operation.addSecurityItem(new SecurityRequirement().addList("auth"));
            operation.setSecurity(List.of(new SecurityRequirement().addList("auth")));
            return operation;
        };
    }
}
```
**访问**：`http://localhost:8080/swagger-ui.html`（`springdoc-openapi-starter` 的 UI 路径是 `/swagger-ui.html` 或 `/swagger-ui/index.html`，取决于版本）。能从页面**直接填参数点 Execute** 调接口，前后端联调不靠嘴对字段。

**注解**：Controller 上 `@Tag("用户管理")`，方法 `@Operation(summary="分页查询")`，DTO 字段 `@Schema(description="登录账号")`。这些会渲染进文档。

**安全放行**：`SecurityConfig` 里已 `permitAll()` 了 `/v3/api-docs/**` 和 `/swagger-ui/**`（见 [[../03-后端基础框架/03-SpringSecurity与JWT鉴权]] 3.1），否则文档页打不开。

## 3.2 Quartz 定时任务

**场景**：定时同步库存、定时清理过期数据、定时推送报表。**核心三件套**：`Job`（干啥）、`Trigger`（啥时干）、`Scheduler`（调度器）。

### 3.2.1 一个 Job（业务逻辑）
```java
@Component("stockSyncJob")   // 名字 = invoke_target 里的 Bean 名
public class StockSyncJob extends QuartzJobBean {
    @Resource private IStockService stockService;
    @Override
    protected void executeInternal(JobExecutionContext ctx) {
        stockService.syncFromWcs();     // 调 WCS 拉最新库存（[[../09-业务扩展/03-报表与第三方对接]]）
        log.info("库存同步完成 @ {}", LocalDateTime.now());
    }
}
```
**注意**：必须是 `QuartzJobBean` 子类（不是普通 `@Component` 就完事），Quartz 才会接管它的执行。

### 3.2.2 配置类（调度器 + 线程池）
```java
@Configuration
public class QuartzConfig {
    @Bean
    public SchedulerFactoryBean schedulerFactory(JobFactory jobFactory) {
        SchedulerFactoryBean fb = new SchedulerFactoryBean();
        fb.setJobFactory(jobFactory);     // 让 Quartz 用 Spring 容器取 Bean
        return fb;
    }
    @Bean
    public JobFactory jobFactory(ApplicationContext ctx) {
        // 关键：Quartz 创建的 Job 也能 @Autowired（否则 Job 里注入为 null）
        return new SpringBeanJobFactory() {
            @Override protected Object createJobInstance(TriggerFiredBundle b) throws Exception {
                Object job = super.createJobInstance(b);
                ctx.getAutowireCapableBeanFactory().autowireBeanProperties(job);
                return job;
            }
        };
    }
}
```

### 3.2.3 任务管理接口
```java
@RestController
@RequestMapping("/monitor/job")
public class SysJobController {
    @Resource private Scheduler scheduler;

    /** 新增并立即调度 */
    @PostMapping
    public AjaxResult add(@RequestBody SysJob job) throws Exception {
        JobDetail detail = JobBuilder.newJob(StockSyncJob.class)  // 或用 job.getInvokeTarget() 反射取类
                .withIdentity(job.getJobName(), job.getJobGroup()).build();
        Trigger trig = TriggerBuilder.newTrigger()
                .withIdentity(job.getJobName(), job.getJobGroup())
                .withSchedule(CronScheduleBuilder.cronSchedule(job.getCronExpression()))
                .build();
        scheduler.scheduleJob(detail, trig);
        return AjaxResult.success();
    }

    /** 立即执行一次 */
    @PutMapping("/run/{id}") public AjaxResult runOnce(@PathVariable Long id) throws Exception {
        SysJob job = jobService.getById(id);
        scheduler.triggerJob(new JobKey(job.getJobName(), job.getJobGroup()));
        return AjaxResult.success();
    }

    /** 暂停 / 恢复 */
    @PutMapping("/pause/{id}") public AjaxResult pause(@PathVariable Long id) throws Exception {
        SysJob j = jobService.getById(id);
        scheduler.pauseJob(new JobKey(j.getJobName(), j.getJobGroup()));
        return AjaxResult.success();
    }

    /** 删除 */
    @DeleteMapping("/{id}") public AjaxResult remove(@PathVariable Long id) throws Exception {
        SysJob j = jobService.getById(id);
        scheduler.deleteJob(new JobKey(j.getJobName(), j.getJobGroup()));
        return AjaxResult.success();
    }
}
```
**讲解**：`cron_expression` 如 `0 0 2 * * ?`（每天 2:00）。`triggerJob` 不新建、直接触发一次，即"立即执行"。**暂停≠删除**：暂停后可恢复，删除要重建。

### 3.2.4 任务执行日志（落 `sys_job_log`）
用 AOP 包 `scheduler.triggerJob` / `scheduleJob`，在 Job 跑完/抛错时写 `sys_job_log`（`status`/`exceptionInfo`）。前端列表展示"上次执行时间/结果"。

### 3.2.5 并发控制
`sys_job.concurrent='0'`（禁止并发）时，用 `ConcurrentExectionException` 防护：上一次没跑完，这次不重复起。Quartz 原生支持 `@DisallowConcurrentExecution`。

## 3.3 验证清单
- [ ] 打开 `/swagger-ui.html`，能看到所有 `@Tag` 接口，点执行返回 200。
- [ ] 文档页 Header 能填 Bearer token 后调受保护接口。
- [ ] 新增一个 `0/1 * * * ?`（每分钟）的测试 Job，观察 `sys_job_log` 每分钟多一条。
- [ ] 点"立即执行一次"，`sys_job_log` 立刻多一条而不等 cron。
- [ ] 暂停后 cron 到点不再触发；恢复后恢复。

> 下一步：[[../07-日志与监控/00-索引]] 操作/登录日志 + 服务器/Redis/缓存/备份。
