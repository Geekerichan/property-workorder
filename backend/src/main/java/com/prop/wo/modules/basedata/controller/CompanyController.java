package com.prop.wo.modules.basedata.controller;

import com.prop.wo.common.annotation.RequireRole;
import com.prop.wo.common.context.LoginUserHolder;
import com.prop.wo.common.result.Result;
import com.prop.wo.modules.basedata.entity.Company;
import com.prop.wo.modules.basedata.entity.CompanyQualification;
import com.prop.wo.modules.basedata.service.CompanyService;
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
 * 维保公司与其资质
 */
@Tag(name = "基础档案-维保公司")
@RestController
@RequestMapping("/api/admin/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final OperLogService operLogService;

    @Operation(summary = "公司列表（维保公司账号仅可见本公司）")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) String keyword) {
        Long onlyId = "COMPANY".equals(LoginUserHolder.role()) ? LoginUserHolder.companyId() : null;
        return Result.ok(companyService.list(onlyId, keyword));
    }

    @Operation(summary = "保存公司")
    @RequireRole("ADMIN")
    @PostMapping("/save")
    public Result<Company> save(@RequestBody Company company) {
        Company saved = companyService.save(company);
        operLogService.record("基础档案", "保存维保公司", saved.getId(), saved.getName());
        return Result.ok(saved);
    }

    @Operation(summary = "删除公司")
    @RequireRole("ADMIN")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        companyService.delete(id);
        operLogService.record("基础档案", "删除维保公司", id, "删除维保公司");
        return Result.ok();
    }

    @Operation(summary = "公司资质列表")
    @GetMapping("/qualification/list")
    public Result<List<CompanyQualification>> qualifications(@RequestParam Long companyId) {
        if ("COMPANY".equals(LoginUserHolder.role()) && !companyId.equals(LoginUserHolder.companyId())) {
            return Result.ok(List.of());
        }
        return Result.ok(companyService.qualifications(companyId));
    }

    @Operation(summary = "保存公司资质")
    @RequireRole("ADMIN")
    @PostMapping("/qualification/save")
    public Result<CompanyQualification> saveQualification(@RequestBody CompanyQualification qualification) {
        CompanyQualification saved = companyService.saveQualification(qualification);
        operLogService.record("基础档案", "保存公司资质", saved.getId(), saved.getQualType());
        return Result.ok(saved);
    }

    @Operation(summary = "删除公司资质")
    @RequireRole("ADMIN")
    @DeleteMapping("/qualification/{id}")
    public Result<Void> deleteQualification(@PathVariable Long id) {
        companyService.deleteQualification(id);
        operLogService.record("基础档案", "删除公司资质", id, "删除公司资质");
        return Result.ok();
    }

    @Operation(summary = "资质临期清单")
    @GetMapping("/qualification/expiring")
    public Result<List<Map<String, Object>>> expiring() {
        return Result.ok(companyService.expiringQualifications());
    }
}
