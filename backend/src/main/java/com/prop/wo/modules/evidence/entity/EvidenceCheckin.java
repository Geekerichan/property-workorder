package com.prop.wo.modules.evidence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 到场打卡（扫码或定位，半径校验）
 */
@Data
@TableName("evidence_checkin")
public class EvidenceCheckin {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long personnelId;
    private String checkinType;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String address;
    private Integer distance;
    private Integer validFlag;
    private LocalDateTime checkinTime;

    @TableLogic
    private Integer isDeleted;
}
