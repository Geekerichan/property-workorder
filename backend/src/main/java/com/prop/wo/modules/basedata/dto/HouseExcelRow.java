package com.prop.wo.modules.basedata.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 房屋档案 Excel 行（导入/导出共用）
 */
@Data
@ColumnWidth(16)
public class HouseExcelRow {

    @ExcelProperty("小区")
    private String communityName;

    @ExcelProperty("楼栋")
    private String buildingName;

    @ExcelProperty("单元")
    private String unitName;

    @ExcelProperty("房号")
    private String roomNo;

    @ExcelProperty("楼层")
    private Integer floorNo;

    @ExcelProperty("建筑面积")
    private BigDecimal area;

    @ExcelProperty("房屋类型")
    private String houseType;

    @ExcelProperty("业主姓名")
    private String ownerName;

    @ExcelProperty("业主手机号")
    private String ownerPhone;

    @ExcelProperty("与房屋关系")
    private String relation;

    @ExcelProperty("备注")
    private String remark;
}
