package com.prop.wo.modules.equipment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
/**
 * 设备二维码：equipment_qr 表，存储二维码内容与签名。
 */
@TableName("equipment_qr")
public class EquipmentQr extends BaseEntity {

    private Long equipmentId;
    private String qrCode;
    private String sign;
    private String content;
    private Integer printCount;
    private LocalDateTime lastPrintTime;
}
