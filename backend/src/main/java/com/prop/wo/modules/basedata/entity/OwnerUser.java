package com.prop.wo.modules.basedata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("owner_user")
public class OwnerUser extends BaseEntity {

    private String phone;
    private String name;
    private String remark;
}
