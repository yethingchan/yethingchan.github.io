package com.example.admin.security;

import com.example.admin.common.AjaxResult;
import com.example.admin.common.ErrorCode;
import com.example.admin.common.utils.ServletUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 未认证（匿名访问受保护资源）→ 返回 401 JSON。
 * 注意：必须在 EntryPoint 手写 JSON，因为 JWT 过滤器在 DispatcherServlet 之前，
 * 这里的异常 @RestControllerAdvice 兜不到，否则前端会收到 Tomcat 的 HTML 错误页。
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        AjaxResult result = AjaxResult.error(Integer.parseInt(ErrorCode.UNAUTHORIZED),
                "认证失败，请重新登录");
        ServletUtils.renderJson(response, objectMapper.writeValueAsString(result),
                Integer.parseInt(ErrorCode.UNAUTHORIZED));
    }
}
