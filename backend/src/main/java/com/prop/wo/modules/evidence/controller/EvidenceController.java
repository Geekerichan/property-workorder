package com.prop.wo.modules.evidence.controller;

import com.prop.wo.common.annotation.RequireRole;
import com.prop.wo.common.result.Result;
import com.prop.wo.modules.evidence.entity.EvidenceCheckin;
import com.prop.wo.modules.evidence.entity.EvidencePhoto;
import com.prop.wo.modules.evidence.entity.EvidenceRecord;
import com.prop.wo.modules.evidence.service.EvidenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 现场作业留痕接口
 */
@Tag(name = "现场留痕")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EvidenceController {

    private final EvidenceService evidenceService;

    @Operation(summary = "到场打卡（扫码/定位）")
    @PostMapping("/app/evidence/checkin")
    @RequireRole({"WORKER", "ADMIN"})
    public Result<EvidenceCheckin> checkin(@RequestParam Long orderId,
                                           @RequestParam(defaultValue = "LOCATION") String type,
                                           @RequestParam(required = false) BigDecimal longitude,
                                           @RequestParam(required = false) BigDecimal latitude,
                                           @RequestParam(required = false) String address) {
        return Result.ok(evidenceService.checkin(orderId, type, longitude, latitude, address));
    }

    @Operation(summary = "上传留痕照片（服务端水印 + MD5）")
    @PostMapping({"/app/evidence/photo", "/h5/owner/report/photo"})
    @RequireRole({"WORKER", "ADMIN", "OWNER"})
    public Result<EvidencePhoto> upload(@RequestParam Long orderId,
                                        @RequestPart("file") MultipartFile file,
                                        @RequestParam(defaultValue = "OTHER") String photoType,
                                        @RequestParam(required = false) BigDecimal longitude,
                                        @RequestParam(required = false) BigDecimal latitude,
                                        @RequestParam(required = false) Long itemId) {
        return Result.ok(evidenceService.upload(orderId, file, photoType, longitude, latitude, itemId));
    }

    @Operation(summary = "工单照片列表")
    @GetMapping("/app/evidence/photos")
    public Result<List<EvidencePhoto>> photos(@RequestParam Long orderId) {
        return Result.ok(evidenceService.photos(orderId));
    }

    @Operation(summary = "现场作业记录")
    @GetMapping("/app/evidence/records")
    public Result<List<EvidenceRecord>> records(@RequestParam Long orderId) {
        return Result.ok(evidenceService.records(orderId));
    }

    @Operation(summary = "新增现场记录/隐患上报")
    @PostMapping("/app/evidence/record")
    @RequireRole({"WORKER", "ADMIN"})
    public Result<EvidenceRecord> addRecord(@RequestParam Long orderId,
                                            @RequestParam(defaultValue = "PROGRESS") String type,
                                            @RequestParam String content) {
        return Result.ok(evidenceService.addRecord(orderId, type, content));
    }

    @Operation(summary = "删除照片")
    @DeleteMapping("/app/evidence/photo/{id}")
    public Result<Void> deletePhoto(@PathVariable Long id) {
        evidenceService.deletePhoto(id);
        return Result.ok();
    }

    @Operation(summary = "稽核视图（照片/打卡/记录汇总）")
    @GetMapping("/admin/evidence/audit-view")
    @RequireRole("ADMIN")
    public Result<Map<String, Object>> auditView(@RequestParam Long orderId) {
        return Result.ok(evidenceService.auditView(orderId));
    }
}
