# @RestControllerAdvice 是干什么的？

**全局异常统一处理 + 全局数据绑定 + 全局数据预处理**

最常用的就是：**全局捕获异常，统一返回格式**。

例子：

java

运行

```
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result handle(Exception e) {
        return Result.fail(e.getMessage());
    }
}
```

作用：

- 整个项目所有接口报错都会进入这里
- 前端永远收到统一格式 {code,msg,data}
- 不用每个接口写 try-catch