package com.prop.wo.modules.workorder.controller;

import com.prop.wo.common.annotation.Idempotent;
import com.prop.wo.common.annotation.RequireRole;
import com.prop.wo.common.context.LoginUserHolder;
import com.prop.wo.common.entity.PageQuery;
import com.prop.wo.common.result.PageResult;
import com.prop.wo.common.result.Result;
import com.prop.wo.modules.workorder.dto.OrderDtos;
import com.prop.wo.modules.workorder.entity.WorkOrder;
import com.prop.wo.modules.workorder.service.WorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * 作业端工单接口
 */
@Tag(name = "工单-作业端")
@RestController
@RequestMapping("/api/app/workorder")
@RequireRole({"WORKER", "COMPANY"})
@RequiredArgsConstructor
public class AppWorkOrderController {

    private final WorkOrderService workOrderService;

    @Operation(summary = "工单大厅（开放抢单）")
    @GetMapping("/hall")
    @RequireRole("WORKER")
    public Result<List<Map<String, Object>>> hall(@RequestParam(required = false) String facilityType) {
        return Result.ok(workOrderService.hall(facilityType));
    }

    @Operation(summary = "我的任务（按状态分组）")
    @GetMapping("/my-tasks")
    @RequireRole("WORKER")
    public Result<Map<String, Object>> myTasks() {
        return Result.ok(workOrderService.myTasks());
    }

    @Operation(summary = "工单详情")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(workOrderService.detail(id));
    }

    @Operation(summary = "抢单")
    @PostMapping("/{id}/claim")
    @RequireRole("WORKER")
    @Idempotent
    public Result<WorkOrder> claim(@PathVariable Long id) {
        return Result.ok(workOrderService.claim(id));
    }

    @Operation(summary = "开始作业")
    @PostMapping("/{id}/start")
    @RequireRole("WORKER")
    public Result<WorkOrder> start(@PathVariable Long id) {
        return Result.ok(workOrderService.start(id));
    }

    @Operation(summary = "完工提交（含作业清单）")
    @PostMapping("/{id}/submit")
    @RequireRole("WORKER")
    @Idempotent
    public Result<WorkOrder> submit(@PathVariable Long id, @RequestBody OrderDtos.SubmitReq req) {
        return Result.ok(workOrderService.submit(id, req.results()));
    }

    @Operation(summary = "我的业绩与结算记录")
    @GetMapping("/performance")
    public Result<Map<String, Object>> performance() {
        return Result.ok(workOrderService.performance());
    }

    @Operation(summary = "结算记录分页")
    @GetMapping("/settlements")
    public Result<PageResult<Map<String, Object>>> settlements(PageQuery query) {
        Long personnelId = LoginUserHolder.personnelId();
        Long companyId = "COMPANY".equals(LoginUserHolder.role()) ? LoginUserHolder.companyId() : null;
        return Result.ok(workOrderService.settlements(companyId == null ? personnelId : null, companyId, query));
    }
}
