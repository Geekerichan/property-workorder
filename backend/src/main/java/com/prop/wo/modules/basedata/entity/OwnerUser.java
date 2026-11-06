package com.prop.wo.modules.basedata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
/**
 * 业主：owner_user 表，业主端登录主体（手机号 + 验证码）。
 */
@TableName("owner_user")
public class OwnerUser extends BaseEntity {

    private String phone;
    private String name;
    private String remark;
}
