# SpringBoot + Vue3 后台管理系统（RuoYi 风格 RBAC）

一套**开箱即跑**、可直接用于学习/二开的 Spring Boot 3 + Vue3 前后端分离后台。
默认零依赖运行（内存数据库 H2 + 本地缓存），不需要安装 MySQL / Redis 即可启动并自测全部接口。

---

## 一、技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Spring Boot 3.2.12 / Java 17 / Jakarta |
| 安全 | Spring Security 6（JWT 无状态，`@PreAuthorize` 方法级鉴权） |
| 持久层 | MyBatis-Plus 3.5.7 |
| 存储（默认） | H2 内存库（`MODE=MySQL`）+ 本地 `ConcurrentHashMap` 缓存 |
| 存储（可选） | MySQL + Redis（激活 `mysql` profile） |
| 前端 | Vue3 + Vite5 + Element Plus + Pinia + vue-router4 |
| 构建 | Maven（后端）/ npm + Vite（前端） |

---

## 二、默认运行（零依赖，推荐先跑这个）

### 1. 后端（端口 8080）

```bash
cd backend
# 若系统 PATH 里没有 mvn，用项目自带的 wrapper：
#   D:/APP/mvn3/mvn spring-boot:run
mvn spring-boot:run
```

- 已默认内置 `--server.port=8080`（写在 `backend/pom.xml` 的 `spring-boot.run.arguments` 里），
  即使某些 IDE 注入了 `SERVER__PORT` 环境变量也能强制到 8080。
- 首次启动会自动执行 `schema.sql` + `data.sql` 建表并写入：
  - 管理员用户 **`admin / 123456`**（`sys_user` + BCrypt 密码运行时生成）
  - 操作员用户 **`operator / 123456`**
  - 角色、菜单、权限、字典、公告等基础数据。
- H2 控制台：http://localhost:8080/h2-console （JDBC URL：`jdbc:h2:mem:admin`）

> 如果你是用 IDE 直接 `Run AdminApplication.main()` 启动（不是 `mvn spring-boot:run`），
> 请在 Run/Debug 配置的 **Program arguments** 加 `--server.port=8080`，
> 否则可能被环境变量 `SERVER__PORT` 强制到一个随机端口。

### 2. 前端（端口 3000）

```bash
cd frontend
npm install      # 依赖已安装过，可跳过
npm run dev       # 开发服务器 http://localhost:3000
```

- Vite 开发代理：`/api` → `http://localhost:8080`（`vite.config.js`，并自动去掉 `/api` 前缀）。
  所以前端实际请求 `/api/login` 会被转发成后端 `/login`，**前后端路径天然对齐**。
- 登录页默认填好 `admin / 123456`，直接点登录即可。
- 生产构建：`npm run build`（产物在 `frontend/dist/`，**已验证可成功打包**）。

---

## 三、切换到 MySQL + Redis（正式环境）

1. 修改 `backend/src/main/resources/application-mysql.yml`：填好 MySQL 地址/库名/账号密码，以及 Redis 地址。
2. 初始化库：先执行 `sql/schema-mysql.sql` 建表，再执行 `backend/src/main/resources/data.sql` 写入基础数据
   （H2 用的 `schema.sql` 字段类型与 MySQL 略有差异，切库请用 `schema-mysql.sql`）。
3. 启动后端时激活 `mysql` profile：

```bash
mvn -Dspring.profiles.active=mysql spring-boot:run
```

此时会自动启用 `RedisCache`（替代本地缓存），并连接真实 MySQL。

---

## 四、功能与接口自检清单（已逐一验证通过 ✅）

| 模块 | 接口 | 结果 |
| --- | --- | --- |
| 认证 | `POST /login`、`GET /getInfo`、`GET /getRouters`、`POST /logout` | ✅ |
| 用户 | `GET/POST/PUT/DELETE /system/user`（删除走 `?ids=1&ids=2`） | ✅ |
| 角色 | `GET/POST/PUT/DELETE /system/role/{id}` + `POST /system/role/menu` | ✅ |
| 菜单 | `GET/POST/PUT/DELETE /system/menu/{id}` + `GET /system/menu/tree` | ✅ |
| 字典 | `GET/POST/PUT/DELETE /system/dict/type`、`/system/dict/data` | ✅ |
| 参数 | `GET/POST/PUT/DELETE /system/config` | ✅ |
| 日志 | `GET/DELETE /monitor/operlog` | ✅ |
| 公告 | `GET/POST/PUT/DELETE /business/notice` | ✅ |
| 鉴权 | 无 token 访问受保护接口 → **HTTP 401**；无权限角色访问 → **HTTP 403** | ✅ |
| 中文 | 数据库中文（角色名/菜单名…）正常无乱码 | ✅ |

> 权限注解示例：`@PreAuthorize("@ps.hasPermi('system:user:list')")`。
> 前端用 `v-hasPermi="'system:user:add'"` 指令按权限隐藏按钮。

---

## 五、目录结构

```
springboot-vue3-app/
├── backend/                 # Spring Boot 3 后端
│   ├── pom.xml
│   └── src/main/java/com/example/admin/
│       ├── AdminApplication.java
│       ├── config/          # SecurityConfig / JwtAuthFilter / GlobalExceptionHandler
│       ├── security/        # JwtUtils / LoginUser / 401&403 处理器
│       ├── controller/      # Auth + system/monitor/business 各模块 Controller
│       ├── service/        # 业务层
│       ├── mapper/         # MyBatis-Plus Mapper
│       ├── domain/         # 实体 + RouterVO 等
│       ├── common/         # AjaxResult / 注解 / 工具 / DTO
│       └── config/DataInitRunner.java  # 启动时播种 admin/operator
│   └── src/main/resources/
│       ├── application.yml / application-mysql.yml
│       ├── schema.sql / schema-mysql.sql / data.sql
└── frontend/                # Vue3 + Vite 前端
    ├── src/api/            # 各模块请求封装
    ├── src/views/         # 页面（login/layout/system/monitor/business…）
    ├── src/store/         # Pinia（user 含 menus 动态路由）
    ├── src/router/        # 静态路由 + 动态路由（/getRouters）
    ├── src/utils/         # request.js（Bearer 头/401/403 处理）、route.js
    └── vite.config.js     # /api 代理到 8080
```

---

## 六、学习时容易踩的坑（本项目已修好，方便对照）

1. **Spring Security 6 包名搬迁**：`AuthenticationManager` 在 `org.springframework.security.authentication`，
   `AuthenticationConfiguration` 在 `org.springframework.security.config.annotation.authentication.configuration`，
   老教程的 `...core.AuthenticationManager` / `...web.configuration.AuthenticationConfiguration` 已失效。
2. **Spring Security 6 不自动暴露 `AuthenticationManager` Bean**：需手动
   `new AuthenticationConfiguration().getAuthenticationManager()` 暴露。
3. **jjwt 0.12 API 变更**：`Jwts.builder().signWith(key, Jwts.SIG.HS256)`（不再是 `JwTSAlgorithm.HS256`）。
4. **MyBatis `@Select` 里的 `<` `&` 必须转义**：写成 `&lt;&gt;`，否则 XML 文档解析报“格式正确的字符数据”错误。
5. **Lombok 需在 maven-compiler-plugin 的 `annotationProcessorPaths` 显式声明**，否则 `@Data` 的 getter 不生成，
   编译期只报一堆“找不到符号 getXxx()”。
6. **`spring.sql.init` 默认用平台编码读 SQL**：Windows 上中文会乱码，需显式配 `spring.sql.init.encoding: UTF-8`。
7. **默认 `LogoutFilter` 会抢 `/logout`**：JWT 无状态场景下应在 `SecurityFilterChain` 里 `.logout(disable)`，
   否则会 302 跳 `/login?logout` 引发连锁 500。
8. **`@PreAuthorize` 抛出的 `AccessDeniedException` 在 DispatcherServlet 内（AOP）**，
   由 `@RestControllerAdvice` 兜，需 `@ResponseStatus(FORBIDDEN)` 才会返回 HTTP 403 而非 200。
9. **前端路由 `Layout` 组件路径要和实际文件一致**：本项目放在 `src/views/layout/index.vue`，
   因此 `router/index.js` 与 `utils/route.js` 都引用 `@/views/layout/index.vue`。
10. **前后端删除接口约定要一致**：本项目约定
    `user/config/notice/operlog/dict` 用 `?ids=1&ids=2`（数组参数，前端 `paramsSerializer` 序列化为重复 key）；
    `role/menu` 用路径变量 `/{id}`。前端 `request.js` 已统一处理数组参数。
