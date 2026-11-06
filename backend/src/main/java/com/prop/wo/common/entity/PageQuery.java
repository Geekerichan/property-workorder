package com.prop.wo.common.entity;

import lombok.Data;

/**
 * 统一分页入参
 */
@Data
public class PageQuery {

    private Integer pageNum = 1;
    private Integer pageSize = 10;

    public int safePageNum() {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    public int safePageSize() {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 200);
    }
}
