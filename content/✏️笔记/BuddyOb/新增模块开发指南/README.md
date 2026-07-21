# 新增管理模块开发指南

> 面向本项目（Spring Boot 3 + Vue3 + MyBatis-Plus + Element Plus）的**新增一个管理界面/模块**的完整手册。
> 无论是"用户管理""信息管理""商品管理"还是任何其它 CRUD 模块，照本指南即可从零加完，前后端贯通。

---

## 这份指南解决什么问题

当你想新增一个后台管理页面（比如"设备管理""订单管理"），你会遇到一连串问题：

- 后端要建哪些类？放在哪个包？
- 接口写在哪？路径怎么定？权限怎么加？
- 前端要建哪些文件？菜单怎么出现？按钮权限怎么控制？
- 数据库要建什么表？菜单和权限要插哪些数据？
- 有哪些坑一不小心就踩？

本指南把这些**全部拆开、按层讲清、给出可照抄的模板**。

---

## 文档结构（建议按顺序阅读）

| 文档 | 内容 | 什么时候看 |
| --- | --- | --- |
| [01-架构与数据流总览.md](./01-架构与数据流总览.md) | 整体分层、一次请求从前端到数据库怎么流转、权限/菜单怎么串起来 | **先看这个**，建立全局认知 |
| [02-后端开发指南.md](./02-后端开发指南.md) | Entity / Mapper / Service / Controller "四件套"怎么写，逐层模板 | 写后端时看 |
| [03-前端开发指南.md](./03-前端开发指南.md) | `views` 页面 + `api` 封装怎么写，Element Plus 标准用法 | 写前端时看 |
| [04-数据库与菜单权限配置.md](./04-数据库与菜单权限配置.md) | 建表 SQL、菜单/按钮/角色授权的 INSERT，权限字符串规则 | 配数据库和权限时看 |
| [05-完整实战示例-商品管理.md](./05-完整实战示例-商品管理.md) | 从零加一个"商品管理"模块的**全套可复制代码** | 想直接抄一遍时看 |
| [06-注意事项与常见坑.md](./06-注意事项与常见坑.md) | 本项目特有约定、易错点、排错清单 | 遇到问题或想避坑时看 |

---

## 一分钟速览：新增一个模块要动哪些文件

### 后端（4 个新文件 + 2 个 SQL 改动）

```
backend/src/main/java/com/example/admin/
├── domain/Xxx.java              ← 新增：实体类
├── mapper/XxxMapper.java        ← 新增：Mapper 接口（空接口继承 BaseMapper）
├── service/XxxService.java      ← 新增：Service（普通 @Service 类，无接口无 Impl）
└── controller/.../XxxController.java  ← 新增：REST 控制器

backend/src/main/resources/
├── schema-mysql.sql             ← 改：加 CREATE TABLE
└── data.sql                     ← 改：加 菜单/按钮权限/角色授权 INSERT
```

### 前端（只需 2 个新文件）

```
frontend/src/
├── views/<模块>/<实体>/index.vue  ← 新增：页面组件
└── api/<模块>/<实体>.js           ← 新增：接口封装
```

> **前端的 `router`、`store`、`main.js` 都不用改**——菜单和路由是后端菜单表动态下发的。这是本项目的一大特点，务必理解（详见 01 文档）。

### 核心口诀

> **"后端四件套 + 两条 SQL，前端两文件，权限串前后端统一为 `业务:模块:动作`。"**

---

## 关键约定速记（本项目特有，容易踩）

1. **Service 没有接口 + Impl 之分**，就是一个直接调 Mapper 的普通 `@Service` 类。
2. **没有 XML Mapper**，纯 MyBatis-Plus，CRUD 由 `BaseMapper` 自动生成 SQL。
3. **权限字符串**（如 `business:notice:add`）后端 `@PreAuthorize("@ps.hasPermi('...')")` 和前端 `v-hasPermi="'...'"` **必须一字不差地一致**。
4. **前端路由是动态的**，来自后端 `sys_menu` 表 + `/getRouters` 接口，前端不写死路由。
5. **删除接口用数组参数** `?ids=1&ids=2`，后端 `@RequestParam List<Long> ids`，前端靠 `request.js` 的 `paramsSerializer` 序列化。

---

准备好了就从 [01-架构与数据流总览.md](./01-架构与数据流总览.md) 开始吧。
