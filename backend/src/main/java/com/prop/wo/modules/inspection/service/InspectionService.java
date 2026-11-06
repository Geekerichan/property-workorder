package com.prop.wo.modules.inspection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.prop.wo.common.constant.BizConst;
import com.prop.wo.common.context.LoginUserHolder;
import com.prop.wo.common.entity.PageQuery;
import com.prop.wo.common.exception.BizException;
import com.prop.wo.common.result.PageResult;
import com.prop.wo.common.util.OrderNoGenerator;
import com.prop.wo.modules.basedata.entity.Personnel;
import com.prop.wo.modules.basedata.mapper.PersonnelMapper;
import com.prop.wo.modules.equipment.entity.Equipment;
import com.prop.wo.modules.equipment.mapper.EquipmentMapper;
import com.prop.wo.modules.evidence.entity.EvidencePhoto;
import com.prop.wo.modules.evidence.mapper.EvidencePhotoMapper;
import com.prop.wo.modules.evidence.service.EvidenceService;
import com.prop.wo.modules.inspection.dto.InspectionDtos;
import com.prop.wo.modules.inspection.dto.InspectionExcelRow;
import com.prop.wo.modules.inspection.entity.InspectionCheckItem;
import com.prop.wo.modules.inspection.entity.InspectionPoint;
import com.prop.wo.modules.inspection.entity.InspectionRecord;
import com.prop.wo.modules.inspection.entity.InspectionRoute;
import com.prop.wo.modules.inspection.entity.InspectionTask;
import com.prop.wo.modules.inspection.entity.InspectionTaskPoint;
import com.prop.wo.modules.inspection.mapper.InspectionCheckItemMapper;
import com.prop.wo.modules.inspection.mapper.InspectionPointMapper;
import com.prop.wo.modules.inspection.mapper.InspectionRecordMapper;
import com.prop.wo.modules.inspection.mapper.InspectionRouteMapper;
import com.prop.wo.modules.inspection.mapper.InspectionTaskMapper;
import com.prop.wo.modules.inspection.mapper.InspectionTaskPointMapper;
import com.prop.wo.modules.workorder.dto.OrderDtos;
import com.prop.wo.modules.workorder.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 巡检：路线点位、任务生成、逐点打卡、异常转工单、统计与导出
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionService {

    private final InspectionRouteMapper routeMapper;
    private final InspectionPointMapper pointMapper;
    private final InspectionCheckItemMapper checkItemMapper;
    private final InspectionTaskMapper taskMapper;
    private final InspectionTaskPointMapper taskPointMapper;
    private final InspectionRecordMapper recordMapper;
    private final EquipmentMapper equipmentMapper;
    private final PersonnelMapper personnelMapper;
    private final EvidencePhotoMapper photoMapper;
    private final EvidenceService evidenceService;
    private final WorkOrderService workOrderService;
    private final OrderNoGenerator orderNoGenerator;

    // ---------------- 路线配置 ----------------

    public List<Map<String, Object>> routes(String facilityType) {
        List<InspectionRoute> routes = routeMapper.selectList(new LambdaQueryWrapper<InspectionRoute>()
                .eq(StringUtils.hasText(facilityType), InspectionRoute::getFacilityType, facilityType)
                .orderByAsc(InspectionRoute::getId));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (InspectionRoute route : routes) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", route.getId());
            row.put("name", route.getName());
            row.put("communityId", route.getCommunityId());
            row.put("facilityType", route.getFacilityType());
            row.put("facilityLabel", BizConst.facilityLabel(route.getFacilityType()));
            row.put("frequency", route.getFrequency());
            row.put("frequencyLabel", switch (route.getFrequency()) {
                case BizConst.FREQ_DAILY -> "每日";
                case BizConst.FREQ_MONTHLY -> "每月";
                default -> "每周";
            });
            row.put("leaderId", route.getLeaderId());
            Personnel leader = route.getLeaderId() == null ? null : personnelMapper.selectById(route.getLeaderId());
            row.put("leaderName", leader == null ? null : leader.getName());
            row.put("status", route.getStatus());
            row.put("remark", route.getRemark());
            row.put("pointCount", pointMapper.selectCount(new LambdaQueryWrapper<InspectionPoint>()
                    .eq(InspectionPoint::getRouteId, route.getId())));
            rows.add(row);
        }
        return rows;
    }

    public Map<String, Object> routeDetail(Long id) {
        InspectionRoute route = routeMapper.selectById(id);
        if (route == null) {
            throw new BizException("巡检路线不存在");
        }
        List<Map<String, Object>> points = new ArrayList<>();
        for (InspectionPoint point : points(id)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("point", point);
            row.put("items", checkItemMapper.selectList(new LambdaQueryWrapper<InspectionCheckItem>()
                    .eq(InspectionCheckItem::getPointId, point.getId())
                    .orderByAsc(InspectionCheckItem::getSortNo)));
            row.put("equipmentName", point.getEquipmentId() == null ? null
                    : equipmentName(point.getEquipmentId()));
            points.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("route", route);
        result.put("points", points);
        return result;
    }

    private String equipmentName(Long equipmentId) {
        Equipment equipment = equipmentMapper.selectById(equipmentId);
        return equipment == null ? null : equipment.getName();
    }

    public List<InspectionPoint> points(Long routeId) {
        return pointMapper.selectList(new LambdaQueryWrapper<InspectionPoint>()
                .eq(InspectionPoint::getRouteId, routeId).orderByAsc(InspectionPoint::getSortNo));
    }

    @Transactional(rollbackFor = Exception.class)
    public InspectionRoute saveRoute(InspectionDtos.RouteSaveReq req) {
        InspectionRoute route = req.getRoute();
        if (route == null || !StringUtils.hasText(route.getName()) || !StringUtils.hasText(route.getFacilityType())) {
            throw new BizException("请填写路线名称与设施类型");
        }
        if (route.getFrequency() == null) {
            route.setFrequency(BizConst.FREQ_WEEKLY);
        }
        if (route.getStatus() == null) {
            route.setStatus("ENABLED");
        }
        if (route.getId() == null) {
            routeMapper.insert(route);
        } else {
            routeMapper.updateById(route);
            for (InspectionPoint old : points(route.getId())) {
                checkItemMapper.delete(new LambdaQueryWrapper<InspectionCheckItem>()
                        .eq(InspectionCheckItem::getPointId, old.getId()));
            }
            pointMapper.delete(new LambdaQueryWrapper<InspectionPoint>()
                    .eq(InspectionPoint::getRouteId, route.getId()));
        }
        List<InspectionDtos.PointReq> pointReqs = req.getPoints() == null ? List.of() : req.getPoints();
        int sort = 1;
        for (InspectionDtos.PointReq pointReq : pointReqs) {
            InspectionPoint point = pointReq.getPoint();
            if (point == null || !StringUtils.hasText(point.getPointName())) {
                continue;
            }
            point.setId(null);
            point.setRouteId(route.getId());
            point.setSortNo(sort++);
            pointMapper.insert(point);
            int itemSort = 1;
            for (InspectionCheckItem item : pointReq.getItems() == null ? List.<InspectionCheckItem>of()
                    : pointReq.getItems()) {
                item.setId(null);
                item.setPointId(point.getId());
                item.setSortNo(itemSort++);
                item.setRequireValue(item.getRequireValue() == null ? 0 : item.getRequireValue());
                checkItemMapper.insert(item);
            }
        }
        return route;
    }

    public void deleteRoute(Long id) {
        Long tasks = taskMapper.selectCount(new LambdaQueryWrapper<InspectionTask>()
                .eq(InspectionTask::getRouteId, id));
        if (tasks != null && tasks > 0) {
            throw new BizException("该路线已生成巡检任务，禁止删除，可停用");
        }
        routeMapper.deleteById(id);
        pointMapper.delete(new LambdaQueryWrapper<InspectionPoint>().eq(InspectionPoint::getRouteId, id));
    }

    // ---------------- 任务生成 ----------------

    @Scheduled(cron = "0 40 0 * * ?")
    public void generateTasks() {
        for (InspectionRoute route : routeMapper.selectList(new LambdaQueryWrapper<InspectionRoute>()
                .eq(InspectionRoute::getStatus, "ENABLED"))) {
            if (!isDue(route)) {
                continue;
            }
            generateTask(route);
        }
    }

    private boolean isDue(InspectionRoute route) {
        LocalDate today = LocalDate.now();
        Long todayCount = taskMapper.selectCount(new LambdaQueryWrapper<InspectionTask>()
                .eq(InspectionTask::getRouteId, route.getId()).eq(InspectionTask::getPlanDate, today));
        if (todayCount != null && todayCount > 0) {
            return false;
        }
        InspectionTask last = taskMapper.selectOne(new LambdaQueryWrapper<InspectionTask>()
                .eq(InspectionTask::getRouteId, route.getId())
                .orderByDesc(InspectionTask::getPlanDate).last("limit 1"));
        if (last == null) {
            return true;
        }
        long days = ChronoUnit.DAYS.between(last.getPlanDate(), today);
        return switch (route.getFrequency()) {
            case BizConst.FREQ_DAILY -> days >= 1;
            case BizConst.FREQ_MONTHLY -> days >= 30;
            default -> days >= 7;
        };
    }

    @Transactional(rollbackFor = Exception.class)
    public InspectionTask generateTask(InspectionRoute route) {
        InspectionTask task = new InspectionTask();
        task.setTaskNo(orderNoGenerator.nextTaskNo());
        task.setRouteId(route.getId());
        task.setPlanDate(LocalDate.now());
        task.setAssigneeId(route.getLeaderId());
        task.setStatus("PENDING");
        task.setAbnormalCount(0);
        taskMapper.insert(task);
        for (InspectionPoint point : points(route.getId())) {
            InspectionTaskPoint taskPoint = new InspectionTaskPoint();
            taskPoint.setTaskId(task.getId());
            taskPoint.setPointId(point.getId());
            taskPoint.setPointName(point.getPointName());
            taskPoint.setEquipmentId(point.getEquipmentId());
            taskPoint.setStatus("PENDING");
            taskPoint.setSortNo(point.getSortNo());
            taskPointMapper.insert(taskPoint);
        }
        if (route.getLeaderId() != null) {
            Personnel leader = personnelMapper.selectById(route.getLeaderId());
            if (leader != null && leader.getUserId() != null) {
                task.setAssigneeId(leader.getId());
                taskMapper.updateById(task);
            }
        }
        return task;
    }

    public InspectionTask generateTaskManually(Long routeId) {
        InspectionRoute route = routeMapper.selectById(routeId);
        if (route == null) {
            throw new BizException("巡检路线不存在");
        }
        return generateTask(route);
    }

    // ---------------- 执行 ----------------

    public List<Map<String, Object>> myTasks() {
        Long personnelId = LoginUserHolder.personnelId();
        if (personnelId == null) {
            throw new BizException("当前账号未关联人员档案");
        }
        List<InspectionTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<InspectionTask>()
                .eq(InspectionTask::getAssigneeId, personnelId)
                .ne(InspectionTask::getStatus, "DONE")
                .orderByAsc(InspectionTask::getPlanDate));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (InspectionTask task : tasks) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", task.getId());
            row.put("taskNo", task.getTaskNo());
            row.put("planDate", task.getPlanDate());
            row.put("status", task.getStatus());
            row.put("abnormalCount", task.getAbnormalCount());
            InspectionRoute route = routeMapper.selectById(task.getRouteId());
            row.put("routeName", route == null ? null : route.getName());
            row.put("facilityLabel", route == null ? null : BizConst.facilityLabel(route.getFacilityType()));
            row.put("points", taskPoints(task.getId()));
            rows.add(row);
        }
        return rows;
    }

    public List<InspectionTaskPoint> taskPoints(Long taskId) {
        return taskPointMapper.selectList(new LambdaQueryWrapper<InspectionTaskPoint>()
                .eq(InspectionTaskPoint::getTaskId, taskId).orderByAsc(InspectionTaskPoint::getSortNo));
    }

    public Map<String, Object> taskDetail(Long taskId) {
        InspectionTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException("巡检任务不存在");
        }
        if (!"ADMIN".equals(LoginUserHolder.role()) && !"COMPANY".equals(LoginUserHolder.role())
                && !task.getAssigneeId().equals(LoginUserHolder.personnelId())) {
            throw BizException.forbidden("仅任务负责人可执行该巡检");
        }
        InspectionRoute route = routeMapper.selectById(task.getRouteId());
        List<Map<String, Object>> points = new ArrayList<>();
        for (InspectionTaskPoint taskPoint : taskPoints(taskId)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("point", taskPoint);
            List<InspectionCheckItem> items = taskPoint.getPointId() == null ? List.of()
                    : checkItemMapper.selectList(new LambdaQueryWrapper<InspectionCheckItem>()
                    .eq(InspectionCheckItem::getPointId, taskPoint.getPointId())
                    .orderByAsc(InspectionCheckItem::getSortNo));
            row.put("items", items);
            row.put("records", recordMapper.selectList(new LambdaQueryWrapper<InspectionRecord>()
                    .eq(InspectionRecord::getTaskPointId, taskPoint.getId())));
            row.put("equipmentName", taskPoint.getEquipmentId() == null ? null
                    : equipmentName(taskPoint.getEquipmentId()));
            points.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task", task);
        result.put("route", route);
        result.put("points", points);
        result.put("photos", evidenceService.photosByTask(taskId));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public InspectionTaskPoint submitPoint(InspectionDtos.PointSubmitReq req) {
        InspectionTaskPoint point = taskPointMapper.selectById(req.getTaskPointId());
        if (point == null) {
            throw new BizException("巡检点位不存在");
        }
        InspectionTask task = taskMapper.selectById(point.getTaskId());
        if (task == null) {
            throw new BizException("巡检任务不存在");
        }
        if (!"ADMIN".equals(LoginUserHolder.role()) && !task.getAssigneeId().equals(LoginUserHolder.personnelId())) {
            throw BizException.forbidden("仅任务负责人可提交巡检记录");
        }
        boolean abnormal = "ABNORMAL".equals(req.getResult());
        point.setStatus("DONE");
        point.setResult(abnormal ? "ABNORMAL" : "NORMAL");
        point.setRemark(req.getRemark());
        point.setCheckinTime(LocalDateTime.now());
        point.setLongitude(req.getLongitude());
        point.setLatitude(req.getLatitude());
        taskPointMapper.updateById(point);

        task.setStatus("DOING");
        if (abnormal) {
            task.setAbnormalCount((task.getAbnormalCount() == null ? 0 : task.getAbnormalCount()) + 1);
        }
        taskMapper.updateById(task);

        List<InspectionDtos.ItemReq> items = req.getItems() == null ? List.of() : req.getItems();
        for (InspectionDtos.ItemReq item : items) {
            InspectionRecord record = new InspectionRecord();
            record.setTaskId(task.getId());
            record.setTaskPointId(point.getId());
            record.setItemId(item.getItemId());
            record.setItemName(StringUtils.hasText(item.getItemName()) ? item.getItemName() : "巡检项");
            record.setResult(abnormal || "ABNORMAL".equals(item.getResult()) ? "ABNORMAL" : "NORMAL");
            record.setItemValue(item.getValue());
            record.setPhotoPath(item.getPhotoPath());
            record.setRemark(item.getRemark());
            record.setCreateTime(LocalDateTime.now());
            recordMapper.insert(record);
        }
        if (items.isEmpty()) {
            InspectionRecord record = new InspectionRecord();
            record.setTaskId(task.getId());
            record.setTaskPointId(point.getId());
            record.setItemName(point.getPointName() + " 点位检查");
            record.setResult(abnormal ? "ABNORMAL" : "NORMAL");
            record.setRemark(req.getRemark());
            record.setCreateTime(LocalDateTime.now());
            recordMapper.insert(record);
        }
        return point;
    }

    @Transactional(rollbackFor = Exception.class)
    public InspectionTask completeTask(Long taskId) {
        InspectionTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException("巡检任务不存在");
        }
        long pending = taskPoints(taskId).stream().filter(p -> !"DONE".equals(p.getStatus())).count();
        if (pending > 0) {
            throw new BizException("还有 " + pending + " 个点位未完成，无法结束巡检");
        }
        task.setStatus("DONE");
        task.setFinishTime(LocalDateTime.now());
        taskMapper.updateById(task);
        return task;
    }

    /** 异常点一键转报修工单，自动带出设备、位置、描述与巡检照片 */
    @Transactional(rollbackFor = Exception.class)
    public Long toWorkOrder(InspectionDtos.ToOrderReq req) {
        InspectionTaskPoint point = taskPointMapper.selectById(req.getTaskPointId());
        if (point == null) {
            throw new BizException("巡检点位不存在");
        }
        if (!"ABNORMAL".equals(point.getResult())) {
            throw new BizException("仅异常点位可以转工单");
        }
        InspectionTask task = taskMapper.selectById(point.getTaskId());
        InspectionRoute route = task == null ? null : routeMapper.selectById(task.getRouteId());
        Equipment equipment = point.getEquipmentId() == null ? null : equipmentMapper.selectById(point.getEquipmentId());
        String title = StringUtils.hasText(req.getTitle()) ? req.getTitle()
                : (equipment == null ? point.getPointName() : equipment.getName()) + " 巡检异常";
        String description = StringUtils.hasText(req.getDescription()) ? req.getDescription()
                : "巡检任务 " + (task == null ? "" : task.getTaskNo()) + " 在点位【" + point.getPointName()
                + "】发现异常：" + (point.getRemark() == null ? "现场检查异常" : point.getRemark());
        OrderDtos.OrderCreateReq createReq = new OrderDtos.OrderCreateReq(
                point.getEquipmentId(),
                BizConst.ORDER_FAULT,
                route == null ? BizConst.FACILITY_WATER : route.getFacilityType(),
                title,
                description,
                StringUtils.hasText(req.getUrgency()) ? req.getUrgency() : BizConst.URGENCY_HIGH,
                BizConst.DISPATCH_GRAB, null, null, null, null, null, "巡检异常转单");
        var order = workOrderService.createWithSource(createReq, BizConst.SOURCE_INSPECTION, null);

        // 巡检照片随单带出
        for (EvidencePhoto photo : evidenceService.photosByTask(task.getId())) {
            if (photo.getOrderId() == null || photo.getOrderId() == 0L) {
                photo.setOrderId(order.getId());
                photoMapper.updateById(photo);
            }
        }
        evidenceService.addRecord(order.getId(), "HAZARD", "由巡检任务 " + task.getTaskNo() + " 异常点位转入");
        return order.getId();
    }

    // ---------------- 统计与导出 ----------------

    public PageResult<Map<String, Object>> taskPage(Long routeId, String status, PageQuery query) {
        LambdaQueryWrapper<InspectionTask> wrapper = new LambdaQueryWrapper<InspectionTask>()
                .eq(routeId != null, InspectionTask::getRouteId, routeId)
                .eq(StringUtils.hasText(status), InspectionTask::getStatus, status)
                .orderByDesc(InspectionTask::getId);
        Page<InspectionTask> page = taskMapper.selectPage(new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (InspectionTask task : page.getRecords()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", task.getId());
            row.put("taskNo", task.getTaskNo());
            row.put("planDate", task.getPlanDate());
            row.put("status", task.getStatus());
            row.put("abnormalCount", task.getAbnormalCount());
            row.put("finishTime", task.getFinishTime());
            InspectionRoute route = routeMapper.selectById(task.getRouteId());
            row.put("routeName", route == null ? null : route.getName());
            Personnel assignee = task.getAssigneeId() == null ? null : personnelMapper.selectById(task.getAssigneeId());
            row.put("assigneeName", assignee == null ? null : assignee.getName());
            row.put("totalPoints", taskPoints(task.getId()).size());
            rows.add(row);
        }
        return PageResult.of(page.getTotal(), rows);
    }

    public Map<String, Object> statistics(int days) {
        LocalDate start = LocalDate.now().minusDays(days);
        List<InspectionTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<InspectionTask>()
                .ge(InspectionTask::getPlanDate, start));
        long total = tasks.size();
        long done = tasks.stream().filter(t -> "DONE".equals(t.getStatus())).count();
        int abnormal = tasks.stream().mapToInt(t -> t.getAbnormalCount() == null ? 0 : t.getAbnormalCount()).sum();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskCount", total);
        result.put("doneCount", done);
        result.put("completionRate", total == 0 ? 0 : Math.round(done * 1000.0 / total) / 10.0);
        result.put("abnormalCount", abnormal);
        result.put("daily", recordMapper.dailyStats(start));
        return result;
    }

    public List<InspectionExcelRow> exportRows(LocalDate start, LocalDate end) {
        List<InspectionTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<InspectionTask>()
                .ge(start != null, InspectionTask::getPlanDate, start)
                .le(end != null, InspectionTask::getPlanDate, end)
                .orderByAsc(InspectionTask::getId));
        List<InspectionExcelRow> rows = new ArrayList<>();
        for (InspectionTask task : tasks) {
            InspectionRoute route = routeMapper.selectById(task.getRouteId());
            Personnel assignee = task.getAssigneeId() == null ? null : personnelMapper.selectById(task.getAssigneeId());
            for (InspectionTaskPoint point : taskPoints(task.getId())) {
                List<InspectionRecord> records = recordMapper.selectList(new LambdaQueryWrapper<InspectionRecord>()
                        .eq(InspectionRecord::getTaskPointId, point.getId()));
                if (records.isEmpty()) {
                    InspectionExcelRow row = new InspectionExcelRow();
                    row.setTaskNo(task.getTaskNo());
                    row.setPlanDate(task.getPlanDate() == null ? "" : task.getPlanDate().toString());
                    row.setRouteName(route == null ? "" : route.getName());
                    row.setPointName(point.getPointName());
                    row.setItemName("未提交记录");
                    row.setResult(point.getResult() == null ? "PENDING" : point.getResult());
                    row.setRemark(point.getRemark());
                    row.setCheckinTime(point.getCheckinTime() == null ? ""
                            : point.getCheckinTime().toString().replace('T', ' '));
                    row.setAssigneeName(assignee == null ? "" : assignee.getName());
                    rows.add(row);
                    continue;
                }
                for (InspectionRecord record : records) {
                    InspectionExcelRow row = new InspectionExcelRow();
                    row.setTaskNo(task.getTaskNo());
                    row.setPlanDate(task.getPlanDate() == null ? "" : task.getPlanDate().toString());
                    row.setRouteName(route == null ? "" : route.getName());
                    row.setPointName(point.getPointName());
                    row.setItemName(record.getItemName());
                    row.setResult(record.getResult());
                    row.setItemValue(record.getItemValue());
                    row.setRemark(record.getRemark());
                    row.setCheckinTime(point.getCheckinTime() == null ? ""
                            : point.getCheckinTime().toString().replace('T', ' '));
                    row.setAssigneeName(assignee == null ? "" : assignee.getName());
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    /** 我的巡检历史 */
    public List<Map<String, Object>> myHistory() {
        Long personnelId = LoginUserHolder.personnelId();
        if (personnelId == null) {
            throw new BizException("当前账号未关联人员档案");
        }
        List<InspectionTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<InspectionTask>()
                .eq(InspectionTask::getAssigneeId, personnelId)
                .eq(InspectionTask::getStatus, "DONE")
                .orderByDesc(InspectionTask::getId).last("limit 50"));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (InspectionTask task : tasks) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", task.getId());
            row.put("taskNo", task.getTaskNo());
            row.put("planDate", task.getPlanDate());
            row.put("finishTime", task.getFinishTime());
            row.put("abnormalCount", task.getAbnormalCount());
            InspectionRoute route = routeMapper.selectById(task.getRouteId());
            row.put("routeName", route == null ? null : route.getName());
            rows.add(row);
        }
        return rows;
    }
}
