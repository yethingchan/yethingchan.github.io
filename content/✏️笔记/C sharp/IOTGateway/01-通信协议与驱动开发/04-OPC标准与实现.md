## 相关链接

- [[通信协议总览]] - 协议全景与选择指南
- [[Siemens-S7协议]] - 西门子PLC专用协议
- [[MQTT协议与物联网通信]] - 北向通信协议
- [[自定义驱动开发指南]] - 创建自己的驱动
- [[驱动插件架构]] - 插件加载机制

---

## OPC标准与实现

OPC（Open Platform Communications，原名OLE for Process Control）是工业自动化领域最重要的通信标准。它解决了一个核心问题：**如何让不同厂商的设备和软件使用统一的方式通信**。IoTGateway同时支持OPC UA和OPC DA两种标准。

---

## OPC发展历史

```
OPC 标准发展时间线:

1996 ──── OPC DA (Data Access) 发布
           基于 Windows COM/DCOM
           仅支持 Windows 平台
           
1999 ──── OPC HDA (Historical Data Access) 发布
           历史数据访问
           
2003 ──── OPC A&E (Alarms & Events) 发布
           报警和事件
           
2008 ──── OPC UA (Unified Architecture) 发布 ← 革命性升级
           跨平台、内置安全、信息建模
           
2020 ──── OPC UA over TSN (时间敏感网络)
           确定性实时通信
```

---

## OPC DA - 传统标准

### 架构

OPC DA基于Windows COM/DCOM技术，是典型的C/S架构：

```
┌──────────────────┐           ┌──────────────────┐
│   OPC DA Client  │           │   OPC DA Client  │
│   (上位机软件)    │           │   (SCADA/HMI)    │
└────────┬─────────┘           └────────┬─────────┘
         │                              │
         │     COM/DCOM 调用             │
         │                              │
         └──────────┬───────────────────┘
                    │
         ┌──────────▼───────────────────┐
         │      OPC DA Server           │
         │   (设备厂商提供)              │
         │   KEPServer / Matrikon 等    │
         └──────────┬───────────────────┘
                    │
         ┌──────────▼───────────────────┐
         │     PLC / 仪表 / 传感器       │
         └──────────────────────────────┘
```

### DCOM的问题

OPC DA最大的痛点是**DCOM配置**：

```
DCOM 常见问题:
┌──────────────────────────────────────────────┐
│ 1. DCOM安全配置极其复杂                        │
│    - 需要配置启动权限、访问权限、标识权限       │
│    - Windows防火墙默认阻止DCOM                  │
│    - 需要开放端口135 + 动态端口范围             │
│                                                │
│ 2. 仅限Windows平台                             │
│    - Linux/macOS无法使用                       │
│                                                │
│ 3. 无内置安全机制                               │
│    - 依赖Windows身份验证                        │
│    - 数据传输不加密                              │
│                                                │
│ 4. 网络配置脆弱                                 │
│    - NAT穿透困难                                │
│    - 跨网段通信问题多                            │
└──────────────────────────────────────────────┘
```

### IoTGateway OPC DA驱动

IoTGateway通过 `Automation.OPCClient` 库对接OPC DA服务器：

```csharp
// DeviceDaClient.cs
[DriverSupported("OPCDaClient")]
[DriverInfo("OPCDaClient", "V1.0.0", "Copyright IoTGateway.net 20230220")]
internal class DeviceDaClient : IDriver
{
    private OPCClientWrapper? _opcDaClient;

    #region 配置参数
    [ConfigParameter("设备Id")] public string DeviceId { get; set; }
    [ConfigParameter("IP")] public string Ip { get; set; } = "127.0.0.1";
    [ConfigParameter("OpcServerName")] 
    public string OpcServerName { get; set; } = "ICONICS.SimulatorOPCDA.2";
    [ConfigParameter("超时时间ms")] public int Timeout { get; set; } = 3000;
    [ConfigParameter("最小通讯周期ms")] public uint MinPeriod { get; set; } = 3000;
    #endregion

    public bool Connect()
    {
        _opcDaClient = new OPCClientWrapper();
        _opcDaClient.Init(Ip, OpcServerName);
        return IsConnected;
    }

    public bool IsConnected => _opcDaClient != null && _opcDaClient.IsOPCServerConnected();
}
```

### OPC DA数据读取

OPC DA使用**Item ID**（标签名）来标识数据点，读取后需要手动进行类型转换：

```csharp
[Method("读OPCDa", description: "读OPCDa节点")]
public DriverReturnValueModel ReadNode(DriverAddressIoArgModel ioArg)
{
    var ret = new DriverReturnValueModel { StatusType = VaribaleStatusTypeEnum.Good };

    if (IsConnected)
    {
        // 通过Item ID读取数据（返回字符串格式）
        var dataValue = _opcDaClient?.ReadNodeLabel(ioArg.Address);
        
        // 根据ValueType手动转换类型
        switch (ioArg.ValueType)
        {
            case DataTypeEnum.Bit:
                ret.Value = dataValue == "On" ? 1 : 0;
                break;
            case DataTypeEnum.Bool:
                ret.Value = dataValue == "On";
                break;
            case DataTypeEnum.Int16:
                ret.Value = short.Parse(dataValue);
                break;
            case DataTypeEnum.Float:
                ret.Value = float.Parse(dataValue);
                break;
            case DataTypeEnum.AsciiString:
                ret.Value = dataValue;
                break;
            // ... 更多类型
        }
    }
    return ret;
}
```

### 常见OPC DA Server

| 软件 | 厂商 | 特点 |
|------|------|------|
| KEPServerEX | Kepware | 功能最全，支持500+驱动，商业软件 |
| Matrikon OPC | Honeywell | 老牌OPC Server |
| ICONICS | ICONICS | 内置模拟器，适合测试 |
| Softing | Softing | 支持多种PLC协议 |

> **实践提示**：测试OPC DA时，可以使用 `ICONICS.SimulatorOPCDA.2`（IoTGateway默认配置的Server），它是一个内置模拟器，无需连接真实设备即可测试。

---

## OPC UA - 统一架构

### 革命性改进

OPC UA（Unified Architecture）是对OPC DA的彻底重写，解决了所有历史问题：

```
OPC DA vs OPC UA 对比:

           OPC DA                    OPC UA
平台:      仅Windows                 跨平台(Win/Linux/Mac)
安全:      依赖DCOM                  内置TLS加密+证书认证
通信:      COM/DCOM                  TCP/HTTPS
信息模型:  扁平Item ID               层级化节点模型
数据类型:  简单类型                   丰富类型+自定义类型
可靠性:    依赖DCOM                  内置会话管理+断线重连
标准:      私有接口                   IEC 62541国际标准
```

### OPC UA架构

```
┌─────────────────────────────────────────────────┐
│                 OPC UA 应用层                     │
│  ┌─────────┐ ┌──────────┐ ┌─────────────────┐   │
│  │地址空间  │ │服务集     │ │信息模型         │   │
│  │Address  │ │Services  │ │Information Model│   │
│  │Space    │ │          │ │                 │   │
│  └─────────┘ └──────────┘ └─────────────────┘   │
├─────────────────────────────────────────────────┤
│              OPC UA 通信层                        │
│  ┌──────────────────┐ ┌──────────────────────┐  │
│  │ OPC UA Binary    │ │ OPC UA XML/SOAP      │  │
│  │ (opc.tcp://)     │ │ (http://)            │  │
│  │ 端口: 4840       │ │                      │  │
│  └──────────────────┘ └──────────────────────┘  │
├─────────────────────────────────────────────────┤
│              安全层                               │
│  ┌────────────────────────────────────────────┐ │
│  │ TLS 1.2/1.3 + X.509证书 + 用户认证        │ │
│  │ None / Sign / SignAndEncrypt              │ │
│  └────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────┤
│              TCP / HTTPS                         │
└─────────────────────────────────────────────────┘
```

### 节点模型 (Node Model)

OPC UA使用**节点**（Node）作为数据的基本单元，每个节点有唯一的 `NodeId`：

```
OPC UA 地址空间（树形结构）:

Root (RootFolder)
├── Objects
│   ├── Server
│   │   ├── ServerStatus
│   │   │   ├── State
│   │   │   ├── StartTime
│   │   │   └── CurrentTime
│   │   └── ...
│   └── MyDevice              ← 自定义设备节点
│       ├── Temperature       ← 温度变量
│       ├── Pressure          ← 压力变量
│       ├── Status            ← 状态变量
│       └── Methods
│           ├── Start()       ← 方法节点
│           └── Stop()
├── Types
│   ├── ObjectTypes
│   ├── VariableTypes
│   └── DataTypes
└── Views
```

### NodeId 格式

```
NodeId 的四种格式:

1. 数字NodeId:    ns=2;i=10853        (命名空间2, 数字标识10853)
2. 字符串NodeId:  ns=2;s=Temperature   (命名空间2, 字符串标识)
3. GUID NodeId:   ns=2;g=xxxxx         (命名空间2, GUID标识)
4. 不透明NodeId:  ns=2;b=xxxxx         (命名空间2, Base64标识)

常见命名空间:
  ns=0 → OPC UA标准命名空间
  ns=1 → 服务器命名空间
  ns=2+ → 自定义命名空间（设备厂商定义）
```

---

## IoTGateway OPC UA驱动

### 驱动实现

```csharp
// DeviceUaClient.cs
[DriverSupported("OPCUaClient")]
[DriverInfo("OPCUaClient", "V1.0.0", "Copyright IoTGateway.net 20230220")]
public class DeviceUaClient : IDriver
{
    private OpcUaClientHelper? _opcUaClient;

    #region 配置参数
    [ConfigParameter("设备Id")] public string DeviceId { get; set; }
    
    [ConfigParameter("uri")]
    public string Uri { get; set; } = "opc.tcp://localhost:62541/Quickstarts/ReferenceServer";
    
    [ConfigParameter("超时时间ms")] public int Timeout { get; set; } = 3000;
    [ConfigParameter("最小通讯周期ms")] public uint MinPeriod { get; set; } = 3000;
    #endregion

    public bool Connect()
    {
        _opcUaClient = new OpcUaClientHelper();
        // 异步连接，等待超时
        _opcUaClient.ConnectServer(Uri).Wait(Timeout);
        return IsConnected;
    }

    public bool IsConnected => _opcUaClient is { Connected: true };
}
```

### 节点读取

```csharp
[Method("读OPCUa", description: "读OPCUa节点")]
public DriverReturnValueModel ReadNode(DriverAddressIoArgModel ioArg)
{
    var ret = new DriverReturnValueModel { StatusType = VaribaleStatusTypeEnum.Good };

    if (IsConnected)
    {
        try
        {
            // 通过NodeId读取OPC UA变量
            var dataValue = _opcUaClient?.ReadNode(new NodeId(ioArg.Address));
            
            // 检查数据质量
            if (DataValue.IsGood(dataValue))
                ret.Value = dataValue?.Value;
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

### OPC UA连接流程

```
OPC UA 客户端连接过程:

1. 创建TCP连接
   客户端 ──TCP──→ opc.tcp://server:4840

2. Hello/Acknowledge 握手
   客户端 ──Hello──→ 服务器   (协商协议版本、缓冲区大小)
   服务器 ──Ack──→ 客户端

3. OpenSecureChannel
   客户端 ──OPN请求──→ 服务器  (建立安全通道)
   服务器 ──OPN响应──→ 客户端  (协商安全策略)
   
   安全策略选项:
   - None: 无安全（仅测试）
   - Basic256Sha256: 签名+加密
   - Aes128_Sha256_RsaOaep: 推荐

4. CreateSession
   客户端 ──CreateSession──→ 服务器
   服务器 ──SessionId──→ 客户端

5. ActivateSession
   客户端 ──用户凭证──→ 服务器  (Anonymous/用户名密码/证书)
   服务器 ──确认──→ 客户端

6. 可以开始读写数据
```

---

## OPC UA安全机制

### 安全策略级别

OPC UA提供了三个层次的安全：

```
安全层次:

┌────────────────────────────────────────┐
│ 1. 传输层安全 (Transport Security)      │
│    - TLS加密所有通信                     │
│    - 防止窃听和篡改                      │
├────────────────────────────────────────┤
│ 2. 用户认证 (User Authentication)       │
│    - Anonymous (匿名)                   │
│    - Username/Password (用户名密码)      │
│    - X.509 Certificate (证书)           │
├────────────────────────────────────────┤
│ 3. 权限控制 (Access Control)            │
│    - 基于角色的节点读写权限              │
│    - 细粒度控制每个变量的访问            │
└────────────────────────────────────────┘
```

### 安全策略选择建议

| 场景 | 推荐安全策略 |
|------|------------|
| 开发测试 | None + Anonymous |
| 内网通信 | Basic256Sha256 + Username/Password |
| 外网通信 | Aes256_Sha256_RsaPss + Certificate |
| 高安全需求 | 最高安全策略 + 证书互信 + 网络隔离 |

---

## OPC UA vs OPC DA 选择指南

### 何时使用OPC UA

- 新项目首选
- 需要跨平台（Linux上位机）
- 需要安全通信
- 需要复杂信息模型
- 远程通信（跨网段/互联网）

### 何时使用OPC DA

- 老系统改造，现有OPC DA Server不能更换
- 只有Windows环境
- 不需要高安全性（内网环境）
- 已有KEPServerEX等成熟部署

### 桥接方案

常见做法是用IoTGateway作为**协议桥接器**：

```
┌──────────┐     OPC DA     ┌──────────────┐    MQTT     ┌──────────┐
│ 老设备    │ ──────────────→│ IoTGateway   │ ───────────→│ 云平台   │
│ +OPC DA   │               │ (协议桥接)    │             │          │
└──────────┘               └──────────────┘             └──────────┘

┌──────────┐     OPC UA     ┌──────────────┐    MQTT     ┌──────────┐
│ 新设备    │ ──────────────→│ IoTGateway   │ ───────────→│ 云平台   │
│ +OPC UA   │               │ (数据采集)    │             │          │
└──────────┘               └──────────────┘             └──────────┘
```

---

## OPC UA调试工具

### UaExpert

UaExpert是最流行的免费OPC UA客户端，用于浏览和测试OPC UA服务器：

```
UaExpert 使用步骤:

1. 添加服务器:
   Add Server → Custom Discovery → 输入URL
   例: opc.tcp://192.168.1.100:4840

2. 连接配置:
   - Security Policy: None (测试) 或 Basic256Sha256
   - User Token: Anonymous 或 用户名密码

3. 浏览地址空间:
   Address Space → 展开树形结构 → 找到目标节点

4. 数据监视:
   拖拽节点到 Data Access View → 实时显示值和状态

5. 写入测试:
   右键节点 → Write → 输入新值
```

### 常用OPC UA Server软件

| 软件 | 类型 | 特点 |
|------|------|------|
| OPC Foundation Reference Server | 开源 | IoTGateway默认配置的连接目标 |
| KEPServerEX 6 | 商业 | 支持OPC DA和UA，驱动最全 |
| Prosys Simulation Server | 免费 | OPC UA模拟服务器，适合测试 |
| S7-1500内置OPC UA Server | 内置 | 西门子新PLC自带 |
| Codesys OPC UA Server | 内置 | 基于Codesys的PLC自带 |

---

## OPC DA到OPC UA迁移

对于需要将OPC DA迁移到OPC UA的项目，以下是典型步骤：

```
迁移路径:

方案1: 使用KEPServerEX的UA接口
  OPC DA Server (KEPServerEX)
  → 自带OPC UA Server接口
  → 上位机改为UA Client

方案2: 使用OPC UA Wrapper
  OPC DA Server
  → Softing OPC UA Wrapper
  → 自动将DA接口暴露为UA

方案3: 使用IoTGateway桥接
  OPC DA Server
  → IoTGateway OPC DA Client
  → MQTT/OPC UA 输出
```

---

## 小结

| 对比项 | OPC DA | OPC UA |
|--------|--------|--------|
| 平台 | 仅Windows | 跨平台 |
| 安全 | DCOM安全（复杂） | 内置TLS+证书 |
| 端口 | 135+动态端口 | 4840(固定) |
| 数据模型 | 扁平Item ID | 层级化节点 |
| IoTGateway库 | Automation.OPCClient | OpcUaClientHelper |
| 新项目推荐 | 不推荐 | 强烈推荐 |
| 配置参数 | IP + ServerName | URI |

---

上一篇: [[Siemens-S7协议]] | 下一篇: [[MQTT协议与物联网通信]]
