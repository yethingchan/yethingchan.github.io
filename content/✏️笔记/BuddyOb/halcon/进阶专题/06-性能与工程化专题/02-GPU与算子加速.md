---
title: GPU 与算子加速
---

# 06-2 GPU 与算子加速

> 上接：[[halcon/进阶专题/06-性能与工程化专题/00-索引]]

## 哪些算子能上 GPU
HALCON 的 GPU 加速是**按算子开放**的，不是整图自动。主要受益：
- **深度学习**：`apply_dl_model` / `train_dl_model` 几乎必上 GPU，提速数倍到数十倍。
- **FFT / 频域滤波**（[[halcon/进阶专题/01-缺陷检测专题/02-频域带通滤波]]）。
- **形态学、二值/灰度常用算子**（部分）。
- **三维**：点云平滑、表面匹配部分支持。

## 启用方式
```halcon
* 1) 检查 GPU 可用性
query_available_compute_devices (DeviceHandles, DeviceInfos)
* 2) 把 DL 模型绑到 GPU 设备
set_dl_model_param (DLModel, 'device', DeviceHandles[0])
*    或全局：set_system('parallelize_operators','true') 让支持的算子自动选设备
* 3) 普通算子可在算子级指定（算子文档会写支持 'device' 参数）
```

## 关键坑
1. **不是所有算子都支持 GPU**：先查算子文档的"Parallelization"段，不支持的仍在 CPU 跑，别以为开了就全快。
2. **GPU 显存**：batch 大/图大/模型大都会爆显存。DL 调 `batch_size` 与输入分辨率是主要杠杆。
3. **数据搬运开销**：CPU↔GPU 传输有成本，单张小图搬来搬去可能比 CPU 还慢——**GPU 适合批量/大图/重算子**。
4. **HALCON 需 GPU 版 License + 装对 CUDA 驱动**：runtime 不含 GPU 支持时算子会静默回退 CPU（要查 warning）。
5. **混合部署**：DL 上 GPU、传统算子留 CPU，是常见且稳妥的组合。

## 算子级加速（不限 GPU）
- `set_system('parallelize_operators','true')`：让 HALCON 内部对**数据并行**的算子自动多线程。
- 用 `count_occurrences`/`tuple` 向量化替代逐像素 HDevelop 循环——**循环是性能杀手**。
- 降分辨率、`reduce_domain` 到 ROI（见 [[halcon/18-性能优化与并行处理]]）。

## 📚 书
NVIDIA CUDA C++ Programming Guide（入门）；HALCON《Parallelization & GPU》白皮书。

## 🎯 检查点
- [ ] 知道 GPU 加速是"按算子开放"而非全自动
- [ ] 写得出把 DL 模型绑到 GPU 设备并验证的方法
- [ ] 理解 CPU↔GPU 搬运开销，知道何时 GPU 反而更慢
