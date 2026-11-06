package com.prop.wo.modules.workorder.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.prop.wo.common.annotation.Idempotent;
import com.prop.wo.common.annotation.RequireRole;
import com.prop.wo.common.entity.PageQuery;
import com.prop.wo.common.result.PageResult;
import com.prop.wo.common.result.Result;
import com.prop.wo.modules.basedata.entity.Personnel;
import com.prop.wo.modules.basedata.mapper.PersonnelMapper;
import com.prop.wo.modules.basedata.service.StaffService;
import com.prop.wo.modules.workorder.dto.OrderDtos;
import com.prop.wo.modules.workorder.entity.WorkOrder;
import com.prop.wo.modules.workorder.entity.WorkOrderClaim;
import com.prop.wo.modules.workorder.mapper.WorkOrderClaimMapper;
import com.prop.wo.modules.workorder.service.WorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工单管理（物业端）
 */
@Tag(name = "工单-管理端")
@RestController
@RequestMapping("/api/admin/workorder")
@RequireRole("ADMIN")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final StaffService staffService;
    private final WorkOrderClaimMapper claimMapper;
    private final PersonnelMapper personnelMapper;

    @Operation(summary = "工单分页（维保公司仅见本公司）")
    @GetMapping("/page")
    @RequireRole({"ADMIN", "COMPANY"})
    public Result<PageResult<Map<String, Object>>> page(OrderDtos.OrderQuery query) {
        return Result.ok(workOrderService.page(query));
    }

    @Operation(summary = "工单详情（含留痕时间轴）")
    @GetMapping("/{id}")
    @RequireRole({"ADMIN", "COMPANY"})
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(workOrderService.detail(id));
    }

    @Operation(summary = "创建工单")
    @PostMapping("/create")
    @Idempotent
    public Result<WorkOrder> create(@Valid @RequestBody OrderDtos.OrderCreateReq req) {
        return Result.ok(workOrderService.create(req));
    }

    @Operation(summary = "指定派单")
    @PostMapping("/{id}/dispatch")
    @Idempotent
    public Result<WorkOrder> dispatch(@PathVariable Long id, @RequestBody OrderDtos.DispatchReq req) {
        return Result.ok(workOrderService.dispatch(id, req.personnelId(), req.teamId(), req.remark(),
                req.assistPersonnelIds()));
    }

    @Operation(summary = "批量派单")
    @PostMapping("/batch-dispatch")
    public Result<Void> batchDispatch(@RequestBody OrderDtos.BatchDispatchReq req) {
        workOrderService.batchDispatch(req.orderIds(), req.personnelId(), req.teamId());
        return Result.ok();
    }

    @Operation(summary = "转派")
    @PostMapping("/{id}/transfer")
    public Result<WorkOrder> transfer(@PathVariable Long id, @RequestBody OrderDtos.TransferReq req) {
        return Result.ok(workOrderService.transfer(id, req.personnelId(), req.reason()));
    }

    @Operation(summary = "催办")
    @PostMapping("/{id}/urge")
    public Result<WorkOrder> urge(@PathVariable Long id) {
        return Result.ok(workOrderService.urge(id));
    }

    @Operation(summary = "验收（通过/驳回重做）")
    @PostMapping("/{id}/accept")
    @Idempotent
    public Result<WorkOrder> accept(@PathVariable Long id, @RequestBody OrderDtos.AcceptReq req) {
        return Result.ok(workOrderService.accept(id, req));
    }

    @Operation(summary = "归档")
    @PostMapping("/{id}/archive")
    public Result<WorkOrder> archive(@PathVariable Long id) {
        return Result.ok(workOrderService.archive(id));
    }

    @Operation(summary = "回访与满意度评价")
    @PostMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id, @RequestBody OrderDtos.ReviewReq req) {
        workOrderService.review(id, req);
        return Result.ok();
    }

    @Operation(summary = "退回重新派单")
    @PostMapping("/{id}/return")
    public Result<WorkOrder> returnToDispatch(@PathVariable Long id, @RequestBody OrderDtos.ReasonReq req) {
        return Result.ok(workOrderService.returnToDispatch(id, req.reason()));
    }

    @Operation(summary = "作废")
    @PostMapping("/{id}/cancel")
    public Result<WorkOrder> cancel(@PathVariable Long id, @RequestBody OrderDtos.ReasonReq req) {
        return Result.ok(workOrderService.cancel(id, req.reason()));
    }

    @Operation(summary = "公示发布/取消")
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id, @RequestBody OrderDtos.PublishReq req) {
        workOrderService.publish(id, req);
        return Result.ok();
    }

    @Operation(summary = "派单工作台：待派池、人员状态、抢单监控")
    @GetMapping("/dispatch-desk")
    public Result<Map<String, Object>> dispatchDesk() {
        OrderDtos.OrderQuery query = new OrderDtos.OrderQuery();
        query.setStatus("PENDING_DISPATCH");
        query.setPageSize(100);
        List<Map<String, Object>> pool = workOrderService.page(query).getList();
        List<Map<String, Object>> personnel = staffService.personnelPage(null, null, null, null, pageQuery()).getList();
        List<WorkOrderClaim> claims = claimMapper.selectList(new LambdaQueryWrapper<WorkOrderClaim>()
                .orderByDesc(WorkOrderClaim::getId).last("limit 20"));
        List<Map<String, Object>> claimRows = new ArrayList<>();
        for (WorkOrderClaim claim : claims) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", claim.getId());
            row.put("orderId", claim.getOrderId());
            row.put("claimTime", claim.getClaimTime());
            row.put("success", claim.getSuccess());
            row.put("failReason", claim.getFailReason());
            Personnel p = personnelMapper.selectById(claim.getPersonnelId());
            row.put("personnelName", p == null ? null : p.getName());
            claimRows.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pool", pool);
        result.put("personnel", personnel);
        result.put("claims", claimRows);
        return Result.ok(result);
    }

    private PageQuery pageQuery() {
        PageQuery query = new PageQuery();
        query.setPageSize(200);
        return query;
    }

    @Operation(summary = "批量导出工单")
    @GetMapping("/export")
    public void export(OrderDtos.OrderQuery query, HttpServletResponse response) throws IOException {
        List<com.prop.wo.modules.workorder.dto.WorkOrderExcelRow> rows = workOrderService.exportRows(query);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("工单批量导出", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), com.prop.wo.modules.workorder.dto.WorkOrderExcelRow.class)
                .sheet("工单").doWrite(rows);
    }

    @Operation(summary = "结算记录")
    @GetMapping("/settlements")
    @RequireRole({"ADMIN", "COMPANY"})
    public Result<PageResult<Map<String, Object>>> settlements(@RequestParam(required = false) Long companyId,
                                                               PageQuery query) {
        return Result.ok(workOrderService.settlements(null, companyId, query));
    }
}
