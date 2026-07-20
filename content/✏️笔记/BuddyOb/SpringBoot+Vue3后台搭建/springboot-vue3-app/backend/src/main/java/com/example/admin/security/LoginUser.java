package com.example.admin.security;

import com.example.admin.common.constants.UserConstants;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 登录用户主体（实现 UserDetails）
 * 权限字符串直接作为 GrantedAuthority，供 @ps.hasPermi / hasAuthority 使用
 */
@Data
public class LoginUser implements UserDetails {
    private Long userId;
    private String userName;
    private String password;
    private String status;
    private Set<String> permissions;
    private Set<String> roles;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<String> perms = this.permissions == null ? Collections.emptySet() : this.permissions;
        List<GrantedAuthority> list = new ArrayList<>(perms.size());
        for (String p : perms) {
            list.add(new SimpleGrantedAuthority(p));
        }
        return list;
    }

    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserConstants.STATUS_NORMAL.equals(status);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return UserConstants.STATUS_NORMAL.equals(status);
    }
}
