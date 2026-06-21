## Vue3 + TypeScript 设计模式与最佳实践

> 本文是 vue-pure-admin 深度教程的第四篇，从设计模式和最佳实践的角度，分析项目中 Vue 3 + TypeScript 的典型用法。
> 每个模式都附带源码示例和适用场景分析。

---

### 一、Composition API 设计模式

#### 1.1 Composable 模式（可组合函数）

Composable 是 Vue 3 Composition API 最核心的设计模式。它将可复用的状态和逻辑封装为独立函数，替代了 Vue 2 中的 Mixin。

**vue-pure-admin 中的典型用法：**

```typescript
// composables/useTable.ts — 通用表格逻辑封装
export function useTable<T>(fetchApi: (params: any) => Promise<PageResult<T>>) {
  const loading = ref(false);
  const tableData = ref<T[]>([]) as Ref<T[]>;
  const pagination = reactive({
    currentPage: 1,
    pageSize: 10,
    total: 0
  });
  const searchParams = ref<Record<string, any>>({});

  async function loadData() {
    loading.value = true;
    try {
      const result = await fetchApi({
        page: pagination.currentPage,
        pageSize: pagination.pageSize,
        ...searchParams.value
      });
      tableData.value = result.list;
      pagination.total = result.total;
    } finally {
      loading.value = false;
    }
  }

  function handleSearch(params: Record<string, any>) {
    searchParams.value = params;
    pagination.currentPage = 1;
    loadData();
  }

  function handlePageChange(page: number) {
    pagination.currentPage = page;
    loadData();
  }

  function handleSizeChange(size: number) {
    pagination.pageSize = size;
    pagination.currentPage = 1;
    loadData();
  }

  // 首次自动加载
  onMounted(() => loadData());

  return {
    loading,
    tableData,
    pagination,
    loadData,
    handleSearch,
    handlePageChange,
    handleSizeChange
  };
}
```

**在页面中使用：**

```vue
<script setup lang="ts">
import { useTable } from "@/composables/useTable";
import { getUserListApi } from "@/api/system";

const {
  loading,
  tableData,
  pagination,
  loadData,
  handleSearch,
  handlePageChange,
  handleSizeChange
} = useTable(getUserListApi);
</script>

<template>
  <el-table v-loading="loading" :data="tableData">
    <el-table-column prop="username" label="用户名" />
    <el-table-column prop="nickname" label="昵称" />
  </el-table>
  <el-pagination
    v-model:current-page="pagination.currentPage"
    v-model:page-size="pagination.pageSize"
    :total="pagination.total"
    @current-change="handlePageChange"
    @size-change="handleSizeChange"
  />
</template>
```

**Composable vs Mixin 对比：**

| 维度 | Mixin（Vue 2） | Composable（Vue 3） |
|------|----------------|---------------------|
| 命名冲突 | 多个 mixin 的同名属性会覆盖 | 通过解构重命名避免冲突 |
| 来源追踪 | 难以确定属性来自哪个 mixin | 明确知道每个变量来自哪个 composable |
| 类型推断 | TypeScript 支持差 | 完整的类型推断 |
| 嵌套使用 | 不支持 | composable 之间可以互相调用 |

#### 1.2 Provide / Inject 模式

当组件嵌套较深时，props 逐层传递会导致 "prop drilling" 问题。Vue 3 的 `provide` / `inject` 可以在祖先组件提供数据，任意后代组件直接消费。

```typescript
// layout/index.vue — 提供布局上下文
<script setup lang="ts">
import { provide, readonly } from "vue";

const layoutContext = reactive({
  isMobile: false,
  sidebarCollapsed: false,
  toggleSidebar: () => { /* ... */ }
});

// 使用 readonly 防止后代组件直接修改
provide("layoutContext", readonly(layoutContext));
</script>
```

```typescript
// layout/components/sidebar/index.vue — 消费布局上下文
<script setup lang="ts">
const layoutContext = inject("layoutContext", {
  isMobile: false,
  sidebarCollapsed: false,
  toggleSidebar: () => {}
});
</script>
```

**最佳实践**：将 `provide` 的 key 定义为 Symbol 或常量，避免字符串冲突；使用 `readonly` 包裹提供的对象，确保数据流的单向性。

#### 1.3 Teleport 模式

`<Teleport>` 将组件的 DOM 渲染到指定的容器元素下，常用于弹窗、通知等需要脱离当前组件层级的场景。

```vue
<!-- 通知组件 — 渲染到 body 下 -->
<template>
  <Teleport to="body">
    <div v-if="visible" class="notification-overlay">
      <div class="notification-content">
        <slot />
      </div>
    </div>
  </Teleport>
</template>
```

vue-pure-admin 的 `ReDialog` 组件内部就使用了 Teleport，确保弹窗的 z-index 和定位不受父组件 CSS 影响。

---

### 二、TypeScript 高级用法

#### 2.1 泛型组件

vue-pure-admin 大量使用泛型组件，让组件在保持类型安全的同时具有灵活性。

```typescript
// components/ReTable/index.vue — 泛型表格组件
<script setup lang="ts" generic="T extends Record<string, any>">
interface Props {
  data: T[];
  columns: ColumnConfig<T>[];
  loading?: boolean;
  pagination?: PaginationConfig;
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
});

const emit = defineEmits<{
  "page-change": [page: number];
  "size-change": [size: number];
  "row-click": [row: T];
}>();
</script>
```

使用 `generic` 属性后，在模板中使用组件时，TypeScript 会自动推断 `T` 的具体类型：

```vue
<ReTable
  :data="userList"
  :columns="userColumns"
  @row-click="(row) => {
    // row 的类型自动推断为 UserType
    console.log(row.username);
  }"
/>
```

#### 2.2 类型守卫与类型断言

```typescript
// utils/type-guards.ts

/** 判断值是否为 Ref */
export function isRef<T>(value: Ref<T> | T): value is Ref<T> {
  return value !== null && typeof value === "object" && "_value" in value;
}

/** 判断是否为 API 错误响应 */
export function isApiError(value: unknown): value is ApiError {
  return (
    typeof value === "object" &&
    value !== null &&
    "code" in value &&
    "message" in value
  );
}

/** 在组件中使用 */
function handleError(error: unknown) {
  if (isApiError(error)) {
    // TypeScript 知道 error 是 ApiError 类型
    ElMessage.error(error.message);
  } else {
    ElMessage.error("未知错误");
  }
}
```

#### 2.3 类型体操：工具类型

```typescript
// types/utils.ts — 常用工具类型

/** 将所有属性变为可选，除了指定的 */
type RequiredKeys<T, K extends keyof T> = Partial<T> & Required<Pick<T, K>>;

/** 将 Ref<T> 展开为 T */
type UnwrapRef<T> = T extends Ref<infer V> ? V : T;

/** 深度 Readonly */
type DeepReadonly<T> = {
  readonly [K in keyof T]: T[K] extends object
    ? DeepReadonly<T[K]>
    : T[K];
};

/** 提取 Pinia Store 的 State 类型 */
type ExtractStoreState<S> = S extends Store<
  string,
  infer State,
  any,
  any
>
  ? State
  : never;

/** 从 API 返回类型中提取 data 字段类型 */
type ExtractApiData<T> = T extends Promise<Result<infer D>> ? D : never;
```

#### 2.4 defineProps 与 defineEmits 的类型声明

```typescript
<script setup lang="ts">
// ✅ 推荐：基于类型的声明（TypeScript 类型推断最佳）
interface Props {
  title: string;
  visible?: boolean;
  width?: string;
  data: UserType[];
  onSubmit: (form: FormData) => void;
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  width: "500px"
});

// ✅ 推荐：基于类型的 emit 声明
const emit = defineEmits<{
  "update:visible": [value: boolean];
  "submit": [data: FormData];
  "cancel": [];
}>();

// 使用
function handleClose() {
  emit("update:visible", false);
}

function handleSubmit() {
  // props.data 类型为 UserType[]
  // props.title 类型为 string
  emit("submit", { /* ... */ });
}
</script>
```

---

### 三、状态管理模式

#### 3.1 Pinia Store 的组织原则

vue-pure-admin 的 Store 遵循**单一职责 + 按领域拆分**的原则：

| Store | 职责 | 关键状态 |
|-------|------|----------|
| **userStore** | 用户认证和信息 | token, roles, permissions, avatar |
| **permissionStore** | 路由权限和菜单 | wholeMenus, isDynamicLoaded |
| **layoutStore** | 布局配置和主题 | device, sidebar, theme |
| **tagsStore** | 标签页管理 | tagList, activeTag |
| **keepAliveStore** | 页面缓存 | cachePageList |
| **epThemeStore** | Element Plus 主题 | theme, primaryColor |

#### 3.2 Store 之间的通信

当多个 Store 需要协作时，vue-pure-admin 采用**在 Action 中调用其他 Store** 的模式：

```typescript
// store/modules/user.ts
export const useUserStore = defineStore({
  id: "pure-user",
  actions: {
    async logOut() {
      await logoutApi();

      // 清理本 Store 状态
      this.resetToken();

      // 联动清理其他 Store
      usePermissionStore().$reset();
      useTagsStore().closeAllTags();
      useKeepAliveStore().clearAll();

      // 重置路由
      resetRouter();

      // 跳转登录页
      router.push("/login");
    }
  }
});
```

#### 3.3 Pinia 持久化

对于需要在页面刷新后保留的状态，使用 `pinia-plugin-persistedstate`：

```typescript
// store/modules/layout.ts
export const useLayoutStore = defineStore({
  id: "pure-layout",
  state: () => ({
    layout: {
      theme: "light",
      primaryColor: "#409EFF",
      sidebarWidth: 210,
      showTags: true
    }
  }),
  persist: {
    // 只持久化 layout 配置，不持久化运行时状态
    paths: ["layout.theme", "layout.primaryColor", "layout.showTags"]
  }
});
```

---

### 四、组件设计模式

#### 4.1 复合组件模式（Compound Components）

复合组件将一组相关的子组件组合在一起，共享内部状态。vue-pure-admin 的 `ReSearchForm` + `RePureTableBar` + `ReTable` 组合就是一个典型：

```vue
<template>
  <!-- 搜索区域 -->
  <ReSearchForm :items="searchItems" @search="handleSearch" />

  <!-- 表格操作栏 -->
  <RePureTableBar :title="'用户列表'" @refresh="loadData">
    <template #buttons>
      <el-button v-auth="'system:user:add'" type="primary" @click="openDialog()">
        新增
      </el-button>
      <el-button v-auth="'system:user:export'" @click="handleExport">
        导出
      </el-button>
    </template>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="tableData" :border="true">
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button
            v-auth="'system:user:edit'"
            link
            type="primary"
            @click="openDialog(row)"
          >
            编辑
          </el-button>
          <el-button
            v-auth="'system:user:delete'"
            link
            type="danger"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </RePureTableBar>

  <!-- 分页 -->
  <el-pagination
    v-model:current-page="pagination.currentPage"
    :total="pagination.total"
    @current-change="handlePageChange"
  />
</template>
```

这种模式的优势在于每个组件职责单一，组合使用时具有高度灵活性。

#### 4.2 渲染函数模式（Render Function）

对于需要高度动态化的组件，vue-pure-admin 使用渲染函数（`h()`）而非模板：

```typescript
// components/ReIcon/src/iconify.ts
export default defineComponent({
  name: "ReIcon",
  props: {
    icon: { type: String, required: true },
    size: { type: [String, Number], default: 16 },
    color: { type: String }
  },
  setup(props) {
    return () => {
      const { icon, size, color } = props;

      // 根据图标类型选择不同的渲染策略
      if (icon.startsWith("svg-")) {
        return h(SvgIcon, { icon, size, color });
      }

      // Iconify 图标
      return h(IconifyIconOnline, {
        icon,
        width: size,
        height: size,
        color
      });
    };
  }
});
```

渲染函数适用于：组件的渲染逻辑有大量条件分支、需要动态生成 DOM 结构、追求极致性能（跳过模板编译）。

#### 4.3 插槽模式（Slots）

vue-pure-admin 的组件大量使用作用域插槽，让使用者可以自定义渲染内容：

```vue
<!-- ReDialog 组件的插槽设计 -->
<template>
  <el-dialog v-model="visible" :title="title" :width="width">
    <!-- 默认插槽：弹窗主体内容 -->
    <slot />

    <!-- 底部按钮插槽 -->
    <template #footer>
      <slot name="footer">
        <!-- 默认底部按钮 -->
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" :loading="confirmLoading" @click="handleConfirm">
          确定
        </el-button>
      </slot>
    </template>
  </el-dialog>
</template>
```

```vue
<!-- 使用作用域插槽自定义 -->
<ReDialog ref="dialogRef">
  <template #default="{ data }">
    <!-- data 由 ReDialog 传递，使用者可以自定义渲染 -->
    <UserForm :model-value="data" />
  </template>
  <template #footer="{ confirm, cancel }">
    <el-button @click="cancel">返回</el-button>
    <el-button type="success" @click="confirm">保存</el-button>
  </template>
</ReDialog>
```

---

### 五、异步操作模式

#### 5.1 异步组件加载

vue-pure-admin 的路由使用懒加载，确保只在访问时才加载对应页面的代码：

```typescript
// 路由懒加载 — 基于 Vite 的代码分割
{
  path: "user",
  component: () => import("@/views/system/user/index.vue")
}

// 带预加载的懒加载 — hover 时提前加载
const UserPage = defineAsyncComponent({
  loader: () => import("@/views/system/user/index.vue"),
  loadingComponent: () => h(SkeletonLoader),
  errorComponent: () => h(ErrorFallback),
  delay: 200,        // 延迟显示 loading（避免闪烁）
  timeout: 10000     // 超时时间
});
```

#### 5.2 并发请求与错误隔离

```typescript
// 使用 Promise.allSettled 并发请求，不因单个失败而中断
async function loadDashboardData() {
  const [userResult, orderResult, statsResult] = await Promise.allSettled([
    getUserStatsApi(),
    getOrderStatsApi(),
    getSystemStatsApi()
  ]);

  return {
    userStats: userResult.status === "fulfilled" ? userResult.value : null,
    orderStats: orderResult.status === "fulfilled" ? orderResult.value : null,
    systemStats: statsResult.status === "fulfilled" ? statsResult.value : null,
    errors: [userResult, orderResult, statsResult]
      .filter((r): r is PromiseRejectedResult => r.status === "rejected")
      .map(r => r.reason)
  };
}
```

#### 5.3 请求重试机制

```typescript
// utils/retry.ts
export async function retryRequest<T>(
  fn: () => Promise<T>,
  maxRetries = 3,
  delay = 1000
): Promise<T> {
  let lastError: Error;

  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error as Error;
      if (attempt < maxRetries) {
        // 指数退避：1s → 2s → 4s
        await new Promise(r => setTimeout(r, delay * Math.pow(2, attempt)));
      }
    }
  }

  throw lastError!;
}

// 使用
const data = await retryRequest(
  () => fetchDataApi(),
  3,     // 最多重试 3 次
  1000   // 初始延迟 1 秒
);
```

---

### 六、性能优化模式

#### 6.1 虚拟滚动

对于大数据量列表，vue-pure-admin 推荐使用虚拟滚动（只渲染可视区域内的元素）：

```vue
<script setup lang="ts">
import { useVirtualList } from "@vueuse/core";

const { list, containerProps, wrapperProps } = useVirtualList(
  allItems,    // 完整数据（可能有 10 万条）
  { itemHeight: 40 }  // 每行高度
);
</script>

<template>
  <div v-bind="containerProps" style="height: 400px; overflow: auto">
    <div v-bind="wrapperProps">
      <div v-for="item in list" :key="item.index" class="list-item">
        {{ item.data.name }}
      </div>
    </div>
  </div>
</template>
```

#### 6.2 防抖与节流

```typescript
import { useDebounceFn, useThrottleFn } from "@vueuse/core";

// 搜索输入框 — 防抖（停止输入 300ms 后触发）
const handleSearch = useDebounceFn((keyword: string) => {
  searchApi(keyword);
}, 300);

// 窗口 resize — 节流（每 200ms 最多触发一次）
const handleResize = useThrottleFn(() => {
  updateLayout();
}, 200);
```

#### 6.3 图片懒加载

```vue
<template>
  <el-image
    :src="imageUrl"
    :lazy="true"
    loading="lazy"
    fit="cover"
  >
    <template #placeholder>
      <el-skeleton animated>
        <template #template>
          <el-skeleton-item variant="image" style="width: 100%; height: 100%" />
        </template>
      </el-skeleton>
    </template>
    <template #error>
      <div class="image-error">
        <ReIcon icon="ep:picture" />
        <span>加载失败</span>
      </div>
    </template>
  </el-image>
</template>
```

#### 6.4 路由级预加载

```typescript
// 在鼠标 hover 菜单时预加载页面
function preloadRoute(path: string) {
  const route = router.getRoutes().find(r => r.path === path);
  if (route?.components?.default) {
    // 触发组件的 import() 加载
    (route.components.default as () => Promise<any>)();
  }
}
```

---

### 七、SOLID 原则在 Vue 3 中的实践

| 原则 | Vue 3 实践 | vue-pure-admin 示例 |
|------|-----------|---------------------|
| **S — 单一职责** | 每个组件只关注一个功能 | ReAuth 只做权限判断，不处理样式 |
| **O — 开闭原则** | 通过插槽和 props 扩展行为 | ReDialog 通过插槽扩展内容，无需修改组件内部 |
| **L — 里氏替换** | 组件可以替换为标准 HTML 元素 | ReIcon 渲染为 `<span>` 或 `<svg>`，可放在任何位置 |
| **I — 接口隔离** | defineProps 只暴露组件需要的属性 | ReTable 不暴露 ElTable 的所有 props，只选择常用的 |
| **D — 依赖反转** | 通过 inject/provide 解耦 | 布局组件通过 provide 提供上下文，子组件通过 inject 消费 |

---

### 八、测试策略

#### 8.1 单元测试（Vitest）

```typescript
// __tests__/composables/useTable.test.ts
import { describe, it, expect, vi } from "vitest";
import { useTable } from "@/composables/useTable";
import { mount } from "@vue/test-utils";

describe("useTable", () => {
  it("should load data on mount", async () => {
    const mockApi = vi.fn().mockResolvedValue({
      list: [{ id: 1, name: "test" }],
      total: 1
    });

    const wrapper = mount({
      setup() {
        const table = useTable(mockApi);
        return { ...table };
      },
      template: "<div />"
    });

    // 等待异步加载完成
    await flushPromises();

    expect(mockApi).toHaveBeenCalledWith({
      page: 1,
      pageSize: 10
    });
    expect(wrapper.vm.tableData).toHaveLength(1);
  });

  it("should handle search", async () => {
    const mockApi = vi.fn().mockResolvedValue({
      list: [],
      total: 0
    });

    const { handleSearch } = useTable(mockApi);
    handleSearch({ keyword: "test" });

    await flushPromises();

    expect(mockApi).toHaveBeenCalledWith({
      page: 1,
      pageSize: 10,
      keyword: "test"
    });
  });
});
```

#### 8.2 组件测试

```typescript
// __tests__/components/ReAuth.test.ts
import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import ReAuth from "@/components/ReAuth/index.vue";
import { createPinia, setActivePinia } from "pinia";

describe("ReAuth", () => {
  it("should render children when user has permission", () => {
    setActivePinia(createPinia());
    // Mock userStore.permissions = ["system:user:add"]
    useUserStore().permissions = ["system:user:add"];

    const wrapper = mount(ReAuth, {
      props: { value: "system:user:add" },
      slots: { default: "<button>Add</button>" }
    });

    expect(wrapper.find("button").exists()).toBe(true);
  });

  it("should not render children when user lacks permission", () => {
    setActivePinia(createPinia());
    useUserStore().permissions = [];

    const wrapper = mount(ReAuth, {
      props: { value: "system:user:add" },
      slots: { default: "<button>Add</button>" }
    });

    expect(wrapper.find("button").exists()).toBe(false);
  });
});
```

---

### 九、小结

本文从设计模式和最佳实践的角度，分析了 vue-pure-admin 中的关键实现：

- **Composable 模式**是 Vue 3 可复用逻辑的核心，替代了 Mixin 并解决了命名冲突和来源追踪问题
- **TypeScript 泛型组件**和工具类型让组件在保持灵活性的同时具有完整的类型安全
- **Pinia Store 的领域拆分**和联动清理模式，确保了状态管理的清晰和一致
- **复合组件 + 作用域插槽**的组合模式，让组件既标准化又高度可定制
- **虚拟滚动 + 防抖节流 + 路由预加载**等性能优化手段，保障了大数据量场景下的流畅体验

这些模式和实践不是 vue-pure-admin 独有的，而是 Vue 3 + TypeScript 生态中的通用最佳实践，可以直接应用到任何企业级前端项目中。
