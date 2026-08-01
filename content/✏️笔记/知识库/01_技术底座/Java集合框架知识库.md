# Java 集合框架（Collections Framework）完整知识库（深度精编版）

> 目标：本知识库不仅讲"怎么用"，更深入"为什么这么设计"——逐行剖析核心源码、揭示扩容与哈希机制、覆盖全部常用集合类型、给出大量由浅入深、可上手的实战案例，并配套 25+ 道源码级面试题。学完可达到面试无死角、生产能选型的 Java 集合高手水平。

---

## 目录（深度版）

1. [集合框架总览与设计哲学](#一集合框架总览与设计哲学)
2. [集合框架继承体系全图](#二集合框架继承体系全图)
3. [Collection 根接口详解](#三collection-根接口详解)
4. [泛型与类型擦除](#四泛型与类型擦除)
5. [List 体系（含源码）](#五list-体系含源码)
   - [5.1 ArrayList 源码级剖析](#51-arraylist-源码级剖析)
   - [5.2 LinkedList 源码级剖析](#52-linkedlist-源码级剖析)
   - [5.3 Vector / Stack](#53-vector--stack)
   - [5.4 CopyOnWriteArrayList](#54-copyonwritearraylist)
   - [5.5 实战：ArrayList 性能优化与陷阱](#55-实战arraylist-性能优化与陷阱)
6. [Set 体系（含源码）](#六set-体系含源码)
   - [6.1 HashSet](#61-hashset)
   - [6.2 LinkedHashSet](#62-linkedhashset)
   - [6.3 TreeSet 与 NavigableSet](#63-treeset-与-navigableset)
   - [6.4 EnumSet / CopyOnWriteArraySet](#64-enumset--copyonwritearrayset)
   - [6.5 IdentityHashMap / WeakHashMap（特殊 Map）](#65-identityhashmap--weakhashmap特殊-map)
7. [Map 体系（含源码）](#七map-体系含源码)
   - [7.1 HashMap 源码级剖析](#71-hashmap-源码级剖析)
   - [7.2 LinkedHashMap](#72-linkedhashmap)
   - [7.3 TreeMap 与红黑树](#73-treemap-与红黑树)
   - [7.4 Hashtable](#74-hashtable)
   - [7.5 ConcurrentHashMap 源码级剖析](#75-concurrenthashmap-源码级剖析)
   - [7.6 实战：多维 Map 与复合 Key 取舍](#76-实战多维-map-与复合-key-取舍)
8. [Queue 与 Deque 体系](#八queue-与-deque-体系)
   - [8.1 PriorityQueue](#81-priorityqueue)
   - [8.2 ArrayDeque 循环数组](#82-arraydeque-循环数组)
   - [8.3 并发队列](#83-并发队列)
   - [8.4 DelayQueue / PriorityBlockingQueue](#84-delayqueue--priorityblockingqueue)
9. [Collections 工具类](#九collections-工具类)
10. [Arrays 工具类](#十arrays-工具类)
11. [Iterator 与 fail-fast/fail-safe](#十一iterator-与-fail-fastfail-safe)
12. [集合与 Stream API 实战](#十二集合与-stream-api-实战)
13. [不可变集合全景](#十三不可变集合全景)
14. [经典实战案例集（分层进阶）](#十四经典实战案例集分层进阶)
15. [性能、内存与选型](#十五性能内存与选型)
16. [常见坑与反模式](#十六常见坑与反模式)
17. [并发集合全景](#十七并发集合全景)
18. [25+ 源码级面试题精解](#十八25-源码级面试题精解)
19. [进阶与现代化（Java 8→21 高阶技巧）](#十九进阶与现代化java-821-高阶技巧)
20. [扩展面试题（现代 API 篇）](#二十扩展面试题现代-api-篇)

---

## 一、集合框架总览与设计哲学

### 1.1 为什么需要集合框架

在集合出现前，程序员只能使用数组，存在三大痛点：

- **长度固定**：数组一旦创建不可扩容。
- **类型单一**：数组只能存同一类型（或 Object，丢失类型安全）。
- **缺少操作**：排序、查找、去重都要手写，且不通用。

集合框架统一了"存储 + 操作"的标准，提供：
- **接口**（List/Set/Map/Queue）——定义契约。
- **实现类**（ArrayList/HashMap/TreeSet…）——提供算法。
- **算法**（Collections 工具类、Stream）——提供通用操作。

### 1.2 设计原则（面试可谈）

1. **面向接口编程**：变量声明用 `List`/`Map`，而非具体类，便于替换实现。
2. **分离接口与实现**：`List` 是接口，`ArrayList`/`LinkedList` 是实现，互不影响演进。
3. **装饰器模式**：缓冲流、`Collections.unmodifiableList`、同步包装都是包装原集合增强功能。
4. **泛型保证类型安全**：JDK 5 引入，编译期检查，避免 `ClassCastException`。
5. **性能与功能权衡**：HashMap 无序但 O(1)，TreeMap 有序但 O(log n)，按需选择。

### 1.3 两大根接口关系

```
Iterable ← Collection ← (List / Set / Queue)
Map（独立，不属于 Collection，但有 entrySet/keySet/values 返回 Collection 视图）
```

> `Map` 不是 `Collection` 的子接口。原因：Map 存的是键值对（两个对象一个关系），语义上不同于单列集合。但 `Map.entrySet()` 返回 `Set<Map.Entry<K,V>>`，借此能与 Collection 体系联动。

---

## 二、集合框架继承体系全图

```
Iterable<E>
 └── Collection<E>
      ├── List<E>
      │    ├── ArrayList
      │    ├── LinkedList
      │    ├── Vector
      │    │    └── Stack
      │    └── CopyOnWriteArrayList
      ├── Set<E>
      │    ├── AbstractSet
      │    │    ├── HashSet
      │    │    │    └── LinkedHashSet
      │    │    └── TreeSet (implements NavigableSet)
      │    ├── EnumSet
      │    └── CopyOnWriteArraySet
      └── Queue<E>
           ├── Deque<E>
           │    ├── ArrayDeque
           │    └── LinkedList
           ├── PriorityQueue (implements NavigableQueue? no, just Queue)
           └── BlockingQueue<E> (concurrent)
                ├── ArrayBlockingQueue
                ├── LinkedBlockingQueue
                ├── PriorityBlockingQueue
                ├── DelayQueue
                ├── SynchronousQueue
                └── LinkedTransferQueue

Map<K,V>
 ├── HashMap
 │    └── LinkedHashMap
 ├── TreeMap (implements NavigableMap)
 ├── WeakHashMap
 ├── IdentityHashMap
 ├── EnumMap
 ├── Hashtable
 │    └── Properties
 └── ConcurrentHashMap
      └── ConcurrentSkipListMap
```

> 记忆技巧：除了 `Map` 和 `Dictionary`（Hashtable 的古老父类），其他单列集合都源自 `Collection`。`NavigableMap`/`NavigableSet` 是在 `Sorted` 基础上增加了反向/范围导航能力。

---

## 三、Collection 根接口详解

### 3.1 核心方法（含默认方法）

| 方法 | 返回 | 说明 |
|------|------|------|
| `boolean add(E e)` | 是否改变集合 | List 总是 true；Set 若重复返回 false |
| `boolean addAll(Collection<? extends E> c)` | 是否改变 | 批量添加 |
| `boolean remove(Object o)` | 是否移除 | 依赖 `equals()`；List 删首个 |
| `default boolean removeIf(Predicate)` | 是否改变 | JDK8，内部用迭代器安全删除 |
| `boolean contains(Object o)` | 是否包含 | 依赖 `equals()` |
| `boolean containsAll(Collection<?>)` | 是否全含 | - |
| `int size()` | 元素数 | - |
| `boolean isEmpty()` | 是否为空 | `size()==0` |
| `void clear()` | void | 清空 |
| `Object[] toArray()` | 数组 | 返回 Object[] |
| `<T> T[] toArray(T[] a)` | 泛型数组 | 推荐 |
| `Iterator<E> iterator()` | 迭代器 | - |
| `default Stream<E> stream()` | 流 | JDK8 |
| `default Stream<E> parallelStream()` | 并行流 | JDK8 |
| `boolean retainAll(Collection<?>)` | 是否改变 | 取交集 |
| `boolean removeAll(Collection<?>)` | 是否改变 | 删差集（删除共有） |
| `default Spliterator<E> spliterator()` | 可分割迭代器 | 并行流底层 |

### 3.2 toArray 的三写法与陷阱

```java
List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));

// ❌ 错误：无参 toArray 返回 Object[]，强转 String[] 抛 ClassCastException
// String[] bad = (String[]) list.toArray();

// ✅ 写法1：传入空数组（JDK 11 起由 JVM 优化，推荐）
String[] arr1 = list.toArray(new String[0]);

// ✅ 写法2：传入精确容量数组（旧版本性能略好，避免反射创建）
String[] arr2 = list.toArray(new String[list.size()]);

// ✅ 写法3：Stream
String[] arr3 = list.stream().toArray(String[]::new);
```

> 现代 JVM 对 `new String[0]` 已做优化，不再反射创建数组，所以**写法1 最简洁且性能不差**，成为主流推荐。

### 3.3 removeIf 源码思想

`removeIf` 内部创建迭代器，循环 `hasNext` + `next`，对满足条件的元素调用 `it.remove()`。它比手写 for-each + remove 安全（后者抛 ConcurrentModificationException），也比下标倒序删除优雅。

```java
// 等价手写（正向迭代器删除，安全）
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    if (it.next().length() > 3) it.remove();
}
// removeIf 一行：
list.removeIf(s -> s.length() > 3);
```

---

## 四、泛型与类型擦除

### 4.1 为什么要有泛型

```java
// 无泛型（JDK4）：类型不安全，需强转
List list = new ArrayList();
list.add("abc");
list.add(123);                 // 编译通过，运行时才发现问题
String s = (String) list.get(1); // ClassCastException

// 有泛型：编译期检查
List<String> list2 = new ArrayList<>();
list2.add(123);                // ❌ 编译错误，提前暴露
```

### 4.2 类型擦除（重点考点）

Java 泛型是**编译期**的，运行时被擦除为 `Object`（或上限类型）。因此：

```java
List<String> a = new ArrayList<>();
List<Integer> b = new ArrayList<>();
System.out.println(a.getClass() == b.getClass()); // true，都是 ArrayList
```

> 推论（坑）：
> - 不能 `new T[]`、不能 `instanceof List<String>`。
> - 重载不能仅靠泛型区分：`void f(List<String>)` 与 `void f(List<Integer>)` 编译失败（擦除后签名相同）。
> - 可用 `List<?>` 通配符、`<? extends T>`（上界，生产者）、`<? super T>`（下界，消费者）。

### 4.3 PECS 原则（Producer Extends, Consumer Super）

```java
// 生产者（取数据出去）→ extends
static <T> void copy(List<? extends T> src, List<? super T> dest) {
    for (T item : src) dest.add(item);
}
// src 是生产者，用 extends；dest 是消费者，用 super
```

> PECS 是《Effective Java》经典原则，理解它才能看懂 `Collections.copy`、`Collections.max` 的方法签名。

---

## 五、List 体系（含源码）

### 5.1 ArrayList 源码级剖析

`ArrayList` 是最常用的集合，底层是**动态扩容的 Object 数组**。

#### 5.1.1 核心字段

```java
// JDK 8 源码节选（概念简化）
transient Object[] elementData; // 真正存数据的数组
private int size;               // 实际元素个数
private static final int DEFAULT_CAPACITY = 10;     // 默认容量
private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {}; // 空数组标记
private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
```

#### 5.1.2 构造器与首次扩容

```java
public ArrayList() {
    this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA; // 懒惰初始化，不立即分配10
}

public boolean add(E e) {
    ensureCapacityInternal(size + 1);  // 确保容量够
    elementData[size++] = e;
    return true;
}

private void ensureCapacityInternal(int minCapacity) {
    if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
        minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity); // 首次 add → 扩容到10
    }
    ensureExplicitCapacity(minCapacity);
}
```

> **关键点**：无参构造器**不立即分配 10 个空间**（JDK 7 会立即分配），而是首次 `add` 时才扩容到 10。这是 JDK 7→8 的优化，避免无谓内存占用。

#### 5.1.3 扩容算法（grow）

```java
private void grow(int minCapacity) {
    int oldCapacity = elementData.length;
    int newCapacity = oldCapacity + (oldCapacity >> 1); // 1.5 倍
    if (newCapacity - minCapacity < 0) newCapacity = minCapacity;
    if (newCapacity - MAX_ARRAY_SIZE > 0) newCapacity = hugeCapacity(minCapacity);
    elementData = Arrays.copyOf(elementData, newCapacity); // 复制旧数组到新数组
}
```

> **扩容代价**：每次扩容都要 `Arrays.copyOf`（本质是 `System.arraycopy`），是 O(n) 操作。因此**预估容量**很重要——若已知要存 1000 个元素，用 `new ArrayList<>(1000)` 可避免约 10 次扩容（10→15→22→33…每次都复制）。

#### 5.1.4 add(int index, E e) 中间插入

```java
public void add(int index, E element) {
    rangeCheckForAdd(index);
    ensureCapacityInternal(size + 1);
    System.arraycopy(elementData, index, elementData, index + 1, size - index); // 后移
    elementData[index] = element;
    size++;
}
```

> 中间插入需要 `System.arraycopy` 把 index 之后的元素整体后移一位，是 O(n)。插入越靠前、数组越大，代价越高。

#### 5.1.5 remove(int index) 删除

```java
public E remove(int index) {
    rangeCheck(index);
    modCount++;
    E oldValue = elementData(index);
    int numMoved = size - index - 1;
    if (numMoved > 0)
        System.arraycopy(elementData, index + 1, elementData, index, numMoved); // 前移
    elementData[--size] = null; // 帮助 GC
    return oldValue;
}
```

> `elementData[--size] = null` 把末尾置 null，让 GC 回收对象，避免"内存泄漏式"引用残留。

#### 5.1.6 复杂度速查

| 操作 | 复杂度 | 原因 |
|------|--------|------|
| `get(i)` | O(1) | 数组随机访问 |
| `set(i,e)` | O(1) | - |
| `add(e)` 尾部 | O(1) 均摊 | 偶尔扩容 O(n) |
| `add(i,e)` 中间 | O(n) | arraycopy 后移 |
| `remove(i)` | O(n) | arraycopy 前移 |
| `remove(Object)` | O(n) | 先遍历找 index 再删 |
| `contains(o)` | O(n) | 线性查找 |

#### 5.1.7 实战：批量初始化最佳实践

```java
// 已知数量：传初始容量，避免反复扩容
List<User> users = new ArrayList<>(10000);
for (int i = 0; i < 10000; i++) users.add(new User(i));

// 由数组转：Arrays.asList 的坑见第十六章
List<String> fromArray = new ArrayList<>(Arrays.asList("a", "b"));
```

---

### 5.2 LinkedList 源码级剖析

底层是**双向链表**，节点结构：

```java
private static class Node<E> {
    E item;
    Node<E> next;  // 后继
    Node<E> prev;  // 前驱
    Node(Node<E> prev, E element, Node<E> next) {
        this.item = element; this.next = next; this.prev = prev;
    }
}
```

#### 5.2.1 add / remove 节点操作

```java
// 在 succ 前插入新节点
void linkBefore(E e, Node<E> succ) {
    Node<E> pred = succ.prev;
    Node<E> newNode = new Node<>(pred, e, succ);
    succ.prev = newNode;
    if (pred == null) first = newNode; else pred.next = newNode;
    size++;
}
```

> 链表插入本身只是改指针 O(1)，但**找到目标节点**需要遍历 O(n)。所以"链表插入快"的前提是"你已经持有节点引用"。

#### 5.2.2 get(int index) 的优化（前后向查找）

```java
Node<E> node(int index) {
    if (index < (size >> 1)) {        // 在前半 → 从头找
        Node<E> x = first;
        for (int i = 0; i < index; i++) x = x.next;
        return x;
    } else {                          // 在后半 → 从尾找
        Node<E> x = last;
        for (int i = size - 1; i > index; i--) x = x.prev;
        return x;
    }
}
```

> 即便如此，`get(i)` 仍是 O(n)，无法媲美 ArrayList 的 O(1)。**随机访问频繁就别用 LinkedList**。

#### 5.2.3 LinkedList 当队列/栈（Deque）

```java
Deque<String> dq = new LinkedList<>();
dq.addLast("a");   // 队尾入
dq.addFirst("b");  // 队头入
String h = dq.pollFirst();  // 队头出 → b
String t = dq.pollLast();   // 队尾出 → a
dq.push("x");      // 栈：头入
dq.pop();          // 栈：头出
```

#### 5.2.4 对比结论

| 场景 | 选谁 |
|------|------|
| 读多、随机访问 | ArrayList（碾压） |
| 频繁头尾增删 | LinkedList / ArrayDeque |
| 频繁中间增删且量小 | 差别不大，ArrayList 更省内存 |
| 持有时刻已知节点 | LinkedList 指针操作 O(1) |

> 实测：在大多数 JVM 上，ArrayList 的 `System.arraycopy`（C 实现，极快）比 LinkedList 的节点遍历+对象分配还快。**LinkedList 在现代几乎只有"队列/栈语义"这一个存在价值**，且常被 `ArrayDeque` 取代。

---

### 5.3 Vector / Stack

`Vector`：JDK 1.0 古老类，方法全 `synchronized`，线程安全但**每个方法都加锁**导致读写都慢，且扩容是 **2 倍**（capacityIncrement 默认 0）。

`Stack`：继承 Vector 实现栈 LIFO，但继承设计糟糕（能调用 Vector 的 insertElementAt 破坏栈语义）。

```java
// 已淘汰，禁止新代码使用
Vector<String> v = new Vector<>();
Stack<String> s = new Stack<>();

// 替代
List<String> safe = Collections.synchronizedList(new ArrayList<>());
Deque<String> stack = new ArrayDeque<>();  // 当作栈
```

> 面试点：为什么不用 Vector？答：锁粒度太粗（方法级）、扩容翻倍浪费、现代有更好替代（CopyOnWriteArrayList / ConcurrentHashMap / ArrayDeque）。

---

### 5.4 CopyOnWriteArrayList

**写时复制（COW）**：修改时复制整个底层数组，读无锁。

```java
public boolean add(E e) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        Object[] newElements = Arrays.copyOf(elements, len + 1); // 复制
        newElements[len] = e;
        setArray(newElements);  // 替换引用
        return true;
    } finally { lock.unlock(); }
}
public E get(int index) { return getArray()[index]; } // 无锁！
```

> 特性：
> - 读绝对无锁、线程安全、迭代器基于快照不抛 ConcurrentModificationException。
> - 写开销大（复制全数组），仅适合**读极多、写极少**（如监听器列表、配置缓存、白名单）。
> - 数据是**最终一致**：写后读可能短暂看到旧值。

---

### 5.5 实战：ArrayList 性能优化与陷阱

```java
// 陷阱1：循环中连续 add 大量元素但没设容量 → 反复扩容
List<Integer> bad = new ArrayList<>();
for (int i = 0; i < 1_000_000; i++) bad.add(i); // 扩容约 20 次，每次 copyOf

// 优化：预设容量
List<Integer> good = new ArrayList<>(1_000_000);
for (int i = 0; i < 1_000_000; i++) good.add(i);

// 陷阱2：subList 是视图（见第十六章）
// 陷阱3：for-each 中 remove → ConcurrentModificationException（见第十一章）
```

---

## 六、Set 体系（含源码）

### 6.1 HashSet

`HashSet` 内部维护一个 `HashMap`，元素作为 **key**，value 是固定空对象 `PRESENT`。

```java
// 源码节选
private transient HashMap<E,Object> map;
private static final Object PRESENT = new Object();

public HashSet() { map = new HashMap<>(); }
public boolean add(E e) { return map.put(e, PRESENT) == null; } // 去重靠 map 的 key
public boolean remove(Object o) { return map.remove(o) == PRESENT; }
public Iterator<E> iterator() { return map.keySet().iterator(); }
```

> 所以 HashSet 的所有特性 = HashMap 对 key 的特性：无序、允许一个 null、O(1)。

#### 6.1.1 去重原理（重点中的重点）

1. 计算 `key.hashCode()` 定位桶。
2. 桶空 → 直接存入。
3. 桶非空 → 遍历桶内节点，调用 `equals()`：
   - `equals` 为 true → 重复，不存（覆盖 value，但 key 不变）。
   - `equals` 为 false → 加入链表/树。
4. 若原对象 `hashCode` 不同，必进不同桶，`equals` 都不会被调用。

```java
class Point {
    int x, y;
    Point(int x, int y) { this.x = x; this.y = y; }
    @Override public boolean equals(Object o) {
        if (!(o instanceof Point)) return false;
        Point p = (Point) o; return x == p.x && y == p.y;
    }
    @Override public int hashCode() {
        return Objects.hash(x, y); // 必须和 equals 一致
    }
}
Set<Point> set = new HashSet<>();
set.add(new Point(1, 2));
set.add(new Point(1, 2)); // equals + hashCode 一致 → 去重，size=1
```

> **铁律**：`equals` 相等的对象，必须 `hashCode` 相等；但 `hashCode` 相等的对象，`equals` 未必相等（哈希冲突是正常现象，靠 `equals` 兜底）。

#### 6.1.2 错误示范：只重写 equals

```java
class BadKey {
    int x;
    BadKey(int x) { this.x = x; }
    @Override public boolean equals(Object o) {
        return o instanceof BadKey && ((BadKey)o).x == x;
    }
    // 未重写 hashCode → 默认 Object.hashCode() 基于内存地址
}
Set<BadKey> set = new HashSet<>();
set.add(new BadKey(1));
set.add(new BadKey(1)); // 两个不同对象地址不同 → hashCode 不同 → 进不同桶 → 都存进去了！
System.out.println(set.size()); // 2（去重失败！）
```

#### 6.1.3 hashCode 计算技巧

```java
// 推荐用 Objects.hash（自动处理 null）
@Override public int hashCode() { return Objects.hash(id, name); }

// 高效写法（避免自动装箱，性能敏感时）
@Override public int hashCode() {
    int result = 17;
    result = 31 * result + id;
    result = 31 * result + (name == null ? 0 : name.hashCode());
    return result;
}
```

> 为什么用 31？31 是奇素数，`31 * i == (i << 5) - i`，JVM 可优化为移位运算；且 31 散列分布好，减少冲突。

---

### 6.2 LinkedHashSet

`HashSet` 子类，额外用双向链表维护**插入顺序**。

```java
Set<String> set = new LinkedHashSet<>();
set.add("c"); set.add("a"); set.add("b");
System.out.println(set); // [c, a, b] 保持插入顺序
```

> 原理：`LinkedHashMap` 的子类，在 HashMap 节点基础上加 before/after 指针。比 HashSet 多维护链表，但插入/查询仍是 O(1)。适合"去重 + 保序"（如热门搜索词去重）。

---

### 6.3 TreeSet 与 NavigableSet

基于红黑树，元素按排序存放。实现 `NavigableSet`，提供丰富的导航方法。

```java
// 自然排序：元素需实现 Comparable
TreeSet<String> t1 = new TreeSet<>();
t1.add("banana"); t1.add("apple"); t1.add("cherry");
System.out.println(t1); // [apple, banana, cherry]

// 定制排序：按长度
TreeSet<String> t2 = new TreeSet<>((a, b) -> Integer.compare(a.length(), b.length()));
t2.add("pp"); t2.add("a"); t2.add("ccc");
System.out.println(t2); // [a, pp, ccc]
```

#### 导航方法（TreeSet / NavigableSet）

```java
TreeSet<Integer> set = new TreeSet<>(Arrays.asList(10, 20, 30, 40, 50));
set.first();                 // 10
set.last();                  // 50
set.lower(25);               // 20（严格小于）
set.floor(20);               // 20（小于等于）
set.higher(25);              // 30（严格大于）
set.ceiling(20);             // 20（大于等于）
set.pollFirst();             // 取并删首
set.pollLast();              // 取并删尾
set.headSet(30);             // [10, 20]（< 30）
set.tailSet(30);             // [30, 40, 50]（>= 30）
set.subSet(20, 40);          // [20, 30]（[from, to)）
set.descendingSet();         // 逆序视图
```

> 注意：TreeSet **不允许 null**（无法比较）。Comparable/Comparator 必须一致且自反、对称、传递，否则破坏树结构。

---

### 6.4 EnumSet / CopyOnWriteArraySet

#### EnumSet（最高效的 Set）

专为枚举设计，底层用**位向量**（long 数组的位运算），空间极小、速度极快。

```java
enum Day { MON, TUE, WED, THU, FRI, SAT, SUN }
EnumSet<Day> weekdays = EnumSet.range(Day.MON, Day.FRI); // 工作日
EnumSet<Day> weekend = EnumSet.of(Day.SAT, Day.SUN);
```

> 当你需要 Set 且元素是枚举时，**永远优先 EnumSet**，不要 HashSet。它是位运算实现，比 HashSet 快得多、省内存。

#### CopyOnWriteArraySet

基于 CopyOnWriteArrayList，去重靠遍历查找（O(n)），写时复制，适合读多写极少并发场景。

---

### 6.5 IdentityHashMap / WeakHashMap（特殊 Map）

#### IdentityHashMap（按引用相等）

用 `==` 判断 key 相等，而非 `equals()`。

```java
IdentityHashMap<String, Integer> map = new IdentityHashMap<>();
String a = new String("k");
String b = new String("k");
map.put(a, 1); map.put(b, 2);
System.out.println(map.size()); // 2（a 和 b 是不同对象，== 不等）
```

> 用途：对象序列化、保持对象身份的缓存（如保存对象到序列化句柄映射）。

#### WeakHashMap（弱引用 key）

key 是**弱引用**，GC 时若 key 只被 WeakHashMap 引用，则被回收，对应条目自动移除。

```java
WeakHashMap<Key, Value> map = new WeakHashMap<>();
Key k = new Key("x");
map.put(k, v);
k = null;          // 去掉强引用
System.gc();       // key 被回收，条目自动清理
```

> 用途：缓存、监听器注册表（防止内存泄漏）。注意：value 也会被这个 key 间接持有，可能造成 value 暂时滞留，需注意。

#### BitSet（位集合，去重/标记神器）

```java
BitSet bits = new BitSet();
bits.set(3); bits.set(7);
bits.set(3); // 重复 set 无效
System.out.println(bits.cardinality()); // 2
System.out.println(bits.get(7));        // true
```

> 海量整数去重/标记时，BitSet 比 HashSet 省几个数量级内存（1 bit 表示一个整数）。局限：仅适用于 0~Integer.MAX 的非负整数。

---

## 七、Map 体系（含源码）

### 7.1 HashMap 源码级剖析

日常最频繁使用的 Map。JDK 8 后为**数组 + 链表 + 红黑树**。

#### 7.1.1 核心字段与常量

```java
static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // 16
static final float DEFAULT_LOAD_FACTOR = 0.75f;
static final int TREEIFY_THRESHOLD = 8;     // 链表转树阈值
static final int UNTREEIFY_THRESHOLD = 6;  // 树转链表阈值
static final int MIN_TREEIFY_CAPACITY = 64;// 树化最小容量
transient Node<K,V>[] table;               // 桶数组
transient int size;
int threshold;                             // 扩容阈值 = capacity * loadFactor
```

#### 7.1.2 hash 计算（精妙设计）

```java
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
// 定位桶下标：table[(n - 1) & hash]，n 为 2 的幂
```

> **为什么高 16 位异或低 16 位？** 当容量较小时（如 16），只有低 4 位参与 `(n-1)&hash` 运算，高位完全浪费，容易冲突。把高 16 位异或到低 16 位，让高位也参与扰动，减少哈希冲突。
>
> **为什么容量必须是 2 的幂？** 因为取模 `hash % n` 等价于 `hash & (n-1)` 当且仅当 n 是 2 的幂。位运算比取模快得多。

#### 7.1.3 put 流程（核心）

```
1. 若 table == null → 扩容（首次 put 初始化为 16）
2. 计算桶下标 i = (n-1) & hash
3. 桶为空 → 直接新建节点放入
4. 桶非空：
   a. 首节点 key 相等 → 覆盖 value
   b. 是红黑树节点 → 树中插入/覆盖
   c. 是链表 → 遍历找相等 key 覆盖；否则尾插新节点
      - 插入后若链表长度 >= 8 且 table.length >= 64 → 树化（转红黑树）
      - 否则若 table.length < 64 → 优先扩容而非树化
5. 若发生覆盖 → 返回旧值
6. 否则 size++，若 size > threshold → resize() 扩容
```

```java
// 链表尾插（JDK 8，避免 JDK 7 头插在并发下成环的死循环问题）
for (int binCount = 0; ; ++binCount) {
    if ((e = p.next) == null) {
        p.next = newNode(hash, key, value, null); // 尾插
        if (binCount >= TREEIFY_THRESHOLD - 1) treeifyBin(tab, hash);
        break;
    }
    if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k))))
        break; // 找到相等 key
    p = e;
}
```

> **JDK 7 vs 8 区别**：JDK 7 用头插法（并发下可能成环导致 HashMap 死循环 CPU 100%）；JDK 8 改为尾插法，且引入红黑树。所以 **JDK 8 的 HashMap 即使并发也只是在逻辑上错乱，不会成环死循环**，但仍非线程安全，必须用 ConcurrentHashMap。

#### 7.1.4 树化与退化

- 链表长度 ≥ 8 且容量 ≥ 64 → 转红黑树（查询 O(n) → O(log n)）。
- 树节点数 ≤ 6 → 退化回链表。
- 为什么是 8？泊松分布下，链表长度达到 8 的概率极低（约 0.00000006），说明哈希设计良好时几乎不会树化；8 是时间/空间权衡点。退化阈值设 6（非 8）是为了避免频繁树化/退化来回震荡。

#### 7.1.5 resize 扩容与精妙 rehash

```java
final Node<K,V>[] resize() {
    Node<K,V>[] oldTab = table;
    int oldCap = (oldTab == null) ? 0 : oldTab.length;
    int oldThr = threshold;
    int newCap, newThr = 0;
    if (oldCap > 0) {
        if (oldCap >= MAXIMUM_CAPACITY) { threshold = Integer.MAX_VALUE; return oldTab; }
        else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY && oldCap >= 16)
            newThr = oldThr << 1; // 容量、阈值都翻倍
    }
    // ... 初次扩容逻辑 ...
    // 重新散列：不需要重新计算 hash！
    // 每个元素要么留在原桶 i，要么移到 i + oldCap
    for (int j = 0; j < oldCap; ++j) {
        Node<K,V> e;
        if ((e = oldTab[j]) != null) {
            oldTab[j] = null;
            if (e.next == null) newTab[e.hash & (newCap - 1)] = e;
            else if (e instanceof TreeNode) ((TreeNode<K,V>)e).split(...);
            else {
                // 低位链：位置不变；高位链：位置 + oldCap
                Node<K,V> loHead = null, loTail = null;
                Node<K,V> hiHead = null, hiTail = null;
                do {
                    if ((e.hash & oldCap) == 0) { /* 低位链 */ }
                    else { /* 高位链：+= oldCap */ }
                } while ((e = e.next) != null);
                if (loTail != null) newTab[j] = loHead;
                if (hiTail != null) newTab[j + oldCap] = hiHead;
            }
        }
    }
}
```

> **扩容 rehash 的精妙**：因为容量是 2 的幂，翻倍后新索引只多了最高位一位。用 `e.hash & oldCap` 判断这一位是 0 还是 1——0 留在原位，1 移到 `原位置 + oldCap`。**无需重新计算 hashCode**，直接拆分链表，高效且 evenly distributed。

#### 7.1.6 get 流程

```
1. 计算桶下标
2. 桶首节点 key 相等 → 返回
3. 是红黑树 → 树中查找
4. 是链表 → 遍历 equals 比较
5. 找不到 → null
```

#### 7.1.7 容量设计实践

```java
// 预期存 100 个元素，负载因子 0.75 → 阈值 75
// 需要容量 ceil(100 / 0.75) = 134 → 向上取整到 2 的幂 = 256
Map<String, String> map = new HashMap<>(256); // 避免扩容

// 或 JDK 提供的方法（注意：tableSizeFor 会算成 >= 期望的最小 2 幂）
// new HashMap<>( (int) Math.ceil(100 / 0.75) ) 仍会被调整为 2 的幂
```

> **最佳实践**：`new HashMap<>(expectedSize)` 传入的其实是"期望容纳的元素数"，构造器内部会向上取整到 2 的幂且满足 `capacity * loadFactor >= expectedSize`。若想精确，传 `(int)(expectedSize / 0.75 + 1)`。

#### 7.1.8 遍历（性能差异）

```java
Map<String, Integer> map = new HashMap<>(Map.of("a",1,"b",2,"c",3));

// ✅ 方式1：entrySet（最快，一次取 k-v）
for (Map.Entry<String, Integer> e : map.entrySet()) {
    System.out.println(e.getKey() + "=" + e.getValue());
}

// ✅ 方式2：forEach + lambda（最简洁）
map.forEach((k, v) -> System.out.println(k + "=" + v));

// ❌ 方式3：keySet + get（慢，每次 get 多一次哈希查找）
for (String k : map.keySet()) System.out.println(k + "=" + map.get(k));
```

#### 7.1.9 现代原子操作（compute / merge / putIfAbsent）

```java
Map<String, Integer> freq = new HashMap<>();
List<String> words = List.of("a","b","a","c","b","a");

// 词频统计：merge 一行
words.forEach(w -> freq.merge(w, 1, Integer::sum));
System.out.println(freq); // {a=3, b=2, c=1}

// 计算（根据旧值算新值）
map.compute("k", (k, v) -> v == null ? 1 : v + 1);

// 不存在才放
map.putIfAbsent("k", createExpensiveValue());

// 分组（多级 Map）：computeIfAbsent 懒建子集合，避免 NPE/判空
Map<String, List<String>> group = new HashMap<>();
words.forEach(w -> group.computeIfAbsent(w.substring(0,1), k -> new ArrayList<>()).add(w));
```

> `merge` / `computeIfAbsent` 是 JDK 8 后写集合代码的"神器"，既简洁又线程不安全场景下逻辑清晰。注意：HashMap 的 computeIfAbsent 在 JDK 8 有个已知 bug（递归映射可能抛 ConcurrentModificationException），JDK 9 已修复，生产用 JDK 9+ 即可。

---

### 7.2 LinkedHashMap

`HashMap` 子类，节点加 `before`/`after` 双向链表，维护**插入顺序**（或访问顺序）。

#### 访问顺序实现 LRU（重点）

```java
class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int maxSize;
    LRUCache(int maxSize) {
        super(16, 0.75f, true); // accessOrder = true → 访问后移到末尾
        this.maxSize = maxSize;
    }
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize; // 超容量删最久未用（链表头）
    }
}
LRUCache<Integer, String> cache = new LRUCache<>(3);
cache.put(1, "A"); cache.put(2, "B"); cache.put(3, "C");
cache.get(1);        // 1 被访问，移到最近
cache.put(4, "D");   // 触发淘汰最久未用（2）
System.out.println(cache.keySet()); // [3, 1, 4]
```

> 这就是 LinkedHashMap 最经典的面试题应用——几十行实现线程不安全的 LRU 缓存。若要线程安全，用 `Collections.synchronizedMap` 包装，或用 Caffeine/Guava Cache 等成熟库。

---

### 7.3 TreeMap 与红黑树

基于**红黑树**（自平衡二叉搜索树），key 必须可比较。

```java
TreeMap<Integer, String> map = new TreeMap<>();
map.put(3, "c"); map.put(1, "a"); map.put(2, "b");
System.out.println(map); // {1=a, 2=b, 3=c} 按键排序

// 范围视图（NavigableMap）
map.headMap(2);       // {1=a}（< 2）
map.tailMap(2);       // {2=b, 3=c}（>= 2）
map.subMap(1, 3);     // {1=a, 2=b}（[from, to)）
map.firstKey();       // 1
map.lastKey();        // 3
map.lowerKey(2);      // 1（严格小于）
map.higherKey(2);     // 3（严格大于）
```

#### 红黑树性质（面试能说）

1. 每个节点非红即黑。
2. 根节点为黑。
3. 红节点的子节点必为黑（不能有连续红节点）。
4. 从任一节点到其所有叶子路径含相同数目的黑节点。
5. 叶子（NIL）为黑。

> 这些性质保证树高 O(log n)，查找/插入/删除都在 O(log n)。TreeMap 的所有操作都基于红黑树的旋转与变色维持平衡。

---

### 7.4 Hashtable

JDK 1.0 古老类，方法全 `synchronized`，**不允许 null key/value**，已被 ConcurrentHashMap 取代，禁止新代码使用。

```java
// 已淘汰
Hashtable<String, Integer> t = new Hashtable<>();
t.put(null, 1); // ❌ NullPointerException
```

---

### 7.5 ConcurrentHashMap 源码级剖析

高并发首选 Map。JDK 8 实现：**数组 + 链表 + 红黑树 + CAS + synchronized 锁单节点**。

#### 7.5.1 与 JDK 7 的区别

| 版本 | 实现 | 锁粒度 |
|------|------|--------|
| JDK 7 | Segment 数组（分段锁，默认 16 段） | 段级（默认 16 并发度） |
| JDK 8 | Node 数组 + CAS + synchronized | 桶级（单节点），更细 |

> JDK 8 抛弃了 Segment，直接用 `synchronized` 锁住冲突的桶头节点，配合 CAS 无锁初始化/扩容，并发度更高。

#### 7.5.2 核心字段

```java
transient volatile Node<K,V>[] table;
private transient volatile Node<K,V>[] nextTable; // 扩容时的新表
private transient volatile int sizeCtl; // 控制状态：负数表示正在初始化/扩容
// sizeCtl:
//  = 0 未初始化
//  > 0 初始化/扩容后的阈值
//  = -1 正在初始化
//  < -1 有 (1 + 扩容线程数) 个线程在扩容
```

#### 7.5.3 put 流程（简化）

```
1. 计算 hash
2. table 为空 → 用 CAS + sizeCtl 初始化（只一个线程成功）
3. 桶为空 → CAS 放入新节点
4. 桶头节点 hash = MOVED（-1）→ 表示正在扩容，当前线程帮着迁移数据
5. 否则 synchronized 锁住桶头节点：
   - 链表：尾插
   - 红黑树：树插入
6. 树化（链表 >= 8）
7. size++（用 baseCount + CounterCell 分段计数，避免 CAS 热点）
8. 超阈值 → transfer 扩容（多线程协助）
```

#### 7.5.4 读为什么无锁

```java
public V get(Object key) {
    Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
    int h = spread(key.hashCode());
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (e = tabAt(tab, (n - 1) & h)) != null) {
        if ((eh = e.hash) == h && ((ek = e.key) == key || (ek != null && key.equals(ek))))
            return e.val;
        else if (eh < 0) return p.find(h, key); // 红黑树/迁移中
        while ((e = e.next) != null) {
            if (e.hash == h && ((ek = e.key) == key || (ek != null && key.equals(ek))))
                return e.val;
        }
    }
    return null;
}
```

> 读无锁靠 `volatile`：Node 的 `val` 和 `next` 是 `volatile`，保证可见性。读操作全程不加锁，性能极高。

#### 7.5.5 不允许 null 的原因

ConcurrentHashMap 的 `get` 无锁，若允许 value 为 null，调用方无法区分"key 不存在返回 null"还是"key 存在但 value 为 null"，在并发下会出错。因此设计上直接禁止 null key/value（区别于 HashMap）。

#### 7.5.6 正确使用原子操作

```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

// ❌ 非原子：检查再操作
if (!map.containsKey("k")) map.put("k", 1); // 并发下可能覆盖/重复创建

// ✅ 原子：putIfAbsent
map.putIfAbsent("k", 1);

// ✅ 原子递增：computeIfPresent / merge
map.merge("k", 1, Integer::sum);

// ✅ 懒加载缓存
map.computeIfAbsent("config", k -> loadConfig(k));
```

#### 7.5.7 高并发计数用 LongAdder 思路

```java
ConcurrentHashMap<String, LongAdder> counters = new ConcurrentHashMap<>();
counters.computeIfAbsent("page_view", k -> new LongAdder()).increment();
```

---

### 7.6 实战：多维 Map 与复合 Key 取舍

#### 场景：按 (城市, 年份) 统计

```java
// 方式1：嵌套 Map（直观，易遍历某城市下所有年份）
Map<String, Map<Integer, Integer>> nested = new HashMap<>();
nested.computeIfAbsent("北京", k -> new HashMap<>())
      .merge(2024, 1, Integer::sum);

// 方式2：复合 Key（省一层 Map，但遍历不方便）
class CityYear {
    String city; int year;
    // 必须正确实现 equals + hashCode
}
Map<CityYear, Integer> flat = new HashMap<>();
flat.merge(new CityYear("北京", 2024), 1, Integer::sum);
```

> 取舍：需要"查某城市全部年份"用嵌套 Map（方式1）；只需精确 (k1,k2) 定位且无需分组遍历用复合 Key（方式2）。方式2 的 key 类要保证 equals/hashCode。

---

## 八、Queue 与 Deque 体系

### 8.1 PriorityQueue

基于**二叉小顶堆**的无界优先级队列。队首永远是最小元素（自然排序或定制 Comparator）。

```java
PriorityQueue<Integer> pq = new PriorityQueue<>(); // 小顶堆
pq.offer(5); pq.offer(1); pq.offer(3);
while (!pq.isEmpty()) System.out.print(pq.poll() + " "); // 1 3 5

PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder()); // 大顶堆
```

> 底层是完全二叉树用数组存储，`poll` 时把堆顶与末位交换再下沉（siftDown），`offer` 时上浮（siftUp），均为 O(log n)。注意：PriorityQueue **不是线程安全**的，并发用 `PriorityBlockingQueue`。

### 8.2 ArrayDeque 循环数组

`ArrayDeque` 基于**循环数组**，既能当队列又能当栈，性能优于 LinkedList 与废弃的 Stack。

```java
Deque<String> dq = new ArrayDeque<>();
dq.addLast("a"); dq.addLast("b");
dq.addFirst("head");      // 头插
System.out.println(dq.poll());      // head（队列：头出）
System.out.println(dq.pop());       // a（栈：头出）

// 容量：默认 16，扩容 2 倍。循环数组利用 head/tail 指针避免整体搬移
```

> 推荐：需要栈/双端队列时一律用 ArrayDeque，不要用 Stack（继承设计缺陷）和 LinkedList（节点开销大）。

### 8.3 并发队列

| 类 | 特点 |
|----|------|
| `ConcurrentLinkedQueue` | 无界、无锁（CAS）、FIFO，高并发首选非阻塞队列 |
| `LinkedBlockingQueue` | 可选有界，链表，两把锁（入队/出队分离） |
| `ArrayBlockingQueue` | 有界、数组、一把锁 + 双条件 |
| `SynchronousQueue` | 不存储元素，直接交接（CachedThreadPool 默认用它） |
| `LinkedTransferQueue` | 无界，支持 transfer（生产者阻塞等消费者） |

### 8.4 DelayQueue / PriorityBlockingQueue

```java
// 延时任务：元素实现 Delayed
class Task implements Delayed {
    long deadline;
    Task(long delayMs) { this.deadline = System.currentTimeMillis() + delayMs; }
    public long getDelay(TimeUnit unit) { return unit.convert(deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS); }
    public int compareTo(Delayed o) { return Long.compare(this.getDelay(TimeUnit.MILLIS), o.getDelay(TimeUnit.MILLIS)); }
}
DelayQueue<Task> queue = new DelayQueue<>();
queue.put(new Task(1000)); // 1 秒后才可取
```

> DelayQueue 基于 PriorityBlockingQueue，按剩余延迟时间排序。常用于定时任务、缓存过期、重试退避。

---

## 九、Collections 工具类

`Collections`（带 s）是集合的静态工具类，注意与接口 `Collection` 区分。

```java
// 排序
Collections.sort(list);
Collections.sort(list, Comparator.reverseOrder());

// 查找
Collections.binarySearch(list, key); // 需先排序
Collections.max(coll); Collections.min(coll);

// 修改
Collections.reverse(list);
Collections.shuffle(list);
Collections.rotate(list, 2);  // 循环移位
Collections.fill(list, obj);
Collections.replaceAll(list, old, new);
Collections.swap(list, i, j);

// 不可变包装（防御性编程）
List<String> ro = Collections.unmodifiableList(new ArrayList<>(list));
// ro.add("x"); // ❌ UnsupportedOperationException

// 同步包装
List<String> sync = Collections.synchronizedList(new ArrayList<>());

// 单元素/空集合
List<String> one = Collections.singletonList("x");
List<String> empty = Collections.emptyList(); // 返回共享空集合，非 null
Set<String> es = Collections.emptySet();
Map<String,Integer> em = Collections.emptyMap();
```

> **返回空集合而非 null**：方法返回集合时优先 `Collections.emptyList()`，避免调用方 NPE，且共享空实例省内存。

---

## 十、Arrays 工具类

`Arrays` 提供数组与集合的桥梁和便利操作。

```java
// 数组 → List（固定大小，不能 add/remove！）
List<String> list = Arrays.asList("a", "b", "c");
// list.add("d"); // ❌ UnsupportedOperationException（底层是定长数组包装）

// 安全转可变 List
List<String> mutable = new ArrayList<>(Arrays.asList("a", "b"));

// 排序
Arrays.sort(arr);
Arrays.sort(arr, Comparator.reverseOrder());

// 并行排序（大数组）
Arrays.parallelSort(arr);

// 二分查找（先排序）
Arrays.binarySearch(arr, key);

// 填充/拷贝
Arrays.fill(arr, 0);
int[] copy = Arrays.copyOf(arr, arr.length * 2);

// 转 Stream
Arrays.stream(arr).forEach(System.out::println);

// 比较（包含 null 安全）
boolean eq = Arrays.equals(a, b);
```

> `Arrays.asList` 返回的是 `java.util.Arrays$ArrayList`（定长内部类），**不是** `java.util.ArrayList`，不能 add/remove，否则抛 `UnsupportedOperationException`。这是高频坑（见第十六章）。

---

## 十一、Iterator 与 fail-fast/fail-safe

### 11.1 Iterator 接口

```java
boolean hasNext();
E next();           // 移到下一位并返回
void remove();      // 删除刚 next 的元素（必须先 next）
default void forEachRemaining(Consumer);
```

### 11.2 fail-fast（快速失败）

`ArrayList`/`HashMap` 等普通集合的迭代器维护 `modCount`（修改次数）。迭代期间若集合被结构性修改（非迭代器自身 remove），`modCount` 与迭代器记录的 `expectedModCount` 不符 → 抛 `ConcurrentModificationException`。

```java
List<String> list = new ArrayList<>(List.of("A","B","C"));

// ❌ for-each 中直接 remove → ConcurrentModificationException
// for (String s : list) if (s.equals("B")) list.remove(s);

// ✅ Iterator.remove
Iterator<String> it = list.iterator();
while (it.hasNext()) if (it.next().equals("B")) it.remove();

// ✅ removeIf（内部用迭代器）
list.removeIf(s -> s.equals("B"));
```

> **陷阱**：单线程 for-each 删除也会触发 fail-fast！不是只有多线程才抛。for-each 底层就是 Iterator，但隐藏了迭代器，无法调用 `it.remove()`，只能靠集合自身 remove → modCount 变化 → 下次 next 检查抛异常。

### 11.3 fail-safe（安全失败）

`CopyOnWriteArrayList`、`ConcurrentHashMap` 的迭代器基于**快照/弱一致性**，不抛异常，但可能读到旧数据。

```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>(List.of("A","B"));
for (String s : list) {
    list.add("C"); // 安全：创建新数组快照，旧迭代器不受影响
}
System.out.println(list); // [A, B, C]
```

### 11.4 ListIterator 双向迭代

```java
ListIterator<String> lit = list.listIterator();
while (lit.hasNext()) lit.next();          // 移到末尾
while (lit.hasPrevious()) System.out.println(lit.previous()); // 倒序
lit.add("X");                              // 迭代中插入
lit.set("Y");                              // 迭代中替换
```

---

## 十二、集合与 Stream API 实战

### 12.1 分组聚合

```java
List<Student> students = List.of(
    new Student("Tom", "Math", 90),
    new Student("Jack", "Math", 85),
    new Student("Lucy", "English", 95)
);

// 按科目分组求平均分
Map<String, Double> avg = students.stream().collect(
    Collectors.groupingBy(Student::getSubject, Collectors.averagingInt(Student::getScore))
);

// 按科目分组取姓名列表
Map<String, List<String>> names = students.stream().collect(
    Collectors.groupingBy(Student::getSubject,
        Collectors.mapping(Student::getName, Collectors.toList()))
);

// 分片：及格/不及格
Map<Boolean, List<Student>> pass = students.stream().collect(
    Collectors.partitioningBy(s -> s.getScore() >= 60)
);
```

### 12.2 多级分组与下游收集

```java
// 按城市分组，再按年级分组，再统计人数
Map<String, Map<String, Long>> result = students.stream().collect(
    Collectors.groupingBy(Student::getCity,
        Collectors.groupingBy(Student::getGrade, Collectors.counting()))
);
```

### 12.3 集合转 Map（注意 key 冲突）

```java
// 错误：key 重复抛 IllegalStateException
// Map<String, Student> m = students.stream().collect(toMap(Student::getName, s -> s));

// 正确：提供 merge 函数处理冲突，保留第一个
Map<String, Student> m = students.stream().collect(
    Collectors.toMap(Student::getName, s -> s, (v1, v2) -> v1)
);
```

### 12.4 并行流谨慎使用

```java
long count = list.parallelStream().filter(s -> s.length() > 3).count();
```

> 并行流适合 CPU 密集、大数据量。共享可变状态、IO、小数据量时反而更慢，且默认用通用 ForkJoinPool 会阻塞其他并行任务。需要时用 `parallelStream` 自定义 ForkJoinPool。

---

## 十三、不可变集合全景

不可变集合创建后不可改，用于常量配置、防御性拷贝、函数返回值。

```java
// JDK 9+：List/Set/Map.of（最简洁，真正不可变，不允许 null）
List<String> list = List.of("A", "B", "C");
Set<String> set = Set.of("A", "B");
Map<String, Integer> map = Map.of("a", 1, "b", 2);
// Map.of 最多 10 对，更多用 Map.ofEntries
Map<String, Integer> m2 = Map.ofEntries(
    Map.entry("a", 1), Map.entry("b", 2) /* ... */);

// JDK 8 及更早：Collections.unmodifiableXXX
List<String> ro = Collections.unmodifiableList(new ArrayList<>(src));
// ⚠️ 只是包装，原集合仍可改，导致"视障式"不可变

// Java 9+ 也可用 List.copyOf / Set.copyOf / Map.copyOf
List<String> copy = List.copyOf(src); // 浅拷贝 + 不可变
```

> `List.of` 创建的集合**真正不可变**（任何修改抛 UnsupportedOperationException），且**不允许 null**。`Collections.unmodifiableList` 只是包装，原集合仍可改。

---

## 十四、经典实战案例集（分层进阶）

### 案例 1：词频统计（3 种写法递进）

```java
List<String> words = List.of("apple","banana","apple","cherry","banana","apple");

// 初级：HashMap + getOrDefault
Map<String, Integer> m1 = new HashMap<>();
for (String w : words) m1.put(w, m1.getOrDefault(w, 0) + 1);

// 中级：merge 一行
Map<String, Integer> m2 = new HashMap<>();
words.forEach(w -> m2.merge(w, 1, Integer::sum));

// 高级：并发安全 + 流式
ConcurrentHashMap<String, Integer> m3 = new ConcurrentHashMap<>();
words.parallelStream().forEach(w -> m3.merge(w, 1, Integer::sum));
```

### 案例 2：Top N 高频词（最小堆）

```java
public static List<Map.Entry<String, Integer>> topN(Map<String, Integer> freq, int n) {
    PriorityQueue<Map.Entry<String, Integer>> minHeap =
        new PriorityQueue<>(Comparator.comparingInt(Map.Entry::getValue));
    for (var e : freq.entrySet()) {
        minHeap.offer(e);
        if (minHeap.size() > n) minHeap.poll();
    }
    List<Map.Entry<String, Integer>> res = new ArrayList<>(minHeap);
    res.sort((a, b) -> b.getValue() - a.getValue()); // 降序输出
    return res;
}
```

### 案例 3：两个集合的交集/并集/差集

```java
Set<String> a = new HashSet<>(List.of("1","2","3","4"));
Set<String> b = new HashSet<>(List.of("3","4","5","6"));

// 交集
Set<String> inter = new HashSet<>(a); inter.retainAll(b); // [3, 4]

// 并集
Set<String> union = new HashSet<>(a); union.addAll(b);    // [1,2,3,4,5,6]

// 差集 a-b（在 a 不在 b）
Set<String> diff = new HashSet<>(a); diff.removeAll(b);    // [1, 2]

// Stream 写法（不修改原集合）
Set<String> inter2 = a.stream().filter(b::contains).collect(Collectors.toSet());
```

### 案例 4：用 HashMap 实现 Trie（前缀树）

```java
class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    boolean isEnd;
}
class Trie {
    TrieNode root = new TrieNode();
    void insert(String word) {
        TrieNode cur = root;
        for (char c : word.toCharArray()) cur = cur.children.computeIfAbsent(c, k -> new TrieNode());
        cur.isEnd = true;
    }
    boolean startsWith(String prefix) {
        TrieNode cur = root;
        for (char c : prefix.toCharArray()) {
            cur = cur.children.get(c);
            if (cur == null) return false;
        }
        return true;
    }
    boolean search(String word) {
        TrieNode cur = root;
        for (char c : word.toCharArray()) {
            cur = cur.children.get(c);
            if (cur == null) return false;
        }
        return cur.isEnd;
    }
}
```

> Trie 用 HashMap 存子节点，适合自动补全、敏感词过滤、前缀统计。若字符集固定（如仅小写字母），可用数组 `TrieNode[] children = new TrieNode[26]` 替代 HashMap 提升性能。

### 案例 5：用集合实现图（邻接表）

```java
class Graph {
    private Map<Integer, List<Integer>> adj = new HashMap<>();
    void addEdge(int u, int v) {
        adj.computeIfAbsent(u, k -> new ArrayList<>()).add(v);
    }
    List<Integer> neighbors(int u) { return adj.getOrDefault(u, List.of()); }

    // 广度优先遍历（用队列）
    void bfs(int start) {
        Set<Integer> visited = new HashSet<>();
        Queue<Integer> q = new LinkedList<>();
        q.offer(start); visited.add(start);
        while (!q.isEmpty()) {
            int cur = q.poll();
            for (int nxt : neighbors(cur))
                if (visited.add(nxt)) q.offer(nxt); // add 返回是否新加入（去重）
        }
    }
}
```

### 案例 6：LRU 缓存（已见 7.2，线程安全增强版）

```java
// 用 Collections.synchronizedMap 包装 LinkedHashMap（简单线程安全）
Map<String, String> safeLRU = Collections.synchronizedMap(new LRUCache<>(100));
// 注意：synchronizedMap 的迭代仍需外部同步；高并发建议 Caffeine
```

### 案例 7：用 BitSet 做大整数去重（内存利器）

```java
// 10 亿个 [0, 1e9] 内的整数去重，用 BitSet 仅约 125MB
BitSet used = new BitSet();
for (int id : ids) used.set(id); // 自动去重
for (int i = used.nextSetBit(0); i >= 0; i = used.nextSetBit(i + 1)) {
    System.out.println(i); // 遍历存在的整数
}
```

> 对比：若用 HashSet<Integer> 存 10 亿个 int，每个 Integer 对象约 16 字节 + 指针 + 哈希桶开销，内存超过数 GB。BitSet 仅 1 bit/整数，差距上百倍。

### 案例 8：用 EnumMap 优化多分支

```java
enum Status { NEW, RUNNING, DONE, ERROR }
// EnumMap 用数组存储（key 是枚举序数），比 HashMap 快且无哈希计算
Map<Status, String> desc = new EnumMap<>(Status.class);
desc.put(Status.NEW, "新建"); desc.put(Status.DONE, "完成");
```

### 案例 9：分组并求 Top K per group

```java
// 每个部门取工资 Top 2
Map<String, List<Employee>> top2ByDept = employees.stream()
    .collect(Collectors.groupingBy(Employee::getDept,
        Collectors.collectingAndThen(
            Collectors.toList(),
            list -> list.stream()
                .sorted(Comparator.comparingInt(Employee::getSalary).reversed())
                .limit(2).collect(Collectors.toList())
        )
    ));
```

### 案例 10：频率稳定 Top K（LFU 思路）

```java
// 简化版：用两个 Map（key→freq, freq→keys）实现 LFU
class LFUCache {
    private final int capacity;
    private int minFreq = 0;
    private final Map<Integer, Integer> keyToVal = new HashMap<>();
    private final Map<Integer, Integer> keyToFreq = new HashMap<>();
    private final Map<Integer, LinkedHashSet<Integer>> freqToKeys = new HashMap<>();

    LFUCache(int capacity) { this.capacity = capacity; }

    public int get(int key) {
        if (!keyToVal.containsKey(key)) return -1;
        incFreq(key);
        return keyToVal.get(key);
    }
    public void put(int key, int value) {
        if (capacity <= 0) return;
        if (keyToVal.containsKey(key)) { keyToVal.put(key, value); incFreq(key); return; }
        if (keyToVal.size() >= capacity) removeMinFreqKey();
        keyToVal.put(key, value);
        keyToFreq.put(key, 1);
        freqToKeys.computeIfAbsent(1, k -> new LinkedHashSet<>()).add(key);
        minFreq = 1;
    }
    private void incFreq(int key) {
        int f = keyToFreq.get(key);
        keyToFreq.put(key, f + 1);
        freqToKeys.get(f).remove(key);
        freqToKeys.computeIfAbsent(f + 1, k -> new LinkedHashSet<>()).add(key);
        if (freqToKeys.get(f).isEmpty() && f == minFreq) minFreq++;
    }
    private void removeMinFreqKey() {
        LinkedHashSet<Integer> keys = freqToKeys.get(minFreq);
        int dead = keys.iterator().next();
        keys.remove(dead);
        keyToVal.remove(dead);
        keyToFreq.remove(dead);
    }
}
```

> 这是面试高频的 LFU 缓存，用 3 个 HashMap + LinkedHashSet 维护频率链表，体现对集合组合的熟练度。

---

## 十五、性能、内存与选型

### 15.1 时间复杂度速查

| 集合 | 查询 | 头插 | 尾插 | 中间插 | 删除 |
|------|------|------|------|--------|------|
| ArrayList | O(1) | O(n) | O(1)* | O(n) | O(n) |
| LinkedList | O(n) | O(1) | O(1) | O(n) | O(n) |
| HashSet/HashMap | O(1) | - | - | - | O(1) |
| TreeSet/TreeMap | O(log n) | - | - | - | O(log n) |
| PriorityQueue | O(n)** | - | O(log n) | - | O(log n) |

> *ArrayList 均摊 O(1)；**PriorityQueue 查指定元素需遍历 O(n)，仅队首 O(1)。

### 15.2 内存占用对比（粗略，每元素额外开销）

- `ArrayList`：4 字节引用数组 + 扩容余量。
- `LinkedList`：每个节点约 24~40 字节（prev/next/item + 对象头）。
- `HashMap`：数组 + 节点（约 32~48 字节/条目）+ 负载因子余量。
- `EnumMap`：仅数组，`<1 对象/条目`。
- `BitSet`：1 bit/整数。

> 结论：**元素数量巨大时，LinkedList 比 ArrayList 更费内存**；EnumMap 比 HashMap 省得多。

### 15.3 自动装箱性能陷阱

```java
// ❌ 海量 int 装箱成 Integer，GC 压力大
List<Integer> list = new ArrayList<>();
for (int i = 0; i < 10_000_000; i++) list.add(i); // 创建千万个 Integer 对象

// 优化：用原始类型集合库（如 TIntArrayList / Eclipse Collections / fastutil）
// 或 primitive 数组，避免装箱
```

> 高频计数/统计场景，考虑 `AtomicIntegerArray`、`int[]`、`LongAdder`，或第三方原始类型集合（Eclipse Collections、fastutil、Koloboke）以消除装箱开销。

### 15.4 选型决策树

```
需要键值对？
├─ 无序高并发 → ConcurrentHashMap
├─ 按键排序/范围 → TreeMap
├─ 保持插入/访问顺序 → LinkedHashMap
└─ 普通 → HashMap

需要去重？
├─ 排序 → TreeSet
├─ 保序 → LinkedHashSet
├─ 枚举 → EnumSet（最快）
└─ 普通 → HashSet

需要有序列表？
├─ 读多写少随机访问 → ArrayList
└─ 头尾频繁增删 → ArrayDeque

需要队列/栈？
├─ 栈/双端 → ArrayDeque
├─ 优先级 → PriorityQueue
└─ 阻塞 → ArrayBlockingQueue
```

---

## 十六、常见坑与反模式

### 坑 1：for-each 中删除元素

```java
// ❌ 抛 ConcurrentModificationException
for (String s : list) if (s.equals("B")) list.remove(s);
// ✅ 用 removeIf 或 Iterator.remove
list.removeIf(s -> s.equals("B"));
```

### 坑 2：Arrays.asList 后 add/remove

```java
List<String> list = Arrays.asList("a", "b");
// list.add("c"); // ❌ UnsupportedOperationException（定长内部类）
List<String> mutable = new ArrayList<>(Arrays.asList("a", "b")); // ✅
```

### 坑 3：Integer 缓存池与 == 

```java
Integer a = 127, b = 127; System.out.println(a == b);   // true（缓存 [-128,127]）
Integer c = 128, d = 128; System.out.println(c == d);   // false（超出缓存，新对象）
// 比较值必须用 equals，不要用 ==
```

### 坑 4：HashMap 可变对象作 key

```java
class MutableKey { int id; /* 无 final，可改 */ }
Map<MutableKey, String> map = new HashMap<>();
MutableKey k = new MutableKey(1);
map.put(k, "v");
k.id = 2;                 // 改了 key 的 hashCode
map.get(k);               // null！因为 key 已被移到别的桶，找不到
```

> key 必须是**不可变对象**（String/Integer 都不可变）。自定义 key 用 final 字段 + 正确 equals/hashCode。

### 坑 5：map.get(key) 返回 null 后自动拆箱

```java
Map<String, Integer> map = new HashMap<>();
int v = map.get("missing"); // ❌ NullPointerException（null 拆箱）
Integer v2 = map.get("missing"); // OK，但后续注意 NPE
int v3 = map.getOrDefault("missing", 0); // ✅
```

### 坑 6：subList 是视图（修改影响原集合 + 并发修改异常）

```java
List<Integer> list = new ArrayList<>(List.of(1,2,3,4,5));
List<Integer> sub = list.subList(1, 4); // [2,3,4] 视图
sub.set(0, 999);
System.out.println(list); // [1, 999, 3, 4, 5] 原集合被改！
list.add(6);
// sub.get(0); // ❌ ConcurrentModificationException
// 需要独立副本：new ArrayList<>(list.subList(...))
```

### 坑 7：Collections.synchronizedList 迭代需外部同步

```java
List<String> sync = Collections.synchronizedList(new ArrayList<>());
// 迭代仍需同步，否则并发修改抛异常
synchronized (sync) {
    for (String s : sync) System.out.println(s);
}
```

### 坑 8：PriorityQueue 非线程安全

```java
// 多线程用 PriorityQueue 会数据错乱，应改用 PriorityBlockingQueue
```

### 坑 9：初始化容量算错导致频繁扩容

```java
// 想存 100 个，传 100 不够（阈值 75 就扩容），应传 (int)(100/0.75)+1 = 134
Map<String, String> map = new HashMap<>(134);
```

### 坑 10：把 List 当去重用

```java
// 需要去重却用 List，应改用 Set
List<String> dup = new ArrayList<>(Arrays.asList("a","a","b")); // 有重复
Set<String> uniq = new HashSet<>(dup); // 去重
```

---

## 十七、并发集合全景

| 需求 | 推荐 |
|------|------|
| 线程安全 List（读多写少） | `CopyOnWriteArrayList` |
| 线程安全 List（通用） | `Collections.synchronizedList` |
| 线程安全 Set | `CopyOnWriteArraySet` / `Collections.synchronizedSet` |
| 线程安全 Map（高频读写） | `ConcurrentHashMap` |
| 线程安全 Map（有序） | `ConcurrentSkipListMap` |
| 线程安全 Map（低频） | `Collections.synchronizedMap` |
| 阻塞队列 | `ArrayBlockingQueue` / `LinkedBlockingQueue` |
| 无锁高并发队列 | `ConcurrentLinkedQueue` |
| 延时队列 | `DelayQueue` |
| 高并发计数 | `LongAdder` / `AtomicLong` |

> 并发集合核心思想：**缩小锁粒度**（ConcurrentHashMap 锁桶）、**写时复制**（COW）、**CAS 无锁**（原子类、ConcurrentLinkedQueue）、**分段计数**（LongAdder）。

---

## 十八、25+ 源码级面试题精解

### Q1：ArrayList 和 LinkedList 区别？
答：底层数组 vs 双向链表。ArrayList 随机访问 O(1)、中间插入删除 O(n)（arraycopy）；LinkedList 插入删除节点本身 O(1) 但定位 O(n)。读多写少用 ArrayList，频繁头尾操作/队列语义用 ArrayDeque/LinkedList。

### Q2：ArrayList 如何扩容？扩容倍数是多少？
答：默认空数组，首次 add 扩到 10；之后 `newCapacity = old + (old>>1)` 即 1.5 倍；超过 Integer.MAX_VALUE-8 用 hugeCapacity；每次扩容复制数组 O(n)。

### Q3：为什么说 ArrayList 无参构造不立即分配容量？
答：JDK 8 优化，初始指向共享空数组 `DEFAULTCAPACITY_EMPTY_ELEMENTDATA`，首次 add 才扩到 10，避免无意义内存占用（对比 JDK 7 立即分配 10）。

### Q4：HashMap 底层结构？
答：JDK 8 数组 + 链表 + 红黑树。容量 2 的幂，冲突少链表、≥8 且容量≥64 转红黑树、≤6 退化链表。

### Q5：HashMap 为什么容量是 2 的幂？
答：取模 `hash % n` 等价于 `hash & (n-1)` 当 n 是 2 的幂，位运算更快；且扩容 split 时只需看一位即可分桶。

### Q6：HashMap 的 hash() 为什么高 16 位异或低 16 位？
答：容量小时只有低位参与 `(n-1)&hash`，高位浪费易冲突。扰动函数让高位参与，减少冲突。

### Q7：HashMap 为什么负载因子是 0.75？
答：时间与空间权衡。太高（如 0.95）冲突多、链表长；太低（如 0.5）空间浪费、扩容频繁。0.75 是泊松分布下冲突概率与空间利用率的平衡点。

### Q8：HashMap 为什么树化阈值是 8？
答：泊松分布下链表长度达 8 概率约 6e-8，几乎不发生，说明哈希良好。8 是查询从 O(8) 退化到 O(log8) 的划算转折点；退化阈值 6 避免震荡。

### Q9：HashMap put 流程？
答：见 7.1.3。计算 hash → 定位桶 → 空则直接放；非空则首节点比较、树插入或链表尾插（超 8 且容量≥64 树化）；覆盖返回旧值，新增 size++ 超阈值扩容。

### Q10：JDK 7 和 JDK 8 的 HashMap 有何区别？
答：JDK 7 数组+链表、头插法（并发可能成环死循环）；JDK 8 引入红黑树、改为尾插法（不成环）、扩容 split 用 `(e.hash & oldCap)` 判断高低位、方法更清晰。

### Q11：HashMap 扩容时如何 rehash？为什么不用重新计算 hashCode？
答：容量翻倍后新索引只多最高位一位，用 `e.hash & oldCap` 判断：0 留原位，1 移到 `原位置+oldCap`，直接拆分链表，无需重算 hashCode，高效且均匀。

### Q12：为什么重写 equals 必须重写 hashCode？
答：HashMap 用 hashCode 定位桶、equals 判相等。只重写 equals 会让相等对象进不同桶，去重失败。约定：equals 相等 → hashCode 必相等。

### Q13：HashMap 和 Hashtable 区别？
答：HashMap 非线程安全、允许 null 键值、效率高；Hashtable 方法全 synchronized、不允许 null、已淘汰。

### Q14：HashMap 和 ConcurrentHashMap 区别？
答：HashMap 非线程安全；ConcurrentHashMap JDK 8 用 CAS + synchronized 锁单桶，读无锁，不允许 null 键值，复合操作需 putIfAbsent/merge 等原子方法。

### Q15：ConcurrentHashMap JDK 7 和 8 区别？
答：JDK 7 用 Segment 分段锁（默认 16 并发度）；JDK 8 弃用 Segment，直接用 Node 数组 + CAS + synchronized 锁桶头，锁粒度更细、并发度更高。

### Q16：ConcurrentHashMap 为什么读不加锁？
答：Node 的 val/next 是 volatile，保证可见性；读全程无锁，性能高。但迭代/弱一致性可能读到旧值。

### Q17：ConcurrentHashMap 为什么不允许 null？
答：get 无锁，无法区分 "key 不存在返回 null" 与 "key 存在 value 为 null"，并发下会出错。

### Q18：HashSet 如何保证不重复？
答：底层是 HashMap，元素作 key，value 是固定 PRESENT 常量，去重靠 HashMap 的 key 不可重复。

### Q19：TreeMap/TreeSet 为什么有序？
答：基于红黑树，key 按 Comparable/Comparator 排序，插入即排序，查找 O(log n)，支持范围/导航。

### Q20：什么是 fail-fast 和 fail-safe？
答：fail-fast（ArrayList 等）迭代时集合被结构性修改（非迭代器自身）抛 ConcurrentModificationException；fail-safe（COW、ConcurrentHashMap）基于快照/弱一致，不抛异常但可能读旧值。

### Q21：如何在 for-each 中安全删除元素？
答：用 `removeIf` 或 `Iterator.remove`，不能对集合直接 remove。

### Q22：Arrays.asList 有什么坑？
答：返回定长内部类 ArrayList，不能 add/remove（抛 UnsupportedOperationException）；且底层共享原数组，修改互相影响。需可变列表用 `new ArrayList<>(Arrays.asList(...))`。

### Q23：什么是 subList 的视图陷阱？
答：subList 返回原集合视图，修改影响原集合；一端结构性修改后另一端访问可能 ConcurrentModificationException。需独立副本用 `new ArrayList<>(subList)`。

### Q24：如何实现一个 LRU 缓存？
答：继承 LinkedHashMap，构造 `accessOrder=true`，重写 `removeEldestEntry` 返回 `size()>maxSize`，即可实现线程不安全的 LRU。线程安全用 synchronizedMap 或 Caffeine。

### Q25：EnumSet 为什么最快？
答：底层用位向量（long 位运算），空间极小、无哈希计算，专为枚举设计，比 HashSet 快得多。枚举集合永远优先 EnumSet。

### Q26：Collection 和 Collections 区别？
答：Collection 是集合根接口（List/Set/Queue 的父接口）；Collections 是操作集合的静态工具类（sort/synchronizedList/unmodifiableList 等）。

### Q27：Comparator 手写相减的隐患？
答：`return a - b` 当 a、b 差值超过 int 范围会整数溢出导致排序错误。应用 `Integer.compare(a, b)` 或 `Comparator.comparing()`。

### Q28：HashMap 容量 13，实际是多少？
答：`tableSizeFor` 会把非 2 幂容量向上取整为 ≥ 它的 2 的幂。13 向上取整为 16。

---

## 附录：25 条最佳实践

1. ✅ 初始化集合预估容量，避免反复扩容（如 `new HashMap<>(134)` 存 100 个）。
2. ✅ 遍历 Map 用 entrySet()/forEach，避免 keySet()+get()。
3. ✅ 删除元素用 removeIf 或 Iterator.remove，禁止 for-each 直接 remove。
4. ✅ 返回集合用 Collections.emptyList() 而非 null。
5. ✅ 不可变集合用 List.of()（JDK9+），而非 unmodifiableList（可被原集合绕过）。
6. ✅ 并发用 ConcurrentHashMap/CopyOnWriteArrayList，杜绝 Hashtable/Vector/Stack。
7. ✅ 自定义对象作 key 必须正确重写 equals+hashCode，且 key 不可变。
8. ✅ subList 返回视图，需独立副本 new ArrayList<>(subList)。
9. ✅ Comparator 用 Comparator.comparing，避免手动相减溢出。
10. ✅ 枚举集合用 EnumSet，映射枚举键用 EnumMap。
11. ✅ 需要栈/双端队列用 ArrayDeque，不用 Stack/LinkedList。
12. ✅ 词频/计数用 merge/computeIfAbsent，简洁且逻辑清晰。
13. ✅ 海量整数去重/标记用 BitSet，省内存。
14. ✅ 生产环境多线程计数用 LongAdder 替代 AtomicLong（高并发）。
15. ✅ HashMap 的 get 结果拆箱前判 null 或用 getOrDefault。
16. ✅ 不要对 Arrays.asList 结果做 add/remove。
17. ✅ Integer 比较用 equals，不用 ==（缓存池陷阱）。
18. ✅ 需要保序去重用 LinkedHashSet。
19. ✅ 优先面向 List/Map 接口声明，便于替换实现。
20. ✅ 大文件/大集合处理用 Stream 惰性遍历，避免 readAllLines 撑爆内存。
21. ✅ 并发复合操作（检查再放）用 putIfAbsent/merge/compute，别用 containsKey+put。
22. ✅ 需要范围查询/前驱后继用 TreeMap/TreeSet（Navigable）。
23. ✅ 频繁头尾增删用 ArrayDeque（循环数组，比 LinkedList 省内存）。
24. ✅ 并行流谨慎使用，避免共享可变状态与阻塞 IO。
25. ✅ 理解所用集合的复杂度，别在 O(n) 操作上套循环写出 O(n²) 性能陷阱。

---

> 本深度版覆盖 Java 集合框架全部核心类型、源码机制、分层实战与 28 道源码级面试题。建议配合 JDK 源码（HashMap.resize、ArrayList.grow、ConcurrentHashMap.put、TreeMap.put 等）逐行阅读，理解远比记忆重要。三份知识库（集合 + 并发 + IO）可系统构建 Java 后端基础能力。

---

## 十九、进阶与现代化（Java 8 → 21 高阶技巧）

掌握前面十八章，你已能"用得对、讲得清、调得动"。这一章把集合用到**现代 Java 的高阶水平**：用更少的代码写出更清晰、更健壮、更高并发的程序，并理解底层机制。

### 19.1 Java 21 SequencedCollection：统一"有序集合"抽象

JDK 21 把 `List`、`LinkedHashSet`、`SortedSet`、`Deque` 共同具备的"首尾操作 + 反向视图"抽象成 `SequencedCollection`（子接口 `SequencedSet` / `SequencedMap`）。从此不必因为类型不同而写两套代码。

```java
// List / LinkedHashSet / SortedSet 现在都实现 SequencedCollection
SequencedCollection<String> seq = new ArrayList<>(List.of("a", "b", "c"));
seq.addFirst("head");   // 头插
seq.addLast("tail");    // 尾插
System.out.println(seq.getFirst()); // head
System.out.println(seq.getLast());  // tail
System.out.println(seq.removeFirst()); // head（取并删）
System.out.println(seq.removeLast());  // tail

// reversed() 返回反向视图（不复制数据），统一逆序遍历
for (String s : seq.reversed()) System.out.print(s + " "); // c b a

// SequencedSet 同样适用（LinkedHashSet 保序）
SequencedSet<String> sset = new LinkedHashSet<>(List.of("x", "y", "z"));
sset.addFirst("first");
System.out.println(sset.getLast()); // z

// SequencedMap：首/尾Entry 与 reversed 视图
SequencedMap<Integer, String> smap = new LinkedHashMap<>();
smap.put(1, "A"); smap.put(2, "B");
System.out.println(smap.firstEntry());    // 1=A
System.out.println(smap.lastEntry());     // 2=B
smap.forEach((k, v) -> System.out.println(k + "=" + v)); // 顺序遍历
```

> 价值：以前 `Deque` 有 `getFirst`/`getLast`，`List` 没有；`Collections.reverse` 要复制。现在 `SequencedCollection` 统一了 API，`reversed()` 零成本反向视图，迁移到 Java 21 后顺手用上。

### 19.2 Record 作 Map 的 Key / Set 的元素（Java 16+）

自定义 key 最大的坑是"忘记正确重写 equals/hashCode"或"key 可变导致找不到"（见第十六章 坑4）。`record` 自带**基于全部组件、且不可变**的 equals/hashCode，是天生理想的集合 Key。

```java
record Coord(int x, int y) {}            // 自动生成 equals/hashCode，字段 final

Map<Coord, String> grid = new HashMap<>();
grid.put(new Coord(1, 2), "A");
System.out.println(grid.get(new Coord(1, 2))); // A（自动正确命中）

Set<Coord> visited = new HashSet<>();
visited.add(new Coord(3, 4));
visited.add(new Coord(3, 4)); // equals + hashCode 一致 → 去重，size 仍为 1

// 多字段复合 key 以前要手写 CityYear 类（见 7.6），现在一行 record 解决
record CityYear(String city, int year) {}
Map<CityYear, Integer> stat = new HashMap<>();
stat.merge(new CityYear("北京", 2024), 1, Integer::sum);
```

> 对比第六章的 `Point`：用 `record` 省去手写 `equals`/`hashCode` 与 `final` 字段声明，天然不可变，从根上杜绝"可变 key 丢失"问题。**新项目优先用 record 作 key/复合 key。**

### 19.3 Comparator 链式排序与 null 安全（面试高频易错）

单字段排序只是起步。真实业务常需：多字段、混合方向、字段可能为 null。

```java
// 先部门升序 → 同部门工资降序 → 同工资姓名升序
list.sort(Comparator
    .comparing(Employee::getDept)                                  // 升序
    .thenComparing(Employee::getSalary, Comparator.reverseOrder()) // 降序
    .thenComparing(Employee::getName));                            // 升序

// null 安全：把 null 永远排到末尾（或开头用 nullsFirst）
list.sort(Comparator.comparing(Employee::getNickname,
    Comparator.nullsLast(Comparator.naturalOrder())));

// 反向写法对照（推荐 reversed() 而非手动减）
Comparator<Employee> bySalaryDesc = Comparator.comparing(Employee::getSalary).reversed();
```

> ⚠️ **致命坑**：绝不要写 `return a.getSalary() - b.getSalary()` 做反向比较！当 `a - b` 差值超过 `int` 范围会**整数溢出**导致排序结果错乱。反向一律用 `Comparator.reverseOrder()` 或 `.reversed()`，正向用 `Comparator.comparing(...)` 或 `Integer.compare(a, b)`。
>
> **排序稳定性**：Java 集合的 `sort`（底层 TimSort）是**稳定排序**——相等元素相对顺序不变。这意味着"先排次要字段、再排主要字段"也能得到正确结果，但 `thenComparing` 链式（一次比较链）更直观、性能更好。

### 19.4 flatMap：多级集合扁平化与"一对多"展开

`map` 是"一进一出"，`flatMap` 是"一进一流（再拼起来）"，用来把嵌套结构压平或把单个元素拆成多个。

```java
// 嵌套 List 扁平化
List<List<Integer>> matrix = List.of(List.of(1, 2), List.of(3, 4, 5));
List<Integer> flat = matrix.stream().flatMap(List::stream).toList();
// [1, 2, 3, 4, 5]

// 一对多：每个订单拆成多个条目
List<Order> orders = ...;
List<OrderItem> allItems = orders.stream()
    .flatMap(o -> o.getItems().stream())
    .toList();

// 每句话拆成单词并统计（多对多）
Map<String, Long> wordFreq = sentences.stream()
    .flatMap(s -> Arrays.stream(s.split("\\s+")))
    .collect(Collectors.groupingBy(w -> w, Collectors.counting()));

// 字符串按字符展开
List<Character> chars = "hello".chars()
    .mapToObj(c -> (char) c)
    .toList();
```

> 记忆口诀：**"要把一个元素变成多个、或把嵌套压平，用 flatMap"**。它是 Stream 高级处理（如文档解析、树遍历、CSV 展开）的标配。

### 19.5 Collectors.teeing（Java 12+）：一次遍历算出多个指标

`teeing` 用两个下游收集器分别处理同一数据流，再用合并函数组合结果——**只遍历一遍**，避免多次 `collect` 或先收集再二次计算。

```java
record Stats(double avg, int max, int min) {}

Stats stats = students.stream().collect(Collectors.teeing(
    Collectors.averagingInt(Student::getScore),            // 下游1：平均分
    Collectors.maxBy(Comparator.comparingInt(Student::getScore)), // 下游2：最高分
    (avg, maxOpt) -> new Stats(avg, maxOpt.orElseThrow().getScore(),
                               students.stream().mapToInt(Student::getScore).min().orElse(0))
));
```

> 替代旧写法（`collect` 成中间 Map 再算、或遍历两遍）。`teeing` 把"多指标聚合"收进单个声明式操作，性能与可读性双优。Java 12 之前可用 `Collectors.collectingAndThen` + 自定义容器模拟。

### 19.6 Spliterator 与并行流底层（真正懂 Parallel Stream）

`Spliterator`（可分割迭代器）是 `Stream` 并行能力的基石：它既负责遍历（`tryAdvance`），又负责把数据**拆分**（`trySplit`）分配到多个线程做分治。

```java
class LineSpliterator implements Spliterator<String> {
    private final BufferedReader reader;
    private String nextLine;
    LineSpliterator(BufferedReader r) { this.reader = r; }
    public boolean tryAdvance(Consumer<? super String> action) {
        try {
            nextLine = reader.readLine();
            if (nextLine == null) return false;
            action.accept(nextLine);
            return true;
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
    public Spliterator<String> trySplit() { return null; } // 简化：本例不支持再拆分
    public long estimateSize() { return Long.MAX_VALUE; }
    public int characteristics() { return ORDERED | NONNULL; }
}
// 用法：StreamSupport.stream(new LineSpliterator(br), false).forEach(System.out::println);
```

> 要点：
> - `trySplit()` 返回 `null` → 该段不可再分（顺序处理）；返回新 Spliterator → 并行框架把数据一分为二递归下去。
> - `characteristics()` 标志（`SIZED`/`SUBSIZED`/`IMMUTABLE`/`CONCURRENT`/`ORDERED`）帮助并行框架优化。
> - `estimateSize()` 越准，拆分越均衡。
>
> 理解 `Spliterator` 才算真正懂"并行流为什么能加速、什么数据适合并行"。

### 19.7 并发队列底层速览（选型不再靠猜）

| 类 | 实现 | 锁 | 适用 |
|----|------|----|------|
| `ConcurrentLinkedQueue` | CAS 无锁 | 无 | 高吞吐、无界、不阻塞 |
| `LinkedBlockingQueue` | 链表 + 双锁 | takeLock / putLock | 有界/无界、入出可真并发 |
| `ArrayBlockingQueue` | 数组 + 单锁 | 1 锁 + 2 Condition | 有界、入出互斥、公平可选 |

```java
// 完全无锁（CAS）：高并发首选非阻塞队列；注意 size() 需遍历 O(n)，热路径勿调
Queue<Task> q = new ConcurrentLinkedQueue<>();
q.offer(task);          // CAS 入队
Task t = q.poll();      // CAS 出队，空返回 null

// 有界阻塞队列：生产者满了就阻塞，消费者空了就阻塞
BlockingQueue<Task> bq = new ArrayBlockingQueue<>(1024);
bq.put(task);           // 满 → 阻塞
Task t2 = bq.take();    // 空 → 阻塞

// LinkedBlockingQueue 双锁：入队与出队用不同锁，可真正并行
BlockingQueue<Task> lbq = new LinkedBlockingQueue<>(1024);
```

> - `ConcurrentLinkedQueue` 的 `size()` 是 **O(n)**（要遍历计数），千万别在并发热路径里调用，否则既慢又是"时刻变化"的错误值。要计数请用 `AtomicLong` 自增。
> - `LinkedBlockingQueue` 默认容量 `Integer.MAX_VALUE`（无界），生产务必显式指定容量以防内存撑爆。

### 19.8 性能基准：用 JMH 代替"凭感觉"

集合选型不能靠直觉，用 JMH（Java Microbenchmark Harness，JDK 自带 `jmh-core`）做微基准。

```java
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 3) @Measurement(iterations = 5)
public class ListGetBenchmark {
    List<Integer> array = new ArrayList<>();
    List<Integer> linked = new LinkedList<>();
    @Setup public void setup() {
        for (int i = 0; i < 100_000; i++) { array.add(i); linked.add(i); }
    }
    @Benchmark public int arrayGet() { return array.get(50_000); }   // 纳秒级
    @Benchmark public int linkedGet() { return linked.get(50_000); } // 慢很多（遍历）
}
```

> 经验结论（来自实测而非猜测）：主流 JVM 上 `ArrayList.get(i)` 是单次数组访问（纳秒级），`LinkedList.get(i)` 需从头/尾遍历约半条链表（随规模线性变慢）。**随机访问频繁时 LinkedList 全面落败**——这正是 5.2.4 结论的实证来源。任何"XX 一定更快"的断言，都应先写基准验证。

### 19.9 序列化与集合的隐藏坑

- `transient`：`ArrayList.elementData` 是 `transient`，序列化时跳过，反序列化用 `readObject` 重建，避免把扩容余量也序列化浪费空间。
- 自定义 `readObject` **必须维护不变式**：恢复 `size`、`modCount`、链表前后指针等，否则反序列化得到损坏对象（如链表断链）。
- 自定义 key 必须可序列化：`HashSet`/自定义 key 序列化时，key 类需 `implements Serializable`（`String`/`Integer`/`record` 都满足），否则抛 `NotSerializableException`。
- 防御性拷贝：返回内部集合字段时用 `new ArrayList<>(internal)` / `List.copyOf(internal)`，避免外部修改内部状态。

### 19.10 进阶最佳实践补充（26–31）

26. ✅ Java 21+ 用 `SequencedCollection` 的 `addFirst/addLast/getFirst/getLast/reversed()`，替代 `Collections.reverse` 与 Deque 专属 API，统一有序集合操作。
27. ✅ 复合 key / 不可变 value 优先用 `record`，省 equals/hashCode 且杜绝可变 key 陷阱。
28. ✅ 多字段排序用 `Comparator.comparing().thenComparing()` 链式；反向用 `reverseOrder()`，禁止手写相减（防溢出）。
29. ✅ 嵌套/一对多展开用 `flatMap`；多指标单遍聚合用 `Collectors.teeing`（Java 12+）。
30. ✅ 并发队列：高吞吐无锁用 `ConcurrentLinkedQueue`（勿在热路径调 `size()`）；有界阻塞用 `ArrayBlockingQueue`（显式指定容量）。
31. ✅ 性能论断先写 JMH 基准验证，不凭直觉；理解 `Spliterator` 才真正懂并行流的分治边界。

---

## 二十、扩展面试题（现代 API 篇）

### Q29：Java 21 的 SequencedCollection 解决了什么问题？
答：此前 `Deque` 有 `getFirst/getLast`，`List` 没有；反向遍历要 `Collections.reverse`（复制）。`SequencedCollection` 把"首尾操作 + `reversed()` 反向视图"统一抽象，`List`/`LinkedHashSet`/`SortedSet`/`Deque` 都实现它，API 一致、零成本反向。

### Q30：为什么用 record 作 HashMap 的 key 更优？
答：record 自动生成"基于全部组件、且字段 final（不可变）"的 equals/hashCode，天然满足"key 不可变 + equals/hashCode 一致"两大铁律，从根上消除手写遗漏与可变 key 丢失问题。

### Q31：Comparator 手写相减 `a - b` 做反向排序有什么隐患？
答：当 `a`、`b` 差值超过 int 范围会发生**整数溢出**，比较结果错乱、排序结果错误且极难排查。反向一律用 `Comparator.reverseOrder()` 或 `.reversed()`；正向用 `Comparator.comparing(...)` 或 `Integer.compare(a, b)`。

### Q32：flatMap 和 map 的区别？何时用 flatMap？
答：`map` 把每个元素映射成 1 个；`flatMap` 把每个元素映射成一个流再拼接，用于嵌套结构扁平化或"一个变多个"（如订单→条目、句子→单词）。需要"压平"或"一对多展开"时用 flatMap。

### Q33：Collectors.teeing 有什么用？
答：用两个下游收集器同时处理同一数据流，再用合并函数组合结果，**只遍历一遍**即可算出多个聚合指标（如平均分+最高分）。比收集成中间 Map 再算、或遍历多遍更优。需 Java 12+。

### Q34：ConcurrentLinkedQueue 的 size() 为什么不能随便调用？
答：它是无锁 CAS 实现，`size()` 需要遍历整个链表计数，复杂度 O(n)，且并发下返回值瞬间变化无意义。计数应改用外部 `AtomicLong` 自增；禁止在并发热路径调 `size()`。

### Q35：Spliterator 在并行流里起什么作用？
答：`Spliterator`（`tryAdvance` 遍历 + `trySplit` 拆分 + `estimateSize`/`characteristics`）是并行流的拆分引擎。`trySplit` 把数据递归一分为二分配到多核，`characteristics`（如 `SIZED`/`SUBSIZED`）帮助框架优化。理解它才懂"什么数据适合并行流、并行边界在哪"。

### Q36：说说 LinkedBlockingQueue 与 ArrayBlockingQueue 的锁设计差异？
答：`ArrayBlockingQueue` 单把锁 + `notEmpty`/`notFull` 两个 Condition，入队出队互斥；`LinkedBlockingQueue` 用 `takeLock`/`putLock` 两把锁（各带 Condition），入队与出队可真正并发，吞吐更高，但两锁带来额外复杂度。两者都有界（Linked 默认无界，须显式指定容量）。

---

> 至此，文档从"基础用法 → 源码机制 → 分层实战 → 常见坑 → 并发集合 → 28 道面试题"一路延伸到"Java 8→21 现代高阶技巧 + 36 道面试题"。配合 JDK 源码逐行阅读、动手跑通本章示例、再用 JMH 验证你的选型判断，即可达到"超高"的 Java 集合水平。
