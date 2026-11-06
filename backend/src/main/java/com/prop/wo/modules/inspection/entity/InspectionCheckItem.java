package com.prop.wo.modules.inspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
/**
 * 巡检检查项：inspection_check_item 表，点位下的检查内容。
 */
@TableName("inspection_check_item")
public class InspectionCheckItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long pointId;
    private String itemName;
    private Integer requireValue;
    private String unit;
    private Integer sortNo;

    @TableLogic
    private Integer isDeleted;
}
