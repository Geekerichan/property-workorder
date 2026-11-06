package com.prop.wo.modules.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 验收记录：通过 / 驳回重做
 */
@Data
@TableName("acceptance_record")
public class AcceptanceRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String result;
    private String reason;
    private String checkDetail;
    private Integer deductScore;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime createTime;

    @TableLogic
    private Integer isDeleted;
}
