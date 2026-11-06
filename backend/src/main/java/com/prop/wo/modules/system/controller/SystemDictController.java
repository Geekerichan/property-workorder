package com.prop.wo.modules.system.controller;

import com.prop.wo.common.annotation.RequireRole;
import com.prop.wo.common.result.Result;
import com.prop.wo.modules.system.dto.SystemDtos;
import com.prop.wo.modules.system.entity.SysConfig;
import com.prop.wo.modules.system.entity.SysDict;
import com.prop.wo.modules.system.entity.SysDictItem;
import com.prop.wo.modules.system.service.DictService;
import com.prop.wo.modules.system.service.OperLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典与参数配置（字典类型、字典项、系统参数）
 */
@Tag(name = "系统-字典与参数")
@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
public class SystemDictController {

    private final DictService dictService;
    private final OperLogService operLogService;

    @Operation(summary = "字典类型列表")
    @GetMapping("/dict/list")
    public Result<List<SysDict>> dictList() {
        return Result.ok(dictService.listDict());
    }

    @Operation(summary = "字典项列表")
    @GetMapping("/dict/items")
    public Result<List<SysDictItem>> items(@RequestParam String dictType) {
        return Result.ok(dictService.items(dictType));
    }

    @Operation(summary = "全部字典项（前端初始化下拉）")
    @GetMapping("/dict/all-items")
    public Result<Map<String, List<SysDictItem>>> allItems() {
        Map<String, List<SysDictItem>> grouped = new LinkedHashMap<>();
        dictService.allItems().forEach(item ->
                grouped.computeIfAbsent(item.getDictType(), k -> new java.util.ArrayList<>()).add(item));
        return Result.ok(grouped);
    }

    @Operation(summary = "保存字典")
    @RequireRole("ADMIN")
    @PostMapping("/dict/save")
    public Result<SysDict> saveDict(@Valid @RequestBody SystemDtos.DictSaveReq req) {
        SysDict dict = dictService.saveDict(req);
        operLogService.record("系统管理", "保存字典", dict.getId(), dict.getDictName());
        return Result.ok(dict);
    }

    @Operation(summary = "保存字典项")
    @RequireRole("ADMIN")
    @PostMapping("/dict/item/save")
    public Result<SysDictItem> saveItem(@Valid @RequestBody SystemDtos.DictItemSaveReq req) {
        SysDictItem item = dictService.saveItem(req);
        operLogService.record("系统管理", "保存字典项", item.getId(), item.getItemLabel());
        return Result.ok(item);
    }

    @Operation(summary = "删除字典项")
    @RequireRole("ADMIN")
    @DeleteMapping("/dict/item/{id}")
    public Result<Void> deleteItem(@PathVariable Long id) {
        dictService.deleteItem(id);
        operLogService.record("系统管理", "删除字典项", id, "删除字典项");
        return Result.ok();
    }

    @Operation(summary = "参数列表")
    @GetMapping("/config/list")
    public Result<List<SysConfig>> configList() {
        return Result.ok(dictService.listConfig());
    }

    @Operation(summary = "保存参数")
    @RequireRole("ADMIN")
    @PostMapping("/config/save")
    public Result<SysConfig> saveConfig(@Valid @RequestBody SystemDtos.ConfigSaveReq req) {
        SysConfig config = dictService.saveConfig(req);
        operLogService.record("系统管理", "保存参数", config.getId(), config.getConfigName());
        return Result.ok(config);
    }
}
