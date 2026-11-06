package com.prop.wo.modules.inspection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
/**
 * 巡检任务：inspection_task 表，按频次生成的执行任务。
 */
@TableName("inspection_task")
public class InspectionTask extends BaseEntity {

    private String taskNo;
    private Long routeId;
    private LocalDate planDate;
    private Long assigneeId;
    private String status;
    private LocalDateTime finishTime;
    private Integer abnormalCount;
}
