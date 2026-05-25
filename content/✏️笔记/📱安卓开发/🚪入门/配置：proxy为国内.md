---
title: "配置proxy为国内"
description: ""
date: "2026-05-25"
tags: []
share: true
cover: https://public.ysjf.com/mediastorm/material/material/%E8%87%AA%E7%84%B6%E9%A3%8E%E5%85%89_%E4%B8%9C%E5%8C%97_5_%E5%85%A8%E6%99%AF.jpg
---
### 配置 SDK 镜像源

![](assets/Pasted%20image%2020260525235249.png)

在弹出的窗口中，将默认的 Google 镜像 地址修改为国内的镜像地址
```
华为镜像：  https://developer.huawei.com/repo/
豆瓣镜像：  https://mirrors.douban.com/android/sdk/
腾讯：     https://mirrors.cloud.tencent.com/AndroidSDK/
阿里：     https://mirrors.aliyun.com/android.googlesource.com/
```

### 在gradle的wrapper中改变下载地址

```
distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-8.7-all.zip
```
![](assets/Pasted%20image%2020260525235459.png)


### 在setting.gradle.kts中两处repositories后加入以下代码

```
maven{url=uri("https://maven.aliyun.com/repository/google")}
maven{url=uri("https://maven.aliyun.com/repository/central")}
```
![](assets/Pasted%20image%2020260525235507.png)
