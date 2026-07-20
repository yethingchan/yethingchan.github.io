---
title: 光度立体法 Photometric Stereo
---

# 01-1 光度立体法（Photometric Stereo）

> 上接：[[halcon/进阶专题/01-缺陷检测专题/00-索引]]

## 原理一句话
同一相机、**至少 3 个不同方向的点/面光源**轮流打光，拍多张图。因为反射随表面法线方向变化，**反解出每个像素的表面法线（梯度场）和反射率（albedo）**。

- 划痕、凹坑会改变局部法线 → 在法线图里**极其明显**，而原图里几乎看不见。
- 反光金属件用普通打光是"灾难"，用光度立体反而**最合适**——因为它关心的是几何起伏，不是亮度绝对値。

## 核心算子链

```halcon
* 1) 三张同视角、不同光源方向的图（张数 = 光源数，>=3）
read_image (Images, ['light_0','light_1','light_2'])

* 2) 务必做反射率归一（避免光源亮度不一）
*    Tilts/Slants 是每个光源相对相机的方位角(tilt)与天顶角(slant)
PhotometricStereo (Images, HeightField, Gradient, Albedo, \
                    [0, 0, 0], [-45, 0, 45], 'gb', 'least_squares', \
                    [], [])

* 3) 法线图 Gradient 是 (n_x, n_y, n_z) 三通道向量场
*    把它转成可看的颜色编码（法线方向→RGB）
*    normal 颜色图：朝不同方向的表面呈现不同颜色
decompose3 (Gradient, Nx, Ny, Nz)

* 4) 用梯度/法线做缺陷：例如法线梯度突变 = 划痕
*    HeightField 是积分得到的相对高度场，可直接当"表面形貌"用
```

**逐行解释**：
- `PhotometricStereo` 第三个参数 `HeightField` 是**积分重建的相对高度**（通过法线积分得到），第四个 `Gradient` 是**每像素法线 (nx,ny,nz)**，第五 `Albedo` 是**反照率（材质固有亮度，与光照无关）**。
- `Tilts = [0,0,0]`、`Slants = [-45,0,45]` 表示三盏灯都在水平面、分别偏上/正对/偏下 45°。真实项目里**灯位要实测标定**，不能拍脑袋写角度。
- `'gb'` 是方法（Gray-Balance 变种）；`'least_squares'` 是求解方式；后两个空 `[]` 是可选参数（掩膜、约束）。

## 关键坑位

1. **灯数 ≥ 3 但 4~6 盏更稳**：噪声大时增加灯数、换更鲁棒的 `Method`（如 `'adaptive_smoothing'`）。
2. **相机与物体必须严格不动**，只有灯变——用机械快门或电子触发同步，否则重建全是鬼影。
3. **阴影/自遮挡区域法线解算失败** → 用 `HeightField` 的置信度，或后期用形态学剔掉这些像素。
4. 反光件别用普通镜面假设；HALCON 提供 `'reflectance_finite'`（Ward 模型）处理高光。

## 进阶：无标定光度立体
连灯位角度都不知道时，用 `photometric_stereo` 的扩展 + `binomial_filter` 平滑，或走 **`reconstruct_surface_stereo`（多视角）**。更省事的是直接上**深度学习异常检测**（见 [[halcon/进阶专题/01-缺陷检测专题/04-异常检测DeepAnomaly]]）。

## 📚 书
Steger 附录 B（光照模型与法线求解推导）；MVTec 例程 `photometric_stereo.hdev`。

## 🎯 检查点
- [ ] 能解释为何反光件反而适合光度立体
- [ ] 写得出 3 光源的 PhotometricStereo 调用并说明灯位参数来源
- [ ] 知道法线图 vs 高度场 各自怎么用于检缺陷
