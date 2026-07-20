package com.example.admin.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.admin.common.constants.UserConstants;
import com.example.admin.domain.SysRole;
import com.example.admin.domain.SysUser;
import com.example.admin.domain.SysUserRole;
import com.example.admin.mapper.SysRoleMapper;
import com.example.admin.mapper.SysUserMapper;
import com.example.admin.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 启动种子：若还没有 admin 账号，则用 Spring BCrypt 真算法创建 admin / operator。
 * 用运行时加密（而非硬编码哈希），规避 Spring BCrypt 版本前缀兼容风险。
 */
@Component
@RequiredArgsConstructor
public class DataInitRunner implements CommandLineRunner {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserName, "admin")) > 0) {
            return;
        }
        String encoded = passwordEncoder.encode(UserConstants.USER_SEED_PASSWORD);

        SysUser admin = new SysUser();
        admin.setUserName("admin");
        admin.setNickName("管理员");
        admin.setPassword(encoded);
        admin.setStatus(UserConstants.STATUS_NORMAL);
        admin.setCreateTime(new Date());
        userMapper.insert(admin);

        SysUser operator = new SysUser();
        operator.setUserName("operator");
        operator.setNickName("操作员");
        operator.setPassword(encoded);
        operator.setStatus(UserConstants.STATUS_NORMAL);
        operator.setCreateTime(new Date());
        userMapper.insert(operator);

        link(admin.getUserId(), UserConstants.ADMIN_ROLE_KEY);
        link(operator.getUserId(), UserConstants.OPERATOR_ROLE_KEY);
    }

    private void link(Long userId, String roleKey) {
        SysRole role = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleKey, roleKey));
        if (role == null) {
            return;
        }
        SysUserRole ur = new SysUserRole();
        ur.setUserId(userId);
        ur.setRoleId(role.getRoleId());
        userRoleMapper.insert(ur);
    }
}
