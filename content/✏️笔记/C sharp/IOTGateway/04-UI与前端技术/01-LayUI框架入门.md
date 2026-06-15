## 相关链接

- [[00-上位机UI技术选型]]
- [[02-Razor视图与TagHelpers]]
- [[05-管理后台设计]]
- [[06-响应式与多端适配]]
- [[架构总览]]

## LayUI框架入门

LayUI是一款由国人开发的轻量级CSS/JS前端框架，以"经典模块化"为设计理念，特别擅长构建后台管理系统。IoTGateway基于LayUI构建了完整的设备管理、用户管理和监控界面。本章介绍LayUI的核心概念和组件体系，帮助工控工程师快速理解前端页面结构。

## LayUI概述

### 框架特点

- **开箱即用** - 内置20+常用组件，无需额外引入第三方库
- **模块化加载** - 通过`layui.use()`按需加载模块，减少初始加载时间
- **响应式栅格** - 12列栅格系统，支持多种屏幕尺寸
- **丰富的主题** - 支持主题切换，内置多种配色方案
- **中文优先** - 文档和社区均为中文，工控工程师学习成本低

### 引入方式

IoTGateway中LayUI的引入方式（见`Login.cshtml`）：

```html
<link rel="stylesheet" href="/layui/css/layui.css">
<script src="/layui/layui.js"></script>
```

LayUI的静态资源文件放置在`wwwroot/layui/`目录下，包含CSS样式、字体图标和各模块JS文件。

## 栅格系统

LayUI采用12列栅格布局，通过CSS类名控制元素的宽度和排列方式。在IoTGateway的首页`FrontPage.cshtml`中可以看到大量栅格用法：

```html
<div class="layui-row layui-col-space15">
    <div class="layui-col-md3">
        <div class="layui-card">
            <div class="layui-card-header">设备状态</div>
            <div class="layui-card-body">
                <!-- 设备状态饼图 -->
            </div>
        </div>
    </div>
    <div class="layui-col-md9">
        <div class="layui-card">
            <div class="layui-card-header">设备变量状态</div>
            <div class="layui-card-body">
                <!-- 变量状态柱状图 -->
            </div>
        </div>
    </div>
</div>
```

**栅格规则：**

| 类名 | 说明 |
|------|------|
| `layui-row` | 行容器，内部放置列元素 |
| `layui-col-md3` | 中等屏幕占3/12（25%宽度） |
| `layui-col-md6` | 中等屏幕占6/12（50%宽度） |
| `layui-col-md9` | 中等屏幕占9/12（75%宽度） |
| `layui-col-md12` | 中等屏幕占满宽度 |
| `layui-col-space15` | 列间距15px |
| `layui-col-xs*` | 超小屏幕（手机）的列宽 |

**前缀说明：**

| 前缀 | 屏幕宽度 | 典型设备 |
|------|---------|---------|
| `xs` | < 768px | 手机 |
| `sm` | >= 768px | 平板 |
| `md` | >= 992px | 小尺寸笔记本 |
| `lg` | >= 1200px | 桌面显示器 |

## 卡片组件

LayUI的卡片（Card）组件是IoTGateway中使用最频繁的布局容器：

```html
<div class="layui-card">
    <div class="layui-card-header">卡片标题</div>
    <div class="layui-card-body">
        卡片内容区域
    </div>
</div>
```

在IoTGateway中，几乎所有页面内容都包裹在卡片中，包括：
- 仪表盘中的图表卡片
- 设备列表页
- 表单编辑弹窗
- 搜索结果展示区

## 表单组件

### 基础表单

LayUI提供了一套完整的表单元素样式，在IoTGateway的设备创建页面中（`Device/Create.cshtml`），使用了多种表单组件：

```html
<!-- 文本输入框 -->
<div class="layui-form-item">
    <label class="layui-form-label">设备名称</label>
    <div class="layui-input-block">
        <input type="text" name="DeviceName" class="layui-input" 
               placeholder="请输入设备名称">
    </div>
</div>

<!-- 下拉选择框 -->
<div class="layui-form-item">
    <label class="layui-form-label">驱动</label>
    <div class="layui-input-block">
        <select name="DriverId" lay-filter="driver">
            <option value="">请选择驱动</option>
            <option value="1">Modbus RTU</option>
            <option value="2">OPC UA</option>
        </select>
    </div>
</div>

<!-- 开关 -->
<div class="layui-form-item">
    <label class="layui-form-label">自动启动</label>
    <div class="layui-input-block">
        <input type="checkbox" name="AutoStart" lay-skin="switch" 
               lay-text="开|关">
    </div>
</div>
```

### 表单验证

LayUI内置了表单验证规则：

```html
<input type="text" name="DeviceName" lay-verify="required" 
       placeholder="必填项" class="layui-input">

<input type="text" name="Email" lay-verify="email" 
       placeholder="邮箱" class="layui-input">

<input type="text" name="Port" lay-verify="number" 
       placeholder="数字" class="layui-input">
```

### 表单事件

```javascript
layui.use('form', function(){
    var form = layui.form;
    
    // 监听提交
    form.on('submit(submit)', function(data){
        console.log(data.field); // 表单数据对象
        return false; // 阻止表单跳转
    });
    
    // 监听下拉框选择
    form.on('select(driver)', function(data){
        console.log(data.value); // 选中的值
    });
    
    // 监听开关切换
    form.on('switch(autoStart)', function(data){
        console.log(data.elem.checked); // 开关状态
    });
});
```

## 表格组件

### 基础表格

LayUI的数据表格是IoTGateway列表页面的核心组件。虽然在IoTGateway中主要使用WTM封装的`<wt:grid>`标签，但底层仍然是LayUI的table模块：

```javascript
layui.use('table', function(){
    var table = layui.table;
    
    table.render({
        elem: '#deviceTable',
        url: '/BasicData/Device/Search',
        cols: [[
            {type: 'checkbox'},
            {field: 'DeviceName', title: '设备名称', sort: true},
            {field: 'DriverName', title: '驱动'},
            {field: 'AutoStart', title: '自动启动', templet: '#switchTpl'},
            {field: 'Description', title: '描述'},
            {fixed: 'right', title: '操作', toolbar: '#barTpl'}
        ]],
        page: true,
        limit: 20
    });
});
```

### 表格配置项

| 配置项 | 说明 | IoTGateway默认值 |
|--------|------|-----------------|
| `page` | 启用分页 | true |
| `limit` | 每页记录数 | 20（appsettings.json中RPP配置） |
| `limits` | 可选每页数 | [10,20,30,50,100] |
| `toolbar` | 头部工具栏 | 含新增、删除、导入、导出按钮 |
| `multi` | 多选模式 | 部分页面启用 |

### 工具栏操作

表格行操作通过模板定义：

```html
<script type="text/html" id="barTpl">
    <a class="layui-btn layui-btn-xs" lay-event="edit">编辑</a>
    <a class="layui-btn layui-btn-danger layui-btn-xs" lay-event="del">删除</a>
</script>

<script>
table.on('tool(deviceTable)', function(obj){
    switch(obj.event){
        case 'edit':
            // 打开编辑弹窗
            layer.open({
                type: 2,
                title: '编辑设备',
                content: '/BasicData/Device/Edit?id=' + obj.data.ID
            });
            break;
        case 'del':
            layer.confirm('确认删除？', function(index){
                obj.del();
                layer.close(index);
            });
            break;
    }
});
</script>
```

## 弹窗组件（Layer）

Layer是LayUI的弹层组件，在IoTGateway中用于：
- 打开新增/编辑表单
- 显示确认对话框
- 展示提示信息
- 加载遮罩

```javascript
layui.use('layer', function(){
    var layer = layui.layer;
    
    // 页面层（iframe模式，IoTGateway常用）
    layer.open({
        type: 2,                    // iframe层
        title: '新增设备',
        area: ['800px', '600px'],   // 宽高
        content: '/BasicData/Device/Create',
        end: function(){
            // 关闭后刷新表格
            table.reload('deviceTable');
        }
    });
    
    // 确认框
    layer.confirm('确定要删除该设备吗？', {
        btn: ['确定', '取消']
    }, function(index){
        // 执行删除
        layer.close(index);
    });
    
    // 提示消息
    layer.msg('操作成功', {icon: 1, time: 2000});
    
    // 加载层
    var loadIndex = layer.load(1);
    // ... 异步操作完成后
    layer.close(loadIndex);
});
```

## 模块化加载

LayUI的核心设计是模块化，通过`layui.use()`按需加载：

```javascript
layui.use(['table', 'form', 'layer'], function(){
    var table = layui.table;
    var form = layui.form;
    var layer = layui.layer;
    
    // 所有模块就绪后执行
    table.render({ /* ... */ });
});
```

IoTGateway中常用的模块：

| 模块名 | 功能 | 使用场景 |
|--------|------|---------|
| `table` | 数据表格 | 设备列表、变量列表、日志列表 |
| `form` | 表单组件 | 设备编辑、参数配置 |
| `layer` | 弹层 | 弹窗表单、确认框、提示 |
| `element` | 常用元素 | Tab切换、导航、进度条 |
| `carousel` | 轮播 | 首页快捷方式轮播 |
| `echarts` | 图表 | 设备状态图、数据统计 |
| `admin` | 后台管理 | 侧边栏、主题、标签页 |
| `tree` | 树组件 | 设备树形结构 |

## 图标字体

LayUI内置了一套图标字体，在IoTGateway的菜单和按钮中广泛使用：

```html
<i class="layui-icon layui-icon-username"></i>   <!-- 用户 -->
<i class="layui-icon layui-icon-tabs"></i>        <!-- 标签 -->
<i class="layui-icon layui-icon-app"></i>         <!-- 应用 -->
<i class="layui-icon layui-icon-auz"></i>         <!-- 权限 -->
<i class="layui-icon layui-icon-console"></i>     <!-- 控制台 -->
<i class="layui-icon layui-icon-website"></i>     <!-- 网站 -->
<i class="layui-icon layui-icon-refresh-3"></i>   <!-- 刷新 -->
<i class="layui-icon layui-icon-theme"></i>       <!-- 主题 -->
<i class="layui-icon layui-icon-screen-full"></i> <!-- 全屏 -->
```

## IoTGateway中的LayUI配置

在`appsettings.json`中，IoTGateway对WTM的UI行为做了全局配置：

```json
{
  "UIOptions": {
    "DataTable": {
      "RPP": 20,           // 每页默认20条记录
      "ShowPrint": true,    // 显示打印按钮
      "ShowFilter": true    // 显示筛选按钮
    },
    "ComboBox": {
      "DefaultEnableSearch": true  // 下拉框默认启用搜索
    },
    "DateTime": {
      "DefaultReadonly": true      // 日期控件只读
    },
    "SearchPanel": {
      "DefaultExpand": false       // 搜索面板默认折叠
    }
  },
  "PageMode": "Tab",      // 页面模式：Tab标签页模式
  "TabMode": "Simple"     // 标签模式：简洁模式
}
```

这些配置影响所有使用WTM TagHelper生成的页面，无需在每个视图中单独设置。

## 小结

LayUI以其轻量、模块化和中文友好的特性，成为IoTGateway前端的技术基石。理解LayUI的栅格系统、表单组件、表格组件和弹窗机制，是深入学习IoTGateway UI开发的前提。下一章将介绍WalkingTec.Mvvm如何在LayUI之上封装TagHelper，进一步简化服务端渲染的开发工作。

---

上一篇: [[00-上位机UI技术选型]] | 下一篇: [[02-Razor视图与TagHelpers]]
