# 05 - HslCommunication库实战

---

## 一、知识讲解

### 1.1 HslCommunication 简介

HslCommunication 是国内开发者维护的一个功能强大的工业通信库，支持多种工业协议（Modbus TCP/RTU/ASCII、西门子、欧姆龙、三菱、汇川等），API 简洁易用，是国内上位机开发中使用率极高的通信库之一。

#### 主要优势

- **多协议支持**：Modbus、西门子S7、三菱、欧姆龙、汇川、台达、变频器等几十种协议
- **API简洁**：一行代码即可读写数据，无需手动处理字节和帧
- **自动类型转换**：支持直接读取 Int16、Int32、Float、Double、String 等类型
- **批量操作**：原生支持批量读写，减少通信次数
- **订阅机制**：支持数据订阅，自动定时轮询并更新数据
- **日志调试**：内置日志系统，方便调试
- **国产文档**：完整的中文文档和示例
- **活跃维护**：持续更新，适配最新 .NET 版本

#### 安装

```bash
# 标准安装
dotnet add package HslCommunication

# 如果需要串口功能
dotnet add package System.IO.Ports

# 如果需要高级功能（如数据可视化组件）
dotnet add package HslCommunication.Extend
```

#### 与 NModbus 的对比

| 特性 | HslCommunication | NModbus |
|------|------------------|---------|
| 支持协议 | Modbus + 西门子 + 三菱 + 欧姆龙 + 汇川等 | 仅 Modbus |
| API风格 | 直接读写（ReadInt16/WriteFloat） | 标准Modbus方法（ReadHoldingRegisters） |
| 数据类型 | 原生支持多种数据类型转换 | 返回ushort，需手动转换 |
| 批量读写 | 支持原生批量 | 需要自行管理地址范围 |
| 订阅机制 | 内置数据订阅 | 无 |
| 中文文档 | 完善 | 较少 |
| 社区活跃度 | 国内活跃 | 国际社区 |

#### 选型建议

- **选 HslCommunication**：项目涉及多种品牌PLC、需要快速开发、需要数据订阅、团队更熟悉中文文档
- **选 NModbus**：仅使用Modbus协议、需要更轻量级的库、国际化项目、对库体积敏感

---

## 二、代码示例

### 2.1 ModbusTcpNet 创建与配置

```csharp
using HslCommunication.ModBus;

/// <summary>
/// HslCommunication 创建Modbus TCP客户端
/// 最简方式：只需要IP地址即可创建
/// </summary>
public class HslModbusTcpSetup
{
    private ModbusTcpNet _modbus;

    /// <summary>
    /// 基本创建方式
    /// </summary>
    public void CreateBasic()
    {
        // 创建ModbusTcpNet客户端，指定PLC的IP地址
        // 默认端口502，从站地址1
        _modbus = new ModbusTcpNet("192.168.1.100");

        // 配置参数（可选）
        _modbus.ConnectTimeout = 5000;     // 连接超时（毫秒）
        _modbus.ReceiveTimeout = 3000;     // 接收超时（毫秒）
        _modbus.SendTimeout = 3000;        // 发送超时（毫秒）
        _modbus.RetryTimes = 3;            // 失败重试次数
        _modbus.IsAutoReconnect = true;    // 自动重连（推荐开启！）
        _modbus.AutoReconnectInterval = 5000;  // 自动重连间隔（毫秒）
    }

    /// <summary>
    /// 带完整参数的创建方式
    /// </summary>
    public void CreateWithParameters(string ip, int port, byte slaveId)
    {
        _modbus = new ModbusTcpNet(ip, port, slaveId)
        {
            ConnectTimeout = 5000,
            ReceiveTimeout = 3000,
            SendTimeout = 3000,
            RetryTimes = 3,
            IsAutoReconnect = true
        };
    }

    /// <summary>
    /// 开启日志记录（调试时非常有用）
    /// </summary>
    public void EnableLog()
    {
        // 设置日志输出到控制台
        _modbus.LogNet = new HslCommunication.LogNet.LogNetSingle("modbus_log.txt");
        _modbus.LogNet.BeforeSaveToFile += (sender, e) =>
        {
            Console.WriteLine($"[Modbus日志] {e.Message}");
        };

        // 或者输出到控制台
        _modbus.LogNet = new HslCommunication.LogNet.LogNetConsole();
    }

    /// <summary>
    /// 连接并检查状态
    /// </summary>
    public bool Connect()
    {
        var result = _modbus.ConnectServer();
        if (result.IsSuccess)
        {
            Console.WriteLine("连接成功");
            return true;
        }
        Console.WriteLine($"连接失败: {result.Message}");
        return false;
    }

    public void Disconnect()
    {
        _modbus.ConnectClose();
    }
}
```

### 2.2 读写操作

```csharp
using HslCommunication.ModBus;

/// <summary>
/// HslCommunication 读写操作
/// API非常简洁，一行代码完成读写
/// </summary>
public class HslModbusReadWrite
{
    private ModbusTcpNet _modbus;

    // ===== 读操作 =====

    /// <summary>
    /// 读取Int16（16位有符号整数，1个寄存器）
    /// 最常用的读取方式，大多数PLC数据都是16位
    /// </summary>
    public short ReadInt16(ushort address)
    {
        var result = _modbus.ReadInt16(address);
        if (result.IsSuccess)
        {
            Console.WriteLine($"寄存器[{address}] = {result.Content}");
            return result.Content;
        }
        Console.WriteLine($"读取失败: {result.Message}");
        return 0;
    }

    /// <summary>
    /// 读取UInt16（16位无符号整数，1个寄存器）
    /// </summary>
    public ushort ReadUInt16(ushort address)
    {
        var result = _modbus.ReadUInt16(address);
        if (result.IsSuccess)
            return result.Content;
        return 0;
    }

    /// <summary>
    /// 读取Int32（32位有符号整数，2个寄存器）
    /// 自动处理两个寄存器的组合
    /// </summary>
    public int ReadInt32(ushort address)
    {
        var result = _modbus.ReadInt32(address);
        if (result.IsSuccess)
        {
            Console.WriteLine($"寄存器[{address}] 32位值 = {result.Content}");
            return result.Content;
        }
        return 0;
    }

    /// <summary>
    /// 读取Float（32位浮点数，2个寄存器）
    /// 工业传感器数据（温度、压力等）通常用浮点数表示
    /// </summary>
    public float ReadFloat(ushort address)
    {
        var result = _modbus.ReadFloat(address);
        if (result.IsSuccess)
        {
            Console.WriteLine($"寄存器[{address}] 浮点值 = {result.Content:F2}");
            return result.Content;
        }
        return 0f;
    }

    /// <summary>
    /// 读取Double（64位双精度，4个寄存器）
    /// </summary>
    public double ReadDouble(ushort address)
    {
        var result = _modbus.ReadDouble(address);
        if (result.IsSuccess) return result.Content;
        return 0.0;
    }

    /// <summary>
    /// 读取Bool值（1个位，对应线圈或寄存器的某一位）
    /// </summary>
    public bool ReadBool(ushort address)
    {
        var result = _modbus.ReadBool(address);
        if (result.IsSuccess) return result.Content;
        return false;
    }

    /// <summary>
    /// 读取寄存器的某一位
    /// 例如读取寄存器100的第3位
    /// </summary>
    public bool ReadBit(ushort address, byte bitIndex)
    {
        var result = _modbus.ReadBool(address, bitIndex);
        if (result.IsSuccess) return result.Content;
        return false;
    }

    // ===== 写操作 =====

    /// <summary>
    /// 写入单个寄存器值（Int16）
    /// </summary>
    public bool WriteInt16(ushort address, short value)
    {
        var result = _modbus.WriteRegister(address, value);
        if (result.IsSuccess)
        {
            Console.WriteLine($"写入成功: 寄存器[{address}] = {value}");
            return true;
        }
        Console.WriteLine($"写入失败: {result.Message}");
        return false;
    }

    /// <summary>
    /// 写入Float值（占2个寄存器）
    /// </summary>
    public bool WriteFloat(ushort address, float value)
    {
        var result = _modbus.WriteFloat(address, value);
        if (result.IsSuccess)
        {
            Console.WriteLine($"写入Float成功: [{address}] = {value:F2}");
            return true;
        }
        return false;
    }

    /// <summary>
    /// 写入Int32值（占2个寄存器）
    /// </summary>
    public bool WriteInt32(ushort address, int value)
    {
        var result = _modbus.WriteInt32(address, value);
        return result.IsSuccess;
    }

    /// <summary>
    /// 写入线圈/Bool值
    /// </summary>
    public bool WriteBool(ushort address, bool value)
    {
        var result = _modbus.WriteCoil(address, value);
        return result.IsSuccess;
    }
}
```

### 2.3 批量读写

```csharp
using HslCommunication.ModBus;

/// <summary>
/// 批量读写操作
/// 一次性读取/写入多个寄存器，大幅提高效率
/// </summary>
public class HslModbusBatchOperations
{
    private ModbusTcpNet _modbus;

    /// <summary>
    /// 批量读取Int16 —— 一次性读取多个连续地址的Int16值
    /// 例如：从地址0开始，读取10个Int16
    /// </summary>
    public void ReadInt16Batch()
    {
        // 读取地址0开始的10个Int16值
        var result = _modbus.ReadInt16("0", 10);
        if (result.IsSuccess)
        {
            for (int i = 0; i < result.Content.Length; i++)
            {
                Console.WriteLine($"  寄存器[{i}] = {result.Content[i]}");
            }
        }
    }

    /// <summary>
    /// 批量读取Float —— 一次读取多个浮点数
    /// 每个Float占2个寄存器，读10个Float需要20个寄存器
    /// </summary>
    public void ReadFloatBatch()
    {
        // 从地址100开始，读取5个Float值（占用寄存器100-109）
        var result = _modbus.ReadFloat("100", 5);
        if (result.IsSuccess)
        {
            for (int i = 0; i < result.Content.Length; i++)
            {
                Console.WriteLine($"  传感器[{i}] = {result.Content[i]:F2}");
            }
        }
    }

    /// <summary>
    /// 按地址数组批量读取
    /// 读取不连续的多个地址
    /// </summary>
    public void ReadDisjointAddresses()
    {
        // HslCommunication支持一次性读取多个不连续地址
        var result = _modbus.ReadInt16("0,10,20,50,100");
        if (result.IsSuccess)
        {
            Console.WriteLine($"读取5个不连续地址: " +
                string.Join(", ", result.Content));
        }
    }

    /// <summary>
    /// 批量写入Int16
    /// </summary>
    public bool WriteInt16Batch(ushort startAddress, short[] values)
    {
        var result = _modbus.WriteRegister(startAddress, values);
        return result.IsSuccess;
    }

    /// <summary>
    /// 批量写入Float
    /// </summary>
    public bool WriteFloatBatch(ushort startAddress, float[] values)
    {
        var result = _modbus.WriteFloat(startAddress, values);
        return result.IsSuccess;
    }
}
```

### 2.4 字符串读写

```csharp
using HslCommunication.ModBus;
using System.Text;

/// <summary>
/// 字符串读写操作
/// 适用于设备ID、配方名称、报警消息等文本数据
/// </summary>
public class HslModbusStringOperations
{
    private ModbusTcpNet _modbus;

    /// <summary>
    /// 读取字符串
    /// 从指定地址读取固定长度的ASCII字符串
    /// </summary>
    public string ReadString(ushort address, int length)
    {
        // 读取ASCII编码的字符串，指定长度
        var result = _modbus.ReadString(address, length, Encoding.ASCII);
        if (result.IsSuccess)
        {
            Console.WriteLine($"读取字符串: \"{result.Content}\"");
            return result.Content;
        }
        return string.Empty;
    }

    /// <summary>
    /// 读取Unicode字符串（UTF-8）
    /// </summary>
    public string ReadUtf8String(ushort address, int length)
    {
        var result = _modbus.ReadString(address, length, Encoding.UTF8);
        if (result.IsSuccess) return result.Content;
        return string.Empty;
    }

    /// <summary>
    /// 写入字符串
    /// </summary>
    public bool WriteString(ushort address, string text)
    {
        // 写入ASCII字符串
        var result = _modbus.WriteString(address, text, Encoding.ASCII);
        if (result.IsSuccess)
        {
            Console.WriteLine($"字符串写入成功");
            return true;
        }
        return false;
    }
}
```

### 2.5 订阅式数据更新

```csharp
using System;
using System.Threading;
using HslCommunication.ModBus;

/// <summary>
/// 订阅式数据更新
/// HslCommunication内置了数据订阅机制，自动定时轮询并回调数据
/// 无需手动写Timer，非常方便
/// </summary>
public class HslModbusSubscription
{
    private ModbusTcpNet _modbus;

    /// <summary>
    /// 启动数据订阅（核心功能）
    /// 自动以指定间隔轮询数据并触发回调
    /// </summary>
    public void StartSubscription()
    {
        // 方式1：订阅单个地址
        int addressHandle = _modbus.Subscribe(
            "100",           // 订阅的地址（字符串格式）
            1000,            // 轮询间隔（毫秒）
            (sender, e) =>   // 数据回调
            {
                // e.Value 是读取到的原始 ushort 值
                Console.WriteLine($"[订阅] 地址100 = {e.Value}");
            });

        // 方式2：订阅Float值（自动类型转换）
        int floatHandle = _modbus.SubscribeFloat(
            "200",           // 地址
            500,             // 500ms轮询一次
            (sender, e) =>
            {
                Console.WriteLine($"[订阅] 温度 = {e.Content:F1} C");
            });

        // 方式3：订阅Int32值
        int int32Handle = _modbus.SubscribeInt32(
            "300",           // 地址
            1000,
            (sender, e) =>
            {
                Console.WriteLine($"[订阅] 计数器 = {e.Content}");
            });

        Console.WriteLine("订阅已启动，按任意键停止...");
        Console.ReadKey();

        // 停止订阅
        _modbus.Unsubscribe(addressHandle);
        _modbus.Unsubscribe(floatHandle);
        _modbus.Unsubscribe(int32Handle);
    }

    /// <summary>
    /// 批量订阅（同时订阅多个地址）
    /// </summary>
    public void StartBatchSubscription()
    {
        // 订阅多个地址的Int16值
        int[] handles = _modbus.Subscribe(
            new string[] { "0", "1", "2", "3", "4" },  // 5个地址
            1000,   // 1000ms轮询
            (sender, e) =>
            {
                // e.Address 是触发订阅的地址
                // e.Value 是读取到的值
                Console.WriteLine($"[批量订阅] 地址{e.Address} = {e.Value}");
            });

        // 保存handles以便后续取消订阅
    }
}
```

### 2.6 日志记录与调试

```csharp
using HslCommunication.LogNet;

/// <summary>
/// HslCommunication 日志系统
/// 内置完善的日志记录功能，调试通信问题时非常有用
/// </summary>
public class HslModbusLogging
{
    private ModbusTcpNet _modbus;

    public void SetupLogging()
    {
        // ===== 方式1：控制台日志（适合控制台程序调试）=====
        _modbus.LogNet = new LogNetConsole();

        // ===== 方式2：文件日志（保存到文件）=====
        // 每天一个日志文件，自动管理
        _modbus.LogNet = new LogNetSingle("modbus_communication.log");

        // ===== 方式3：自定义日志处理（最灵活）=====
        _modbus.LogNet = new LogNetLogNet(
            msg => Console.WriteLine($"[自定义日志] {msg}")
        );

        // ===== 方式4：设置日志级别 =====
        _modbus.LogNet.LogNetLogLevel = HslCommunication.LogNet.LogNetLogLevel.Info;
        // Debug：最详细，包含发送/接收的原始字节
        // Info：常规信息
        // Warn：警告
        // Error：仅错误

        // ===== 方式5：拦截日志内容 =====
        _modbus.LogNet.BeforeSaveToFile += (sender, e) =>
        {
            // 可以在这里做日志转发（如发送到UI界面）
            // 注意：不要在此处做耗时操作
            if (e.LogLevel == LogNetLogLevel.Debug)
            {
                // 原始帧数据
                Console.WriteLine(e.Message);
            }
        };
    }
}
```

### 2.7 完整封装示例

```csharp
using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using HslCommunication;
using HslCommunication.ModBus;

/// <summary>
/// 基于HslCommunication的完整通信服务封装
/// 支持Modbus TCP，带自动重连、数据订阅、异常处理
/// 适用于上位机数据采集项目
/// </summary>
public class HslModbusDataService : IDisposable
{
    // ========== 私有字段 ==========
    private ModbusTcpNet _modbus;
    private Timer _pollingTimer;
    private bool _isRunning;

    // ========== 配置 ==========
    public string IpAddress { get; set; } = "127.0.0.1";
    public int Port { get; set; } = 502;
    public byte SlaveId { get; set; } = 1;
    public int PollingInterval { get; set; } = 1000;  // 轮询间隔（ms）

    // ========== 事件 ==========
    public event Action<string> OnLog;
    public event Action<string> OnError;
    public event Action<bool> OnConnectionChanged;
    public event Action<Dictionary<string, object>> OnDataUpdated;

    // ========== 状态 ==========
    public bool IsConnected => _modbus?.IsConnected ?? false;

    /// <summary>
    /// 构造函数：初始化Modbus客户端
    /// </summary>
    public HslModbusDataService(string ip, int port = 502, byte slaveId = 1)
    {
        IpAddress = ip;
        Port = port;
        SlaveId = slaveId;

        // 创建ModbusTcpNet实例
        _modbus = new ModbusTcpNet(ip, port, slaveId)
        {
            ConnectTimeout = 5000,
            ReceiveTimeout = 3000,
            SendTimeout = 3000,
            RetryTimes = 3,
            IsAutoReconnect = true,  // 核心功能：自动重连！
            AutoReconnectInterval = 5000
        };

        // 配置日志
        _modbus.LogNet = new HslCommunication.LogNet.LogNetSingle(
            $"modbus_{slaveId}.log");
        _modbus.LogNet.BeforeSaveToFile += (s, e) =>
        {
            Log(e.Message);
        };
    }

    /// <summary>
    /// 连接服务器
    /// </summary>
    public OperateResult Connect()
    {
        var result = _modbus.ConnectServer();
        OnConnectionChanged?.Invoke(result.IsSuccess);
        if (result.IsSuccess)
            Log($"已连接到 {IpAddress}:{Port} (从站{SlaveId})");
        else
            OnError?.Invoke($"连接失败: {result.Message}");
        return result;
    }

    /// <summary>
    /// 断开连接
    /// </summary>
    public void Disconnect()
    {
        StopPolling();
        _modbus.ConnectClose();
        OnConnectionChanged?.Invoke(false);
    }

    // ========== 便捷读写方法 ==========

    /// <summary>
    /// 读取Int16
    /// </summary>
    public short ReadInt16(ushort address)
    {
        var result = _modbus.ReadInt16(address);
        if (!result.IsSuccess) OnError?.Invoke($"读Int16[{address}]失败: {result.Message}");
        return result.Content;
    }

    /// <summary>
    /// 读取Float
    /// </summary>
    public float ReadFloat(ushort address)
    {
        var result = _modbus.ReadFloat(address);
        if (!result.IsSuccess) OnError?.Invoke($"读Float[{address}]失败: {result.Message}");
        return result.Content;
    }

    /// <summary>
    /// 批量读取Int16
    /// </summary>
    public short[] ReadInt16Batch(ushort startAddress, ushort count)
    {
        var result = _modbus.ReadInt16(startAddress.ToString(), count);
        if (!result.IsSuccess) OnError?.Invoke($"批量读失败: {result.Message}");
        return result.Content;
    }

    /// <summary>
    /// 写入Int16
    /// </summary>
    public bool WriteInt16(ushort address, short value)
    {
        var result = _modbus.WriteRegister(address, value);
        if (!result.IsSuccess) OnError?.Invoke($"写Int16[{address}]失败: {result.Message}");
        return result.IsSuccess;
    }

    /// <summary>
    /// 写入Float
    /// </summary>
    public bool WriteFloat(ushort address, float value)
    {
        var result = _modbus.WriteFloat(address, value);
        if (!result.IsSuccess) OnError?.Invoke($"写Float[{address}]失败: {result.Message}");
        return result.IsSuccess;
    }

    // ========== 自动轮询采集 ==========

    /// <summary>
    /// 启动自动轮询采集
    /// 使用Timer定时读取数据，并通过事件返回
    /// </summary>
    /// <param name="addresses">需要轮询的地址列表</param>
    public void StartPolling(ushort[] addresses)
    {
        if (_isRunning) return;

        _isRunning = true;

        // 使用HslCommunication内置订阅功能
        string[] addressStr = Array.ConvertAll(addresses, a => a.ToString());
        _modbus.Subscribe(addressStr, PollingInterval, (sender, e) =>
        {
            if (OnDataUpdated != null)
            {
                var data = new Dictionary<string, object>
                {
                    { e.Address, e.Value }
                };
                OnDataUpdated?.Invoke(data);
            }
        });

        Log($"自动轮询已启动，间隔{PollingInterval}ms，" +
            $"监控{addresses.Length}个地址");
    }

    /// <summary>
    /// 启动Float数据订阅
    /// </summary>
    public void StartFloatPolling(ushort[] addresses)
    {
        foreach (var addr in addresses)
        {
            _modbus.SubscribeFloat(addr.ToString(), PollingInterval, (sender, e) =>
            {
                var data = new Dictionary<string, object>
                {
                    { addr.ToString(), e.Content }
                };
                OnDataUpdated?.Invoke(data);
            });
        }
    }

    /// <summary>
    /// 停止轮询
    /// </summary>
    public void StopPolling()
    {
        _modbus.UnsubscribeAll();
        _isRunning = false;
        Log("自动轮询已停止");
    }

    // ========== 使用示例 ==========

    public static void Example()
    {
        using var service = new HslModbusDataService("192.168.1.100", 502, 1);
        service.OnLog = msg => Console.WriteLine($"[LOG] {msg}");
        service.OnError = msg => Console.WriteLine($"[ERR] {msg}");

        // 连接
        if (!service.Connect().IsSuccess)
        {
            Console.WriteLine("连接失败，但设置了自动重连，程序将继续运行");
        }

        // 读取单个值
        short tempReg = service.ReadInt16(0);
        float temperature = service.ReadFloat(100);
        Console.WriteLine($"温度: {temperature:F1} C");

        // 批量读取
        short[] values = service.ReadInt16Batch(0, 10);

        // 写入值
        service.WriteInt16(200, 1000);
        service.WriteFloat(202, 25.5f);

        // 启动自动轮询
        service.OnDataUpdated = (data) =>
        {
            foreach (var kv in data)
            {
                Console.WriteLine($"  地址{kv.Key} = {kv.Value}");
            }
        };

        service.StartPolling(new ushort[] { 0, 1, 2, 100, 101 });

        Console.WriteLine("按任意键退出...");
        Console.ReadKey();

        service.Disconnect();
    }

    private void Log(string message)
    {
        OnLog?.Invoke($"[{DateTime.Now:HH:mm:ss.fff}] {message}");
    }

    public void Dispose()
    {
        Disconnect();
    }
}
```

---

## 三、注意事项

1. **IsAutoReconnect = true**：强烈建议开启自动重连。工业网络不稳定，设备可能因断电、重启等原因短暂离线。
2. **OperateResult 模式**：HslCommunication 的所有读写方法返回 `OperateResult<T>`，务必检查 `IsSuccess` 后再使用 `Content`。
3. **地址格式**：HslCommunication 支持字符串格式的地址（如 `"0"`, `"100"`, `"D0"`），也支持直接传数值。批量操作时推荐字符串格式。
4. **字节顺序**：不同PLC的浮点数/32位数据的字节顺序可能不同，可通过 `DataFormat` 属性设置（ABCD/CDAB/BADC/DCBA）。
5. **订阅性能**：订阅的地址越多，轮询间隔越短，网络负载越大。建议根据实际需要设置合理的轮询频率。
6. **线程安全**：HslCommunication 的操作方法是线程安全的，可以在多线程环境中使用。

---

## 四、练习建议

### 练习1：HslCommunication 通信测试工具
- WinForms/WPF界面，配置IP、端口、从站地址
- 输入地址选择数据类型（Int16/Float/String/Bool）
- 支持单点读写和批量读写
- 显示通信日志（发送/接收原始帧）

### 练习2：多设备数据采集面板
- 使用 HslModbusDataService 封装类管理多台设备
- 每台设备配置不同的IP和从站地址
- 启动数据订阅，实时更新界面数据
- 显示每个设备的连接状态和通信统计

### 练习3：报警监控系统
- 使用订阅机制实时采集PLC数据
- 设定报警阈值（如温度 > 80度报警）
- 报警时记录日志、弹出通知、改变界面颜色
- 支持报警确认和报警历史查询

---

## 五、常见错误

### 错误1：OperateResult.IsSuccess 为 false
```
现象：读取结果总是失败
```
**常见原因与解决**：
- IP地址或端口错误 --> 检查设备实际地址
- 从站地址不匹配 --> 确认 SlaveId 配置
- 寄存器地址超出范围 --> 查阅设备手册
- 网络不通 --> ping 测试网络连通性

### 错误2：Float值读取不正确
```
现象：Float值明显不合理（如 1.175494E-38 或 NaN）
```
**原因**：设备使用的字节顺序与默认不一致。
**解决**：
```csharp
// 设置数据格式为CDAB（某些PLC使用此顺序）
_modbus.DataFormat = HslCommunication.Core.DataFormat.CDAB;
```

### 错误3：NuGet包安装后编译报错
```
原因：可能安装了错误的包名
```
**解决**：
```bash
dotnet add package HslCommunication   # 正确的包名
```

### 错误4：订阅回调不触发
```
现象：启动订阅后，回调方法从未被调用
```
**原因**：
- 未调用 `ConnectServer()` 先建立连接
- 轮询间隔过短，设备响应不及时
- 设备地址/从站ID配置错误
**解决**：确保先连接成功，检查轮询间隔和地址配置。

### 错误5：自动重连后第一次读取仍然失败
```
原因：重连成功后立即读取，设备可能尚未完全就绪
```
**解决**：在自动重连回调中添加延迟再重试。
