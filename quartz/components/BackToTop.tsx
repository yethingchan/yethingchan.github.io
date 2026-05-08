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
        let clockTimer = null;

        function init() {
          if (clockTimer) clearInterval(clockTimer);
          
          const scrollBtn = document.getElementById("scroll-toggle");
          const scrollIcon = document.getElementById("scroll-icon");
          const clockBtn = document.getElementById("floating-clock");
          const tocPanel = document.getElementById("floating-toc");
          const tocContent = document.getElementById("floating-toc-content");

          /* 扇形阅读进度 + 箭头切换 */
          const updateScroll = () => {
            const scrollTop = window.scrollY;
            const scrollHeight = document.body.scrollHeight - window.innerHeight;
            const progress = Math.min(scrollTop / scrollHeight, 1);

            // 核心：更新扇形进度CSS变量
            if(scrollBtn) {
              scrollBtn.style.setProperty('--progress', progress);
            }

            // 箭头切换
            if(scrollIcon) {
              scrollIcon.innerHTML = scrollTop < 200 ? "▼" : "▲";
            }
          };

          // 点击滚动到顶部/底部
          if(scrollBtn) {
            scrollBtn.onclick = () => {
              window.scrollTo({
                top: window.scrollY < 200 ? document.body.scrollHeight : 0,
                behavior: "smooth",
              });
            };
          }

          window.addEventListener("scroll", updateScroll);
          updateScroll();

          /* 时钟功能 */
          const updateClock = () => {
            if(!clockBtn) return;
            const now = new Date();
            const hh = String(now.getHours()).padStart(2,'0');
            const mm = String(now.getMinutes()).padStart(2,'0');
            clockBtn.innerText = hh + ':' + mm;
          };
          updateClock();
          clockTimer = setInterval(updateClock, 60000);

          /* TOC功能 */
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
              if(!tocPanel.contains(e.target) && !clockBtn.contains(e.target)) {
                tocPanel.classList.remove("show");
              }
            });
          }
        }

        init();
        document.addEventListener("nav", () => {
          setTimeout(() => {
            init();
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