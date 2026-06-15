## 相关链接

- [[通信协议总览]] - 协议全景与选择指南
- [[其他PLC协议]] - PLC通信协议
- [[自定义驱动开发指南]] - 创建自己的CNC驱动
- [[驱动插件架构]] - 插件加载机制
- [[MQTT协议与物联网通信]] - 数据上报到云端

---

## CNC系统通信

数控机床（CNC）是制造业的核心设备，实时采集CNC运行数据对于OEE分析、预测性维护和产能管理至关重要。IoTGateway支持两种CNC通信方式：**Fanuc FOCAS**（Fanuc专有协议）和**MTConnect**（行业开放标准）。

---

## Fanuc FOCAS协议

### FOCAS概述

FOCAS（FANUC Open CNC API Specification）是Fanuc公司提供的CNC通信API库。它是一个**本地C库**（DLL），C#通过P/Invoke方式调用：

```
FOCAS 通信架构:

┌──────────────────┐
│  C# 驱动层        │
│  DeviceFanuc.cs  │ ← IoTGateway驱动
├──────────────────┤
│  P/Invoke 封装    │
│  FanucFocas.cs   │ ← C#函数声明
│  fwlib32.cs      │ ← 结构体定义
│  fwlib64.cs      │ ← 64位结构体
├──────────────────┤
│  FOCAS C库       │
│  fwlib32.dll     │ ← Fanuc提供的32位DLL
│  fwlib64.dll     │ ← Fanuc提供的64位DLL
├──────────────────┤
│  TCP/IP 网络     │ ← 端口 8193
├──────────────────┤
│  Fanuc CNC系统   │
│  0i/30i/31i/32i  │
└──────────────────┘
```

### FOCAS许可证

FOCAS库需要Fanuc的授权才能使用：

```
FOCAS 许可说明:

1. FOCAS1 (fwlib32.dll) - 基础版本
   - 包含基本的读写功能
   - 通常随Fanuc CNC一起提供
   - 需要Fanuc Data Server功能选项

2. FOCAS2 - 增强版本
   - 更多高级功能
   - 需要单独购买许可证

注意: 使用FOCAS前需确认CNC系统已开通Data Server功能，
      否则cnc_allclibhndl3连接会返回错误。
```

### IoTGateway Fanuc驱动实现

#### 配置参数

```csharp
// DeviceFanuc.cs
[DriverSupported("Fanuc")]
[DriverInfo("Fanuc", "V1.0.0", "Copyright iotgateway.net 20230220")]
public class DeviceFanuc : IDriver
{
    private ushort _hndl;    // FOCAS句柄
    private int _result = -1;

    #region 配置参数
    [ConfigParameter("设备Id")] public string DeviceId { get; set; }
    [ConfigParameter("超时时间ms")] public int Timeout { get; set; } = 3000;
    [ConfigParameter("最小通讯周期ms")] public uint MinPeriod { get; set; } = 3000;
    [ConfigParameter("设备Ip")] public string DeviceIp { get; set; } = "127.0.0.1";
    [ConfigParameter("设备Port")] public int DevicePort { get; set; } = 8193;
    #endregion
}
```

#### 连接与断开

FOCAS使用句柄（handle）管理连接，类似文件句柄：

```csharp
public bool Connect()
{
    _result = -1;
    // cnc_allclibhndl3: 建立FOCAS连接
    // 参数: IP地址, 端口号, 超时时间(秒), 输出句柄
    _result = FanucFocas.cnc_allclibhndl3(
        DeviceIp, 
        Convert.ToUInt16(DevicePort), 
        Convert.ToInt32(Timeout),
        out _hndl);  // 连接成功后获取句柄

    return _result == FanucFocas.EW_OK;  // EW_OK = 0 表示成功
}

public bool Close()
{
    // cnc_freelibhndl: 释放FOCAS句柄
    _result = FanucFocas.cnc_freelibhndl(_hndl);
    return _result == FanucFocas.EW_OK;
}

// 连接状态检测 - 通过尝试读取系统信息来判断
public bool IsConnected
{
    get
    {
        Focas1.ODBSYS a = new Focas1.ODBSYS();
        _result = FanucFocas.cnc_sysinfo(_hndl, a);
        return _result == FanucFocas.EW_OK;
    }
}
```

### FOCAS数据采集功能

IoTGateway的Fanuc驱动实现了丰富的数据采集方法：

#### 设备类型信息

```csharp
[Method("Fanuc", description: "读Fanuc设备类型")]
public DriverReturnValueModel ReadDeviceType(DriverAddressIoArgModel ioarg)
{
    Focas1.ODBSYS a = new Focas1.ODBSYS();
    _result = FanucFocas.cnc_sysinfo(_hndl, a);
    if (_result == FanucFocas.EW_OK)
    {
        string type = new string(a.mt_type);   // 机床类型
        string num = new string(a.cnc_type);   // CNC型号
        ret.Value = type + num;                // 例如: "M15" (加工中心+15i系统)
    }
}
```

#### 运行状态

```csharp
[Method("Fanuc", description: "读Fanuc设备运行状态")]
public DriverReturnValueModel ReadDeviceRunStatus(DriverAddressIoArgModel ioarg)
{
    Focas1.ODBST aa = new Focas1.ODBST();
    _result = FanucFocas.cnc_statinfo(_hndl, aa);
    if (_result == FanucFocas.EW_OK)
    {
        // aa.run 表示运行状态，含义因CNC型号而异
        var runRet = aa.run;
        // 15系列: 0=STOP, 1=HOLD, 2=START, 3=MSTR(jog mdi)
        // 16/18系列: 0=NOT READY, 1=M-READY, 2=C-START, 3=F-HOLD, 4=B-STOP
        // 其他系列: 0=RESET, 1=STOP, 2=HOLD, 3=START
    }
}
```

#### 主轴转速与进给速度

```csharp
// 实际主轴转速 (cnc_acts)
[Method("Fanuc", description: "读Fanuc设备实际主轴转速")]
public DriverReturnValueModel ReadDeviceActSpindleSpeed(DriverAddressIoArgModel ioarg)
{
    Focas1.ODBACT a = new Focas1.ODBACT();
    _result = FanucFocas.cnc_acts(_hndl, a);
    if (_result == FanucFocas.EW_OK)
        ret.Value = a.data;  // 单位: rpm
}

// 实际进给速度 (cnc_actf)
[Method("Fanuc", description: "读Fanuc设备实际进给速度")]
public DriverReturnValueModel ReadDeviceActFeedRate(DriverAddressIoArgModel ioarg)
{
    Focas1.ODBACT a = new Focas1.ODBACT();
    _result = FanucFocas.cnc_actf(_hndl, a);
    if (_result == FanucFocas.EW_OK)
        ret.Value = a.data;  // 单位: mm/min
}
```

#### 倍率信息

```csharp
// 主轴倍率和进给倍率 (cnc_rdopnlsgnl)
[Method("Fanuc", description: "读Fanuc设备主轴倍率")]
public DriverReturnValueModel ReadDeviceSpindleOvr(DriverAddressIoArgModel ioarg)
{
    Focas1.IODBSGNL a = new Focas1.IODBSGNL();
    _result = FanucFocas.cnc_rdopnlsgnl(_hndl, 0, a);
    if (_result == FanucFocas.EW_OK)
        ret.Value = a.spdl_ovrd;  // 主轴倍率 (%, 如100表示100%)
}

[Method("Fanuc", description: "读Fanuc设备进给倍率")]
public DriverReturnValueModel ReadDeviceFeedOvr(DriverAddressIoArgModel ioarg)
{
    Focas1.IODBSGNL a = new Focas1.IODBSGNL();
    _result = FanucFocas.cnc_rdopnlsgnl(_hndl, 0, a);
    if (_result == FanucFocas.EW_OK)
        ret.Value = a.feed_ovrd;  // 进给倍率 (%)
}
```

#### 报警信息

```csharp
// 报警号 (cnc_rdalmmsg2)
[Method("Fanuc", description: "读Fanuc设备报警号")]
public DriverReturnValueModel ReadDeviceAlarmNum(DriverAddressIoArgModel ioarg)
{
    short inInt = 1;
    Focas1.ODBALMMSG2 a = new Focas1.ODBALMMSG2();
    _result = FanucFocas.cnc_rdalmmsg2(_hndl, -1, ref inInt, a);
    if (_result == FanucFocas.EW_OK)
        ret.Value = a.msg1.alm_no;  // 报警号
}

// 报警文本
[Method("Fanuc", description: "读Fanuc设备报警文本")]
public DriverReturnValueModel ReadDeviceAlarmText(DriverAddressIoArgModel ioarg)
{
    // ...
    ret.Value = new string(a.msg1.alm_msg);  // 报警描述文本
}

// 报警类型分类
// 0=Parameter switch on    3=Foreground P/S
// 4=Overtravel             5=Overheat alarm
// 6=Servo alarm            7=Data I/O error
// 8=Macro alarm           15=External alarm message
```

#### 生产数据

```csharp
// 执行程序号 (cnc_rdprgnum)
[Method("Fanuc", description: "读Fanuc设备执行程序号")]
public DriverReturnValueModel ReadDeviceExeProgamNumber(DriverAddressIoArgModel ioarg)
{
    Focas1.ODBPRO a = new Focas1.ODBPRO();
    _result = FanucFocas.cnc_rdprgnum(_hndl, a);
    if (_result == FanucFocas.EW_OK)
        ret.Value = a.data;  // 当前执行的程序号（如 O0001）
}

// 工件计数 (cnc_rdblkcount)
[Method("Fanuc", description: "读Fanuc设备计数器值")]
public DriverReturnValueModel ReadDeviceCountVaule(DriverAddressIoArgModel ioarg)
{
    int a = 0;
    _result = FanucFocas.cnc_rdblkcount(_hndl, out a);
    if (_result == FanucFocas.EW_OK)
        ret.Value = a;  // 已加工工件数
}

// 刀具号 (cnc_toolnum)
[Method("Fanuc", description: "读Fanuc设备刀具号")]
public DriverReturnValueModel ReadDeviceToolNumber(DriverAddressIoArgModel ioarg)
{
    Focas1.ODBTLIFE4 a = new Focas1.ODBTLIFE4();
    _result = FanucFocas.cnc_toolnum(_hndl, 0, 0, a);
    if (_result == FanucFocas.EW_OK)
        ret.Value = a.data;  // 当前使用的刀具号
}
```

#### 时间统计

```csharp
// 上电总时间 (cnc_rdtimer, type=0)
[Method("Fanuc", description: "读Fanuc设备上电时间")]
public DriverReturnValueModel ReadDevicePowerTime(DriverAddressIoArgModel ioarg)
{
    Focas1.IODBTIME a = new Focas1.IODBTIME();
    _result = FanucFocas.cnc_rdtimer(_hndl, 0, a);
    if (_result == FanucFocas.EW_OK)
        ret.Value = a.minute;  // 单位: 分钟
}

// 运行总时间 (cnc_rdtimer, type=1)
[Method("Fanuc", description: "读Fanuc设备运行时间")]
public DriverReturnValueModel ReadDeviceOperateTime(DriverAddressIoArgModel ioarg)
{
    _result = FanucFocas.cnc_rdtimer(_hndl, 1, a);
    ret.Value = a.minute * 60 + a.msec;  // 转换为秒
}

// 切削总时间 (cnc_rdtimer, type=2)
[Method("Fanuc", description: "读Fanuc设备切削时间")]
public DriverReturnValueModel ReadDeviceCutTime(DriverAddressIoArgModel ioarg)
{
    _result = FanucFocas.cnc_rdtimer(_hndl, 2, a);
    ret.Value = a.minute * 60 + a.msec;
}
```

#### 宏变量与PMC寄存器

```csharp
// 读取宏变量 (cnc_rdmacro)
[Method("Fanuc", description: "读Fanuc宏变量")]
public DriverReturnValueModel ReadHongNew(DriverAddressIoArgModel ioarg)
{
    short number = Convert.ToInt16(ioarg.Address);  // 宏变量编号
    Focas1.ODBM odbm = new Focas1.ODBM();
    short res = Focas1.cnc_rdmacro(_hndl, number, 10, odbm);
    if (res == FanucFocas.EW_OK)
    {
        // 宏变量值 = mcr_val × 10^(-dec_val)
        ret.Value = (Math.Pow(10, -odbm.dec_val) * odbm.mcr_val).ToString();
    }
}

// 读取PMC寄存器 (pmc_rdpmcrng)
[Method("Fanuc", description: "读Fanuc寄存器")]
public DriverReturnValueModel ReadStringNew(DriverAddressIoArgModel ioarg)
{
    // 地址格式: "起始地址,读取字节数"
    short number = Convert.ToInt16(ioarg.Address.Split(',')[0]);
    short AddrNum = Convert.ToInt16(ioarg.Address.Split(',')[1]);
    
    // 分批读取（每次2字节）
    Focas1.IODBPMC0 iodbpmc0 = new Focas1.IODBPMC0();
    int res = Focas1.pmc_rdpmcrng(_hndl, 9, 0, start, end, f, iodbpmc0);
    
    // 拼接为ASCII字符串
    var rawString = Encoding.ASCII.GetString(valueReceive).TrimEnd('\0', ' ');
}
```

### FOCAS函数速查表

| FOCAS函数 | 功能 | 对应方法 |
|-----------|------|---------|
| cnc_allclibhndl3 | 建立连接 | Connect() |
| cnc_freelibhndl | 断开连接 | Close() |
| cnc_sysinfo | 系统信息 | ReadDeviceType() |
| cnc_statinfo | 状态信息 | ReadDeviceRunStatus() |
| cnc_acts | 实际主轴转速 | ReadDeviceActSpindleSpeed() |
| cnc_actf | 实际进给速度 | ReadDeviceActFeedRate() |
| cnc_rdopnlsgnl | 面板信号(倍率) | ReadDeviceSpindleOvr() |
| cnc_rdalmmsg2 | 报警消息 | ReadDeviceAlarmNum/Text/Type() |
| cnc_rdprgnum | 程序号 | ReadDeviceExeProgamNumber() |
| cnc_rdblkcount | 工件计数 | ReadDeviceCountVaule() |
| cnc_toolnum | 刀具号 | ReadDeviceToolNumber() |
| cnc_rdtimer | 时间统计 | ReadDevicePowerTime/OperateTime/CutTime() |
| cnc_rdmacro | 宏变量 | ReadHongNew() |
| pmc_rdpmcrng | PMC寄存器 | ReadStringNew() |
| cnc_rdspmeter | 主轴负载 | ReadDeviceSpindle() |

---

## MTConnect标准

### MTConnect概述

MTConnect是机床行业的**开放通信标准**，由AMT（美国制造技术协会）制定。它基于HTTP/XML，提供了一种统一的方式来获取不同品牌机床的运行数据：

```
MTConnect 架构:

┌──────────────┐   HTTP/REST   ┌──────────────┐   专有协议   ┌──────────┐
│ MTConnect    │ ◄────────────► │ MTConnect    │ ◄──────────► │ CNC      │
│ Client       │   XML/JSON     │ Agent        │              │ 机床     │
│ (IoTGateway) │                │ (适配器)      │              │          │
└──────────────┘                └──────────────┘              └──────────┘

特点:
- 基于HTTP，无需特殊库
- XML/JSON数据格式，可读性好
- 跨品牌统一接口
- 免费开放标准
- 延迟较高（50-500ms）
```

### MTConnect数据模型

```
MTConnect 数据层次:

Devices (设备)
├── Device: "CNC_Lathe_01"
│   ├── Components (组件)
│   │   ├── Axes (轴)
│   │   │   ├── Linear: X轴
│   │   │   ├── Linear: Z轴
│   │   │   └── Rotary: 主轴
│   │   ├── Controller (控制器)
│   │   │   ├── Path (加工路径)
│   │   │   └── Logic (逻辑)
│   │   └── Systems (系统)
│   │       ├── Electric (电气)
│   │       └── Coolant (冷却液)
│   │
│   └── DataItems (数据项)
│       ├── avail         → 可用性 (AVAILABLE/UNAVAILABLE)
│       ├── execution     → 执行状态 (ACTIVE/IDLE/STOPPED)
│       ├── mode          → 运行模式 (AUTOMATIC/MANUAL)
│       ├── program       → 当前程序名
│       ├── SpindleSpeed  → 主轴转速
│       ├── FeedRate      → 进给速度
│       ├── Xposition     → X轴位置
│       └── Zposition     → Z轴位置
```

### IoTGateway MTConnect驱动

```csharp
// DeviceMTClient.cs
[DriverSupported("MTConnectClient")]
[DriverInfo("MTConnectClient", "V1.0.0", "Copyright IoTGateway.net 20230220")]
public class DeviceMTClient : IDriver
{
    private EntityClient? _mClient;

    #region 配置参数
    [ConfigParameter("设备Id")] public string DeviceId { get; set; }
    [ConfigParameter("uri")] public string Uri { get; set; }
    [ConfigParameter("超时时间ms")] public int Timeout { get; set; } = 3000;
    [ConfigParameter("最小通讯周期ms")] public uint MinPeriod { get; set; } = 3000;
    #endregion

    public bool Connect()
    {
        _mClient = new EntityClient(Uri);  // URI例如: http://agent.mtconnect.org
        _mClient.RequestTimeout = Timeout;
        IsConnected = true;
        return IsConnected;
    }

    // 通过DataItem ID读取数据
    [Method("读MTConnect", description: "读MTConnect ID")]
    public DriverReturnValueModel ReadById(DriverAddressIoArgModel ioarg)
    {
        // ioarg.Address 就是DataItem的ID
        var dataValue = _mClient?.GetDataItemById(ioarg.Address).Value;
        ret.Value = dataValue;
    }
}
```

### MTConnect HTTP接口

MTConnect Agent提供三种REST API：

```
1. Probe (设备探测)
   GET http://agent:7878/probe
   → 返回设备结构和所有DataItem定义

2. Current (当前值)
   GET http://agent:7878/current
   → 返回所有DataItem的当前值

3. Sample (历史采样)
   GET http://agent:7878/sample?from=100&count=100
   → 返回从序列号100开始的100条历史数据
```

### MTConnect XML响应示例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<MTConnectStreams xmlns="urn:mtconnect.org:MTConnectStreams:1.7">
  <Streams>
    <DeviceStream name="CNC_Lathe_01" uuid="xxx">
      <ComponentStream component="Rotary" name="S">
        <Samples>
          <SpindleSpeed dataItemId="Sspeed" timestamp="2024-01-15T10:30:00">
            3500
          </SpindleSpeed>
        </Samples>
      </ComponentStream>
      <ComponentStream component="Controller">
        <Events>
          <Execution dataItemId="exec" timestamp="2024-01-15T10:30:00">
            ACTIVE
          </Execution>
          <Program dataItemId="prog" timestamp="2024-01-15T10:28:00">
            O0001
          </Program>
        </Events>
      </ComponentStream>
    </DeviceStream>
  </Streams>
</MTConnectStreams>
```

---

## FOCAS vs MTConnect 选择指南

```
选择决策树:

你的CNC系统是什么品牌?
│
├─ Fanuc ──────┬─ 有Data Server? ──→ FOCAS (延迟低,功能全)
│              │
│              └─ 没有Data Server ──→ 加装Fanuc Data Server
│                                    或使用PMC信号方式
│
├─ Siemens ────→ OPC UA (840D sl内置) 或 S7协议
│
├─ Mitsubishi ─→ SLMP协议 或 MTConnect Agent
│
├─ Mazak ──────→ MTConnect (Mazak内置MTConnect Agent)
│
├─ Haas ───────→ MTConnect (Haas内置MTConnect支持)
│
└─ 多品牌混用 ──→ MTConnect (统一标准)
```

### 对比总结

| 对比项 | Fanuc FOCAS | MTConnect |
|--------|------------|-----------|
| 适用品牌 | 仅Fanuc | 所有品牌 |
| 通信方式 | TCP + C DLL | HTTP + XML |
| 延迟 | 5-20ms | 50-500ms |
| 功能丰富度 | 非常全面 | 标准化子集 |
| 许可证 | 需要Fanuc授权 | 免费开放 |
| 实现复杂度 | 高（P/Invoke） | 低（HTTP调用） |
| IoTGateway库 | fwlib32/64.dll | OpenNETCF.MTConnect |
| 推荐场景 | Fanuc CNC深度采集 | 多品牌统一管理 |

---

## CNC数据采集方案设计

### OEE分析所需数据

```
OEE = 可用率 × 性能率 × 良品率

需要采集的CNC数据:

可用率相关:
├─ 运行状态 (运行/停止/报警) → ReadDeviceRunStatus
├─ 报警信息 (报警号+文本)    → ReadDeviceAlarmNum/Text
└─ 上电时间                  → ReadDevicePowerTime

性能率相关:
├─ 主轴转速                  → ReadDeviceActSpindleSpeed
├─ 进给速度                  → ReadDeviceActFeedRate
├─ 切削时间                  → ReadDeviceCutTime
├─ 倍率(主轴/进给)           → ReadDeviceSpindleOvr/FeedOvr
└─ 循环时间                  → ReadDeviceCycleTime

良品率相关:
├─ 工件计数                  → ReadDeviceCountVaule
└─ 执行程序号                → ReadDeviceExeProgamNumber

附加信息:
├─ 当前刀具号                → ReadDeviceToolNumber
├─ 主轴负载                  → ReadDeviceSpindle
└─ 宏变量(自定义数据)        → ReadHongNew
```

### 推荐采集周期

| 数据类型 | 推荐周期 | 原因 |
|---------|---------|------|
| 运行状态 | 1-5秒 | 状态变化需要快速捕获 |
| 报警信息 | 1秒 | 报警需要即时响应 |
| 主轴转速/进给 | 2-5秒 | 模拟量变化频繁，不需要太密 |
| 工件计数 | 5-10秒 | 每个工件完成后才变化 |
| 时间统计 | 30-60秒 | 累积值，变化缓慢 |
| 程序号/刀具号 | 5秒 | 换刀/换程序时变化 |

---

## 小结

| 知识点 | 要点 |
|--------|------|
| Fanuc FOCAS | P/Invoke调用C DLL，句柄管理连接，功能全面 |
| MTConnect | HTTP/XML开放标准，跨品牌统一，延迟较高 |
| FOCAS核心函数 | cnc_allclibhndl3连接, cnc_sysinfo系统信息, cnc_statinfo状态 |
| 常用采集项 | 运行状态、主轴转速、进给速度、报警、工件计数、倍率 |
| 选择建议 | Fanuc系统用FOCAS，多品牌混用选MTConnect |

---

上一篇: [[其他PLC协议]] | 下一篇: [[自定义驱动开发指南]]
