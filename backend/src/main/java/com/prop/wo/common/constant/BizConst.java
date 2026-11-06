package com.prop.wo.common.constant;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务常量与中文标签
 */
public final class BizConst {

    private BizConst() {
    }

    // 设施类型（消防仅做合规隔离）
    public static final String FACILITY_ELEVATOR = "ELEVATOR";
    public static final String FACILITY_WATER = "WATER";
    public static final String FACILITY_POWER = "POWER";
    public static final String FACILITY_HVAC = "HVAC";
    public static final String FACILITY_SECURITY = "SECURITY";
    public static final String FACILITY_GARDEN = "GARDEN";
    public static final String FACILITY_FIRE = "FIRE";

    // 工单业务类型
    public static final String ORDER_PLAN = "PLAN_MAINTAIN";
    public static final String ORDER_DEMAND = "DEMAND_MAINTAIN";
    public static final String ORDER_FAULT = "FAULT_REPAIR";
    public static final String ORDER_EMERGENCY = "EMERGENCY";
    public static final String ORDER_INSPECTION = "INSPECTION";

    // 触发源
    public static final String SOURCE_PLAN = "PLAN";
    public static final String SOURCE_OWNER = "OWNER";
    public static final String SOURCE_INSPECTION = "INSPECTION";
    public static final String SOURCE_MANUAL = "MANUAL";

    // 紧急等级
    public static final String URGENCY_LOW = "LOW";
    public static final String URGENCY_NORMAL = "NORMAL";
    public static final String URGENCY_HIGH = "HIGH";
    public static final String URGENCY_URGENT = "URGENT";

    // 派单方式
    public static final String DISPATCH_ASSIGN = "ASSIGN";
    public static final String DISPATCH_GRAB = "GRAB";

    // 人员在岗状态
    public static final String DUTY_ON = "ON_DUTY";
    public static final String DUTY_LEAVE = "LEAVE";
    public static final String DUTY_RESIGNED = "RESIGNED";

    // 设备状态
    public static final String EQUIP_NORMAL = "NORMAL";
    public static final String EQUIP_REPAIR = "REPAIR";
    public static final String EQUIP_LOCKED = "LOCKED";

    // 照片点位
    public static final String PHOTO_BEFORE = "BEFORE";
    public static final String PHOTO_AFTER = "AFTER";
    public static final String PHOTO_KEY = "KEY";
    public static final String PHOTO_OTHER = "OTHER";

    // 巡检频次
    public static final String FREQ_DAILY = "DAILY";
    public static final String FREQ_WEEKLY = "WEEKLY";
    public static final String FREQ_MONTHLY = "MONTHLY";

    // 合同状态
    public static final String CONTRACT_PENDING = "PENDING";
    public static final String CONTRACT_ACTIVE = "ACTIVE";
    public static final String CONTRACT_EXPIRED = "EXPIRED";

    private static final Map<String, String> FACILITY_LABELS = new LinkedHashMap<>();
    private static final Map<String, String> ORDER_TYPE_LABELS = new LinkedHashMap<>();
    private static final Map<String, String> URGENCY_LABELS = new LinkedHashMap<>();
    private static final Map<String, String> EQUIP_STATUS_LABELS = new LinkedHashMap<>();
    private static final Map<String, String> SOURCE_LABELS = new LinkedHashMap<>();

    static {
        FACILITY_LABELS.put(FACILITY_ELEVATOR, "电梯");
        FACILITY_LABELS.put(FACILITY_WATER, "给排水");
        FACILITY_LABELS.put(FACILITY_POWER, "供配电");
        FACILITY_LABELS.put(FACILITY_HVAC, "暖通空调");
        FACILITY_LABELS.put(FACILITY_SECURITY, "安防监控");
        FACILITY_LABELS.put(FACILITY_GARDEN, "园林绿化");
        FACILITY_LABELS.put(FACILITY_FIRE, "消防");

        ORDER_TYPE_LABELS.put(ORDER_PLAN, "计划性维保");
        ORDER_TYPE_LABELS.put(ORDER_DEMAND, "按需维保");
        ORDER_TYPE_LABELS.put(ORDER_FAULT, "故障报修");
        ORDER_TYPE_LABELS.put(ORDER_EMERGENCY, "应急处置");
        ORDER_TYPE_LABELS.put(ORDER_INSPECTION, "日常巡检");

        URGENCY_LABELS.put(URGENCY_LOW, "低");
        URGENCY_LABELS.put(URGENCY_NORMAL, "一般");
        URGENCY_LABELS.put(URGENCY_HIGH, "紧急");
        URGENCY_LABELS.put(URGENCY_URGENT, "特急");

        EQUIP_STATUS_LABELS.put(EQUIP_NORMAL, "正常运行");
        EQUIP_STATUS_LABELS.put(EQUIP_REPAIR, "停用检修");
        EQUIP_STATUS_LABELS.put("OVERDUE", "维保逾期");
        EQUIP_STATUS_LABELS.put(EQUIP_LOCKED, "禁止使用");

        SOURCE_LABELS.put(SOURCE_PLAN, "计划引擎");
        SOURCE_LABELS.put(SOURCE_OWNER, "业主上报");
        SOURCE_LABELS.put(SOURCE_INSPECTION, "巡检异常");
        SOURCE_LABELS.put(SOURCE_MANUAL, "手动创建");
    }

    public static String facilityLabel(String value) {
        return FACILITY_LABELS.getOrDefault(value, value);
    }

    public static String orderTypeLabel(String value) {
        return ORDER_TYPE_LABELS.getOrDefault(value, value);
    }

    public static String urgencyLabel(String value) {
        return URGENCY_LABELS.getOrDefault(value, value);
    }

    public static String equipStatusLabel(String value) {
        return EQUIP_STATUS_LABELS.getOrDefault(value, value);
    }

    public static String sourceLabel(String value) {
        return SOURCE_LABELS.getOrDefault(value, value);
    }

    public static Map<String, String> facilityLabels() {
        return FACILITY_LABELS;
    }

    /** 紧急等级排序权重，数字越大越紧急 */
    public static int urgencyWeight(String urgency) {
        return switch (urgency == null ? "" : urgency) {
            case URGENCY_URGENT -> 4;
            case URGENCY_HIGH -> 3;
            case URGENCY_NORMAL -> 2;
            case URGENCY_LOW -> 1;
            default -> 0;
        };
    }
}
