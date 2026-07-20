---
title: XSS 与 SQL 注入过滤
---

# 10-2 XSS 消毒 + SQL 注入防护

> 上接：[[SpringBoot+Vue3后台搭建/10-安全防护/01-密码加密与防暴破IP名单]]
> [[../03-后端基础框架/04-跨域CORS与XSS防护]] 的 4.2 给了 XSS 包装的**骨架**，但 `clean()` 用的是手写正则——生产不够。这里升级成 **OWASP 标准消毒**，并补上 SQL 注入这条独立防线。

## 2.1 XSS：用 OWASP Encoder 替代手写正则

手写正则永远有漏网（编码绕过、嵌套标签…）。**生产请直接用 OWASP 官方库**。

`pom.xml` 加依赖：
```xml
<dependency>
  <groupId>org.owasp.encoder</groupId>
  <artifactId>encoder</artifactId>
  <version>1.2.3</version>
</dependency>
```

改 `XssHttpServletRequestWrapper.clean()`（其余包装代码同 03-4 的 4.2）：

```java
import org.owasp.encoder.Encode;

private String clean(String v) {
    if (v == null) return null;
    // forHtml：把 < > & " ' 转义成实体，浏览器当文本渲染，绝不执行
    return Encode.forHtml(v);
}
```

- `Encode.forHtml(value)`：用于普通文本字段（昵称、备注…）。
- `Encode.forHtmlAttribute(value)`：用于塞进 HTML **属性**的值。
- `Encode.forJavaScript(value)`：用于嵌进 JS 字符串。
- 不同上下文用不同方法，这是 OWASP 的"按上下文编码"原则。

> 若字段要支持**富文本**（如公告正文），不能简单转义——要用 **Google `html.sanitizer`**（白名单策略，只允许 `<b>/<p>/<img>` 等安全标签，过滤 `onclick`/`script`）。简单转义会把合法格式也干掉。

## 2.2 前端：v-html 是 XSS 重灾区

Vue 的 `{{ }}` 文本插值**默认转义**，安全。危险的是 `v-html`：

```vue
<!-- ❌ 危险：内容含 <script> 会执行 -->
<div v-html="userComment"></div>

<!-- ✅ 安全：文本插值，Vue 自动转义 -->
<div>{{ userComment }}</div>
```

结论：**尽量不用 `v-html`**；必须用时，内容必须来自可信源（自己后端已消毒的富文本），且后端过了 `html.sanitizer`。

## 2.3 SQL 注入：为什么 MP 天然免疫（大部分）

SQL 注入根因是**把用户输入拼进 SQL 字符串**。MyBatis-Plus 的 `QueryWrapper` / `#{}` 用**预编译参数（? 占位符）**，用户输入永远当"数据"不当"代码"：

```java
// ✅ 安全：eq 走预编译参数，' 等特殊字符被转义，无法注入
QueryWrapper<SysUser> w = new QueryWrapper<>();
w.eq("user_name", userInput);   // 即使 userInput = "admin' OR '1'='1" 也只是个普通值

// ❌ 致命：用 ${} 拼接用户输入 → 注入漏洞
// @Select("SELECT * FROM sys_user WHERE user_name = '${name}'")  ← 永远别这么写
```

**铁律**
1. MyBatis 里**只用 `#{}`，绝不用 `${}`** 接任何用户输入（`${}` 只用于排序字段等白名单值，且需后端校验）。
2. MP 的 `QueryWrapper`/`LambdaQueryWrapper` 全程参数化，放心用。
3. 必须写原生 SQL 时，用 `?` + `PreparedStatement`，不拼字符串。

## 2.4 SQL 注入关键词过滤（纵深防御）

即便用了 MP，仍可在入口加一道"关键词拦截"兜住手滑：

```java
public class SqlInjectionFilter implements Filter {
    // 常见注入特征（防御性，非唯一手段）
    private static final String[] BAD = {
        "select ", "insert ", "update ", "delete ", "drop ", "truncate ",
        "exec ", "or 1=1", "and 1=1", "' or '", "--", "/*", "union "
    };
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest r = (HttpServletRequest) req;
        String q = r.getQueryString();
        if (q != null && containsSql(q)) {
            ((HttpServletResponse) res).sendError(400, "非法请求");
            return;
        }
        chain.doFilter(req, res);
    }
    private boolean containsSql(String s) {
        String lower = s.toLowerCase();
        return Arrays.stream(BAD).anyMatch(lower::contains);
    }
}
```

> 这层是"双保险"，**不能替代** 2.3 的预编译。真正兜底的是参数化查询。

## 验证清单

- [ ] 输入 `<img src=x onerror=alert(1)>` → 库里是 `&lt;img...` 实体，页面不弹窗。
- [ ] 富文本公告经 `html.sanitizer` 后，合法 `<b>` 保留、`<script>` 被删。
- [ ] 把 `user_name` 设成 `admin' OR '1'='1`，用 `QueryWrapper.eq` 查不到注入效果（只当普通串）。
- [ ] 代码全仓搜 `${`，确认没有任何 `${}` 拼接用户输入。

> 下一步：[[../10-安全防护/03-Token鉴权强化与防重复提交]] 让 token 更难被盗用，并把防重复提交完整注册。
