package com.prop.wo.modules.plan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 维保计划：设备范围 + 周期/间隔 + 模式 + 清单模板
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("maintenance_plan")
public class MaintenancePlan extends BaseEntity {

    private String name;
    private String facilityType;
    private String mode;
    private Integer periodDays;
    private Integer intervalDays;
    private Integer advanceDays;
    private String orderType;
    private String dispatchMode;
    private Long templateId;
    private Integer seasonalFlag;
    private LocalDate nextGenerateDate;
    private LocalDate lastGenerateDate;
    private String status;
    private String remark;
}
