package com.example.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.admin.domain.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /** 用户可见的菜单树（目录+菜单），用于前端动态路由 */
    @Select("<script>" +
            "SELECT DISTINCT m.menu_id, m.parent_id, m.order_num, m.menu_name, m.path, " +
            "m.component, m.query, m.menu_type, m.perms, m.icon, m.status, m.create_time " +
            "FROM sys_menu m " +
            "JOIN sys_role_menu rm ON m.menu_id = rm.menu_id " +
            "JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.menu_type IN ('M','C') AND m.status = '0' " +
            "ORDER BY m.parent_id, m.order_num" +
            "</script>")
    List<SysMenu> selectMenuTreeByUserId(@Param("userId") Long userId);

    /** 用户拥有的权限字符串集合（distinct，非空） */
    @Select("<script>" +
            "SELECT DISTINCT m.perms FROM sys_menu m " +
            "JOIN sys_role_menu rm ON m.menu_id = rm.menu_id " +
            "JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.perms IS NOT NULL AND m.perms &lt;&gt; ''" +
            "</script>")
    List<String> selectPermsByUserId(@Param("userId") Long userId);

    /** 用户角色 key 集合 */
    @Select("<script>" +
            "SELECT DISTINCT r.role_key FROM sys_role r " +
            "JOIN sys_user_role ur ON r.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}" +
            "</script>")
    List<String> selectRoleKeysByUserId(@Param("userId") Long userId);

    /** 角色已分配的菜单 id */
    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    /** 全部目录/菜单（角色赋权时展示整棵树） */
    @Select("SELECT * FROM sys_menu WHERE menu_type IN ('M','C') ORDER BY parent_id, order_num")
    List<SysMenu> selectMenuTreeAll();
}
