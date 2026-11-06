package com.prop.wo.modules.owner.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prop.wo.common.annotation.RequireRole;
import com.prop.wo.common.entity.PageQuery;
import com.prop.wo.common.result.PageResult;
import com.prop.wo.common.result.Result;
import com.prop.wo.modules.owner.dto.OwnerDtos;
import com.prop.wo.modules.owner.entity.PublishNotice;
import com.prop.wo.modules.owner.service.OwnerService;
import com.prop.wo.modules.workorder.entity.WorkOrder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 业主端 H5 接口（扫码查询与公示无需登录）
 */
@Tag(name = "业主端")
@RestController
@RequestMapping("/api/h5")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;

    @Operation(summary = "发送验证码（开发环境固定 123456）")
    @PostMapping("/auth/sms-code")
    public Result<Map<String, Object>> smsCode(@Valid @RequestBody OwnerDtos.SmsReq req) {
        return Result.ok(ownerService.sendCode(req.phone()));
    }

    @Operation(summary = "业主登录（手机号+验证码，可校验房号）")
    @PostMapping("/auth/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody OwnerDtos.LoginReq req) {
        return Result.ok(ownerService.login(req));
    }

    @Operation(summary = "业主当前信息与绑定房屋")
    @GetMapping("/owner/me")
    @RequireRole("OWNER")
    public Result<Map<String, Object>> me() {
        return Result.ok(ownerService.me());
    }

    @Operation(summary = "退出登录")
    @PostMapping("/owner/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.ok();
    }

    @Operation(summary = "扫码查询设备公开信息（无需登录）")
    @GetMapping("/equip/{code}")
    public Result<Map<String, Object>> equipment(@PathVariable String code,
                                                 @RequestParam(required = false) String sign) {
        return Result.ok(ownerService.equipmentInfo(code, sign));
    }

    @Operation(summary = "报修可选设备（消防设备不返回）")
    @GetMapping("/public/equipments")
    public Result<List<Map<String, Object>>> equipments(@RequestParam(required = false) Long communityId) {
        return Result.ok(ownerService.publicEquipments(communityId));
    }

    @Operation(summary = "提交报修")
    @PostMapping("/owner/report")
    @RequireRole("OWNER")
    public Result<WorkOrder> report(@Valid @RequestBody OwnerDtos.ReportReq req) {
        return Result.ok(ownerService.report(req));
    }

    @Operation(summary = "我的上报列表")
    @GetMapping("/owner/reports")
    @RequireRole("OWNER")
    public Result<PageResult<Map<String, Object>>> reports(PageQuery query) {
        return Result.ok(ownerService.myReports(query));
    }

    @Operation(summary = "上报详情与进度")
    @GetMapping("/owner/reports/{id}")
    @RequireRole("OWNER")
    public Result<Map<String, Object>> reportDetail(@PathVariable Long id) {
        return Result.ok(ownerService.reportDetail(id));
    }

    @Operation(summary = "满意度评价")
    @PostMapping("/owner/reports/{id}/evaluate")
    @RequireRole("OWNER")
    public Result<Void> evaluate(@PathVariable Long id, @RequestBody OwnerDtos.EvaluateReq req) {
        ownerService.evaluate(id, req);
        return Result.ok();
    }

    @Operation(summary = "提交投诉")
    @PostMapping("/owner/complaint")
    @RequireRole("OWNER")
    public Result<WorkOrder> complaint(@Valid @RequestBody OwnerDtos.ComplaintReq req) {
        return Result.ok(ownerService.complaint(req));
    }

    @Operation(summary = "公示栏（消防内容永久过滤）")
    @GetMapping("/notice/page")
    public Result<PageResult<Map<String, Object>>> notices(PageQuery query) {
        return Result.ok(ownerService.notices(query));
    }

    @Operation(summary = "公示详情")
    @GetMapping("/notice/{id}")
    public Result<PublishNotice> noticeDetail(@PathVariable Long id) {
        return Result.ok(ownerService.noticeDetail(id));
    }
}
