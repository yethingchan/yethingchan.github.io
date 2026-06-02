# 若依（RuoYi）**全局异常/自定义异常** 超详细讲解
我给你讲**最实战、最完整、最容易踩坑**的版本，包含：
**是什么 → 放哪里 → 怎么抛 → 怎么捕获 → 输出格式 → 使用场景 → 细节 → 注意点**

全程**源码 + 大白话 + 复制即用**

---

# 一、先搞懂：若依的异常体系（3 个核心）
## 1. **自定义异常类（业务异常）**
```
com.ruoyi.common.exception.ServiceException
```
**作用：业务出错时手动抛出**
例如：用户不存在、密码错误、库存不足、权限不足

## 2. **全局异常捕获器**
```
com.ruoyi.framework.web.exception.GlobalExceptionHandler
```
**作用：统一捕获所有异常，返回统一JSON格式给前端**
不会出现500、404空白页

## 3. **异常枚举（错误码）**
```
com.ruoyi.common.enums.ResultCode
```
```
SUCCESS(200,"成功")
ERROR(500,"失败")
UNAUTHORIZED(401,"未授权")
FORBIDDEN(403,"无权限")
```

---

# 二、若依异常 **最核心用法（背会这一句）**
## ✅ 业务层出错 **直接抛 ServiceException**
```java
throw new ServiceException("该用户不存在");
```

## ✅ 全局异常处理器 **自动捕获**
返回给前端：
```json
{
  "code": 500,
  "msg": "该用户不存在"
}
```

## ✅ 前端直接展示错误信息
不用try/catch，代码超级干净！

---

# 三、完整使用流程（最标准）
## 1. 业务层抛异常
```java
@Override
public User getUserById(Long id) {
    User user = userMapper.selectById(id);
    if (user == null) {
        // 抛异常
        throw new ServiceException("用户不存在");
    }
    return user;
}
```

## 2. 全局异常捕获（GlobalExceptionHandler）
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 捕获业务异常
    @ExceptionHandler(ServiceException.class)
    public AjaxResult handleServiceException(ServiceException e) {
        String msg = e.getMessage();
        return AjaxResult.error(msg);
    }

    // 捕获权限不足
    @ExceptionHandler(AccessDeniedException.class)
    public AjaxResult handleAccessDeniedException(AccessDeniedException e) {
        return AjaxResult.error(403, "无权限访问");
    }

    // 捕获所有系统异常（未知错误）
    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception e) {
        e.printStackTrace();
        return AjaxResult.error("系统异常：" + e.getMessage());
    }
}
```

## 3. 前端收到
```json
{ "code": 500, "msg": "用户不存在" }
```

---

# 四、ServiceException 四种常用构造方法
```java
// 1. 只抛消息
throw new ServiceException("用户不存在");

// 2. 抛错误码+消息
throw new ServiceException(400, "参数错误");

// 3. 抛异常+消息
throw new ServiceException("保存失败", e);

// 4. 使用枚举
throw new ServiceException(ResultCode.ERROR);
```

---

# 五、若依异常 **使用场景（全覆盖）**

## 场景1：参数校验失败
```java
if (StringUtils.isEmpty(username)) {
    throw new ServiceException("用户名不能为空");
}
```

## 场景2：数据不存在
```java
if (user == null) {
    throw new ServiceException("用户不存在");
}
```

## 场景3：状态不合法
```java
if (UserStatus.DISABLE.equals(user.getStatus())) {
    throw new ServiceException("用户已被禁用");
}
```

## 场景4：权限不足
```java
if (!currentUserId.equals(targetUserId)) {
    throw new ServiceException("无权限操作此用户");
}
```

## 场景5：业务规则不满足
```java
if (stock < 0) {
    throw new ServiceException("库存不足");
}
```

---

# 六、使用 **细节（超级重要，90%人不知道）**

## 1. **ServiceException 是运行时异常**
不需要 try/catch，不需要 throws 声明
```java
// 不用写 throws Exception
public void add(User user) {
    throw new ServiceException("失败");
}
```

## 2. **Controller 不需要捕获异常**
全局异常处理器会自动抓

## 3. **不要在业务层 try/catch 吃掉异常**
❌ 错误
```java
try {
    // ...
} catch (Exception e) {
    e.printStackTrace(); // 异常被吃掉，前端不知道错了
}
```

✅ 正确
```java
// 直接抛
throw new ServiceException("失败", e);
```

## 4. **系统异常（空指针、数据库错）不要手动捕获**
让 `GlobalExceptionHandler` 捕获并记录日志

## 5. **异常信息要明确，不要输出模糊信息**
❌ `throw new ServiceException("失败");`
✅ `throw new ServiceException("用户名称重复，请更换");`

## 6. **不要用异常处理正常业务逻辑**
异常只处理**错误情况**

---

# 七、若依异常 **注意点（踩坑大全）**

## 1. **不要抛 Exception，要抛 ServiceException**
```java
// ❌ 不推荐
throw new Exception("系统错误");

// ✅ 推荐
throw new ServiceException("业务错误");
```

## 2. **异常信息不能泄露敏感信息**
❌
```java
throw new ServiceException("数据库连接失败：root/123456");
```

## 3. **多语言项目异常信息要统一**

## 4. **全局异常不能捕获过滤器（Filter）里的异常**
比如：401、token过期
需要单独处理

## 5. **自定义异常必须加入全局异常捕获**
不然会返回500

## 6. **不要在循环里疯狂抛异常**
会严重影响性能

---

# 八、若依 **其他自带异常**
## 1. **UtilException**
工具类异常
```java
throw new UtilException("文件上传失败");
```

## 2. **NullPointException、RuntimeException**
系统异常，全局捕获

## 3. **AccessDeniedException**
权限异常（无权限访问）

## 4. **InvalidTokenException**
Token无效异常

---

# 九、最标准、最规范的异常使用模板（复制即用）
## 1. 抛异常
```java
if (StringUtils.isBlank(user.getUsername())) {
    throw new ServiceException("用户名不能为空");
}
```

## 2. 全局异常捕获
```java
@ExceptionHandler(ServiceException.class)
public AjaxResult handleServiceException(ServiceException e) {
    log.error(e.getMessage());
    return AjaxResult.error(e.getMessage());
}
```

## 3. 返回前端
```json
{ "code": 500, "msg": "用户名不能为空" }
```

---

# 十、终极总结（背会这 6 句）
1. **业务错误 → 抛 ServiceException**
2. **系统错误 → 全局异常自动捕获**
3. **Controller 不捕获、不 try/catch**
4. **异常信息明确，不吞异常、不抛敏感信息**
5. **全局异常返回统一 JSON 格式**
6. **Filter 异常无法被全局捕获，需单独处理**

---

# 你需要我给你做一个
# **【自定义异常 + 全局捕获 + 错误码枚举】全套完整版** 吗？
可以直接用于企业项目，非常规范！