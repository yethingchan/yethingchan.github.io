---
title: 与 C# / .NET / VS 集成
---

# L5-1 与 C#/.NET/Visual Studio 集成

> 前置：[[halcon/16-深度学习Deep Learning]]
> 下接：[[halcon/18-性能优化与并行处理]]

## 1. 两种集成方式

| 方式 | 说明 | 适用 |
|------|------|------|
| **导出代码** | HDevelop `文件→导出` 成 C# | 简单、一次性 |
| **HALCON/.NET 库** | 直接引用 `halcondotnet.dll` 调算子 | 企业级、可维护 ★ |

## 2. HALCON/.NET 工程配置

1. VS 新建 WinForm/WPF 项目（.NET Framework 4.x 或 .NET 6+）。
2. 引用：`%HALCONROOT%/bin/dotnet35/halcondotnet.dll`（按框架选版本）。
3. 把 `halcon.dll`（native）所在目录加入 PATH 或拷贝到输出目录。
4. 代码：
```csharp
using HalconDotNet;

HOperatorSet.ReadImage(out HObject image, "C:/pic/board.png");
HOperatorSet.Threshold(image, out HObject region, 100, 255);
HOperatorSet.Connection(region, out HObject connected);
HOperatorSet.AreaCenter(connected, out HTuple area, out HTuple row, out HTuple col);
```

## 3. 关键 C# 映射规则

- HALCON 图标对象 → `HObject`；控制变量 → `HTuple`。
- **算子参数顺序与 HDevelop 完全一致**（输入在前、输出在后）。
- `HTuple` 可当数组：`area[0]`、`area.Length`、`area.TupleSum()`。
- 显示：用 `HWindowControl` 控件 `hWindowControl1.HalconWindow.DispObj(image)`。

## 4. 内存管理（企业级最重要！）

- 每个 `HObject` 都是非托管资源，**必须 Dispose**：
```csharp
using (HObject region = new HObject())
{
    HOperatorSet.Threshold(image, out region, 100, 255);
} // 自动 Dispose
```
- 循环里不 Dispose 会**显存/内存疯涨**直到崩溃。

## 5. 控件显示与 ROI 交互

- `HWindowControl` + `HMouseEventArgs` 实现鼠标画 ROI。
- 用 `HDevEngine` 可直接在 C# 里跑 `.hdev` 程序（保留 HDevelop 逻辑）。

## 📚 本阶段要看的书

- HALCON《Programming Guide C#/.NET》《HDevEngine》
- 《C# 图解教程》（基础语法）

## 🎯 达标水平

能把 `.hdev` 算法封装进 C# WinForm 程序，正确 Dispose、显示结果、捕获异常。

## 学习检查点

- [ ] 会引用 halcondotnet.dll 并调算子
- [ ] 理解 HObject/HTuple 映射
- [ ] 会用 using 正确 Dispose 防内存泄漏
- [ ] 会用 HWindowControl 显示图像
