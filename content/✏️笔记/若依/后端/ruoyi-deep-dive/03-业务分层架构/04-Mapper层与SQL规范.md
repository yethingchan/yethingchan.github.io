# 04 - Mapper 层与 SQL 规范

> 本文档深入剖析 RuoYi-Vue 项目 Mapper 层的设计规范，包括 Mapper 接口规范、XML 映射规范、分页处理、数据权限 SQL 拼接。

---

## 一、Mapper 层在架构中的定位

Mapper 层是数据访问层（DAO），负责 Java 对象与数据库表之间的映射和 SQL 执行。RuoYi-Vue 使用 MyBatis 作为 ORM 框架，采用**接口 + XML** 的映射方式。

```
Service 层
    │ 调用 Mapper 接口
    ▼
┌──────────────────────────────┐
│  SysUserMapper.java          │  ← Mapper 接口（方法签名）
│  - selectUserList()          │
│  - insertUser()              │
│  - updateUser()              │
└──────────┬───────────────────┘
           │ MyBatis 自动代理
           ▼
┌──────────────────────────────┐
│  SysUserMapper.xml            │  ← XML 映射文件（SQL 定义）
│  - <select> 查询语句          │
│  - <insert> 插入语句          │
│  - <update> 更新语句          │
│  - <delete> 删除语句          │
│  - <sql> 可复用 SQL 片段      │
│  - <resultMap> 结果映射       │
└──────────┬───────────────────┘
           │ JDBC
           ▼
┌──────────────────────────────┐
│  MySQL Database              │
└──────────────────────────────┘
```

---

## 二、Mapper 接口规范

### 2.1 接口定义规范

```java
// 文件：ruoyi-system/src/main/java/com/ruoyi/system/mapper/SysUserMapper.java

public interface SysUserMapper
{
    // 查询方法 - 返回列表
    public List<SysUser> selectUserList(SysUser sysUser);
    public List<SysUser> selectAllocatedList(SysUser user);
    public List<SysUser> selectUnallocatedList(SysUser user);

    // 查询方法 - 返回单个对象
    public SysUser selectUserByUserName(String userName);
    public SysUser selectUserById(Long userId);

    // 唯一性校验 - 返回已存在的记录或 null
    public SysUser checkUserNameUnique(String userName);
    public SysUser checkPhoneUnique(String phonenumber);
    public SysUser checkEmailUnique(String email);

    // 新增方法 - 返回影响行数
    public int insertUser(SysUser user);

    // 修改方法 - 返回影响行数
    public int updateUser(SysUser user);
    public int updateUserStatus(@Param("userId") Long userId, @Param("status") String status);
    public int updateUserAvatar(@Param("userId") Long userId, @Param("avatar") String avatar);
    public int updateLoginInfo(@Param("userId") Long userId, @Param("loginIp") String loginIp, @Param("loginDate") Date loginDate);
    public int resetUserPwd(@Param("userId") Long userId, @Param("password") String password);

    // 删除方法 - 返回影响行数
    public int deleteUserById(Long userId);
    public int deleteUserByIds(Long[] userIds);
}
```

### 2.2 接口设计规范总结

| 规范 | 说明 | 示例 |
|------|------|------|
| 命名规范 | `动词` + `实体名` + `条件` | `selectUserByUserName` |
| 查询返回 | 列表返回 `List<T>`，单条返回 `T` | `selectUserList` / `selectUserById` |
| 增删改返回 | 返回 `int` 影响行数 | `insertUser` / `updateUser` |
| 参数传递 | 单参数直接传，多参数用 `@Param` | `@Param("userId") Long userId` |
| 不含业务逻辑 | Mapper 只做数据访问，不含校验 | 无 checkAllowed 等方法 |

### 2.3 @Param 注解使用场景

```java
// 单参数 - 不需要 @Param（MyBatis 自动绑定）
public SysUser selectUserByUserName(String userName);
public SysUser selectUserById(Long userId);

// 多参数 - 必须使用 @Param
public int updateUserStatus(@Param("userId") Long userId, @Param("status") String status);
public int updateLoginInfo(@Param("userId") Long userId, @Param("loginIp") String loginIp, @Param("loginDate") Date loginDate);
public int resetUserPwd(@Param("userId") Long userId, @Param("password") String password);

// 对象参数 - 不需要 @Param（MyBatis 通过对象属性绑定）
public int insertUser(SysUser user);
public int updateUser(SysUser user);
```

---

## 三、XML 映射规范

### 3.1 XML 文件结构

```xml
<!-- 文件：ruoyi-system/src/main/resources/mapper/system/SysUserMapper.xml -->

<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ruoyi.system.mapper.SysUserMapper">

    <!-- 1. resultMap 结果映射 -->
    <resultMap type="SysUser" id="SysUserResult">...</resultMap>

    <!-- 2. sql 可复用片段 -->
    <sql id="selectUserVo">...</sql>

    <!-- 3. select 查询语句 -->
    <select id="selectUserList" ...>...</select>

    <!-- 4. insert 插入语句 -->
    <insert id="insertUser" ...>...</insert>

    <!-- 5. update 更新语句 -->
    <update id="updateUser" ...>...</update>

    <!-- 6. delete 删除语句 -->
    <delete id="deleteUserByIds" ...>...</delete>

</mapper>
```

### 3.2 ResultMap 结果映射

```xml
<resultMap type="SysUser" id="SysUserResult">
    <id     property="userId"        column="user_id"         />
    <result property="deptId"        column="dept_id"         />
    <result property="userName"      column="user_name"       />
    <result property="nickName"      column="nick_name"       />
    <result property="email"         column="email"           />
    <result property="phonenumber"   column="phonenumber"     />
    <result property="password"      column="password"        />
    <result property="status"        column="status"          />
    <result property="delFlag"       column="del_flag"        />
    <result property="loginIp"       column="login_ip"        />
    <result property="loginDate"     column="login_date"      />
    <result property="pwdUpdateDate" column="pwd_update_date" />
    <result property="createBy"      column="create_by"       />
    <result property="createTime"    column="create_time"     />
    <result property="updateBy"      column="update_by"       />
    <result property="updateTime"    column="update_time"     />
    <result property="remark"        column="remark"          />
    <!-- 关联对象：部门 -->
    <association property="dept"  javaType="SysDept"  resultMap="deptResult" />
    <!-- 关联集合：角色 -->
    <collection  property="roles" javaType="java.util.List" resultMap="RoleResult" />
</resultMap>
```

**设计要点：**
- 使用 `<id>` 标记主键字段，MyBatis 缓存依赖主键
- 使用 `<association>` 映射一对一关联（用户 → 部门）
- 使用 `<collection>` 映射一对多关联（用户 → 角色）
- 关联的 resultMap 单独定义（`deptResult`、`RoleResult`），避免嵌套过深

### 3.3 SQL 可复用片段

```xml
<sql id="selectUserVo">
    select u.user_id, u.dept_id, u.user_name, u.nick_name, u.email, u.avatar,
           u.phonenumber, u.password, u.sex, u.status, u.del_flag, u.login_ip,
           u.login_date, u.pwd_update_date, u.create_by, u.create_time,
           u.update_by, u.update_time, u.remark,
           d.dept_id, d.parent_id, d.ancestors, d.dept_name, d.order_num, d.leader,
           d.status as dept_status,
           r.role_id, r.role_name, r.role_key, r.role_sort, r.data_scope,
           r.status as role_status
    from sys_user u
        left join sys_dept d on u.dept_id = d.dept_id
        left join sys_user_role ur on u.user_id = ur.user_id
        left join sys_role r on r.role_id = ur.role_id
</sql>
```

**使用方式：**
```xml
<select id="selectUserByUserName" parameterType="String" resultMap="SysUserResult">
    <include refid="selectUserVo"/>
    where u.user_name = #{userName} and u.del_flag = '0'
</select>
```

### 3.4 动态 SQL 规范

RuoYi-Vue 的 XML 映射大量使用 MyBatis 动态 SQL：

```xml
<!-- 条件查询 - 使用 <if> 标签 -->
<select id="selectUserList" parameterType="SysUser" resultMap="SysUserResult">
    select u.user_id, u.dept_id, u.nick_name, u.user_name, u.email, u.avatar,
           u.phonenumber, u.sex, u.status, u.del_flag, u.login_ip, u.login_date,
           u.create_by, u.create_time, u.remark, d.dept_name, d.leader
    from sys_user u
    left join sys_dept d on u.dept_id = d.dept_id
    where u.del_flag = '0'
    <if test="userId != null and userId != 0">
        AND u.user_id = #{userId}
    </if>
    <if test="userName != null and userName != ''">
        AND u.user_name like concat('%', #{userName}, '%')
    </if>
    <if test="status != null and status != ''">
        AND u.status = #{status}
    </if>
    <if test="phonenumber != null and phonenumber != ''">
        AND u.phonenumber like concat('%', #{phonenumber}, '%')
    </if>
    <!-- 时间范围查询 -->
    <if test="params.beginTime != null and params.beginTime != ''">
        AND date_format(u.create_time,'%Y%m%d') &gt;= date_format(#{params.beginTime},'%Y%m%d')
    </if>
    <if test="params.endTime != null and params.endTime != ''">
        AND date_format(u.create_time,'%Y%m%d') &lt;= date_format(#{params.endTime},'%Y%m%d')
    </if>
    <!-- 部门权限（包含子部门） -->
    <if test="deptId != null and deptId != 0">
        AND (u.dept_id = #{deptId} OR u.dept_id IN (
            SELECT t.dept_id FROM sys_dept t WHERE find_in_set(#{deptId}, ancestors)
        ))
    </if>
    <!-- 数据范围过滤 -->
    ${params.dataScope}
</select>
```

### 3.5 插入语句规范

```xml
<insert id="insertUser" parameterType="SysUser" useGeneratedKeys="true" keyProperty="userId">
    insert into sys_user(
        <if test="userId != null and userId != 0">user_id,</if>
        <if test="deptId != null and deptId != 0">dept_id,</if>
        <if test="userName != null and userName != ''">user_name,</if>
        <if test="nickName != null and nickName != ''">nick_name,</if>
        <if test="email != null and email != ''">email,</if>
        <if test="password != null and password != ''">password,</if>
        <if test="status != null and status != ''">status,</if>
        <if test="createBy != null and createBy != ''">create_by,</if>
        <if test="remark != null and remark != ''">remark,</if>
        create_time
    )values(
        <if test="userId != null and userId != ''">#{userId},</if>
        <if test="deptId != null and deptId != ''">#{deptId},</if>
        <if test="userName != null and userName != ''">#{userName},</if>
        <if test="nickName != null and nickName != ''">#{nickName},</if>
        <if test="email != null and email != ''">#{email},</if>
        <if test="password != null and password != ''">#{password},</if>
        <if test="status != null and status != ''">#{status},</if>
        <if test="createBy != null and createBy != ''">#{createBy},</if>
        <if test="remark != null and remark != ''">#{remark},</if>
        sysdate()
    )
</insert>
```

**设计要点：**
- `useGeneratedKeys="true" keyProperty="userId"`：插入后自动回填主键
- `create_time` 使用 `sysdate()` 由数据库生成，不从 Java 传入
- 所有字段都用 `<if>` 判断，只插入非空字段

### 3.6 更新语句规范

```xml
<update id="updateUser" parameterType="SysUser">
    update sys_user
    <set>
        <if test="deptId != 0">dept_id = #{deptId},</if>
        <if test="nickName != null and nickName != ''">nick_name = #{nickName},</if>
        <if test="email != null ">email = #{email},</if>
        <if test="phonenumber != null ">phonenumber = #{phonenumber},</if>
        <if test="sex != null and sex != ''">sex = #{sex},</if>
        <if test="avatar != null and avatar != ''">avatar = #{avatar},</if>
        <if test="password != null and password != ''">password = #{password},</if>
        <if test="status != null and status != ''">status = #{status},</if>
        <if test="updateBy != null and updateBy != ''">update_by = #{updateBy},</if>
        <if test="remark != null">remark = #{remark},</if>
        update_time = sysdate()
    </set>
    where user_id = #{userId}
</update>
```

**设计要点：**
- 使用 `<set>` 标签自动处理末尾逗号
- `update_time = sysdate()` 始终更新，不在 `<if>` 中
- `email` 和 `phonenumber` 只判 null 不判空串，允许清空

### 3.7 删除语句规范（逻辑删除）

```xml
<delete id="deleteUserById" parameterType="Long">
    update sys_user set del_flag = '2' where user_id = #{userId}
</delete>

<delete id="deleteUserByIds" parameterType="Long">
    update sys_user set del_flag = '2' where user_id in
    <foreach collection="array" item="userId" open="(" separator="," close=")">
        #{userId}
    </foreach>
</delete>
```

**设计要点：**
- 标签是 `<delete>` 但实际执行的是 `UPDATE` 语句（逻辑删除）
- `del_flag = '2'` 标记为已删除，`'0'` 为正常
- 批量删除使用 `<foreach>` 遍历数组参数

---

## 四、分页处理机制

### 4.1 PageHelper 工作原理

```
1. Controller 调用 startPage()
2. PageHelper 从 ThreadLocal 获取分页参数（pageNum, pageSize）
3. PageHelper 拦截下一个 MyBatis 查询
4. 自动在 SQL 末尾添加 LIMIT offset, size
5. 执行 COUNT 查询获取总数
6. 执行带 LIMIT 的查询获取分页数据
7. 返回 PageInfo 对象（包含 total, list 等信息）
```

### 4.2 分页参数传递

前端通过 URL 参数传递分页信息：

```
GET /system/user/list?pageNum=1&pageSize=10&orderByColumn=user_name&isAsc=asc
```

后端通过 `PageUtils.startPage()` 解析这些参数：

```java
// BaseController.startPage() → PageUtils.startPage()
// PageUtils 从 ThreadLocal 的 HttpServletRequest 中获取分页参数
```

### 4.3 分页返回封装

```java
// BaseController.getDataTable()
protected TableDataInfo getDataTable(List<?> list) {
    TableDataInfo rspData = new TableDataInfo();
    rspData.setCode(HttpStatus.SUCCESS);
    rspData.setMsg("查询成功");
    rspData.setRows(list);                          // 当前页数据
    rspData.setTotal(new PageInfo(list).getTotal()); // 总记录数
    return rspData;
}
```

---

## 五、数据权限 SQL 拼接

### 5.1 拼接位置

数据权限 SQL 通过 `${params.dataScope}` 拼接在 WHERE 条件末尾：

```xml
<select id="selectUserList" ...>
    select ... from sys_user u left join sys_dept d on u.dept_id = d.dept_id
    where u.del_flag = '0'
    <!-- 各种业务条件 -->
    <!-- 数据范围过滤 -->
    ${params.dataScope}
</select>
```

### 5.2 拼接过程

```
DataScopeAspect 切面
    │
    ├─ 1. clearDataScope() - 清空 params.dataScope（防注入）
    │
    ├─ 2. 获取当前用户的角色列表
    │
    ├─ 3. 遍历角色，根据 dataScope 拼接 SQL
    │     ├─ DATA_SCOPE_ALL (1)     → 不拼接
    │     ├─ DATA_SCOPE_CUSTOM (2)  → OR d.dept_id IN (SELECT dept_id FROM sys_role_dept WHERE role_id = ?)
    │     ├─ DATA_SCOPE_DEPT (3)    → OR d.dept_id = ?
    │     ├─ DATA_SCOPE_DEPT_AND_CHILD (4) → OR d.dept_id IN (SELECT ...)
    │     └─ DATA_SCOPE_SELF (5)    → OR u.user_id = ?
    │
    └─ 4. 设置到 BaseEntity.params.dataScope
         → " AND (拼接的SQL条件)"
```

### 5.3 SQL 注入防护

```java
// DataScopeAspect.clearDataScope()
private void clearDataScope(final JoinPoint joinPoint) {
    Object params = joinPoint.getArgs()[0];
    if (StringUtils.isNotNull(params) && params instanceof BaseEntity) {
        BaseEntity baseEntity = (BaseEntity) params;
        baseEntity.getParams().put(DATA_SCOPE, "");  // 先清空
    }
}
```

**安全机制：**
1. 切面先清空 `dataScope`，再由切面自己拼接
2. 拼接的 SQL 完全由服务端控制，不接受前端输入
3. 使用 `#{}` 占位符传递参数值（预编译）
4. 只有 SQL 结构使用 `${}` 拼接，且来源可信

---

## 六、Mapper XML 文件清单

| Mapper XML | 对应表 | 位置 |
|------------|--------|------|
| SysUserMapper.xml | sys_user | mapper/system/ |
| SysRoleMapper.xml | sys_role | mapper/system/ |
| SysMenuMapper.xml | sys_menu | mapper/system/ |
| SysDeptMapper.xml | sys_dept | mapper/system/ |
| SysPostMapper.xml | sys_post | mapper/system/ |
| SysDictTypeMapper.xml | sys_dict_type | mapper/system/ |
| SysDictDataMapper.xml | sys_dict_data | mapper/system/ |
| SysConfigMapper.xml | sys_config | mapper/system/ |
| SysNoticeMapper.xml | sys_notice | mapper/system/ |
| SysOperLogMapper.xml | sys_oper_log | mapper/system/ |
| SysLogininforMapper.xml | sys_logininfor | mapper/system/ |
| SysUserRoleMapper.xml | sys_user_role | mapper/system/ |
| SysUserPostMapper.xml | sys_user_post | mapper/system/ |
| SysRoleMenuMapper.xml | sys_role_menu | mapper/system/ |
| SysRoleDeptMapper.xml | sys_role_dept | mapper/system/ |
| GenTableMapper.xml | gen_table | mapper/generator/ |
| GenTableColumnMapper.xml | gen_table_column | mapper/generator/ |
| SysJobMapper.xml | sys_job | mapper/quartz/ |
| SysJobLogMapper.xml | sys_job_log | mapper/quartz/ |

---

## 七、查看与剖析点

1. **查看 SysUserMapper.xml 的完整内容**，理解 resultMap、sql 片段、动态 SQL 的完整用法
   - 文件：`ruoyi-system/src/main/resources/mapper/system/SysUserMapper.xml`

2. **查看 SysRoleMapper.xml**，对比与 SysUserMapper.xml 的差异，理解关联查询的不同方式
   - 文件：`ruoyi-system/src/main/resources/mapper/system/SysRoleMapper.xml`

3. **查看 PageUtils 的实现**，理解分页参数如何从前端传递到 PageHelper
   - 文件：`ruoyi-common/src/main/java/com/ruoyi/common/utils/PageUtils.java`

4. **查看 BaseEntity 的 params 字段定义**，理解数据权限 SQL 如何通过 BaseEntity 传递
   - 文件：`ruoyi-common/src/main/java/com/ruoyi/common/core/domain/BaseEntity.java`

5. **查看 MyBatisConfig 配置**，理解 MyBatis 的全局配置
   - 文件：`ruoyi-framework/src/main/java/com/ruoyi/framework/config/MyBatisConfig.java`

---

## 八、细节留神

1. **`${params.dataScope}` 使用 `${}` 而非 `#{}`**：这是因为需要直接拼接 SQL 片段（如 `AND (d.dept_id = 1 OR ...)`），不是简单的参数替换。虽然 `${}` 有 SQL 注入风险，但这里的值完全由服务端 AOP 切面生成，不接受外部输入
2. **逻辑删除使用 `<delete>` 标签**：虽然标签名是 delete，但实际 SQL 是 update，这是一种约定，通过标签名表达业务语义
3. **查询列表和查询详情使用不同的 SQL**：`selectUserList` 不查询密码字段（安全考虑），`selectUserByUserName` 通过 `<include refid="selectUserVo"/>` 查询完整字段包括密码
4. **`create_time` 使用 `sysdate()` 而非 Java 传入**：利用数据库函数生成时间，避免应用服务器时间不一致的问题
5. **`update_time` 始终更新**：在 `<set>` 标签中不在 `<if>` 判断内，确保每次修改都更新时间戳

---

## 九、提问方向

1. **`${params.dataScope}` 使用了 `${}` 直接拼接 SQL，一般情况下这是不安全的。RuoYi-Vue 是如何保证这里的 SQL 注入安全的？如果攻击者通过某种方式在请求参数中设置了 `params.dataScope` 的值，会发生什么？**

2. **SysUserMapper.xml 中 `selectUserList` 和 `selectUserByUserName` 使用了不同的 SQL，前者不查密码字段，后者通过 `<include>` 包含密码字段。为什么要做这种区分？如果列表查询也包含密码字段会有什么安全风险？**

3. **逻辑删除使用 `<delete>` 标签但实际执行 UPDATE 语句，这种做法是否规范？如果使用 `<update>` 标签是否更合适？这种命名方式对代码可读性有什么影响？**

4. **PageHelper 的分页参数存储在 ThreadLocal 中，如果在同一个线程中连续调用两个分页查询，第二个查询会怎样？如何避免分页参数"污染"的问题？**

5. **`<insert>` 语句中 `create_time` 使用 `sysdate()` 由数据库生成，而 `update_time` 也是 `sysdate()`。如果应用服务器和数据库服务器的时间不一致，会导致什么问题？如何解决？**

6. **SysUserMapper.xml 中的 `selectUserList` 使用了 `left join sys_dept`，但如果用户没有关联部门（dept_id 为 null），left join 仍然会返回一条记录。这种设计在什么场景下可能产生问题？**

7. **RuoYi-Vue 的 Mapper XML 中大量使用 `<if test="xxx != null and xxx != ''">` 进行条件判断，如果某个字段是 Integer 类型且值为 0，`xxx != null` 为 true 但 `xxx != ''` 会怎样？这是否是一个潜在的 bug？**
