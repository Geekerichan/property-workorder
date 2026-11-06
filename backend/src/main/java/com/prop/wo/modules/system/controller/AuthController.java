package com.prop.wo.modules.system.controller;

import com.prop.wo.common.context.LoginUser;
import com.prop.wo.common.result.Result;
import com.prop.wo.modules.system.dto.SystemDtos;
import com.prop.wo.modules.system.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证接口：PC 管理端与 H5 作业端
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "管理端登录")
    @PostMapping("/admin/auth/login")
    public Result<Map<String, Object>> adminLogin(@Valid @RequestBody SystemDtos.LoginReq req) {
        return Result.ok(authService.login(req, true));
    }

    @Operation(summary = "作业端登录")
    @PostMapping("/app/auth/login")
    public Result<Map<String, Object>> appLogin(@Valid @RequestBody SystemDtos.LoginReq req) {
        return Result.ok(authService.login(req, false));
    }

    @Operation(summary = "管理端当前用户")
    @GetMapping("/admin/auth/me")
    public Result<LoginUser> adminMe() {
        return Result.ok(authService.me());
    }

    @Operation(summary = "作业端当前用户")
    @GetMapping("/app/auth/me")
    public Result<LoginUser> appMe() {
        return Result.ok(authService.me());
    }

    @Operation(summary = "退出登录")
    @PostMapping({"/admin/auth/logout", "/app/auth/logout"})
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    @Operation(summary = "修改密码")
    @PostMapping({"/admin/auth/password", "/app/auth/password"})
    public Result<Void> changePassword(@Valid @RequestBody SystemDtos.PasswordReq req) {
        authService.changePassword(req);
        return Result.ok();
    }
}
