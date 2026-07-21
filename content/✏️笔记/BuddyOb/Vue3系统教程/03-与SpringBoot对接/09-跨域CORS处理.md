# 09 · 跨域 CORS 处理

## 什么是跨域

浏览器出于安全，**同源策略**禁止页面向不同源（协议/域名/端口任一不同）发请求。

- 开发态：前端 `http://localhost:3000` 调后端 `http://localhost:8080` → **端口不同 → 跨域**。
- 生产态：前端 `http://域名` 调后端 `http://域名:8080` 或 `http://api域名` → 也可能跨域。

## 开发态：Vite 代理（本项目已解决）

`vite.config.js` 的 `proxy` 把 `/api` 转发到 8080，**对浏览器来说请求还是发往 3000**（同源），跨域被代理消掉：

```js
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
    rewrite: p => p.replace(/^\/api/, '')
  }
}
```

> 所以**开发态不需要后端配 CORS**（前提是前端请求都走 `/api` 前缀）。

## 生产态：两种解法

### 方案 A：Nginx 反代（推荐，本项目采用）

前端和后端同域（都走 Nginx），浏览器无跨域：

```nginx
location /api/ { proxy_pass http://localhost:8080/; }
```

### 方案 B：后端开 CORS（前后端不同域时）

后端加 CORS 配置（Spring Boot）：

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
```

## OPTIONS 预检

带 token 的非简单请求，浏览器先发 `OPTIONS` 预检。后端必须放行 `OPTIONS`（Spring Security 白名单加 `OPTIONS`，或 CORS 配置覆盖）。本项目 JWT 过滤器需注意放行预检。

## 小结

开发态靠 Vite 代理消跨域；生产态靠 Nginx 同域反代（本项目采用）或后端开 CORS。本项目前端统一 `/api` 前缀是前提。

下一篇：[分页查询参数对接](./10-分页查询参数对接.md)
