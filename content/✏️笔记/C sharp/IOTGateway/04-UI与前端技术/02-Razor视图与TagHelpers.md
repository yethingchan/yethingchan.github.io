## 相关链接

- [[00-上位机UI技术选型]]
- [[01-LayUI框架入门]]
- [[03-ECharts数据可视化]]
- [[04-实时数据推送]]
- [[05-管理后台设计]]
- [[架构总览]]
- [[通信协议总览]]

## Razor视图与WTM TagHelpers

WalkingTec.Mvvm（WTM）框架为ASP.NET Core MVC提供了一套强大的TagHelper体系，使得开发者可以通过声明式的标签语法快速构建CRUD管理页面。在IoTGateway中，90+个视图文件几乎全部基于WTM TagHelper编写，极大地减少了前端样板代码。本章深入解析WTM TagHelper的使用方法和数据绑定机制。

## Razor视图引擎基础

Razor是ASP.NET Core的视图模板引擎，使用`@`符号嵌入C#代码。在IoTGateway中，每个`.cshtml`文件就是一个Razor视图。

### 基本语法

```cshtml
@model IoTGateway.ViewModel.BasicData.DeviceVMs.DeviceVM
@inject IStringLocalizer<Program> Localizer;

<h2>@Model.Entity.DeviceName</h2>
<p>@Localizer["Device.Description"]</p>

@if (Model.Entity.AutoStart)
{
    <span>自动启动已开启</span>
}
```

关键语法说明：
- `@model` - 声明视图绑定的ViewModel类型
- `@inject` - 注入服务（如本地化服务）
- `@{ }` - 代码块
- `@if` / `@foreach` - 控制流

### 视图导入文件

`_ViewImports.cshtml`定义了所有视图共享的引用：

```cshtml
@using WalkingTec.Mvvm.TagHelpers.LayUI
@using WalkingTec.Mvvm.Core
@addTagHelper *, WalkingTec.Mvvm.TagHelpers.LayUI
```

`@addTagHelper`指令让Razor识别WTM的自定义标签（`<wt:*>`前缀），这是TagHelper生效的前提。

## WTM TagHelper总览

WTM提供的TagHelper可以分为以下几类：

| 类别 | TagHelper | 说明 |
|------|-----------|------|
| 布局 | `<wt:row>` | 行容器，控制子元素排列 |
| 表单容器 | `<wt:form>` | 表单容器，绑定ViewModel |
| 搜索面板 | `<wt:searchpanel>` | 搜索条件区域 |
| 数据表格 | `<wt:grid>` | 数据列表表格 |
| 输入控件 | `<wt:textbox>` | 文本输入框 |
| 选择控件 | `<wt:combobox>` | 下拉选择框 |
| 开关控件 | `<wt:switch>` | 布尔开关 |
| 日期控件 | `<wt:datetime>` | 日期时间选择 |
| 显示控件 | `<wt:display>` | 只读显示字段值 |
| 隐藏字段 | `<wt:hidden>` | 隐藏表单字段 |
| 按钮 | `<wt:submitbutton>` | 提交按钮 |
| 按钮 | `<wt:closebutton>` | 关闭按钮 |
| 链接按钮 | `<wt:linkbutton>` | 超链接/弹窗按钮 |
| 图表 | `<wt:chart>` | ECharts图表 |
| 树容器 | `<wt:treecontainer>` | 左侧树形导航 |
| 引用块 | `<wt:quote>` | 引用文本块 |

## 搜索面板 - searchpanel

搜索面板是列表页的标配组件，用于定义筛选条件。以设备管理列表页为例（`Device/Index.cshtml`）：

```cshtml
@model IoTGateway.ViewModel.BasicData.DeviceVMs.DeviceListVM
@inject IStringLocalizer<Program> Localizer;

<wt:searchpanel vm="@Model" reset-btn="true">
    <wt:row items-per-row="ItemsPerRowEnum.Three">
        <wt:textbox field="Searcher.DeviceName" />
        <wt:combobox field="Searcher.DriverId" 
                     items="Searcher.AllDrivers" 
                     empty-text="@Localizer["Sys.All"]" />
        <wt:combobox field="Searcher.AutoStart" 
                     empty-text="@Localizer["Sys.All"]" />
        <wt:combobox field="Searcher.DeviceTypeEnum" 
                     empty-text="@Localizer["Sys.All"]" />
        <wt:combobox field="Searcher.ParentId" 
                     items="Searcher.AllParents" 
                     empty-text="@Localizer["Sys.All"]" />
    </wt:row>
</wt:searchpanel>
<wt:grid vm="@Model" url="/BasicData/Device/Search"/>
```

**属性说明：**

| 属性 | 说明 |
|------|------|
| `vm` | 绑定的ListVM实例 |
| `reset-btn` | 是否显示重置按钮 |
| `items-per-row` | 每行放置的控件数量 |

**`ItemsPerRowEnum`可选值：**
- `ItemsPerRowEnum.One` - 每行1个
- `ItemsPerRowEnum.Two` - 每行2个
- `ItemsPerRowEnum.Three` - 每行3个（搜索面板常用）
- `ItemsPerRowEnum.Four` - 每行4个

## 数据表格 - grid

`<wt:grid>`是WTM中最核心的组件，它将ViewModel中的数据列表渲染为LayUI表格：

```cshtml
<!-- 基础用法 -->
<wt:grid vm="@Model" url="/BasicData/Device/Search"/>

<!-- 高级用法 -->
<wt:grid vm="@Model" 
         url="/Rpc/RpcLog/Search" 
         hidden-checkbox="false" 
         hidden-grid-index="true" />

<!-- 带回调函数 -->
<wt:grid vm="@Model" 
         url="/BasicData/DeviceVariable/Search" 
         hidden-grid-index="true" 
         done-func="subTopics" />
```

**属性说明：**

| 属性 | 说明 |
|------|------|
| `vm` | 绑定的ListVM实例 |
| `url` | 数据加载的API地址 |
| `hidden-checkbox` | 隐藏复选框列 |
| `hidden-grid-index` | 隐藏序号列 |
| `done-func` | 表格渲染完成后的回调函数名 |
| `multi-line` | 多行显示模式 |

### done-func的妙用

在设备变量页面中，`done-func="subTopics"`用于在表格渲染完成后自动订阅MQTT主题，实现实时数据推送。这是一个非常巧妙的设计——将表格渲染事件与实时数据订阅绑定在一起：

```javascript
function subTopics() {
    // 获取表格中所有变量名
    const variableNames = Array.from(
        document.querySelectorAll('td[data-field="Name"]')
    ).map(el => el.getAttribute('data-content'));

    // 订阅MQTT主题
    let topics = variableNames.map(
        v => "internal/v1/gateway/telemetry/+/" + v
    );
    client.subscribe(topics);
}
```

## 表单组件 - form

### 表单容器

```cshtml
<wt:form vm="@Model">
    <wt:row items-per-row="ItemsPerRowEnum.Two">
        <!-- 表单控件 -->
    </wt:row>
    <wt:row align="AlignEnum.Right">
        <wt:submitbutton />
        <wt:closebutton />
    </wt:row>
</wt:form>
```

### 自定义提交URL

```cshtml
<wt:form vm="@Model" Url="/BasicData/DeviceVariable/DoSetValue">
    <!-- 表单内容 -->
    <wt:submitbutton text="下发" />
</wt:form>
```

## 输入控件详解

### 文本框 - textbox

```cshtml
<!-- 基础文本框 -->
<wt:textbox field="Entity.DeviceName" />

<!-- 必填文本框 -->
<wt:textbox field="Entity.DeviceName" required="true" />

<!-- 带标签的文本框 -->
<wt:textbox field="Entity.Description" />
```

`field`属性使用Lambda表达式路径，WTM会自动：
1. 生成`name`和`id`属性
2. 填充当前值
3. 读取`[Display]`特性作为标签文本
4. 读取`[Required]`等验证特性

### 下拉框 - combobox

```cshtml
<!-- 绑定数据源的下拉框 -->
<wt:combobox field="Entity.DriverId" 
             items="AllDrivers" 
             required="true" />

<!-- 搜索条件中的下拉框，含"全部"选项 -->
<wt:combobox field="Searcher.DriverId" 
             items="Searcher.AllDrivers" 
             empty-text="@Localizer["Sys.All"]" />

<!-- 枚举下拉框（自动生成选项） -->
<wt:combobox field="Searcher.DataType" 
             empty-text="@Localizer["Sys.All"]" />
```

**属性说明：**

| 属性 | 说明 |
|------|------|
| `field` | 绑定的字段路径 |
| `items` | 选项数据源（`List<ComboSelectListItem>`） |
| `empty-text` | 空选项文本（如"全部"） |
| `required` | 是否必填 |

### 开关 - switch

```cshtml
<wt:switch field="Entity.AutoStart" />
<wt:switch field="Entity.CgUpload" />
```

开关控件用于布尔值字段的编辑，在IoTGateway中常用于设备的"自动启动"和"变更上传"配置。

### 日期时间 - datetime

```cshtml
<!-- 单个日期 -->
<wt:datetime field="Searcher.StartTime" />

<!-- 日期范围 -->
<wt:datetime field="Searcher.StartTime" range="true" />
```

RPC日志页面的搜索面板使用了日期范围选择器，用于筛选指定时间段内的调用记录。

### 只读显示 - display

```cshtml
<wt:display field="Entity.DeviceName" />
<wt:display field="Entity.Driver.DriverName" />
<wt:display field="Entity.DeviceTypeEnum" />
<wt:display field="Entity.Parent.DeviceName" />
```

`<wt:display>`用于详情页或编辑页中需要只读显示的字段。它支持导航属性（如`Entity.Driver.DriverName`），WTM会自动解析关联数据。

### 隐藏字段 - hidden

```cshtml
<wt:hidden field="Entity.ID" />
```

编辑表单中必须包含实体的主键字段作为隐藏字段，以便提交时识别更新目标。

## 条件渲染

在编辑页面中，经常需要根据数据状态显示不同的控件。IoTGateway的设备编辑页（`Device/Edit.cshtml`）演示了条件渲染：

```cshtml
<wt:form vm="@Model">
    <wt:row items-per-row="ItemsPerRowEnum.Two">
        <wt:textbox field="Entity.DeviceName" />
        <wt:textbox field="Entity.Index" />
        <wt:textbox field="Entity.Description" />
        <wt:display field="Entity.DeviceTypeEnum" />
        @{
            if (Model.Entity.DeviceTypeEnum == IoTGateway.Model.DeviceTypeEnum.Device)
            {
                <wt:combobox field="Entity.ParentId" items="AllParents" />
                <wt:switch field="Entity.AutoStart" />
                <wt:switch field="Entity.CgUpload" />
                <wt:textbox field="Entity.EnforcePeriod" />
                <wt:textbox field="Entity.CmdPeriod" />
            }
        }
    </wt:row>
    <wt:hidden field="Entity.ID" />
    <wt:row align="AlignEnum.Right">
        <wt:submitbutton />
        <wt:closebutton />
    </wt:row>
</wt:form>
```

这里通过`@{ if (...) { } }`代码块实现了：只有当设备类型为"Device"时才显示驱动相关配置项。分组设备（Group类型）不需要这些配置。

## 树形容器 - treecontainer

树形容器在左侧显示树状结构，右侧显示关联的数据表格。在设备变量管理中，左侧显示设备树，右侧显示选中设备的变量列表：

```cshtml
<wt:treecontainer items="AllDevices" 
                  id-field="Searcher.DeviceId" 
                  height="500">
    <wt:searchpanel vm="@Model" reset-btn="true">
        <wt:row items-per-row="ItemsPerRowEnum.Three">
            <wt:textbox field="Searcher.Name" />
            <wt:textbox field="Searcher.Method" />
            <wt:textbox field="Searcher.DeviceAddress" />
            <wt:combobox field="Searcher.DataType" 
                         empty-text="@Localizer["Sys.All"]" />
        </wt:row>
    </wt:searchpanel>
    <wt:grid vm="@Model" 
             url="/BasicData/DeviceVariable/Search" 
             hidden-grid-index="true" 
             done-func="subTopics" />
</wt:treecontainer>
```

**属性说明：**

| 属性 | 说明 |
|------|------|
| `items` | 树节点数据源 |
| `id-field` | 选中节点后绑定的筛选字段 |
| `height` | 容器高度（px） |

## 链接按钮 - linkbutton

```cshtml
<!-- 弹窗链接 -->
<wt:linkbutton url="/Login/ChangePassword" 
               window-width="400" 
               text="@Model.Localizer["Login.ChangePassword"]" 
               is-link="true" />

<!-- 新窗口链接 -->
<wt:linkbutton url="~/swagger" 
               target="ButtonTargetEnum.newwindow" 
               is-link="true" 
               text="@Model.Localizer["Sys.ApiDoc"]" />

<!-- 页面内跳转 -->
<wt:linkbutton url="/_Framework/SetLanguage?culture=@item.Value" 
               target="ButtonTargetEnum.self" 
               window-width="400" 
               text="@item.Text" 
               is-link="true" />
```

## ViewModel与TagHelper的协作

WTM TagHelper的强大之处在于与ViewModel的深度集成。以设备管理为例：

```
DeviceListVM (列表ViewModel)
├── Searcher (DeviceSearcher)   → 绑定到 <wt:searchpanel>
│   ├── DeviceName              → <wt:textbox>
│   ├── DriverId                → <wt:combobox>
│   ├── AllDrivers              → 下拉框选项数据源
│   └── AllParents              → 下拉框选项数据源
└── 列表数据                    → 绑定到 <wt:grid>

DeviceVM (编辑ViewModel)
├── Entity (Device)             → 绑定到 <wt:form>
│   ├── DeviceName              → <wt:textbox>
│   ├── DriverId                → <wt:combobox>
│   ├── AutoStart               → <wt:switch>
│   └── ...
├── AllDrivers                  → 驱动下拉选项
└── AllParents                  → 父设备下拉选项
```

TagHelper通过`field`属性的路径表达式（如`Entity.DeviceName`、`Searcher.DriverId`）自动完成：
1. HTML元素的`name`和`id`生成
2. 当前值填充
3. 标签文本读取（来自Model的`[Display]`特性）
4. 验证规则注入

## 国际化支持

IoTGateway通过`IStringLocalizer`实现多语言：

```cshtml
@inject IStringLocalizer<Program> Localizer;

<wt:combobox field="Searcher.AutoStart" 
             empty-text="@Localizer["Sys.All"]" />
```

语言配置在`appsettings.json`中指定：`"Languages": "zh,en"`。WTM框架会自动根据浏览器语言或用户设置切换显示语言。

## 小结

WTM TagHelper将传统的"HTML + JS + 数据绑定"三步骤简化为声明式的标签语法，开发者只需关注ViewModel的定义，视图层的渲染工作由TagHelper自动完成。这种模式极大地提高了CRUD管理页面的开发效率，非常适合IoTGateway这类以数据管理为核心的工业物联网系统。

---

上一篇: [[01-LayUI框架入门]] | 下一篇: [[03-ECharts数据可视化]]
