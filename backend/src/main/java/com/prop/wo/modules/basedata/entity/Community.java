package com.prop.wo.modules.basedata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
/**
 * 小区：community 表，楼盘档案顶层。
 */
@TableName("community")
public class Community extends BaseEntity {

    private String name;
    private String address;
    private String contactName;
    private String contactPhone;
    private String buildYear;
    private String remark;
}
