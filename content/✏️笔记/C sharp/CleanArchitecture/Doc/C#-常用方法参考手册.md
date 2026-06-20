## C# 常用方法参考手册

> 按类别整理的 C# 开发中最常用的方法速查表。
> 每个方法包含：签名、参数说明、作用、使用场景、注意事项。

---

### 一、String 字符串方法

#### 1.1 查找与判断

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `Contains(string)` | 要查找的子串 | `bool` | 判断是否包含指定子串 |
| `StartsWith(string)` | 前缀字符串 | `bool` | 判断是否以指定前缀开头 |
| `EndsWith(string)` | 后缀字符串 | `bool` | 判断是否以指定后缀结尾 |
| `IndexOf(string)` | 要查找的子串 | `int`（从0开始，未找到返回-1） | 返回子串首次出现的位置 |
| `LastIndexOf(string)` | 要查找的子串 | `int` | 返回子串最后出现的位置 |

```csharp
string path = "C:\\Users\\admin\\Desktop\\file.txt";

path.Contains("admin");        // true
path.StartsWith("C:\\");       // true
path.EndsWith(".txt");         // true
path.IndexOf("admin");         // 10
path.LastIndexOf("\\");        // 18
```

**注意细节：**
- `Contains`、`StartsWith`、`EndsWith` 默认区分大小写。如需忽略大小写，使用 `StringComparison` 参数：`path.Contains("Admin", StringComparison.OrdinalIgnoreCase)`
- `IndexOf` 找不到返回 `-1`，不是 `0`。做判断时要写 `if (index >= 0)` 而不是 `if (index > 0)`
- 在循环中频繁调用 `Contains` 时，考虑使用 `HashSet<string>` 替代

#### 1.2 截取与分割

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `Substring(int)` | 起始索引 | `string` | 从指定位置截取到末尾 |
| `Substring(int, int)` | 起始索引, 长度 | `string` | 从指定位置截取指定长度 |
| `Split(char[])` | 分隔符数组 | `string[]` | 按分隔符拆分为数组 |
| `Split(string, StringSplitOptions)` | 分隔符, 选项 | `string[]` | 按字符串分隔，可去除空项 |

```csharp
string csv = "apple,banana,,cherry,date";

// 基础分割
csv.Split(',');
// → ["apple", "banana", "", "cherry", "date"]

// 去除空项
csv.Split(',', StringSplitOptions.RemoveEmptyEntries);
// → ["apple", "banana", "cherry", "date"]

// 限制分割次数
csv.Split(',', 3);
// → ["apple", "banana", ",cherry,date"]  ← 只分割前2个逗号

// Substring
string name = "Hello World";
name.Substring(6);       // "World"
name.Substring(0, 5);    // "Hello"
```

**注意细节：**
- `Substring(startIndex, length)` 的第二个参数是**长度**不是结束位置（与 Java 的 `substring(beginIndex, endIndex)` 不同！）
- `Substring` 超出范围会抛 `ArgumentOutOfRangeException`，调用前务必检查长度
- `Split` 传入 `StringSplitOptions.TrimEntries`（.NET 5+）可以同时去除空白

#### 1.3 替换与去除

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `Replace(string, string)` | 旧值, 新值 | `string` | 替换所有匹配的子串 |
| `Replace(char, char)` | 旧字符, 新字符 | `string` | 替换所有匹配的字符 |
| `Trim()` | 无 | `string` | 去除首尾空白字符 |
| `TrimStart()` / `TrimEnd()` | 无 | `string` | 去除开头/结尾空白 |
| `Trim(char)` | 要去除的字符 | `string` | 去除首尾指定字符 |
| `PadLeft(int)` / `PadRight(int)` | 总宽度 | `string` | 左/右填充空格到指定宽度 |
| `PadLeft(int, char)` | 总宽度, 填充字符 | `string` | 左填充指定字符 |

```csharp
"Hello World".Replace("World", "C#");    // "Hello C#"
"  hello  ".Trim();                        // "hello"
"007".PadLeft(5, '0');                     // "00007"
"42".PadRight(5, '.');                     // "42..."

// 去除引号
"\"hello\"".Trim('"');                     // "hello"

// 去除路径中的多余斜杠
"C:\\\\Users\\\\admin".Replace("\\\\", "\\");  // "C:\Users\admin"
```

**注意细节：**
- `Replace` 替换所有匹配项（不是只替换第一个，这点和 Java 的 `String.replace` 一样，但和 Java 的 `replaceFirst` 不同）
- `Replace` 不支持正则，需要正则替换请用 `Regex.Replace`
- `Trim()` 只去除空白字符（空格、Tab、换行等），不去除所有 Unicode 空白。如需去除 Unicode 空白使用 `Trim(char.GetUnicodeCategory...)`

#### 1.4 拼接与格式化

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `string.Join(string, IEnumerable)` | 分隔符, 集合 | `string` | 将集合元素用分隔符拼接 |
| `string.Concat(params string[])` | 多个字符串 | `string` | 拼接多个字符串 |
| `string.Format(string, params object[])` | 格式串, 参数 | `string` | 格式化字符串 |
| `string.IsNullOrEmpty(string)` | 字符串 | `bool` | 判断是否为 null 或空串 |
| `string.IsNullOrWhiteSpace(string)` | 字符串 | `bool` | 判断是否为 null、空串或纯空白 |

```csharp
var names = new[] { "Alice", "Bob", "Charlie" };
string.Join(", ", names);                  // "Alice, Bob, Charlie"

// 拼接路径
string.Join("\\", new[] { "C:", "Users", "admin" });  // "C:\Users\admin"

// 判断字符串
string.IsNullOrEmpty("");                  // true
string.IsNullOrEmpty(null);                // true
string.IsNullOrEmpty(" ");                 // false  ← 注意！空格不是空的

string.IsNullOrWhiteSpace("");             // true
string.IsNullOrWhiteSpace("  ");           // true  ← 纯空白也算
string.IsNullOrWhiteSpace(null);           // true

// 字符串插值（推荐方式，替代 string.Format）
var name = "Alice";
var age = 30;
$"Name: {name}, Age: {age}";               // "Name: Alice, Age: 30"
$"Price: {99.5:C}";                         // "Price: ¥99.50"（取决于文化）
$"Today: {DateTime.Now:yyyy-MM-dd}";        // "Today: 2026-06-20"
```

**注意细节：**
- `IsNullOrEmpty` vs `IsNullOrWhiteSpace`：在实际业务中，大多数时候应该用 `IsNullOrWhiteSpace`，因为用户输入的 `" "` 通常应该被视为空
- 在循环中拼接大量字符串时，使用 `StringBuilder` 而不是 `+` 或 `string.Concat`，避免产生大量中间字符串对象
- `$"..."` 字符串插值底层使用 `string.Create` 和 `IFormattable`，性能优于 `string.Format`

---

### 二、List\<T> 列表方法

#### 2.1 增删元素

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `Add(T)` | 元素 | `void` | 在末尾添加元素 |
| `AddRange(IEnumerable<T>)` | 集合 | `void` | 批量添加多个元素 |
| `Insert(int, T)` | 索引, 元素 | `void` | 在指定位置插入 |
| `Remove(T)` | 元素 | `bool` | 移除第一个匹配项 |
| `RemoveAt(int)` | 索引 | `void` | 移除指定位置的元素 |
| `RemoveAll(Predicate<T>)` | 条件 | `int`（移除数量） | 移除所有满足条件的元素 |
| `Clear()` | 无 | `void` | 清空所有元素 |

```csharp
var list = new List<string> { "apple", "banana", "cherry" };

list.Add("date");                          // ["apple","banana","cherry","date"]
list.AddRange(new[] { "elderberry", "fig" });  // 批量添加
list.Insert(1, "avocado");                 // 在索引1处插入

list.Remove("banana");                     // 移除第一个"banana"
list.RemoveAt(0);                          // 移除第一个元素
list.RemoveAll(x => x.StartsWith("e"));    // 移除所有以"e"开头的
list.Clear();                              // 清空
```

**注意细节：**
- `Remove` 对于自定义类型，需要正确实现 `Equals` 和 `GetHashCode`（或使用 `IEquatable<T>`）
- `RemoveAll` 是 `List<T>` 独有的方法，`IList<T>` 接口没有。它是原地修改，不会创建新列表
- 在 `foreach` 循环中不能调用 `Remove`/`RemoveAt`，会抛 `InvalidOperationException`。应该用 `RemoveAll` 或者用 `for` 循环倒序删除
- `List<T>` 不是线程安全的。多线程环境下使用 `ConcurrentBag<T>` 或加锁

#### 2.2 查找与筛选

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `Find(Predicate<T>)` | 条件 | `T`（未找到返回 default） | 返回第一个满足条件的元素 |
| `FindAll(Predicate<T>)` | 条件 | `List<T>` | 返回所有满足条件的元素 |
| `FindIndex(Predicate<T>)` | 条件 | `int`（未找到返回-1） | 返回第一个满足条件的索引 |
| `Exists(Predicate<T>)` | 条件 | `bool` | 判断是否存在满足条件的元素 |
| `Contains(T)` | 元素 | `bool` | 判断是否包含指定元素 |
| `IndexOf(T)` | 元素 | `int` | 返回元素的索引 |

```csharp
var numbers = new List<int> { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

numbers.Find(x => x > 5);                 // 6
numbers.FindAll(x => x % 2 == 0);        // [2, 4, 6, 8, 10]
numbers.FindIndex(x => x > 5);            // 5（值为6的索引）
numbers.Exists(x => x > 100);             // false
numbers.Contains(7);                       // true
numbers.IndexOf(7);                        // 6

// 实际场景：在设备列表中查找
var devices = new List<Device> { ... };
var target = devices.Find(d => d.PlcAddress == "192.168.1.100");
if (target != null) { /* 找到了 */ }

// 更安全的写法（C# 9+）
var target = devices.Find(d => d.PlcAddress == "192.168.1.100");
if (target is not null) { /* 找到了 */ }
```

**注意细节：**
- `Find` 找不到引用类型返回 `null`，找不到值类型返回 `default(T)`（int 返回 0）。对于值类型建议用 `FindIndex` + 索引访问
- `Find` 和 `FirstOrDefault`（LINQ）功能类似，但 `Find` 是 `List<T>` 的方法，性能略优；`FirstOrDefault` 适用于所有 `IEnumerable<T>`
- `Exists` 比 `Find` + null 判断更清晰，推荐在只判断存在性时使用

#### 2.3 排序与转换

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `Sort()` | 无 | `void` | 原地排序（需要类型实现 IComparable） |
| `Sort(Comparison<T>)` | 比较委托 | `void` | 用自定义比较器原地排序 |
| `Reverse()` | 无 | `void` | 原地反转列表 |
| `ToArray()` | 无 | `T[]` | 转为数组 |
| `ConvertAll<TOutput>(Converter)` | 转换函数 | `List<TOutput>` | 将所有元素转换为另一种类型 |
| `ForEach(Action<T>)` | 动作 | `void` | 对每个元素执行操作 |

```csharp
var numbers = new List<int> { 5, 3, 8, 1, 9, 2 };

numbers.Sort();                             // [1, 2, 3, 5, 8, 9]
numbers.Sort((a, b) => b.CompareTo(a));    // [9, 8, 5, 3, 2, 1] 降序
numbers.Reverse();                          // 反转

// ConvertAll：类型转换
var strings = new List<int> { 1, 2, 3 }.ConvertAll(x => x.ToString());

// ForEach：批量操作
var names = new List<string> { "alice", "bob" };
names.ForEach(n => Console.WriteLine(n.ToUpper()));

// 实际场景：批量更新状态
var orders = GetAllOrders();
orders.ForEach(o => o.Status = "Processed");
```

**注意细节：**
- `Sort()` 是原地排序（修改原列表），LINQ 的 `OrderBy` 返回新的 `IOrderedEnumerable`（不修改原列表）
- `ForEach` 是 `List<T>` 独有的方法，不能在 `IEnumerable<T>` 上使用。在 LINQ 链中，应该用 `foreach` 循环替代
- `ConvertAll` 和 LINQ 的 `Select` + `ToList()` 功能等价，但 `ConvertAll` 性能更好（预分配数组大小）

---

### 三、LINQ 方法

LINQ 是 C# 最强大的特性之一，以下是最常用的方法。

#### 3.1 筛选与投影

| 方法 | 参数 | 作用 | 使用场景 |
|------|------|------|----------|
| `Where(Func<T, bool>)` | 条件 | 筛选满足条件的元素 | 过滤数据 |
| `Select(Func<T, TResult>)` | 映射函数 | 将每个元素投影为新形式 | 提取字段、类型转换 |
| `SelectMany(Func<T, IEnumerable<TResult>>)` | 映射函数 | 将嵌套集合展平 | 一对多关系展平 |
| `OfType<TResult>()` | 无 | 筛选指定类型的元素 | 混合类型集合过滤 |
| `Cast<TResult>()` | 无 | 将所有元素转换为目标类型 | 非泛型集合转泛型 |

```csharp
var orders = new List<Order> { /* ... */ };

// Where：筛选
var highValueOrders = orders.Where(o => o.Total > 1000);

// Select：投影
var orderIds = orders.Select(o => o.Id);
var summaries = orders.Select(o => new { o.CustomerName, o.Total });

// SelectMany：展平嵌套集合
// 每个 Order 包含多个 OrderItems，展平为所有订单项的列表
var allItems = orders.SelectMany(o => o.Items);

// 组合使用：找出所有金额大于100的订单项
var expensiveItems = orders
    .SelectMany(o => o.Items)
    .Where(item => item.Price > 100);

// 查询语法等价写法
var query = from o in orders
            where o.Total > 1000
            select new { o.CustomerName, o.Total };
```

#### 3.2 聚合

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `Count()` | 无 | `int` | 元素总数 |
| `Count(Func<T, bool>)` | 条件 | `int` | 满足条件的元素数 |
| `LongCount()` | 无 | `long` | 大集合的元素总数 |
| `Sum(Func<T, decimal>)` | 选择器 | `decimal` | 求和 |
| `Average(Func<T, decimal>)` | 选择器 | `decimal` | 平均值 |
| `Min()` / `Max()` | 无 | `T` | 最小/最大值 |
| `MinBy(Func<T, TKey>)` | 键选择器 | `T` | 按指定键取最小的元素（.NET 6+） |
| `MaxBy(Func<T, TKey>)` | 键选择器 | `T` | 按指定键取最大的元素（.NET 6+） |
| `Aggregate(func)` | 累积函数 | `T` | 自定义聚合 |

```csharp
var prices = new[] { 10.5m, 20.0m, 15.75m, 8.25m, 30.0m };

prices.Count();                              // 5
prices.Count(p => p > 15);                   // 3
prices.Sum();                                // 84.50
prices.Average();                            // 16.90
prices.Min();                                // 8.25
prices.Max();                                // 30.00

// MinBy / MaxBy：返回元素本身，而非键值
var orders = new List<Order> { ... };
var mostExpensive = orders.MaxBy(o => o.Total);  // 返回 Order 对象
var cheapest = orders.MinBy(o => o.Total);

// Aggregate：自定义聚合（类似 JavaScript 的 reduce）
var total = prices.Aggregate(0m, (acc, price) => acc + price);
var csv = names.Aggregate((acc, name) => acc + ", " + name);
```

#### 3.3 元素获取

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `First()` | 无 | `T` | 第一个元素（空则抛异常） |
| `First(Func<T, bool>)` | 条件 | `T` | 第一个满足条件的元素 |
| `FirstOrDefault()` | 无 | `T?` | 第一个元素（空则返回 default） |
| `FirstOrDefault(Func<T, bool>)` | 条件 | `T?` | 第一个满足条件的元素 |
| `Single()` | 无 | `T` | 唯一元素（0个或2个以上抛异常） |
| `SingleOrDefault()` | 无 | `T?` | 唯一元素（0个返回 default，2个以上抛异常） |
| `Last()` / `LastOrDefault()` | 同 First | `T` / `T?` | 最后一个元素 |
| `ElementAt(int)` | 索引 | `T` | 指定位置的元素 |
| `ElementAtOrDefault(int)` | 索引 | `T?` | 指定位置的元素（越界返回 default） |

```csharp
var list = new List<string> { "alpha", "beta", "gamma" };

list.First();                                // "alpha"
list.First(x => x.StartsWith("g"));         // "gamma"
list.FirstOrDefault(x => x == "delta");    // null（不抛异常）
list.Single();                               // 抛异常（因为有3个元素）
list.ElementAt(1);                           // "beta"
list.ElementAtOrDefault(99);                 // null（不抛异常）

// 实际场景：数据库查询
var user = dbContext.Users.FirstOrDefault(u => u.Email == "admin@test.com");
if (user != null)
{
    // 找到了
}

// 期望有且只有一个的场景
var config = dbContext.Configs.Single(c => c.Key == "MaxRetries");
// 如果配置缺失或有重复，会抛异常，适合在启动时校验配置完整性
```

**注意细节：**
- `First` vs `Single`：`First` 取第一个不管后面有多少，`Single` 要求严格只有 1 个。在业务逻辑中，如果"应该只有一个"，用 `Single` 可以在数据异常时尽早报错
- `FirstOrDefault` 对引用类型返回 `null`，对值类型返回 `default(T)`（int 返回 0，DateTime 返回 `0001-01-01`）。如果 `default` 值在业务中有含义，需要用 `DefaultIfEmpty(specificDefault)` 指定
- EF Core 中 `First`/`Single` 会生成 `TOP(1)`/`TOP(2)` SQL，`Single` 取 2 条是为了验证唯一性

#### 3.4 分组与排序

| 方法 | 参数 | 作用 |
|------|------|------|
| `OrderBy(Func<T, TKey>)` | 键选择器 | 升序排序 |
| `OrderByDescending(Func<T, TKey>)` | 键选择器 | 降序排序 |
| `ThenBy(Func<T, TKey>)` | 键选择器 | 次要键升序（跟在 OrderBy 后面） |
| `ThenByDescending(Func<T, TKey>)` | 键选择器 | 次要键降序 |
| `GroupBy(Func<T, TKey>)` | 键选择器 | 按键分组 |
| `Reverse()` | 无 | 反转序列 |

```csharp
var orders = new List<Order> { ... };

// 多字段排序
var sorted = orders
    .OrderBy(o => o.CustomerName)          // 先按客户名升序
    .ThenByDescending(o => o.Total);       // 再按金额降序

// 分组统计
var grouped = orders.GroupBy(o => o.CustomerName);
foreach (var group in grouped)
{
    Console.WriteLine($"{group.Key}: {group.Count()} orders, Total: {group.Sum(o => o.Total)}");
}

// 分组 + 投影
var summary = orders
    .GroupBy(o => o.CustomerName)
    .Select(g => new
    {
        Customer = g.Key,
        OrderCount = g.Count(),
        TotalAmount = g.Sum(o => o.Total),
        AverageAmount = g.Average(o => o.Total)
    })
    .OrderByDescending(s => s.TotalAmount);
```

#### 3.5 集合操作

| 方法 | 参数 | 作用 |
|------|------|------|
| `Distinct()` | 无 | 去重 |
| `DistinctBy(Func<T, TKey>)` | 键选择器 | 按指定键去重（.NET 6+） |
| `Union(IEnumerable<T>)` | 另一个集合 | 并集（去重） |
| `Intersect(IEnumerable<T>)` | 另一个集合 | 交集 |
| `Except(IEnumerable<T>)` | 另一个集合 | 差集 |
| `Any()` / `Any(Func<T, bool>)` | 条件（可选） | 是否存在满足条件的元素 |
| `All(Func<T, bool>)` | 条件 | 是否所有元素都满足条件 |
| `SequenceEqual(IEnumerable<T>)` | 另一个集合 | 两个序列是否完全相同 |
| `Skip(int)` | 数量 | 跳过前 N 个元素 |
| `Take(int)` | 数量 | 取前 N 个元素 |

```csharp
var a = new[] { 1, 2, 3, 4, 5 };
var b = new[] { 4, 5, 6, 7, 8 };

a.Union(b);           // [1, 2, 3, 4, 5, 6, 7, 8]
a.Intersect(b);       // [4, 5]
a.Except(b);          // [1, 2, 3]
a.Distinct();          // 去重

// 分页：Skip + Take
int page = 2, pageSize = 10;
var pagedData = orders
    .OrderBy(o => o.Id)
    .Skip((page - 1) * pageSize)
    .Take(pageSize)
    .ToList();

// DistinctBy：按指定属性去重（保留第一个）
var uniqueCustomers = orders.DistinctBy(o => o.CustomerId);

// Any vs Count
// 判断是否有数据，用 Any() 不要用 Count() > 0
// Any() 找到第一个就返回，Count() 会遍历全部
bool hasErrors = logs.Any(l => l.Level == LogLevel.Error);   // 高效
bool hasErrors2 = logs.Count(l => l.Level == LogLevel.Error) > 0;  // 低效
```

**注意细节：**
- LINQ 方法大多返回 `IEnumerable<T>`，是**延迟执行**的。只有调用 `ToList()`、`ToArray()`、`Count()`、`First()` 等"终结"方法时才真正执行
- 多次遍历同一个 `IEnumerable<T>` 会导致重复计算（尤其是数据库查询）。如果需要多次使用，先 `ToList()` 缓存
- `Union`/`Intersect`/`Except` 默认使用 `EqualityComparer<T>.Default` 比较。对于自定义类型需要实现 `IEquatable<T>` 或传入自定义比较器

---

### 四、Dictionary\<TKey, TValue> 方法

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `Add(TKey, TValue)` | 键, 值 | `void` | 添加键值对（键已存在则抛异常） |
| `TryAdd(TKey, TValue)` | 键, 值 | `bool` | 尝试添加（.NET Core 2.0+，键已存在返回 false） |
| `Remove(TKey)` | 键 | `bool` | 移除指定键 |
| `ContainsKey(TKey)` | 键 | `bool` | 判断键是否存在 |
| `TryGetValue(TKey, out TValue)` | 键, out 值 | `bool` | 安全获取值 |
| `GetValueOrDefault(TKey)` | 键 | `TValue?` | 获取值（不存在返回 default） |

```csharp
var dict = new Dictionary<string, int>
{
    ["apple"] = 5,
    ["banana"] = 3,
    ["cherry"] = 10
};

// 安全读取（推荐方式）
if (dict.TryGetValue("apple", out int count))
{
    Console.WriteLine($"apple: {count}");  // apple: 5
}

// 或使用 GetValueOrDefault（.NET Core 2.0+）
int value = dict.GetValueOrDefault("grape", 0);  // 0（指定默认值）

// 索引器直接访问（键不存在会抛 KeyNotFoundException！）
dict["apple"];      // 5
// dict["grape"];   // 抛 KeyNotFoundException

// Add vs 索引器
dict.Add("date", 7);        // 如果"date"已存在，抛 ArgumentException
dict["date"] = 8;           // 如果"date"已存在，覆盖旧值

// TryAdd（.NET Core 2.0+）
dict.TryAdd("date", 7);     // 返回 false（"date"已存在），不抛异常

// 遍历
foreach (var kvp in dict)
{
    Console.WriteLine($"{kvp.Key} = {kvp.Value}");
}

// 实际场景：设备状态映射
var deviceStatus = new Dictionary<string, DeviceState>();
deviceStatus["PLC-001"] = DeviceState.Running;
deviceStatus["PLC-002"] = DeviceState.Stopped;

// GroupBy 结果转 Dictionary
var statusGroups = orders
    .GroupBy(o => o.Status)
    .ToDictionary(g => g.Key, g => g.Count());
```

**注意细节：**
- `dict[key]` 在键不存在时抛 `KeyNotFoundException`，而不是返回 null。务必先 `ContainsKey` 或使用 `TryGetValue`
- `Dictionary<TKey, TValue>` 不是线程安全的。多线程环境使用 `ConcurrentDictionary<TKey, TValue>`
- `ConcurrentDictionary` 提供了原子操作 `GetOrAdd` 和 `AddOrUpdate`，非常适合缓存场景
- 遍历 Dictionary 的顺序不保证是插入顺序（虽然 .NET Core 的实现事实上保持了插入顺序，但不应依赖此行为）

---

### 五、File / Path / Directory 文件操作方法

#### 5.1 File 静态方法

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `ReadAllText(string)` | 路径 | `string` | 读取全部文本 |
| `ReadAllText(string, Encoding)` | 路径, 编码 | `string` | 以指定编码读取 |
| `ReadAllLines(string)` | 路径 | `string[]` | 按行读取为数组 |
| `ReadAllBytes(string)` | 路径 | `byte[]` | 读取为字节数组 |
| `WriteAllText(string, string)` | 路径, 内容 | `void` | 写入文本（覆盖） |
| `WriteAllText(string, string, Encoding)` | 路径, 内容, 编码 | `void` | 以指定编码写入 |
| `WriteAllLines(string, IEnumerable)` | 路径, 行集合 | `void` | 按行写入 |
| `AppendAllText(string, string)` | 路径, 内容 | `void` | 追加文本 |
| `Exists(string)` | 路径 | `bool` | 文件是否存在 |
| `Copy(string, string, bool)` | 源, 目标, 覆盖 | `void` | 复制文件 |
| `Move(string, string)` | 源, 目标 | `void` | 移动/重命名文件 |
| `Delete(string)` | 路径 | `void` | 删除文件 |

```csharp
// 读取配置文件
string config = File.ReadAllText("config.json");
string configUtf8 = File.ReadAllText("config.json", Encoding.UTF8);

// 读取日志文件（按行）
string[] lines = File.ReadAllLines("log.txt");
var errorLines = lines.Where(l => l.Contains("ERROR"));

// 写入文件
File.WriteAllText("output.txt", "Hello World", Encoding.UTF8);
File.WriteAllLines("output.txt", new[] { "line1", "line2", "line3" });

// 追加日志
File.AppendAllText("app.log", $"[{DateTime.Now}] Operation completed\n");

// 读取二进制文件（图片、PDF 等）
byte[] data = File.ReadAllBytes("image.png");

// 实际场景：读取 PLC 配置文件
var plcConfig = File.ReadAllText("plc-config.json");
var settings = JsonSerializer.Deserialize<PlcSettings>(plcConfig);
```

**注意细节：**
- `ReadAllText` 默认使用 UTF-8 编码。读取 GBK 编码的文件（如某些中文 Windows 生成的文件）需要指定编码：`File.ReadAllText(path, Encoding.GetEncoding("GBK"))`
- `ReadAllLines` 会一次性将整个文件加载到内存。对于大文件（> 100MB），使用 `File.ReadLines`（延迟读取，一次只读一行）
- `WriteAllText` 是覆盖写入，不是追加。追加使用 `AppendAllText`
- 所有 File 方法在文件不存在时，读取类方法抛 `FileNotFoundException`，`Delete`/`Exists` 不抛异常

#### 5.2 Path 静态方法

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `Combine(params string[])` | 路径片段 | `string` | 拼接路径 |
| `GetFileName(string)` | 路径 | `string` | 获取文件名（含扩展名） |
| `GetFileNameWithoutExtension(string)` | 路径 | `string` | 获取文件名（不含扩展名） |
| `GetExtension(string)` | 路径 | `string` | 获取扩展名（含点号） |
| `GetDirectoryName(string)` | 路径 | `string` | 获取目录部分 |
| `ChangeExtension(string, string)` | 路径, 新扩展名 | `string` | 修改扩展名 |
| `GetTempPath()` | 无 | `string` | 获取系统临时目录 |
| `GetTempFileName()` | 无 | `string` | 创建临时文件并返回路径 |
| `GetRandomFileName()` | 无 | `string` | 生成随机文件名 |

```csharp
// 拼接路径（推荐方式，不要手动拼 \ 或 /）
Path.Combine("C:", "Users", "admin", "Desktop");   // "C:\Users\admin\Desktop"
Path.Combine(basePath, "logs", "app.log");

// 解析路径
string filePath = @"C:\Users\admin\Desktop\report.pdf";
Path.GetFileName(filePath);              // "report.pdf"
Path.GetFileNameWithoutExtension(filePath);  // "report"
Path.GetExtension(filePath);             // ".pdf"
Path.GetDirectoryName(filePath);         // "C:\Users\admin\Desktop"

// 修改扩展名
Path.ChangeExtension("report.pdf", ".docx");  // "report.docx"

// 实际场景：生成带时间戳的备份文件名
string backupPath = Path.Combine(
    backupDir,
    $"{Path.GetFileNameWithoutExtension(original)}_{DateTime.Now:yyyyMMdd_HHmmss}{Path.GetExtension(original)}"
);
// → "backup\database_20260620_143000.bak"
```

**注意细节：**
- 始终使用 `Path.Combine` 拼接路径，不要手动用 `+` 和 `\` 拼接。`Path.Combine` 会自动处理分隔符，跨平台兼容
- `Path` 的方法都是纯字符串操作，不检查文件是否存在
- `GetExtension` 返回的扩展名包含点号（`.pdf`），如果文件没有扩展名返回空字符串（不是 null）

---

### 六、DateTime 日期时间方法

| 方法/属性 | 参数 | 返回值 | 作用 |
|-----------|------|--------|------|
| `DateTime.Now` | — | `DateTime` | 当前本地时间 |
| `DateTime.UtcNow` | — | `DateTime` | 当前 UTC 时间 |
| `DateTime.Today` | — | `DateTime` | 今天（时间部分为 00:00:00） |
| `AddDays(double)` | 天数 | `DateTime` | 加/减天数 |
| `AddHours(double)` | 小时数 | `DateTime` | 加/减小时 |
| `AddMonths(int)` | 月数 | `DateTime` | 加/减月份 |
| `AddYears(int)` | 年数 | `DateTime` | 加/减年份 |
| `ToString(string)` | 格式串 | `string` | 格式化输出 |
| `Parse(string)` | 字符串 | `DateTime` | 解析字符串为日期（失败抛异常） |
| `TryParse(string, out DateTime)` | 字符串, out 结果 | `bool` | 安全解析 |
| `ToString("o")` | — | `string` | ISO 8601 格式 |
| `(dt1 - dt2).TotalHours` | — | `double` | 两个日期的时间差 |

```csharp
var now = DateTime.Now;

// 格式化
now.ToString("yyyy-MM-dd");                  // "2026-06-20"
now.ToString("yyyy-MM-dd HH:mm:ss");         // "2026-06-20 14:30:00"
now.ToString("yyyyMMdd_HHmmss");             // "20260620_143000"
now.ToString("yyyy年MM月dd日 dddd");          // "2026年06月20日 星期五"
now.ToString("o");                            // "2026-06-20T14:30:00.0000000+08:00" (ISO 8601)

// 日期计算
now.AddDays(7);                              // 7天后
now.AddDays(-1);                             // 昨天
now.AddMonths(1);                            // 下个月的今天
now.AddHours(2.5);                           // 2.5小时后

// 日期差
var start = new DateTime(2026, 1, 1);
var days = (now - start).Days;               // 今年的第几天
var hours = (now - start).TotalHours;        // 小时数（含小数）

// 获取月份的第一天和最后一天
var firstDayOfMonth = new DateTime(now.Year, now.Month, 1);
var lastDayOfMonth = firstDayOfMonth.AddMonths(1).AddDays(-1);

// 安全解析
if (DateTime.TryParse("2026-06-20", out var parsed))
{
    Console.WriteLine(parsed.Year);           // 2026
}

// 解析中文日期格式（需要指定文化）
DateTime.Parse("2026年6月20日", CultureInfo.GetCultureInfo("zh-CN"));
```

**注意细节：**
- `DateTime.Now` 是本地时间，`DateTime.UtcNow` 是 UTC 时间。存储到数据库的时间建议使用 UTC，展示给用户时转换为本地时间
- `DateTime` 是不可变类型（immutable），`AddDays` 等方法返回新的 `DateTime`，不会修改原值
- `Parse` 在格式不匹配时抛 `FormatException`，生产代码中应使用 `TryParse`
- 时间差用 `TimeSpan` 表示。`TotalDays`/`TotalHours` 返回 `double`（含小数），`Days`/`Hours` 返回 `int`（取整）
- 如果需要精确的时间计算（如计时器），使用 `Stopwatch` 而不是 `DateTime.Now` 相减

---

### 七、Math / 数值方法

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `Math.Abs(T)` | 数值 | `T` | 绝对值 |
| `Math.Max(T, T)` | 两个值 | `T` | 较大值 |
| `Math.Min(T, T)` | 两个值 | `T` | 较小值 |
| `Math.Clamp(T, T, T)` | 值, 最小, 最大 | `T` | 限制在范围内（.NET Core 2.0+） |
| `Math.Round(double, int)` | 值, 小数位 | `double` | 四舍五入 |
| `Math.Ceiling(double)` | 值 | `double` | 向上取整 |
| `Math.Floor(double)` | 值 | `double` | 向下取整 |
| `Math.Truncate(double)` | 值 | `double` | 截断小数部分 |
| `Math.Pow(double, double)` | 底数, 指数 | `double` | 幂运算 |
| `Math.Sqrt(double)` | 值 | `double` | 平方根 |
| `int.Parse(string)` | 字符串 | `int` | 字符串转整数 |
| `int.TryParse(string, out int)` | 字符串, out 结果 | `bool` | 安全转换 |
| `decimal.Parse(string)` | 字符串 | `decimal` | 字符串转 decimal |
| `Convert.ToInt32(object)` | 对象 | `int` | 通用转换 |

```csharp
// 数值范围限制
int speed = Math.Clamp(requestedSpeed, 0, 100);  // 限制在 0-100 之间

// 四舍五入
Math.Round(3.45, 1);                    // 3.4  ← 注意：默认使用"银行家舍入"
Math.Round(3.45, 1, MidpointRounding.AwayFromZero);  // 3.5  ← 传统四舍五入
Math.Round(3.55, 1, MidpointRounding.AwayFromZero);  // 3.6

// 向上/向下取整
Math.Ceiling(3.1);                       // 4.0
Math.Floor(3.9);                         // 3.0
Math.Truncate(3.9);                      // 3.0

// 实际场景：计算设备运行时间
var uptime = DateTime.Now - startTime;
var hours = Math.Round(uptime.TotalHours, 1);

// 安全转换字符串为数字
string input = "42";
if (int.TryParse(input, out int number))
{
    Console.WriteLine(number * 2);       // 84
}

// 金额计算用 decimal，不要用 double
decimal price = 19.99m;
decimal tax = price * 0.13m;            // 精确计算
decimal total = Math.Round(price + tax, 2);
```

**注意细节：**
- `Math.Round` 默认使用"银行家舍入"（Banker's Rounding），即 .5 时舍入到偶数。`3.5 → 4`，`4.5 → 4`。如果需要传统四舍五入，使用 `MidpointRounding.AwayFromZero`
- 金额计算必须使用 `decimal`，不要用 `double`/`float`。`double` 有精度丢失问题：`0.1 + 0.2 != 0.3`
- `int.Parse` 在字符串不是有效数字时抛异常，`int.TryParse` 返回 false。生产代码中应该优先使用 `TryParse`
- `Convert.ToInt32(null)` 返回 0，而 `int.Parse(null)` 抛异常

---

### 八、Task / 异步方法

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `Task.Run(Func<T>)` | 委托 | `Task<T>` | 在线程池中执行异步操作 |
| `Task.Delay(int)` | 毫秒数 | `Task` | 延迟指定时间 |
| `Task.WhenAll(params Task[])` | 任务数组 | `Task` | 等待所有任务完成 |
| `Task.WhenAny(params Task[])` | 任务数组 | `Task<Task>` | 等待任一任务完成 |
| `Task.CompletedTask` | — | `Task` | 已完成的 Task（用于返回空异步方法） |
| `Task.FromResult(T)` | 值 | `Task<T>` | 创建已完成的结果 Task |
| `CancellationTokenSource.Cancel()` | — | `void` | 取消异步操作 |

```csharp
// 基本的异步方法
public async Task<string> ReadDataAsync()
{
    var result = await SomeOperationAsync();
    return result;
}

// 并行执行多个异步操作
var task1 = GetDeviceStatusAsync("PLC-001");
var task2 = GetDeviceStatusAsync("PLC-002");
var task3 = GetDeviceStatusAsync("PLC-003");

await Task.WhenAll(task1, task2, task3);

// 获取所有结果
var results = new[] { task1.Result, task2.Result, task3.Result };

// 更好的写法（避免 .Result 阻塞）
var results = await Task.WhenAll(
    GetDeviceStatusAsync("PLC-001"),
    GetDeviceStatusAsync("PLC-002"),
    GetDeviceStatusAsync("PLC-003")
);

// 带超时的异步操作
using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(5));
try
{
    var result = await ReadPlcDataAsync(cts.Token);
}
catch (OperationCanceledException)
{
    Console.WriteLine("读取超时");
}

// 延迟
await Task.Delay(1000);  // 等待 1 秒（不阻塞线程）

// 同步接口返回异步空结果
public Task DoNothingAsync()
{
    return Task.CompletedTask;  // 不要写 async + return
}

// 同步接口返回异步有结果
public Task<int> GetDefaultValueAsync()
{
    return Task.FromResult(42);  // 不要写 async + return 42
}

// 实际场景：批量读取 PLC 数据
public async Task<Dictionary<string, float>> ReadAllRegistersAsync(
    string[] addresses, CancellationToken ct)
{
    var tasks = addresses.Select(addr => 
        ReadRegisterAsync(addr, ct));
    
    var results = await Task.WhenAll(tasks);
    
    return addresses.Zip(results)
        .ToDictionary(pair => pair.First, pair => pair.Second);
}
```

**注意细节：**
- 不要在异步方法中使用 `.Result` 或 `.Wait()`，会导致死锁（特别是在 WinForms/WPF 的 UI 线程上）。始终使用 `await`
- `Task.Delay` 比 `Thread.Sleep` 好，因为前者不阻塞线程
- `CancellationToken` 应该贯穿整个异步调用链。在你的 WinForms 项目中，窗口关闭时应该取消所有正在执行的异步操作
- `async void` 只用于事件处理器（如按钮点击），其他场景应该返回 `Task` 或 `Task<T>`
- `Task.Run` 会将工作放到线程池线程上。对于 I/O 操作（网络、文件），应该使用原生的异步 API（如 `HttpClient.GetAsync`），而不是 `Task.Run(() => syncMethod())`

---

### 九、JsonSerializer（System.Text.Json）

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `Serialize<T>(T, options)` | 对象, 选项 | `string` | 对象序列化为 JSON |
| `Deserialize<T>(string)` | JSON 字符串 | `T` | JSON 反序列化为对象 |
| `SerializeToDocument<T>(T)` | 对象 | `JsonDocument` | 序列化为可查询的 JSON 文档 |

```csharp
using System.Text.Json;
using System.Text.Json.Serialization;

// 序列化
var settings = new PlcSettings { Ip = "192.168.1.100", Port = 502 };
string json = JsonSerializer.Serialize(settings);
// {"Ip":"192.168.1.100","Port":502}

// 美化输出
var options = new JsonSerializerOptions { WriteIndented = true };
string prettyJson = JsonSerializer.Serialize(settings, options);

// 反序列化
var parsed = JsonSerializer.Deserialize<PlcSettings>(json);

// 常用选项
var options = new JsonSerializerOptions
{
    PropertyNameCaseInsensitive = true,              // 属性名不区分大小写
    PropertyNamingPolicy = JsonNamingPolicy.CamelCase, // 输出驼峰命名
    WriteIndented = true,                             // 美化输出
    DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull  // 忽略 null 值
};

// 处理枚举
[JsonConverter(typeof(JsonStringEnumConverter))]
public DeviceState State { get; set; }
// 序列化为 "Running" 而不是 0

// 处理日期格式
var options = new JsonSerializerOptions
{
    // 指定自定义的日期格式
};

// 从文件读取并反序列化
string jsonContent = await File.ReadAllTextAsync("config.json");
var config = JsonSerializer.Deserialize<AppConfig>(jsonContent);

// 反序列化匿名类型/动态数据
var doc = JsonDocument.Parse(json);
var name = doc.RootElement.GetProperty("name").GetString();
var count = doc.RootElement.GetProperty("count").GetInt32();
```

**注意细节：**
- `System.Text.Json` 是 .NET 内置的高性能 JSON 库，性能远超 `Newtonsoft.Json`。新项目应优先使用它
- 默认情况下属性名区分大小写。从 API 接收的 JSON 如果键名大小写不一致，需要设置 `PropertyNameCaseInsensitive = true`
- 反序列化时，类需要有 `public` 无参构造函数（或构造函数参数与 JSON 属性匹配）
- `JsonSerializer.Deserialize<T>` 在 JSON 为 null 时返回 null（对引用类型），不会抛异常

---

### 十、Regex 正则表达式方法

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `Regex.IsMatch(string, string)` | 输入, 模式 | `bool` | 是否匹配 |
| `Regex.Match(string, string)` | 输入, 模式 | `Match` | 第一个匹配 |
| `Regex.Matches(string, string)` | 输入, 模式 | `MatchCollection` | 所有匹配 |
| `Regex.Replace(string, string, string)` | 输入, 模式, 替换 | `string` | 替换所有匹配 |
| `Regex.Split(string, string)` | 输入, 模式 | `string[]` | 按正则分割 |

```csharp
using System.Text.RegularExpressions;

// 验证格式
Regex.IsMatch("admin@test.com", @"^[\w.-]+@[\w.-]+\.\w+$");  // true
Regex.IsMatch("12345", @"^\d{6}$");                            // false（需要6位）

// 提取内容
string text = "订单号：ORD-2026-001，金额：¥199.50";
var orderNo = Regex.Match(text, @"ORD-\d{4}-\d{3}");
orderNo.Value;  // "ORD-2026-001"

// 提取所有数字
var numbers = Regex.Matches("abc123def456ghi789", @"\d+");
foreach (Match m in numbers)
    Console.WriteLine(m.Value);  // 123, 456, 789

// 正则替换
string cleaned = Regex.Replace("  hello   world  ", @"\s+", " ");  // "hello world"
string removed = Regex.Replace("Hello123World456", @"\d", "");      // "HelloWorld"

// 实际场景：解析 PLC 地址
string address = "D100";
var match = Regex.Match(address, @"^([A-Z]+)(\d+)$");
if (match.Success)
{
    string prefix = match.Groups[1].Value;   // "D"
    int number = int.Parse(match.Groups[2].Value);  // 100
}
```

**注意细节：**
- 如果同一个正则表达式会被多次使用，创建 `Regex` 实例并设置 `RegexOptions.Compiled` 以提升性能：`private static readonly Regex _regex = new(@"\d+", RegexOptions.Compiled);`
- `RegexOptions.IgnoreCase` 忽略大小写，`RegexOptions.Multiline` 让 `^`/`$` 匹配每行的开头/结尾
- `@""` 是 C# 的 verbatim 字符串，反斜杠不需要转义。正则表达式应该始终用 `@""`

---

### 十一、HttpClient 常用方法

| 方法 | 参数 | 返回值 | 作用 |
|------|------|--------|------|
| `GetStringAsync(string)` | URL | `Task<string>` | GET 请求返回字符串 |
| `GetByteArrayAsync(string)` | URL | `Task<byte[]>` | GET 请求返回字节数组 |
| `GetStreamAsync(string)` | URL | `Task<Stream>` | GET 请求返回流 |
| `PostAsJsonAsync<T>(string, T)` | URL, 数据 | `Task<HttpResponseMessage>` | POST JSON 数据 |
| `PutAsJsonAsync<T>(string, T)` | URL, 数据 | `Task<HttpResponseMessage>` | PUT JSON 数据 |
| `DeleteAsync(string)` | URL | `Task<HttpResponseMessage>` | DELETE 请求 |
| `SendAsync(HttpRequestMessage)` | 请求 | `Task<HttpResponseMessage>` | 发送自定义请求 |

```csharp
// HttpClient 应该复用实例，不要每次 new
private static readonly HttpClient _httpClient = new HttpClient();

// GET 请求
string json = await _httpClient.GetStringAsync("https://api.example.com/data");

// POST JSON
var data = new { Name = "Test", Value = 42 };
var response = await _httpClient.PostAsJsonAsync("https://api.example.com/create", data);
response.EnsureSuccessStatusCode();  // 非 2xx 抛异常

// 读取响应内容
string responseBody = await response.Content.ReadAsStringAsync();
var result = await response.Content.ReadFromJsonAsync<MyResult>();

// 设置 Header
_httpClient.DefaultRequestHeaders.Add("X-Api-Key", "your-api-key");
_httpClient.DefaultRequestHeaders.Authorization = 
    new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", token);

// 设置超时
_httpClient.Timeout = TimeSpan.FromSeconds(30);

// 实际场景：调用外部 API 并处理错误
try
{
    var response = await _httpClient.GetAsync(url);
    if (response.IsSuccessStatusCode)
    {
        var result = await response.Content.ReadFromJsonAsync<ApiResponse>();
        return result;
    }
    else
    {
        var error = await response.Content.ReadAsStringAsync();
        _logger.LogError("API 调用失败：{StatusCode} {Error}", response.StatusCode, error);
    }
}
catch (HttpRequestException ex)
{
    _logger.LogError(ex, "网络请求异常");
}
catch (TaskCanceledException)
{
    _logger.LogWarning("请求超时");
}
```

**注意细节：**
- `HttpClient` 必须复用实例（`static` 或通过 DI 注入 `IHttpClientFactory`），每次 new 会导致端口耗尽
- `PostAsJsonAsync` / `PutAsJsonAsync` 是 `System.Net.Http.Json` 命名空间的扩展方法，需要引用该包
- `GetStringAsync` 在 HTTP 错误（404、500 等）时也会抛异常。如果需要检查状态码，使用 `GetAsync` + `HttpResponseMessage`
- `TaskCanceledException` 表示超时（HttpClient 的超时机制抛出的是这个异常，而不是 `TimeoutException`）
