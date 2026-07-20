package com.example.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.admin.common.utils.StringUtils;
import com.example.admin.domain.SysMenu;
import com.example.admin.domain.vo.MetaVO;
import com.example.admin.domain.vo.RouterVO;
import com.example.admin.mapper.SysMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单管理 + 动态路由树构建
 */
@Service
@RequiredArgsConstructor
public class MenuService {

    private final SysMenuMapper menuMapper;

    public List<SysMenu> listMenus(String menuName, String status) {
        LambdaQueryWrapper<SysMenu> q = new LambdaQueryWrapper<>();
        q.like(StringUtils.isNotBlank(menuName), SysMenu::getMenuName, menuName);
        q.eq(StringUtils.isNotBlank(status), SysMenu::getStatus, status);
        q.orderByAsc(SysMenu::getParentId);
        q.orderByAsc(SysMenu::getOrderNum);
        return menuMapper.selectList(q);
    }

    /** 角色赋权时展示全部目录/菜单树 */
    public List<RouterVO> getMenuTreeForAssign() {
        return buildRouters(menuMapper.selectMenuTreeAll());
    }

    public List<Long> getMenuIdsByRole(Long roleId) {
        return menuMapper.selectMenuIdsByRoleId(roleId);
    }

    public void addMenu(SysMenu menu) {
        menuMapper.insert(menu);
    }

    public void updateMenu(SysMenu menu) {
        menuMapper.updateById(menu);
    }

    public void deleteMenu(Long id) {
        menuMapper.deleteById(id);
    }

    /** 由扁平菜单列表构建前端动态路由树 */
    public List<RouterVO> buildRouters(List<SysMenu> menus) {
        Map<Long, SysMenu> map = new HashMap<>();
        for (SysMenu m : menus) {
            map.put(m.getMenuId(), m);
        }
        Map<Long, List<RouterVO>> childrenMap = new HashMap<>();
        List<RouterVO> roots = new ArrayList<>();

        for (SysMenu m : menus) {
            RouterVO r = toRouter(m);
            Long pid = m.getParentId() == null ? 0L : m.getParentId();
            if (pid == 0L || !map.containsKey(pid)) {
                roots.add(r);
            } else {
                childrenMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(r);
            }
        }
        for (RouterVO r : roots) {
            attach(r, childrenMap);
        }
        return roots;
    }

    private void attach(RouterVO r, Map<Long, List<RouterVO>> childrenMap) {
        List<RouterVO> kids = childrenMap.get(r.getMenuId());
        if (kids != null) {
            for (RouterVO k : kids) {
                attach(k, childrenMap);
            }
            r.setChildren(kids);
        }
    }

    private RouterVO toRouter(SysMenu m) {
        RouterVO r = new RouterVO();
        r.setMenuId(m.getMenuId());
        r.setName(m.getPath());
        r.setPath(m.getPath());
        r.setComponent("M".equals(m.getMenuType()) ? "Layout" : m.getComponent());
        MetaVO meta = new MetaVO();
        meta.setTitle(m.getMenuName());
        meta.setIcon(m.getIcon());
        r.setMeta(meta);
        return r;
    }
}
