# Spring Boot 事务（Transaction）完整知识库（深度精编版）

> 目标：系统讲透 Spring Boot 中事务的原理、用法、传播行为、隔离级别、失效场景与底层机制，从"会用 @Transactional"进阶到"理解代理、传播、回滚的每一个细节"，从容应对生产问题与面试。

---

## 目录

1. [事务与 ACID 基础](#一事务与-acid-基础)
2. [Spring 事务抽象](#二spring-事务抽象)
3. [声明式事务 @Transactional](#三声明式事务-transactional)
4. [@Transactional 属性全解](#四transactional-属性全解)
5. [事务传播机制（7 种）](#五事务传播机制7-种)
6. [事务隔离级别与并发问题](#六事务隔离级别与并发问题)
7. [编程式事务](#七编程式事务)
8. [事务失效的 10 大场景](#八事务失效的-10-大场景)
9. [Spring 代理与自调用问题](#九spring-代理与自调用问题)
10. [事务同步管理器与底层原理](#十事务同步管理器与底层原理)
11. [多数据源与事务](#十一多数据源与事务)
12. [分布式事务简介](#十二分布式事务简介)
13. [实战案例](#十三实战案例)
14. [最佳实践清单](#十四最佳实践清单)
15. [20+ 面试题精解](#十五20-面试题精解)

---

## 一、事务与 ACID 基础

### 1.1 什么是事务

事务（Transaction）是数据库操作的最小逻辑单元，一组操作要么**全部成功**，要么**全部失败回滚**，保证数据一致性。

### 1.2 ACID 特性

| 特性 | 含义 | 实现手段 |
|------|------|----------|
| **A**tomicity 原子性 | 操作要么全做要么全不做 | undo log（回滚日志） |
| **C**onsistency 一致性 | 数据从一个一致态到另一个一致态 | 由应用 + 约束保证 |
| **I**solation 隔离性 | 并发事务互不干扰 | 锁 + MVCC + 隔离级别 |
| **D**urability 持久性 | 提交后修改永久生效 | redo log（重做日志） |

> MySQL InnoDB 用 **undo log** 实现原子性/回滚，用 **redo log** 实现持久性，用 **MVCC + 锁** 实现隔离性。

### 1.3 为什么需要事务

```java
// 转账：A 扣钱、B 加钱，必须在一个事务内
accountMapper.decrease("A", 100);
// 若此处抛异常，A 已扣钱但 B 没加 → 数据不一致！
accountMapper.increase("B", 100);
```

> 没有事务，半途失败会导致脏数据。事务保证这两步"同生共死"。

---

## 二、Spring 事务抽象

### 2.1 核心接口

Spring 把事务管理抽象为统一接口，业务代码无需关心底层是 JDBC、JPA 还是 MyBatis。

```
PlatformTransactionManager  ← 事务管理器（核心）
   ├── DataSourceTransactionManager   （JDBC/MyBatis，最常用）
   ├── JpaTransactionManager          （JPA/Hibernate）
   ├── HibernateTransactionManager    （Hibernate）
   └── JtaTransactionManager          （JTA 多资源/分布式）
```

```java
public interface PlatformTransactionManager {
    TransactionStatus getTransaction(TransactionDefinition definition); // 开启/获取事务
    void commit(TransactionStatus status);   // 提交
    void rollback(TransactionStatus status); // 回滚
}
```

### 2.2 Spring Boot 自动装配

Spring Boot 引入 `spring-boot-starter-jdbc` 或 `mybatis-spring-boot-starter` 后，会自动装配 `DataSourceTransactionManager`，并开启注解事务支持。无需手动声明 `@EnableTransactionManagement`（Spring Boot 已通过 `TransactionAutoConfiguration` 自动开启）。

```java
@SpringBootApplication
public class App {
    public static void main(String[] args) { SpringApplication.run(App.class); }
}
// 只要类路径有事务管理器 Bean，@Transactional 即可直接用
```

> 对比 Spring（非 Boot）：需手动 `@Configuration` + `@EnableTransactionManagement` + 声明 `DataSourceTransactionManager` Bean。Spring Boot 自动完成这些。

---

## 三、声明式事务 @Transactional

### 3.1 最基础用法

```java
@Service
public class AccountService {

    @Transactional
    public void transfer(String from, String to, BigDecimal amount) {
        accountMapper.decrease(from, amount);
        accountMapper.increase(to, amount);
        // 方法正常结束 → 提交；抛 RuntimeException → 回滚
    }
}
```

> 这就是声明式事务：**方法上加 `@Transactional`，Spring 通过 AOP 在方法前后织入"开启事务/提交/回滚"**，业务代码零侵入。

### 3.2 应用在类上

```java
@Transactional  // 类上：所有 public 方法都开启事务（可被子方法覆盖）
@Service
public class OrderService { ... }
```

### 3.3 工作原理简述（AOP 代理）

```
调用方 → 调用 proxy.transfer() 
   → TransactionInterceptor 开启事务（绑定连接、关闭自动提交）
   → 执行目标方法 transfer()
       成功 → commit
       抛 RuntimeException → rollback
   → 返回结果给调用方
```

> 关键点：事务是通过**代理（Proxy）** 实现的。调用 `transfer` 时实际调用的是 Spring 生成的代理对象，由代理负责开启/提交/回滚。这直接引出"自调用失效"（第九章）。

---

## 四、@Transactional 属性全解

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Transactional {
    String value() default "";              // 指定事务管理器 Bean 名
    Propagation propagation() default REQUIRED;  // 传播行为
    Isolation isolation() default DEFAULT;   // 隔离级别
    int timeout() default TransactionDefinition.TIMEOUT_DEFAULT; // 超时（秒）
    boolean readOnly() default false;        // 只读
    Class<? extends Throwable>[] rollbackFor() default {};  // 指定回滚异常
    String[] rollbackForClassName() default {};
    Class<? extends Throwable>[] noRollbackFor() default {};  // 指定不回滚异常
    String[] noRollbackForClassName() default {};
}
```

### 4.1 propagation（传播行为）

控制"当前方法的事务如何与已有事务交互"。详见第五章。

### 4.2 isolation（隔离级别）

控制并发事务的可见性。详见第六章。

### 4.3 timeout（超时）

```java
@Transactional(timeout = 3) // 3 秒，超过则回滚并抛 TransactionTimedOutException
```

> 超时从事务开启算起（含传播嵌套的耗时）。只读/短事务建议设合理超时，防止长事务占连接。

### 4.4 readOnly（只读）

```java
@Transactional(readOnly = true)
public List<Order> queryOrders() { ... }
```

> `readOnly=true` 告诉数据库这是只读事务，数据库可做优化（如 MySQL 避免快照开销、只读路由到从库）。**仅查询的方法务必加 readOnly=true**，既优化又明确语义。注意：readOnly 事务中若执行写操作，某些数据库（如 MySQL）会抛异常或忽略。

### 4.5 rollbackFor / noRollbackFor（回滚规则）

```java
// 默认只回滚 RuntimeException 和 Error，不回滚受检异常（Exception 子类但不是 RuntimeException）
@Transactional(rollbackFor = Exception.class) // 任何 Exception 都回滚（常见规范）
public void biz() throws Exception { ... }

@Transactional(noRollbackFor = BusinessException.class) // 指定异常不回滚
public void biz2() { ... }
```

> ⚠️ 这是最高频坑之一：默认规则下，**抛出受检异常（如 IOException、自定义继承 Exception 的异常）不会回滚**。项目规范通常统一 `rollbackFor = Exception.class`。

### 4.6 value（指定事务管理器）

```java
@Transactional("orderTxManager") // 多数据源时指定用哪个事务管理器
public void biz() { ... }
```

---

## 五、事务传播机制（7 种）

传播行为决定：当方法 A（已有事务）调用方法 B（也有 @Transactional）时，B 是加入 A 的事务，还是开新事务，还是无事务运行。

| 传播行为 | 含义 | 是否新建事务 |
|----------|------|--------------|
| `REQUIRED`（默认） | 有则加入，无则新建 | 否（加入已有） |
| `REQUIRES_NEW` | 总是新建，挂起当前 | 是（挂起外层） |
| `SUPPORTS` | 有则加入，无则非事务 | 否 |
| `NOT_SUPPORTED` | 非事务执行，挂起当前 | 否（挂起） |
| `MANDATORY` | 必须在事务中，否则抛异常 | 否 |
| `NEVER` | 必须非事务，否则抛异常 | 否 |
| `NESTED` | 嵌套事务（保存点） | 是（嵌套，依赖 JDBC 保存点） |

### 5.1 REQUIRED（默认，最常用）

```java
@Service
public class OrderService {
    @Autowired AccountService accountService;

    @Transactional
    public void createOrder() {
        orderMapper.insert(order);
        accountService.deduct(); // 加入 createOrder 的事务
    }
}
@Service
public class AccountService {
    @Transactional(propagation = Propagation.REQUIRED) // 默认值，可省略
    public void deduct() { accountMapper.decrease(...); }
}
```

> 结果：`createOrder` 与 `deduct` 在**同一个事务**中。任一处抛异常，整体回滚。

### 5.2 REQUIRES_NEW（独立事务，互不干扰）

```java
@Transactional
public void outer() {
    try {
        inner(); // REQUIRES_NEW
    } catch (Exception e) {
        // inner 的失败不影响 outer 的提交
    }
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void inner() { ... throw new RuntimeException(); }
```

> 关键点：`inner` 用**新事务**执行，外层事务被**挂起**。inner 回滚不影响 outer，outer 回滚也不影响 inner（若 inner 先提交）。典型应用：日志记录/审计必须成功（用 REQUIRES_NEW），即使主业务失败也要留下日志。

### 5.3 NESTED（嵌套事务，保存点回退）

```java
@Transactional
public void outer() {
    outerUpdate();
    try {
        nested(); // NESTED：在外部事务中创建保存点
    } catch (Exception e) {
        // 仅回滚到保存点，outer 其余操作仍可提交
    }
    outerUpdate2();
}

@Transactional(propagation = Propagation.NESTED)
public void nested() { ... throw new RuntimeException(); }
```

> `NESTED` 与 `REQUIRES_NEW` 区别：
> - REQUIRES_NEW：完全独立的新事务，内外互不影响（inner 先提交就提交了）。
> - NESTED：外部事务的子事务，用**保存点（Savepoint）** 实现。嵌套内回滚只回退到保存点，但**外层事务整体提交时嵌套才真正生效；外层回滚则嵌套一并回滚**。
> - 依赖 JDBC 3.0 保存点，并非所有数据源都支持（如某些 JTA 不支持）。

### 5.4 其他四种

```java
@Transactional(propagation = Propagation.SUPPORTS)  // 有事务就加入，没有就以非事务运行（查询方法兼容）
@Transactional(propagation = Propagation.NOT_SUPPORTED) // 强制非事务（挂起外层，用于不需要事务的耗时操作）
@Transactional(propagation = Propagation.MANDATORY) // 必须在事务内调用，否则抛 IllegalTransactionStateException
@Transactional(propagation = Propagation.NEVER)     // 必须非事务内调用，否则抛异常
```

> 记忆口诀：REQUIRED（加入/新建）、REQUIRES_NEW（新开）、NESTED（嵌套保存点）、SUPPORTS（随大流）、MANDATORY（强制要有）、NEVER（强制不要）、NOT_SUPPORTED（挂起不用）。

---

## 六、事务隔离级别与并发问题

### 6.1 并发事务三大问题

| 问题 | 描述 | 后果 |
|------|------|------|
| **脏读** | 读到另一事务**未提交**的数据 | 对方回滚，读到的是脏数据 |
| **不可重复读** | 同一事务内两次读同一行，结果**不同**（被别的事务修改并提交了） | 数据不一致 |
| **幻读** | 同一事务内两次查询同一条件，返回的**行数不同**（别的事务插入/删除了符合条件的行） | 行集合变化 |

> 不可重复读侧重"行内容被改"，幻读侧重"结果集行数变了"。

### 6.2 四种隔离级别

| 隔离级别 | 脏读 | 不可重复读 | 幻读 | 性能 |
|----------|------|------------|------|------|
| `READ_UNCOMMITTED` 读未提交 | ❌ 可能 | ❌ 可能 | ❌ 可能 | 最高 |
| `READ_COMMITTED` 读已提交 | ✅ 避免 | ❌ 可能 | ❌ 可能 | 较高 |
| `REPEATABLE_READ` 可重复读（MySQL 默认） | ✅ | ✅ 避免 | ❌/✅* | 中 |
| `SERIALIZABLE` 串行化 | ✅ | ✅ | ✅ | 最低 |

> \* MySQL 的 `REPEATABLE_READ` 通过 **MVCC + 间隙锁（Next-Key Lock）** 实际也能避免幻读。Oracle 默认 `READ_COMMITTED`，SQL Server 默认 `READ_COMMITTED`。

### 6.3 Spring 中指定隔离级别

```java
@Transactional(isolation = Isolation.REPEATABLE_READ) // 显式指定
public void biz() { ... }
```

> 若不指定（ISOLATION_DEFAULT），使用底层数据库默认隔离级别。

### 6.4 MVCC 简述

MySQL InnoDB 通过 MVCC（多版本并发控制）实现高并发读：每行有隐藏的事务 ID 和回滚指针，读操作读快照，写操作加锁，读写互不阻塞。这是 `READ_COMMITTED`/`REPEATABLE_READ` 高性能的根本原因。

---

## 七、编程式事务

声明式事务不够灵活时（如需要在方法内精确控制事务边界、混用多个事务），用编程式事务。

### 7.1 TransactionTemplate（推荐）

```java
@Autowired
private TransactionTemplate transactionTemplate;

public void biz() {
    transactionTemplate.execute(status -> {
        // 在事务内执行
        orderMapper.insert(order);
        accountMapper.decrease(...);
        return null; // 返回值；抛异常自动回滚
    });
}
```

> `TransactionTemplate` 把"开启/提交/回滚"模板化，业务只需写核心逻辑，比手动用 `PlatformTransactionManager` 简洁安全。

### 7.2 手动 PlatformTransactionManager

```java
@Autowired
private PlatformTransactionManager txManager;

public void manualTx() {
    DefaultTransactionDefinition def = new DefaultTransactionDefinition();
    def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    TransactionStatus status = txManager.getTransaction(def);
    try {
        // 业务操作
        txManager.commit(status);
    } catch (Exception e) {
        txManager.rollback(status);
        throw e;
    }
}
```

### 7.3 声明式 vs 编程式

| 维度 | 声明式（@Transactional） | 编程式（TransactionTemplate） |
|------|--------------------------|-------------------------------|
| 侵入性 | 零侵入（注解） | 代码耦合事务 API |
| 灵活度 | 方法级粒度 | 可在代码任意位置控制 |
| 适用 | 绝大多数业务方法 | 复杂边界、混用多事务 |

---

## 八、事务失效的 10 大场景

这是面试与生产的重中之重。@Transactional 用错就"静默失效"，数据不一致却无报错。

### 场景 1：自调用（同类方法互调）

```java
@Service
public class UserService {
    public void createUser() {
        this.saveUser(); // ❌ 自调用：直接调用 this，绕过代理 → 事务失效
    }
    @Transactional
    public void saveUser() { userMapper.insert(...); }
}
```

> 原因：Spring 事务靠代理，自调用 `this.xxx()` 不经过代理，没有开启事务。解决见第九章。

### 场景 2：方法不是 public

```java
@Transactional
private void save() { ... } // ❌ 非 public，CGLIB 也无法代理（默认只代理 public）
```

> Spring 默认只对 `public` 方法创建事务代理。private/protected 上的 @Transactional 不生效。

### 场景 3：异常被 catch 吞掉

```java
@Transactional
public void biz() {
    try {
        riskyOp();
    } catch (Exception e) {
        log.error("err", e);
        // ❌ 异常被吞，没往外抛 → Spring 感知不到 → 不回滚
    }
}
```

> 修复：catch 中 `throw new RuntimeException(e)` 或 `TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()`。

### 场景 4：抛了受检异常（默认不回滚）

```java
@Transactional // 默认只回滚 RuntimeException
public void biz() throws IOException {  // ❌ 抛受检异常，不回滚
    throw new IOException("x");
}
// 修复：@Transactional(rollbackFor = Exception.class)
```

### 场景 5：数据库引擎不支持

```java
// MySQL MyISAM 引擎不支持事务！改用 InnoDB
// 即使代码正确，MyISAM 表上的事务也完全无效
```

> 确认表引擎为 InnoDB：`SHOW TABLE STATUS;`。

### 场景 6：多线程调用

```java
@Transactional
public void biz() {
    new Thread(() -> {
        // ❌ 新线程没有事务上下文（ThreadLocal 不跨线程）
        jdbcTemplate.update(...); // 不在事务内
    }).start();
}
```

> Spring 事务上下文存在 `ThreadLocal`，跨线程丢失。异步/多线程中的 DB 操作需在新线程内自行开启事务，或用 `@Async` + 事务传播配合。

### 场景 7：使用了错误的事务管理器 / 数据源

```java
// 多数据源时，若 @Transactional 未指定正确的事务管理器，可能作用在错误数据源上
@Transactional("wrongTxManager") // ❌ 用错管理器
public void biz() { ... }
```

### 场景 8：方法用 final / static（CGLIB 限制）

```java
@Transactional
public final void save() { ... } // ❌ final 方法无法被 CGLIB 重写（代理失败）
```

> Spring Boot 2.x 默认用 CGLIB（子类代理），final/static 方法无法被重写代理，事务失效。

### 场景 9：异常类型不在 rollbackFor 内（自定义异常继承错）

```java
class MyException extends Exception {} // 受检异常
@Transactional // 默认不回滚受检异常
public void biz() throws MyException { throw new MyException(); } // ❌ 不回滚
```

### 场景 10：传播行为配置错误

```java
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public void biz() { ... } // ❌ 显式非事务，方法内操作不受事务保护
```

---

## 九、Spring 代理与自调用问题

### 9.1 代理机制

Spring Boot 2.x 起默认使用 **CGLIB**（基于继承的子类代理）；早期或指定 `proxyTargetClass=false` 时用 **JDK 动态代理**（基于接口）。

```java
// CGLIB：生成子类 AccountService$$EnhancerBySpringCGLIB，重写方法织入事务
AccountService proxy = context.getBean(AccountService.class);
// 实际拿到的是代理对象，代理里才有事务逻辑
```

### 9.2 自调用的 3 种解决方案

**方案 A：注入自身（最常见、推荐）**

```java
@Service
public class UserService {
    @Autowired
    private UserService self; // 注入代理后的自己

    public void createUser() {
        self.saveUser(); // ✅ 走代理 → 事务生效
    }
    @Transactional
    public void saveUser() { ... }
}
```

> 注意：用 `@Lazy` 避免循环依赖报错：`@Lazy @Autowired private UserService self;`

**方案 B：用 ApplicationContext 获取代理**

```java
@Autowired
private ApplicationContext applicationContext;
public void createUser() {
    applicationContext.getBean(UserService.class).saveUser(); // ✅ 走代理
}
```

**方案 C：用 AopContext（需开启 exposeProxy）**

```java
// 启动类或配置类：@EnableAspectJAutoProxy(exposeProxy = true)
public void createUser() {
    ((UserService) AopContext.currentProxy()).saveUser(); // ✅
}
```

**方案 D：拆分成两个 Service（最佳实践）**

```java
@Service
public class UserCreator {
    @Autowired UserSaver saver;
    public void createUser() { saver.saveUser(); } // 跨 Bean 调用，走代理
}
@Service
public class UserSaver {
    @Transactional
    public void saveUser() { ... }
}
```

> 推荐**方案 D（职责拆分）**，最符合设计原则；其次是方案 A（自注入）。避免方案 C（侵入式、依赖 AOP 配置）。

---

## 十、事务同步管理器与底层原理

### 10.1 核心类

| 类 | 职责 |
|----|------|
| `@EnableTransactionManagement` | 开启事务管理（Boot 自动开启） |
| `ProxyTransactionManagementConfiguration` | 注册事务 Advisor/Interceptor |
| `TransactionInterceptor` | 事务切面核心，开启/提交/回滚 |
| `TransactionAspectSupport` | 事务处理逻辑基类 |
| `AbstractPlatformTransactionManager` | 事务管理器模板实现 |
| `TransactionSynchronizationManager` | 用 ThreadLocal 绑定当前事务资源（连接） |

### 10.2 事务执行流程（源码级）

```
1. @Transactional 方法被代理拦截
2. TransactionInterceptor.invokeWithinTransaction()
3. 根据 @Transactional 属性构造 TransactionDefinition
4. 调用 PlatformTransactionManager.getTransaction()
   → 从数据源获取 Connection，设置 autoCommit=false
   → 绑定到 TransactionSynchronizationManager（ThreadLocal）
5. 执行目标方法
6. 成功 → commit（connection.commit()，恢复 autoCommit）
7. 抛 RuntimeException/Error（或 rollbackFor 匹配）→ rollback（connection.rollback()）
8. 清理 ThreadLocal，释放连接
```

> 关键点：事务本质是**把 JDBC 连接的 autoCommit 设为 false**，最后统一 commit/rollback。事务上下文通过 `TransactionSynchronizationManager` 的 ThreadLocal 在调用链中传递——这也解释了为什么多线程、自调用会丢失事务（都绕过了 ThreadLocal 传递）。

### 10.3 连接与 MyBatis 的协作

MyBatis 的 `SqlSession` 从 Spring 管理的连接获取。同一事务内多次 Mapper 调用共用**同一个数据库连接**（从 ThreadLocal 取出），保证操作在同一事务中。事务提交/回滚由 Spring 统一控制，MyBatis 不再自行提交。

---

## 十一、多数据源与事务

### 11.1 单数据源事务

单一 `DataSource` + 单一 `DataSourceTransactionManager`，@Transactional 直接生效。

### 11.2 多数据源

需为每个数据源配置独立的事务管理器，并用 `value` 指定。

```java
@Configuration
public class TxConfig {
    @Bean("orderTx")
    public PlatformTransactionManager orderTx(@Qualifier("orderDs") DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }
    @Bean("userTx")
    public PlatformTransactionManager userTx(@Qualifier("userDs") DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }
}

@Service
public class BizService {
    @Transactional("orderTx")
    public void orderBiz() { ... }

    @Transactional("userTx")
    public void userBiz() { ... }
}
```

### 11.3 多数据源跨库事务（分布式事务）

普通 `@Transactional` 只能管一个数据源。跨库一致性需要 **JTA（Atomikos/Bitronix）** 或 **分布式事务方案（见第十二章）**。

---

## 十二、分布式事务简介

单体事务局限：只能保证单个数据库/单个事务管理器内的 ACID。微服务/多库的跨资源一致性需分布式事务。

### 12.1 常见方案

| 方案 | 思想 | 一致性 | 复杂度 |
|------|------|--------|--------|
| **2PC（两阶段提交）** | 协调者统一提交/回滚 | 强一致 | 高，有阻塞 |
| **TCC** | Try-Confirm-Cancel 三阶段 | 最终一致 | 高，需写补偿 |
| **本地消息表** | 本地事务 + 消息表 + 重试 | 最终一致 | 中 |
| **消息队列事务消息** | 半消息 + 确认 | 最终一致 | 中 |
| **Saga** | 长事务拆成子事务 + 补偿链 | 最终一致 | 中高 |
| **Seata AT** | 无侵入自动补偿（基于 undo_log） | 最终一致 | 低（框架托管） |

### 12.2 本地消息表（经典最终一致）

```java
@Transactional // 本地事务：业务 + 消息表写入原子
public void createOrder() {
    orderMapper.insert(order);
    messageMapper.insert(new Message(order.getId(), "PENDING"));
    // 提交后由定时任务/MQ 发送消息，失败重试
}
```

> 思路：把"业务操作"和"发送消息"放进同一个本地事务，保证要么都成功；下游消费失败则不断重试，最终一致。简单可靠，是中小团队首选。

### 12.3 Seata（阿里开源，最常用）

Seata 的 AT 模式对业务代码几乎无侵入：加 `@GlobalTransactional` 即可，框架自动记录 undo_log 实现回滚。适合需要跨服务强管控的场景。

---

## 十三、实战案例

### 案例 1：标准转账（REQUIRED）

```java
@Service
public class TransferService {
    @Autowired AccountMapper accountMapper;

    @Transactional(rollbackFor = Exception.class, timeout = 5, isolation = Isolation.REPEATABLE_READ)
    public void transfer(String from, String to, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("金额非法");
        int r1 = accountMapper.decrease(from, amount);
        int r2 = accountMapper.increase(to, amount);
        if (r1 == 0 || r2 == 0) throw new RuntimeException("账户不存在或余额不足");
    }
}
```

### 案例 2：主业务失败但审计日志必须留存（REQUIRES_NEW）

```java
@Transactional(rollbackFor = Exception.class)
public void placeOrder(Order order) {
    try {
        orderMapper.insert(order);
        deductStock(order);
    } catch (Exception e) {
        auditLog.logFail(order, e.getMessage()); // REQUIRES_NEW，主事务回滚也留痕
        throw e;
    }
}

// 审计服务
@Service
public class AuditLog {
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void logFail(Order o, String msg) { auditMapper.insert(...); }
}
```

### 案例 3：只读查询优化

```java
@Transactional(readOnly = true)
public Page<Order> query(Pageable pageable) {
    return orderRepository.findAll(pageable);
}
// 配合 MyBatis：readOnly 可让连接路由到只读从库，MySQL 也可跳过一致性快照开销
```

### 案例 4：批量导入，失败整体回滚

```java
@Transactional(rollbackFor = Exception.class)
public void batchImport(List<Data> list) {
    for (Data d : list) {
        if (!validate(d)) throw new BizException("校验失败：" + d);
        dataMapper.insert(d);
    }
    // 任一失败 → 全部回滚
}
```

### 案例 5：编程式事务按需控制

```java
public void processWithNested() {
    transactionTemplate.execute(status -> {
        masterOp();
        try {
            transactionTemplate.execute(inner -> { childOp(); return null; });
        } catch (Exception e) {
            status.setRollbackOnly(); // 子失败，标记外层回滚
        }
        return null;
    });
}
```

---

## 十四、最佳实践清单

1. ✅ 始终指定 `rollbackFor = Exception.class`，避免受检异常不回滚。
2. ✅ 只读查询加 `readOnly = true`，提升性能 + 明确语义。
3. ✅ 给事务方法设合理 `timeout`，防止长事务占连接。
4. ✅ 事务方法保持**短小**：只包裹必要的 DB 操作，不要在事务内做远程调用、文件 IO、sleep。
5. ✅ 避免在事务内抛异常又被 catch 吞掉而不回滚。
6. ✅ 自调用问题用"拆 Service"或"自注入"解决，不要依赖 AopContext。
7. ✅ 确认表引擎是 InnoDB，不是 MyISAM。
8. ✅ 不要在 private/final 方法上用 @Transactional。
9. ✅ 不要在事务内起新线程做 DB 操作（事务上下文不跨线程）。
10. ✅ 跨库/跨服务一致性用分布式事务方案（本地消息表/Seata），不要指望单 @Transactional。
11. ✅ 方法命名与事务边界清晰，避免"一个大方法包所有逻辑"的长事务。
12. ✅ 统一异常处理 + 全局异常处理器，确保异常能正确传播到事务拦截器触发回滚。

---

## 十五、20+ 面试题精解

### Q1：Spring 事务的底层实现？
答：基于 AOP + 动态代理（CGLIB/JDK）。在 @Transactional 方法前后织入事务拦截器（TransactionInterceptor），本质是获取连接、关闭 autoCommit、统一 commit/rollback，事务上下文存 ThreadLocal。

### Q2：@Transactional 用在 private 方法上有效吗？
答：无效。Spring 默认只对 public 方法创建事务代理，private/final 方法无法被代理（CGLIB 需重写，private 不可重写）。

### Q3：为什么自调用事务会失效？怎么解决？
答：自调用 `this.xxx()` 不经过代理对象，没有开启事务。解决：拆成两个 Bean 互相调用、自注入代理（`@Lazy @Autowired self`）、用 AopContext.currentProxy()（需 exposeProxy=true）。

### Q4：Spring 事务默认回滚什么异常？
答：默认只回滚 `RuntimeException` 和 `Error`，不回滚受检异常（Exception 非 RuntimeException 子类）。项目规范通常配 `rollbackFor = Exception.class`。

### Q5：异常被 catch 了事务还回滚吗？
答：不回滚。Spring 靠异常传播触发回滚，catch 吞掉后拦截器收不到异常。可在 catch 中 `throw` 或 `setRollbackOnly()`。

### Q6：7 种传播行为分别是什么？
答：REQUIRED（加入/新建）、REQUIRES_NEW（新事务挂起外层）、NESTED（嵌套保存点）、SUPPORTS（随事务有无）、NOT_SUPPORTED（非事务挂起）、MANDATORY（必须事务内否则抛异常）、NEVER（必须非事务否则抛异常）。

### Q7：REQUIRED 和 REQUIRES_NEW 区别？
答：REQUIRED 加入外层事务，同生共死；REQUIRES_NEW 挂起外层、开独立新事务，内外互不影响（内层先提交就生效，外层回滚不影响内层）。

### Q8：REQUIRES_NEW 和 NESTED 区别？
答：REQUIRES_NEW 是完全独立事务，外层回滚不影响内层；NESTED 是外层事务的嵌套子事务（保存点），外层提交才生效，外层回滚则嵌套一并回滚。NESTED 依赖 JDBC 保存点。

### Q9：事务隔离级别有哪几种？MySQL 默认？
答：READ_UNCOMMITTED、READ_COMMITTED、REPEATABLE_READ、SERIALIZABLE。MySQL InnoDB 默认 REPEATABLE_READ（靠 MVCC + 间隙锁也避免了幻读）。

### Q10：脏读、不可重复读、幻读区别？
答：脏读=读未提交；不可重复读=同事务两次读同行结果不同（行被改）；幻读=同事务两次同条件查询结果集行数不同（行被增删）。

### Q11：MySQL 的 REPEATABLE_READ 如何避免幻读？
答：MVCC 保证快照读一致，间隙锁（Next-Key Lock）防止其他事务在范围内插入，从而避免幻读。

### Q12：readOnly=true 有什么用？
答：提示数据库这是只读事务，可做优化（如避免快照、路由到从库）。仅查询务必加，既优化又明确语义。但 readOnly 事务中写操作会报错/被忽略。

### Q13：Spring Boot 如何开启事务？
答：引入 `spring-boot-starter-jdbc`/`mybatis-spring-boot-starter` 后，TransactionAutoConfiguration 自动装配事务管理器并开启 `@EnableTransactionManagement`，直接 @Transactional 即可。

### Q14：事务超时 timeout 怎么算？
答：从事务开启（getTransaction）算起，包括嵌套传播耗时。超时未完成则回滚抛 TransactionTimedOutException。

### Q15：为什么长事务是坏味道？
答：长事务长时间占用数据库连接、持有锁、阻塞其他事务，易引发连接池耗尽、死锁、主从延迟。事务内应避免远程调用/IO/sleep。

### Q16：多线程里的事务为什么失效？
答：事务上下文在 ThreadLocal，新线程不继承，导致新线程的 DB 操作不在事务内。需在新线程内自行开启事务或用 @Async + 传播。

### Q17：MyISAM 表用事务会怎样？
答：MyISAM 不支持事务，@Transactional 完全无效，操作立即生效无法回滚。必须用 InnoDB。

### Q18：@Transactional 可以加在接口上吗？
答：可以（注解可继承），但不推荐——可读性差，且 CGLIB 基于类代理时接口注解不生效。推荐加在具体实现类的 public 方法上。

### Q19：声明式 vs 编程式事务怎么选？
答：绝大多数业务方法用声明式（@Transactional，零侵入）；需要精确控制事务边界、方法内混用多个事务时用编程式（TransactionTemplate）。

### Q20：分布式事务有哪些方案？
答：2PC、TCC、本地消息表、MQ 事务消息、Saga、Seata AT。中小团队首选本地消息表或 Seata，强管控用 2PC/TCC。

### Q21：多个 @Transactional 方法互相调用，异常怎么回滚？
答：取决于传播行为。默认 REQUIRED 同事务，任一抛 RuntimeException 整体回滚；若某方法 REQUIRES_NEW，则它独立回滚不影响外层；若异常被 catch 吞掉则不回滚。

### Q22：Spring 事务和数据库事务的关系？
答：Spring 事务是**编程框架层**的事务管理封装，最终通过 JDBC 调用数据库事务（begin/commit/rollback）。Spring 管理连接、边界和回滚规则，真正执行由数据库完成。

### Q23：TransactionTemplate 有什么优势？
答：模板化事务边界，自动开/提/回，业务只写核心逻辑，比手动 PlatformTransactionManager 简洁且不易出错，适合需要编程式控制时。

### Q24：如何排查事务没回滚的问题？
答：检查——是否自调用、方法是否 public、异常是否被 catch、异常是否在 rollbackFor 内、表是否为 InnoDB、是否跨线程、传播行为是否正确、是否用错事务管理器。

### Q25：为什么建议给 @Transactional 指定 rollbackFor？
答：默认不回滚受检异常，业务常抛受检异常（如自定义 Exception），会导致"以为回滚了实际没回滚"，数据不一致且无报错。统一指定 `Exception.class` 最稳妥。

---

> Spring 事务的核心是"AOP 代理 + 连接绑定 + 回滚规则"。理解代理机制就能解释几乎所有失效场景；理解传播行为就能驾驭复杂业务编排。建议结合源码 `TransactionInterceptor.invokeWithinTransaction` 与 `AbstractPlatformTransactionManager.getTransaction` 深读，并动手复现第 8 章的 10 个失效场景，印象最深刻。
