Quartz 是一个由 Jacky Zhao 开发的开源静态站点生成器（SSG），专注于将 Markdown 笔记转化为可发布的网站。它的核心理念是"内容为王"——你只需专注于用 Markdown 写作，Quartz 负责处理所有的展示逻辑、导航结构、搜索索引和样式渲染。同时，Quartz 不只是一个博客工具，它更是一个**知识管理工具**，天然支持双向链接、图谱视图、文件夹层级浏览，非常适合用来构建"数字花园"（Digital Garden）。

与传统的博客框架不同，Quartz 并不追求开箱即用的丰富主题或模板市场。它的定位是提供一个**高度可编程的内容管道**，让开发者能够通过插件和组件对每一个环节进行精确控制。这种设计哲学与 Obsidian（一个流行的 Markdown 笔记应用）的"核心极简、插件扩展"思路如出一辙——事实上，Quartz 原本就是为了将 Obsidian 笔记库发布为网站而诞生的。

## 与其他静态博客框架的对比

在静态站点生成器的生态中，选择非常多。以下表格将 Quartz 与主流方案进行横向对比：

| 特性 | Quartz | Hugo | Jekyll | VitePress | Hexo |
|:---|:---|:---|:---|:---|:---|
| **语言** | TypeScript | Go | Ruby | TypeScript/Vue | JavaScript/Node.js |
| **UI 框架** | Preact | Go Templates | Liquid | Vue 3 | EJS/Pug |
| **构建速度** | 快（多线程） | 极快（原生编译） | 慢（Ruby） | 快（Vite） | 中等 |
| **SPA 支持** | 原生支持 | 无 | 无 | 有限 | 无 |
| **双向链接** | 内置 | 需插件 | 需插件 | 无 | 需插件 |
| **图谱视图** | 内置 | 需插件 | 需插件 | 无 | 需插件 |
| **全文搜索** | 内置（FlexSearch） | 需配置 | 需插件 | 内置 | 需插件 |
| **插件生态** | 内置管道，高度可编程 | 丰富但文档参差 | 成熟但维护慢 | Vue 生态 | 主题丰富 |
| **学习曲线** | 中等（需 TS 基础） | 低（YAML 配置） | 低 | 中等（Vue 基础） | 低 |
| **适合场景** | 知识库/数字花园 | 多主题博客/文档站 | 简单博客 | API 文档/技术文档 | 中文博客社区 |

Quartz 的独特优势在于它同时实现了**静态生成（SSG）的高性能**和**单页应用（SPA）的流畅体验**。大多数 SSG 框架在页面切换时会触发完整的浏览器刷新，而 Quartz 通过 DOM morphing 技术实现了无刷新导航，体验接近原生应用。

## 技术栈详解

Quartz v4 的技术栈经过精心选择，每一项都在"功能"和"体积"之间做了权衡。

### Preact 而非 React

这是一个常见的问题：既然 React 的生态更庞大，为什么 Quartz 选择了 Preact？

答案是**体积**。React 的运行时体积约为 40KB（gzipped 后约 13KB），而 Preact 只有 3KB（gzipped 后约 1KB）。对于一个静态博客来说，页面的 JavaScript 体积直接影响首屏加载速度，而博客的 UI 交互又相对简单——不需要复杂的 State Management、不需要 Context API 的高级用法、不需要 Concurrent Mode。Preact 提供了与 React 兼容的 API（`useState`、`useEffect`、`jsx`），足以满足 Quartz 的所有需求。

```typescript
// quartz/components/StarryBackground.tsx 中的组件写法
// 完全使用 Preact 的 API，语法与 React 一致
const StarryBackground: QuartzComponentConstructor = () => {
  return () => (
    <>
      <div id="starry-background"></div>
      <script
        type="module"
        dangerouslySetInnerHTML={{
          __html: `
            import { tsParticles } from "https://cdn.jsdelivr.net/npm/tsparticles-engine@2/+esm";
            // ... 粒子效果初始化代码
          `,
        }}
      />
    </>
  )
}
```

更重要的是，Quartz 在**构建时**使用 `preact-render-to-string` 将组件渲染为静态 HTML 字符串，这意味着页面在初始加载时不需要等待 JavaScript 执行——内容已经嵌入在 HTML 中了。

### TypeScript 全栈

Quartz 从配置文件到插件系统全部使用 TypeScript 编写，提供了完整的类型安全保障。例如，插件的类型定义清晰地划分了三种插件的能力边界：

```typescript
// quartz/plugins/types.ts
export interface PluginTypes {
  transformers: QuartzTransformerPluginInstance[]
  filters: QuartzFilterPluginInstance[]
  emitters: QuartzEmitterPluginInstance[]
}
```

### remark/rehype 生态

Quartz 的 Markdown 处理建立在 unified 生态之上，这是目前 JavaScript 世界中最成熟的 Markdown/HTML 处理方案。整个处理流程分为三个阶段：

1. **remark 阶段** -- 将 Markdown 文本解析为 MDAST（Markdown Abstract Syntax Tree），并在此层执行 Markdown 级别的转换（如 frontmatter 提取、GFM 语法、Obsidian 特殊语法）
2. **remark-rehype 桥接** -- 将 MDAST 转换为 HAST（HTML Abstract Syntax Tree）
3. **rehype 阶段** -- 在 HTML AST 层面执行转换（如语法高亮、KaTeX 公式渲染、标题自动链接）

### SCSS 样式

Quartz 使用 SCSS 作为样式预处理器，通过 esbuild 的 sass 插件在构建时编译。样式架构分为全局样式（`base.scss`、`variables.scss`、`custom.scss`）和组件样式（各组件目录下的 `.scss` 文件），利用 CSS 变量实现了暗色模式的切换。

### Vite 构建管线

开发模式下，Quartz 使用 Vite 的开发服务器提供热更新体验。构建时则通过 esbuild 进行编译，配合 workerpool 实现多线程 Markdown 解析。

## 内容管道：从 Markdown 到 HTML

理解 Quartz 的关键在于理解它的内容管道。你可以把整个过程想象成一条工厂流水线：

```
Markdown 文件
  |  (读取文件内容)
  v
Text Transform (文本级转换，如去除首尾空白)
  |  (remark-parse)
  v
MDAST (Markdown 抽象语法树)
  |  (Markdown 插件链：FrontMatter → GFM → OFM → TOC → CrawlLinks → ...)
  v
MDAST (转换后的语法树)
  |  (remark-rehype)
  v
HAST (HTML 抽象语法树)
  |  (HTML 插件链：SyntaxHighlight → Katex → Slug → AutoLink)
  v
HTML AST (最终语法树)
  |  (hast-util-to-html)
  v
HTML 字符串 → Preact 组件渲染 → 完整 HTML 页面
```

这个过程在源码中体现为三个处理器，定义在 `quartz/processors/parse.ts` 中：

```typescript
// 创建 Markdown 处理器：text -> MDAST -> transformed MDAST
export function createMdProcessor(ctx: BuildCtx): QuartzMdProcessor {
  const transformers = ctx.cfg.plugins.transformers
  return (
    unified()
      .use(remarkParse)                                    // 文本 -> MDAST
      .use(transformers.flatMap(
        (plugin) => plugin.markdownPlugins?.(ctx) ?? []
      )) as unknown as QuartzMdProcessor                   // MDAST 变换
  )
}

// 创建 HTML 处理器：MDAST -> HAST -> transformed HAST
export function createHtmlProcessor(ctx: BuildCtx): QuartzHtmlProcessor {
  const transformers = ctx.cfg.plugins.transformers
  return (
    unified()
      .use(remarkRehype, { allowDangerousHtml: true })    // MDAST -> HAST
      .use(transformers.flatMap(
        (plugin) => plugin.htmlPlugins?.(ctx) ?? []
      ))                                                   // HAST 变换
  )
}
```

处理完毕后，内容会经过 Filter（过滤器）决定哪些文件应该发布，最终由 Emitter（发射器）将内容写入输出目录。每一步都可以通过插件来扩展或替换。

## 项目源码结构总览

Odyssey 项目的 `quartz/` 目录是整个博客的核心。以下是完整的目录树及其功能说明：

```
quartz/
  build.ts                   # 构建入口：完整构建 + 文件监听 + 增量重建
  cfg.ts                     # 配置类型定义（QuartzConfig、FullPageLayout 等）
  worker.ts                  # Worker 线程：Markdown 解析的并行处理单元
  
  cli/                       # 命令行工具
    args.js                  # yargs 命令行参数定义
    handlers.js              # 命令处理逻辑（build、build 指令等）
    constants.js             # CLI 常量
    helpers.js               # CLI 辅助函数
  
  components/                # UI 组件（Preact）
    index.ts                 # 组件统一导出
    renderPage.tsx           # 页面渲染引擎
    types.ts                 # 组件类型定义（QuartzComponent）
    Head.tsx                 # <head> 标签管理
    Header.tsx / Footer.tsx  # 页头页脚
    Explorer.tsx             # 文件树浏览器（核心组件）
    Search.tsx               # 全文搜索
    Graph.tsx                # 关系图谱（D3.js）
    StarryBackground.tsx     # 星空粒子背景（自定义）
    CursorGlow.tsx           # 光标发光效果（自定义）
    BackToTop.tsx            # 返回顶部按钮
    pages/                   # 页面级组件
      Content.tsx            # 文章内容页
      FolderContent.tsx      # 文件夹列表页
      TagContent.tsx          # 标签列表页
      404.tsx                 # 404 页面
    scripts/                 # 客户端脚本（inline scripts）
      spa.inline.ts          # SPA 路由与 DOM morphing
      explorer.inline.ts     # Explorer 交互逻辑
      search.inline.ts       # 搜索逻辑
      darkmode.inline.ts      # 暗色模式切换
      graph.inline.ts        # 图谱渲染
      toc.inline.ts          # 目录交互
    styles/                  # 组件级 SCSS 样式
  
  plugins/                   # 插件系统
    types.ts                 # 插件类型定义
    index.ts                 # 插件统一导出
    transformers/            # 内容转换插件
      frontmatter.ts         # Frontmatter 解析
      gfm.ts                 # GitHub Flavored Markdown
      ofm.ts                 # Obsidian Flavored Markdown
      syntax.ts              # 代码语法高亮（Shiki）
      toc.ts                 # 目录生成
      links.ts               # 链接解析与双向链接
      latex.ts               # LaTeX/KaTeX 公式
      citations.ts           # 引用管理
    filters/                 # 内容过滤插件
      draft.ts               # 草稿过滤
      explicit.ts            # 显式发布控制
    emitters/                # 内容发射插件
      contentPage.tsx        # HTML 页面生成
      folderPage.tsx          # 文件夹页面
      tagPage.tsx            # 标签页面
      contentIndex.tsx       # 内容索引 + RSS + Sitemap
      assets.ts              # 静态资源拷贝
      static.ts              # 通用静态文件
      aliases.ts             # 别名重定向
      ogImage.tsx            # OG 社交图片生成
      favicon.ts             # Favicon 处理
  
  processors/                 # 核心处理器
    parse.ts                 # 解析管道（MD/HTML）
    filter.ts                # 过滤管道
    emit.ts                  # 发射管道
  
  i18n/                      # 国际化
    index.ts                 # 翻译入口与 locale 映射
    locales/                 # 各语言翻译文件（zh-CN.ts 等）
    definition.ts            # 翻译类型定义
  
  styles/                    # 全局样式
    base.scss                # 基础样式
    variables.scss           # CSS 变量（颜色、字体、间距）
    custom.scss              # 用户自定义样式（核心定制入口）
    callouts.scss            # Callout 样式
    syntax.scss              # 代码高亮主题
  
  util/                      # 工具模块
    path.ts                  # 路径处理与 slug 生成
    fileTrie.ts              # Trie 树数据结构
    resources.tsx            # 静态资源管理
    theme.ts                 # 主题类型定义
    glob.ts                  # 文件匹配
    perf.ts                  # 性能计时器
    log.ts                   # 日志工具
    emoji.ts                 # Emoji 处理
  
  static/                    # 静态资源
    fonts/                   # 字体文件
    icon.png                 # 网站图标
    noise.svg                # 纹理背景
```

理解这个目录结构是深入 Quartz 的第一步。在后续的教程中，我们会频繁引用这些文件。你不需要一次性记住所有细节——每当你遇到一个具体问题时，回来查阅这个目录树，就能快速定位到对应的源文件。
