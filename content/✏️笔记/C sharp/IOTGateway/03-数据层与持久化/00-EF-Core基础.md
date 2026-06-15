## 相关链接

- [[01-分层架构设计]] - DataContext在分层架构中的位置
- [[01-多数据库支持]] - 多数据库配置详解
- [[02-数据建模与关系]] - 实体模型与关系设计
- [[03-数据库迁移实践]] - 迁移创建与应用
- [[04-数据初始化与种子数据]] - DataInit与种子数据
- [[依赖注入与服务管理]] - DataContext注册方式

---

## EF Core 基础

### 什么是 Entity Framework Core

Entity Framework Core（EF Core）是 .NET 生态中最主流的 ORM（对象关系映射）框架。它将数据库表映射为 C# 类，将 SQL 查询转化为 LINQ 表达式，让开发者可以用面向对象的方式操作数据库，而无需手写 SQL。

对于上位机开发者来说，EF Core 的价值在于：

- **降低数据库门槛**：不需要精通 SQL 也能进行数据操作
- **多数据库透明切换**：同一套代码支持 SQLite、MySQL、PostgreSQL 等
- **类型安全**：编译期就能发现字段名错误，而非运行时
- **自动迁移**：实体模型变更后自动生成数据库变更脚本

### IoTGateway 的 DataContext

IoTGateway 使用 `DataContext` 类作为 EF Core 的入口，它继承自 WTM 框架的 `FrameworkContext`：

```csharp
public class DataContext : FrameworkContext
{
    public DbSet<FrameworkUser> FrameworkUsers { get; set; }
    public DbSet<Device> Devices { get; set; }
    public DbSet<DeviceConfig> DeviceConfigs { get; set; }
    public DbSet<DeviceVariable> DeviceVariables { get; set; }
    public DbSet<Driver> Drivers { get; set; }
    public DbSet<SystemConfig> SystemConfig { get; set; }
    public DbSet<RpcLog> RpcLogs { get; set; }

    // 构造函数：连接字符串 + 数据库类型
    public DataContext(string cs, DBTypeEnum dbtype) : base(cs, dbtype) { }
    public DataContext(string cs, DBTypeEnum dbtype, string version = null)
        : base(cs, dbtype, version) { }
    public DataContext(DbContextOptions<DataContext> options) : base(options) { }
}
```

#### DbSet 的含义

`DbSet<T>` 是 EF Core 中对应数据库表的集合。每个 `DbSet` 属性对应一张数据库表：

| DbSet 属性 | 对应数据库表 | 实体类型 |
|-----------|------------|---------|
| `Devices` | Devices | Device |
| `DeviceConfigs` | DeviceConfigs | DeviceConfig |
| `DeviceVariables` | DeviceVariables | DeviceVariable |
| `Drivers` | Drivers | Driver |
| `SystemConfig` | SystemConfig | SystemConfig |
| `RpcLogs` | RpcLogs | RpcLog |
| `FrameworkUsers` | FrameworkUsers | FrameworkUser |

### FrameworkContext 基类

`FrameworkContext` 是 WTM 框架提供的 DbContext 基类，它除了包含 IoTGateway 自定义的表之外，还自动管理以下框架内置表：

| 框架表 | 用途 |
|--------|------|
| FrameworkUsers | 用户账户 |
| FrameworkUserRoles | 用户角色关联 |
| FrameworkGroups | 用户组 |
| FrameworkMenus | 菜单 |
| FrameworkActions | 操作权限 |
| FrameworkDataPrivileges | 数据权限 |
| PersistedGrants | OAuth持久化授权 |

> **设计优势**：继承 `FrameworkContext` 而非直接使用 `DbContext`，意味着用户管理、权限管理、菜单管理等通用功能无需自己实现。

### 实体配置方式

EF Core 支持两种配置方式，IoTGateway 同时使用了这两种：

#### 方式一：数据注解（Data Annotations）

直接在实体类上使用 Attribute 标注：

```csharp
[Comment("设备维护")]                           // 表注释
[Index(nameof(DeviceName))]                    // 索引
[Index(nameof(AutoStart))]
public class Device : TreePoco<Device>, IBasePoco
{
    [Comment("名称")]                          // 列注释
    [Display(Name = "DeviceName")]             // 显示名称
    public string DeviceName { get; set; }

    [Comment("驱动")]
    [Display(Name = "Driver")]
    public Guid? DriverId { get; set; }         // 外键
    public Driver Driver { get; set; }          // 导航属性

    [NotMapped]                                // 不映射到数据库
    public object Value { get; set; }
}
```

常用的数据注解：

| 注解 | 作用 | IoTGateway 使用场景 |
|------|------|-------------------|
| `[Comment]` | 数据库列/表注释 | 所有实体和字段 |
| `[Display(Name)]` | 前端显示名称 | 所有字段 |
| `[Index]` | 创建数据库索引 | DeviceName, AutoStart等高频查询字段 |
| `[NotMapped]` | 不映射到数据库 | Value, CookedValue等运行时字段 |
| `[Required]` | 必填字段 | 框架内置实体 |
| `[StringLength]` | 字符串长度限制 | FrameworkUser.Email |

#### 方式二：Fluent API（在 OnModelCreating 中）

WTM 框架的 `FrameworkContext` 内部使用 Fluent API 配置框架内置表的详细映射规则。开发者通常不需要手动编写 Fluent API 配置。

### 基本 CRUD 操作

在 IoTGateway 中，CRUD 操作主要通过 ViewModel 层的 `BaseCRUDVM` 基类自动完成。了解底层的 EF Core 操作有助于排查问题和编写自定义逻辑。

#### 查询（Read）

```csharp
// 简单查询
using var dc = new DataContext(connnectSetting, dbType);
var devices = dc.Set<Device>()
    .Where(x => x.DeviceTypeEnum == DeviceTypeEnum.Device)
    .ToList();

// 关联查询（Include）
var devices = dc.Set<Device>()
    .Include(x => x.Driver)
    .Include(x => x.DeviceConfigs)
    .Include(x => x.DeviceVariables)
    .AsNoTracking()  // 只读查询，不跟踪变更（性能优化）
    .ToList();

// 单条查询
var device = dc.Set<Device>()
    .SingleOrDefault(x => x.ID == deviceId);
```

> **`AsNoTracking()` 的重要性**：在只读查询中使用 `AsNoTracking()` 可以避免 EF Core 创建变更跟踪代理，显著提升查询性能。在 IoTGateway 的 `DeviceService.CreateDeviceThreads()` 中，所有查询都使用了 `AsNoTracking()`。

#### 插入（Create）

```csharp
// 通过 ViewModel 自动完成（推荐）
public override void DoAdd()
{
    base.DoAdd(); // 内部调用 dc.Set<Device>().Add(Entity) + SaveChanges()
}

// 手动操作
var rpcLog = new RpcLog
{
    DeviceId = Device.ID,
    StartTime = DateTime.Now,
    Method = e.Method,
    Params = JsonConvert.SerializeObject(e.Params)
};
dc.Set<RpcLog>().Add(rpcLog);
await dc.SaveChangesAsync();
```

#### 更新（Update）

```csharp
// 通过 ViewModel 自动完成
public override void DoEdit(bool updateAllFields = false)
{
    base.DoEdit(updateAllFields);
}

// 手动操作
var device = dc.Set<Device>().Find(deviceId);
device.DeviceName = "NewName";
dc.SaveChanges();
```

#### 删除（Delete）

```csharp
// 通过 ViewModel 自动完成
public override void DoDelete()
{
    base.DoDelete(); // 内部调用 dc.Set<Device>().Remove(Entity) + SaveChanges()
}
```

### DataContext 的创建模式

IoTGateway 中 DataContext 有两种创建方式：

#### 方式一：DI 注入（HTTP 请求中）

```csharp
// WTM 框架自动管理，在 Controller/ViewModel 中通过 DC 属性访问
public class DeviceVM : BaseCRUDVM<Device>
{
    // DC 属性由 BaseVM 提供，是当前请求作用域的 DataContext
    protected override void InitVM()
    {
        AllDrivers = DC.Set<Driver>().GetSelectListItems(Wtm, y => y.FileName);
    }
}
```

#### 方式二：手动创建（后台服务中）

```csharp
// 后台服务中，没有 HTTP 请求上下文，需要手动创建
public void CreateDeviceThreads()
{
    using var dc = new DataContext(
        IoTBackgroundService.connnectSetting,
        IoTBackgroundService.DbType);
    var devices = dc.Set<Device>()
        .Where(x => x.DeviceTypeEnum == DeviceTypeEnum.Device)
        .Include(x => x.Driver)
        .AsNoTracking()
        .ToList();
}
```

> **关键区别**：HTTP 请求中的 `DataContext` 由 DI 容器管理（Scoped），自动在请求结束时释放。后台服务中的 `DataContext` 需要手动 `using` 语句确保释放。

### DesignTimeFactory

EF Core 的迁移工具（`dotnet ef`）在设计时需要能够创建 `DataContext` 实例。`DataContextFactory` 提供了这个能力：

```csharp
public class DataContextFactory : IDesignTimeDbContextFactory<DataContext>
{
    public DataContext CreateDbContext(string[] args)
    {
        return new DataContext(
            "Data Source = ../IoTGateway/data/iotgateway.db",
            DBTypeEnum.SQLite);
    }
}
```

> 这个工厂仅在 `dotnet ef migrations add` 等命令行工具中被使用，运行时不会被调用。

### 性能优化要点

| 优化手段 | 使用场景 | IoTGateway 示例 |
|---------|---------|----------------|
| `AsNoTracking()` | 只读查询 | `CreateDeviceThreads()` 中加载设备 |
| `Include()` 预加载 | 需要关联数据 | 加载设备时Include驱动和变量 |
| `Select()` 投影 | 只需部分字段 | `GetSearchQuery()` 中构建视图模型 |
| `Count()` vs `Any()` | 判断是否存在 | `DataInit()` 中检查空库 |
| 批量操作 | 大量数据插入 | 种子数据初始化 |

### EF Core 在上位机开发中的注意事项

1. **连接池管理**：EF Core 的连接池在高并发场景下非常重要，但后台服务中的手动创建模式不经过连接池
2. **线程安全**：`DataContext` 不是线程安全的，每个线程/任务应该使用独立的 `DataContext` 实例
3. **变更跟踪开销**：对于大量只读查询，务必使用 `AsNoTracking()` 减少内存开销
4. **N+1查询**：避免在循环中执行查询，使用 `Include()` 或 `Join` 替代

---

上一篇: [[07-依赖注入与服务管理]] | 下一篇: [[01-多数据库支持]]
