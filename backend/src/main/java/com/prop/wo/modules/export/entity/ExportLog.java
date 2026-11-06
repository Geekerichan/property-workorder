package com.prop.wo.modules.export.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 导出留痕：谁、何时、导出范围
 */
@Data
@TableName("export_log")
public class ExportLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String exportType;
    private String params;
    private String filePath;
    private Integer recordCount;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime createTime;

    @TableLogic
    private Integer isDeleted;
}
