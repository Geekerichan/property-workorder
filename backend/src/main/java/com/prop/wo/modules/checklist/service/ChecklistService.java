package com.prop.wo.modules.checklist.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.prop.wo.common.constant.BizConst;
import com.prop.wo.common.exception.BizException;
import com.prop.wo.modules.checklist.entity.ChecklistItem;
import com.prop.wo.modules.checklist.entity.ChecklistTemplate;
import com.prop.wo.modules.checklist.entity.WorkOrderChecklist;
import com.prop.wo.modules.checklist.mapper.ChecklistItemMapper;
import com.prop.wo.modules.checklist.mapper.ChecklistTemplateMapper;
import com.prop.wo.modules.checklist.mapper.WorkOrderChecklistMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 作业清单：模板实例化、逐项勾选、强制拍照项校验
 */
@Service
@RequiredArgsConstructor
public class ChecklistService {

    private final ChecklistTemplateMapper templateMapper;
    private final ChecklistItemMapper itemMapper;
    private final WorkOrderChecklistMapper orderChecklistMapper;

    public List<ChecklistTemplate> templates(String facilityType) {
        return templateMapper.selectList(new LambdaQueryWrapper<ChecklistTemplate>()
                .eq(StringUtils.hasText(facilityType), ChecklistTemplate::getFacilityType, facilityType)
                .orderByAsc(ChecklistTemplate::getId));
    }

    public List<ChecklistItem> templateItems(Long templateId) {
        return itemMapper.selectList(new LambdaQueryWrapper<ChecklistItem>()
                .eq(ChecklistItem::getTemplateId, templateId).orderByAsc(ChecklistItem::getSortNo));
    }

    @Transactional(rollbackFor = Exception.class)
    public ChecklistTemplate saveTemplate(ChecklistTemplate template, List<ChecklistItem> items) {
        if (!StringUtils.hasText(template.getName()) || !StringUtils.hasText(template.getFacilityType())) {
            throw new BizException("请填写模板名称与设施类型");
        }
        if (template.getId() == null) {
            template.setStatus(template.getStatus() == null ? "ENABLED" : template.getStatus());
            templateMapper.insert(template);
        } else {
            templateMapper.updateById(template);
            itemMapper.delete(new LambdaQueryWrapper<ChecklistItem>()
                    .eq(ChecklistItem::getTemplateId, template.getId()));
        }
        if (items != null) {
            int sort = 1;
            for (ChecklistItem item : items) {
                item.setId(null);
                item.setTemplateId(template.getId());
                item.setSortNo(sort++);
                item.setRequirePhoto(item.getRequirePhoto() == null ? 0 : item.getRequirePhoto());
                item.setRequireValue(item.getRequireValue() == null ? 0 : item.getRequireValue());
                itemMapper.insert(item);
            }
        }
        return template;
    }

    /**
     * 为工单生成清单实例：优先使用指定模板，其次按设施类型匹配，最后使用通用兜底清单
     */
    @Transactional(rollbackFor = Exception.class)
    public void instantiate(Long orderId, Long templateId, String facilityType) {
        Long exists = orderChecklistMapper.selectCount(new LambdaQueryWrapper<WorkOrderChecklist>()
                .eq(WorkOrderChecklist::getOrderId, orderId));
        if (exists != null && exists > 0) {
            return;
        }
        ChecklistTemplate template = null;
        if (templateId != null) {
            template = templateMapper.selectById(templateId);
        }
        if (template == null) {
            template = templateMapper.selectOne(new LambdaQueryWrapper<ChecklistTemplate>()
                    .eq(ChecklistTemplate::getFacilityType, facilityType)
                    .eq(ChecklistTemplate::getStatus, "ENABLED")
                    .orderByAsc(ChecklistTemplate::getId).last("limit 1"));
        }
        List<ChecklistItem> items = template == null ? genericItems() : templateItems(template.getId());
        int sort = 1;
        for (ChecklistItem item : items) {
            WorkOrderChecklist row = new WorkOrderChecklist();
            row.setOrderId(orderId);
            row.setItemId(item.getId() == null ? 0L : item.getId());
            row.setGroupName(item.getGroupName());
            row.setItemName(item.getItemName());
            row.setRequirePhoto(item.getRequirePhoto());
            row.setSortNo(sort++);
            row.setResult(null);
            orderChecklistMapper.insert(row);
        }
    }

    /** 通用兜底清单（无匹配模板时保证流程不断链） */
    private List<ChecklistItem> genericItems() {
        List<ChecklistItem> items = new ArrayList<>();
        items.add(item("现场确认", "作业前安全确认（断电/挂牌/围挡）", 1));
        items.add(item("现场确认", "设备运行状态检查", 0));
        items.add(item("作业过程", "作业内容执行并记录", 0));
        items.add(item("作业完成", "现场清理与功能复位测试", 1));
        return items;
    }

    private ChecklistItem item(String group, String name, int requirePhoto) {
        ChecklistItem item = new ChecklistItem();
        item.setGroupName(group);
        item.setItemName(name);
        item.setRequirePhoto(requirePhoto);
        item.setRequireValue(0);
        return item;
    }

    public List<WorkOrderChecklist> items(Long orderId) {
        return orderChecklistMapper.selectList(new LambdaQueryWrapper<WorkOrderChecklist>()
                .eq(WorkOrderChecklist::getOrderId, orderId).orderByAsc(WorkOrderChecklist::getSortNo));
    }

    /** 按分组返回，便于移动端折叠展示 */
    public List<Map<String, Object>> groupedItems(Long orderId) {
        Map<String, List<WorkOrderChecklist>> groups = new LinkedHashMap<>();
        for (WorkOrderChecklist item : items(orderId)) {
            groups.computeIfAbsent(item.getGroupName() == null ? "作业项目" : item.getGroupName(),
                    k -> new ArrayList<>()).add(item);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        groups.forEach((group, list) -> {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("groupName", group);
            node.put("items", list);
            result.add(node);
        });
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveResults(Long orderId, List<ChecklistResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }
        for (ChecklistResult req : results) {
            WorkOrderChecklist row = orderChecklistMapper.selectById(req.itemId());
            if (row == null || !row.getOrderId().equals(orderId)) {
                throw new BizException("作业清单项不存在");
            }
            row.setResult(req.result() == null ? "NORMAL" : req.result());
            row.setItemValue(req.value());
            row.setRemark(req.remark());
            row.setPhotoIds(req.photoIds());
            orderChecklistMapper.updateById(row);
        }
    }

    /** 提交前校验：全部勾选 + 强制拍照项必须有照片 */
    public void validateCanSubmit(Long orderId) {
        List<WorkOrderChecklist> items = items(orderId);
        if (items.isEmpty()) {
            throw new BizException("该工单尚未生成作业清单");
        }
        List<String> missing = new ArrayList<>();
        for (WorkOrderChecklist item : items) {
            if (!StringUtils.hasText(item.getResult())) {
                missing.add(item.getItemName() + "（未勾选）");
            } else if (Integer.valueOf(1).equals(item.getRequirePhoto())
                    && !StringUtils.hasText(item.getPhotoIds())) {
                missing.add(item.getItemName() + "（强制拍照项缺照片）");
            }
        }
        if (!missing.isEmpty()) {
            throw new BizException("作业清单未完成：" + String.join("；", missing));
        }
    }

    /** 清单完成度（用于详情与稽核展示） */
    public Map<String, Object> completeness(Long orderId) {
        List<WorkOrderChecklist> items = items(orderId);
        long done = items.stream().filter(i -> StringUtils.hasText(i.getResult())).count();
        long photoRequired = items.stream().filter(i -> Integer.valueOf(1).equals(i.getRequirePhoto())).count();
        long photoDone = items.stream().filter(i -> Integer.valueOf(1).equals(i.getRequirePhoto())
                && StringUtils.hasText(i.getPhotoIds())).count();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", items.size());
        result.put("done", done);
        result.put("percent", items.isEmpty() ? 0 : Math.round(done * 100.0 / items.size()));
        result.put("photoRequired", photoRequired);
        result.put("photoDone", photoDone);
        result.put("allDone", items.size() > 0 && done == items.size() && photoDone == photoRequired);
        return result;
    }

    public record ChecklistResult(Long itemId, String result, String value, String remark, String photoIds) {
    }

    /** 模板是否适用于该设施类型（供计划引擎选择） */
    public ChecklistTemplate firstByFacility(String facilityType) {
        return templateMapper.selectOne(new LambdaQueryWrapper<ChecklistTemplate>()
                .eq(ChecklistTemplate::getFacilityType, facilityType)
                .eq(ChecklistTemplate::getStatus, "ENABLED")
                .orderByAsc(ChecklistTemplate::getId).last("limit 1"));
    }

    /** 电梯等强制拍照项提示文案 */
    public String mandatoryHint(String facilityType) {
        return switch (facilityType == null ? "" : facilityType) {
            case BizConst.FACILITY_ELEVATOR -> "电梯强制拍照项：制动器、限速器、门系统";
            case BizConst.FACILITY_WATER -> "给排水强制拍照项：水泵机械密封、水箱人孔、阀门井";
            case BizConst.FACILITY_POWER -> "供配电强制拍照项：母排连接、开关触头、接地电阻测试";
            default -> "请按清单要求拍摄关键点位（人员出镜、场景可见）";
        };
    }
}
