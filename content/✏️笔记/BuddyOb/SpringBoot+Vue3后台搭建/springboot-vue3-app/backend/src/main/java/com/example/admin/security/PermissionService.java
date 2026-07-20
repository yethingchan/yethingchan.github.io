package com.example.admin.security;

import com.example.admin.common.utils.SecurityUtils;
import org.springframework.stereotype.Service;

/**
 * 权限校验 Bean，SpringEL 中通过 @ps.hasPermi('xxx') 调用。
 * 与前端 v-hasPermi="'xxx'" 使用同一套权限字符串。
 */
@Service("ps")
public class PermissionService {

    public boolean hasPermi(String permission) {
        if (SecurityUtils.isAdmin()) {
            return true;
        }
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null || loginUser.getPermissions() == null) {
            return false;
        }
        return loginUser.getPermissions().contains(permission);
    }
}
