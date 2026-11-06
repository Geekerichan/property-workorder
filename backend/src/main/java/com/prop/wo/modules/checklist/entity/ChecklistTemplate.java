package com.prop.wo.modules.checklist.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
/**
 * 作业清单模板：checklist_template 表，按设施类型定义检查项。
 */
@TableName("checklist_template")
public class ChecklistTemplate extends BaseEntity {

    private String name;
    private String facilityType;
    private String status;
    private String remark;
}
