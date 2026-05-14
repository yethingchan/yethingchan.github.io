import { QuartzComponent, QuartzComponentConstructor, QuartzComponentProps } from "./types"
import { classNames } from "../util/lang"

const ArticleTitle: QuartzComponent = ({
  fileData,
  displayClass,
}: QuartzComponentProps) => {
  const title = fileData.frontmatter?.title
  const cover = fileData.frontmatter?.cover

  if (title) {
    return (
      <>
        {/* Article Cover */}
        {cover && (
          <div class="article-cover">
           <img src={String(cover)} alt={title} />
          </div>
        )}

        {/* Article Title */}
        <h1 class={classNames(displayClass, "article-title")}>
          {title}
        </h1>
      </>
    )
  } else {
    return null
  }
}

ArticleTitle.css = `
.article-title {
  margin: 2rem 0 0 0;
}

/* ==============================
   Article Cover
   ============================== */

.article-cover {
  width: 100%;
  margin-bottom: 1.5rem;
  border-radius: 22px;
  overflow: hidden;
  position: relative;
}

.article-cover img {
  width: 100%;
  height: 320px;
  object-fit: cover;
  display: block;

  transform: scale(1.01);

  transition:
    transform 0.6s ease,
    filter 0.6s ease;
}

/* Hover 微动效 */
.article-cover:hover img {
  transform: scale(1.03);
}

/* 暗色模式 */
:root[saved-theme="dark"] .article-cover img {
  filter:
    brightness(0.88)
    saturate(1.12)
    contrast(1.05);
}

/* 移动端 */
@media (max-width: 800px) {
  .article-cover img {
    height: 220px;
  }

  .article-cover {
    border-radius: 16px;
  }
}
`

export default (() => ArticleTitle) satisfies QuartzComponentConstructor