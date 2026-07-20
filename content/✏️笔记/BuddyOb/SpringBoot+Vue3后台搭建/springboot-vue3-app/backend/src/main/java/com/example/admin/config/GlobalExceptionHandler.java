package com.example.admin.config;

import com.example.admin.common.AjaxResult;
import com.example.admin.common.BusinessException;
import com.example.admin.common.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常（仅兜 DispatcherServlet 之后的 Controller/Service 层）。
 * Filter 链之前的异常由 Security 的 EntryPoint / AccessDeniedHandler 处理。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public AjaxResult handleBusiness(BusinessException ex) {
        return AjaxResult.error(Integer.parseInt(ex.getCode()), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public AjaxResult handleValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return AjaxResult.error(Integer.parseInt(ErrorCode.BAD_REQUEST), msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public AjaxResult handleConstraint(ConstraintViolationException ex) {
        return AjaxResult.error(Integer.parseInt(ErrorCode.BAD_REQUEST), ex.getMessage());
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public AjaxResult handleAccessDenied(AccessDeniedException ex) {
        return AjaxResult.error(Integer.parseInt(ErrorCode.FORBIDDEN), "没有访问权限，请联系管理员");
    }

    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception ex) {
        return AjaxResult.error(Integer.parseInt(ErrorCode.SERVER_ERROR), "服务器内部异常：" + ex.getMessage());
    }
}
