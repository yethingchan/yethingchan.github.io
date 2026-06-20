## JetBrains Rider 开发技巧

> 本文是 CleanArchitecture 深度教程的第五篇，介绍如何使用 JetBrains Rider 高效开发 .NET 后端项目。
> 适用于 CleanArchitecture 项目以及所有 C# / ASP.NET Core 项目。

---

### 一、为什么选择 Rider 而不是 Visual Studio

JetBrains Rider 基于 ReSharper 的分析引擎，在 C# 代码智能分析方面远超 Visual Studio。对于 CleanArchitecture 这种大量使用泛型接口（`IRequestHandler<TRequest, TResponse>`）、管道行为（`IPipelineBehavior<TRequest, TResponse>`）和反射的项目，Rider 的类型推断和导航能力可以节省大量时间。

Rider 跨平台支持（Windows/macOS/Linux）也是重要优势，尤其是在 .NET Aspire 和容器化开发场景下。

---

### 二、代码导航：在分层架构中快速跳转

CleanArchitecture 的多层结构意味着你经常需要在 Domain → Application → Infrastructure → Web 之间跳转。以下导航技巧至关重要。

#### 2.1 核心导航快捷键

| 操作 | 快捷键 | 使用场景 |
|------|--------|----------|
| **Search Everywhere** | `Shift Shift` | 跨层查找任何类/文件/符号 |
| **Go to Type** | `Ctrl+N` | 快速定位 TodoItem、BaseEntity 等类型 |
| **Go to Symbol** | `Ctrl+Alt+Shift+N` | 查找方法、属性等符号 |
| **Go to File** | `Ctrl+Shift+N` | 直接跳转到特定文件 |
| **Go to Implementation** | `Ctrl+Alt+B` | 从 IApplicationDbContext 跳转到 ApplicationDbContext |
| **Go to Derived Symbols** | `Ctrl+Alt+B` | 从 BaseEntity 查看所有继承类 |
| **Find Usages** | `Alt+F7` | 查找某个 Command 在哪些地方被 Send |
| **Type Hierarchy** | `Ctrl+H` | 查看 BaseEntity 的完整继承树 |
| **Go to Base** | `Ctrl+U` | 从 TodoItem 跳到 BaseAuditableEntity |

#### 2.2 CamelHumps 搜索

开启 CamelHumps 后，搜索符号时只需输入大写字母。比如要找到 `CreateTodoListCommandHandler`，只需输入 `CTLC` 或 `CTLCH`。在 CleanArchitecture 这种类名较长的项目中，这个功能可以节省大量时间。

设置路径：Settings → Editor → General → Code Completion → 勾选 "Match from start" 和 CamelHumps。

#### 2.3 Navigate To 上下文菜单

在任意类/方法上按 `Ctrl+Shift+G`（或通过右键菜单），会弹出 Navigate To 菜单，提供精确的跳转选项：

- **Declaration** — 跳到定义
- **Implementation** — 跳到实现（特别有用：从 `IRequestHandler<T>` 跳到具体 Handler）
- **Consuming APIs** — 查看哪些地方调用了这个方法
- **Base Symbols / Derived Symbols** — 继承链导航
- **Containing Declaration** — 跳到包含当前元素的声明

#### 2.4 定位当前文件在 Solution 中的位置

按 `Ctrl+J, P`（Locate in Solution View）可以在 Solution Explorer 中高亮当前打开的文件。在 CleanArchitecture 的多层目录结构中，这个功能帮助你快速了解当前文件在整个解决方案中的位置。

#### 2.5 Structure 窗口

`Alt+7` 打开 Structure 窗口，显示当前文件的完整大纲：类、方法、属性、字段。在 Handler + Validator + Command 同文件的模式下，Structure 窗口可以快速定位到特定的类。

---

### 三、重构：安全地修改代码结构

Rider 提供 60+ 种重构操作和 450+ 种上下文操作。以下是在 CleanArchitecture 项目中常用的重构。

#### 3.1 常用重构快捷键

| 重构 | 快捷键 | 使用场景 |
|------|--------|----------|
| **Refactor This** | `Ctrl+Alt+Shift+T` | 打开重构菜单 |
| **Rename** | `Shift+F6` | 重命名类/方法/变量，自动更新所有引用 |
| **Extract Method** | `Ctrl+Alt+M` | 将 Handler 中的复杂逻辑提取为独立方法 |
| **Introduce Variable** | `Ctrl+Alt+V` | 提取临时变量 |
| **Introduce Field** | `Ctrl+Alt+F` | 从局部变量提升为类字段 |
| **Introduce Parameter** | `Ctrl+Alt+P` | 将硬编码值提升为方法参数 |
| **Inline** | `Ctrl+Alt+N` | 内联变量或方法 |
| **Safe Delete** | `Alt+Delete` | 安全删除，检查是否有引用 |
| **Change Signature** | `Ctrl+F6` | 修改方法签名 |
| **Move** | `F6` | 将类移动到其他命名空间/项目 |

#### 3.2 Move to Separate File

在 CleanArchitecture 中，如果你决定将 Handler、Validator 和 Command 拆分到不同文件，可以使用 `Ctrl+Alt+Shift+T` → "Move Type into Matching File"，Rider 会自动将类移动到以类名命名的新文件中，并更新命名空间。

#### 3.3 参数自动生成字段

在构造函数中，将光标放在参数上，按 `Alt+Enter`，选择 "Create and assign field"，Rider 会自动生成私有字段和赋值语句。这在编写依赖注入的构造函数时非常高效。

#### 3.4 批量重命名

`Shift+F6` 重命名时，Rider 会搜索整个解决方案中的所有引用（包括字符串字面量中的引用），并提供预览窗口让你在应用前检查每一处修改。

---

### 四、代码生成与 Live Templates

#### 4.1 Generate 菜单（Alt+Insert）

在编辑器中按 `Alt+Insert` 可以生成：

- **Constructor** — 自动生成包含所有字段的构造函数（依赖注入时极有用）
- **Properties** — 从字段生成属性
- **Equality Members** — 生成 Equals/GetHashCode（值对象开发时常用）
- **ToString()** — 自动生成 ToString 方法
- **Implement Members** — 实现接口中的所有成员
- **Missing Members** — 实现基类中的抽象成员

#### 4.2 高效 Live Templates

| 模板 | 输入 | 效果 | 使用场景 |
|------|------|------|----------|
| `prop` | prop + Tab | `public string Name { get; set; }` | 定义实体属性 |
| `propg` | propg + Tab | `public string Name { get; private set; }` | 值对象属性 |
| `ctor` | ctor + Tab | 构造函数 | 依赖注入 |
| `cw` | cw + Tab | `Console.WriteLine()` | 快速调试 |
| `class` | class + Tab | 类定义 | 新建类 |
| `foreach` | foreach + Tab | foreach 循环 | 遍历集合 |
| `try` | try + Tab | try-catch 块 | 异常处理 |
| `using` | using + Tab | using 语句 | 资源释放 |
| `iterator` | iterator + Tab | yield return 迭代器 | 值对象 GetEqualityComponents |

#### 4.3 自定义 Live Template 建议

针对 CleanArchitecture 项目，可以创建以下自定义模板：

**Command 模板** (`cmd`)：
```csharp
public record $NAME$Command : IRequest<$RESULT$>
{
    $PROPERTIES$
}

public class $NAME$CommandValidator : AbstractValidator<$NAME$Command>
{
    public $NAME$CommandValidator()
    {
        $RULES$
    }
}

public class $NAME$CommandHandler : IRequestHandler<$NAME$Command, $RESULT$>
{
    private readonly IApplicationDbContext _context;

    public $NAME$CommandHandler(IApplicationDbContext context)
    {
        _context = context;
    }

    public async Task<$RESULT$> Handle($NAME$Command request, CancellationToken cancellationToken)
    {
        $END$
    }
}
```

**Query 模板** (`qry`)：
```csharp
public record $NAME$Query : IRequest<$RESULT$>
{
    $PROPERTIES$
}

public class $NAME$QueryHandler : IRequestHandler<$NAME$Query, $RESULT$>
{
    private readonly IApplicationDbContext _context;

    public $NAME$QueryHandler(IApplicationDbContext context)
    {
        _context = context;
    }

    public async Task<$RESULT$> Handle($NAME$Query request, CancellationToken cancellationToken)
    {
        $END$
    }
}
```

创建路径：Settings → Editor → Live Templates → C# → 点击 + 号添加。

---

### 五、调试技巧

#### 5.1 断点类型

| 断点类型 | 使用场景 |
|----------|----------|
| **行断点** (`Ctrl+F8`) | 在 Handler 中设置，观察 Command 的处理过程 |
| **条件断点** (右键断点) | 只在特定 TodoList Id 时中断 |
| **异常断点** | 当 ValidationException 抛出时自动中断 |
| **方法断点** | 在 SaveChangesAsync 的入口/出口中断 |
| **命中计数** | 在循环中只在第 N 次迭代时中断 |
| **Tracepoint** | 不中断执行，只输出日志（适合生产调试） |

#### 5.2 调试 MediatR 管道

由于管道行为是链式调用的，调试时可能会在多个 Behavior 中跳转。建议：

1. 在目标 Handler 的 `Handle` 方法中设置断点
2. 使用 `Step Over`（F8）而不是 `Step Into`（F7）跳过管道行为
3. 如果需要查看管道行为内部的逻辑，在特定的 Behavior 中设置条件断点

#### 5.3 Evaluate Expression（Alt+F8）

在调试暂停时，按 `Alt+F8` 打开表达式求值窗口。你可以在这里：
- 执行 LINQ 查询查看数据库状态
- 调用方法测试返回值
- 检查 MediatR 管道中 request 的具体内容
- 使用 `$[label]` 语法引用被追踪的对象

#### 5.4 线程控制（2024.3+）

调试异步代码时，多个线程可能导致调试器在无关线程上中断。使用 Rider 的线程冻结功能：在 Threads 窗口中冻结不相关的线程，只保留当前请求的处理线程。

---

### 六、单元测试

#### 6.1 运行测试

| 操作 | 快捷键 |
|------|--------|
| 运行所有测试 | `Ctrl+;, L` |
| 运行选中测试 | `Ctrl+;, R` |
| 调试选中测试 | `Ctrl+;, D` |
| 重复上次运行 | `Ctrl+;, T` |
| 只运行失败的测试 | `Ctrl+;, F` |

#### 6.2 Gutter 图标

测试方法左侧的 gutter 图标可以直接运行或调试单个测试。在 CleanArchitecture 中，Domain.UnitTests 的测试运行速度极快（毫秒级），适合频繁运行。

#### 6.3 持续测试

开启 Continuous Testing 后，Rider 会在你修改代码时自动重新运行受影响的测试。这对于重构 Handler 时特别有用——你可以在修改代码的同时看到测试结果实时更新。

#### 6.4 代码覆盖率

运行测试时选择 "Cover" 模式（而非 Run 或 Debug），Rider 会通过 dotCover 收集代码覆盖率数据。覆盖率结果以 gutter 标记的形式显示在代码编辑器中：绿色表示已覆盖，红色表示未覆盖。

---

### 七、HTTP Client：测试 API 端点

Rider 内置的 HTTP Client 可以替代 Postman 来测试 API。

#### 7.1 创建 .http 文件

在 Web 项目下创建 `Requests` 文件夹，添加 `.http` 文件：

```http
### Get all Todo Lists
GET {{HostAddress}}/api/TodoLists
Authorization: Bearer {{AuthToken}}

### Create a Todo List
POST {{HostAddress}}/api/TodoLists
Content-Type: application/json
Authorization: Bearer {{AuthToken}}

{
  "title": "Shopping List"
}

### Update a Todo List
PUT {{HostAddress}}/api/TodoLists/1
Content-Type: application/json
Authorization: Bearer {{AuthToken}}

{
  "id": 1,
  "title": "Updated Shopping List"
}

### Delete a Todo List
DELETE {{HostAddress}}/api/TodoLists/1
Authorization: Bearer {{AuthToken}}
```

#### 7.2 环境变量

创建 `http-client.env.json`：
```json
{
  "dev": {
    "HostAddress": "https://localhost:5001",
    "AuthToken": "eyJhbGciOi..."
  },
  "staging": {
    "HostAddress": "https://staging.example.com",
    "AuthToken": "eyJhbGciOi..."
  }
}
```

敏感信息（如 AuthToken）放在 `http-client.private.env.json` 中，该文件应加入 `.gitignore`。

#### 7.3 Endpoints 工具窗口

Rider 的 Endpoints 工具窗口会自动扫描项目中的所有 API 端点。双击端点可以直接跳转到源代码，也可以直接在窗口中发送请求进行测试。

---

### 八、Git 集成

#### 8.1 三向合并工具

解决合并冲突时，Rider 的三向合并工具提供三个面板：左侧（你的修改）、右侧（对方修改）、中间（合并结果）。中间面板是一个完整的编辑器，你可以选择性地接受左侧或右侧的修改，也可以直接编辑合并结果。

#### 8.2 Local History

即使不使用 Git，Rider 也会自动追踪文件的所有修改历史。右键文件 → Local History → Show History 可以查看文件在过去的所有版本，并回滚到任意时间点。这个功能在误删代码或错误重构后特别有用。

#### 8.3 VCS 操作弹窗

按 `Alt+`` 打开 VCS 操作弹窗，快速访问 Commit、Push、Pull、Branch、Merge 等所有版本控制操作。

---

### 九、数据库工具

Rider 内置 DataGrip 的数据库功能，可以直接连接 SQL Server / PostgreSQL / SQLite 等数据库。

#### 9.1 连接数据库

打开 Database 工具窗口 → 点击 + → Data Source → 选择数据库类型 → 配置连接字符串。

#### 9.2 查看 EF Core 生成的 Schema

连接数据库后，可以在 Database 工具窗口中浏览 EF Core 迁移生成的表结构、索引、外键约束等。双击表名可以查看 DDL，右键可以查看/编辑数据。

#### 9.3 SQL 编辑器

Rider 的 SQL 编辑器支持智能补全、语法高亮、执行计划查看。可以直接在编辑器中编写查询来验证 Handler 的数据操作是否正确。

---

### 十、性能分析

#### 10.1 dotTrace 集成

Rider 集成了 dotTrace 性能分析器。如果怀疑某个 Handler 执行缓慢，可以通过 Profile 功能找出瓶颈：

1. 右键 Run Configuration → Profile
2. 选择 Timeline 模式
3. 执行目标操作
4. 查看 Call Tree 找出耗时最长的方法

#### 10.2 性能分析场景

- **EF Core 查询**：检查是否有 N+1 查询问题
- **MediatR 管道**：检查哪个 Behavior 耗时最长
- **序列化**：检查 JSON 序列化的性能开销

---

### 十一、自定义代码风格

#### 11.1 配置 C# 代码风格

Settings → Editor → Code Style → C# 中可以配置：

- **Tabs and Indents**：缩进大小、是否使用 Tab
- **Braces Layout**：大括号是否换行
- **Blank Lines**：方法间的空行数
- **Spaces**：运算符两侧的空格
- **Naming**：命名约定（如私有字段使用 `_camelCase`）
- **Syntax Style**：var vs 显式类型、表达式体成员

#### 11.2 .editorconfig 支持

Rider 完整支持 `.editorconfig` 文件。在解决方案根目录放置 `.editorconfig`，团队成员使用不同 IDE 也能保持一致的代码风格。

#### 11.3 Code Cleanup

`Ctrl+R, C` 运行 Full Cleanup，自动格式化代码、应用命名约定、移除未使用的 using、修复代码风格问题。`Ctrl+R, F` 仅重新格式化。

建议在保存时自动运行 Code Cleanup：Settings → Tools → Actions on Save → 勾选 "Cleanup code"。

---

### 十二、高效开发的其他技巧

#### 12.1 剪贴板历史

`Ctrl+Shift+V` 打开剪贴板历史，可以访问最近复制的所有内容。在复制粘贴 Handler 模板并修改时非常有用。

#### 12.2 扩展/收缩选择

`Ctrl+W` 逐步扩展选择范围（单词 → 表达式 → 语句 → 代码块 → 方法 → 类），`Ctrl+Shift+W` 收缩选择。这在选中特定代码块进行重构时非常高效。

#### 12.3 语句补全

`Ctrl+Shift+Enter` 自动补全当前语句所需的所有语法元素（分号、大括号、括号等）。在快速编写 Handler 时减少手动补全的时间。

#### 12.4 Find Action（命令面板）

`Ctrl+Shift+A` 打开命令面板，可以通过名称搜索并执行任何 Rider 命令。当你忘记某个功能的快捷键时，用这个搜索即可。

#### 12.5 Solution-Wide Error Analysis

Rider 底部状态栏有一个代码分析指示器，实时显示整个解决方案中的错误和警告数量。在重构时，这个指示器可以立即告诉你是否引入了新问题。
