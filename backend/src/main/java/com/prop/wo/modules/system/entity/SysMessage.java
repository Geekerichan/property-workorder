package com.prop.wo.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 站内信
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message")
public class SysMessage extends BaseEntity {

    private Long userId;
    private String msgType;
    private String title;
    private String content;
    private String bizType;
    private Long bizId;
    private Integer readFlag;
    private LocalDateTime readTime;
}
