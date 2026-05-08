---
title: Quartz使用说明
description: ""
date: 2026-05-07
tags: []
share: true
---


C U 发布
C M插入模板

### 官方支持的 Callout 类型

# Quartz / Obsidian 全量 Callout 类型表格
| Callout 标识（支持别名） | 中文名称 | 样式&用途 | 直接复制代码 |
| :--- | :--- | :--- | :--- |
| `note` | 普通笔记 | 蓝色基础提示框，通用备注 | `> [!note] 标题` |
| `abstract` / `summary` / `tldr` | 摘要/总结 | 浅蓝色，用于内容概括 | `> [!abstract] 摘要` |
| `info` / `todo` | 信息/待办 | 蓝色，用于信息说明、任务 | `> [!info] 信息` |
| `tip` / `hint` / `important` | 技巧/重点 | 浅绿色，用于技巧、关键点 | `> [!tip] 小技巧` |
| `success` / `check` / `done` | 成功/完成 | 绿色，用于完成、正确、结果 | `> [!success] 完成` |
| `question` / `help` / `faq` | 问题/帮助 | 浅橙色，用于疑问、FAQ | `> [!question] 问题` |
| `warning` / `caution` / `attention` | 警告/注意 | 黄色，用于提醒、注意事项 | `> [!warning] 警告` |
| `failure` / `fail` / `missing` | 失败/缺失 | 红色，用于错误、缺失内容 | `> [!failure] 失败` |
| `danger` / `error` | 危险/错误 | 深红色，用于严重错误、风险 | `> [!danger] 危险` |
| `bug` | 程序漏洞 | 紫色，用于BUG、代码问题 | `> [!bug] 漏洞` |
| `example` | 示例 | 紫色，用于代码/功能示例 | `> [!example] 示例` |
| `quote` / `cite` | 引用/引证 | 灰色，用于引用文字、名言 | `> [!quote] 引用` |

---

# 通用基础语法（所有类型都适用）
```markdown
> [!类型] 自定义标题
> 这里是内容
> 可以换行写
```

## 进阶用法（Quartz 都支持）
1. **可折叠 Callout**（加 `+` 展开，`-` 默认折叠）
```markdown
> [!note]+ 点击展开
> 隐藏内容
```

2. **无标题 Callout**
```markdown
> [!note]
> 直接写内容，不显示标题栏
```

3. **自定义任意标题**
```markdown
> [!success] ✅ 部署完成
> 你的网站已成功上线！
```





