没问题！我给你整理一张**最全、最清晰、可直接对照使用**的 **@Excel 注解属性用法表**，覆盖你发的代码里**所有属性**，每个都配**作用 + 示例**，工作直接查表就行！

# 📘 @Excel 注解全属性一览表（若依完整版）
| 属性名 | 作用 | 常用值/示例 |
|---|---|---|
| **name** | Excel 表头列名（必填） | `name = "用户姓名"` |
| **sort** | 列排序（数字越小越靠前） | `sort = 1` |
| **dateFormat** | 日期格式化 | `dateFormat = "yyyy-MM-dd HH:mm:ss"` |
| **dictType** | 系统字典翻译 | `dictType = "sys_user_sex"` |
| **readConverterExp** | 手动值翻译（不用字典） | `readConverterExp = "0=正常,1=停用"` |
| **separator** | 数组分隔符 | `separator = ","` |
| **scale** | 小数保留位数（BigDecimal） | `scale = 2` |
| **roundingMode** | 小数四舍五入规则 | 默认即可 |
| **height** | 行高 | `height = 14` |
| **width** | 列宽 | `width = 20` |
| **suffix** | 值后缀（%、元等） | `suffix = "%"` |
| **defaultValue** | 空值时显示的默认值 | `defaultValue = "无"` |
| **prompt** | 提示信息 | `prompt = "请输入姓名"` |
| **wrapText** | 是否自动换行 | `wrapText = true` |
| **combo** | 下拉框选项 | `combo = {"男","女"}` |
| **comboReadDict** | 下拉框从字典读取 | `comboReadDict = true` |
| **needMerge** | 是否合并单元格 | `needMerge = true` |
| **isExport** | 是否导出此字段 | `isExport = false` |
| **targetAttr** | 关联对象属性 | `targetAttr = "dept.deptName"` |
| **isStatistics** | 是否自动统计求和 | `isStatistics = true` |
| **cellType** | 单元格类型 | 0数字 1文本 2图片 3纯文本 |
| **headerBackgroundColor** | 表头背景色 | `GREY_50_PERCENT` |
| **headerColor** | 表头字体颜色 | `WHITE` |
| **backgroundColor** | 单元格背景色 | `WHITE` |
| **color** | 字体颜色 | `BLACK` |
| **align** | 对齐方式 | LEFT / CENTER / RIGHT |
| **handler** | 自定义数据处理器 | 自定义class |
| **args** | 处理器参数 | 字符串数组 |
| **type** | 导出/导入类型 | ALL/EXPORT/IMPORT |

---

# 📌 最常用组合（直接复制）
## 1. 基础导出
```java
@Excel(name = "用户姓名", sort = 1)
```

## 2. 日期导出
```java
@Excel(name = "创建时间", dateFormat = "yyyy-MM-dd HH:mm:ss", sort = 2)
```

## 3. 字典翻译
```java
@Excel(name = "性别", dictType = "sys_user_sex", sort = 3)
```

## 4. 手动翻译
```java
@Excel(name = "状态", readConverterExp = "0=正常,1=停用", sort = 4)
```

## 5. 金额保留2位小数
```java
@Excel(name = "余额", scale = 2, sort = 5)
```

## 6. 百分比
```java
@Excel(name = "占比", suffix = "%", sort = 6)
```

## 7. 下拉框
```java
@Excel(name = "学历", combo = {"大专","本科","硕士"}, sort = 7)
```

## 8. 不导出此字段
```java
@Excel(isExport = false)
```

## 9. 仅导出、不导入
```java
@Excel(name = "操作人", type = Type.EXPORT)
```

## 10. 宽度设置
```java
@Excel(name = "邮箱", width = 25)
```

---

# 🎯 两个内部枚举（完整）
## Type（导入导出类型）
```java
ALL(0)      // 导出+导入
EXPORT(1)   // 仅导出
IMPORT(2)   // 仅导入
```

## ColumnType（单元格类型）
```java
NUMERIC(0)  // 数字
STRING(1)   // 字符串
IMAGE(2)    // 图片
TEXT(3)     // 文本
```

---

需要我把这张表**生成一张图片版**给你保存吗？随时可以打开看！