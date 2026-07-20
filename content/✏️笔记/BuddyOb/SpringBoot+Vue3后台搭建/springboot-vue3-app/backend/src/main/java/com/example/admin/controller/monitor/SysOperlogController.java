package com.example.admin.controller.monitor;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.annotation.Log;
import com.example.admin.common.AjaxResult;
import com.example.admin.domain.SysOperLog;
import com.example.admin.service.OperLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 操作日志（监控模块）
 */
@RestController
@RequestMapping("/monitor/operlog")
@RequiredArgsConstructor
public class SysOperlogController {

    private final OperLogService operLogService;

    @PreAuthorize("@ps.hasPermi('monitor:operlog:list')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(defaultValue = "1") int pageNum,
                           @RequestParam(defaultValue = "10") int pageSize,
                           String title, String operName, Integer status) {
        IPage<SysOperLog> page = operLogService.list(new Page<>(pageNum, pageSize), title, operName, status);
        return AjaxResult.success(page.getRecords(), page.getTotal());
    }

    @PreAuthorize("@ps.hasPermi('monitor:operlog:remove')")
    @Log(title = "操作日志", businessType = 3)
    @DeleteMapping
    public AjaxResult remove(@RequestParam List<Long> ids) {
        operLogService.removeByIds(ids);
        return AjaxResult.success();
    }

    @PreAuthorize("@ps.hasPermi('monitor:operlog:remove')")
    @Log(title = "操作日志", businessType = 3)
    @DeleteMapping("/clean")
    public AjaxResult clean() {
        operLogService.clean();
        return AjaxResult.success();
    }
}
