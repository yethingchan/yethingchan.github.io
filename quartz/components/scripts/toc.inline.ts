// TOC 高亮：使用 scroll 事件 + 精确计算当前可见标题
function updateTocHighlight() {
  const tocLinks = document.querySelectorAll(".toc a[data-for]")
  if (tocLinks.length === 0) return

  const headers = Array.from(
    document.querySelectorAll("article h1[id], article h2[id], article h3[id], article h4[id], article h5[id], article h6[id]")
  )

  if (headers.length === 0) return

  // 找到滚动容器
  const container =
    document.querySelector(".page > #quartz-body") ||
    document.documentElement

  const containerTop =
    container === document.documentElement ? 0 : container.getBoundingClientRect().top
  const scrollTop =
    container === document.documentElement
      ? window.scrollY
      : (container as HTMLElement).scrollTop
  const viewportHeight =
    container === document.documentElement
      ? window.innerHeight
      : (container as HTMLElement).clientHeight

  // 当前阅读位置：视口上方 25% 处（偏上一点，更符合阅读习惯）
  const readLine = scrollTop + viewportHeight * 0.25

  // 找到最后一个在阅读线上方的标题
  let activeSlug = ""
  for (const header of headers) {
    const headerTop =
      header.getBoundingClientRect().top - containerTop + scrollTop
    if (headerTop <= readLine) {
      activeSlug = header.id
    } else {
      break
    }
  }

  // 更新 TOC 高亮
  tocLinks.forEach((link) => {
    const slug = link.getAttribute("data-for")
    if (slug === activeSlug) {
      link.classList.add("in-view")
    } else {
      link.classList.remove("in-view")
    }
  })
}

let tocScrollBound = false

function toggleToc(this: HTMLElement) {
  this.classList.toggle("collapsed")
  this.setAttribute(
    "aria-expanded",
    this.getAttribute("aria-expanded") === "true" ? "false" : "true",
  )
  const content = this.nextElementSibling as HTMLElement | undefined
  if (!content) return
  content.classList.toggle("collapsed")
}

function setupToc() {
  // @ts-ignore: HTMLCollection iteration
  for (const toc of document.getElementsByClassName("toc")) {
    const button = toc.querySelector(".toc-header")
    const content = toc.querySelector(".toc-content")
    if (!button || !content) return
    button.addEventListener("click", toggleToc)
    if (window.addCleanup) {
      // @ts-ignore: addCleanup is a Quartz global
      window.addCleanup(() => button.removeEventListener("click", toggleToc))
    }
  }
}

document.addEventListener("nav", () => {
  setupToc()

  // 绑定 scroll 事件监听（只绑定一次）
  if (!tocScrollBound) {
    const container =
      document.querySelector(".page > #quartz-body") || window
    container.addEventListener("scroll", updateTocHighlight, { passive: true })
    window.addEventListener("resize", updateTocHighlight, { passive: true })
    tocScrollBound = true
  }

  // 初始高亮
  requestAnimationFrame(updateTocHighlight)
})

// 首次加载时也初始化（nav 事件只在 SPA 导航时触发）
setupToc()
if (!tocScrollBound) {
  const container =
    document.querySelector(".page > #quartz-body") || window
  container.addEventListener("scroll", updateTocHighlight, { passive: true })
  window.addEventListener("resize", updateTocHighlight, { passive: true })
  tocScrollBound = true
}
requestAnimationFrame(updateTocHighlight)
