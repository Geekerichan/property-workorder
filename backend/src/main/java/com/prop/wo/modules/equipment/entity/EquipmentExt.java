package com.prop.wo.modules.equipment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
/**
 * 设备扩展参数：equipment_ext 表，按设施类型存储差异化 JSON。
 */
@TableName("equipment_ext")
public class EquipmentExt extends BaseEntity {

    private Long equipmentId;
    private String extJson;
}
