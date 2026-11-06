package com.prop.wo.modules.workorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("work_order_claim")
public class WorkOrderClaim {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long personnelId;
    private Long userId;
    private LocalDateTime claimTime;
    private Integer success;
    private String failReason;

    @TableLogic
    private Integer isDeleted;
}
