package com.prop.wo.modules.contract.controller;

import com.prop.wo.common.annotation.RequireRole;
import com.prop.wo.common.context.LoginUserHolder;
import com.prop.wo.common.entity.PageQuery;
import com.prop.wo.common.result.PageResult;
import com.prop.wo.common.result.Result;
import com.prop.wo.modules.contract.entity.Contract;
import com.prop.wo.modules.contract.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 电子合同与责任链
 */
@Tag(name = "电子合同")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @Operation(summary = "合同分页")
    @GetMapping("/admin/contract/page")
    @RequireRole({"ADMIN", "COMPANY"})
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String status,
                                                        PageQuery query) {
        Long companyId = "COMPANY".equals(LoginUserHolder.role()) ? LoginUserHolder.companyId() : null;
        return Result.ok(contractService.page(keyword, status, companyId, query));
    }

    @Operation(summary = "合同详情")
    @GetMapping({"/admin/contract/{id}", "/app/contract/{id}"})
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(contractService.detail(id));
    }

    @Operation(summary = "按工单查询合同")
    @GetMapping("/app/contract/by-order")
    public Result<Contract> byOrder(@RequestParam Long orderId) {
        return Result.ok(contractService.byOrder(orderId));
    }

    @Operation(summary = "责任链视图")
    @GetMapping("/admin/contract/chain")
    @RequireRole("ADMIN")
    public Result<List<Map<String, Object>>> chain(@RequestParam Long equipmentId) {
        return Result.ok(contractService.responsibilityChain(equipmentId));
    }

    @Operation(summary = "合同确认（A 物业方 / B 接单方）")
    @PostMapping({"/admin/contract/{id}/confirm", "/app/contract/{id}/confirm"})
    public Result<Contract> confirm(@PathVariable Long id, @RequestParam String party,
                                    @RequestParam(required = false) String remark) {
        return Result.ok(contractService.confirm(id, party, remark));
    }

    @Operation(summary = "生成合同 PDF")
    @PostMapping("/admin/contract/{id}/pdf")
    @RequireRole("ADMIN")
    public Result<Contract> generatePdf(@PathVariable Long id) {
        return Result.ok(contractService.generatePdf(id));
    }

    @Operation(summary = "下载合同 PDF")
    @GetMapping("/admin/contract/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        byte[] bytes = contractService.pdfBytes(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''contract-" + id + ".pdf")
                .body(bytes);
    }
}
