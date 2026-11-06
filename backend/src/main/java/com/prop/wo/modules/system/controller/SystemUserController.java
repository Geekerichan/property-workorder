package com.prop.wo.modules.system.controller;

import com.prop.wo.common.annotation.RequireRole;
import com.prop.wo.common.entity.PageQuery;
import com.prop.wo.common.result.PageResult;
import com.prop.wo.common.result.Result;
import com.prop.wo.modules.system.dto.SystemDtos;
import com.prop.wo.modules.system.entity.SysUser;
import com.prop.wo.modules.system.service.OperLogService;
import com.prop.wo.modules.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户管理：四类角色分配
 */
@Tag(name = "系统-用户")
@RestController
@RequestMapping("/api/admin/system/user")
@RequireRole("ADMIN")
@RequiredArgsConstructor
public class SystemUserController {

    private final UserService userService;
    private final OperLogService operLogService;

    @Operation(summary = "用户分页")
    @GetMapping("/page")
    public Result<PageResult<SysUser>> page(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String roleCode,
                                            PageQuery query) {
        return Result.ok(userService.page(keyword, roleCode, query));
    }

    @Operation(summary = "按角色列出账号")
    @GetMapping("/list")
    public Result<List<SysUser>> list(@RequestParam String roleCode) {
        return Result.ok(userService.listByRole(roleCode));
    }

    @Operation(summary = "新增或修改用户")
    @PostMapping("/save")
    public Result<SysUser> save(@Valid @RequestBody SystemDtos.UserSaveReq req) {
        SysUser user = userService.save(req);
        operLogService.record("系统管理", req.id() == null ? "新增用户" : "修改用户", user.getId(), user.getUsername());
        return Result.ok(user);
    }

    @Operation(summary = "重置密码")
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestParam Long id, @RequestParam String password) {
        userService.resetPassword(id, password);
        operLogService.record("系统管理", "重置密码", id, "重置用户密码");
        return Result.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        operLogService.record("系统管理", "删除用户", id, "删除用户");
        return Result.ok();
    }
}
