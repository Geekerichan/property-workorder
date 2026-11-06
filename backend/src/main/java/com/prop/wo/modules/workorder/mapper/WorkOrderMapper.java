package com.prop.wo.modules.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.prop.wo.modules.workorder.entity.WorkOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface WorkOrderMapper extends BaseMapper<WorkOrder> {

    /** 抢单乐观更新：只有待派单状态才能被抢占，受影响行数为 0 表示已被抢 */
    @Update("UPDATE work_order SET status = 'DISPATCHED', main_worker_id = #{personnelId}, team_id = #{teamId}, "
            + "update_time = NOW() WHERE id = #{id} AND status = 'PENDING_DISPATCH' AND is_deleted = 0")
    int claimUpdate(@Param("id") Long id, @Param("personnelId") Long personnelId, @Param("teamId") Long teamId);

    /** 状态机条件更新：from 状态不匹配则失败，防止并发跳变 */
    @Update("UPDATE work_order SET status = #{to}, update_time = NOW() "
            + "WHERE id = #{id} AND status = #{from} AND is_deleted = 0")
    int transitStatus(@Param("id") Long id, @Param("from") String from, @Param("to") String to);

    /** 看板统计：按状态计数 */
    @Select("SELECT status, COUNT(*) AS cnt FROM work_order WHERE is_deleted = 0 GROUP BY status")
    List<Map<String, Object>> countByStatus();

    /** 看板统计：按设施类型计数 */
    @Select("SELECT facility_type AS facilityType, COUNT(*) AS cnt FROM work_order WHERE is_deleted = 0 GROUP BY facility_type")
    List<Map<String, Object>> countByFacility();

    /** 看板统计：按业务类型计数 */
    @Select("SELECT order_type AS orderType, COUNT(*) AS cnt FROM work_order WHERE is_deleted = 0 GROUP BY order_type")
    List<Map<String, Object>> countByType();

    /** 看板趋势：按天计数 */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS day, COUNT(*) AS cnt FROM work_order "
            + "WHERE is_deleted = 0 AND create_time >= #{start} GROUP BY day ORDER BY day")
    List<Map<String, Object>> countByDay(@Param("start") LocalDateTime start);

    /** 超时未接单/未完成 */
    @Select("SELECT COUNT(*) FROM work_order WHERE is_deleted = 0 AND deadline < NOW() "
            + "AND status IN ('PENDING_DISPATCH','DISPATCHED','PROCESSING')")
    long countOverdue();

    /** 已完成工单数（待回访 + 归档） */
    @Select("SELECT COUNT(*) FROM work_order WHERE is_deleted = 0 AND finish_time IS NOT NULL "
            + "AND status IN ('PENDING_REVIEW','ARCHIVED')")
    long countFinished();

    /** 按时完成工单数 */
    @Select("SELECT COUNT(*) FROM work_order WHERE is_deleted = 0 AND finish_time IS NOT NULL "
            + "AND status IN ('PENDING_REVIEW','ARCHIVED') AND finish_time <= deadline")
    long countOnTime();

    /** 巡检来源工单 */
    @Select("SELECT COUNT(*) FROM work_order WHERE is_deleted = 0 AND source = 'INSPECTION'")
    long countInspectionOrders();

    @Select("SELECT COUNT(*) FROM work_order WHERE is_deleted = 0 AND source = 'INSPECTION' AND status = 'ARCHIVED'")
    long countInspectionArchived();

    /** 人员业绩：主责工单统计 */
    @Select("SELECT COUNT(*) AS total, SUM(CASE WHEN status IN ('PENDING_REVIEW','ARCHIVED') THEN 1 ELSE 0 END) AS finished "
            + "FROM work_order WHERE is_deleted = 0 AND main_worker_id = #{personnelId}")
    Map<String, Object> workerStats(@Param("personnelId") Long personnelId);

    /** 公司业绩：合同关联工单统计 */
    @Select("SELECT COUNT(*) AS total, SUM(CASE WHEN w.status IN ('PENDING_REVIEW','ARCHIVED') THEN 1 ELSE 0 END) AS finished "
            + "FROM work_order w JOIN contract c ON c.id = w.contract_id "
            + "WHERE w.is_deleted = 0 AND c.company_id = #{companyId}")
    Map<String, Object> companyStats(@Param("companyId") Long companyId);

    /** 电梯 15 天维保执行率：近 90 天完成单数 / 应完成单数 */
    @Select("SELECT COUNT(*) FROM work_order WHERE is_deleted = 0 AND facility_type = 'ELEVATOR' "
            + "AND status IN ('PENDING_REVIEW','ARCHIVED') AND finish_time >= #{start}")
    long countElevatorFinished(@Param("start") LocalDateTime start);

    /** 待催办：超过时限仍未完成 */
    @Select("SELECT * FROM work_order WHERE is_deleted = 0 AND status IN ('PENDING_DISPATCH','DISPATCHED','PROCESSING') "
            + "AND deadline < NOW() AND (last_urge_time IS NULL OR last_urge_time < #{before}) LIMIT 100")
    List<WorkOrder> findNeedUrge(@Param("before") LocalDateTime before);
}
