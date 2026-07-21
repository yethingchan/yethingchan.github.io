# 12 · 表单输入绑定 v-model

后台系统大量表单（新增/编辑/搜索），`v-model` 是核心。

## 基础：文本输入

```vue
<input v-model="name" />
<p>你输入了：{{ name }}</p>
```

`v-model` = `:value` + `@input` 的语法糖，实现双向绑定。

## 多行文本

```vue
<textarea v-model="desc"></textarea>
```

## 复选框（单/多）

```vue
<!-- 单个：布尔 -->
<input type="checkbox" v-model="agree" /> 同意

<!-- 多个：数组 -->
<input type="checkbox" value="a" v-model="hobbies" /> A
<input type="checkbox" value="b" v-model="hobbies" /> B
```

## 单选框

```vue
<input type="radio" value="male" v-model="gender" /> 男
<input type="radio" value="female" v-model="gender" /> 女
```

## 下拉选择

```vue
<select v-model="city">
  <option value="bj">北京</option>
  <option value="sh">上海</option>
</select>
```

## 修饰符

| 修饰符 | 作用 |
|--------|------|
| `.lazy` | 失焦才更新（而非每次输入） |
| `.number` | 自动转数字 |
| `.trim` | 去首尾空格 |

```vue
<input v-model.number="age" />     <!-- age 是数字 -->
<input v-model.trim="username" />  <!-- 自动去空格 -->
```

## 本项目中的例子（Element Plus）

Element Plus 组件同样支持 `v-model`：

```vue
<el-form :model="form" label-width="80px">
  <el-form-item label="用户名">
    <el-input v-model="form.userName" />
  </el-form-item>
  <el-form-item label="状态">
    <el-radio-group v-model="form.status">
      <el-radio value="0">正常</el-radio>
      <el-radio value="1">停用</el-radio>
    </el-radio-group>
  </el-form-item>
  <el-form-item label="角色">
    <el-select v-model="form.roleIds" multiple>
      <el-option label="管理员" value="1" />
    </el-select>
  </el-form-item>
</el-form>
```

> 本项目所有新增/编辑弹窗都遵循"表单对象 + `v-model` 绑定 + `el-form` 校验"模式。

## 小结

- `v-model` 双向绑定，表单开发基石。
- 配合 `.number` / `.trim` / `.lazy` 修饰符。
- Element Plus 的 `el-input` / `el-select` / `el-radio-group` 都支持 `v-model`。

下一篇：[组件基础](./13-组件基础.md)
