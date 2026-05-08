import { QuartzComponentConstructor } from "./types";

const BackToTop: QuartzComponentConstructor = () => {
  return () => (
    <>
      <div id="floating-stack">
        {/* 浮动时间按钮 - 点击显示 TOC */}
        <div id="floating-clock">--:--</div>

        {/* 上下滚动按钮 */}
        <button id="scroll-toggle" aria-label="Scroll Toggle">
          <div className="progress-ring"></div>
          <span id="scroll-icon">▲</span>
        </button>
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
              // 存储定时器ID，用于切换页面时清除
              let clockTimer = null;

              /* =========================
                 初始化函数（核心修复：每次导航都重新执行）
              ========================= */
              function init() {
                // 1. 清除旧定时器，避免重复计时
                if (clockTimer) clearInterval(clockTimer);
                
                // 获取最新的DOM元素（修复切换页面后元素失效）
                const scrollBtn = document.getElementById("scroll-toggle");
                const scrollIcon = document.getElementById("scroll-icon");
                const progressRing = scrollBtn?.querySelector(".progress-ring");
                const clockBtn = document.getElementById("floating-clock");
                const tocPanel = document.getElementById("floating-toc");
                const tocContent = document.getElementById("floating-toc-content");

                /* =========================
                   Scroll Button + Progress Ring
                ========================= */
                const updateScroll = () => {
                  const scrollTop = window.scrollY;
                  const scrollHeight = document.body.scrollHeight - window.innerHeight;
                  const progress = Math.min(scrollTop / scrollHeight, 1);

                  if(progressRing){
                    progressRing.style.setProperty('--progress', progress);
                  }

                  if(scrollTop < 200){
                    scrollIcon.innerHTML = "▼";
                  } else {
                    scrollIcon.innerHTML = "▲";
                  }
                };

                if(scrollBtn) {
                  scrollBtn.onclick = () => {
                    const atTop = window.scrollY < 200;
                    window.scrollTo({
                      top: atTop ? document.body.scrollHeight : 0,
                      behavior: "smooth",
                    });
                  };
                  window.addEventListener("scroll", updateScroll);
                  updateScroll();
                }

                /* =========================
                   Floating Clock + TOC（修复时钟切换页面失效）
                ========================= */
                const updateClock = () => {
                  if(!clockBtn) return;
                  const now = new Date();
                  const hh = String(now.getHours()).padStart(2,'0');
                  const mm = String(now.getMinutes()).padStart(2,'0');
                  clockBtn.innerText = hh + ':' + mm;
                };
                
                // 立即更新时间 + 启动定时器
                updateClock();
                clockTimer = setInterval(updateClock, 60000);

                /* =========================
                   TOC 功能
                ========================= */
                if(clockBtn && tocPanel && tocContent) {
                  clockBtn.onclick = (e) => {
                    e.stopPropagation();
                    tocContent.innerHTML = "";
                    const headings = document.querySelectorAll("article h1, article h2, article h3");
                    headings.forEach((heading,index)=>{
                      if(!heading.id) heading.id = "heading-"+index;
                      const a = document.createElement("a");
                      a.href = "#"+heading.id;
                      a.textContent = heading.innerText;
                      a.className = "toc-item toc-"+heading.tagName.toLowerCase();
                      a.onclick = ()=>tocPanel.classList.remove("show");
                      tocContent.appendChild(a);
                    });
                    tocPanel.classList.toggle("show");
                  };

                  document.addEventListener("click",(e)=>{
                    if(!tocPanel.contains(e.target) && !clockBtn.contains(e.target)){
                      tocPanel.classList.remove("show");
                    }
                  });
                }
              }

              // 首次加载初始化
              init();

              // 🔥 核心修复：监听 Quartz 页面切换事件，重新初始化
              document.addEventListener("nav", () => {
                setTimeout(() => {
                  init();
                  // 切换页面自动关闭TOC
                  const tocPanel = document.getElementById("floating-toc");
                  if(tocPanel) tocPanel.classList.remove("show");
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