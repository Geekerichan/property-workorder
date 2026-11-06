package com.prop.wo.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一分页响应
 */
@Data
public class PageResult<T> {

    private long total;
    private List<T> list;

    public PageResult() {
        this.list = new ArrayList<>();
    }

    public PageResult(long total, List<T> list) {
        this.total = total;
        this.list = list == null ? new ArrayList<>() : list;
    }

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public static <T> PageResult<T> of(long total, List<T> list) {
        return new PageResult<>(total, list);
    }
}
