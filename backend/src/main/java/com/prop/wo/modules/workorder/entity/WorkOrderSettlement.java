package com.prop.wo.modules.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
/**
 * 工单结算：work_order_settlement 表，报酬与结算记录。
 */
@TableName("work_order_settlement")
public class WorkOrderSettlement extends BaseEntity {

    private Long orderId;
    private Long companyId;
    private Long personnelId;
    private BigDecimal amount;
    private String settleStatus;
    private LocalDateTime settleTime;
    private String remark;
}
