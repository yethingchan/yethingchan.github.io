## WebStorm 开发技巧

> 本文是 vue-pure-admin 深度教程的第五篇，介绍如何使用 JetBrains WebStorm 高效开发 Vue 3 + TypeScript 前端项目。
> 适用于 vue-pure-admin 项目以及所有 Vue 3 项目。

---

### 一、为什么选择 WebStorm 而不是 VS Code

VS Code 是前端开发的主流编辑器，但 WebStorm 在以下方面有显著优势：

| 维度 | VS Code | WebStorm |
|------|---------|----------|
| **TypeScript 分析** | 依赖 tsserver | 内置深度分析引擎，重构更安全 |
| **Vue SFC 支持** | 需要 Volar 插件 | 原生支持，无需额外配置 |
| **重构能力** | 基础重命名 | 60+ 种重构操作（提取组件、提取方法等） |
| **调试** | 需要 Chrome 扩展 | 内置浏览器调试，支持断点调试 .vue 文件 |
| **测试** | 需要配置 | 原生集成 Vitest / Jest / Cypress |
| **Git 集成** | 基础 | 三向合并、Local History、交互式 rebase |
| **开箱即用** | 需要大量插件 | 内置 ESLint、Prettier、Tailwind CSS 支持 |

对于 vue-pure-admin 这种大量使用 TypeScript + Composition API + `<script setup>` 的项目，WebStorm 的深度集成可以显著提升开发效率。

---

### 二、项目初始化与配置

#### 2.1 打开 vue-pure-admin 项目

```bash
# 1. 克隆项目
git clone https://github.com/pure-admin/vue-pure-admin.git

# 2. 安装依赖
cd vue-pure-admin
pnpm install

# 3. 用 WebStorm 打开项目目录
webstorm .
```

WebStorm 会自动识别项目类型（Vite + Vue 3 + TypeScript），并配置相应的运行器、Linter 和格式化器。

#### 2.2 推荐设置

**编辑器设置**：

| 设置项 | 路径 | 推荐值 |
|--------|------|--------|
| 自动导入 | Settings → Editor → General → Auto Import | 勾选 "Add unambiguous imports on the fly" |
| Tab 缩进 | Settings → Editor → Code Style → TypeScript | Tab size: 2 |
| 保存时格式化 | Settings → Tools → Actions on Save | 勾选 "Run Prettier" |
| 文件编码 | Settings → Editor → File Encodings | 全部设为 UTF-8 |

**Vue 特定设置**：

| 设置项 | 路径 | 推荐值 |
|--------|------|--------|
| Vue 模板格式 | Settings → Editor → Code Style → Vue | 勾选 "Format embedded CSS/JS" |
| 组件名称格式 | Settings → Editor → Inspections → Vue | 启用 "Component name casing" 检查 |

#### 2.3 运行配置

打开 Run → Edit Configurations → 点击 + → 选择 "npm"：

| 配置项 | 值 |
|--------|------|
| Name | dev |
| Command | run |
| Scripts | dev |
| Node interpreter | 选择 Node.js 22+ |
| Package manager | pnpm |

也可以添加生产构建配置：

| 配置项 | 值 |
|--------|------|
| Name | build |
| Command | run |
| Scripts | build |

配置完成后，点击工具栏的绿色三角按钮或使用 `Shift+F10` 运行开发服务器。

---

### 三、代码导航：在大型前端项目中快速跳转

vue-pure-admin 的 `src/` 目录有 12 个子目录、数百个文件。以下导航技巧至关重要。

#### 3.1 核心导航快捷键

| 操作 | 快捷键 | 使用场景 |
|------|--------|----------|
| **Search Everywhere** | `Shift Shift` | 跨目录查找任何文件/类/符号 |
| **Go to File** | `Ctrl+Shift+N` | 快速跳转到 user.ts、index.vue 等文件 |
| **Go to Symbol** | `Ctrl+Alt+Shift+N` | 查找函数、变量、组件名 |
| **Go to Declaration** | `Ctrl+B` | 从 `useUserStore()` 跳转到 store 定义 |
| **Go to Implementation** | `Ctrl+Alt+B` | 从接口定义跳转到具体实现 |
| **Go to Type Definition** | `Ctrl+Shift+B` | 从变量跳转到其 TypeScript 类型 |
| **Find Usages** | `Alt+F7` | 查找某个 composable 或组件在哪里被使用 |
| **Type Hierarchy** | `Ctrl+H` | 查看 TypeScript 类型的继承/实现关系 |
| **Recent Files** | `Ctrl+E` | 快速切换最近打开的文件 |
| **Navigate to Line** | `Ctrl+G` | 跳转到指定行号 |

#### 3.2 Vue 专属导航

**模板到脚本跳转**：在 `<template>` 中按住 `Ctrl` 点击组件名或变量名，WebStorm 会自动跳转到 `<script setup>` 中的对应定义。

```vue
<template>
  <!-- Ctrl+Click 跳转到 useTable 的定义 -->
  <el-table :data="tableData" v-loading="loading">
    <!--                    ↑ Ctrl+Click 跳转到 script 中的 tableData 变量 -->
  </el-table>
</template>

<script setup lang="ts">
import { useTable } from "@/composables/useTable";
const { loading, tableData } = useTable(getUserListApi);
</script>
```

**路由到组件跳转**：在路由文件中 `Ctrl+Click` 点击 `import()` 路径，直接跳转到对应的 `.vue` 页面组件。

```typescript
// router/modules/system.ts
// Ctrl+Click 直接跳转到页面组件
component: () => import("@/views/system/user/index.vue")
```

**别名路径解析**：WebStorm 自动识别 `tsconfig.json` 中的 `paths` 配置（`@/` → `src/`），在代码补全和导航中正确处理路径别名。

#### 3.3 CamelHumps 搜索

开启 CamelHumps 后，搜索时只需输入大写字母：

- 要找到 `CreateTodoListCommandHandler`，输入 `CTLC` 或 `CTLCH`
- 要找到 `usePermissionStoreHook`，输入 `uPSH`
- 要找到 `ReSearchForm`，输入 `RSF`

设置路径：Settings → Editor → General → Code Completion → 勾选 "Match from start" 和 "CamelHumps"。

#### 3.4 Structure 窗口

按 `Alt+7` 打开 Structure 窗口，显示当前 `.vue` 文件的完整大纲：

```
index.vue
├── <template>
│   ├── <ReSearchForm>
│   ├── <RePureTableBar>
│   └── <el-pagination>
├── <script setup>
│   ├── imports
│   ├── useTable()
│   ├── openDialog()
│   ├── handleDelete()
│   └── handleExport()
└── <style scoped>
    └── .page-container
```

在大型页面组件中（500+ 行），Structure 窗口可以快速定位到特定的函数或模板区块。

#### 3.5 Services 工具窗口

`Alt+8` 打开 Services 窗口，集中管理 npm scripts、运行配置和开发服务器。可以同时运行 dev server 和 test runner，并查看各自的输出。

---

### 四、Vue SFC 编辑：高效的组件开发

#### 4.1 组件创建

在文件资源管理器中右键 → New → Vue Component，输入组件名即可创建：

```vue
<!-- WebStorm 自动生成的 Vue 3 组件模板 -->
<script setup lang="ts">
// 组件逻辑
</script>

<template>
  <div>
    <!-- 模板内容 -->
  </div>
</template>

<style scoped lang="scss">
/* 组件样式 */
</style>
```

可以在 Settings → Editor → File and Code Templates → Vue Single File Component 中自定义模板。

#### 4.2 模板智能补全

WebStorm 对 Vue 模板提供了极强的智能补全：

**组件补全**：在 `<template>` 中输入 `<el-b`，自动提示 Element Plus 组件：

```
<el-b
  ├── <el-button>          Element Plus 按钮
  ├── <el-badge>           Element Plus 徽章
  ├── <el-breadcrumb>      Element Plus 面包屑
  └── <el-breadcrumb-item>
```

**属性补全**：输入 `<el-button ` 后，自动提示所有 props：

```
<el-button
  ├── type          "primary" | "success" | "warning" | "danger"
  ├── size          "large" | "default" | "small"
  ├── plain         Boolean
  ├── round         Boolean
  ├── circle        Boolean
  ├── loading       Boolean
  ├── disabled      Boolean
  └── icon          String (Iconify / Element Plus 图标名)
```

**事件补全**：输入 `@` 后提示组件的自定义事件：

```
<el-button @
  ├── @click         点击事件
  ├── @mouseenter    鼠标进入
  └── @mouseleave    鼠标离开
```

**表达式补全**：在 `{{ }}` 或 `:prop=""` 中，自动提示 `<script setup>` 中定义的所有变量和函数。

#### 4.3 Emmet 支持

WebStorm 内置 Emmet，在 Vue 模板中可以直接使用：

| 输入 | 展开结果 |
|------|----------|
| `div.container` | `<div class="container"></div>` |
| `ul>li*5` | 包含 5 个 `<li>` 的 `<ul>` |
| `.card>h2+p+button.btn` | 卡片布局结构 |
| `table>(thead>tr>th*3)+(tbody>tr*5>td*3)` | 完整表格结构 |

#### 4.4 重构 Vue 组件

**提取 Vue 组件（Extract Vue Component）**：

选中模板中的一段代码 → `Ctrl+Alt+Shift+T` → "Extract Vue Component"：

```vue
<!-- 选中这段模板代码 -->
<div class="user-card">
  <el-avatar :src="user.avatar" />
  <span>{{ user.name }}</span>
  <el-tag>{{ user.role }}</el-tag>
</div>

<!-- WebStorm 自动提取为独立组件 -->
```

提取后生成新文件 `UserCard.vue`：

```vue
<script setup lang="ts">
interface Props {
  user: {
    avatar: string;
    name: string;
    role: string;
  };
}
defineProps<Props>();
</script>

<template>
  <div class="user-card">
    <el-avatar :src="user.avatar" />
    <span>{{ user.name }}</span>
    <el-tag>{{ user.role }}</el-tag>
  </div>
</template>
```

原文件中自动替换为组件引用：

```vue
<UserCard :user="currentUser" />
```

**提取方法（Extract Method）**：

选中 `<script setup>` 中的一段逻辑代码 → `Ctrl+Alt+M`，WebStorm 自动分析依赖变量，生成函数签名：

```typescript
// 选中这段代码
const result = tableData.value.filter(item =>
  item.status === "active" && item.createTime > startDate
);
totalActive.value = result.length;

// 提取为方法
function getActiveItems() {
  const result = tableData.value.filter(item =>
    item.status === "active" && item.createTime > startDate
  );
  totalActive.value = result.length;
}
```

**重命名（Rename）**：

`Shift+F6` 重命名变量/函数/组件名时，WebStorm 会同时更新：
- `<script>` 中的定义和所有引用
- `<template>` 中的绑定和事件
- 其他文件中对该导出的引用
- 路由配置中的组件名（如果适用）

---

### 五、TypeScript 支持

#### 5.1 类型提示与推断

WebStorm 对 TypeScript 的支持极为完善，在 Vue 项目中特别体现为：

**泛型推断**：在 `useTable<UserType>(...)` 中，`tableData` 自动推断为 `Ref<UserType[]>`，所有属性和方法都有完整的类型提示。

**Props 类型推断**：使用 `defineProps<Props>()` 声明的 props，在 `<template>` 中自动获得类型检查：

```vue
<script setup lang="ts">
interface Props {
  status: "active" | "inactive" | "pending";
  count: number;
}
const props = defineProps<Props>();
</script>

<template>
  <!-- ✅ status 只接受 "active" | "inactive" | "pending" -->
  <span :class="props.status">...</span>

  <!-- ❌ 类型错误：count 是 number，不能直接拼接 -->
  <span>{{ props.count + " items" }}</span>  <!-- WebStorm 会标记警告 -->
</template>
```

#### 5.2 类型导航

- `Ctrl+Click` 类型名 → 跳转到类型定义
- `Ctrl+Shift+B` → 跳转到类型声明文件
- `Ctrl+H` → 查看类型的实现层次结构
- `Alt+F7` → 查找类型的所有使用位置

#### 5.3 快速修复

按 `Alt+Enter` 触发上下文操作：

| 操作 | 触发条件 |
|------|----------|
| **添加缺失的属性** | 接口缺少必需字段时 |
| **移除未使用的 import** | 导入了但未使用的模块 |
| **添加类型注解** | 变量缺少显式类型时 |
| **添加 `as const`** | 将对象字面量推断为字面量类型 |
| **导入缺失的类型** | 使用了未导入的类型名 |
| **将 any 替换为具体类型** | 使用了 `any` 类型时 |

---

### 六、Live Templates：代码片段加速

#### 6.1 内置模板

| 模板 | 输入 | 效果 | 使用场景 |
|------|------|------|----------|
| `vbase` | vbase + Tab | Vue SFC 基础结构 | 创建新组件 |
| `vue-script-setup` | vss + Tab | `<script setup lang="ts">` 块 | 脚本定义 |
| `vfor` | v-for + Tab | `v-for` 指令 | 列表渲染 |
| `vif` | v-if + Tab | `v-if` 指令 | 条件渲染 |
| `vmodel` | v-model + Tab | `v-model` 绑定 | 双向绑定 |
| `von` | v-on + Tab | 事件监听 | 事件处理 |
| `vslot` | v-slot + Tab | 插槽定义 | 自定义插槽 |
| `vteleport` | vtel + Tab | `<Teleport>` 组件 | 传送门 |

#### 6.2 自定义模板建议

**Composable 模板**（Settings → Live Templates → Vue → 添加）：

缩写：`comp`

```typescript
import { ref, computed, onMounted, onUnmounted } from "vue";

export function use$NAME$() {
  const $FIELD$ = ref($DEFAULT$);

  function $METHOD$() {
    $END$
  }

  onMounted(() => {
    // 初始化逻辑
  });

  onUnmounted(() => {
    // 清理逻辑
  });

  return {
    $FIELD$,
    $METHOD$
  };
}
```

**API 模块模板**：

缩写：`api`

```typescript
import { http } from "@/utils/http";
import type { Result, PageResult } from "@/api/types";

/** 获取$NAME$列表 */
export const get$NAME$ListApi = (params?: QueryParams) => {
  return http.request<PageResult<$TYPE$>>("get", "/api/$PATH$", { params });
};

/** 获取$NAME$详情 */
export const get$NAME$DetailApi = (id: number | string) => {
  return http.request<$TYPE$>("get", `/api/$PATH$/${id}`);
};

/** 新增$NAME$ */
export const add$NAME$Api = (data: Partial<$TYPE$>) => {
  return http.request<Result<boolean>>("post", "/api/$PATH$", { data });
};

/** 修改$NAME$ */
export const update$NAME$Api = (data: $TYPE$) => {
  return http.request<Result<boolean>>("put", "/api/$PATH$", { data });
};

/** 删除$NAME$ */
export const delete$NAME$Api = (id: number | string) => {
  return http.request<Result<boolean>>("delete", `/api/$PATH$/${id}`);
};
```

**页面组件模板**：

缩写：`page`

```vue
<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import type { $TYPE$ } from "@/api/types";

defineOptions({ name: "$NAME$" });

const router = useRouter();
const loading = ref(false);
const tableData = ref<$TYPE$[]>([]);

async function loadData() {
  loading.value = true;
  try {
    // TODO: 调用 API
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadData();
});
</script>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" :border="true">
        $END$
      </el-table>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.app-container {
  padding: 16px;
}
</style>
```

---

### 七、调试技巧

#### 7.1 JavaScript 调试

在 WebStorm 中调试 Vue 应用，可以直接在 `.vue` 文件的 `<script>` 和 `<template>` 中设置断点。

**设置步骤**：

1. 创建 JavaScript Debug 运行配置：Run → Edit Configurations → + → JavaScript Debug
   - URL: `http://localhost:8848`（vue-pure-admin 默认端口）
   - Browser: Chrome
2. 在代码中点击行号旁边的 gutter 设置断点
3. 点击 Debug 按钮（小虫子图标）启动

**支持的断点类型**：

| 断点类型 | 使用场景 |
|----------|----------|
| **行断点** (`Ctrl+F8`) | 在 composable 函数、事件处理中暂停 |
| **条件断点** (右键断点) | 只在特定条件下中断（如 `item.id === 10`） |
| **日志断点** (Tracepoint) | 不中断执行，只输出日志到控制台 |
| **异常断点** | 当 `Error` 被抛出时自动中断 |
| **DOM 断点** | 当指定 DOM 元素被修改时中断 |

#### 7.2 调试 Vue 响应式状态

在调试暂停时，可以在 Watches 窗口中查看 Vue 的响应式状态：

```
Watches:
  ├── count                    → RefImpl { _value: 0 }
  ├── count._value             → 0
  ├── userStore.roles          → Proxy ['admin']
  ├── tableData.value[0]       → { id: 1, name: "张三", ... }
  └── route.meta.title         → "用户管理"
```

**Evaluate Expression** (`Alt+F8`) 中可以执行 JavaScript 表达式：

```javascript
// 在调试时检查 Store 状态
document.querySelector(".el-table").__vue_app__

// 执行 API 请求测试
fetch("/api/getUserInfo").then(r => r.json())

// 检查响应式引用
tableData.value.filter(item => item.status === "active")
```

#### 7.3 Vue DevTools 集成

WebStorm 可以与 Vue DevTools 浏览器扩展协同工作。在调试模式下，Vue DevTools 的面板会显示：

- **Components**：组件树和每个组件的 props / state / computed
- **Pinia**：所有 Store 的当前状态和 action 调用历史
- **Router**：当前路由和导航历史
- **Timeline**：事件时间线（emit、mutation、navigation）

---

### 八、ESLint 与 Prettier 集成

#### 8.1 自动配置

WebStorm 自动检测项目中的 `.eslintrc.js` 和 `.prettierrc` 配置文件，并启用对应的检查和格式化。

#### 8.2 实时检查

ESLint 错误在编辑器中以波浪线标记：

- 红色波浪线：错误（必须修复）
- 黄色波浪线：警告（建议修复）

`Alt+Enter` 在波浪线上可以快速修复：自动导入缺失的模块、移除未使用的变量、修复代码风格问题。

#### 8.3 Prettier 配置

在 Settings → Languages & Frameworks → JavaScript → Prettier 中：

| 设置项 | 推荐值 |
|--------|--------|
| Prettier package | 自动检测（node_modules/prettier） |
| Run for files | `{**/*,*}.{js,ts,jsx,tsx,vue,json,css,scss,html}` |
| On save | 勾选 "Run Prettier" |

---

### 九、Tailwind CSS 支持

#### 9.1 类名补全

WebStorm 内置 Tailwind CSS 插件，在 `class` 属性中提供完整的类名补全：

```vue
<!-- 输入 class="flex " 后自动提示 -->
class="flex
  ├── items-center
  ├── justify-between
  ├── flex-col
  ├── flex-wrap
  ├── gap-4
  └── ..."
```

#### 9.2 类名预览

将鼠标悬停在 Tailwind 类名上，WebStorm 显示对应的 CSS 属性：

```
items-center → align-items: center
p-4          → padding: 1rem (16px)
text-sm      → font-size: 0.875rem; line-height: 1.25rem
bg-blue-500  → background-color: #3b82f6
```

#### 9.3 类名排序

安装 Tailwind CSS 插件后，`Alt+Enter` 可以自动排序 Tailwind 类名（按照官方推荐顺序：布局 → 盒模型 → 排版 → 视觉 → 其他）。

---

### 十、Git 集成

#### 10.1 三向合并工具

解决 Git 合并冲突时，WebStorm 提供三面板界面：

```
┌──────────────┬──────────────┬──────────────┐
│   本地修改    │   合并结果    │   远程修改    │
│  (Your)      │  (Result)    │  (Their)     │
│              │              │              │
│  << << >>  接受 →  ← 接受   │              │
│              │              │              │
└──────────────┴──────────────┴──────────────┘
```

中间面板是完整的编辑器，可以手动编辑合并结果。

#### 10.2 Local History

即使不使用 Git，WebStorm 也会自动追踪文件的所有修改历史：

右键文件 → Local History → Show History

```
时间线：
├── 14:30  External change (保存)
├── 14:25  Typing (添加了 handleDelete 函数)
├── 14:20  Paste (粘贴了 useTable 调用)
└── 14:15  Refactoring (重命名了变量)
```

每个时间点都可以查看文件差异和恢复。在误删代码或错误重构后，Local History 是救命稻草。

#### 10.3 Git 操作快捷键

| 操作 | 快捷键 |
|------|--------|
| VCS 操作弹窗 | `Alt+`` |
| Commit | `Ctrl+K` |
| Push | `Ctrl+Shift+K` |
| Pull | 通过 VCS 弹窗 |
| 查看 Git 日志 | `Alt+9` (Version Control 窗口) |
| 比较差异 | `Ctrl+D` |
| Annotate（Git Blame） | 右键 gutter → Annotate |

---

### 十一、HTTP Client：API 测试

WebStorm 内置 HTTP Client，可以替代 Postman 直接在 IDE 中测试 API。

#### 11.1 创建 .http 文件

在项目根目录创建 `requests/` 文件夹，添加 `.http` 文件：

```http
### 登录
POST http://localhost:8848/api/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}

> {%
  client.global.set("authToken", response.body.data.accessToken);
%}

### 获取用户信息
GET http://localhost:8848/api/getUserInfo
Authorization: Bearer {{authToken}}

### 获取用户列表
GET http://localhost:8848/api/system/user?page=1&pageSize=10
Authorization: Bearer {{authToken}}

### 新增用户
POST http://localhost:8848/api/system/user
Content-Type: application/json
Authorization: Bearer {{authToken}}

{
  "username": "newuser",
  "nickname": "新用户",
  "password": "123456",
  "roles": ["admin"]
}
```

#### 11.2 环境变量

创建 `http-client.env.json`：

```json
{
  "dev": {
    "baseUrl": "http://localhost:8848"
  },
  "staging": {
    "baseUrl": "https://staging.example.com"
  },
  "prod": {
    "baseUrl": "https://api.example.com"
  }
}
```

在请求中使用 `{{baseUrl}}` 引用环境变量。

---

### 十二、终端集成

#### 12.1 内置终端

`Alt+F12` 打开内置终端。WebStorm 的终端支持：

- 多标签页
- 自动激活 Node.js / pnpm 环境
- 命令历史
- 复制粘贴优化

#### 12.2 常用终端命令

```bash
# 开发服务器
pnpm dev

# 构建生产包
pnpm build

# 运行单元测试
pnpm test:unit

# 运行 E2E 测试
pnpm test:e2e

# 代码检查
pnpm lint

# 代码格式化
pnpm format

# 预览生产包
pnpm preview

# 查看依赖分析
pnpm build --report
```

---

### 十三、高效开发的其他技巧

#### 13.1 剪贴板历史

`Ctrl+Shift+V` 打开剪贴板历史，访问最近复制的所有内容。在复制粘贴组件代码并修改时非常有用。

#### 13.2 多光标编辑

| 操作 | 快捷键 | 使用场景 |
|------|--------|----------|
| 添加光标 | `Alt+Click` | 同时编辑多处 |
| 选中下一个相同词 | `Alt+J` | 批量修改变量名 |
| 选中所有相同词 | `Ctrl+Alt+Shift+J` | 全局替换 |
| 列选择模式 | `Alt+Shift+Insert` | 垂直选择 |

#### 13.3 代码折叠

| 操作 | 快捷键 |
|------|--------|
| 折叠当前代码块 | `Ctrl+-`（小键盘） |
| 展开当前代码块 | `Ctrl++`（小键盘） |
| 折叠所有 | `Ctrl+Shift+-` |
| 展开所有 | `Ctrl+Shift++` |
| 折叠模板/脚本/样式块 | 在 `<template>` / `<script>` / `<style>` 标签旁点击折叠箭头 |

#### 13.4 Find Action（命令面板）

`Ctrl+Shift+A` 打开命令面板，搜索并执行任何 WebStorm 命令。忘记快捷键时的救星。

#### 13.5 书签

| 操作 | 快捷键 | 使用场景 |
|------|--------|----------|
| 添加/移除书签 | `F11` | 标记重要代码位置 |
| 添加带编号的书签 | `Ctrl+F11` → 选择数字 | 快速跳转到常用位置 |
| 跳转到编号书签 | `Ctrl+数字` | 例如 `Ctrl+1` 跳到书签 1 |
| 查看所有书签 | `Shift+F11` | 浏览所有书签 |

在阅读 vue-pure-admin 源码时，可以将路由入口、Store 定义、HTTP 封装等关键文件设为书签，快速跳转。

#### 13.6 TODO 窗口

`Alt+6` 打开 TODO 窗口，列出代码中所有 `TODO`、`FIXME`、`HACK` 注释。帮助追踪待完成的工作。

```typescript
// TODO: 添加用户头像上传功能
// FIXME: 分页在数据量 > 10000 时性能问题
// HACK: 临时绕过 Element Plus 的 bug
```

---

### 十四、小结

WebStorm 为 Vue 3 + TypeScript 开发提供了全方位的支持：

- **导航**：Ctrl+Click 跨文件跳转、CamelHumps 搜索、Structure 窗口，在 vue-pure-admin 的数百个文件中快速定位
- **编辑**：Vue SFC 模板补全、TypeScript 类型推断、Emmet、Live Templates，大幅减少重复输入
- **重构**：提取组件、提取方法、安全重命名，确保代码修改不会引入新 bug
- **调试**：直接在 .vue 文件中设置断点、查看 Vue 响应式状态、Vue DevTools 集成
- **工具链**：内置 ESLint、Prettier、Tailwind CSS、HTTP Client、Git 集成，无需配置额外插件

掌握这些技巧后，开发效率可以提升 30-50%。建议从最常用的快捷键开始，逐步扩展到高阶功能。
