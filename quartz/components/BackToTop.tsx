import { QuartzComponentConstructor } from "./types";

const BackToTop: QuartzComponentConstructor = () => {
  return () => (
    <>
      {/* 上下滚动按钮 */}
      <button id="scroll-toggle" aria-label="Scroll Toggle">
        <span id="scroll-icon">▲</span>
      </button>

      {/* TOC 按钮 */}
      <button id="toc-toggle" aria-label="Table of contents">
        ☰
      </button>

      {/* TOC 面板 */}
      <div id="floating-toc">
        <div id="floating-toc-title">目录</div>
        <div id="floating-toc-content"></div>
      </div>

      <script
        dangerouslySetInnerHTML={{
          __html: `
            (() => {

              /* =========================
                 Scroll Toggle
              ========================= */

              const initScrollButton = () => {
                const scrollBtn = document.getElementById("scroll-toggle");
                const scrollIcon = document.getElementById("scroll-icon");
                if (!scrollBtn || !scrollIcon) return;

                const updateState = () => {
                  const isNearTop = window.scrollY < 200;
                  if (isNearTop) {
                    scrollBtn.classList.add("down");
                    scrollIcon.innerHTML = "▼";
                  } else {
                    scrollBtn.classList.remove("down");
                    scrollIcon.innerHTML = "▲";
                  }
                };

                scrollBtn.onclick = () => {
                  const isNearTop = window.scrollY < 200;
                  if (isNearTop) {
                    window.scrollTo({
                      top: document.body.scrollHeight,
                      behavior: "smooth",
                    });
                  } else {
                    window.scrollTo({
                      top: 0,
                      behavior: "smooth",
                    });
                  }
                };

                window.addEventListener("scroll", updateState);
                updateState();
              };

              /* =========================
                 Floating TOC
              ========================= */

              const initTOC = () => {
                const tocBtn = document.getElementById("toc-toggle");
                const tocPanel = document.getElementById("floating-toc");
                const tocContent = document.getElementById("floating-toc-content");
                if (!tocBtn || !tocPanel || !tocContent) return;

                /* 清空旧目录 */
                tocContent.innerHTML = "";

                const headings = document.querySelectorAll("article h1, article h2, article h3");

                /* 无目录则隐藏按钮 */
                if (headings.length === 0) {
                  tocBtn.style.display = "none";
                  tocPanel.style.display = "none";
                  return;
                } else {
                  tocBtn.style.display = "flex";
                  tocPanel.style.display = "block";
                }

                /* 生成目录 */
                headings.forEach((heading, index) => {
                  if (!heading.id) heading.id = "heading-" + index;

                  const item = document.createElement("a");
                  item.href = "#" + heading.id;
                  item.innerText = heading.innerText;
                  item.className = "toc-item toc-" + heading.tagName.toLowerCase();

                  item.onclick = () => {
                    tocPanel.classList.remove("show");
                  };

                  tocContent.appendChild(item);
                });

                /* TOC 按钮显示/隐藏逻辑 */
                tocBtn.onclick = (e) => {
                  e.stopPropagation();
                  tocPanel.classList.toggle("show");
                };

                /* 点击空白区域关闭 TOC */
                document.addEventListener("click", (e) => {
                  const target = e.target;
                  if (!tocPanel.contains(target) && !tocBtn.contains(target)) {
                    tocPanel.classList.remove("show");
                  }
                });
              };

              /* =========================
                 Init All
              ========================= */

              const initAll = () => {
                initScrollButton();
                initTOC();
              };

              /* 首次加载 */
              initAll();

              /* Quartz SPA 页面切换后重新生成 TOC */
              document.addEventListener("nav", () => {
                setTimeout(() => {
                  initTOC();
                }, 100);
              });

            })();
          `,
        }}
      />
    </>
  );
};

export default BackToTop;