---
title: 跨域完善 / XSS 防护 / 防重复提交
---

# 03-4 跨域完善 / XSS 防护 / 防重复提交

> 上接：[[SpringBoot+Vue3后台搭建/03-后端基础框架/03-SpringSecurity与JWT鉴权]]

## 4.1 跨域再完善（生产级）
[[../01-工程初始化/03-前后端联调与跨域]] 的 `CorsFilter` 是"放行所有源"。生产应**限定具体域名**并开放自定义头（前端可能带 `Authorization`/`X-Token`）：

```java
@Bean
public CorsFilter corsFilter() {
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowCredentials(true);
    // 生产写具体前端域名，别用 *
    c.setAllowedOriginPatterns(List.of("https://admin.example.com"));
    c.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Token", "X-Requested-With"));
    c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
    c.setExposedHeaders(List.of("Content-Disposition")); // 让前端能读下载头
    c.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", c);
    return new CorsFilter(src);
}
```
**讲解**：`setExposedHeaders` 很关键——文件下载接口设了 `Content-Disposition: attachment`，前端 axios 想读它必须后端"暴露"出来，否则取不到文件名。

## 4.2 XSS 防护（输入消毒）

**原理**：攻击者在输入框塞 `<script>alert(1)</script>`，若原样存库、再原样回显到页面，浏览器会执行。防护在**入口处把尖括号转义**成 `&lt;script&gt;`（纯文本，不执行）。

```java
// 1) 过滤器的注册
@Configuration
public class XssConfig {
    @Bean
    public FilterRegistrationBean<XssFilter> xssFilter() {
        FilterRegistrationBean<XssFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new XssFilter());
        bean.addUrlPatterns("/*");           // 对所有请求生效
        bean.setName("xssFilter");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1); // 尽早执行（在 Security 之前）
        return bean;
    }
}

// 2) 过滤器：用包装类替换 request
public class XssFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        // 只对普通表单/查询参数包装；上传等二进制放行
        if (!(request instanceof HttpServletRequestWrapper)
                && !"multipart".equals(request.getContentType())) {
            chain.doFilter(new XssHttpServletRequestWrapper(request), res);
        } else {
            chain.doFilter(req, res);
        }
    }
}

// 3) 包装类：重写取值方法，做转义
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {
    private static final String REGEX = "<script.*?>.*?</script>|<.*?javascript:.*?>|onload=.*?=|<.*?/>";
    public XssHttpServletRequestWrapper(HttpServletRequest request) { super(request); }

    @Override public String getParameter(String name) { return clean(super.getParameter(name)); }
    @Override public String[] getParameterValues(String name) {
        String[] vals = super.getParameterValues(name);
        if (vals == null) return null;
        return Arrays.stream(vals).map(this::clean).toArray(String[]::new);
    }
    @Override public String getHeader(String name) { return clean(super.getHeader(name)); }

    private String clean(String v) {
        if (v == null) return null;
        // 简化版：移除 script/onload/javascript: 等危险片段
        // 生产请用 OWASP Java Encoder 或 org.owasp.html.sanitizer 做标准消毒
        return v.replaceAll("(?i)<script.*?>.*?</script>", "")
                .replaceAll("(?i)javascript:", "")
                .replaceAll("(?i)on(load|click|error)=", "");
    }
}
```
**讲解**
- **包装模式（Wrapper）**是 Servlet 过滤的标准套路：不修改原 req，而是套一层"取值时自动净化"。
- 这里用正则**简化演示**。**真实生产请用 OWASP `org.owasp.encoder.Encode.forHtml()` 或 Google `html.sanitizer`**，自己写正则极易漏。
- 注意只对 `application/x-www-form-urlencoded`/`json` 等文本参数处理，**文件上传（`multipart`）必须放行**，否则上传炸。

## 4.3 防重复提交（接口幂等）

**场景**：用户手抖连点两次"提交"，后端建出两条重复数据。

```java
// 注解
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RepeatSubmit {
    int interval() default 5;   // 多少秒内禁止重复
    String message() default "不允许重复提交";
}

// 拦截器
@Component
public class RepeatSubmitInterceptor implements HandlerInterceptor {
    @Resource private RedisCache redisCache;
    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        if (handler instanceof HandlerMethod hm) {
            RepeatSubmit anno = hm.getMethodAnnotation(RepeatSubmit.class);
            if (anno != null) {
                String key = "repeat:" + SecurityUtils.getUserId() + ":" + req.getRequestURI()
                        + ":" + req.getHeader("X-Repeat-Token"); // 前端每次请求带唯一 token
                if (redisCache.getCacheObject(key) != null) {
                    throw new BusinessException(anno.message()); // 命中 → 拒绝
                }
                redisCache.setCacheObject(key, "1", anno.interval(), TimeUnit.SECONDS);
            }
        }
        return true;
    }
}
```
**讲解**：用 `用户ID + URL + 前端唯一 token` 做 Redis 锁，N 秒内同 key 直接拒。前端每次请求 header 带一个随机 `X-Repeat-Token`（每次不同），保证"同一操作"可被识别。

## 验证清单
- [ ] 输入 `<script>` 提交后，库里/回显的是转义文本，弹不出 alert。
- [ ] 连点两次提交，第二次被 `BusinessException("不允许重复提交")` 拦截。
- [ ] 文件上传接口不受 XSS 包装影响（能正常传文件）。

> 地基（03 章）全部完成。**重头戏在 [[../04-权限管理模块/00-索引]]：把 RBAC 五张表用代码跑起来。**
