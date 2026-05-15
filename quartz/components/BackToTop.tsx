import { QuartzComponentConstructor } from "./types";

const BackToTop: QuartzComponentConstructor = () => {
  return () => (
    <>
      {/* 星空粒子背景 */}
      <div id="tsparticles"></div>

      {/* 顶部阅读进度条 */}
      <div id="reading-progress"></div>

      <div id="floating-stack">
        {/* 上下滚动按钮 */}
        <button id="scroll-toggle" aria-label="Scroll Toggle">
          <div className="progress-ring"></div>
          <span id="scroll-icon">▲</span>
        </button>

        {/* 浮动时间按钮 */}
        <div id="floating-clock">--:--</div>
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

    const scrollTop = window.scrollY;

    const scrollHeight =
      document.body.scrollHeight -
      window.innerHeight;

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
        window.scrollY * 0.12;

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

      window.addEventListener(
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

        window.scrollTo({

          top:
            window.scrollY < 200
              ? document.body.scrollHeight
              : 0,

          behavior: "smooth",
        });
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

        tocContent.innerHTML = "";

        const headings =
          document.querySelectorAll(
            "article h1, article h2, article h3"
          );

        headings.forEach(
          (heading, index) => {

            if (!heading.id) {

              heading.id =
                "heading-" + index;
            }

            const a =
              document.createElement("a");

            a.href =
              "#" + heading.id;

            a.textContent =
              heading.innerText;

            a.className =
              "toc-item toc-" +
              heading.tagName.toLowerCase();

            a.onclick = () => {

              tocPanel.classList.remove(
                "show"
              );
            };

            tocContent.appendChild(a);
          }
        );

        /* TOC 当前标题高亮 */
        if (tocObserver) {

          tocObserver.disconnect();
        }

        tocObserver =
          new IntersectionObserver(
            (entries) => {

              entries.forEach((entry) => {

                if (!entry.isIntersecting)
                  return;

                const id =
                  entry.target.id;

                const tocLinks =
                  document.querySelectorAll(
                    ".toc-item"
                  );

                tocLinks.forEach((link) => {

                  link.classList.remove(
                    "active"
                  );

                  if (
                    link.getAttribute("href") ===
                    "#" + id
                  ) {

                    link.classList.add(
                      "active"
                    );
                  }
                });
              });
            },
            {
              rootMargin:
                "-25% 0px -60% 0px",

              threshold: 0,
            }
          );

        headings.forEach((h) =>
          tocObserver.observe(h)
        );

        tocPanel.classList.toggle("show");
      };

      /* 点击外部关闭 */
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