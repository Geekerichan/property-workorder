package com.prop.wo.modules.equipment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.prop.wo.modules.equipment.entity.Equipment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface EquipmentMapper extends BaseMapper<Equipment> {

    /** 最近一次已完成维保时间（用于维保逾期判定） */
    @Select("SELECT equipment_id AS equipmentId, MAX(finish_time) AS lastFinish "
            + "FROM work_order WHERE is_deleted = 0 AND status IN ('PENDING_REVIEW','ARCHIVED') "
            + "AND equipment_id IS NOT NULL GROUP BY equipment_id")
    List<Map<String, Object>> lastMaintainTimes();

    /** 维保逾期：电梯 15 天未完成维保 */
    @Select("SELECT e.id FROM equipment e WHERE e.is_deleted = 0 AND e.facility_type = 'ELEVATOR' "
            + "AND (SELECT MAX(w.finish_time) FROM work_order w WHERE w.equipment_id = e.id AND w.is_deleted = 0 "
            + "AND w.status IN ('PENDING_REVIEW','ARCHIVED')) < #{deadline}")
    List<Long> overdueElevatorIds(LocalDateTime deadline);

    /** 设备关联工单数量（删除前校验） */
    @Select("SELECT COUNT(*) FROM work_order WHERE equipment_id = #{id} AND is_deleted = 0")
    long countOrders(Long id);

    /** 设备近期维保记录（对外公开字段，不含人员隐私） */
    @Select("SELECT w.order_no AS orderNo, w.order_type AS orderType, w.finish_time AS finishTime, "
            + "w.accept_result AS acceptResult, c.party_b AS maintainCompany, c.responsibility_days AS responsibilityDays "
            + "FROM work_order w LEFT JOIN contract c ON c.id = w.contract_id "
            + "WHERE w.equipment_id = #{id} AND w.is_deleted = 0 "
            + "AND w.status IN ('PENDING_REVIEW','ARCHIVED') ORDER BY w.finish_time DESC LIMIT #{limit}")
    List<Map<String, Object>> recentMaintainRecords(Long id, int limit);
}
