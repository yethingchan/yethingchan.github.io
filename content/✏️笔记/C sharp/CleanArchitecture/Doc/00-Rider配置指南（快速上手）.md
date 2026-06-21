## CleanArchitecture 项目 Rider 配置指南

> 本文手把手教你在 JetBrains Rider 中配置和运行 CleanArchitecture 项目。

---

### 一、前置环境准备

在打开 Rider 之前，确保你的机器上安装了以下工具：

#### 1.1 .NET SDK

CleanArchitecture 当前使用 .NET 10，你需要安装对应版本的 SDK。在终端运行以下命令检查：

```bash
dotnet --version
```

如果没有安装或版本过低，前往 https://dotnet.microsoft.com/download 下载最新的 .NET 10 SDK。

如果你暂时不想升级 .NET 10，可以在项目根目录找到 `global.json` 文件修改目标版本（前提是 SDK 已安装）：

```json
{
  "sdk": {
    "version": "10.0.100",
    "rollForward": "latestFeature"
  }
}
```

#### 1.2 EF Core 工具

项目使用了 EF Core 迁移，需要安装 dotnet-ef 全局工具：

```bash
dotnet tool install --global dotnet-ef
```

安装完成后验证：

```bash
dotnet ef --version
```

#### 1.3 .NET Aspire 工作负载

项目使用了 .NET Aspire 进行服务编排，需要安装 Aspire 工作负载：

```bash
dotnet workload install aspire
```

#### 1.4 Node.js（可选）

如果你想运行前端（Angular 或 React），需要安装 Node.js 18+。后端 API 开发不需要。

---

### 二、在 Rider 中打开项目

#### 2.1 打开解决方案

1. 启动 JetBrains Rider
2. 点击 **Open**（或 File → Open）
3. 导航到 CleanArchitecture 克隆目录
4. 选择根目录下的 **`CleanArchitecture.sln`** 文件
5. 点击 **Open as Project**

Rider 会自动识别 .sln 文件并加载整个解决方案。

#### 2.2 等待索引完成

首次打开时，Rider 右下角会显示进度条，表示正在进行以下操作：

- **Restoring NuGet packages** — 下载所有 NuGet 依赖
- **Indexing** — 建立代码索引（类型、符号、引用关系）
- **Analyzing** — 运行 ReSharper 代码分析

这个过程在首次打开可能需要 2-5 分钟（取决于网络速度和机器性能）。在索引完成前，代码导航和补全功能可能不完整，请耐心等待。

#### 2.3 验证 NuGet 恢复

NuGet 恢复通常是自动的。如果没有自动触发，手动操作：

- 右键解决方案 → **Restore NuGet Packages**
- 或者在 NuGet 工具窗口（底部工具栏）中点击刷新按钮

确认所有项目的 NuGet 包都已成功恢复（没有黄色警告图标）。

---

### 三、Rider 核心配置

#### 3.1 选择键盘映射

Rider 提供了多种预设键盘映射：

- **Settings → Keymap**（`Ctrl+Alt+S` 打开 Settings）
- 如果你之前用 Visual Studio：选择 **Visual Studio 2022** 映射
- 如果你用 IntelliJ / WebStorm：选择 **IntelliJ IDEA** 映射
- 如果你用 VS Code：安装 **VSCode Keymap** 插件

建议选 Visual Studio 2022 映射，因为大部分 .NET 教程和社区资源都基于这套键位。

#### 3.2 开启 CamelHumps 搜索

CleanArchitecture 的类名普遍较长（如 `CreateTodoListCommandHandler`），开启 CamelHumps 后可以只输入大写字母来搜索：

1. Settings → Editor → General → Code Completion
2. 勾选 **Match from start**
3. 勾选 **Use CamelHumps words**

之后搜索 `CTLC` 就能定位到 `CreateTodoListCommand`。

#### 3.3 配置文件系统大小写敏感性

Windows 系统下通常不需要特殊配置。但如果你用 macOS/Linux，需要确保：

Settings → Editor → General → 勾选 **Use case-sensitive file names**

#### 3.4 配置 .editorconfig

项目根目录可能包含 `.editorconfig` 文件。Rider 会自动读取并应用其中的代码风格规则。确认设置：

Settings → Editor → Code Style → 底部勾选 **Enable EditorConfig support**

如果 `.editorconfig` 中的设置覆盖了你的本地设置，编辑器底部状态栏会显示黄色警告。

#### 3.5 配置 Actions on Save

让 Rider 在保存文件时自动进行代码清理：

1. Settings → Tools → Actions on Save
2. 勾选 **Reformat code** — 自动格式化
3. 勾选 **Optimize imports** — 移除未使用的 using
4. （可选）勾选 **Cleanup code** — 运行完整的代码清理

---

### 四、Run Configuration 配置

#### 4.1 识别启动项目

CleanArchitecture 有两个启动方式：

| 项目 | 说明 | 推荐场景 |
|------|------|----------|
| **Web** | 直接启动 ASP.NET Core Web API | 日常开发和调试 |
| **AppHost** | 通过 .NET Aspire 启动（含数据库编排） | 完整体验，含可观测性 |

#### 4.2 配置 Web 项目启动

1. 顶部工具栏 → 点击 Run Configuration 下拉框 → **Edit Configurations**
2. 选择 **Web** 项目（或 .NET Launch Settings 下的 Web）
3. 配置：
   - **Launch browser**：勾选，设为 `https://localhost:5001`（或项目配置的端口）
   - **Environment variables**：`ASPNETCORE_ENVIRONMENT=Development`
   - **Working directory**：设为 `src/Web` 目录
4. 点击 **Apply** → **OK**

#### 4.3 配置 AppHost 启动（推荐）

如果你想体验完整的 .NET Aspire 编排：

1. 在 Run Configuration 中选择 **AppHost**
2. AppHost 会自动启动 Web 项目和 SQL Server 容器
3. 启动后会打开 Aspire Dashboard，可以查看分布式追踪、日志和健康检查

#### 4.4 数据库配置

项目默认使用 SQL Server（通过 Aspire 编排）。如果你想使用本地 SQL Server 而不是容器：

1. 打开 `src/Web/appsettings.Development.json`
2. 修改连接字符串：

```json
{
  "ConnectionStrings": {
    "CleanArchitectureDb": "Server=(localdb)\\mssqllocaldb;Database=CleanArchitectureDb;Trusted_Connection=True;MultipleActiveResultSets=true"
  }
}
```

如果使用 SQLite（更轻量）：

```json
{
  "ConnectionStrings": {
    "CleanArchitectureDb": "Data Source=CleanArchitecture.db"
  }
}
```

注意：使用 SQLite 时，项目内置了对 SQLite 的支持，EF Core 迁移会自动适配。

#### 4.5 运行数据库迁移

首次运行前需要初始化数据库：

```bash
# 在终端中执行
dotnet ef database update --project src/Infrastructure --startup-project src/Web
```

或者在 Rider 的 Terminal 工具窗口中运行同样的命令。

如果数据库已经存在但需要重新创建：

```bash
dotnet ef database drop --project src/Infrastructure --startup-project src/Web --force
dotnet ef database update --project src/Infrastructure --startup-project src/Web
```

---

### 五、首次运行与验证

#### 5.1 启动项目

1. 选择 **Web** 或 **AppHost** 作为 Run Configuration
2. 点击绿色三角按钮（或 `Shift+F10`）启动
3. 如果是 Debug 模式，点击虫子图标（或 `Shift+F9`）

#### 5.2 验证运行

启动后，浏览器会自动打开。你应该能看到：

- **Scalar API 文档页面**：项目的 API 文档界面（替代了传统的 Swagger UI）
- **健康检查端点**：访问 `/health` 确认数据库连接正常

#### 5.3 测试 API

通过 Rider 的 HTTP Client 测试：

1. 在 `src/Web` 下创建一个 `Requests` 文件夹
2. 新建 `TodoLists.http` 文件：

```http
### 获取所有 Todo Lists
GET https://localhost:5001/api/TodoLists
Authorization: Bearer {{token}}

### 创建 Todo List
POST https://localhost:5001/api/TodoLists
Content-Type: application/json
Authorization: Bearer {{token}}

{
  "title": "我的第一个列表"
}
```

注意：首次测试需要先通过 `/api/Identity/register` 和 `/api/Identity/login` 获取 token。

#### 5.4 运行测试

在 Rider 的 Unit Tests 工具窗口中：

1. `Ctrl+;, L` 运行所有测试
2. 确认 Domain.UnitTests 和 Application.UnitTests 全部通过
3. FunctionalTests 需要数据库环境，可能需要先确保数据库已初始化

---

### 六、推荐的 Rider 插件

以下插件对 CleanArchitecture 项目开发有帮助：

| 插件 | 作用 | 安装方式 |
|------|------|----------|
| **Key Promoter X** | 用鼠标操作时弹出对应快捷键提示，帮你记住快捷键 | Settings → Plugins → Marketplace |
| **.ignore** | 管理 .gitignore 文件，提供模板和语法高亮 | Settings → Plugins → Marketplace |
| **EnvFile** | 从文件加载环境变量到 Run Configuration | Settings → Plugins → Marketplace |
| **Rainbow Brackets** | 用不同颜色标记嵌套括号，提升可读性 | Settings → Plugins → Marketplace |
| **String Manipulation** | 字符串大小写转换、驼峰/蛇形互转 | Settings → Plugins → Marketplace |

---

### 七、Rider 工具窗口布局建议

CleanArchitecture 项目是分层架构，建议以下布局：

```
┌──────────────────────────────────────────────────────────┐
│                    主编辑器区域                            │
│               （代码编辑、断点调试）                        │
├──────────┬──────────────────────────────┬────────────────┤
│ Solution │                              │   Structure    │
│ Explorer │                              │   (Alt+7)      │
│ (Alt+1)  │                              │   或 Git       │
│          │                              │   (Alt+9)      │
├──────────┴──────────────────────────────┴────────────────┤
│ Terminal / Run / Unit Tests / Database / HTTP Client     │
│                    （底部工具栏）                          │
└──────────────────────────────────────────────────────────┘
```

常用工具窗口快捷键：

| 窗口 | 快捷键 | 用途 |
|------|--------|------|
| Solution Explorer | `Alt+1` | 浏览项目结构 |
| Find in Files | `Ctrl+Shift+F` | 全局搜索 |
| Structure | `Alt+7` | 当前文件大纲 |
| Git | `Alt+9` | 版本控制 |
| Run | `Alt+8`（或底部图标） | 运行/调试输出 |
| Terminal | `Alt+F12` | 内置终端 |
| Database | 底部工具栏 | 数据库浏览器 |

---

### 八、调试配置

#### 8.1 设置条件断点

调试 MediatR Handler 时，你可能只想在特定条件下中断：

1. 在 Handler 的 `Handle` 方法入口设置断点（`Ctrl+F8`）
2. 右键断点 → 输入条件表达式，如：`request.Title == "Shopping"`
3. 只有当请求的 Title 是 "Shopping" 时才会中断

#### 8.2 异常断点

在调试管道行为时，当特定异常抛出时自动中断：

1. Run → View Breakpoints（`Ctrl+Shift+F8`）
2. 点击 + → **.NET Exception Breakpoints**
3. 输入异常类型：`CleanArchitecture.Application.Common.Exceptions.ValidationException`
4. 选择 **Thrown**（抛出时中断）

#### 8.3 调试 EF Core SQL

查看 EF Core 实际生成的 SQL 语句：

1. 在 `appsettings.Development.json` 中添加日志级别配置：

```json
{
  "Logging": {
    "LogLevel": {
      "Default": "Information",
      "Microsoft.EntityFrameworkCore.Database.Command": "Information"
    }
  }
}
```

2. 运行项目后，在 Rider 的 Run 工具窗口中可以看到 EF Core 输出的 SQL 语句。

---

### 九、性能优化

#### 9.1 加速 Rider 启动

如果解决方案很大，可以通过以下方式加速：

1. Settings → Build, Execution, Deployment → Toolset and Build
2. **Build tool**：选择 **.NET CLI**（而非 MSBuild）
3. Settings → Editor → General → 取消勾选 **Reformat on paste**（粘贴时不自动格式化）

#### 9.2 排除不需要分析的目录

如果 `node_modules`（前端项目）或 `bin/obj` 目录影响了索引速度：

1. 右键目录 → **Mark Directory as** → **Excluded**
2. 或在 Settings → Editor → File Types → Ignored Files and Folders 中添加

#### 9.3 Solution-Wide Analysis

Rider 默认会分析整个解决方案。如果机器性能有限，可以缩小分析范围：

1. 底部状态栏的代码分析指示器
2. 点击 → 选择 **Current Project** 而不是 **Solution-wide**

---

### 十、常用 Rider 快捷键速查表（CleanArchitecture 场景）

| 场景 | 快捷键 | 说明 |
|------|--------|------|
| 从接口跳到实现 | `Ctrl+Alt+B` | 从 `IApplicationDbContext` → `ApplicationDbContext` |
| 从 Handler 跳到端点 | `Alt+F7` | 查找 `CreateTodoListCommand` 被 `Send` 的位置 |
| 查看继承链 | `Ctrl+H` | `BaseEntity` → `BaseAuditableEntity` → `TodoItem` |
| 搜索所有类 | `Shift Shift` | 输入类名的几个大写字母即可 |
| 生成构造函数 | `Alt+Insert` → Constructor | 依赖注入时自动生成 |
| 重命名 | `Shift+F6` | 安全重命名，自动更新所有引用 |
| 提取方法 | `Ctrl+Alt+M` | 将 Handler 中的复杂逻辑提取为方法 |
| 运行测试 | `Ctrl+;, R` | 运行选中的测试 |
| 格式化代码 | `Ctrl+R, F` | 重新格式化当前文件 |
| 打开终端 | `Alt+F12` | 运行 dotnet CLI 命令 |
| HTTP 请求 | 新建 .http 文件 | 替代 Postman 测试 API |
