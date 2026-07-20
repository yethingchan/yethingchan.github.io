package com.example.admin.security;

import com.example.admin.common.AjaxResult;
import com.example.admin.common.ErrorCode;
import com.example.admin.common.utils.ServletUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 已认证但无权限（@PreAuthorize 不满足）→ 返回 403 JSON。
 * 同样需手写 JSON，原因同 EntryPoint（在 DispatcherServlet 之前）。
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        AjaxResult result = AjaxResult.error(Integer.parseInt(ErrorCode.FORBIDDEN),
                "没有访问权限，请联系管理员");
        ServletUtils.renderJson(response, objectMapper.writeValueAsString(result),
                Integer.parseInt(ErrorCode.FORBIDDEN));
    }
}
