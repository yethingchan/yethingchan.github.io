package com.example.admin.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 菜单表：菜单类型 M=目录 C=菜单 F=按钮（权限）
 * perms 仅在按钮节点有意义，例如 system:user:add
 */
@Data
@TableName("sys_menu")
public class SysMenu implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long menuId;
    private Long parentId;
    private Integer orderNum;
    private String menuName;
    private String path;
    private String component;
    private String query;
    private String menuType; // M / C / F
    private String perms;
    private String icon;
    private String status;
    private Date createTime;
}
