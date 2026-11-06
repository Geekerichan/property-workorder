package com.prop.wo.modules.audit.controller;

import com.prop.wo.common.annotation.RequireRole;
import com.prop.wo.common.entity.PageQuery;
import com.prop.wo.common.result.PageResult;
import com.prop.wo.common.result.Result;
import com.prop.wo.modules.audit.entity.AcceptanceRecord;
import com.prop.wo.modules.audit.service.AuditService;
import com.prop.wo.modules.workorder.dto.OrderDtos;
import com.prop.wo.modules.workorder.service.WorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 质量稽核与信用
 */
@Tag(name = "稽核与信用")
@RestController
@RequestMapping("/api/admin/audit")
@RequireRole("ADMIN")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;
    private final WorkOrderService workOrderService;

    @Operation(summary = "待稽核工单（待回访）")
    @GetMapping("/pending")
    public Result<PageResult<Map<String, Object>>> pending(PageQuery query) {
        OrderDtos.OrderQuery orderQuery = new OrderDtos.OrderQuery();
        orderQuery.setStatus("PENDING_REVIEW");
        orderQuery.setPageNum(query.getPageNum());
        orderQuery.setPageSize(query.getPageSize());
        return Result.ok(workOrderService.page(orderQuery));
    }

    @Operation(summary = "验收记录")
    @GetMapping("/acceptance/page")
    public Result<PageResult<AcceptanceRecord>> acceptance(@RequestParam(required = false) String result,
                                                           PageQuery query) {
        return Result.ok(auditService.acceptancePage(result, query));
    }

    @Operation(summary = "信用记录")
    @GetMapping("/credit/page")
    public Result<PageResult<Map<String, Object>>> credit(@RequestParam(required = false) String targetType,
                                                          @RequestParam(required = false) Long targetId,
                                                          PageQuery query) {
        return Result.ok(auditService.creditPage(targetType, targetId, query));
    }

    @Operation(summary = "信用汇总")
    @GetMapping("/credit/summary")
    public Result<List<Map<String, Object>>> creditSummary(@RequestParam(required = false) String targetType) {
        return Result.ok(auditService.creditSummary(targetType));
    }
}
