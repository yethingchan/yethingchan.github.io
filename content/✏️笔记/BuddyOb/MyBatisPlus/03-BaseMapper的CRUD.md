---
title: 03-BaseMapper的CRUD
---

# 03 BaseMapper 的 CRUD（MP 白给的方法）

> 上接：[[MyBatisPlus/02-实体注解与表映射]]
> `UserMapper extends BaseMapper<User>` 后，下面这些方法**直接能用**，不用写任何实现。

## 3.1 增

```java
int insert(T entity);   // 插入，主键回填；返回影响行数
```
```java
User u = new User(); u.setUserName("张三"); u.setAge(20);
userMapper.insert(u);
Long id = u.getUserId();   // 自增/雪花已回填
```

## 3.2 删

```java
int deleteById(Serializable id);              // 按主键删（逻辑删则变 UPDATE，见 12 章）
int deleteByMap(Map<String,Object> map);    // WHERE k1=v1 AND k2=v2
int delete(Wrapper<T> wrapper);              // 按 Wrapper 删（★ 危险，见 3.6）
int deleteBatchIds(Collection<?> idList);     // 按主键批量删（IN）
```
```java
userMapper.deleteById(10L);
userMapper.deleteBatchIds(Arrays.asList(1L,2L,3L));   // WHERE user_id IN (1,2,3)
```

## 3.3 改

```java
int updateById(T entity);          // 按主键改【非空字段】
int update(T entity, Wrapper<T> wrapper);  // 按 Wrapper 改（entity 给值，wrapper 给 WHERE）
```
```java
// 按主键改（只改 set 了非 null 的字段）
User u = new User(); u.setUserId(1L); u.setStatus("1");
userMapper.updateById(u);   // UPDATE sys_user SET status='1' WHERE user_id=1

// 按 Wrapper 改（不带实体主键也行）
userMapper.update(null, Wrappers.<User>update()
    .set("status","1").eq("age", 20));   // 把所有 age=20 的置停用
```
> `updateById` 只更新**非 null** 字段（靠 MP 的 `FieldStrategy`），避免了"传 null 把库里值冲掉"。想强制更新 null 要配 `UpdateStrategy`。

## 3.4 查

```java
T selectById(Serializable id);
List<T> selectBatchIds(Collection<?> idList);
List<T> selectByMap(Map<String,Object> map);
T selectOne(Wrapper<T> wrapper);            // 期望至多 1 条，多条抛异常
Long selectCount(Wrapper<T> wrapper);       // 计数
List<T> selectList(Wrapper<T> wrapper);    // 列表（★ 最常用）
// 分页见 [[MyBatisPlus/09-分页插件]]：IPage<T> selectPage(IPage<T>, Wrapper<T>)
```
```java
User u = userMapper.selectById(1L);
List<User> list = userMapper.selectList(
    Wrappers.<User>query().eq("status","0").orderByDesc("age"));
long cnt = userMapper.selectCount(Wrappers.<User>query().gt("age", 18));
```

## 3.5 空实体当查询条件（entity 查询）

`selectList(entity)` 会把**实体里非 null 字段**当等值条件：
```java
User q = new User(); q.setStatus("0"); q.setAge(20);
List<User> list = userMapper.selectList(Wrappers.query(q));
// 等价 WHERE status='0' AND age=20（null 字段忽略）
```
> 简单等值好用，复杂（like/范围/in）还是老老实实上 [[MyBatisPlus/05-QueryWrapper详解]]。

## 3.6 ⚠️ 全表操作防护（企业级必看）

`delete(wrapper)` / `update(entity, wrapper)` / `selectList(wrapper)` **若 wrapper 为空，会命中全表**！
```java
userMapper.delete(Wrappers.query());          // ❌ 清空整张表！
userMapper.selectList(null);                 // ❌ 全表扫描（数据量大直接拖垮）
```
MP 提供**防全表更新/删除拦截器**（[[MyBatisPlus/15-企业级实战与避坑]] 详述）：
```java
@Bean
public MybatisPlusInterceptor mpInterceptor() {
    MybatisPlusInterceptor i = new MybatisPlusInterceptor();
    i.addInnerInterceptor(new BlockAttackInnerInterceptor()); // 全表更新/删除直接抛异常
    return i;
}
```
> **铁律**：任何 `Wrapper` 查询/更新/删除，**先确认 WHERE 非空**再执行。生产事故 Top1 就是"wrapper 没拼上，全表误删"。

## 验证清单

- [ ] `insert` 后主键回填；`updateById` 只改非 null 字段。
- [ ] `deleteBatchIds` 生成 `IN (...)`。
- [ ] `selectOne` 命中多条时抛 `TooManyResultsException`。
- [ ] 加了 `BlockAttackInnerInterceptor` 后，空 wrapper 全表操作被拦截抛异常。

> 下一步：[[MyBatisPlus/04-IServer与ServiceImpl]] 看比 BaseMapper 更顺手的 Service 层。
