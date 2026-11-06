package com.prop.wo.modules.export.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 电梯维保记录监管报送字段
 */
@Data
@ColumnWidth(20)
public class RegulatoryExcelRow {

    @ExcelProperty("设备注册代码")
    private String deviceCode;

    @ExcelProperty("设备名称")
    private String deviceName;

    @ExcelProperty("使用单位")
    private String useUnit;

    @ExcelProperty("维保单位")
    private String maintainUnit;

    @ExcelProperty("维保日期")
    private String maintainDate;

    @ExcelProperty("维保人员")
    private String workerName;

    @ExcelProperty("维保项目结论")
    private String conclusion;

    @ExcelProperty("下次维保日期")
    private String nextMaintainDate;

    @ExcelProperty("维保单位签章位")
    private String signPlace;
}
