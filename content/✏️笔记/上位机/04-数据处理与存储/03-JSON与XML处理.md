# 03 - JSON 与 XML 处理

---

## 一、知识讲解

### 1.1 JSON 序列化与反序列化

JSON（JavaScript Object Notation）是上位机中最常用的数据交换格式，用于配置文件、设备参数、通信数据结构等。

#### System.Text.Json（.NET 内置，推荐）

.NET Core 3.0+ 和 .NET 5+ 内置了 `System.Text.Json`，性能优于 Newtonsoft.Json，无需安装第三方包。

#### 核心 API

```csharp
using System.Text.Json;
using System.Text.Json.Serialization;

// 序列化：C#对象 -> JSON字符串
string json = JsonSerializer.Serialize(obj, options);

// 反序列化：JSON字符串 -> C#对象
MyClass obj = JsonSerializer.Deserialize<MyClass>(json, options);
```

#### 常用 JsonSerializerOptions

```csharp
var options = new JsonSerializerOptions
{
    WriteIndented = true,                              // 缩进格式化（便于阅读）
    Encoder = System.Text.Encodings.Web.JavaScriptEncoder.UnsafeRelaxedJsonEscaping, // 支持中文
    PropertyNameCaseInsensitive = true,                // 反序列化时忽略大小写
    DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull,  // 忽略null值
    NumberHandling = JsonNumberHandling.AllowReadingFromString | JsonNumberHandling.WriteAsString
};
```

### 1.2 JSON 配置文件读写

上位机通常需要保存连接参数、设备配置、报警阈值等配置信息，JSON是最常用的配置文件格式。

```json
// 典型的上位机配置文件格式
{
  "Devices": [
    {
      "Name": "1号PLC",
      "Protocol": "ModbusTcp",
      "IpAddress": "192.168.1.100",
      "Port": 502,
      "SlaveId": 1,
      "PollInterval": 1000,
      "Tags": [
        { "Name": "Temperature", "Address": 0, "DataType": "Float", "Scale": 0.1 },
        { "Name": "Pressure", "Address": 2, "DataType": "Int16", "Scale": 1 }
      ]
    },
    {
      "Name": "2号PLC",
      "Protocol": "ModbusTcp",
      "IpAddress": "192.168.1.101",
      "Port": 502,
      "SlaveId": 2,
      "PollInterval": 500,
      "Tags": [
        { "Name": "FlowRate", "Address": 0, "DataType": "Int32", "Scale": 1 }
      ]
    }
  ],
  "AlarmSettings": {
    "Temperature": { "High": 80, "Low": 5, "Level": 2 },
    "Pressure": { "High": 3.0, "Low": 0.1, "Level": 3 }
  },
  "Database": {
    "Type": "SQLite",
    "Path": "plc_data.db",
    "RetentionDays": 30
  }
}
```

### 1.3 XML 基础操作

XML（eXtensible Markup Language）是传统的数据交换格式，部分工业设备仍然使用XML作为配置格式。

#### XML 基本结构

```xml
<?xml version="1.0" encoding="utf-8"?>
<Configuration>
  <Devices>
    <Device Name="PLC_01" Protocol="ModbusTcp">
      <IpAddress>192.168.1.100</IpAddress>
      <Port>502</Port>
      <SlaveId>1</SlaveId>
    </Device>
  </Devices>
  <AlarmSettings>
    <Alarm Tag="Temperature" High="80" Low="5" />
  </AlarmSettings>
</Configuration>
```

#### C# XML 操作方式

| 方式 | 命名空间 | 适用场景 |
|------|----------|----------|
| LINQ to XML | `System.Xml.Linq` | 推荐，API简洁现代 |
| XmlDocument | `System.Xml` | 传统方式，DOM操作 |
| XmlSerializer | `System.Xml.Serialization` | 对象序列化/反序列化 |
| XmlReader/Writer | `System.Xml` | 流式读写，适合大文件 |

---

## 二、代码示例

### 2.1 JSON 序列化与反序列化（System.Text.Json）

```csharp
using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;

// ===== 定义数据模型 =====

/// <summary>
/// 上位机设备配置
/// </summary>
public class DeviceConfig
{
    public string Name { get; set; }                    // 设备名称
    public string Protocol { get; set; }               // 通信协议
    public string IpAddress { get; set; }              // IP地址
    public int Port { get; set; }                       // 端口号
    public byte SlaveId { get; set; }                  // 从站地址
    public int PollInterval { get; set; }              // 轮询间隔（ms）
    public List<TagConfig> Tags { get; set; }          // 数据点列表
}

/// <summary>
/// 数据点（标签）配置
/// </summary>
public class TagConfig
{
    public string Name { get; set; }                    // 标签名称
    public int Address { get; set; }                   // 寄存器地址
    public string DataType { get; set; }               // 数据类型
    public double Scale { get; set; }                  // 比例系数
    public string Unit { get; set; }                   // 单位
    public string Description { get; set; }            // 描述
}

/// <summary>
/// 上位机完整配置
/// </summary>
public class AppConfig
{
    [JsonPropertyName("Devices")]                       // 指定JSON属性名（可选）
    public List<DeviceConfig> Devices { get; set; }

    [JsonPropertyName("AlarmSettings")]
    public Dictionary<string, AlarmThreshold> AlarmSettings { get; set; }

    [JsonPropertyName("Database")]
    public DatabaseConfig Database { get; set; }
}

public class AlarmThreshold
{
    public double High { get; set; }
    public double Low { get; set; }
    public int Level { get; set; }
}

public class DatabaseConfig
{
    public string Type { get; set; }
    public string Path { get; set; }
    public int RetentionDays { get; set; }
}

// ===== 序列化与反序列化工具类 =====

public class JsonConfigHelper
{
    /// <summary>
    /// JSON序列化选项（推荐配置）
    /// </summary>
    private static readonly JsonSerializerOptions Options = new JsonSerializerOptions
    {
        WriteIndented = true,                           // 格式化缩进
        Encoder = System.Text.Encodings.Web.JavaScriptEncoder
            .UnsafeRelaxedJsonEscaping,                 // 不转义中文字符
        PropertyNameCaseInsensitive = true,             // 属性名不区分大小写
        DefaultIgnoreCondition = JsonIgnoreCondition
            .WhenWritingNull,                          // 忽略null值属性
        ReadCommentHandling = JsonCommentHandling.Skip, // 跳过注释
        AllowTrailingCommas = true                      // 允许尾逗号
    };

    /// <summary>
    /// 序列化对象为JSON字符串
    /// </summary>
    public static string Serialize<T>(T obj)
    {
        return JsonSerializer.Serialize(obj, Options);
    }

    /// <summary>
    /// 反序列化JSON字符串为对象
    /// </summary>
    public static T Deserialize<T>(string json)
    {
        return JsonSerializer.Deserialize<T>(json, Options);
    }

    /// <summary>
    /// 保存对象到JSON文件
    /// </summary>
    public static void SaveToFile<T>(T obj, string filePath)
    {
        string json = JsonSerializer.Serialize(obj, Options);
        File.WriteAllText(filePath, json, Encoding.UTF8);
    }

    /// <summary>
    /// 从JSON文件加载对象
    /// </summary>
    public static T LoadFromFile<T>(string filePath)
    {
        if (!File.Exists(filePath))
            throw new FileNotFoundException($"配置文件不存在: {filePath}");

        string json = File.ReadAllText(filePath, Encoding.UTF8);
        return JsonSerializer.Deserialize<T>(json, Options);
    }
}
```

### 2.2 JSON 配置文件读写（完整案例）

```csharp
using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using System.Text.Json;

/// <summary>
/// JSON配置文件管理器
/// 场景：上位机启动时读取配置，运行时修改后保存
/// </summary>
public class ConfigManager
{
    private readonly string _configPath;
    private AppConfig _config;

    public AppConfig Config => _config;

    public ConfigManager(string configPath = "app_config.json")
    {
        _configPath = configPath;
    }

    /// <summary>
    /// 加载配置文件
    /// 如果文件不存在，创建默认配置
    /// </summary>
    public AppConfig LoadOrCreate()
    {
        if (File.Exists(_configPath))
        {
            string json = File.ReadAllText(_configPath, Encoding.UTF8);
            _config = JsonSerializer.Deserialize<AppConfig>(json, new JsonSerializerOptions
            {
                PropertyNameCaseInsensitive = true,
                ReadCommentHandling = JsonCommentHandling.Skip,
                AllowTrailingCommas = true
            });
            Console.WriteLine($"配置已加载: {_configPath}");
        }
        else
        {
            _config = CreateDefaultConfig();
            Save();
            Console.WriteLine($"已创建默认配置: {_configPath}");
        }

        return _config;
    }

    /// <summary>
    /// 保存当前配置到文件
    /// </summary>
    public void Save()
    {
        var options = new JsonSerializerOptions
        {
            WriteIndented = true,
            Encoder = System.Text.Encodings.Web.JavaScriptEncoder.UnsafeRelaxedJsonEscaping
        };
        string json = JsonSerializer.Serialize(_config, options);
        File.WriteAllText(_configPath, json, Encoding.UTF8);
    }

    /// <summary>
    /// 创建默认配置
    /// </summary>
    private AppConfig CreateDefaultConfig()
    {
        return new AppConfig
        {
            Devices = new List<DeviceConfig>
            {
                new DeviceConfig
                {
                    Name = "1号PLC",
                    Protocol = "ModbusTcp",
                    IpAddress = "192.168.1.100",
                    Port = 502,
                    SlaveId = 1,
                    PollInterval = 1000,
                    Tags = new List<TagConfig>
                    {
                        new TagConfig { Name = "Temperature", Address = 0, DataType = "Float", Scale = 0.1, Unit = "C" },
                        new TagConfig { Name = "Pressure", Address = 2, DataType = "Int16", Scale = 0.01, Unit = "MPa" },
                        new TagConfig { Name = "MotorSpeed", Address = 10, DataType = "Int16", Scale = 1, Unit = "RPM" }
                    }
                }
            },
            AlarmSettings = new Dictionary<string, AlarmThreshold>
            {
                { "Temperature", new AlarmThreshold { High = 80, Low = 5, Level = 2 } },
                { "Pressure", new AlarmThreshold { High = 3.0, Low = 0.1, Level = 3 } },
                { "MotorSpeed", new AlarmThreshold { High = 2000, Low = 0, Level = 1 } }
            },
            Database = new DatabaseConfig
            {
                Type = "SQLite",
                Path = "plc_data.db",
                RetentionDays = 30
            }
        };
    }

    // ===== 配置操作示例 =====

    /// <summary>
    /// 添加新设备
    /// </summary>
    public void AddDevice(DeviceConfig device)
    {
        _config.Devices.Add(device);
        Save();
    }

    /// <summary>
    /// 修改轮询间隔
    /// </summary>
    public void UpdatePollInterval(string deviceName, int newInterval)
    {
        var device = _config.Devices.Find(d => d.Name == deviceName);
        if (device != null)
        {
            device.PollInterval = newInterval;
            Save();
        }
    }

    /// <summary>
    /// 修改报警阈值
    /// </summary>
    public void UpdateAlarmThreshold(string tagName, double high, double low)
    {
        if (_config.AlarmSettings.TryGetValue(tagName, out var alarm))
        {
            alarm.High = high;
            alarm.Low = low;
            Save();
        }
    }
}
```

### 2.3 XML 基础操作（LINQ to XML）

```csharp
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Xml.Linq;

/// <summary>
/// XML操作工具类（使用LINQ to XML，推荐方式）
/// LINQ to XML 比 XmlDocument 更简洁易用
/// </summary>
public class XmlConfigHelper
{
    /// <summary>
    /// 创建XML配置文件
    /// </summary>
    public static void CreateXml(string filePath)
    {
        var doc = new XDocument(
            new XDeclaration("1.0", "utf-8", "yes"),
            new XComment("上位机配置文件"),
            new XElement("Configuration",
                new XElement("Devices",
                    new XElement("Device",
                        new XAttribute("Name", "PLC_01"),
                        new XAttribute("Protocol", "ModbusTcp"),
                        new XElement("IpAddress", "192.168.1.100"),
                        new XElement("Port", "502"),
                        new XElement("SlaveId", "1"),
                        new XElement("Tags",
                            new XElement("Tag",
                                new XAttribute("Name", "Temperature"),
                                new XAttribute("Address", "0"),
                                new XAttribute("DataType", "Float")
                            ),
                            new XElement("Tag",
                                new XAttribute("Name", "Pressure"),
                                new XAttribute("Address", "2"),
                                new XAttribute("DataType", "Int16")
                            )
                        )
                    )
                ),
                new XElement("AlarmSettings",
                    new XElement("Alarm",
                        new XAttribute("Tag", "Temperature"),
                        new XAttribute("High", "80"),
                        new XAttribute("Low", "5")
                    )
                )
            )
        );

        doc.Save(filePath);
        Console.WriteLine($"XML文件已创建: {filePath}");
    }

    /// <summary>
    /// 读取XML配置文件
    /// </summary>
    public static List<DeviceConfig> ReadXml(string filePath)
    {
        var doc = XDocument.Load(filePath);
        var devices = new List<DeviceConfig>();

        // LINQ查询XML
        var deviceElements = doc.Descendants("Device");
        foreach (var deviceEl in deviceElements)
        {
            var device = new DeviceConfig
            {
                Name = deviceEl.Attribute("Name")?.Value,
                Protocol = deviceEl.Attribute("Protocol")?.Value,
                IpAddress = deviceEl.Element("IpAddress")?.Value,
                Port = int.TryParse(deviceEl.Element("Port")?.Value, out int port) ? port : 502,
                SlaveId = byte.TryParse(deviceEl.Element("SlaveId")?.Value, out byte slave) ? slave : (byte)1,
                Tags = new List<TagConfig>()
            };

            // 读取标签
            var tags = deviceEl.Descendants("Tag");
            foreach (var tagEl in tags)
            {
                device.Tags.Add(new TagConfig
                {
                    Name = tagEl.Attribute("Name")?.Value,
                    Address = int.TryParse(tagEl.Attribute("Address")?.Value, out int addr) ? addr : 0,
                    DataType = tagEl.Attribute("DataType")?.Value
                });
            }

            devices.Add(device);
        }

        return devices;
    }

    /// <summary>
    /// 修改XML中的某个节点值
    /// </summary>
    public static void UpdateXmlElement(string filePath, string deviceName, string elementName, string newValue)
    {
        var doc = XDocument.Load(filePath);

        var element = doc.Descendants("Device")
            .Where(d => d.Attribute("Name")?.Value == deviceName)
            .SelectMany(d => d.Elements(elementName))
            .FirstOrDefault();

        if (element != null)
        {
            element.Value = newValue;
            doc.Save(filePath);
            Console.WriteLine($"已更新 {deviceName} 的 {elementName} = {newValue}");
        }
    }

    /// <summary>
    /// 向XML添加新节点
    /// </summary>
    public static void AddDeviceToXml(string filePath, DeviceConfig device)
    {
        var doc = XDocument.Load(filePath);
        var devices = doc.Root.Element("Devices");

        devices?.Add(new XElement("Device",
            new XAttribute("Name", device.Name),
            new XAttribute("Protocol", device.Protocol),
            new XElement("IpAddress", device.IpAddress),
            new XElement("Port", device.Port.ToString()),
            new XElement("SlaveId", device.SlaveId.ToString())
        ));

        doc.Save(filePath);
    }

    /// <summary>
    /// 删除XML中的某个设备节点
    /// </summary>
    public static void RemoveDeviceFromXml(string filePath, string deviceName)
    {
        var doc = XDocument.Load(filePath);
        var device = doc.Descendants("Device")
            .Where(d => d.Attribute("Name")?.Value == deviceName)
            .FirstOrDefault();

        device?.Remove();
        doc.Save(filePath);
    }
}
```

### 2.4 上位机场景案例：JSON 格式的设备配置文件与通信参数配置

```csharp
using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;

/// <summary>
/// 上位机场景案例：完整的JSON配置管理系统
/// 场景：上位机程序使用JSON文件保存所有设备和通信参数
/// 支持启动加载、运行时修改、退出保存
/// </summary>
public class PlcConfigManager
{
    private readonly string _configPath;
    private PlcProjectConfig _project;
    private bool _isModified;

    // ========== 事件 ==========
    public event Action<string> OnConfigChanged;

    // ========== 属性 ==========
    public PlcProjectConfig Project => _project;
    public bool IsModified => _isModified;

    public PlcConfigManager(string configPath = "plc_project.json")
    {
        _configPath = configPath;
    }

    /// <summary>
    /// 加载项目配置
    /// </summary>
    public void Load()
    {
        if (!File.Exists(_configPath))
        {
            _project = CreateDefaultProject();
            Save();
            Console.WriteLine("已创建默认项目配置");
            return;
        }

        try
        {
            string json = File.ReadAllText(_configPath, Encoding.UTF8);

            var options = new JsonSerializerOptions
            {
                PropertyNameCaseInsensitive = true,
                ReadCommentHandling = JsonCommentHandling.Skip,
                AllowTrailingCommas = true,
                Encoder = System.Text.Encodings.Web.JavaScriptEncoder
                    .UnsafeRelaxedJsonEscaping
            };

            _project = JsonSerializer.Deserialize<PlcProjectConfig>(json, options);
            Console.WriteLine($"项目配置已加载: {_project.ProjectName}");
            Console.WriteLine($"  设备数量: {_project.Devices?.Count ?? 0}");
        }
        catch (Exception ex)
        {
            Console.WriteLine($"配置加载失败: {ex.Message}");
            _project = CreateDefaultProject();
        }
    }

    /// <summary>
    /// 保存配置（仅在有修改时保存）
    /// </summary>
    public void Save()
    {
        if (_project == null) return;

        var options = new JsonSerializerOptions
        {
            WriteIndented = true,
            Encoder = System.Text.Encodings.Web.JavaScriptEncoder
                .UnsafeRelaxedJsonEscaping,
            DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull
        };

        string json = JsonSerializer.Serialize(_project, options);
        File.WriteAllText(_configPath, json, Encoding.UTF8);

        _isModified = false;
        Console.WriteLine("配置已保存");
    }

    /// <summary>
    /// 获取指定设备的配置
    /// </summary>
    public DeviceConfig GetDevice(string deviceName)
    {
        return _project.Devices?.Find(d => d.Name == deviceName);
    }

    /// <summary>
    /// 获取所有设备名称列表
    /// </summary>
    public List<string> GetDeviceNames()
    {
        var names = new List<string>();
        if (_project.Devices != null)
        {
            foreach (var device in _project.Devices)
                names.Add(device.Name);
        }
        return names;
    }

    /// <summary>
    /// 创建默认项目配置
    /// </summary>
    private PlcProjectConfig CreateDefaultProject()
    {
        return new PlcProjectConfig
        {
            ProjectName = "默认上位机项目",
            Version = "1.0.0",
            CreateTime = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"),
            Devices = new List<DeviceConfig>
            {
                new DeviceConfig
                {
                    Name = "1号PLC",
                    Protocol = "ModbusTcp",
                    IpAddress = "192.168.1.100",
                    Port = 502,
                    SlaveId = 1,
                    PollInterval = 1000,
                    Timeout = 3000,
                    RetryTimes = 3,
                    Tags = new List<TagConfig>
                    {
                        new TagConfig
                        {
                            Name = "温度",
                            Address = 0,
                            DataType = "Float",
                            Scale = 0.1,
                            Unit = "C",
                            Description = "车间环境温度"
                        },
                        new TagConfig
                        {
                            Name = "压力",
                            Address = 2,
                            DataType = "Int16",
                            Scale = 0.01,
                            Unit = "MPa",
                            Description = "管道压力"
                        },
                        new TagConfig
                        {
                            Name = "电机转速",
                            Address = 10,
                            DataType = "Int16",
                            Scale = 1,
                            Unit = "RPM",
                            Description = "主电机转速"
                        }
                    }
                }
            },
            AlarmSettings = new Dictionary<string, AlarmThreshold>
            {
                { "温度", new AlarmThreshold { High = 80, Low = 5, Level = 2 } },
                { "压力", new AlarmThreshold { High = 3.0, Low = 0.1, Level = 3 } }
            },
            Database = new DatabaseConfig
            {
                Type = "SQLite",
                Path = "plc_data.db",
                RetentionDays = 30
            }
        };
    }
}

// ===== 完整的数据模型 =====

/// <summary>
/// 上位机项目配置（顶层）
/// </summary>
public class PlcProjectConfig
{
    public string ProjectName { get; set; }
    public string Version { get; set; }
    public string CreateTime { get; set; }
    public List<DeviceConfig> Devices { get; set; }
    public Dictionary<string, AlarmThreshold> AlarmSettings { get; set; }
    public DatabaseConfig Database { get; set; }
}
```

---

## 三、注意事项

1. **编码问题**：JSON/XML文件建议统一使用 UTF-8 编码。写入文件时显式指定 `Encoding.UTF8`。
2. **JSON中文转义**：`System.Text.Json` 默认会转义中文（`\uXXXX`），需要设置 `JavaScriptEncoder.UnsafeRelaxedJsonEscaping` 显示中文。
3. **属性名大小写**：JSON中属性名区分大小写。使用 `PropertyNameCaseInsensitive = true` 可以忽略大小写匹配。
4. **循环引用**：JSON序列化不支持循环引用的对象图，会导致 `StackOverflowException`。
5. **XML vs JSON**：新项目优先选择JSON（更简洁、解析更快）。XML仅在对接需要XML的旧系统时使用。
6. **配置文件备份**：保存配置前建议先备份原文件，防止保存失败导致配置丢失。

---

## 四、练习建议

### 练习1：设备配置编辑器
- 读取JSON配置文件并在界面显示
- 支持添加/删除/修改设备和标签
- 实时预览JSON内容
- 保存配置并验证

### 练习2：配置导入导出
- 将JSON配置导出为XML格式
- 将XML配置导入为JSON格式
- 验证两种格式之间的数据一致性

### 练习3：通信参数动态配置
- 程序运行时通过修改配置文件动态添加新设备
- 实现配置文件变化检测（FileSystemWatcher）
- 热加载配置（不重启程序）

---

## 五、常见错误

### 错误1：JSON中文显示为 `\uXXXX`
```
现象：序列化后的JSON中中文变成转义序列
```
**原因**：`System.Text.Json` 默认转义非ASCII字符。
**解决**：
```csharp
var options = new JsonSerializerOptions
{
    Encoder = System.Text.Encodings.Web.JavaScriptEncoder.UnsafeRelaxedJsonEscaping
};
```

### 错误2：反序列化返回 null
```
现象：JsonSerializer.Deserialize 返回 null
```
**原因**：JSON属性名与C#属性名大小写不匹配，或JSON结构不匹配。
**解决**：设置 `PropertyNameCaseInsensitive = true`；检查JSON结构和类定义是否一致。

### 错误3：JSON解析异常 `JsonException`
```
现象：反序列化时报错 "The JSON value could not be converted to type"
```
**原因**：JSON值的类型与C#属性类型不匹配（如JSON中是字符串但C#中是int）。
**解决**：检查JSON数据格式，或使用 `JsonElement` 动态类型处理。

### 错误4：XML文件被占用
```
现象：保存XML时报 "文件正被另一个进程使用"
```
**原因**：上一次操作未释放文件句柄。
**解决**：确保使用 `using` 语句及时释放文件资源。

### 错误5：JSON序列化忽略某些属性
```
现象：序列化后某些属性丢失
```
**原因**：属性值为 null 且设置了 `JsonIgnoreCondition.WhenWritingNull`，或属性有 `[JsonIgnore]` 特性。
**解决**：检查是否需要这些属性，或在序列化选项中调整忽略策略。
