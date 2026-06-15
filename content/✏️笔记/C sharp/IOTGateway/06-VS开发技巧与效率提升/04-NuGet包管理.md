# NuGet 包管理

## 相关链接

- [[03-项目配置与构建]] - 项目配置基础
- [[00-VS开发环境搭建]] - NuGet 源配置
- [[驱动插件架构]] - 驱动插件的依赖管理
- [[通信协议总览]] - 各通信协议对应的 NuGet 包
- [[EF-Core基础]] - EF Core 相关 NuGet 包

---

## NuGet 在上位机开发中的角色

上位机项目依赖大量 NuGet 包来实现工业通信功能。IoTGateway 项目引用了数十个 NuGet 包，涵盖了从工业协议到 Web 框架的各个方面。正确管理这些依赖是项目成功的关键。

### IoTGateway 的依赖全景

```
IoTGateway 主要依赖分类：

1. Web 框架层
   ├── ASP.NET Core（内置于 SDK）
   ├── Swashbuckle.AspNetCore（Swagger API 文档）
   └── Serilog.AspNetCore（结构化日志）

2. 数据访问层
   ├── Microsoft.EntityFrameworkCore（ORM 框架）
   ├── Pomelo.EntityFrameworkCore.MySql（MySQL 支持）
   └── Microsoft.EntityFrameworkCore.Sqlite（SQLite 支持）

3. 工业通信协议
   ├── HslCommunication（综合工业通信库）
   ├── MQTTnet（MQTT 客户端和服务端）
   ├── OPCFoundation.NetStandard.Opc.Ua（OPC UA 支持）
   └── S7.Net（西门子 S7 通信）

4. 基础设施
   ├── Autofac（依赖注入容器）
   ├── AutoMapper（对象映射）
   └── FreeRedis（Redis 缓存）
```

---

## 一、在 VS 中管理 NuGet 包

### 1.1 图形界面操作

**打开 NuGet 包管理器的方式：**

```
方法 1：右键项目 → 管理 NuGet 程序包
方法 2：工具 → NuGet 包管理器 → 管理解决方案的 NuGet 程序包
方法 3：快捷键（需要先自定义）
```

**解决方案级别 vs 项目级别：**

| 操作方式 | 适用场景 |
|---------|---------|
| 管理解决方案的 NuGet 程序包 | 统一多个项目的包版本 |
| 管理项目的 NuGet 程序包 | 为单个项目添加/更新包 |

> [!tip] 统一版本管理
> 对于 IoTGateway 这种多项目解决方案，建议使用"管理解决方案的 NuGet 程序包"功能，在"合并"选项卡中统一所有项目的包版本，避免版本不一致导致的运行时错误。

### 1.2 Package Manager Console

```
打开方式：工具 → NuGet 包管理器 → 程序包管理器控制台
```

常用命令：

```powershell
# 安装包
Install-Package MQTTnet -Version 4.3.3.952
Install-Package HslCommunication -ProjectName Driver.Modbus

# 更新包
Update-Package MQTTnet
Update-Package -ProjectName IoTGateway.Host

# 卸载包
Uninstall-Package HslCommunication -ProjectName Driver.Modbus

# 查看已安装的包
Get-Package -ProjectName IoTGateway.Host

# 恢复包
dotnet restore
```

### 1.3 dotnet CLI 管理包

```bash
# 添加包
dotnet add package MQTTnet --version 4.3.3.952
dotnet add IoTGateway.Host package Serilog.AspNetCore

# 移除包
dotnet remove package MQTTnet

# 列出包
dotnet list package
dotnet list package --include-transitive  # 包含传递依赖

# 检查过期包
dotnet list package --outdated

# 检查有漏洞的包
dotnet list package --vulnerable
```

---

## 二、上位机开发必备 NuGet 包

### 2.1 工业通信包

| 包名 | 版本建议 | 用途 | 使用项目 |
|------|---------|------|---------|
| `HslCommunication` | 12.x | 综合工业通信库（Modbus/PLC/仪表） | Driver.* |
| `MQTTnet` | 4.x | MQTT 3.1.1/5.0 客户端和服务端 | IoTGateway.Host |
| `OPCFoundation.NetStandard.Opc.Ua` | 1.5.x | OPC UA 客户端/服务端 | Driver.OpcUA |
| `S7.Net` | 0.20.x | 西门子 S7 PLC 通信 | Driver.S7 |
| `System.IO.Ports` | 8.x | 串口通信 | Driver.Serial |
| `NModbus` | 3.x | 开源 Modbus 实现 | Driver.Modbus |

### 2.2 数据处理包

| 包名 | 用途 | 使用项目 |
|------|------|---------|
| `MessagePack` | 高性能序列化（二进制） | IoTGateway.Core |
| `System.Reactive` | 响应式编程（数据流处理） | IoTGateway.Core |
| `MathNet.Numerics` | 数值计算（滤波、插值） | IoTGateway.Service |
| `ClosedXML` | Excel 报表生成 | IoTGateway.Service |
| `NPOI` | Office 文档处理（替代方案） | IoTGateway.Service |

### 2.3 日志和监控包

| 包名 | 用途 |
|------|------|
| `Serilog` | 结构化日志框架 |
| `Serilog.Sinks.File` | 日志输出到文件 |
| `Serilog.Sinks.Console` | 日志输出到控制台 |
| `Serilog.Sinks.Seq` | 日志输出到 Seq 服务器 |
| `OpenTelemetry` | 分布式追踪和指标收集 |
| `prometheus-net` | Prometheus 指标暴露 |

### 2.4 测试包

| 包名 | 用途 |
|------|------|
| `xunit` | 单元测试框架 |
| `Moq` | Mock 框架 |
| `FluentAssertions` | 流畅断言 |
| `Testcontainers` | 集成测试容器 |
| `BenchmarkDotNet` | 性能基准测试 |

---

## 三、包版本管理

### 3.1 中央包管理（Central Package Management）

.NET 8 支持中央包管理，在解决方案级别统一控制包版本：

```xml
<!-- Directory.Packages.props - 解决方案根目录 -->
<Project>
  <PropertyGroup>
    <ManagePackageVersionsCentrally>true</ManagePackageVersionsCentrally>
  </PropertyGroup>

  <ItemGroup>
    <!-- 定义所有包的版本（各项目引用时不需要指定版本） -->
    <PackageVersion Include="MQTTnet" Version="4.3.3.952" />
    <PackageVersion Include="HslCommunication" Version="12.2.0" />
    <PackageVersion Include="Serilog.AspNetCore" Version="8.0.0" />
    <PackageVersion Include="Microsoft.EntityFrameworkCore" Version="8.0.1" />
    <PackageVersion Include="Autofac" Version="8.0.0" />
    <PackageVersion Include="AutoMapper" Version="13.0.1" />

    <!-- 测试包版本 -->
    <PackageVersion Include="xunit" Version="2.6.6" />
    <PackageVersion Include="Moq" Version="4.20.70" />
    <PackageVersion Include="FluentAssertions" Version="6.12.0" />
  </ItemGroup>
</Project>
```

```xml
<!-- 项目 .csproj 中只需要包名，不需要版本 -->
<ItemGroup>
  <PackageReference Include="MQTTnet" />
  <PackageReference Include="Serilog.AspNetCore" />
</ItemGroup>
```

> [!important] 中央包管理的优势
> 对于 IoTGateway 的 21 个项目，中央包管理确保所有项目使用相同版本的依赖。当一个包需要升级时，只需修改一处，避免版本碎片化。

### 3.2 包版本冲突诊断

```bash
# 检查传递依赖树
dotnet list IoTGateway.Host package --include-transitive

# 输出示例：
# > MQTTnet/4.3.3.952
#   > System.Memory/4.5.5 (传递依赖)
# > HslCommunication/12.2.0
#   > System.Memory/4.5.4 (传递依赖) ← 版本冲突！
```

**解决版本冲突：**

```xml
<!-- 方法 1：显式引用冲突的包，指定统一版本 -->
<ItemGroup>
  <PackageReference Include="System.Memory" Version="4.5.5" />
</ItemGroup>

<!-- 方法 2：在 Directory.Packages.props 中统一版本 -->
<PackageVersion Include="System.Memory" Version="4.5.5" />
```

### 3.3 包降级警告

```
NU1605: 检测到包降级：
  IoTGateway.Host -> MQTTnet 4.3.3.952 -> System.Memory 4.5.5
  IoTGateway.Host -> HslCommunication 12.2.0 -> System.Memory 4.5.4

解决方法：将 System.Memory 显式升级到 4.5.5
```

---

## 四、创建本地 NuGet 源

### 4.1 本地文件夹源

对于公司内部开发的共享库，可以创建本地 NuGet 源：

```
目录结构：
D:\NuGetPackages\            # 本地包源目录
├── Company.IoT.Common.1.0.0.nupkg
├── Company.IoT.Protocols.2.1.0.nupkg
├── Company.IoT.Drivers.Base.1.3.0.nupkg
└── ...
```

### 4.2 配置本地源

```xml
<!-- NuGet.Config - 解决方案根目录 -->
<?xml version="1.0" encoding="utf-8"?>
<configuration>
  <packageSources>
    <clear />
    <!-- 官方源 -->
    <add key="nuget.org" value="https://api.nuget.org/v3/index.json" />
    <!-- 国内镜像 -->
    <add key="tencent" value="https://mirrors.cloud.tencent.com/nuget/" />
    <!-- 公司内部本地源 -->
    <add key="local" value="D:\NuGetPackages" />
    <!-- 公司内部私有源（网络） -->
    <add key="company-nuget"
         value="https://nuget.company.com/v3/index.json" />
  </packageSources>

  <packageSourceCredentials>
    <company-nuget>
      <add key="Username" value="developer" />
      <add key="ClearTextPassword" value="***" />
    </company-nuget>
  </packageSourceCredentials>
</configuration>
```

### 4.3 创建 NuGet 包

```xml
<!-- Company.IoT.Common.csproj -->
<Project Sdk="Microsoft.NET.Sdk">

  <PropertyGroup>
    <TargetFramework>net8.0</TargetFramework>

    <!-- NuGet 包元数据 -->
    <PackageId>Company.IoT.Common</PackageId>
    <Version>1.0.0</Version>
    <Authors>Your Team</Authors>
    <Company>Your Company</Company>
    <Description>公司 IoT 项目共享库</Description>
    <PackageTags>iot;industrial;communication</PackageTags>
    <PackageLicenseExpression>MIT</PackageLicenseExpression>

    <!-- 生成包 -->
    <GeneratePackageOnBuild>true</GeneratePackageOnBuild>
    <PackageOutputPath>D:\NuGetPackages</PackageOutputPath>
  </PropertyGroup>

</Project>
```

```bash
# 命令行打包
dotnet pack -c Release
dotnet pack -c Release -o D:\NuGetPackages

# 推送到私有源
dotnet nuget push Company.IoT.Common.1.0.0.nupkg \
  --source https://nuget.company.com/v3/index.json \
  --api-key YOUR_API_KEY
```

---

## 五、NuGet 包还原问题排查

### 5.1 常见还原错误

| 错误 | 原因 | 解决方案 |
|------|------|---------|
| `Unable to find package` | 包源不可用或包名错误 | 检查 NuGet.Config 和包名 |
| `401 Unauthorized` | 私有源认证失败 | 更新 NuGet.Config 中的凭据 |
| `The HTTP request to 'GET ...' has timed out` | 网络超时 | 添加国内镜像源 |
| `NU1101: Unable to find package` | 版本不存在 | 检查包版本是否正确 |
| `NU1301: Unable to load the service index` | 源不可达 | 检查网络连接和源 URL |

### 5.2 清理 NuGet 缓存

```bash
# 清理所有缓存
dotnet nuget locals all --clear

# 仅清理 HTTP 缓存
dotnet nuget locals http-cache --clear

# 仅清理全局包缓存
dotnet nuget locals global-packages --clear

# 仅清理临时缓存
dotnet nuget locals temp --clear
```

```
缓存位置（Windows）：
全局包：%userprofile%\.nuget\packages\
HTTP 缓存：%LocalAppData%\NuGet\v3-cache
插件缓存：%LocalAppData%\NuGet\plugins-cache
```

### 5.3 离线开发环境

在没有网络的工控环境中，需要离线包还原：

```bash
# 第一步：在有网络的环境中下载所有包
dotnet restore --packages D:\OfflinePackages

# 第二步：将 OfflinePackages 文件夹复制到离线环境

# 第三步：在离线环境中配置本地源
# NuGet.Config 中添加：
<add key="offline" value="D:\OfflinePackages" />

# 第四步：正常还原
dotnet restore
```

---

## 六、私有包源搭建

### 6.1 使用 BaGet（轻量级 NuGet 服务器）

```bash
# 安装 BaGet
dotnet tool install --global baget

# 启动 BaGet
baget

# 默认地址：http://localhost:5000
```

```bash
# 推送包到 BaGet
dotnet nuget push Company.IoT.Common.1.0.0.nupkg \
  --source http://localhost:5000/v3/index.json \
  --api-key YOUR_KEY
```

### 6.2 使用 Azure Artifacts

对于需要云托管的团队：

```xml
<!-- NuGet.Config -->
<packageSources>
  <add key="AzureArtifacts"
       value="https://pkgs.dev.azure.com/your-org/your-project/_packaging/your-feed/nuget/v3/index.json" />
</packageSources>
```

### 6.3 GitHub Packages

```xml
<!-- NuGet.Config -->
<packageSources>
  <add key="github"
       value="https://nuget.pkg.github.com/your-org/index.json" />
</packageSources>
<packageSourceCredentials>
  <github>
    <add key="Username" value="your-username" />
    <add key="ClearTextPassword" value="***" />
  </github>
</packageSourceCredentials>
```

---

## 七、驱动插件的依赖管理

### 驱动插件的特殊性

驱动插件作为独立 DLL 加载，其依赖管理需要注意：

```xml
<!-- Driver.Modbus.csproj -->
<ItemGroup>
  <!-- 驱动专属的依赖 - 会被复制到 drivers 目录 -->
  <PackageReference Include="NModbus" Version="3.0.62" />

  <!-- 共享的依赖 - 不需要复制（主程序已经有了） -->
  <PackageReference Include="Microsoft.Extensions.Logging">
    <PrivateAssets>all</PrivateAssets>
    <ExcludeAssets>runtime</ExcludeAssets>
  </PackageReference>
</ItemGroup>
```

### 依赖隔离原则

```
驱动 DLL 依赖原则：

1. 驱动专属的第三方库（如 NModbus、S7.Net）
   → 复制到 drivers 目录
   → 只被该驱动使用

2. 共享的基础库（如 Logging、DI）
   → 不复制（PrivateAssets=all）
   → 使用主程序提供的版本

3. 避免驱动之间共享第三方库
   → 每个驱动应该是自包含的
   → 减少版本冲突
```

### 加载驱动时的依赖解析

```csharp
// 主程序加载驱动插件时的依赖解析
public class PluginLoader
{
    public Assembly LoadPlugin(string dllPath)
    {
        var pluginDir = Path.GetDirectoryName(dllPath);

        // 自定义 AssemblyLoadContext 来解析驱动目录下的依赖
        var context = new PluginLoadContext(pluginDir);
        return context.LoadFromAssemblyPath(dllPath);
    }
}

public class PluginLoadContext : AssemblyLoadContext
{
    private readonly AssemblyDependencyResolver _resolver;

    public PluginLoadContext(string pluginPath)
    {
        _resolver = new AssemblyDependencyResolver(pluginPath);
    }

    protected override Assembly Load(AssemblyName assemblyName)
    {
        // 先从驱动目录加载
        var path = _resolver.ResolveAssemblyToPath(assemblyName);
        if (path != null)
            return LoadFromAssemblyPath(path);

        // 找不到则回退到默认上下文（主程序的依赖）
        return null;
    }
}
```

---

## 八、包安全和审计

### 8.1 检查包漏洞

```bash
# 检查已安装包的已知漏洞
dotnet list package --vulnerable

# 输出示例：
# 以下包存在已知漏洞：
# [net8.0]
# Top-level Package      Requested   Resolved   Severity
# > System.Text.Json     6.0.0       6.0.0      High
#   Upgrade to >= 8.0.1 to address this vulnerability.
```

### 8.2 包锁定文件

使用 `packages.lock.json` 确保可重现的构建：

```xml
<!-- .csproj 或 Directory.Build.props -->
<PropertyGroup>
  <RestorePackagesWithLockFile>true</RestorePackagesWithLockFile>
  <RestoreLockedMode Condition="'$(CI)' == 'true'">true</RestoreLockedMode>
</PropertyGroup>
```

```json
// packages.lock.json（自动生成）
{
  "version": 1,
  "dependencies": {
    "net8.0": {
      "MQTTnet": {
        "type": "Direct",
        "requested": "[4.3.3.952, )",
        "resolved": "4.3.3.952"
      },
      "System.Memory": {
        "type": "Transitive",
        "resolved": "4.5.5"
      }
    }
  }
}
```

> [!tip] CI/CD 中使用锁定模式
> 在 CI/CD 环境中使用 `RestoreLockedMode`，确保每次构建使用完全相同的包版本。如果包版本发生变化，构建会失败而不是静默更新。

---

## 九、常见问题速查

### Q: 如何查看所有项目的包版本是否一致？

```bash
# 使用 dotnet list 逐个检查
for proj in $(find . -name "*.csproj"); do
  echo "=== $proj ==="
  dotnet list "$proj" package
done

# 或使用 VS 的"管理解决方案的 NuGet 程序包" → "合并"选项卡
```

### Q: 如何强制所有项目使用最新版本的某个包？

```bash
# 更新解决方案中所有项目的特定包
dotnet add IoTGateway.sln package MQTTnet --version 4.3.3.952
# 或使用中央包管理，修改 Directory.Packages.props 中的版本
```

### Q: 包安装后找不到命名空间？

```
排查步骤：
1. 确认包已成功安装（检查 .csproj 中的 PackageReference）
2. 尝试重新构建：Ctrl+Shift+B
3. 清理并重建：右键解决方案 → 清理解决方案 → 重新生成
4. 清理 NuGet 缓存：dotnet nuget locals all --clear
5. 检查目标框架兼容性（某些包不支持特定 TFM）
```

### Q: 如何知道哪些包可以安全升级？

```bash
# 列出所有可升级的包及最新版本
dotnet list package --outdated

# 关注以下列：
# - Latest：最新版本
# - Resolved：当前使用版本
# - 主版本号变化可能有破坏性变更
# - 次版本号和修订号变化通常是安全的
```

---

## 小结

NuGet 包管理是上位机项目依赖管理的基础。本章涵盖了：

- VS 中和命令行的包管理操作
- 上位机开发常用的 NuGet 包清单
- 中央包管理（Directory.Packages.props）
- 本地和私有 NuGet 源搭建
- 驱动插件的特殊依赖管理
- 包安全审计和锁定文件

下一章学习 [[05-代码重构与质量工具]]，提升代码质量。

---

上一篇: [[03-项目配置与构建]] | 下一篇: [[05-代码重构与质量工具]]
