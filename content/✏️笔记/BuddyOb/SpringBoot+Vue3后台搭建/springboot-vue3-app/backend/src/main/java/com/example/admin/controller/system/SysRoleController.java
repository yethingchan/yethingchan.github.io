package com.example.admin.controller.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.annotation.Log;
import com.example.admin.common.AjaxResult;
import com.example.admin.common.dto.RoleDTO;
import com.example.admin.domain.SysRole;
import com.example.admin.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final RoleService roleService;

    @PreAuthorize("@ps.hasPermi('system:role:list')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") int pageSize,
                          String roleName, String status) {
        IPage<SysRole> page = roleService.listRoles(new Page<>(pageNum, pageSize), roleName, status);
        return AjaxResult.success(page.getRecords(), page.getTotal());
    }

    @PreAuthorize("@ps.hasPermi('system:role:list')")
    @GetMapping("/{id}")
    public AjaxResult get(@PathVariable Long id) {
        return AjaxResult.success(roleService.getRole(id));
    }

    @PreAuthorize("@ps.hasPermi('system:role:add')")
    @Log(title = "角色管理", businessType = 1)
    @PostMapping
    public AjaxResult add(@RequestBody SysRole role) {
        roleService.addRole(role);
        return AjaxResult.success();
    }

    @PreAuthorize("@ps.hasPermi('system:role:edit')")
    @Log(title = "角色管理", businessType = 2)
    @PutMapping
    public AjaxResult edit(@RequestBody SysRole role) {
        roleService.updateRole(role);
        return AjaxResult.success();
    }

    @PreAuthorize("@ps.hasPermi('system:role:remove')")
    @Log(title = "角色管理", businessType = 3)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        roleService.deleteRole(id);
        return AjaxResult.success();
    }

    @PreAuthorize("@ps.hasPermi('system:role:edit')")
    @Log(title = "角色菜单授权", businessType = 2)
    @PostMapping("/menu")
    public AjaxResult saveMenus(@RequestBody RoleDTO dto) {
        roleService.updateRoleMenus(dto.getRole().getRoleId(), dto.getMenuIds());
        return AjaxResult.success();
    }
}
