import { QuartzComponentConstructor } from "./types"

const CursorGlow: QuartzComponentConstructor = () => {
  return () => (
    <>
      <div className="shooting-star"></div>
      <div className="shooting-star"></div>
      <div className="shooting-star"></div>
      {/* <div id="cursor-glow"></div> */}

      <script
        dangerouslySetInnerHTML={{
          __html: `
            (() => {
              const glow = document.getElementById("cursor-glow");

              if (!glow) return;

              document.addEventListener("mousemove", (e) => {
                glow.style.left = e.clientX + "px";
                glow.style.top = e.clientY + "px";
              });
            })();
          `,
        }}
      />
    </>
  )
}

export default CursorGlow