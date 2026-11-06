package com.prop.wo.modules.workorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单流转日志（每次状态变更必须写）
 */
@Data
@TableName("work_order_log")
public class WorkOrderLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String action;
    private String fromStatus;
    private String toStatus;
    private String reason;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime createTime;

    @TableLogic
    private Integer isDeleted;
}
