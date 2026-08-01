# HTML / CSS / JavaScript 学习路线

> 对应 Java 全栈路线思维导图「前端与工程」第一叶。全栈工程师的前端地基。

## 一、学习阶段

### 入门：能写静态页
- HTML：语义化标签、表单、a/img/table
- CSS：选择器、盒模型、Flex 布局、定位
- JS：变量/函数/DOM 操作/事件

### 进阶：能写交互
- CSS：Grid、响应式、动画、BEM 命名
- JS：ES6+（let/const、解构、箭头函数、Promise、async/await）
- DOM 进阶：事件委托、防抖节流
- 浏览器：渲染流程、同源策略、跨域

### 高级：懂原理
- JS 原型链、闭包、this、作用域
- 事件循环（宏任务/微任务）
- 性能：重排重绘、懒加载、CDN

## 二、关键要点与常见坑
- 盒模型 box-sizing: border-box 省心
- var 提升 vs let 暂时性死区
- 跨域：CORS / 代理（开发用 devServer proxy）
- 防抖节流别混：输入用防抖，滚动用节流

## 三、实战
- 入门：仿一个静态官网首页
- 进阶：用原生 JS 写待办清单（增删改+本地存储）

## 四、衔接
- 进阶接《Vue 与 React 与 TS 学习路线》
- 联动《Spring 与 SpringBoot》（前后端联调、接口）

## 五、资源
- MDN Web Docs；freeCodeCamp

## 六、心法
1. 全栈不要求像素级切图，但要能独立写页面调接口。
2. 把 HTML 当结构、CSS 当样式、JS 当行为，各司其职。
3. 事件循环和闭包是 JS 两道坎，过完就通透。
