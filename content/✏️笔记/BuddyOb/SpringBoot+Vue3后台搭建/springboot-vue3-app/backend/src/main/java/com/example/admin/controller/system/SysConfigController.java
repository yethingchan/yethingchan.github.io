package com.example.admin.controller.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.annotation.Log;
import com.example.admin.common.AjaxResult;
import com.example.admin.domain.SysConfig;
import com.example.admin.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final ConfigService configService;

    @PreAuthorize("@ps.hasPermi('system:config:list')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(defaultValue = "1") int pageNum,
                           @RequestParam(defaultValue = "10") int pageSize,
                           String configName, String configKey, String configType) {
        IPage<SysConfig> page = configService.list(new Page<>(pageNum, pageSize), configName, configKey, configType);
        return AjaxResult.success(page.getRecords(), page.getTotal());
    }

    @PreAuthorize("@ps.hasPermi('system:config:list')")
    @GetMapping("/{id}")
    public AjaxResult get(@PathVariable Long id) {
        return AjaxResult.success(configService.getById(id));
    }

    @PreAuthorize("@ps.hasPermi('system:config:add')")
    @Log(title = "参数管理", businessType = 1)
    @PostMapping
    public AjaxResult add(@RequestBody SysConfig config) {
        configService.add(config);
        return AjaxResult.success();
    }

    @PreAuthorize("@ps.hasPermi('system:config:edit')")
    @Log(title = "参数管理", businessType = 2)
    @PutMapping
    public AjaxResult edit(@RequestBody SysConfig config) {
        configService.update(config);
        return AjaxResult.success();
    }

    @PreAuthorize("@ps.hasPermi('system:config:remove')")
    @Log(title = "参数管理", businessType = 3)
    @DeleteMapping
    public AjaxResult remove(@RequestParam java.util.List<Long> ids) {
        configService.delete(ids);
        return AjaxResult.success();
    }
}
