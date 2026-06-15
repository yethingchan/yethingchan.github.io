## 相关链接

- [[02-跨平台部署指南]]
- [[04-配置管理与环境隔离]]
- [[05-监控与故障排查]]
- [[架构总览]]
- [[上位机逻辑思维概述]]

## NLog日志系统

日志是工业系统运维的生命线。在无人值守的边缘网关环境中，完善的日志记录是排查故障、分析性能、追溯操作的唯一手段。IoTGateway使用NLog作为日志框架，支持文件、控制台等多种输出目标，并可以按命名空间精细控制日志级别。本章深入解析NLog在IoTGateway中的配置和使用。

## NLog概述

NLog是.NET生态中最流行的日志框架之一，具有以下特点：

- **高性能** - 异步写入，低延迟
- **灵活路由** - 按Logger名称路由到不同目标
- **丰富布局** - 支持数百种布局渲染器
- **自动归档** - 按时间/大小自动归档旧日志
- **热加载** - 修改配置文件后自动生效，无需重启

## IoTGateway的NLog配置

IoTGateway的NLog配置文件位于`nlog.config`：

```xml
<?xml version="1.0" encoding="utf-8" ?>
<nlog xmlns="http://www.nlog-project.org/schemas/NLog.xsd"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      autoReload="true"
      internalLogLevel="Error"
      internalLogFile="./logs/internal-nlog-AspNetCore.txt">

    <!-- 启用ASP.NET Core布局渲染器 -->
    <extensions>
        <add assembly="NLog.Web.AspNetCore" />
    </extensions>

    <!-- 日志输出目标 -->
    <targets>
        <!-- 文件目标 -->
        <target xsi:type="File" name="ownFile-web"
                fileName="./logs/${shortdate}_${level}.log"
                archiveEvery="Hour"
                archiveAboveSize="10485760"
                maxArchiveDays="7"
                maxArchiveFiles="10"
                layout="${longdate}|${logger}|${message} ${exception:format=tostring}|url: ${aspnet-request-url}|action: ${aspnet-mvc-action}|${callsite}" />

        <!-- 控制台目标 -->
        <target xsi:type="Console" name="lifetimeConsole"
                layout="${MicrosoftConsoleLayout}" />
    </targets>

    <!-- 路由规则 -->
    <rules>
        <!-- 启动生命周期消息输出到控制台和文件 -->
        <logger name="Microsoft.Hosting.Lifetime"
                minlevel="Info"
                writeTo="lifetimeConsole, ownFile-web"
                final="true" />

        <!-- 过滤非关键的Microsoft日志（黑洞规则） -->
        <logger name="Microsoft.*"
                minlevel="Error"
                final="true" />
        <logger name="System.Net.Http.*"
                minlevel="Error"
                final="true" />

        <!-- 所有其他日志器的Error及以上级别写入文件 -->
        <logger name="*"
                minlevel="Error"
                writeTo="ownFile-web" />
    </rules>
</nlog>
```

## 配置项详解

### 全局设置

| 属性 | 值 | 说明 |
|------|-----|------|
| `autoReload` | `true` | 配置文件修改后自动重新加载，无需重启应用 |
| `internalLogLevel` | `Error` | NLog自身的内部日志级别 |
| `internalLogFile` | `./logs/internal-nlog-AspNetCore.txt` | NLog内部日志输出路径 |

`autoReload="true"`在运维中非常实用——当需要临时调整日志级别时，直接修改`nlog.config`即可立即生效。

### 文件目标（File Target）

```xml
<target xsi:type="File" name="ownFile-web"
        fileName="./logs/${shortdate}_${level}.log"
        archiveEvery="Hour"
        archiveAboveSize="10485760"
        maxArchiveDays="7"
        maxArchiveFiles="10"
        layout="${longdate}|${logger}|${message} ${exception:format=tostring}|url: ${aspnet-request-url}|action: ${aspnet-mvc-action}|${callsite}" />
```

**文件命名规则：**
- `${shortdate}` - 当前日期（如`2024-01-15`）
- `${level}` - 日志级别（如`Error`、`Info`）
- 实际文件名示例：`2024-01-15_Error.log`

**归档策略：**

| 参数 | 值 | 说明 |
|------|-----|------|
| `archiveEvery` | `Hour` | 每小时归档一次 |
| `archiveAboveSize` | `10485760` | 文件超过10MB时归档 |
| `maxArchiveDays` | `7` | 最多保留7天的归档文件 |
| `maxArchiveFiles` | `10` | 最多保留10个归档文件 |

**布局渲染器说明：**

| 渲染器 | 说明 | 示例输出 |
|--------|------|---------|
| `${longdate}` | 完整时间戳 | `2024-01-15 14:30:22.1234` |
| `${logger}` | 日志器名称（通常是类的全限定名） | `IoTGateway.Service.DeviceService` |
| `${message}` | 日志消息 | `设备PLC-01连接成功` |
| `${exception:format=tostring}` | 异常完整信息 | `System.IO.IOException: ...` |
| `${aspnet-request-url}` | HTTP请求URL | `/BasicData/Device/Search` |
| `${aspnet-mvc-action}` | MVC Action名称 | `Search` |
| `${callsite}` | 调用位置（类名.方法名） | `DeviceService.GetDevice` |

### 控制台目标（Console Target）

```xml
<target xsi:type="Console" name="lifetimeConsole"
        layout="${MicrosoftConsoleLayout}" />
```

使用`${MicrosoftConsoleLayout}`布局，与ASP.NET Core的标准控制台日志格式保持一致。这对于Docker环境特别重要——`docker logs`命令捕获的就是控制台输出。

## 路由规则详解

NLog的路由规则按顺序匹配，`final="true"`表示匹配后不再继续检查后续规则。

### 规则一：应用生命周期日志

```xml
<logger name="Microsoft.Hosting.Lifetime"
        minlevel="Info"
        writeTo="lifetimeConsole, ownFile-web"
        final="true" />
```

- 捕获应用启动/关闭等生命周期消息
- 同时输出到控制台和文件
- 确保Docker和systemd能看到启动完成消息

### 规则二：过滤框架噪音

```xml
<logger name="Microsoft.*" minlevel="Error" final="true" />
<logger name="System.Net.Http.*" minlevel="Error" final="true" />
```

- 将ASP.NET Core框架和HTTP客户端的非关键日志过滤掉
- 只有Error级别及以上的框架日志才会被处理（但因为没有writeTo，实际是"黑洞"）
- 避免大量框架日志淹没业务日志

### 规则三：业务日志

```xml
<logger name="*" minlevel="Error" writeTo="ownFile-web" />
```

- 所有日志器（包括IoTGateway的业务代码）的Error及以上级别写入文件
- 当前配置只记录Error级别，如果需要更详细的日志，需要调整`minlevel`

## 日志级别调整

### 提高日志详细程度

在排查问题时，可以临时将日志级别降低为`Info`或`Debug`：

```xml
<!-- 将minlevel从Error改为Info -->
<logger name="*" minlevel="Info" writeTo="ownFile-web" />
```

由于`autoReload="true"`，修改保存后立即生效。

### 按命名空间设置不同级别

IoTGateway包含多个子系统（驱动、MQTT、RPC等），可以为不同命名空间设置不同的日志级别：

```xml
<rules>
    <!-- 驱动层：详细日志 -->
    <logger name="Plugin.*" minlevel="Debug" writeTo="ownFile-web" final="true" />
    
    <!-- MQTT服务：Info级别 -->
    <logger name="IoTGateway.Service.MQTTService" minlevel="Info" writeTo="ownFile-web" final="true" />
    
    <!-- RPC调用：Info级别 -->
    <logger name="IoTGateway.Service.*" minlevel="Info" writeTo="ownFile-web" final="true" />
    
    <!-- 其他业务代码：Error级别 -->
    <logger name="*" minlevel="Error" writeTo="ownFile-web" />
</rules>
```

### ASP.NET Core日志级别

除了NLog配置，`appsettings.json`中也有日志级别设置：

```json
{
  "Logging": {
    "Console": {
      "IncludeScopes": true,
      "LogLevel": {
        "Default": "Warning",
        "Plugin": "Information",
        "Microsoft.WebTools.BrowserLink.Net.BrowserLinkMiddleware": "Error"
      }
    },
    "Debug": {
      "IncludeScopes": true,
      "LogLevel": {
        "Default": "Information"
      }
    },
    "WTM": {
      "LogLevel": {
        "Default": "Debug"
      }
    }
  }
}
```

**层次结构：**
- `Logging`节点的配置影响ASP.NET Core的`ILogger`接口
- `NLog`的配置影响NLog自身的日志路由
- 两者配合工作：`ILogger`先按`Logging`节点过滤，然后传递给NLog做进一步路由

**命名空间级别配置：**

| 命名空间 | 级别 | 说明 |
|---------|------|------|
| `Default` (Console) | Warning | 控制台默认只显示警告以上 |
| `Plugin` | Information | 驱动插件的Info级别日志输出到控制台 |
| `WTM` | Debug | WTM框架的详细日志 |

## 在代码中使用日志

### 通过ILogger注入

```csharp
public class DeviceService
{
    private readonly ILogger<DeviceService> _logger;

    public DeviceService(ILogger<DeviceService> logger)
    {
        _logger = logger;
    }

    public async Task StartDevice(Device device)
    {
        _logger.LogInformation("正在启动设备: {DeviceName}, 驱动: {DriverName}",
            device.DeviceName, device.Driver?.DriverName);

        try
        {
            // 启动逻辑...
            _logger.LogInformation("设备 {DeviceName} 启动成功", device.DeviceName);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "设备 {DeviceName} 启动失败", device.DeviceName);
        }
    }
}
```

### 结构化日志

NLog支持结构化日志，使用命名占位符而非字符串拼接：

```csharp
// 推荐：结构化日志（可搜索、可索引）
_logger.LogInformation("变量 {VarName} 值: {Value}, 状态: {Status}",
    variable.Name, variable.Value, variable.Status);

// 不推荐：字符串拼接
_logger.LogInformation($"变量 {variable.Name} 值: {variable.Value}");
```

## 日志分析实用技巧

### 查看最近的错误

```bash
# Linux
grep -i "error" /opt/iotgateway/logs/*.log | tail -50

# Windows PowerShell
Select-String -Path "C:\IoTGateway\logs\*.log" -Pattern "Error" | Select-Object -Last 50
```

### 搜索特定设备的日志

```bash
# 搜索PLC-01相关的所有日志
grep "PLC-01" /opt/iotgateway/logs/*.log

# 按时间范围过滤
grep "2024-01-15 14:3" /opt/iotgateway/logs/2024-01-15_Error.log
```

### Docker环境查看日志

```bash
# 查看实时日志
docker logs -f --tail 100 iotgateway

# 按时间过滤
docker logs --since "2024-01-15T14:00:00" iotgateway
```

### 日志文件管理

```bash
# 查看日志目录大小
du -sh /opt/iotgateway/logs/

# 手动清理7天前的日志
find /opt/iotgateway/logs/ -name "*.log" -mtime +7 -delete

# 压缩归档旧日志
find /opt/iotgateway/logs/ -name "*.log" -mtime +1 -exec gzip {} \;
```

## 高级配置：多目标输出

对于需要同时输出到多个目标的场景：

```xml
<targets>
    <!-- 错误日志单独文件 -->
    <target xsi:type="File" name="errorFile"
            fileName="./logs/${shortdate}_Error.log"
            layout="${longdate}|${logger}|${message} ${exception:format=tostring}" />

    <!-- 全量日志文件 -->
    <target xsi:type="File" name="allFile"
            fileName="./logs/${shortdate}_All.log"
            archiveEvery="Day"
            maxArchiveDays="3"
            layout="${longdate}|${level}|${logger}|${message}" />

    <!-- 控制台 -->
    <target xsi:type="Console" name="console"
            layout="${MicrosoftConsoleLayout}" />
</targets>

<rules>
    <!-- 错误日志写入errorFile -->
    <logger name="*" minlevel="Error" writeTo="errorFile" />
    
    <!-- Info及以上写入allFile -->
    <logger name="*" minlevel="Info" writeTo="allFile" />
    
    <!-- 控制台输出 -->
    <logger name="*" minlevel="Warning" writeTo="console" />
</rules>
```

## 小结

NLog为IoTGateway提供了灵活而强大的日志系统。通过合理的日志级别配置、按命名空间分级、自动归档策略，可以在保证日志信息充分性的同时控制存储空间。在工业物联网的运维实践中，日志往往是排查通信故障、追溯操作记录的关键依据，建议在生产环境中至少保留7天的日志，并为驱动层和通信层配置较详细的日志级别。

---

上一篇: [[02-跨平台部署指南]] | 下一篇: [[04-配置管理与环境隔离]]
