## 相关链接

- [[01-LayUI框架入门]]
- [[02-Razor视图与TagHelpers]]
- [[04-实时数据推送]]
- [[05-管理后台设计]]
- [[架构总览]]
- [[上位机逻辑思维概述]]

## ECharts数据可视化

在工业物联网上位机中，数据可视化是核心需求之一。设备运行状态、变量趋势、告警分布等信息需要通过图表直观展示。IoTGateway使用ECharts作为图表引擎，通过WTM的`<wt:chart>`标签实现服务端与图表的无缝集成。本章详解ECharts在IoTGateway中的集成方式和典型应用。

## ECharts概述

ECharts是百度开源的JavaScript数据可视化图表库，具有以下优势：

- **丰富的图表类型** - 折线图、柱状图、饼图、散点图、地图、仪表盘等
- **交互式操作** - 缩放、图例开关、数据筛选、动态更新
- **响应式设计** - 自适应容器尺寸
- **性能优秀** - Canvas/SVG双引擎，支持大数据量渲染
- **中文文档** - 对国内开发者友好

## WTM Chart TagHelper

WTM框架将ECharts封装为`<wt:chart>`标签，开发者只需指定数据接口地址，无需手动编写JavaScript初始化代码。

### 基础用法

```cshtml
<!-- 饼图 -->
<wt:chart type="Pie" 
          height="200" 
          trigger-url="/Home/GetDeviceChart"
          is-horizontal="true"
          show-legend="true"
          show-tooltip="true" />

<!-- 柱状图 -->
<wt:chart type="Bar" 
          height="200" 
          trigger-url="/Home/GetDeviceVariableChart"
          show-legend="false"
          show-tooltip="true" />

<!-- 散点图 -->
<wt:chart type="Scatter" 
          height="400" 
          trigger-url="/Home/GetSampleChart"
          is-horizontal="true"
          radius="50"
          name-x="X" 
          name-y="Y" 
          name-category="C" 
          name-addition="A" />
```

### 属性说明

| 属性 | 类型 | 说明 |
|------|------|------|
| `type` | 枚举 | 图表类型：`Pie`、`Bar`、`Line`、`Scatter` |
| `height` | int | 图表高度（px） |
| `trigger-url` | string | 数据加载API地址 |
| `is-horizontal` | bool | 水平方向显示 |
| `show-legend` | bool | 是否显示图例 |
| `show-tooltip` | bool | 是否显示提示框 |
| `radius` | int | 散点图的点大小 |
| `name-x` | string | X轴名称 |
| `name-y` | string | Y轴名称 |
| `name-category` | string | 分类维度名称 |
| `name-addition` | string | 附加维度名称 |

## IoTGateway仪表盘图表

IoTGateway的首页`FrontPage.cshtml`包含了多个图表组件，构成完整的系统仪表盘。

### 设备状态饼图

展示系统中各状态设备的数量分布：

```cshtml
<div class="layui-col-md3">
    <div class="layui-card">
        <div class="layui-card-header">设备状态</div>
        <div class="layui-card-body">
            <wt:chart is-horizontal="true" 
                      show-legend="true" 
                      show-tooltip="true" 
                      type="Pie" 
                      height="200" 
                      trigger-url="/Home/GetDeviceChart" />
        </div>
    </div>
</div>
```

后端API返回的数据格式（WTM图表标准格式）：

```json
{
    "series": [
        {
            "name": "在线",
            "value": 15
        },
        {
            "name": "离线",
            "value": 3
        },
        {
            "name": "异常",
            "value": 1
        }
    ]
}
```

### 设备变量状态柱状图

展示各设备下变量的通信状态统计：

```cshtml
<div class="layui-col-md9">
    <div class="layui-card">
        <div class="layui-card-header">设备变量状态</div>
        <div class="layui-card-body">
            <wt:chart show-legend="false" 
                      show-tooltip="true" 
                      type="Bar" 
                      height="200" 
                      trigger-url="/Home/GetDeviceVariableChart" />
        </div>
    </div>
</div>
```

### 控制器访问统计

统计各Controller的访问频次，帮助运维人员了解系统热点：

```cshtml
<div class="layui-col-md6">
    <div class="layui-card">
        <div class="layui-card-header">控制器</div>
        <div class="layui-card-body">
            <wt:chart is-horizontal="true" 
                      show-legend="true" 
                      show-tooltip="true" 
                      type="Bar" 
                      height="300" 
                      trigger-url="/Home/GetActionChart" />
        </div>
    </div>
</div>
```

### 模型分布饼图

展示系统中各数据模型的使用情况：

```cshtml
<div class="layui-col-md6">
    <div class="layui-card">
        <div class="layui-card-header">模型</div>
        <div class="layui-card-body">
            <wt:chart show-legend="false" 
                      show-tooltip="true" 
                      type="Pie" 
                      height="300" 
                      trigger-url="/Home/GetModelChart" />
        </div>
    </div>
</div>
```

## 直接使用ECharts

对于`<wt:chart>`无法满足的需求，IoTGateway也直接使用ECharts API。例如首页的中国地图可视化：

```javascript
layui.use(['admin', 'carousel', 'echarts'], function () {
    var $ = layui.$,
        echarts = layui.echarts;

    var map;
    $.get('layui/china.json', function (chinaJson) {
        // 注册地图数据
        echarts.registerMap('china', chinaJson);

        var mapOption = {
            title: {
                text: '',
                subtext: ''
            },
            tooltip: {
                trigger: 'item'
            },
            dataRange: {
                orient: 'horizontal',
                min: 0,
                max: 10000,
                text: ['10000', '0'],
                splitNumber: 0
            },
            series: [{
                name: '访问量',
                type: 'map',
                mapType: 'china',
                selectedMode: 'multiple',
                itemStyle: {
                    normal: { label: { show: true } },
                    emphasis: { label: { show: true } }
                },
                data: [
                    { name: '江苏', value: 10000 }
                ]
            }]
        };

        map = echarts.init(document.getElementById('map'), layui.echartsTheme);
        map.setOption(mapOption);
    });
});
```

### ECharts主题集成

注意`echarts.init`的第二个参数使用了`layui.echartsTheme`，这使得图表配色与LayUI管理后台的主题保持一致。WTM框架内置了与LayUI配色方案匹配的ECharts主题文件。

## 图表数据API设计

WTM图表的数据接口需要返回标准格式。以下是后端Controller的典型实现模式：

```csharp
// HomeController.cs
public ActionResult GetDeviceChart()
{
    // 统计各状态设备数量
    var data = DC.Set<Device>()
        .GroupBy(d => d.AutoStart)
        .Select(g => new ChartData
        {
            Name = g.Key ? "自动启动" : "手动启动",
            Value = g.Count()
        }).ToList();

    return ChartData(data, "设备状态分布");
}

public ActionResult GetDeviceVariableChart()
{
    // 按设备分组统计变量数量
    var data = DC.Set<DeviceVariable>()
        .GroupBy(v => v.Device.DeviceName)
        .Select(g => new 
        {
            DeviceName = g.Key,
            Count = g.Count()
        }).ToList();

    var series = new List<ChartData>();
    // 转换为图表数据格式...
    return ChartData(series, "变量统计");
}
```

## 工业场景可视化建议

在工业物联网上位机中，除了IoTGateway已有的图表类型，以下可视化方式也值得考虑：

### 1. 实时趋势图

用于展示温度、压力、流量等变量的实时曲线：

```javascript
var trendChart = echarts.init(document.getElementById('trend'));
var option = {
    xAxis: {
        type: 'time',
        splitLine: { show: false }
    },
    yAxis: {
        type: 'value',
        name: '温度 (℃)'
    },
    series: [{
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.3 },
        data: [] // 通过MQTT实时更新
    }]
};
trendChart.setOption(option);
```

### 2. 仪表盘

用于展示设备负载、通信成功率等百分比指标：

```javascript
var gaugeOption = {
    series: [{
        type: 'gauge',
        detail: { formatter: '{value}%' },
        data: [{ value: 85.5, name: '通信成功率' }],
        axisLine: {
            lineStyle: {
                width: 15,
                color: [[0.3, '#fd666d'], [0.7, '#37a2da'], [1, '#67e0e3']]
            }
        }
    }]
};
```

### 3. 设备拓扑图

用关系图展示设备之间的连接关系：

```javascript
var topoOption = {
    series: [{
        type: 'graph',
        layout: 'force',
        data: [
            { name: '网关', symbolSize: 50, category: 0 },
            { name: 'PLC-01', symbolSize: 30, category: 1 },
            { name: 'PLC-02', symbolSize: 30, category: 1 }
        ],
        links: [
            { source: '网关', target: 'PLC-01' },
            { source: '网关', target: 'PLC-02' }
        ],
        force: { repulsion: 200 }
    }]
};
```

## 图表性能优化

在工业监控场景中，图表可能需要频繁更新数据。以下优化策略值得注意：

1. **数据量控制** - 实时趋势图保留最近N个点，旧数据定期移除
2. **节流更新** - 使用`setOption`时传入`notMerge: false`，增量更新而非全量替换
3. **Canvas渲染** - 数据量大时使用Canvas模式（默认），小数据量可用SVG
4. **窗口resize监听** - 图表容器大小变化时调用`chart.resize()`

```javascript
// 窗口大小变化时自适应
window.addEventListener('resize', function() {
    trendChart.resize();
    gaugeChart.resize();
});
```

## 小结

ECharts为IoTGateway提供了强大的数据可视化能力。通过WTM的`<wt:chart>`标签，开发者可以快速集成饼图、柱状图等标准图表；对于更复杂的需求（如地图、拓扑图），可以直接调用ECharts API。在工业物联网上位机中，合理利用数据可视化可以大幅提高运维效率和故障发现能力。

---

上一篇: [[02-Razor视图与TagHelpers]] | 下一篇: [[04-实时数据推送]]
