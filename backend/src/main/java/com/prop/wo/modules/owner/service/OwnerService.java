package com.prop.wo.modules.owner.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.prop.wo.common.constant.BizConst;
import com.prop.wo.common.context.LoginUser;
import com.prop.wo.common.context.LoginUserHolder;
import com.prop.wo.common.entity.PageQuery;
import com.prop.wo.common.exception.BizException;
import com.prop.wo.common.result.PageResult;
import com.prop.wo.config.AppProperties;
import com.prop.wo.modules.basedata.entity.OwnerUser;
import com.prop.wo.modules.basedata.mapper.OwnerUserMapper;
import com.prop.wo.modules.basedata.service.HouseService;
import com.prop.wo.modules.equipment.entity.Equipment;
import com.prop.wo.modules.equipment.mapper.EquipmentMapper;
import com.prop.wo.modules.equipment.service.EquipmentService;
import com.prop.wo.modules.evidence.mapper.EvidencePhotoMapper;
import com.prop.wo.modules.evidence.entity.EvidencePhoto;
import com.prop.wo.modules.owner.dto.OwnerDtos;
import com.prop.wo.modules.owner.entity.PublishNotice;
import com.prop.wo.modules.owner.mapper.PublishNoticeMapper;
import com.prop.wo.modules.system.entity.SysUser;
import com.prop.wo.modules.system.mapper.SysUserMapper;
import com.prop.wo.modules.workorder.dto.OrderDtos;
import com.prop.wo.modules.workorder.entity.WorkOrder;
import com.prop.wo.modules.workorder.entity.WorkOrderLog;
import com.prop.wo.modules.workorder.mapper.WorkOrderLogMapper;
import com.prop.wo.modules.workorder.mapper.WorkOrderMapper;
import com.prop.wo.modules.workorder.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业主端：验证码登录、扫码查询、三步报修、进度跟踪、评价投诉、公示
 */
@Service
@RequiredArgsConstructor
public class OwnerService {

    private final OwnerUserMapper ownerUserMapper;
    private final SysUserMapper sysUserMapper;
    private final WorkOrderMapper orderMapper;
    private final WorkOrderLogMapper logMapper;
    private final EvidencePhotoMapper photoMapper;
    private final PublishNoticeMapper noticeMapper;
    private final EquipmentMapper equipmentMapper;
    private final EquipmentService equipmentService;
    private final HouseService houseService;
    private final WorkOrderService workOrderService;
    private final StringRedisTemplate redisTemplate;
    private final AppProperties appProperties;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ---------------- 登录 ----------------

    public Map<String, Object> sendCode(String phone) {
        if (phone == null || !phone.matches("^1\\d{10}$")) {
            throw new BizException("手机号需为 11 位");
        }
        String code = appProperties.getSmsFixedCode();
        redisTemplate.opsForValue().set("sms:owner:" + phone, code, Duration.ofMinutes(5));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sent", true);
        result.put("tip", "开发环境验证码固定为 " + code + "，有效期 5 分钟");
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> login(OwnerDtos.LoginReq req) {
        String cached = redisTemplate.opsForValue().get("sms:owner:" + req.phone());
        if (cached == null || !cached.equals(req.code())) {
            throw new BizException("验证码不正确或已过期");
        }
        OwnerUser owner = houseService.ownerByPhone(req.phone());
        if (owner == null) {
            throw new BizException("该手机号未登记业主档案，请联系物业服务中心");
        }
        if (StringUtils.hasText(req.roomNo()) && !houseService.matchesOwner(req.phone(), req.roomNo())) {
            throw new BizException("房号与该手机号不匹配，请核对后重试");
        }
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.phone()).last("limit 1"));
        if (user == null) {
            user = new SysUser();
            user.setUsername(req.phone());
            user.setPassword(passwordEncoder.encode("owner@" + req.phone()));
            user.setRealName(owner.getName());
            user.setPhone(req.phone());
            user.setRoleCode("OWNER");
            user.setStatus("ENABLED");
            user.setRemark("业主端验证码登录自动创建");
            sysUserMapper.insert(user);
        }
        StpUtil.login(user.getId());
        LoginUser loginUser = new LoginUser();
        loginUser.setId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setRealName(user.getRealName());
        loginUser.setRole("OWNER");
        loginUser.setPhone(req.phone());
        LoginUserHolder.put(loginUser);
        redisTemplate.delete("sms:owner:" + req.phone());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", StpUtil.getTokenValue());
        result.put("tokenName", StpUtil.getTokenName());
        result.put("name", owner.getName());
        result.put("phone", owner.getPhone());
        result.put("houses", houseService.housesOfOwner(owner.getId()));
        return result;
    }

    public Map<String, Object> me() {
        LoginUser loginUser = LoginUserHolder.require();
        OwnerUser owner = houseService.ownerByPhone(loginUser.getPhone());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", owner == null ? loginUser.getRealName() : owner.getName());
        result.put("phone", loginUser.getPhone());
        result.put("houses", owner == null ? List.of() : houseService.housesOfOwner(owner.getId()));
        return result;
    }

    // ---------------- 扫码查询与报修 ----------------

    public Map<String, Object> equipmentInfo(String code, String sign) {
        return equipmentService.publicInfo(code, sign);
    }

    /** 报修可选设备：消防设备不出现 */
    public List<Map<String, Object>> publicEquipments(Long communityId) {
        List<Equipment> list = equipmentMapper.selectList(new LambdaQueryWrapper<Equipment>()
                .ne(Equipment::getFacilityType, BizConst.FACILITY_FIRE)
                .eq(communityId != null, Equipment::getCommunityId, communityId)
                .orderByAsc(Equipment::getFacilityType).orderByAsc(Equipment::getCode));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Equipment equipment : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", equipment.getId());
            row.put("code", equipment.getCode());
            row.put("name", equipment.getName());
            row.put("location", equipment.getLocation());
            row.put("facilityLabel", BizConst.facilityLabel(equipment.getFacilityType()));
            rows.add(row);
        }
        return rows;
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkOrder report(OwnerDtos.ReportReq req) {
        if (!StringUtils.hasText(req.description())) {
            throw new BizException("请描述需要处理的问题");
        }
        Equipment equipment = null;
        if (req.equipmentId() != null) {
            equipment = equipmentMapper.selectById(req.equipmentId());
            if (equipment == null) {
                throw new BizException("设备不存在");
            }
            if (BizConst.FACILITY_FIRE.equals(equipment.getFacilityType())) {
                throw new BizException("该设备信息不对外公开，无法在线报修");
            }
        }
        String facilityType = equipment != null ? equipment.getFacilityType()
                : (StringUtils.hasText(req.facilityType()) ? req.facilityType() : BizConst.FACILITY_WATER);
        if (BizConst.FACILITY_FIRE.equals(facilityType)) {
            throw new BizException("消防类事项请直接联系物业服务中心");
        }
        String title = StringUtils.hasText(req.title()) ? req.title()
                : (equipment == null
                ? "业主报修：" + truncate(req.description(), 18)
                : equipment.getName() + " 报修");
        OrderDtos.OrderCreateReq createReq = new OrderDtos.OrderCreateReq(
                req.equipmentId(), BizConst.ORDER_FAULT, facilityType, title, req.description(),
                StringUtils.hasText(req.urgency()) ? req.urgency() : BizConst.URGENCY_NORMAL,
                BizConst.DISPATCH_ASSIGN, null, null, req.houseId(), null, null, "业主端上报");
        LoginUser loginUser = LoginUserHolder.require();
        OwnerUser owner = houseService.ownerByPhone(loginUser.getPhone());
        WorkOrder order = workOrderService.createOwnerReport(createReq, loginUser.getId(),
                owner == null ? loginUser.getRealName() : owner.getName(),
                loginUser.getPhone());
        return order;
    }

    private String truncate(String text, int length) {
        return text.length() <= length ? text : text.substring(0, length) + "…";
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkOrder complaint(OwnerDtos.ComplaintReq req) {
        OrderDtos.OrderCreateReq createReq = new OrderDtos.OrderCreateReq(
                req.equipmentId(), BizConst.ORDER_FAULT, BizConst.FACILITY_WATER,
                "投诉：" + truncate(req.content(), 20), req.content(),
                BizConst.URGENCY_HIGH, BizConst.DISPATCH_ASSIGN, null, null, req.houseId(), null, null, "业主投诉");
        LoginUser loginUser = LoginUserHolder.require();
        OwnerUser owner = houseService.ownerByPhone(loginUser.getPhone());
        return workOrderService.createOwnerReport(createReq, loginUser.getId(),
                owner == null ? loginUser.getRealName() : owner.getName(), loginUser.getPhone());
    }

    // ---------------- 我的上报 ----------------

    public PageResult<Map<String, Object>> myReports(PageQuery query) {
        Page<WorkOrder> page = orderMapper.selectPage(new Page<>(query.safePageNum(), query.safePageSize()),
                new LambdaQueryWrapper<WorkOrder>()
                        .eq(WorkOrder::getReporterUserId, LoginUserHolder.userId())
                        .ne(WorkOrder::getFacilityType, BizConst.FACILITY_FIRE)
                        .orderByDesc(WorkOrder::getId));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (WorkOrder order : page.getRecords()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", order.getId());
            row.put("orderNo", order.getOrderNo());
            row.put("title", order.getTitle());
            row.put("status", order.getStatus());
            row.put("statusLabel", statusLabel(order.getStatus()));
            row.put("facilityLabel", BizConst.facilityLabel(order.getFacilityType()));
            row.put("urgencyLabel", BizConst.urgencyLabel(order.getUrgency()));
            row.put("createTime", order.getCreateTime());
            row.put("finishTime", order.getFinishTime());
            row.put("satisfactionScore", order.getSatisfactionScore());
            row.put("acceptResult", order.getAcceptResult());
            rows.add(row);
        }
        return PageResult.of(page.getTotal(), rows);
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "PENDING_DISPATCH" -> "已提交";
            case "DISPATCHED" -> "已派单";
            case "PROCESSING" -> "处理中";
            case "PENDING_REVIEW" -> "已完成";
            case "ARCHIVED" -> "已完成";
            case "CANCELED" -> "已作废";
            default -> status;
        };
    }

    public Map<String, Object> reportDetail(Long id) {
        WorkOrder order = requireOwnOrder(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", order.getId());
        result.put("orderNo", order.getOrderNo());
        result.put("title", order.getTitle());
        result.put("description", order.getDescription());
        result.put("status", order.getStatus());
        result.put("statusLabel", statusLabel(order.getStatus()));
        result.put("facilityLabel", BizConst.facilityLabel(order.getFacilityType()));
        result.put("urgencyLabel", BizConst.urgencyLabel(order.getUrgency()));
        result.put("createTime", order.getCreateTime());
        result.put("finishTime", order.getFinishTime());
        result.put("acceptResult", order.getAcceptResult());
        result.put("satisfactionScore", order.getSatisfactionScore());
        result.put("deadline", order.getDeadline());
        List<WorkOrderLog> logs = logMapper.selectList(new LambdaQueryWrapper<WorkOrderLog>()
                .eq(WorkOrderLog::getOrderId, id).orderByAsc(WorkOrderLog::getId));
        List<Map<String, Object>> timeline = new ArrayList<>();
        for (WorkOrderLog log : logs) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("action", log.getAction());
            row.put("fromStatus", log.getFromStatus());
            row.put("toStatus", log.getToStatus());
            row.put("createTime", log.getCreateTime());
            timeline.add(row);
        }
        result.put("timeline", timeline);
        List<EvidencePhoto> photos = photoMapper.selectList(new LambdaQueryWrapper<EvidencePhoto>()
                .eq(EvidencePhoto::getOrderId, id).orderByAsc(EvidencePhoto::getId));
        List<Map<String, Object>> photoRows = new ArrayList<>();
        for (EvidencePhoto photo : photos) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", photo.getId());
            row.put("watermarkPath", photo.getWatermarkPath());
            row.put("shootTime", photo.getShootTime());
            photoRows.add(row);
        }
        result.put("photos", photoRows);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void evaluate(Long id, OwnerDtos.EvaluateReq req) {
        WorkOrder order = requireOwnOrder(id);
        if (!"PENDING_REVIEW".equals(order.getStatus()) && !"ARCHIVED".equals(order.getStatus())) {
            throw new BizException("工单处置完成后才能评价");
        }
        if (order.getSatisfactionScore() != null) {
            throw new BizException("该工单已评价，感谢您的反馈");
        }
        if (req.score() == null || req.score() < 1 || req.score() > 5) {
            throw new BizException("请选择 1-5 星满意度");
        }
        workOrderService.review(id, new OrderDtos.ReviewReq(req.content(), req.score()));
    }

    private WorkOrder requireOwnOrder(Long id) {
        WorkOrder order = orderMapper.selectById(id);
        if (order == null || !LoginUserHolder.userId().equals(order.getReporterUserId())) {
            throw BizException.forbidden("无权查看该上报记录");
        }
        if (BizConst.FACILITY_FIRE.equals(order.getFacilityType())) {
            throw BizException.forbidden("该设备信息不对外公开");
        }
        return order;
    }

    // ---------------- 公示 ----------------

    public PageResult<Map<String, Object>> notices(PageQuery query) {
        Page<PublishNotice> page = noticeMapper.selectPage(new Page<>(query.safePageNum(), query.safePageSize()),
                new LambdaQueryWrapper<PublishNotice>()
                        .eq(PublishNotice::getStatus, "PUBLISHED")
                        .ne(PublishNotice::getFacilityType, BizConst.FACILITY_FIRE)
                        .and(w -> w.isNull(PublishNotice::getExpireTime)
                                .or().gt(PublishNotice::getExpireTime, LocalDateTime.now()))
                        .orderByDesc(PublishNotice::getPublishTime));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (PublishNotice notice : page.getRecords()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", notice.getId());
            row.put("title", notice.getTitle());
            row.put("noticeType", notice.getNoticeType());
            row.put("publishTime", notice.getPublishTime());
            row.put("content", notice.getContent());
            rows.add(row);
        }
        return PageResult.of(page.getTotal(), rows);
    }

    public PublishNotice noticeDetail(Long id) {
        PublishNotice notice = noticeMapper.selectById(id);
        if (notice == null || BizConst.FACILITY_FIRE.equals(notice.getFacilityType())) {
            throw new BizException("公示内容不存在");
        }
        return notice;
    }
}
