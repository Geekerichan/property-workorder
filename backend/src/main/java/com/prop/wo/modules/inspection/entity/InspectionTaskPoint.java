package com.prop.wo.modules.inspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inspection_task_point")
public class InspectionTaskPoint {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long pointId;
    private String pointName;
    private Long equipmentId;
    private String status;
    private String result;
    private LocalDateTime checkinTime;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String remark;
    private Integer sortNo;

    @TableLogic
    private Integer isDeleted;
}
