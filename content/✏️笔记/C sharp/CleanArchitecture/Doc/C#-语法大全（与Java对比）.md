## C# 语法大全（与 Java 对比）

> 系统性梳理 C# 语言特性，对有 Java 基础的开发者标注两者的差异。
> 以 C# 13 / .NET 10 为基准，Java 以 21 LTS 为基准。

---

### 一、基础类型与变量

#### 1.1 值类型 vs 引用类型

C# 明确区分值类型和引用类型，Java 则通过装箱/拆箱模糊处理。

| 分类 | C# | Java |
|------|-----|------|
| 基本类型 | `int`, `double`, `bool`, `char` 等（真正的值类型，存在栈上） | `int`, `double`, `boolean`, `char`（原始类型） |
| 包装类型 | 无独立包装类型。`int` 和 `System.Int32` 是同一个类型 | `Integer`, `Double`, `Boolean`（对象，存在堆上） |
| 字符串 | `string`（引用类型，但表现像值类型，不可变） | `String`（引用类型，不可变） |
| 空值 | 值类型默认不能为 null，需要 `int?` 声明可空 | 原始类型不能为 null，包装类型可以 |

```csharp
// C# —— 值类型不需要装箱
int a = 10;
int b = a;        // 复制值，不是引用
b = 20;            // 不影响 a

// 可空值类型（Java 没有对应概念）
int? nullableInt = null;
if (nullableInt.HasValue)
{
    int value = nullableInt.Value;
}
int value2 = nullableInt ?? 0;  // null 合并运算符
```

```java
// Java —— 原始类型和包装类型是分开的
int a = 10;
Integer b = a;     // 自动装箱
int c = b;         // 自动拆箱

// 空值只能通过包装类型
Integer nullableInt = null;
```

#### 1.2 常量与只读

| 特性 | C# | Java |
|------|-----|------|
| 编译时常量 | `const int MAX = 100;` | `static final int MAX = 100;` |
| 运行时常量 | `readonly int Max;`（可在构造函数中赋值） | 无直接等价物 |
| 不可变变量 | `var x = 10;`（类型推断，但值可变） | `var x = 10;`（Java 10+，同 C#） |
| 不可变引用 | 无直接等价物 | `final var x = 10;`（引用不可变，但对象内容可变） |

```csharp
// C#
const int MAX_RETRY = 3;           // 编译时确定，不能修改
readonly string _configPath;       // 运行时确定，只能在声明或构造函数中赋值

public class MyService
{
    private readonly IHttpClient _client;  // 依赖注入常用 readonly
    
    public MyService(IHttpClient client)
    {
        _client = client;  // 构造函数中赋值
    }
}
```

#### 1.3 var 类型推断

```csharp
// C# —— var 是隐式类型，编译器推断实际类型
var name = "hello";        // string
var count = 42;            // int
var list = new List<int>();  // List<int>

// C# 13 新增：var 可以用于更多场景
var (x, y) = GetPoint();   // 元组解构

// 注意：var 不是动态类型！编译后就是强类型
var x = 10;
// x = "hello";  // 编译错误！x 是 int
```

```java
// Java 10+ —— var 只能用于局部变量
var name = "hello";        // String
var list = new ArrayList<String>();  // ArrayList<String>

// Java 的 var 不能用于字段、方法参数、返回类型
// C# 的 var 也不能用于这些场景
```

**关键差异：** C# 的 `var` 推断更广泛，支持匿名类型 `var x = new { Name = "Alice", Age = 30 };`，Java 没有匿名类型。

---

### 二、字符串

#### 2.1 字符串字面量

```csharp
// C#
string s1 = "Hello";                    // 普通字符串
string s2 = @"C:\Users\admin\file.txt"; // verbatim 字符串（反斜杠不转义）
string s3 = $"Name: {name}, Age: {age}"; // 插值字符串
string s4 = $@"Path: {basePath}\logs";   // verbatim + 插值
string s5 = """
    这是多行字符串（C# 11+）
    支持任意换行和引号 "hello"
    缩进会自动对齐
    """;                                  // 原始字符串字面量
```

```java
// Java
String s1 = "Hello";
String s2 = "C:\\Users\\admin\\file.txt";  // 需要手动转义
String s3 = "Name: " + name + ", Age: " + age;  // 手动拼接
String s4 = String.format("Name: %s, Age: %d", name, age);  // printf 风格
String s5 = """
    这是多行字符串（Java 15+）
    支持任意换行和引号 "hello"
    """;  // Text Blocks
```

**关键差异：** C# 的 `$"..."` 插值字符串比 Java 的 `String.format` 更直观、类型安全、性能更好。C# 的 `@""` verbatim 字符串在处理路径和正则表达式时特别有用。

#### 2.2 字符串比较

```csharp
// C#
"a" == "b"                          // 值比较（C# 重载了 == 运算符）
"Hello".Equals("hello")             // 默认区分大小写
"Hello".Equals("hello", 
    StringComparison.OrdinalIgnoreCase) // 忽略大小写
string.Compare("a", "b", 
    StringComparison.OrdinalIgnoreCase) // 返回 -1/0/1

// C# 中 string 的 == 是比较值，不是比较引用！
string a = "hello";
string b = "hel" + "lo";
a == b;  // true（值相同）
```

```java
// Java
"a".equals("b")                     // 值比较
"a" == "b"                          // 引用比较！（新手常见 bug）
"a".equalsIgnoreCase("A")           // 忽略大小写

// Java 中 String 的 == 是比较引用，不是比较值！
String a = "hello";
String b = new String("hello");
a == b;  // false（不同的对象）
a.equals(b);  // true（值相同）
```

**关键差异：** C# 的 `==` 对 string 做了特殊处理（值比较），Java 的 `==` 始终是引用比较。这是 C# 和 Java 最大的差异之一。

---

### 三、控制流

#### 3.1 if / else / switch

```csharp
// C# —— if 与 Java 基本相同
if (count > 0)
{
    Process(count);
}
else if (count == 0)
{
    Reset();
}
else
{
    HandleNegative();
}

// C# 的模式匹配 switch（比 Java 强大得多）
string GetStatusDescription(int statusCode) => statusCode switch
{
    200 => "OK",
    301 or 302 => "Redirect",        // or 模式（C# 9+）
    >= 400 and < 500 => "Client Error",  // 关系模式
    >= 500 => "Server Error",
    _ => "Unknown"                    // 默认分支（必须用 _ 而不是 default）
};

// 带类型的 switch
string Describe(object obj) => obj switch
{
    int n when n > 0 => $"正整数: {n}",
    int n => $"非正整数: {n}",
    string s => $"字符串，长度: {s.Length}",
    null => "null",
    _ => obj.GetType().Name
};
```

```java
// Java
// if/else 与 C# 相同

// Java 的 switch 表达式（Java 14+）
String description = switch (statusCode) {
    case 200 -> "OK";
    case 301, 302 -> "Redirect";
    case 400, 401, 403, 404 -> "Client Error";
    case 500, 502, 503 -> "Server Error";
    default -> "Unknown";
};

// Java 21 的模式匹配 switch
String describe(Object obj) {
    return switch (obj) {
        case Integer n when n > 0 -> "正整数: " + n;
        case Integer n -> "非正整数: " + n;
        case String s -> "字符串，长度: " + s.length();
        case null -> "null";
        default -> obj.getClass().getSimpleName();
    };
}
```

**关键差异：** C# 的 switch 表达式支持 `or`/`and`/关系运算符模式，更灵活。Java 21 的模式匹配已经追上来了，但 C# 的 guard 子句（`when` 子句）更灵活。

#### 3.2 for / foreach / while

```csharp
// C# —— for 与 Java 相同
for (int i = 0; i < 10; i++) { }

// foreach（C# 用 foreach，Java 用 for-each）
foreach (var item in list)           // C#
{
    Console.WriteLine(item);
}

// 带索引的 foreach（C# 没有内置语法，但可以用 LINQ）
foreach (var (item, index) in list.Select((item, i) => (item, i)))
{
    Console.WriteLine($"{index}: {item}");
}

// C# 8+ 的 Index 和 Range
var first = array[0];        // 第一个元素
var last = array[^1];        // 最后一个元素（^1 = 从末尾数第1个）
var slice = array[1..4];     // 索引1到3的元素（不含4）
var allFromSecond = array[2..];  // 从索引2到末尾
```

```java
// Java
for (int i = 0; i < 10; i++) { }

// for-each
for (var item : list) {
    System.out.println(item);
}

// Java 没有 ^ 和 .. 运算符
// 获取最后一个元素需要 list.get(list.size() - 1)
```

**关键差异：** C# 的 `^` 和 `..` 运算符（Index/Range）是 Java 没有的特性，处理数组和列表的子范围非常方便。

---

### 四、面向对象

#### 4.1 类与构造函数

```csharp
// C#
public class Person
{
    // 属性（C# 独有，Java 用 getter/setter）
    public string Name { get; set; }
    public int Age { get; set; }
    
    // 只读属性
    public string Id { get; }
    
    // 带逻辑的属性
    public bool IsAdult => Age >= 18;    // 表达式体属性
    
    public string FullName
    {
        get => $"{Name} (Age: {Age})";
        set
        {
            var parts = value.Split(" (");
            Name = parts[0];
        }
    }

    // 构造函数
    public Person(string name, int age)
    {
        Name = name;
        Age = age;
        Id = Guid.NewGuid().ToString();
    }

    // 主构造函数（C# 12+）
    // 可以直接写在类声明上
}

// C# 12 主构造函数
public class Point(double x, double y)
{
    public double X { get; } = x;
    public double Y { get; } = y;
    public double Distance => Math.Sqrt(X * X + Y * Y);
}
```

```java
// Java
public class Person {
    private String name;
    private int age;
    private final String id;

    // getter/setter（或使用 Lombok @Data）
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    // 只读字段
    public String getId() { return id; }

    // 计算属性
    public boolean isAdult() { return age >= 18; }

    // 构造函数
    public Person(String name, int age) {
        this.name = name;
        this.age = age;
        this.id = UUID.randomUUID().toString();
    }
}
```

**关键差异：** C# 的**属性**（Property）是语言级别的特性，不需要手写 getter/setter。`{ get; set; }` 自动生成支持字段，编译后和手写 getter/setter 等价。Java 需要 Lombok 或 Record 来减少样板代码。

#### 4.2 继承与接口

```csharp
// C#
public abstract class Animal
{
    public abstract string Name { get; }       // 抽象属性
    public abstract void Speak();               // 抽象方法
    
    public virtual void Eat()                   // 虚方法（可以被 override）
    {
        Console.WriteLine("Eating...");
    }
    
    public void Sleep()                         // 非虚方法（不能被 override）
    {
        Console.WriteLine("Sleeping...");
    }
}

public class Dog : Animal                       // 继承用冒号（Java 用 extends）
{
    public override string Name => "Dog";       // override 关键字（必须显式标注）
    
    public override void Speak()
    {
        Console.WriteLine("Woof!");
    }
    
    public override void Eat()                  // override 虚方法
    {
        base.Eat();                             // 调用基类方法
        Console.WriteLine("Dog is eating bones.");
    }
}

// 接口
public interface IRepository<T>                 // 接口命名习惯用 I 前缀
{
    Task<T> GetByIdAsync(int id);
    Task<List<T>> GetAllAsync();
    Task AddAsync(T entity);
    Task DeleteAsync(int id);
}

// 接口默认实现（C# 8+）
public interface ILogger
{
    void Log(string message);
    void LogError(string message) => Log($"[ERROR] {message}");  // 默认实现
    void LogWarning(string message) => Log($"[WARN] {message}");
}
```

```java
// Java
public abstract class Animal {
    public abstract String getName();
    public abstract void speak();
    
    public void eat() {            // 没有 virtual 关键字，所有非 final 方法都可被 override
        System.out.println("Eating...");
    }
    
    public final void sleep() {    // final 方法不能被 override
        System.out.println("Sleeping...");
    }
}

public class Dog extends Animal {  // extends（不是冒号）
    @Override                      // @Override 注解（建议但不强制）
    public String getName() { return "Dog"; }
    
    @Override
    public void speak() {
        System.out.println("Woof!");
    }
    
    @Override
    public void eat() {
        super.eat();
        System.out.println("Dog is eating bones.");
    }
}

// 接口
public interface IRepository<T> {
    T getById(int id);
    List<T> getAll();
    // Java 8+ 支持 default 方法
    default void delete(int id) { throw new UnsupportedOperationException(); }
}
```

**关键差异：**

| 特性 | C# | Java |
|------|-----|------|
| 继承关键字 | `:` | `extends` |
| 接口实现关键字 | `:` （和继承同一个符号） | `implements` |
| 重写标注 | `override` 关键字（强制） | `@Override` 注解（可选但推荐） |
| 虚方法 | `virtual` 关键字标注 | 默认所有非 `final` 方法都是虚方法 |
| 调用基类 | `base.Method()` | `super.method()` |
| 多继承 | 单继承 + 多接口 | 单继承 + 多接口 |
| 密封类 | `sealed class` | `final class` |

#### 4.3 访问修饰符

| C# | Java | 含义 |
|-----|------|------|
| `public` | `public` | 任何地方可访问 |
| `private` | `private` | 仅当前类可访问 |
| `protected` | `protected` | 当前类 + 派生类 |
| `internal` | 包私有（无修饰符） | 仅当前程序集/包可访问 |
| `protected internal` | — | 当前程序集 **或** 派生类 |
| `private protected` | — | 当前程序集 **且** 派生类 |
| `file`（C# 11+） | — | 仅当前文件可访问 |

**关键差异：** C# 的 `internal` 是按程序集（`.dll`/`.exe`）划分的，Java 的包私有是按包划分的。C# 有更细粒度的控制（`protected internal`、`private protected`）。

---

### 五、泛型

```csharp
// C#
public class Repository<T> where T : class, IEntity, new()
{
    private readonly List<T> _items = new();
    
    public T Create()
    {
        var item = new T();     // C# 泛型支持 new T()（需要 new() 约束）
        _items.Add(item);
        return item;
    }
    
    public T GetById(int id)
    {
        return _items.FirstOrDefault(x => x.Id == id);
    }
}

// 泛型约束
where T : class              // 引用类型约束
where T : struct             // 值类型约束
where T : new()              // 无参构造函数约束
where T : IComparable<T>     // 接口约束
where T : BaseEntity         // 基类约束
where T : notnull            // 非空约束（C# 8+）
```

```java
// Java
public class Repository<T extends IEntity> {
    private final List<T> items = new ArrayList<>();
    
    public T create(Class<T> clazz) {
        T item = clazz.getDeclaredConstructor().newInstance();  // 需要反射
        items.add(item);
        return item;
    }
}

// Java 泛型约束只能用 extends（上界）或 super（下界）
// 不能约束"必须有构造函数"
```

**关键差异：**

| 特性 | C# | Java |
|------|-----|------|
| 类型擦除 | 运行时保留类型信息（reified） | 编译时擦除（erasure） |
| `new T()` | 支持（需要 `new()` 约束） | 不支持（需要反射或工厂方法） |
| `T[]` 数组 | 支持 | 不支持（`new T[0]` 编译错误） |
| `default(T)` | 支持（值类型返回 0，引用类型返回 null） | 不支持 |
| 泛型约束 | 丰富的约束（class, struct, new(), 接口, 基类） | 只有 `extends` 和 `super` |
| `List<int>` | 合法（真正的值类型列表，无装箱） | 不合法（必须用 `List<Integer>`） |

---

### 六、委托、事件与 Lambda

这是 C# 与 Java 差异最大的的部分之一。

#### 6.1 委托（Delegate）

```csharp
// C# —— 委托是类型安全的方法引用
public delegate void LogHandler(string message);

// 使用委托
LogHandler handler = Console.WriteLine;
handler("Hello");              // 调用
handler += File.WriteLine;     // 多播委托（一次调用多个方法）

// 内置委托类型（不需要自定义 delegate）
Action<string> log = Console.WriteLine;          // 无返回值
Func<int, int, int> add = (a, b) => a + b;       // 有返回值
Predicate<int> isPositive = n => n > 0;          // 返回 bool
```

```java
// Java —— 没有委托，用函数式接口替代
@FunctionalInterface
interface LogHandler {
    void log(String message);
}

// 使用
LogHandler handler = System.out::println;
handler.log("Hello");
// 没有多播机制
```

#### 6.2 事件（Event）

```csharp
// C# —— 事件是委托的封装，提供发布/订阅模式
public class Button
{
    public event EventHandler Click;              // 声明事件
    public event EventHandler<MouseEventArgs> MouseMove;  // 带参数的事件
    
    protected void OnClick()
    {
        Click?.Invoke(this, EventArgs.Empty);     // 触发事件（?. 空安全调用）
    }
    
    protected void OnMouseMove(int x, int y)
    {
        MouseMove?.Invoke(this, new MouseEventArgs(x, y));
    }
}

// 订阅事件
var button = new Button();
button.Click += (sender, e) => Console.WriteLine("Clicked!");
button.Click += HandleClick;                      // 方法引用
button.Click -= HandleClick;                      // 取消订阅

// WinForms 常见模式
button.Click += async (sender, e) =>
{
    await ProcessOrderAsync();
};
```

```java
// Java —— 没有事件关键字，用接口+监听器模式
public class Button {
    private List<ActionListener> listeners = new ArrayList<>();
    
    public void addActionListener(ActionListener listener) {
        listeners.add(listener);
    }
    
    public void removeActionListener(ActionListener listener) {
        listeners.remove(listener);
    }
    
    private void fireActionEvent() {
        for (ActionListener listener : listeners) {
            listener.actionPerformed(new ActionEvent(this, 0, "click"));
        }
    }
}

// 订阅
button.addActionListener(e -> System.out.println("Clicked!"));
```

**关键差异：** C# 的 `event` 是语言级别的特性，语法简洁（`+=`/`-=`），支持多播。Java 需要手写监听器管理代码。在你的 WinForms 项目中，事件无处不在（按钮点击、定时器触发等）。

#### 6.3 Lambda 表达式

```csharp
// C#
Func<int, int, int> add = (a, b) => a + b;
Action<string> log = msg => Console.WriteLine(msg);
Func<int, bool> isPositive = n => n > 0;

// LINQ 中的 Lambda
var result = numbers
    .Where(n => n > 0)
    .Select(n => n * 2)
    .ToList();

// 多行 Lambda
Func<int, string> describe = n =>
{
    if (n > 0) return "positive";
    if (n < 0) return "negative";
    return "zero";
};

// C# 10+ 的 Lambda 改进
var handler = (object sender, EventArgs e) =>
{
    Console.WriteLine($"Event from {sender}");
};

// 自然类型 Lambda（C# 10+）
var lambda = (int x, int y) => x + y;  // 编译器推断为 Func<int, int, int>
```

```java
// Java
BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b;
Consumer<String> log = msg -> System.out.println(msg);
Predicate<Integer> isPositive = n -> n > 0;

// Stream 中的 Lambda
var result = numbers.stream()
    .filter(n -> n > 0)
    .map(n -> n * 2)
    .toList();

// 方法引用（两者都支持）
list.sort(Comparator.naturalOrder());  // Java
list.Sort();                            // C#（List 自带方法）
```

**关键差异：** C# 用 `=>`，Java 用 `->`。C# 的 Lambda 可以直接赋值给 `Func`/`Action` 委托，Java 需要函数式接口。

---

### 七、Record 类型（不可变数据类）

```csharp
// C# Record（C# 9+）
public record Person(string Name, int Age);

// 自动生成：构造函数、属性、Equals、GetHashCode、ToString、Deconstruct
var p1 = new Person("Alice", 30);
var p2 = new Person("Alice", 30);
p1 == p2;  // true（值相等）

// with 表达式（创建修改后的副本）
var p3 = p1 with { Age = 31 };   // Person("Alice", 31)
// p1 不受影响，仍然 Age = 30

// 解构
var (name, age) = p1;

// Record 也可以有自定义逻辑
public record Product(string Name, decimal Price)
{
    public decimal PriceWithTax => Price * 1.13m;
    
    public bool IsExpensive => Price > 1000;
}

// Record struct（C# 10+，值类型的 Record）
public readonly record struct Point(double X, double Y);
```

```java
// Java Record（Java 16+）
public record Person(String name, int age) {}

// 自动生成：构造函数、getter、equals、hashCode、toString
var p1 = new Person("Alice", 30);
var p2 = new Person("Alice", 30);
p1.equals(p2);  // true
// p1 == p2;    // false！（Java record 的 == 仍然是引用比较）

// Java record 没有 with 表达式
// 需要手动创建新实例
var p3 = new Person(p1.name(), 31);

// Java record 没有解构语法
var name = p1.name();   // 注意：getter 没有 get 前缀
var age = p1.age();
```

**关键差异：**

| 特性 | C# Record | Java Record |
|------|-----------|-------------|
| 值相等 | `==` 即可 | 必须用 `.equals()` |
| `with` 表达式 | 支持（部分修改创建副本） | 不支持 |
| 解构 | 支持 `var (x, y) = point;` | 不支持 |
| 可变 Record | `record class`（可包含 set 属性） | 不可变（所有字段 final） |
| 值类型 Record | `record struct` | 无对应概念 |

---

### 八、元组（Tuple）

```csharp
// C# 值元组（C# 7+）—— 轻量、高性能
(string Name, int Age) GetPerson()
{
    return ("Alice", 30);
}

var person = GetPerson();
Console.WriteLine(person.Name);      // "Alice"
Console.WriteLine(person.Age);       // 30

// 解构
var (name, age) = GetPerson();

// 元组作为方法参数
void Print((string Name, int Age) person)
{
    Console.WriteLine($"{person.Name}, {person.Age}");
}

// 实际场景：返回多个值
(bool Success, string Message, int Id) CreateUser(string name)
{
    if (string.IsNullOrWhiteSpace(name))
        return (false, "名称不能为空", 0);
    
    var id = SaveToDatabase(name);
    return (true, "创建成功", id);
}

var result = CreateUser("Bob");
if (result.Success)
{
    Console.WriteLine($"用户 ID: {result.Id}");
}
```

```java
// Java 没有内置元组
// 方式1：自定义类
record Pair<A, B>(A first, B second) {}

// 方式2：使用第三方库（如 Vavr、Apache Commons）
import org.javatuples.Pair;
var pair = Pair.with("Alice", 30);

// 方式3：Map.Entry（仅两个值）
Map.Entry<String, Integer> entry = Map.entry("Alice", 30);
```

**关键差异：** C# 的值元组是语言级别的支持，高性能（struct，栈分配），支持命名和解构。Java 没有内置元组，需要自定义类或使用第三方库。

---

### 九、模式匹配

C# 的模式匹配比 Java 更成熟、更强大。

```csharp
// C# 模式匹配

// 1. is 模式
if (obj is string s)
{
    Console.WriteLine($"字符串: {s}");
}

if (obj is int n and > 0)    // 组合模式
{
    Console.WriteLine($"正整数: {n}");
}

// 2. switch 表达式（最常用）
decimal CalculateDiscount(Order order) => order switch
{
    { Total: > 1000, Customer.Type: "VIP" } => order.Total * 0.1m,  // 属性模式
    { Total: > 1000 } => order.Total * 0.05m,
    { Items.Count: > 10 } => order.Total * 0.02m,
    _ => 0m
};

// 3. 列表模式（C# 11+）
string Describe(int[] numbers) => numbers switch
{
    [] => "空数组",
    [var x] => $"只有一个元素: {x}",
    [var first, .., var last] => $"首: {first}, 尾: {last}",
    [0, 1, ..] => "以 0,1 开头",
    _ => "其他"
};

// 4. not 模式
if (obj is not null)
{
    // 使用 obj（C# 知道它不为 null）
}

if (value is not string)
{
    // value 不是字符串
}
```

```java
// Java 模式匹配

// 1. instanceof 模式（Java 16+）
if (obj instanceof String s) {
    System.out.println("字符串: " + s);
}

// 2. switch 模式（Java 21+）
String describe(Object obj) {
    return switch (obj) {
        case Integer n when n > 0 -> "正整数: " + n;
        case String s -> "字符串: " + s;
        case null -> "null";
        default -> "其他";
    };
}

// 3. Record 模式解构（Java 21+）
record Point(int x, int y) {}
if (obj instanceof Point(var x, var y)) {
    System.out.println(x + y);
}

// Java 没有：列表模式、属性模式、not 模式
```

---

### 十、异步编程

```csharp
// C# async/await —— 语言级别的异步支持
public async Task<string> DownloadDataAsync(string url)
{
    using var client = new HttpClient();
    var result = await client.GetStringAsync(url);  // 不阻塞线程
    return result;
}

// 多个异步操作并行
public async Task<(string, string)> DownloadBothAsync()
{
    var task1 = DownloadDataAsync("https://api1.com");
    var task2 = DownloadDataAsync("https://api2.com");
    
    await Task.WhenAll(task1, task2);
    return (task1.Result, task2.Result);
}

// async 方法返回类型
public async Task DoWorkAsync() { }            // 无返回值
public async Task<int> GetCountAsync() { }     // 返回 int
public async Task<List<string>> GetNamesAsync() { }  // 返回 List<string>
public async void OnButtonClick() { }          // 仅用于事件处理器！

// 取消异步操作
public async Task ProcessAsync(CancellationToken ct)
{
    for (int i = 0; i < 100; i++)
    {
        ct.ThrowIfCancellationRequested();  // 检查是否被取消
        await Task.Delay(100, ct);
    }
}

// WinForms 中的典型用法
private async void btnStart_Click(object sender, EventArgs e)
{
    btnStart.Enabled = false;
    try
    {
        var result = await ReadPlcDataAsync();
        txtResult.Text = result.ToString();
    }
    catch (Exception ex)
    {
        MessageBox.Show(ex.Message);
    }
    finally
    {
        btnStart.Enabled = true;
    }
}
```

```java
// Java —— CompletableFuture（没有 async/await 关键字）
public CompletableFuture<String> downloadData(String url) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            return new String(java.net.URI.create(url).toURL().openStream().readAllBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });
}

// 组合异步操作
public CompletableFuture<Void> downloadBoth() {
    CompletableFuture<String> f1 = downloadData("https://api1.com");
    CompletableFuture<String> f2 = downloadData("https://api2.com");
    return CompletableFuture.allOf(f1, f2);
}

// Java 21 虚拟线程（Virtual Threads）
Thread.startVirtualThread(() -> {
    var result = blockingOperation();  // 看起来是同步的，但不阻塞平台线程
});
```

**关键差异：**

| 特性 | C# | Java |
|------|-----|------|
| 语法 | `async`/`await` 关键字 | `CompletableFuture` 链式调用 |
| 可读性 | 看起来像同步代码 | 需要 `.thenApply()`, `.thenCompose()` 等 |
| 取消 | `CancellationToken` 贯穿调用链 | `Future.cancel()` 或自定义检查 |
| UI 线程 | `await` 后自动回到 UI 线程 | 需要 `SwingUtilities.invokeLater()` |
| 异常处理 | `try/catch` 直接捕获异步异常 | `.exceptionally()` 或 `try/catch` 在虚拟线程中 |

---

### 十一、异常处理

```csharp
// C#
try
{
    var result = Divide(10, 0);
}
catch (DivideByZeroException ex)
{
    Console.WriteLine($"除零错误: {ex.Message}");
}
catch (Exception ex) when (ex.Message.Contains("timeout"))  // 异常过滤器（Java 没有）
{
    Console.WriteLine($"超时错误: {ex.Message}");
}
catch (Exception ex)
{
    Console.WriteLine($"其他错误: {ex.Message}");
}
finally
{
    Cleanup();
}

// 抛出异常
throw new InvalidOperationException("操作无效");
throw new ArgumentException("参数不能为空", nameof(name));  // 带参数名

// C# 自定义异常
public class PlcCommunicationException : Exception
{
    public string DeviceAddress { get; }
    
    public PlcCommunicationException(string message, string deviceAddress)
        : base(message)
    {
        DeviceAddress = deviceAddress;
    }
    
    public PlcCommunicationException(string message, string deviceAddress, Exception inner)
        : base(message, inner)
    {
        DeviceAddress = deviceAddress;
    }
}
```

```java
// Java
try {
    int result = divide(10, 0);
} catch (ArithmeticException e) {
    System.out.println("除零错误: " + e.getMessage());
} catch (Exception e) {
    System.out.println("其他错误: " + e.getMessage());
} finally {
    cleanup();
}

// Java 的 multi-catch（C# 没有）
try {
    riskyOperation();
} catch (IOException | SQLException e) {
    // 同时捕获多种异常
    System.out.println(e.getMessage());
}

// 抛出异常
throw new IllegalStateException("操作无效");
throw new IllegalArgumentException("参数不能为空");
```

**关键差异：**

| 特性 | C# | Java |
|------|-----|------|
| 检查异常 | 无（所有异常都是 unchecked） | 有（`throws` 声明，编译器强制处理） |
| 异常过滤器 | `catch (Exception e) when (condition)` | 无（需要 catch 后 if 判断） |
| Multi-catch | 无（用多个 catch 块） | `catch (A \| B e)` |
| Rethrow | `throw;`（保留堆栈）/ `throw ex;`（丢失堆栈） | `throw e;`（保留堆栈） |
| 异常规范 | 无 | `throws IOException`（方法声明抛出的异常） |

---

### 十二、命名空间与程序集

```csharp
// C# —— 命名空间 + using
namespace CleanArchitecture.Application.TodoItems.Commands;  // 文件范围命名空间（C# 10+）

using CleanArchitecture.Domain.Entities;
using System.Text.Json;
using MediatR;

// 也可以给 using 起别名
using Json = System.Text.Json.JsonSerializer;
Json.Serialize(data);

// Global using（C# 10+，放在 GlobalUsings.cs 中）
global using System;
global using System.Collections.Generic;
global using System.Linq;
global using System.Threading;
global using System.Threading.Tasks;
global using MediatR;
```

```java
// Java —— 包 + import
package com.example.application.todoitems.commands;  // 包名必须匹配目录结构

import com.example.domain.entities.*;       // 通配符 import
import java.util.List;
import java.time.LocalDateTime;

// Java 的包名和目录结构必须一致
// C# 的命名空间和目录结构可以不一致（但建议保持一致）
```

**关键差异：**

| 特性 | C# | Java |
|------|-----|------|
| 命名规则 | PascalCase（`CleanArchitecture.Application`） | 全小写（`com.example.application`） |
| 目录对应 | 建议但不强制 | 强制 |
| 文件范围 | `namespace X;` 一行搞定 | `package X;` 一行（但不含花括号） |
| 全局引用 | `global using`（C# 10+） | 无等价物 |
| 别名 | `using Json = ...` | 无（需要写全限定名） |
| 多命名空间 | 一个文件可以有多个命名空间 | 一个文件只能有一个包 |

---

### 十三、其他重要差异速查表

| 特性 | C# | Java |
|------|-----|------|
| 入口点 | `Program.cs`（顶级语句 `await app.Run();`） | `public static void main(String[] args)` |
| 输出 | `Console.WriteLine()` | `System.out.println()` |
| 数组初始化 | `new[] { 1, 2, 3 }` 或 `new int[] { 1, 2, 3 }` | `new int[] { 1, 2, 3 }` |
| 集合初始化 | `new List<int> { 1, 2, 3 }` | `List.of(1, 2, 3)`（不可变）或 `new ArrayList<>(List.of(1,2,3))` |
| 字典初始化 | `new Dictionary<string, int> { ["a"] = 1 }` | `Map.of("a", 1)`（不可变） |
| Null 合并 | `x ?? y`（x 为 null 时返回 y） | `Objects.requireNonNullElse(x, y)` |
| Null 条件 | `obj?.Method()`（obj 为 null 时返回 null） | 无内置（Java 14 有 `Optional`） |
| 字符串相等 | `a == b`（值比较） | `a.equals(b)`（值比较） |
| 枚举 | `enum Day { Mon, Tue }` + 可以有方法和字段 | `enum Day { MON, TUESDAY }` + 可以有方法和字段 |
| 扩展方法 | `static void Ext(this string s)` | 无（需要工具类） |
| LINQ | 语言级别支持 | 无（需要 Stream API） |
| 属性 | `public int Age { get; set; }` | getter/setter 方法 |
| 索引器 | `public int this[int i] { get; set; }` | 无 |
| 运算符重载 | 支持 `public static bool operator ==(A a, B b)` | 不支持 |
| `using` 语句 | `using var stream = new FileStream(...)` | try-with-resources |
| `is` / `as` | `obj is string s` / `obj as string` | `instanceof` / 强制转换 |
| 默认参数 | `void Foo(int x = 10)` | 不支持（需要方法重载） |
| 命名参数 | `Foo(name: "Alice", age: 30)` | 不支持 |
| `params` | `void Foo(params int[] nums)` | `void foo(int... nums)` |
| 值类型 | `struct`（用户定义的值类型） | 无（Java 的 Valhalla 项目在做） |
| `Span<T>` | 高性能内存操作 | 无等价物 |

---

### 十四、C# 独有的高频语法模式

以下是 C# 开发中经常使用但 Java 没有直接等价物的语法：

```csharp
// 1. 扩展方法 —— 给现有类型添加方法
public static class StringExtensions
{
    public static bool IsNullOrEmpty(this string? s) => string.IsNullOrEmpty(s);
    public static string Truncate(this string s, int maxLength) 
        => s.Length <= maxLength ? s : s[..maxLength];
}

"hello world".Truncate(5);   // "hello"

// 2. Null 条件运算符链
var city = person?.Address?.City ?? "Unknown";
// 等价于 Java 的：
// Optional.ofNullable(person).map(Person::getAddress).map(Address::getCity).orElse("Unknown")

// 3. 对象初始化器
var person = new Person
{
    Name = "Alice",
    Age = 30,
    Address = new Address { City = "Beijing" }
};

// 4. 集合表达式（C# 12+）
int[] combined = [..list1, ..list2, extraItem];

// 5. required 关键字（C# 11+）
public class Config
{
    public required string ConnectionString { get; set; }  // 必须被初始化
    public int Timeout { get; set; } = 30;                  // 有默认值，可选
}

var config = new Config { ConnectionString = "..." };  // 如果不设置 ConnectionString，编译报错

// 6. 模式匹配赋值
string result = count switch
{
    > 100 => "很多",
    > 10 => "一些",
    > 0 => "少量",
    _ => "无"
};

// 7. using 声明（自动释放资源）
using var file = File.OpenRead("data.txt");
using var reader = new StreamReader(file);
var content = await reader.ReadToEndAsync();
// file 和 reader 在作用域结束时自动 Dispose

// 8. 表达式体成员
public int Area => Width * Height;                              // 属性
public override string ToString() => $"({X}, {Y})";            // 方法
public static implicit operator string(Point p) => p.ToString(); // 转换运算符
```
