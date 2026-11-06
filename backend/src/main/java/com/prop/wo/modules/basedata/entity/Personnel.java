package com.prop.wo.modules.basedata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
/**
 * 作业人员：personnel 表，工单主责人与巡检执行主体。
 */
@TableName("personnel")
public class Personnel extends BaseEntity {

    private String name;
    private String phone;
    private Long teamId;
    private Long companyId;
    private Long userId;
    private String skills;
    private String certName;
    private String certNo;
    private LocalDate certExpireDate;
    private String dutyStatus;
    private String remark;
}
