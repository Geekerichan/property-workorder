package com.prop.wo.modules.workorder.dto;

import com.prop.wo.common.entity.PageQuery;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单入参
 */
public class OrderDtos {

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class OrderQuery extends PageQuery {
        private String status;
        private String orderType;
        private String facilityType;
        private Long communityId;
        private Long buildingId;
        private Long mainWorkerId;
        private String keyword;
        private String urgency;
        /** 是否只看超时 */
        private Boolean overdueOnly;
    }

    public record OrderCreateReq(
            Long equipmentId,
            @NotBlank(message = "请选择工单业务类型") String orderType,
            String facilityType,
            @NotBlank(message = "请填写工单标题") String title,
            String description,
            String urgency,
            String dispatchMode,
            Long mainWorkerId,
            Long teamId,
            Long houseId,
            LocalDateTime deadline,
            BigDecimal rewardAmount,
            String remark) {
    }

    public record DispatchReq(Long personnelId, Long teamId, String remark, List<Long> assistPersonnelIds) {
    }

    public record TransferReq(Long personnelId, String reason) {
    }

    public record ReasonReq(String reason) {
    }

    public record AcceptReq(String result, String reason, Integer deductScore) {
    }

    public record ReviewReq(String content, Integer score) {
    }

    public record SubmitReq(List<com.prop.wo.modules.checklist.service.ChecklistService.ChecklistResult> results) {
    }

    public record BatchDispatchReq(List<Long> orderIds, Long personnelId, Long teamId) {
    }

    public record PublishReq(Boolean publish, String title, String content, String noticeType) {
    }

    public record SettleReq(Long id) {
    }
}
