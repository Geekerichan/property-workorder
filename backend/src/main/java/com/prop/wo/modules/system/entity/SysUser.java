package com.prop.wo.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统用户
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String username;
    private String password;
    private String realName;
    private String phone;
    private String roleCode;
    private Long companyId;
    private Long personnelId;
    private String status;
    private String remark;
}
