---
title: "Gradle版本对照"
description: ""
date: "2026-05-26"
tags: []
share: true
cover: https://public.ysjf.com/mediastorm/material/material/%E8%87%AA%E7%84%B6%E9%A3%8E%E5%85%89_%E4%B8%9C%E5%8C%97_5_%E5%85%A8%E6%99%AF.jpg
---

| Android Studio 版本 | AGP 版本    | Gradle 版本 |
| ----------------- | --------- | --------- |
| Hedgehog          | 8.1+      | 8.3+      |
| Giraffe           | 8.0+      | 8.1+      |
| Flamingo          | 7.4+      | 7.5+      |
| Chipmunk          | 7.2+      | 7.3.3+    |
| **Bumblebee**     | **7.0.x** | **7.2+**  |
| Arctic Fox        | 7.0.x     | 7.0+      |
| Arctic Fox        | 4.2.0     | 6.7.1     |
## 1. 修改 Gradle 版本

文件：`gradle/wrapper/gradle-wrapper.properties`

plaintext

```
distributionUrl=https\://services.gradle.org/distributions/gradle-7.2-all.zip
```

## 2. 修改 AGP 版本

文件：**项目根目录 build.gradle**

groovy

```
dependencies {
    classpath 'com.android.tools.build:gradle:7.0.4'
}
```

---

 