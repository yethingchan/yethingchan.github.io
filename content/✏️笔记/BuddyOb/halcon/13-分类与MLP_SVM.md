---
title: 分类与 MLP / SVM
---

# L4-3 分类与 MLP / SVM

> 前置：[[halcon/12-条形码与二维码识别]]
> 下接：[[halcon/14-相机标定与几何测量]]

## 1. 分类能做什么

把图像/区域判定为某一类：良品/不良品、水果分级、缺陷类型。HALCON 提供 **MLP（多层感知机）**、**SVM（支持向量机）**、**GMM（高斯混合）** 分类器。

## 2. MLP 分类流程

```halcon
* 1) 创建
create_class_mlp (NumInput, NumHidden, NumOutput, 'softmax', 'normalization', \
                  42, MLPHandle)
* 特征维数 NumInput（如区域特征 8 维）
* 2) 加样本（特征向量, 类别标签）
add_sample_class_mlp (MLPHandle, FeatureVector, ClassLabel)
* 3) 训练
train_class_mlp (MLPHandle, 200, 1, 0.01, Error, ErrorLog)
* 4) 分类
classify_class_mlp (MLPHandle, FeatureVector, Class, Confidence)
* 5) 保存
write_class_mlp (MLPHandle, 'model.mlp')
```

## 3. SVM 分类流程

```halcon
create_class_svm (NumFeatures, 'rbf', 0.02, 0.001, NumClasses, 'one-versus-all', 'normalization', 42, SVMHandle)
add_sample_class_svm (SVMHandle, FeatureVector, ClassLabel)
train_class_svm (SVMHandle, 0.001, 'default')
classify_class_svm (SVMHandle, FeatureVector, 1, Class)
```

## 4. 特征怎么来

- 用 `region_features` / `select_features`（如面积、圆度、纹理 `gen_cooc_matrix`）。
- 也可对整图取特征（颜色直方图、Hu 矩）。

## 5. MLP vs SVM 取舍

| | 优点 | 适合 |
|--|------|------|
| MLP | 多类好、概率输出 | 多分类、特征多 |
| SVM | 小样本稳、核函数强 | 小样本、边界清晰 |

## 📚 本阶段要看的书

- HALCON《Solution Guide III Classification》
- 机器学习基础（周志华《机器学习》对应章节）

## 🎯 达标水平

能为"良/不良品"任务提取特征并训练 MLP/SVM 分类器，理解特征选择的重要性。

## 学习检查点

- [ ] 会 create/train/classify MLP 与 SVM
- [ ] 懂得从 region 提取分类特征
- [ ] 知道 MLP 与 SVM 的取舍
