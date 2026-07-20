---
title: 阈值分割与 Blob 分析
---

# L2-3 阈值分割与 Blob 分析

> 前置：[[halcon/05-图像预处理与滤波]]
> 下接：[[halcon/07-形态学运算]]

## 1. 阈值分割（像素级）

```halcon
* 全局固定阈值
threshold (Image, Region, 100, 255)
* 自动阈值（基于直方图波谷）
auto_threshold (Image, Regions, 8)
* 双峰法
binary_threshold (Image, Region, 'max_separability', 'dark', UsedThreshold)
* 局部自适应（光照不均利器）
var_threshold (Image, Region, 15, 15, 0.2, 2, 'dark')
* 局部均值法
dyn_threshold (ImageSmooth, Image, RegionDyn, 10, 'not_equal')
```

## 2. watershed 分水岭（黏连目标分离）

```halcon
* 距离变换 + 分水岭 分离重叠颗粒
distance_transform (Region, Dist, 'distance', 0)
watersheds_threshold (Dist, Basins, 10)
```

## 3. Blob 分析（连通域 + 特征筛选）

```halcon
* 连通域
connection (Region, ConnectedRegions)
* 按面积筛选
select_shape (ConnectedRegions, SelectedRegions, 'area', 'and', 500, 99999)
* 取最大区域
select_shape_std (ConnectedRegions, MaxRegion, 'max', 'area')
* 求特征
area_center (SelectedRegions, Area, Row, Column)
```

## 4. 常用 select_shape 特征

`area` 面积、`circularity` 圆度、`rectangularity` 矩形度、`compactness` 紧密度、`anisometry` 长宽比、`roundness`、`convexity`、`phi` 方向角、`row`/`column`。

## 5. 完整 Blob 流程套路

```
阈值 → connection → select_shape 筛选 → 求特征(area_center/orientation) → 输出
```

## 📚 本阶段要看的书

- Steger 书第 4 章（分割）第 5 章（特征）
- HALCON《Solution Guide I-Basics》Blob 例程

## 🎯 达标水平

能独立完成"阈值→连通域→按面积/形状筛目标→计数/定位"的最经典视觉任务。

## 学习检查点

- [ ] 会 threshold / binary_threshold / dyn_threshold 及适用场景
- [ ] 会 connection + select_shape 筛选
- [ ] 能写出标准 Blob 流程
