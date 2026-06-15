## 相关链接

- [[通信协议总览]] - 协议全景与选择指南
- [[Modbus协议详解]] - 另一种广泛使用的协议
- [[OPC标准与实现]] - 统一工业通信标准
- [[驱动插件架构]] - 插件加载机制
- [[自定义驱动开发指南]] - 创建自己的驱动

---

## Siemens S7协议

西门子S7系列PLC是全球市场占有率最高的PLC品牌之一。S7协议是西门子PLC的专有通信协议，基于ISO-on-TCP（RFC 1006），默认使用TCP端口102。IoTGateway通过S7.Net库实现了对S7全系列PLC的支持。

---

## S7协议概述

### 协议栈层次

S7协议基于西门子私有的ISO-on-TCP协议栈：

```
┌─────────────────────────────────┐
│ S7 Communication (应用层)       │ ← S7 PDU: 读写请求/响应
├─────────────────────────────────┤
│ S7 Connection (会话层)          │ ← 连接建立/维护
├─────────────────────────────────┤
│ ISO 8327-1 (会话层)             │ ← ISO会话协议
├─────────────────────────────────┤
│ ISO 8073 (传输层)               │ ← ISO传输协议（类4）
├─────────────────────────────────┤
│ RFC 1006 / ISO-on-TCP           │ ← TCP封装ISO协议
├─────────────────────────────────┤
│ TCP (端口102)                   │ ← 标准TCP传输
├─────────────────────────────────┤
│ IP                              │ ← 网络层
├─────────────────────────────────┤
│ Ethernet                        │ ← 物理层
└─────────────────────────────────┘
```

### 支持的PLC型号

IoTGateway的S7驱动支持西门子全系列PLC：

```csharp
[DriverSupported("1500")]    // S7-1500 高端PLC
[DriverSupported("1200")]    // S7-1200 中端PLC（最常用）
[DriverSupported("400")]     // S7-400 大型PLC
[DriverSupported("300")]     // S7-300 中型PLC
[DriverSupported("200")]     // S7-200 小型PLC（停产）
[DriverSupported("200Smart")]// S7-200 Smart（中国市场）
```

### 各型号连接参数

| PLC型号 | Rack | Slot | 默认端口 | 连接限制 |
|---------|------|------|---------|---------|
| S7-1200 | 0 | 0 | 102 | 最多3个HMI连接 |
| S7-1500 | 0 | 1 | 102 | 需开放PUT/GET权限 |
| S7-300 | 0 | 2 | 102 | 取决于CP模块 |
| S7-400 | 0 | 2~3 | 102 | 取决于CP模块 |
| S7-200 | 0 | 2 | 102 | 需CP243-1模块 |
| S7-200Smart | 0 | 1 | 102 | 最多4个连接 |

> **重要**：S7-1500默认关闭PUT/GET通信，需要在TIA Portal中手动开启：
> 设备组态 → CPU属性 → 保护与安全 → 连接机制 → 勾选"允许来自远程对象的PUT/GET通信访问"

---

## S7内存区域与寻址

### 数据块类型

S7 PLC的内存分为多个区域，上位机主要访问数据块（DB）：

```
┌────────────────────────────────────────────┐
│            S7 PLC 内存区域                  │
├───────────┬────────────────────────────────┤
│ OB        │ 组织块（程序入口，不可外部访问） │
│ FB        │ 功能块（带背景数据块）          │
│ FC        │ 功能（无背景数据块）            │
│ DB        │ 数据块 ← 上位机主要读写区域     │
│ I         │ 过程映像输入（PII）            │
│ Q         │ 过程映像输出（PIQ）            │
│ M         │ 位存储器（中间变量）            │
│ T         │ 定时器                         │
│ C         │ 计数器                         │
└───────────┴────────────────────────────────┘
```

### 地址表示方式

S7协议使用独特的地址表示方式：

```
DB块地址:
  DB1.DBX0.0     → DB1的位0.0（布尔类型）
  DB1.DBB0       → DB1的字节0（8位）
  DB1.DBW0       → DB1的字0（16位，从字节0开始）
  DB1.DBD0       → DB1的双字0（32位，从字节0开始）

其他区域:
  M0.0           → 位存储器位0.0
  MW0            → 位存储器字0
  MD0            → 位存储器双字0
  I0.0           → 输入位0.0
  Q0.0           → 输出位0.0
```

### 地址偏移量与数据类型

```
DB100 数据块内容示例:

字节偏移:  0     1     2     3     4     5     6     7
         ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
DBB:     │DBB0 │DBB1 │DBB2 │DBB3 │DBB4 │DBB5 │DBB6 │DBB7 │
         ├─────┴─────┤     │     │     │     │     │     │
DBW:     │  DBW0     │DBW2 │     │DBW4 │     │DBW6 │     │
         ├───────────┴─────┴─────┤     │     │     │     │
DBD:     │      DBD0            │DBD4 │     │     │     │
         └───────────────────────┴─────┴─────┴─────┴─────┘

数据类型对应:
  DBX0.0  → Bool     (1 bit)
  DBB0    → Byte     (8 bit, 无符号)
  DBW0    → Word     (16 bit, 无符号) / Int (16 bit, 有符号)
  DBD0    → DWord    (32 bit, 无符号) / DInt (32 bit, 有符号)
  DBD0    → Real     (32 bit, 浮点数)
```

---

## IoTGateway S7驱动实现

### 驱动类定义

```csharp
// DeviceSiemensS7.cs
[DriverSupported("1500")]
[DriverSupported("1200")]
[DriverSupported("400")]
[DriverSupported("300")]
[DriverSupported("200")]
[DriverSupported("200Smart")]
[DriverInfo("SiemensS7", "V1.0.0", "Copyright IoTGateway.net 20230220")]
public class DeviceSiemensS7 : IDriver
{
    private Plc _plc;

    #region 配置参数
    [ConfigParameter("设备Id")] public string DeviceId { get; set; }
    [ConfigParameter("PLC类型")] public CpuType CpuType { get; set; } = CpuType.S71200;
    [ConfigParameter("IP地址")] public string IpAddress { get; set; } = "127.0.0.1";
    [ConfigParameter("端口号")] public int Port { get; set; } = 102;
    [ConfigParameter("Rack")] public short Rack { get; set; } = 0;
    [ConfigParameter("Slot")] public short Slot { get; set; } = 0;
    [ConfigParameter("超时时间ms")] public int Timeout { get; set; } = 3000;
    [ConfigParameter("最小通讯周期ms")] public uint MinPeriod { get; set; } = 3000;
    #endregion
}
```

### 连接建立

```csharp
public bool Connect()
{
    try
    {
        _logger.LogInformation($"Device:[{_device}],Connect()");

        // 创建PLC对象，指定CPU类型、IP、端口、Rack和Slot
        _plc = new Plc(CpuType, IpAddress, Port, Rack, Slot);
        
        // 设置读写超时
        _plc.ReadTimeout = Timeout;
        _plc.WriteTimeout = Timeout;
        
        // 打开连接（执行COTP和S7连接握手）
        _plc.Open();
    }
    catch (Exception ex)
    {
        _logger.LogInformation($"Device:[{_device}],Connect()");
        return false;
    }

    return IsConnected;
}

// 连接状态检查
public bool IsConnected => _plc is { IsConnected: true };
```

S7连接建立过程（三次握手）：

```
上位机                              S7 PLC
  │                                   │
  │──── COTP CR (连接请求) ──────────→│
  │                                   │
  │←─── COTP CC (连接确认) ───────────│
  │                                   │
  │──── S7 Setup Communication ──────→│  协商PDU大小
  │                                   │
  │←─── S7 Setup Ack ─────────────────│  返回最大PDU大小
  │                                   │
  │    连接建立完成，可以读写数据       │
```

### 数据读取

IoTGateway的S7驱动提供了两种读取方法：

#### 标准地址读取

```csharp
[Method("读西门子PLC标准地址", description: "读西门子PLC标准地址")]
public DriverReturnValueModel Read(DriverAddressIoArgModel ioArg)
{
    var ret = new DriverReturnValueModel { StatusType = VaribaleStatusTypeEnum.Good };

    if (_plc is { IsConnected: true })
    {
        try
        {
            // 字符串类型需要特殊处理
            if (ioArg.ValueType == DataTypeEnum.AsciiString || 
                ioArg.ValueType == DataTypeEnum.Utf8String || 
                ioArg.ValueType == DataTypeEnum.Gb2312String)
            {
                // 1. 解析地址
                var dataItem = S7.Net.Types.DataItem.FromAddress(ioArg.Address);
                
                // 2. 读取字符串头（前2字节：最大长度和实际长度）
                var head = _plc.ReadBytes(dataItem.DataType, dataItem.DB, 
                    dataItem.StartByteAdr, 2);
                
                // 3. 根据实际长度读取字符串内容
                var strBytes = _plc.ReadBytes(dataItem.DataType, dataItem.DB, 
                    dataItem.StartByteAdr + 2, head[1]);
                
                ret.Value = GetString(ioArg.ValueType, strBytes);
            }
            else
            {
                // 通用读取：S7.Net库自动解析地址
                ret.Value = _plc.Read(ioArg.Address);
                
                // 根据数据类型做类型转换
                switch (ioArg.ValueType)
                {
                    case DataTypeEnum.Int16:
                        ret.Value = (short)(ushort)ret.Value;
                        break;
                    case DataTypeEnum.Int32:
                        ret.Value = (int)(uint)ret.Value;
                        break;
                    case DataTypeEnum.Float:
                        // 手动处理浮点数字节序
                        var buffer = new byte[4];
                        buffer[3] = (byte)((uint)ret.Value >> 24);
                        buffer[2] = (byte)((uint)ret.Value >> 16);
                        buffer[1] = (byte)((uint)ret.Value >> 8);
                        buffer[0] = (byte)((uint)ret.Value >> 0);
                        ret.Value = BitConverter.ToSingle(buffer, 0);
                        break;
                }
            }
        }
        catch (Exception ex)
        {
            ret.StatusType = VaribaleStatusTypeEnum.Bad;
            ret.Message = $"读取失败,{ex.Message}";
        }
    }
    return ret;
}
```

#### 字节字符串读取

用于读取PLC中自定义格式的字符串：

```csharp
[Method("读西门子字节字符串", description: "DB10.DBW6,10  即开始地址，字节长度")]
public DriverReturnValueModel ReadByteString(DriverAddressIoArgModel ioArg)
{
    // 地址格式: "DB块地址,字节长度"
    // 例如: "DB10.DBW6,10" 表示从DB10的DBW6开始读取10个字节
    var arrParams = ioArg.Address.Trim().Split(',');
    
    var dataItem = S7.Net.Types.DataItem.FromAddress(arrParams[0]);
    int.TryParse(arrParams[1], out var length);

    // 读取指定长度的字节
    var data = _plc.ReadBytes(dataItem.DataType, dataItem.DB, 
        dataItem.StartByteAdr, length);
    
    // 转换为ASCII字符串，过滤不可见字符
    var strRaw = Encoding.ASCII.GetString(data).TrimEnd('\0');
    // ...
}
```

### 数据写入

```csharp
public async Task<RpcResponse> WriteAsync(string requestId, string method,
    DriverAddressIoArgModel ioArg)
{
    if (method == nameof(Read)) // 使用标准地址写入
    {
        var dataItem = DataItem.FromAddress(ioArg.Address);
        
        if (ioArg.ValueType == DataTypeEnum.AsciiString)
        {
            // 西门子String类型写入：先写长度，再写内容
            await _plc.WriteAsync(dataItem.DataType, dataItem.DB, 
                dataItem.StartByteAdr + 1,
                (byte)((byte[])toWrite).Length);
            await _plc.WriteAsync(dataItem.DataType, dataItem.DB, 
                dataItem.StartByteAdr + 2, (byte[])toWrite);
        }
        else
        {
            // 通用写入
            await _plc.WriteAsync(ioArg.Address, toWrite);
        }
    }
}
```

### 西门子String类型的内存结构

西门子PLC的String类型有特殊的前缀结构：

```
西门子 String 内存布局（以String[254]为例）:

字节偏移:  0        1        2        3        4       ...
         ┌────────┬────────┬────────┬────────┬────────┬───
         │最大长度 │实际长度 │字符1   │字符2   │字符3   │ ...
         │ 0xFE   │ 0x05   │ 'H'    │ 'e'    │ 'l'    │
         │ (254)  │ (5)    │ 0x48   │ 0x65   │ 0x6C   │
         └────────┴────────┴────────┴────────┴────────┴───

写入时需要:
1. 先读取字节0获取最大长度（用于校验）
2. 写入字节1为实际长度
3. 从字节2开始写入字符串内容
```

---

## S7.Net库地址解析

### DataItem解析

S7.Net库可以自动解析标准S7地址格式：

```csharp
// DataItem.FromAddress() 支持的地址格式
var item = DataItem.FromAddress("DB1.DBX0.0");   // DB1的位0.0
var item = DataItem.FromAddress("DB1.DBB0");      // DB1的字节0
var item = DataItem.FromAddress("DB1.DBW0");      // DB1的字0
var item = DataItem.FromAddress("DB1.DBD0");      // DB1的双字0
var item = DataItem.FromAddress("M0.0");           // 位存储器位0.0
var item = DataItem.FromAddress("MW0");            // 位存储器字0
var item = DataItem.FromAddress("I0.0");           // 输入位0.0
var item = DataItem.FromAddress("Q0.0");           // 输出位0.0

// DataItem的属性
// item.DataType → DataBlock, Input, Output, Memory, Timer, Counter
// item.DB       → DB块编号（仅DataBlock类型有效）
// item.StartByteAdr → 起始字节地址
// item.BitAdr  → 位地址（仅Bool类型有效）
```

### 支持的地址前缀

| 前缀 | 含义 | 示例 |
|------|------|------|
| DB | 数据块 | DB1.DBW0 |
| I | 输入 | I0.0, IW0, ID0 |
| Q | 输出 | Q0.0, QW0, QD0 |
| M | 位存储器 | M0.0, MW0, MD0 |
| T | 定时器 | T0 |
| C | 计数器 | C0 |

---

## S7通信优化

### PDU大小与批量读写

S7 PLC有PDU（Protocol Data Unit）大小限制：

| PLC型号 | 最大PDU大小 | 单次最大读取字节数 |
|---------|------------|-----------------|
| S7-1200 | 240 bytes | ~220 bytes |
| S7-1500 | 960 bytes | ~940 bytes |
| S7-300 | 240 bytes | ~220 bytes |
| S7-400 | 480 bytes | ~460 bytes |

> 这意味着对于S7-1200，一次最多读取约220字节（约110个寄存器或220个Bool）。如果需要读取更多数据，需要分批。

### 批量读取示例

```csharp
// S7.Net支持批量读取多个地址
var dataItems = new List<DataItem>
{
    new DataItem { DataType = DataType.DataBlock, DB = 1, 
                   StartByteAdr = 0, VarType = VarType.Word },
    new DataItem { DataType = DataType.DataBlock, DB = 1, 
                   StartByteAdr = 2, VarType = VarType.DWord },
    new DataItem { DataType = DataType.DataBlock, DB = 1, 
                   StartByteAdr = 6, VarType = VarType.Real },
};

_plc.ReadMultiple(dataItems.ToArray());
// 结果存在每个DataItem.Value中
```

### 连接保活

S7连接长时间不通信可能被PLC主动断开。IoTGateway通过最小通信周期保证连接存活：

```csharp
[ConfigParameter("最小通讯周期ms")] public uint MinPeriod { get; set; } = 3000;
```

建议设置：
- 正常采集：500ms-3000ms
- 保活心跳：不超过PLC的连接超时时间（通常60秒）

---

## 常见问题排查

### 连接失败

| 错误信息 | 原因 | 解决方案 |
|---------|------|---------|
| Connection refused | IP或端口错误 | 确认PLC的IP和端口102 |
| ISO 8073 error | Rack/Slot错误 | 检查Rack和Slot值 |
| CPU not accessible | 权限不足 | S7-1500需开启PUT/GET |
| Timeout | 网络不通 | ping测试，检查防火墙 |

### Rack和Slot速查表

```
S7-1200:  Rack=0, Slot=0  (固定值)
S7-1500:  Rack=0, Slot=1  (固定值)
S7-300:   Rack=0, Slot=2  (CPU通常在Slot2)
S7-400:   Rack=0, Slot=X  (X取决于硬件配置)
S7-200Smart: Rack=0, Slot=1
```

### 读取返回异常值

| 现象 | 可能原因 |
|------|---------|
| 全为0 | DB块不存在或地址错误 |
| 值偏大/偏小 | 数据类型不匹配（Int vs DInt vs Real） |
| 布尔值反向 | 位地址错误（注意DBX后的.0~.7） |
| Float读为Int | 需要使用Real类型读取 |

### TIA Portal侧配置检查清单

```
□ CPU已配置以太网IP地址
□ CPU属性中已开启PUT/GET通信（S7-1500）
□ DB块未勾选"优化的块访问"（S7-1200/1500标准DB）
  或者 使用优化的块访问时，使用符号地址
□ 通信连接数未超过PLC上限
□ PLC程序中未锁定DB块访问
□ 防火墙允许TCP 102端口通信
```

> **重要**：S7-1200/1500的DB块默认启用"优化的块访问"，此模式下DB块没有固定偏移地址，**无法通过S7协议直接按偏移量读取**。解决方法：
> 1. 取消勾选"优化的块访问"（推荐用于上位机数据交换的DB块）
> 2. 或使用OPC UA方式访问（支持符号寻址）

---

## S7协议安全

### 通信安全现状

S7协议本身**不包含任何认证和加密机制**：

```
标准S7通信:
  上位机 ──── 明文传输 ──── PLC
  
  风险:
  - 任何人可以读取PLC数据
  - 任何人可以写入PLC数据
  - 无审计日志
```

### 安全加固建议

1. **网络隔离**：将PLC网络与办公网络物理隔离
2. **VPN隧道**：远程访问时通过VPN加密传输
3. **OPC UA代理**：通过OPC UA Server代理S7数据，利用OPC UA的安全机制
4. **防火墙规则**：限制TCP 102端口只允许特定IP访问
5. **S7-1500访问保护**：利用TIA Portal配置HMI访问权限

---

## 小结

| 知识点 | 要点 |
|--------|------|
| 协议基础 | 基于ISO-on-TCP，端口102，需COTP和S7两次握手 |
| PLC型号 | 支持1200/1500/300/400/200/200Smart全系列 |
| 寻址方式 | DB块.DBX/DBB/DBW/DBD + 偏移量，注意优化DB限制 |
| S7.Net库 | `Plc.Read(address)` 自动解析地址，`DataItem.FromAddress()` |
| 连接参数 | Rack和Slot因PLC型号而异，1200用0/0，1500用0/1 |
| 安全 | S7协议本身不安全，需网络层面保护 |
| 优化DB | S7-1200/1500需取消"优化的块访问"才能按偏移量读写 |

---

上一篇: [[Modbus协议详解]] | 下一篇: [[OPC标准与实现]]
