package com.prop.wo.modules.workorder.service;

import com.prop.wo.common.context.LoginUserHolder;
import com.prop.wo.common.enums.WorkOrderStatus;
import com.prop.wo.common.exception.BizException;
import com.prop.wo.modules.workorder.entity.WorkOrder;
import com.prop.wo.modules.workorder.entity.WorkOrderLog;
import com.prop.wo.modules.workorder.mapper.WorkOrderLogMapper;
import com.prop.wo.modules.workorder.mapper.WorkOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 工单状态迁移统一入口：校验单向流转、条件更新、写流转日志
 */
@Component
@RequiredArgsConstructor
public class OrderStateSupport {

    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderLogMapper logMapper;

    public WorkOrder transit(Long orderId, WorkOrderStatus target, String action, String reason) {
        WorkOrder order = workOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("工单不存在");
        }
        WorkOrderStatus current = WorkOrderStatus.of(order.getStatus());
        if (current == null) {
            throw new BizException("工单状态异常");
        }
        if (!current.canTransitTo(target)) {
            throw new BizException("工单当前为「" + current.getLabel() + "」，不允许变更为「" + target.getLabel() + "」");
        }
        int updated = workOrderMapper.transitStatus(orderId, current.name(), target.name());
        if (updated == 0) {
            throw new BizException("工单状态已被其他操作变更，请刷新后重试");
        }
        order.setStatus(target.name());
        writeLog(order, action, current.name(), target.name(), reason);
        return order;
    }

    public void writeLog(WorkOrder order, String action, String from, String to, String reason) {
        WorkOrderLog log = new WorkOrderLog();
        log.setOrderId(order.getId());
        log.setAction(action);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setReason(reason);
        log.setOperatorId(LoginUserHolder.get() == null ? null : LoginUserHolder.get().getId());
        log.setOperatorName(LoginUserHolder.get() == null ? "系统" : LoginUserHolder.realName());
        log.setCreateTime(LocalDateTime.now());
        logMapper.insert(log);
    }

    /** 无状态变更的操作日志 */
    public void writeActionLog(WorkOrder order, String action, String reason) {
        writeLog(order, action, order.getStatus(), order.getStatus(), reason);
    }
}
