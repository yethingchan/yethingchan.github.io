# Android Studio 打包新 Release 版 APK 完整步骤
你修改完代码后，只需要按照**标准打包流程**操作，就能生成全新的、可发布的 Release 版 APK 包，我给你整理了最简洁、一步不踩坑的操作步骤：

## 一、打包前必做检查（关键）
1. **确认代码已保存**：`Ctrl+S` 全量保存修改
2. **切换到正式构建变体**：
   底部左侧找到 **Build Variants**（构建变体）→ 选择 **release**（不要选 debug）
3. **清理旧构建缓存**（避免打包异常）：
   顶部菜单栏 → `Build` → `Clean Project`（清理项目）
   清理完成后 → `Build` → `Rebuild Project`（重新构建）

---

## 二、正式生成 Release APK（两种方法任选）
### 方法1：传统 APK 打包（最常用）
1. 顶部菜单栏 → **Build** → **Generate Signed Bundle/APK...**
2. 弹出窗口选择：**APK** → 点击 **Next**
3. **密钥配置页面（必须填）**：
   - **Key store path**：选择你之前用的签名文件（.jks / .keystore）
   - **Key store password**：签名库密码
   - **Key alias**：密钥别名
   - **Key password**：密钥密码
   ✅ 勾选 **Remember passwords** 下次不用重复输
4. 点击 **Next**
5. **选择打包目录**：
   - **Destination Folder**：默认即可（一般在 `app/release/` 下）
   - 两个勾选框**保持默认全开**
6. 点击 **Finish**
7. 等待右下角提示 **APK generated successfully** 就打包完成了！

---

### 方法2：Gradle 一键打包（更快）
1. 打开右侧边栏 **Gradle**
2. 展开：`你的项目名` → `app` → `Tasks` → `build`
3. 双击执行：**assembleRelease**
4. 等待执行完成，APK 自动生成在：
   ```
   app/build/outputs/apk/release/
   ```

---

## 三、找到新生成的 APK 文件
打包完成后，新的 Release APK 位置固定在这里：
```
你的项目文件夹/app/release/
或者
你的项目文件夹/app/build/outputs/apk/release/
```
文件名一般是：
- `app-release.apk`
- `app-release-signed.apk`

---

## 四、重要提醒
1. **必须用正式签名文件打包**
   不要用 debug 签名，否则无法覆盖安装/上架应用市场
2. **每次修改代码都要重新打包**
   直接安装旧包不会生效，必须重新执行打包流程
3. **打包失败常见原因**
   - 代码有编译报错（先修复红色错误）
   - 签名密码/别名填错
   - 未 Clean 项目导致缓存冲突

---

### 总结
1. 清理 + 重新构建项目
2. Build → Generate Signed Bundle/APK
3. 选择 APK → 填入签名信息
4. 点击 Finish 生成新包
5. 去 `app/release/` 目录拿最新 APK

按照这个流程操作，就能得到你修改代码后的**最新正式版 APK**。