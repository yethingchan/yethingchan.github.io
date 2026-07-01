import { QuartzComponent, QuartzComponentConstructor, QuartzComponentProps } from "./types"
import style from "./styles/prevNextNav.scss"
import { resolveRelative, simplifySlug, isFolderPath } from "../util/path"
import { QuartzPluginData } from "../plugins/vfile"
import { i18n } from "../i18n"

interface PrevNextNavOptions {
  /**
   * Text to show when there is no previous article
   */
  noPrevText?: string
  /**
   * Text to show when there is no next article
   */
  noNextText?: string
}

const defaultOptions: PrevNextNavOptions = {}

export default ((opts?: Partial<PrevNextNavOptions>) => {
  const options: PrevNextNavOptions = { ...defaultOptions, ...opts }

  /**
   * Get a flat ordered list of all non-index, non-folder files in the trie,
   * sorted by their slug path (which respects filesystem order).
   */
  function getFlatFileList(allFiles: QuartzPluginData[]): QuartzPluginData[] {
    return allFiles
      .filter((file) => {
        if (!file.slug) return false
        // Skip index files and folder pages
        if (isFolderPath(file.slug!)) return false
        return true
      })
      .sort((a, b) => a.slug!.localeCompare(b.slug!))
  }

  const PrevNextNav: QuartzComponent = ({
    fileData,
    allFiles,
    cfg,
  }: QuartzComponentProps) => {
    const currentSlug = simplifySlug(fileData.slug!)
    const flatList = getFlatFileList(allFiles)
    const currentIndex = flatList.findIndex((f) => simplifySlug(f.slug!) === currentSlug)

    const prevFile = currentIndex > 0 ? flatList[currentIndex - 1] : null
    const nextFile =
      currentIndex >= 0 && currentIndex < flatList.length - 1 ? flatList[currentIndex + 1] : null

    const prevTitle = prevFile?.frontmatter?.title
    const nextTitle = nextFile?.frontmatter?.title

    return (
      <nav class="prev-next-nav">
        {prevFile ? (
          <a href={resolveRelative(fileData.slug!, prevFile.slug!)} class="prev-next-link prev">
            <span class="prev-next-direction">
              {i18n(cfg.locale).components.prevNextNav.previous ?? "上一章"}
            </span>
            <span class="prev-next-title">{prevTitle}</span>
          </a>
        ) : (
          <div class="prev-next-link prev disabled">
            <span class="prev-next-direction">
              {i18n(cfg.locale).components.prevNextNav.previous ?? "上一章"}
            </span>
            <span class="prev-next-title">
              {options.noPrevText ?? (i18n(cfg.locale).components.prevNextNav.noPrev ?? "已是第一章")}
            </span>
          </div>
        )}
        {nextFile ? (
          <a href={resolveRelative(fileData.slug!, nextFile.slug!)} class="prev-next-link next">
            <span class="prev-next-direction">
              {i18n(cfg.locale).components.prevNextNav.next ?? "下一章"}
            </span>
            <span class="prev-next-title">{nextTitle}</span>
          </a>
        ) : (
          <div class="prev-next-link next disabled">
            <span class="prev-next-direction">
              {i18n(cfg.locale).components.prevNextNav.next ?? "下一章"}
            </span>
            <span class="prev-next-title">
              {options.noNextText ?? (i18n(cfg.locale).components.prevNextNav.noNext ?? "已是最后一章")}
            </span>
          </div>
        )}
      </nav>
    )
  }

  PrevNextNav.css = style

  return PrevNextNav
}) satisfies QuartzComponentConstructor
