# 若依 Vue2 → Vue3 最全场景转换手册（源码对比 + 直接复制）
我把**项目里 100% 会遇到的所有场景**全部整理出来，**左边 Vue2，右边 Vue3**，你对照改就行！

## 一、最核心区别（先记这 3 句）
1. **Vue2**：`export default { dicts, data, methods, created }`
2. **Vue3**：`<script setup>` 组合式 API
3. **字典/弹窗/表格/路由/请求**全部有新写法

---

# 二、场景 1：页面字典使用（最常用！）
## Vue2 写法
```vue
<script>
export default {
  dicts: ['sys_user_sex', 'sys_normal_disable'], // 自动加载字典
  data() {
    return {
      form: {},
      queryParams: {}
    }
  }
}
</script>

<template>
<el-select v-model="form.status">
  <el-option
    v-for="d in dict.sys_normal_disable"
    :key="d.dictValue"
    :label="d.dictLabel"
    :value="d.dictValue"
  />
</el-select>
</template>
```

## Vue3 写法（setup）
```vue
<script setup>
import { useDict } from "@/utils/dict";

// 自动加载字典，直接解构使用
const { sys_user_sex, sys_normal_disable } = useDict(
  "sys_user_sex",
  "sys_normal_disable"
);

const form = reactive({});
const queryParams = reactive({});
</script>

<template>
<el-select v-model="form.status">
  <el-option
    v-for="d in sys_normal_disable"
    :key="d.dictValue"
    :label="d.dictLabel"
    :value="d.dictValue"
  />
</el-select>
</template>
```

---

# 三、场景 2：data 定义变量
## Vue2
```js
export default {
  data() {
    return {
      name: "张三",
      age: 20,
      list: []
    }
  }
}
```

## Vue3
```js
import { reactive, ref } from "vue";

const name = ref("张三");
const age = ref(20);
const list = ref([]);

// 对象推荐 reactive
const form = reactive({
  name: "",
  status: ""
});
```

---

# 四、场景 3：方法 methods
## Vue2
```js
export default {
  methods: {
    submit() {
      console.log("提交")
    },
    getList() { }
  }
}
```

## Vue3
```js
function submit() {
  console.log("提交");
}

function getList() { }
```

---

# 五、场景 4：生命周期
## Vue2
```js
created() {
  this.getList();
},
mounted() { }
```

## Vue3
```js
import { onMounted, onBeforeMount } from "vue";

onMounted(() => {
  getList();
});
```

---

# 六、场景 5：axios 请求
## Vue2
```js
export default {
  methods: {
    getList() {
      listUser(this.queryParams).then(res => {
        this.list = res.rows;
      });
    }
  }
}
```

## Vue3
```js
import { listUser } from "@/api/system/user";

async function getList() {
  const res = await listUser(queryParams);
  list.value = res.rows;
}
```

---

# 七、场景 6：表格 + 分页
## Vue2
```vue
<el-pagination
  @size-change="handlePageChange"
  @current-change="handleCurrentChange"
  :current-page="pageNum"
  :page-size="pageSize"
  layout="total,prev, pager, next, jumper"
  :total="total"
>
</el-pagination>
```

## Vue3
```vue
<el-pagination
  v-model:current-page="queryParams.pageNum"
  v-model:page-size="queryParams.pageSize"
  :total="total"
  layout="total, prev, pager, next, jumper"
  @size-change="getList"
  @current-change="getList"
/>
```

---

# 八、场景 7：弹窗（新增/修改）
## Vue2
```js
export default {
  data() {
    return {
      title: "",
      open: false
    }
  },
  methods: {
    openDialog() {
      this.open = true;
    }
  }
}
```

## Vue3
```js
const title = ref("");
const open = ref(false);

function openDialog() {
  open.value = true;
}
```

---

# 九、场景 8：路由跳转
## Vue2
```js
this.$router.push("/system/user");
```

## Vue3
```js
import { useRouter } from "vue-router";
const router = useRouter();

router.push("/system/user");
```

---

# 十、场景 9：获取当前路由参数
## Vue2
```js
this.$route.query.id
```

## Vue3
```js
import { useRoute } from "vue-router";
const route = useRoute();
const id = route.query.id;
```

---

# 十一、场景 10：vuex 使用
## Vue2
```js
this.$store.state.user.name
```

## Vue3
```js
import { useStore } from "vuex";
const store = useStore();

store.state.user.name
```

---

# 十二、场景 11：消息提示
## Vue2
```js
this.$msg.success("成功");
this.$modal.confirm("确认删除？");
```

## Vue3
```js
import { ElMessage, ElMessageBox } from "element-plus";

ElMessage.success("成功");

ElMessageBox.confirm("确认删除？");
```

---

# 十三、场景 12：表单提交与重置
## Vue2
```js
reset() {
  this.$refs.form.resetFields();
}
```

## Vue3
```js
const formRef = ref(null);

function reset() {
  formRef.value.resetFields();
}
```

```vue
<el-form ref="formRef" />
```

---

# 十四、场景 13：watch 监听
## Vue2
```js
watch: {
  name(val) { }
}
```

## Vue3
```js
import { watch } from "vue";

watch(name, (val) => { });
```

---

# 十五、场景 14：计算属性 computed
## Vue2
```js
computed: {
  fullName() { return this.first + this.last }
}
```

## Vue3
```js
import { computed } from "vue";
const fullName = computed(() => first.value + last.value);
```

---

# 十六、场景 15：权限指令
## Vue2
```vue
<el-button v-hasPermi="['system:user:add']">
```

## Vue3（完全一样）
```vue
<el-button v-hasPermi="['system:user:add']">
```

---

# 十七、场景 16：字典标签
## Vue2
```vue
<dict-tag :options="dict.sys_normal_disable" :value="scope.row.status"/>
```

## Vue3（完全一样）
```vue
<dict-tag :options="sys_normal_disable" :value="scope.row.status"/>
```

---

# 十八、场景 17：导出 Excel
## Vue2
```js
this.download('export', params, "user.xlsx");
```

## Vue3
```js
import { download } from "@/utils/request";

download('export', params, "user.xlsx");
```

---

# 十九、场景 18：上传组件
## Vue2
```vue
<el-upload :before-upload="beforeUpload" />
```

## Vue3
```vue
<el-upload :before-upload="beforeUpload" />
```
方法一样。

---

# 二十、场景 19：props 父传子
## Vue2
```js
props: { visible: Boolean }
```

## Vue3
```js
const props = defineProps({
  visible: Boolean
});
```

---

# 二十一、场景 20：子传父 emit
## Vue2
```js
this.$emit("close");
```

## Vue3
```js
const emit = defineEmits(["close"]);
emit("close");
```

---

# 🎯 我给你一个 **万能 Vue2 → Vue3 转换模板**
## Vue2
```vue
<script>
export default {
  dicts: ['sys_user_sex'],
  data() {
    return { form: {} }
  },
  created() { this.getList() },
  methods: { getList() {} }
}
</script>
```

## Vue3（直接替换）
```vue
<script setup>
import { useDict } from "@/utils/dict";
import { reactive, onMounted } from "vue";
import { listUser } from "@/api/system/user";

const { sys_user_sex } = useDict("sys_user_sex");
const form = reactive({});

onMounted(() => {
  getList();
});

async function getList() {
  const res = await listUser(form);
}
</script>
```

---

# 🚀 你要的话，我可以再给你：
## **1. 一张完整 A4 转换对照表（可打印）**
## **2. 若依 Vue3 标准页面模板（CRUD 全套）**

你要哪个？我直接给你！