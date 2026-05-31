# RuoYi-Vue 数据库设计规范与SQL最佳实践

## 一、核心表结构

### 1.1 系统管理表

| 表名 | 说明 | 主键类型 |
|------|------|----------|
| sys_user | 用户信息表 | bigint(20) |
| sys_role | 角色信息表 | bigint(20) |
| sys_menu | 菜单权限表 | bigint(20) |
| sys_dept | 部门表 | bigint(20) |
| sys_post | 岗位信息表 | bigint(20) |
| sys_dict_type | 字典类型表 | bigint(20) |
| sys_dict_data | 字典数据表 | bigint(20) |
| sys_config | 参数配置表 | int(5) |

### 1.2 关联表

| 表名 | 说明 | 主键 |
|------|------|------|
| sys_user_role | 用户角色关联 | (user_id, role_id) |
| sys_role_menu | 角色菜单关联 | (role_id, menu_id) |
| sys_role_dept | 角色部门关联 | (role_id, dept_id) |
| sys_user_post | 用户岗位关联 | (user_id, post_id) |

### 1.3 日志表

| 表名 | 说明 | 索引 |
|------|------|------|
| sys_oper_log | 操作日志 | business_type, status, oper_time |
| sys_logininfor | 登录日志 | status, login_time |

---

## 二、字段设计规范

### 2.1 为什么使用bigint作为主键

```sql
user_id  bigint(20)  not null auto_increment
```

**原因**:
1. **容量充足**: 范围远超业务需求
2. **分布式兼容**: 为分库分表、雪花算法预留
3. **统一规范**: 外键保持一致类型，避免隐式转换
4. **Java映射**: 对应Long类型，无缝对接MyBatis

### 2.2 char(1)用于状态字段

```sql
status       char(1)   default '0'   -- 状态
del_flag     char(1)   default '0'   -- 删除标志
sex          char(1)   default '0'   -- 性别
```

**优势**:
- 固定1字节，无长度计算开销
- 可读性好（'0'/'1'直观）
- 前端直接绑定字符串

**约定**:
- '0' = 正常/启用
- '1' = 停用/异常
- '2' = 删除（预留'1'用于其他状态）

### 2.3 varchar长度选择原则

```sql
user_name    varchar(30)     -- 用户名，中文最多10个
phonenumber  varchar(11)     -- 手机号，固定11位
email        varchar(50)     -- 邮箱，实际很少超过50
password     varchar(100)    -- BCrypt加密后60字符，留余量
remark       varchar(500)    -- 备注，适中长度
```

**原则**:
1. 基于业务实际，不要所有字段都设255
2. utf8mb4下，varchar(30)可存30个任意字符
3. 过长的varchar影响性能和内存

### 2.4 datetime vs timestamp

RuoYi **统一使用datetime**:

```sql
create_time   datetime
update_time   datetime
```

**datetime优势**:
- 时间范围广（1000-9999年）
- 不受时区影响
- 避免2038年问题
- 与Java的LocalDateTime映射直观

### 2.5 del_flag逻辑删除

```sql
del_flag  char(1)  default '0'  comment '删除标志（0存在 2删除）'
```

**为什么用'2'而不是'1'**:
- '0' = 存在
- '1' = 预留（冻结、审核中等）
- '2' = 删除

**好处**:
- 数据可恢复
- 保留历史关联
- 满足审计要求

**注意**:
- 查询必须加 `del_flag = '0'`
- 唯一索引需改为联合唯一（包含del_flag）

### 2.6 审计字段

```sql
create_by     varchar(64)  -- 创建者
create_time   datetime     -- 创建时间
update_by     varchar(64)  -- 更新者
update_time   datetime     -- 更新时间
```

**规范**:
- create_by/update_by存储用户名（非ID），便于展示
- 通过AOP或BaseController自动填充
- varchar(64)足够存储用户名

---

## 三、索引设计

### 3.1 已建立的索引

```sql
-- sys_oper_log
key idx_sys_oper_log_bt (business_type)
key idx_sys_oper_log_s  (status)
key idx_sys_oper_log_ot (oper_time)

-- sys_logininfor
key idx_sys_logininfor_s  (status)
key idx_sys_logininfor_lt (login_time)

-- sys_dict_type
unique (dict_type)

-- sys_notice_read
unique key uk_user_notice (user_id, notice_id)
```

### 3.2 索引设计原则

**高频查询字段建索引**:
- 日志表的时间字段（范围查询）
- 状态字段（等值查询）
- 业务类型字段（分类统计）

**复合主键形成联合索引**:
```sql
primary key(user_id, role_id)  -- sys_user_role
primary key(role_id, menu_id)  -- sys_role_menu
```

---

## 四、不使用物理外键

RuoYi **完全不使用FOREIGN KEY约束**。

**原因**:
1. **性能**: 外键检查增加INSERT/UPDATE/DELETE开销
2. **分布式**: 分库分表后无法维护跨库外键
3. **灵活性**: 应用层控制级联更灵活
4. **批量操作**: 大批量导入时外键显著降低性能

**级联删除应用层实现**:

```java
@Transactional
public int deleteUserByIds(Long[] userIds) {
    // 1. 先删除关联
    userRoleMapper.deleteUserRoleInfos(userIds);
    userPostMapper.deleteUserPostInfos(userIds);
    // 2. 再删除用户
    return userMapper.deleteUserByIds(userIds);
}
```

---

## 五、MyBatis SQL最佳实践

### 5.1 结果集映射

```xml
<resultMap type="SysUser" id="SysUserResult">
    <id     property="userId"   column="user_id" />
    <result property="userName" column="user_name" />
    
    <!-- 一对一关联 -->
    <association property="dept" javaType="SysDept" 
                 resultMap="deptResult" />
    
    <!-- 一对多关联 -->
    <collection property="roles" javaType="java.util.List" 
                resultMap="RoleResult" />
</resultMap>
```

### 5.2 SQL片段复用

```xml
<sql id="selectUserVo">
    select u.user_id, u.user_name, ..., d.dept_name, ...
    from sys_user u
    left join sys_dept d on u.dept_id = d.dept_id
</sql>

<select id="selectUserList">
    <include refid="selectUserVo"/>
    where u.del_flag = '0'
</select>
```

### 5.3 动态SQL

**条件查询**:

```xml
<select id="selectUserList">
    select ... from sys_user u
    where u.del_flag = '0'
    
    <if test="userName != null and userName != ''">
        AND u.user_name like concat('%', #{userName}, '%')
    </if>
    <if test="status != null and status != ''">
        AND u.status = #{status}
    </if>
    
    <!-- 数据权限过滤 -->
    ${params.dataScope}
</select>
```

**动态更新**:

```xml
<update id="updateUser">
    update sys_user
    <set>
        <if test="nickName != null and nickName != ''">
            nick_name = #{nickName},
        </if>
        <if test="email != null">
            email = #{email},
        </if>
        update_time = sysdate()
    </set>
    where user_id = #{userId}
</update>
```

**批量操作**:

```xml
<delete id="deleteUserByIds">
    update sys_user set del_flag = '2' 
    where user_id in
    <foreach collection="array" item="userId" 
             open="(" separator="," close=")">
        #{userId}
    </foreach>
</delete>
```

### 5.4 模糊查询安全写法

```xml
<!-- 正确：使用concat防止SQL注入 -->
AND u.user_name like concat('%', #{userName}, '%')

<!-- 错误：直接拼接 -->
AND u.user_name like '%${userName}%'  <!-- 危险！ -->
```

### 5.5 数据权限动态注入

```xml
<!-- 数据范围过滤 -->
${params.dataScope}
```

通过AOP在Service层动态生成：
```sql
AND (d.dept_id = 103 OR d.dept_id IN (...))
```

**注意**: 使用`${}`而非`#{}`，因为是拼接SQL片段。

---

## 六、命名规范

| 层级 | 规范 | 示例 |
|------|------|------|
| 表名 | 小写+下划线，sys_前缀 | sys_user |
| 字段名 | 小写+下划线 | user_name |
| 主键 | 表名单数+_id | user_id |
| 外键 | 引用表名+_id | dept_id |
| 索引名 | idx_表名缩写_字段 | idx_sys_oper_log_bt |
| 唯一索引 | uk_字段组合 | uk_user_notice |

---

## 七、设计最佳实践

### 7.1 默认值策略

```sql
-- 字符串：空字符串
user_name  varchar(30)  default ''

-- 数值：0
order_num  int(4)       default 0

-- 状态：明确初始状态
status     char(1)      default '0'

-- 可选字段：允许NULL
remark     varchar(500) default null
```

### 7.2 注释规范

每个表和字段都必须有COMMENT：
```sql
comment '用户ID'
comment '账号状态（0正常 1停用）'
```

### 7.3 引擎和字符集

```sql
engine=innodb           -- 支持事务
auto_increment=100      -- 预分配起始值
-- 字符集通常utf8mb4
```

---

## 八、改进建议

1. **补充索引**: sys_user.user_name, sys_menu.parent_id
2. **分区表**: 日志表按月分区
3. **归档策略**: 定期迁移历史日志
4. **字段一致性**: sys_config主键用int，建议统一bigint
5. **JSON字段**: MySQL 5.7+可用JSON类型存储结构化数据

---

**上一章**: [登录授权流程详解](./08-登录授权流程详解.md)  
**下一章**: [优秀技术细节总结](./10-优秀技术细节总结.md)
