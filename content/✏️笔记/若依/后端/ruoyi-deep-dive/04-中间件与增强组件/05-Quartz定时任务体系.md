# 05 - Quartz定时任务体系

## 一、概述

若依框架集成了Quartz定时任务调度框架，实现了一套可视化的定时任务管理系统。支持通过Web界面动态创建、暂停、恢复、删除、立即执行定时任务，支持Cron表达式配置、并发控制策略、失败重试策略等功能。任务执行采用反射调用或Spring Bean调用两种方式，并具备完善的执行日志记录机制。

---

## 二、查看与剖析点

### 2.1 核心文件清单

| 文件路径 | 作用 |
|---------|------|
| `ruoyi-quartz/.../config/ScheduleConfig.java` | Quartz调度器配置（已注释，使用默认内存模式） |
| `ruoyi-quartz/.../domain/SysJob.java` | 定时任务实体类 |
| `ruoyi-quartz/.../service/impl/SysJobServiceImpl.java` | 任务管理Service |
| `ruoyi-quartz/.../util/AbstractQuartzJob.java` | 抽象任务基类，执行前后日志 |
| `ruoyi-quartz/.../util/QuartzJobExecution.java` | 允许并发执行的任务类 |
| `ruoyi-quartz/.../util/QuartzDisallowConcurrentExecution.java` | 禁止并发执行的任务类 |
| `ruoyi-quartz/.../util/JobInvokeUtil.java` | 任务调用工具（反射/Bean） |
| `ruoyi-quartz/.../util/ScheduleUtils.java` | Quartz JobDetail/Trigger创建工具 |
| `ruoyi-quartz/.../util/CronUtils.java` | Cron表达式工具 |
| `ruoyi-quartz/.../task/RyTask.java` | 示例任务（多类型参数） |
| `ruoyi-common/.../constant/ScheduleConstants.java` | 定时任务常量 |
| `ruoyi-common/.../constant/Constants.java` | 任务白名单/黑名单配置 |

### 2.2 架构层次

```
SysJobController (Web管理接口)
    |
SysJobServiceImpl (任务CRUD + 调度管理)
    |
ScheduleUtils (创建JobDetail/Trigger)
    |
Quartz Scheduler (调度引擎)
    |
AbstractQuartzJob (抽象基类 - 日志记录)
    |
QuartzJobExecution / QuartzDisallowConcurrentExecution (并发控制)
    |
JobInvokeUtil (反射/Bean调用)
    |
目标方法 (com.ruoyi.quartz.task.*)
```

---

## 三、源码关键片段引用

### 3.1 ScheduleConfig - 调度器配置

> 源码位置：`ruoyi-quartz/src/main/java/com/ruoyi/quartz/config/ScheduleConfig.java`

```java
// 当前已被注释掉，使用Quartz默认的内存存储模式
// 单机部署建议删除此类和qrtz数据库表，默认走内存会最高效
//@Configuration
//public class ScheduleConfig
//{
//    @Bean
//    public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource)
//    {
//        SchedulerFactoryBean factory = new SchedulerFactoryBean();
//        factory.setDataSource(dataSource);
//
//        Properties prop = new Properties();
//        prop.put("org.quartz.scheduler.instanceName", "RuoyiScheduler");
//        prop.put("org.quartz.scheduler.instanceId", "AUTO");
//        prop.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
//        prop.put("org.quartz.threadPool.threadCount", "20");
//        prop.put("org.quartz.threadPool.threadPriority", "5");
//        prop.put("org.quartz.jobStore.class", "org.springframework.scheduling.quartz.LocalDataSourceJobStore");
//        prop.put("org.quartz.jobStore.isClustered", "true");
//        prop.put("org.quartz.jobStore.clusterCheckinInterval", "15000");
//        prop.put("org.quartz.jobStore.maxMisfiresToHandleAtATime", "10");
//        prop.put("org.quartz.jobStore.txIsolationLevelSerializable", "true");
//        prop.put("org.quartz.jobStore.misfireThreshold", "12000");
//        prop.put("org.quartz.jobStore.tablePrefix", "QRTZ_");
//        factory.setQuartzProperties(prop);
//
//        factory.setSchedulerName("RuoyiScheduler");
//        factory.setStartupDelay(1);
//        factory.setApplicationContextSchedulerContextKey("applicationContextKey");
//        factory.setOverwriteExistingJobs(true);
//        factory.setAutoStartup(true);
//
//        return factory;
//    }
//}
```

**剖析要点：**
- 当前配置已被注释，使用Quartz默认的RAMJobStore（内存存储），适合单机部署
- 如果需要集群部署或持久化任务信息，取消注释并配置数据库存储
- 线程池配置：20个线程，优先级5
- 集群配置：`isClustered=true`，集群检查间隔15秒
- `overwriteExistingJobs=true`：启动时覆盖已存在的Job定义

### 3.2 SysJob - 任务实体

> 源码位置：`ruoyi-quartz/src/main/java/com/ruoyi/quartz/domain/SysJob.java`

SysJob实体包含以下核心字段：
- `jobId`：任务ID
- `jobName`：任务名称
- `jobGroup`：任务组名
- `invokeTarget`：调用目标字符串（如`ryTask.ryParams('ry')`）
- `cronExpression`：Cron表达式
- `misfirePolicy`：计划执行错误策略（1-默认/2-立即执行/3-执行一次/4-放弃执行）
- `concurrent`：是否并发执行（0-允许/1-禁止）
- `status`：状态（0-正常/1-暂停）

### 3.3 AbstractQuartzJob - 抽象任务基类

> 源码位置：`ruoyi-quartz/src/main/java/com/ruoyi/quartz/util/AbstractQuartzJob.java`

```java
public abstract class AbstractQuartzJob implements Job
{
    private static final Logger log = LoggerFactory.getLogger(AbstractQuartzJob.class);
    private static ThreadLocal<Date> threadLocal = new ThreadLocal<>();

    @Override
    public void execute(JobExecutionContext context)
    {
        SysJob sysJob = new SysJob();
        BeanUtils.copyBeanProp(sysJob, context.getMergedJobDataMap().get(ScheduleConstants.TASK_PROPERTIES));
        try
        {
            before(context, sysJob);
            if (sysJob != null)
            {
                doExecute(context, sysJob);
            }
            after(context, sysJob, null);
        }
        catch (Exception e)
        {
            log.error("任务执行异常  - ：", e);
            after(context, sysJob, e);
        }
    }

    protected void before(JobExecutionContext context, SysJob sysJob)
    {
        threadLocal.set(new Date());
    }

    protected void after(JobExecutionContext context, SysJob sysJob, Exception e)
    {
        Date startTime = threadLocal.get();
        threadLocal.remove();

        final SysJobLog sysJobLog = new SysJobLog();
        sysJobLog.setJobName(sysJob.getJobName());
        sysJobLog.setJobGroup(sysJob.getJobGroup());
        sysJobLog.setInvokeTarget(sysJob.getInvokeTarget());
        sysJobLog.setStartTime(startTime);
        sysJobLog.setEndTime(new Date());
        long runMs = sysJobLog.getEndTime().getTime() - sysJobLog.getStartTime().getTime();
        sysJobLog.setJobMessage(sysJobLog.getJobName() + " 总共耗时：" + runMs + "毫秒");
        if (e != null)
        {
            sysJobLog.setStatus(Constants.FAIL);
            String errorMsg = StringUtils.substring(ExceptionUtil.getExceptionMessage(e), 0, 2000);
            sysJobLog.setExceptionInfo(errorMsg);
        }
        else
        {
            sysJobLog.setStatus(Constants.SUCCESS);
        }
        SpringUtils.getBean(ISysJobLogService.class).addJobLog(sysJobLog);
    }

    protected abstract void doExecute(JobExecutionContext context, SysJob sysJob) throws Exception;
}
```

**剖析要点：**
- 模板方法模式：`execute` -> `before` -> `doExecute` -> `after`
- `before`记录开始时间，`after`计算耗时并写入执行日志
- 异常信息截断为2000字符，防止超长异常信息
- 使用ThreadLocal记录开始时间，保证线程安全
- `after`无论成功还是异常都会执行，确保日志完整记录

### 3.4 QuartzJobExecution vs QuartzDisallowConcurrentExecution

> 源码位置：`ruoyi-quartz/src/main/java/com/ruoyi/quartz/util/`

**允许并发执行：**
```java
public class QuartzJobExecution extends AbstractQuartzJob
{
    @Override
    protected void doExecute(JobExecutionContext context, SysJob sysJob) throws Exception
    {
        JobInvokeUtil.invokeMethod(sysJob);
    }
}
```

**禁止并发执行：**
```java
@DisallowConcurrentExecution
public class QuartzDisallowConcurrentExecution extends AbstractQuartzJob
{
    @Override
    protected void doExecute(JobExecutionContext context, SysJob sysJob) throws Exception
    {
        JobInvokeUtil.invokeMethod(sysJob);
    }
}
```

**剖析要点：**
- 两者唯一区别：`QuartzDisallowConcurrentExecution`添加了`@DisallowConcurrentExecution`注解
- `@DisallowConcurrentExecution`是Quartz提供的注解，确保同一JobDetail不会并发执行
- 通过`ScheduleUtils.getQuartzJobClass()`根据`concurrent`字段动态选择使用哪个类

### 3.5 JobInvokeUtil - 任务调用工具

> 源码位置：`ruoyi-quartz/src/main/java/com/ruoyi/quartz/util/JobInvokeUtil.java`

```java
public static void invokeMethod(SysJob sysJob) throws Exception
{
    String invokeTarget = sysJob.getInvokeTarget();
    String beanName = getBeanName(invokeTarget);
    String methodName = getMethodName(invokeTarget);
    List<Object[]> methodParams = getMethodParams(invokeTarget);

    if (!isValidClassName(beanName))
    {
        // Spring Bean调用
        Object bean = SpringUtils.getBean(beanName);
        invokeMethod(bean, methodName, methodParams);
    }
    else
    {
        // 反射调用（全限定类名）
        Object bean = Class.forName(beanName).getDeclaredConstructor().newInstance();
        invokeMethod(bean, methodName, methodParams);
    }
}

public static boolean isValidClassName(String invokeTarget)
{
    return StringUtils.countMatches(invokeTarget, ".") > 1;
}
```

**剖析要点：**
- 两种调用方式：Spring Bean调用（如`ryTask.ryParams('ry')`）和反射调用（全限定类名）
- `isValidClassName`通过点号数量判断：点号>1为全限定类名，否则为Bean名称
- 参数类型自动推断：字符串（引号包裹）、布尔值（true/false）、Long（L结尾）、Double（D结尾）、Integer（其他）
- 参数解析支持单引号和双引号字符串

### 3.6 ScheduleUtils - 任务创建

> 源码位置：`ruoyi-quartz/src/main/java/com/ruoyi/quartz/util/ScheduleUtils.java`

```java
private static Class<? extends Job> getQuartzJobClass(SysJob sysJob)
{
    boolean isConcurrent = "0".equals(sysJob.getConcurrent());
    return isConcurrent ? QuartzJobExecution.class : QuartzDisallowConcurrentExecution.class;
}

public static void createScheduleJob(Scheduler scheduler, SysJob job) throws SchedulerException, TaskException
{
    Class<? extends Job> jobClass = getQuartzJobClass(job);
    Long jobId = job.getJobId();
    String jobGroup = job.getJobGroup();
    JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(getJobKey(jobId, jobGroup)).build();

    CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(job.getCronExpression());
    cronScheduleBuilder = handleCronScheduleMisfirePolicy(job, cronScheduleBuilder);

    CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(getTriggerKey(jobId, jobGroup))
            .withSchedule(cronScheduleBuilder).build();

    jobDetail.getJobDataMap().put(ScheduleConstants.TASK_PROPERTIES, job);

    if (scheduler.checkExists(getJobKey(jobId, jobGroup)))
    {
        scheduler.deleteJob(getJobKey(jobId, jobGroup));
    }

    if (StringUtils.isNotNull(CronUtils.getNextExecution(job.getCronExpression())))
    {
        scheduler.scheduleJob(jobDetail, trigger);
    }

    if (job.getStatus().equals(ScheduleConstants.Status.PAUSE.getValue()))
    {
        scheduler.pauseJob(ScheduleUtils.getJobKey(jobId, jobGroup));
    }
}
```

**白名单校验：**
```java
public static boolean whiteList(String invokeTarget)
{
    String packageName = StringUtils.substringBefore(invokeTarget, "(");
    int count = StringUtils.countMatches(packageName, ".");
    if (count > 1)
    {
        return StringUtils.startsWithAny(invokeTarget, Constants.JOB_WHITELIST_STR);
    }
    Object obj = SpringUtils.getBean(StringUtils.split(invokeTarget, ".")[0]);
    String beanPackageName = obj.getClass().getPackage().getName();
    return StringUtils.startsWithAny(beanPackageName, Constants.JOB_WHITELIST_STR)
            && !StringUtils.startsWithAny(beanPackageName, Constants.JOB_ERROR_STR);
}
```

**剖析要点：**
- `concurrent=0`使用允许并发类，`concurrent=1`使用禁止并发类
- 创建前先检查是否存在，存在则先删除再创建（避免数据冲突）
- 检查Cron表达式是否有下一次执行时间，无则不创建（避免无效任务）
- 白名单校验：Bean调用检查包名是否在白名单内，反射调用检查全限定名前缀
- 黑名单排除：`JOB_ERROR_STR`包含`java.net.URL`、`javax.naming`、`org.springframework`等危险类

### 3.7 任务白名单与黑名单

> 源码位置：`ruoyi-common/src/main/java/com/ruoyi/common/constant/Constants.java`

```java
/** 定时任务白名单配置 */
public static final String[] JOB_WHITELIST_STR = { "com.ruoyi.quartz.task" };

/** 定时任务违规的字符 */
public static final String[] JOB_ERROR_STR = { "java.net.URL", "javax.naming.InitialContext",
        "org.yaml.snakeyaml", "org.springframework", "org.apache",
        "com.ruoyi.common.utils.file", "com.ruoyi.common.config", "com.ruoyi.generator" };
```

**剖析要点：**
- 白名单限制任务只能调用`com.ruoyi.quartz.task`包下的类
- 黑名单排除可能执行系统命令或访问敏感资源的类
- 这是防止定时任务被利用执行恶意代码的安全措施

---

## 四、细节留神

1. **内存 vs 数据库存储**：默认使用RAMJobStore，应用重启后任务状态丢失。集群部署必须使用JDBCJobStore。
2. **白名单限制**：自定义任务类必须放在`com.ruoyi.quartz.task`包下，否则无法通过白名单校验。
3. **参数解析限制**：`JobInvokeUtil`的参数解析较为简单，不支持复杂对象参数，仅支持基本类型。
4. **并发控制粒度**：`@DisallowConcurrentExecution`是基于JobDetail的，同一任务定义不会并发，但不同任务可以并发执行。
5. **Misfire策略**：四种策略的选择影响任务错过执行后的补偿行为，需要根据业务场景选择。

---

## 五、提问方向

1. **若依默认使用RAMJobStore（内存存储），如果需要集群部署保证任务不重复执行，应该如何配置JDBCJobStore？需要哪些数据库表？**

2. **定时任务白名单限制为`com.ruoyi.quartz.task`，如果需要在其他包下创建任务类，如何安全地扩展白名单？**

3. **`JobInvokeUtil`的参数解析只支持基本类型，如果需要传递复杂对象（如Map、List），应该如何扩展？**

4. **`@DisallowConcurrentExecution`注解保证同一任务不并发执行，但如果任务执行时间超过Cron间隔，Quartz会如何处理？会不会丢失执行？**

5. **AbstractQuartzJob中异常信息截断为2000字符，如果异常堆栈很长，如何设计一个更完善的异常信息存储方案？**

6. **当前的任务执行日志直接写入数据库，如果任务执行频率很高（如每秒一次），大量日志写入会不会影响数据库性能？如何优化？**
