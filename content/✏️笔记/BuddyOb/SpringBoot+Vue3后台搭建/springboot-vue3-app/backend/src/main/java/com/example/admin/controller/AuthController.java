package com.example.admin.controller;

import com.example.admin.common.AjaxResult;
import com.example.admin.common.dto.LoginBody;
import com.example.admin.common.utils.SecurityUtils;
import com.example.admin.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证接口（免鉴权：SecurityConfig 已 permitAll）
 */
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AjaxResult login(@Valid @RequestBody LoginBody body) {
        String token = authService.login(body);
        Map<String, String> data = new HashMap<>();
        data.put("token", token);
        return AjaxResult.success(data);
    }

    @GetMapping("/getInfo")
    public AjaxResult getInfo() {
        return AjaxResult.success(authService.getInfo());
    }

    @GetMapping("/getRouters")
    public AjaxResult getRouters() {
        return AjaxResult.success(authService.getRouters());
    }

    @PostMapping("/logout")
    public AjaxResult logout() {
        authService.logout();
        return AjaxResult.success("退出成功");
    }
}
