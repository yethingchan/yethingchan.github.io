## 相关链接

- [[通信协议总览]] - 协议全景与选择指南
- [[Modbus协议详解]] - 通用工业协议
- [[Siemens-S7协议]] - 西门子PLC协议
- [[自定义驱动开发指南]] - 创建自己的驱动
- [[驱动插件架构]] - 插件加载机制

---

## 其他PLC协议

除了Modbus和Siemens S7之外，IoTGateway还支持三菱Melsec MC、欧姆龙FINS和罗克韦尔Allen-Bradley CIP三种主流PLC协议。这三种驱动都通过 **IoTClient** NuGet库实现，具有高度一致的代码结构。

---

## IoTClient库概述

IoTClient是一个开源的.NET工业通信库，封装了多种PLC协议：

```
IoTClient 支持的协议:
├─ MitsubishiClient    → 三菱 MC协议 (A-1E / Qna-3E)
├─ OmronFinsClient     → 欧姆龙 FINS协议
├─ AllenBradleyClient  → AB CIP协议 (EtherNet/IP)
├─ SiemensClient       → 西门子 S7协议
├─ ModbusTcpClient     → Modbus TCP
└─ ModbusRtuClient     → Modbus RTU
```

IoTGateway使用IoTClient统一对接三菱、欧姆龙和AB三种PLC，使得这三种驱动的API风格高度一致。

### 统一的API模式

三种驱动都遵循相同的调用模式：

```csharp
// 创建客户端
var client = new XxxClient(ipAddress, port);

// 打开连接
client.Open();

// 读取不同类型的数据
client.ReadBoolean(address);    // 读取Bool
client.ReadByte(address);       // 读取Byte
client.ReadUInt16(address);     // 读取UInt16
client.ReadInt16(address);      // 读取Int16
client.ReadUInt32(address);     // 读取UInt32
client.ReadInt32(address);      // 读取Int32
client.ReadFloat(address);      // 读取Float
client.ReadDouble(address);     // 读取Double
client.ReadString(address);     // 读取String

// 关闭连接
client.Close();
```

---

## 三菱 Melsec MC 协议

### 协议概述

MC协议（MELSEC Communication Protocol）是三菱PLC的通信协议，支持多种帧格式。IoTGateway通过IoTClient支持两种主要帧格式：

```
MC协议帧格式:
├─ A兼容1E帧 (A_1E)
│   ├─ 适用于: FX3U/FX3G + FX3U-ENET-ADP
│   ├─ 简单帧结构，兼容性好
│   └─ 通信速度较慢
│
└─ Qna兼容3E帧 (Qna_3E)  ← 推荐
    ├─ 适用于: Q系列/iQ-R系列/iQ-F系列
    ├─ 结构化帧，功能丰富
    ├─ 支持批量读写
    └─ 现代三菱PLC首选
```

### 三菱PLC地址体系

```
三菱 PLC 数据寄存器:

┌──────────┬──────────┬──────────────────────────────┐
│ 软元件    │ 地址前缀  │ 说明                         │
├──────────┼──────────┼──────────────────────────────┤
│ 数据寄存器│ D        │ D0, D100, D1000 等           │
│ 位寄存器  │ M        │ M0, M100 等                  │
│ 输入      │ X        │ X0, X1 (八进制)              │
│ 输出      │ Y        │ Y0, Y1 (八进制)              │
│ 文件寄存器│ R        │ R0, R100 等                  │
│ 链接寄存器│ W        │ W0, W100 (十六进制)          │
│ 定时器    │ T        │ TN (当前值) TS (触点) TC (线圈)│
│ 计数器    │ C        │ CN (当前值) CS (触点) CC (线圈)│
└──────────┴──────────┴──────────────────────────────┘
```

### IoTGateway三菱驱动实现

```csharp
// DeviceMelsecMc.cs
[DriverSupported("A_1E")]
[DriverSupported("Qna_3E")]
[DriverInfo("MelsecMc", "V1.0.0", "Copyright IoTGateway.net 20230220")]
public class DeviceMelsecMc : IDriver
{
    private MitsubishiClient? _plc;

    #region 配置参数
    [ConfigParameter("设备Id")] public string DeviceId { get; set; }
    [ConfigParameter("PLC类型")] 
    public MitsubishiVersion CpuType { get; set; } = MitsubishiVersion.Qna_3E;
    [ConfigParameter("IP地址")] public string IpAddress { get; set; } = "127.0.0.1";
    [ConfigParameter("端口号")] public int Port { get; set; } = 6000;
    [ConfigParameter("超时时间ms")] public int Timeout { get; set; } = 3000;
    [ConfigParameter("最小通讯周期ms")] public uint MinPeriod { get; set; } = 3000;
    #endregion

    public bool Connect()
    {
        _plc = new MitsubishiClient(CpuType, IpAddress, Port);
        _plc.Open();
        return IsConnected;
    }

    public bool IsConnected => _plc is { Connected: true };
}
```

### 数据读取

```csharp
[Method("读PLC标准地址", description: "读PLC标准地址")]
public DriverReturnValueModel Read(DriverAddressIoArgModel ioArg)
{
    var ret = new DriverReturnValueModel { StatusType = VaribaleStatusTypeEnum.Good };

    if (_plc != null && this.IsConnected)
    {
        switch (ioArg.ValueType)
        {
            case PluginInterface.DataTypeEnum.Bool:
                ret.Value = _plc.ReadBoolean(ioArg.Address).Value;
                break;
            case PluginInterface.DataTypeEnum.Uint16:
                ret.Value = _plc.ReadUInt16(ioArg.Address).Value;
                break;
            case PluginInterface.DataTypeEnum.Int32:
                ret.Value = _plc.ReadInt32(ioArg.Address).Value;
                break;
            case PluginInterface.DataTypeEnum.Float:
                ret.Value = _plc.ReadFloat(ioArg.Address).Value;
                break;
            case PluginInterface.DataTypeEnum.AsciiString:
                ret.Value = _plc.ReadString(ioArg.Address);
                break;
            // ... 其他类型
        }
    }
    return ret;
}
```

### 批量读取（三菱特色功能）

三菱驱动实现了高效的批量读取功能，一次通信读取多个分散地址：

```csharp
[Method("批量读PLC标准地址", description: "数据区,开始地址,地址间隔,读取总数")]
public DriverReturnValueModel ReadMultiple(DriverAddressIoArgModel ioArg)
{
    // 地址格式: "D,100,1,50" → 从D100开始，间隔1，读取50个
    var addresStrings = ioArg.Address.Split(',');
    string block = addresStrings[0];     // 数据区（如"D"）
    ushort start = ushort.Parse(addresStrings[1]);  // 起始地址
    ushort step = ushort.Parse(addresStrings[2]);   // 地址间隔
    ushort count = ushort.Parse(addresStrings[3]);  // 读取数量

    // 构建批量地址字典
    var batchAddress = new Dictionary<string, DataTypeEnum>();
    for (int i = 0; i < count; i++)
    {
        batchAddress[$"{block}{start + i * step}"] = dataType;
    }

    // 一次通信读取所有地址
    var batchResult = _plc.BatchRead(batchAddress, count);

    if (batchResult.IsSucceed)
    {
        // 存入缓存供后续使用
        foreach (var value in batchResult.Value)
            _cache[value.Key] = value.Value;
        ret.Value = batchResult.Value.Select(x => x.Value).ToList();
    }
}
```

> **性能优势**：批量读取将50个地址的读取从50次通信减少为1次，延迟从50*10ms=500ms降低到约20ms。对于点位多的场景，这是非常关键的优化。

---

## 欧姆龙 FINS 协议

### 协议概述

FINS（Factory Interface Network Service）是欧姆龙PLC的通信协议，支持串口和以太网两种传输方式。IoTGateway支持基于以太网的FINS/TCP。

```
FINS 通信模型:

┌──────────┐        FINS/TCP        ┌──────────┐
│ 上位机    │ ◄────────────────────► │ 欧姆龙PLC │
│          │     TCP 端口 6000       │ CJ2M     │
│ FINS命令  │                        │ NJ/NX    │
│ FINS响应  │                        │ CP1H     │
└──────────┘                        └──────────┘

FINS 节点寻址:
  FINS使用三层地址: 网络号.节点号.单元号
  例如: 0.0.0 表示 网络0, 节点0, 单元0
```

### 欧姆龙PLC地址体系

```
欧姆龙 PLC 数据区域:

┌──────────┬──────────┬──────────────────────────────┐
│ 区域      │ 地址前缀  │ 说明                         │
├──────────┼──────────┼──────────────────────────────┤
│ DM区      │ D        │ 数据存储器 D0~D32767         │
│ CIO区     │ (无前缀)  │ 核心I/O区 0~6143            │
│ WR区      │ W        │ 工作区 W0~W511              │
│ HR区      │ H        │ 保持继电器 H0~H511          │
│ 定时器    │ T        │ 定时器当前值                  │
│ 计数器    │ C        │ 计数器当前值                  │
└──────────┴──────────┴──────────────────────────────┘

地址示例:
  D100     → DM区第100个字
  D100.00  → DM区第100个字的第0位
  100      → CIO区第100个字
  100.00   → CIO区第100个字的第0位
  W100     → WR区第100个字
  H100     → HR区第100个字
```

### IoTGateway欧姆龙驱动实现

```csharp
// DeviceOmronFins.cs
[DriverSupported("OmronFins")]
[DriverInfo("OmronFins", "V1.0.0", "Copyright IoTGateway.net 20230220")]
public class DeviceOmronFins : IDriver
{
    private OmronFinsClient? _plc;

    #region 配置参数
    [ConfigParameter("设备Id")] public string DeviceId { get; set; }
    [ConfigParameter("IP地址")] public string IpAddress { get; set; } = "127.0.0.1";
    [ConfigParameter("端口号")] public int Port { get; set; } = 6000;
    [ConfigParameter("超时时间ms")] public int Timeout { get; set; } = 3000;
    [ConfigParameter("最小通讯周期ms")] public uint MinPeriod { get; set; } = 3000;
    #endregion

    public bool Connect()
    {
        _plc = new OmronFinsClient(IpAddress, Port);
        _plc.Open();
        return IsConnected;
    }

    [Method("读PLC标准地址", description: "读PLC标准地址")]
    public DriverReturnValueModel Read(DriverAddressIoArgModel ioArg)
    {
        switch (ioArg.ValueType)
        {
            case DataTypeEnum.Bool:
                ret.Value = _plc.ReadBoolean(ioArg.Address).Value;
                break;
            case DataTypeEnum.Uint16:
                ret.Value = _plc.ReadUInt16(ioArg.Address).Value;
                break;
            case DataTypeEnum.Float:
                ret.Value = _plc.ReadFloat(ioArg.Address).Value;
                break;
            case DataTypeEnum.Double:
                ret.Value = _plc.ReadDouble(ioArg.Address).Value;
                break;
            // ...
        }
        return ret;
    }
}
```

### 欧姆龙PLC连接注意事项

```
连接检查清单:

□ PLC的FINS/TCP功能已启用
  CX-Programmer → PLC设定 → 内置以太网络 → FINS/TCP

□ PLC的IP地址已正确配置
  通常通过CX-Programmer的PLC设定

□ FINS节点号正确
  上位机的FINS节点号需要与PLC在同一网段
  IoTClient默认自动协商节点号

□ 端口6000未被防火墙阻止
  欧姆龙默认使用TCP 6000端口
```

---

## Allen-Bradley CIP 协议

### 协议概述

CIP（Common Industrial Protocol）是罗克韦尔（Allen-Bradley）的工业通信协议，运行在EtherNet/IP传输层之上：

```
CIP 协议栈:

┌─────────────────────────┐
│ CIP 应用层               │ ← 对象模型，标签访问
│ (Common Industrial       │
│  Protocol)               │
├─────────────────────────┤
│ CIP 传输层               │ ← UCMM / 连接管理
├─────────────────────────┤
│ EtherNet/IP             │ ← TCP/UDP封装
│ (Industrial Protocol)   │
├─────────────────────────┤
│ TCP/UDP                 │ ← 端口 44818 (显式消息)
├─────────────────────────┤  端口 2222 (隐式消息/I/O)
│ Ethernet               │
└─────────────────────────┘
```

### AB PLC地址体系

AB PLC使用**标签（Tag）**作为数据访问的基本单元，与西门子/三菱的地址方式完全不同：

```
AB PLC 标签体系:

Controller Tags (控制器标签):
  Temperature         REAL     温度值
  Pressure            REAL     压力值
  Speed               DINT     速度
  IsRunning           BOOL     运行状态
  ProductName         STRING   产品名

Program Tags (程序标签):
  Program:MainProgram.LocalVar

数组标签:
  DataArray[0]        INT      数组元素
  DataArray[10]       INT

结构体标签:
  Motor.Speed         REAL     结构体成员
  Motor.Status        BOOL
  Motor.Current       REAL
```

### IoTGateway AB驱动实现

```csharp
// DeviceAllenBradley.cs
[DriverSupported("AllenBradley")]
[DriverInfo("AllenBradley", "V1.0.0", "Copyright IoTGateway.net 20230220")]
public class DeviceAllenBradley : IDriver
{
    private AllenBradleyClient? _plc;

    #region 配置参数
    [ConfigParameter("设备Id")] public string DeviceId { get; set; }
    [ConfigParameter("IP地址")] public string IpAddress { get; set; } = "127.0.0.1";
    [ConfigParameter("端口号")] public int Port { get; set; } = 44818;
    [ConfigParameter("超时时间ms")] public int Timeout { get; set; } = 3000;
    [ConfigParameter("最小通讯周期ms")] public uint MinPeriod { get; set; } = 3000;
    #endregion

    public bool Connect()
    {
        _plc = new AllenBradleyClient(IpAddress, Port);
        _plc.Open();
        return IsConnected;
    }

    [Method("读AllenBradleyPLC标准地址", description: "读AllenBradleyPLC标准地址")]
    public DriverReturnValueModel Read(DriverAddressIoArgModel ioarg)
    {
        switch (ioarg.ValueType)
        {
            case DataTypeEnum.Bool:
                ret.Value = _plc.ReadBoolean(ioarg.Address).Value;
                break;
            case DataTypeEnum.Float:
                ret.Value = _plc.ReadFloat(ioarg.Address).Value;
                break;
            case DataTypeEnum.Int32:
                ret.Value = _plc.ReadInt32(ioarg.Address).Value;
                break;
            // ...
        }
        return ret;
    }
}
```

### AB PLC连接注意事项

```
连接检查清单:

□ PLC以太网模块已配置IP地址
  RSLogix5000/Studio5000 → Controller Properties → Ethernet

□ 目标Tag已创建
  Controller Tags 或 Program Tags

□ 通信端口44818可达
  使用 telnet 192.168.1.x 44818 测试

□ PLC的通信负载未满
  AB PLC有连接数限制（通常32个CIP连接）

□ 没有使用安全PLC (GuardLogix)
  安全PLC需要额外的安全连接配置
```

---

## 三种协议的对比

### 特性对比

| 特性 | 三菱 MC | 欧姆龙 FINS | AB CIP |
|------|---------|------------|--------|
| 默认端口 | 5000/6000 | 6000 | 44818 |
| 地址方式 | 软元件+偏移量 | 区域+偏移量 | 标签名 |
| 数据类型 | 字/双字/实数 | 字/双字/实数 | BOOL/INT/DINT/REAL |
| 批量读取 | 支持 | 支持 | 支持 |
| 字符串处理 | 2字/字符 | 2字/字符 | 1字/字符 |
| 字节序 | 小端 | 大端 | 小端 |
| 连接方式 | TCP | TCP | TCP |
| 编程软件 | GX Works | CX-Programmer | Studio 5000 |

### 地址格式对比

```
读取一个浮点数值:

三菱:  _plc.ReadFloat("D100")        // D区第100个字开始
欧姆龙: _plc.ReadFloat("D100")        // DM区第100个字开始
AB:    _plc.ReadFloat("Temperature")  // 标签名"Temperature"
```

### 连接参数对比

```
三菱:
  _plc = new MitsubishiClient(
      MitsubishiVersion.Qna_3E,  // PLC帧格式
      "192.168.1.10",           // IP
      6000                       // 端口
  );

欧姆龙:
  _plc = new OmronFinsClient(
      "192.168.1.20",           // IP
      6000                       // 端口
  );

AB:
  _plc = new AllenBradleyClient(
      "192.168.1.30",           // IP
      44818                      // 端口
  );
```

---

## 统一驱动模式分析

### 代码结构一致性

观察三种驱动的实现，可以发现高度一致的代码结构：

```csharp
// 所有驱动的统一模式
public class DeviceXxx : IDriver
{
    private XxxClient? _plc;           // 1. 客户端实例

    // 2. 配置参数（通过Attribute自动发现）
    [ConfigParameter("IP地址")] public string IpAddress { get; set; }
    [ConfigParameter("端口号")] public int Port { get; set; }
    [ConfigParameter("超时时间ms")] public int Timeout { get; set; }
    [ConfigParameter("最小通讯周期ms")] public uint MinPeriod { get; set; }

    // 3. 生命周期
    public bool Connect()  { _plc = new XxxClient(...); _plc.Open(); return IsConnected; }
    public bool Close()    { _plc?.Close(); return !IsConnected; }
    public void Dispose()  { _plc = null; GC.SuppressFinalize(this); }

    // 4. 读取方法（通过Attribute暴露给上层）
    [Method("读PLC标准地址", description: "...")]
    public DriverReturnValueModel Read(DriverAddressIoArgModel ioArg)
    {
        // 根据ValueType分派到具体的读取方法
        switch (ioArg.ValueType) { ... }
    }

    // 5. 写入方法
    public async Task<RpcResponse> WriteAsync(...) { ... }
}
```

这种一致性是**驱动插件架构**的设计成果，详见[[驱动插件架构]]和[[自定义驱动开发指南]]。

### IoTClient结果处理

IoTClient的所有读取方法返回统一的结果类型：

```csharp
// IoTClient 返回结果模式
var result = _plc.ReadUInt16("D100");

// result.IsSucceed  → 是否成功
// result.Value      → 读取的值
// result.Err        → 错误信息
// result.ReqBytes   → 请求报文字节
// result.RespBytes  → 响应报文字节

// IoTGateway中的使用方式:
if (result.IsSucceed)
    ret.Value = result.Value;
else
    ret.StatusType = VaribaleStatusTypeEnum.Bad;
```

---

## 实际部署建议

### 三菱PLC

```
GX Works2/3 配置步骤:

1. 启用以太网模块
   Parameter → Ethernet Port → Built-in Ethernet

2. 设置IP地址
   IP Address: 192.168.1.10
   Subnet Mask: 255.255.255.0

3. 打开端口
   Open Setting → 添加一条规则
   Protocol: TCP, Port: 6000 (或5000)

4. 设置通信数据
   允许MC协议通信（默认允许）

5. 测试连接
   ping 192.168.1.10
   使用GX Works2的通信测试功能
```

### 欧姆龙PLC

```
CX-Programmer 配置步骤:

1. PLC设定中启用FINS/TCP
   PLC Settings → Built-in Ethernet → FINS/TCP

2. 配置IP地址
   IP Address: 192.168.1.20
   
3. 设置FINS节点号
   Network: 0, Node: 20 (通常与IP末段一致)

4. 开放通信端口
   TCP Port: 6000

5. 检查通信设置
   确保"Enable FINS/TCP Communications"已勾选
```

### AB PLC

```
Studio 5000 配置步骤:

1. 配置以太网模块
   Controller Properties → Ethernet Module

2. 创建Controller Tags
   在Controller Tags中添加需要上位机访问的变量
   注意: 变量类型需要匹配上位机的读取类型

3. 检查连接数
   Communication → Who Active → 确认连接数未满

4. 无需额外配置端口
   EtherNet/IP默认使用44818端口

5. 测试连接
   RSLogix → Communications → Who Active
   使用RSLinx查看通信状态
```

---

## 小结

| 协议 | 厂商 | IoTClient类 | 默认端口 | 地址方式 |
|------|------|------------|---------|---------|
| MC | 三菱 | MitsubishiClient | 6000 | D100, M100 |
| FINS | 欧姆龙 | OmronFinsClient | 6000 | D100, W100 |
| CIP | AB | AllenBradleyClient | 44818 | TagName |

三种协议通过IoTClient库实现了统一的API，IoTGateway的驱动代码结构高度一致，便于维护和扩展。选择协议时主要取决于现场使用的PLC品牌。

---

上一篇: [[MQTT协议与物联网通信]] | 下一篇: [[CNC系统通信]]
