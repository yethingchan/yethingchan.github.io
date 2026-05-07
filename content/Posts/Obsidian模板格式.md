---
title: Obsidian模板格式
description: ""
date: 2026-05-07
tags: []
share: true
---
可以做到，而且不需要模板文件，直接给你一个**最短路径、最稳定的实现**。核心用 QuickAdd，因为它支持**内联变量 + 一键插入**。

---

# ✅ 目标效果

按一个快捷键 → 在当前笔记插入：

```md
---
title: "当前文件名"
description: ""
date: "2026-05-07"
tags: []
share: false
---
```

---

# 🚀 实现（无模板文件）

## 1. 安装插件

- Community Plugins → 搜索 **QuickAdd** → 安装启用
    

---

## 2. 新建一个 Choice

进入：

- Settings → QuickAdd → **Add Choice**
    
- 类型选：👉 **Capture**
    

---

## 3. 关键配置（直接复制）

### Capture 内容：

```text
---
title: "{{VALUE:title}}"
description: ""
date: "{{DATE:YYYY-MM-DD}}"
tags: []
share: false
---
```

### 重要修改👇

把 `{{VALUE:title}}` 改成：

```text
{{VALUE:title:tp.file.title}}
```

👉 但更简单、更稳的做法是直接用：

```text
---
title: "{{NAME}}"
description: ""
date: "{{DATE:YYYY-MM-DD}}"
tags: []
share: false
---
```

解释：

- `{{NAME}}` = 当前文件名（就是你要的标题）
    
- `{{DATE:YYYY-MM-DD}}` = 当前日期
    

---

## 4. 插入到当前文件（关键）

在 Capture 里设置：

- ✅ Insert into current file（插入当前文件）
    
- 位置：Top（插入到最上面）
    

---

## 5. 绑定快捷键

- Settings → Hotkeys
    
- 搜索你刚刚的 Choice 名称
    
- 绑定一个键（比如 `Alt + T`）
    

---

# ✅ 使用方式

新建笔记 → 按快捷键

👉 直接插入（无需模板文件、无需选择）：

```md
---
title: "你的文件名"
description: ""
date: "2026-05-07"
tags: []
share: false
---
```

---

# 🧠 为什么不用 Templater

用 Templater 也能做，但：

- 你这个需求**不需要 JS**
    
- 不需要复杂逻辑
    
- QuickAdd 更轻、更直接
    

---

# ⚠️ 一个你可能会忽略的点

如果你是用：

👉 “新建空白文件 → 再插入”

那标题就是文件名 ✔️

但如果你是：  
👉 先写内容再改文件名

那 `{{NAME}}` 不会自动更新（这是 Obsidian 机制，不是插件问题）

---

# 结论

你这个需求本质就是：

> “不用模板文件的动态模板”

👉 **QuickAdd Capture 是最干净的实现，没有之一**

---

如果你下一步想升级成：

- 自动加 tag
    
- 不同类型笔记不同结构
    
- 新建就自动生成（连快捷键都不用）
    

我可以帮你把整个 Obsidian 工作流做成一套“工程化系统”。