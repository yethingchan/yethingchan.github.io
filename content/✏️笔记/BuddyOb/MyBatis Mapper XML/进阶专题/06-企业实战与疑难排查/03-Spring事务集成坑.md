# 06-企业实战与疑难排查 · Spring 事务集成坑

> 前置：[[MyBatis Mapper XML/进阶专题/06-企业实战与疑难排查/00-索引]] ｜ 缓存背景：[[MyBatis Mapper XML/进阶专题/03-缓存机制深度/01-一级缓存SqlSession生命周期]]

## Spring 怎么管 MyBatis 的 Session

MyBatis-Spring 用 `SqlSessionTemplate` 代理 `SqlSession`：
- **`@Transactional` 内**：从 `TransactionSynchronizationManager` 取**同一个** `SqlSession`（绑定到线程），一级缓存生效。
- **无事务**：每次 Mapper 调用新建/关闭 `SqlSession`（一级缓存不共享，见 03-01）。

## 坑 1：`@Transactional` 不回滚（默认只回滚 RuntimeException）

```java
@Transactional
public void transfer() throws Exception {
    deduct();
    if (true) throw new Exception("失败");   // 受检异常 → 不回滚！
}
```
- Spring 默认只回滚 **`RuntimeException` / `Error`**。
- 受检异常（`Exception`）**不回滚**。
- 解法：`@Transactional(rollbackFor = Exception.class)`。

## 坑 2：自调用失效（同一个类内调自己的 `@Transactional`）

```java
@Service
public class OrderService {
    public void a() { this.b(); }          // this 调用 → b 的事务不生效！
    @Transactional public void b() { ... }
}
```
- `this.b()` 绕过 Spring 代理 → `@Transactional` 失效。
- 解法：注入自己（`@Lazy` 防循环）或拆到另一个 Bean，或用 AOP 暴露（`AopContext.currentProxy()`）。

## 坑 3：方法 `private` / `final` → 代理不了

- Spring AOP（CGLIB）**不能代理 `private` 和 `final` 方法**。
- `@Transactional` 标在 `private` 方法上**静默失效**。

## 坑 4：事务内查了又改，一级缓存让人"看不出问题"

```java
@Transactional
public void updateName(Long id, String name) {
    User u = userMapper.selectById(id);  // 查（进一级缓存）
    userMapper.updateName(id, name);    // 改（清空一级缓存）
    User u2 = userMapper.selectById(id); // 命中一级缓存 → 拿到"改名后"的值
}
```
- 在同事务内，第二次 `selectById` 走一级缓存，返回的是**内存里已更新的对象**，不是"又查了一次 DB"。
- 调试时若发现"改完马上查是对的"，别误以为 SQL 有问题——是一级缓存。

## 坑 5：长事务占锁 + 慢查询

```java
@Transactional
public void importBig(List<User> list) {
    for (User u : list) userMapper.insert(u);  // 10 万行一个事务
}
```
- 整个方法持有行锁/间隙锁，时间长，其他事务阻塞。
- 解法：**分批 + 每批独立小事务**（见 [[MyBatis Mapper XML/进阶专题/04-批量与性能优化/02-ExecutorType.BATCH手动批处理]]）。

## 坑 6：事务传播行为用错

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void log() { ... }   // 挂起外层事务，自己新开
```
- `REQUIRED`（默认）：加入外层。
- `REQUIRES_NEW`：挂起外层，自己新开（用于"主流程失败但日志必须落库"）。
- `NESTED`：嵌套保存点（部分回滚）。
- 用错会导致"想独立的事务被外层一起回滚"或"想一起回滚的却各自提交"。

## 坑 7：MyBatis 二级缓存 + Spring 事务的脏读

见 [[MyBatis Mapper XML/进阶专题/03-缓存机制深度/04-缓存三大问题与事务脏读]]——跨 Mapper 写同表时二级缓存不失效。

## 结论

- 回滚范围：`rollbackFor = Exception.class`。
- 自调用 / `private` / `final` → 事务失效三兄弟。
- 大批量 → 分批小事务。
- 同事务内"查了又查"是一级缓存在帮忙，不是 SQL 问题。

下一步：[[MyBatis Mapper XML/进阶专题/06-企业实战与疑难排查/04-诡异Bug排查与Locations坑根源]]
