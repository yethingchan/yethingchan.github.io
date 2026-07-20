package com.example.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.admin.common.BusinessException;
import com.example.admin.common.constants.UserConstants;
import com.example.admin.common.utils.StringUtils;
import com.example.admin.domain.SysUser;
import com.example.admin.domain.SysUserRole;
import com.example.admin.mapper.SysUserMapper;
import com.example.admin.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public IPage<SysUser> listUsers(IPage<SysUser> page, String userName, String status) {
        LambdaQueryWrapper<SysUser> q = new LambdaQueryWrapper<>();
        q.like(StringUtils.isNotBlank(userName), SysUser::getUserName, userName);
        q.eq(StringUtils.isNotBlank(status), SysUser::getStatus, status);
        q.orderByDesc(SysUser::getUserId);
        return userMapper.selectPage(page, q);
    }

    public Map<String, Object> getUser(Long id) {
        SysUser user = userMapper.selectById(id);
        List<Long> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id))
                .stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("roleIds", roleIds);
        return result;
    }

    public void addUser(SysUser user, List<Long> roleIds) {
        user.setPassword(passwordEncoder.encode(
                StringUtils.isBlank(user.getPassword())
                        ? UserConstants.USER_SEED_PASSWORD : user.getPassword()));
        userMapper.insert(user);
        saveUserRoles(user.getUserId(), roleIds);
    }

    public void updateUser(SysUser user, List<Long> roleIds) {
        LambdaUpdateWrapper<SysUser> uw = new LambdaUpdateWrapper<>();
        uw.eq(SysUser::getUserId, user.getUserId());
        uw.set(SysUser::getNickName, user.getNickName());
        uw.set(SysUser::getEmail, user.getEmail());
        uw.set(SysUser::getPhonenumber, user.getPhonenumber());
        uw.set(SysUser::getSex, user.getSex());
        uw.set(SysUser::getStatus, user.getStatus());
        uw.set(SysUser::getRemark, user.getRemark());
        if (StringUtils.isNotBlank(user.getPassword())) {
            uw.set(SysUser::getPassword, passwordEncoder.encode(user.getPassword()));
        }
        userMapper.update(null, uw);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getUserId()));
        saveUserRoles(user.getUserId(), roleIds);
    }

    public void deleteUsers(List<Long> ids) {
        for (Long id : ids) {
            if (UserConstants.ADMIN_ID.equals(id)) {
                throw new BusinessException("A006", "不能删除管理员账号");
            }
            userMapper.deleteById(id);
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        }
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }
}
