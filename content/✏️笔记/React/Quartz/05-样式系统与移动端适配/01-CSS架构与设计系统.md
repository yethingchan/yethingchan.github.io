本项目的样式系统采用分层架构，从底层变量到高层组件逐级构建。理解这套架构，是后续自定义主题、适配移动端、以及添加高级视觉效果的基础。

## CSS 架构分层总览

整个样式系统由四个层次组成，加载顺序如下：

```
variables.scss     -- SCSS 变量（断点、网格、字重）
  |
  v
base.scss          -- 基础布局（CSS Grid 三栏、侧栏、代码块、排版）
  |
  v
custom.scss        -- 自定义增强（代码块美化、毛玻璃、星空、浮动组件、移动端）
  |
  v
styles/*.scss      -- 组件级样式（explorer、toc、search、graph 等）
```

- `variables.scss` 定义所有全局 SCSS 变量，是整个样式系统的"配置中心"。
- `base.scss`（651 行）引入 `variables.scss`、`syntax.scss`、`callouts.scss`，搭建页面的骨架布局和基础排版。
- `custom.scss`（1491 行）引入 `base.scss`，在此基础上叠加项目专属的视觉增强。
- 各组件的 `styles/xxx.scss` 负责单一组件的样式，与全局样式解耦。

这种分层的好处是：修改全局断点只需改 `variables.scss`，修改代码块外观只需改 `custom.scss`，互不干扰。

## SCSS 变量系统详解

### 断点变量

`quartz/styles/variables.scss` 通过 Sass map 定义了两个核心断点：

```scss
$breakpoints: (
  mobile: 800px,
  desktop: 1200px,
);
```

然后派生出三个媒体查询字符串变量：

```scss
$mobile:  "(max-width: 800px)";            // <= 800px：移动端
$tablet:  "(min-width: 800px) and (max-width: 1200px)"; // 801~1199px：平板端
$desktop: "(min-width: 1200px)";           // >= 1200px：桌面端
```

这三个变量被所有样式文件通过 `@use "./variables.scss" as *` 引入，保证断点值全局一致。本项目还额外使用了一个 480px 断点（直接写在 `custom.scss` 中），用于极小屏幕的进一步适配。

### Grid 模板

同一文件定义了三种屏幕尺寸下的 CSS Grid 模板，每种模板规定了 `grid-template-areas`（命名区域）和 `grid-template-columns/rows`：

```scss
$mobileGrid: (
  templateRows: "auto auto auto auto auto",
  templateColumns: "auto",
  templateAreas:
    '"grid-sidebar-left"
      "grid-header"
      "grid-center"
      "grid-sidebar-right"
      "grid-footer"',
);

$tabletGrid: (
  templateRows: "auto auto auto auto",
  templateColumns: "#{$sidePanelWidth} auto",  // 380px + 自适应
  templateAreas:
    '"grid-sidebar-left grid-header"
      "grid-sidebar-left grid-center"
      "grid-sidebar-left grid-sidebar-right"
      "grid-sidebar-left grid-footer"',
);

$desktopGrid: (
  templateRows: "auto auto auto",
  templateColumns: "#{$sidePanelWidth} auto #{$sidePanelWidth}", // 380px + 自适应 + 380px
  templateAreas:
    '"grid-sidebar-left grid-header grid-sidebar-right"
      "grid-sidebar-left grid-center grid-sidebar-right"
      "grid-sidebar-left grid-footer grid-sidebar-right"',
);
```

### 其他全局变量

```scss
$sidePanelWidth: 380px;    // 侧栏宽度
$topSpacing: 6rem;         // 侧栏顶部留白
$boldWeight: 700;          // 粗体
$semiBoldWeight: 600;      // 半粗体（用于标题、链接）
$normalWeight: 400;         // 常规字重
```

这些变量在 `base.scss` 和组件样式中广泛使用，例如 `h1` 到 `h6` 的 `font-weight` 和链接的 `font-weight` 都引用 `$semiBoldWeight`。

## CSS Grid 三栏布局系统

布局的核心是 `.page > #quartz-body` 上的 `display: grid`。`base.scss` 根据三个断点切换不同的 Grid 模板：

```scss
& > #quartz-body {
  display: grid;
  // 默认使用桌面端模板
  grid-template-columns: 380px auto 380px;
  grid-template-rows: auto auto auto;
  grid-template-areas:
    "grid-sidebar-left grid-header grid-sidebar-right"
    "grid-sidebar-left grid-center grid-sidebar-right"
    "grid-sidebar-left grid-footer grid-sidebar-right";

  @media all and ($tablet) {
    // 平板端：右侧栏折叠到左栏下方
    grid-template-columns: 380px auto;
    grid-template-areas:
      "grid-sidebar-left grid-header"
      "grid-sidebar-left grid-center"
      "grid-sidebar-left grid-sidebar-right"
      "grid-sidebar-left grid-footer";
  }

  @media all and ($mobile) {
    // 移动端：单列全宽
    grid-template-columns: auto;
    grid-template-areas:
      "grid-sidebar-left"
      "grid-header"
      "grid-center"
      "grid-sidebar-right"
      "grid-footer";
  }
}
```

**桌面端（>= 1200px）**：左 380px（Explorer）+ 中间自适应（正文）+ 右 380px（TOC/Graph）。`#quartz-body` 自身设为 `overflow-y: auto; height: 100dvh`，成为滚动容器，使两侧侧栏的 `position: sticky` 正常工作。

**平板端（801-1199px）**：左 380px（Explorer）+ 中间自适应。右侧栏内容（TOC、Graph 等）折叠到左栏下方，以水平 flex 排列。TOC 隐藏（`display: none`）。

**移动端（<= 800px）**：单列布局，所有区域垂直堆叠。左右侧栏均脱离 sticky 定位，`position: initial; height: unset`。`#quartz-body` 不再是滚动容器，由 `window` 接管滚动。

侧栏的 sticky 行为通过以下 CSS 实现：

```scss
& .sidebar {
  position: sticky;
  top: 0;
  height: 100dvh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
```

在非桌面端，`base.scss` 将侧栏重置为普通文档流：

```scss
@media all and not ($desktop) {
  & .sidebar.right {
    position: initial;
    height: unset;
    overflow: visible;
  }
}
@media all and ($mobile) {
  & .sidebar.left {
    position: initial;
    height: unset;
    overflow: visible;
  }
}
```

## CSS 自定义属性与主题系统

Quartz 使用 CSS 自定义属性（CSS Variables）实现主题切换。`quartz/util/theme.ts` 中的 `joinStyles` 函数会根据用户配置动态生成 `:root` 和 `:root[saved-theme="dark"]` 两个选择器下的变量值：

```scss
:root {
  --light: <亮色模式背景色>;
  --lightgray: <亮灰色>;
  --gray: <灰色>;
  --darkgray: <深灰色，正文颜色>;
  --dark: <最深色，标题颜色>;
  --secondary: <强调色>;
  --tertiary: <次强调色>;
  --highlight: <高亮色>;
  --textHighlight: <文本高亮色>;
  --headerFont: "...", system-ui, sans-serif;
  --bodyFont: "...", system-ui, sans-serif;
  --codeFont: "...", monospace;
}

:root[saved-theme="dark"] {
  --light: <暗色模式背景色>;
  --darkgray: <暗色模式正文颜色>;
  // ... 所有变量覆盖
}
```

这样，所有引用 `var(--darkgray)` 的元素都会在主题切换时自动变色，无需编写额外的条件样式。

## 亮/暗色主题的切换机制

主题切换由 `quartz/components/scripts/darkmode.inline.ts` 实现，核心逻辑非常简洁：

```typescript
// 1. 首次加载：读取 localStorage 或系统偏好
const userPref = window.matchMedia("(prefers-color-scheme: light)").matches
  ? "light" : "dark";
const currentTheme = localStorage.getItem("theme") ?? userPref;
document.documentElement.setAttribute("saved-theme", currentTheme);

// 2. 用户点击切换按钮
const switchTheme = () => {
  const newTheme = document.documentElement.getAttribute("saved-theme") === "dark"
    ? "light" : "dark";
  document.documentElement.setAttribute("saved-theme", newTheme);
  localStorage.setItem("theme", newTheme);
  emitThemeChangeEvent(newTheme);
};

// 3. 监听系统主题变化
const themeChange = (e: MediaQueryListEvent) => {
  const newTheme = e.matches ? "dark" : "light";
  document.documentElement.setAttribute("saved-theme", newTheme);
  localStorage.setItem("theme", newTheme);
  emitThemeChangeEvent(newTheme);
};
```

流程总结：在 `<html>` 元素上设置 `saved-theme` 属性 -> CSS 选择器 `:root[saved-theme="dark"]` 生效 -> 所有 CSS 变量值被覆盖 -> 页面呈现暗色主题。同时通过 `CustomEvent("themechange")` 通知其他组件（如星空背景的亮度调整）。

## 代码块样式系统

本项目使用 `rehype-pretty-code` 插件 + Shiki 语法高亮引擎，支持双主题（亮/暗）。`base.scss` 定义了代码块的骨架：

```scss
pre {
  font-family: var(--codeFont);
  padding: 0 0.5rem;
  border-radius: 5px;
  overflow-x: auto;
  border: 1px solid var(--lightgray);
}

pre > code {
  font-size: 0.85rem;
  display: grid;
  // 行号、高亮行样式
}
```

`custom.scss` 在此基础上大幅增强了代码块的视觉效果：

```scss
pre {
  padding: 1.15rem 3.4rem 1.15rem 1.15rem !important;
  min-height: 3.2rem;
  border-radius: 14px;
  font-size: 0.92rem;
  line-height: 1.7;
  font-family: "JetBrains Mono", "Cascadia Code", "Fira Code",
    "Consolas", "Menlo", monospace;
  border: 1px solid rgba(148, 163, 184, 0.22);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

// 行内代码
:not(pre) > code {
  font-family: "JetBrains Mono", "Cascadia Code", "Fira Code",
    "Consolas", "Menlo", monospace;
  background: color-mix(in srgb, var(--secondary) 12%, transparent);
  color: var(--secondary);
  border: 1px solid color-mix(in srgb, var(--secondary) 18%, transparent);
}
```

关键设计决策：使用 `color-mix()` 函数让行内代码的背景色自动跟随主题的 `--secondary` 色值，保持视觉一致性。自定义滚动条样式也让代码块在长代码时体验更佳。

## 文章卡片的毛玻璃效果

`custom.scss` 为 `article` 元素添加了半透明背景 + 边框 + 阴影，形成毛玻璃卡片效果：

```scss
// 暗色模式
:root[saved-theme="dark"] article {
  background: rgba(8, 13, 28, 0.42);
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  box-shadow: 0 14px 36px rgba(0, 0, 0, 0.22);
  padding: 1rem 2rem;
}

// 亮色模式
:root[saved-theme="light"] article {
  background: rgba(255, 255, 255, 0.78);
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.06);
  padding: 1rem 2rem;
}
```

由于 `body` 背景使用了多层径向渐变（暗色模式的紫色/青色光晕），半透明的 `article` 背景会让这些渐变隐约透出，配合 `backdrop-filter: blur` 风格的 `box-shadow`，视觉上呈现出高级的毛玻璃质感。

## 组件级样式与全局样式的分离原则

每个 Quartz 组件的样式都独立存放在 `quartz/components/styles/` 目录下，例如：

| 文件 | 职责 |
|------|------|
| `explorer.scss` | 文件浏览器布局、移动端滑入动画、文件夹折叠 |
| `toc.scss` | 右侧目录的嵌套样式 |
| `search.scss` | 全局搜索弹窗 |
| `graph.scss` | 关系图面板 |
| `prevNextNav.scss` | 上一章/下一章导航 |
| `clipboard.scss` | 代码块复制按钮 |

组件通过 `Explorer.css = style` 的方式在 TypeScript 中注册样式，Quartz 构建时会自动收集并打包。这种分离确保了组件的可插拔性：移除一个组件时，其样式也会随之消失，不会在全局 CSS 中留下无用代码。

总结来说，本项目的样式系统遵循"变量驱动 -> 全局基础 -> 自定义增强 -> 组件独立"的分层哲学，使得每一层都有明确的职责边界，便于维护和扩展。
