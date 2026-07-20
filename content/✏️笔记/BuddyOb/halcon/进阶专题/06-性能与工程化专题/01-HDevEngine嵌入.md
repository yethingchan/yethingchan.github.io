---
title: HDevEngine 把 .hdev 嵌入 C# 工程
---

# 06-1 HDevEngine：把 .hdev 当算法库嵌入

> 上接：[[halcon/进阶专题/06-性能与工程化专题/00-索引]]、[[halcon/17-与C#_NET_VS集成]]

## 为什么用它
你用 HDevelop 调好的算法是一个 `.hdev` 文件。传统做法是**手动翻译成 C# 算子调用**——费时且算法一改就要重翻。HDevEngine 让你**直接在 C# 里加载并运行 .hdev**，算法和工程解耦：算法工程师改 .hdev，软件工程师不动 C#。

## C# 调用骨架

```csharp
// 1) 引用 halcondotnet.dll 与 halconenginedotnet.dll
using HalconDotNet;

// 2) 启动引擎，指定过程函数目录（放 .hdev 的地方）
HDevEngine engine = new HDevEngine();
engine.SetProcedurePath(@"D:\vision\proc");   // 你的算法 .hdev 目录

// 3) 加载一个过程（.hdev 里的 procedure）
HDevProcedure proc = new HDevProcedure("detect_defect");
HDevProcedureCall call = new HDevProcedureCall(proc);

// 4) 设输入变量（对应 .hdev 的 input 控制/图像变量）
HObject img;
HOperatorSet.ReadImage(out img, "test.png");
call.SetInputIconicParamObject("Image", img);

// 5) 执行
call.Execute();

// 6) 取输出
HObject resultRegion = call.GetOutputIconicParamObject("Defect");
HTuple score = call.GetOutputCtrlParamTuple("Score");
```

**逐段解释**：
- `SetProcedurePath`：让引擎找到你的 `.hdev` 过程文件，相当于"算法库路径"。
- `HDevProcedure` 加载的是 .hdev 里**一个 procedure**（不是整个脚本），所以要把算法写成"带 input/output 参数的 procedure"才好被调用——这点要在 HDevelop 里规范好。
- `SetInputIconicParamObject("Image",..)`：名字必须与 .hdev 里声明的 input 变量名**完全一致**，大小写敏感。
- `GetOutput..`：取回结果，类型要对应（图像→`HObject`，数值/元组→`HTuple`）。

## 关键坑（工程必踩）
1. **变量名必须对齐**：input/output 名字拼错，运行时才报错，建议加 try-catch 校验。
2. **资源释放**：`HObject`/`HTuple` 用完要 `Dispose()`，否则**显存/内存泄漏**导致产线跑几天就崩（与 [[halcon/17-与C#_NET_VS集成]] 同坑）。
3. **引擎线程安全**：一个 `HDevEngine` **不是线程安全**的。多线程要**每线程一个引擎实例**，或加锁串行化调用。
4. **发布部署**：目标机要装 HALCON Runtime（有 License），且 .hdev 文件要随程序打包到 `ProcedurePath` 指向的目录。
5. **异常溯源**：`call.Exception` 能拿到 HDevelop 里的报错行，便于算法/工程联调。

## 何时用 HDevEngine vs 直接翻译算子
- ✅ 算法频繁迭代、算法/软件分工 → HDevEngine（敏捷）。
- ✅ 追求极致性能/体积、或要进 Linux 无界面环境 → 直接翻译算子进 C++/C#（可剥离 HDevelop 依赖）。

## 📚 书
HALCON《HDevelop Engine Manual》；[[halcon/17-与C#_NET_VS集成]]。

## 🎯 检查点
- [ ] 能用 HDevEngine 在 C# 加载并运行一个 .hdev procedure
- [ ] 知道 input/output 变量名对齐与 Dispose 的坑
- [ ] 知道引擎非线程安全、多线程要每线程一实例
