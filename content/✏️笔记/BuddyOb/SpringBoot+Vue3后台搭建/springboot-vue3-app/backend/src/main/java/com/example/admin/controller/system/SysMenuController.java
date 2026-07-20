package com.example.admin.controller.system;

import com.example.admin.annotation.Log;
import com.example.admin.common.AjaxResult;
import com.example.admin.domain.SysMenu;
import com.example.admin.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final MenuService menuService;

    @PreAuthorize("@ps.hasPermi('system:menu:list')")
    @GetMapping("/list")
    public AjaxResult list(String menuName, String status) {
        return AjaxResult.success(menuService.listMenus(menuName, status));
    }

    @PreAuthorize("@ps.hasPermi('system:menu:query')")
    @GetMapping("/tree")
    public AjaxResult tree() {
        return AjaxResult.success(menuService.getMenuTreeForAssign());
    }

    @PreAuthorize("@ps.hasPermi('system:menu:add')")
    @Log(title = "菜单管理", businessType = 1)
    @PostMapping
    public AjaxResult add(@RequestBody SysMenu menu) {
        menuService.addMenu(menu);
        return AjaxResult.success();
    }

    @PreAuthorize("@ps.hasPermi('system:menu:edit')")
    @Log(title = "菜单管理", businessType = 2)
    @PutMapping
    public AjaxResult edit(@RequestBody SysMenu menu) {
        menuService.updateMenu(menu);
        return AjaxResult.success();
    }

    @PreAuthorize("@ps.hasPermi('system:menu:remove')")
    @Log(title = "菜单管理", businessType = 3)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return AjaxResult.success();
    }
}
