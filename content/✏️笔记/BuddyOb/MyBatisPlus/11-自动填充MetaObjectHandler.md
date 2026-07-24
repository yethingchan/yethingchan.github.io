---
title: 11-自动填充MetaObjectHandler
---

# 11 自动填充（MetaObjectHandler）

> 上接：[[MyBatisPlus/10-主键策略与雪花算法]]
> `createTime`/`updateTime`/`createBy` 几乎每张表都要。MP 的**字段自动填充**让你插/改时不用手 set，由拦截器统一注入。

## 11.1 实体标注 fill

```java
public class SysUser {
    @TableField(fill = FieldFill.INSERT)          // 仅插入时填
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)     // 插入+更新都填
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;                         // 当前登录人
}
```
> 只标 `@TableField(fill=...)` **不够**，还得写下面的 Handler 真正注入值，否则字段是 null。

## 11.2 写 MetaObjectHandler（3.5.x 用 strict 系列）

```java
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    // 插入时填充
    @Override
    public void insertFill(MetaObject metaObject) {
        // strictInsertFill(对象, 字段的 get 方法引用, 类型.class, 值)
        this.strictInsertFill(metaObject, SysUser::getCreateTime,
                LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, SysUser::getCreateBy,
                String.class, SecurityUtils.getUsername());  // 当前登录人
        // updateTime 也顺手在插入时填一份
        this.strictInsertFill(metaObject, SysUser::getUpdateTime,
                LocalDateTime.class, LocalDateTime.now());
    }

    // 更新时填充
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, SysUser::getUpdateTime,
                LocalDateTime.class, LocalDateTime.now());
    }
}
```
> `SecurityUtils.getUsername()` 取当前登录人（本教程的 `SecurityUtils`，见 [[../SpringBoot+Vue3后台搭建/03-后端基础框架/03-SpringSecurity与JWT鉴权]] 3.5）。无登录上下文的定时任务场景要判空。

**通用填充** --- 弥补上面只针对SysUser填充，其他类使用的时候会报错的情况 
```java
	@Override
public void insertFill(MetaObject metaObject) {
    // 创建时间
    this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
    // 创建人
    this.strictInsertFill(metaObject, "createBy", String.class, SecurityUtils.getUsername());
    // 更新时间
    this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
}

@Override
public void updateFill(MetaObject metaObject) {
    this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
    this.strictUpdateFill(metaObject, "updateBy", String.class, SecurityUtils.getUsername());
}
```

**strictInsertFill 小知识点**
- strictInsertFill：实体字段值为 null 才填充，已有值不覆盖；
- fillStrategy=FieldFill.INSERT 实体字段注解配合使用：
 
```
@TableField(fill = FieldFill.INSERT)
private LocalDateTime createTime;

@TableField(fill = FieldFill.INSERT_UPDATE)
private LocalDateTime updateTime;
如果用 this.fill() 非严格填充，会强制覆盖原有字段值。
```

## 11.3 效果

```java
SysUser u = new SysUser();
u.setUserName("张三"); u.setAge(20);
// u 没 set createTime / updateTime / createBy
userService.save(u);
// 入库时三列已被自动填入（不用手 set）
```
> `strictInsertFill` 有个**安全特性**：若实体对象**已经手 set 了该字段**，Handler **不会覆盖**你的显式值（除非用非 strict 老方法）。这正是"strict"的含义。

## 11.4 为什么不用数据库 `DEFAULT CURRENT_TIMESTAMP`

| 方案 | 缺点 |
|------|------|
| DB 列默认 `CURRENT_TIMESTAMP` | 只管时间；**`create_by`（谁建的）DB 填不了**，且取出时 MP 不会回写实体 |
| **MP `MetaObjectHandler`** | 时间 + 操作人 + 任意列统一注入，且回填实体 |

## 验证清单

- [ ] `save(u)` 后不手 set `createTime`，库里该列有值且实体被回填（`u.getCreateTime()` 非 null）。
- [ ] `updateById` 后 `updateTime` 自动刷新为当前时间。
- [ ] 手 set 了 `createTime` 再 `save`，Handler **不覆盖**你的显式值（strict 行为）。
- [ ] 定时任务（无登录人）调 `save` 时 `createBy` 判空不报错。

> 下一步：[[MyBatisPlus/12-逻辑删除与乐观锁]] 用注解让"删除"变"更新"，并防并发改写冲突。
