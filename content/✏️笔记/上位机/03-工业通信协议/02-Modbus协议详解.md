# 02 - Modbus协议详解

---

## 一、知识讲解

### 1.1 Modbus 协议概述

#### 发展历史

Modbus 协议由 Modicon 公司（现施耐德电气）于 1979 年发布，是最早的工业通信协议之一。由于其开放、免费、简单可靠的特点，Modbus 成为工业自动化领域应用最广泛的通信协议，至今仍然是事实上的工业通信标准。

#### 核心特点

- **主从架构（Master-Slave）**：主站发起请求，从站被动响应
- **开放免费**：无授权费用，任何人都可以实现
- **简单可靠**：帧结构简洁，实现门槛低
- **跨平台**：不依赖特定硬件或操作系统
- **广泛支持**：几乎所有PLC、变频器、仪表都支持Modbus

#### 应用场景

| 场景 | 说明 |
|------|------|
| PLC数据采集 | 上位机读取PLC的寄存器数据（温度、压力、速度等） |
| 变频器控制 | 通过Modbus设置变频器的频率、启停 |
| 仪表读取 | 读取电力仪表、流量计、温控仪等的数据 |
| 设备联网 | 将不支持以太网的设备通过串口网关接入网络 |
| SCADA系统 | 作为SCADA系统的底层通信协议 |

### 1.2 Modbus RTU vs Modbus TCP

| 特性 | Modbus RTU | Modbus TCP |
|------|-----------|------------|
| 传输层 | 串行口（RS232/RS485） | TCP/IP以太网 |
| 编码方式 | 二进制（RTU模式） | 二进制（RTU模式） |
| 帧结构 | [地址][功能码][数据][CRC] | [MBAP头][功能码][数据] |
| 校验方式 | CRC16 | TCP自身校验（不需要CRC） |
| 最大从站数 | 247（1个RS485总线） | 无限制（网络设备） |
| 传输距离 | RS485最长1200米 | 无限制（网络范围） |
| 传输速率 | 通常9600~115200bps | 10Mbps~1Gbps |
| 通信方式 | 主从轮询 | 主从轮询（基于TCP连接） |
| 适用场景 | 现场设备、短距离、少量设备 | 车间级、远距离、多设备 |

#### 选型建议

- **优先选 Modbus TCP**：如果设备支持以太网，优先使用TCP，速率快、布线灵活、不需要CRC计算
- **Modbus RTU 适用场景**：设备只有串口接口、RS485长距离传输、抗干扰要求高的环境
- **混合使用**：可以通过"串口服务器/协议网关"将RTU设备转换到TCP网络

### 1.3 Modbus TCP 帧结构详解

Modbus TCP 在 Modbus RTU 的 PDU（协议数据单元）前面增加了 MBAP 头部。

```
 Modbus TCP 帧结构（共 N 字节）:
 ┌──────────┬──────────┬─────┬───────────┬─────┐
 │ 事务ID   │ 协议ID   │ 长度 │  单元ID    │ PDU │
 │ 2字节    │ 2字节    │ 2字节│  1字节    │ N字节│
 └──────────┴──────────┴─────┴───────────┴─────┘
  └────── MBAP头(7字节) ──────┘  └─── PDU ───┘
```

#### MBAP 头部各字段

| 字段 | 长度 | 说明 |
|------|------|------|
| 事务标识（Transaction ID） | 2字节 | 由主站生成，用于匹配请求和响应。每次请求递增即可。 |
| 协议标识（Protocol ID） | 2字节 | Modbus协议固定为 `0x0000` |
| 长度（Length） | 2字节 | 后续字节的数量 = 单元ID(1) + PDU长度 |
| 单元标识（Unit ID） | 1字节 | 对应RTU中的从站地址，通过网关时用于路由 |

#### 示例：读保持寄存器请求

```
请求帧（读从站01，地址0x0000，读10个寄存器）：
事务ID: 00 01          （事务编号1）
协议ID: 00 00          （Modbus协议）
长度:   00 06          （后续6字节：单元ID + 功能码 + 起始地址 + 数量）
单元ID: 01             （从站地址1）
功能码: 03             （读保持寄存器）
起始地址: 00 00        （寄存器地址0）
数量:   00 0A          （读取10个寄存器）
───────────────────
完整帧: 00 01 00 00 00 06 01 03 00 00 00 0A
```

```
正常响应帧：
事务ID: 00 01          （与请求匹配）
协议ID: 00 00
长度:   00 17          （后续23字节）
单元ID: 01
功能码: 03
字节数: 14             （20字节数据 = 10个寄存器 x 2字节）
数据:   00 64 01 F4 02 BC 03 84 ... （10个寄存器值）
```

### 1.4 Modbus RTU 帧结构详解

```
 Modbus RTU 帧结构:
 ┌──────┬──────┬──────┬──────┬──────┐
 │ 地址  │功能码│ 数据  │ CRC_L│ CRC_H│
 │ 1字节 │ 1字节│ N字节 │ 1字节 │ 1字节│
 └──────┴──────┴──────┴──────┴──────┘
```

| 字段 | 长度 | 说明 |
|------|------|------|
| 地址 | 1字节 | 从站地址（1-247），0为广播地址 |
| 功能码 | 1字节 | 指定执行的操作类型 |
| 数据 | N字节 | 根据功能码不同而不同 |
| CRC | 2字节 | CRC16校验码（低字节在前，高字节在后） |

#### 示例：读保持寄存器请求

```
请求帧（读从站01，地址0x0000，读10个寄存器）：
完整帧: 01 03 00 00 00 0A C5 CD
         │  │  │     │     │     └── CRC高字节
         │  │  │     │     └────── CRC低字节
         │  │  │     └────────── 读取数量（10）
         │  │  └──────────────── 起始地址高字节(00) + 低字节(00)
         │  └─────────────────── 功能码03
         └────────────────────── 从站地址01
```

#### 示例：读保持寄存器响应

```
响应帧：01 03 14 00 64 01 F4 02 BC 03 84 ... [CRC_L] [CRC_H]
         │  │  │  │     │     │
         │  │  │  └── 数据开始（寄存器值，每个2字节）
         │  │  └───── 字节数（0x14 = 20字节 = 10个寄存器 x 2字节）
         │  └──────── 功能码03
         └─────────── 从站地址01
```

### 1.5 CRC16 校验计算

Modbus RTU 使用 CRC-16 校验（多项式 0x8005，初始值 0xFFFF）。

```csharp
/// <summary>
/// Modbus CRC16 校验计算
/// 算法：CRC-16/Modbus
/// 多项式：0xA001（反转后的0x8005）
/// 初始值：0xFFFF
/// 输入反转：否
/// 输出反转：是
/// 结果异或值：0x0000
/// </summary>
public static ushort CalculateCRC16(byte[] data, int offset, int length)
{
    ushort crc = 0xFFFF;  // CRC初始值

    for (int i = offset; i < offset + length; i++)
    {
        crc ^= data[i];    // 将当前字节与CRC异或

        // 对每一位进行处理
        for (int j = 0; j < 8; j++)
        {
            if ((crc & 0x0001) != 0)    // 检查最低位
            {
                crc >>= 1;               // 右移一位
                crc ^= 0xA001;          // 与多项式异或
            }
            else
            {
                crc >>= 1;               // 仅右移一位
            }
        }
    }

    return crc;
}

/// <summary>
/// 验证CRC校验是否通过
/// </summary>
public static bool ValidateCRC16(byte[] frame)
{
    if (frame.Length < 3) return false;

    int dataLength = frame.Length - 2;
    ushort calculatedCRC = CalculateCRC16(frame, 0, dataLength);
    ushort receivedCRC = (ushort)((frame[frame.Length - 1] << 8) | frame[frame.Length - 2]);

    return calculatedCRC == receivedCRC;
}

/// <summary>
/// 为数据帧追加CRC（原地修改数组或在末尾添加CRC）
/// </summary>
public static byte[] AppendCRC16(byte[] data)
{
    ushort crc = CalculateCRC16(data, 0, data.Length);
    byte[] result = new byte[data.Length + 2];
    Array.Copy(data, result, data.Length);
    result[data.Length] = (byte)(crc & 0xFF);         // CRC低字节在前
    result[data.Length + 1] = (byte)((crc >> 8) & 0xFF); // CRC高字节在后
    return result;
}
```

### 1.6 所有功能码详解

#### 位操作（读写单个位）

| 功能码 | 名称 | 操作 | 数据区（请求） | 数据区（响应） |
|--------|------|------|----------------|----------------|
| **01** | 读线圈 | 读取DO状态 | 起始地址(2B) + 数量(2B) | 字节数 + 线圈状态 |
| **02** | 读离散输入 | 读取DI状态 | 起始地址(2B) + 数量(2B) | 字节数 + 输入状态 |
| **05** | 写单个线圈 | 设置单个DO | 线圈地址(2B) + 值(2B) | 线圈地址(2B) + 值(2B) |
| **15(0x0F)** | 写多个线圈 | 批量设置DO | 起始地址(2B) + 数量(2B) + 字节数 + 数据 | 起始地址(2B) + 数量(2B) |

#### 寄存器操作（读写16位值）

| 功能码 | 名称 | 操作 | 数据区（请求） | 数据区（响应） |
|--------|------|------|----------------|----------------|
| **03** | 读保持寄存器 | 读取 Holding Register | 起始地址(2B) + 数量(2B) | 字节数 + 寄存器数据 |
| **04** | 读输入寄存器 | 读取 Input Register | 起始地址(2B) + 数量(2B) | 字节数 + 寄存器数据 |
| **06** | 写单个寄存器 | 设置单个寄存器 | 地址(2B) + 值(2B) | 地址(2B) + 值(2B) |
| **16(0x10)** | 写多个寄存器 | 批量设置寄存器 | 起始地址(2B) + 数量(2B) + 字节数 + 数据 | 起始地址(2B) + 数量(2B) |

#### 四种寄存器类型说明

| 寄存器类型 | 功能码 | 可读 | 可写 | 用途 |
|-----------|--------|------|------|------|
| 线圈（Coil） | 01/05/15 | 是 | 是 | 数字量输出（DO） |
| 离散输入（Discrete Input） | 02 | 是 | 否 | 数字量输入（DI） |
| 输入寄存器（Input Register） | 04 | 是 | 否 | 模拟量输入（AI），如温度值 |
| 保持寄存器（Holding Register） | 03/06/16 | 是 | 是 | 模拟量输入输出，如设定值、实际值 |

#### 功能码 01/02 详细说明：位读取

```
请求：01 01 00 00 00 08 [CRC]   （读取从站1，地址0，8个线圈）
响应：01 01 01 AC               （返回1字节数据，AC=10101100，8位对应8个线圈）

位解析（低位在前）：
AC = 1010 1100
Bit0=0, Bit1=0, Bit2=1, Bit3=1, Bit4=0, Bit5=1, Bit6=0, Bit7=1
```

#### 功能码 05/06 详细说明：单点写入

```
功能码05（写单个线圈）：
请求：01 05 00 03 FF 00 [CRC]   （设置从站1的线圈3为ON）
响应：01 05 00 03 FF 00 [CRC]   （ echoes 回请求）
      FF 00 = ON,  00 00 = OFF

功能码06（写单个寄存器）：
请求：01 06 00 01 03 E8 [CRC]   （向从站1的寄存器1写入1000）
响应：01 06 00 01 03 E8 [CRC]   （ echoes 回请求）
```

#### 功能码 15/16 详细说明：多点写入

```
功能码15（写多个线圈）：
请求：01 0F 00 00 00 0A 02 03 AC [CRC]
      起始地址=0, 数量=10(0x0A), 字节数=2, 数据=03 AC
响应：01 0F 00 00 00 0A [CRC]   （返回起始地址和写入数量）

功能码16（写多个寄存器）：
请求：01 10 00 00 00 02 04 03 E8 01 F4 [CRC]
      起始地址=0, 数量=2, 字节数=4, 数据=[1000, 500]
响应：01 10 00 00 00 02 [CRC]   （返回起始地址和写入数量）
```

### 1.7 异常码含义

当从站收到无效请求时，会将功能码的最高位置1并返回异常码。

```
异常响应格式：
[从站地址] [功能码 | 0x80] [异常码] [CRC]
```

| 异常码 | 含义 | 常见原因 |
|--------|------|----------|
| **01** | 非法功能码 | 从站不支持该功能码 |
| **02** | 非法数据地址 | 请求的寄存器/线圈地址不存在 |
| **03** | 非法数据值 | 写入的值超出范围或数量不合法 |
| **04** | 从站设备故障 | 从站执行命令时发生硬件故障 |
| **05** | 确认（需较长时间处理） | 从站已收到请求，但需要较长时间处理 |
| **06** | 从站设备忙 | 从站正在处理其他请求 |

```
示例异常响应：
01 83 02 [CRC]    → 从站01，功能码03异常，异常码02（非法数据地址）
01 86 02 [CRC]    → 从站01，功能码06异常，异常码02（非法数据地址）
```

---

## 二、代码示例

### 2.1 Modbus TCP 帧构造工具类

```csharp
using System;
using System.Text;

/// <summary>
/// Modbus TCP 帧构造与解析工具类
/// 手动构造Modbus TCP帧，理解协议原理
/// </summary>
public class ModbusTcpFrameBuilder
{
    private ushort _transactionId = 0;  // 事务ID，每次请求自动递增

    /// <summary>
    /// 构造Modbus TCP读保持寄存器请求帧
    /// </summary>
    /// <param name="unitId">从站地址</param>
    /// <param name="startAddress">起始地址</param>
    /// <param name="quantity">读取数量</param>
    /// <returns>完整的Modbus TCP帧字节数组</returns>
    public byte[] BuildReadHoldingRegisters(byte unitId, ushort startAddress, ushort quantity)
    {
        // PDU部分：功能码(1) + 起始地址(2) + 数量(2) = 5字节
        byte[] pdu = new byte[5];
        pdu[0] = 0x03;  // 功能码：读保持寄存器
        pdu[1] = (byte)(startAddress >> 8);     // 起始地址高字节
        pdu[2] = (byte)(startAddress & 0xFF);   // 起始地址低字节
        pdu[3] = (byte)(quantity >> 8);         // 数量高字节
        pdu[4] = (byte)(quantity & 0xFF);       // 数量低字节

        return BuildTcpFrame(unitId, pdu);
    }

    /// <summary>
    /// 构造Modbus TCP读输入寄存器请求帧（功能码04）
    /// </summary>
    public byte[] BuildReadInputRegisters(byte unitId, ushort startAddress, ushort quantity)
    {
        byte[] pdu = new byte[5];
        pdu[0] = 0x04;
        pdu[1] = (byte)(startAddress >> 8);
        pdu[2] = (byte)(startAddress & 0xFF);
        pdu[3] = (byte)(quantity >> 8);
        pdu[4] = (byte)(quantity & 0xFF);

        return BuildTcpFrame(unitId, pdu);
    }

    /// <summary>
    /// 构造Modbus TCP写单个寄存器请求帧（功能码06）
    /// </summary>
    public byte[] BuildWriteSingleRegister(byte unitId, ushort address, ushort value)
    {
        byte[] pdu = new byte[5];
        pdu[0] = 0x06;
        pdu[1] = (byte)(address >> 8);
        pdu[2] = (byte)(address & 0xFF);
        pdu[3] = (byte)(value >> 8);
        pdu[4] = (byte)(value & 0xFF);

        return BuildTcpFrame(unitId, pdu);
    }

    /// <summary>
    /// 构造Modbus TCP写多个寄存器请求帧（功能码16）
    /// </summary>
    public byte[] BuildWriteMultipleRegisters(
        byte unitId, ushort startAddress, ushort[] values)
    {
        int byteCount = values.Length * 2;
        byte[] pdu = new byte[6 + byteCount];
        pdu[0] = 0x10;                              // 功能码
        pdu[1] = (byte)(startAddress >> 8);          // 起始地址高
        pdu[2] = (byte)(startAddress & 0xFF);        // 起始地址低
        pdu[3] = (byte)(values.Length >> 8);          // 数量高
        pdu[4] = (byte)(values.Length & 0xFF);        // 数量低
        pdu[5] = (byte)byteCount;                    // 字节数

        // 写入寄存器值（每个寄存器2字节，高字节在前）
        for (int i = 0; i < values.Length; i++)
        {
            pdu[6 + i * 2] = (byte)(values[i] >> 8);
            pdu[6 + i * 2 + 1] = (byte)(values[i] & 0xFF);
        }

        return BuildTcpFrame(unitId, pdu);
    }

    /// <summary>
    /// 构造Modbus TCP读线圈请求帧（功能码01）
    /// </summary>
    public byte[] BuildReadCoils(byte unitId, ushort startAddress, ushort quantity)
    {
        byte[] pdu = new byte[5];
        pdu[0] = 0x01;
        pdu[1] = (byte)(startAddress >> 8);
        pdu[2] = (byte)(startAddress & 0xFF);
        pdu[3] = (byte)(quantity >> 8);
        pdu[4] = (byte)(quantity & 0xFF);

        return BuildTcpFrame(unitId, pdu);
    }

    /// <summary>
    /// 构造Modbus TCP写单个线圈请求帧（功能码05）
    /// </summary>
    public byte[] BuildWriteSingleCoil(byte unitId, ushort address, bool value)
    {
        byte[] pdu = new byte[5];
        pdu[0] = 0x05;
        pdu[1] = (byte)(address >> 8);
        pdu[2] = (byte)(address & 0xFF);
        // FF00 = ON, 0000 = OFF
        pdu[3] = value ? (byte)0xFF : (byte)0x00;
        pdu[4] = 0x00;

        return BuildTcpFrame(unitId, pdu);
    }

    /// <summary>
    /// 通用MBAP头+PDU 组装方法
    /// MBAP头：事务ID(2) + 协议ID(2) + 长度(2) + 单元ID(1) = 7字节
    /// </summary>
    private byte[] BuildTcpFrame(byte unitId, byte[] pdu)
    {
        _transactionId++;  // 事务ID自增

        byte[] frame = new byte[7 + pdu.Length];
        // MBAP头
        frame[0] = (byte)(_transactionId >> 8);     // 事务ID高字节
        frame[1] = (byte)(_transactionId & 0xFF);   // 事务ID低字节
        frame[2] = 0x00;                             // 协议ID高字节（固定0x00）
        frame[3] = 0x00;                             // 协议ID低字节（固定0x00）
        frame[4] = (byte)((1 + pdu.Length) >> 8);   // 长度高字节
        frame[5] = (byte)((1 + pdu.Length) & 0xFF); // 长度低字节
        frame[6] = unitId;                           // 单元ID

        // PDU
        Array.Copy(pdu, 0, frame, 7, pdu.Length);

        return frame;
    }
}
```

### 2.2 Modbus TCP 帧解析类

```csharp
using System;
using System.Collections.Generic;

/// <summary>
/// Modbus TCP 响应帧解析器
/// </summary>
public class ModbusTcpFrameParser
{
    /// <summary>
    /// 解析Modbus TCP响应帧
    /// </summary>
    public ModbusResponse Parse(byte[] frame)
    {
        if (frame.Length < 8)
            throw new Exception($"帧长度不足: {frame.Length}，最小需要8字节");

        var response = new ModbusResponse();

        // ===== 解析MBAP头 =====
        response.TransactionId = (ushort)((frame[0] << 8) | frame[1]);
        response.ProtocolId = (ushort)((frame[2] << 8) | frame[3]);
        response.Length = (ushort)((frame[4] << 8) | frame[5]);
        response.UnitId = frame[6];

        // ===== 解析PDU =====
        response.FunctionCode = frame[7];

        // 检查是否为异常响应
        if ((response.FunctionCode & 0x80) != 0)
        {
            response.IsError = true;
            response.ErrorCode = frame[8];
            response.ErrorMessage = GetExceptionMessage(response.ErrorCode);
            return response;
        }

        // 根据功能码解析数据
        switch (response.FunctionCode)
        {
            case 0x01:  // 读线圈
            case 0x02:  // 读离散输入
                ParseBitResponse(frame, response);
                break;

            case 0x03:  // 读保持寄存器
            case 0x04:  // 读输入寄存器
                ParseRegisterResponse(frame, response);
                break;

            case 0x05:  // 写单个线圈
            case 0x06:  // 写单个寄存器
                ParseWriteSingleResponse(frame, response);
                break;

            case 0x0F:  // 写多个线圈
            case 0x10:  // 写多个寄存器
                ParseWriteMultipleResponse(frame, response);
                break;
        }

        return response;
    }

    /// <summary>
    /// 解析位操作响应（功能码01/02）
    /// </summary>
    private void ParseBitResponse(byte[] frame, ModbusResponse response)
    {
        int byteCount = frame[8];
        response.Values = new List<object>();

        for (int i = 0; i < byteCount; i++)
        {
            byte b = frame[9 + i];
            for (int bit = 0; bit < 8; bit++)
            {
                response.Values.Add((b >> bit) & 0x01 == 1);
            }
        }
    }

    /// <summary>
    /// 解析寄存器响应（功能码03/04）
    /// </summary>
    private void ParseRegisterResponse(byte[] frame, ModbusResponse response)
    {
        int byteCount = frame[8];
        int registerCount = byteCount / 2;
        response.Values = new List<object>();

        for (int i = 0; i < registerCount; i++)
        {
            ushort value = (ushort)((frame[9 + i * 2] << 8) | frame[10 + i * 2]);
            response.Values.Add(value);
        }
    }

    /// <summary>
    /// 解析写单个响应（功能码05/06）—— echoes回请求
    /// </summary>
    private void ParseWriteSingleResponse(byte[] frame, ModbusResponse response)
    {
        ushort address = (ushort)((frame[8] << 8) | frame[9]);
        ushort value = (ushort)((frame[10] << 8) | frame[11]);
        response.Values = new List<object> { address, value };
    }

    /// <summary>
    /// 解析写多个响应（功能码15/16）—— 返回起始地址和数量
    /// </summary>
    private void ParseWriteMultipleResponse(byte[] frame, ModbusResponse response)
    {
        ushort address = (ushort)((frame[8] << 8) | frame[9]);
        ushort quantity = (ushort)((frame[10] << 8) | frame[11]);
        response.Values = new List<object> { address, quantity };
    }

    /// <summary>
    /// 获取异常码对应的中文说明
    /// </summary>
    private string GetExceptionMessage(byte errorCode)
    {
        return errorCode switch
        {
            0x01 => "非法功能码：从站不支持该功能",
            0x02 => "非法数据地址：请求的地址超出范围",
            0x03 => "非法数据值：写入的值无效",
            0x04 => "从站设备故障",
            0x05 => "确认：请求已收到，处理中",
            0x06 => "从站设备忙",
            _ => $"未知异常码: 0x{errorCode:X2}"
        };
    }
}

/// <summary>
/// Modbus响应数据结构
/// </summary>
public class ModbusResponse
{
    // MBAP头
    public ushort TransactionId { get; set; }
    public ushort ProtocolId { get; set; }
    public ushort Length { get; set; }
    public byte UnitId { get; set; }

    // PDU
    public byte FunctionCode { get; set; }
    public bool IsError { get; set; } = false;
    public byte ErrorCode { get; set; }
    public string ErrorMessage { get; set; }

    // 解析结果
    public List<object> Values { get; set; }
}
```

### 2.3 上位机场景案例：手动构造Modbus TCP帧发送与解析

```csharp
using System;
using System.Net.Sockets;
using System.Threading;

/// <summary>
/// 上位机案例：手动构造Modbus TCP帧与PLC通信
/// 场景：不依赖第三方库，纯手动构造和解析Modbus TCP帧
/// 适用：理解协议原理、嵌入式设备、特殊定制场景
/// </summary>
public class ModbusTcpManualExample
{
    private TcpClient _tcpClient;
    private NetworkStream _stream;
    private ModbusTcpFrameBuilder _builder;
    private ModbusTcpFrameParser _parser;

    public void Run()
    {
        string ip = "192.168.1.100";
        int port = 502;

        _builder = new ModbusTcpFrameBuilder();
        _parser = new ModbusTcpFrameParser();

        try
        {
            // ====== 第一步：建立TCP连接 ======
            Console.WriteLine($"正在连接 {ip}:{port} ...");
            _tcpClient = new TcpClient();
            _tcpClient.Connect(ip, port);
            _tcpClient.ReceiveTimeout = 3000;  // 接收超时3秒
            _tcpClient.SendTimeout = 3000;
            _stream = _tcpClient.GetStream();
            Console.WriteLine("连接成功！");

            // ====== 第二步：构造并发送读保持寄存器请求 ======
            byte unitId = 1;
            ushort startAddr = 0;
            ushort quantity = 10;

            byte[] requestFrame = _builder.BuildReadHoldingRegisters(unitId, startAddr, quantity);
            Console.WriteLine($"发送请求: {BytesToHex(requestFrame)}");
            _stream.Write(requestFrame, 0, requestFrame.Length);

            // ====== 第三步：接收并解析响应 ======
            byte[] responseBuffer = new byte[256];
            int bytesRead = _stream.Read(responseBuffer, 0, responseBuffer.Length);

            if (bytesRead > 0)
            {
                byte[] responseFrame = new byte[bytesRead];
                Array.Copy(responseBuffer, responseFrame, bytesRead);

                Console.WriteLine($"收到响应: {BytesToHex(responseFrame)}");

                // 解析响应帧
                ModbusResponse response = _parser.Parse(responseFrame);

                if (response.IsError)
                {
                    Console.WriteLine($"通信异常: {response.ErrorMessage}");
                }
                else
                {
                    Console.WriteLine($"事务ID: {response.TransactionId}");
                    Console.WriteLine($"功能码: 0x{response.FunctionCode:X2}");
                    Console.WriteLine($"寄存器值:");

                    if (response.Values != null)
                    {
                        for (int i = 0; i < response.Values.Count; i++)
                        {
                            Console.WriteLine($"  [{startAddr + i}] = {response.Values[i]}");
                        }
                    }
                }
            }

            // ====== 第四步：写单个寄存器 ======
            byte[] writeFrame = _builder.BuildWriteSingleRegister(unitId, 0, 1000);
            Console.WriteLine($"\n发送写请求: {BytesToHex(writeFrame)}");
            _stream.Write(writeFrame, 0, writeFrame.Length);

            bytesRead = _stream.Read(responseBuffer, 0, responseBuffer.Length);
            if (bytesRead > 0)
            {
                byte[] writeResponse = new byte[bytesRead];
                Array.Copy(responseBuffer, writeResponse, bytesRead);
                Console.WriteLine($"写入响应: {BytesToHex(writeResponse)}");

                var writeResult = _parser.Parse(writeResponse);
                Console.WriteLine(writeResult.IsError
                    ? $"写入失败: {writeResult.ErrorMessage}"
                    : "写入成功！");
            }
        }
        catch (SocketException ex)
        {
            Console.WriteLine($"网络错误: {ex.Message}");
        }
        catch (Exception ex)
        {
            Console.WriteLine($"错误: {ex.Message}");
        }
        finally
        {
            // ====== 第五步：关闭连接 ======
            _stream?.Close();
            _tcpClient?.Close();
            Console.WriteLine("连接已关闭");
        }
    }

    /// <summary>
    /// 字节数组转十六进制字符串
    /// </summary>
    private string BytesToHex(byte[] bytes)
    {
        return BitConverter.ToString(bytes).Replace("-", " ");
    }
}
```

---

## 三、注意事项

1. **大端序（Big-Endian）**：Modbus协议中所有多字节数值都采用高字节在前的顺序。C#中读取时需要手动处理字节序。
2. **RTU帧间隔**：Modbus RTU要求帧与帧之间至少有3.5个字符的静默间隔（如9600波特率下约3.5ms），否则可能被合并成一帧。
3. **事务ID匹配**：Modbus TCP中事务ID用于匹配请求和响应。如果使用异步方式发送多个请求，必须通过事务ID来区分响应属于哪个请求。
4. **广播地址**：RTU中地址0为广播地址，从站不响应广播，仅执行命令。TCP中没有广播概念。
5. **寄存器数量限制**：一次读取的寄存器数量有限制（RTU模式最大125个寄存器/200个线圈），超出会返回异常码03。
6. **字节顺序**：某些厂商的设备可能使用不同的字节顺序（如浮点数的AB/CD/DC/BA顺序），需查阅设备手册确认。

---

## 四、练习建议

### 练习1：Modbus TCP调试工具
使用 `TcpClient` 编写一个Modbus TCP调试工具：
- 输入IP、端口、从站地址、功能码、寄存器地址和数量
- 自动构造Modbus TCP请求帧并发送
- 接收响应帧并解析显示
- 支持所有常用功能码（01/02/03/04/05/06/15/16）

### 练习2：Modbus RTU帧模拟器
编写一个程序，模拟Modbus RTU从站：
- 监听串口数据
- 解析收到的Modbus RTU请求帧（包含CRC校验）
- 构造正确的响应帧（包含CRC计算）并发回

### 练习3：Modbus协议分析器
编写一个帧解析工具：
- 输入原始字节数据
- 自动识别是Modbus TCP还是RTU帧
- 详细解析每个字段的含义
- 对RTU帧自动验证CRC

---

## 五、常见错误

### 错误1：字节序错误导致数据不对
```
现象：读取到的寄存器值明显偏大或偏小，如读取1000但得到值为38400
```
**原因**：Modbus是大端序（高字节在前），直接将两个字节反序解析。
**解决**：
```csharp
// 正确的解析方式（大端序）
ushort value = (ushort)((buffer[offset] << 8) | buffer[offset + 1]);

// 错误的解析方式（小端序）
// ushort value = (ushort)((buffer[offset + 1] << 8) | buffer[offset]);
```

### 错误2：CRC校验失败
```
现象：每次接收到的帧CRC都不通过
```
**原因**：计算CRC时包含了CRC本身的字节，或CRC高低字节位置搞反。
**解决**：CRC计算范围是除CRC外的前N-2个字节。CRC低字节在前，高字节在后。

### 错误3：读取数量超限
```
现象：设备返回异常码03（非法数据值）
```
**原因**：一次请求读取的寄存器超过125个，或线圈超过2000个。
**解决**：分批读取，每批不超过限制。

### 错误4：Modbus TCP连接后无响应
```
现象：TCP连接成功，但发送请求后收不到响应
```
**原因**：MBAP头长度字段错误，或协议ID不为0x0000。
**解决**：检查长度字段的计算是否正确：长度 = 单元ID长度(1) + PDU长度。

### 错误5：功能码03读取到的是随机值
```
现象：读取保持寄存器返回的数据不稳定
```
**原因**：寄存器地址映射不正确。PLC的Modbus地址和PLC内部地址的映射关系需要查阅设备手册。
**解决**：确认设备的寄存器地址映射表，不同品牌PLC的地址规则不同。

### 错误6：异常码02（非法数据地址）
```
现象：所有读请求都返回异常码02
```
**原因**：请求的寄存器地址超出设备实际范围，或设备不支持该地址区域。
**解决**：查阅设备手册确认支持的寄存器地址范围，或尝试读取已知存在的地址。
