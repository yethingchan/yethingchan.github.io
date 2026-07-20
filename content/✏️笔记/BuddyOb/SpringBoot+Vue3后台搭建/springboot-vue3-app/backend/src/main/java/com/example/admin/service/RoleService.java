package com.example.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.admin.common.BusinessException;
import com.example.admin.common.ErrorCode;
import com.example.admin.common.utils.StringUtils;
import com.example.admin.domain.SysRole;
import com.example.admin.domain.SysRoleMenu;
import com.example.admin.domain.SysUserRole;
import com.example.admin.mapper.SysRoleMapper;
import com.example.admin.mapper.SysRoleMenuMapper;
import com.example.admin.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色管理 + 角色菜单授权
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    public IPage<SysRole> listRoles(IPage<SysRole> page, String roleName, String status) {
        LambdaQueryWrapper<SysRole> q = new LambdaQueryWrapper<>();
        q.like(StringUtils.isNotBlank(roleName), SysRole::getRoleName, roleName);
        q.eq(StringUtils.isNotBlank(status), SysRole::getStatus, status);
        q.orderByDesc(SysRole::getRoleId);
        return roleMapper.selectPage(page, q);
    }

    public Map<String, Object> getRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        List<Long> menuIds = roleMenuMapper.selectList(
                        new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id))
                .stream().map(SysRoleMenu::getMenuId).toList();
        Map<String, Object> m = new HashMap<>();
        m.put("role", role);
        m.put("menuIds", menuIds);
        return m;
    }

    public void addRole(SysRole role) {
        roleMapper.insert(role);
    }

    public void updateRole(SysRole role) {
        roleMapper.updateById(role);
    }

    public void deleteRole(Long id) {
        long assigned = userRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
        if (assigned > 0) {
            throw new BusinessException(ErrorCode.ROLE_ASSIGNED, "角色已分配，不能删除");
        }
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
    }

    public void updateRoleMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        if (menuIds != null) {
            for (Long menuId : menuIds) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                roleMenuMapper.insert(rm);
            }
        }
    }
}
