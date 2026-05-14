import { QuartzComponent, QuartzComponentConstructor, QuartzComponentProps } from "./types"
import style from "./styles/explorer.scss"

// @ts-ignore
import script from "./scripts/explorer.inline"
import { classNames } from "../util/lang"
import { i18n } from "../i18n"
import { FileTrieNode } from "../util/fileTrie"
import OverflowListFactory from "./OverflowList"
import { concatenateResources } from "../util/resources"

type OrderEntries = "sort" | "filter" | "map"

export interface Options {
  title?: string
  folderDefaultState: "collapsed" | "open"
  folderClickBehavior: "collapse" | "link"
  useSavedState: boolean
  sortFn: (a: FileTrieNode, b: FileTrieNode) => number
  filterFn: (node: FileTrieNode) => boolean
  mapFn: (node: FileTrieNode) => void
  order: OrderEntries[]
}

const defaultOptions: Options = {
  folderDefaultState: "collapsed",

  /* 点击文件夹 = 展开/折叠 */
  folderClickBehavior: "collapse",

  /* 禁止 Quartz 记忆目录状态 */
  useSavedState: false,

  mapFn: (node) => {
    return node
  },

  sortFn: (a, b) => {
    // 文件夹优先，然后按字母排序
    if ((!a.isFolder && !b.isFolder) || (a.isFolder && b.isFolder)) {
      return a.displayName.localeCompare(b.displayName, undefined, {
        numeric: true,
        sensitivity: "base",
      })
    }

    if (!a.isFolder && b.isFolder) {
      return 1
    } else {
      return -1
    }
  },

  filterFn: (node) => node.slugSegment !== "tags",

  order: ["filter", "map", "sort"],
}

export type FolderState = {
  path: string
  collapsed: boolean
}

let numExplorers = 0

export default ((userOpts?: Partial<Options>) => {
  const opts: Options = { ...defaultOptions, ...userOpts }

  const { OverflowList, overflowListAfterDOMLoaded } = OverflowListFactory()

  const Explorer: QuartzComponent = ({ cfg, displayClass }: QuartzComponentProps) => {
    const id = `explorer-${numExplorers++}`

    return (
      <div
        class={classNames(displayClass, "explorer")}
        data-behavior={opts.folderClickBehavior}
        data-collapsed={opts.folderDefaultState}
        data-savestate={opts.useSavedState}
        data-data-fns={JSON.stringify({
          order: opts.order,
          sortFn: opts.sortFn.toString(),
          filterFn: opts.filterFn.toString(),
          mapFn: opts.mapFn.toString(),
        })}
      >
        {/* Mobile Explorer */}
        <button
          type="button"
          class="explorer-toggle mobile-explorer hide-until-loaded"
          data-mobile={true}
          aria-controls={id}
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="24"
            height="24"
            viewBox="0 0 24 24"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            class="lucide-menu"
          >
            <line x1="4" x2="20" y1="12" y2="12" />
            <line x1="4" x2="20" y1="6" y2="6" />
            <line x1="4" x2="20" y1="18" y2="18" />
          </svg>
        </button>

        {/* Explorer Header */}
        <div class="explorer-header">
          <button
            type="button"
            class="title-button explorer-toggle desktop-explorer"
            data-mobile={false}
            aria-expanded={true}
          >
            <h2>{opts.title ?? i18n(cfg.locale).components.explorer.title}</h2>

            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="14"
              height="14"
              viewBox="5 8 14 8"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              class="fold"
            >
              <polyline points="6 9 12 15 18 9"></polyline>
            </svg>
          </button>

          {/* 一键折叠 */}
          <button id="explorer-collapse-all">
            ⤢
          </button>
        </div>

        {/* Explorer Content */}
        <div id={id} class="explorer-content" aria-expanded={false} role="group">
          <OverflowList class="explorer-ul" />
        </div>

        {/* 文件模板 */}
        <template id="template-file">
          <li>
            <a href="#"></a>
          </li>
        </template>

        {/* 文件夹模板 */}
        <template id="template-folder">
          <li>
            <div class="folder-container">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="12"
                height="12"
                viewBox="5 8 14 8"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
                class="folder-icon"
              >
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>

              <div>
                <button class="folder-button">
                  <span class="folder-title"></span>
                </button>
              </div>
            </div>

            <div class="folder-outer">
              <ul class="content"></ul>
            </div>
          </li>
        </template>
      </div>
    )
  }

  Explorer.css = style

  Explorer.afterDOMLoaded = concatenateResources(
    script,
    overflowListAfterDOMLoaded,

    `
(() => {

  function initExplorerCollapse() {

    const btn = document.getElementById("explorer-collapse-all")

    if (!btn) return

    let collapsed = false

    btn.onclick = () => {

      const folders = document.querySelectorAll(".folder-outer")

      folders.forEach((folder) => {

        if (collapsed) {
          folder.classList.add("open")
        } else {
          folder.classList.remove("open")
        }

      })

      collapsed = !collapsed

      btn.innerText = collapsed ? "⤡" : "⤢"
    }
  }

  initExplorerCollapse()

  document.addEventListener("nav", () => {
    setTimeout(initExplorerCollapse, 100)
  })

})()
`,
  )

  return Explorer
}) satisfies QuartzComponentConstructor