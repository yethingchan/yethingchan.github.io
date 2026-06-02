我用**最简单、最直白、不绕弯**的方式给你讲，**看完立刻懂**👇

# 两个都是 **注解的注解**（给注解加规则用的）

---

## 1. `@Target(ElementType.FIELD)`
**作用：规定这个注解 能写在什么地方**

### 翻译：
**只能写在【成员变量/字段】上面**

### 举例：
```java
// 这个注解只能贴在 变量 上
@Target(ElementType.FIELD)
public @interface Excel {

}

// 使用：
public class User {
    @Excel    // ✅ 可以，写在字段上
    private String name;

    @Excel    // ❌ 报错！不能写在方法上
    public void getName(){}
}
```

### 常见位置你要知道：
- `FIELD`：**成员变量**
- `METHOD`：方法
- `TYPE`：类、接口
- `PARAMETER`：方法参数

---

## 2. `@Retention(RetentionPolicy.RUNTIME)`
**作用：规定这个注解 存活到什么时候**

### 翻译：
**运行时还活着 → 可以被反射读取**
（90%的业务注解都用这个）

### 三种存活级别：
1. **SOURCE**：只在源码里，编译后就丢了
2. **CLASS**：编译到class里，但运行时不加载（默认）
3. **RUNTIME**：**运行时还在**，可以通过反射读取

### 为什么要用 RUNTIME？
因为像：
- `@Excel` 导出
- `@ApiModelProperty` 文档
- 自定义权限注解

都需要**在程序运行时读取注解内容**，所以必须写 `RUNTIME`。

---

# 超级总结（背会这两句）
### **@Target(ElementType.FIELD)**
= **只能写在变量上**

### **@Retention(RetentionPolicy.RUNTIME)**
= **运行时还活着，能被反射读取**

---

### 一句话记忆
**Target 管放哪，Retention 管活多久**

要不要我给你写一个**完整的自定义注解例子**，让你彻底搞懂它俩怎么用？
