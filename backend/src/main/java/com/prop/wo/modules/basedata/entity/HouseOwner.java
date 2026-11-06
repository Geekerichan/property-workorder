package com.prop.wo.modules.basedata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("house_owner")
public class HouseOwner extends BaseEntity {

    private Long houseId;
    private Long ownerId;
    private String relation;
    private Integer isPrimary;
    private String remark;
}
