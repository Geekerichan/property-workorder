package com.prop.wo.modules.equipment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("annual_check")
public class AnnualCheck extends BaseEntity {

    private Long equipmentId;
    private LocalDate checkDate;
    private LocalDate expireDate;
    private String result;
    private String agency;
    private String reportNo;
    private String filePath;
    private String remark;
}
