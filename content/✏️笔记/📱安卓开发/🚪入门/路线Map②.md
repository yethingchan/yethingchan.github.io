---
title: "路线Map②"
description: ""
date: "2026-05-26"
tags: []
share: true
cover: https://public.ysjf.com/mediastorm/material/material/%E8%87%AA%E7%84%B6%E9%A3%8E%E5%85%89_%E4%B8%9C%E5%8C%97_5_%E5%85%A8%E6%99%AF.jpg
---

如果你是以“就业 + 独立开发 + 长期技术成长”为目标学习 Android，建议你不要再走以前那种“只学 Java + XML + Activity”的老路线，而是直接进入现代 Android 技术栈：

- Kotlin
    
- Jetpack
    
- Compose
    
- 协程
    
- MVVM
    
- Room
    
- Retrofit
    
- Flow
    
- Hilt
    
- Gradle
    
- CI/CD
    

这是目前 Android 中高级岗位的主流路线。

下面我给你一套比较完整的 Android Studio 学习路线，偏“实战型”。

---

# Android Studio 技术学习路线（2026版）

---

# 第一阶段：基础能力（2~4周）

目标：

- 能独立写简单 App
    
- 熟悉 Android Studio
    
- 理解 Android 基础运行机制
    

---

## 1. Kotlin 基础（必须）

Android 现在核心语言已经是 Kotlin。

重点：

- 变量
    
- 函数
    
- 类与对象
    
- data class
    
- sealed class
    
- null safety
    
- lambda
    
- 扩展函数
    
- 集合 API
    
- 协程基础
    

推荐资源：

### 官方

- [Kotlin Official Docs](https://kotlinlang.org/docs/home.html?utm_source=chatgpt.com)
    
- [Kotlin Playground](https://play.kotlinlang.org/?utm_source=chatgpt.com)
    

### 视频

- [Philipp Lackner YouTube](https://www.youtube.com/@PhilippLackner?utm_source=chatgpt.com)
    
- [freeCodeCamp Kotlin Course](https://www.youtube.com/watch?v=F9UC9DY-vIU&utm_source=chatgpt.com)
    

### 中文

- [菜鸟 Kotlin 教程](https://www.runoob.com/kotlin/kotlin-tutorial.html?utm_source=chatgpt.com)
    

---

## 2. Android Studio 基础

熟悉：

- Project Structure
    
- Gradle
    
- Logcat
    
- Emulator
    
- Debug
    
- APK 打包
    
- 真机调试
    
- Git 集成
    

官方：

- [Android Studio 官方](https://developer.android.com/studio?utm_source=chatgpt.com)
    

---

# 第二阶段：Android 核心开发（1~2个月）

目标：

- 能开发完整 App
    
- 理解 Android 生命周期
    

---

## 3. 四大组件（核心）

必须掌握：

|组件|作用|
|---|---|
|Activity|页面|
|Fragment|页面模块|
|Service|后台任务|
|BroadcastReceiver|广播|
|ContentProvider|数据共享|

重点：

- 生命周期
    
- Intent
    
- 页面跳转
    
- Fragment 通信
    

官方：

- [Android Developers 官方文档](https://developer.android.com/docs?utm_source=chatgpt.com)
    

---

## 4. UI 开发

这里分两条路线：

## 老路线（了解即可）

- XML
    
- ConstraintLayout
    
- RecyclerView
    

## 新路线（重点）

- Jetpack Compose
    

现在越来越多公司开始使用 Compose。

推荐：

### 官方 Compose

- [Jetpack Compose 官方](https://developer.android.com/jetpack/compose?utm_source=chatgpt.com)
    

### 学习资源

- [Compose Pathway](https://developer.android.com/courses/pathways/compose?utm_source=chatgpt.com)
    
- [Compose Codelab](https://developer.android.com/codelabs/jetpack-compose-basics?utm_source=chatgpt.com)
    

### YouTube

- [Android Developers YouTube](https://www.youtube.com/@AndroidDevelopers?utm_source=chatgpt.com)
    

---

# 第三阶段：现代 Android 架构（1~2个月）

这一阶段决定你能不能从“会写页面”变成“真正会 Android”。

---

## 5. MVVM 架构

核心：

- ViewModel
    
- State
    
- Repository
    
- UI State
    
- 单向数据流
    

理解：

```text
UI -> ViewModel -> Repository -> API/DB
```

---

## 6. Jetpack 组件

必须掌握：

|技术|用途|
|---|---|
|ViewModel|状态管理|
|LiveData / StateFlow|数据流|
|Navigation|页面导航|
|Room|本地数据库|
|WorkManager|后台任务|
|DataStore|本地存储|

官方：

- [Jetpack 官方](https://developer.android.com/jetpack?utm_source=chatgpt.com)
    

---

## 7. Kotlin Coroutines 协程

现代 Android 必学。

核心：

- suspend
    
- launch
    
- async
    
- Flow
    
- StateFlow
    
- SharedFlow
    

推荐：

- [Coroutines 官方指南](https://kotlinlang.org/docs/coroutines-overview.html?utm_source=chatgpt.com)
    

---

# 第四阶段：网络与数据（2~4周）

---

## 8. 网络请求

必须：

|技术|用途|
|---|---|
|Retrofit|HTTP 请求|
|OkHttp|网络层|
|Gson / Kotlin Serialization|JSON|
|REST API|接口通信|

推荐：

- [Retrofit GitHub](https://github.com/square/retrofit?utm_source=chatgpt.com)
    
- [OkHttp GitHub](https://github.com/square/okhttp?utm_source=chatgpt.com)
    

---

## 9. 本地数据库

重点：

- Room
    
- SQLite 基础
    
- DAO
    
- Migration
    

---

# 第五阶段：高级开发（长期）

---

## 10. Dependency Injection（依赖注入）

学习：

- Hilt（推荐）
    
- Dagger（了解）
    

官方：

- [Hilt 官方](https://developer.android.com/training/dependency-injection/hilt-android?utm_source=chatgpt.com)
    

---

## 11. 性能优化

必须懂：

- 内存泄漏
    
- ANR
    
- 启动优化
    
- RecyclerView 优化
    
- Compose Recomposition
    
- LeakCanary
    
- Profiler
    

推荐：

- [LeakCanary GitHub](https://github.com/square/leakcanary?utm_source=chatgpt.com)
    

---

## 12. 多模块 & 工程化

中高级必学：

- Gradle
    
- 多 Module
    
- CI/CD
    
- GitHub Actions
    
- 组件化
    

---

# 第六阶段：项目实战（非常重要）

真正成长主要靠项目。

---

## 推荐项目路线

---

## 初级项目

### Todo App

练：

- Room
    
- Compose
    
- MVVM
    

---

## 中级项目

### 仿 Bilibili / 网易云

练：

- Retrofit
    
- Paging
    
- 视频播放
    
- Compose
    
- 状态管理
    

---

## 高级项目

### Chat App

练：

- WebSocket
    
- Firebase
    
- 实时通信
    
- 推送
    
- Clean Architecture
    

---

# Android 必看学习资源

---

# 官方路线（优先级最高）

## Android 官方学习路线

- [Android Developer Roadmap](https://developer.android.com/courses?utm_source=chatgpt.com)
    

## 官方 Codelabs

- [Android Codelabs](https://developer.android.com/codelabs?utm_source=chatgpt.com)
    

---

# YouTube 高质量频道

---

## Android 官方

- [Android Developers](https://www.youtube.com/@AndroidDevelopers?utm_source=chatgpt.com)
    

---

## 非常推荐（实战派）

### Philipp Lackner

- [Philipp Lackner Channel](https://www.youtube.com/@PhilippLackner?utm_source=chatgpt.com)
    

特点：

- 现代 Android
    
- MVVM
    
- Compose
    
- Hilt
    
- Clean Architecture
    

非常适合就业。

---

## 进阶

### Coding in Flow

- [Coding in Flow](https://www.youtube.com/@codinginflow?utm_source=chatgpt.com)
    

---

# GitHub 开源项目（一定要看）

---

## Google 官方

- [Now in Android](https://github.com/android/nowinandroid?utm_source=chatgpt.com)
    

这是 Google 官方现代 Android 示例项目。

学习价值极高。

---

## Compose 学习

- [JetNews](https://github.com/android/compose-samples?utm_source=chatgpt.com)
    

---

# 中文资源推荐

---

## B站推荐 UP

可以搜：

- Android Jetpack
    
- Kotlin 协程
    
- Compose 实战
    
- MVVM Android
    

重点找：

- 最新两年
    
- Compose
    
- Kotlin
    
- Hilt
    

不要学太老的 Java Android 视频。

---

# 学习顺序（最推荐）

建议按这个顺序：

```text
Kotlin
↓
Android Studio
↓
Activity + Fragment
↓
Compose
↓
MVVM
↓
Coroutine + Flow
↓
Retrofit
↓
Room
↓
Hilt
↓
项目实战
↓
性能优化
↓
工程化
```

---

# 给你的建议（很重要）

你之前是 Java 实习开发背景，这对 Android 很有优势。

你真正应该重点突破的是：

|技术|重要程度|
|---|---|
|Kotlin|极高|
|Compose|极高|
|MVVM|极高|
|协程 Flow|极高|
|Jetpack|极高|
|工程化|高|
|性能优化|高|

不要陷入：

- 只会 XML
    
- 只会 Activity
    
- 只会 Java Android
    
- 只会 CRUD
    

这些已经偏旧了。

---

# 最后给你一套“就业型学习组合”

如果你时间有限：

## 第一优先级

- Kotlin
    
- Compose
    
- MVVM
    
- Retrofit
    
- Room
    
- Coroutine
    

## 第二优先级

- Hilt
    
- Clean Architecture
    
- Flow
    
- DataStore
    

## 第三优先级

- 性能优化
    
- 多模块
    
- CI/CD
    

---

如果你愿意，我还能继续帮你整理：

1. Android Studio 从零到就业的「6个月学习计划」
    
2. Android 项目推荐（适合写进简历）
    
3. Android 校招/实习面试八股路线
    
4. Compose 完整学习路线
    
5. Android + Spring Boot 全栈路线
    
6. Android 独立开发赚钱方向
    
7. Android GitHub 项目推荐（适合学习源码）