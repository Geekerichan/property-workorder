package com.prop.wo.modules.checklist.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_order_checklist")
public class WorkOrderChecklist extends BaseEntity {

    private Long orderId;
    private Long itemId;
    private String groupName;
    private String itemName;
    private String result;
    private String itemValue;
    private String remark;
    private String photoIds;
    private Integer requirePhoto;
    private Integer sortNo;
}
