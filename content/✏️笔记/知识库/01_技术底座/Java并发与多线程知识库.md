# Java 并发与多线程（Concurrency & Multithreading）完整知识库

> 目标：系统掌握 Java 多线程机制、线程安全、锁体系、线程池与并发工具类，从"会用 Thread"进阶到"能设计高并发程序"，从容应对面试与生产问题。

---

## 目录

1. [并发基础概念](#一并发基础概念)
2. [创建线程的 4 种方式](#二创建线程的-4-种方式)
   1. [线程生命周期与状态](#三线程生命周期与状态)
3. [线程优先级与守护线程](#四线程优先级与守护线程)
4. [线程安全与 synchronized](#五线程安全与-synchronized)
5. [volatile 关键字](#六volatile-关键字)
6. [原子类 Atomic](#七原子类-atomic)
7. [Lock 接口与 ReentrantLock](#八lock-接口与-reentrantlock)
8. [等待/通知机制（wait/notify）](#九等待通知机制waitnotify)
9. [线程间通信与生产者消费者](#十线程间通信与生产者消费者)
10. [线程池 ThreadPoolExecutor](#十一线程池-threadpoolexecutor)
11. [阻塞队列（BlockingQueue）](#十二阻塞队列blockingqueue)
12. [并发工具类](#十三并发工具类)
13. [Future 与 CompletableFuture](#十四future-与-completablefuture)
14. [死锁与活锁](#十五死锁与活锁)
15. [内存可见性与 happens-before](#十六内存可见性与-happens-before)
16. [最佳实践清单](#十七最佳实践清单)
17. [常见面试题精解](#十八常见面试题精解)

---

## 一、并发基础概念

### 1.1 进程 vs 线程

| 维度   | 进程            | 线程             |
| ---- | ------------- | -------------- |
| 定义   | 程序的一次运行实例     | 进程内的执行单元       |
| 资源   | 独立内存空间        | 共享进程内存         |
| 切换开销 | 大             | 小              |
| 通信   | IPC（管道/消息队列等） | 共享变量（需同步）      |
| 崩溃影响 | 互不影响          | 一个线程崩溃可能拖垮整个进程 |

### 1.2 并发 vs 并行

- **并发（Concurrency）**：同一时间段内交替执行，宏观同时，微观可能串行（单核也能并发）。
- **并行（Parallelism）**：同一时刻真正同时执行（需多核）。

> 并发是"结构"，并行是"执行"。并发程序在多核上才能并行。

### 1.3 为什么需要多线程

- 提高 CPU 利用率（IO 等待时不空转）。
- 提升响应速度（UI 与耗时任务分离）。
- 便于任务建模（生产者/消费者、主从模式）。

### 1.4 线程调度

Java 使用**抢占式调度**：JVM 把线程交给 OS 调度，高优先级更可能被选中，但不保证绝对顺序。线程不能主动控制时间片。

---

## 二、创建线程的 4 种方式

### 方式一：继承 Thread（不推荐）

```java
class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName());
    }
}
new MyThread().start(); // 调用 start，不是 run！
```

> ❌ 缺点：Java 单继承，继承 Thread 后不能再继承其他类；任务与线程耦合。

### 方式二：实现 Runnable（推荐基础方式）

```java
Runnable task = () -> System.out.println("Runnable: " + Thread.currentThread().getName());
new Thread(task, "t1").start();
```

> 优点：任务与线程解耦，可复用；支持lambda。

### 方式三：实现 Callable + Future（有返回值/可抛异常）

```java
Callable<Integer> callable = () -> {
    Thread.sleep(1000);
    return 42;
};
FutureTask<Integer> futureTask = new FutureTask<>(callable);
new Thread(futureTask).start();
System.out.println(futureTask.get()); // 阻塞获取结果：42
```

### 方式四：线程池（生产环境唯一推荐）

```java
ExecutorService pool = Executors.newFixedThreadPool(4);
Future<String> f = pool.submit(() -> "result");
System.out.println(f.get());
pool.shutdown();
```

> **结论**：实际项目一律用线程池，避免频繁创建销毁线程。

---

## 三、线程生命周期与状态

### 3.1 6 种状态（Thread.State）

```
NEW → RUNNABLE ⇄ (BLOCKED / WAITING / TIMED_WAITING) → TERMINATED
```

| 状态              | 触发条件                                                |
| --------------- | --------------------------------------------------- |
| `NEW`           | 创建未 start                                           |
| `RUNNABLE`      | 运行中或就绪等待 CPU（含 OS 的 running + ready）                |
| `BLOCKED`       | 等待进入 synchronized 块（锁被占）                            |
| `WAITING`       | `wait()` / `join()` / `LockSupport.park()` 无时限      |
| `TIMED_WAITING` | `sleep(n)` / `wait(n)` / `join(n)` / 超时 `parkNanos` |
| `TERMINATED`    | run 执行完毕或异常退出                                       |

### 3.2 状态流转示例

```java
Thread t = new Thread(() -> {
    try {
        Thread.sleep(1000); // → TIMED_WAITING
    } catch (InterruptedException e) { }
});
System.out.println(t.getState()); // NEW
t.start();
System.out.println(t.getState()); // RUNNABLE
t.join();
System.out.println(t.getState()); // TERMINATED
```

> ⚠️ 注意：Java 的 `RUNNABLE` 包含了操作系统层面的"运行中"和"就绪"，无法区分。且 `BLOCKED` 仅指等 synchronized 锁，等 `ReentrantLock` 是 `WAITING/TIMED_WAITING`。

---

## 四、线程优先级与守护线程

### 4.1 优先级

```java
Thread t = new Thread(() -> {});
t.setPriority(Thread.MAX_PRIORITY); // 10
t.setPriority(Thread.MIN_PRIORITY); // 1
// 默认 Thread.NORM_PRIORITY = 5
```

> 优先级只是**建议**，取决于 OS，不保证效果。不要依赖优先级控制正确性。

### 4.2 守护线程（Daemon）

```java
Thread t = new Thread(() -> {
    while (true) { /* 后台任务，如 GC、心跳 */ }
});
t.setDaemon(true); // 必须 start 前设置
t.start();
```

> 守护线程：JVM 退出时不会等待它。用户线程全部结束时 JVM 直接退出，守护线程被强制终止。适合后台支撑任务。

---

## 五、线程安全与 synchronized

### 5.1 线程安全定义

多个线程访问同一资源时，不用额外同步也能得到正确结果。反之需要「原子性 + 可见性 + 有序性」三要素保障。

### 5.2 经典问题：卖票

```java
// ❌ 线程不安全
class Ticket implements Runnable {
    private int count = 100;
    public void run() {
        if (count > 0) {            // 多个线程同时通过此判断
            System.out.println(Thread.currentThread().getName() + " 卖了 " + count--);
        }
    }
}
```

### 5.3 synchronized 三种用法

```java
// 1. 修饰实例方法：锁当前对象 this
public synchronized void method1() { /* ... */ }

// 2. 修饰静态方法：锁 Class 对象
public static synchronized void method2() { /* ... */ }

// 3. 修饰代码块：锁指定对象（更灵活、粒度更细）
private final Object lock = new Object();
public void method3() {
    synchronized (lock) { /* 临界区 */ }
}
```

### 5.4 修正卖票（加锁）

```java
class SafeTicket implements Runnable {
    private int count = 100;
    private final Object lock = new Object();
    public void run() {
        while (true) {
            synchronized (lock) {
                if (count <= 0) break;
                System.out.println(Thread.currentThread().getName() + " 卖第 " + count--);
            }
        }
    }
}
```

### 5.5 synchronized 特性与底层

- **原子性**：锁内代码同一时刻仅一个线程执行。
- **可见性**：解锁前把工作内存写回主存。
- **可重入性**：同一线程可重复获取同一把锁（计数器+1）。
- **底层**：JDK 6 后引入**锁升级**：无锁 → 偏向锁 → 轻量级锁（CAS自旋）→ 重量级锁（OS 互斥量）。逃逸分析还支持锁消除、锁粗化。

> 偏向锁：偏向第一个获取它的线程，该线程再次进入无需 CAS。轻量级锁：多线程交替访问时用 CAS 自旋。重量级锁：竞争激烈时挂起线程，开销最大。

---

## 六、volatile 关键字

### 6.1 解决可见性与有序性，不保证原子性

```java
// ❌ 错误：volatile 不保证 i++ 原子
private volatile int i = 0;
// i++ 实际是 读-改-写 三步，volatile 无法保证这三步不被打断
```

```java
// ✅ 正确典型用途：状态标志位
class Worker {
    private volatile boolean running = true;
    void run() { while (running) { /* 工作 */ } }
    void stop() { running = true; running = false; } // 其他线程立刻可见
}
```

### 6.2 双重检查锁单例（volatile 防指令重排）

```java
class Singleton {
    // volatile 防止「分配内存→引用赋值→初始化」重排导致拿到半初始化对象
    private static volatile Singleton instance;
    private Singleton() {}
    public static Singleton getInstance() {
        if (instance == null) {                 // 第一次检查（无锁，性能）
            synchronized (Singleton.class) {
                if (instance == null) {         // 第二次检查（加锁，安全）
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

> `instance = new Singleton()` 不是原子的，包含「分配内存、初始化对象、引用指向内存」三步，JVM 可能重排 2、3。`volatile` 在此保证可见性 + 禁止重排，避免其他线程拿到未初始化完成的对象。

---

## 七、原子类 Atomic

`java.util.concurrent.atomic` 提供无锁原子操作，基于 **CAS（Compare And Swap）**。

### 7.1 核心类

| 类                                                | 说明              |
| ------------------------------------------------ | --------------- |
| `AtomicInteger` / `AtomicLong` / `AtomicBoolean` | 基本类型原子封装        |
| `AtomicReference<V>`                             | 引用类型原子封装        |
| `AtomicIntegerArray`                             | 数组元素原子操作        |
| `AtomicStampedReference`                         | 带版本号（解决 ABA 问题） |
| `LongAdder` / `DoubleAdder`                      | 高并发分段计数（JDK8）   |

### 7.2 使用示例

```java
AtomicInteger count = new AtomicInteger(0);
count.incrementAndGet();          // ++i，原子
count.getAndIncrement();          // i++，原子
count.addAndGet(5);               // +=5，原子
boolean ok = count.compareAndSet(5, 10); // CAS：若为5则设10
```

### 7.3 i++ 正确写法

```java
// 替代 volatile int i; i++;
AtomicInteger i = new AtomicInteger();
i.incrementAndGet(); // 线程安全
```

### 7.4 CAS 原理与 ABA 问题

- **CAS**：比较内存值是否等于预期值，是则更新，否则失败重试（自旋）。
- **ABA 问题**：值从 A→B→A，CAS 误以为没变。解决：加版本号 `AtomicStampedReference`。

```java
AtomicStampedReference<String> ref = new AtomicStampedReference<>("A", 0);
int stamp = ref.getStamp();
ref.compareAndSet("A", "B", stamp, stamp + 1);
```

### 7.5 LongAdder（高并发计数首选）

```java
LongAdder adder = new LongAdder();
// 高并发下内部分多个 cell，减少 CAS 竞争，sum() 求和
adder.increment();
long total = adder.sum();
```

> 相比 `AtomicLong` 单一变量 CAS 竞争，`LongAdder` 用分片思想，写多读少场景性能更好（如统计接口调用次数）。

---

## 八、Lock 接口与 ReentrantLock

`java.util.concurrent.locks.Lock` 是比 `synchronized` 更灵活的锁。

### 8.1 与 synchronized 对比

| 维度    | synchronized   | ReentrantLock             |
| ----- | -------------- | ------------------------- |
| 获取/释放 | 自动             | 手动 `lock()/unlock()`      |
| 可中断   | 否              | `lockInterruptibly()` 可中断 |
| 超时获取  | 否              | `tryLock(timeout)`        |
| 公平锁   | 否              | 可选公平/非公平                  |
| 多条件   | 单一 wait/notify | 多个 `Condition`            |
| 性能    | JDK6 后接近       | 高竞争时略优                    |

### 8.2 基本用法（务必 finally 释放）

```java
ReentrantLock lock = new ReentrantLock();
lock.lock();
try {
    // 临界区
} finally {
    lock.unlock(); // 防止异常导致锁无法释放
}
```

### 8.3 tryLock 与超时

```java
if (lock.tryLock(1, TimeUnit.SECONDS)) {
    try { /* ... */ } finally { lock.unlock(); }
} else {
    // 获取锁超时，走降级/重试逻辑
}
```

### 8.4 公平锁

```java
ReentrantLock fairLock = new ReentrantLock(true); // 公平：按申请顺序获取
```

> 公平锁保证先到先得，但吞吐量低于非公平锁（默认）。多数场景用默认非公平锁即可。

### 8.5 读写锁 ReentrantReadWriteLock

读多写少场景：读共享、写独占。

```java
ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
rw.readLock().lock();    // 多个读线程可同时进入
try { /* 读 */ } finally { rw.readLock().unlock(); }

rw.writeLock().lock();   // 写时独占
try { /* 写 */ } finally { rw.writeLock().unlock(); }
```

> JDK 8 引入 `StampedLock`，提供乐观读，读多写极少时性能更高；但 API 复杂且不可重入，谨慎使用。

---

## 九、等待/通知机制（wait/notify）

### 9.1 方法说明

- `wait()`：释放锁，进入等待（需被 notify 唤醒）。
- `notify()`：随机唤醒一个等待线程。
- `notifyAll()`：唤醒所有等待线程。
- **必须在 synchronized 内调用**，且调用对象为锁对象。

### 9.2 标准范式

```java
// 等待方
synchronized (lock) {
    while (条件不满足) {        // 用 while 而非 if，防止虚假唤醒
        lock.wait();
    }
    执行操作;
}

// 通知方
synchronized (lock) {
    改变条件;
    lock.notifyAll();          // 推荐 notifyAll 而非 notify
}
```

> **虚假唤醒（spurious wakeup）**：线程可能无故被唤醒，必须用 `while` 循环重新检查条件，不能用 `if`。

---

## 十、线程间通信与生产者消费者

### 10.1 用 wait/notify 实现

```java
class Container {
    private final Queue<Integer> queue = new LinkedList<>();
    private final int capacity = 10;
    private final Object lock = new Object();

    void produce(int item) throws InterruptedException {
        synchronized (lock) {
            while (queue.size() == capacity) lock.wait();
            queue.offer(item);
            lock.notifyAll();
        }
    }

    int consume() throws InterruptedException {
        synchronized (lock) {
            while (queue.isEmpty()) lock.wait();
            int v = queue.poll();
            lock.notifyAll();
            return v;
        }
    }
}
```

### 10.2 用 BlockingQueue 实现（推荐）

```java
BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);

// 生产者
new Thread(() -> {
    try { queue.put(1); } catch (InterruptedException e) {}
}).start();

// 消费者
new Thread(() -> {
    try { int v = queue.take(); } catch (InterruptedException e) {}
}).start();
```

> `BlockingQueue` 内部已封装 wait/notify 与条件判断，生产环境首选，避免手写同步出错。

---

## 十一、线程池 ThreadPoolExecutor

### 11.1 为什么用线程池

- 降低线程创建/销毁开销。
- 控制并发数量，防止资源耗尽。
- 提供任务队列、拒绝策略、线程管理。

### 11.2 手动创建（推荐，不用 Executors）

```java
ThreadPoolExecutor pool = new ThreadPoolExecutor(
    2,                      // corePoolSize 核心线程数
    4,                      // maximumPoolSize 最大线程数
    60L, TimeUnit.SECONDS,  // keepAliveTime 空闲线程存活时间
    new ArrayBlockingQueue<>(100),     // 工作队列
    Executors.defaultThreadFactory(),  // 线程工厂
    new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
);
```

### 11.3 执行流程（核心考点）

```
提交任务
  → 核心线程未满？→ 创建核心线程执行
  → 否，队列未满？→ 入队等待
  → 否，线程数 < 最大线程数？→ 创建非核心线程执行
  → 否，触发拒绝策略
```

### 11.4 拒绝策略

| 策略                    | 行为                             |
| --------------------- | ------------------------------ |
| `AbortPolicy`（默认）     | 抛 `RejectedExecutionException` |
| `CallerRunsPolicy`    | 由提交任务的线程自己执行                   |
| `DiscardPolicy`       | 直接丢弃，不抛异常                      |
| `DiscardOldestPolicy` | 丢弃队列最旧任务，重试提交                  |

### 11.5 为什么不推荐 Executors 快速创建

```java
// ❌ 危险：FixedThreadPool / SingleThread 用无界队列，任务积压可能 OOM
Executors.newFixedThreadPool(10);
// ❌ 危险：CachedThreadPool 最大线程数 Integer.MAX，可能创建海量线程
Executors.newCachedThreadPool();
```

> 《阿里巴巴开发手册》明确建议**手动创建 ThreadPoolExecutor**，明确核心参数与队列容量，避免资源耗尽。

### 11.6 关闭线程池

```java
pool.shutdown();        // 平缓：不再接收新任务，执行完已提交任务
// pool.shutdownNow();  // 粗暴：尝试中断所有线程，返回未执行任务
```

### 11.7 合理配置参考

- **CPU 密集型**：核心线程数 ≈ CPU 核数（`Runtime.getRuntime().availableProcessors()`）。
- **IO 密集型**：核心线程数 ≈ CPU 核数 × (1 + 等待时间/计算时间)，通常设大些（如 2N）。
- 任务执行时间短且量大 → 适当增大队列；任务重 → 控制队列、增加线程。

---

## 十二、阻塞队列（BlockingQueue）

| 实现                      | 特点                              |
| ----------------------- | ------------------------------- |
| `ArrayBlockingQueue`    | 有界，数组，构造指定容量                    |
| `LinkedBlockingQueue`   | 默认无界（实际 `Integer.MAX`），链表       |
| `SynchronousQueue`      | 不存储元素，直接交接（CachedThreadPool 用它） |
| `PriorityBlockingQueue` | 无界优先级                           |
| `DelayQueue`            | 延迟出队，元素需实现 `Delayed`            |
| `LinkedTransferQueue`   | 无锁高吞吐                           |

```java
BlockingQueue<String> q = new ArrayBlockingQueue<>(10);
q.put("a");          // 满则阻塞
String s = q.take(); // 空则阻塞
```

---

## 十三、并发工具类

### 13.1 CountDownLatch（倒计时门闩）

等待 N 个线程完成后主线程继续。

```java
int n = 3;
CountDownLatch latch = new CountDownLatch(n);
for (int i = 0; i < n; i++) {
    new Thread(() -> {
        // 子任务
        latch.countDown(); // 计数 -1
    }).start();
}
latch.await(); // 阻塞直到计数归零
System.out.println("所有子任务完成");
```

> 一次性，计数归零后不可重置。

### 13.2 CyclicBarrier（循环栅栏）

等待 N 个线程都到达屏障点再同时放行，可重复使用。

```java
CyclicBarrier barrier = new CyclicBarrier(3, () -> System.out.println("全员到齐，开始！"));
for (int i = 0; i < 3; i++) {
    new Thread(() -> {
        System.out.println("到达");
        barrier.await(); // 等待其他线程
        System.out.println("继续");
    }).start();
}
```

### 13.3 Semaphore（信号量）

控制同时访问的线程数（限流）。

```java
Semaphore sem = new Semaphore(3); // 最多 3 个许可
sem.acquire();
try { /* 受限资源访问 */ } finally { sem.release(); }
```

### 13.4 Exchanger（交换器）

两个线程在同步点交换数据。

```java
Exchanger<String> ex = new Exchanger<>();
new Thread(() -> {
    try { String r = ex.exchange("来自A"); System.out.println(r); }
    catch (InterruptedException e) {}
}).start();
```

---

## 十四、Future 与 CompletableFuture

### 14.1 Future（异步结果占位符）

```java
ExecutorService pool = Executors.newFixedThreadPool(2);
Future<Integer> f = pool.submit(() -> {
    Thread.sleep(1000);
    return 100;
});
// ... 干其他事 ...
Integer r = f.get(); // 阻塞等待结果
```

> `Future` 局限：`get()` 阻塞、无法链式组合、难以异常处理。

### 14.2 CompletableFuture（异步编排，JDK8）

```java
CompletableFuture.supplyAsync(() -> "Hello")      // 异步有返回值
    .thenApply(s -> s + " World")                  // 转换
    .thenAccept(System.out::println)               // 消费
    .join();                                        // 等待完成
```

### 14.3 组合多个异步任务

```java
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> "用户");
CompletableFuture<Integer> f2 = CompletableFuture.supplyAsync(() -> 18);

// 两个都完成后合并
CompletableFuture<String> combined = f1.thenCombine(f2,
    (name, age) -> name + " 年龄 " + age);
System.out.println(combined.join());

// 谁先完成用谁
CompletableFuture<Object> either = f1.applyToEither(f2, x -> x);
```

### 14.4 异常处理

```java
CompletableFuture.supplyAsync(() -> { throw new RuntimeException("err"); })
    .exceptionally(ex -> "降级结果")          // 异常时返回默认值
    .thenAccept(System.out::println);
```

### 14.5 指定线程池

```java
ExecutorService pool = Executors.newFixedThreadPool(4);
CompletableFuture.supplyAsync(() -> "task", pool);
```

> 默认 `ForkJoinPool.commonPool()`，生产环境建议传自定义线程池，避免共用池被阻塞。

---

## 十五、死锁与活锁

### 15.1 死锁四要素

1. **互斥**：资源不可共享。
2. **占有且等待**：持有一资源等待另一资源。
3. **不可抢占**：资源不能被强制剥夺。
4. **循环等待**：A等B、B等A。

### 15.2 死锁示例

```java
Object lockA = new Object(), lockB = new Object();
new Thread(() -> {
    synchronized (lockA) {
        synchronized (lockB) { /* ... */ }
    }
}).start();
new Thread(() -> {
    synchronized (lockB) {          // 顺序相反，可能死锁
        synchronized (lockA) { /* ... */ }
    }
}).start();
```

### 15.3 避免死锁

- 统一加锁顺序（都先 A 后 B）。
- 使用 `tryLock(timeout)` 超时放弃。
- 减少锁粒度/锁数量。
- 使用开放调用（调用外部方法时不持锁）。

### 15.4 活锁与饥饿

- **活锁**：线程不断重试但总失败（互相谦让），看似运行实则无进展。
- **饥饿**：低优先级线程长期得不到 CPU/锁。

---

## 十六、内存可见性与 happens-before

### 16.1 Java 内存模型（JMM）

线程操作共享变量时，从主内存拷贝到工作内存，修改后写回。若无同步，其他线程可能看到旧值。

### 16.2 happens-before 规则（保证可见性的"先行发生"）

- **程序顺序**：单线程内，前面的操作先于后面的。
- **锁规则**：`unlock` 先于后续对同一锁的 `lock`。
- **volatile**：写 volatile 先于后续读。
- **线程启动**：`start()` 先于线程内任何动作。
- **线程终止**：线程内动作先于 `join()` 返回。
- **传递性**：A hb B，B hb C ⇒ A hb C。

> happens-before 是判断"一个线程的修改能否被另一个线程看到"的权威依据。

---

## 十七、最佳实践清单

1. ✅ 始终用线程池，手动创建 `ThreadPoolExecutor`，明确参数。
2. ✅ `ReentrantLock` 必须在 `finally` 中 `unlock()`。
3. ✅ `wait()` 用 `while` 循环检查条件，防虚假唤醒。
4. ✅ `volatile` 只用于状态标志，不替代锁做 `i++`。
5. ✅ 计数用 `AtomicInteger` / `LongAdder`，避免锁。
6. ✅ 单例双重检查锁必须 `volatile`。
7. ✅ 生产者消费者用 `BlockingQueue`，别手写 wait/notify。
8. ✅ `CompletableFuture` 指定自定义线程池。
9. ✅ 加锁顺序一致，避免死锁。
10. ✅ 不要依赖线程优先级和 `Thread.stop()`（已废弃，不安全）。

---

## 十八、常见面试题精解

### Q1：创建线程有几种方式？

答：4 种——继承 Thread、实现 Runnable、实现 Callable+Future、线程池。本质上只有"继承 Thread"和"实现 Runnable/Callable"两类（线程池底层也是 Thread），Callable 有返回值且可抛异常。

### Q2：Runnable 和 Callable 区别？

答：Runnable 的 run 无返回值、不能抛受检异常；Callable 的 call 有返回值（泛型）、可抛异常，配合 Future 获取结果。

### Q3：synchronized 和 volatile 区别？

答：synchronized 保证原子性+可见性+有序性，锁粒度大；volatile 仅保证可见性+有序性（禁止重排），不保证原子性。volatile 适合状态标志，synchronized 适合复合操作。

### Q4：synchronized 和 ReentrantLock 区别？

答：synchronized 自动加解锁、不可中断、非公平；ReentrantLock 手动加解锁（finally 释放）、可中断、可超时、可公平、支持多 Condition。高竞争且需高级特性时选 Lock。

### Q5：volatile 为什么不能保证 i++ 原子性？

答：i++ 是「读-改-写」三步，volatile 只保证可见性，不保证这三步不被其他线程穿插。需用 AtomicInteger 或锁。

### Q6：线程池执行流程？

答：核心线程→工作队列→非核心线程→拒绝策略。详见 11.3。

### Q7：为什么不建议用 Executors 创建线程池？

答：FixedThreadPool/SingleThread 用无界队列可能 OOM；CachedThreadPool 最大线程数无限可能耗尽资源。应手动 new ThreadPoolExecutor 明确容量。

### Q8：什么是死锁？如何避免？

答：见 15 节。避免：统一加锁顺序、tryLock 超时、减少锁粒度。

### Q9：sleep 和 wait 区别？

答：sleep 不释放锁、属于 Thread 类、可任意处调用；wait 释放锁、属于 Object 类、必须在 synchronized 内调用，需 notify 唤醒。

### Q10：CAS 是什么？有什么缺点？

答：Compare-And-Swap，无锁原子操作，失败自旋重试。缺点：ABA 问题（用 AtomicStampedReference 解决）、高竞争下自旋开销大、只能保证一个变量的原子性。

### Q11：CompletableFuture 相比 Future 优势？

答：支持链式编排（thenApply/thenCombine）、异常处理（exceptionally）、多任务组合、指定线程池，避免 Future.get() 阻塞。

### Q12：ThreadLocal 是什么？

答：线程局部变量，每个线程有独立副本，实现线程间数据隔离（常用于管理数据库连接、用户上下文）。注意：线程池中使用后必须 `remove()`，否则线程复用导致数据串号/内存泄漏。

---

> 并发编程贵在"先保证正确，再追求性能"。建议结合 `java.util.concurrent` 包源码（ThreadPoolExecutor、AQS、ConcurrentHashMap）深入阅读 AQS（AbstractQueuedSynchronizer）这一并发基石。
