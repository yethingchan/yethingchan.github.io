# 04 · Vite 配置详解

本项目 `frontend/vite.config.js`（已核对）：

```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/api/, '')
      }
    }
  }
})
```

## 逐段解析

### `plugins: [vue()]`
让 Vite 能编译 `.vue` 单文件组件。

### `resolve.alias['@']`
- `@` → `./src`，所以 `@/api/login` 等价于 `src/api/login`。
- 全项目都用 `@` 别名，**不要写相对路径 `../../../`**。

### `server.port: 3000`
开发服务器端口。`npm run dev` 后访问 `http://localhost:3000`。

### `server.proxy['/api']`（**核心**）
解决开发态跨域：
```
前端请求 /api/login
  → Vite 代理拦截
  → 转发到 http://localhost:8080/login   （注意：/api 被 rewrite 剥离）
  → 后端 Controller 看到的是 /login（不带 /api）
```

- `target`：后端地址（本机 8080）。
- `changeOrigin: true`：修改请求头 host，避免后端校验 host 报错。
- `rewrite`：把 `/api` 前缀去掉，所以**后端路由不要带 `/api`**。

## 没有配置的部分（重要）

- 没配 `build.outDir` → 默认 `dist/`。
- 没配 `base` → 默认 `''`（相对路径部署友好，可放任意子目录）。
- 没配 `preview` 端口 → 默认 4173。

## 生产部署说明（详见 04-部署分册）

生产环境 Vite 代理**不生效**（代理只在 `npm run dev` 时）。生产靠 **Nginx** 把 `/api` 反代到后端：

```nginx
location /api/ {
    proxy_pass http://localhost:8080/;   # 末尾 / 会剥离 /api
}
```

## 小结

`vite.config.js` 三件事：编译 `.vue`、配 `@` 别名、开发态代理 `/api → 8080`。生产态的反代交给 Nginx。

下一篇：[入口文件 main.js 解析](./05-入口文件main.js解析.md)
