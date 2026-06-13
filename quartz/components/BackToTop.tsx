import { QuartzComponentConstructor } from "./types";

const BackToTop: QuartzComponentConstructor = () => {
  return () => (
    <>
      {/* 顶部阅读进度条 */}
      <div id="reading-progress"></div>

      <div id="floating-stack">
        {/* 上下滚动按钮 */}
        <button id="scroll-toggle" aria-label="Scroll Toggle">
          <div className="progress-ring"></div>
          <span id="scroll-icon">▲</span>
        </button>

        {/* 浮动时间按钮 */}
        <div id="floating-clock" role="timer" aria-label="当前时间">--:--</div>
      </div>

      {/* TOC 面板 */}
      <div id="floating-toc">
        <div id="floating-toc-title">目录</div>
        <div id="floating-toc-content"></div>
      </div>

      <script
        dangerouslySetInnerHTML={{
          __html: `
(() => {

  let clockTimer = null;
  let tocObserver = null;
  let scrollHandlerBound = false;
  let clickHandlerBound = false;

  /* =========================
     锁定滚动条宽度
     防止 SPA 切换抖动
  ========================= */
  const scrollbarWidth =
    window.innerWidth -
    document.documentElement.clientWidth;

  document.documentElement.style.setProperty(
    "--scrollbar-width",
    scrollbarWidth + "px"
  );

  document.documentElement.style.overflowY = "scroll";

  /* =========================
     获取滚动容器（桌面端为 #quartz-body，移动端为 window）
  ========================= */
  function getScrollContainer() {
    const quartzBody = document.querySelector('.page > #quartz-body');
    if (quartzBody && getComputedStyle(quartzBody).overflowY === 'auto') {
      return quartzBody;
    }
    return window;
  }

  function getScrollTop() {
    const c = getScrollContainer();
    return c === window ? window.scrollY : c.scrollTop;
  }

  function getScrollHeight() {
    const c = getScrollContainer();
    if (c === window) return document.body.scrollHeight;
    return c.scrollHeight;
  }

  function getClientHeight() {
    const c = getScrollContainer();
    if (c === window) return window.innerHeight;
    return c.clientHeight;
  }

  function scrollToTop() {
    const c = getScrollContainer();
    if (c === window) {
      window.scrollTo({ top: 0, behavior: "smooth" });
    } else {
      c.scrollTo({ top: 0, behavior: "smooth" });
    }
  }

  function scrollToBottom() {
    const c = getScrollContainer();
    if (c === window) {
      window.scrollTo({ top: document.body.scrollHeight, behavior: "smooth" });
    } else {
      c.scrollTo({ top: c.scrollHeight, behavior: "smooth" });
    }
  }

  /* =========================
     公共 TOC 构建函数
  ========================= */
  function buildAndShowToc() {
    const tocPanel = document.getElementById("floating-toc");
    const tocContent = document.getElementById("floating-toc-content");
    if (!tocPanel || !tocContent) return;

    tocContent.innerHTML = "";

    const headings = document.querySelectorAll(
      "article h1, article h2, article h3"
    );

    headings.forEach((heading, index) => {
      if (!heading.id) {
        heading.id = "heading-" + index;
      }
      const a = document.createElement("a");
      a.href = "#" + heading.id;
      a.textContent = heading.innerText;
      a.className = "toc-item toc-" + heading.tagName.toLowerCase();
      a.onclick = () => {
        tocPanel.classList.remove("show");
      };
      tocContent.appendChild(a);
    });

    /* TOC 当前标题高亮 */
    if (tocObserver) {
      tocObserver.disconnect();
    }

    tocObserver = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return;
          const id = entry.target.id;
          const tocLinks = document.querySelectorAll(".toc-item");
          tocLinks.forEach((link) => {
            link.classList.remove("active");
            if (link.getAttribute("href") === "#" + id) {
              link.classList.add("active");
            }
          });
        });
      },
      {
        rootMargin: "-25% 0px -60% 0px",
        threshold: 0,
      }
    );

    headings.forEach((h) => tocObserver.observe(h));
    tocPanel.classList.toggle("show");
  }

  /* =========================
     页面切换动画
  ========================= */
  function triggerPageTransition() {

    document.body.classList.remove(
      "page-transition"
    );

    requestAnimationFrame(() => {

      requestAnimationFrame(() => {

        document.body.classList.add(
          "page-transition"
        );

      });

    });
  }

  /* =========================
     Scroll Progress
  ========================= */
  function updateScroll() {

    const scrollBtn =
      document.getElementById("scroll-toggle");

    const scrollIcon =
      document.getElementById("scroll-icon");

    const progressBar =
      document.getElementById("reading-progress");

    const scrollTop = getScrollTop();

    const scrollHeight =
      getScrollHeight() -
      getClientHeight();

    const progress =
      Math.min(
        scrollTop / Math.max(scrollHeight, 1),
        1
      );

    /* 顶部阅读进度 */
    if (progressBar) {

      progressBar.style.transform =
        \`scaleX(\${progress})\`;
    }

    /* 圆形按钮进度 */
    if (scrollBtn) {

      scrollBtn.style.setProperty(
        "--progress",
        progress.toString()
      );
    }

    /* 箭头切换 */
    if (scrollIcon) {

      scrollIcon.innerHTML =
        scrollTop < 200 ? "▼" : "▲";
    }

    /* Banner Parallax */
    const banner =
      document.querySelector(".article-cover img");

    if (banner) {

      const offset =
        getScrollTop() * 0.12;

      banner.style.transform =
        \`scale(1.06) translateY(\${offset}px)\`;
    }
  }

  /* =========================
     初始化
  ========================= */
  function init() {

    if (clockTimer) {
      clearInterval(clockTimer);
    }

    const scrollBtn =
      document.getElementById("scroll-toggle");

    const clockBtn =
      document.getElementById("floating-clock");

    const tocPanel =
      document.getElementById("floating-toc");

    const tocContent =
      document.getElementById("floating-toc-content");

    /* =========================
       绑定 Scroll
    ========================= */
    if (!scrollHandlerBound) {

      const container = getScrollContainer();
      container.addEventListener(
        "scroll",
        updateScroll,
        { passive: true }
      );

      scrollHandlerBound = true;
    }

    updateScroll();

    /* =========================
       Scroll Button
    ========================= */
    if (scrollBtn) {

      scrollBtn.onclick = () => {

        if (getScrollTop() < 200) {
          scrollToBottom();
        } else {
          scrollToTop();
        }
      };
    }

    /* =========================
       Clock
    ========================= */
    const updateClock = () => {

      if (!clockBtn) return;

      const now = new Date();

      const hh =
        String(now.getHours())
          .padStart(2, "0");

      const mm =
        String(now.getMinutes())
          .padStart(2, "0");

      clockBtn.innerText =
        hh + ":" + mm;
    };

    updateClock();

    clockTimer =
      setInterval(updateClock, 60000);

    /* =========================
       TOC
    ========================= */
    if (
      clockBtn &&
      tocPanel &&
      tocContent
    ) {

      clockBtn.onclick = (e) => {

        e.stopPropagation();

        buildAndShowToc();
      };

      /* 点击外部关闭 - 只绑定一次 */
      if (!clickHandlerBound) {
        document.addEventListener(
          "click",
          (e) => {

            if (
              !tocPanel.contains(e.target) &&
              !clockBtn.contains(e.target)
            ) {

              tocPanel.classList.remove(
                "show"
              );
            }
          }
        );
        clickHandlerBound = true;
      }
      /* PC端浮动目录触发快捷键 - 只绑定一次 */
      if (!clickHandlerBound) {
        document.addEventListener("keydown", (e) => {
          if (e.ctrlKey && e.key.toLowerCase() === "m") {
            buildAndShowToc();
          }
        });
        clickHandlerBound = true;
      }
    }
  }

  /* =========================
     首次加载
  ========================= */
  triggerPageTransition();

  init();

  /* =========================
     Quartz SPA Navigation
  ========================= */
  document.addEventListener(
    "nav",
    () => {

      triggerPageTransition();

      setTimeout(() => {

        init();

        updateScroll();

        const tocPanel =
          document.getElementById(
            "floating-toc"
          );

        if (tocPanel) {

          tocPanel.classList.remove(
            "show"
          );
        }

      }, 80);
    }
  );

})();
          `,
        }}
      />
    </>
  );
};

export default BackToTop;