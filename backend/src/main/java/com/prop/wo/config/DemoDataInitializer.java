package com.prop.wo.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.prop.wo.common.constant.BizConst;
import com.prop.wo.common.storage.FileStorageService;
import com.prop.wo.common.util.Md5Util;
import com.prop.wo.common.util.WatermarkUtil;
import com.prop.wo.modules.basedata.entity.Personnel;
import com.prop.wo.modules.basedata.mapper.PersonnelMapper;
import com.prop.wo.modules.checklist.entity.WorkOrderChecklist;
import com.prop.wo.modules.checklist.mapper.WorkOrderChecklistMapper;
import com.prop.wo.modules.checklist.service.ChecklistService;
import com.prop.wo.modules.equipment.entity.Equipment;
import com.prop.wo.modules.equipment.mapper.EquipmentMapper;
import com.prop.wo.modules.equipment.service.EquipmentService;
import com.prop.wo.modules.evidence.entity.EvidenceCheckin;
import com.prop.wo.modules.evidence.entity.EvidencePhoto;
import com.prop.wo.modules.evidence.mapper.EvidenceCheckinMapper;
import com.prop.wo.modules.evidence.mapper.EvidencePhotoMapper;
import com.prop.wo.modules.workorder.entity.WorkOrder;
import com.prop.wo.modules.workorder.mapper.WorkOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 演示数据补全：生成真实带水印照片、二维码、打卡记录，并把作业清单补齐为可验收状态
 * 仅在 app.demo-init-photos=true 且数据缺失时执行，可重复启动
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {

    private final AppProperties appProperties;
    private final WorkOrderMapper orderMapper;
    private final EquipmentMapper equipmentMapper;
    private final PersonnelMapper personnelMapper;
    private final EvidencePhotoMapper photoMapper;
    private final EvidenceCheckinMapper checkinMapper;
    private final WorkOrderChecklistMapper checklistMapper;
    private final ChecklistService checklistService;
    private final EquipmentService equipmentService;
    private final FileStorageService fileStorageService;

    @Override
    public void run(ApplicationArguments args) {
        if (!appProperties.isDemoInitPhotos()) {
            return;
        }
        try {
            initQrCodes();
            initPhotos();
        } catch (Exception e) {
            log.warn("演示数据初始化失败：{}", e.getMessage(), e);
        }
    }

    private void initQrCodes() {
        List<Equipment> equipments = equipmentMapper.selectList(null);
        for (Equipment equipment : equipments) {
            try {
                equipmentService.ensureQr(equipment);
            } catch (Exception e) {
                log.warn("设备 {} 二维码生成失败", equipment.getCode());
            }
        }
        log.info("设备二维码初始化完成，共 {} 台", equipments.size());
    }

    private void initPhotos() {
        List<WorkOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<WorkOrder>()
                .in(WorkOrder::getStatus, "PENDING_REVIEW", "ARCHIVED")
                .isNotNull(WorkOrder::getEquipmentId));
        int generated = 0;
        for (WorkOrder order : orders) {
            try {
                checklistService.instantiate(order.getId(), null, order.getFacilityType());
                List<WorkOrderChecklist> items = checklistMapper.selectList(
                        new LambdaQueryWrapper<WorkOrderChecklist>()
                                .eq(WorkOrderChecklist::getOrderId, order.getId())
                                .orderByAsc(WorkOrderChecklist::getSortNo));
                if (items.isEmpty()) {
                    continue;
                }
                Long photoCount = photoMapper.selectCount(new LambdaQueryWrapper<EvidencePhoto>()
                        .eq(EvidencePhoto::getOrderId, order.getId()));
                Long photoId = null;
                if (photoCount == null || photoCount == 0) {
                    photoId = createPhoto(order, BizConst.PHOTO_BEFORE, "作业前");
                    createPhoto(order, BizConst.PHOTO_AFTER, "作业后");
                    createPhoto(order, BizConst.PHOTO_KEY, "关键操作");
                    createCheckin(order);
                    generated += 3;
                } else {
                    EvidencePhoto first = photoMapper.selectOne(new LambdaQueryWrapper<EvidencePhoto>()
                            .eq(EvidencePhoto::getOrderId, order.getId())
                            .orderByAsc(EvidencePhoto::getId).last("limit 1"));
                    photoId = first == null ? null : first.getId();
                }
                // 补齐清单结果与强制项照片，保证演示可走完验收
                for (WorkOrderChecklist item : items) {
                    boolean changed = false;
                    if (item.getResult() == null) {
                        item.setResult("NORMAL");
                        changed = true;
                    }
                    if (Integer.valueOf(1).equals(item.getRequirePhoto())
                            && (item.getPhotoIds() == null || item.getPhotoIds().isBlank()) && photoId != null) {
                        item.setPhotoIds(String.valueOf(photoId));
                        changed = true;
                    }
                    if (changed) {
                        checklistMapper.updateById(item);
                    }
                }
            } catch (Exception e) {
                log.warn("工单 {} 演示照片生成失败：{}", order.getOrderNo(), e.getMessage());
            }
        }
        log.info("演示留痕照片生成完成，新增 {} 张", generated);
    }

    private Long createPhoto(WorkOrder order, String photoType, String label) throws Exception {
        Equipment equipment = order.getEquipmentId() == null ? null
                : equipmentMapper.selectById(order.getEquipmentId());
        String equipmentName = equipment == null ? order.getTitle() : equipment.getName();
        Personnel worker = order.getMainWorkerId() == null ? null
                : personnelMapper.selectById(order.getMainWorkerId());
        String operator = worker == null ? "张伟" : worker.getName();
        LocalDateTime shootTime = order.getFinishTime() == null
                ? LocalDateTime.now().minusHours(2) : order.getFinishTime().minusMinutes(30);

        byte[] original = placeholderImage(equipmentName, label, shootTime);
        String geoText = equipment == null || equipment.getLongitude() == null
                ? "未获取" : equipment.getLongitude() + "," + equipment.getLatitude();
        byte[] marked = WatermarkUtil.mark(original, shootTime, operator, equipmentName, geoText);

        String originalPath = fileStorageService.save(original, "photo/original", "jpg");
        String watermarkPath = fileStorageService.save(marked, "photo/watermark", "jpg");

        EvidencePhoto photo = new EvidencePhoto();
        photo.setOrderId(order.getId());
        photo.setPhotoType(photoType);
        photo.setOriginalPath(originalPath);
        photo.setWatermarkPath(watermarkPath);
        photo.setMd5(Md5Util.of(marked));
        photo.setLongitude(equipment == null ? null : equipment.getLongitude());
        photo.setLatitude(equipment == null ? null : equipment.getLatitude());
        photo.setShootTime(shootTime);
        photo.setOperatorId(worker == null ? null : worker.getUserId());
        photo.setOperatorName(operator);
        photo.setEquipmentName(equipmentName);
        photo.setRemark(label + "（演示数据）");
        photo.setCreateTime(shootTime);
        photoMapper.insert(photo);
        return photo.getId();
    }

    private void createCheckin(WorkOrder order) {
        Long exists = checkinMapper.selectCount(new LambdaQueryWrapper<EvidenceCheckin>()
                .eq(EvidenceCheckin::getOrderId, order.getId()));
        if (exists != null && exists > 0) {
            return;
        }
        Equipment equipment = order.getEquipmentId() == null ? null
                : equipmentMapper.selectById(order.getEquipmentId());
        EvidenceCheckin checkin = new EvidenceCheckin();
        checkin.setOrderId(order.getId());
        checkin.setPersonnelId(order.getMainWorkerId());
        checkin.setCheckinType("LOCATION");
        checkin.setLongitude(equipment == null ? null : equipment.getLongitude());
        checkin.setLatitude(equipment == null ? null : equipment.getLatitude());
        checkin.setAddress(equipment == null ? "现场" : equipment.getLocation());
        checkin.setDistance(12);
        checkin.setValidFlag(1);
        checkin.setCheckinTime(order.getFinishTime() == null
                ? LocalDateTime.now().minusHours(3) : order.getFinishTime().minusHours(2));
        checkinMapper.insert(checkin);
    }

    /** 生成演示用现场照片（灰底 + 设备信息），再叠加服务端水印 */
    private byte[] placeholderImage(String equipmentName, String label, LocalDateTime time) throws Exception {
        int width = 1200;
        int height = 800;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0xE9, 0xE6, 0xDE));
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(0xC9, 0xC4, 0xB8));
        for (int x = 0; x < width; x += 60) {
            g.drawLine(x, 0, x, height);
        }
        for (int y = 0; y < height; y += 60) {
            g.drawLine(0, y, width, y);
        }
        g.setColor(new Color(0x22, 0x20, 0x1B));
        Font titleFont = chineseFont(56);
        Font subFont = chineseFont(30);
        g.setFont(titleFont);
        drawCenter(g, "现场作业留痕照片", 300, titleFont);
        g.setFont(subFont);
        drawCenter(g, equipmentName, 400, subFont);
        drawCenter(g, label + " · " + time.toLocalDate(), 470, subFont);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }

    private void drawCenter(Graphics2D g, String text, int y, Font font) {
        FontMetrics fm = g.getFontMetrics(font);
        int x = (1200 - fm.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }

    private Font chineseFont(int size) {
        Path path = com.prop.wo.common.util.FontRegistry.resolve(
                com.prop.wo.common.util.FontRegistry.SANS);
        if (path != null) {
            try {
                return Font.createFont(Font.TRUETYPE_FONT, path.toFile()).deriveFont(Font.PLAIN, (float) size);
            } catch (Exception ignored) {
                // 回退系统字体
            }
        }
        return new Font("Microsoft YaHei", Font.PLAIN, size);
    }
}
