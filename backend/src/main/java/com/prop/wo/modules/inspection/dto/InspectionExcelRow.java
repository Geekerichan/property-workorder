package com.prop.wo.modules.inspection.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 巡检记录导出
 */
@Data
@ColumnWidth(18)
public class InspectionExcelRow {

    @ExcelProperty("任务编号")
    private String taskNo;

    @ExcelProperty("计划日期")
    private String planDate;

    @ExcelProperty("路线")
    private String routeName;

    @ExcelProperty("点位")
    private String pointName;

    @ExcelProperty("检查项")
    private String itemName;

    @ExcelProperty("结果")
    private String result;

    @ExcelProperty("读数")
    private String itemValue;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("打卡时间")
    private String checkinTime;

    @ExcelProperty("巡检人")
    private String assigneeName;
}
