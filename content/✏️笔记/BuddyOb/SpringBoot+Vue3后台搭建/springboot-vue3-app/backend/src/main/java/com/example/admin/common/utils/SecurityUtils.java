package com.example.admin.common.utils;

import com.example.admin.common.constants.UserConstants;
import com.example.admin.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

/**
 * 安全上下文工具：取出当前登录用户、权限、角色，判断管理员
 */
public class SecurityUtils {

    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    public static Long getUserId() {
        LoginUser loginUser = getLoginUser();
        return loginUser == null ? null : loginUser.getUserId();
    }

    public static String getUsername() {
        LoginUser loginUser = getLoginUser();
        return loginUser == null ? null : loginUser.getUsername();
    }

    public static Set<String> getPermissions() {
        LoginUser loginUser = getLoginUser();
        return loginUser == null ? null : loginUser.getPermissions();
    }

    public static boolean isAdmin() {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null) {
            return false;
        }
        Set<String> roles = loginUser.getRoles();
        return roles != null && roles.contains(UserConstants.ADMIN_ROLE_KEY);
    }
}
