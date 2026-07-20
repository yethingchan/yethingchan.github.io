package com.example.admin.common.constants;

/**
 * 用户相关常量
 */
public final class UserConstants {
    /** 管理员角色 key */
    public static final String ADMIN_ROLE_KEY = "admin";
    /** 普通操作员角色 key */
    public static final String OPERATOR_ROLE_KEY = "operator";
    /** 管理员用户 ID（种子数据） */
    public static final Long ADMIN_ID = 1L;
    /** 初始化默认密码 */
    public static final String USER_SEED_PASSWORD = "123456";
    /** 正常状态 */
    public static final String STATUS_NORMAL = "0";
    /** 停用状态 */
    public static final String STATUS_DISABLE = "1";

    private UserConstants() {
    }
}
