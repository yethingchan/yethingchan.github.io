package com.example.admin.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Servlet 工具：从请求上下文取 request / response
 */
public class ServletUtils {

    public static HttpServletRequest getRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    public static HttpServletResponse getResponse() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getResponse();
    }

    /** 写出 JSON（用于 Filter / 异常处理器 等拿不到 @RestControllerAdvice 的场景） */
    public static void renderJson(HttpServletResponse response, String json) {
        renderJson(response, json, HttpServletResponse.SC_OK);
    }

    /** 写出 JSON 并显式设置 HTTP 状态码（401/403 需让前端/网关按状态码判断） */
    public static void renderJson(HttpServletResponse response, String json, int status) {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        try {
            response.getWriter().write(json);
            response.getWriter().flush();
        } catch (Exception e) {
            // ignore
        }
    }
}
