package com.prop.wo.modules.plan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 计划生成日志：(plan_id, period_start) 唯一约束保证幂等
 */
@Data
@TableName("plan_generate_log")
public class PlanGenerateLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private LocalDate periodStart;
    private Long orderId;
    private Integer orderCount;
    private String triggerType;
    private LocalDateTime createTime;

    @TableLogic
    private Integer isDeleted;
}
