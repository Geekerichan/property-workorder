package com.prop.wo.modules.evidence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 留痕照片：服务端水印 + MD5 校验码
 */
@Data
@TableName("evidence_photo")
public class EvidencePhoto {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long checkinId;
    private Long taskId;
    private String photoType;
    private String originalPath;
    private String watermarkPath;
    private String md5;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private LocalDateTime shootTime;
    private Long operatorId;
    private String operatorName;
    private String equipmentName;
    private Long itemId;
    private String remark;
    private LocalDateTime createTime;

    @TableLogic
    private Integer isDeleted;
}
