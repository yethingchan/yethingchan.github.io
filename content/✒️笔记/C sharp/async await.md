---
title: "async await"
description: ""
date: "2026-05-07"
tags: []
share: true
---
# C# async/await 从入门到精通（全实例讲解）
**核心一句话**：`async/await`是C# 5.0引入的语法糖，让异步代码写起来像同步代码一样简单，**解决了耗时操作阻塞线程**的问题（比如上位机中的串口通信、数据库查询、网络请求）。

---

## 一、入门级：最基础的用法（理解"暂停"）
### 核心概念
- `async`：修饰方法，表示这是一个异步方法
- `await`：**暂停点**，遇到它时，当前方法会暂停执行，把线程让出来去做其他事情，等await的任务完成后，再从暂停的地方继续执行
- 注意：`async/await`本身**不会创建新线程**，真正的异步操作由底层API（如`Task.Delay`、网络库）提供

### 最简单的例子
```csharp
using System;
using System.Threading.Tasks;

class Program
{
    // C# 7.1+ 支持 async Main 方法（必须返回Task）
    static async Task Main(string[] args)
    {
        Console.WriteLine("1. 程序开始");
        
        // 调用异步方法，遇到await会暂停
        await MakeCoffeeAsync();
        
        Console.WriteLine("4. 程序结束");
    }

    // 异步方法命名约定：以Async结尾
    static async Task MakeCoffeeAsync()
    {
        Console.WriteLine("2. 开始煮咖啡");
        
        // 模拟耗时操作（比如煮咖啡需要3秒）
        // 这里会暂停3秒，但不会阻塞线程
        await Task.Delay(3000);
        
        Console.WriteLine("3. 咖啡煮好了");
    }
}
```

**输出结果**：
```
1. 程序开始
2. 开始煮咖啡
（等待3秒，线程空闲）
3. 咖啡煮好了
4. 程序结束
```

---

## 二、基础级：异步方法的三种返回值
异步方法**只能有三种返回值**，这是初学者最容易搞错的地方：

| 返回值类型 | 适用场景 | 能否await | 能否捕获异常 |
|------------|----------|-----------|--------------|
| `Task` | 无返回值的异步操作 | ✅ 可以 | ✅ 可以 |
| `Task<T>` | 有返回值的异步操作 | ✅ 可以 | ✅ 可以 |
| `void` | **仅用于事件处理程序** | ❌ 不可以 | ❌ 不可以 |

### 完整例子
```csharp
using System;
using System.Threading.Tasks;

class Program
{
    static async Task Main(string[] args)
    {
        // 1. 调用无返回值的异步方法
        Console.WriteLine("\n--- 无返回值异步方法 ---");
        await NoReturnValueAsync();

        // 2. 调用有返回值的异步方法
        Console.WriteLine("\n--- 有返回值异步方法 ---");
        int result = await CalculateSumAsync(10, 20);
        Console.WriteLine($"计算结果: {result}");

        // 3. 调用void返回值的异步方法（仅用于事件）
        Console.WriteLine("\n--- void返回值异步方法 ---");
        VoidReturnValueAsync();
        // 无法await，程序会继续执行，不会等它完成
        Console.WriteLine("Main方法继续执行");
    }

    // 无返回值：返回Task
    static async Task NoReturnValueAsync()
    {
        await Task.Delay(1000);
        Console.WriteLine("无返回值方法执行完毕");
    }

    // 有返回值：返回Task<T>，直接return T类型的值即可
    static async Task<int> CalculateSumAsync(int a, int b)
    {
        await Task.Delay(1000);
        return a + b; // 编译器会自动包装成Task<int>
    }

    // void返回值：仅用于事件处理程序（如按钮点击）
    static async void VoidReturnValueAsync()
    {
        await Task.Delay(2000);
        Console.WriteLine("void方法执行完毕（可能在程序退出后才输出）");
    }
}
```

**输出结果**：
```
--- 无返回值异步方法 ---
无返回值方法执行完毕

--- 有返回值异步方法 ---
计算结果: 30

--- void返回值异步方法 ---
Main方法继续执行
（程序可能会在void方法执行完毕前退出）
```

---

## 三、进阶级1：串行 vs 并行执行
### 1. 串行执行（一个接一个）
适用于**任务之间有依赖关系**，必须先完成前一个才能执行后一个的情况。

```csharp
using System;
using System.Diagnostics;
using System.Threading.Tasks;

class Program
{
    static async Task Main(string[] args)
    {
        var stopwatch = Stopwatch.StartNew();
        Console.WriteLine("开始串行执行三个任务");

        // 每个await都会等待前一个任务完成
        await BoilWaterAsync();
        await WashCupAsync();
        await MakeTeaAsync();

        stopwatch.Stop();
        Console.WriteLine($"串行总耗时: {stopwatch.ElapsedMilliseconds}ms");
    }

    static async Task BoilWaterAsync()
    {
        await Task.Delay(2000);
        Console.WriteLine("水烧开了");
    }

    static async Task WashCupAsync()
    {
        await Task.Delay(1000);
        Console.WriteLine("杯子洗好了");
    }

    static async Task MakeTeaAsync()
    {
        await Task.Delay(1000);
        Console.WriteLine("茶泡好了");
    }
}
```

**输出结果**：
```
开始串行执行三个任务
水烧开了
杯子洗好了
茶泡好了
串行总耗时: 4000ms左右
```

### 2. 并行执行（同时执行）
适用于**任务之间没有依赖关系**的情况，可以大幅提高效率。使用`Task.WhenAll`等待所有任务完成。

```csharp
using System;
using System.Diagnostics;
using System.Threading.Tasks;

class Program
{
    static async Task Main(string[] args)
    {
        var stopwatch = Stopwatch.StartNew();
        Console.WriteLine("开始并行执行三个任务");

        // 第一步：先启动所有任务（不要await）
        var boilWaterTask = BoilWaterAsync();
        var washCupTask = WashCupAsync();
        var makeTeaTask = MakeTeaAsync();

        // 第二步：等待所有任务完成
        await Task.WhenAll(boilWaterTask, washCupTask, makeTeaTask);

        stopwatch.Stop();
        Console.WriteLine($"并行总耗时: {stopwatch.ElapsedMilliseconds}ms");
    }

    // 三个方法和上面一样
    static async Task BoilWaterAsync() { await Task.Delay(2000); Console.WriteLine("水烧开了"); }
    static async Task WashCupAsync() { await Task.Delay(1000); Console.WriteLine("杯子洗好了"); }
    static async Task MakeTeaAsync() { await Task.Delay(1000); Console.WriteLine("茶泡好了"); }
}
```

**输出结果**：
```
开始并行执行三个任务
杯子洗好了
茶泡好了
水烧开了
并行总耗时: 2000ms左右（等于最长任务的耗时）
```

### 3. 等待任意一个任务完成（Task.WhenAny）
适用于需要快速响应的场景，比如同时请求多个服务器，哪个先返回就用哪个的结果。

```csharp
var task1 = DownloadFromServer1Async();
var task2 = DownloadFromServer2Async();
var task3 = DownloadFromServer3Async();

// 等待第一个完成的任务
var firstCompletedTask = await Task.WhenAny(task1, task2, task3);
Console.WriteLine($"第一个完成的任务结果: {await firstCompletedTask}");
```

---

## 四、进阶级2：异步方法的异常处理
异步方法中的异常会被包装在`Task`对象中，**只有当你await这个Task时，异常才会被抛出**。

### 1. 单个异步任务的异常处理
```csharp
try
{
    await RiskyOperationAsync();
}
catch (InvalidOperationException ex)
{
    Console.WriteLine($"捕获到异常: {ex.Message}");
}
```

### 2. 多个并行任务的异常处理
当多个并行任务都抛出异常时，`Task.WhenAll`会把所有异常包装在一个`AggregateException`中。

```csharp
var task1 = ThrowExceptionAsync("任务1异常");
var task2 = ThrowExceptionAsync("任务2异常");
var task3 = ThrowExceptionAsync("任务3异常");

try
{
    await Task.WhenAll(task1, task2, task3);
}
catch (AggregateException ex)
{
    Console.WriteLine($"捕获到{ex.InnerExceptions.Count}个异常:");
    foreach (var innerEx in ex.InnerExceptions)
    {
        Console.WriteLine($"- {innerEx.Message}");
    }
}

static async Task ThrowExceptionAsync(string message)
{
    await Task.Delay(1000);
    throw new InvalidOperationException(message);
}
```

**输出结果**：
```
捕获到3个异常:
- 任务1异常
- 任务2异常
- 任务3异常
```

---

## 五、高级级：UI线程死锁与ConfigureAwait(false)
这是**上位机开发（WinForms/WPF）中最容易踩的坑**！

### 什么是死锁？
在UI线程中，有一个特殊的"同步上下文"，它会把`await`之后的代码**封送回UI线程执行**（这样你才能直接更新UI控件）。

如果你在UI线程中**同步等待**一个异步任务（用`.Result`或`.Wait()`），就会发生死锁：
1. UI线程调用`.Wait()`，阻塞自己等待任务完成
2. 任务完成后，需要回到UI线程执行`await`之后的代码
3. 但UI线程已经被阻塞了，永远无法执行后续代码 → 死锁

### 错误代码（会导致死锁）
```csharp
// WinForms按钮点击事件（运行在UI线程）
private void Button_Click(object sender, EventArgs e)
{
    // ❌ 错误：在UI线程中同步等待异步任务
    DoSomethingAsync().Wait(); // 程序会卡死！
}

async Task DoSomethingAsync()
{
    await Task.Delay(1000);
    // 这里需要回到UI线程执行，但UI线程正在Wait()
    label1.Text = "完成";
}
```

### 正确解决方法
#### 方法1：永远使用await，不要用.Result或.Wait()
```csharp
// ✅ 正确：使用await异步等待
private async void Button_Click(object sender, EventArgs e)
{
    await DoSomethingAsync();
}
```

#### 方法2：使用ConfigureAwait(false)
告诉编译器：**await之后不需要回到原来的同步上下文**，直接在线程池线程中执行后续代码。

```csharp
async Task DoSomeBackgroundWorkAsync()
{
    // ✅ 正确：不需要更新UI时，使用ConfigureAwait(false)
    await Task.Delay(1000).ConfigureAwait(false);
    
    // 这里在线程池线程中执行，不能直接更新UI
    int result = CalculateSomething();
}
```

**重要注意**：
- 如果`await`之后需要**更新UI控件**，**绝对不能**用`ConfigureAwait(false)`
- 如果`await`之后不需要更新UI，**建议总是**使用`ConfigureAwait(false)`，可以提高性能，避免死锁

---

## 六、核心总结与常见误区
### 核心要点
1. `async`修饰方法，`await`标记暂停点
2. 异步方法返回值只能是`Task`、`Task<T>`或`void`（仅用于事件）
3. 无依赖的任务用`Task.WhenAll`并行执行，效率更高
4. 异常在`await`时抛出，多个并行任务的异常用`AggregateException`捕获
5. UI线程中永远不要用`.Result`或`.Wait()`，会导致死锁
6. 不需要更新UI时，使用`ConfigureAwait(false)`

### 常见误区
❌ 认为`async/await`会创建新线程 → 不会，它只是语法糖
❌ 异步方法返回`void` → 除了事件处理程序，永远不要这么做
❌ 并行任务时直接`await`每个任务 → 会变成串行执行
❌ 忘记`await`异步方法 → 异常会被忽略，程序可能崩溃
❌ 在`ConfigureAwait(false)`之后更新UI → 会抛出跨线程异常

需要我给你一个**上位机串口通信的async/await实战例子**吗？这样你可以直接用到你的项目中。