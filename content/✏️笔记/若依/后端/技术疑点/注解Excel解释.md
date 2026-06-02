我直接**逐行、逐段、大白话、零基础**给你讲透这个类，**你看完绝对完全懂**！
这是 **若依框架里最常用的 Excel 导出/导入注解**，作用就是：
**给实体类字段打个标记，告诉系统：这个字段要导出到 Excel，并且按我指定的样式生成。**

---

# 一、最开头两行（你刚问过的）
```java
@Retention(RetentionPolicy.RUNTIME)  // 运行时生效，能被反射读取
@Target(ElementType.FIELD)           // 只能写在【成员变量】上
```
**意思：这个 @Excel 注解只能贴在变量上，程序跑起来时能读到它。**

---

# 二、注解定义
```java
public @interface Excel
```
**定义一个名字叫 Excel 的注解**
以后你在实体类里就能写：
```java
@Excel(name = "用户名")
private String userName;
```

---

# 三、里面一堆配置 = **导出Excel的规则**
我给你**每个属性用最简单的话讲**：

## 1. 排序
```java
int sort() default Integer.MAX_VALUE;
```
导出到 Excel 时，**列的先后顺序**，数字越小越靠前。

## 2. 列名（最常用！）
```java
String name() default "";
```
**Excel 表头显示什么文字**
比如：`@Excel(name = "用户姓名")`

## 3. 日期格式化
```java
String dateFormat() default "";
```
日期导出格式，如：`yyyy-MM-dd`

## 4. 字典类型（超级常用）
```java
String dictType() default "";
```
比如性别：`sys_user_sex`
系统会自动把 0→男，1→女

## 5. 手动翻译值
```java
String readConverterExp() default "";
```
直接写：`0=男,1=女`
不用配字典，直接翻译

## 6. 分隔符
```java
String separator() default ",";
```
数组转字符串用的分隔符

## 7. 数值精度（小数保留几位）
```java
int scale() default -1;
```
保留 2 位小数就写 `scale=2`

## 8. 列高度、宽度
```java
double height() default 14;
double width() default 16;
```
Excel 单元格大小

## 9. 后缀
```java
String suffix() default "";
```
比如 `suffix="%"`
数字 90 → 导出变成 90%

## 10. 默认值
```java
String defaultValue() default "";
```
值为空时显示什么

## 11. 下拉框
```java
String[] combo() default {};
```
导出 Excel 带下拉选择框，方便导入

## 12. 是否导出
```java
boolean isExport() default true;
```
要不要把这个字段导出

## 13. 对齐方式、颜色、字体
```java
HorizontalAlignment align() default CENTER;
IndexedColors headerBackgroundColor() default GREY;
```
设置 Excel 样式：居中、背景色、字体颜色

## 14. 字段类型（导入/导出/都支持）
```java
Type type() default Type.ALL;
```
- ALL：导出+导入
- EXPORT：仅导出
- IMPORT：仅导入

## 15. 单元格类型（数字/文本/图片）
```java
ColumnType cellType() default STRING;
```
数字、文本、图片、纯文本

---

# 四、里面两个枚举
## 1. Type（导入导出类型）
```java
ALL(0), EXPORT(1), IMPORT(2)
```

## 2. ColumnType（单元格数据类型）
```java
NUMERIC(0), STRING(1), IMAGE(2), TEXT(3)
```

---

# 🔥 最终一句话总结（最关键）
## **@Excel 是一个“Excel导出配置注解”**
你只要在实体类字段上加：
```java
@Excel(name = "性别", dictType = "sys_user_sex")
private String sex;
```
系统就能**自动生成漂亮的 Excel**，包含：
- 表头
- 格式
- 颜色
- 翻译
- 下拉框
- 对齐方式
- 宽度高度

**不用你写一行 POI 操作代码！**

---

# 你以后会经常这样用（真实项目代码）
```java
public class User {
    @Excel(name = "用户ID", sort = 1)
    private Long id;

    @Excel(name = "用户名", sort = 2)
    private String userName;

    @Excel(name = "性别", dictType = "sys_user_sex", sort = 3)
    private String sex;
}
```

---

### 超级简单记忆
**@Excel = 给字段打标签，让系统自动帮你导出漂亮的Excel表格**

---
 
[常用Excel注解解释](✏️笔记/若依/后端/技术疑点/常用Excel注解解释.md)

