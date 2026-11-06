package com.prop.wo.common.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 工单状态机：单向流转，例外迁移必须携带原因并写日志
 */
public enum WorkOrderStatus {

    /** 待派单 */
    PENDING_DISPATCH("待派单"),
    /** 已派单 */
    DISPATCHED("已派单"),
    /** 处理中 */
    PROCESSING("处理中"),
    /** 待回访 */
    PENDING_REVIEW("待回访"),
    /** 归档 */
    ARCHIVED("归档"),
    /** 已作废 */
    CANCELED("已作废");

    private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> TRANSITIONS = Map.of(
            PENDING_DISPATCH, EnumSet.of(DISPATCHED, CANCELED),
            DISPATCHED, EnumSet.of(PROCESSING, PENDING_DISPATCH, CANCELED),
            PROCESSING, EnumSet.of(PENDING_REVIEW, PENDING_DISPATCH, CANCELED),
            PENDING_REVIEW, EnumSet.of(ARCHIVED, PROCESSING),
            ARCHIVED, EnumSet.noneOf(WorkOrderStatus.class),
            CANCELED, EnumSet.noneOf(WorkOrderStatus.class)
    );

    private final String label;

    WorkOrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean canTransitTo(WorkOrderStatus target) {
        return TRANSITIONS.getOrDefault(this, EnumSet.noneOf(WorkOrderStatus.class)).contains(target);
    }

    public static WorkOrderStatus of(String value) {
        for (WorkOrderStatus status : values()) {
            if (status.name().equals(value)) {
                return status;
            }
        }
        return null;
    }
}
