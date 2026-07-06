# 07 - OPC UA 协议基础

---

## 一、知识讲解

### 1.1 OPC UA 简介

#### 为什么需要 OPC UA

OPC UA（Open Platform Communications Unified Architecture）是工业自动化领域的下一代通信标准，由 OPC 基金会制定。它解决了传统 OPC（基于COM/DCOM）的诸多问题：

| 问题 | 传统OPC (DA/HDA/A&E) | OPC UA |
|------|---------------------|--------|
| 跨平台 | 仅Windows | Windows/Linux/嵌入式/云 |
| 安全性 | 几乎无安全机制 | 内置认证、加密、签名 |
| 跨网络 | 不支持（COM限制） | 基于TCP/IP，跨互联网 |
| 防火墙 | 需要特殊配置 | 标准443/4840端口 |
| 信息模型 | 扁平化地址空间 | 面向对象的节点模型 |
| 数据类型 | 有限 | 扩展性强，自定义类型 |
| 规范版本 | 多个互不兼容 | 统一架构，向后兼容 |

#### OPC UA 核心概念

- **服务器（Server）**：提供数据的服务端（PLC、SCADA、数据网关）
- **客户端（Client）**：访问数据的上位机/应用程序
- **节点（Node）**：信息模型中的基本单元（变量、方法、对象等）
- **节点ID（NodeId）**：每个节点的唯一标识符
- **地址空间（Address Space）**：服务器中所有节点的层次结构
- **会话（Session）**：客户端与服务器之间的逻辑连接
- **订阅（Subscription）**：客户端订阅节点数据变化通知

#### OPC UA 信息模型

```
地址空间示例：
Objects (对象根节点)
  └── Server
       └── Data
            ├── Temperature     (变量节点, Double)
            ├── Pressure         (变量节点, Double)
            ├── MotorSpeed       (变量节点, Int32)
            └── MotorStatus      (变量节点, Boolean)
```

### 1.2 OPC UA vs Modbus 对比

| 特性 | OPC UA | Modbus |
|------|--------|--------|
| 通信模型 | 客户端-服务器 | 主站-从站 |
| 数据模型 | 面向对象、层次结构 | 扁平寄存器地址 |
| 数据类型 | 丰富（所有基本类型+自定义） | 仅16位寄存器和位 |
| 安全性 | 内置认证/加密/签名 | 无安全机制 |
| 元数据 | 支持描述、单位、范围等 | 无 |
| 批量操作 | 订阅机制，自动推送 | 主站轮询 |
| 标准化 | 完整规范+配套认证 | 开放但无正式标准组织 |
| 学习难度 | 较高 | 低 |
| 实现复杂度 | 较高 | 低 |
| 适用场景 | 大型系统、跨平台、高安全 | 简单设备、低成本、快速开发 |

#### 选型建议

- **选 OPC UA**：大型SCADA/MES系统、需要跨平台、需要安全认证、数据模型复杂、需要标准化信息模型
- **选 Modbus**：简单设备通信、现有设备升级、快速开发原型、小规模系统

### 1.3 C# OPC UA 客户端库

#### OPC Foundation .NET SDK

官方推荐的 OPC UA 客户端开发库。

```bash
# 安装 OPC UA SDK（.NET Standard 2.0 / .NET 5+）
dotnet add package OPCFoundation.NetStandard.Opc.Ua.Client
dotnet add package OPCFoundation.NetStandard.Opc.Ua.Core
```

> 注意：OPC Foundation 的 NuGet 包较多，对于纯客户端开发，安装上述两个包即可。
> 也可以使用简化封装库如 `Opc.Ua.Client` 或第三方库如 `OPC UA .NET SDK`。

### 1.4 连接 OPC UA 服务器

```csharp
using Opc.Ua;
using Opc.Ua.Client;

/// <summary>
/// 连接OPC UA服务器的基本步骤
/// </summary>
public class OpcUaConnection
{
    private UaApplication _application;
    private Session _session;
    private ConfiguredEndpointCollection _endpoints;

    /// <summary>
    /// 创建并连接OPC UA Session
    /// </summary>
    public async Task<bool> ConnectAsync(
        string serverUrl, string username = null, string password = null)
    {
        try
        {
            // 第一步：创建应用程序配置
            var config = new ApplicationConfiguration()
            {
                ApplicationName = "MyOpcUaClient",
                ApplicationUri = Utils.GetApplicationUriFromCertificate(
                    "MyOpcUaClient"),
                ApplicationType = ApplicationType.Client,
                SecurityConfiguration = new SecurityConfiguration
                {
                    AutoAcceptUntrustedCertificates = true,  // 开发环境接受所有证书
                    RejectSHA1SignedCertificates = false,
                    MinimumCertificateKeySize = 1024
                },
                TransportConfigurations = new TransportConfigurationCollection(),
                ClientConfiguration = new ClientConfiguration
                {
                    DefaultSessionTimeout = 30000,  // 会话超时30秒
                    MinSubscriptionLifetime = 10000
                },
                CertificateValidator = new CertificateValidator()
            };

            // 初始化证书验证器
            config.CertificateValidator.CertificateValidation +=
                (validator, e) =>
            {
                // 开发环境：接受所有证书
                if (e.Error.StatusCode == StatusCodes.BadCertificateUntrusted)
                {
                    e.Accept = true;
                }
            };

            // 第二步：创建Session
            _session = await Session.Create(
                config,
                new Uri(serverUrl),        // 服务器URL，如 opc.tcp://192.168.1.100:4840
                updateBeforeConnect: true,
                sessionName: "MySession",
                sessionTimeout: 30000,
                identity: string.IsNullOrEmpty(username)
                    ? null
                    : new UserIdentity(username, password),
                preferredLocales: new string[] { "zh-CN", "en-US" });

            Console.WriteLine($"已连接到OPC UA服务器: {serverUrl}");
            Console.WriteLine($"服务器名称: {_session.ServerName}");
            Console.WriteLine($"会话ID: {_session.SessionId}");

            // 设置KeepAlive
            _session.KeepAlive += Session_KeepAlive;

            return true;
        }
        catch (Exception ex)
        {
            Console.WriteLine($"连接失败: {ex.Message}");
            return false;
        }
    }

    /// <summary>
    /// KeepAlive事件处理
    /// 用于检测会话是否仍然活跃
    /// </summary>
    private void Session_KeepAlive(Session session, KeepAliveEventArgs e)
    {
        if (e.Status != null && ServiceResult.IsNotGood(e.Status))
        {
            Console.WriteLine($"[KeepAlive] 状态异常: {e.Status}");
        }
    }

    /// <summary>
    /// 断开连接
    /// </summary>
    public void Disconnect()
    {
        if (_session != null)
        {
            _session.KeepAlive -= Session_KeepAlive;
            _session.Close();
            _session.Dispose();
            _session = null;
        }
    }
}
```

### 1.5 浏览节点空间

```csharp
using Opc.Ua;
using Opc.Ua.Client;

/// <summary>
/// 浏览OPC UA服务器的地址空间
/// 了解服务器提供了哪些节点（变量、对象等）
/// </summary>
public class OpcUaBrowser
{
    private Session _session;

    /// <summary>
    /// 递归浏览节点空间
    /// 从根节点开始，遍历所有子节点
    /// </summary>
    public void BrowseAll()
    {
        // OPC UA的根节点
        BrowseFromNode(ObjectIds.ObjectsFolder, indent: 0);
    }

    /// <summary>
    /// 从指定节点开始浏览
    /// </summary>
    private void BrowseFromNode(NodeId startNode, int indent)
    {
        // 构造浏览描述
        var browseDesc = new BrowseDescriptionCollection();
        browseDesc.Add(new BrowseDescription
        {
            NodeId = startNode,
            BrowseDirection = BrowseDirection.Forward,
            ReferenceTypeId = ReferenceTypeIds.HierarchicalReferences,
            IncludeSubtypes = true,
            NodeClassMask = (uint)NodeClass.All,
            ResultMask = (uint)BrowseResultMask.All
        });

        // 浏览
        var response = _session.Browse(null, null, 0, browseDesc);

        foreach (var result in response.Results)
        {
            if (result.References == null) continue;

            foreach (var reference in result.References)
            {
                string indentStr = new string(' ', indent * 2);
                string nodeClass = reference.NodeClass.ToString();
                string displayName = reference.DisplayName?.Text ?? "无名称";
                string nodeId = reference.NodeId.ToString();

                Console.WriteLine($"{indentStr}[{nodeClass}] {displayName} ({nodeId})");

                // 如果是对象或视图，继续递归浏览
                if (reference.NodeClass == NodeClass.Object ||
                    reference.NodeClass == NodeClass.View)
                {
                    BrowseFromNode(expandedNodeId: reference.NodeId, indent + 1);
                }
            }
        }
    }

    /// <summary>
    /// 查找指定名称的节点
    /// </summary>
    public NodeId FindNodeByName(string nameToFind)
    {
        return FindNodeByNameRecursive(ObjectIds.ObjectsFolder, nameToFind);
    }

    private NodeId FindNodeByNameRecursive(NodeId startNode, string nameToFind)
    {
        var browseDesc = new BrowseDescriptionCollection();
        browseDesc.Add(new BrowseDescription
        {
            NodeId = startNode,
            BrowseDirection = BrowseDirection.Forward,
            ReferenceTypeId = ReferenceTypeIds.HierarchicalReferences,
            IncludeSubtypes = true,
            NodeClassMask = (uint)NodeClass.Variable,  // 只查找变量节点
            ResultMask = (uint)BrowseResultMask.DisplayName
        });

        var response = _session.Browse(null, null, 0, browseDesc);

        foreach (var result in response.Results)
        {
            if (result.References == null) continue;

            foreach (var reference in result.References)
            {
                if (reference.DisplayName?.Text == nameToFind)
                {
                    return reference.NodeId;
                }
            }
        }

        return null;
    }
}
```

### 1.6 读写节点数据

```csharp
using Opc.Ua;
using Opc.Ua.Client;

/// <summary>
/// OPC UA 节点数据读写
/// </summary>
public class OpcUaReadWrite
{
    private Session _session;

    /// <summary>
    /// 读取单个节点值
    /// </summary>
    public object ReadValue(string nodeIdString)
    {
        // 将字符串NodeId解析为NodeId对象
        var nodeId = new NodeId(nodeIdString);

        // 读取值
        DataValue dataValue = _session.ReadValue(nodeId);

        if (dataValue.StatusCode == StatusCodes.Good)
        {
            Console.WriteLine($"读取成功: {nodeIdString} = {dataValue.Value}");
            return dataValue.Value;
        }
        else
        {
            Console.WriteLine($"读取失败: {dataValue.StatusCode}");
            return null;
        }
    }

    /// <summary>
    /// 批量读取多个节点值
    /// </summary>
    public DataValueCollection ReadMultipleValues(string[] nodeIdStrings)
    {
        // 构造NodeId数组
        ReadValueIdCollection nodesToRead = new ReadValueIdCollection();
        foreach (var id in nodeIdStrings)
        {
            nodesToRead.Add(new ReadValueId
            {
                NodeId = new NodeId(id),
                AttributeId = Attributes.Value
            });
        }

        // 批量读取
        var results = _session.Read(
            null, 0, TimestampsToReturn.Both, nodesToRead);

        // 输出结果
        for (int i = 0; i < results.Count; i++)
        {
            var result = results[i];
            Console.WriteLine($"[{nodeIdStrings[i]}] = {result.Value} " +
                $"(状态: {result.StatusCode})");
        }

        return results;
    }

    /// <summary>
    /// 写入单个节点值
    /// </summary>
    public bool WriteValue(string nodeIdString, object value)
    {
        try
        {
            var nodeId = new NodeId(nodeIdString);
            var dataValue = new DataValue(new Variant(value), StatusCode.Good,
                DateTime.UtcNow);

            StatusCode status = _session.Write(nodeId, dataValue,
                indexRange: null);

            if (status == StatusCodes.Good)
            {
                Console.WriteLine($"写入成功: {nodeIdString} = {value}");
                return true;
            }
            else
            {
                Console.WriteLine($"写入失败: {status}");
                return false;
            }
        }
        catch (Exception ex)
        {
            Console.WriteLine($"写入异常: {ex.Message}");
            return false;
        }
    }

    /// <summary>
    /// 批量写入多个节点值
    /// </summary>
    public void WriteMultipleValues(
        Dictionary<string, object> nodeValues)
    {
        var writeValues = new WriteValueCollection();
        foreach (var kv in nodeValues)
        {
            writeValues.Add(new WriteValue
            {
                NodeId = new NodeId(kv.Key),
                AttributeId = Attributes.Value,
                Value = new DataValue(new Variant(kv.Value), StatusCode.Good,
                    DateTime.UtcNow)
            });
        }

        var results = _session.Write(null, writeValues);
        for (int i = 0; i < results.Count; i++)
        {
            var kv = nodeValues.ElementAt(i);
            Console.WriteLine($"写入 [{kv.Key}] = {kv.Value}, " +
                $"结果: {results[i]}");
        }
    }
}
```

### 1.7 订阅数据变化

```csharp
using System;
using System.Collections.Generic;
using Opc.Ua;
using Opc.Ua.Client;

/// <summary>
/// OPC UA 订阅机制
/// 服务端在数据变化时主动推送通知，无需客户端轮询
/// 这是OPC UA相比Modbus的一大优势
/// </summary>
public class OpcUaSubscription
{
    private Session _session;
    private Subscription _subscription;
    private List<MonitoredItem> _monitoredItems = new List<MonitoredItem>();

    /// <summary>
    /// 创建订阅
    /// </summary>
    public void CreateSubscription(int intervalMs = 1000)
    {
        // 创建订阅对象
        _subscription = new Subscription(_session.DefaultSubscription)
        {
            DisplayName = "MySubscription",
            PublishingInterval = intervalMs,  // 发布间隔（毫秒）
            PublishingEnabled = true,
            LifetimeCount = 100,               // 生命周期计数
            KeepAliveCount = 10,               // KeepAlive计数
        };

        // 添加到Session
        _session.AddSubscription(_subscription);
        _subscription.Create();

        Console.WriteLine($"订阅已创建，发布间隔: {intervalMs}ms");
    }

    /// <summary>
    /// 添加监控项（订阅某个节点的数据变化）
    /// </summary>
    public void AddMonitoredItem(string nodeIdString, string displayName = null)
    {
        var nodeId = new NodeId(nodeIdString);

        // 创建监控项
        var item = new MonitoredItem
        {
            DisplayName = displayName ?? nodeIdString,
            StartNodeId = nodeId,
            SamplingInterval = 1000,  // 采样间隔
            DiscardOldest = true,
            QueueSize = 10,           // 队列大小
            AttributeId = Attributes.Value,
            MonitoringMode = MonitoringMode.Reporting
        };

        // 设置通知处理器
        item.Notification += OnMonitoredItemNotification;

        // 添加监控项到订阅
        _subscription.AddItem(item);
        _subscription.ApplyChanges();

        _monitoredItems.Add(item);
        Console.WriteLine($"已订阅节点: {nodeIdString}");
    }

    /// <summary>
    /// 数据变化通知回调
    /// </summary>
    private void OnMonitoredItemNotification(
        MonitoredItem item, MonitoredItemNotificationEventArgs e)
    {
        foreach (var value in e.DequeueValues())
        {
            Console.WriteLine($"[订阅通知] {item.DisplayName} = {value.Value} " +
                $"(状态: {value.StatusCode}, 时间: {value.SourceTimestamp})");

            // 可以在这里更新UI界面
            // 注意：此回调在OPC UA线程上，更新UI需要Invoke
        }
    }

    /// <summary>
    /// 批量添加监控项
    /// </summary>
    public void AddMonitoredItems(Dictionary<string, string> nodeIdDisplayNames)
    {
        foreach (var kv in nodeIdDisplayNames)
        {
            AddMonitoredItem(kv.Key, kv.Value);
        }
    }

    /// <summary>
    /// 删除订阅
    /// </summary>
    public void RemoveSubscription()
    {
        if (_subscription != null)
        {
            _session.RemoveSubscription(_subscription);
            _subscription.Delete(true);
            _subscription = null;
        }
    }
}
```

---

## 二、代码示例

### 2.1 上位机场景案例：OPC UA 数据采集

```csharp
using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Opc.Ua;
using Opc.Ua.Client;

/// <summary>
/// OPC UA数据采集完整案例
/// 场景：上位机通过OPC UA协议采集多台PLC的实时数据
/// 包含：连接、浏览、读写、订阅的完整流程
/// </summary>
public class OpcUaDataCollector : IDisposable
{
    // ========== 私有字段 ==========
    private Session _session;
    private Subscription _subscription;
    private readonly string _serverUrl;

    // ========== 事件 ==========
    public event Action<string> OnLog;
    public event Action<string, object, DateTime> OnDataChanged;
    public event Action OnDisconnected;

    // ========== 属性 ==========
    public bool IsConnected => _session?.Connected ?? false;

    public OpcUaDataCollector(string serverUrl)
    {
        _serverUrl = serverUrl;
    }

    /// <summary>
    /// 完整工作流程
    /// </summary>
    public async Task RunAsync()
    {
        // ====== 步骤1：连接服务器 ======
        if (!await ConnectAsync())
        {
            Log("连接失败，程序退出");
            return;
        }

        // ====== 步骤2：浏览节点空间 ======
        Log("=== 浏览节点空间 ===");
        // 只浏览一级（避免输出太多）
        BrowseTopLevel();

        // ====== 步骤3：读取数据 ======
        Log("=== 读取数据 ===");
        // 读取已知NodeId的值（需要根据实际服务器调整）
        ReadKnownNodes();

        // ====== 步骤4：写入数据 ======
        Log("=== 写入数据 ===");
        // WriteKnownNodes();

        // ====== 步骤5：订阅数据变化 ======
        Log("=== 订阅数据变化 ===");
        SubscribeToNodes();

        Log("数据采集已启动，按任意键退出...");
        Console.ReadKey();
    }

    /// <summary>
    /// 连接OPC UA服务器
    /// </summary>
    private async Task<bool> ConnectAsync()
    {
        try
        {
            var config = new ApplicationConfiguration
            {
                ApplicationName = "OpcUaDataCollector",
                ApplicationType = ApplicationType.Client,
                SecurityConfiguration = new SecurityConfiguration
                {
                    AutoAcceptUntrustedCertificates = true,
                    RejectSHA1SignedCertificates = false
                },
                ClientConfiguration = new ClientConfiguration
                {
                    DefaultSessionTimeout = 30000
                }
            };

            config.CertificateValidator.CertificateValidation +=
                (v, e) => { if (e.Error.StatusCode == StatusCodes.BadCertificateUntrusted) e.Accept = true; };

            _session = await Session.Create(
                config,
                new Uri(_serverUrl),
                updateBeforeConnect: true,
                sessionName: "DataCollectorSession",
                sessionTimeout: 30000);

            _session.KeepAlive += (s, e) =>
            {
                if (e.Status != null && ServiceResult.IsNotGood(e.Status))
                {
                    Log($"会话异常: {e.Status}");
                    OnDisconnected?.Invoke();
                }
            };

            Log($"已连接到: {_serverUrl}");
            Log($"服务器: {_session.ServerName}, " +
                $"命名空间: {_session.NamespaceUris.Count}个");
            return true;
        }
        catch (Exception ex)
        {
            Log($"连接失败: {ex.Message}");
            return false;
        }
    }

    /// <summary>
    /// 浏览顶级节点
    /// </summary>
    private void BrowseTopLevel()
    {
        var browseDesc = new BrowseDescriptionCollection();
        browseDesc.Add(new BrowseDescription
        {
            NodeId = ObjectIds.ObjectsFolder,
            BrowseDirection = BrowseDirection.Forward,
            ReferenceTypeId = ReferenceTypeIds.HierarchicalReferences,
            IncludeSubtypes = true,
            NodeClassMask = (uint)NodeClass.All,
            ResultMask = (uint)BrowseResultMask.DisplayName
        });

        var response = _session.Browse(null, null, 0, browseDesc);
        foreach (var result in response.Results)
        {
            if (result.References == null) continue;
            foreach (var reference in result.References)
            {
                Log($"  [{reference.NodeClass}] " +
                    $"{reference.DisplayName?.Text} ({reference.NodeId})");
            }
        }
    }

    /// <summary>
    /// 读取已知节点（示例NodeId，需根据实际服务器调整）
    /// </summary>
    private void ReadKnownNodes()
    {
        // 示例：读取几个已知NodeId的值
        string[] knownNodes = new string[]
        {
            "ns=2;s=Temperature",   // 温度
            "ns=2;s=Pressure",      // 压力
            "ns=2;s=MotorSpeed",    // 电机转速
        };

        ReadValueIdCollection nodesToRead = new ReadValueIdCollection();
        foreach (var id in knownNodes)
        {
            nodesToRead.Add(new ReadValueId
            {
                NodeId = new NodeId(id),
                AttributeId = Attributes.Value
            });
        }

        var results = _session.Read(null, 0,
            TimestampsToReturn.Both, nodesToRead);

        for (int i = 0; i < results.Count; i++)
        {
            var r = results[i];
            string status = r.StatusCode == StatusCodes.Good ? "OK" : "FAIL";
            Log($"  [{knownNodes[i]}] = {r.Value} ({status})");
        }
    }

    /// <summary>
    /// 订阅数据变化通知
    /// </summary>
    private void SubscribeToNodes()
    {
        _subscription = new Subscription(_session.DefaultSubscription)
        {
            DisplayName = "DataCollectorSubscription",
            PublishingInterval = 1000,
            PublishingEnabled = true,
            LifetimeCount = 100,
            KeepAliveCount = 10
        };

        _session.AddSubscription(_subscription);
        _subscription.Create();

        // 订阅节点（需根据实际服务器NodeId调整）
        string[] subscribeNodes = new string[]
        {
            "ns=2;s=Temperature",
            "ns=2;s=Pressure",
            "ns=2;s=MotorSpeed",
        };

        foreach (var nodeIdStr in subscribeNodes)
        {
            var item = new MonitoredItem
            {
                DisplayName = nodeIdStr,
                StartNodeId = new NodeId(nodeIdStr),
                SamplingInterval = 1000,
                QueueSize = 10,
                AttributeId = Attributes.Value,
                MonitoringMode = MonitoringMode.Reporting
            };

            item.Notification += (sender, e) =>
            {
                foreach (var val in e.DequeueValues())
                {
                    OnDataChanged?.Invoke(
                        item.DisplayName,
                        val.Value,
                        val.SourceTimestamp);
                    Log($"[通知] {item.DisplayName} = {val.Value}");
                }
            };

            _subscription.AddItem(item);
        }

        _subscription.ApplyChanges();
        Log($"已订阅 {subscribeNodes.Length} 个节点");
    }

    public void Dispose()
    {
        _subscription?.Delete(true);
        _session?.Close();
        _session?.Dispose();
    }

    private void Log(string message)
    {
        OnLog?.Invoke($"[{DateTime.Now:HH:mm:ss}] {message}");
    }
}
```

---

## 三、注意事项

1. **证书管理**：OPC UA 强制要求安全证书。开发环境可以设置 `AutoAcceptUntrustedCertificates = true`，但生产环境必须使用正规证书。
2. **NodeId 格式**：OPC UA 的 NodeId 格式为 `ns=编号;[s|i|g]=标识符`，如 `ns=2;s=Temperature` 表示命名空间2中字符串标识符为"Temperature"的节点。
3. **会话超时**：必须定期 KeepAlive，否则会话会被服务器回收。SDK 已内置 KeepAlive 机制。
4. **订阅 vs 轮询**：OPC UA 推荐使用订阅机制获取实时数据，效率远高于轮询。订阅由服务器在数据变化时主动推送。
5. **命名空间**：不同 OPC UA 服务器的节点命名空间不同，必须先浏览获取正确的 NodeId。
6. **线程安全**：订阅通知回调在 OPC UA 库的线程上触发，更新 WinForms/WPF 界面时需要使用 `Invoke` 或 `Dispatcher`。

---

## 四、练习建议

### 练习1：OPC UA 浏览器
- 连接到 OPC UA 服务器
- 树形显示节点空间
- 点击节点查看属性和值
- 支持读写操作

### 练习2：OPC UA 数据监控面板
- 订阅多个节点的数据变化
- 实时显示数据值和时间戳
- 数据趋势图（使用图表控件）
- 报警阈值检测

### 练习3：OPC UA 网关
- 实现一个简单的 OPC UA 服务器
- 模拟温度、压力等传感器数据
- 通过 OPC UA 客户端读取并验证

---

## 五、常见错误

### 错误1：证书相关错误
```
错误信息：BadCertificateUntrusted / BadCertificateChainIncomplete
```
**原因**：客户端不信任服务器的证书。
**解决**：开发环境添加 `AutoAcceptUntrustedCertificates = true`。

### 错误2：NodeId 找不到
```
现象：读取/订阅节点时返回 BadNodeIdUnknown
```
**原因**：NodeId 字符串格式不正确，或该节点在服务器中不存在。
**解决**：先用浏览功能查看服务器实际的节点结构，获取正确的 NodeId。

### 错误3：NuGet 包版本冲突
```
现象：安装了多个OPC UA相关包后编译报错
```
**原因**：OPC Foundation 的包之间有版本依赖关系。
**解决**：确保所有 OPC UA 包使用相同的主版本号，或通过 OPC Foundation 官方模板创建项目。

### 错误4：会话超时断开
```
现象：程序运行一段时间后连接断开
```
**原因**：KeepAlive 超时，服务器回收了会话。
**解决**：检查网络连接，确认 KeepAlive 事件处理没有阻塞。

### 错误5：权限不足无法写入
```
现象：读取正常，写入返回 BadUserAccessDenied
```
**原因**：当前用户身份没有写入权限。
**解决**：使用有权限的用户身份连接，或检查服务器端权限配置。
