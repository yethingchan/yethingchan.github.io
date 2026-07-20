package com.example.admin.controller.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.annotation.Log;
import com.example.admin.common.AjaxResult;
import com.example.admin.common.dto.UserDTO;
import com.example.admin.domain.SysUser;
import com.example.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final UserService userService;

    @PreAuthorize("@ps.hasPermi('system:user:list')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") int pageSize,
                          String userName, String status) {
        IPage<SysUser> page = userService.listUsers(new Page<>(pageNum, pageSize), userName, status);
        return AjaxResult.success(page.getRecords(), page.getTotal());
    }

    @PreAuthorize("@ps.hasPermi('system:user:list')")
    @GetMapping("/{id}")
    public AjaxResult get(@PathVariable Long id) {
        return AjaxResult.success(userService.getUser(id));
    }

    @PreAuthorize("@ps.hasPermi('system:user:add')")
    @Log(title = "用户管理", businessType = 1)
    @PostMapping
    public AjaxResult add(@RequestBody UserDTO dto) {
        userService.addUser(dto.getUser(), dto.getRoleIds());
        return AjaxResult.success();
    }

    @PreAuthorize("@ps.hasPermi('system:user:edit')")
    @Log(title = "用户管理", businessType = 2)
    @PutMapping
    public AjaxResult edit(@RequestBody UserDTO dto) {
        userService.updateUser(dto.getUser(), dto.getRoleIds());
        return AjaxResult.success();
    }

    @PreAuthorize("@ps.hasPermi('system:user:remove')")
    @Log(title = "用户管理", businessType = 3)
    @DeleteMapping
    public AjaxResult remove(@RequestParam List<Long> ids) {
        userService.deleteUsers(ids);
        return AjaxResult.success();
    }
}
