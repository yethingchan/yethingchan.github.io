---
title: "配置：AGP 7版本"
description: ""
date: "2026-05-27"
tags: []
share: true
cover: https://public.ysjf.com/mediastorm/material/material/%E8%87%AA%E7%84%B6%E9%A3%8E%E5%85%89_%E4%B8%9C%E5%8C%97_5_%E5%85%A8%E6%99%AF.jpg
---

## 版本配对（稳定组合，JDK11 即可运行）

- AGP：`7.2.2`（主流稳定版）
- Gradle：`7.5`
- compileSdk：`31`（和 AGP7.2.2 官方适配）
- JDK：保持你当前 **JDK11** 不用改动

---

## 一、逐个文件替换内容（按顺序改）

### 1. 根目录 `gradle-wrapper.properties`（Gradle 版本 + 国内镜像）

路径：项目根目录 /gradle/wrapper/gradle-wrapper.properties

properties

```
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
# 国内腾讯镜像 Gradle7.5
distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-7.5-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

### 2. 根目录 `build.gradle`（外层，修改 AGP 版本）

删除之前残留的 `allprojects > repositories` 块（Gradle7/8 通用规则），完整内容：

gradle

```
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // 改为 AGP 7.2.2
        classpath "com.android.tools.build:gradle:7.2.2"
    }
}

task clean(type: Delete) {
    delete rootProject.buildDir
}
```

### 3. 根目录 `settings.gradle`（统一仓库 + 国内镜像，必配）

Gradle7 也推荐仓库统一放这里，完整内容：

gradle

```
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 国内镜像优先
        maven { url 'https://mirrors.cloud.tencent.com/repository/maven-public/' }
        google()
        mavenCentral()
    }
}

rootProject.name = "My Application"
include ':app'
```

### 4. `app/build.gradle`（模块配置，修改 compileSdk）

gradle

```
plugins {
    id 'com.android.application'
}

android {
    // 适配AGP7.2.2，改为 31
    compileSdk 31

    defaultConfig {
        applicationId "com.example.myapplication"
        minSdk 21
        targetSdk 31
        versionCode 1
        versionName "1.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.4.2'
    implementation 'com.google.android.material:material:1.5.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.3'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.4.0'
}
```

---

## 二、收尾操作（必做）

1. 删掉之前的全局 `init.gradle`（如果还有）
2. 点击 Android Studio 右上角 **Sync Project with Gradle Files** 同步
3. 同步完成后，点击 `Build -> Clean Project` 清理缓存
4. 重新运行项目

---

## 补充说明

1. 该整套配置 **完全兼容 JDK11**，不用切换 JDK，解决类版本不匹配报错；
2. 所有仓库已配置国内镜像，下载依赖速度更快；
3. 如果你后续想升级其他 AGP7 版本，参考配对表：
    
    - AGP 7.0.x → Gradle 7.0.2~7.0.4
    - AGP 7.1.x → Gradle 7.2
    - AGP 7.2.x → Gradle 7.3~7.5（当前选用）
    - AGP 7.3.x → Gradle 7.4+
    
4. 依旧遵循规则：**不要在根 build.gradle、app/build.gradle 里写 repositories**，全部统一在 `settings.gradle` 管理。