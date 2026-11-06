package com.prop.wo.modules.basedata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
/**
 * 班组：team 表，按设施类型或区域划分。
 */
@TableName("team")
public class Team extends BaseEntity {

    private String name;
    private String teamType;
    private Long communityId;
    private Long leaderId;
    private String remark;
}
