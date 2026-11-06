package com.prop.wo.modules.equipment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 设备台账（一物一档）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("equipment")
public class Equipment extends BaseEntity {

    private String code;
    private String name;
    private String facilityType;
    private Long communityId;
    private Long buildingId;
    private String location;
    private LocalDate commissionDate;
    private String status;
    private Integer maintainOverdue;
    private Long companyId;
    private String safetyManager;
    private String safetyPhone;
    private LocalDate nextAnnualDate;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String remark;

    /** 分类型扩展参数 JSON，落 equipment_ext 表 */
    @TableField(exist = false)
    private String extJson;
}
