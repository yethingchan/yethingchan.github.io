---
title: 技术细节与难点 · 02 数据库与 RBAC
---

# 02 数据库与 RBAC · 技术细节与难点

> 配套原教程：[[../02-数据库与RBAC/00-索引]]（RBAC 五表 + 字典/参数/公告 + 日志/任务/监控表 SQL）
> 本篇讲**表怎么设计才不反人类、权限串怎么粒度才合适、种子数据为什么顺序会坑人**。

## 一、技术细节

### 2.1 RBAC 五张核心表的关系
```
sys_user ──< sys_user_role >── sys_role ──< sys_role_menu >── sys_menu
   (人)           (多对多)        (角色)          (多对多)        (菜单/权限)
```
- `sys_menu` 是枢纽：一行既可能是"目录/菜单"（有 `component` 路由），也可能是"按钮"（有 `perms` 权限串，如 `system:user:add`）。
- `perms` 用 **`模块:业务:动作` 三级**（如 `system:user:export`），比若依的 `system:user:add` 同级写法多了一层"业务"语义，方便前端 `v-hasPermi` 做"整块显隐"。
- `parent_id` 自关联实现**菜单树**（目录→菜单→按钮三级）。

### 2.2 辅助表
- 字典：`sys_dict_type`（类型）+ `sys_dict_data`（数据），`dict_data.dict_type` 外键关联类型。下拉框统一 `WHERE dict_type='xxx'` 拉。
- 参数：`sys_config(config_key, config_value)` 存系统参数（分页大小、上传路径、限流阈值）。
- 公告：`sys_notice`（富文本 `content`、状态、已读）。
- 日志/任务：`sys_oper_log` / `sys_login_log` / `sys_job` / `sys_job_log`。

### 2.3 几个关键设计决策
- **逻辑删除 `del_flag`**：所有业务表加 `del_flag=0/1`，不物理 `DELETE`，保留审计痕迹（呼应 [[../10-安全防护/04-文件上传拦截与接口限流]] 里"核心操作不可物理删"）。
- **自增主键 vs 雪花**：本项目用自增（简单、可读）；分布式部署才需要雪花 ID，那时前端 JS 的 `Number` 会被截断 → 后端用 Jackson `ToStringSerializer` 把 `Long` 转 `String`（见 [[../03-后端基础框架/02-MyBatisPlus与Redis配置]]）。
- **索引**：`username`（唯一）、`role_id`、`menu_id`、`dict_type` 建索引；但别给每个 `varchar` 都加，写入会变慢。

## 二、技术难点

1. **菜单树递归性能**：深层菜单若"查一个节点→查它孩子→再查孩子的孩子"会 N+1 查询爆炸。解法：**一次 `SELECT * FROM sys_menu` 全查出来，内存里按 `parent_id` 建树（O(n) 一遍）**，前端直接拿树渲染。本项目就是这个思路。
2. **多对多赋权残留**：角色改权限时，若只 `INSERT` 新关联不 `DELETE` 旧的 → 旧权限删不掉、越权残留。必须**事务内"先删后插"**（`DELETE FROM sys_role_menu WHERE role_id=?` 再批量 `INSERT`）。
3. **权限粒度失衡**：太粗（只有一个 `system:user`）控不住按钮；太细（几千个 `system:user:add:btn1:xxx`）管理和维护会爆炸。**三级 `模块:业务:动作` 是平衡点**——按钮级够用，又不至于失控。
4. **种子数据顺序坑**：`sys_menu` 用自增 `id`，`sys_role_menu` 要 `INSERT` `role_id, menu_id`。若先插 `role_menu` 再插 `menu` → 外键/关联 id 对不上。本项目 seed 用**固定 id**（如菜单 id 从 1 编起），角色关联直接写死 `VALUES (1,1),(1,2)...`，绕开"插完才知道 id"的问题。
5. **⚠️ 种子只覆盖"用户管理"模块**（已在本项目 SQL 文件补提示）：原 init 仅插入 `system:user:*` 的 `perms`。角色、菜单、字典、监控、业务(`wms:*`) 等模块的 `@PreAuthorize` 权限串**没有对应 `sys_menu.perms` 行**，全新库直接跑会因这些接口 403 而打不开页面。补法：登录后到 [[../04-权限管理模块/03-菜单树管理]] 用"自动生成菜单 SQL"一键种入，或手动补 `sys_menu.perms`。
6. **数据权限字段 `dept_id`**：部门树同样 `parent_id` 自关联；数据权限要"当前用户部门 + 其所有子部门"，需用递归或闭包表把子部门 id 收集齐，再 `IN (...)`。

## 三、本项目的解法

| 难点 | 本项目做法 |
|------|--------------|
| 菜单树 | 一次全查 + 内存递归建树，O(n) 无 N+1 |
| 多对多赋权 | 事务内先 `DELETE` 后 `INSERT`，无残留 |
| 权限粒度 | 统一 `模块:业务:动作` 三级，前后端同源 |
| 种子顺序 | 菜单固定 id，角色关联写死，避免回填 |
| 种子缺口 | SQL 文件已加提示：用菜单自动生成或手动补 `perms` |
| 逻辑删除 | 全表 `del_flag`，MP `@TableLogic` 自动带条件 |
| 数据权限 | `@DataScope` + 切面拼 `dept_id IN(...)`，列名走注解白名单 |

> 一句话：**RBAC 的设计难点不在"建表"，而在"树怎么查、权怎么赋、种子怎么种"；本项目用"全查内存建树 + 先删后插 + 固定 id 种子"三招化解。**

## 四、关联
- 权限怎么落到按钮/接口：[[技术细节与难点/04-权限管理]]
- 为什么 `@PreAuthorize` 会和菜单 `perms` 强绑定：[[技术细节与难点/03-后端基础框架]]
- 原教程详写：[[../02-数据库与RBAC/01-RBAC核心表SQL]]、[[../02-数据库与RBAC/02-辅助表(字典参数公告)]]、[[../02-数据库与RBAC/03-日志任务监控表]]
