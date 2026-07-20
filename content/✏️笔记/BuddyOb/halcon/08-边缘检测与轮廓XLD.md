---
title: 边缘检测与轮廓 XLD
---

# L3-1 边缘检测与轮廓 XLD

> 前置：[[halcon/07-形态学运算]]
> 下接：[[halcon/09-形状匹配与模板匹配]]

## 1. 边缘检测（得到 Region 边缘或 XLD）

```halcon
* 经典 Sobel 边缘（得到 Region 边界）
sobel_amp (Image, EdgeAmplitude, 'sum_abs', 3)
threshold (EdgeAmplitude, Edges, 30, 255)

* 亚像素边缘提取（得到 XLD 轮廓）★重点
edges_sub_pix (Image, EdgesXLD, 'canny', 1.0, 20, 40)
* 其他方法：'lanser2' 'deriche1' 'shen' 'mshen'，canny 最常用
```

## 2. edges_sub_pix 参数含义

- 第 3 参：算子类型（canny 鲁棒）。
- 第 4 参：**Alpha（平滑系数）** 越大越平滑、边缘越少。
- 第 5/6 参：**Low/High 阈值**，低阈值决定弱边保留，高阈值决定强边。

## 3. 轮廓（XLD）的处理

```halcon
* 按长度筛选轮廓
select_contours_xld (EdgesXLD, ContoursSel, 'contour_length', 20, 99999, -0.5, 0.5)
* 合并相邻
union_adjacent_contours_xld (ContoursSel, UnionContours, 10, 1, 'attr_keep')
* 拟合直线
fit_line_contour_xld (UnionContours, 'tukey', -1, 0, 5, 2, RowBegin, ColBegin, RowEnd, ColEnd, Nr, Nc, Dist)
* 拟合圆
fit_circle_contour_xld (UnionContours, 'algebraic', -1, 0, 0, 3, 2, Row, Column, Radius, StartPhi, EndPhi, PointOrder)
* 拟合矩形 / 椭圆
fit_rectangle2_contour_xld (...)
fit_ellipse_contour_xld (...)
```

## 4. 轮廓特征与排序

```halcon
* 轮廓长度
length_xld (Contours, Length)
* 轮廓面积（封闭）
area_center_xld (Contours, Area, Row, Column, PointOrder)
* 按位置排序（从左到右）
sort_contours_xld (Contours, SortedContours, 'upper_left', 'true', 'row')
```

## 5. 闭合轮廓转 Region

```halcon
* XLD -> Region（需要填充时）
gen_region_contour_xld (Contours, Region, 'filled')
```

## 📚 本阶段要看的书

- Steger 书第 4 章（边缘与亚像素定位，Steger 本人是亚像素边缘算法作者）
- HALCON《1D/2D Measuring》文档

## 🎯 达标水平

能提取亚像素边缘，筛选并拟合出直线/圆/矩形，理解 canny 三参数作用。

## 学习检查点

- [ ] 会用 edges_sub_pix 并解释 canny 参数
- [ ] 会 fit_line/circle/rectangle2_contour_xld
- [ ] 理解 Region 边界 vs XLD 亚像素轮廓的区别
