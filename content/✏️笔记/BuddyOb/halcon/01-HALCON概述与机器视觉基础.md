---
title: HALCON 概述与机器视觉基础
---

# L1-1 HALCON 概述与机器视觉基础

> 前置：[[halcon/00-总览与学习路线]]
> 下接：[[halcon/02-HDevelop环境与HALCON语法]]

## 1. HALCON 是什么

- MVTec 出品的**商业机器视觉算法库**，提供 2000+ 算子（operator），覆盖采集、预处理、分割、匹配、测量、OCR、3D、深度学习。
- 自带 **HDevelop** 交互式开发环境，可一键导出 C/C++/C#/.NET 代码。
- 支持 Windows / Linux，提供 **HALCON/.NET**（C#）与 **HALCON/C++** 接口，适合工业软件集成。

## 2. 一个机器视觉系统的基本链路

```
相机 → 图像采集卡/接口(GigE/USB3) → 预处理 → 分割/特征提取 → 测量/识别/决策 → 输出(PLC/IO)
        └─────────────── HALCON 负责中间算法部分 ───────────────┘
```

## 3. 三大核心数据对象（务必分清）

| 对象 | 含义 | 例子 |
|------|------|------|
| **Image** | 像素矩阵（灰度/彩色/RGB） | 相机拍的一张图 |
| **Region** | 像素集合（没有亚像素边界，整数坐标） | 阈值后得到的"亮区域" |
| **XLD**（eXtended Line Description） | 亚像素精度的轮廓/多边形 | 边缘提取出的轮廓线 |

> 关系：Image 经过分割得到 Region；Region/Image 提取边缘得到 XLD（更精确）。

## 4. 机器视觉常见任务分类

- **定位**：找物体位置/角度（形状匹配）
- **测量**：尺寸、距离、角度（1D/2D 计量）
- **识别**：OCR 字符、条码、二维码、分类
- **检测**：缺陷、有无、污点
- **3D**：点云、体积、平面度

## 📚 本阶段要看的书

- Steger《Machine Vision Algorithms and Applications》第 1~2 章（机器视觉系统构成）
- HALCON《Quick Guide》《Installation Guide》

## 🎯 达标水平

能口述"图像→Region→XLD"的区别，能说出 HALCON 在一个视觉系统里负责哪一段。

## 学习检查点

- [ ] 能解释 Image / Region / XLD 三者区别
- [ ] 能说出机器视觉系统的 4 个常见任务类型
- [ ] 知道 HALCON 可导出到哪些语言
