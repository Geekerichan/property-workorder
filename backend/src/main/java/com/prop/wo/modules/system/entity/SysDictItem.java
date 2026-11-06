package com.prop.wo.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.prop.wo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典项
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_item")
public class SysDictItem extends BaseEntity {

    private String dictType;
    private String itemLabel;
    private String itemValue;
    private Integer sortNo;
    private String status;
    private String remark;
}
