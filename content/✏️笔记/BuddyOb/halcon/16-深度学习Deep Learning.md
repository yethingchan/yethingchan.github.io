---
title: 深度学习 Deep Learning
---

# L4-6 深度学习（HALCON DL）

> 前置：[[halcon/15-三维视觉基础]]
> 下接：[[halcon/17-与C#_NET_VS集成]]

## 1. HALCON 支持的 DL 任务

| 任务 | 算子/模型 | 工业用途 |
|------|-----------|----------|
| **分类 Classification** | `classify_object` | 良/不良/类型 |
| **目标检测 Detection** | `get_dl_model_param` + 检测 | 缺陷定位框 |
| **语义分割 Segmentation** | `segment_object` | 像素级缺陷 |
| **异常检测 Anomaly** | **无监督**，少量良品即可 | 未知缺陷发现 ★ |
| **OCR（DL）** | `model_type='dl'` | 复杂字体字符 |

## 2. 通用训练流程（以分类为例）

```halcon
* 1) 读预训练模型
read_dl_model ('pretrained_dl_classifier_compact.hdl', DLModelHandle)
* 2) 设置类别与图像尺寸
set_dl_model_param (DLModelHandle, 'class_names', ['good','bad'])
set_dl_model_param (DLModelHandle, 'image_width', 224)
* 3) 准备数据集（需用 MVTec Deep Learning Tool 标注）
* 4) 训练
train_dl_model (DLModelHandle, DLDataset, DLPreprocessParam, TrainParam, _)
* 5) 评估 + 推理
apply_dl_model (DLModelHandle, DLSampleBatch, [], DLResultBatch)
get_dl_model_param (DLModelHandle, 'class_names', ClassNames)
```

## 3. 数据准备工具

- **MVTec Deep Learning Tool**（独立 GUI）标注、划分训练/验证集，导出 `.hdict` 数据集。
- 数据量建议：每类 **数百~数千**张起步，异常检测可只标良品。

## 4. 工程要点

- **数据增强**：旋转、亮度、镜像（HALCON 预处理自动做）。
- **小样本优先异常检测**，避免大量标注。
- **推理前必须预处理**（`preprocess_dl_dataset`）与训练一致。
- GPU 加速：安装对应 CUDA 版 HALCON。

## 📚 本阶段要看的书

- MVTec Deep Learning Tool 官方文档（**必读**）
- 吴恩达《Deep Learning》专项（理解原理）

## 🎯 达标水平

能用 DL Tool 标注数据、训练一个缺陷分类/异常检测模型并推理。

## 学习检查点

- [ ] 知道 HALCON 五种 DL 任务
- [ ] 会用 Deep Learning Tool 标注导出
- [ ] 会 train/apply_dl_model 全流程
- [ ] 理解异常检测的优势
