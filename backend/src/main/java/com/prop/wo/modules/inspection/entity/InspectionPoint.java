package com.prop.wo.modules.inspection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inspection_point")
public class InspectionPoint extends BaseEntity {

    private Long routeId;
    private Long equipmentId;
    private String pointName;
    private Integer sortNo;
    private String remark;
}
