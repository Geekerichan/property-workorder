package com.prop.wo.modules.plan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("plan_device_rel")
public class PlanDeviceRel {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private Long equipmentId;
    private LocalDateTime createTime;

    @TableLogic
    private Integer isDeleted;
}
