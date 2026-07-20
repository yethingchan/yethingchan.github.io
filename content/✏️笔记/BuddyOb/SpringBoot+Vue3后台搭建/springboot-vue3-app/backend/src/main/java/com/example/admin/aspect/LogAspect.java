package com.example.admin.aspect;

import com.example.admin.annotation.Log;
import com.example.admin.common.utils.SecurityUtils;
import com.example.admin.common.utils.ServletUtils;
import com.example.admin.domain.SysOperLog;
import com.example.admin.mapper.SysOperLogMapper;
import com.example.admin.security.LoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 操作日志切面（AOP）：在 DispatcherServlet 内执行，故异常能被 @RestControllerAdvice 同一套兜住。
 * 注意：切面顺序在事务之外/之后，避免长事务；这里只做"落库一条日志"。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final SysOperLogMapper operLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterReturning(pointcut = "@annotation(log)", returning = "ret")
    public void doAfterReturning(JoinPoint jp, Log log, Object ret) {
        record(jp, log, null);
    }

    @AfterThrowing(pointcut = "@annotation(log)", throwing = "ex")
    public void doAfterThrowing(JoinPoint jp, Log log, Exception ex) {
        record(jp, log, ex);
    }

    private void record(JoinPoint jp, Log log, Exception ex) {
        try {
            SysOperLog o = new SysOperLog();
            o.setTitle(log.title());
            o.setBusinessType(log.businessType());
            o.setStatus(ex == null ? 0 : 1);
            o.setErrorMsg(ex == null ? null : ex.getMessage());
            LoginUser lu = SecurityUtils.getLoginUser();
            o.setOperName(lu == null ? "未知" : lu.getUsername());
            HttpServletRequest request = ServletUtils.getRequest();
            if (request != null) {
                o.setOperUrl(request.getRequestURI());
                o.setRequestMethod(request.getMethod());
                o.setOperIp(request.getRemoteAddr());
            }
            o.setOperParam(argsToJson(jp));
            o.setMethod(jp.getSignature().toShortString());
            o.setOperTime(new Date());
            operLogMapper.insert(o);
        } catch (Exception ignored) {
            // 日志失败不影响主流程
        }
    }

    private String argsToJson(JoinPoint jp) {
        try {
            return objectMapper.writeValueAsString(jp.getArgs());
        } catch (Exception e) {
            return "[]";
        }
    }
}
