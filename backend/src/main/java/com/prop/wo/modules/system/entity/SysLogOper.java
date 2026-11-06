package com.prop.wo.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志
 */
@Data
@TableName("sys_log_oper")
public class SysLogOper {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String module;
    private String action;
    private Long bizId;
    private String content;
    private String beforeData;
    private String afterData;
    private String ip;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime operTime;

    @TableLogic
    private Integer isDeleted;
}
