---
<<<<<<< HEAD
title: "Quartz的目录原理"
description: ""
date: "2026-05-07"
=======
title: Quartz的目录原理
description: ""
date: 2026-05-07
>>>>>>> 78388e8a0696d60e009b49dc364d9a310e687cf0
tags: []
share: true
---
下面把 **Quartz（通常指 Quartz 4）** 的目录原理、路径到页面的映射，以及可定制点系统化拆开讲。你可以把它理解为：**“用文件系统当数据库 + 用构建器把 Markdown 编译成一棵网站路由树”**。

---

# 一、核心模型：文件系统 → 路由树

Quartz 的核心规则很简单但很严格：

> **content/ 目录的层级 = 网站 URL 层级 = 页面侧边栏结构（在启用对应组件时）**

## 1. 基本映射规则

假设你的仓库是：

```text
content/
├── index.md
├── notes/
│   ├── index.md
│   ├── java.md
│   └── spring/
│       └── ioc.md
└── blog/
    └── 2026-05-07.md
```

构建后得到：

|文件|URL|页面表现|
|---|---|---|
|`content/index.md`|`/`|首页|
|`content/notes/index.md`|`/notes/`|notes目录首页|
|`content/notes/java.md`|`/notes/java`|普通文章|
|`content/notes/spring/ioc.md`|`/notes/spring/ioc`|多级路径|
|`content/blog/2026-05-07.md`|`/blog/2026-05-07`|博客文章|

👉 **结论：**

- 文件夹 = URL path segment
    
- `.md` 文件 = 页面
    
- `index.md` = 该目录的“默认页”
    

---

## 2. index.md 的语义（关键）

`index.md` 不是普通文章，它有两个作用：

### ✅ 作用1：目录入口页

访问 `/notes/` 时展示

### ✅ 作用2：决定目录展示逻辑

配合组件（如 FolderContent / Explorer）时：

- 它可以控制这个目录的展示方式
    
- 可以写介绍、导航、列表
    

---

# 二、目录如何影响“博客结构表现”

Quartz 的“页面结构”其实由组件控制，但**数据来源仍是目录**。

## 常见三种表现形式：

---

## 1. Explorer（文件树）

类似 Obsidian 左侧目录：

- 完全按照 `content/` 层级展开
    
- 文件夹就是树节点
    
- 文件就是叶子节点
    

👉 这是最“原生目录映射”的表现

---

## 2. FolderContent（目录聚合页）

比如访问：

```
/notes/
```

可以自动列出：

- 子文件
    
- 子目录文章
    

排序方式可配置（后面讲）

---

## 3. Tag / Graph / Recent

这些是“派生视图”，不依赖目录结构：

- 标签页：基于 frontmatter
    
- 最近文章：基于日期
    
- 图谱：基于链接关系
    

👉 这些是“非目录驱动”的结构

---

# 三、Frontmatter 如何影响结构

每个 `.md` 文件顶部：

```yaml
---
title: xxx
date: 2026-05-07
tags: [java, backend]
draft: false
---
```

## 关键字段作用：

### 1. title

- 默认：文件名
    
- 覆盖页面显示标题
    

---

### 2. date

- 用于排序（博客流）
    
- Recent / RSS / BlogList 会用
    

---

### 3. tags

- 生成 `/tags/...` 页面
    
- 与目录无关，是“横向分类”
    

---

### 4. draft

```yaml
draft: true
```

👉 构建时不会发布

---

# 四、构建流程（你必须理解）

Quartz 本质是一个 pipeline：

## 步骤：

1. 读取 `content/`
    
2. 解析 Markdown → AST
    
3. 应用插件（links、math、code等）
    
4. 生成 HTML
    
5. 写入 `public/`
    

---

## 路由生成逻辑（简化）

```text
content/a/b/c.md
→ route: /a/b/c
→ 输出: public/a/b/c/index.html
```

👉 所以它是“静态路由展开”，不是动态服务器

---

# 五、自定义方式（重点）

你可以从 4 个层面改 Quartz：

---

## 1. 目录结构（最基础）

直接改：

```text
content/
```

比如：

```text
content/
├── docs/
├── blog/
├── projects/
```

👉 就会变成：

```
/docs
/blog
/projects
```

---

## 2. 组件配置（决定页面长什么样）

在：

```text
quartz.config.ts
```

例如：

```ts
components: {
  left: [Explorer()],
  right: [TableOfContents()],
  content: [Content()],
}
```

你可以：

- 去掉 Explorer（不显示目录树）
    
- 加 BlogList（变博客站）
    
- 加 Graph（知识图谱）
    

---

## 3. 插件系统（决定数据怎么处理）

Quartz 有一套插件链：

```ts
plugins: {
  transformers: [],
  filters: [],
  emitters: [],
}
```

### 常见用途：

- 自动生成目录页
    
- 修改路径规则
    
- 自定义 Markdown 解析
    

---

## 4. 样式 / UI（主题层）

位置：

```text
quartz/styles/
```

你可以：

- 改 CSS
    
- 改布局
    
- 改组件样式
    

---

## 5. 路由重写（进阶）

可以通过插件或配置实现：

- `/post/xxx` 替代 `/blog/xxx`
    
- 扁平化路径
    
- 自定义 slug
    

---

# 六、几种典型博客结构设计

给你几个“工程化结构”参考：

---

## 1. Obsidian 知识库型

```text
content/
├── index.md
├── notes/
│   ├── java/
│   ├── python/
│   └── system-design/
```

👉 特点：

- 强依赖目录
    
- 类似 wiki
    

---

## 2. 博客型（推荐）

```text
content/
├── index.md
├── blog/
│   ├── 2026-05-07-xxx.md
│   └── 2026-05-08-yyy.md
├── about.md
```

👉 配合：

- BlogList
    
- 按 date 排序
    

---

## 3. 混合型（最常见）

```text
content/
├── blog/
├── notes/
├── projects/
```

👉 目录 = 主导航

---

# 七、你必须注意的坑

### ❗1. 重名问题

```text
a.md
a/index.md
```

👉 会冲突（路由相同）

---

### ❗2. 中文路径

可以用，但：

- URL 会被编码
    
- SEO 不友好
    

---

### ❗3. index.md 滥用

太多 index 会导致结构混乱

---

### ❗4. 目录 ≠ 标签

不要用目录做分类 + 再用 tag 做同样分类（重复建模）

---

# 八、总结一句话

> Quartz 的本质是：  
> **“用文件夹结构定义网站结构，用 frontmatter 定义内容语义，用组件决定展示方式”**

---

如果你下一步想深入，我可以帮你做一套：

- 👉 **适合你（Java + 学习 + 项目）的 Quartz 目录设计**
    
- 👉 URL、标签、分类一体化方案
    
- 👉 SEO + 博客流 + 知识库融合结构
    

直接说你想做“博客 / 知识库 / 简历 / 项目展示”，我给你一套完整架构。