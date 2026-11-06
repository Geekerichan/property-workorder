package com.prop.wo.modules.inspection.dto;

import com.prop.wo.modules.inspection.entity.InspectionCheckItem;
import com.prop.wo.modules.inspection.entity.InspectionPoint;
import com.prop.wo.modules.inspection.entity.InspectionRoute;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 巡检入参
 */
public class InspectionDtos {

    @Data
    public static class RouteSaveReq {
        private InspectionRoute route;
        private List<PointReq> points;
    }

    @Data
    public static class PointReq {
        private InspectionPoint point;
        private List<InspectionCheckItem> items;
    }

    @Data
    public static class PointSubmitReq {
        private Long taskPointId;
        private String result;
        private String remark;
        private BigDecimal longitude;
        private BigDecimal latitude;
        private List<ItemReq> items;
    }

    @Data
    public static class ItemReq {
        private Long itemId;
        private String itemName;
        private String result;
        private String value;
        private String photoPath;
        private String remark;
    }

    @Data
    public static class ToOrderReq {
        private Long taskPointId;
        private String title;
        private String description;
        private String urgency;
    }
}
