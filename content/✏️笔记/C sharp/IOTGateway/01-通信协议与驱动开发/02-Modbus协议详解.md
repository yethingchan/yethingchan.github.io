## 相关链接

- [[通信协议总览]] - 协议全景与选择指南
- [[串口通信基础]] - RS232/RS485物理层基础
- [[Siemens-S7协议]] - 另一种广泛使用的PLC协议
- [[自定义驱动开发指南]] - 基于Modbus驱动开发自己的驱动
- [[驱动插件架构]] - 插件加载与属性系统

---

## Modbus协议详解

Modbus是工业自动化领域应用最广泛的通信协议，由Modicon（现施耐德电气）于1979年发布。几乎所有PLC、仪表、传感器、变频器都支持Modbus。IoTGateway的Modbus驱动是所有驱动中功能最完善的，支持8种变体、完整的数据类型转换和字节序处理。

---

## Modbus协议体系

### 三种主要变体

```
Modbus 协议家族
│
├─ Modbus RTU (Remote Terminal Unit)
│   ├─ 二进制编码，高效紧凑
│   ├─ 基于RS232/RS485串口
│   ├─ CRC-16校验
│   └─ 工业现场最常用
│
├─ Modbus ASCII
│   ├─ ASCII字符编码，可读性好
│   ├─ 基于RS232/RS485串口
│   ├─ LRC校验
│   └─ 调试方便，效率较低
│
└─ Modbus TCP (Modbus/TCP)
    ├─ 二进制编码，封装在TCP/IP中
    ├─ 基于以太网
    ├─ 无额外校验（TCP保证可靠性）
    └─ 现代工厂首选
```

### IoTGateway支持的8种变体

IoTGateway的Modbus驱动是所有驱动中支持变体最多的：

```csharp
// DeviceModBusMaster.cs
[DriverSupported("TCP")]           // Modbus TCP
[DriverSupported("UDP")]           // Modbus over UDP
[DriverSupported("Rtu")]           // Modbus RTU (串口)
[DriverSupported("Rtu Over TCP")]  // RTU帧格式 + TCP传输
[DriverSupported("Rtu Over UDP")]  // RTU帧格式 + UDP传输
[DriverSupported("Ascii")]         // Modbus ASCII (串口)
[DriverSupported("Ascii Over TCP")]// ASCII帧格式 + TCP传输
[DriverSupported("Ascii Over UDP")]// ASCII帧格式 + UDP传输
```

这种灵活性使得IoTGateway可以对接几乎所有Modbus设备，无论其使用何种物理层。

---

## Modbus数据模型

### 四种数据区

Modbus协议定义了四种标准数据区，对应不同的功能码：

```
┌─────────────────────────────────────────────────────┐
│                  Modbus 数据模型                      │
├─────────────┬──────────┬─────────┬──────────────────┤
│   数据区     │ 访问类型  │ 功能码   │  地址范围         │
├─────────────┼──────────┼─────────┼──────────────────┤
│ 线圈(Coil)   │ 读/写    │ 01/05/15│ 00001-09999      │
│ 离散输入     │ 只读     │ 02      │ 10001-19999      │
│ 输入寄存器   │ 只读     │ 04      │ 30001-39999      │
│ 保持寄存器   │ 读/写    │ 03/06/16│ 40001-49999      │
└─────────────┴──────────┴─────────┴──────────────────┘
```

| 数据区 | 数据大小 | 读写 | 典型用途 |
|--------|---------|------|---------|
| 线圈 (Coils) | 1 bit | 读写 | 继电器控制、阀门开关 |
| 离散输入 (Discrete Inputs) | 1 bit | 只读 | 开关量输入、限位信号 |
| 输入寄存器 (Input Registers) | 16 bit | 只读 | 传感器测量值、仪表读数 |
| 保持寄存器 (Holding Registers) | 16 bit | 读写 | 参数设置、控制命令 |

### 寄存器寻址

Modbus有两种地址表示方式，容易混淆：

```
协议地址（从0开始）    PLC地址（从1开始，带前缀）
    0x0000              40001  (保持寄存器第1个)
    0x0001              40002  (保持寄存器第2个)
    0x0002              40003  (保持寄存器第3个)
    ...
    0x0063              40100  (保持寄存器第100个)

    0x0000              30001  (输入寄存器第1个)
    0x0000              00001  (线圈第1个)
    0x0000              10001  (离散输入第1个)
```

> **注意**：在NModbus4库中，地址从0开始。如果你查看PLC手册中写的"40001"，在代码中应使用地址"0"。

---

## Modbus帧格式

### Modbus RTU 帧结构

```
┌──────────┬────────────┬──────────┬──────────┬───────────┐
│ 帧间隔    │ 从站地址    │ 功能码   │ 数据      │ CRC校验   │
│ ≥3.5字符  │ 1 byte     │ 1 byte   │ N bytes  │ 2 bytes   │
│ (静默)    │ 0x01-0xF7  │          │          │ 低字节在前 │
└──────────┴────────────┴──────────┴──────────┴───────────┘
```

### Modbus TCP 帧结构（MBAP Header）

```
┌──────────────────────────────┬────────────┬──────────┬──────────┐
│ MBAP Header (7 bytes)        │ 单元标识符  │ 功能码   │ 数据     │
├───────┬───────┬───────┬─────┤  (1 byte)  │ (1 byte) │ (N bytes)│
│事务ID │协议ID │长度   │单元ID│           │          │          │
│2 bytes│2 bytes│2 bytes│1byte│           │          │          │
│0x0001 │0x0000 │       │     │           │          │          │
└───────┴───────┴───────┴─────┴────────────┴──────────┴──────────┘
```

### Modbus ASCII 帧结构

```
┌──────┬──────────┬──────────┬──────────┬──────────┬──────┐
│  ':' │ 从站地址  │ 功能码   │ 数据      │ LRC校验  │CR+LF │
│ 0x3A │ 2 chars  │ 2 chars  │ N chars  │ 2 chars  │      │
│(起始)│ (ASCII)  │ (ASCII)  │ (ASCII)  │ (ASCII)  │(结束)│
└──────┴──────────┴──────────┴──────────┴──────────┴──────┘

每字节的二进制数据用两个ASCII字符表示:
  0xAB → 'A' 'B' (0x41 0x42)
```

---

## 功能码详解

### 常用功能码

| 功能码 | 名称 | 操作 | 数据区 |
|--------|------|------|--------|
| 0x01 (01) | Read Coils | 读线圈 | 线圈 |
| 0x02 (02) | Read Discrete Inputs | 读离散输入 | 离散输入 |
| 0x03 (03) | Read Holding Registers | 读保持寄存器 | 保持寄存器 |
| 0x04 (04) | Read Input Registers | 读输入寄存器 | 输入寄存器 |
| 0x05 (05) | Write Single Coil | 写单个线圈 | 线圈 |
| 0x06 (06) | Write Single Register | 写单个寄存器 | 保持寄存器 |
| 0x0F (15) | Write Multiple Coils | 写多个线圈 | 线圈 |
| 0x10 (16) | Write Multiple Registers | 写多个寄存器 | 保持寄存器 |

### 功能码03读取示例

读取从站地址1的保持寄存器，从地址0开始读取2个寄存器：

```
请求帧（RTU）:
01 03 00 00 00 02 C4 0B
│  │  │     │     │
│  │  │     │     └─ CRC校验
│  │  │     └─ 读取数量: 2个寄存器
│  │  └─ 起始地址: 0x0000
│  └─ 功能码: 03 (读保持寄存器)
└─ 从站地址: 1

响应帧（RTU）:
01 03 04 00 64 00 C8 FA 33
│  │  │  │     │     │
│  │  │  │     │     └─ CRC校验
│  │  │  │     └─ 第2个寄存器: 0x00C8 (200)
│  │  │  └─ 第1个寄存器: 0x0064 (100)
│  │  └─ 字节数: 4 (2个寄存器 × 2字节)
│  └─ 功能码: 03
└─ 从站地址: 1
```

---

## IoTGateway Modbus驱动实现

### 驱动类结构

```csharp
// DeviceModBusMaster.cs 核心结构
[DriverSupported("TCP")]
// ... 其他DriverSupported
[DriverInfo("ModBusMaster", "V1.0.0", "Copyright IoTGateway.net 20230220")]
public class DeviceModBusMaster : IDriver
{
    // 传输层对象（三选一）
    private TcpClient? _tcpClient;
    private UdpClient? _udpClient;
    private SerialPort? _serialPort;
    
    // Modbus主站对象
    private ModbusMaster? _master;
    private SerialPortAdapter? _adapter;

    // 配置参数
    [ConfigParameter("主站类型")] public MasterType MasterType { get; set; }
    [ConfigParameter("IP地址")] public string IpAddress { get; set; }
    [ConfigParameter("端口号")] public int Port { get; set; } = 502;
    [ConfigParameter("串口名")] public string PortName { get; set; } = "COM1";
    [ConfigParameter("波特率")] public int BaudRate { get; set; } = 9600;
    [ConfigParameter("从站号")] public byte SlaveAddress { get; set; } = 1;
    // ...
}
```

### 连接建立

IoTGateway根据主站类型创建不同的传输层和Modbus主站：

```csharp
public bool Connect()
{
    switch (MasterType)
    {
        case MasterType.Tcp:
            // Modbus TCP: 直接TCP连接
            _tcpClient = new TcpClient(IpAddress, Port);
            _tcpClient.ReceiveTimeout = Timeout;
            _tcpClient.SendTimeout = Timeout;
            _master = ModbusIpMaster.CreateIp(_tcpClient);
            break;

        case MasterType.Udp:
            // Modbus over UDP
            _udpClient = new UdpClient(IpAddress, Port);
            _master = ModbusIpMaster.CreateIp(_udpClient);
            break;

        case MasterType.Rtu:
            // Modbus RTU: 串口通信
            _serialPort = new SerialPort(PortName, BaudRate, Parity, DataBits, StopBits);
            _serialPort.ReadTimeout = Timeout;
            _serialPort.WriteTimeout = Timeout;
            _serialPort.Open();
            _adapter = new SerialPortAdapter(_serialPort);
            _master = ModbusSerialMaster.CreateRtu(_adapter);
            break;

        case MasterType.RtuOnTcp:
            // RTU帧格式 + TCP传输（特殊场景）
            _tcpClient = new TcpClient(IpAddress, Port);
            _master = ModbusSerialMaster.CreateRtu(_tcpClient);
            break;
        // ... 其他变体类似
    }

    _master!.Transport.ReadTimeout = Timeout;
    _master!.Transport.WriteTimeout = Timeout;
    return IsConnected;
}
```

### 功能码01 - 读线圈

```csharp
[Method("功能码:01", description: "Coil读线圈")]
public DriverReturnValueModel Coil(DriverAddressIoArgModel ioArg)
{
    DriverReturnValueModel ret = new();
    if (IsConnected)
    {
        var (slaveAddress, ioAddress) = GetSlaveAddress(ioArg);
        // 读取1个线圈状态
        var retBool = _master.ReadCoils(slaveAddress, ushort.Parse(ioAddress), 1)[0];
        
        if (ioArg.ValueType == DataTypeEnum.Bit)
            ret.Value = retBool ? 1 : 0;
        else
            ret.Value = retBool;
            
        ret.StatusType = VaribaleStatusTypeEnum.Good;
    }
    return ret;
}
```

### 功能码03 - 读保持寄存器

这是最常用的读取方法，支持多种数据类型：

```csharp
[Method("功能码:03", description: "HoldingRegisters读保持寄存器")]
public DriverReturnValueModel HoldingRegisters(DriverAddressIoArgModel ioArg)
{
    DriverReturnValueModel ret = new();
    if (IsConnected)
        ret = ReadRegistersBuffers(3, ioArg);  // 功能码3
    return ret;
}

// 核心读取逻辑
private DriverReturnValueModel ReadRegistersBuffers(byte funCode, 
    DriverAddressIoArgModel ioArg)
{
    // 1. 解析地址和读取数量
    AnalyzeAddress(ioArg, out ushort startAddress, out ushort count);
    var (slaveAddress, ioAddress) = GetSlaveAddress(ioArg);

    // 2. 读取原始寄存器数据
    ushort[] rawBuffers;
    if (funCode == 3)
        rawBuffers = _master.ReadHoldingRegisters(slaveAddress, startAddress, count);
    else
        rawBuffers = _master.ReadInputRegisters(slaveAddress, startAddress, count);

    // 3. 字节序转换
    ushort[] retBuffers = ChangeBuffersOrder(rawBuffers, ioArg.EndianType);

    // 4. 根据数据类型解析
    if (ioArg.ValueType == DataTypeEnum.Uint16)
        ret.Value = retBuffers[0];
    else if (ioArg.ValueType == DataTypeEnum.Int16)
        ret.Value = (short)retBuffers[0];
    else if (ioArg.ValueType == DataTypeEnum.Float)
    {
        var bytes = new[] {
            (byte)(retBuffers[1] & 0xff), (byte)((retBuffers[1] >> 8) & 0xff),
            (byte)(retBuffers[0] & 0xff), (byte)((retBuffers[0] >> 8) & 0xff)
        };
        ret.Value = BitConverter.ToSingle(bytes, 0);
    }
    // ... 更多类型
}
```

### 数据写入

IoTGateway的Modbus驱动支持多种数据类型的写入：

```csharp
public async Task<RpcResponse> WriteAsync(string requestId, string method,
    DriverAddressIoArgModel ioArg)
{
    if (method == nameof(HoldingRegisters))
    {
        switch (ioArg.ValueType)
        {
            case DataTypeEnum.Float:
                var f = float.Parse(ioArg.Value.ToString());
                var fValue = BitConverter.SingleToUInt32Bits(f);
                shortArray[1] = (ushort)(fValue & 0xffff);
                shortArray[0] = (ushort)(fValue >> 16 & 0xffff);
                toWriteArray = ChangeBuffersOrder(shortArray, ioArg.EndianType);
                await _master.WriteMultipleRegistersAsync(slaveAddress, address, toWriteArray);
                break;

            case DataTypeEnum.Int16:
                shortArray[0] = (ushort)short.Parse(ioArg.Value.ToString());
                toWriteArray = ChangeBuffersOrder(shortArray, ioArg.EndianType);
                await _master.WriteSingleRegisterAsync(slaveAddress, address, toWriteArray[0]);
                break;

            case DataTypeEnum.Int32:
                var int32Value = int.Parse(ioArg.Value.ToString());
                shortArray[1] = (ushort)(int32Value & 0xffff);
                shortArray[0] = (ushort)(int32Value >> 16 & 0xffff);
                toWriteArray = ChangeBuffersOrder(shortArray, ioArg.EndianType);
                await _master.WriteMultipleRegistersAsync(slaveAddress, address, toWriteArray);
                break;
            // ...
        }
    }
}
```

---

## 字节序（Endianness）处理

### 字节序问题

Modbus协议规定16位寄存器采用**大端序**（Big-Endian），即高字节在前。但32位和64位数据需要多个寄存器组合，不同厂商的寄存器排列方式不同。

IoTGateway定义了四种字节序模式：

```csharp
public enum EndianEnum
{
    None,            // 不转换
    BigEndian,       // ABCD - 大端序（默认）
    LittleEndian,    // DCBA - 小端序
    BigEndianSwap,   // BADC - 大端交换
    LittleEndianSwap // CDAB - 小端交换（Word Swap）
}
```

### 32位数据的字节序示意

假设一个Float值 123.456 的IEEE 754表示为 `0x42F6E979`：

```
字节标记:  A    B    C    D
原始值:   0x42 0xF6 0xE9 0x79

寄存器1(高): 0x42F6    寄存器2(低): 0xE979

四种排列方式:
┌──────────────┬───────────────┬─────────────────┐
│ 字节序       │ 寄存器排列     │ 字节排列         │
├──────────────┼───────────────┼─────────────────┤
│ ABCD(大端)   │ Reg1=42F6     │ 42 F6 E9 79     │
│              │ Reg2=E979     │                 │
├──────────────┼───────────────┼─────────────────┤
│ DCBA(小端)   │ Reg1=79E9     │ 79 E9 F6 42     │
│              │ Reg2=F642     │                 │
├──────────────┼───────────────┼─────────────────┤
│ BADC(大端交换)│ Reg1=F642     │ F6 42 79 E9     │
│              │ Reg2=79E9     │                 │
├──────────────┼───────────────┼─────────────────┤
│ CDAB(小端交换)│ Reg1=E979     │ E9 79 42 F6     │
│              │ Reg2=42F6     │                 │
└──────────────┴───────────────┴─────────────────┘
```

### 字节序转换实现

```csharp
// DeviceModBusMaster.cs - 32位字节序转换
private ushort[] ChangeBuffersOrder(ushort[] buffers, EndianEnum dataType)
{
    if (datalen == 2) // 32位数据 = 2个寄存器
    {
        var ab = BitConverter.GetBytes(buffers[0]);
        var cd = BitConverter.GetBytes(buffers[1]);
        var _ab = new byte[2];
        var _cd = new byte[2];
        
        switch (dataType)
        {
            case EndianEnum.BigEndian: // ABCD
                _ab = ab;
                _cd = cd;
                break;
            case EndianEnum.LittleEndian: // DCBA
                _ab[0] = cd[1]; _ab[1] = cd[0];
                _cd[0] = ab[1]; _cd[1] = ab[0];
                break;
            case EndianEnum.BigEndianSwap: // BADC
                _ab[0] = ab[1]; _ab[1] = ab[0];
                _cd[0] = cd[1]; _cd[1] = cd[0];
                break;
            case EndianEnum.LittleEndianSwap: // CDAB
                _ab[0] = cd[0]; _ab[1] = cd[1];
                _cd[0] = ab[0]; _cd[1] = ab[1];
                break;
        }
        newBuffers[0] = BitConverter.ToUInt16(_ab, 0);
        newBuffers[1] = BitConverter.ToUInt16(_cd, 0);
    }
}
```

> **实践提示**：如果你读取的Float值明显不对（如温度应该是25.3却读出了一个大得离谱的值），90%的可能性是字节序设置错误。尝试切换四种字节序模式通常可以解决。

---

## 多地址批量读取与缓存

### 批量读取优化

IoTGateway实现了一种高效的"批量读取 + 缓存"机制，减少通信次数：

```csharp
// 第一步：批量读取多个连续寄存器到缓存
[Method("多地址读取", description: "多地址读取缓存")]
public DriverReturnValueModel ReadMultiple(DriverAddressIoArgModel ioArg)
{
    // 地址格式: "功能码,起始地址,读取数量,缓存键名"
    // 例: "3,0,100,cache1" 表示功能码03,从地址0开始读100个寄存器
    var args = ioArg.Address.Split(',');
    var func = args[0];
    var startAddress = ushort.Parse(args[1]);
    var length = ushort.Parse(args[2]);
    var cacheKey = args[3];
    
    switch (func)
    {
        case "3":
            var holdingRs = _master.ReadHoldingRegisters(slaveId, startAddress, length);
            _cache[cacheKey] = holdingRs;
            _cacheType[cacheKey] = "ushort";
            break;
        // ...
    }
}

// 第二步：从缓存中按需读取特定地址
[Method("从缓存读取", description: "从缓存读取")]
public DriverReturnValueModel ReadFromCache(DriverAddressIoArgModel ioArg)
{
    // 直接从内存缓存读取，无需再次通信
    var cacheBuffers = (ushort[])_cache[cacheName];
    var value = cacheBuffers.Skip(startIndex).Take(wordLen).ToArray();
    // 类型转换...
}
```

### 灵活从站地址

IoTGateway支持在同一条总线上读取不同从站号的数据：

```csharp
// 地址格式: "从站号|寄存器地址"
// 例: "2|100" 表示从站号2的地址100
private (byte slaveAddress, string ioAddress) GetSlaveAddress(
    DriverAddressIoArgModel ioArg)
{
    byte slaveAddress = SlaveAddress; // 默认从站号
    string ioAddress = ioArg.Address;
    
    if (ioArg.Address.Contains('|'))
    {
        slaveAddress = byte.Parse(ioArg.Address.Split('|')[0]);
        ioAddress = ioArg.Address.Split('|')[1];
    }
    return (slaveAddress, ioAddress);
}
```

---

## 寄存器数量与数据类型

### 数据类型占用的寄存器数

```
┌──────────────────┬──────────┬──────────────────┐
│ 数据类型          │ 字节数   │ 占用寄存器数      │
├──────────────────┼──────────┼──────────────────┤
│ Bool / Bit       │ 1 bit    │ 使用线圈/离散输入  │
│ Int16 / UInt16   │ 2 bytes  │ 1个寄存器          │
│ Int32 / UInt32   │ 4 bytes  │ 2个寄存器          │
│ Float            │ 4 bytes  │ 2个寄存器          │
│ Int64 / UInt64   │ 8 bytes  │ 4个寄存器          │
│ Double           │ 8 bytes  │ 4个寄存器          │
│ BCD16            │ 2 bytes  │ 1个寄存器          │
│ BCD32            │ 4 bytes  │ 2个寄存器          │
│ ASCII String     │ N bytes  │ N/2个寄存器        │
└──────────────────┴──────────┴──────────────────┘
```

IoTGateway中根据数据类型自动计算读取数量：

```csharp
private ushort GetModbusReadCount(uint functionCode, DataTypeEnum dataType)
{
    if (dataType.ToString().Contains("32") || dataType.ToString().Contains("Float"))
        return 2;
    if (dataType.ToString().Contains("64") || dataType.ToString().Contains("Double"))
        return 4;
    return 1;
}
```

---

## Modbus调试技巧

### 使用Modbus Poll/Slave测试

1. **Modbus Slave**：模拟从站设备
   - 设置从站ID、功能码、寄存器数量
   - 手动设置寄存器值用于测试

2. **Modbus Poll**：模拟主站读取
   - 设置连接参数（IP/串口）
   - 设置读取地址和数据类型

### Wireshark抓包分析Modbus TCP

```
Modbus TCP 抓包示例:

No.  Source      Dest      Protocol  Info
1    192.168.1.10 192.168.1.20 Modbus  Query: Read Holding Registers
2    192.168.1.20 192.168.1.10 Modbus  Response: Read Holding Registers

展开Modbus层:
  Modbus/TCP
    MBAP Header
      Transaction Identifier: 0x0001
      Protocol Identifier: 0x0000 (Modbus)
      Length: 6
      Unit Identifier: 1
    Modbus PDU
      Function Code: Read Holding Registers (0x03)
      Reference Number: 0
      Word Count: 2
```

### 常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| 异常码01 | 功能码不支持 | 确认设备支持该功能码 |
| 异常码02 | 地址越界 | 检查寄存器地址范围 |
| 异常码03 | 数据值无效 | 检查写入值范围 |
| 超时 | 通信不通 | 检查网络/串口连接 |
| 读取值全0 | 地址错误 | 确认0基址还是1基址 |
| Float值异常 | 字节序错误 | 切换四种字节序尝试 |

---

## 小结

| 知识点 | 要点 |
|--------|------|
| 三种变体 | RTU(串口二进制)、ASCII(串口字符)、TCP(以太网) |
| 四种数据区 | 线圈、离散输入、输入寄存器、保持寄存器 |
| 功能码 | 01/02读位, 03/04读寄存器, 05/06写单个, 0F/10写多个 |
| 字节序 | 32位以上数据需注意字节序，四种模式：ABCD/DCBA/BADC/CDAB |
| IoTGateway实现 | 8种变体支持、缓存批量读取、灵活从站地址 |
| NModbus4库 | `ModbusIpMaster.CreateIp()` 和 `ModbusSerialMaster.CreateRtu()` |

---

上一篇: [[串口通信基础]] | 下一篇: [[Siemens-S7协议]]
