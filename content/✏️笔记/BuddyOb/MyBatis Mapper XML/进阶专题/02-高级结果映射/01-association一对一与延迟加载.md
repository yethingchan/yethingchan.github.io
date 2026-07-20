# 02-高级结果映射 · `association` 一对一与延迟加载

> 前置：[[MyBatis Mapper XML/进阶专题/02-高级结果映射/00-索引]]

## 场景：用户 → 部门（一对一）

`SysUser` 里嵌一个 `SysDept` 对象。两种写法。

### 写法 A：嵌套结果（一次 JOIN 查出）

```xml
<resultMap id="userWithDept" type="SysUser" autoMapping="true">
  <id property="userId" column="user_id"/>
  <association property="dept" javaType="SysDept">
    <id property="deptId" column="dept_id"/>
    <result property="deptName" column="dept_name"/>
  </association>
</resultMap>

<select id="selectUserWithDept" resultMap="userWithDept">
  SELECT u.user_id, u.user_name, d.dept_id, d.dept_name
  FROM sys_user u LEFT JOIN sys_dept d ON u.dept_id = d.dept_id
  WHERE u.user_id = #{id}
</select>
```
- 一条 SQL 搞定，无 N+1 问题。
- `association` 内是**独立的子 resultMap**，可单独抽出来复用（见 04）。

### 写法 B：嵌套查询（延迟加载关键）

```xml
<resultMap id="userLazy" type="SysUser">
  <id property="userId" column="user_id"/>
  <association property="dept" column="dept_id"
               select="com.x.mapper.SysDeptMapper.selectById"
               fetchType="lazy"/>
</resultMap>

<select id="selectUserLazy" resultMap="userLazy">
  SELECT user_id, dept_id FROM sys_user WHERE user_id = #{id}
</select>
```
- `select` 指向另一个 Mapper 的语句（`namespace + id`）。
- `column="dept_id"` 会把这一列的值作为参数传给子查询。
- `fetchType="lazy"` → **用到 `user.getDept()` 时才发第二条 SQL**。

## 延迟加载（Lazy Loading）的全局开关

`fetchType="lazy"` 受全局 `lazyLoadingEnabled` 控制（默认 false）：

```yaml
mybatis:
  configuration:
    lazy-loading-enabled: true
    aggressive-lazy-loading: false   # 关闭"访问任一属性就加载全部"
```

| 配置 | 行为 |
|------|------|
| `lazyLoadingEnabled=false` | 一律立即加载（嵌套查询也会马上发 SQL） |
| `lazyLoadingEnabled=true, aggressive=false` | 真正按需：调 `getDept()` 才查部门 |
| `aggressive=true` | 访问**任意**属性都触发**全部**懒加载（不推荐） |

## 延迟加载的"坑"

1. **序列化即触发**：把带懒加载属性的对象 `JSON.toJSONString()` 或丢进 Redis，序列化会调 getter → 立刻发 SQL，懒加载失效。
2. **SqlSession 已关**：嵌套查询的第二条 SQL 需要原 `SqlSession`。Spring 事务外（无事务）`SqlSession` 可能已关 → 报 `LazyInitializationException`。解决：保证在事务内访问，或改用嵌套结果（写法 A）。
3. **`equals/hashCode/toString` 触发**：`lombok @Data` 的 `toString` 会调关联对象的 getter → 触发加载。
4. **本仓库 RuoYi 的做法**：VO 返回前端前，**手动把关联字段 copy 成扁平字段**，避免把懒加载代理序列化出去（见 [[MyBatis Mapper XML/进阶专题/04-批量与性能优化/04-性能陷阱N+1与类型开销]]）。

## 结论

- 关联数据**必用** → 嵌套结果（JOIN），无额外 SQL。
- 关联数据**偶尔用** → 嵌套查询 + `fetchType="lazy"`，但要防序列化/关 Session 两个坑。
- `column` 可以传多列：`column="{did=dept_id, name=user_name}"` 用 map 形式。

下一步：[[MyBatis Mapper XML/进阶专题/02-高级结果映射/02-collection一对多与嵌套查询vs结果]]
