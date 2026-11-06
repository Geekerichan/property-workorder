package com.prop.wo.modules.evidence.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.prop.wo.common.constant.BizConst;
import com.prop.wo.common.context.LoginUserHolder;
import com.prop.wo.common.enums.WorkOrderStatus;
import com.prop.wo.common.exception.BizException;
import com.prop.wo.common.storage.FileStorageService;
import com.prop.wo.common.util.GeoUtil;
import com.prop.wo.common.util.Md5Util;
import com.prop.wo.common.util.WatermarkUtil;
import com.prop.wo.config.AppProperties;
import com.prop.wo.modules.equipment.entity.Equipment;
import com.prop.wo.modules.equipment.mapper.EquipmentMapper;
import com.prop.wo.modules.evidence.entity.EvidenceCheckin;
import com.prop.wo.modules.evidence.entity.EvidencePhoto;
import com.prop.wo.modules.evidence.entity.EvidenceRecord;
import com.prop.wo.modules.evidence.mapper.EvidenceCheckinMapper;
import com.prop.wo.modules.evidence.mapper.EvidencePhotoMapper;
import com.prop.wo.modules.evidence.mapper.EvidenceRecordMapper;
import com.prop.wo.modules.workorder.entity.WorkOrder;
import com.prop.wo.modules.workorder.mapper.WorkOrderMapper;
import com.prop.wo.modules.workorder.service.OrderStateSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 现场作业留痕：打卡、水印照片、MD5、作业记录
 */
@Service
@RequiredArgsConstructor
public class EvidenceService {

    private final EvidenceCheckinMapper checkinMapper;
    private final EvidencePhotoMapper photoMapper;
    private final EvidenceRecordMapper recordMapper;
    private final WorkOrderMapper workOrderMapper;
    private final EquipmentMapper equipmentMapper;
    private final OrderStateSupport orderStateSupport;
    private final FileStorageService fileStorageService;
    private final AppProperties appProperties;

    /** 到场打卡：扫码或定位，定位需在设备半径内 */
    @Transactional(rollbackFor = Exception.class)
    public EvidenceCheckin checkin(Long orderId, String type, BigDecimal longitude, BigDecimal latitude,
                                   String address) {
        WorkOrder order = requireOrder(orderId);
        checkWorker(order);
        if (!WorkOrderStatus.DISPATCHED.name().equals(order.getStatus())
                && !WorkOrderStatus.PROCESSING.name().equals(order.getStatus())) {
            throw new BizException("当前工单状态不允许打卡");
        }
        EvidenceCheckin checkin = new EvidenceCheckin();
        checkin.setOrderId(orderId);
        checkin.setPersonnelId(order.getMainWorkerId());
        checkin.setCheckinType(StringUtils.hasText(type) ? type : "LOCATION");
        checkin.setLongitude(longitude);
        checkin.setLatitude(latitude);
        checkin.setAddress(address);
        checkin.setCheckinTime(LocalDateTime.now());
        checkin.setValidFlag(1);

        if ("LOCATION".equals(checkin.getCheckinType())) {
            if (longitude == null || latitude == null) {
                throw new BizException("定位打卡需要授权位置信息");
            }
            Equipment equipment = order.getEquipmentId() == null ? null
                    : equipmentMapper.selectById(order.getEquipmentId());
            if (equipment == null || equipment.getLongitude() == null || equipment.getLatitude() == null) {
                throw new BizException("该设备未登记坐标，无法定位打卡，请使用扫码打卡");
            }
            Integer distance = GeoUtil.distanceMeter(longitude, latitude,
                    equipment.getLongitude(), equipment.getLatitude());
            checkin.setDistance(distance);
            if (!appProperties.isCheckinSkipDistance()
                    && distance != null && distance > appProperties.getCheckinRadiusMeter()) {
                throw new BizException("当前距离设备约 " + distance + " 米，超出 "
                        + appProperties.getCheckinRadiusMeter() + " 米打卡范围");
            }
        }
        checkinMapper.insert(checkin);

        if (WorkOrderStatus.DISPATCHED.name().equals(order.getStatus())) {
            orderStateSupport.transit(orderId, WorkOrderStatus.PROCESSING, "START", "到场打卡，开始作业");
        }
        addRecord(orderId, "PROGRESS", "到场打卡（" + ("SCAN".equals(checkin.getCheckinType()) ? "扫码" : "定位")
                + (checkin.getDistance() == null ? "" : "，距设备 " + checkin.getDistance() + " 米") + "）");
        return checkin;
    }

    /** 上传留痕照片：服务端水印 + MD5，禁止前端传入已加水印图片 */
    @Transactional(rollbackFor = Exception.class)
    public EvidencePhoto upload(Long orderId, MultipartFile file, String photoType,
                                BigDecimal longitude, BigDecimal latitude, Long itemId) {
        WorkOrder order = requireOrder(orderId);
        checkWorker(order);
        validateFile(file);
        byte[] original;
        try {
            original = file.getBytes();
        } catch (IOException e) {
            throw new BizException("照片读取失败，请重试");
        }
        LocalDateTime shootTime = LocalDateTime.now();
        String equipmentName = order.getTitle();
        if (order.getEquipmentId() != null) {
            Equipment equipment = equipmentMapper.selectById(order.getEquipmentId());
            if (equipment != null) {
                equipmentName = equipment.getName();
            }
        }
        String geoText = (longitude == null || latitude == null)
                ? "未获取" : longitude + "," + latitude;
        byte[] marked = WatermarkUtil.mark(original, shootTime, LoginUserHolder.realName(), equipmentName, geoText);

        String ext = extension(file.getOriginalFilename());
        String originalPath = fileStorageService.save(original, "photo/original", ext);
        String watermarkPath = fileStorageService.save(marked, "photo/watermark", "jpg");

        EvidencePhoto photo = new EvidencePhoto();
        photo.setOrderId(orderId);
        photo.setPhotoType(StringUtils.hasText(photoType) ? photoType : BizConst.PHOTO_OTHER);
        photo.setOriginalPath(originalPath);
        photo.setWatermarkPath(watermarkPath);
        photo.setMd5(Md5Util.of(marked));
        photo.setLongitude(longitude);
        photo.setLatitude(latitude);
        photo.setShootTime(shootTime);
        photo.setOperatorId(LoginUserHolder.get().getId());
        photo.setOperatorName(LoginUserHolder.realName());
        photo.setEquipmentName(equipmentName);
        photo.setItemId(itemId);
        photo.setCreateTime(shootTime);
        photoMapper.insert(photo);
        return photo;
    }

    /** 巡检等场景的通用留痕照片（不绑定工单） */
    @Transactional(rollbackFor = Exception.class)
    public EvidencePhoto uploadForTask(Long taskId, Long orderId, MultipartFile file, String photoType,
                                       BigDecimal longitude, BigDecimal latitude) {
        validateFile(file);
        byte[] original;
        try {
            original = file.getBytes();
        } catch (IOException e) {
            throw new BizException("照片读取失败，请重试");
        }
        LocalDateTime shootTime = LocalDateTime.now();
        String geoText = (longitude == null || latitude == null) ? "未获取" : longitude + "," + latitude;
        byte[] marked = WatermarkUtil.mark(original, shootTime, LoginUserHolder.realName(), "巡检任务", geoText);
        String ext = extension(file.getOriginalFilename());
        String originalPath = fileStorageService.save(original, "photo/original", ext);
        String watermarkPath = fileStorageService.save(marked, "photo/watermark", "jpg");

        EvidencePhoto photo = new EvidencePhoto();
        photo.setOrderId(orderId == null ? 0L : orderId);
        photo.setTaskId(taskId);
        photo.setPhotoType(StringUtils.hasText(photoType) ? photoType : BizConst.PHOTO_OTHER);
        photo.setOriginalPath(originalPath);
        photo.setWatermarkPath(watermarkPath);
        photo.setMd5(Md5Util.of(marked));
        photo.setLongitude(longitude);
        photo.setLatitude(latitude);
        photo.setShootTime(shootTime);
        photo.setOperatorId(LoginUserHolder.get().getId());
        photo.setOperatorName(LoginUserHolder.realName());
        photo.setEquipmentName("巡检任务");
        photo.setCreateTime(shootTime);
        photoMapper.insert(photo);
        return photo;
    }

    public List<EvidencePhoto> photos(Long orderId) {
        return photoMapper.selectList(new LambdaQueryWrapper<EvidencePhoto>()
                .eq(EvidencePhoto::getOrderId, orderId).orderByAsc(EvidencePhoto::getId));
    }

    public List<EvidencePhoto> photosByTask(Long taskId) {
        return photoMapper.selectList(new LambdaQueryWrapper<EvidencePhoto>()
                .eq(EvidencePhoto::getTaskId, taskId).orderByAsc(EvidencePhoto::getId));
    }

    public void deletePhoto(Long id) {
        EvidencePhoto photo = photoMapper.selectById(id);
        if (photo == null) {
            return;
        }
        if ("ADMIN".equals(LoginUserHolder.role()) || (photo.getOperatorId() != null
                && photo.getOperatorId().equals(LoginUserHolder.userId()))) {
            photoMapper.deleteById(id);
        } else {
            throw BizException.forbidden("无权删除他人上传的留痕照片");
        }
    }

    public EvidenceRecord addRecord(Long orderId, String type, String content) {
        EvidenceRecord record = new EvidenceRecord();
        record.setOrderId(orderId);
        record.setPersonnelId(LoginUserHolder.get() == null ? null : LoginUserHolder.get().getPersonnelId());
        record.setRecordType(type);
        record.setContent(content);
        record.setCreateBy(LoginUserHolder.get() == null ? null : LoginUserHolder.userId());
        record.setCreateTime(LocalDateTime.now());
        recordMapper.insert(record);
        return record;
    }

    public List<EvidenceRecord> records(Long orderId) {
        return recordMapper.selectList(new LambdaQueryWrapper<EvidenceRecord>()
                .eq(EvidenceRecord::getOrderId, orderId).orderByAsc(EvidenceRecord::getId));
    }

    public List<EvidenceCheckin> checkins(Long orderId) {
        return checkinMapper.selectList(new LambdaQueryWrapper<EvidenceCheckin>()
                .eq(EvidenceCheckin::getOrderId, orderId).orderByAsc(EvidenceCheckin::getId));
    }

    private WorkOrder requireOrder(Long orderId) {
        WorkOrder order = workOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("工单不存在");
        }
        return order;
    }

    /** 仅主责人、上报业主或管理员可操作 */
    private void checkWorker(WorkOrder order) {
        String role = LoginUserHolder.role();
        if ("ADMIN".equals(role)) {
            return;
        }
        if ("OWNER".equals(role)) {
            if (java.util.Objects.equals(order.getReporterUserId(), LoginUserHolder.userId())) {
                return;
            }
            throw BizException.forbidden("仅上报人可上传该工单照片");
        }
        Long personnelId = LoginUserHolder.personnelId();
        if (personnelId == null || !personnelId.equals(order.getMainWorkerId())) {
            throw BizException.forbidden("仅本单主责人可执行现场作业");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择照片");
        }
        long maxBytes = appProperties.getUploadMaxSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BizException("照片大小不能超过 " + appProperties.getUploadMaxSizeMb() + "MB");
        }
        String ext = extension(file.getOriginalFilename());
        if (!List.of(appProperties.getUploadAllowExt().split(",")).contains(ext)) {
            throw new BizException("仅支持 " + appProperties.getUploadAllowExt() + " 格式");
        }
    }

    private String extension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "jpg";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    /** 稽核视图：照片、清单、打卡汇总 */
    public Map<String, Object> auditView(Long orderId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkins", checkins(orderId));
        List<EvidencePhoto> photos = photos(orderId);
        List<Map<String, Object>> photoRows = new ArrayList<>();
        for (EvidencePhoto photo : photos) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", photo.getId());
            row.put("photoType", photo.getPhotoType());
            row.put("watermarkPath", photo.getWatermarkPath());
            row.put("originalPath", photo.getOriginalPath());
            row.put("md5", photo.getMd5());
            row.put("longitude", photo.getLongitude());
            row.put("latitude", photo.getLatitude());
            row.put("shootTime", photo.getShootTime());
            row.put("operatorName", photo.getOperatorName());
            row.put("equipmentName", photo.getEquipmentName());
            row.put("itemId", photo.getItemId());
            photoRows.add(row);
        }
        result.put("photos", photoRows);
        result.put("records", records(orderId));
        return result;
    }
}
