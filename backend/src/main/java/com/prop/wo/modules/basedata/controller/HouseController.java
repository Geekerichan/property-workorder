package com.prop.wo.modules.basedata.controller;

import com.alibaba.excel.EasyExcel;
import com.prop.wo.common.annotation.RequireRole;
import com.prop.wo.common.entity.PageQuery;
import com.prop.wo.common.exception.BizException;
import com.prop.wo.common.result.PageResult;
import com.prop.wo.common.result.Result;
import com.prop.wo.modules.basedata.dto.HouseExcelRow;
import com.prop.wo.modules.basedata.entity.House;
import com.prop.wo.modules.basedata.entity.OwnerUser;
import com.prop.wo.modules.basedata.service.HouseService;
import com.prop.wo.modules.system.service.OperLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
import java.util.List;
import java.util.Map;

/**
 * 房屋与业主档案
 */
@Tag(name = "基础档案-房屋与业主")
@RestController
@RequestMapping("/api/admin/basedata/house")
@RequireRole("ADMIN")
@RequiredArgsConstructor
public class HouseController {

    private final HouseService houseService;
    private final OperLogService operLogService;

    @Operation(summary = "房屋分页")
    @GetMapping("/page")
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(required = false) Long communityId,
                                                        @RequestParam(required = false) Long buildingId,
                                                        @RequestParam(required = false) Long unitId,
                                                        @RequestParam(required = false) String roomNo,
                                                        @RequestParam(required = false) String ownerKeyword,
                                                        PageQuery query) {
        return Result.ok(houseService.page(communityId, buildingId, unitId, roomNo, ownerKeyword, query));
    }

    @Operation(summary = "房屋业主列表")
    @GetMapping("/owners")
    public Result<List<Map<String, Object>>> owners(@RequestParam Long houseId) {
        return Result.ok(houseService.ownersOf(houseId));
    }

    @Operation(summary = "保存房屋")
    @PostMapping("/save")
    public Result<House> save(@RequestBody House house) {
        House saved = houseService.save(house);
        operLogService.record("基础档案", "保存房屋", saved.getId(), saved.getRoomNo());
        return Result.ok(saved);
    }

    @Operation(summary = "删除房屋")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        houseService.delete(id);
        operLogService.record("基础档案", "删除房屋", id, "删除房屋");
        return Result.ok();
    }

    @Operation(summary = "保存业主档案")
    @PostMapping("/owner/save")
    public Result<OwnerUser> saveOwner(@RequestBody OwnerUser owner) {
        OwnerUser saved = houseService.saveOwner(owner);
        operLogService.record("基础档案", "保存业主", saved.getId(), saved.getName());
        return Result.ok(saved);
    }

    @Operation(summary = "绑定业主")
    @PostMapping("/bind-owner")
    public Result<Void> bindOwner(@RequestBody BindOwnerReq req) {
        houseService.bindOwner(req.houseId(), req.ownerId(), req.relation(), req.primary());
        operLogService.record("基础档案", "绑定业主", req.houseId(), "绑定业主 " + req.ownerId());
        return Result.ok();
    }

    @Operation(summary = "解绑业主")
    @PostMapping("/unbind-owner")
    public Result<Void> unbindOwner(@RequestParam Long relationId) {
        houseService.unbindOwner(relationId);
        operLogService.record("基础档案", "解绑业主", relationId, "解绑业主");
        return Result.ok();
    }

    @Operation(summary = "按房号带出业主信息")
    @GetMapping("/lookup")
    public Result<Map<String, Object>> lookup(@RequestParam String roomNo) {
        return Result.ok(houseService.lookup(roomNo));
    }

    @Operation(summary = "下载导入模板")
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        writeExcel(response, "房屋档案导入模板", List.of());
    }

    @Operation(summary = "导入房屋档案")
    @PostMapping("/import")
    public Result<Map<String, Object>> importExcel(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要导入的 Excel 文件");
        }
        Map<String, Object> result = houseService.importExcel(file);
        operLogService.record("基础档案", "导入房屋档案", null, "导入 " + result.get("success") + " 条");
        return Result.ok(result);
    }

    @Operation(summary = "导出房屋档案")
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        List<HouseExcelRow> rows = houseService.exportRows();
        operLogService.record("导出中心", "导出房屋档案", null, "导出 " + rows.size() + " 条");
        writeExcel(response, "房屋档案", rows);
    }

    private void writeExcel(HttpServletResponse response, String sheetName, List<HouseExcelRow> rows) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode(sheetName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), HouseExcelRow.class).sheet(sheetName).doWrite(rows);
    }

    public record BindOwnerReq(Long houseId, Long ownerId, String relation, Boolean primary) {
    }
}
