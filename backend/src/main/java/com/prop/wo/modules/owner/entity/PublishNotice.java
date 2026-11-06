package com.prop.wo.modules.owner.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 对外公示：消防类内容永不公开
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("publish_notice")
public class PublishNotice extends BaseEntity {

    private String title;
    private String content;
    private String noticeType;
    private String facilityType;
    private Long orderId;
    private LocalDateTime publishTime;
    private LocalDateTime expireTime;
    private String status;
    private Long publishUserId;
}
