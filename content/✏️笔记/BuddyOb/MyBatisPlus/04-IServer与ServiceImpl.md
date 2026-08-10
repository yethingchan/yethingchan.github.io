---
title: 04-IServer与ServiceImpl
---

# 04 IServer<T> 与 ServiceImpl<M,T>

> 上接：[[03-BaseMapper的CRUD]]
> `BaseMapper` 已经够用，但 Service 层再包一层 **`IService` + `ServiceImpl`**，多了一堆批量/链式/分页的便利方法，企业项目几乎都用它（本 Spring 教程的 Service 全是这个写法，见 [[../SpringBoot+Vue3后台搭建/04-权限管理模块/01-用户管理]]）。

## 4.1 定义

```java
// 接口
public interface IUserService extends IService<User> {}

// 实现
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements IUserService {
    // 业务方法写这里，BaseMapper 用 this.baseMapper 拿
}
```
> `ServiceImpl<M extends BaseMapper<T>, T>` 已经把 `baseMapper` 注入好，直接 `this.baseMapper.xxx` 或 `this.list(...)` 都行。

## 4.2 IService 常用方法（比 BaseMapper 多的）

| 方法 | 说明 |
|------|------|
| `save(T)` / `saveBatch(List)` / `saveOrUpdate(T)` | 新增 / **批量新增** / 有 id 则改无则增 |
| `saveBatch(Collection, batchSize)` | 分批提交，防一条 SQL 过长 |
| `getById` / `getOne(wrapper)` / `getByIdOpt` | 查一个（`Optional`） |
| `list()` / `list(wrapper)` / `listByIds(ids)` | 查列表 |
| `page(IPage)` / `page(IPage, wrapper)` | **分页**（见 [[09-分页插件]]） |
| `count()` / `count(wrapper)` | 计数 |
| `updateById` / `update(T,wrapper)` / `update(Wrapper)` | 改 |
| `removeById` / `remove(wrapper)` / `removeBatchByIds` | 删（逻辑删见 [[12-逻辑删除与乐观锁]]） |
| `lambdaQuery()` / `lambdaUpdate()` / `chainQuery()` | **链式调用**（见 4.4） |

## 4.3 批量（性能关键）

```java
List<User> users = ...; // 1000 条
userService.saveBatch(users, 500);   // 每 500 条一批，分两批 INSERT
```
> 不用 `saveBatch` 的话，要么循环 `save`（N 次网络往返，慢死），要么手写 `foreach` 大 SQL（超长易报错）。`saveBatch` 自动分批，企业必用。

## 4.4 链式调用（lambdaQuery / lambdaUpdate）

不用先 new Wrapper，直接链式拼完 `.list()` / `.update()`：
```java
// 链式查
List<User> list = userService.lambdaQuery()
    .eq(User::getStatus, "0")
    .gt(User::getAge, 18)
    .orderByDesc(User::getAge)
    .list();                       // 末尾 .list() 触发执行

// 链式改（不用实体对象）
userService.lambdaUpdate()
    .eq(User::getAge, 20)
    .set(User::getStatus, "1")
    .update();                    // 末尾 .update() 触发
```
> 这俩本质就是帮你 `new LambdaQueryWrapper` 再调 `baseMapper`，**只是语法糖**，但写起来清爽很多。注意 `.list()` / `.one()` / `.count()` / `.update()` 是"执行动作"，调了才真正查库。

## 4.5 和 BaseMapper 怎么选

| 场景 | 用 |
|------|----|
| 单表 CRUD、批量、链式、分页 | **IService**（优先） |
| 想直接拿 Mapper 做细活 / 自定义方法 | `this.baseMapper` 或注入 Mapper |
| 多表联查 | Mapper 里写 XML / 注解 SQL（Wrapper 不擅长联表） |

## 验证清单

- [ ] `UserServiceImpl extends ServiceImpl<UserMapper,User>` 后，`list()/saveBatch()/page()` 直接可用。
- [ ] `saveBatch(users, 500)` 比循环 `save` 快一个数量级（大数据量时体感明显）。
- [ ] `lambdaQuery().eq(...).gt(...).list()` 链式写法跑通，等价于 `selectList(lambdaQuery)`。

> 下一步进入**本库重点**：[[05-QueryWrapper详解]] 把所有查询条件方法一次讲透。
