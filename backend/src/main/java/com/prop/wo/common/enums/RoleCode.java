package com.prop.wo.common.enums;

/**
 * 简化后的四类角色
 */
public enum RoleCode {

    /** 管理员：物业侧全部职能（派单、验收、稽核、台账、档案、公示、系统配置） */
    ADMIN("管理员"),
    /** 作业人员：维保/维修/巡检执行 */
    WORKER("作业人员"),
    /** 维保公司管理员：本公司人员、合同、业绩（数据范围=本公司） */
    COMPANY("维保公司管理员"),
    /** 业主：扫码查询、报修、进度、评价（数据范围=绑定房屋） */
    OWNER("业主");

    private final String label;

    RoleCode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static boolean isValid(String code) {
        for (RoleCode role : values()) {
            if (role.name().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
