package com.prop.wo.modules.workorder.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 工单批量导出
 */
@Data
@ColumnWidth(18)
public class WorkOrderExcelRow {

    @ExcelProperty("工单号")
    private String orderNo;

    @ExcelProperty("标题")
    private String title;

    @ExcelProperty("业务类型")
    private String orderType;

    @ExcelProperty("设施类型")
    private String facilityType;

    @ExcelProperty("设备")
    private String equipmentName;

    @ExcelProperty("紧急等级")
    private String urgency;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("触发源")
    private String source;

    @ExcelProperty("主责人")
    private String mainWorkerName;

    @ExcelProperty("响应时限")
    private String deadline;

    @ExcelProperty("完成时间")
    private String finishTime;

    @ExcelProperty("满意度")
    private String satisfaction;

    @ExcelProperty("创建时间")
    private String createTime;
}
