package com.prop.wo.modules.export.controller;

import com.prop.wo.common.annotation.RequireRole;
import com.prop.wo.common.entity.PageQuery;
import com.prop.wo.common.result.PageResult;
import com.prop.wo.common.result.Result;
import com.prop.wo.modules.export.entity.ExportLog;
import com.prop.wo.modules.export.service.ExportService;
import com.prop.wo.modules.system.service.OperLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 导出中心
 */
@Tag(name = "导出中心")
@RestController
@RequestMapping("/api/admin/export")
@RequireRole("ADMIN")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;
    private final OperLogService operLogService;

    @Operation(summary = "导出监管报送 Excel（电梯维保记录）")
    @GetMapping("/regulatory")
    public ResponseEntity<byte[]> regulatory() {
        byte[] bytes = exportService.regulatoryExcel();
        return file(bytes, "电梯维保记录-监管报送.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @Operation(summary = "导出责任记录包（zip）")
    @PostMapping("/responsibility")
    public Result<Map<String, Object>> responsibility(@RequestParam Long equipmentId) {
        Map<String, Object> result = exportService.responsibilityPackage(equipmentId);
        operLogService.record("导出中心", "导出责任记录包", equipmentId, result.get("fileName").toString());
        return Result.ok(result);
    }

    @Operation(summary = "导出记录")
    @GetMapping("/logs")
    public Result<PageResult<ExportLog>> logs(PageQuery query) {
        return Result.ok(exportService.logs(query));
    }

    private ResponseEntity<byte[]> file(byte[] bytes, String fileName, String contentType) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(contentType))
                .body(bytes);
    }
}
