package com.prop.wo.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.prop.wo.common.annotation.RequireRole;
import com.prop.wo.common.entity.PageQuery;
import com.prop.wo.common.result.PageResult;
import com.prop.wo.common.result.Result;
import com.prop.wo.modules.system.entity.SysLogLogin;
import com.prop.wo.modules.system.entity.SysLogOper;
import com.prop.wo.modules.system.mapper.SysLogLoginMapper;
import com.prop.wo.modules.system.mapper.SysLogOperMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审计日志查询
 */
@Tag(name = "系统-日志")
@RestController
@RequestMapping("/api/admin/system/log")
@RequireRole("ADMIN")
@RequiredArgsConstructor
public class SystemLogController {

    private final SysLogLoginMapper loginMapper;
    private final SysLogOperMapper operMapper;

    @Operation(summary = "登录日志")
    @GetMapping("/login")
    public Result<PageResult<SysLogLogin>> loginLogs(@RequestParam(required = false) String keyword, PageQuery query) {
        Page<SysLogLogin> page = loginMapper.selectPage(new Page<>(query.safePageNum(), query.safePageSize()),
                new LambdaQueryWrapper<SysLogLogin>()
                        .like(keyword != null && !keyword.isBlank(), SysLogLogin::getUsername, keyword)
                        .orderByDesc(SysLogLogin::getId));
        return Result.ok(PageResult.of(page));
    }

    @Operation(summary = "操作日志")
    @GetMapping("/oper")
    public Result<PageResult<SysLogOper>> operLogs(@RequestParam(required = false) String keyword, PageQuery query) {
        Page<SysLogOper> page = operMapper.selectPage(new Page<>(query.safePageNum(), query.safePageSize()),
                new LambdaQueryWrapper<SysLogOper>()
                        .and(keyword != null && !keyword.isBlank(), w -> w
                                .like(SysLogOper::getAction, keyword).or().like(SysLogOper::getModule, keyword))
                        .orderByDesc(SysLogOper::getId));
        return Result.ok(PageResult.of(page));
    }
}
