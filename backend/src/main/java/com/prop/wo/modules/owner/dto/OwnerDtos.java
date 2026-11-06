package com.prop.wo.modules.owner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 业主端入参
 */
public class OwnerDtos {

    public record SmsReq(@NotBlank(message = "请输入手机号")
                         @Pattern(regexp = "^1\\d{10}$", message = "手机号需为 11 位") String phone) {
    }

    public record LoginReq(@NotBlank(message = "请输入手机号") String phone,
                           @NotBlank(message = "请输入验证码") String code,
                           String roomNo) {
    }

    public record ReportReq(Long equipmentId, String facilityType,
                            @NotBlank(message = "请描述问题") String description,
                            String urgency, Long houseId, String title) {
    }

    public record EvaluateReq(Integer score, String content) {
    }

    public record ComplaintReq(@NotBlank(message = "请填写投诉内容") String content,
                               Long equipmentId, Long houseId) {
    }
}
