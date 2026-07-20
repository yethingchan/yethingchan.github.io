package com.example.admin.common.dto;

import com.example.admin.domain.SysRole;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 角色保存入参：角色实体 + 菜单 id 列表
 */
@Data
public class RoleDTO implements Serializable {
    private SysRole role;
    private List<Long> menuIds;
}
