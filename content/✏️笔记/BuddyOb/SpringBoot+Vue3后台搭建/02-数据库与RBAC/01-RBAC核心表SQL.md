---
title: RBAC 核心表 SQL
---

# 02-1 RBAC 核心表（完整 DDL）

> 上接：[[SpringBoot+Vue3后台搭建/02-数据库与RBAC/00-索引]]
> 直接复制到 MySQL 8 执行。每张表都带注释说明字段含义与 RBAC 关系。

```sql
-- 建库
CREATE DATABASE IF NOT EXISTS `admin` DEFAULT CHARSET utf8mb4;
USE `admin`;

/* =========================================================
   1) 部门表（树形：公司→分公司→车间→班组）
   parent_id = 0 表示顶级
   ========================================================= */
CREATE TABLE `sys_dept` (
  `dept_id`    BIGINT NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `parent_id`   BIGINT DEFAULT 0 COMMENT '父部门ID（0=顶级）',
  `dept_name`   VARCHAR(30) DEFAULT '' COMMENT '部门名称',
  `order_num`   INT DEFAULT 0 COMMENT '显示顺序',
  `leader`      VARCHAR(20) DEFAULT NULL COMMENT '负责人',
  `phone`       VARCHAR(11) DEFAULT NULL COMMENT '联系电话',
  `status`      CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_time`  DATETIME DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`dept_id`)
) ENGINE=InnoDB COMMENT='部门表';

/* =========================================================
   2) 岗位表（用户绑定岗位，用于筛选/审批）
   ========================================================= */
CREATE TABLE `sys_post` (
  `post_id`    BIGINT NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `post_code`  VARCHAR(64) DEFAULT '' COMMENT '岗位编码',
  `post_name`  VARCHAR(50) DEFAULT '' COMMENT '岗位名称',
  `post_sort`  INT DEFAULT 0 COMMENT '显示顺序',
  `status`      CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`post_id`)
) ENGINE=InnoDB COMMENT='岗位表';

/* =========================================================
   3) 用户表（核心）
   dept_id → 部门（决定数据权限范围）
   password 用 BCrypt 加密存储（见 [[../10-安全防护/01-密码加密与防暴破IP名单]]）
   ========================================================= */
CREATE TABLE `sys_user` (
  `user_id`    BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `dept_id`    BIGINT DEFAULT NULL COMMENT '部门ID',
  `user_name`  VARCHAR(30) NOT NULL COMMENT '登录账号',
  `nick_name`  VARCHAR(30) DEFAULT '' COMMENT '昵称',
  `user_type`  VARCHAR(2) DEFAULT '00' COMMENT '用户类型（00系统）',
  `email`       VARCHAR(50) DEFAULT '' COMMENT '邮箱',
  `phonenumber` VARCHAR(11) DEFAULT '' COMMENT '手机号',
  `sex`         CHAR(1) DEFAULT '0' COMMENT '性别（0男 1女 2未知）',
  `avatar`      VARCHAR(100) DEFAULT '' COMMENT '头像路径',
  `password`    VARCHAR(100) DEFAULT '' COMMENT '密码（BCrypt哈希）',
  `status`      CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag`    CHAR(1) DEFAULT '0' COMMENT '删除标志（0未删 2已删，逻辑删）',
  `login_ip`    VARCHAR(50) DEFAULT '' COMMENT '最后登录IP',
  `login_date`  DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uq_user_name` (`user_name`)   -- 账号唯一
) ENGINE=InnoDB COMMENT='用户信息表';

/* =========================================================
   4) 角色表（权限载体）
   data_scope：数据权限范围
     1=全部 2=自定义(按dept) 3=本部门 4=本部门及子部门 5=仅本人
   ========================================================= */
CREATE TABLE `sys_role` (
  `role_id`     BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name`   VARCHAR(30) DEFAULT '' COMMENT '角色名称',
  `role_key`    VARCHAR(100) DEFAULT '' COMMENT '角色权限字符串（如 admin/warehouse）',
  `role_sort`    INT DEFAULT 0 COMMENT '显示顺序',
  `data_scope`  CHAR(1) DEFAULT '1' COMMENT '数据范围（1全部..5仅本人）',
  `status`       CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag`     CHAR(1) DEFAULT '0',
  `create_time`   DATETIME DEFAULT NULL,
  `remark`       VARCHAR(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uq_role_key` (`role_key`)
) ENGINE=InnoDB COMMENT='角色信息表';

/* =========================================================
   5) 菜单表（菜单+页面+按钮 三级合一）
   menu_type: M=目录 C=菜单(页面) F=按钮(权限点)
   perms: 权限标识，如 'system:user:add'（后端 @PreAuthorize 用）
   component: 前端 Vue 组件路径，如 'system/user/index'
   ========================================================= */
CREATE TABLE `sys_menu` (
  `menu_id`     BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name`   VARCHAR(50) NOT NULL COMMENT '菜单名称',
  `parent_id`   BIGINT DEFAULT 0 COMMENT '父菜单ID（0=顶级）',
  `order_num`    INT DEFAULT 0 COMMENT '显示顺序',
  `path`         VARCHAR(200) DEFAULT '' COMMENT '路由地址',
  `component`    VARCHAR(255) DEFAULT '' COMMENT '组件路径',
  `query`        VARCHAR(255) DEFAULT NULL COMMENT '路由参数',
  `menu_type`    CHAR(1) DEFAULT '' COMMENT '类型（M目录 C菜单 F按钮）',
  `is_frame`     INT DEFAULT 1 COMMENT '是否外链（1是 0否）',
  `is_cache`     INT DEFAULT 0 COMMENT '是否缓存（1缓存 0不缓存）',
  `visible`      CHAR(1) DEFAULT '0' COMMENT '是否显示（0显示 1隐藏）',
  `status`       CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `perms`        VARCHAR(100) DEFAULT '' COMMENT '权限标识（如 system:user:list）',
  `icon`         VARCHAR(100) DEFAULT '#' COMMENT '菜单图标',
  `create_time`   DATETIME DEFAULT NULL,
  PRIMARY KEY (`menu_id`)
) ENGINE=InnoDB COMMENT='菜单权限表';

/* =========================================================
   6) 用户↔角色（多对多）
   ========================================================= */
CREATE TABLE `sys_user_role` (
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB COMMENT='用户和角色关联表';

/* =========================================================
   7) 角色↔菜单（多对多，核心授权）
   ========================================================= */
CREATE TABLE `sys_role_menu` (
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB COMMENT='角色和菜单关联表';

/* =========================================================
   8) 用户↔岗位（多对多）
   ========================================================= */
CREATE TABLE `sys_user_post` (
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `post_id` BIGINT NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`user_id`,`post_id`)
) ENGINE=InnoDB COMMENT='用户与岗位关联表';
```

## 初始化数据（超级管理员）

```sql
-- 部门：公司→仓库部
INSERT INTO sys_dept(dept_id,parent_id,dept_name,order_num,status)
VALUES (100,0,'若依科技',1,'0'),(101,100,'仓库部',1,'0');

-- 岗位
INSERT INTO sys_post(post_id,post_code,post_name,post_sort,status)
VALUES (1,'ceo','董事长',1,'0'),(2,'wms','仓库员',2,'0');

-- 角色
INSERT INTO sys_role(role_id,role_name,role_key,role_sort,data_scope,status)
VALUES (1,'超级管理员','admin',1,'1','0'),
       (2,'仓库管理员','warehouse',2,'3','0');   -- data_scope=3 仅本部门

-- 管理员用户（密码 123456 的 BCrypt，见 [[../10-安全防护/01-密码加密与防暴破IP名单]]）
-- 下面这段用 SQL 写死哈希不现实，建议首次启动用代码初始化，或临时用：
INSERT INTO sys_user(user_id,dept_id,user_name,nick_name,password,status)
VALUES (1,100,'admin','管理员',
  '$2a$10$7JB720yubVSZaM9Eon/4uzDb.1S5VH9ZX6Xz/3KMfeSA9U7KeoIu','0');

-- 授权：admin 用户挂 admin 角色
INSERT INTO sys_user_role(user_id,role_id) VALUES (1,1);

-- 菜单（示例：系统管理目录 + 用户管理菜单 + 新增按钮）
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon)
VALUES
 (1,'系统管理',0,1,'system','','M','',  'system'),
 (2,'用户管理',1,1,'user','system/user/index','C','system:user:list','user'),
 (3,'用户新增',2,1,'','',  'F','system:user:add',''),
 (4,'用户编辑',2,2,'','',  'F','system:user:edit',''),
 (5,'用户删除',2,3,'','',  'F','system:user:remove','');

-- admin 角色拥有上述所有菜单权限
INSERT INTO sys_role_menu(role_id,menu_id) VALUES (1,1),(1,2),(1,3),(1,4),(1,5);
```

## 字段设计要点（讲解）
- **逻辑删除 `del_flag`**：不用 `DELETE`，用标志位，保留审计痕迹（[[../10-安全防护/04-文件上传拦截与接口限流]] 提到核心操作不可物理删）。
- **`perms` 权限串**：格式 `模块:业务:动作`。后端用 `@PreAuthorize("hasPermi('system:user:add')")` 校验（见 [[../04-权限管理模块/02-角色与菜单权限分配]]）。
- **`data_scope`**：数据权限的"元数据"，真正生效靠注解+切面拼 SQL（详见 [[../04-权限管理模块/05-数据权限实现]]）。
- **树形靠 `parent_id`**：菜单、部门都一样，前端用递归组件渲染（[[../08-前端通用封装/03-页面缓存主题与树形导入导出]]）。

## 验证清单
- [ ] 8 张表建成功，`sys_user`/`sys_role`/`sys_menu` 有 init 数据。
- [ ] `SELECT * FROM sys_role_menu WHERE role_id=1` 能看到 5 条菜单授权。

> ⚠️ **权限种子只覆盖"用户管理"模块**：上面的 init 仅插入 `system:user:list/add/edit/remove/export` 等用户菜单的 `perms`。
> 角色、菜单、部门、字典、参数、监控(`monitor:*`)、业务(`wms:*`) 等模块在 `@PreAuthorize("hasPermi('...')")` 里用到的权限串，**当前种子没有对应 `sys_menu.perms` 行**。
> 全新库直接跑会因为那些接口 403 而打不开对应页面。两种补法（任选）：
> 1. 登录后到「[[../04-权限管理模块/03-菜单树管理]]」用"自动生成菜单 SQL"把各模块菜单 + 权限一次性种进去（推荐，最省事）；
> 2. 或手动给 `sys_menu` 补 `perms` 行，例如：
> ```sql
> INSERT INTO sys_menu(menu_name,parent_id,order_num,path,component,perms,menu_type,visible,status)
> VALUES ('操作日志',0,1,'monitor/operlog','monitor/operlog/index','monitor:operlog:list',1,0,0);
> INSERT INTO sys_menu(menu_name,parent_id,order_num,path,component,perms,menu_type,visible,status)
> VALUES ('字典管理',0,1,'system/dict','system/dict/index','system:dict:list',1,0,0);
> ```
> 权限串格式统一 `模块:业务:动作`，**前后端必须一致**：前端用 `v-hasPermi="'monitor:operlog:list'"` 与后端 `@PreAuthorize("hasPermi('monitor:operlog:list')")` 同源。

> 下一步：[[../02-数据库与RBAC/02-辅助表(字典参数公告)]] 建字典/参数/公告表。
