# 09 · 特殊字符处理与 CDATA

> 上接：[[00-总览与目录]] ｜ 相关：[[05-动态SQL]]

XML 里 `<` `>` `&` 是保留字符，直接写会解析报错。两种处理：

## 方式一：实体转义

```xml
<if test="age &gt;= 18"> ... </if>
```

常用转义：

| 字符 | 转义 |
|------|------|
| `<` | `&lt;` |
| `>` | `&gt;` |
| `&` | `&amp;` |
| `"` | `&quot;` |
| `'` | `&apos;` |

## 方式二：CDATA 包裹（推荐写复杂条件/运算符时）

```xml
<if test="createTime != null">
    and create_time <![CDATA[ >= ]]> #{createTime}
</if>
```

`<![CDATA[ ... ]]>` 内的内容会被当纯文本，不被 XML 解析器解释。

## 完整示例

```xml
<select id="selectByRange" resultType="SysUser">
    select * from sys_user
    <where>
        <if test="minAge != null">
            and age <![CDATA[ >= ]]> #{minAge}
        </if>
        <if test="maxAge != null">
            and age <![CDATA[ &lt;= ]]> #{maxAge}
        </if>
    </where>
</select>
```

> 注意：CDATA 里写的是真实字符（`>=`、`<`），不要再套转义；混用容易出错。简单符号用转义，复杂区间用 CDATA。

## 速记

- 简单符号（`>` `<` `&`）→ 用实体转义 `&gt;` `&lt;` `&amp;`
- 一段含多个特殊符号 → 用 `<![CDATA[ ... ]]>`
- `#{}` 占位符本身不受影响，照常写
