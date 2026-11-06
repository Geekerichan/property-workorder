package com.prop.wo.modules.basedata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("house")
public class House extends BaseEntity {

    private Long communityId;
    private Long buildingId;
    private Long unitId;
    private String roomNo;
    private Integer floorNo;
    private BigDecimal area;
    private String houseType;
    private String status;
    private String remark;
}
