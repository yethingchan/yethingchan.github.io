# 汇川PLC寄存器读写详解

> **适用PLC**：汇川(Inovance) H3U / H5U 系列
> **通信协议**：Modbus TCP / Modbus RTU
> **开发语言**：C#
> **前置知识**：了解C#基础语法、TCP通信基础

---

## 一、汇川PLC寄存器类型总览

汇川PLC拥有多种寄存器类型，每种寄存器用于不同的用途。下表列出了所有常见的寄存器类型：

| 寄存器类型 | 说明 | 数据类型 | 位数 | 用途 | 是否掉电保持 |
|-----------|------|---------|------|------|------------|
| **X** | 输入继电器 | Bit | 1位 | 外部输入信号（开关、传感器等） | 否 |
| **Y** | 输出继电器 | Bit | 1位 | 控制外部设备（继电器、指示灯等） | 否 |
| **M** | 内部辅助继电器 | Bit | 1位 | 内部逻辑运算中间变量 | 部分保持（取决于地址） |
| **S** | 步进继电器 | Bit | 1位 | 顺序控制编程（SFC） | 否 |
| **SM** | 特殊辅助继电器 | Bit | 1位 | PLC状态标志（运行/停止/错误等） | - |
| **T** | 定时器 | Word | 16位 | 定时器当前值/设定值 | 设定值保持 |
| **C** | 计数器 | Word | 16位 | 计数器当前值/设定值 | 设定值保持 |
| **D** | 数据寄存器 | Word | 16位 | 通用数据存储（最常用） | 部分保持（取决于地址） |
| **SD** | 特殊数据寄存器 | Word | 16位 | PLC系统参数（版本号、状态字等） | - |
| **R** | 文件寄存器 | Word | 16位 | 扩展数据存储（掉电保持） | 是 |
| **DD** | 32位数据寄存器 | DWord | 32位 | 大数值存储 | 部分保持 |
| **UW** | 链接继电器（字） | Word | 16位 | PLC间通信（数据） | 否 |
| **W** | 链接继电器（位） | Bit | 1位 | PLC间通信（位信号） | 否 |

### 关键说明

1. **D寄存器是最常用的**：大部分数据交换（温度值、压力值、频率设定等）都通过D寄存器完成
2. **D寄存器的掉电保持**：D200及以上通常掉电保持，D0-D199通常掉电不保持（具体以PLC型号手册为准）
3. **X只读**：X是输入信号，只能读取，不能通过上位机写入
4. **Y可读可写**：Y可以通过上位机写线圈来控制输出
5. **UW/W不支持Modbus**：这两个区域用于PLC间通信，不支持标准Modbus协议

---

## 二、Modbus地址映射规则

### 2.1 五位Modbus地址表示法

Modbus协议使用5位数字地址来表示不同类型的寄存器，前1-2位数字代表寄存器类型，后几位代表地址编号。

| 前缀数字 | 寄存器类型 | 读写属性 | 对应PLC寄存器 |
|---------|-----------|---------|-------------|
| **0** (00001-09999) | 线圈 (Coil) | 可读可写 | Y（输出继电器）、M（内部继电器） |
| **1** (10001-19999) | 离散输入 (Discrete Input) | 只读 | X（输入继电器） |
| **3** (30001-39999) | 输入寄存器 (Input Register) | 只读 | 部分PLC的模拟量输入 |
| **4** (40001-49999) | 保持寄存器 (Holding Register) | 可读可写 | D、SD、R、T、C |

**举例理解**：
- `40001` 表示保持寄存器区域的第1个地址（偏移地址为0）
- `40010` 表示保持寄存器区域的第10个地址（偏移地址为9）
- `00005` 表示线圈区域的第5个地址（偏移地址为4）

### 2.2 汇川各寄存器到Modbus地址的完整映射表

| PLC寄存器 | Modbus地址范围 | 功能码(读) | 功能码(写) | 备注 |
|----------|--------------|-----------|-----------|------|
| X0-X377（八进制） | 10001+ | 02 | - | 只读，八进制地址 |
| Y0-Y377（八进制） | 00001+ | 01 | 05/15 | 八进制地址 |
| M0-M383 | 00001+（部分型号从10001+） | 01 | 05/15 | **注意与Y重叠，需查阅手册确认具体映射** |
| S0-S999 | 视型号而定 | 01 | 05/15 | 部分型号支持 |
| T（当前值） | 40001 + T编号 | 03 | - | 只读当前值 |
| T（设定值） | 40001 + T编号 + 偏移 | 03 | 06/16 | 可读写设定值 |
| C（当前值） | 40001 + C编号 + 偏移 | 03 | - | 只读当前值 |
| C（设定值） | 40001 + C编号 + 偏移 | 03 | 06/16 | 可读写设定值 |
| **D0** | **40001** | **03** | **06/16** | **D0 = Modbus地址40001，偏移地址0** |
| **D100** | **40101** | **03** | **06/16** | **D100 = Modbus地址40101，偏移地址100** |
| **D900** | **40901** | **03** | **06/16** | **D900 = Modbus地址40901，偏移地址900** |
| SD | 40001 + 偏移 | 03 | - | 特殊寄存器，偏移地址需查手册 |
| R | 40001 + 偏移 | 03 | 06/16 | 文件寄存器，偏移地址需查手册 |

### 2.3 重点说明：D寄存器的地址映射

D寄存器的地址映射规则非常简单直观：

> **D后面的数字就是Modbus偏移地址，Modbus地址 = 40000 + D编号**

| PLC地址 | Modbus地址 | 偏移地址（编程用） | 说明 |
|---------|-----------|-----------------|------|
| D0 | 40001 | 0 | 第一个D寄存器 |
| D1 | 40002 | 1 | 第二个D寄存器 |
| D100 | 40101 | 100 | 第101个D寄存器 |
| D200 | 40201 | 200 | 第201个D寄存器 |
| D900 | 40901 | 900 | 第901个D寄存器 |
| D9999 | 49999（部分PLC） | 9999 | 最大编号取决于PLC型号 |

**代码中的对应关系**：
```csharp
// 在编程时，使用偏移地址（从0开始）
// Modbus地址40001 = 偏移地址0
// Modbus地址40101 = 偏移地址100

// NModbus4中：
master.ReadHoldingRegisters(slaveId, startAddress: 100, count: 1);  // 读取D100

// HslCommunication中：
client.ReadInt16("4101");  // 4101 = 40001 + 100，读取D100

// 注意两种库的地址表示方式不同！
```

### 2.4 特殊寄存器的地址映射

特殊寄存器（SD、R、T、C）的地址映射需要查阅具体PLC型号手册，因为不同型号的偏移地址不同。

**SD（特殊数据寄存器）示例**（以H3U为例）：

| SD编号 | Modbus偏移地址 | Modbus地址 | 含义 |
|--------|-------------|-----------|------|
| SD0 | 8000 | 48001 | PLC型号代码 |
| SD1 | 8001 | 48002 | PLC版本 |
| SD2 | 8002 | 48003 | 系统状态 |
| ... | ... | ... | ... |

> **注意**：SD的偏移地址不是从0开始的！需要查手册确认具体映射。不同的汇川PLC型号，SD的Modbus偏移地址可能不同。

**建议**：在实际项目中，先查阅汇川PLC对应型号的《Modbus通信手册》，确认所有寄存器的具体映射关系后再编写代码。

---

## 三、Modbus功能码详解

### 3.1 功能码总览

| 功能码（十进制） | 功能码（十六进制） | 名称 | 操作对象 | 读写属性 |
|-----------------|-------------------|------|---------|---------|
| 01 | 0x01 | 读线圈 | Y、M、S | 读 |
| 02 | 0x02 | 读离散输入 | X | 读（只读） |
| 03 | 0x03 | 读保持寄存器 | D、SD、R、T、C | 读 |
| 04 | 0x04 | 读输入寄存器 | 模拟量输入 | 读（只读） |
| 05 | 0x05 | 写单个线圈 | Y、M、S | 写 |
| 06 | 0x06 | 写单个保持寄存器 | D | **写（重点）** |
| 15 | 0x0F | 写多个线圈 | Y、M、S | 写 |
| 16 | 0x10 | 写多个保持寄存器 | D | 写（批量） |

### 3.2 功能码01：读线圈（Y/M/S）

**用途**：读取PLC的输出继电器（Y）或内部继电器（M）的ON/OFF状态。

**请求帧格式**：
```
[从站地址(1B)] + [功能码01(1B)] + [起始地址高(1B)] + [起始地址低(1B)] + [数量高(1B)] + [数量低(1B)]
```

**示例**：读取从站1的线圈0-7（共8个）
```
请求：01 01 00 00 00 08
响应：01 01 01 AA          （01=功能码，01=字节数，AA=10101010，即8个线圈的状态）
```

### 3.3 功能码02：读离散输入（X）

**用途**：读取PLC的输入继电器（X）的状态。**X是只读的，只能用功能码02读取**。

**请求帧格式**：与功能码01相同。

### 3.4 功能码03：读保持寄存器（D/SD/R/T/C）-- 最常用

**用途**：读取PLC的数据寄存器（D）等16位数据。**这是上位机开发中使用频率最高的功能码**。

**请求帧格式**：
```
[从站地址(1B)] + [功能码03(1B)] + [起始地址高(1B)] + [起始地址低(1B)] + [数量高(1B)] + [数量低(1B)]
```

**示例**：读取从站1的D100-D109（起始地址100，共10个寄存器）
```
请求：01 03 00 64 00 0A
      -- -- ----- -----     （01=从站，03=功能码，0064=十进制100，000A=十进制10）

响应：01 03 14 [20字节数据]
      -- -- --              （01=从站，03=功能码，14=20字节=10个寄存器x2字节）
```

**C#示例**：
```csharp
// NModbus4: 读取D100-D109
ushort startAddress = 100;  // D100的偏移地址
ushort count = 10;           // 读取10个寄存器
ushort[] registers = master.ReadHoldingRegisters(1, startAddress, count);
// registers[0] = D100的值
// registers[1] = D101的值
// ...
// registers[9] = D109的值
```

### 3.5 功能码05：写单个线圈（Y/M/S）

**用途**：将PLC的某个输出继电器（Y）或内部继电器（M）设置为ON或OFF。

**请求帧格式**：
```
[从站地址(1B)] + [功能码05(1B)] + [线圈地址高(1B)] + [线圈地址低(1B)] + [FF00(ON)/0000(OFF)] + [校验]
```

**示例**：将从站1的Y0设置为ON
```
请求：01 05 00 00 FF 00       （00 00=Y0地址，FF 00=ON）
响应：01 05 00 00 FF 00       （回显）
```

### 3.6 功能码06：写单个保持寄存器（D）-- 重点

**用途**：向PLC的某个D寄存器写入一个16位值。**这是修改单个寄存器值的标准方式**。

**请求帧格式**：
```
[从站地址(1B)] + [功能码06(1B)] + [寄存器地址高(1B)] + [寄存器地址低(1B)] + [写入值高(1B)] + [写入值低(1B)]
```

**示例**：向从站1的D900写入值500
```
请求：01 06 03 84 01 F4
      -- -- ----- -----     （01=从站，06=功能码，0384=十进制900，01F4=十进制500）
响应：01 06 03 84 01 F4       （回显，与请求完全相同）
```

**为什么功能码06是重点？**
- 上位机向PLC发送设定值（如频率设定、温度设定）时，最常用的就是功能码06
- 它**天然只影响目标地址**，不会意外修改相邻寄存器
- 响应简短（8字节），通信效率高

### 3.7 功能码15(0x0F)：写多个线圈

**用途**：一次性将多个线圈（Y/M/S）设置为ON或OFF。

**与功能码05的区别**：功能码05一次只能写1个线圈，功能码15可以一次写多个。

### 3.8 功能码16(0x10)：写多个保持寄存器

**用途**：一次性向多个连续的D寄存器写入数据。

**请求帧格式**：
```
[从站地址(1B)] + [功能码10(1B)] + [起始地址高(1B)] + [起始地址低(1B)] + [数量高(1B)] + [数量低(1B)] + [字节数(1B)] + [数据(NB)] + [CRC校验]
```

**示例**：向从站1的D100-D102写入值100、200、300
```
请求：01 10 00 64 00 03 06 00 64 00 C8 01 2C
      -- -- ----- ----- -- -- ----- ----- -----
      |  |  起始地址100  数量3 6字节 100  200  300
      |  功能码16
      从站1
```

---

## 四、核心问题：如何只修改D900而不影响其他地址

这是初学者最常见的问题之一。比如PLC中D898-D902存储了一组连续的数据，你只想修改D900的值，而不影响D898、D899、D901、D902。

### 4.1 功能码06的工作原理

功能码06（Write Single Register）的设计初衷就是**精确修改单个寄存器**。

**工作流程**：
1. 上位机发送请求：指定从站地址 + 功能码06 + 目标寄存器地址 + 写入值
2. PLC收到请求后，**只修改指定的那个寄存器**
3. PLC返回响应（回显请求内容），确认写入成功

**关键点**：
- 请求帧中只包含**一个寄存器地址**和**一个值**
- PLC严格按照请求中的地址执行写入
- 不涉及任何相邻地址

```
功能码06请求帧分析（写入D900=500）：
01       - 从站地址
06       - 功能码：写单个寄存器
03 84    - 寄存器地址：900（0x0384 = 900）
01 F4    - 写入值：500（0x01F4 = 500）

PLC只看到了一个明确的指令：
"把地址900的寄存器值改成500"
其他地址完全不受影响！
```

### 4.2 为什么功能码06天然只影响目标地址

这是因为功能码06的协议设计本身就**不携带任何范围信息**：

| 对比项 | 功能码06 | 功能码16 |
|--------|---------|---------|
| 写入数量 | 固定为1个寄存器 | 可指定N个寄存器 |
| 地址范围 | 只指定起始地址 | 指定起始地址 + 数量 |
| 影响范围 | 只有精确的1个地址 | 从起始地址开始的连续N个地址 |
| 适用场景 | 修改单个寄存器 | 批量初始化或更新 |

**形象比喻**：
- 功能码06 = 用钥匙开一扇精确的门，只进入那一间房间
- 功能码16 = 推开一扇大门，从起始位置开始连续操作多个房间

### 4.3 与功能码16（批量写）的对比

```csharp
// 场景：D898=100, D899=200, D900=300, D901=400, D902=500
// 目标：只修改D900为999

// ====== 方案1：功能码06（推荐）======
// 只修改D900，D898/D899/D901/D902完全不受影响
master.WriteSingleRegister(slaveId, address: 900, value: 999);
// 结果：D898=100, D899=200, D900=999, D901=400, D902=500  正确！

// ====== 方案2：功能码16（危险！）======
// 如果不慎把起始地址设为D898，数量设为5：
ushort[] values = new ushort[] { 100, 200, 999, 400, 500 };
master.WriteMultipleRegisters(slaveId, startAddress: 898, values);
// 虽然结果看起来正确，但需要重新发送所有5个值，通信量更大
// 更危险的情况：如果你记错了其他地址的值，就会覆盖错误！

// ====== 方案3：功能码16（错误用法！）======
ushort[] values2 = new ushort[] { 999 };
master.WriteMultipleRegisters(slaveId, startAddress: 898, values2);
// 结果：D898=999, D899=0(!), D900=0(!), D901=0(!), D902=0(!)
// 这不是功能码16的错，而是起始地址设错了！
```

### 4.4 完整代码示例：修改D900而不影响其他地址

#### 方案A：使用NModbus4

```csharp
using System;
using System.Net.Sockets;
using NModbus;

class Program
{
    static void Main(string[] args)
    {
        // PLC连接参数
        string plcIp = "192.168.1.100";
        int port = 502;
        byte slaveId = 1;

        using (TcpClient client = new TcpClient())
        {
            Console.WriteLine($"正在连接PLC {plcIp}:{port}...");
            client.Connect(plcIp, port);

            var factory = new ModbusFactory();
            IModbusMaster master = factory.CreateMaster(client);
            master.Transport.ReadTimeout = 3000;  // 读超时3秒
            master.Transport.WriteTimeout = 3000; // 写超时3秒

            Console.WriteLine("连接成功！");

            // ========================================
            // 读取D900的当前值（用于确认）
            // ========================================
            ushort startAddress = 900;  // D900
            ushort[] readResult = master.ReadHoldingRegisters(slaveId, startAddress, 1);
            Console.WriteLine($"D900 当前值: {readResult[0]}");

            // ========================================
            // 写入D900 = 1234（使用功能码06）
            // 只修改D900，不会影响D898、D899、D901、D902等任何其他地址
            // ========================================
            master.WriteSingleRegister(slaveId, address: 900, value: 1234);
            Console.WriteLine("已写入 D900 = 1234");

            // ========================================
            // 读回确认
            // ========================================
            ushort[] confirmResult = master.ReadHoldingRegisters(slaveId, 900, 1);
            Console.WriteLine($"D900 确认值: {confirmResult[0]}");

            // ========================================
            // 读取相邻地址，验证未被影响
            // ========================================
            ushort[] neighborResult = master.ReadHoldingRegisters(slaveId, 898, 5);
            Console.WriteLine($"D898 = {neighborResult[0]}");
            Console.WriteLine($"D899 = {neighborResult[1]}");
            Console.WriteLine($"D900 = {neighborResult[2]}");  // 应该是1234
            Console.WriteLine($"D901 = {neighborResult[3]}");
            Console.WriteLine($"D902 = {neighborResult[4]}");
        }

        Console.WriteLine("按任意键退出...");
        Console.ReadKey();
    }
}
```

#### 方案B：使用HslCommunication

```csharp
using System;
using HslCommunication.ModBus;

class Program
{
    static void Main(string[] args)
    {
        // PLC连接参数
        string plcIp = "192.168.1.100";
        int port = 502;

        // 创建Modbus TCP客户端
        var client = new ModbusTcpNet(plcIp, port);
        client.DataFormat = HslCommunication.Core.DataFormat.ABCD;  // 字序设置（根据PLC配置调整）
        client.ConnectServer();
        Console.WriteLine($"连接PLC {plcIp} 成功！");

        try
        {
            // ========================================
            // 读取D900的当前值
            // HslCommunication地址格式："4" + (偏移地址+1)
            // D900 -> 偏移地址900 -> 地址字符串 "4901"
            // ========================================
            var readResult = client.ReadInt16("4901");
            Console.WriteLine($"D900 当前值: {readResult.Content}");

            // ========================================
            // 写入D900 = 1234（内部自动使用功能码06）
            // 只修改D900，不影响其他地址
            // ========================================
            var writeResult = client.Write("4901", (short)1234);
            if (writeResult.IsSuccess)
            {
                Console.WriteLine("已写入 D900 = 1234");
            }
            else
            {
                Console.WriteLine($"写入失败: {writeResult.Message}");
            }

            // ========================================
            // 读回确认
            // ========================================
            var confirmResult = client.ReadInt16("4901");
            Console.WriteLine($"D900 确认值: {confirmResult.Content}");

            // ========================================
            // 读取相邻地址验证
            // D898="4899", D899="4900", D900="4901", D901="4902", D902="4903"
            // ========================================
            var neighbors = client.ReadInt16("4899", 5);
            for (int i = 0; i < neighbors.Content.Length; i++)
            {
                Console.WriteLine($"D{898 + i} = {neighbors.Content[i]}");
            }
        }
        finally
        {
            client.ConnectClose();
        }

        Console.WriteLine("按任意键退出...");
        Console.ReadKey();
    }
}
```

**HslCommunication地址格式说明**：
```
格式："功能区域码" + "地址编号"

功能区域码：
  0 = 线圈（Coil，Y/M/S）
  1 = 离散输入（X）
  3 = 输入寄存器
  4 = 保持寄存器（D）

地址编号 = 偏移地址 + 1（因为Modbus地址从1开始，偏移从0开始）

示例：
  D0   -> 偏移0  -> "4" + "1"   = "41"    或 "40001"
  D100 -> 偏移100 -> "4" + "101" = "4101"  或 "40101"
  D900 -> 偏移900 -> "4" + "901" = "4901"  或 "40901"
```

---

## 五、C#代码实战

### 5.1 NModbus4方案完整代码

```csharp
using System;
using System.Net.Sockets;
using System.Threading;
using System.Threading.Tasks;
using NModbus;

namespace ModbusDemo
{
    /// <summary>
    /// 汇川PLC通信管理类（NModbus4方案）
    /// </summary>
    public class InovancePLCManager : IDisposable
    {
        private TcpClient _tcpClient;
        private IModbusMaster _master;
        private byte _slaveId = 1;
        private bool _isConnected = false;

        /// <summary>
        /// 连接状态变化事件
        /// </summary>
        public event EventHandler<bool> ConnectionChanged;

        /// <summary>
        /// 当前连接状态
        /// </summary>
        public bool IsConnected => _isConnected;

        /// <summary>
        /// 连接PLC
        /// </summary>
        /// <param name="ip">PLC的IP地址</param>
        /// <param name="port">Modbus TCP端口（默认502）</param>
        /// <param name="slaveId">从站地址（默认1）</param>
        public bool Connect(string ip, int port = 502, byte slaveId = 1)
        {
            try
            {
                _slaveId = slaveId;
                _tcpClient = new TcpClient();
                _tcpClient.Connect(ip, port);

                var factory = new ModbusFactory();
                _master = factory.CreateMaster(_tcpClient);
                _master.Transport.ReadTimeout = 3000;
                _master.Transport.WriteTimeout = 3000;

                // 预读测试连接
                _master.ReadHoldingRegisters(_slaveId, 0, 1);

                _isConnected = true;
                ConnectionChanged?.Invoke(this, true);
                return true;
            }
            catch (Exception)
            {
                Disconnect();
                return false;
            }
        }

        /// <summary>
        /// 断开连接
        /// </summary>
        public void Disconnect()
        {
            try
            {
                _master?.Dispose();
                _tcpClient?.Close();
            }
            catch { }

            _isConnected = false;
            ConnectionChanged?.Invoke(this, false);
        }

        #region 读取D寄存器

        /// <summary>
        /// 读取单个D寄存器（16位无符号整数）
        /// </summary>
        /// <param name="address">D寄存器编号（如D100传100）</param>
        /// <returns>寄存器值</returns>
        public ushort ReadD_UInt16(int address)
        {
            CheckConnection();
            ushort[] result = _master.ReadHoldingRegisters(_slaveId, (ushort)address, 1);
            return result[0];
        }

        /// <summary>
        /// 读取单个D寄存器（16位有符号整数）
        /// </summary>
        public short ReadD_Int16(int address)
        {
            return unchecked((short)ReadD_UInt16(address));
        }

        /// <summary>
        /// 读取D寄存器的32位浮点数（两个D寄存器组合）
        /// </summary>
        /// <param name="address">D寄存器编号（如D100传100，占用D100和D101）</param>
        /// <param name="isBigEndian">大端模式（高字在前）</param>
        public float ReadD_Float32(int address, bool isBigEndian = true)
        {
            CheckConnection();
            ushort[] result = _master.ReadHoldingRegisters(_slaveId, (ushort)address, 2);

            // 将两个16位寄存器合并为32位浮点数
            byte[] bytes = new byte[4];
            if (isBigEndian)
            {
                // 高字在前（D100=高字, D101=低字）
                bytes[0] = (byte)(result[0] >> 8);
                bytes[1] = (byte)(result[0] & 0xFF);
                bytes[2] = (byte)(result[1] >> 8);
                bytes[3] = (byte)(result[1] & 0xFF);
            }
            else
            {
                // 低字在前（D100=低字, D101=高字）
                bytes[0] = (byte)(result[1] >> 8);
                bytes[1] = (byte)(result[1] & 0xFF);
                bytes[2] = (byte)(result[0] >> 8);
                bytes[3] = (byte)(result[0] & 0xFF);
            }

            return BitConverter.ToSingle(bytes, 0);
        }

        /// <summary>
        /// 批量读取D寄存器
        /// </summary>
        /// <param name="startAddress">起始D编号</param>
        /// <param name="count">读取数量</param>
        public ushort[] ReadD_Multiple(int startAddress, int count)
        {
            CheckConnection();
            return _master.ReadHoldingRegisters(
                _slaveId,
                (ushort)startAddress,
                (ushort)count);
        }

        #endregion

        #region 写入D寄存器

        /// <summary>
        /// 写入单个D寄存器（16位值）
        /// 使用功能码06，只影响目标地址，不影响相邻地址
        /// </summary>
        /// <param name="address">D寄存器编号（如D900传900）</param>
        /// <param name="value">写入值</param>
        public void WriteD_UInt16(int address, ushort value)
        {
            CheckConnection();
            _master.WriteSingleRegister(_slaveId, (ushort)address, value);
        }

        /// <summary>
        /// 写入D寄存器的32位浮点数（写入两个D寄存器）
        /// 使用功能码16（批量写），写入两个连续地址
        /// </summary>
        /// <param name="address">D寄存器编号</param>
        /// <param name="value">浮点数值</param>
        /// <param name="isBigEndian">大端模式</param>
        public void WriteD_Float32(int address, float value, bool isBigEndian = true)
        {
            CheckConnection();

            byte[] bytes = BitConverter.GetBytes(value);
            ushort highWord = 0;
            ushort lowWord = 0;

            if (isBigEndian)
            {
                highWord = (ushort)((bytes[0] << 8) | bytes[1]);
                lowWord = (ushort)((bytes[2] << 8) | bytes[3]);
            }
            else
            {
                highWord = (ushort)((bytes[2] << 8) | bytes[3]);
                lowWord = (ushort)((bytes[0] << 8) | bytes[1]);
            }

            ushort[] values = new ushort[] { highWord, lowWord };
            _master.WriteMultipleRegisters(_slaveId, (ushort)address, values);
        }

        #endregion

        #region 读写M寄存器（线圈）

        /// <summary>
        /// 读取单个M继电器状态
        /// </summary>
        /// <param name="address">M编号</param>
        /// <returns>true=ON, false=OFF</returns>
        public bool ReadM(int address)
        {
            CheckConnection();
            bool[] result = _master.ReadCoils(_slaveId, (ushort)address, 1);
            return result[0];
        }

        /// <summary>
        /// 写入单个M继电器（功能码05）
        /// </summary>
        /// <param name="address">M编号</param>
        /// <param name="value">true=ON, false=OFF</param>
        public void WriteM(int address, bool value)
        {
            CheckConnection();
            _master.WriteSingleCoil(_slaveId, (ushort)address, value);
        }

        /// <summary>
        /// 读取Y继电器状态
        /// </summary>
        public bool ReadY(int address)
        {
            CheckConnection();
            bool[] result = _master.ReadCoils(_slaveId, (ushort)address, 1);
            return result[0];
        }

        /// <summary>
        /// 读取X继电器状态（功能码02，只读）
        /// </summary>
        public bool ReadX(int address)
        {
            CheckConnection();
            bool[] result = _master.ReadInputs(_slaveId, (ushort)address, 1);
            return result[0];
        }

        #endregion

        #region 辅助方法

        private void CheckConnection()
        {
            if (!_isConnected || _tcpClient == null || !_tcpClient.Connected)
            {
                throw new InvalidOperationException("PLC未连接，请先调用Connect方法");
            }
        }

        public void Dispose()
        {
            Disconnect();
        }

        #endregion
    }

    // ===== 使用示例 =====
    class Program
    {
        static void Main()
        {
            using var plc = new InovancePLCManager();

            // 连接PLC
            if (!plc.Connect("192.168.1.100", 502, 1))
            {
                Console.WriteLine("PLC连接失败！");
                return;
            }
            Console.WriteLine("PLC连接成功！");

            try
            {
                // --- 读取D寄存器 ---
                ushort d100 = plc.ReadD_UInt16(100);
                Console.WriteLine($"D100 = {d100}");

                short d200 = plc.ReadD_Int16(200);
                Console.WriteLine($"D200 = {d200}");

                float d300 = plc.ReadD_Float32(300);
                Console.WriteLine($"D300(浮点数) = {d300:F2}");

                // --- 写入D寄存器 ---
                // 只修改D900，不影响其他地址（功能码06）
                plc.WriteD_UInt16(900, 1234);
                Console.WriteLine("已写入 D900 = 1234");

                // 写入浮点数到D500-D501
                plc.WriteD_Float32(500, 36.5f);
                Console.WriteLine("已写入 D500 = 36.5 (浮点数)");

                // --- 批量读取 ---
                ushort[] d100_109 = plc.ReadD_Multiple(100, 10);
                Console.WriteLine("D100-D109:");
                for (int i = 0; i < d100_109.Length; i++)
                {
                    Console.WriteLine($"  D{100 + i} = {d100_109[i]}");
                }

                // --- 读写M继电器 ---
                bool m10 = plc.ReadM(10);
                Console.WriteLine($"M10 = {(m10 ? "ON" : "OFF")}");

                plc.WriteM(10, true);
                Console.WriteLine("已将M10设为ON");

                // --- 读取X/Y ---
                bool x0 = plc.ReadX(0);
                bool y0 = plc.ReadY(0);
                Console.WriteLine($"X0 = {(x0 ? "ON" : "OFF")}, Y0 = {(y0 ? "ON" : "OFF")}");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"操作出错: {ex.Message}");
            }
        }
    }
}
```

### 5.2 HslCommunication方案完整代码

```csharp
using System;
using HslCommunication;
using HslCommunication.ModBus;

namespace HslCommunicationDemo
{
    /// <summary>
    /// 汇川PLC通信管理类（HslCommunication方案）
    /// HslCommunication封装更简洁，API更友好，推荐新手使用
    /// </summary>
    public class InovancePLCManager_HSL : IDisposable
    {
        private ModbusTcpNet _client;
        private bool _isConnected = false;

        /// <summary>
        /// 连接状态变化事件
        /// </summary>
        public event EventHandler<bool> ConnectionChanged;

        public bool IsConnected => _isConnected;

        /// <summary>
        /// 初始化并连接PLC
        /// </summary>
        /// <param name="ip">PLC IP地址</param>
        /// <param name="port">Modbus TCP端口</param>
        /// <param name="slaveId">从站地址</param>
        public bool Connect(string ip, int port = 502, byte slaveId = 1)
        {
            try
            {
                _client = new ModbusTcpNet(ip, port, slaveId);
                // 字序设置：根据汇川PLC的实际配置来调整
                // ABCD = 大端序（高字在前）
                // CDAB = 小端序（低字在前）
                // DCBA = 字节反转
                // BADC = 字交换
                _client.DataFormat = DataFormat.ABCD;
                _client.IsStringReverse = false;
                _client.ConnectServer();

                _isConnected = true;
                ConnectionChanged?.Invoke(this, true);
                return true;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"连接失败: {ex.Message}");
                _isConnected = false;
                return false;
            }
        }

        /// <summary>
        /// 断开连接
        /// </summary>
        public void Disconnect()
        {
            _client?.ConnectClose();
            _isConnected = false;
            ConnectionChanged?.Invoke(this, false);
        }

        #region 读取D寄存器

        /// <summary>
        /// 读取单个D寄存器（16位无符号整数）
        /// </summary>
        /// <param name="dNumber">D编号（如D100传100）</param>
        public ushort ReadD_UInt16(int dNumber)
        {
            CheckConnection();
            // 地址格式："4" + (偏移地址 + 1) = "4" + (dNumber + 1)
            // D100 -> "4101"  即 40001 + 100
            // 也可以直接用 "40101"
            string address = $"4{dNumber + 1}";
            return (ushort)_client.ReadUInt16(address).Content;
        }

        /// <summary>
        /// 读取单个D寄存器（16位有符号整数）
        /// </summary>
        public short ReadD_Int16(int dNumber)
        {
            CheckConnection();
            string address = $"4{dNumber + 1}";
            return _client.ReadInt16(address).Content;
        }

        /// <summary>
        /// 读取D寄存器的32位浮点数（占用2个D寄存器）
        /// </summary>
        /// <param name="dNumber">D编号</param>
        public float ReadD_Float32(int dNumber)
        {
            CheckConnection();
            string address = $"4{dNumber + 1}";
            return _client.ReadFloat(address).Content;
        }

        /// <summary>
        /// 批量读取D寄存器（16位）
        /// </summary>
        public ushort[] ReadD_Multiple(int startDNumber, int count)
        {
            CheckConnection();
            string address = $"4{startDNumber + 1}";
            return _client.ReadUInt16(address, count).Content;
        }

        #endregion

        #region 写入D寄存器

        /// <summary>
        /// 写入单个D寄存器（功能码06）
        /// 天然只影响目标地址
        /// </summary>
        /// <param name="dNumber">D编号</param>
        /// <param name="value">写入值</param>
        public OperateResult WriteD_UInt16(int dNumber, ushort value)
        {
            CheckConnection();
            string address = $"4{dNumber + 1}";
            return _client.Write(address, value);
        }

        /// <summary>
        /// 写入单个D寄存器（有符号）
        /// </summary>
        public OperateResult WriteD_Int16(int dNumber, short value)
        {
            CheckConnection();
            string address = $"4{dNumber + 1}";
            return _client.Write(address, value);
        }

        /// <summary>
        /// 写入浮点数到D寄存器（占用2个D）
        /// </summary>
        public OperateResult WriteD_Float32(int dNumber, float value)
        {
            CheckConnection();
            string address = $"4{dNumber + 1}";
            return _client.Write(address, value);
        }

        #endregion

        #region 读写M/Y/X继电器

        /// <summary>
        /// 读取M继电器状态
        /// 地址格式："0" + (地址+1) = "00001"起始
        /// </summary>
        public bool ReadM(int mNumber)
        {
            CheckConnection();
            string address = $"0{mNumber + 1}";
            return _client.ReadCoil(address).Content;
        }

        /// <summary>
        /// 写入M继电器（功能码05）
        /// </summary>
        public OperateResult WriteM(int mNumber, bool value)
        {
            CheckConnection();
            string address = $"0{mNumber + 1}";
            return _client.WriteCoil(address, value);
        }

        /// <summary>
        /// 读取X继电器（只读，功能码02）
        /// 地址格式："1" + (地址+1) = "10001"起始
        /// </summary>
        public bool ReadX(int xNumber)
        {
            CheckConnection();
            string address = $"1{xNumber + 1}";
            return _client.ReadDiscrete(address).Content;
        }

        /// <summary>
        /// 读取Y继电器
        /// </summary>
        public bool ReadY(int yNumber)
        {
            // 注意：Y和M的地址映射可能重叠
            // 具体映射方式需要查阅汇川PLC手册
            string address = $"0{yNumber + 1}";
            return _client.ReadCoil(address).Content;
        }

        #endregion

        #region 辅助方法

        private void CheckConnection()
        {
            if (!_isConnected)
            {
                throw new InvalidOperationException("PLC未连接，请先调用Connect方法");
            }
        }

        public void Dispose()
        {
            Disconnect();
        }

        #endregion
    }

    // ===== 使用示例 =====
    class Program
    {
        static void Main()
        {
            using var plc = new InovancePLCManager_HSL();

            // 连接PLC
            if (!plc.Connect("192.168.1.100", 502, 1))
            {
                Console.WriteLine("PLC连接失败！");
                return;
            }
            Console.WriteLine("PLC连接成功！");

            try
            {
                // === 读取D寄存器 ===
                ushort d100 = plc.ReadD_UInt16(100);
                Console.WriteLine($"D100 = {d100}");

                short d200 = plc.ReadD_Int16(200);
                Console.WriteLine($"D200 = {d200}");

                float d300 = plc.ReadD_Float32(300);
                Console.WriteLine($"D300(浮点数) = {d300:F2}");

                // === 写入D寄存器（功能码06，只影响目标地址）===
                var result = plc.WriteD_UInt16(900, 1234);
                Console.WriteLine($"写入D900=1234: {(result.IsSuccess ? "成功" : "失败: " + result.Message)}");

                // 写入浮点数
                result = plc.WriteD_Float32(500, 36.5f);
                Console.WriteLine($"写入D500=36.5: {(result.IsSuccess ? "成功" : "失败: " + result.Message)}");

                // === 批量读取 ===
                ushort[] batch = plc.ReadD_Multiple(100, 10);
                Console.WriteLine("D100-D109:");
                for (int i = 0; i < batch.Length; i++)
                    Console.WriteLine($"  D{100 + i} = {batch[i]}");

                // === 读写继电器 ===
                bool m10 = plc.ReadM(10);
                Console.WriteLine($"M10 = {(m10 ? "ON" : "OFF")}");

                plc.WriteM(10, true);
                Console.WriteLine("已将M10设为ON");

                bool x0 = plc.ReadX(0);
                Console.WriteLine($"X0 = {(x0 ? "ON" : "OFF")}");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"操作出错: {ex.Message}");
            }
        }
    }
}
```

### 5.3 原生Socket方案完整代码（无第三方库）

```csharp
using System;
using System.Net.Sockets;
using System.Text;

namespace RawSocketModbus
{
    /// <summary>
    /// 原生Socket实现Modbus TCP通信（无第三方库）
    /// 适合学习Modbus协议原理，理解底层通信过程
    /// </summary>
    public class RawModbusTCP : IDisposable
    {
        private TcpClient _tcpClient;
        private NetworkStream _stream;
        private ushort _transactionId = 0;
        private int _timeout = 3000;

        public bool IsConnected => _tcpClient?.Connected ?? false;

        /// <summary>
        /// 连接PLC
        /// </summary>
        public bool Connect(string ip, int port = 502)
        {
            try
            {
                _tcpClient = new TcpClient();
                _tcpClient.ReceiveTimeout = _timeout;
                _tcpClient.SendTimeout = _timeout;
                _tcpClient.Connect(ip, port);
                _stream = _tcpClient.GetStream();
                return true;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"连接失败: {ex.Message}");
                return false;
            }
        }

        /// <summary>
        /// 断开连接
        /// </summary>
        public void Disconnect()
        {
            try { _stream?.Close(); } catch { }
            try { _tcpClient?.Close(); } catch { }
        }

        #region Modbus TCP底层通信

        /// <summary>
        /// 发送Modbus TCP请求并接收响应
        /// </summary>
        /// <param name="slaveId">从站地址</param>
        /// <param name="functionCode">功能码</param>
        /// <param name="requestData">请求数据（功能码之后的部分）</param>
        /// <returns>响应数据（PDU部分，不含MBAP头）</returns>
        private byte[] SendModbusRequest(byte slaveId, byte functionCode, byte[] requestData)
        {
            if (!IsConnected)
                throw new InvalidOperationException("未连接到PLC");

            _transactionId++;  // 事务ID自增

            // 构建MBAP头 + PDU
            // MBAP头(7字节) + PDU
            byte[] mbapHeader = new byte[7];
            mbapHeader[0] = (byte)(_transactionId >> 8);   // 事务ID高字节
            mbapHeader[1] = (byte)(_transactionId & 0xFF); // 事务ID低字节
            mbapHeader[2] = 0x00;  // 协议标识（Modbus = 0x0000）
            mbapHeader[3] = 0x00;
            mbapHeader[4] = (byte)((requestData.Length + 1) >> 8); // 长度高字节（PDU长度 = 功能码1字节 + 数据N字节）
            mbapHeader[5] = (byte)((requestData.Length + 1) & 0xFF); // 长度低字节
            mbapHeader[6] = slaveId;  // 单元标识（从站地址）

            // 组合：MBAP头 + 功能码 + 数据
            byte[] fullRequest = new byte[7 + 1 + requestData.Length];
            Array.Copy(mbapHeader, 0, fullRequest, 0, 7);
            fullRequest[7] = functionCode;
            Array.Copy(requestData, 0, fullRequest, 8, requestData.Length);

            // 发送请求
            _stream.Write(fullRequest, 0, fullRequest.Length);

            // 接收响应MBAP头（7字节）
            byte[] responseHeader = new byte[7];
            int bytesRead = ReadExact(_stream, responseHeader, 0, 7);
            if (bytesRead < 7)
                throw new Exception("接收响应头失败");

            // 验证事务ID
            ushort respTransactionId = (ushort)((responseHeader[0] << 8) | responseHeader[1]);
            if (respTransactionId != _transactionId)
                throw new Exception($"事务ID不匹配: 期望{_transactionId}, 收到{respTransactionId}");

            // 获取数据长度
            int dataLength = (responseHeader[4] << 8) | responseHeader[5];

            // 接收PDU数据
            byte[] pduData = new byte[dataLength];
            bytesRead = ReadExact(_stream, pduData, 0, dataLength);
            if (bytesRead < dataLength)
                throw new Exception("接收响应数据失败");

            // 检查异常响应（功能码最高位为1表示异常）
            if ((pduData[0] & 0x80) != 0)
            {
                byte exceptionCode = pduData[1];
                throw new Exception($"Modbus异常响应: 功能码={functionCode}, 异常码={exceptionCode}");
            }

            // 返回PDU部分（去掉功能码，只返回数据部分）
            byte[] resultData = new byte[dataLength - 1];
            Array.Copy(pduData, 1, resultData, 0, dataLength - 1);
            return resultData;
        }

        /// <summary>
        /// 精确读取指定长度的数据
        /// </summary>
        private int ReadExact(NetworkStream stream, byte[] buffer, int offset, int count)
        {
            int totalRead = 0;
            while (totalRead < count)
            {
                int read = stream.Read(buffer, offset + totalRead, count - totalRead);
                if (read == 0)
                    throw new Exception("连接已关闭");
                totalRead += read;
            }
            return totalRead;
        }

        #endregion

        #region 功能码03：读保持寄存器

        /// <summary>
        /// 读取保持寄存器（功能码03）
        /// </summary>
        /// <param name="slaveId">从站地址</param>
        /// <param name="startAddress">起始地址（偏移地址，从0开始）</param>
        /// <param name="count">读取数量</param>
        /// <returns>寄存器值数组</returns>
        public ushort[] ReadHoldingRegisters(byte slaveId, ushort startAddress, ushort count)
        {
            // 构建请求数据（PDU中的数据部分，不含功能码）
            byte[] requestData = new byte[4];
            requestData[0] = (byte)(startAddress >> 8);    // 起始地址高字节
            requestData[1] = (byte)(startAddress & 0xFF);   // 起始地址低字节
            requestData[2] = (byte)(count >> 8);            // 数量高字节
            requestData[3] = (byte)(count & 0xFF);          // 数量低字节

            // 发送请求（功能码 = 0x03）
            byte[] responseData = SendModbusRequest(slaveId, 0x03, requestData);

            // 解析响应
            // 响应格式：[字节数(1B)] + [数据(N*2B)]
            int byteCount = responseData[0];
            if (byteCount != count * 2)
                throw new Exception($"返回数据长度不匹配: 期望{count * 2}, 收到{byteCount}");

            ushort[] registers = new ushort[count];
            for (int i = 0; i < count; i++)
            {
                registers[i] = (ushort)((responseData[1 + i * 2] << 8) | responseData[2 + i * 2]);
            }

            return registers;
        }

        #endregion

        #region 功能码06：写单个保持寄存器

        /// <summary>
        /// 写入单个保持寄存器（功能码06）
        /// 天然只影响目标地址
        /// </summary>
        /// <param name="slaveId">从站地址</param>
        /// <param name="address">寄存器地址（偏移地址）</param>
        /// <param name="value">写入值</param>
        public void WriteSingleRegister(byte slaveId, ushort address, ushort value)
        {
            // 构建请求数据
            byte[] requestData = new byte[4];
            requestData[0] = (byte)(address >> 8);    // 地址高字节
            requestData[1] = (byte)(address & 0xFF); // 地址低字节
            requestData[2] = (byte)(value >> 8);      // 值高字节
            requestData[3] = (byte)(value & 0xFF);     // 值低字节

            // 发送请求（功能码 = 0x06）
            byte[] responseData = SendModbusRequest(slaveId, 0x06, requestData);

            // 功能码06的响应 = 请求的回显（4字节数据）
            // 验证回显
            ushort echoAddress = (ushort)((responseData[0] << 8) | responseData[1]);
            ushort echoValue = (ushort)((responseData[2] << 8) | responseData[3]);

            if (echoAddress != address)
                throw new Exception($"写寄存器回显地址不匹配");
            if (echoValue != value)
                throw new Exception($"写寄存器回显值不匹配");
        }

        #endregion

        #region 功能码05：写单个线圈

        /// <summary>
        /// 写单个线圈（功能码05）
        /// </summary>
        public void WriteSingleCoil(byte slaveId, ushort address, bool value)
        {
            byte[] requestData = new byte[4];
            requestData[0] = (byte)(address >> 8);
            requestData[1] = (byte)(address & 0xFF);
            requestData[2] = value ? (byte)0xFF : (byte)0x00;
            requestData[3] = 0x00;

            SendModbusRequest(slaveId, 0x05, requestData);
        }

        #endregion

        #region 功能码01：读线圈

        /// <summary>
        /// 读线圈（功能码01）
        /// </summary>
        public bool[] ReadCoils(byte slaveId, ushort startAddress, ushort count)
        {
            byte[] requestData = new byte[4];
            requestData[0] = (byte)(startAddress >> 8);
            requestData[1] = (byte)(startAddress & 0xFF);
            requestData[2] = (byte)(count >> 8);
            requestData[3] = (byte)(count & 0xFF);

            byte[] responseData = SendModbusRequest(slaveId, 0x01, requestData);

            int byteCount = responseData[0];
            bool[] coils = new bool[count];
            for (int i = 0; i < count; i++)
            {
                int byteIndex = 1 + (i / 8);
                int bitIndex = i % 8;
                coils[i] = (responseData[byteIndex] & (1 << bitIndex)) != 0;
            }

            return coils;
        }

        #endregion

        #region 功能码16：写多个保持寄存器

        /// <summary>
        /// 写多个保持寄存器（功能码16）
        /// </summary>
        public void WriteMultipleRegisters(byte slaveId, ushort startAddress, ushort[] values)
        {
            int count = values.Length;
            byte[] requestData = new byte[5 + count * 2];
            requestData[0] = (byte)(startAddress >> 8);
            requestData[1] = (byte)(startAddress & 0xFF);
            requestData[2] = (byte)(count >> 8);
            requestData[3] = (byte)(count & 0xFF);
            requestData[4] = (byte)(count * 2);  // 字节数

            for (int i = 0; i < count; i++)
            {
                requestData[5 + i * 2] = (byte)(values[i] >> 8);
                requestData[6 + i * 2] = (byte)(values[i] & 0xFF);
            }

            SendModbusRequest(slaveId, 0x10, requestData);
        }

        #endregion

        public void Dispose()
        {
            Disconnect();
        }
    }

    // ===== 使用示例 =====
    class Program
    {
        static void Main()
        {
            using var modbus = new RawModbusTCP();

            Console.WriteLine("=== 原生Socket Modbus TCP 演示 ===\n");

            // 连接PLC
            Console.Write("连接PLC...");
            if (!modbus.Connect("192.168.1.100", 502))
            {
                Console.WriteLine("失败！");
                return;
            }
            Console.WriteLine("成功！\n");

            byte slaveId = 1;

            try
            {
                // --- 功能码03：读保持寄存器 ---
                Console.WriteLine("【功能码03】读取D100-D104:");
                ushort[] values = modbus.ReadHoldingRegisters(slaveId, startAddress: 100, count: 5);
                for (int i = 0; i < values.Length; i++)
                {
                    Console.WriteLine($"  D{100 + i} = {values[i]}");
                }

                // --- 功能码06：写单个保持寄存器 ---
                Console.WriteLine("\n【功能码06】写入D900 = 1234:");
                modbus.WriteSingleRegister(slaveId, address: 900, value: 1234);
                Console.WriteLine("  写入成功！（只影响D900，不影响其他地址）");

                // 读回确认
                ushort[] confirm = modbus.ReadHoldingRegisters(slaveId, 900, 1);
                Console.WriteLine($"  D900确认值 = {confirm[0]}");

                // --- 功能码05：写单个线圈 ---
                Console.WriteLine("\n【功能码05】设置M10 = ON:");
                modbus.WriteSingleCoil(slaveId, address: 10, value: true);
                Console.WriteLine("  写入成功！");

                // --- 功能码01：读线圈 ---
                Console.WriteLine("\n【功能码01】读取M0-M7:");
                bool[] coils = modbus.ReadCoils(slaveId, 0, 8);
                for (int i = 0; i < coils.Length; i++)
                {
                    Console.WriteLine($"  M{i} = {(coils[i] ? "ON" : "OFF")}");
                }

                // --- 功能码16：写多个保持寄存器 ---
                Console.WriteLine("\n【功能码16】批量写入D100=100, D101=200, D102=300:");
                ushort[] writeValues = new ushort[] { 100, 200, 300 };
                modbus.WriteMultipleRegisters(slaveId, 100, writeValues);
                Console.WriteLine("  批量写入成功！");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"\n操作出错: {ex.Message}");
            }
        }
    }
}
```

---

## 六、常见问题与注意事项

### 6.1 PLC侧Modbus TCP配置方法

要让汇川PLC支持Modbus TCP通信，需要在PLC编程软件中进行配置：

**InoProShop配置步骤**：
1. 打开InoProShop编程软件，打开PLC工程
2. 在左侧导航栏找到 **"通信"** 或 **"网络"** 配置
3. 找到 **"Modbus TCP"** 或 **"MODBUS"** 选项
4. **启用Modbus TCP Server**（让PLC作为Modbus从站）
5. **设置端口**：通常为502（默认值）
6. **设置从站地址（站号）**：通常为1
7. **配置允许访问的寄存器区域**：确保D寄存器区域开放
8. **下载配置到PLC并重启**

**注意事项**：
- 确保PLC和上位机在同一网段（或能互相Ping通）
- 确保PLC的IP地址配置正确
- 有些型号需要在PLC程序中调用 `MODBUS_INIT` 等初始化指令
- 具体配置步骤请参考对应型号的PLC用户手册

### 6.2 UW/W区不支持Modbus

| 区域 | 是否支持Modbus | 替代方案 |
|------|--------------|---------|
| UW（链接字） | 不支持 | 使用D区作为通信缓冲区，在PLC程序中手动搬运数据 |
| W（链接位） | 不支持 | 使用M区作为通信缓冲区 |

**原因**：UW/W是PLC间高速通信专用区域，使用的是汇川自己的通信协议，不是标准Modbus协议。

**替代方案**：
```plaintext
# 如果需要在两台PLC之间通过上位机传递数据：
# 1. PLC1将需要传递的数据写入D100-D109
# 2. 上位机读取PLC1的D100-D109
# 3. 上位机将数据写入PLC2的D100-D109
# 4. PLC2从D100-D109读取数据

# 或者在PLC程序中：
# 1. PLC1将UW区的数据复制到D区
# 2. 上位机通过Modbus读写D区
# 3. PLC2从D区读取数据，再复制到自己的UW区
```

### 6.3 字序问题（大端序/小端序）

Modbus协议使用**大端序（Big-Endian）**传输数据，即高字节在前，低字节在后。

```
大端序（Modbus标准）：发送值0x1234时，先发0x12再发0x34
小端序（x86系统）：内存中存0x1234时，先存0x34再存0x12

示例：寄存器值 = 0x1234（十进制4660）
Modbus传输顺序：[0x12] [0x34]
```

**32位数据的字序问题**：
32位数据（浮点数、双字）占用2个寄存器，存在"字序"问题：

```
假设D100-D101存储一个32位浮点数 25.5（IEEE 754: 0x41CC0000）

大端序（高字在前）：D100=0x41CC（高字）, D101=0x0000（低字）
小端序（低字在前）：D100=0x0000（低字）, D101=0x41CC（高字）

不同的字序配置会导致读取的浮点数完全不同！
```

**解决方法**：
```csharp
// HslCommunication中设置字序
client.DataFormat = DataFormat.ABCD;  // 大端序（高字在前）
// 或
client.DataFormat = DataFormat.CDAB;  // 小端序（低字在前）
// 或
client.DataFormat = DataFormat.BADC; // 字交换
// 或
client.DataFormat = DataFormat.DCBA; // 字节反转

// NModbus4中需要手动处理（参考前面5.1节的ReadD_Float32方法）
```

**建议**：先确认PLC的字序设置，然后在代码中做相应调整。如果不确定，可以做一个测试：在PLC中给D100-D101写入一个已知值（如浮点数100.0），然后上位机分别用不同字序读取，看哪个结果正确。

### 6.4 32位数据读写（浮点数、双字）

#### 读取浮点数

```csharp
// 方法1：HslCommunication（最简单）
float value = client.ReadFloat("4901").Content;  // 读取D900-D901组成的浮点数

// 方法2：NModbus4（手动处理）
ushort[] regs = master.ReadHoldingRegisters(1, 900, 2);
byte[] bytes = new byte[4];
bytes[0] = (byte)(regs[0] >> 8);   // 高字高字节
bytes[1] = (byte)(regs[0] & 0xFF); // 高字低字节
bytes[2] = (byte)(regs[1] >> 8);   // 低字高字节
bytes[3] = (byte)(regs[1] & 0xFF); // 低字低字节
float value = BitConverter.ToSingle(bytes, 0);
```

#### 读取32位有符号整数

```csharp
ushort[] regs = master.ReadHoldingRegisters(1, 900, 2);
// 高字在前
int value = (regs[0] << 16) | regs[1];
```

#### 写入浮点数

```csharp
// 写入浮点数36.5到D900-D901
// 使用功能码16（批量写2个寄存器）
byte[] bytes = BitConverter.GetBytes(36.5f);
ushort highWord = (ushort)((bytes[0] << 8) | bytes[1]); // D900（高字）
ushort lowWord = (ushort)((bytes[2] << 8) | bytes[3]);  // D901（低字）
master.WriteMultipleRegisters(1, 900, new ushort[] { highWord, lowWord });
```

> **注意**：写入32位数据时，会同时写入2个连续地址（如D900和D901），这会影响相邻地址！如果D901有其他用途，需要特别小心。

### 6.5 超时设置与重连机制

```csharp
/// <summary>
/// 带自动重连的读取方法
/// </summary>
public ushort ReadWithRetry(string ip, int port, byte slaveId, ushort address, int maxRetries = 3)
{
    int retryCount = 0;

    while (retryCount < maxRetries)
    {
        try
        {
            using (TcpClient client = new TcpClient())
            {
                client.ReceiveTimeout = 3000;  // 读超时3秒
                client.SendTimeout = 3000;     // 写超时3秒
                client.Connect(ip, port);

                var factory = new ModbusFactory();
                IModbusMaster master = factory.CreateMaster(client);
                master.Transport.ReadTimeout = 3000;

                ushort[] result = master.ReadHoldingRegisters(slaveId, address, 1);
                return result[0];  // 成功则返回
            }
        }
        catch (Exception ex)
        {
            retryCount++;
            Console.WriteLine($"第{retryCount}次重试: {ex.Message}");

            if (retryCount >= maxRetries)
            {
                throw new Exception($"读取失败，已重试{maxRetries}次", ex);
            }

            // 指数退避：等待时间逐渐增加
            int waitMs = 1000 * retryCount;  // 1秒, 2秒, 3秒...
            Thread.Sleep(waitMs);
        }
    }

    throw new Exception("读取失败");
}
```

**超时设置建议**：
| 场景 | 推荐超时时间 | 说明 |
|------|------------|------|
| 局域网（正常情况） | 1000-2000ms | 局域网通信快，1-2秒足够 |
| 局域网（网络不稳定） | 3000-5000ms | 留更多余量 |
| 串口通信 | 500-1000ms | 串口速度较慢 |
| 通过交换机/路由器 | 3000-5000ms | 中间设备增加延迟 |

### 6.6 批量读写的建议（减少通信次数）

**核心原则**：尽量减少通信次数，一次读取尽可能多的数据。

```
# 不推荐：逐个读取（通信10次）
D100 = Read(100)
D101 = Read(101)
D102 = Read(102)
...（共10次通信）

# 推荐：批量读取（通信1次）
D100-D109 = Read(100, 10)   （只需1次通信）
```

**代码示例**：
```csharp
// ====== 批量读取示例 ======
// 假设需要读取以下地址的数据：
// D100=温度, D101=压力, D102=流量, D103=液位, D104=频率, D105=电流
// D200=报警代码, D201=运行时间

// 方案1：分两次批量读取（更清晰）
ushort[] block1 = master.ReadHoldingRegisters(1, 100, 6);  // 读取D100-D105
ushort[] block2 = master.ReadHoldingRegisters(1, 200, 2);  // 读取D200-D201

float temp = ConvertToFloat(block1[0]);   // D100
float pressure = ConvertToFloat(block1[1]); // D101
// ...

// 方案2：如果地址不连续怎么办？
// 只能分开读取，但仍然比逐个读取高效
ushort[] tempData = master.ReadHoldingRegisters(1, 100, 1);   // D100
ushort[] alarmData = master.ReadHoldingRegisters(1, 200, 1);   // D200

// ====== 批量写入示例 ======
// 假设需要初始化多个参数
ushort[] initValues = new ushort[]
{
    100,   // D100: 温度上限
    0,     // D101: 温度下限
    50,    // D102: 压力上限
    0      // D103: 压力下限
};
master.WriteMultipleRegisters(1, 100, initValues);  // 一次写入4个值
```

**批量读写的注意事项**：
1. **单次读取数量限制**：Modbus协议规定单次最多读取125个寄存器，建议控制在100以内
2. **通信数据量**：一次读取太多数据会增加响应时间和出错概率，建议根据实际需求合理分批
3. **地址连续性**：批量读写要求地址是连续的，不连续的地址只能分开读取
4. **响应时间**：单次通信的响应时间通常在10-100ms，批量读取可以大幅减少总通信时间

---

## 七、Modbus异常码参考

当PLC返回异常响应时，功能码的最高位会被置1，并附带一个异常码：

| 异常码 | 名称 | 含义 | 常见原因 |
|--------|------|------|---------|
| 0x01 | 非法功能码 | PLC不支持该功能码 | 功能码写错或PLC不支持 |
| 0x02 | 非法数据地址 | 请求的地址超出PLC范围 | 地址偏移计算错误 |
| 0x03 | 非法数据值 | 请求中的数据值无效 | 写入值超出范围或数量无效 |
| 0x04 | 从站设备故障 | PLC内部处理出错 | PLC程序错误或硬件故障 |
| 0x05 | 确认 | 请求已接受，正在处理 | 长时间操作中 |
| 0x06 | 从站设备忙 | PLC正在处理其他请求 | 请求过于频繁，降低轮询频率 |

```csharp
// 异常处理示例
try
{
    ushort[] result = master.ReadHoldingRegisters(1, 900, 1);
}
catch (ModbusSlaveException ex)
{
    switch (ex.SlaveExceptionCode)
    {
        case 1:
            Console.WriteLine("错误：非法功能码，PLC不支持此操作");
            break;
        case 2:
            Console.WriteLine("错误：非法地址，请检查寄存器地址是否正确");
            break;
        case 3:
            Console.WriteLine("错误：非法数据值");
            break;
        default:
            Console.WriteLine($"Modbus异常: 异常码={ex.SlaveExceptionCode}");
            break;
    }
}
catch (TimeoutException)
{
    Console.WriteLine("通信超时，请检查网络连接和PLC状态");
}
catch (Exception ex)
{
    Console.WriteLine($"未知错误: {ex.Message}");
}
```

---

## 八、快速参考卡片

### 常用操作速查表

| 操作 | NModbus4 | HslCommunication | 功能码 |
|------|----------|-----------------|--------|
| 读D寄存器(单个) | `ReadHoldingRegisters(id, addr, 1)` | `ReadUInt16("4xxx")` | 03 |
| 读D寄存器(批量) | `ReadHoldingRegisters(id, addr, n)` | `ReadUInt16("4xxx", n)` | 03 |
| 写D寄存器(单个) | `WriteSingleRegister(id, addr, val)` | `Write("4xxx", val)` | **06** |
| 写D寄存器(批量) | `WriteMultipleRegisters(id, addr, vals)` | `Write("4xxx", vals)` | 16 |
| 读D浮点数 | 读2个寄存器+手动合并 | `ReadFloat("4xxx")` | 03 |
| 写D浮点数 | 手动拆分+批量写2个寄存器 | `Write("4xxx", floatVal)` | 16 |
| 读M继电器 | `ReadCoils(id, addr, 1)` | `ReadCoil("0xxx")` | 01 |
| 写M继电器 | `WriteSingleCoil(id, addr, val)` | `WriteCoil("0xxx", val)` | 05 |
| 读X继电器 | `ReadInputs(id, addr, 1)` | `ReadDiscrete("1xxx")` | 02 |

### 地址转换速查表

| PLC地址 | 偏移地址 | Modbus地址 | NModbus4参数 | HslCommunication地址 |
|---------|---------|-----------|-------------|-------------------|
| D0 | 0 | 40001 | `startAddress=0` | `"41"` 或 `"40001"` |
| D100 | 100 | 40101 | `startAddress=100` | `"4101"` 或 `"40101"` |
| D900 | 900 | 40901 | `startAddress=900` | `"4901"` 或 `"40901"` |
| D2000 | 2000 | 42001 | `startAddress=2000` | `"42001"` |

---

> **重要提示**：本文档中的地址映射和寄存器信息基于汇川H3U/H5U系列的通用规则。不同型号和固件版本可能存在差异，实际使用时请务必查阅对应型号的《Modbus通信手册》确认具体映射关系。
