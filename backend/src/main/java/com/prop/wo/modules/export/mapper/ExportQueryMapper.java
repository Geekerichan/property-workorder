package com.prop.wo.modules.export.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ExportQueryMapper {

    /** 电梯维保记录（监管报送字段） */
    @Select("SELECT e.code AS deviceCode, e.name AS deviceName, c.party_a AS useUnit, c.party_b AS maintainUnit, "
            + "w.finish_time AS maintainDate, p.name AS workerName, w.accept_result AS acceptResult, "
            + "DATE_ADD(w.finish_time, INTERVAL 15 DAY) AS nextMaintainDate "
            + "FROM work_order w JOIN equipment e ON e.id = w.equipment_id "
            + "LEFT JOIN contract c ON c.id = w.contract_id "
            + "LEFT JOIN personnel p ON p.id = w.main_worker_id "
            + "WHERE w.is_deleted = 0 AND e.facility_type = 'ELEVATOR' "
            + "AND w.status IN ('PENDING_REVIEW','ARCHIVED') AND w.finish_time IS NOT NULL "
            + "ORDER BY w.finish_time DESC")
    List<Map<String, Object>> elevatorMaintenanceRecords();

    @Select("SELECT e.code AS deviceCode, e.name AS deviceName, c.party_a AS useUnit, c.party_b AS maintainUnit, "
            + "w.finish_time AS maintainDate, p.name AS workerName, w.accept_result AS acceptResult, "
            + "DATE_ADD(w.finish_time, INTERVAL 15 DAY) AS nextMaintainDate "
            + "FROM work_order w JOIN equipment e ON e.id = w.equipment_id "
            + "LEFT JOIN contract c ON c.id = w.contract_id "
            + "LEFT JOIN personnel p ON p.id = w.main_worker_id "
            + "WHERE w.is_deleted = 0 AND e.facility_type = 'ELEVATOR' AND w.id = #{orderId}")
    List<Map<String, Object>> elevatorRecordByOrder(@Param("orderId") Long orderId);

    /** 设备近 6 个月履历 */
    @Select("SELECT w.id AS orderId, w.order_no AS orderNo, w.order_type AS orderType, w.title AS title, w.status AS status, "
            + "w.finish_time AS finishTime, w.create_time AS createTime, p.name AS workerName, "
            + "c.contract_no AS contractNo, c.party_b AS partyB, c.resp_start AS respStart, c.resp_end AS respEnd "
            + "FROM work_order w LEFT JOIN personnel p ON p.id = w.main_worker_id "
            + "LEFT JOIN contract c ON c.id = w.contract_id "
            + "WHERE w.is_deleted = 0 AND w.equipment_id = #{equipmentId} AND w.create_time >= #{since} "
            + "ORDER BY w.create_time DESC")
    List<Map<String, Object>> equipmentHistory(@Param("equipmentId") Long equipmentId,
                                               @Param("since") LocalDateTime since);
}
