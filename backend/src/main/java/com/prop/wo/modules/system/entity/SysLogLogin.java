package com.prop.wo.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志（允许物理清理，逻辑删除字段保留以便统一查询）
 */
@Data
@TableName("sys_log_login")
public class SysLogLogin {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String ip;
    private String userAgent;
    private Integer success;
    private String message;
    private LocalDateTime loginTime;

    @TableLogic
    private Integer isDeleted;
}
