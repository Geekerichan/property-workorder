package com.prop.wo.modules.checklist.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
/**
 * 清单项：checklist_item 表，模板下的检查项。
 */
@TableName("checklist_item")
public class ChecklistItem extends BaseEntity {

    private Long templateId;
    private String groupName;
    private String itemName;
    private Integer requirePhoto;
    private Integer requireValue;
    private String unit;
    private Integer sortNo;
}
