import { QuartzComponent, QuartzComponentConstructor, QuartzComponentProps } from "./types"
import style from "./styles/footer.scss"
import { version } from "../../package.json"
import { i18n } from "../i18n"

interface Options {
  links: Record<string, string>
}

export default ((opts?: Options) => {
  const Footer: QuartzComponent = ({ displayClass, cfg }: QuartzComponentProps) => {
    const year = new Date().getFullYear()
    // const links = opts?.links ?? [("https://yethingchan.github.io/")]
    return (
      <footer class={`${displayClass ?? ""}`}>

        <p>© {year} <a href="https://yethingchan.github.io/">Yethingchan </a> | 基于 <a href="https://quartz.jzhao.xyz/">Quartz v{version}</a>  构建</p>
        <b><u>   风起于青萍之末，浪成于微澜之间   </u></b>
      </footer>
    )
  }

  Footer.css = style
  return Footer
}) satisfies QuartzComponentConstructor
