# 若依(RuoYi-Vue)XSS全套详解：双防护（全局XssFilter + @Xss注解）、用法、场景、细节、踩坑注意
若依采用**双层XSS防护架构**：**全局过滤器自动转义（默认开启）+ @Xss注解强校验拦截**，两套机制分工不同、互补防护，杜绝**存储型/反射型/DOM型XSS**（恶意`<script>、onclick、javascript:`注入盗取Cookie、篡改页面）。

## 一、若依XSS核心5个基础类（存放位置）
| 类路径 | 作用 |
| ---- | ---- |
| `com.ruoyi.common.filter.XssFilter` | XSS全局过滤器入口，yml配置生效 |
| `com.ruoyi.common.filter.XssHttpServletRequestWrapper` | 请求包装器，GET/POST/JSON参数过滤转义 |
| `com.ruoyi.framework.config.properties.XssProperties` | 读取yml配置（开关、排除路径、拦截路径） |
| `com.ruoyi.common.xss.@Xss` | 字段/参数校验注解 |
| `com.ruoyi.common.xss.XssValidator` | @Xss注解的校验实现类（JSoup黑名单校验） |

## 二、第一种：全局XssFilter过滤器（自动全局过滤，项目最常用）
### 1.application.yml配置（核心配置，默认配置）
```yaml
# XSS防跨站配置
xss:
  enabled: true          # true开启全局过滤；false关闭
  excludes: /system/notice,/common/editor/upload # 排除接口（不走过滤）
  urlPatterns: /system/*,/monitor/*,/tool/* # 需要过滤的接口路径
```
- **生效规则**：`FilterConfig`里注册XssFilter，**过滤器优先级最高(HIGHEST_PRECEDENCE)**，早于权限过滤器执行
- **放行规则（默认）**：
  1. GET请求**默认跳过XssFilter**（若依设计：GET参数由前端Vue插值自动转义，后端不过滤）；
  2. 配置在`excludes`排除列表的接口，无论什么请求直接放行；
  3. POST/PUT/DELETE JSON表单参数进入包装器做HTML转义。

### 2.过滤器过滤逻辑（XssHttpServletRequestWrapper）
1. **普通表单参数（form-data）**：把`< > & ' " `转成HTML实体 `&lt; &gt; &amp;`，恶意`<script>alert(1)</script>`变成纯文本入库，前端渲染不会执行脚本；
2. **JSON请求体（@RequestBody）**：递归解析JSON所有字符串字段，批量转义危险标签，**不会破坏JSON结构**；
> 特点：**只转义、不报错**，恶意标签自动净化，接口正常接收数据。

### 3.使用场景（全局过滤器适用）
1. **普通单行输入框**：用户名、手机号、备注、搜索框、字典名称（无富文本、无HTML）；
2. **批量导入Excel普通文本字段**：导入的姓名、地址纯文本，自动过滤脚本；
3. **绝大多数业务CRUD接口**：系统管理、菜单、角色、部门等默认被`/system/*`拦截过滤；
4. **URL传参（POST携带Query参数）**：反射型XSS防护。

### 4.排除路径什么时候配置？
**富文本编辑器接口必须加excludes排除**：公告详情、文章编辑、富文本上传，需要保存完整HTML（`<p><img><b>`），不能被转义，示例：
```yaml
excludes: /system/notice/edit,/article/save
```
> 被排除的接口**过滤器不再转义**，改用**@Xss(allowHtml=true)**白名单放行合法HTML。

## 三、第二种：@Xss注解（精准字段拦截，强校验，不转义、非法直接抛异常）
### 1.注解源码与两种用法
```java
//1.默认：禁止任何HTML/脚本，包含标签直接报错
@Xss(message = "账号不能包含脚本字符")
private String userName;

//2.allowHtml=true：放行合法HTML（p/img/b等白名单标签，拦截script/onerror/javascript）
@Xss(allowHtml = true,message = "内容包含非法脚本代码")
private String content; //富文本字段专用
```
**区别过滤器：@Xss发现恶意代码直接抛出校验异常，全局异常捕获返回前端报错，不会自动转义数据**。

### 2.两种标注位置
#### ①实体类字段（最常用，接收@RequestBody JSON）
```java
public class SysUser {
    private Long userId;
    @Xss(message = "登录账号不能包含脚本、特殊标签")
    private String userName;
    private String nickName;
}
```
#### ②Controller入参（@RequestParam单个参数）
```java
@GetMapping("/list")
public AjaxResult list(@Xss String keyword){}
```

### 3.@Xss三大使用场景（精准管控高危字段）
1. **账号、登录名、编码字段（严禁带任何HTML）**：用户名、角色编码，一旦输入`<script>`直接拦截报错，防止恶意注册注入；
2. **评论、留言、用户自定义输入（高危存储型XSS）**：用户留言区，防止存恶意JS窃取管理员Cookie；
3. **富文本字段**：`@Xss(allowHtml=true)`，白名单放行`<p><img><span>`，黑名单拦截`<script>、onload、javascript:alert()`，**富文本标准写法**；
4. **导入Excel单元格内容校验**：Excel批量导入时单元格带恶意脚本，注解拦截导入失败。

### 过滤器VS@Xss注解对比（重中之重）
| 特性 | Xss全局过滤器 | @Xss注解 |
| ---- | ---- | ---- |
| 处理方式 | 自动转义危险字符，**数据净化，接口正常保存** | 发现非法内容**直接抛异常，拒绝保存** |
| 生效范围 | yml配置路径下全接口全局生效 | 仅标注字段/参数生效 |
| 富文本适配 | 开启就全转义HTML（不适合富文） | allowHtml=true白名单放行合法标签 |
| 适用场景 | 普通纯文本字段 | 关键敏感字段、富文本、用户自由输入 |

## 四、Vue前端配套XSS防护（前后端双保险）
1. Vue插值`{{ msg }}`**默认自动转义HTML**，无法执行JS（天然防DOM-XSS）；
2. 富文本使用`v-html`渲染内容：**后端必须@Xss(allowHtml=true)做白名单过滤**，禁止前端直接保存原始HTML入库；
3. 前端输入框校验：特殊字符前端正则预拦截，减少后端报错。

## 五、高频踩坑点&注意事项（项目90%问题来源）
### 1.富文本编辑保存报错「目标字符串不在白名单内」
原因：接口在XssFilter拦截路径里，全局过滤器自动转义所有HTML，富文本标签被变成`&lt;p&gt;`；
**解决方案二选一**：
①接口路径加到yml`excludes`排除列表，关闭过滤器；
②字段添加`@Xss(allowHtml=true)`，过滤器正常开启，注解白名单放行合法HTML。

### 2.GET请求参数过滤器不生效
若依原生：**XssFilter只拦截POST/PUT等非GET请求**，GET参数靠Vue前端渲染转义+@Xss注解校验；如果需要GET也过滤，修改XssFilter源码去掉GET放行逻辑。

### 3.不能全项目关闭XSS（enabled:false）
关闭全局过滤器=所有接口无自动转义，用户输入恶意脚本直接入库，造成全系统存储XSS漏洞，仅测试环境临时关闭。

### 4.@Xss和过滤器同时存在时执行顺序
**先过滤器转义→再@Xss校验**；过滤器把`<script>`转成`&lt;script&gt;`，@Xss检测不到标签，不会报错；
> 若要**严格禁止字段带任何标签**：字段所在接口加入`excludes`排除过滤器，只靠@Xss拦截。

### 5.文件上传文件名带特殊标签报错
上传文件名含`<script>.jpg`触发XSS拦截，上传接口加入excludes放行，后端单独对文件名做XSS校验。

### 6.字典、代码生成场景
字典名称、菜单名称建议加@Xss，防止字典数据XSS注入，页面下拉渲染触发脚本。

## 六、项目标准落地规范（企业最佳实践）
1. **普通纯文本字段**：不写@Xss，依赖**全局XssFilter自动转义**；
2. **账号/编码/关键业务字段**：实体字段加`@Xss`，双重防护，非法输入直接拦截；
3. **富文本字段**：接口放excludes排除过滤，字段标注`@Xss(allowHtml=true)`；
4. **导入导出Excel**：导入实体关键字段加@Xss，防止恶意文件注入XSS。

## 七、补充：手动工具类过滤（自定义场景）
```java
//XssUtils工具类手动转义字符串
String safeStr = XssUtil.filterHtml("<script>alert(1)</script>");
```
用于定时任务、第三方接口回调、非Controller入参的字符串XSS过滤。

需要我给你一份**富文本完整示例代码（Controller+实体+yml配置全套）**吗？