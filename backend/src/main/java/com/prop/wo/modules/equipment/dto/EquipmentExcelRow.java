package com.prop.wo.modules.equipment.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 设备台账导出 / 迎检台账
 */
@Data
@ColumnWidth(18)
public class EquipmentExcelRow {

    @ExcelProperty("设备编号")
    private String code;

    @ExcelProperty("设备名称")
    private String name;

    @ExcelProperty("设施类型")
    private String facilityType;

    @ExcelProperty("所属小区")
    private String communityName;

    @ExcelProperty("安装位置")
    private String location;

    @ExcelProperty("投用日期")
    private String commissionDate;

    @ExcelProperty("运行状态")
    private String status;

    @ExcelProperty("维保单位")
    private String companyName;

    @ExcelProperty("安全管理员")
    private String safetyManager;

    @ExcelProperty("安全管理员电话")
    private String safetyPhone;

    @ExcelProperty("下次年检日期")
    private String nextAnnualDate;

    @ExcelProperty("备注")
    private String remark;
}
