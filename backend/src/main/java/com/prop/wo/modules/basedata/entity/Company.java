package com.prop.wo.modules.basedata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
/**
 * 维保公司：company 表，派单与数据隔离的主体。
 */
@TableName("company")
public class Company extends BaseEntity {

    private String name;
    private String creditCode;
    private String contactName;
    private String contactPhone;
    private String address;
    private String status;
    private String remark;
}
