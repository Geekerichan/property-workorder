package com.prop.wo.modules.checklist.controller;

import com.prop.wo.common.annotation.RequireRole;
import com.prop.wo.common.result.Result;
import com.prop.wo.modules.checklist.entity.ChecklistItem;
import com.prop.wo.modules.checklist.entity.ChecklistTemplate;
import com.prop.wo.modules.checklist.service.ChecklistService;
import com.prop.wo.modules.workorder.entity.WorkOrder;
import com.prop.wo.modules.workorder.mapper.WorkOrderMapper;
import com.prop.wo.modules.system.service.OperLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 作业清单模板与工单清单
 */
@Tag(name = "作业清单")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChecklistController {

    private final ChecklistService checklistService;
    private final WorkOrderMapper workOrderMapper;
    private final OperLogService operLogService;

    @Operation(summary = "模板列表")
    @GetMapping("/admin/checklist/template/list")
    @RequireRole("ADMIN")
    public Result<List<ChecklistTemplate>> templates(@RequestParam(required = false) String facilityType) {
        return Result.ok(checklistService.templates(facilityType));
    }

    @Operation(summary = "模板清单项")
    @GetMapping("/admin/checklist/template/items")
    @RequireRole("ADMIN")
    public Result<List<ChecklistItem>> items(@RequestParam Long templateId) {
        return Result.ok(checklistService.templateItems(templateId));
    }

    @Operation(summary = "保存模板（含清单项）")
    @PostMapping("/admin/checklist/template/save")
    @RequireRole("ADMIN")
    public Result<ChecklistTemplate> save(@RequestBody TemplateSaveReq req) {
        ChecklistTemplate template = checklistService.saveTemplate(req.template(), req.items());
        operLogService.record("系统管理", "保存作业清单模板", template.getId(), template.getName());
        return Result.ok(template);
    }

    @Operation(summary = "工单清单（分组）")
    @GetMapping({"/app/checklist", "/admin/checklist"})
    public Result<Map<String, Object>> orderChecklist(@RequestParam Long orderId) {
        WorkOrder order = workOrderMapper.selectById(orderId);
        return Result.ok(Map.of(
                "groups", checklistService.groupedItems(orderId),
                "completeness", checklistService.completeness(orderId),
                "mandatoryHint", checklistService.mandatoryHint(order == null ? null : order.getFacilityType())
        ));
    }

    @Operation(summary = "保存清单勾选结果")
    @PostMapping("/app/checklist/save")
    @RequireRole({"WORKER", "ADMIN"})
    public Result<Void> saveResults(@RequestBody ResultSaveReq req) {
        checklistService.saveResults(req.orderId(), req.results());
        return Result.ok();
    }

    @Operation(summary = "强制拍照项提示")
    @GetMapping("/admin/checklist/hint")
    @RequireRole("ADMIN")
    public Result<String> hint(@RequestParam String facilityType) {
        return Result.ok(checklistService.mandatoryHint(facilityType));
    }

    public record TemplateSaveReq(ChecklistTemplate template, List<ChecklistItem> items) {
    }

    public record ResultSaveReq(Long orderId, List<ChecklistService.ChecklistResult> results) {
    }
}
