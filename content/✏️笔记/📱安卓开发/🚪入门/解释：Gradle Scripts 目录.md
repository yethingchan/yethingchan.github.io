---
title: "解释：Gradle Scripts 目录"
description: ""
date: "2026-05-26"
tags: []
share: true
cover: https://public.ysjf.com/mediastorm/material/material/%E8%87%AA%E7%84%B6%E9%A3%8E%E5%85%89_%E4%B8%9C%E5%8C%97_5_%E5%85%A8%E6%99%AF.jpg
---

这是 Android Studio 里 **Gradle Scripts** 目录下的文件，我给你一个一个拆解开讲清楚它们的作用和常见用法👇

---

## 一、先整体说一下
Gradle 是 Android 项目的**构建工具**，这些 `.gradle`、`.properties`、`.pro` 文件，就是 Gradle 用来构建、编译、打包你的 App 的配置文件。
它们分工明确，各司其职，是 Android 项目的“构建说明书”。

---

## 二、逐个文件详解

### 1. `build.gradle (Project: My_Application)`
这是**项目根目录**下的 `build.gradle`，是整个项目的“全局配置文件”。

- **作用**：
  - 配置整个项目的 Gradle 插件版本（比如 Android Gradle Plugin）
  - 配置项目级别的仓库地址（比如 Maven 仓库、Google 仓库）
  - 定义所有模块共享的依赖版本（比如 Kotlin 版本、第三方库版本）

- 常见结构示例：
```groovy
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath "com.android.tools.build:gradle:7.0.4" // AGP 版本
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.0"
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
```
- 你需要改的场景：
  - 升级 Gradle 插件版本
  - 添加私有 Maven 仓库地址

---

### 2. `build.gradle (Module: My_Application.app)`
这是**app 模块**下的 `build.gradle`，是你项目中**最核心、改得最多**的配置文件。

- **作用**：
  - 配置 App 的编译 SDK 版本、目标 SDK 版本、最低支持 SDK 版本
  - 配置应用 ID（包名）、版本号、版本名称
  - 配置依赖库（比如 Retrofit、Room、RecyclerView 等）
  - 配置构建类型（debug/release）、签名配置、混淆开关等

- 常见结构示例：
```groovy
plugins {
    id 'com.android.application'
    id 'kotlin-android'
}

android {
    compileSdk 31

    defaultConfig {
        applicationId "com.example.myapplication"
        minSdk 21
        targetSdk 31
        versionCode 1
        versionName "1.0"
    }

    buildTypes {
        release {
            minifyEnabled true // 开启混淆
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.7.0'
    implementation 'androidx.appcompat:appcompat:1.4.1'
}
```
- 你需要改的场景：
  - 加第三方依赖库
  - 改包名、版本号
  - 配置签名、开启/关闭混淆
  - 修改 SDK 版本

---

### 3. `gradle-wrapper.properties`
这个文件是 Gradle Wrapper 的配置，用来指定**项目使用的 Gradle 版本**。

- **作用**：
  - 告诉 Android Studio 下载哪个版本的 Gradle
  - 配置 Gradle 的下载地址（国内常用阿里云镜像加速）

- 示例内容：
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-7.0.2-bin.zip
```
- 你需要改的场景：
  - 升级 Gradle 版本
  - 配置国内镜像解决下载慢的问题

---

### 4. `proguard-rules.pro`
这是**混淆规则文件**，ProGuard/R8 工具会根据这里的规则，对代码进行压缩、混淆、优化。

- **作用**：
  - 保留你不想被混淆的类、方法（比如 JNI 调用的类、反射用到的类、第三方库的类）
  - 防止混淆导致的代码运行异常
  - 减少 App 包体积，增加逆向工程难度

- 示例规则：
```proguard
-keep class com.example.myapplication.bean.** { *; } # 保留所有 bean 类
-keepnames class * implements java.io.Serializable # 保留序列化类名
```
- 你需要改的场景：
  - 集成第三方库时，添加库的混淆规则
  - 遇到 `NoClassDefFoundError` 或 `ClassNotFoundException` 时，排查是否被误混淆了

---

### 5. `gradle.properties`
这是**项目全局的属性配置文件**，可以配置 Gradle 构建时的参数。

- **作用**：
  - 配置 Gradle 虚拟机参数（比如内存大小）
  - 开启/关闭 Gradle 特性（比如并行构建、缓存）
  - 定义项目中共享的常量（比如版本号、开关）

- 示例内容：
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
```
- 你需要改的场景：
  - 构建时内存不足，调整 `org.gradle.jvmargs`
  - 开启/关闭 AndroidX
  - 配置全局的构建开关

---

### 6. `settings.gradle`
这是**项目设置文件**，用来配置项目包含哪些模块。

- **作用**：
  - 声明项目包含的模块（比如 app 模块、library 模块）
  - 配置模块名称和路径

- 示例内容：
```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "My_Application"
include ':app'
```
- 你需要改的场景：
  - 添加新的 library 模块
  - 配置多模块项目的模块路径

---

### 7. `local.properties`
这是**本地配置文件**，记录了你的电脑上 Android SDK 的路径。

- **作用**：
  - 告诉 Android Studio 你的 SDK 安装位置
  - 这个文件是本地生成的，不会提交到 Git（`.gitignore` 默认忽略它）

- 示例内容：
```properties
sdk.dir=C\:\\Users\\xxx\\AppData\\Local\\Android\\Sdk
```
- 注意：**不要手动修改它**，Android Studio 会自动维护。如果 SDK 路径变了，重新在 Studio 里设置 SDK 路径即可。

---

## 三、它们之间的关系（简单梳理）
1.  `settings.gradle` → 先确定项目有哪些模块
2.  根目录 `build.gradle` + `gradle.properties` → 配置全局仓库、插件、属性
3.  `gradle-wrapper.properties` → 决定用哪个版本的 Gradle
4.  app 模块 `build.gradle` → 配置 App 本身的 SDK、依赖、签名、混淆
5.  `proguard-rules.pro` → 给混淆工具用的规则
6.  `local.properties` → 本地 SDK 路径，和项目代码无关

---

## 四、常见修改场景总结
| 需求 | 要改哪个文件 |
| :--- | :--- |
| 加第三方依赖库 | `build.gradle (Module: app)` |
| 升级 Gradle 版本 | `gradle-wrapper.properties` |
| 升级 Android Gradle 插件版本 | 根目录 `build.gradle` |
| 配置混淆规则 | `proguard-rules.pro` |
| 调整构建内存、开启/关闭 AndroidX | `gradle.properties` |
| 添加新模块 | `settings.gradle` |

---

如果你想的话，我可以帮你整理一份**常用修改模板**（比如依赖库添加、签名配置、混淆规则），你直接复制粘贴就能用，需要吗？