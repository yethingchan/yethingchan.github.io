package com.example.admin.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.admin.domain.SysUser;
import com.example.admin.mapper.SysMenuMapper;
import com.example.admin.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * 根据用户名加载用户 + 权限集合（供 Spring Security 与 JWT 过滤器使用）
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUserName, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getUserId());
        loginUser.setUserName(user.getUserName());
        loginUser.setPassword(user.getPassword());
        loginUser.setStatus(user.getStatus());
        Set<String> perms = new HashSet<>(menuMapper.selectPermsByUserId(user.getUserId()));
        loginUser.setPermissions(perms);
        Set<String> roles = new HashSet<>(menuMapper.selectRoleKeysByUserId(user.getUserId()));
        loginUser.setRoles(roles);
        return loginUser;
    }
}
