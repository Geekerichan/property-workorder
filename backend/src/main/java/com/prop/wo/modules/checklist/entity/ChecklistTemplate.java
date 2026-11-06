package com.prop.wo.modules.checklist.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("checklist_template")
public class ChecklistTemplate extends BaseEntity {

    private String name;
    private String facilityType;
    private String status;
    private String remark;
}
