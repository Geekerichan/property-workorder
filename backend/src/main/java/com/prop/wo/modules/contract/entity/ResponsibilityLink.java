package com.prop.wo.modules.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 同一设备相邻两次维保合同的责任链
 */
@Data
@TableName("responsibility_link")
public class ResponsibilityLink {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long equipmentId;
    private Long prevContractId;
    private Long nextContractId;
    private LocalDateTime createTime;

    @TableLogic
    private Integer isDeleted;
}
