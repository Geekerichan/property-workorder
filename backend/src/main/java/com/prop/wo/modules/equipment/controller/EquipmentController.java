package com.prop.wo.modules.equipment.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.prop.wo.common.annotation.RequireRole;
import com.prop.wo.common.entity.PageQuery;
import com.prop.wo.common.result.PageResult;
import com.prop.wo.common.result.Result;
import com.prop.wo.modules.equipment.dto.EquipmentExcelRow;
import com.prop.wo.modules.equipment.entity.AnnualCheck;
import com.prop.wo.modules.equipment.entity.Equipment;
import com.prop.wo.modules.equipment.entity.EquipmentQr;
import com.prop.wo.modules.equipment.mapper.AnnualCheckMapper;
import com.prop.wo.modules.equipment.service.EquipmentService;
import com.prop.wo.modules.system.service.OperLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import java.util.List;
import java.util.Map;

/**
 * 设施设备台账
 */
@Tag(name = "设备台账")
@RestController
@RequestMapping("/api/admin/equipment")
@RequireRole("ADMIN")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;
    private final AnnualCheckMapper annualCheckMapper;
    private final OperLogService operLogService;

    @Operation(summary = "设备分页")
    @GetMapping("/page")
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(required = false) String facilityType,
                                                        @RequestParam(required = false) Long communityId,
                                                        @RequestParam(required = false) Long buildingId,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) String keyword,
                                                        PageQuery query) {
        return Result.ok(equipmentService.page(facilityType, communityId, buildingId, status, keyword, query));
    }

    @Operation(summary = "设备详情（含扩展参数、二维码、年检、状态日志）")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(equipmentService.detail(id));
    }

    @Operation(summary = "保存设备")
    @PostMapping("/save")
    public Result<Equipment> save(@RequestBody Equipment equipment) {
        Equipment saved = equipmentService.save(equipment);
        operLogService.record("设备台账", equipment.getId() == null ? "新增设备" : "修改设备",
                saved.getId(), saved.getName());
        return Result.ok(saved);
    }

    @Operation(summary = "删除设备")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        equipmentService.delete(id);
        operLogService.record("设备台账", "删除设备", id, "删除设备");
        return Result.ok();
    }

    @Operation(summary = "设备二维码图片")
    @GetMapping(value = "/{id}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrPng(@PathVariable Long id,
                                        @RequestParam(defaultValue = "300") int size) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                .body(equipmentService.qrPng(id, Math.min(Math.max(size, 120), 800)));
    }

    @Operation(summary = "二维码内容与签名信息")
    @GetMapping("/{id}/qr-info")
    public Result<EquipmentQr> qrInfo(@PathVariable Long id) {
        return Result.ok(equipmentService.qrInfo(id));
    }

    @Operation(summary = "记录二维码打印")
    @PostMapping("/{id}/print")
    public Result<Void> print(@PathVariable Long id) {
        equipmentService.recordPrint(id);
        operLogService.record("设备台账", "打印二维码", id, "打印设备二维码");
        return Result.ok();
    }

    @Operation(summary = "新增年检记录")
    @PostMapping("/annual-check")
    public Result<AnnualCheck> saveAnnualCheck(@RequestBody AnnualCheck check) {
        AnnualCheck saved = equipmentService.saveAnnualCheck(check);
        operLogService.record("设备台账", "登记年检", saved.getEquipmentId(),
                "检验日期 " + saved.getCheckDate() + "，结论 " + saved.getResult());
        return Result.ok(saved);
    }

    @Operation(summary = "年检记录列表")
    @GetMapping("/annual-check/list")
    public Result<List<AnnualCheck>> annualChecks(@RequestParam Long equipmentId) {
        return Result.ok(annualCheckMapper.selectList(new LambdaQueryWrapper<AnnualCheck>()
                .eq(AnnualCheck::getEquipmentId, equipmentId).orderByDesc(AnnualCheck::getCheckDate)));
    }

    @Operation(summary = "变更设备状态（锁停/恢复）")
    @PostMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam String status,
                                     @RequestParam(required = false) String reason) {
        equipmentService.changeStatus(id, status, reason);
        operLogService.record("设备台账", "变更设备状态", id, status + "：" + (reason == null ? "" : reason));
        return Result.ok();
    }

    @Operation(summary = "设备下拉选项")
    @GetMapping("/options")
    public Result<List<Equipment>> options(@RequestParam(required = false) String facilityType) {
        return Result.ok(equipmentService.options(facilityType));
    }

    @Operation(summary = "导出台账（迎检）")
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String facilityType,
                       @RequestParam(required = false) Long communityId,
                       HttpServletResponse response) throws IOException {
        List<EquipmentExcelRow> rows = equipmentService.exportRows(facilityType, communityId);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("设备台账", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), EquipmentExcelRow.class).sheet("设备台账").doWrite(rows);
        operLogService.record("导出中心", "导出设备台账", null, "导出 " + rows.size() + " 条");
    }
}
