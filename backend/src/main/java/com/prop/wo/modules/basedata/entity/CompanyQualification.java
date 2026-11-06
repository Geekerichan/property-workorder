package com.prop.wo.modules.basedata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
/**
 * 维保公司资质：company_qualification 表，用于资质有效期拦截。
 */
@TableName("company_qualification")
public class CompanyQualification extends BaseEntity {

    private Long companyId;
    private String qualType;
    private String certNo;
    private LocalDate expireDate;
    private String filePath;
    private String remark;
}
