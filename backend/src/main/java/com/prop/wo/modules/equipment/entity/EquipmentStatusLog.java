package com.prop.wo.modules.equipment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("equipment_status_log")
public class EquipmentStatusLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long equipmentId;
    private String fromStatus;
    private String toStatus;
    private String reason;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime createTime;

    @TableLogic
    private Integer isDeleted;
}
