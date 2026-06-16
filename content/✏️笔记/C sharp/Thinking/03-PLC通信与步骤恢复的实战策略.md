## PLC 通信与步骤恢复的实战策略

### 你描述的核心问题

你遇到的PLC通信问题，本质上是**分布式系统的状态一致性问题**。上微机和PLC是两个独立的"大脑"，它们通过共享的D寄存器来传递信息。问题出在：这个"共享内存"没有内置的可靠传递机制，就像两个人通过一块小黑板写字沟通，但黑板可能被擦掉、可能来不及看就被覆盖了。

你提到的几个具体痛点，我来逐一拆解。

---

### 问题一：PLC发过一次信号，上位机没收到就重启了

#### 为什么会发生

PLC的逻辑是："我在D900写入了指令值，我这边就完成了，我不会再写一遍。" 上位机的逻辑是："我在读D900，但如果这时候我重启了，我就错过了这个值。"

这是一个典型的**"发射即忘"（fire-and-forget）通信模式的缺陷**。

#### 解决方案：握手确认机制

最可靠的方案是把通信改成**握手式**，分四步：

```
1. PLC写入指令值到D900，同时设置一个"发送标志位" D901=1
2. 上位机读到D901=1，处理指令，处理完后写入D902=1（"已收到确认"）
3. PLC读到D902=1，清除发送标志 D901=0
4. 上位机读到D901=0，清除确认标志 D902=0
```

这样，如果上位机在步骤2之前重启了，D901仍然是1，上位机重启后重新读取D901，发现"PLC有未处理的指令"，可以继续处理。**关键就是那个标志位——它让指令变成了"可重放"的。**

#### 代码层面的实现思路

```csharp
// 上位机在每个扫描周期检查
if (ReadPlcInt("D901") == 1)  // PLC有新指令
{
    int command = ReadPlcInt("D900");
    ProcessCommand(command);     // 处理指令
    WritePlcInt("D902", 1);     // 告诉PLC我收到了
}

if (ReadPlcInt("D901") == 0 && ReadPlcInt("D902") == 1)
{
    WritePlcInt("D902", 0);     // 双方都清除标志
}
```

---

### 问题二：断步——重启后不知道执行到哪了

#### 为什么会发生

你的步骤变量 step 存在上微机的内存里（一个int变量）。上微机一关，内存清零，step变成0或者默认值。重启后程序从step=0开始跑，但PLC可能已经走到流程的中间了，两边对不上。

#### 解决方案：步骤变量双写

**核心原则：步骤信息不能只存在一个地方。**

方案A：步骤变量写入PLC的D寄存器

```csharp
// 每次step变化时，同步写入PLC
private void SetStep(int newStep)
{
    this.step = newStep;
    WritePlcInt("D800", newStep);  // D800专门存放当前步骤号
}
```

重启时：
```csharp
// 启动时先从PLC读回步骤号
int savedStep = ReadPlcInt("D800");
if (savedStep > 0 && IsValidStep(savedStep))
{
    this.step = savedStep;  // 恢复到上次的步骤
    Log("断步恢复，从步骤 " + savedStep + " 继续");
}
```

方案B：本地文件持久化（作为备份）

```csharp
// 每次step变化时，同时写入本地文件
File.WriteAllText("step_backup.txt", step.ToString());

// 重启时优先读PLC，PLC不可用时读本地文件
```

#### 你已经在做的 D940/D950 备份机制

你们之前重构时引入的 `statusReg=D940`（PLC状态只读）和 `memoryReg=D950`（状态备份），其实就是在做类似的事情。`TryRecoverStepFromPlcStatus` 方法通过读D950来推断上微机应该在哪一步，这正是"断步恢复"的核心思路。

**建议把这个机制做得更彻底：** 不只是D940/D950，把当前流程的关键上下文信息（当前操作的抽屉号、推盒号、扫码结果等）也持久化到PLC或者本地文件，这样恢复时不只是知道"在第几步"，还知道"这一步的参数是什么"。

---

### 问题三：信号值对不上——PLC写的值不是上位机期望的

#### 常见原因排查清单

**原因1：数据类型不匹配。** PLC那边写的是一个32位浮点数（占两个D寄存器，比如D900+D901），但上位机读的是16位整数（只读了D900）。结果读出来的值"看起来不对"。

**原因2：读写时序冲突。** 上位机正在读D900~D901的时候，PLC恰好在写D901。这样上位机读到的是"半旧半新"的值。解决方案是：先写D900，再写D901（高位先写），读的时候先读D901再读D900（高位先读），或者用标志位控制读写互斥。

**原因3：字节序问题。** 汇川PLC和C#程序对多寄存器浮点数的字节序可能不一致。如果你用HslCommunication库，注意它的 `DataFormat` 属性可以设置字节序（ABCD、CDAB、BADC、DCBA），需要根据PLC的设定来选择。

**原因4：寄存器地址偏移。** PLC那边的D900和上位机通过HslCommunication读的"D900"，有时因为协议映射的关系，实际地址可能有偏差。建议用一个已知的固定值（比如PLC写死一个常数12345到D900）来校准地址。

#### 调试技巧

在代码中加入一个**通信日志线程**，每隔100ms把所有关键D寄存器的值打印到日志文件：

```csharp
// 调试用：周期性记录D寄存器快照
Task.Run(() =>
{
    while (running)
    {
        var snapshot = new
        {
            Time = DateTime.Now.ToString("HH:mm:ss.fff"),
            D800_Step = ReadPlcInt("D800"),
            D900_Cmd = ReadPlcInt("D900"),
            D901_Flag = ReadPlcInt("D901"),
            D940_Status = ReadPlcInt("D940"),
            D950_Backup = ReadPlcInt("D950"),
        };
        Log(snapshot.ToString());
        Thread.Sleep(100);
    }
});
```

这样出问题的时候，回头翻日志，能精确看到"哪个时刻哪个寄存器的值变了"，比对着代码干想要有效得多。

---

### 问题四：上微机任务结束后PLC不再重发指令

这个问题和"问题一"是同一类。PLC的逻辑是"任务中发一次指令，发了就不管了"。如果上微机因为某种原因（重启、崩溃、网络中断）没有在那一刻接收到，就永远错过了。

#### 解决方案：上位机主动拉取模式

改变通信的主动权：**不是PLC推指令给上位机，而是上位机主动问PLC"有活干吗"。**

```csharp
// 上位机主循环中
while (true)
{
    // 如果当前空闲，主动检查PLC是否有待处理的任务
    if (step == 0 || step == IDLE_STEP)
    {
        int pendingTask = ReadPlcInt("D910");  // D910=待处理任务类型
        if (pendingTask > 0)
        {
            // 有新任务，开始执行
            SetStep(1000);  // 进入入库流程
        }
    }
    // ... 其他case处理
}
```

PLC那边只需要把"有任务"这个信息持续保持在D910里，上位机处理完后把D910清零。这样即使上位机重启了，只要D910里的值还在（PLC保持型寄存器），重启后上位机自然会读到并继续处理。

---

### 架构建议：把PLC通信封装成"可靠的通道"

你之前的重构已经把基本的读写封装成了 `ReadPlcFloat`、`WritePlcOrder`、`WritePlcOrderDirect`，这是很好的方向。下一步可以考虑建立一个更完整的通信层：

```csharp
public class PlcChannel
{
    // 带握手的指令发送
    public bool SendCommand(int regAddr, int command, int ackAddr, int timeoutMs = 3000)
    {
        WritePlcInt(regAddr, command);
        WritePlcInt(ackAddr, 1);  // 设置发送标志
        
        // 等待对方确认
        var sw = Stopwatch.StartNew();
        while (sw.ElapsedMilliseconds < timeoutMs)
        {
            if (ReadPlcInt(ackAddr) == 0) return true;  // 对方已处理
            Thread.Sleep(50);
        }
        return false;  // 超时
    }
    
    // 带重试的读取
    public int ReadWithRetry(string addr, int maxRetry = 3)
    {
        for (int i = 0; i < maxRetry; i++)
        {
            try { return ReadPlcInt(addr); }
            catch { Thread.Sleep(100); }
        }
        throw new Exception($"读取 {addr} 失败，已重试 {maxRetry} 次");
    }
}
```

**核心思想是：把"可靠的通信"封装成一个工具类，业务代码不需要关心握手、重试、超时这些底层细节。** 这样你写入库/出库流程的时候，只需要 `plcChannel.SendCommand(...)` 一行代码，不用担心底层通信的各种坑。

---

### 给你的实操建议

从今天开始，可以做这几件事来减少通信问题：

**第一步：** 整理一份"寄存器地址表"，列出所有用到的D寄存器、用途、数据格式、读/写方向、哪边写哪边读。这个表贴在屏幕旁边，每次写代码前先看表，避免地址冲突或类型搞错。

**第二步：** 对于关键指令（入库开始、出库开始、扫码结果等），全部改成握手确认模式。非关键的周期性信息（心跳、状态上报）可以保持fire-and-forget。

**第三步：** 加上通信日志线程，至少在调试阶段保持开启。数据在日志里白纸黑字写着，比任何推理都可靠。

**第四步：** 每次写完一段通信相关的代码，先别急着联调，用纸笔"模拟"一遍：假设PLC写了什么值，上微机读到了什么，然后怎么响应，一步步推演，看看有没有对不上的地方。
