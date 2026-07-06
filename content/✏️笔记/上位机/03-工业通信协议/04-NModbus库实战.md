# 04 - NModbus库实战

---

## 一、知识讲解

### 1.1 NModbus 简介

NModbus 是 C# 中最常用的 Modbus 通信开源库，封装了 Modbus TCP 和 Modbus RTU 的底层协议细节，让开发者可以像调用普通方法一样执行 Modbus 读写操作，无需手动构造和解析协议帧。

#### NModbus4 vs NModbus5

| 特性 | NModbus4 | NModbus5 |
|------|----------|----------|
| NuGet包名 | `NModbus4` 或 `NModbus4.Net` | `NModbus5` 或 `NModbus.Core` |
| .NET支持 | .NET Framework 4.5+, .NET Standard 2.0 | .NET Standard 2.0, .NET 5+ |
| 维护状态 | 社区维护，较成熟 | 更新活跃，API更现代 |
| 核心API | `ModbusFactory.CreateMaster()` | `ModbusFactory.CreateMaster()` |
| 推荐度 | 稳定项目使用 | 新项目推荐 |

#### 安装方式

```bash
# NModbus4（推荐用于 .NET Framework）
dotnet add package NModbus4

# NModbus5（推荐用于 .NET Core / .NET 5+）
dotnet add package NModbus5

# 串口通信需要的包
dotnet add package System.IO.Ports
```

### 1.2 核心概念

**Master（主站）**：上位机发起 Modbus 请求的一方。
**Slave（从站）**：被读取/写入的设备（PLC、仪表等）。

NModbus 提供了两种 Master：
- `ModbusMaster` — 通过串口通信的 Modbus RTU 主站
- `ModbusTcpMaster` — 通过 TCP 通信的 Modbus TCP 主站

### 1.3 读写操作对照表

| 操作 | 方法 | 功能码 |
|------|------|--------|
| 读线圈 | `ReadCoils()` | 01 |
| 读离散输入 | `ReadInputs()` | 02 |
| 读保持寄存器 | `ReadHoldingRegisters()` | 03 |
| 读输入寄存器 | `ReadInputRegisters()` | 04 |
| 写单个线圈 | `WriteSingleCoil()` | 05 |
| 写单个寄存器 | `WriteSingleRegister()` | 06 |
| 写多个线圈 | `WriteMultipleCoils()` | 15 |
| 写多个寄存器 | `WriteMultipleRegisters()` | 16 |

---

## 二、代码示例

### 2.1 创建 Modbus TCP Master

```csharp
using System;
using System.Net.Sockets;
using NModbus;

/// <summary>
/// 创建Modbus TCP Master的基本步骤
/// 适用于上位机通过以太网与PLC/设备通信
/// </summary>
public class ModbusTcpBasic
{
    private TcpClient _tcpClient;
    private IModbusMaster _master;

    public void Connect(string ip, int port = 502)
    {
        // 第一步：创建TCP客户端并连接
        _tcpClient = new TcpClient();
        _tcpClient.Connect(ip, port);
        Console.WriteLine($"已连接到 {ip}:{port}");

        // 第二步：通过工厂创建Modbus Master
        var factory = new ModbusFactory();
        _master = factory.CreateMaster(_tcpClient);

        // 第三步：设置超时时间（毫秒）
        _master.Transport.ReadTimeout = 3000;
        _master.Transport.WriteTimeout = 3000;

        // 第四步：设置重试次数
        _master.Transport.Retries = 3;

        Console.WriteLine("Modbus TCP Master 创建成功");
    }

    public void Disconnect()
    {
        _master?.Dispose();
        _tcpClient?.Close();
        Console.WriteLine("连接已断开");
    }
}
```

### 2.2 创建 Modbus RTU Master（串口）

```csharp
using System;
using System.IO.Ports;
using NModbus;

/// <summary>
/// 创建Modbus RTU Master的基本步骤
/// 适用于上位机通过RS485串口与PLC/设备通信
/// </summary>
public class ModbusRtuBasic
{
    private SerialPort _serialPort;
    private IModbusMaster _master;

    public void Connect(string portName, int baudRate = 9600)
    {
        // 第一步：创建并配置串口
        _serialPort = new SerialPort(portName, baudRate, Parity.None, 8, StopBits.One);
        _serialPort.ReadTimeout = 3000;
        _serialPort.WriteTimeout = 3000;

        // 第二步：打开串口
        _serialPort.Open();
        Console.WriteLine($"串口 {portName} 已打开 (波特率: {baudRate})");

        // 第三步：创建Modbus RTU Master
        var factory = new ModbusFactory();
        _master = factory.CreateRtuMaster(_serialPort);

        // 第四步：设置超时和重试
        _master.Transport.ReadTimeout = 3000;
        _master.Transport.WriteTimeout = 3000;
        _master.Transport.Retries = 3;

        Console.WriteLine("Modbus RTU Master 创建成功");
    }

    public void Disconnect()
    {
        _master?.Dispose();
        if (_serialPort != null && _serialPort.IsOpen)
        {
            _serialPort.Close();
        }
    }
}
```

### 2.3 读线圈（01）和读离散输入（02）

```csharp
using System;

/// <summary>
/// 功能码01：读线圈（Coil）—— 可读可写的位
/// 功能码02：读离散输入（Discrete Input）—— 只读的位
/// 返回值为 bool 数组，每个元素代表一个位的状态（true=ON, false=OFF）
/// </summary>
public void ReadBitsExample(IModbusMaster master, byte slaveAddress)
{
    // ===== 读线圈（功能码01）=====
    // 参数：从站地址, 起始地址, 读取数量
    bool[] coils = master.ReadCoils(slaveAddress, 0, 10);

    Console.WriteLine("线圈状态:");
    for (int i = 0; i < coils.Length; i++)
    {
        Console.WriteLine($"  线圈[{0 + i}] = {(coils[i] ? "ON" : "OFF")}");
    }

    // ===== 读离散输入（功能码02）=====
    // 参数：从站地址, 起始地址, 读取数量
    bool[] inputs = master.ReadInputs(slaveAddress, 0, 8);

    Console.WriteLine("离散输入状态:");
    for (int i = 0; i < inputs.Length; i++)
    {
        Console.WriteLine($"  输入[{0 + i}] = {(inputs[i] ? "ON" : "OFF")}");
    }
}
```

### 2.4 读保持寄存器（03）和读输入寄存器（04）

```csharp
using System;

/// <summary>
/// 功能码03：读保持寄存器（Holding Register）—— 可读可写的16位寄存器
/// 功能码04：读输入寄存器（Input Register）—— 只读的16位寄存器
/// 返回值为 ushort[] 数组，每个元素代表一个16位无符号整数
/// </summary>
public void ReadRegistersExample(IModbusMaster master, byte slaveAddress)
{
    // ===== 读保持寄存器（功能码03）=====
    // 参数：从站地址, 起始地址, 读取数量
    ushort[] holdingRegisters = master.ReadHoldingRegisters(slaveAddress, 0, 10);

    Console.WriteLine("保持寄存器值:");
    for (int i = 0; i < holdingRegisters.Length; i++)
    {
        Console.WriteLine($"  HR[{0 + i}] = {holdingRegisters[i]} (0x{holdingRegisters[i]:X4})");
    }

    // ===== 读输入寄存器（功能码04）=====
    ushort[] inputRegisters = master.ReadInputRegisters(slaveAddress, 0, 5);

    Console.WriteLine("输入寄存器值:");
    for (int i = 0; i < inputRegisters.Length; i++)
    {
        Console.WriteLine($"  IR[{0 + i}] = {inputRegisters[i]} (0x{inputRegisters[i]:X4})");
    }
}

/// <summary>
/// 数据类型转换示例
/// 工业设备返回的原始数据是 ushort（16位无符号整数）
/// 实际应用中常常需要转换为其他数据类型
/// </summary>
public class DataTypeConverter
{
    /// <summary>
    /// 将两个ushort组合为一个有符号整数（32位）
    /// 注意：不同设备可能有不同的字节顺序和符号位处理方式
    /// </summary>
    public static int RegistersToInt32(ushort high, ushort low)
    {
        // 大端序（高字在前）
        return (high << 16) | low;
    }

    /// <summary>
    /// 将两个ushort组合为一个浮点数（32位）
    /// </summary>
    public static float RegistersToFloat(ushort high, ushort low)
    {
        // 将两个16位值组合为4字节数组
        byte[] bytes = new byte[4];
        bytes[0] = (byte)(high >> 8);
        bytes[1] = (byte)(high & 0xFF);
        bytes[2] = (byte)(low >> 8);
        bytes[3] = (byte)(low & 0xFF);

        // 转换为float（注意字节序）
        if (BitConverter.IsLittleEndian)
        {
            Array.Reverse(bytes);
        }
        return BitConverter.ToSingle(bytes, 0);
    }

    /// <summary>
    /// 将ushort值按比例转换为实际物理量
    /// 例如：寄存器值1000，量程0-100，比例系数0.01
    /// </summary>
    public static double RegisterToScaledValue(ushort registerValue, double scale)
    {
        return registerValue * scale;
    }

    /// <summary>
    /// 将ushort值按量程转换为实际值
    /// 例如：寄存器值0-10000对应0-100.0度
    /// </summary>
    public static double RegisterToRangeValue(
        ushort registerValue, double rangeMin, double rangeMax, ushort rawMin, ushort rawMax)
    {
        double ratio = (double)(registerValue - rawMin) / (rawMax - rawMin);
        return rangeMin + ratio * (rangeMax - rangeMin);
    }
}
```

### 2.5 写单个线圈（05）和写单个寄存器（06）

```csharp
using System;

/// <summary>
/// 功能码05：写单个线圈（Force Single Coil）
/// 功能码06：写单个寄存器（Preset Single Register）
/// 这两个操作都是同步的，设备会返回 echoes 确认
/// </summary>
public void WriteSingleExample(IModbusMaster master, byte slaveAddress)
{
    // ===== 写单个线圈（功能码05）=====
    // 设置线圈0为 ON
    master.WriteSingleCoil(slaveAddress, 0, true);
    Console.WriteLine("线圈[0] 已设为 ON");

    // 设置线圈0为 OFF
    master.WriteSingleCoil(slaveAddress, 0, false);
    Console.WriteLine("线圈[0] 已设为 OFF");

    // ===== 写单个寄存器（功能码06）=====
    // 向寄存器1写入值 1000
    master.WriteSingleRegister(slaveAddress, 1, 1000);
    Console.WriteLine("寄存器[1] 已写入 1000");

    // 向寄存器1写入值 0
    master.WriteSingleRegister(slaveAddress, 1, 0);
    Console.WriteLine("寄存器[1] 已写入 0");
}
```

### 2.6 写多个线圈（15）和写多个寄存器（16）

```csharp
using System;

/// <summary>
/// 功能码15：写多个线圈（Force Multiple Coils）
/// 功能码16：写多个寄存器（Preset Multiple Registers）
/// 批量写入效率更高，一条命令写入多个值
/// </summary>
public void WriteMultipleExample(IModbusMaster master, byte slaveAddress)
{
    // ===== 写多个线圈（功能码15）=====
    bool[] coilsToWrite = new bool[] { true, false, true, true, false };
    master.WriteMultipleCoils(slaveAddress, 0, coilsToWrite);
    Console.WriteLine($"已写入 {coilsToWrite.Length} 个线圈状态");

    // ===== 写多个寄存器（功能码16）=====
    ushort[] registersToWrite = new ushort[] { 1000, 2000, 3000, 4000, 5000 };
    master.WriteMultipleRegisters(slaveAddress, 0, registersToWrite);
    Console.WriteLine($"已写入 {registersToWrite.Length} 个寄存器值");
}
```

### 2.7 超时设置与异常处理

```csharp
using System;
using System.IO.Ports;
using NModbus;

/// <summary>
/// Modbus通信中的异常处理最佳实践
/// 工业现场环境复杂，必须做好异常处理
/// </summary>
public class ModbusErrorHandling
{
    private IModbusMaster _master;
    private int _failCount = 0;
    private int _totalRequests = 0;

    /// <summary>
    /// 安全的读操作封装（带异常处理和重试）
    /// </summary>
    public ushort[] SafeReadHoldingRegisters(
        byte slaveAddress, ushort startAddress, ushort count)
    {
        const int maxRetry = 3;
        _totalRequests++;

        for (int retry = 0; retry < maxRetry; retry++)
        {
            try
            {
                ushort[] values = _master.ReadHoldingRegisters(
                    slaveAddress, startAddress, count);
                _failCount = 0;  // 成功后重置失败计数
                return values;
            }
            catch (TimeoutException ex)
            {
                Console.WriteLine($"[超时] 第{retry + 1}次重试: {ex.Message}");
            }
            catch (IOException ex)
            {
                Console.WriteLine($"[IO异常] 第{retry + 1}次重试: {ex.Message}");
            }
            catch (SlaveException ex)
            {
                // 从站返回了异常码
                Console.WriteLine($"[从站异常] 功能码:0x{(ex.FunctionCode):X2}, " +
                                  $"异常码:0x{(ex.SlaveExceptionCode):X2}");
                // 从站异常通常重试无意义，直接返回
                _failCount++;
                return null;
            }
            catch (FormatException ex)
            {
                Console.WriteLine($"[格式异常] 帧格式错误: {ex.Message}");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[未知异常] {ex.Message}");
            }

            // 重试前等待一段时间
            Thread.Sleep(200);
        }

        _failCount++;
        Console.WriteLine($"[失败] 读取失败，已达最大重试次数({maxRetry})");
        return null;
    }

    /// <summary>
    /// 获取通信统计信息
    /// </summary>
    public string GetStatistics()
    {
        double successRate = _totalRequests > 0
            ? (1.0 - (double)_failCount / _totalRequests) * 100
            : 0;
        return $"总请求: {_totalRequests}, 失败: {_failCount}, 成功率: {successRate:F1}%";
    }
}
```

### 2.8 批量读写优化技巧

```csharp
using System;
using System.Collections.Generic;
using System.Diagnostics;

/// <summary>
/// Modbus批量读写优化策略
/// 减少通信次数，提高采集效率
/// </summary>
public class ModbusBatchOptimization
{
    private IModbusMaster _master;
    private byte _slaveAddress = 1;

    /// <summary>
    /// 优化策略1：合并相邻地址的读取
    /// 如果需要读取地址 100、101、102 的寄存器，
    /// 不要发3次请求，而是发1次读3个寄存器的请求
    /// </summary>
    public Dictionary<ushort, ushort> ReadDisjointRegisters(
        List<ushort> addresses)
    {
        // 按地址排序
        addresses.Sort();

        // 计算需要读取的起始地址和数量
        ushort startAddr = addresses[0];
        ushort endAddr = addresses[addresses.Count - 1];
        ushort count = (ushort)(endAddr - startAddr + 1);

        // 一次性读取整个范围
        ushort[] allValues = _master.ReadHoldingRegisters(
            _slaveAddress, startAddr, count);

        // 提取需要的地址
        var result = new Dictionary<ushort, ushort>();
        foreach (var addr in addresses)
        {
            int index = addr - startAddr;
            result[addr] = allValues[index];
        }

        return result;
    }

    /// <summary>
    /// 优化策略2：按设备区域分批读取
    /// 例如：设备A的寄存器在0-99，设备B的寄存器在100-199
    /// 分成两批读取，每批读100个寄存器
    /// </summary>
    public void ReadByGroups()
    {
        // 第一批：读寄存器0-49（温度区域）
        ushort[] tempData = _master.ReadHoldingRegisters(
            _slaveAddress, 0, 50);

        // 第二批：读寄存器50-99（压力区域）
        ushort[] pressureData = _master.ReadHoldingRegisters(
            _slaveAddress, 50, 50);

        // 第三批：读寄存器100-149（状态区域）
        ushort[] statusData = _master.ReadHoldingRegisters(
            _slaveAddress, 100, 50);
    }

    /// <summary>
    /// 优化策略3：读写分离，先读后写
    /// 将所有读取操作集中执行，然后再执行所有写入操作
    /// 避免读和写交替进行导致的总线冲突
    /// </summary>
    public void ReadThenWrite()
    {
        // === 阶段1：集中读取所有需要的数据 ===
        var tempValues = _master.ReadHoldingRegisters(_slaveAddress, 0, 10);
        var pressureValues = _master.ReadHoldingRegisters(_slaveAddress, 100, 5);

        // === 阶段2：处理数据，计算控制量 ===
        ushort controlValue = CalculateControl(tempValues, pressureValues);

        // === 阶段3：集中写入控制量 ===
        _master.WriteSingleRegister(_slaveAddress, 200, controlValue);
    }

    private ushort CalculateControl(ushort[] temp, ushort[] pressure)
    {
        // 简单示例：取温度平均值
        uint sum = 0;
        foreach (var v in temp) sum += v;
        return (ushort)(sum / temp.Length);
    }
}
```

### 2.9 完整封装示例：ModbusCommunicationService 类

```csharp
using System;
using System.Collections.Generic;
using System.Net.Sockets;
using System.Threading;
using System.Threading.Tasks;
using NModbus;

/// <summary>
/// Modbus通信服务完整封装类
/// 支持TCP和RTU两种模式
/// 支持自动重连、超时处理、数据类型转换
/// 适用于上位机数据采集项目
/// </summary>
public class ModbusCommunicationService : IDisposable
{
    // ========== 枚举定义 ==========
    public enum ConnectionType { Tcp, Rtu }

    // ========== 私有字段 ==========
    private IModbusMaster _master;
    private TcpClient _tcpClient;
    private SerialPortWrapper _serialPort;
    private ModbusFactory _factory;
    private readonly ConnectionType _connType;
    private readonly object _lock = new object();

    // 连接参数
    private readonly string _ip;
    private readonly int _port;
    private readonly string _portName;
    private readonly int _baudRate;

    // 统计
    private int _successCount;
    private int _failCount;

    // ========== 事件 ==========
    /// <summary>通信成功事件</summary>
    public event Action<string> OnLog;
    /// <summary>通信失败事件</summary>
    public event Action<string> OnError;
    /// <summary>连接状态变化事件</summary>
    public event Action<bool> OnConnectionChanged;

    // ========== 属性 ==========
    public bool IsConnected { get; private set; }
    public byte SlaveAddress { get; set; } = 1;
    public int ReadTimeout { get; set; } = 3000;
    public int WriteTimeout { get; set; } = 3000;
    public int RetryCount { get; set; } = 3;

    // ========== 构造函数 ==========

    /// <summary>
    /// TCP模式构造函数
    /// </summary>
    public ModbusCommunicationService(string ip, int port = 502)
    {
        _connType = ConnectionType.Tcp;
        _ip = ip;
        _port = port;
    }

    /// <summary>
    /// RTU模式构造函数
    /// </summary>
    public ModbusCommunicationService(string portName, int baudRate)
    {
        _connType = ConnectionType.Rtu;
        _portName = portName;
        _baudRate = baudRate;
    }

    // ========== 连接管理 ==========

    /// <summary>
    /// 建立Modbus连接
    /// </summary>
    public bool Connect()
    {
        lock (_lock)
        {
            try
            {
                Disconnect();  // 先断开旧连接

                _factory = new ModbusFactory();

                if (_connType == ConnectionType.Tcp)
                {
                    _tcpClient = new TcpClient();
                    _tcpClient.Connect(_ip, _port);
                    _master = _factory.CreateMaster(_tcpClient);
                    Log($"[TCP] 已连接到 {_ip}:{_port}");
                }
                else
                {
                    _serialPort = new SerialPortWrapper(
                        _portName, _baudRate, Parity.None, 8, StopBits.One);
                    _serialPort.Open();
                    _master = _factory.CreateRtuMaster(_serialPort.SerialPort);
                    Log($"[RTU] 已打开串口 {_portName} ({_baudRate})");
                }

                // 配置传输参数
                _master.Transport.ReadTimeout = ReadTimeout;
                _master.Transport.WriteTimeout = WriteTimeout;
                _master.Transport.Retries = 0;  // 我们自己实现重试逻辑
                _master.Transport.WaitToRetryMilliseconds = 100;

                IsConnected = true;
                OnConnectionChanged?.Invoke(true);
                return true;
            }
            catch (Exception ex)
            {
                IsConnected = false;
                OnError?.Invoke($"连接失败: {ex.Message}");
                OnConnectionChanged?.Invoke(false);
                return false;
            }
        }
    }

    /// <summary>
    /// 断开连接
    /// </summary>
    public void Disconnect()
    {
        lock (_lock)
        {
            try
            {
                _master?.Dispose();
                _master = null;
                _tcpClient?.Close();
                _tcpClient = null;
                _serialPort?.Close();
                _serialPort = null;
            }
            catch { }

            IsConnected = false;
            OnConnectionChanged?.Invoke(false);
        }
    }

    // ========== 读操作 ==========

    /// <summary>
    /// 读保持寄存器（功能码03），带重试和异常处理
    /// </summary>
    public ushort[] ReadHoldingRegisters(ushort startAddress, ushort count)
    {
        return ExecuteWithRetry(() =>
            _master.ReadHoldingRegisters(SlaveAddress, startAddress, count));
    }

    /// <summary>
    /// 读输入寄存器（功能码04），带重试和异常处理
    /// </summary>
    public ushort[] ReadInputRegisters(ushort startAddress, ushort count)
    {
        return ExecuteWithRetry(() =>
            _master.ReadInputRegisters(SlaveAddress, startAddress, count));
    }

    /// <summary>
    /// 读线圈（功能码01），带重试和异常处理
    /// </summary>
    public bool[] ReadCoils(ushort startAddress, ushort count)
    {
        return ExecuteWithRetry(() =>
            _master.ReadCoils(SlaveAddress, startAddress, count));
    }

    /// <summary>
    /// 读离散输入（功能码02），带重试和异常处理
    /// </summary>
    public bool[] ReadDiscreteInputs(ushort startAddress, ushort count)
    {
        return ExecuteWithRetry(() =>
            _master.ReadInputs(SlaveAddress, startAddress, count));
    }

    // ========== 写操作 ==========

    /// <summary>
    /// 写单个寄存器（功能码06）
    /// </summary>
    public bool WriteSingleRegister(ushort address, ushort value)
    {
        return ExecuteWithRetry(() =>
        {
            _master.WriteSingleRegister(SlaveAddress, address, value);
            return true;
        });
    }

    /// <summary>
    /// 写多个寄存器（功能码16）
    /// </summary>
    public bool WriteMultipleRegisters(ushort startAddress, ushort[] values)
    {
        return ExecuteWithRetry(() =>
        {
            _master.WriteMultipleRegisters(SlaveAddress, startAddress, values);
            return true;
        });
    }

    /// <summary>
    /// 写单个线圈（功能码05）
    /// </summary>
    public bool WriteSingleCoil(ushort address, bool value)
    {
        return ExecuteWithRetry(() =>
        {
            _master.WriteSingleCoil(SlaveAddress, address, value);
            return true;
        });
    }

    /// <summary>
    /// 写多个线圈（功能码15）
    /// </summary>
    public bool WriteMultipleCoils(ushort startAddress, bool[] values)
    {
        return ExecuteWithRetry(() =>
        {
            _master.WriteMultipleCoils(SlaveAddress, startAddress, values);
            return true;
        });
    }

    // ========== 数据类型转换 ==========

    /// <summary>
    /// 读取一个32位有符号整数（占2个寄存器）
    /// </summary>
    public int ReadInt32(ushort address)
    {
        var regs = ReadHoldingRegisters(address, 2);
        if (regs == null) return 0;
        return (regs[0] << 16) | regs[1];
    }

    /// <summary>
    /// 读取一个32位浮点数（占2个寄存器）
    /// </summary>
    public float ReadFloat(ushort address)
    {
        var regs = ReadHoldingRegisters(address, 2);
        if (regs == null) return 0f;

        byte[] bytes = new byte[4];
        bytes[0] = (byte)(regs[0] >> 8);
        bytes[1] = (byte)(regs[0] & 0xFF);
        bytes[2] = (byte)(regs[1] >> 8);
        bytes[3] = (byte)(regs[1] & 0xFF);

        if (BitConverter.IsLittleEndian)
            Array.Reverse(bytes);
        return BitConverter.ToSingle(bytes, 0);
    }

    /// <summary>
    /// 读取缩放后的实际值（寄存器值 * 比例系数）
    /// </summary>
    public double ReadScaledValue(ushort address, double scale)
    {
        var regs = ReadHoldingRegisters(address, 1);
        if (regs == null) return 0;
        return regs[0] * scale;
    }

    // ========== 内部方法 ==========

    /// <summary>
    /// 带重试的执行方法（核心方法）
    /// </summary>
    private T ExecuteWithRetry<T>(Func<T> action)
    {
        for (int i = 0; i < RetryCount; i++)
        {
            try
            {
                if (!IsConnected)
                {
                    OnError?.Invoke("未连接，尝试重连...");
                    if (!Connect()) continue;
                }

                T result = action();
                _successCount++;
                return result;
            }
            catch (TimeoutException ex)
            {
                OnError?.Invoke($"[超时] 第{i + 1}/{RetryCount}次: {ex.Message}");
            }
            catch (IOException ex)
            {
                OnError?.Invoke($"[IO异常] 第{i + 1}/{RetryCount}次: {ex.Message}");
                // IO异常可能连接已断开，尝试重连
                IsConnected = false;
                Connect();
            }
            catch (SlaveException ex)
            {
                _failCount++;
                OnError?.Invoke($"[从站异常] 功能码:0x{ex.FunctionCode:X2} " +
                                $"异常码:0x{ex.SlaveExceptionCode:X2}");
                return default;
            }
            catch (Exception ex)
            {
                OnError?.Invoke($"[异常] 第{i + 1}/{RetryCount}次: {ex.Message}");
            }

            Thread.Sleep(200);  // 重试间隔
        }

        _failCount++;
        return default;
    }

    private void Log(string message)
    {
        OnLog?.Invoke(message);
    }

    // ========== 使用示例 ==========

    public static void ExampleUsage()
    {
        // 创建TCP连接的服务
        using var service = new ModbusCommunicationService("192.168.1.100", 502);
        service.SlaveAddress = 1;
        service.ReadTimeout = 2000;
        service.RetryCount = 3;
        service.OnLog = msg => Console.WriteLine($"[日志] {msg}");
        service.OnError = msg => Console.WriteLine($"[错误] {msg}");

        // 连接
        if (!service.Connect())
        {
            Console.WriteLine("连接失败");
            return;
        }

        // 读取10个保持寄存器
        ushort[] values = service.ReadHoldingRegisters(0, 10);
        if (values != null)
        {
            for (int i = 0; i < values.Length; i++)
            {
                Console.WriteLine($"寄存器[{i}] = {values[i]}");
            }
        }

        // 读取32位浮点数（温度传感器）
        float temperature = service.ReadFloat(100);
        Console.WriteLine($"温度: {temperature:F1} C");

        // 读取缩放值（0.01精度）
        double pressure = service.ReadScaledValue(102, 0.01);
        Console.WriteLine($"压力: {pressure:F2} MPa");

        // 写单个寄存器
        service.WriteSingleRegister(200, 1000);

        // 写多个寄存器
        service.WriteMultipleRegisters(200, new ushort[] { 100, 200, 300 });

        // 断开连接
        service.Disconnect();
    }

    public void Dispose()
    {
        Disconnect();
    }
}

/// <summary>
/// 串口包装类（简化串口管理）
/// </summary>
internal class SerialPortWrapper
{
    public SerialPort SerialPort { get; }
    public SerialPortWrapper(string port, int baud, Parity parity, int dataBits, StopBits stopBits)
    {
        SerialPort = new SerialPort(port, baud, parity, dataBits, stopBits)
        {
            ReadTimeout = 3000,
            WriteTimeout = 3000
        };
    }
    public void Open() { SerialPort.Open(); }
    public void Close() { if (SerialPort.IsOpen) SerialPort.Close(); }
}
```

---

## 三、注意事项

1. **线程安全**：NModbus 的 `IModbusMaster` 不是线程安全的。如果多个线程同时读写，需要加锁。
2. **NModbus4 和 NModbus5 的API几乎一致**，只是包名不同，迁移成本低。
3. **SlaveException** 表示从站正常响应了，但返回了异常码（如地址不存在），这种情况重试通常无意义，应直接处理。
4. **串口模式**下，`SerialPort` 必须保持打开状态，如果被其他程序占用，Modbus通信会失败。
5. **RTU模式**的串口波特率必须与设备一致，且 RTU 通信不需要计算 CRC（NModbus 自动处理）。
6. **连接释放**：使用完 `IModbusMaster` 后必须调用 `Dispose()`，否则会泄漏 TCP 连接或串口资源。

---

## 四、练习建议

### 练习1：Modbus TCP 通信测试程序
- 创建WinForms/WPF界面
- 配置IP、端口、从站地址
- 选择功能码，输入地址和数量
- 发送请求并显示结果
- 显示原始帧（发送/接收）用于调试

### 练习2：自动轮询采集系统
- 使用 `System.Timers.Timer` 或 `async/await` 实现定时轮询
- 每500ms读取一组寄存器
- 将数据显示在表格或图表中
- 统计通信成功率和延迟

### 练习3：多设备采集系统
- 使用 `ModbusCommunicationService` 封装类
- 同时管理多个设备（不同IP/从站地址）
- 使用 `Task.WhenAll` 并行采集
- 实现断线自动重连

---

## 五、常见错误

### 错误1：`SlaveException: Function Code 3, Exception Code 2`
```
含义：从站不支持请求的寄存器地址
```
**解决**：检查寄存器地址是否正确，查阅设备手册确认地址映射表。

### 错误2：`TimeoutException`
```
含义：请求在指定时间内未收到响应
```
**原因**：网络不通、设备离线、从站地址错误、波特率不匹配（RTU模式）。
**解决**：检查网络连接、设备电源、通信参数配置。

### 错误3：`IOException: Unable to read data from the transport connection`
```
含义：TCP连接已断开
```
**原因**：设备重启、网线断开、设备端主动关闭连接。
**解决**：捕获异常后自动重连。

### 错误4：NuGet包安装后找不到 NModbus 命名空间
```
原因：可能安装了错误的包名
```
**解决**：
```bash
# 确认安装了正确的包
dotnet remove package NModbus
dotnet add package NModbus4
# 或
dotnet add package NModbus5
```

### 错误5：`InvalidOperationException: 已有打开的串口`
```
原因：串口被之前的连接未释放占用
```
**解决**：确保调用 `Disconnect()` 和 `Dispose()` 释放资源。

### 错误6：读取浮点数值不对
```
现象：读取到的浮点数明显不合理（如 NaN 或极大值）
```
**原因**：字节顺序与设备不一致。不同设备可能使用不同的浮点数字节顺序（AB/CD/BA/DC）。
**解决**：查阅设备手册确认字节顺序，必要时交换高低字或反转字节。
