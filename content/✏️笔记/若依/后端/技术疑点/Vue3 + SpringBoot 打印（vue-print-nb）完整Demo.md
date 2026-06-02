# Vue3 + ElementPlus + vue-print-nb 打印全套实战代码（后端不用改代码，纯前端打印单据/报表，若依项目直接复制）
## 一、第一步安装依赖
```shell
npm install vue-print-nb --save
```
## 二、main.js全局注册
```js
import { createApp } from 'vue'
import App from './App.vue'
import print from 'vue-print-nb'
const app = createApp(App)
app.use(print)
app.mount('#app')
```

## 三、三种常用打印场景源码（复制即用）
### 场景1：按钮打印当前页面指定区域【最常用：订单单据、出库单】
```vue
<template>
  <!-- 打印按钮 -->
  <el-button v-print="'printDom'" type="primary">打印单据</el-button>

  <!-- 需要打印的区域，id和v-print绑定一致 -->
  <div id="printDom" style="padding:20px;width:800px;">
    <h2 align="center">产品出库单</h2>
    <table border="1" cellpadding="6" cellspacing="0" width="100%">
      <tr>
        <td>单据编号</td>
        <td>{{ order.orderNo }}</td>
        <td>出库日期</td>
        <td>{{ order.outDate }}</td>
      </tr>
      <tr>
        <td>客户名称</td>
        <td colspan="3">{{ order.customerName }}</td>
      </tr>
      <tr>
        <td>产品名称</td>
        <td>规格</td>
        <td>数量</td>
        <td>单价</td>
      </tr>
      <tr v-for="item in tableList" :key="item.id">
        <td>{{ item.goodsName }}</td>
        <td>{{ item.spec }}</td>
        <td>{{ item.num }}</td>
        <td>{{ item.price }}</td>
      </tr>
      <tr>
        <td colspan="3" align="right">合计金额：</td>
        <td>{{ totalMoney }}</td>
      </tr>
    </table>
    <p style="margin-top:30px;">制单人：{{ order.createUser }}</p>
  </div>
</template>

<script setup>
import {ref,reactive} from 'vue'
const order = reactive({
  orderNo:'CK20250603001',
  outDate:'2025-06-03',
  customerName:'XX商贸有限公司',
  createUser:'管理员'
})
const tableList = ref([
  {id:1,goodsName:'轴承',spec:'50*30',num:100,price:25.5},
  {id:2,goodsName:'螺丝',spec:'M8',num:500,price:0.8}
])
const totalMoney = ref(2950)
</script>

<style scoped>
/* 非打印样式不影响 */
</style>
```

### 场景2：自定义打印配置（页边距、标题、页眉、隐藏不需要打印内容）
```vue
<template>
<el-button @click="printCustom">自定义配置打印</el-button>
<div id="printArea">
  <h3>客户对账单</h3>
  <p>对账周期：2025-01~2025-05</p>
</div>
</template>
<script setup>
import {getCurrentInstance} from 'vue'
const {proxy} = getCurrentInstance()
const printCustom = ()=>{
  // 自定义打印参数
  proxy.$print({
    id:'printArea',
    title:'客户对账单', // 页眉标题
    preview:false, // 是否预览再打印
    popTitle:'', // 浏览器标题
    margin:'20px 20px 20px 20px' // 上下左右页边距
  })
}
</script>
```

### 场景3：打印隐藏页面侧边栏、顶部导航（若依后台必备，不然菜单一起被打印）
> 原理：CSS打印媒体查询，`@media print` 只在打印时生效，隐藏不需要DOM
```vue
<template>
  <el-button v-print="'printContent'">打印报表</el-button>
  <!-- 页面原有侧边栏、头部导航（页面自带dom，不用自己写） -->
  <div class="layout-sidebar">侧边导航</div>
  <div class="layout-header">顶部菜单</div>

  <div id="printContent">
    <h2>数据报表</h2>
    <p>测试打印内容</p>
  </div>
</template>

<style>
/* 注意：不要加scoped，否则css不生效 */
@media print {
  /* 打印时隐藏侧边栏、头部、按钮、分页等不需要内容 */
  .layout-sidebar,.layout-header,.el-button,.el-pagination{
    display: none !important;
  }
  /* 打印区域铺满纸张 */
  #printContent{
    width:100%;
  }
}
</style>
```

## 四、四大常见难点+解决方案
### 难点1：打印时页面侧边栏、按钮、分页一起被打印
**解决**：使用`@media print {}` CSS样式，指定类名打印隐藏

### 难点2：打印分页错乱、内容被截断
**解决**：需要分页位置添加样式
```css
.page-cut{
  page-break-after: always;
}
```
```html
<div class="page-cut"></div> <!-- 在此处强制分页 -->
```

### 难点3：表格边框打印缺失、样式变形
**解决**：table统一设置`border="1" cellpadding="5" cellspacing="0"`，避免CSS边框打印失效

### 难点4：需要固定页眉页脚（页码、制表日期）
**两种方案**
1. 简易：直接写在打印DOM首尾（前端固定文字）
2. 复杂：后端Freemarker+Itext生成PDF文件，前端打开PDF打印（合同、票据高精度场景）

## 五、两种打印方案优劣对比
|方案|实现方式|优点|缺点|适用场景|
|----|----|----|----|----|
|前端vue-print-nb|DOM页面打印|开发快、零后端代码、改数据即改打印|复杂分页/高精度格式容易错位|出库单、订单、普通报表（90%业务）|
|后端生成PDF下载打印|Freemarker+Itext/EasyPdf|格式100%统一、跨设备无错乱|需要后端编码、修改模板要改代码|合同、发票、正式公文|

## 六、选型结论
1. **日常单据打印（订单/出入库）：首选vue-print-nb前端打印**（开发最快，项目首选）
2. **正式合同、盖章票据：后端生成PDF再打印**（格式严格统一）

## 补充：若依项目通用封装
若依全局已经集成`vue-print-nb`，多数版本无需npm安装，直接`v-print="id"`即可使用。

需要我补充**后端Freemarker生成PDF打印简易demo**吗？