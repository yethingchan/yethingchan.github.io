---
title: "路线Map"
description: ""
date: "2026-05-25"
tags: []
share: true
cover: https://public.ysjf.com/mediastorm/material/material/%E8%87%AA%E7%84%B6%E9%A3%8E%E5%85%89_%E4%B8%9C%E5%8C%97_5_%E5%85%A8%E6%99%AF.jpg
---

## 整体说明（你是Java转安卓）
- 优势：**Java基础扎实、面向对象、集合、线程、IO都不用重学**
- 建议语言：**主学Kotlin（官方首选），兼容Java**；你Java写demo完全没问题
- UI：**先学XML基础，再主攻Jetpack Compose（未来主流）**
- 周期：**4–6个月**（每天2–3小时），从零基础到能独立做商业级App

---

# 阶段一：环境+Kotlin+安卓基础（第1个月）
### 目标
能跑通第一个自己写的App，懂安卓基本概念，会用Android Studio。

## 第1周：环境搭建+熟悉工具
1. 安装 **Android Studio（最新稳定版）**
   - 安装SDK（默认自带）、模拟器（或直接用你自己安卓手机）
   - 配置：**Gradle、SDK版本、国内镜像（阿里云）**
2. 真机调试（必做）
   - 手机开**开发者选项 → USB调试 → USB安装**
   - 数据线连电脑，Studio直接Run，自动装到手机
3. 熟悉Studio常用窗口
   - Logcat（看日志）、Layout Inspector（看布局）、Profiler（性能）
4. 练习：创建空项目，改个文字，跑在模拟器/真机上

## 第2周：Kotlin快速入门（Java转安卓重点）
因为你会Java，**Kotlin一周足够**，重点学差异：
1. 变量：val（只读）/var（可变）
2. 空安全：`?`、`!!`、`?:`（避免空指针）
3. 函数：默认参数、命名参数、Lambda
4. 数据类：`data class`（自动get/set/toString）
5. 扩展函数：给系统类加方法（非常好用）
6. 协程基础：`CoroutineScope`、`launch`（异步必备）
- 练习：用Kotlin写个计算器逻辑、字符串处理、集合遍历

## 第3–4周：安卓四大组件+基础UI（XML）
### 1）Activity（核心）
- 生命周期：`onCreate`、`onStart`、`onResume`、`onPause`、`onStop`、`onDestroy`
- 启动方式：显式Intent、隐式Intent
- 数据传递：Intent传值、Bundle、Serializable/Parcelable
- 练习：2个Activity互相跳转，传用户名+年龄

### 2）XML布局（传统UI，必须会）
- 常用布局：LinearLayout、ConstraintLayout（重点）、FrameLayout
- 常用控件：TextView、Button、EditText、ImageView、ListView/RecyclerView（重点）
- 事件处理：onClick、长按、触摸事件
- 练习：写一个登录页面（账号+密码+登录按钮）、一个列表展示10条数据

### 3）Fragment（页面模块化）
- 生命周期、与Activity通信、返回栈管理
- 练习：一个Activity里放2个Fragment，切换显示

### 4）BroadcastReceiver（广播）
- 静态注册、动态注册、接收系统广播（如开机、网络变化）
- 练习：监听网络状态变化，弹出提示

### 5）Service（后台服务）
- 启动方式：startService、bindService
- 前台服务（通知栏显示，如音乐播放）
- 练习：写一个后台计时服务，通知栏显示时间

---

# 阶段二：数据存储+网络+Jetpack（第2个月）
### 目标
能做**网络请求+本地缓存+MVVM架构**的完整模块，比如新闻App、天气App。

## 第5周：数据存储（本地持久化）
1. **SharedPreferences**：轻量键值对（存登录状态、设置）
2. **内部存储/外部存储**：文件读写（下载图片、文档）
3. **Room数据库（重点）**：安卓官方ORM，替代SQLite
   - 实体类、DAO、数据库实例、增删改查
   - 结合协程异步操作
- 练习：写一个备忘录App（Room存笔记，增删改查）

## 第6周：网络请求（必学）
1. 基础：HTTP/HTTPS、GET/POST、JSON解析（Gson）
2. 主流框架：**Retrofit + OkHttp**（行业标准）
   - 接口定义、请求/响应实体、拦截器（日志、缓存）
3. 异步：**Kotlin协程 + ViewModel**（避免内存泄漏）
4. 错误处理：网络异常、数据为空、token过期
- 练习：对接一个公开API（如天气、新闻），展示列表数据

## 第7–8周：Jetpack组件（现代安卓核心）
Jetpack是官方组件库，**必学，直接决定你代码质量**：
1. **ViewModel**：生命周期独立，存UI数据（屏幕旋转不丢）
2. **LiveData/StateFlow**：可观察数据，UI自动更新
3. **Navigation**：页面跳转管理，简化Fragment栈
4. **Lifecycle**：生命周期感知，自动管理组件（如协程取消）
5. **WorkManager**：后台任务（定时、约束条件）
- 架构：**MVVM**（ViewModel + LiveData + 数据绑定）
- 练习：把之前的新闻/天气App改造成**MVVM架构**，用ViewModel+StateFlow管理数据

---

# 阶段三：Jetpack Compose + 高级UI（第3个月）
### 目标
掌握**声明式UI**，能写流畅、美观的现代界面，替代XML。

## 第9–10周：Jetpack Compose基础
1. 核心概念：**声明式UI、状态驱动、重组**
2. 基础组件：Text、Button、Image、Icon、Row/Column/Box、LazyColumn（列表）、Scaffold（页面骨架）
3. 状态管理：`mutableStateOf`、`remember`、`viewModel`
4. 导航：Compose Navigation
5. 主题：颜色、字体、形状、深色模式
- 练习：用Compose重写之前的登录页、新闻列表页

## 第11–12周：高级UI与适配
1. 自定义View/Compose组件：封装通用控件（如自定义按钮、卡片）
2. 屏幕适配：不同尺寸、分辨率、横竖屏
3. 图片加载：**Coil**（Compose首选，替代Glide）
4. 动画：Compose内置动画（渐入、平移、缩放）
5. Material Design 3：现代设计规范（按钮、卡片、对话框）
- 练习：做一个个人主页（头像、列表、设置项、深色模式切换）

---

# 阶段四：高级特性+性能优化+项目实战（第4–5个月）
### 目标
能独立开发**商业级完整App**，懂性能优化、常见问题解决。

## 第13–14周：高级特性
1. **权限处理**：动态申请（相机、存储、定位）、权限回调
2. **多媒体**：相机、相册、录音、视频播放（ExoPlayer）
3. **推送**：极光推送/个推（接收后台消息）
4. **支付**：支付宝/微信支付集成
5. **WebView**：内嵌网页、JS交互
6. **NDK基础（可选）**：JNI调用C++代码（高性能场景）

## 第15–16周：性能优化（面试+实战重点）
1. **内存优化**：内存泄漏检测（LeakCanary）、Bitmap优化、避免静态Activity/Context
2. **启动优化**：冷启动/热启动、异步初始化、启动器（AppStartup）
3. **渲染优化**：过度绘制、帧率监控、Systrace/Perfetto工具
4. **包体积优化**：资源压缩、R8/ProGuard混淆、动态下发
5. **网络优化**：缓存、请求合并、弱网处理

## 第17–20周：完整项目实战（必做，2个）
### 项目1：简易电商App（综合练手）
- 功能：首页轮播、商品列表、商品详情、购物车、登录注册、个人中心
- 技术：Compose + MVVM + Retrofit + Room + Coil + 权限
- 目标：代码规范、架构清晰、可直接放GitHub

### 项目2：自选主题（深化）
- 可选：音乐播放器、短视频App、待办任务、笔记App、健身记录
- 要求：加入**推送、支付、离线缓存、深色模式、性能优化**

---

# 阶段五：面试/进阶方向（第6个月+）
### 1）面试高频知识点
- 四大组件原理、Activity启动模式、Intent过滤
- 事件分发机制、View绘制流程
- Handler消息机制（Looper、MessageQueue）
- BinderIPC机制（进程通信）
- 内存泄漏常见场景与解决
- Jetpack组件原理（ViewModel、LiveData）

### 2）进阶方向（选一个深耕）
- **原生开发**：Framework源码、NDK、ROM定制、性能调优
- **跨平台**：Flutter、Kotlin Multiplatform（KMP）
- **大前端**：React Native、UniApp
- **车载/穿戴**：Android Auto、Wear OS

---

# 每天学习计划（参考，2–3小时）
1. 30分钟：复习昨天知识点+看官方文档
2. 60–90分钟：学新知识点+写Demo代码
3. 30–60分钟：做项目实战+解决Bug
4. 每周日：总结+重构代码+GitHub提交

---

# 避坑指南（Java转安卓必看）
1. **别一直用Java写安卓**：尽快转Kotlin，语法简洁、安全、官方主推
2. **别只学XML不学Compose**：2026年开始，Compose是主流，面试必问
3. **别只学API不学原理**：四大组件、Handler、事件分发原理一定要懂
4. **别不做项目**：光看视频没用，**项目是最好的学习方式**
5. **别用老教程**：安卓更新快，优先看**2024–2026年**的教程+官方文档

---

# 推荐学习资源
1. 官方文档：developer.android.com（最权威）
2. 书籍：《Kotlin编程权威指南》《Android Jetpack开发指南》《Jetpack Compose实战》
3. 视频：B站（郭霖、字节技术、安卓巴士）
4. 工具：GitHub（看优秀项目）、LeakCanary（内存泄漏）、Profiler（性能）
 