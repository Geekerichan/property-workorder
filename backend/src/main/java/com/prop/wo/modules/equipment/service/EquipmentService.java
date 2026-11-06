package com.prop.wo.modules.equipment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.prop.wo.common.constant.BizConst;
import com.prop.wo.common.context.LoginUserHolder;
import com.prop.wo.common.entity.PageQuery;
import com.prop.wo.common.exception.BizException;
import com.prop.wo.common.result.PageResult;
import com.prop.wo.common.util.QrCodeUtil;
import com.prop.wo.config.AppProperties;
import com.prop.wo.modules.basedata.entity.Building;
import com.prop.wo.modules.basedata.entity.Community;
import com.prop.wo.modules.basedata.mapper.BuildingMapper;
import com.prop.wo.modules.basedata.mapper.CommunityMapper;
import com.prop.wo.modules.basedata.mapper.CompanyMapper;
import com.prop.wo.modules.equipment.dto.EquipmentExcelRow;
import com.prop.wo.modules.equipment.entity.AnnualCheck;
import com.prop.wo.modules.equipment.entity.Equipment;
import com.prop.wo.modules.equipment.entity.EquipmentExt;
import com.prop.wo.modules.equipment.entity.EquipmentQr;
import com.prop.wo.modules.equipment.entity.EquipmentStatusLog;
import com.prop.wo.modules.equipment.mapper.AnnualCheckMapper;
import com.prop.wo.modules.equipment.mapper.EquipmentExtMapper;
import com.prop.wo.modules.equipment.mapper.EquipmentMapper;
import com.prop.wo.modules.equipment.mapper.EquipmentQrMapper;
import com.prop.wo.modules.equipment.mapper.EquipmentStatusLogMapper;
import com.prop.wo.modules.system.entity.SysUser;
import com.prop.wo.modules.system.mapper.SysUserMapper;
import com.prop.wo.modules.system.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备台账：一物一档、二维码签名、年检预警与锁停
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentMapper equipmentMapper;
    private final EquipmentExtMapper extMapper;
    private final EquipmentQrMapper qrMapper;
    private final AnnualCheckMapper annualCheckMapper;
    private final EquipmentStatusLogMapper statusLogMapper;
    private final CommunityMapper communityMapper;
    private final BuildingMapper buildingMapper;
    private final CompanyMapper companyMapper;
    private final SysUserMapper sysUserMapper;
    private final MessageService messageService;
    private final AppProperties appProperties;

    public PageResult<Map<String, Object>> page(String facilityType, Long communityId, Long buildingId,
                                               String status, String keyword, PageQuery query) {
        LambdaQueryWrapper<Equipment> wrapper = new LambdaQueryWrapper<Equipment>()
                .eq(StringUtils.hasText(facilityType), Equipment::getFacilityType, facilityType)
                .eq(communityId != null, Equipment::getCommunityId, communityId)
                .eq(buildingId != null, Equipment::getBuildingId, buildingId)
                .eq(StringUtils.hasText(status), Equipment::getStatus, status)
                .and(StringUtils.hasText(keyword), w -> w.like(Equipment::getName, keyword)
                        .or().like(Equipment::getCode, keyword)
                        .or().like(Equipment::getLocation, keyword))
                .orderByAsc(Equipment::getFacilityType).orderByDesc(Equipment::getId);
        Page<Equipment> page = equipmentMapper.selectPage(new Page<>(query.safePageNum(), query.safePageSize()), wrapper);

        Map<Long, String> communityNames = new LinkedHashMap<>();
        communityMapper.selectList(null).forEach(c -> communityNames.put(c.getId(), c.getName()));
        Map<Long, String> buildingNames = new LinkedHashMap<>();
        buildingMapper.selectList(null).forEach(b -> buildingNames.put(b.getId(), b.getName()));
        Map<Long, String> companyNames = new LinkedHashMap<>();
        companyMapper.selectList(null).forEach(c -> companyNames.put(c.getId(), c.getName()));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Equipment equipment : page.getRecords()) {
            Map<String, Object> row = baseRow(equipment);
            row.put("communityName", communityNames.get(equipment.getCommunityId()));
            row.put("buildingName", buildingNames.get(equipment.getBuildingId()));
            row.put("companyName", companyNames.get(equipment.getCompanyId()));
            rows.add(row);
        }
        return PageResult.of(page.getTotal(), rows);
    }

    private Map<String, Object> baseRow(Equipment equipment) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", equipment.getId());
        row.put("code", equipment.getCode());
        row.put("name", equipment.getName());
        row.put("facilityType", equipment.getFacilityType());
        row.put("facilityLabel", BizConst.facilityLabel(equipment.getFacilityType()));
        row.put("communityId", equipment.getCommunityId());
        row.put("buildingId", equipment.getBuildingId());
        row.put("location", equipment.getLocation());
        row.put("commissionDate", equipment.getCommissionDate());
        row.put("status", equipment.getStatus());
        row.put("statusLabel", BizConst.equipStatusLabel(equipment.getStatus()));
        row.put("maintainOverdue", equipment.getMaintainOverdue());
        row.put("companyId", equipment.getCompanyId());
        row.put("safetyManager", equipment.getSafetyManager());
        row.put("safetyPhone", equipment.getSafetyPhone());
        row.put("nextAnnualDate", equipment.getNextAnnualDate());
        row.put("longitude", equipment.getLongitude());
        row.put("latitude", equipment.getLatitude());
        row.put("remark", equipment.getRemark());
        return row;
    }

    public Map<String, Object> detail(Long id) {
        Equipment equipment = equipmentMapper.selectById(id);
        if (equipment == null) {
            throw new BizException("设备不存在");
        }
        Map<String, Object> row = baseRow(equipment);
        EquipmentExt ext = extMapper.selectOne(new LambdaQueryWrapper<EquipmentExt>()
                .eq(EquipmentExt::getEquipmentId, id).last("limit 1"));
        row.put("extJson", ext == null ? null : ext.getExtJson());
        EquipmentQr qr = qrMapper.selectOne(new LambdaQueryWrapper<EquipmentQr>()
                .eq(EquipmentQr::getEquipmentId, id).last("limit 1"));
        row.put("qrContent", qr == null ? null : qr.getContent());
        row.put("qrCode", qr == null ? null : qr.getQrCode());
        row.put("printCount", qr == null ? 0 : qr.getPrintCount());
        row.put("annualChecks", annualCheckMapper.selectList(new LambdaQueryWrapper<AnnualCheck>()
                .eq(AnnualCheck::getEquipmentId, id).orderByDesc(AnnualCheck::getCheckDate)));
        row.put("statusLogs", statusLogMapper.selectList(new LambdaQueryWrapper<EquipmentStatusLog>()
                .eq(EquipmentStatusLog::getEquipmentId, id).orderByDesc(EquipmentStatusLog::getId).last("limit 20")));
        row.put("recentMaintain", equipmentMapper.recentMaintainRecords(id, 5));
        return row;
    }

    @Transactional(rollbackFor = Exception.class)
    public Equipment save(Equipment equipment) {
        if (!StringUtils.hasText(equipment.getCode()) || !StringUtils.hasText(equipment.getName())
                || !StringUtils.hasText(equipment.getFacilityType())) {
            throw new BizException("请填写设备编号、名称与设施类型");
        }
        Long same = equipmentMapper.selectCount(new LambdaQueryWrapper<Equipment>()
                .eq(Equipment::getCode, equipment.getCode())
                .ne(equipment.getId() != null, Equipment::getId, equipment.getId()));
        if (same != null && same > 0) {
            throw new BizException("设备编号已存在");
        }
        if (equipment.getStatus() == null) {
            equipment.setStatus(BizConst.EQUIP_NORMAL);
        }
        if (equipment.getMaintainOverdue() == null) {
            equipment.setMaintainOverdue(0);
        }
        if (equipment.getId() == null) {
            equipmentMapper.insert(equipment);
        } else {
            equipmentMapper.updateById(equipment);
        }
        if (equipment.getExtJson() != null) {
            EquipmentExt ext = extMapper.selectOne(new LambdaQueryWrapper<EquipmentExt>()
                    .eq(EquipmentExt::getEquipmentId, equipment.getId()).last("limit 1"));
            if (ext == null) {
                ext = new EquipmentExt();
                ext.setEquipmentId(equipment.getId());
                ext.setExtJson(equipment.getExtJson());
                extMapper.insert(ext);
            } else {
                ext.setExtJson(equipment.getExtJson());
                extMapper.updateById(ext);
            }
        }
        ensureQr(equipment);
        return equipment;
    }

    public void delete(Long id) {
        long orders = equipmentMapper.countOrders(id);
        if (orders > 0) {
            throw new BizException("该设备已关联工单，禁止删除");
        }
        equipmentMapper.deleteById(id);
    }

    /** 生成或刷新设备二维码（含 HMAC 签名） */
    @Transactional(rollbackFor = Exception.class)
    public EquipmentQr ensureQr(Equipment equipment) {
        EquipmentQr qr = qrMapper.selectOne(new LambdaQueryWrapper<EquipmentQr>()
                .eq(EquipmentQr::getEquipmentId, equipment.getId()).last("limit 1"));
        String sign = sign(equipment.getCode());
        String content = appProperties.getQrBaseUrl() + "/" + equipment.getCode() + "?sign=" + sign;
        if (qr == null) {
            qr = new EquipmentQr();
            qr.setEquipmentId(equipment.getId());
            qr.setQrCode(equipment.getCode());
            qr.setSign(sign);
            qr.setContent(content);
            qr.setPrintCount(0);
            qrMapper.insert(qr);
        } else if (!sign.equals(qr.getSign())) {
            qr.setSign(sign);
            qr.setContent(content);
            qrMapper.updateById(qr);
        }
        return qr;
    }

    public byte[] qrPng(Long equipmentId, int size) {
        Equipment equipment = equipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            throw new BizException("设备不存在");
        }
        EquipmentQr qr = ensureQr(equipment);
        return QrCodeUtil.png(qr.getContent(), size);
    }

    public EquipmentQr qrInfo(Long equipmentId) {
        Equipment equipment = equipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            throw new BizException("设备不存在");
        }
        return ensureQr(equipment);
    }

    public void recordPrint(Long equipmentId) {
        Equipment equipment = equipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            throw new BizException("设备不存在");
        }
        EquipmentQr qr = ensureQr(equipment);
        qr.setPrintCount((qr.getPrintCount() == null ? 0 : qr.getPrintCount()) + 1);
        qr.setLastPrintTime(LocalDateTime.now());
        qrMapper.updateById(qr);
    }

    /** 扫码校验：签名不通过直接拒绝 */
    public Equipment verify(String code, String sign) {
        EquipmentQr qr = qrMapper.selectOne(new LambdaQueryWrapper<EquipmentQr>()
                .eq(EquipmentQr::getQrCode, code).last("limit 1"));
        if (qr == null || !qr.getSign().equals(sign)) {
            throw new BizException("二维码校验失败，请联系物业服务中心");
        }
        Equipment equipment = equipmentMapper.selectById(qr.getEquipmentId());
        if (equipment == null) {
            throw new BizException("设备档案不存在");
        }
        return equipment;
    }

    /** 业主端公开信息：消防设备永不公开，不含任何人员个人信息 */
    public Map<String, Object> publicInfo(String code, String sign) {
        Equipment equipment = verify(code, sign);
        Map<String, Object> result = new LinkedHashMap<>();
        if (BizConst.FACILITY_FIRE.equals(equipment.getFacilityType())) {
            result.put("publicVisible", false);
            result.put("message", "该设备信息不对外公开");
            return result;
        }
        result.put("publicVisible", true);
        result.put("id", equipment.getId());
        result.put("name", equipment.getName());
        result.put("code", equipment.getCode());
        result.put("location", equipment.getLocation());
        result.put("facilityLabel", BizConst.facilityLabel(equipment.getFacilityType()));
        String status = equipment.getStatus();
        String statusText = BizConst.equipStatusLabel(status);
        if (BizConst.EQUIP_NORMAL.equals(status) && Integer.valueOf(1).equals(equipment.getMaintainOverdue())) {
            statusText = "维保逾期";
        }
        result.put("statusText", statusText);
        result.put("statusLevel", statusLevel(status, equipment.getMaintainOverdue()));
        result.put("nextAnnualDate", equipment.getNextAnnualDate());

        List<Map<String, Object>> records = equipmentMapper.recentMaintainRecords(equipment.getId(), 5);
        List<Map<String, Object>> publicRecords = new ArrayList<>();
        for (Map<String, Object> record : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("finishTime", record.get("finishTime"));
            item.put("maintainCompany", record.get("maintainCompany"));
            Object acceptResult = record.get("acceptResult");
            item.put("conclusion", acceptResult == null ? "已完成" : ("PASS".equals(acceptResult.toString()) ? "合格" : "待整改"));
            publicRecords.add(item);
        }
        result.put("records", publicRecords);
        if (!records.isEmpty()) {
            result.put("lastMaintainTime", records.get(0).get("finishTime"));
            result.put("lastMaintainCompany", records.get(0).get("maintainCompany"));
        }
        result.put("nextMaintainDate", nextMaintainDate(equipment, records));
        return result;
    }

    private String statusLevel(String status, Integer overdue) {
        if (BizConst.EQUIP_LOCKED.equals(status)) {
            return "DANGER";
        }
        if (BizConst.EQUIP_REPAIR.equals(status)) {
            return "WARN";
        }
        return Integer.valueOf(1).equals(overdue) ? "OVERDUE" : "NORMAL";
    }

    private LocalDate nextMaintainDate(Equipment equipment, List<Map<String, Object>> records) {
        if (records.isEmpty() || records.get(0).get("finishTime") == null) {
            return null;
        }
        Object finish = records.get(0).get("finishTime");
        LocalDate finishDate = finish instanceof LocalDateTime time ? time.toLocalDate()
                : LocalDate.parse(finish.toString().substring(0, 10));
        int cycle = BizConst.FACILITY_ELEVATOR.equals(equipment.getFacilityType()) ? 15 : 30;
        return finishDate.plusDays(cycle);
    }

    @Transactional(rollbackFor = Exception.class)
    public AnnualCheck saveAnnualCheck(AnnualCheck check) {
        if (check.getEquipmentId() == null || check.getCheckDate() == null || check.getExpireDate() == null) {
            throw new BizException("请填写检验日期与有效期");
        }
        Equipment equipment = equipmentMapper.selectById(check.getEquipmentId());
        if (equipment == null) {
            throw new BizException("设备不存在");
        }
        if (check.getResult() == null) {
            check.setResult("PASS");
        }
        annualCheckMapper.insert(check);
        equipment.setNextAnnualDate(check.getExpireDate());
        boolean invalid = "FAIL".equals(check.getResult()) || check.getExpireDate().isBefore(LocalDate.now());
        if (invalid) {
            equipment.setStatus(BizConst.EQUIP_LOCKED);
            logStatus(equipment.getId(), BizConst.EQUIP_LOCKED, "年检未通过或已过期，系统自动锁停");
            notifyAdmins("设备年检锁停", equipment.getName() + " 年检未通过/已过期，已自动锁停，请立即停用并安排复检");
        }
        equipmentMapper.updateById(equipment);
        return check;
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, String status, String reason) {
        Equipment equipment = equipmentMapper.selectById(id);
        if (equipment == null) {
            throw new BizException("设备不存在");
        }
        String from = equipment.getStatus();
        equipment.setStatus(status);
        equipment.setMaintainOverdue(BizConst.EQUIP_LOCKED.equals(status) ? 0 : equipment.getMaintainOverdue());
        equipmentMapper.updateById(equipment);
        logStatus(id, status, reason == null ? "人工变更设备状态" : reason);
        if (BizConst.EQUIP_LOCKED.equals(status)) {
            notifyAdmins("设备锁停", equipment.getName() + " 已被锁停：" + (reason == null ? "人工锁停" : reason));
        }
        if (from == null) {
            log.debug("设备 {} 初始状态 {}", id, status);
        }
    }

    private void logStatus(Long equipmentId, String toStatus, String reason) {
        EquipmentStatusLog log = new EquipmentStatusLog();
        log.setEquipmentId(equipmentId);
        log.setToStatus(toStatus);
        log.setReason(reason);
        log.setOperatorId(LoginUserHolder.get() == null ? null : LoginUserHolder.get().getId());
        log.setOperatorName(LoginUserHolder.get() == null ? "系统" : LoginUserHolder.realName());
        log.setCreateTime(LocalDateTime.now());
        statusLogMapper.insert(log);
    }

    private void notifyAdmins(String title, String content) {
        List<SysUser> admins = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRoleCode, "ADMIN").eq(SysUser::getStatus, "ENABLED"));
        admins.forEach(admin -> messageService.send(admin.getId(), "ANNUAL", title, content, "EQUIPMENT", null));
    }

    /** 年检临期与超期扫描：每日 00:05 执行 */
    @Scheduled(cron = "0 5 0 * * ?")
    public void annualCheckScan() {
        LocalDate today = LocalDate.now();
        List<Integer> remindDays = new ArrayList<>();
        for (String part : appProperties.getAnnualRemindDays().split(",")) {
            try {
                remindDays.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
                // 忽略非法配置
            }
        }
        List<Equipment> list = equipmentMapper.selectList(new LambdaQueryWrapper<Equipment>()
                .isNotNull(Equipment::getNextAnnualDate));
        for (Equipment equipment : list) {
            LocalDate expire = equipment.getNextAnnualDate();
            long days = java.time.temporal.ChronoUnit.DAYS.between(today, expire);
            if (days < 0 && !BizConst.EQUIP_LOCKED.equals(equipment.getStatus())) {
                equipment.setStatus(BizConst.EQUIP_LOCKED);
                equipmentMapper.updateById(equipment);
                logStatus(equipment.getId(), BizConst.EQUIP_LOCKED, "年检超期未检，系统自动锁停");
                notifyAdmins("年检超期锁停", equipment.getName() + " 年检已超期，已自动锁停");
            } else if (remindDays.contains((int) days)) {
                notifyAdmins("年检临期提醒", equipment.getName() + " 年检将于 " + expire + " 到期，剩余 " + days + " 天");
            }
        }
    }

    /** 维保逾期扫描：每日 00:30 执行 */
    @Scheduled(cron = "0 30 0 * * ?")
    public void overdueScan() {
        List<Long> overdueIds = equipmentMapper.overdueElevatorIds(LocalDateTime.now().minusDays(15));
        for (Long id : overdueIds) {
            Equipment equipment = equipmentMapper.selectById(id);
            if (equipment == null || Integer.valueOf(1).equals(equipment.getMaintainOverdue())) {
                continue;
            }
            equipment.setMaintainOverdue(1);
            equipmentMapper.updateById(equipment);
            log.info("设备 {} 维保逾期", equipment.getName());
        }
    }

    public List<Equipment> options(String facilityType) {
        return equipmentMapper.selectList(new LambdaQueryWrapper<Equipment>()
                .eq(StringUtils.hasText(facilityType), Equipment::getFacilityType, facilityType)
                .orderByAsc(Equipment::getFacilityType).orderByAsc(Equipment::getCode));
    }

    public List<EquipmentExcelRow> exportRows(String facilityType, Long communityId) {
        List<Equipment> list = equipmentMapper.selectList(new LambdaQueryWrapper<Equipment>()
                .eq(StringUtils.hasText(facilityType), Equipment::getFacilityType, facilityType)
                .eq(communityId != null, Equipment::getCommunityId, communityId)
                .orderByAsc(Equipment::getFacilityType).orderByAsc(Equipment::getCode));
        Map<Long, String> communityNames = new LinkedHashMap<>();
        communityMapper.selectList(null).forEach(c -> communityNames.put(c.getId(), c.getName()));
        Map<Long, String> companyNames = new LinkedHashMap<>();
        companyMapper.selectList(null).forEach(c -> companyNames.put(c.getId(), c.getName()));
        List<EquipmentExcelRow> rows = new ArrayList<>();
        for (Equipment equipment : list) {
            EquipmentExcelRow row = new EquipmentExcelRow();
            row.setCode(equipment.getCode());
            row.setName(equipment.getName());
            row.setFacilityType(BizConst.facilityLabel(equipment.getFacilityType()));
            row.setCommunityName(communityNames.get(equipment.getCommunityId()));
            row.setLocation(equipment.getLocation());
            row.setCommissionDate(equipment.getCommissionDate() == null ? "" : equipment.getCommissionDate().toString());
            row.setStatus(BizConst.equipStatusLabel(equipment.getStatus()));
            row.setCompanyName(companyNames.get(equipment.getCompanyId()));
            row.setSafetyManager(equipment.getSafetyManager());
            row.setSafetyPhone(equipment.getSafetyPhone());
            row.setNextAnnualDate(equipment.getNextAnnualDate() == null ? "" : equipment.getNextAnnualDate().toString());
            row.setRemark(equipment.getRemark());
            rows.add(row);
        }
        return rows;
    }

    /** 年检临期清单（看板预警） */
    public List<Map<String, Object>> annualWarnings(int days) {
        List<Equipment> list = equipmentMapper.selectList(new LambdaQueryWrapper<Equipment>()
                .isNotNull(Equipment::getNextAnnualDate)
                .le(Equipment::getNextAnnualDate, LocalDate.now().plusDays(days))
                .orderByAsc(Equipment::getNextAnnualDate));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Equipment equipment : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", equipment.getId());
            row.put("name", equipment.getName());
            row.put("code", equipment.getCode());
            row.put("nextAnnualDate", equipment.getNextAnnualDate());
            row.put("daysLeft", java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), equipment.getNextAnnualDate()));
            row.put("status", equipment.getStatus());
            rows.add(row);
        }
        return rows;
    }

    /** HMAC-SHA256 签名 */
    public String sign(String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appProperties.getQrSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(code.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.substring(0, 32);
        } catch (Exception e) {
            throw new IllegalStateException("二维码签名失败", e);
        }
    }

}
