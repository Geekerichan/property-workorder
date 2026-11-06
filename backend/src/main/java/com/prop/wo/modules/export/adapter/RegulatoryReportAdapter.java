package com.prop.wo.modules.export.adapter;

import com.prop.wo.modules.export.dto.RegulatoryExcelRow;

import java.util.List;

/**
 * 监管平台实时报送适配点（二期）。
 * <p>当前监管数据按字段导出 Excel 人工报送；二期对接监管平台时实现本接口，
 * 在 {@code ExportService} 导出后调用 {@link #report} 推送上报。</p>
 */
public interface RegulatoryReportAdapter {

    /**
     * 将电梯维保记录推送至监管平台。
     *
     * @param rows 监管报送字段（与导出 Excel 同构）
     */
    void report(List<RegulatoryExcelRow> rows);
}
