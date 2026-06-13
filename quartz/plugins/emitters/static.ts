import { FilePath, QUARTZ, joinSegments } from "../../util/path"
import { QuartzEmitterPlugin } from "../types"
import fs from "fs"
import { glob } from "../../util/glob"
import { dirname } from "path"

export const Static: QuartzEmitterPlugin = () => ({
  name: "Static",
  async *emit({ argv, cfg }) {
    const staticPath = joinSegments(QUARTZ, "static")
    const fps = await glob("**", staticPath, cfg.configuration.ignorePatterns)
    const outputStaticPath = joinSegments(argv.output, "static")
    await fs.promises.mkdir(outputStaticPath, { recursive: true })
    for (const fp of fps) {
      const src = joinSegments(staticPath, fp) as FilePath
      const dest = joinSegments(outputStaticPath, fp) as FilePath
      await fs.promises.mkdir(dirname(dest), { recursive: true })
      await fs.promises.copyFile(src, dest)
      yield dest
    }
    // robots.txt 需要放在站点根目录，复制到 output 根目录
    const robotsSrc = joinSegments(staticPath, "robots.txt") as FilePath
    if (fs.existsSync(robotsSrc)) {
      const robotsDest = joinSegments(argv.output, "robots.txt") as FilePath
      await fs.promises.copyFile(robotsSrc, robotsDest)
      yield robotsDest
    }
  },
  async *partialEmit() {},
})
