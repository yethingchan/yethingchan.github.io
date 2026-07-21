# 07 · 组合式 API 与选项式 API

Vue 3 支持两种写法。**本项目使用组合式 API**，但理解两者的差异有助于读老代码。

## 选项式 API（Options API，Vue 2 风格）

数据和逻辑按"选项"归类：

```vue
<script>
export default {
  data() { return { count: 0 } },
  computed: { double() { return this.count * 2 } },
  methods: { inc() { this.count++ } },
  mounted() { console.log('挂载完成') }
}
</script>
```

- 优点：结构清晰，新手好懂。
- 缺点：一个功能散落在 `data`/`methods`/`computed` 多处，复杂组件难维护。

## 组合式 API（Composition API，Vue 3 推荐）

按"功能"聚合逻辑：

```vue
<script setup>
import { ref, computed, onMounted } from 'vue'
const count = ref(0)
const double = computed(() => count.value * 2)
function inc() { count.value++ }
onMounted(() => console.log('挂载完成'))
</script>
```

- 优点：相关代码写在一起，复用性高（可抽成"组合函数" `useXxx`）。
- 本项目 `user.js` store、`request.js` 拦截器都体现了这种聚合思维。

## `<script setup>` 语法糖

本项目所有 `.vue` 都用 `<script setup>`：

```vue
<script setup>
// 顶层变量/函数自动暴露给模板，无需 return
const msg = 'hi'
</script>
```

## 两种 API 对比

| 维度 | 选项式 | 组合式 |
|------|--------|--------|
| 组织单位 | 选项（data/methods） | 功能（逻辑块） |
| 复用逻辑 | mixins（易冲突） | 组合函数（useXxx） |
| TypeScript | 一般 | 友好 |
| 本项目使用 | 否 | ✅ 是 |

## 什么时候用选项式？

- 维护老项目、团队不熟悉组合式时。
- 本教程与本项目默认组合式，建议新代码都用 `<script setup>`。

## 小结

- 组合式 API = 按功能组织代码，复用性强，是 Vue 3 主流。
- `<script setup>` 是最简洁的写法，本项目统一使用。

下一篇：[生命周期钩子](./08-生命周期钩子.md)
