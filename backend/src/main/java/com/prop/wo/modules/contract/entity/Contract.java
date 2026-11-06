package com.prop.wo.modules.contract.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 单次电子合同：接单自动生成，双方平台内确认生效
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contract")
public class Contract extends BaseEntity {

    private String contractNo;
    private Long orderId;
    private Long equipmentId;
    private String equipmentName;
    private String partyA;
    private Long companyId;
    private String partyB;
    private String serviceContent;
    private Integer responsibilityDays;
    private LocalDateTime respStart;
    private LocalDateTime respEnd;
    private String status;
    private LocalDateTime signTimeA;
    private String signUserA;
    private LocalDateTime signTimeB;
    private String signUserB;
    private String pdfPath;
    private Integer remindFlag;
}
