# 08 · 常见问题 FAQ

## 前端相关

**Q1：npm run dev 白屏？**
- 看 Console 报错；确认 `npm install` 已执行；Node 版本用 22。

**Q2：刷新子路由 404？**
- Nginx 漏 `try_files $uri $uri/ /index.html`。开发态 Vite 不会（它处理了 history）。

**Q3：接口一直 404？**
- 开发态：检查 `vite.config.js` 的 `proxy.target` 是否 8080，且后端路由没带 `/api`。
- 生产态：Nginx `proxy_pass` 末尾是否带 `/`（用于剥离 `/api`）。

**Q4：网络请求报 CORS？**
- 开发态请求必须走 `/api`（被代理消跨域）；别直接打 8080 且没配 CORS。

**Q5：表格没数据？**
- 数据在 `res.data.rows`，不是 `res.rows`。

## 后端相关

**Q6：启动时连不上 MySQL？**
- MySQL 没起 / 库 `ruoyi` 没建 / 没 `source` 两个 SQL / `application.yml` 账号密码错。

**Q7：登录后菜单空白？**
- `/getRouters` 没返回；或 `sys_menu` 的 `component` 路径与 `views/` 不符。

**Q8：按钮点了报 403？**
- 后端 `@PreAuthorize` 权限串与数据库 `perms` 不一致。

**Q9：分页 count 报错？**
- 本项目已固定 MySQL 方言（之前修过 H2 方言 bug）。若仍报错，确认 `MybatisPlusConfig` 是 MySQL 方言。

## 部署相关

**Q10：Linux 起不来 jar？**
- `java -version` 是否 17+；端口是否被占；MySQL 是否可达。

**Q11：HTTPS 后接口还是 http？**
- 反代加 `proxy_set_header X-Forwarded-Proto https;`，让后端知道原始协议。

**Q12：改了代码怎么生效？**
- 前端：`npm run build` 重新生成 `dist/` 并覆盖。
- 后端：重新 `mvn package` 生成 jar，重启服务。

## 通用

**Q13：前后端契约对不上？**
- 统一看 `{code,msg,data}`；列表 `{rows,total}`；权限串 `业务:模块:动作` 三处一致。

**Q14：想快速验证后端？**
- `curl http://localhost:8080/captcha` 和 `/login`（注意直连后端**不带 /api**）。

## 小结

大部分问题集中在：跨域/代理前缀、MySQL 建库、权限三处一致、数据取 `res.data`、刷新 404 补 `try_files`。按 04 分册 21 篇决策树逐层定位。

---

## 全教程完结 🎉

你已读完 **Vue3系统教程** 全部 5 个分册（共约 90 篇）。从 Vue 原理 → 前端工程 → 前后端对接 → 部署上线 → 实战案例，形成完整闭环。

建议把本教程当作"案头手册"，开发中遇到对应主题就回来查对应分册。
