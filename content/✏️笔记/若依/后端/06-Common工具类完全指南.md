# RuoYi-Vue Common工具类完全指南

## 一、工具类概览

ruoyi-common模块提供了**37个工具类文件**,覆盖字符串、日期、安全、Web、文件、Excel等各个方面。

### 1.1 目录结构

```
com.ruoyi.common.utils/
├── StringUtils.java           # 字符串处理（最核心）
├── DateUtils.java             # 日期时间处理
├── SecurityUtils.java         # Spring Security工具
├── ServletUtils.java          # Servlet/HTTP工具
├── DictUtils.java             # 字典工具
├── Arith.java                 # 精确运算
├── PageUtils.java             # 分页工具
├── IpUtils.java               # IP处理
├── MessageUtils.java          # 国际化消息
├── LogUtils.java              # 日志格式化
│
├── bean/
│   ├── BeanUtils.java         # JavaBean属性复制
│   └── BeanValidators.java    # Bean验证
│
├── file/
│   ├── FileUploadUtils.java   # 文件上传
│   ├── FileUtils.java         # 文件读写
│   ├── FileTypeUtils.java     # 文件类型识别
│   ├── ImageUtils.java        # 图片处理
│   └── MimeTypeUtils.java     # MIME类型常量
│
├── html/
│   ├── HTMLFilter.java        # HTML过滤器(XSS防护)
│   └── EscapeUtil.java        # HTML转义
│
├── http/
│   ├── HttpUtils.java         # HTTP请求工具
│   └── UserAgentUtils.java    # User-Agent解析
│
├── ip/
│   ├── IpUtils.java           # IP地址获取
│   └── AddressUtils.java      # IP归属地查询
│
├── poi/
│   ├── ExcelUtil.java         # Excel导入导出（71KB）
│   └── ExcelSheet.java        # 多Sheet配置
│
├── reflect/
│   └── ReflectUtils.java      # 反射操作
│
├── sign/
│   ├── Base64.java            # Base64编解码
│   └── Md5Utils.java          # MD5加密
│
├── spring/
│   └── SpringUtils.java       # Spring容器工具
│
├── sql/
│   └── SqlUtil.java           # SQL注入防护
│
├── uuid/
│   ├── UUID.java              # UUID生成器
│   ├── IdUtils.java           # ID生成器
│   └── Seq.java               # 序列号生成器
│
└── core/redis/
    └── RedisCache.java        # Redis缓存操作
```

---

## 二、核心工具类详解

### 2.1 StringUtils（字符串处理）

**功能清单**:

| 分类 | 方法 | 说明 | 示例 |
|------|------|------|------|
| **空值判断** | isEmpty() | 判断null或空字符串 | `StringUtils.isEmpty(str)` |
| | isNotEmpty() | 判断非空 | `StringUtils.isNotEmpty(str)` |
| | isNull() | 判断null | `StringUtils.isNull(obj)` |
| | hasText() | 判断包含非空白字符 | `StringUtils.hasText("  ")` → false |
| **字符串处理** | trim() | 去除首尾空格 | `StringUtils.trim(" abc ")` → "abc" |
| | hide() | 脱敏隐藏 | `StringUtils.hide("13812345678", 3, 7)` → "138****5678" |
| | substring() | 安全截取 | `StringUtils.substring("abc", 0, 2)` → "ab" |
| | format() | 占位符格式化 | `StringUtils.format("用户{}登录", name)` |
| **类型转换** | toUnderScoreCase() | 驼峰转下划线 | `toUnderScoreCase("userName")` → "user_name" |
| | toCamelCase() | 下划线转小驼峰 | `toCamelCase("user_name")` → "userName" |
| | convertToCamelCase() | 下划线转大驼峰 | `convertToCamelCase("user_name")` → "UserName" |
| | str2List() | 字符串转List | `str2List("a,b,c", ",")` → ["a","b","c"] |
| **匹配查找** | isMatch() | Ant路径匹配 | `isMatch("/api/**", "/api/user")` → true |
| | equalsIgnoreCase() | 忽略大小写比较 | `equalsIgnoreCase("ABC", "abc")` → true |
| | startsWithAny() | 前缀判断 | `startsWithAny(str, "=", "-")` |
| **其他** | nvl() | null默认值替换 | `nvl(null, "-")` → "-" |
| | lastStringDel() | 删除最后一个字符 | `lastStringDel("abc,")` → "abc" |

**使用示例**:

```java
// 空值判断
if (StringUtils.isEmpty(userName)) {
    throw new ServiceException("用户名不能为空");
}

// 格式化
String msg = StringUtils.format("用户{}于{}登录成功", userName, DateUtils.getTime());

// 脱敏
String phone = StringUtils.hide("13812345678", 3, 7);  // 138****5678

// 驼峰转换
String under = StringUtils.toUnderScoreCase("userName");  // user_name
String camel = StringUtils.toCamelCase("user_name");      // userName

// 路径匹配
boolean match = StringUtils.isMatch("/system/**", "/system/user/list");
```

---

### 2.2 DateUtils（日期处理）

**功能清单**:

| 方法 | 说明 | 返回值 | 示例 |
|------|------|--------|------|
| getNowDate() | 获取当前Date | Date | `new Date()` |
| getDate() | 获取当前日期 | String | "2024-01-01" |
| getTime() | 获取当前时间 | String | "2024-01-01 12:00:00" |
| dateTimeNow() | 获取时间戳 | String | "20240101120000" |
| dateTimeNow(format) | 按格式获取时间 | String | `dateTimeNow("yyyy年MM月dd日")` |
| parseDateToStr(format, date) | Date转字符串 | String | `parseDateToStr("yyyy-MM-dd", date)` |
| parseDate(str) | 智能解析日期 | Date | 自动识别多种格式 |
| differentDaysByMillisecond(d1, d2) | 计算相差天数 | int | `differentDays(start, end)` |
| timeDistance(end, start) | 时间差描述 | String | "3天5小时20分钟" |

**常量定义**:

```java
public static final String YYYY = "yyyy";
public static final String YYYY_MM = "yyyy-MM";
public static final String YYYY_MM_DD = "yyyy-MM-dd";
public static final String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";
public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
```

**使用示例**:

```java
// 获取当前时间
String now = DateUtils.getTime();  // 2024-01-01 12:00:00

// 日期格式化
String formatted = DateUtils.parseDateToStr("yyyy年MM月dd日", new Date());

// 日期解析
Date date = DateUtils.parseDate("2024-01-01");

// 计算天数差
int days = DateUtils.differentDaysByMillisecond(startDate, endDate);

// 时间差描述
String distance = DateUtils.timeDistance(endTime, startTime);
// 输出: "3天5小时20分钟"
```

---

### 2.3 SecurityUtils（安全工具）

**功能清单**:

| 方法 | 说明 | 返回值 |
|------|------|--------|
| getUserId() | 获取当前用户ID | Long |
| getDeptId() | 获取部门ID | Long |
| getUsername() | 获取用户名 | String |
| getLoginUser() | 获取登录用户对象 | LoginUser |
| encryptPassword(password) | BCrypt加密 | String |
| matchesPassword(raw, encoded) | 验证密码 | boolean |
| isAdmin() | 判断是否管理员 | boolean |
| hasPermi(permission) | 验证权限 | boolean |
| hasRole(role) | 验证角色 | boolean |

**使用示例**:

```java
// 获取当前用户信息
Long userId = SecurityUtils.getUserId();
String username = SecurityUtils.getUsername();
LoginUser loginUser = SecurityUtils.getLoginUser();

// 密码加密与验证
String encrypted = SecurityUtils.encryptPassword("123456");
boolean valid = SecurityUtils.matchesPassword("123456", encrypted);

// 权限验证
if (SecurityUtils.hasPermi("system:user:add")) {
    // 有权限执行
}

// 角色验证
if (SecurityUtils.hasRole("admin")) {
    // 是管理员
}
```

---

### 2.4 ServletUtils（Servlet工具）

**功能清单**:

| 方法 | 说明 | 返回值 |
|------|------|--------|
| getParameter(name) | 获取请求参数 | String |
| getParameterToInt(name) | 获取Integer参数 | Integer |
| getParameterToBool(name) | 获取Boolean参数 | Boolean |
| getRequest() | 获取HttpServletRequest | HttpServletRequest |
| getResponse() | 获取HttpServletResponse | HttpServletResponse |
| getSession() | 获取HttpSession | HttpSession |
| renderString(response, json) | 输出JSON响应 | void |
| isAjaxRequest(request) | 判断Ajax请求 | boolean |
| urlEncode(str) | URL编码 | String |
| urlDecode(str) | URL解码 | String |

**使用示例**:

```java
// 获取请求参数
String name = ServletUtils.getParameter("username");
Integer page = ServletUtils.getParameterToInt("pageNum", 1);
Boolean flag = ServletUtils.getParameterToBool("enabled", false);

// 判断Ajax请求
if (ServletUtils.isAjaxRequest(request)) {
    ServletUtils.renderString(response, "{\"code\":200}");
}

// URL编解码
String encoded = ServletUtils.urlEncode("中文参数");
String decoded = ServletUtils.urlDecode(encoded);
```

---

### 2.5 DictUtils（字典工具）

**功能清单**:

| 方法 | 说明 | 返回值 |
|------|------|--------|
| getDictLabel(dictType, dictValue) | 值→标签 | String |
| getDictValue(dictType, dictLabel) | 标签→值 | String |
| getDictLabel(dictType, dictValue, separator) | 多值转换 | String |
| setDictCache(key, datas) | 设置缓存 | void |
| getDictCache(key) | 获取缓存 | List<SysDictData> |
| removeDictCache(key) | 删除缓存 | void |
| clearDictCache() | 清空所有缓存 | void |

**使用示例**:

```java
// 单个值转换
String label = DictUtils.getDictLabel("sys_user_sex", "0");  // "男"
String value = DictUtils.getDictValue("sys_user_sex", "男");  // "0"

// 多值转换（逗号分隔）
String labels = DictUtils.getDictLabel("sys_normal_disable", "0,1", ",");
// 输出: "正常,停用"

// 清除缓存
DictUtils.removeDictCache("sys_user_sex");
```

---

### 2.6 RedisCache（Redis缓存）

**功能清单**:

| 方法 | 说明 | 返回值 |
|------|------|--------|
| setCacheObject(key, value) | 缓存对象 | void |
| setCacheObject(key, value, timeout, unit) | 缓存(带过期) | void |
| getCacheObject(key) | 获取对象 | T |
| deleteObject(key) | 删除key | boolean |
| expire(key, timeout) | 设置过期时间 | boolean |
| setCacheList(key, dataList) | 缓存List | long |
| getCacheList(key) | 获取List | List<T> |
| setCacheSet(key, dataSet) | 缓存Set | BoundSetOperations |
| getCacheSet(key) | 获取Set | Set<T> |
| setCacheMap(key, dataMap) | 缓存Map | void |
| getCacheMap(key) | 获取Map | Map<String, T> |
| setCacheMapValue(key, hKey, value) | 设置Hash字段 | void |
| getCacheMapValue(key, hKey) | 获取Hash字段 | T |
| keys(pattern) | 模糊查询keys | Collection<String> |

**使用示例**:

```java
@Autowired
private RedisCache redisCache;

// 基本缓存
redisCache.setCacheObject("user:1", userObject);
User user = redisCache.getCacheObject("user:1");

// 带过期时间
redisCache.setCacheObject("token:abc", token, 30, TimeUnit.MINUTES);

// List缓存
redisCache.setCacheList("user:list", userList);
List<User> users = redisCache.getCacheList("user:list");

// Hash缓存
redisCache.setCacheMapValue("user:hash:1", "name", "张三");
String name = redisCache.getCacheMapValue("user:hash:1", "name");

// 模糊删除
Collection<String> keys = redisCache.keys("user:*");
redisCache.deleteObject(keys);
```

---

### 2.7 FileUploadUtils（文件上传）

**功能清单**:

| 方法 | 说明 | 返回值 |
|------|------|--------|
| upload(file) | 默认配置上传 | String (路径) |
| upload(baseDir, file) | 指定目录上传 | String |
| upload(baseDir, file, allowedExtension) | 指定扩展名 | String |
| extractFilename(file) | 生成编码文件名 | String |
| getExtension(file) | 获取扩展名 | String |

**常量**:

```java
public static final int DEFAULT_MAX_SIZE = 50 * 1024 * 1024;  // 50MB
```

**使用示例**:

```java
// 简单上传
String filePath = FileUploadUtils.upload(file);

// 指定目录和允许类型
String imagePath = FileUploadUtils.upload(
    "/profile/upload", 
    file, 
    MimeTypeUtils.IMAGE_EXTENSION  // {"png", "jpg", "jpeg", "gif"}
);

// 生成的文件名: 2024/01/01/originalName_123456.jpg
```

---

### 2.8 IpUtils（IP处理）

**功能清单**:

| 方法 | 说明 | 返回值 |
|------|------|--------|
| getIpAddr() | 获取客户端IP | String |
| internalIp(ip) | 判断内网IP | boolean |
| getHostIp() | 获取本机IP | String |
| isIP(ip) | 验证合法IP | boolean |
| isMatchedIp(filter, ip) | IP是否符合规则 | boolean |

**使用示例**:

```java
// 获取客户端IP
String ip = IpUtils.getIpAddr();

// 判断内网IP
if (IpUtils.internalIp(ip)) {
    // 内网访问
}

// IP白名单校验
boolean allowed = IpUtils.isMatchedIp(
    "192.168.1.*;10.0.0.1-10.0.0.100", 
    ip
);

// 验证IP格式
if (IpUtils.isIP(ip)) {
    // 合法IP
}
```

---

### 2.9 SpringUtils（Spring工具）

**功能清单**:

| 方法 | 说明 | 返回值 |
|------|------|--------|
| getBean(name) | 根据名称获取Bean | T |
| getBean(clz) | 根据类型获取Bean | T |
| containsBean(name) | 判断Bean是否存在 | boolean |
| getActiveProfile() | 获取激活环境 | String |
| getRequiredProperty(key) | 获取配置值 | String |

**使用示例**:

```java
// 获取Bean
UserService userService = SpringUtils.getBean(UserService.class);
RedisCache redisCache = SpringUtils.getBean(RedisCache.class);

// 获取配置
String profile = SpringUtils.getActiveProfile();  // dev/prod
String dbUrl = SpringUtils.getRequiredProperty("spring.datasource.url");
```

---

### 2.10 PageUtils（分页工具）

**功能清单**:

| 方法 | 说明 |
|------|------|
| startPage() | 开启分页 |
| clearPage() | 清理分页线程变量 |

**使用示例**:

```java
@GetMapping("/list")
public TableDataInfo list(SysUser user) {
    PageUtils.startPage();  // 开启分页
    List<SysUser> list = userService.selectUserList(user);
    return getDataTable(list);
}
```

---

## 三、其他重要工具类速查

### 3.1 Arith（精确运算）

```java
// 避免浮点数精度丢失
double result = Arith.add(0.1, 0.2);      // 0.3
result = Arith.sub(1.0, 0.9);             // 0.1
result = Arith.mul(0.1, 3);               // 0.3
result = Arith.div(1.0, 3, 2);            // 0.33
result = Arith.round(3.14159, 2);         // 3.14
```

### 3.2 IdUtils（ID生成）

```java
String uuid = IdUtils.randomUUID();           // 带横杠
String simpleUuid = IdUtils.simpleUUID();     // 不带横杠
String fastUuid = IdUtils.fastUUID();         // 高性能
```

### 3.3 Md5Utils（MD5加密）

```java
String md5 = Md5Utils.hash("password");  // 32位MD5
```

### 3.4 HttpUtils（HTTP请求）

```java
// GET请求
String result = HttpUtils.sendGet("http://api.example.com/data");

// POST请求
String result = HttpUtils.sendPost("http://api.example.com/data", "param=value");
```

### 3.5 ExceptionUtil（异常处理）

```java
// 获取完整堆栈信息
String stackTrace = ExceptionUtil.getExceptionMessage(e);

// 获取根本原因
String rootMsg = ExceptionUtil.getRootErrorMessage(e);
```

---

## 四、工具类依赖关系

```
StringUtils (最基础，被广泛引用)
    ├── DictUtils → RedisCache, SpringUtils
    ├── SecurityUtils → StringUtils
    ├── ServletUtils → StringUtils, Convert
    ├── IpUtils → ServletUtils, StringUtils
    ├── FileUploadUtils → StringUtils, DateUtils, IdUtils
    ├── SpringUtils → StringUtils
    
RedisCache (缓存核心)
    └── 被DictUtils、TokenService等多处使用
```

---

## 五、最佳实践

### 5.1 工具类选择原则

| 场景 | 推荐工具类 |
|------|-----------|
| 字符串判空 | StringUtils.isEmpty() |
| 日期格式化 | DateUtils.parseDateToStr() |
| 获取当前用户 | SecurityUtils.getLoginUser() |
| 字典转换 | DictUtils.getDictLabel() |
| 文件上传 | FileUploadUtils.upload() |
| Redis操作 | RedisCache |
| 精确计算 | Arith |
| ID生成 | IdUtils |

### 5.2 注意事项

1. **StringUtils优先**: 不要用`str == null`，用`StringUtils.isEmpty(str)`
2. **日期统一格式**: 使用DateUtils常量，不要硬编码格式字符串
3. **密码加密**: 必须使用SecurityUtils.encryptPassword()，不要明文存储
4. **文件上传**: 必须校验扩展名，防止上传恶意文件
5. **SQL安全**: 排序字段必须用SqlUtil.escapeOrderBySql()过滤

---

**上一章**: [Excel导入导出功能详解](✏️笔记/若依/后端/05-Excel导入导出功能详解.md)  
**下一章**: [分页处理机制详解](✏️笔记/若依/后端/07-分页处理机制详解.md)
