package com.prop.wo.modules.workbench.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.prop.wo.common.constant.BizConst;
import com.prop.wo.modules.audit.entity.AcceptanceRecord;
import com.prop.wo.modules.audit.mapper.AcceptanceRecordMapper;
import com.prop.wo.modules.basedata.service.CompanyService;
import com.prop.wo.modules.basedata.service.StaffService;
import com.prop.wo.modules.contract.entity.Contract;
import com.prop.wo.modules.contract.mapper.ContractMapper;
import com.prop.wo.modules.equipment.entity.Equipment;
import com.prop.wo.modules.equipment.mapper.EquipmentMapper;
import com.prop.wo.modules.equipment.service.EquipmentService;
import com.prop.wo.modules.plan.service.PlanService;
import com.prop.wo.modules.workorder.mapper.WorkOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据看板：核心指标、趋势、分布、预警清单、电梯维保专项
 */
@Service
@RequiredArgsConstructor
public class WorkbenchService {

    private final WorkOrderMapper orderMapper;
    private final AcceptanceRecordMapper acceptanceMapper;
    private final EquipmentMapper equipmentMapper;
    private final ContractMapper contractMapper;
    private final EquipmentService equipmentService;
    private final PlanService planService;
    private final StaffService staffService;
    private final CompanyService companyService;

    public Map<String, Object> dashboard() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 核心指标
        long total = orderMapper.selectCount(null);
        Map<String, Long> statusCount = new LinkedHashMap<>();
        for (Map<String, Object> row : orderMapper.countByStatus()) {
            statusCount.put(String.valueOf(row.get("status")), parseLong(row.get("cnt")));
        }
        long pending = statusCount.getOrDefault("PENDING_DISPATCH", 0L)
                + statusCount.getOrDefault("DISPATCHED", 0L)
                + statusCount.getOrDefault("PROCESSING", 0L);
        long finished = orderMapper.countFinished();
        long onTime = orderMapper.countOnTime();
        long passCount = acceptanceMapper.selectCount(new LambdaQueryWrapper<AcceptanceRecord>()
                .eq(AcceptanceRecord::getResult, "PASS"));
        long acceptanceTotal = acceptanceMapper.selectCount(null);
        long inspectionOrders = orderMapper.countInspectionOrders();
        long inspectionArchived = orderMapper.countInspectionArchived();

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalOrders", total);
        metrics.put("pendingOrders", pending);
        metrics.put("overdueOrders", orderMapper.countOverdue());
        metrics.put("maintainOnTimeRate", finished == 0 ? 0 : Math.round(onTime * 1000.0 / finished) / 10.0);
        metrics.put("acceptPassRate", acceptanceTotal == 0 ? 0
                : Math.round(passCount * 1000.0 / acceptanceTotal) / 10.0);
        metrics.put("hazardRectifyRate", inspectionOrders == 0 ? 0
                : Math.round(inspectionArchived * 1000.0 / inspectionOrders) / 10.0);
        metrics.put("todayOrders", orderMapper.selectCount(new LambdaQueryWrapper<com.prop.wo.modules.workorder.entity.WorkOrder>()
                .ge(com.prop.wo.modules.workorder.entity.WorkOrder::getCreateTime, LocalDate.now().atStartOfDay())));
        result.put("metrics", metrics);
        result.put("statusCount", statusCount);

        // 报修趋势（近 30 天，补齐空日）
        Map<String, Long> dayMap = new LinkedHashMap<>();
        for (Map<String, Object> row : orderMapper.countByDay(LocalDateTime.now().minusDays(29))) {
            dayMap.put(String.valueOf(row.get("day")), parseLong(row.get("cnt")));
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 29; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("day", day.format(fmt));
            node.put("count", dayMap.getOrDefault(day.toString(), 0L));
            trend.add(node);
        }
        result.put("trend", trend);

        // 设施分布与业务类型占比
        List<Map<String, Object>> facility = new ArrayList<>();
        for (Map<String, Object> row : orderMapper.countByFacility()) {
            Map<String, Object> node = new LinkedHashMap<>();
            String type = String.valueOf(row.get("facilityType"));
            node.put("name", BizConst.facilityLabel(type));
            node.put("value", parseLong(row.get("cnt")));
            facility.add(node);
        }
        List<Map<String, Object>> orderTypes = new ArrayList<>();
        for (Map<String, Object> row : orderMapper.countByType()) {
            Map<String, Object> node = new LinkedHashMap<>();
            String type = String.valueOf(row.get("orderType"));
            node.put("name", BizConst.orderTypeLabel(type));
            node.put("value", parseLong(row.get("cnt")));
            orderTypes.add(node);
        }
        result.put("facilityDistribution", facility);
        result.put("typeDistribution", orderTypes);

        // 预警区
        Map<String, Object> warnings = new LinkedHashMap<>();
        warnings.put("overdueEquipments", planService.overdueEquipments());
        warnings.put("annualChecks", equipmentService.annualWarnings(30));
        warnings.put("certificates", staffService.expiringCertificates());
        warnings.put("qualifications", companyService.expiringQualifications());
        warnings.put("contracts", expiringContracts());
        result.put("warnings", warnings);

        // 电梯维保专项：近 90 天执行率
        long elevatorCount = equipmentMapper.selectCount(new LambdaQueryWrapper<Equipment>()
                .eq(Equipment::getFacilityType, BizConst.FACILITY_ELEVATOR));
        long elevatorFinished = orderMapper.countElevatorFinished(LocalDateTime.now().minusDays(90));
        long expected = Math.max(elevatorCount, 1) * 6;
        Map<String, Object> elevator = new LinkedHashMap<>();
        elevator.put("equipmentCount", elevatorCount);
        elevator.put("finishedCount", elevatorFinished);
        elevator.put("executionRate", Math.min(100, Math.round(elevatorFinished * 1000.0 / expected) / 10.0));
        elevator.put("overdueCount", planService.overdueEquipments().size());
        result.put("elevator", elevator);
        return result;
    }

    private List<Map<String, Object>> expiringContracts() {
        List<Contract> contracts = contractMapper.selectList(new LambdaQueryWrapper<Contract>()
                .isNotNull(Contract::getRespEnd)
                .gt(Contract::getRespEnd, LocalDateTime.now())
                .le(Contract::getRespEnd, LocalDateTime.now().plusDays(7))
                .orderByAsc(Contract::getRespEnd));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Contract contract : contracts) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", contract.getId());
            row.put("contractNo", contract.getContractNo());
            row.put("partyB", contract.getPartyB());
            row.put("respEnd", contract.getRespEnd());
            row.put("daysLeft", java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), contract.getRespEnd()));
            rows.add(row);
        }
        return rows;
    }

    private long parseLong(Object value) {
        if (value == null) {
            return 0L;
        }
        return Long.parseLong(value.toString());
    }
}
