package com.prop.wo.common.context;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录用户快照（存于 Sa-Token Session）
 */
@Data
public class LoginUser implements Serializable {

    private Long id;
    private String username;
    private String realName;
    private String role;
    private String phone;
    private Long companyId;
    private Long personnelId;

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
