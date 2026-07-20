package com.example.admin.common;

/**
 * 错误码常量（与前端约定：非 200 都按 message 弹错）。
 */
public final class ErrorCode {
    public static final String UNAUTHORIZED = "401";
    public static final String FORBIDDEN = "403";
    public static final String BAD_REQUEST = "400";
    public static final String SERVER_ERROR = "500";

    /** 用户不存在 */
    public static final String USER_NOT_FOUND = "A001";
    /** 用户名或密码错误 */
    public static final String CREDENTIAL_WRONG = "A002";
    /** 账号已停用 */
    public static final String USER_DISABLED = "A003";
    /** 角色已分配，禁止删除 */
    public static final String ROLE_ASSIGNED = "A004";
    /** 菜单存在子菜单，禁止删除 */
    public static final String MENU_HAS_CHILD = "A005";

    private ErrorCode() {
    }
}
