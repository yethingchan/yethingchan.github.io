package com.example.admin.controller.monitor;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.annotation.Log;
import com.example.admin.common.AjaxResult;
import com.example.admin.domain.SysNotice;
import com.example.admin.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告（业务模块演示）
 */
@RestController
@RequestMapping("/business/notice")
@RequiredArgsConstructor
public class SysNoticeController {

    private final NoticeService noticeService;

    @PreAuthorize("@ps.hasPermi('business:notice:list')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(defaultValue = "1") int pageNum,
                           @RequestParam(defaultValue = "10") int pageSize,
                           String title, String type) {
        IPage<SysNotice> page = noticeService.list(new Page<>(pageNum, pageSize), title, type);
        return AjaxResult.success(page.getRecords(), page.getTotal());
    }

    @PreAuthorize("@ps.hasPermi('business:notice:query')")
    @GetMapping("/{id}")
    public AjaxResult get(@PathVariable Long id) {
        return AjaxResult.success(noticeService.get(id));
    }

    @PreAuthorize("@ps.hasPermi('business:notice:add')")
    @Log(title = "公告管理", businessType = 1)
    @PostMapping
    public AjaxResult add(@RequestBody SysNotice notice) {
        noticeService.add(notice);
        return AjaxResult.success();
    }

    @PreAuthorize("@ps.hasPermi('business:notice:edit')")
    @Log(title = "公告管理", businessType = 2)
    @PutMapping
    public AjaxResult edit(@RequestBody SysNotice notice) {
        noticeService.update(notice);
        return AjaxResult.success();
    }

    @PreAuthorize("@ps.hasPermi('business:notice:remove')")
    @Log(title = "公告管理", businessType = 3)
    @DeleteMapping
    public AjaxResult remove(@RequestParam List<Long> ids) {
        noticeService.delete(ids);
        return AjaxResult.success();
    }
}
