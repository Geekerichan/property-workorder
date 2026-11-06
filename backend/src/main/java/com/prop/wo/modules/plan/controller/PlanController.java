package com.prop.wo.modules.plan.controller;

import com.prop.wo.common.annotation.RequireRole;
import com.prop.wo.common.result.Result;
import com.prop.wo.modules.plan.entity.MaintenancePlan;
import com.prop.wo.modules.plan.entity.SeasonalNode;
import com.prop.wo.modules.plan.service.PlanService;
import com.prop.wo.modules.system.service.OperLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.Map;

/**
 * 维保计划与季节节点
 */
@Tag(name = "维保计划")
@RestController
@RequestMapping("/api/admin/plan")
@RequireRole("ADMIN")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;
    private final OperLogService operLogService;

    @Operation(summary = "计划列表")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) String facilityType,
                                                  @RequestParam(required = false) String status) {
        return Result.ok(planService.list(facilityType, status));
    }

    @Operation(summary = "保存计划（含设备范围）")
    @PostMapping("/save")
    public Result<MaintenancePlan> save(@RequestBody SaveReq req) {
        MaintenancePlan plan = planService.save(req.plan(), req.equipmentIds());
        operLogService.record("维保计划", "保存计划", plan.getId(), plan.getName());
        return Result.ok(plan);
    }

    @Operation(summary = "删除计划")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        planService.delete(id);
        operLogService.record("维保计划", "删除计划", id, "删除计划");
        return Result.ok();
    }

    @Operation(summary = "手动触发生成工单")
    @PostMapping("/{id}/generate")
    public Result<Map<String, Object>> generate(@PathVariable Long id) {
        Map<String, Object> result = planService.generate(id, true);
        operLogService.record("维保计划", "手动生成工单", id, "生成 " + result.get("count") + " 张");
        return Result.ok(result);
    }

    @Operation(summary = "生成记录")
    @GetMapping("/generate-logs")
    public Result<List<Map<String, Object>>> generateLogs(@RequestParam(required = false) Long planId) {
        return Result.ok(planService.generateLogs(planId));
    }

    @Operation(summary = "季节节点列表")
    @GetMapping("/seasonal/list")
    public Result<List<SeasonalNode>> seasonalNodes() {
        return Result.ok(planService.seasonalNodes());
    }

    @Operation(summary = "保存季节节点")
    @PostMapping("/seasonal/save")
    public Result<SeasonalNode> saveSeasonal(@RequestBody SeasonalNode node) {
        SeasonalNode saved = planService.saveSeasonal(node);
        operLogService.record("维保计划", "保存季节节点", saved.getId(), saved.getName());
        return Result.ok(saved);
    }

    @Operation(summary = "删除季节节点")
    @DeleteMapping("/seasonal/{id}")
    public Result<Void> deleteSeasonal(@PathVariable Long id) {
        planService.deleteSeasonal(id);
        operLogService.record("维保计划", "删除季节节点", id, "删除季节节点");
        return Result.ok();
    }

    @Operation(summary = "维保逾期设备清单")
    @GetMapping("/overdue")
    public Result<List<Map<String, Object>>> overdue() {
        return Result.ok(planService.overdueEquipments());
    }

    public record SaveReq(MaintenancePlan plan, List<Long> equipmentIds) {
    }
}
