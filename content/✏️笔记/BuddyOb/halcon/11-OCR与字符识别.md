---
title: OCR 与字符识别
---

# L4-1 OCR 与字符识别

> 前置：[[halcon/10-一维测量与二维计量]]
> 下接：[[halcon/12-条形码与二维码识别]]

## 1. OCR 两种方式

| 方式 | 说明 | 适用 |
|------|------|------|
| **基于字体（FFF/OML）** | 已知字体，训练/使用现成字体模型 | 工业固定字体（最稳）|
| **通用分类 OCR** | 用 MLP/SVM 分类单个字符 | 字体多变 |

## 2. 基于字体的 OCR（最常用）

```halcon
* 1) 读训练好的字体
read_ocr_class_mlp (OCRHandle, 'Industrial_0-9_NoRej.omc')
* 或 read_ocr_class_svm / read_ocr_class_cnn
* 2) 分割字符（先阈值+Blob+排序）
threshold (Image, Region, 0, 100)
connection (Region, Connected)
select_shape (Connected, Chars, 'height', 'and', 20, 60)
sort_region (Chars, SortedChars, 'character', 'true', 'row')
* 3) 识别
do_ocr_multi_class_mlp (SortedChars, Image, OCRHandle, Class, Confidence)
* Class 即识别出的字符元组
```

## 3. 训练自己的字体（FFF）

```halcon
* 1) 准备带真值的样本
* 2) 创建字体训练器
create_ocr_class_mlp (Width, Height, Interpolation, Features, ['0','1',...,'9'], \
                      NumHidden, 'none', 'normalization', 42, OCRHandle)
* 3) 训练
trainf_ocr_class_mlp (OCRHandle, 'train_samples.trf', 200, 1, 0.01, Error, ErrorLog)
* 4) 保存
write_ocr_class_mlp (OCRHandle, 'my_font.omc')
```

## 4. 提升 OCR 准确率

- 先**校正透视/倾斜**（`hom_vector_to_aniso`、`affine_trans_image`）。
- 字符**排序**正确（sort_region by 'character'）。
- 字体不全 → 自己训练 FFF。
- 噪声大 → 先滤波/阈值清理背景。

## 📚 本阶段要看的书

- HALCON《Solution Guide III-C OCR》
- 例程：`examples/hdevelop/ocr`

## 🎯 达标水平

能读现成字体完成序列号/铭牌识别；会自行训练一种工业字体。

## 学习检查点

- [ ] 会用 read_ocr_class_mlp + do_ocr_multi_class_mlp
- [ ] 会字符排序 sort_region
- [ ] 会训练并保存一个 .omc 字体
