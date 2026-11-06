package com.prop.wo.modules.basedata.controller;

import com.prop.wo.common.annotation.RequireRole;
import com.prop.wo.common.entity.PageQuery;
import com.prop.wo.common.result.PageResult;
import com.prop.wo.common.result.Result;
import com.prop.wo.modules.basedata.entity.Personnel;
import com.prop.wo.modules.basedata.entity.Team;
import com.prop.wo.modules.basedata.service.StaffService;
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
 * 班组与人员
 */
@Tag(name = "基础档案-班组与人员")
@RestController
@RequestMapping("/api/admin/basedata")
@RequireRole("ADMIN")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;
    private final OperLogService operLogService;

    @Operation(summary = "班组列表")
    @GetMapping("/team/list")
    public Result<List<Team>> teams() {
        return Result.ok(staffService.teams());
    }

    @Operation(summary = "保存班组")
    @PostMapping("/team/save")
    public Result<Team> saveTeam(@RequestBody Team team) {
        Team saved = staffService.saveTeam(team);
        operLogService.record("基础档案", "保存班组", saved.getId(), saved.getName());
        return Result.ok(saved);
    }

    @Operation(summary = "删除班组")
    @DeleteMapping("/team/{id}")
    public Result<Void> deleteTeam(@PathVariable Long id) {
        staffService.deleteTeam(id);
        operLogService.record("基础档案", "删除班组", id, "删除班组");
        return Result.ok();
    }

    @Operation(summary = "人员分页")
    @GetMapping("/personnel/page")
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) Long teamId,
                                                        @RequestParam(required = false) Long companyId,
                                                        @RequestParam(required = false) String dutyStatus,
                                                        PageQuery query) {
        return Result.ok(staffService.personnelPage(keyword, teamId, companyId, dutyStatus, query));
    }

    @Operation(summary = "可派单人员（在岗+资质有效）")
    @GetMapping("/personnel/available")
    public Result<List<Map<String, Object>>> available(@RequestParam(required = false) Long teamId) {
        return Result.ok(staffService.availablePersonnel(teamId));
    }

    @Operation(summary = "保存人员")
    @PostMapping("/personnel/save")
    public Result<Personnel> save(@RequestBody Personnel personnel) {
        Personnel saved = staffService.savePersonnel(personnel);
        operLogService.record("基础档案", "保存人员", saved.getId(), saved.getName());
        return Result.ok(saved);
    }

    @Operation(summary = "变更在岗状态")
    @PostMapping("/personnel/duty-status")
    public Result<Void> dutyStatus(@RequestParam Long id, @RequestParam String dutyStatus) {
        staffService.changeDutyStatus(id, dutyStatus);
        operLogService.record("基础档案", "变更在岗状态", id, dutyStatus);
        return Result.ok();
    }

    @Operation(summary = "删除人员")
    @DeleteMapping("/personnel/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        staffService.deletePersonnel(id);
        operLogService.record("基础档案", "删除人员", id, "删除人员");
        return Result.ok();
    }

    @Operation(summary = "资质临期人员")
    @GetMapping("/personnel/cert-expiring")
    public Result<List<Map<String, Object>>> certExpiring() {
        return Result.ok(staffService.expiringCertificates());
    }
}
