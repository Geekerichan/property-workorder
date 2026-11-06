package com.prop.wo.modules.equipment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("equipment_ext")
public class EquipmentExt extends BaseEntity {

    private Long equipmentId;
    private String extJson;
}
