package com.prop.wo.modules.plan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.prop.wo.common.constant.BizConst;
import com.prop.wo.common.exception.BizException;
import com.prop.wo.modules.equipment.entity.Equipment;
import com.prop.wo.modules.equipment.mapper.EquipmentMapper;
import com.prop.wo.modules.plan.entity.MaintenancePlan;
import com.prop.wo.modules.plan.entity.PlanDeviceRel;
import com.prop.wo.modules.plan.entity.PlanGenerateLog;
import com.prop.wo.modules.plan.entity.SeasonalNode;
import com.prop.wo.modules.plan.mapper.MaintenancePlanMapper;
import com.prop.wo.modules.plan.mapper.PlanDeviceRelMapper;
import com.prop.wo.modules.plan.mapper.PlanGenerateLogMapper;
import com.prop.wo.modules.plan.mapper.SeasonalNodeMapper;
import com.prop.wo.modules.workorder.dto.OrderDtos;
import com.prop.wo.modules.workorder.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 维保计划引擎：定期/按需生成、季节节点、幂等生成记录、手动触发
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanService {

    private final MaintenancePlanMapper planMapper;
    private final PlanDeviceRelMapper relMapper;
    private final SeasonalNodeMapper seasonalMapper;
    private final PlanGenerateLogMapper genLogMapper;
    private final EquipmentMapper equipmentMapper;
    private final WorkOrderService workOrderService;

    public List<Map<String, Object>> list(String facilityType, String status) {
        List<MaintenancePlan> plans = planMapper.selectList(new LambdaQueryWrapper<MaintenancePlan>()
                .eq(StringUtils.hasText(facilityType), MaintenancePlan::getFacilityType, facilityType)
                .eq(StringUtils.hasText(status), MaintenancePlan::getStatus, status)
                .orderByAsc(MaintenancePlan::getId));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (MaintenancePlan plan : plans) {
            Map<String, Object> row = planRow(plan);
            row.put("devices", devices(plan.getId()));
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> planRow(MaintenancePlan plan) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", plan.getId());
        row.put("name", plan.getName());
        row.put("facilityType", plan.getFacilityType());
        row.put("facilityLabel", BizConst.facilityLabel(plan.getFacilityType()));
        row.put("mode", plan.getMode());
        row.put("modeLabel", "PERIODIC".equals(plan.getMode()) ? "定期" : "按需");
        row.put("periodDays", plan.getPeriodDays());
        row.put("intervalDays", plan.getIntervalDays());
        row.put("advanceDays", plan.getAdvanceDays());
        row.put("orderType", plan.getOrderType());
        row.put("dispatchMode", plan.getDispatchMode());
        row.put("templateId", plan.getTemplateId());
        row.put("seasonalFlag", plan.getSeasonalFlag());
        row.put("nextGenerateDate", plan.getNextGenerateDate());
        row.put("lastGenerateDate", plan.getLastGenerateDate());
        row.put("status", plan.getStatus());
        row.put("remark", plan.getRemark());
        return row;
    }

    public List<Map<String, Object>> devices(Long planId) {
        List<PlanDeviceRel> rels = relMapper.selectList(new LambdaQueryWrapper<PlanDeviceRel>()
                .eq(PlanDeviceRel::getPlanId, planId));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (PlanDeviceRel rel : rels) {
            Equipment equipment = equipmentMapper.selectById(rel.getEquipmentId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("equipmentId", rel.getEquipmentId());
            row.put("equipmentName", equipment == null ? null : equipment.getName());
            row.put("code", equipment == null ? null : equipment.getCode());
            row.put("location", equipment == null ? null : equipment.getLocation());
            rows.add(row);
        }
        return rows;
    }

    @Transactional(rollbackFor = Exception.class)
    public MaintenancePlan save(MaintenancePlan plan, List<Long> equipmentIds) {
        if (!StringUtils.hasText(plan.getName()) || !StringUtils.hasText(plan.getFacilityType())) {
            throw new BizException("请填写计划名称与设施类型");
        }
        if (equipmentIds == null || equipmentIds.isEmpty()) {
            throw new BizException("请至少选择一台设备");
        }
        if ("ON_DEMAND".equals(plan.getMode())) {
            if (plan.getIntervalDays() == null || plan.getIntervalDays() < 1 || plan.getIntervalDays() > 90) {
                throw new BizException("按需维保间隔需在 1-90 天之间");
            }
        } else {
            if (plan.getPeriodDays() == null || plan.getPeriodDays() < 1) {
                throw new BizException("请填写定期维保周期天数");
            }
            if (BizConst.FACILITY_ELEVATOR.equals(plan.getFacilityType()) && plan.getPeriodDays() > 15) {
                plan.setPeriodDays(15);
            }
        }
        plan.setAdvanceDays(plan.getAdvanceDays() == null ? 3 : plan.getAdvanceDays());
        plan.setOrderType(StringUtils.hasText(plan.getOrderType())
                ? plan.getOrderType()
                : ("ON_DEMAND".equals(plan.getMode()) ? BizConst.ORDER_DEMAND : BizConst.ORDER_PLAN));
        plan.setDispatchMode(StringUtils.hasText(plan.getDispatchMode())
                ? plan.getDispatchMode() : BizConst.DISPATCH_GRAB);
        plan.setStatus(StringUtils.hasText(plan.getStatus()) ? plan.getStatus() : "ENABLED");
        if (plan.getNextGenerateDate() == null) {
            int cycle = "ON_DEMAND".equals(plan.getMode()) ? plan.getIntervalDays() : plan.getPeriodDays();
            plan.setNextGenerateDate(LocalDate.now().plusDays(cycle));
        }
        if (plan.getId() == null) {
            planMapper.insert(plan);
        } else {
            planMapper.updateById(plan);
            relMapper.delete(new LambdaQueryWrapper<PlanDeviceRel>().eq(PlanDeviceRel::getPlanId, plan.getId()));
        }
        for (Long equipmentId : equipmentIds) {
            PlanDeviceRel rel = new PlanDeviceRel();
            rel.setPlanId(plan.getId());
            rel.setEquipmentId(equipmentId);
            rel.setCreateTime(LocalDateTime.now());
            relMapper.insert(rel);
        }
        return plan;
    }

    public void delete(Long id) {
        planMapper.deleteById(id);
        relMapper.delete(new LambdaQueryWrapper<PlanDeviceRel>().eq(PlanDeviceRel::getPlanId, id));
    }

    /**
     * 生成计划工单：一个设备一张待派工单；(plan_id, period_start) 唯一约束保证幂等
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generate(Long planId, boolean manual) {
        MaintenancePlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BizException("计划不存在");
        }
        if (!"ENABLED".equals(plan.getStatus())) {
            throw new BizException("计划已停用");
        }
        LocalDate periodStart = plan.getNextGenerateDate() == null ? LocalDate.now() : plan.getNextGenerateDate();
        PlanGenerateLog genLog = new PlanGenerateLog();
        genLog.setPlanId(planId);
        genLog.setPeriodStart(periodStart);
        genLog.setTriggerType(manual ? "MANUAL" : "SCHEDULE");
        genLog.setOrderCount(0);
        genLog.setCreateTime(LocalDateTime.now());
        try {
            genLogMapper.insert(genLog);
        } catch (DuplicateKeyException e) {
            throw new BizException("本期工单已生成，请勿重复触发");
        }

        List<Long> equipmentIds = devices(planId).stream()
                .map(row -> (Long) row.get("equipmentId")).toList();
        int count = 0;
        Long firstOrderId = null;
        for (Long equipmentId : equipmentIds) {
            Equipment equipment = equipmentMapper.selectById(equipmentId);
            if (equipment == null) {
                continue;
            }
            OrderDtos.OrderCreateReq req = new OrderDtos.OrderCreateReq(
                    equipmentId,
                    plan.getOrderType(),
                    plan.getFacilityType(),
                    plan.getName() + " - " + equipment.getName(),
                    "按计划执行" + BizConst.facilityLabel(plan.getFacilityType()) + "保养：" + plan.getName(),
                    BizConst.URGENCY_NORMAL,
                    plan.getDispatchMode(),
                    null,
                    null,
                    null,
                    LocalDateTime.now().plusDays(plan.getAdvanceDays() == null ? 3 : plan.getAdvanceDays()),
                    null,
                    "计划生成"
            );
            var order = workOrderService.createWithSource(req, BizConst.SOURCE_PLAN, planId);
            if (firstOrderId == null) {
                firstOrderId = order.getId();
            }
            count++;
        }
        genLog.setOrderId(firstOrderId);
        genLog.setOrderCount(count);
        genLogMapper.updateById(genLog);

        int cycle = "ON_DEMAND".equals(plan.getMode())
                ? (plan.getIntervalDays() == null ? 30 : plan.getIntervalDays())
                : (plan.getPeriodDays() == null ? 30 : plan.getPeriodDays());
        plan.setLastGenerateDate(LocalDate.now());
        plan.setNextGenerateDate(periodStart.plusDays(cycle));
        planMapper.updateById(plan);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", count);
        result.put("periodStart", periodStart);
        result.put("nextGenerateDate", plan.getNextGenerateDate());
        return result;
    }

    /** 每日 00:10 扫描：到达提前生成日则生成工单 */
    @Scheduled(cron = "0 10 0 * * ?")
    public void scheduleScan() {
        LocalDate today = LocalDate.now();
        List<MaintenancePlan> plans = planMapper.selectList(new LambdaQueryWrapper<MaintenancePlan>()
                .eq(MaintenancePlan::getStatus, "ENABLED")
                .isNotNull(MaintenancePlan::getNextGenerateDate));
        for (MaintenancePlan plan : plans) {
            int advance = plan.getAdvanceDays() == null ? 3 : plan.getAdvanceDays();
            if (!plan.getNextGenerateDate().minusDays(advance).isAfter(today)) {
                try {
                    generate(plan.getId(), false);
                } catch (Exception e) {
                    log.warn("计划 {} 生成失败：{}", plan.getName(), e.getMessage());
                }
            }
        }
    }

    /** 季节节点：命中当天则按设施类型生成保养工单，一年一次 */
    @Scheduled(cron = "0 20 0 * * ?")
    public void seasonalScan() {
        LocalDate today = LocalDate.now();
        List<SeasonalNode> nodes = seasonalMapper.selectList(new LambdaQueryWrapper<SeasonalNode>()
                .eq(SeasonalNode::getEnabled, 1));
        for (SeasonalNode node : nodes) {
            if (node.getMonthNo() != today.getMonthValue() || node.getDayNo() != today.getDayOfMonth()) {
                continue;
            }
            PlanGenerateLog genLog = new PlanGenerateLog();
            genLog.setPlanId(-node.getId());
            genLog.setPeriodStart(LocalDate.of(today.getYear(), node.getMonthNo(), node.getDayNo()));
            genLog.setOrderCount(0);
            genLog.setTriggerType("SEASONAL");
            genLog.setCreateTime(LocalDateTime.now());
            try {
                genLogMapper.insert(genLog);
            } catch (DuplicateKeyException e) {
                continue;
            }
            List<Equipment> equipments = equipmentMapper.selectList(new LambdaQueryWrapper<Equipment>()
                    .eq(Equipment::getFacilityType, node.getFacilityType()));
            int count = 0;
            Long firstOrderId = null;
            for (Equipment equipment : equipments) {
                OrderDtos.OrderCreateReq req = new OrderDtos.OrderCreateReq(
                        equipment.getId(), BizConst.ORDER_PLAN, node.getFacilityType(),
                        node.getName() + " - " + equipment.getName(),
                        node.getDescription() == null ? node.getName() : node.getDescription(),
                        BizConst.URGENCY_NORMAL, BizConst.DISPATCH_GRAB, null, null, null,
                        LocalDateTime.now().plusDays(7), null, "季节节点生成");
                var order = workOrderService.createWithSource(req, BizConst.SOURCE_PLAN, null);
                if (firstOrderId == null) {
                    firstOrderId = order.getId();
                }
                count++;
            }
            genLog.setOrderId(firstOrderId);
            genLog.setOrderCount(count);
            genLogMapper.updateById(genLog);
        }
    }

    public List<Map<String, Object>> generateLogs(Long planId) {
        List<PlanGenerateLog> logs = genLogMapper.selectList(new LambdaQueryWrapper<PlanGenerateLog>()
                .eq(planId != null, PlanGenerateLog::getPlanId, planId)
                .orderByDesc(PlanGenerateLog::getId).last("limit 100"));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (PlanGenerateLog log : logs) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", log.getId());
            row.put("planId", log.getPlanId());
            row.put("periodStart", log.getPeriodStart());
            row.put("orderId", log.getOrderId());
            row.put("orderCount", log.getOrderCount());
            row.put("triggerType", log.getTriggerType());
            row.put("createTime", log.getCreateTime());
            rows.add(row);
        }
        return rows;
    }

    public List<SeasonalNode> seasonalNodes() {
        return seasonalMapper.selectList(new LambdaQueryWrapper<SeasonalNode>()
                .orderByAsc(SeasonalNode::getMonthNo));
    }

    public SeasonalNode saveSeasonal(SeasonalNode node) {
        if (!StringUtils.hasText(node.getName()) || node.getMonthNo() == null) {
            throw new BizException("请填写节点名称与月份");
        }
        if (node.getDayNo() == null) {
            node.setDayNo(1);
        }
        if (node.getEnabled() == null) {
            node.setEnabled(1);
        }
        if (node.getId() == null) {
            seasonalMapper.insert(node);
        } else {
            seasonalMapper.updateById(node);
        }
        return node;
    }

    public void deleteSeasonal(Long id) {
        seasonalMapper.deleteById(id);
    }

    /** 逾期未维保设备清单（看板红牌） */
    public List<Map<String, Object>> overdueEquipments() {
        List<Equipment> list = equipmentMapper.selectList(new LambdaQueryWrapper<Equipment>()
                .eq(Equipment::getMaintainOverdue, 1).orderByAsc(Equipment::getId));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Equipment equipment : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", equipment.getId());
            row.put("code", equipment.getCode());
            row.put("name", equipment.getName());
            row.put("facilityLabel", BizConst.facilityLabel(equipment.getFacilityType()));
            row.put("location", equipment.getLocation());
            row.put("status", equipment.getStatus());
            rows.add(row);
        }
        return rows;
    }

}
