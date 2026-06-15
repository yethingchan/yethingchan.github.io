## 相关链接

- [[通信协议总览]] - 协议全景与选择指南
- [[OPC标准与实现]] - 南向统一标准
- [[驱动插件架构]] - 插件加载机制
- [[自定义驱动开发指南]] - 创建自己的驱动
- [[轮询逻辑设计]] - 数据采集与上报流程

---

## MQTT协议与物联网通信

MQTT（Message Queuing Telemetry Transport）是物联网领域最主流的通信协议。在IoTGateway架构中，MQTT承担着**北向通信**的角色——将从各种PLC/设备采集到的数据上报到云端物联网平台。

---

## MQTT协议基础

### 发布/订阅模型

MQTT采用**发布/订阅**（Publish/Subscribe）模式，与HTTP的请求/响应模式完全不同：

```
传统 HTTP:                     MQTT Pub/Sub:

Client ──请求──→ Server       Publisher ──发布──→ Broker ←──订阅── Subscriber
Client ←─响应── Server                                 │
                                 Subscriber ←─推送──────┘

特点:
- 发布者和订阅者无需互相知道
- Broker负责消息路由
- 支持一对多消息分发
- 天然适合IoT场景
```

### 核心概念

```
MQTT 通信模型:

┌────────────┐                           ┌────────────┐
│  发布者     │                           │  订阅者     │
│ (网关)      │                           │ (平台)      │
│            │   Publish("temp", 25.3)   │            │
│  ──────────┼───────────────────────────┼─→          │
│            │                           │            │
└────────────┘                           └────────────┘
                  ┌──────────────┐
                  │  MQTT Broker │
                  │  (消息代理)   │
                  │              │
                  │  主题路由表:  │
                  │  "temp" ──→  │──→ 订阅者A
                  │  "temp" ──→  │──→ 订阅者B
                  │  "alarm" ──→ │──→ 订阅者C
                  └──────────────┘
```

### 主题 (Topic)

MQTT使用层级化的主题来组织消息：

```
主题格式示例:

v1/devices/me/telemetry           ← ThingsBoard遥测
v1/gateway/telemetry              ← 网关遥测（代理设备）
v1/gateway/rpc                    ← 网关RPC

device/temperature                ← 自定义主题
device/pressure
device/line1/plc1/data
device/line1/plc1/alarm

通配符:
+ : 单层通配符，匹配一个层级
    device/+/data → 匹配 device/line1/data, device/line2/data
    
# : 多层通配符，匹配任意层级
    device/# → 匹配 device/temperature, device/line1/plc1/data 等
```

### QoS 服务质量

MQTT定义了三个QoS级别：

```
QoS 0 - At most once (最多一次)
  Publisher ──PUBLISH──→ Broker
  (不确认，可能丢失)
  
  适用: 高频遥测数据，偶尔丢一两条不影响

QoS 1 - At least once (至少一次)
  Publisher ──PUBLISH──→ Broker
  Publisher ←──PUBACK── Broker
  (确认收到，可能重复)
  
  适用: 重要状态变更，需要确保到达

QoS 2 - Exactly once (精确一次)
  Publisher ──PUBLISH──→ Broker
  Publisher ←──PUBREC── Broker
  Publisher ──PUBREL──→ Broker
  Publisher ←──PUBCOMP── Broker
  (四次握手，确保精确一次)
  
  适用: 计费数据、关键操作命令
```

### 保留消息与会话

```
保留消息 (Retained Message):
  Publisher 发布消息时设置 Retain=true
  Broker保存该主题的最后一条消息
  新的订阅者订阅时立即收到最后一条保留消息
  
  用途: 设备在线状态、最新配置参数

持久会话 (Clean Session = false):
  Broker为离线客户端缓存消息
  客户端重连后收到离线期间的消息
  
  用途: 确保不丢失重要命令
```

---

## IoTGateway MQTT通信架构

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    IoTGateway 网关                            │
│                                                             │
│  ┌──────────┐    ┌───────────────┐    ┌─────────────────┐   │
│  │DriverThread│───→│MessageService │───→│ PlatformHandler │   │
│  │(数据采集)  │    │ (消息管理)     │    │  (平台适配)     │   │
│  └──────────┘    └───────────────┘    └────────┬────────┘   │
│                                                │            │
│  ┌──────────────────────────────────────────┐  │            │
│  │ 内置 MQTT Broker (端口1888)               │  │            │
│  │ 用于Web UI和外部客户端接入                 │  │            │
│  └──────────────────────────────────────────┘  │            │
└────────────────────────────────────────────────┼────────────┘
                                                 │
                              MQTT (端口1883/1888) │
                                                 │
              ┌──────────────────────────────────┼──────┐
              │                                  │      │
    ┌─────────▼───┐  ┌───────────┐  ┌──────────▼──┐   │
    │ ThingsBoard  │  │ IoTSharp  │  │ ThingsCloud │   │
    │  (Java)      │  │ (.NET)    │  │ (商业)      │   │
    └─────────────┘  └───────────┘  └─────────────┘   │
                                                        │
                                           其他MQTT兼容平台
```

### MessageService 核心实现

MessageService是IoTGateway的MQTT通信核心，管理客户端连接和消息收发：

```csharp
// MessageService.cs
public class MessageService
{
    private IPlatformHandler _platformHandler;
    private IManagedMqttClient? Client { get; set; }
    private readonly ConcurrentDictionary<string, List<PayLoad>> _lastTelemetrys = new();

    public bool IsConnected => Client?.IsConnected ?? false;
    
    // RPC调用事件（平台下发的写操作）
    public event EventHandler<RpcRequest>? OnExcRpc;
    
    // 属性响应事件
    public event EventHandler<ISAttributeResponse>? OnReceiveAttributes;
}
```

### MQTT客户端配置

```csharp
// MessageService.cs - 客户端启动配置
public async Task StartClientAsync()
{
    Client = new MqttFactory().CreateManagedMqttClient();

    // 从数据库加载系统配置
    _systemConfig = await dc.Set<SystemConfig>().FirstOrDefaultAsync();

    _options = new ManagedMqttClientOptionsBuilder()
        .WithAutoReconnectDelay(TimeSpan.FromSeconds(5))  // 自动重连延迟5秒
        .WithMaxPendingMessages(100000)                    // 最大待发送消息数
        .WithClientOptions(new MqttClientOptionsBuilder()
            .WithClientId(_systemConfig.ClientId)          // 客户端ID
            .WithTcpServer(_systemConfig.MqttIp, _systemConfig.MqttPort)
            .WithCredentials(_systemConfig.MqttUName, _systemConfig.MqttUPwd)
            .WithTimeout(TimeSpan.FromSeconds(30))         // 连接超时30秒
            .WithKeepAlivePeriod(TimeSpan.FromSeconds(60)) // 心跳间隔60秒
            .WithProtocolVersion(MqttProtocolVersion.V311) // MQTT 3.1.1
            .WithCleanSession(true)                         // 每次连接清除会话
            .Build())
        .Build();

    // 注册事件处理
    Client.ConnectedAsync += Client_ConnectedAsync;
    Client.DisconnectedAsync += Client_DisconnectedAsync;
    Client.ApplicationMessageReceivedAsync += Client_ApplicationMessageReceivedAsync;

    await Client.StartAsync(_options);

    // 根据配置的平台类型创建对应的处理器
    _platformHandler = PlatformHandlerFactory.CreateHandler(
        _systemConfig.IoTPlatformType, Client, _logger, OnExcRpc);
}
```

### MQTTnet库

IoTGateway使用 **MQTTnet** 库（.NET最流行的MQTT客户端/服务端库），关键特性：

```
MQTTnet 特性:
- 支持 MQTT 3.1 / 3.1.1 / 5.0
- ManagedMqttClient: 自动重连、消息队列
- 内置 MQTT Broker
- 异步API，基于Task
- 高性能，低内存占用
```

---

## 平台处理器 (Platform Handler)

### 策略模式实现

IoTGateway支持多个物联网平台，通过**策略模式**（Strategy Pattern）实现平台适配：

```csharp
// IPlatformHandler.cs - 平台处理器接口
public interface IPlatformHandler
{
    IManagedMqttClient MqttClient { get; }
    ILogger<MessageService> Logger { get; }

    event EventHandler<RpcRequest> OnExcRpc;

    // 连接后订阅主题
    Task ClientConnected();

    // 接收RPC命令
    void ReceiveRpc(MqttApplicationMessageReceivedEventArgs e);

    // 发布遥测数据
    Task PublishTelemetryAsync(string deviceName, Device device, 
        Dictionary<string, List<PayLoad>> sendModel);

    // 上传设备属性
    Task UploadAttributeAsync(string deviceName, object obj);

    // 设备在线/离线通知
    Task DeviceConnected(string deviceName, Device device);
    Task DeviceDisconnected(string deviceName, Device device);

    // 设备增删通知
    Task DeviceAdded(Device device);
    Task DeviceDeleted(Device device);

    // RPC响应
    Task ResponseRpcAsync(RpcResponse rpcResponse);

    // 请求属性
    Task RequestAttributes(string deviceName, bool anySide, params string[] args);
}
```

### 工厂模式创建处理器

```csharp
// PlatformHandlerFactory.cs
public static class PlatformHandlerFactory
{
    public static IPlatformHandler CreateHandler(
        IoTPlatformType platformType,
        IManagedMqttClient client,
        ILogger logger,
        EventHandler<RpcRequest> onExcRpc)
    {
        return platformType switch
        {
            IoTPlatformType.ThingsBoard => new ThingsBoardHandler(...),
            IoTPlatformType.IoTSharp => new IoTSharpHandler(...),
            IoTPlatformType.ThingsCloud => new ThingsCloudHandler(...),
            IoTPlatformType.ThingsPanel => new ThingsPanelHandler(...),
            _ => throw new ArgumentException("不支持的平台类型")
        };
    }
}
```

### 支持的物联网平台

| 平台 | 技术栈 | MQTT主题模式 | 特点 |
|------|--------|-------------|------|
| ThingsBoard | Java | `v1/devices/me/telemetry` | 开源，功能完善 |
| IoTSharp | .NET | 自定义 | 国产开源，.NET生态 |
| ThingsCloud | 商业 | 自定义 | 商业SaaS，开箱即用 |
| ThingsPanel | Go | 自定义 | 国产开源，轻量级 |

### ThingsBoard RPC处理示例

```
ThingsBoard 远程RPC调用流程:

1. 用户在ThingsBoard仪表板点击"设置温度"按钮

2. ThingsBoard 发送RPC请求:
   Topic: v1/gateway/rpc
   Payload: {
     "device": "PLC-001",
     "data": {
       "id": "abc123",
       "method": "HoldingRegisters",
       "params": {
         "Address": "100",
         "Value": "25.5",
         "ValueType": "Float"
       }
     }
   }

3. IoTGateway 接收并解析:
   Client_ApplicationMessageReceivedAsync()
   → _platformHandler.ReceiveRpc(e)
   → 触发 OnExcRpc 事件
   → DriverThread 执行写入操作

4. IoTGateway 返回结果:
   Topic: v1/gateway/rpc
   Payload: {
     "id": "abc123",
     "device": "PLC-001",
     "data": { "success": true }
   }
```

---

## 差异化数据上报

### 智能发布策略

IoTGateway实现了智能的数据上报机制，避免无意义的数据重复发送：

```csharp
// MessageService.cs - 差异化上报
private bool CanPubTelemetry(string deviceName, Device device,
    Dictionary<string, List<PayLoad>> sendModel)
{
    var newTelemetry = sendModel[deviceName];
    var newPayload = newTelemetry[0];

    // 没有历史数据 → 直接发布
    if (!_lastTelemetrys.TryGetValue(deviceName, out var lastTelemetry) 
        || lastTelemetry == null || lastTelemetry.Count == 0)
    {
        _lastTelemetrys[deviceName] = newTelemetry;
        return true;
    }

    var lastPayload = lastTelemetry[0];

    // 启用了差异检测 (CgUpload)
    if (device.CgUpload)
    {
        // 条件1: 超过强制上报周期
        bool isTimeExceeded = (newPayload.TS - lastPayload.TS) > device.EnforcePeriod;
        
        // 条件2: 数据值发生变化
        bool isValueChanged = !newPayload.Values.SequenceEqual(lastPayload.Values);

        if (isTimeExceeded || isValueChanged)
        {
            _lastTelemetrys[deviceName] = newTelemetry;
            return true;
        }

        // 未超时且数据未变化 → 不发布
        return false;
    }

    // 未启用差异检测 → 每次都发布
    _lastTelemetrys[deviceName] = newTelemetry;
    return true;
}

// 发布遥测数据
public async Task PublishTelemetryAsync(string deviceName, Device device,
    Dictionary<string, List<PayLoad>> sendModel)
{
    if (CanPubTelemetry(deviceName, device, sendModel))
    {
        await _platformHandler.PublishTelemetryAsync(deviceName, device, sendModel);
    }
}
```

### 上报策略配置

```
上报策略说明:

CgUpload = false (关闭差异检测):
  每个轮询周期都上报数据
  → 数据量大，但实时性好
  
CgUpload = true (开启差异检测):
  仅在数据变化时上报
  超过 EnforcePeriod 强制上报一次
  
  例如:
  EnforcePeriod = 60秒
  MinPeriod = 1秒（轮询周期）
  
  → 每秒轮询采集数据
  → 只在值变化时发送
  → 最多60秒不发送则强制发送一次

效果: 网络流量减少80%以上（数据稳定时）
```

---

## 内置MQTT Broker

IoTGateway内置了一个MQTT Broker，运行在端口1888：

```
端口用途:
  1883 → 外部MQTT Broker（如EMQX/Mosquitto）的连接端口
  1888 → IoTGateway内置Broker的端口
  
内置Broker用途:
  - Web UI实时数据展示
  - 外部调试工具接入
  - 本地其他应用获取数据
  - 开发测试环境
```

### MQTTX调试连接

使用MQTTX工具连接内置Broker进行调试：

```
连接配置:
  Host: localhost (或网关IP)
  Port: 1888
  Username: (根据配置)
  Password: (根据配置)

订阅主题:
  # (订阅所有主题，查看所有消息)
  
  或特定主题:
  v1/gateway/telemetry  (遥测数据)
  v1/gateway/attributes (属性数据)
  v1/gateway/rpc        (RPC命令)
```

---

## 设备生命周期管理

### 设备上下线通知

IoTGateway在设备连接状态变化时通知物联网平台：

```csharp
// 设备连接成功
public async Task DeviceConnected(string deviceName, Device device) =>
    await _platformHandler.DeviceConnected(deviceName, device);

// 设备连接断开
public async Task DeviceDisconnected(string deviceName, Device device) =>
    await _platformHandler.DeviceDisconnected(deviceName, device);

// 新设备添加
public async Task DeviceAdded(Device device) =>
    await _platformHandler.DeviceAdded(device);

// 设备删除
public async Task DeviceDeleted(Device device) =>
    await _platformHandler.DeviceDeleted(device);
```

### ThingsBoard设备上线消息格式

```json
{
  "device": "PLC-001",
  "type": "CONNECT"
}
```

### ThingsBoard设备属性上报

```json
// 客户端属性 (Client Side)
{
  "device": "PLC-001",
  "clientAttributes": [
    { "key": "firmware", "value": "v1.0.0" },
    { "key": "serialNumber", "value": "SN12345" }
  ]
}

// 共享属性 (Shared)
{
  "device": "PLC-001",
  "sharedAttributes": [
    { "key": "targetTemperature", "value": 25.5 }
  ]
}
```

---

## MQTT安全最佳实践

### 生产环境安全配置

```
MQTT 安全加固清单:

1. 身份认证
   □ 使用用户名/密码认证（不要匿名）
   □ 强密码策略
   □ 考虑使用X.509客户端证书

2. 传输加密
   □ 使用TLS加密（MQTTS，端口8883）
   □ 使用有效的TLS证书
   □ 禁用TLS 1.0/1.1

3. 访问控制
   □ 配置ACL限制主题访问
   □ 每个设备只能发布自己的主题
   □ 只读用户不能发布

4. 网络隔离
   □ MQTT端口不暴露到公网
   □ 使用VPN或专线连接
   □ 防火墙限制源IP

5. 监控
   □ 启用Broker日志审计
   □ 监控异常连接
   □ 设置消息频率限制
```

---

## MQTT常见问题排查

| 问题 | 可能原因 | 解决方案 |
|------|---------|---------|
| 连接超时 | Broker地址或端口错误 | 检查IP/端口，telnet测试 |
| 认证失败 | 用户名密码错误 | 检查SystemConfig配置 |
| 频繁断线 | Keep Alive设置不当 | 增大KeepAlivePeriod |
| 消息丢失 | QoS设置过低 | 重要消息使用QoS 1 |
| 消息积压 | 发布频率过高 | 启用差异化上报 |
| 平台无数据 | 主题格式不匹配 | 检查平台要求的主题格式 |

---

## 小结

| 知识点 | 要点 |
|--------|------|
| MQTT基础 | 发布/订阅模式、Topic层级、QoS 0/1/2 |
| IoTGateway实现 | MessageService + MQTTnet ManagedMqttClient |
| 平台适配 | IPlatformHandler策略模式，工厂模式创建 |
| 差异化上报 | CgUpload + EnforcePeriod 减少冗余发送 |
| 内置Broker | 端口1888，用于Web UI和本地调试 |
| 安全 | 生产环境务必启用TLS+认证+ACL |

---

上一篇: [[OPC标准与实现]] | 下一篇: [[其他PLC协议]]
