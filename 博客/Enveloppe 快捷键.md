---
title: Enveloppe 快捷键
description: ""
date: 2026-05-07
tags: []
share: true
---


---

## 一、核心上传/同步功能（你最常用的）
| 快捷键/命令                                             | 作用                                       | 日常用法场景                                            |
| :------------------------------------------------- | :--------------------------------------- | :------------------------------------------------ |
| **`Enveloppe: Upload all shared notes`**           | 上传**所有标记了 `share: true`** 的笔记（包括新增、修改过的） | 最常用的一键同步，写完博客后按 `Ctrl + U` 直接推送到 GitHub，自动处理所有变更。 |
| **`Enveloppe: Upload only new notes`**             | 只上传**从未推送过的新笔记**，不更新已修改的旧笔记              | 适合批量写了很多新博客，只想一次性发布新内容，不想覆盖旧修改。                   |
| **`Enveloppe: Upload single current active note`** | 上传**当前打开的这一篇笔记**                         | 改完单篇博客后，只想单独发布这一篇，不想同步所有文件，用这个最快。                 |

---

## 二、刷新/重新加载类功能
| 命令 | 作用 | 用法场景 |
| :--- | :--- | :--- |
| **`Enveloppe: Refresh published and upload new notes`** | 先刷新已发布笔记的状态，再上传新笔记 | 适合怀疑本地状态和 GitHub 不一致时，先同步状态再上传，避免冲突。 |
| **`Enveloppe: Refresh all published notes`** | 只刷新所有已发布笔记的状态，不上传新内容 | 比如你在 GitHub 手动改了文件，用这个把本地状态同步回来，保持两边一致。 |
| **`Enveloppe: (Other repositories): Reload opened frontmatter`** | 刷新当前笔记的 `frontmatter`（比如 `share: true` 状态） | 改了笔记的 `share` 字段后，用这个让 Enveloppe 立刻识别到，不用重启插件。 |
| **`Enveloppe: (Others repositories): Reload all saved sets`** | 重新加载所有仓库的配置和笔记集合 | 多仓库配置出问题时，用这个重置插件状态。 |

---

## 三、测试/诊断工具
| 命令 | 作用 | 用法场景 |
| :--- | :--- | :--- |
| **`Enveloppe: Test the connection to the configured repository`** | 测试和你的 GitHub 仓库的连接是否正常 | 上传失败时，先跑这个命令，看是不是 Token、仓库地址配置错了。 |
| **`Enveloppe: Check the rate limit of the GitHub API`** | 查看你的 GitHub API 请求剩余次数 | 上传频繁失败时，用这个检查是不是触发了 GitHub API 限流。 |

---

## 四、给你的使用建议
1.  **日常最常用**：直接用你已经绑定的 `Ctrl + U`（`Upload all shared notes`），写完博客一键上传，简单高效。
2.  **单篇更新用**：`Upload single current active note`，比如只改了一篇博客，用这个比全量上传快很多。
3.  **排查问题用**：先跑 `Test the connection`，再看 `Check the rate limit`，基本能定位上传失败的原因。
