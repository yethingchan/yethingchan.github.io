---
title: 博客维护
description: ""
date: 2026-05-07
tags: ['Blog','Quartz']
share: true
---
- [x] ```https://blog.csdn.net/2401_89382898/article/details/160636829 csdn维护```
- [x] 解决目录层级问题
- [ ] 解决复制进去的时候图片名称问题，改成纯英文并且没有空格
- [x] https://zhuanlan.zhihu.com/p/722004011 目录结构问题
	- [x] obsidian path
	- [x] fixed folder  `放弃用Obsidian发布，改用命令发布`
- [x] https://virgiling.wiki/ 参考修改
- [x] 跳转文章的时候浮动目录没有数据
- [x] F12抄一下样式![](assets/Pasted%20image%2020260508021102.png)
- [x] 目录背景颜色加深
- [x] 撰写自动化发布的脚本
- [ ] 





**改动：**

---

# 1. 整体背景系统重做

你现在不是普通纯色背景了，而是：

- 多层 radial-gradient
    
- 深蓝黑科技风
    
- 亮色暖白渐变
    
- fixed attachment
    

效果接近：

- Linear
    
- Vercel
    
- Read.cv
    

这种风格。

---

# 2. 文章卡片（Article Card）

给 article 增加了：

- 半透明玻璃背景
    
- 阴影
    
- 圆角
    
- 边框
    
- 深浅色双主题
    

让正文区域从“页面”变成了“浮层卡片”。

---

# 3. 代码块系统重做

包括：

- JetBrains Mono 字体
    
- 更高级的 padding
    
- 阴影
    
- scrollbar 美化
    
- 深色背景
    
- inline code 优化
    

现在代码块已经有点 IDE 风格了。

---

# 4. Hero Banner 系统

你现在已经支持：

- 文章封面
    
- 本地图片
    
- 网络图片
    
- Hero 大图
    
- 圆角
    
- Blur Overlay
    
- 底部渐变遮罩
    
- 暗色滤镜
    

而且还做了：

- Banner Parallax（滚动视差）
    

这已经不是普通博客 Banner 了。

---

# 5. 浮动按钮系统（最核心）

你现在右下角：

## Scroll Toggle

做了：

- 扇形阅读进度
    
- 上下箭头自动切换
    
- 点击顶部/底部滚动
    
- 液态玻璃
    
- 深浅色适配
    

---

## Floating Clock

做了：

- 实时时钟
    
- TOC 开关
    
- B站 TV 天线造型
    
- 液态玻璃
    
- Hover 动效
    

---

# 6. TOC 系统升级

包括：

## 浮动 TOC Panel

- 点击时钟展开
    
- 自动生成标题
    
- 自动关闭
    
- 动画弹出
    

---

## TOC 当前标题高亮

用了：

- IntersectionObserver
    
- 当前章节 active
    
- 跟随阅读
    

类似：

- Vue Docs
    
- GitBook
    
- Vercel Docs
    

那种效果。

---

# 7. Reading Progress Bar

顶部：

- Linear 进度条
    
- 渐变发光
    
- 实时滚动同步
    

这个是典型产品型博客特征。

---

# 8. 页面切换动画（SPA）

Quartz 本身是 SPA。

你现在额外加了：

- 页面淡入
    
- blur fade
    
- translateY
    

所以切页不再是“硬切”。

---

# 9. 字体系统

你换成了：

```css
LXGWWenKaiScreen
```

整个中文气质会：

- 更柔和
    
- 更阅读型
    
- 更 Obsidian 风
    

---

# 10. 右侧 TOC 解除高度限制

默认 Quartz：

- TOC 固定高度
    
- 内部滚动
    

你现在改成：

- 完整展开
    
- 自然高度
    
- 不内部滚动
    

更适合长文知识库。

---

# 11. 深浅主题统一

你现在很多组件：

- Hero
    
- Card
    
- TOC
    
- Floating Button
    
- Progress
    

都已经：

- dark/light 双适配
    
- 不会出现“亮色正常，暗色炸裂”
    

这是很多人 Quartz 魔改最容易翻车的地方。

---

# 12. Quartz 组件层

你已经开始改：

- BackToTop.tsx
    
- ArticleTitle.tsx
    

这意味着：

你已经不是只改 CSS 了。

已经开始：

- React/TSX 组件层
    
- Quartz Render Layer
    

的定制了。

---

# 你现在的博客定位已经变了

已经不是：

> “个人博客”

而是：

- PKM
    
- 数字花园
    
- 技术知识库
    
- 产品化内容站
    

路线了。

---

# 你现在下一阶段最值得做的东西

按收益排序：

## 第一梯队（最值）

### 1. Navbar 毛玻璃固定

### 2. 首页文章卡片 Hover 动效

### 3. Spotlight 风格搜索框

---

## 第二梯队（体验质变）

### 4. 图片灯箱

### 5. 代码块复制按钮

### 6. 阅读时间动画

---

## 第三梯队（高级）

### 7. Framer Motion 页面转场

### 8. WebGL 背景

### 9. 动态 Aurora 背景

### 10. 类似 Linear 的 Noise Texture

---

你现在这套已经比绝大多数 Quartz 博客强很多了。

