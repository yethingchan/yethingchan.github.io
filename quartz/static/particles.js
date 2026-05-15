document.addEventListener("DOMContentLoaded", () => {
  if (window.tsParticles) {
    tsParticles.load("tsparticles", {
      fullScreen: {
        enable: false
      },

      background: {
        color: "transparent"
      },

      fpsLimit: 60,

      particles: {
        number: {
          value: 80,
          density: {
            enable: true,
            area: 1000
          }
        },

        color: {
          value: [
            "#38bdf8",
            "#818cf8",
            "#c084fc",
            "#ffffff"
          ]
        },

        shape: {
          type: "circle"
        },

        opacity: {
          value: 0.45
        },

        size: {
          value: {
            min: 1,
            max: 3
          }
        },

        move: {
          enable: true,
          speed: 0.45,
          direction: "none",
          random: false,
          straight: false,
          outModes: {
            default: "out"
          }
        },

        links: {
          enable: true,
          distance: 120,
          color: "#818cf8",
          opacity: 0.18,
          width: 1
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
            distance: 140,

            links: {
              opacity: 0.4
            }
          }
        }
      },

      detectRetina: true
    })
  }
})