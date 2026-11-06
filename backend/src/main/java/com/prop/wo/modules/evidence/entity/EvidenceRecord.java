package com.prop.wo.modules.evidence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * 现场记录：evidence_record 表，记录作业过程文字留痕。
 */
@TableName("evidence_record")
public class EvidenceRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long personnelId;
    private String recordType;
    private String content;
    private Long createBy;
    private LocalDateTime createTime;

    @TableLogic
    private Integer isDeleted;
}
