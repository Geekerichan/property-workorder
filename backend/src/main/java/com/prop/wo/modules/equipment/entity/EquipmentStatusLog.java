package com.prop.wo.modules.equipment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * 设备状态日志：equipment_status_log 表，记录锁停等状态变更。
 */
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
