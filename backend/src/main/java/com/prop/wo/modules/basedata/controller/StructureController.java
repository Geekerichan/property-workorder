package com.prop.wo.modules.basedata.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.prop.wo.common.annotation.RequireRole;
import com.prop.wo.common.result.Result;
import com.prop.wo.modules.basedata.entity.Building;
import com.prop.wo.modules.basedata.entity.Community;
import com.prop.wo.modules.basedata.entity.Unit;
import com.prop.wo.modules.basedata.mapper.BuildingMapper;
import com.prop.wo.modules.basedata.mapper.CommunityMapper;
import com.prop.wo.modules.basedata.mapper.UnitMapper;
import com.prop.wo.modules.basedata.service.StructureService;
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
 * 楼盘档案：小区/楼栋/单元
 */
@Tag(name = "基础档案-楼盘")
@RestController
@RequestMapping("/api/admin/basedata")
@RequireRole("ADMIN")
@RequiredArgsConstructor
public class StructureController {

    private final StructureService structureService;
    private final CommunityMapper communityMapper;
    private final BuildingMapper buildingMapper;
    private final UnitMapper unitMapper;
    private final OperLogService operLogService;

    @Operation(summary = "楼盘树")
    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> tree() {
        return Result.ok(structureService.tree());
    }

    @Operation(summary = "小区列表")
    @GetMapping("/community/list")
    public Result<List<Community>> communities() {
        return Result.ok(communityMapper.selectList(new LambdaQueryWrapper<Community>().orderByAsc(Community::getId)));
    }

    @Operation(summary = "楼栋列表")
    @GetMapping("/building/list")
    public Result<List<Building>> buildings(@RequestParam(required = false) Long communityId) {
        return Result.ok(buildingMapper.selectList(new LambdaQueryWrapper<Building>()
                .eq(communityId != null, Building::getCommunityId, communityId).orderByAsc(Building::getId)));
    }

    @Operation(summary = "单元列表")
    @GetMapping("/unit/list")
    public Result<List<Unit>> units(@RequestParam(required = false) Long buildingId) {
        return Result.ok(unitMapper.selectList(new LambdaQueryWrapper<Unit>()
                .eq(buildingId != null, Unit::getBuildingId, buildingId).orderByAsc(Unit::getId)));
    }

    @Operation(summary = "保存小区")
    @PostMapping("/community/save")
    public Result<Community> saveCommunity(@RequestBody Community community) {
        Community saved = structureService.saveCommunity(community);
        operLogService.record("基础档案", "保存小区", saved.getId(), saved.getName());
        return Result.ok(saved);
    }

    @Operation(summary = "保存楼栋")
    @PostMapping("/building/save")
    public Result<Building> saveBuilding(@RequestBody Building building) {
        Building saved = structureService.saveBuilding(building);
        operLogService.record("基础档案", "保存楼栋", saved.getId(), saved.getName());
        return Result.ok(saved);
    }

    @Operation(summary = "保存单元")
    @PostMapping("/unit/save")
    public Result<Unit> saveUnit(@RequestBody Unit unit) {
        Unit saved = structureService.saveUnit(unit);
        operLogService.record("基础档案", "保存单元", saved.getId(), saved.getName());
        return Result.ok(saved);
    }

    @Operation(summary = "删除小区")
    @DeleteMapping("/community/{id}")
    public Result<Void> deleteCommunity(@PathVariable Long id) {
        structureService.deleteCommunity(id);
        operLogService.record("基础档案", "删除小区", id, "删除小区");
        return Result.ok();
    }

    @Operation(summary = "删除楼栋")
    @DeleteMapping("/building/{id}")
    public Result<Void> deleteBuilding(@PathVariable Long id) {
        structureService.deleteBuilding(id);
        operLogService.record("基础档案", "删除楼栋", id, "删除楼栋");
        return Result.ok();
    }

    @Operation(summary = "删除单元")
    @DeleteMapping("/unit/{id}")
    public Result<Void> deleteUnit(@PathVariable Long id) {
        structureService.deleteUnit(id);
        operLogService.record("基础档案", "删除单元", id, "删除单元");
        return Result.ok();
    }
}
