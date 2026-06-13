import { QuartzComponentConstructor } from "./types"

const StarryBackground: QuartzComponentConstructor = () => {
  return () => (
    <>
      <div id="starry-background"></div>

      <script
        type="module"
        dangerouslySetInnerHTML={{
          __html: `
            import { tsParticles } from "https://cdn.jsdelivr.net/npm/tsparticles-engine@2/+esm";
            import { loadSlim } from "https://cdn.jsdelivr.net/npm/tsparticles-slim@2/+esm";

            async function initParticles() {
              const container = document.getElementById("starry-background");

              if (!container) return;

              // 防止 Quartz SPA 重复初始化
              if (container.dataset.loaded === "true") return;

              container.dataset.loaded = "true";

              try {
                await loadSlim(tsParticles);

                await tsParticles.load({
                  id: "starry-background",
                  options: {
                  fullScreen: {
                    enable: false
                  },

                  background: {
                    color: "transparent"
                  },

                  fpsLimit: 60,

                  particles: {
                    number: {
                      value: 90,
                      density: {
                        enable: true,
                        area: 1000
                      }
                    },

                    color: {
                      value: [
                        "#ffffff",
                        "#c4b5fd",
                        "#93c5fd",
                        "#67e8f9"
                      ]
                    },

                    shape: {
                      type: "circle"
                    },

                    opacity: {
                      value: {
                        min: 0.1,
                        max: 0.8
                      },
                      animation: {
                        enable: true,
                        speed: 0.8,
                        minimumValue: 0.1
                      }
                    },

                    size: {
                      value: {
                        min: 1,
                        max: 3
                      }
                    },

                    move: {
                      enable: true,
                      speed: 0.25,
                      direction: "none",
                      random: true,
                      straight: false,
                      outModes: {
                        default: "out"
                      }
                    },

                    links: {
                      enable: false
                    }
                  },

                  interactivity: {
                    events: {
                      onHover: {
                        enable: true,
                        mode: "grab"
                      },

                      resize: true
                    },

                    modes: {
                      grab: {
                        distance: 120,
                        links: {
                          opacity: 0.18
                        }
                      }
                    }
                  },

                  detectRetina: true
                }
              });
              } catch (e) {
                console.warn("StarryBackground: CDN 加载失败，粒子效果已禁用", e);
              }
            }

            initParticles();

            document.addEventListener("nav", () => {
              setTimeout(initParticles, 50);
            });
          `,
        }}
      />
    </>
  )
}

export default StarryBackground