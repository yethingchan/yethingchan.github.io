---
title: HDevelop 环境与 HALCON 语法
---

# L1-2 HDevelop 环境与 HALCON 语法

> 前置：[[halcon/01-HALCON概述与机器视觉基础]]
> 下接：[[halcon/03-图像基础与坐标系]]

## 1. HDevelop 界面

- **程序窗口**：写 `.hdev` 程序（算子调用）。
- **变量窗口**：实时查看 image/region/xld 变量，可双击可视化。
- **算子窗口（F1）**：查算子签名、参数说明、例程。
- **图形窗口**：显示图像与结果覆盖层。
- 快捷键：`F5` 运行、`F6` 单步、`F9` 断点。

## 2. HALCON 程序基本结构

```halcon
* 这是注释
read_image (Image, 'C:/pic/board.jpg')   * 读图，算子名后空格跟参数
dev_open_window (0, 0, 512, 512, 'black', WindowHandle)
dev_display (Image)

* 变量赋值用 :=  （注意不是 = ）
Width := 512
Threshold := 128
```

## 3. 算子（Operator）调用语法

```
算子名 (输入1, 输入2, ..., 输出1, 输出2, ...)
```

- 输入在前、输出在后，按**位置**传参，**没有命名参数**。
- 这是 HALCON 与 C/C# 调用时最容易出错的地方（顺序要对齐）。

## 4. 控制变量 vs 图标变量

| 类型 | 例子 | 说明 |
|------|------|------|
| **图标变量（iconic）** | Image, Region, XLD | 图像类数据，在图形窗口显示 |
| **控制变量（control）** | 整数/实数/字符串/句柄 | 在变量窗口显示数值 |

```halcon
* 控制变量
dev_close_window ()
Tuple := [1,2,3]            * 元组(tuple)，HALCON 的"数组"
for Index := 0 to 2 by 1
    Val := Tuple[Index]
endfor

* 图标变量
threshold (Image, Region, 128, 255)
```

## 5. 元组（Tuple）基础

- HALCON 没有数组/列表，用 **tuple** 表达多值：`[1,2,3]`、`['a','b']`。
- 常用：`tuple_length`、`tuple_sum`、`tuple_gen_sequence(1,10,1)`。

## 6. 程序流程

`if / else / endif`、`for / endfor`、`while / endwhile`、`case / endswitch`。

## 📚 本阶段要看的书

- HALCON《HDevelop User's Guide》
- 跟着 `help → Examples` 里 `hdevelop` 基础例程敲一遍

## 🎯 达标水平

能在 HDevelop 新建 `.hdev`，读懂算子签名（输入输出顺序），会赋值、循环、显示图像。

## 学习检查点

- [ ] 能区分图标变量和控制变量
- [ ] 能写出 for 循环遍历 tuple
- [ ] 知道算子调用是"输入在前输出在后"
