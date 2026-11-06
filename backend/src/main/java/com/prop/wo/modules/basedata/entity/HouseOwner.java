package com.prop.wo.modules.basedata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
/**
 * 房屋-业主关联：house_owner 表，支持一房多业主。
 */
@TableName("house_owner")
public class HouseOwner extends BaseEntity {

    private Long houseId;
    private Long ownerId;
    private String relation;
    private Integer isPrimary;
    private String remark;
}
