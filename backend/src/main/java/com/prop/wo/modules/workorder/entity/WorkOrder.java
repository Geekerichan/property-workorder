package com.prop.wo.modules.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运维工单（最小业务单元）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_order")
public class WorkOrder extends BaseEntity {

    private String orderNo;
    private String orderType;
    private String facilityType;
    private Long equipmentId;
    private Long communityId;
    private Long buildingId;
    private Long houseId;
    private String title;
    private String description;
    private String urgency;
    private String status;
    private String source;
    private Long planId;
    private String dispatchMode;
    private LocalDateTime deadline;
    private Long mainWorkerId;
    private Long teamId;
    private Long contractId;
    private LocalDateTime responsibilityStart;
    private LocalDateTime responsibilityEnd;
    private String acceptResult;
    private Integer satisfactionScore;
    private Integer publishFlag;
    private Long reporterUserId;
    private String reporterName;
    private String reporterPhone;
    private BigDecimal rewardAmount;
    private LocalDateTime finishTime;
    private LocalDateTime archiveTime;
    private String cancelReason;
    private Integer urgeCount;
    private LocalDateTime lastUrgeTime;
    private String remark;
}
