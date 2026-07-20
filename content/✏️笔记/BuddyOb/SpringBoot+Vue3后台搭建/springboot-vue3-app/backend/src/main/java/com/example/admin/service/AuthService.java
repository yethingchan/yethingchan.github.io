package com.example.admin.service;

import com.example.admin.common.BusinessException;
import com.example.admin.common.ErrorCode;
import com.example.admin.common.dto.LoginBody;
import com.example.admin.common.utils.SecurityUtils;
import com.example.admin.domain.SysMenu;
import com.example.admin.domain.vo.MetaVO;
import com.example.admin.domain.vo.RouterVO;
import com.example.admin.mapper.SysMenuMapper;
import com.example.admin.security.JwtUtils;
import com.example.admin.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 认证服务：登录签发 JWT、获取用户信息、动态路由
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final SysMenuMapper menuMapper;
    private final MenuService menuService;

    public String login(LoginBody loginBody) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginBody.getUserName(), loginBody.getPassword()));
        } catch (AuthenticationException e) {
            throw new BusinessException(ErrorCode.CREDENTIAL_WRONG, "用户名或密码错误");
        }
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return jwtUtils.generateToken(loginUser);
    }

    public Map<String, Object> getInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        Set<String> roles = loginUser.getRoles() == null ? Collections.emptySet() : loginUser.getRoles();
        Set<String> perms = loginUser.getPermissions() == null ? Collections.emptySet() : loginUser.getPermissions();

        Map<String, Object> user = new HashMap<>();
        user.put("userId", loginUser.getUserId());
        user.put("userName", loginUser.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("roles", new ArrayList<>(roles));
        result.put("permissions", new ArrayList<>(perms));
        return result;
    }

    public List<RouterVO> getRouters() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        List<SysMenu> menus = menuMapper.selectMenuTreeByUserId(userId);
        return menuService.buildRouters(menus);
    }

    public void logout() {
        // 无状态 JWT：服务端无需销毁，前端丢弃 token 即可
        SecurityUtils.getLoginUser();
    }
}
