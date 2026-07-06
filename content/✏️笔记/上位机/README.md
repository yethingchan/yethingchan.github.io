# C#上位机开发学习体系

## 项目说明

本仓库是C#上位机开发的学习与实战项目，涵盖从基础语法到工业通信、界面设计、数据管理等全方位内容。

## 目录结构

```
C#shangweiji/
│
├── README.md                          ← 你正在看的文件（总览）
│
├── 00-学习规划/
│   ├── 学习计划与优化计划.md             ← 项目整体学习路线
│   └── 上位机开发技能树.md              ← 所需技能全景图
│
├── 01-C#语言基础/
│   ├── 01-C#基础语法.md                ← 变量、数据类型、运算符
│   ├── 02-流程控制.md                  ← 条件判断、循环
│   ├── 03-面向对象编程.md              ← 类、继承、多态、接口
│   ├── 04-集合与泛型.md                ← List、Dictionary、LINQ
│   ├── 05-委托与事件.md                ← delegate、event、Lambda
│   ├── 06-异常处理与调试.md            ← try-catch、日志、断点调试
│   └── 07-异步编程.md                  ← async/await、Task、并行
│
├── 02-WinForms界面开发/
│   ├── 01-WinForms基础控件.md          ← Button、TextBox、Label等
│   ├── 02-布局与容器控件.md            ← TableLayoutPanel、FlowLayoutPanel
│   ├── 03-数据展示控件.md              ← DataGridView、ListView、TreeView
│   ├── 04-自定义控件开发.md             ← UserControl、GDI+绘图
│   ├── 05-界面美化与主题.md            ← 现代化UI、圆角控件、动画效果
│   └── 06-多线程与界面更新.md          ← Invoke、BackgroundWorker
│
├── 03-工业通信协议/
│   ├── 01-串口通信.md                  ← SerialPort、RS232/485
│   ├── 02-Modbus协议详解.md            ← RTU/TCP、功能码、帧格式
│   ├── 03-汇川PLC寄存器读写.md         ← 汇川地址映射、读写方法
│   ├── 04-NModbus库实战.md              ← NModbus4/5使用方法
│   ├── 05-HslCommunication库实战.md    ← HslCommunication使用方法
│   ├── 06-TCP-UDP网络编程.md          ← Socket、TcpClient、UdpClient
│   └── 07-OPC-UA协议基础.md           ← OPC UA简介与C#实现
│
├── 04-数据处理与存储/
│   ├── 01-SQLite数据库.md              ← SQLite基础、Dapper ORM
│   ├── 02-CSV-Excel数据导入导出.md     ← CSV读写、NPOI/EPPlus
│   ├── 03-JSON与XML处理.md             ← System.Text.Json、序列化
│   └── 04-实时数据采集与缓存.md        ← 环形缓冲区、数据队列
│
├── 05-项目实战/
│   ├── 01-项目架构设计.md              ← 分层架构、MVVM模式
│   ├── 02-配置管理.md                  ← app.config、JSON配置文件
│   ├── 03-日志系统.md                  ← 日志框架选型与使用
│   ├── 04-数据备份与恢复.md            ← 自动备份策略
│   └── 05-上位机项目完整示例.md        ← 综合实战案例
│
└── 06-进阶提升/
    ├── 01-设计模式在上位机中的应用.md
    ├── 02-性能优化.md
    ├── 03-自动化测试.md
    └── 04-部署与打包.md
```

## 学习建议

1. **按顺序学习**：从 `01-C#语言基础` 开始，逐步进阶
2. **边学边练**：每个教程都有代码示例，建议动手运行
3. **结合项目**：学完基础知识后进入 `05-项目实战`
4. **随时查阅**：所有文档均可作为参考手册使用

## 使用的PLC型号

- 汇川(Inovance) H3U/H5U 系列
- 通信方式：Modbus TCP / Modbus RTU
