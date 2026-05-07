---
title: Enveloppe 命令
description: ""
date: 2026-05-07
tags: []
share: true
---

下面是按**常用程度和场景**整理的 Markdown 表格，方便你直接对照使用：

| 命令                                                            | 核心作用                          | 推荐使用场景                             | 频率        |
| :------------------------------------------------------------ | :---------------------------- | :--------------------------------- | :-------- |
| `Enveloppe: Upload only new notes`                            | 只上传**从未发布过**的新笔记，不更新已发布笔记     | 第一次发布新内容、只想同步新增笔记                  | 低         |
| `Enveloppe: Upload all shared notes`                          | 上传/更新**所有标记为共享**的笔记（含修改过的旧笔记） | 日常写完笔记后，一次性同步所有变更                  | 高（主力命令）   |
| `Enveloppe: Refresh published and upload new notes`           | 同步已发布笔记的修改 + 上传新笔记（组合命令）      | 定期全面同步，比如每周一次批量更新                  | 中         |
| `Enveloppe: Refresh all published notes`                      | 强制刷新所有已发布笔记，同步线上仓库状态          | 线上仓库手动修改后、本地/线上状态不一致时              | 低         |
| `Enveloppe: Purge depublished and deleted files`              | 清理线上仓库中已取消共享/已删除的笔记/图片        | 在 Obsidian 删除笔记后，清理线上冗余文件          | 中         |
| `Enveloppe: Test the connection to the configured repository` | 测试与 GitHub 仓库的连接是否正常          | 同步失败、换 Token/改仓库地址后排查问题            | 按需（排错用）   |
| `Enveloppe: Check the rate limit of the GitHub API`           | 查看 GitHub API 请求次数限制与剩余额度     | 频繁同步后出现「API rate limit exceeded」报错 | 按需（排错用）   |
| `Enveloppe: (Other repositories): Reload opened frontmatter`  | 为多仓库配置，重新加载当前笔记的 Frontmatter  | 使用多个仓库且切换后 Frontmatter 不生效         | 极低（多仓库用户） |
| `Enveloppe: (Others repositories): Reload all saved sets`     | 为所有配置的仓库，重新加载已保存的同步配置         | 修改多仓库配置后、同步异常重置状态                  | 极低（多仓库用户） |

---

💡 给你一个最省事的「日常使用流程」：
1. 写完笔记 → 执行 `Upload all shared notes`
2. 删除笔记/取消共享 → 执行 `Purge depublished and deleted files`
3. 同步失败/报错 → 先 `Test the connection`，再 `Check the rate limit`

 