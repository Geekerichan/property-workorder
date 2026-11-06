package com.prop.wo.modules.inspection.controller;

import com.alibaba.excel.EasyExcel;
import com.prop.wo.common.annotation.RequireRole;
import com.prop.wo.common.entity.PageQuery;
import com.prop.wo.common.result.PageResult;
import com.prop.wo.common.result.Result;
import com.prop.wo.modules.inspection.dto.InspectionDtos;
import com.prop.wo.modules.inspection.dto.InspectionExcelRow;
import com.prop.wo.modules.inspection.entity.InspectionRoute;
import com.prop.wo.modules.inspection.entity.InspectionTask;
import com.prop.wo.modules.inspection.entity.InspectionTaskPoint;
import com.prop.wo.modules.inspection.service.InspectionService;
import com.prop.wo.modules.system.service.OperLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 巡检配置端与执行端
 */
@Tag(name = "巡检")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;
    private final com.prop.wo.modules.evidence.service.EvidenceService evidenceService;
    private final OperLogService operLogService;

    @Operation(summary = "路线列表")
    @GetMapping("/admin/inspection/route/list")
    @RequireRole("ADMIN")
    public Result<List<Map<String, Object>>> routes(@RequestParam(required = false) String facilityType) {
        return Result.ok(inspectionService.routes(facilityType));
    }

    @Operation(summary = "路线详情（含点位与检查项）")
    @GetMapping("/admin/inspection/route/{id}")
    @RequireRole("ADMIN")
    public Result<Map<String, Object>> routeDetail(@PathVariable Long id) {
        return Result.ok(inspectionService.routeDetail(id));
    }

    @Operation(summary = "保存路线")
    @PostMapping("/admin/inspection/route/save")
    @RequireRole("ADMIN")
    public Result<InspectionRoute> saveRoute(@RequestBody InspectionDtos.RouteSaveReq req) {
        InspectionRoute route = inspectionService.saveRoute(req);
        operLogService.record("巡检管理", "保存路线", route.getId(), route.getName());
        return Result.ok(route);
    }

    @Operation(summary = "删除路线")
    @DeleteMapping("/admin/inspection/route/{id}")
    @RequireRole("ADMIN")
    public Result<Void> deleteRoute(@PathVariable Long id) {
        inspectionService.deleteRoute(id);
        operLogService.record("巡检管理", "删除路线", id, "删除路线");
        return Result.ok();
    }

    @Operation(summary = "手动生成巡检任务")
    @PostMapping("/admin/inspection/task/generate")
    @RequireRole("ADMIN")
    public Result<InspectionTask> generate(@RequestParam Long routeId) {
        InspectionTask task = inspectionService.generateTaskManually(routeId);
        operLogService.record("巡检管理", "生成巡检任务", task.getId(), task.getTaskNo());
        return Result.ok(task);
    }

    @Operation(summary = "巡检任务分页")
    @GetMapping("/admin/inspection/task/page")
    @RequireRole("ADMIN")
    public Result<PageResult<Map<String, Object>>> taskPage(@RequestParam(required = false) Long routeId,
                                                            @RequestParam(required = false) String status,
                                                            PageQuery query) {
        return Result.ok(inspectionService.taskPage(routeId, status, query));
    }

    @Operation(summary = "巡检统计（完成率、隐患数）")
    @GetMapping("/admin/inspection/stats")
    @RequireRole("ADMIN")
    public Result<Map<String, Object>> stats(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(inspectionService.statistics(days));
    }

    @Operation(summary = "巡检记录导出")
    @GetMapping("/admin/inspection/export")
    @RequireRole("ADMIN")
    public void export(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                       HttpServletResponse response) throws IOException {
        List<InspectionExcelRow> rows = inspectionService.exportRows(start, end);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("巡检记录", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), InspectionExcelRow.class).sheet("巡检记录").doWrite(rows);
        operLogService.record("导出中心", "导出巡检记录", null, "导出 " + rows.size() + " 行");
    }

    // ---------------- 执行端 ----------------

    @Operation(summary = "我的巡检任务")
    @GetMapping("/app/inspection/my-tasks")
    @RequireRole("WORKER")
    public Result<List<Map<String, Object>>> myTasks() {
        return Result.ok(inspectionService.myTasks());
    }

    @Operation(summary = "巡检任务详情")
    @GetMapping("/app/inspection/task/{id}")
    public Result<Map<String, Object>> taskDetail(@PathVariable Long id) {
        return Result.ok(inspectionService.taskDetail(id));
    }

    @Operation(summary = "点位提交（正常/异常 + 读数 + 照片）")
    @PostMapping("/app/inspection/point/submit")
    @RequireRole("WORKER")
    public Result<InspectionTaskPoint> submitPoint(@RequestBody InspectionDtos.PointSubmitReq req) {
        return Result.ok(inspectionService.submitPoint(req));
    }

    @Operation(summary = "结束巡检")
    @PostMapping("/app/inspection/task/{id}/complete")
    @RequireRole("WORKER")
    public Result<InspectionTask> complete(@PathVariable Long id) {
        return Result.ok(inspectionService.completeTask(id));
    }

    @Operation(summary = "巡检拍照（服务端水印）")
    @PostMapping("/app/inspection/photo")
    @RequireRole("WORKER")
    public Result<com.prop.wo.modules.evidence.entity.EvidencePhoto> uploadPhoto(
            @RequestParam Long taskId,
            @RequestPart("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(defaultValue = "OTHER") String photoType,
            @RequestParam(required = false) java.math.BigDecimal longitude,
            @RequestParam(required = false) java.math.BigDecimal latitude) {
        return Result.ok(evidenceService.uploadForTask(taskId, null, file, photoType, longitude, latitude));
    }

    @Operation(summary = "异常点一键转报修工单")
    @PostMapping("/app/inspection/to-order")
    @RequireRole("WORKER")
    public Result<Long> toOrder(@RequestBody InspectionDtos.ToOrderReq req) {
        Long orderId = inspectionService.toWorkOrder(req);
        operLogService.record("巡检管理", "异常转工单", orderId, "巡检异常生成工单");
        return Result.ok(orderId);
    }

    @Operation(summary = "我的巡检历史")
    @GetMapping("/app/inspection/history")
    @RequireRole("WORKER")
    public Result<List<Map<String, Object>>> history() {
        return Result.ok(inspectionService.myHistory());
    }
}
