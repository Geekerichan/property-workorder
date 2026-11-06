package com.prop.wo.modules.inspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inspection_record")
public class InspectionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long taskPointId;
    private Long itemId;
    private String itemName;
    private String result;
    private String itemValue;
    private String photoPath;
    private String remark;
    private LocalDateTime createTime;

    @TableLogic
    private Integer isDeleted;
}
