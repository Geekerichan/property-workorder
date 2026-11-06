package com.prop.wo.modules.plan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 季节保养节点：汛前排水、入冬防冻、制冷/制热季前、季度配电除尘等
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("seasonal_node")
public class SeasonalNode extends BaseEntity {

    private String name;
    private String facilityType;
    private Integer monthNo;
    private Integer dayNo;
    private Long templateId;
    private String description;
    private Integer enabled;
}
