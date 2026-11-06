package com.prop.wo.modules.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 信用记录：业主评价与扣分
 */
@Data
@TableName("credit_record")
public class CreditRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String targetType;
    private Long targetId;
    private String targetName;
    private Long orderId;
    private String creditType;
    private Integer score;
    private String reason;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime createTime;

    @TableLogic
    private Integer isDeleted;
}
