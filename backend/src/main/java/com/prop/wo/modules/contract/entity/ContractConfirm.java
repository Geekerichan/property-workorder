package com.prop.wo.modules.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("contract_confirm")
public class ContractConfirm {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;
    private String party;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime confirmTime;
    private String remark;

    @TableLogic
    private Integer isDeleted;
}
