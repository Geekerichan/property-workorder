package com.prop.wo.modules.basedata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
/**
 * 单元：unit 表，隶属于楼栋。
 */
@TableName("unit")
public class Unit extends BaseEntity {

    private Long communityId;
    private Long buildingId;
    private String name;
    private Integer floors;
    private String remark;
}
