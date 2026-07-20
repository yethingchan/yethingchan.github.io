package com.example.admin.common.dto;

import com.example.admin.domain.SysUser;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户保存/编辑入参：用户实体 + 角色 id 列表
 */
@Data
public class UserDTO implements Serializable {
    private SysUser user;
    private List<Long> roleIds;
}
