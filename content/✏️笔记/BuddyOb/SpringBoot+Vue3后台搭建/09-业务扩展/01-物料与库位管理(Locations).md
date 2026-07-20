---
title: 物料与库位管理（Locations）
---

# 09-1 物料与库位管理（含 Locations 实体）

> 上接：[[SpringBoot+Vue3后台搭建/09-业务扩展/00-索引]]
> 本文件直接对接你 `test-module` 里的 `Locations` 实体——你最初启动报错 `Could not resolve type alias 'Locations'`，根因就是 MyBatis 的 `typeAliasesPackage` 没覆盖 `cn.yething.**.domain`。这里给出**正确且可运行**的写法。

## 1.1 数据库表（物料 + 库位 + 库存三张核心表）

```sql
-- 物料主数据
CREATE TABLE wms_material (
  mat_id      BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '物料ID',
  mat_code    VARCHAR(64)  NOT NULL COMMENT '物料编码(唯一)',
  mat_name    VARCHAR(128) NOT NULL COMMENT '物料名称',
  spec        VARCHAR(128)  DEFAULT '' COMMENT '规格型号',
  unit        VARCHAR(16)   DEFAULT 'PCS' COMMENT '单位',
  category    VARCHAR(64)   DEFAULT '' COMMENT '分类',
  safe_qty    INT           DEFAULT 0 COMMENT '安全库存',
  del_flag    CHAR(1)       DEFAULT '0' COMMENT '0正常 2删除',
  create_time DATETIME      DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_mat_code (mat_code)
) COMMENT='物料主数据';

-- 库位（就是你 test-module 的 Locations！）
CREATE TABLE wms_location (
  loc_id      BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '库位ID',
  loc_code    VARCHAR(64)  NOT NULL COMMENT '库位编码(A-01-03)',
  loc_name    VARCHAR(128) NOT NULL COMMENT '库位名称',
  warehouse   VARCHAR(64)  NOT NULL COMMENT '所属仓库',
  zone        VARCHAR(64)   DEFAULT '' COMMENT '库区',
  capacity    INT           DEFAULT 0 COMMENT '容量(件)',
  status      CHAR(1)       DEFAULT '0' COMMENT '0启用 1停用',
  del_flag    CHAR(1)       DEFAULT '0',
  create_time DATETIME      DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_loc_code (loc_code)
) COMMENT='库位 Locations';

-- 库存（物料×库位的实时存量）
CREATE TABLE wms_inventory (
  inv_id      BIGINT PRIMARY KEY AUTO_INCREMENT,
  mat_id      BIGINT NOT NULL COMMENT '物料ID',
  loc_id      BIGINT NOT NULL COMMENT '库位ID',
  qty         INT    DEFAULT 0 COMMENT '可用数量',
  lock_qty    INT    DEFAULT 0 COMMENT '锁定数量(已占用未出)',
  del_flag    CHAR(1) DEFAULT '0',
  UNIQUE KEY uk_mat_loc (mat_id, loc_id)
) COMMENT='库存';
```

## 1.2 实体（domain）—— 注意 typeAlias 避坑

你的问题就出在这：实体放在 `cn.yething.test.domain.Locations`，而 `application.yml` 的 `typeAliasesPackage` 只写了 `com.ruoyi.**.domain`。MyBatis 扫描不到，于是 XML 里 `resultType="Locations"` 报 `Could not resolve type alias 'Locations'`。

**规范写法**（放业务模块包内，并为避免再踩坑，XML 里用全限定名最稳）：

```java
package com.example.admin.modules.wms.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("wms_location")
public class Locations {
    @TableId(type = IdType.AUTO)
    private Long locId;
    private String locCode;
    private String locName;
    private String warehouse;
    private String zone;
    private Integer capacity;
    private String status;        // 0启用 1停用
    @TableLogic            // 逻辑删除，MP 自动补 WHERE del_flag=0 和 UPDATE
    private String delFlag;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

> ⚠️ **避坑（你已踩过的）**：若用 XML 写 SQL，`resultType`/`parameterType` 要么写全限定名 `com.example.admin.modules.wms.domain.Locations`，要么保证 `typeAliasesPackage` 含该包。教程推荐**能不写 XML 就不写**——用 MP 的 `BaseMapper`，连 `resultType` 都不用配（见 1.4）。

## 1.3 Mapper（MP 零 XML）

```java
package com.example.admin.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.admin.modules.wms.domain.Locations;

public interface LocationsMapper extends BaseMapper<Locations> {
    // 啥都不用写，MP 已提供 selectById / insert / update / selectList...
}
```

如果**必须**写 XML（比如复杂联表），正确写法（注意没有 `parameterType`，且 `collection="array"` 即可）：

```xml
<!-- resources/mapper/wms/LocationsMapper.xml -->
<mapper namespace="com.example.admin.modules.wms.mapper.LocationsMapper">
    <delete id="deleteLocationsByIds">
        UPDATE wms_location SET del_flag = '2'
        WHERE loc_id IN
        <foreach collection="array" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
    </delete>
</mapper>
```

> 这和当初报错的 `test-module/LocationsMapper.xml` 是同一处：`collection="array"` 接收 `Long[]`，**不需要** `parameterType`；之前误加 `parameterType="String"`/`"Long[]"` 反而解析失败。删掉 `parameterType` 才对——两者是独立属性。

## 1.4 Service（含库存联动雏形）

```java
package com.example.admin.modules.wms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.admin.modules.wms.domain.Locations;

public interface ILocationsService extends IService<Locations> {
    void deleteByIds(Long[] ids);
}
```

```java
@Service
public class LocationsServiceImpl extends ServiceImpl<LocationsMapper, Locations>
        implements ILocationsService {
    @Override
    public void deleteByIds(Long[] ids) {
        // 逻辑删：MP 的 @TableLogic 让 removeBatchByIds 自动 UPDATE del_flag
        this.removeBatchByIds(Arrays.asList(ids));
    }
}
```

## 1.5 Controller（完整 CRUD，带权限）

```java
@RestController
@RequestMapping("/wms/location")
public class LocationsController {
    @Resource private ILocationsService locService;

    @PreAuthorize("hasPermi('wms:location:list')")
    @GetMapping("/list")
    public R<Locations> list(Locations q, PageQuery pq) {
        Page<Locations> page = locService.page(
            new Page<>(pq.getPageNum(), pq.getPageSize()),
            Wrappers.lambdaQuery(q).orderByDesc(Locations::getCreateTime));
        return R.ok(page);
    }

    @PreAuthorize("hasPermi('wms:location:add')")
    @PostMapping
    public AjaxResult add(@Valid @RequestBody Locations loc) {
        if (locService.count(Wrappers.lambdaQuery(Locations.class)
                .eq(Locations::getLocCode, loc.getLocCode())) > 0)
            throw new BusinessException("库位编码已存在");
        locService.save(loc);
        return AjaxResult.success();
    }

    @PreAuthorize("hasPermi('wms:location:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody Locations loc) {
        locService.updateById(loc);
        return AjaxResult.success();
    }

    @PreAuthorize("hasPermi('wms:location:remove')")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        locService.deleteByIds(ids);
        return AjaxResult.success();
    }
}
```

> `PageQuery`（`getPageNum()`/`getPageSize()`）和 `R.ok(page)` 在 [[../03-后端基础框架/01-统一返回与全局异常]] 已定义；`@PreAuthorize("hasPermi('wms:location:list')")` 的权限串要和前端 `v-hasPermi` 完全一致。

## 1.6 前端：api + 页面

`src/api/wms/location.js`
```js
import request from '@/utils/request'
export function listLocation(query){ return request({ url:'/wms/location/list', method:'get', params:query }) }
export function addLocation(data){ return request({ url:'/wms/location', method:'post', data }) }
export function updateLocation(data){ return request({ url:'/wms/location', method:'put', data }) }
export function delLocation(ids){ return request({ url:'/wms/location/'+ids, method:'delete' }) }
```

`src/views/wms/location/index.vue`（复用 [[../08-前端通用封装/01-表格与表单通用组件]] 的套路，只列关键）
```vue
<template>
  <div>
    <el-form :inline="true" :model="query">
      <el-form-item label="库位编码"><el-input v-model="query.locCode" /></el-form-item>
      <el-form-item label="仓库">
        <el-select v-model="query.warehouse" clearable>
          <el-option label="华东仓" value="华东仓"/>
          <el-option label="华南仓" value="华南仓"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="getList">查询</el-button>
        <el-button v-hasPermi="['wms:location:add']" type="success" @click="openAdd">新增库位</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="tableData.rows" v-loading="loading">
      <el-table-column prop="locCode" label="库位编码"/>
      <el-table-column prop="locName" label="名称"/>
      <el-table-column prop="warehouse" label="仓库"/>
      <el-table-column prop="status" label="状态">
        <template #default="{row}">
          <el-tag :type="row.status==='0'?'success':'info'">
            {{ row.status==='0'?'启用':'停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{row}">
          <el-button v-hasPermi="['wms:location:edit']" link @click="openEdit(row)">编辑</el-button>
          <el-button v-hasPermi="['wms:location:remove']" link type="danger" @click="handleDel(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
      :total="tableData.total" @current-change="getList" layout="total,prev,pager,next"/>
  </div>
</template>
<script setup>
import { ref, reactive } from 'vue'
import { listLocation, addLocation, updateLocation, delLocation } from '@/api/wms/location'
import { ElMessage, ElMessageBox } from 'element-plus'

const query = reactive({ pageNum:1, pageSize:10, locCode:'', warehouse:'' })
const tableData = ref({ rows:[], total:0 })
const loading = ref(false)
async function getList(){ loading.value=true; tableData.value = await listLocation(query); loading.value=false }
getList()

function handleDel(row){
  ElMessageBox.confirm('确认删除该库位？').then(async ()=>{
    await delLocation(row.locId); ElMessage.success('已删除'); getList()
  })
}
// openAdd / openEdit 复用 08 章弹窗套路，提交时 addLocation/updateLocation
</script>
```

## 验证清单

- [ ] `wms_location` 表建好，`Locations` 实体被 MP 正常扫描（再无 `type alias` 报错）。
- [ ] `GET /wms/location/list` 带 token 返回 `{rows,total}`，不带 401。
- [ ] 新增重复 `locCode` → 前端/后端都拦截（业务异常）。
- [ ] 删除库位，DB 里 `del_flag=2`（逻辑删，非物理删）。
- [ ] 无 `wms:location:add` 权限时，前端"新增库位"按钮不渲染。

> 下一步：[[SpringBoot+Vue3后台搭建/09-业务扩展/02-库存单据与预警]] 把"入库/出库/盘点"和"库存预警"做出来。
