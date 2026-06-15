## 相关链接

- [[00-架构总览]] - 整体架构鸟瞰
- [[01-分层架构设计]] - 四层架构中的ViewModel层定位
- [[02-插件化架构]] - 插件系统与ViewModel的交互
- [[07-依赖注入与服务管理]] - WTM服务注册机制
- [[EF-Core基础]] - DataContext在ViewModel中的使用
- [[数据建模与关系]] - 实体模型关系设计

---

## MVVM模式应用

### 什么是MVVM

MVVM（Model-View-ViewModel）是WPF时代经典的设计模式，核心思想是将界面逻辑（View）与业务数据（Model）通过中间层（ViewModel）进行解耦。

在传统的上位机开发中，你可能已经习惯了 WinForm 的事件驱动模型——所有逻辑都写在 Form 的代码隐藏文件中。而 MVVM 将这种"大一统"的结构拆分为三个可独立测试和维护的部分。

虽然 IoTGateway 是一个 Web 应用，但 WalkingTec.Mvvm（WTM）框架将 MVVM 模式成功应用到了 ASP.NET Core MVC 中，提供了类似 WPF MVVM 的开发体验。

### WTM框架的MVVM体系

WTM 框架提供了一套完整的 ViewModel 基类体系：

```
BaseVM                         ← 所有ViewModel的基类
├── BaseCRUDVM<TEntity>        ← 增删改查操作
│   ├── DoAdd()                ← 新增实体
│   ├── DoEdit()               ← 编辑实体
│   ├── DoDelete()             ← 删除实体
│   └── SetDuplicatedCheck()   ← 重复性校验
├── BasePagedListVM<T,S>       ← 分页列表查询
│   ├── InitGridAction()       ← 定义操作按钮
│   ├── InitGridHeader()       ← 定义列头
│   └── GetSearchQuery()       ← 定义查询逻辑
└── BaseSearcher               ← 搜索条件
    └── InitVM()               ← 初始化搜索选项
```

### BaseCRUDVM：增删改查的统一抽象

`BaseCRUDVM<T>` 是设备管理中最常用的 ViewModel 基类。它封装了完整的 CRUD 流程，开发者只需重写特定的钩子方法来添加自定义逻辑。

#### 设备ViewModel完整实现

```csharp
public class DeviceVM : BaseCRUDVM<Device>
{
    // 用于前端下拉框的选项列表
    public List<ComboSelectListItem> AllDrivers { get; set; }
    public List<ComboSelectListItem> AllParents { get; set; }

    public DeviceVM()
    {
        // 声明需要关联加载的导航属性
        SetInclude(x => x.Driver);
        SetInclude(x => x.Parent);
    }

    // 初始化方法：在ViewModel创建后自动调用
    protected override void InitVM()
    {
        // 从数据库加载所有驱动，生成下拉选项
        AllDrivers = DC.Set<Driver>().GetSelectListItems(Wtm, y => y.FileName);
        AllParents = DC.Set<Device>()
            .Where(x => x.DeviceTypeEnum == DeviceTypeEnum.Group)
            .GetSelectListItems(Wtm, y => y.DeviceName);
    }

    // 新增设备后的联动操作
    public override void DoAdd()
    {
        base.DoAdd(); // 先执行基类的数据库插入

        if (Entity.DeviceTypeEnum == DeviceTypeEnum.Device)
        {
            // 1. 自动生成驱动的配置参数
            var deviceService = Wtm.ServiceProvider
                .GetService(typeof(DeviceService)) as DeviceService;
            deviceService.DriverManager.AddConfigs(Entity.ID, Entity.DriverId);

            // 2. 创建采集线程
            var device = DC.Set<Device>()
                .Where(x => x.ID == Entity.ID)
                .Include(x => x.Parent)
                .Include(x => x.Driver)
                .Include(x => x.DeviceVariables)
                .SingleOrDefault();
            deviceService.CreateDeviceThread(device);

            // 3. 通知云平台设备已添加
            var messageService = Wtm.ServiceProvider
                .GetService(typeof(MessageService)) as MessageService;
            messageService.DeviceAdded(device);
        }
    }

    // 编辑设备后的联动操作
    public override void DoEdit(bool updateAllFields = false)
    {
        base.DoEdit(updateAllFields);
        var pluginManager = Wtm.ServiceProvider
            .GetService(typeof(DeviceService)) as DeviceService;
        UpdateDevices.UpdateDevice(DC, pluginManager, FC);
    }

    // 删除设备后的联动操作
    public override void DoDelete()
    {
        List<Guid> Ids = new List<Guid>() { Guid.Parse(FC["id"].ToString()) };
        var pluginManager = Wtm.ServiceProvider
            .GetService(typeof(DeviceService)) as DeviceService;
        var messageService = Wtm.ServiceProvider
            .GetService(typeof(MessageService)) as MessageService;
        messageService.DeviceDeleted(Entity);
        var ret = DeleteDevices.doDelete(pluginManager, DC, Ids);
        if (!ret.IsSuccess)
        {
            MSD.AddModelError("", ret.Message);
        }
    }

    // 定义唯一性约束
    public override DuplicatedInfo<Device> SetDuplicatedCheck()
    {
        return CreateFieldsInfo(SimpleField(x => x.DeviceName));
    }
}
```

#### CRUD生命周期

```
DoAdd() 流程：
  1. 数据绑定：HTTP请求参数 → Entity 属性
  2. 校验：Model Validation + DuplicatedCheck
  3. base.DoAdd()：EF Core 插入数据库
  4. 自定义逻辑：创建驱动配置、启动采集线程
  5. 返回结果：成功/失败 + 错误信息

DoEdit() 流程：
  1. 数据绑定：HTTP请求参数 → Entity 属性
  2. 校验：同上
  3. base.DoEdit()：EF Core 更新数据库
  4. 自定义逻辑：更新DeviceThread（先移除后重建）

DoDelete() 流程：
  1. 获取待删除ID
  2. 通知云平台设备已删除
  3. 停止DeviceThread
  4. EF Core 删除数据库记录
```

### BasePagedListVM：列表查询与展示

`BasePagedListVM<TView, TSearcher>` 负责列表页面的数据查询和操作按钮定义。

#### 设备列表ViewModel

```csharp
public class DeviceListVM : BasePagedListVM<Device_View, DeviceSearcher>
{
    // 定义工具栏和行内操作按钮
    protected override List<GridAction> InitGridAction()
    {
        return new List<GridAction>
        {
            // 复制设备（行内按钮，仅设备类型显示）
            this.MakeAction("Device", "Copy", "复制设备", "复制设备",
                GridActionParameterTypesEnum.SingleId, "BasicData", 600)
                .SetIconCls("layui-icon layui-icon-template-1")
                .SetShowInRow(true)
                .SetBindVisiableColName("copy"),

            // 标准CRUD按钮
            this.MakeStandardAction("Device",
                GridActionStandardTypesEnum.Create, "创建设备", "BasicData"),
            this.MakeStandardAction("Device",
                GridActionStandardTypesEnum.Edit, "编辑", "BasicData"),
            this.MakeStandardAction("Device",
                GridActionStandardTypesEnum.Delete, "删除", "BasicData"),
            this.MakeStandardAction("Device",
                GridActionStandardTypesEnum.ExportExcel, "导出", "BasicData"),
        };
    }

    // 定义表格列
    protected override IEnumerable<IGridColumn<Device_View>> InitGridHeader()
    {
        return new List<GridColumn<Device_View>>
        {
            this.MakeGridHeader(x => x.DeviceName).SetWidth(150),
            this.MakeGridHeader(x => x.DriverName_view).SetWidth(150),
            this.MakeGridHeader(x => x.AutoStart).SetWidth(80),
            this.MakeGridHeader(x => x.CgUpload).SetWidth(100),
            this.MakeGridHeader(x => x.DeviceTypeEnum).SetWidth(80),
            this.MakeGridHeaderAction(width: 300)
        };
    }

    // 自定义查询逻辑（树形结构）
    public override IOrderedQueryable<Device_View> GetSearchQuery()
    {
        // 先查所有设备组
        var groups = DC.Set<Device>().AsNoTracking()
            .Where(x => x.DeviceTypeEnum == DeviceTypeEnum.Group)
            .OrderBy(x => x.Index).ToList();

        var dataRet = new List<Device_View>();
        foreach (var group in groups)
        {
            // 添加组节点
            dataRet.Add(new Device_View { ... });
            // 查询并添加组下的设备节点
            var children = DC.Set<Device>().AsNoTracking()
                .Where(y => y.ParentId == group.ID)
                .Include(x => x.Driver)
                .OrderBy(x => x.Index).ToList();
            foreach (var child in children)
            {
                dataRet.Add(new Device_View { DeviceName = "    " + child.DeviceName, ... });
            }
        }
        return dataRet.AsQueryable().OrderBy(x => x.ExtraOrder);
    }
}
```

> **设计要点**：`Device_View` 是一个视图模型类（继承自 `Device`），它添加了 `DriverName_view` 等仅用于展示的字段。这种"实体 + 视图扩展"的模式避免了在实体类中混入展示逻辑。

### BaseSearcher：搜索条件封装

```csharp
public class DeviceSearcher : BaseSearcher
{
    [Display(Name = "名称")]
    public String DeviceName { get; set; }

    [Display(Name = "驱动")]
    public Guid? DriverId { get; set; }

    [Display(Name = "自启动")]
    public Boolean? AutoStart { get; set; }

    [Display(Name = "类型")]
    public DeviceTypeEnum? DeviceTypeEnum { get; set; }

    // 下拉选项数据
    public List<ComboSelectListItem> AllDrivers { get; set; }

    protected override void InitVM()
    {
        AllDrivers = DC.Set<Driver>()
            .GetSelectListItems(Wtm, y => y.DriverName);
    }
}
```

### ViewModel 与 View 的绑定

WTM 框架通过约定来实现 ViewModel 和 View 的自动绑定。

#### 命名约定

```
Controller: DeviceController
  ├── Create  → DeviceVM + Views/Device/Create.cshtml
  ├── Edit    → DeviceVM + Views/Device/Edit.cshtml
  ├── Delete  → DeviceVM + Views/Device/Delete.cshtml
  ├── Details → DeviceVM + Views/Device/Details.cshtml
  └── Index   → DeviceListVM + Views/Device/Index.cshtml
```

#### View 中使用 LayUI 标签

```html
@model IoTGateway.ViewModel.BasicData.DeviceVMs.DeviceVM

<wt:form vm="@Model">
    <wt:row items-per-row="ItemsPerRowEnum.Two">
        <wt:textbox field="Entity.DeviceName" />
        <wt:combobox field="Entity.DriverId" items="Model.AllDrivers" />
    </wt:row>
    <wt:row items-per-row="ItemsPerRowEnum.Two">
        <wt:checkbox field="Entity.AutoStart" />
        <wt:checkbox field="Entity.CgUpload" />
    </wt:row>
    <wt:row align="AlignEnum.Center">
        <wt:submitbutton />
        <wt:closebutton />
    </wt:row>
</wt:form>
```

### ViewModel 中访问运行时服务

ViewModel 层通过 `Wtm.ServiceProvider` 访问 DI 容器中注册的服务，实现配置管理与运行时的联动：

```csharp
// 获取DeviceService（Singleton）
var deviceService = Wtm.ServiceProvider
    .GetService(typeof(DeviceService)) as DeviceService;

// 获取MessageService（Singleton）
var messageService = Wtm.ServiceProvider
    .GetService(typeof(MessageService)) as MessageService;
```

> **注意**：这里的 `DeviceService` 和 `MessageService` 是以 `Singleton` 方式注册的，所有 ViewModel 实例共享同一个服务实例。这确保了Web界面操作能影响到正在运行的采集线程。

### MVVM 在工业上位机中的价值

| 传统做法 | MVVM 做法 | 优势 |
|---------|----------|------|
| Controller直接写EF查询 | ViewModel封装CRUD流程 | 业务逻辑可复用 |
| 前端拼接SQL查询 | GetSearchQuery定义查询 | 类型安全，可测试 |
| 手动构建表格HTML | InitGridHeader声明式定义 | 减少前端代码 |
| 手动校验重复数据 | SetDuplicatedCheck声明式校验 | 声明式验证 |
| 手动管理下拉数据 | InitVM自动初始化 | 生命周期统一管理 |

### 扩展 ViewModel 的实践建议

1. **不要绕过基类方法**：始终先调用 `base.DoAdd()` 再添加自定义逻辑，否则 EF Core 的变更跟踪会失效
2. **使用视图模型类**：列表查询返回 `T_View` 而非 `T`，避免暴露不必要的字段
3. **服务获取要安全**：对 `ServiceProvider.GetService()` 的返回值做 null 检查
4. **异常要捕获**：`DoAdd`/`DoEdit` 中的联动操作可能失败（如驱动加载失败），需要用 `try-catch` 包裹并通过 `MSD.AddModelError` 反馈给用户

---

上一篇: [[02-插件化架构]] | 下一篇: [[04-后台服务与任务调度]]
