package com.prop.wo.modules.inspection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.prop.wo.modules.inspection.entity.InspectionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 巡检记录 Mapper：含巡检统计查询。
 */
@Mapper
public interface InspectionRecordMapper extends BaseMapper<InspectionRecord> {

    @Select("SELECT t.plan_date AS planDate, COUNT(DISTINCT t.id) AS taskCount, "
            + "SUM(CASE WHEN t.status = 'DONE' THEN 1 ELSE 0 END) AS doneCount, "
            + "SUM(t.abnormal_count) AS abnormalCount FROM inspection_task t "
            + "WHERE t.is_deleted = 0 AND t.plan_date >= #{start} GROUP BY t.plan_date ORDER BY t.plan_date")
    List<Map<String, Object>> dailyStats(java.time.LocalDate start);
}
