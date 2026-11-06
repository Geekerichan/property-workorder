package com.prop.wo.modules.inspection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inspection_route")
public class InspectionRoute extends BaseEntity {

    private String name;
    private Long communityId;
    private String facilityType;
    private String frequency;
    private Long leaderId;
    private String status;
    private String remark;
}
