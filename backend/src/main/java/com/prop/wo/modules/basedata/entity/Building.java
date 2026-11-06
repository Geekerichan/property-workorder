package com.prop.wo.modules.basedata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
/**
 * 楼栋：building 表，隶属于小区。
 */
@TableName("building")
public class Building extends BaseEntity {

    private Long communityId;
    private String name;
    private Integer floors;
    private Integer unitCount;
    private String remark;
}
