package com.example.admin.controller.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.annotation.Log;
import com.example.admin.common.AjaxResult;
import com.example.admin.domain.SysDictData;
import com.example.admin.domain.SysDictType;
import com.example.admin.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典管理：类型 + 数据（下拉框统一读取）
 */
@RestController
@RequestMapping("/system/dict")
@RequiredArgsConstructor
public class SysDictController {

    private final DictService dictService;

    @PreAuthorize("@ps.hasPermi('system:dict:list')")
    @GetMapping("/type/list")
    public AjaxResult typeList(@RequestParam(defaultValue = "1") int pageNum,
                               @RequestParam(defaultValue = "10") int pageSize,
                               String dictName, String dictType) {
        IPage<SysDictType> page = dictService.listTypes(new Page<>(pageNum, pageSize), dictName, dictType);
        return AjaxResult.success(page.getRecords(), page.getTotal());
    }

    @PreAuthorize("@ps.hasPermi('system:dict:list')")
    @GetMapping("/type/optionselect")
    public AjaxResult typeOptions() {
        return AjaxResult.success(dictService.typesAll());
    }

    @PreAuthorize("@ps.hasPermi('system:dict:list')")
    @GetMapping("/type/{id}")
    public AjaxResult getType(@PathVariable Long id) {
        return AjaxResult.success(dictService.getByIdType(id));
    }

    @PreAuthorize("@ps.hasPermi('system:dict:add')")
    @Log(title = "字典类型", businessType = 1)
    @PostMapping("/type")
    public AjaxResult addType(@RequestBody SysDictType type) {
        dictService.addType(type);
        return AjaxResult.success();
    }

    @PreAuthorize("@ps.hasPermi('system:dict:edit')")
    @Log(title = "字典类型", businessType = 2)
    @PutMapping("/type")
    public AjaxResult editType(@RequestBody SysDictType type) {
        dictService.updateType(type);
        return AjaxResult.success();
    }

    @PreAuthorize("@ps.hasPermi('system:dict:remove')")
    @Log(title = "字典类型", businessType = 3)
    @DeleteMapping("/type")
    public AjaxResult removeTypes(@RequestParam List<Long> ids) {
        dictService.deleteTypes(ids);
        return AjaxResult.success();
    }

    @PreAuthorize("@ps.hasPermi('system:dict:list')")
    @GetMapping("/data/list")
    public AjaxResult dataList(@RequestParam(defaultValue = "1") int pageNum,
                               @RequestParam(defaultValue = "10") int pageSize,
                               String dictType) {
        IPage<SysDictData> page = dictService.listData(new Page<>(pageNum, pageSize), dictType);
        return AjaxResult.success(page.getRecords(), page.getTotal());
    }

    @PreAuthorize("@ps.hasPermi('system:dict:list')")
    @GetMapping("/data/type")
    public AjaxResult dataByType(String dictType) {
        return AjaxResult.success(dictService.dataByType(dictType));
    }

    @PreAuthorize("@ps.hasPermi('system:dict:add')")
    @Log(title = "字典数据", businessType = 1)
    @PostMapping("/data")
    public AjaxResult addData(@RequestBody SysDictData data) {
        dictService.addData(data);
        return AjaxResult.success();
    }

    @PreAuthorize("@ps.hasPermi('system:dict:edit')")
    @Log(title = "字典数据", businessType = 2)
    @PutMapping("/data")
    public AjaxResult editData(@RequestBody SysDictData data) {
        dictService.updateData(data);
        return AjaxResult.success();
    }

    @PreAuthorize("@ps.hasPermi('system:dict:remove')")
    @Log(title = "字典数据", businessType = 3)
    @DeleteMapping("/data")
    public AjaxResult removeData(@RequestParam List<Long> ids) {
        dictService.deleteData(ids);
        return AjaxResult.success();
    }
}
