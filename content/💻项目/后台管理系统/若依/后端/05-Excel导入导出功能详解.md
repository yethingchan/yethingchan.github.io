# RuoYi-Vue Excel导入导出功能详解

## 一、功能概述

RuoYi-Vue基于 **Apache POI** 实现了强大的Excel导入导出功能，通过简单的注解配置即可完成复杂的Excel操作。

**核心特性**:
- 基于注解驱动，零代码实现导出
- 支持字典自动转换
- 支持表达式转换（0→男,1→女）
- 支持图片导入导出
- 支持多Sheet导出
- 大数据量优化（SXSSFWorkbook）
- 防止CSV注入攻击

---

## 二、@Excel注解详解

### 2.1 完整属性列表

```java
@Excel(
    // ========== 基础属性 ==========
    name = "用户名称",                    // 列头名称
    sort = 1,                            // 排序（值越小越靠前）
    type = Type.ALL,                     // ALL/EXPORT/IMPORT
    
    // ========== 数据转换 ==========
    dateFormat = "yyyy-MM-dd",           // 日期格式
    dictType = "sys_user_sex",          // 字典类型
    readConverterExp = "0=男,1=女",     // 表达式转换
    separator = ",",                     // 分隔符
    
    // ========== 样式配置 ==========
    width = 30,                          // 列宽
    height = 14,                         // 行高
    align = HorizontalAlignment.CENTER,  // 对齐方式
    suffix = "%",                        // 后缀
    defaultValue = "-",                  // 空值默认值
    
    // ========== 高级功能 ==========
    prompt = "请输入用户名",              // 提示信息
    combo = {"男", "女"},               // 下拉框选项
    comboReadDict = false,               // 从字典读取下拉框
    needMerge = false,                   // 纵向合并单元格
    isStatistics = false,                // 自动统计
    cellType = ColumnType.STRING,        // 单元格类型
    
    // ========== 跨对象属性 ==========
    targetAttr = "deptName"              // 关联对象属性
)
private String userName;
```

### 2.2 枚举类型

```java
// Type - 字段用途
enum Type {
    ALL(0),      // 既导出也导入
    EXPORT(1),   // 仅导出
    IMPORT(2)    // 仅导入
}

// ColumnType - 单元格类型
enum ColumnType {
    NUMERIC(0),  // 数字（防止科学计数法用TEXT）
    STRING(1),   // 字符串
    IMAGE(2),    // 图片
    TEXT(3)      // 文本（防CSV注入）
}
```

---

## 三、实体类配置示例

### 3.1 用户实体

**文件**: `SysUser.java`

```java
public class SysUser extends BaseEntity {
    
    @Excel(name = "用户序号", type = Type.EXPORT, cellType = ColumnType.NUMERIC)
    private Long userId;
    
    @Excel(name = "部门编号", type = Type.IMPORT)
    private Long deptId;
    
    @Excel(name = "用户名称", width = 30)
    private String userName;
    
    @Excel(name = "用户昵称")
    private String nickName;
    
    @Excel(name = "用户邮箱")
    private String email;
    
    @Excel(name = "手机号码", cellType = ColumnType.TEXT)
    private String phonenumber;
    
    // 表达式转换：0→男, 1→女, 2→未知
    @Excel(name = "用户性别", readConverterExp = "0=男,1=女,2=未知")
    private String sex;
    
    // 字典转换：从sys_normal_disable字典获取标签
    @Excel(name = "帐号状态", dictType = "sys_normal_disable")
    private String status;
    
    // 日期格式化
    @Excel(name = "最后登录时间", width = 30, 
           dateFormat = "yyyy-MM-dd HH:mm:ss", type = Type.EXPORT)
    private Date loginDate;
    
    // 跨对象属性（dept.deptName）
    @Excels({
        @Excel(name = "部门名称", targetAttr = "deptName", type = Type.EXPORT),
        @Excel(name = "部门负责人", targetAttr = "leader", type = Type.EXPORT)
    })
    private SysDept dept;
    
    // 带后缀
    @Excel(name = "消耗时间", suffix = "毫秒")
    private Long costTime;
    
    // 自动统计
    @Excel(name = "金额", isStatistics = true)
    private BigDecimal amount;
    
    // getter/setter...
}
```

---

## 四、导出功能实现

### 4.1 Controller导出方法

**标准导出**:

```java
@RestController
@RequestMapping("/system/user")
public class SysUserController extends BaseController {
    
    @Autowired
    private ISysUserService userService;
    
    /**
     * 导出用户数据
     */
    @Log(title = "用户管理", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('system:user:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysUser user) {
        // 1. 查询数据
        List<SysUser> list = userService.selectUserList(user);
        
        // 2. 创建ExcelUtil实例
        ExcelUtil<SysUser> util = new ExcelUtil<>(SysUser.class);
        
        // 3. 导出到响应流（浏览器直接下载）
        util.exportExcel(response, list, "用户数据");
    }
}
```

### 4.2 导出变体

#### 带标题的导出

```java
@PostMapping("/exportWithTitle")
public void exportWithTitle(HttpServletResponse response) {
    List<SysUser> list = userService.selectUserList(new SysUser());
    ExcelUtil<SysUser> util = new ExcelUtil<>(SysUser.class);
    util.exportExcel(response, list, "用户数据", "用户信息报表");
}
```

#### 选择性导出列

```java
@PostMapping("/exportSelective")
public void exportSelective(HttpServletResponse response) {
    List<SysUser> list = userService.selectUserList(new SysUser());
    ExcelUtil<SysUser> util = new ExcelUtil<>(SysUser.class);
    
    // 只显示指定列
    util.showColumn("userName", "nickName", "phonenumber");
    util.exportExcel(response, list, "用户数据");
}
```

#### 隐藏某些列

```java
util.hideColumn("password", "delFlag");
```

#### 多Sheet导出

```java
@PostMapping("/exportMultiSheet")
public void exportMultiSheet(HttpServletResponse response) {
    List<SysUser> userList = userService.selectUserList(new SysUser());
    List<SysRole> roleList = roleService.selectRoleAll();
    
    List<ExcelSheet<?>> sheets = new ArrayList<>();
    sheets.add(new ExcelSheet<>("用户数据", userList, SysUser.class, "用户信息"));
    sheets.add(new ExcelSheet<>("角色数据", roleList, SysRole.class, "角色信息"));
    
    ExcelUtil.exportMultiSheet(response, sheets);
}
```

---

## 五、导入功能实现

### 5.1 Controller导入方法

```java
@RestController
@RequestMapping("/system/user")
public class SysUserController extends BaseController {
    
    /**
     * 导入用户数据
     */
    @Log(title = "用户管理", businessType = BusinessType.IMPORT)
    @PreAuthorize("@ss.hasPermi('system:user:import')")
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) 
            throws Exception {
        
        // 1. 创建ExcelUtil实例
        ExcelUtil<SysUser> util = new ExcelUtil<>(SysUser.class);
        
        // 2. 从文件流导入
        List<SysUser> userList = util.importExcel(file.getInputStream());
        
        // 3. 业务层处理导入数据
        String operName = getUsername();
        String message = userService.importUser(userList, updateSupport, operName);
        
        return success(message);
    }
    
    /**
     * 下载导入模板
     */
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) {
        ExcelUtil<SysUser> util = new ExcelUtil<>(SysUser.class);
        util.importTemplateExcel(response, "用户数据");
    }
}
```

### 5.2 Service层导入逻辑

```java
@Service
public class SysUserServiceImpl implements ISysUserService {
    
    @Override
    @Transactional
    public String importUser(List<SysUser> userList, Boolean isUpdateSupport, 
                             String operName) {
        
        if (StringUtils.isNull(userList) || userList.size() == 0) {
            throw new ServiceException("导入用户数据不能为空！");
        }
        
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        
        for (SysUser user : userList) {
            try {
                // 验证账号是否存在
                SysUser existUser = selectUserByUserName(user.getUserName());
                
                if (StringUtils.isNull(existUser)) {
                    // 新增用户
                    this.insertUser(user);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、账号 " + 
                        user.getUserName() + " 导入成功");
                } else if (isUpdateSupport) {
                    // 更新用户
                    user.setUserId(existUser.getUserId());
                    this.updateUser(user);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、账号 " + 
                        user.getUserName() + " 更新成功");
                } else {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、账号 " + 
                        user.getUserName() + " 已存在");
                }
            } catch (Exception e) {
                failureNum++;
                String msg = "<br/>" + failureNum + "、账号 " + 
                    user.getUserName() + " 导入失败：";
                failureMsg.append(msg + e.getMessage());
            }
        }
        
        if (failureNum > 0) {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + 
                " 条数据格式不正确，错误如下：");
            throw new ServiceException(failureMsg.toString());
        } else {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + 
                successNum + " 条，数据如下：");
        }
        
        return successMsg.toString();
    }
}
```

---

## 六、字典转换机制

### 6.1 导出时：字典值 → 字典标签

```java
// ExcelUtil.addCell() 方法中
if (StringUtils.isNotEmpty(dictType) && StringUtils.isNotNull(value)) {
    // 从缓存获取字典标签
    String label = DictUtils.getDictLabel(dictType, value.toString(), separator);
    cell.setCellValue(label);
}
```

**示例**:
```
数据库存储: sex = "0"
Excel显示: "男"（从sys_user_sex字典获取）
```

### 6.2 导入时：字典标签 → 字典值

```java
// ExcelUtil.importExcel() 方法中
if (StringUtils.isNotEmpty(attr.dictType())) {
    // 反向转换：标签 → 值
    String dictValue = DictUtils.getDictValue(dictType, val.toString(), separator);
    val = dictValue;
}
```

**示例**:
```
Excel填写: "男"
数据库存储: sex = "0"
```

### 6.3 字典缓存

**文件**: `DictUtils.java`

```java
public class DictUtils {
    
    /**
     * 根据字典类型和字典值获取字典标签
     */
    public static String getDictLabel(String dictType, String dictValue, 
                                      String separator) {
        // 1. 从Redis获取字典数据
        List<SysDictData> datas = getDictCache(dictType);
        
        // 2. 构建 Map<值, 标签>
        Map<String, String> dictMap = datas.stream()
            .collect(Collectors.toMap(
                SysDictData::getDictValue, 
                SysDictData::getDictLabel
            ));
        
        // 3. 查找并返回
        return dictMap.getOrDefault(dictValue, "");
    }
    
    /**
     * 设置字典缓存
     */
    public static void setDictCache(String key, List<SysDictData> datas) {
        SpringUtils.getBean(RedisCache.class)
            .setCacheObject(getCacheKey(key), datas);
    }
}
```

---

## 七、常见问题与解决方案

### 问题1: 手机号显示为科学计数法

**原因**: Excel自动将长数字转换为科学计数法

**解决**: 使用 `ColumnType.TEXT`

```java
@Excel(name = "手机号码", cellType = ColumnType.TEXT)
private String phonenumber;
```

### 问题2: 日期格式不正确

**解决**: 指定dateFormat

```java
@Excel(name = "创建时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
private Date createTime;
```

### 问题3: 大数据量导出OOM

**解决**: RuoYi已使用SXSSFWorkbook优化

```java
// ExcelUtil.createWorkbook()
this.wb = new SXSSFWorkbook(500);  // 窗口大小500行
```

超大数据量建议：
- 分批导出
- 增加JVM内存：`-Xmx2g`

### 问题4: CSV注入安全

**解决**: RuoYi已自动处理

```java
// 以 =-+@ 开头的单元格添加\t前缀
if (StringUtils.startsWithAny(cellValue, "=", "-", "+", "@")) {
    cellValue = "\t" + cellValue;
}
```

### 问题5: 下拉框选项过多

**解决**: RuoYi自动处理

```java
if (comboArray.length > 15 || totalLength > 255) {
    // 使用隐藏sheet存储
    setXSSFValidationWithHidden(...);
} else {
    // 直接使用下拉框
    setPromptOrValidation(...);
}
```

---

## 八、自定义数据处理器

### 8.1 实现ExcelHandlerAdapter接口

```java
public class CustomHandler implements ExcelHandlerAdapter {
    
    @Override
    public Object format(Object value, String[] args, Cell cell, Workbook wb) {
        // 自定义格式化逻辑
        if (value != null) {
            return "前缀-" + value.toString();
        }
        return value;
    }
}
```

### 8.2 在实体中使用

```java
@Excel(name = "自定义字段", 
       handler = CustomHandler.class, 
       args = {"param1", "param2"})
private String customField;
```

---

## 九、总结

### 9.1 使用流程

**导出**:
```
1. 实体类添加@Excel注解
2. Controller查询数据
3. 创建ExcelUtil实例
4. 调用exportExcel()
```

**导入**:
```
1. 实体类添加@Excel注解
2. Controller接收MultipartFile
3. 调用util.importExcel()
4. Service层处理业务逻辑
```

### 9.2 关键要点

1. **注解驱动**: 只需配置注解，无需编写POI代码
2. **字典转换**: 自动从Redis缓存获取字典数据
3. **大数据优化**: SXSSFWorkbook内存友好
4. **安全防护**: 防止CSV注入、公式注入
5. **灵活配置**: 支持表达式、自定义处理器、多Sheet

---

**上一章**: [权限管理系统深度解析](./04-权限管理系统深度解析.md)  
**下一章**: [Common工具类完全指南](./06-Common工具类完全指南.md)
