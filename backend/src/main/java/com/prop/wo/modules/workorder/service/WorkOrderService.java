package com.prop.wo.modules.workorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.prop.wo.common.constant.BizConst;
import com.prop.wo.common.context.LoginUserHolder;
import com.prop.wo.common.entity.PageQuery;
import com.prop.wo.common.enums.WorkOrderStatus;
import com.prop.wo.common.exception.BizException;
import com.prop.wo.common.result.PageResult;
import com.prop.wo.common.util.OrderNoGenerator;
import com.prop.wo.modules.audit.entity.AcceptanceRecord;
import com.prop.wo.modules.audit.entity.CreditRecord;
import com.prop.wo.modules.audit.mapper.AcceptanceRecordMapper;
import com.prop.wo.modules.audit.mapper.CreditRecordMapper;
import com.prop.wo.modules.basedata.entity.Personnel;
import com.prop.wo.modules.basedata.mapper.BuildingMapper;
import com.prop.wo.modules.basedata.mapper.CommunityMapper;
import com.prop.wo.modules.basedata.mapper.PersonnelMapper;
import com.prop.wo.modules.basedata.mapper.TeamMapper;
import com.prop.wo.modules.basedata.service.StaffService;
import com.prop.wo.modules.checklist.service.ChecklistService;
import com.prop.wo.modules.contract.entity.Contract;
import com.prop.wo.modules.contract.mapper.ContractMapper;
import com.prop.wo.modules.contract.service.ContractService;
import com.prop.wo.modules.equipment.entity.Equipment;
import com.prop.wo.modules.equipment.mapper.EquipmentMapper;
import com.prop.wo.modules.evidence.service.EvidenceService;
import com.prop.wo.modules.owner.entity.PublishNotice;
import com.prop.wo.modules.owner.mapper.PublishNoticeMapper;
import com.prop.wo.modules.plan.entity.MaintenancePlan;
import com.prop.wo.modules.plan.mapper.MaintenancePlanMapper;
import com.prop.wo.modules.system.entity.SysUser;
import com.prop.wo.modules.system.mapper.SysUserMapper;
import com.prop.wo.modules.system.service.DictService;
import com.prop.wo.modules.system.service.MessageService;
import com.prop.wo.modules.workorder.dto.OrderDtos;
import com.prop.wo.modules.workorder.entity.WorkOrder;
import com.prop.wo.modules.workorder.entity.WorkOrderAssign;
import com.prop.wo.modules.workorder.entity.WorkOrderClaim;
import com.prop.wo.modules.workorder.entity.WorkOrderLog;
import com.prop.wo.modules.workorder.entity.WorkOrderSettlement;
import com.prop.wo.modules.workorder.mapper.WorkOrderAssignMapper;
import com.prop.wo.modules.workorder.mapper.WorkOrderClaimMapper;
import com.prop.wo.modules.workorder.mapper.WorkOrderLogMapper;
import com.prop.wo.modules.workorder.mapper.WorkOrderMapper;
import com.prop.wo.modules.workorder.mapper.WorkOrderSettlementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 工单生命周期：建单、派单、抢单、执行、验收、驳回、退回、作废、归档、催办、结算
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderMapper orderMapper;
    private final WorkOrderLogMapper logMapper;
    private final WorkOrderClaimMapper claimMapper;
    private final WorkOrderAssignMapper assignMapper;
    private final WorkOrderSettlementMapper settlementMapper;
    private final OrderStateSupport stateSupport;
    private final EquipmentMapper equipmentMapper;
    private final CommunityMapper communityMapper;
    private final BuildingMapper buildingMapper;
    private final TeamMapper teamMapper;
    private final PersonnelMapper personnelMapper;
    private final ContractMapper contractMapper;
    private final MaintenancePlanMapper planMapper;
    private final ContractService contractService;
    private final StaffService staffService;
    private final ChecklistService checklistService;
    private final EvidenceService evidenceService;
    private final AcceptanceRecordMapper acceptanceMapper;
    private final CreditRecordMapper creditMapper;
    private final PublishNoticeMapper noticeMapper;
    private final MessageService messageService;
    private final SysUserMapper sysUserMapper;
    private final DictService dictService;
    private final OrderNoGenerator orderNoGenerator;
    private final StringRedisTemplate redisTemplate;

    // ---------------------------------------------------------------
    // 建单
    // ---------------------------------------------------------------

    @Transactional(rollbackFor = Exception.class)
    public WorkOrder create(OrderDtos.OrderCreateReq req) {
        WorkOrder order = new WorkOrder();
        order.setOrderNo(orderNoGenerator.next());
        order.setOrderType(req.orderType());
        order.setTitle(req.title());
        order.setDescription(req.description());
        order.setUrgency(StringUtils.hasText(req.urgency()) ? req.urgency() : BizConst.URGENCY_NORMAL);
        order.setStatus(WorkOrderStatus.PENDING_DISPATCH.name());
        order.setSource(BizConst.SOURCE_MANUAL);
        order.setPublishFlag(0);
        order.setUrgeCount(0);
        order.setRewardAmount(req.rewardAmount());
        order.setRemark(req.remark());
        order.setHouseId(req.houseId());

        Equipment equipment = null;
        if (req.equipmentId() != null) {
            equipment = equipmentMapper.selectById(req.equipmentId());
            if (equipment == null) {
                throw new BizException("设备不存在");
            }
            order.setEquipmentId(equipment.getId());
            order.setFacilityType(equipment.getFacilityType());
            order.setCommunityId(equipment.getCommunityId());
            order.setBuildingId(equipment.getBuildingId());
        } else {
            if (!StringUtils.hasText(req.facilityType())) {
                throw new BizException("请选择设备或设施类型");
            }
            order.setFacilityType(req.facilityType());
        }
        if (BizConst.FACILITY_FIRE.equals(order.getFacilityType())) {
            throw new BizException("消防类维保不在本平台业务范围，请按合规渠道处理");
        }
        String dispatchMode = StringUtils.hasText(req.dispatchMode()) ? req.dispatchMode() : BizConst.DISPATCH_ASSIGN;
        if (BizConst.ORDER_EMERGENCY.equals(req.orderType())) {
            // 应急工单不进抢单池
            dispatchMode = BizConst.DISPATCH_ASSIGN;
        }
        order.setDispatchMode(dispatchMode);
        order.setTeamId(req.teamId());
        order.setDeadline(req.deadline() == null ? defaultDeadline(order.getUrgency()) : req.deadline());
        orderMapper.insert(order);
        stateSupport.writeLog(order, "CREATE", null, order.getStatus(), "创建工单");

        if (req.mainWorkerId() != null) {
            dispatch(order.getId(), req.mainWorkerId(), req.teamId(), "建单直接指派", null);
        }
        return orderMapper.selectById(order.getId());
    }

    /** 计划引擎/巡检/业主端等系统来源建单：补充触发源与关联业务 */
    @Transactional(rollbackFor = Exception.class)
    public WorkOrder createWithSource(OrderDtos.OrderCreateReq req, String source, Long planId) {
        WorkOrder order = create(req);
        order.setSource(source);
        order.setPlanId(planId);
        orderMapper.updateById(order);
        stateSupport.writeActionLog(order, "SOURCE", "触发源：" + BizConst.sourceLabel(source));
        return order;
    }

    /** 业主上报建单：带出上报人信息，进入待派单池 */
    @Transactional(rollbackFor = Exception.class)
    public WorkOrder createOwnerReport(OrderDtos.OrderCreateReq req, Long reporterUserId,
                                       String reporterName, String reporterPhone) {
        WorkOrder order = create(req);
        order.setSource(BizConst.SOURCE_OWNER);
        order.setReporterUserId(reporterUserId);
        order.setReporterName(reporterName);
        order.setReporterPhone(reporterPhone);
        order.setDispatchMode(BizConst.DISPATCH_ASSIGN);
        orderMapper.updateById(order);
        stateSupport.writeActionLog(order, "SOURCE", "业主上报：" + reporterName);
        return order;
    }

    private LocalDateTime defaultDeadline(String urgency) {
        int hours = switch (urgency) {
            case BizConst.URGENCY_URGENT -> 2;
            case BizConst.URGENCY_HIGH -> 8;
            case BizConst.URGENCY_LOW -> 48;
            default -> 24;
        };
        return LocalDateTime.now().plusHours(hours);
    }

    // ---------------------------------------------------------------
    // 查询
    // ---------------------------------------------------------------

    public PageResult<Map<String, Object>> page(OrderDtos.OrderQuery query) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<WorkOrder>()
                .eq(StringUtils.hasText(query.getStatus()), WorkOrder::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getOrderType()), WorkOrder::getOrderType, query.getOrderType())
                .eq(StringUtils.hasText(query.getFacilityType()), WorkOrder::getFacilityType, query.getFacilityType())
                .eq(query.getCommunityId() != null, WorkOrder::getCommunityId, query.getCommunityId())
                .eq(query.getBuildingId() != null, WorkOrder::getBuildingId, query.getBuildingId())
                .eq(query.getMainWorkerId() != null, WorkOrder::getMainWorkerId, query.getMainWorkerId())
                .eq(StringUtils.hasText(query.getUrgency()), WorkOrder::getUrgency, query.getUrgency())
                .and(StringUtils.hasText(query.getKeyword()), w -> w.like(WorkOrder::getOrderNo, query.getKeyword())
                        .or().like(WorkOrder::getTitle, query.getKeyword()))
                .orderByDesc(WorkOrder::getId);
        if (Boolean.TRUE.equals(query.getOverdueOnly())) {
            wrapper.lt(WorkOrder::getDeadline, LocalDateTime.now())
                    .in(WorkOrder::getStatus, WorkOrderStatus.PENDING_DISPATCH.name(),
                            WorkOrderStatus.DISPATCHED.name(), WorkOrderStatus.PROCESSING.name());
        }
        applyDataScope(wrapper);
        Page<WorkOrder> page = orderMapper.selectPage(new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        return PageResult.of(page.getTotal(), rows(page.getRecords()));
    }

    /** 数据范围：公司仅本公司、作业人员仅本人、业主仅本人上报 */
    private void applyDataScope(LambdaQueryWrapper<WorkOrder> wrapper) {
        String role = LoginUserHolder.role();
        if ("COMPANY".equals(role)) {
            Long companyId = LoginUserHolder.companyId();
            wrapper.apply("(contract_id IN (SELECT id FROM contract WHERE company_id = {0} AND is_deleted = 0) "
                    + "OR main_worker_id IN (SELECT id FROM personnel WHERE company_id = {0} AND is_deleted = 0))",
                    companyId == null ? -1L : companyId);
        } else if ("WORKER".equals(role)) {
            Long personnelId = LoginUserHolder.personnelId();
            wrapper.eq(WorkOrder::getMainWorkerId, personnelId == null ? -1L : personnelId);
        } else if ("OWNER".equals(role)) {
            wrapper.eq(WorkOrder::getReporterUserId, LoginUserHolder.userId())
                    .ne(WorkOrder::getFacilityType, BizConst.FACILITY_FIRE);
        }
    }

    private List<Map<String, Object>> rows(List<WorkOrder> orders) {
        Map<Long, String> equipmentNames = new LinkedHashMap<>();
        Map<Long, String> communityNames = new LinkedHashMap<>();
        Map<Long, String> buildingNames = new LinkedHashMap<>();
        Map<Long, String> workerNames = new LinkedHashMap<>();
        Map<Long, String> teamNames = new LinkedHashMap<>();
        equipmentMapper.selectList(null).forEach(e -> equipmentNames.put(e.getId(), e.getName()));
        communityMapper.selectList(null).forEach(c -> communityNames.put(c.getId(), c.getName()));
        buildingMapper.selectList(null).forEach(b -> buildingNames.put(b.getId(), b.getName()));
        personnelMapper.selectList(null).forEach(p -> workerNames.put(p.getId(), p.getName()));
        teamMapper.selectList(null).forEach(t -> teamNames.put(t.getId(), t.getName()));

        List<Map<String, Object>> rows = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (WorkOrder order : orders) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", order.getId());
            row.put("orderNo", order.getOrderNo());
            row.put("title", order.getTitle());
            row.put("orderType", order.getOrderType());
            row.put("orderTypeLabel", BizConst.orderTypeLabel(order.getOrderType()));
            row.put("facilityType", order.getFacilityType());
            row.put("facilityLabel", BizConst.facilityLabel(order.getFacilityType()));
            row.put("equipmentId", order.getEquipmentId());
            row.put("equipmentName", equipmentNames.get(order.getEquipmentId()));
            row.put("communityName", communityNames.get(order.getCommunityId()));
            row.put("buildingName", buildingNames.get(order.getBuildingId()));
            row.put("urgency", order.getUrgency());
            row.put("urgencyLabel", BizConst.urgencyLabel(order.getUrgency()));
            row.put("status", order.getStatus());
            row.put("statusLabel", WorkOrderStatus.of(order.getStatus()) == null
                    ? order.getStatus() : WorkOrderStatus.of(order.getStatus()).getLabel());
            row.put("source", order.getSource());
            row.put("sourceLabel", BizConst.sourceLabel(order.getSource()));
            row.put("dispatchMode", order.getDispatchMode());
            row.put("deadline", order.getDeadline());
            row.put("mainWorkerId", order.getMainWorkerId());
            row.put("mainWorkerName", workerNames.get(order.getMainWorkerId()));
            row.put("teamId", order.getTeamId());
            row.put("teamName", teamNames.get(order.getTeamId()));
            row.put("rewardAmount", order.getRewardAmount());
            row.put("acceptResult", order.getAcceptResult());
            row.put("publishFlag", order.getPublishFlag());
            row.put("urgeCount", order.getUrgeCount());
            row.put("finishTime", order.getFinishTime());
            row.put("createTime", order.getCreateTime());
            row.put("overdue", order.getDeadline() != null && order.getDeadline().isBefore(now)
                    && !WorkOrderStatus.ARCHIVED.name().equals(order.getStatus())
                    && !WorkOrderStatus.CANCELED.name().equals(order.getStatus()));
            rows.add(row);
        }
        return rows;
    }

    public Map<String, Object> detail(Long id) {
        WorkOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException("工单不存在");
        }
        checkViewPermission(order);
        Map<String, Object> result = rows(List.of(order)).get(0);
        Equipment equipment = order.getEquipmentId() == null ? null : equipmentMapper.selectById(order.getEquipmentId());
        result.put("equipment", equipment == null ? null : Map.of(
                "id", equipment.getId(), "code", equipment.getCode(), "name", equipment.getName(),
                "location", equipment.getLocation() == null ? "" : equipment.getLocation(),
                "status", equipment.getStatus()));
        result.put("description", order.getDescription());
        result.put("responsibilityStart", order.getResponsibilityStart());
        result.put("responsibilityEnd", order.getResponsibilityEnd());
        result.put("satisfactionScore", order.getSatisfactionScore());
        result.put("cancelReason", order.getCancelReason());
        result.put("remark", order.getRemark());
        result.put("reporterName", order.getReporterName());
        result.put("reporterPhone", order.getReporterPhone());
        Personnel worker = order.getMainWorkerId() == null ? null : personnelMapper.selectById(order.getMainWorkerId());
        if (worker != null) {
            result.put("workerPhone", worker.getPhone());
            result.put("workerCertName", worker.getCertName());
        }
        result.put("logs", logMapper.selectList(new LambdaQueryWrapper<WorkOrderLog>()
                .eq(WorkOrderLog::getOrderId, id).orderByAsc(WorkOrderLog::getId)));
        result.put("checklistGroups", checklistService.groupedItems(id));
        result.put("checklistCompleteness", checklistService.completeness(id));
        result.put("mandatoryHint", checklistService.mandatoryHint(order.getFacilityType()));
        result.put("checkins", evidenceService.checkins(id));
        result.put("photos", evidenceService.photos(id));
        result.put("records", evidenceService.records(id));
        result.put("assigns", assignRows(id));
        result.put("acceptances", acceptanceMapper.selectList(new LambdaQueryWrapper<AcceptanceRecord>()
                .eq(AcceptanceRecord::getOrderId, id).orderByAsc(AcceptanceRecord::getId)));
        Contract contract = order.getContractId() == null ? null : contractMapper.selectById(order.getContractId());
        result.put("contract", contract);
        return result;
    }

    private List<Map<String, Object>> assignRows(Long orderId) {
        List<WorkOrderAssign> assigns = assignMapper.selectList(new LambdaQueryWrapper<WorkOrderAssign>()
                .eq(WorkOrderAssign::getOrderId, orderId).orderByAsc(WorkOrderAssign::getId));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (WorkOrderAssign assign : assigns) {
            Personnel personnel = personnelMapper.selectById(assign.getPersonnelId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", assign.getId());
            row.put("personnelId", assign.getPersonnelId());
            row.put("personnelName", personnel == null ? null : personnel.getName());
            row.put("assignType", assign.getAssignType());
            row.put("remark", assign.getRemark());
            row.put("assignTime", assign.getAssignTime());
            rows.add(row);
        }
        return rows;
    }

    private void checkViewPermission(WorkOrder order) {
        String role = LoginUserHolder.role();
        if ("ADMIN".equals(role) || role == null) {
            return;
        }
        if ("OWNER".equals(role)) {
            if (!Objects.equals(order.getReporterUserId(), LoginUserHolder.userId())) {
                throw BizException.forbidden("无权查看他人工单");
            }
            return;
        }
        if ("WORKER".equals(role)) {
            if (Objects.equals(order.getMainWorkerId(), LoginUserHolder.personnelId())) {
                return;
            }
            if (WorkOrderStatus.PENDING_DISPATCH.name().equals(order.getStatus())
                    && BizConst.DISPATCH_GRAB.equals(order.getDispatchMode())) {
                return;
            }
            throw BizException.forbidden("无权查看该工单");
        }
        if ("COMPANY".equals(role)) {
            Long companyId = LoginUserHolder.companyId();
            if (order.getMainWorkerId() != null) {
                Personnel personnel = personnelMapper.selectById(order.getMainWorkerId());
                if (personnel != null && Objects.equals(personnel.getCompanyId(), companyId)) {
                    return;
                }
            }
            if (order.getContractId() != null) {
                Contract contract = contractMapper.selectById(order.getContractId());
                if (contract != null && Objects.equals(contract.getCompanyId(), companyId)) {
                    return;
                }
            }
            throw BizException.forbidden("无法查看其他公司的工单");
        }
    }

    // ---------------------------------------------------------------
    // 派单 / 抢单
    // ---------------------------------------------------------------

    @Transactional(rollbackFor = Exception.class)
    public WorkOrder dispatch(Long id, Long personnelId, Long teamId, String remark, List<Long> assistIds) {
        WorkOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException("工单不存在");
        }
        if (!WorkOrderStatus.PENDING_DISPATCH.name().equals(order.getStatus())) {
            throw new BizException("仅待派单工单可以派单");
        }
        Personnel personnel = staffService.checkCanAccept(personnelId);
        order.setMainWorkerId(personnelId);
        order.setTeamId(teamId == null ? personnel.getTeamId() : teamId);
        order.setDispatchMode(BizConst.DISPATCH_ASSIGN);
        if (order.getDeadline() == null) {
            order.setDeadline(defaultDeadline(order.getUrgency()));
        }
        orderMapper.updateById(order);
        stateSupport.transit(id, WorkOrderStatus.DISPATCHED, "DISPATCH",
                "派单给 " + personnel.getName() + (StringUtils.hasText(remark) ? "：" + remark : ""));

        WorkOrderAssign assign = new WorkOrderAssign();
        assign.setOrderId(id);
        assign.setPersonnelId(personnelId);
        assign.setAssignType("MAIN");
        assign.setRemark(remark);
        assign.setAssignTime(LocalDateTime.now());
        assignMapper.insert(assign);
        if (assistIds != null) {
            for (Long assistId : assistIds) {
                WorkOrderAssign assist = new WorkOrderAssign();
                assist.setOrderId(id);
                assist.setPersonnelId(assistId);
                assist.setAssignType("ASSIST");
                assist.setRemark("协助人员（不占派单位）");
                assist.setAssignTime(LocalDateTime.now());
                assignMapper.insert(assist);
            }
        }
        Long templateId = null;
        if (order.getPlanId() != null) {
            MaintenancePlan plan = planMapper.selectById(order.getPlanId());
            if (plan != null) {
                templateId = plan.getTemplateId();
            }
        }
        WorkOrder dispatched = orderMapper.selectById(id);
        checklistService.instantiate(id, templateId, order.getFacilityType());
        contractService.createForOrder(dispatched, personnel);
        createSettlement(dispatched, personnel);
        for (Long assistId : assistIds == null ? List.<Long>of() : assistIds) {
            notifyPersonnel(assistId, "协助工单", "工单 " + order.getOrderNo() + " 指派您协助作业", id);
        }
        notifyPersonnel(personnelId, "新工单待接单", "工单 " + order.getOrderNo() + " 已派给您，请及时到场", id);
        return orderMapper.selectById(id);
    }

    /** 开放抢单：Redis 锁削峰 + 条件更新保证只有一人抢到 */
    @Transactional(rollbackFor = Exception.class)
    public WorkOrder claim(Long id) {
        if (!"WORKER".equals(LoginUserHolder.role())) {
            throw BizException.forbidden("仅作业人员可以抢单");
        }
        Long personnelId = LoginUserHolder.personnelId();
        if (personnelId == null) {
            throw new BizException("当前账号未关联人员档案，无法抢单");
        }
        String lockKey = "workorder:claim:" + id;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, String.valueOf(personnelId),
                Duration.ofSeconds(10));
        if (!Boolean.TRUE.equals(locked)) {
            throw new BizException("该工单正在被其他人员抢单，请稍后重试");
        }
        try {
            WorkOrder order = orderMapper.selectById(id);
            if (order == null) {
                throw new BizException("工单不存在");
            }
            if (!WorkOrderStatus.PENDING_DISPATCH.name().equals(order.getStatus())) {
                throw new BizException("该工单已被抢或已派单");
            }
            if (!BizConst.DISPATCH_GRAB.equals(order.getDispatchMode())) {
                throw new BizException("该工单为指定派单，不参与抢单");
            }
            Personnel personnel = staffService.checkCanAccept(personnelId);
            int updated = orderMapper.claimUpdate(id, personnelId, personnel.getTeamId());
            if (updated == 0) {
                recordClaim(id, personnelId, false, "已被其他人员抢单");
                throw new BizException("手慢了，该工单已被抢");
            }
            recordClaim(id, personnelId, true, null);

            WorkOrderAssign assign = new WorkOrderAssign();
            assign.setOrderId(id);
            assign.setPersonnelId(personnelId);
            assign.setAssignType("MAIN");
            assign.setRemark("开放抢单");
            assign.setAssignTime(LocalDateTime.now());
            assignMapper.insert(assign);

            WorkOrder fresh = orderMapper.selectById(id);
            checklistService.instantiate(id, null, fresh.getFacilityType());
            contractService.createForOrder(fresh, personnel);
            createSettlement(fresh, personnel);
            stateSupport.writeLog(fresh, "CLAIM", WorkOrderStatus.PENDING_DISPATCH.name(),
                    WorkOrderStatus.DISPATCHED.name(), "抢单成功");
            notifyAdmins("工单已被抢单", "工单 " + fresh.getOrderNo() + " 已被 " + personnel.getName() + " 抢单", id);
            return fresh;
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private void recordClaim(Long orderId, Long personnelId, boolean success, String reason) {
        WorkOrderClaim claim = new WorkOrderClaim();
        claim.setOrderId(orderId);
        claim.setPersonnelId(personnelId);
        claim.setUserId(LoginUserHolder.userId());
        claim.setClaimTime(LocalDateTime.now());
        claim.setSuccess(success ? 1 : 0);
        claim.setFailReason(reason);
        claimMapper.insert(claim);
    }

    private void createSettlement(WorkOrder order, Personnel personnel) {
        WorkOrderSettlement settlement = new WorkOrderSettlement();
        settlement.setOrderId(order.getId());
        settlement.setCompanyId(personnel == null ? null : personnel.getCompanyId());
        settlement.setPersonnelId(personnel == null ? null : personnel.getId());
        settlement.setAmount(order.getRewardAmount() == null ? BigDecimal.ZERO : order.getRewardAmount());
        settlement.setSettleStatus("PENDING");
        settlementMapper.insert(settlement);
    }

    // ---------------------------------------------------------------
    // 执行 / 提交 / 验收
    // ---------------------------------------------------------------

    @Transactional(rollbackFor = Exception.class)
    public WorkOrder start(Long id) {
        WorkOrder order = requireOrder(id);
        checkMainWorker(order);
        stateSupport.transit(id, WorkOrderStatus.PROCESSING, "START", "开始作业");
        return orderMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkOrder submit(Long id, List<ChecklistService.ChecklistResult> results) {
        WorkOrder order = requireOrder(id);
        checkMainWorker(order);
        if (!WorkOrderStatus.PROCESSING.name().equals(order.getStatus())) {
            throw new BizException("仅处理中的工单可以提交完工");
        }
        checklistService.saveResults(id, results);
        checklistService.validateCanSubmit(id);
        LocalDateTime finishTime = LocalDateTime.now();
        order.setFinishTime(finishTime);
        orderMapper.updateById(order);
        WorkOrder updated = stateSupport.transit(id, WorkOrderStatus.PENDING_REVIEW, "SUBMIT", "完工提交，等待验收");
        updated.setFinishTime(finishTime);
        contractService.onOrderFinished(updated);
        notifyAdmins("工单待验收", "工单 " + order.getOrderNo() + " 已提交完工，请及时验收", id);
        return orderMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkOrder accept(Long id, OrderDtos.AcceptReq req) {
        WorkOrder order = requireOrder(id);
        if (!WorkOrderStatus.PENDING_REVIEW.name().equals(order.getStatus())) {
            throw new BizException("仅待回访（待验收）工单可以验收");
        }
        Map<String, Object> completeness = checklistService.completeness(id);
        boolean allDone = Boolean.TRUE.equals(completeness.get("allDone"));
        String checkDetail = "作业清单 " + completeness.get("done") + "/" + completeness.get("total")
                + "，强制拍照项 " + completeness.get("photoDone") + "/" + completeness.get("photoRequired");
        boolean pass = "PASS".equals(req.result());
        if (pass && !allDone) {
            throw new BizException("存在未完成或未拍照的强制项，不能通过验收：" + checkDetail);
        }
        AcceptanceRecord record = new AcceptanceRecord();
        record.setOrderId(id);
        record.setResult(pass ? "PASS" : "REJECT");
        record.setReason(req.reason());
        record.setCheckDetail(checkDetail);
        record.setDeductScore(0);
        record.setOperatorId(LoginUserHolder.userId());
        record.setOperatorName(LoginUserHolder.realName());
        record.setCreateTime(LocalDateTime.now());

        if (pass) {
            order.setAcceptResult("PASS");
            orderMapper.updateById(order);
            stateSupport.writeActionLog(order, "ACCEPT", "验收通过：" + checkDetail);
            record.setDeductScore(0);
            acceptanceMapper.insert(record);
            notifyPersonnel(order.getMainWorkerId(), "验收通过",
                    "工单 " + order.getOrderNo() + " 已验收通过，待回访后归档", id);
        } else {
            if (!StringUtils.hasText(req.reason())) {
                throw new BizException("驳回重做必须填写原因");
            }
            int deduct = req.deductScore() == null
                    ? dictService.configInt("DEDUCT_SCORE_REJECT", 2) : req.deductScore();
            record.setDeductScore(deduct);
            acceptanceMapper.insert(record);
            order.setAcceptResult("REJECT");
            order.setFinishTime(null);
            orderMapper.updateById(order);
            stateSupport.transit(id, WorkOrderStatus.PROCESSING, "REJECT", "验收不合格驳回重做：" + req.reason());
            addCredit(order, -Math.abs(deduct), "验收驳回重做：" + req.reason());
            notifyPersonnel(order.getMainWorkerId(), "工单被驳回",
                    "工单 " + order.getOrderNo() + " 验收不合格，原因：" + req.reason() + "，请整改后重新提交", id);
        }
        return orderMapper.selectById(id);
    }

    /** 归档：验收通过后由管理员归档 */
    @Transactional(rollbackFor = Exception.class)
    public WorkOrder archive(Long id) {
        WorkOrder order = requireOrder(id);
        if (!"PASS".equals(order.getAcceptResult())) {
            throw new BizException("请先完成验收通过后再归档");
        }
        order.setArchiveTime(LocalDateTime.now());
        orderMapper.updateById(order);
        stateSupport.transit(id, WorkOrderStatus.ARCHIVED, "ARCHIVE", "回访完成，工单归档");
        WorkOrderSettlement settlement = settlementMapper.selectOne(new LambdaQueryWrapper<WorkOrderSettlement>()
                .eq(WorkOrderSettlement::getOrderId, id).last("limit 1"));
        if (settlement != null) {
            settlement.setSettleStatus("SETTLED");
            settlement.setSettleTime(LocalDateTime.now());
            settlementMapper.updateById(settlement);
        }
        return orderMapper.selectById(id);
    }

    /** 回访与评价：记录回访内容与满意度，写入信用记录 */
    @Transactional(rollbackFor = Exception.class)
    public void review(Long id, OrderDtos.ReviewReq req) {
        WorkOrder order = requireOrder(id);
        if (req.score() != null && (req.score() < 1 || req.score() > 5)) {
            throw new BizException("满意度评分需在 1-5 星之间");
        }
        order.setSatisfactionScore(req.score());
        orderMapper.updateById(order);
        evidenceService.addRecord(id, "REVIEW", "回访：" + (req.content() == null ? "业主无补充意见" : req.content())
                + (req.score() == null ? "" : "，满意度 " + req.score() + " 星"));
        stateSupport.writeActionLog(order, "REVIEW", "回访记录");
        if (req.score() != null) {
            addCredit(order, req.score(), "业主满意度评价 " + req.score() + " 星");
        }
    }

    private void addCredit(WorkOrder order, int score, String reason) {
        Personnel personnel = order.getMainWorkerId() == null ? null
                : personnelMapper.selectById(order.getMainWorkerId());
        CreditRecord record = new CreditRecord();
        record.setTargetType("PERSONNEL");
        record.setTargetId(order.getMainWorkerId());
        record.setTargetName(personnel == null ? null : personnel.getName());
        record.setOrderId(order.getId());
        record.setCreditType(score < 0 ? "DEDUCT" : "EVALUATE");
        record.setScore(score);
        record.setReason(reason);
        record.setOperatorId(LoginUserHolder.get() == null ? null : LoginUserHolder.userId());
        record.setOperatorName(LoginUserHolder.realName());
        record.setCreateTime(LocalDateTime.now());
        creditMapper.insert(record);

        if (personnel != null && personnel.getCompanyId() != null) {
            CreditRecord companyRecord = new CreditRecord();
            companyRecord.setTargetType("COMPANY");
            companyRecord.setTargetId(personnel.getCompanyId());
            companyRecord.setTargetName(personnel.getName() + " 所属公司");
            companyRecord.setOrderId(order.getId());
            companyRecord.setCreditType(score < 0 ? "DEDUCT" : "EVALUATE");
            companyRecord.setScore(score);
            companyRecord.setReason(reason);
            companyRecord.setOperatorId(LoginUserHolder.get() == null ? null : LoginUserHolder.userId());
            companyRecord.setOperatorName(LoginUserHolder.realName());
            companyRecord.setCreateTime(LocalDateTime.now());
            creditMapper.insert(companyRecord);
        }
    }

    // ---------------------------------------------------------------
    // 其他流转
    // ---------------------------------------------------------------

    @Transactional(rollbackFor = Exception.class)
    public WorkOrder urge(Long id) {
        WorkOrder order = requireOrder(id);
        if (WorkOrderStatus.ARCHIVED.name().equals(order.getStatus())
                || WorkOrderStatus.CANCELED.name().equals(order.getStatus())) {
            throw new BizException("已归档或已作废工单无需催办");
        }
        order.setUrgeCount((order.getUrgeCount() == null ? 0 : order.getUrgeCount()) + 1);
        order.setLastUrgeTime(LocalDateTime.now());
        orderMapper.updateById(order);
        stateSupport.writeActionLog(order, "URGE", "第 " + order.getUrgeCount() + " 次催办");
        Long target = order.getMainWorkerId() != null ? order.getMainWorkerId() : null;
        if (target != null) {
            notifyPersonnel(target, "工单催办", "工单 " + order.getOrderNo() + " 已超时，请尽快处理", id);
        } else {
            notifyAdmins("工单待派单催办", "工单 " + order.getOrderNo() + " 仍未派单", id);
        }
        return orderMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkOrder returnToDispatch(Long id, String reason) {
        WorkOrder order = requireOrder(id);
        if (!StringUtils.hasText(reason)) {
            throw new BizException("退回重新派单须填写原因");
        }
        order.setMainWorkerId(null);
        order.setTeamId(null);
        order.setContractId(null);
        order.setAcceptResult(null);
        orderMapper.updateById(order);
        stateSupport.transit(id, WorkOrderStatus.PENDING_DISPATCH, "RETURN", "退回重新派单：" + reason);
        return orderMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkOrder cancel(Long id, String reason) {
        WorkOrder order = requireOrder(id);
        if (!StringUtils.hasText(reason)) {
            throw new BizException("作废须填写原因");
        }
        order.setCancelReason(reason);
        orderMapper.updateById(order);
        stateSupport.transit(id, WorkOrderStatus.CANCELED, "CANCEL", "工单作废：" + reason);
        notifyPersonnel(order.getMainWorkerId(), "工单已作废", "工单 " + order.getOrderNo() + " 已作废：" + reason, id);
        return orderMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkOrder transfer(Long id, Long personnelId, String reason) {
        WorkOrder order = requireOrder(id);
        if (!WorkOrderStatus.DISPATCHED.name().equals(order.getStatus())
                && !WorkOrderStatus.PROCESSING.name().equals(order.getStatus())) {
            throw new BizException("仅已派单或处理中的工单可以转派");
        }
        Personnel personnel = staffService.checkCanAccept(personnelId);
        Long oldWorker = order.getMainWorkerId();
        order.setMainWorkerId(personnelId);
        order.setTeamId(personnel.getTeamId());
        orderMapper.updateById(order);
        WorkOrderAssign assign = new WorkOrderAssign();
        assign.setOrderId(id);
        assign.setPersonnelId(personnelId);
        assign.setAssignType("MAIN");
        assign.setRemark("转派：" + (reason == null ? "" : reason));
        assign.setAssignTime(LocalDateTime.now());
        assignMapper.insert(assign);
        stateSupport.writeActionLog(order, "TRANSFER", "转派给 " + personnel.getName()
                + (StringUtils.hasText(reason) ? "：" + reason : ""));
        notifyPersonnel(oldWorker, "工单已转派", "工单 " + order.getOrderNo() + " 已转派他人", id);
        notifyPersonnel(personnelId, "转派工单", "工单 " + order.getOrderNo() + " 已转派给您", id);
        return orderMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchDispatch(List<Long> orderIds, Long personnelId, Long teamId) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new BizException("请选择要派单的工单");
        }
        for (Long id : orderIds) {
            dispatch(id, personnelId, teamId, "批量派单", null);
        }
    }

    /** 公示发布：消防类工单永久禁止公开 */
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id, OrderDtos.PublishReq req) {
        WorkOrder order = requireOrder(id);
        if (BizConst.FACILITY_FIRE.equals(order.getFacilityType())) {
            throw new BizException("消防类工单不允许对外公示");
        }
        if (!WorkOrderStatus.PENDING_REVIEW.name().equals(order.getStatus())
                && !WorkOrderStatus.ARCHIVED.name().equals(order.getStatus())) {
            throw new BizException("仅已完成处置的工单可以公示");
        }
        boolean publish = !Boolean.FALSE.equals(req.publish());
        order.setPublishFlag(publish ? 1 : 0);
        orderMapper.updateById(order);
        if (publish) {
            PublishNotice notice = new PublishNotice();
            notice.setTitle(StringUtils.hasText(req.title()) ? req.title() : order.getTitle());
            notice.setContent(StringUtils.hasText(req.content()) ? req.content()
                    : "设备：" + order.getTitle() + "，已完成处置并验收，恢复正常使用。");
            notice.setNoticeType(StringUtils.hasText(req.noticeType()) ? req.noticeType() : "REPAIR");
            notice.setFacilityType(order.getFacilityType());
            notice.setOrderId(order.getId());
            notice.setPublishTime(LocalDateTime.now());
            notice.setStatus("PUBLISHED");
            notice.setPublishUserId(LoginUserHolder.userId());
            noticeMapper.insert(notice);
        } else {
            noticeMapper.delete(new LambdaQueryWrapper<PublishNotice>().eq(PublishNotice::getOrderId, order.getId()));
        }
        stateSupport.writeActionLog(order, "PUBLISH", publish ? "发布对外公示" : "取消公示");
    }

    // ---------------------------------------------------------------
    // 作业端
    // ---------------------------------------------------------------

    /** 工单大厅：开放抢单池，按紧急等级与时限排序 */
    public List<Map<String, Object>> hall(String facilityType) {
        List<WorkOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getStatus, WorkOrderStatus.PENDING_DISPATCH.name())
                .eq(WorkOrder::getDispatchMode, BizConst.DISPATCH_GRAB)
                .eq(StringUtils.hasText(facilityType), WorkOrder::getFacilityType, facilityType)
                .orderByDesc(WorkOrder::getId));
        orders.sort((a, b) -> Integer.compare(BizConst.urgencyWeight(b.getUrgency()),
                BizConst.urgencyWeight(a.getUrgency())));
        return rows(orders);
    }

    /** 我的任务：按状态分组 */
    public Map<String, Object> myTasks() {
        Long personnelId = LoginUserHolder.personnelId();
        if (personnelId == null) {
            throw new BizException("当前账号未关联人员档案");
        }
        List<WorkOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getMainWorkerId, personnelId)
                .ne(WorkOrder::getStatus, WorkOrderStatus.CANCELED.name())
                .orderByDesc(WorkOrder::getId));
        Map<String, Object> result = new LinkedHashMap<>();
        for (WorkOrderStatus status : WorkOrderStatus.values()) {
            List<WorkOrder> list = orders.stream().filter(o -> status.name().equals(o.getStatus())).toList();
            result.put(status.name(), rows(list));
        }
        return result;
    }

    /** 业绩：接单量、完成率、评价、结算记录 */
    public Map<String, Object> performance() {
        Long personnelId = LoginUserHolder.personnelId();
        if (personnelId == null && !"COMPANY".equals(LoginUserHolder.role())) {
            throw new BizException("当前账号未关联人员档案");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        List<WorkOrderSettlement> settlements;
        if ("COMPANY".equals(LoginUserHolder.role())) {
            Long companyId = LoginUserHolder.companyId();
            Map<String, Object> stats = orderMapper.companyStats(companyId == null ? -1L : companyId);
            result.put("stats", stats == null ? Map.of() : stats);
            settlements = settlementMapper.selectList(new LambdaQueryWrapper<WorkOrderSettlement>()
                    .eq(WorkOrderSettlement::getCompanyId, companyId).orderByDesc(WorkOrderSettlement::getId));
        } else {
            Map<String, Object> stats = orderMapper.workerStats(personnelId);
            result.put("stats", stats == null ? Map.of() : stats);
            settlements = settlementMapper.selectList(new LambdaQueryWrapper<WorkOrderSettlement>()
                    .eq(WorkOrderSettlement::getPersonnelId, personnelId).orderByDesc(WorkOrderSettlement::getId));
        }
        List<Map<String, Object>> settleRows = new ArrayList<>();
        for (WorkOrderSettlement settlement : settlements) {
            WorkOrder order = orderMapper.selectById(settlement.getOrderId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", settlement.getId());
            row.put("orderNo", order == null ? null : order.getOrderNo());
            row.put("title", order == null ? null : order.getTitle());
            row.put("amount", settlement.getAmount());
            row.put("settleStatus", settlement.getSettleStatus());
            row.put("settleTime", settlement.getSettleTime());
            row.put("createTime", settlement.getCreateTime());
            settleRows.add(row);
        }
        result.put("settlements", settleRows);
        double total = 0;
        if (result.get("stats") instanceof Map<?, ?> stats) {
            Object totalObj = stats.get("total");
            Object finishedObj = stats.get("finished");
            long totalCount = totalObj == null ? 0 : Long.parseLong(totalObj.toString());
            long finished = finishedObj == null ? 0 : Long.parseLong(finishedObj.toString());
            total = totalCount == 0 ? 0 : Math.round(finished * 1000.0 / totalCount) / 10.0;
        }
        result.put("finishRate", total);
        CreditRecord credit = creditMapper.selectOne(new LambdaQueryWrapper<CreditRecord>()
                .eq(CreditRecord::getTargetType, "PERSONNEL").eq(CreditRecord::getTargetId, personnelId)
                .orderByDesc(CreditRecord::getId).last("limit 1"));
        result.put("latestCredit", credit);
        return result;
    }

    public PageResult<Map<String, Object>> settlements(Long personnelId, Long companyId, PageQuery query) {
        LambdaQueryWrapper<WorkOrderSettlement> wrapper = new LambdaQueryWrapper<WorkOrderSettlement>()
                .eq(personnelId != null, WorkOrderSettlement::getPersonnelId, personnelId)
                .eq(companyId != null, WorkOrderSettlement::getCompanyId, companyId)
                .orderByDesc(WorkOrderSettlement::getId);
        Page<WorkOrderSettlement> page = settlementMapper.selectPage(
                new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (WorkOrderSettlement settlement : page.getRecords()) {
            WorkOrder order = orderMapper.selectById(settlement.getOrderId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", settlement.getId());
            row.put("orderId", settlement.getOrderId());
            row.put("orderNo", order == null ? null : order.getOrderNo());
            row.put("title", order == null ? null : order.getTitle());
            row.put("amount", settlement.getAmount());
            row.put("settleStatus", settlement.getSettleStatus());
            row.put("settleTime", settlement.getSettleTime());
            rows.add(row);
        }
        return PageResult.of(page.getTotal(), rows);
    }

    // ---------------------------------------------------------------
    // 内部工具
    // ---------------------------------------------------------------

    private WorkOrder requireOrder(Long id) {
        WorkOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException("工单不存在");
        }
        return order;
    }

    private void checkMainWorker(WorkOrder order) {
        if ("ADMIN".equals(LoginUserHolder.role())) {
            return;
        }
        Long personnelId = LoginUserHolder.personnelId();
        if (personnelId == null || !personnelId.equals(order.getMainWorkerId())) {
            throw BizException.forbidden("仅本单主责人可执行该操作");
        }
    }

    private void notifyPersonnel(Long personnelId, String title, String content, Long orderId) {
        if (personnelId == null) {
            return;
        }
        Personnel personnel = personnelMapper.selectById(personnelId);
        if (personnel != null && personnel.getUserId() != null) {
            messageService.send(personnel.getUserId(), "ORDER", title, content, "WORK_ORDER", orderId);
        }
    }

    private void notifyAdmins(String title, String content, Long orderId) {
        List<SysUser> admins = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRoleCode, "ADMIN").eq(SysUser::getStatus, "ENABLED"));
        admins.forEach(admin -> messageService.send(admin.getId(), "ORDER", title, content, "WORK_ORDER", orderId));
    }

    /** 超时催办扫描：每 10 分钟检查一次 */
    @Scheduled(cron = "${app.urge-scan-cron}")
    public void urgeScan() {
        List<WorkOrder> list = orderMapper.findNeedUrge(LocalDateTime.now().minusMinutes(30));
        for (WorkOrder order : list) {
            order.setUrgeCount((order.getUrgeCount() == null ? 0 : order.getUrgeCount()) + 1);
            order.setLastUrgeTime(LocalDateTime.now());
            orderMapper.updateById(order);
            notifyPersonnel(order.getMainWorkerId(), "工单超时催办",
                    "工单 " + order.getOrderNo() + " 已超时，请尽快处理", order.getId());
            notifyAdmins("工单超时", "工单 " + order.getOrderNo() + " 已超时未完成", order.getId());
        }
    }

    /** 工单批量导出（千级数据按当前筛选条件导出） */
    public List<com.prop.wo.modules.workorder.dto.WorkOrderExcelRow> exportRows(OrderDtos.OrderQuery query) {
        OrderDtos.OrderQuery copy = new OrderDtos.OrderQuery();
        copy.setStatus(query.getStatus());
        copy.setOrderType(query.getOrderType());
        copy.setFacilityType(query.getFacilityType());
        copy.setCommunityId(query.getCommunityId());
        copy.setBuildingId(query.getBuildingId());
        copy.setMainWorkerId(query.getMainWorkerId());
        copy.setKeyword(query.getKeyword());
        copy.setUrgency(query.getUrgency());
        copy.setOverdueOnly(query.getOverdueOnly());
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<WorkOrder>()
                .eq(StringUtils.hasText(copy.getStatus()), WorkOrder::getStatus, copy.getStatus())
                .eq(StringUtils.hasText(copy.getOrderType()), WorkOrder::getOrderType, copy.getOrderType())
                .eq(StringUtils.hasText(copy.getFacilityType()), WorkOrder::getFacilityType, copy.getFacilityType())
                .eq(copy.getCommunityId() != null, WorkOrder::getCommunityId, copy.getCommunityId())
                .eq(copy.getBuildingId() != null, WorkOrder::getBuildingId, copy.getBuildingId())
                .eq(copy.getMainWorkerId() != null, WorkOrder::getMainWorkerId, copy.getMainWorkerId())
                .eq(StringUtils.hasText(copy.getUrgency()), WorkOrder::getUrgency, copy.getUrgency())
                .and(StringUtils.hasText(copy.getKeyword()), w -> w.like(WorkOrder::getOrderNo, copy.getKeyword())
                        .or().like(WorkOrder::getTitle, copy.getKeyword()))
                .orderByDesc(WorkOrder::getId);
        applyDataScope(wrapper);
        List<WorkOrder> orders = orderMapper.selectList(wrapper);
        Map<Long, String> equipmentNames = new LinkedHashMap<>();
        equipmentMapper.selectList(null).forEach(e -> equipmentNames.put(e.getId(), e.getName()));
        Map<Long, String> workerNames = new LinkedHashMap<>();
        personnelMapper.selectList(null).forEach(p -> workerNames.put(p.getId(), p.getName()));
        List<com.prop.wo.modules.workorder.dto.WorkOrderExcelRow> rows = new ArrayList<>();
        for (WorkOrder order : orders) {
            var row = new com.prop.wo.modules.workorder.dto.WorkOrderExcelRow();
            row.setOrderNo(order.getOrderNo());
            row.setTitle(order.getTitle());
            row.setOrderType(BizConst.orderTypeLabel(order.getOrderType()));
            row.setFacilityType(BizConst.facilityLabel(order.getFacilityType()));
            row.setEquipmentName(equipmentNames.get(order.getEquipmentId()));
            row.setUrgency(BizConst.urgencyLabel(order.getUrgency()));
            WorkOrderStatus status = WorkOrderStatus.of(order.getStatus());
            row.setStatus(status == null ? order.getStatus() : status.getLabel());
            row.setSource(BizConst.sourceLabel(order.getSource()));
            row.setMainWorkerName(workerNames.get(order.getMainWorkerId()));
            row.setDeadline(order.getDeadline() == null ? "" : order.getDeadline().toString().replace('T', ' '));
            row.setFinishTime(order.getFinishTime() == null ? "" : order.getFinishTime().toString().replace('T', ' '));
            row.setSatisfaction(order.getSatisfactionScore() == null ? "" : order.getSatisfactionScore() + " 星");
            row.setCreateTime(order.getCreateTime() == null ? "" : order.getCreateTime().toString().replace('T', ' '));
            rows.add(row);
        }
        return rows;
    }

}
