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

  /* =========================
     Page Transition
  ========================= */
  document.body.classList.remove("page-transition");

  requestAnimationFrame(() => {
    document.body.classList.add("page-transition");
  });

  function init() {

    if (clockTimer) clearInterval(clockTimer);

    const scrollBtn = document.getElementById("scroll-toggle");
    const scrollIcon = document.getElementById("scroll-icon");

    const clockBtn = document.getElementById("floating-clock");

    const tocPanel = document.getElementById("floating-toc");
    const tocContent = document.getElementById("floating-toc-content");

    const progressBar = document.getElementById("reading-progress");

    /* =========================
       Scroll Progress
    ========================= */
    const updateScroll = () => {

      const scrollTop = window.scrollY;

      const scrollHeight =
        document.body.scrollHeight - window.innerHeight;

      const progress =
        Math.min(scrollTop / scrollHeight, 1);

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

        const offset = window.scrollY * 0.12;

        banner.style.setProperty(
          "--banner-offset",
          offset + "px"
        );
      }
    };

    window.addEventListener("scroll", updateScroll);

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
        String(now.getHours()).padStart(2, "0");

      const mm =
        String(now.getMinutes()).padStart(2, "0");

      clockBtn.innerText = hh + ":" + mm;
    };

    updateClock();

    clockTimer = setInterval(updateClock, 60000);

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

        headings.forEach((heading, index) => {

          if (!heading.id) {
            heading.id = "heading-" + index;
          }

          const a = document.createElement("a");

          a.href = "#" + heading.id;

          a.textContent = heading.innerText;

          a.className =
            "toc-item toc-" +
            heading.tagName.toLowerCase();

          a.onclick = () => {
            tocPanel.classList.remove("show");
          };

          tocContent.appendChild(a);
        });

        /* TOC Active Highlight */
        if (tocObserver) {
          tocObserver.disconnect();
        }

        tocObserver =
          new IntersectionObserver(
            (entries) => {

              entries.forEach((entry) => {

                const id = entry.target.id;

                const tocLinks =
                  document.querySelectorAll(
                    ".toc-item"
                  );

                if (entry.isIntersecting) {

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
                }
              });
            },
            {
              rootMargin:
                "-30% 0px -60% 0px",

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
            tocPanel.classList.remove("show");
          }
        }
      );
    }
  }

  /* =========================
     Init
  ========================= */
  init();

  /* =========================
     Quartz SPA Nav
  ========================= */
  document.addEventListener(
    "nav",
    () => {

      document.body.classList.remove(
        "page-transition"
      );

      requestAnimationFrame(() => {

        document.body.classList.add(
          "page-transition"
        );
      });

      setTimeout(() => {

        init();

        const tocPanel =
          document.getElementById(
            "floating-toc"
          );

        if (tocPanel) {
          tocPanel.classList.remove("show");
        }

      }, 100);
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