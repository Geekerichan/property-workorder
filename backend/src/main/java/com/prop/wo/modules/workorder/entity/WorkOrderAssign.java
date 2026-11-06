package com.prop.wo.modules.workorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("work_order_assign")
public class WorkOrderAssign {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long personnelId;
    private String assignType;
    private String remark;
    private LocalDateTime assignTime;

    @TableLogic
    private Integer isDeleted;
}
