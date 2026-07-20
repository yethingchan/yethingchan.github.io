---
title: 深度学习异常检测（无缺陷样本训练）
---

# 01-4 深度学习异常检测（Anomaly Detection）

> 上接：[[halcon/进阶专题/01-缺陷检测专题/00-索引]]、[[halcon/16-深度学习Deep Learning]]

## 为什么是"终极武器"
工业里**坏样本极稀缺**——一个月可能就几个 NG，根本不够训练分类/分割网络。异常检测（AD）的牛在于：**只用 OK 样本训练**，推理时给出每个像素的"异常分数图"，分数高=缺陷。

HALCON 提供完整 AD 工作流（基于 **PatchCore / 师生网络** 思想，MVTec AD 数据集 SOTA 级别）：

```halcon
* 1) 建模型（指定图像尺寸，通常下采样到 256 左右）
create_anomaly_model ('plain', AnomalyModel)
set_anomaly_model_param (AnomalyModel, 'image_dimension', [256, 256])

* 2) 用大量 OK 图训练（无需任何标注！）
*    ImagesOK 是一个仅含正常样本的目录/元组
train_anomaly_model (ImagesOK, AnomalyModel, \
                     'auto', 10, 0.001, true, TrainResults, TrainHyper)

* 3) 推理：返回异常分数图 + 分类标签
apply_anomaly_model (TestImages, PixelScores, AnomalyModel, 'true')
*    PixelScores：每个像素的异常概率（0~1），可阈值化得缺陷掩膜
threshold (PixelScores, DefectMask, 0.5, 1.0)
```

**逐行解释**：
- `create_anomaly_model('plain',..)`：`'plain'` 是标准 AD 网络（也支持 `'synthetic'` 用生成模型增广正常样本，进一步抗过拟合）。
- `train_anomaly_model(.., 'auto', 10, 0.001, ..)`：`'auto'` 自动划分验证集；`10` 是 epoch；`0.001` 学习率；`true` 表示继续训练（可增量）。
- `apply_anomaly_model` 输出**逐像素分数图**——这就是它强于分类网络的地方：既能说"有缺陷"，还能**定位到哪**。

## 关键技巧
1. **样本组织**：OK 图放一个文件夹，用 `list_files`+`tuple_filter` 或直接传 image tuple。**绝不混入 NG** 进训练集。
2. **数据增强**：HALCON DL 工具内置，但 AD 对增强更敏感——旋转/亮度抖动要保守，否则把"正常变化"当异常学进去。
3. **阈值校准**：分数阈值不是 0.5 一成不变，用验证集的 ROC/PR 曲线定（DL 工具里 `evaluate_anomaly_model` 给出）。
4. **小样本救星**：若连 OK 都少，用 `'synthetic'` 模式做自监督增广。
5. **推理速度**：AD 比分类重，但比分割轻；产线要换 `set_dl_model_param(.., 'batch_size',..)` 与 GPU（见 [[halcon/进阶专题/06-性能与工程化专题/02-GPU与算子加速]]）。

## 与其他路线对比
| 方法 | 需 NG 样本 | 能定位 | 速度 | 硬件 |
|------|-----------|--------|------|------|
| 光度立体 | 否 | 是(几何) | 快 | 多光源 |
| 频域带通 | 否 | 是 | 快 | 单相机 |
| 纹理统计 | 否 | 部分 | 中 | 单相机 |
| **AD 深度学习** | **否** | **是(像素级)** | 中 | **GPU 推荐** |

## 📚 书
MVTec AD 数据集论文 (Bergmann et al. 2019)；Goodfellow《Deep Learning》第 12 章（无监督/自监督）。

## 🎯 检查点
- [ ] 能说清 AD 与"分类/分割"在样本需求上的本质区别
- [ ] 写得出仅用 OK 图训练并输出像素分数图的流程
- [ ] 知道分数阈值怎么用验证集校准
