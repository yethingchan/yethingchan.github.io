# 08 · 权限 perms 校验对接

权限 = 后端 `@PreAuthorize` + 数据库 `sys_menu.perms` + 前端 `v-hasPermi`，**三处串同一个权限串**。

## 权限串规范

格式：`业务:模块:动作`，如：
- `system:user:add`
- `system:user:edit`
- `system:user:del`
- `system:user:list`
- `system:user:export`

## 后端：接口加注解

```java
@RestController
@RequestMapping("/system/user")
public class SysUserController {
    @PreAuthorize("hasAuthority('system:user:list')")
    @GetMapping("/list")
    public TableDataInfo list() { ... }

    @PreAuthorize("hasAuthority('system:user:add')")
    @PostMapping
    public AjaxResult add(@RequestBody SysUser user) { ... }
}
```

> 后端不校验也能跑，但**加了才是真安全**：没权限的人直接调接口会被拒（返回 code=403）。

## 数据库：`sys_menu.perms`

每个按钮级菜单在 `sys_menu` 表有一条 `perms = 'system:user:add'` 的记录。角色关联菜单后，用户登录时后端把这些 `perms` 汇总成 `permissions` 数组返回给 `/getInfo`。

## 前端：`v-hasPermi`

```vue
<el-button v-hasPermi="'system:user:add'">新增</el-button>
```

无权限时 DOM 被移除（见 02 分册 18 篇）。

## 三处一致性矩阵

| 位置 | 写法 | 来源 |
|------|------|------|
| 后端注解 | `hasAuthority('system:user:add')` | 写死在代码 |
| 数据库 | `sys_menu.perms = 'system:user:add'` | 建表/菜单管理 |
| 前端指令 | `v-hasPermi="'system:user:add'"` | 写死在模板 |

三者字符串**逐字符一致**才生效。

## 常见故障

- 按钮不显示但接口能调 → 数据库 `perms` 没配，或 `getInfo` 没返回该权限。
- 按钮显示但点了报 403 → 后端注解的权限串与数据库不一致。
- 全站按钮都不显示 → `/getInfo` 没返回 `permissions` 字段。

## 小结

权限三件套：后端 `@PreAuthorize`、库里 `perms`、前端 `v-hasPermi`，权限串必须一模一样。

下一篇：[跨域 CORS 处理](./09-跨域CORS处理.md)
