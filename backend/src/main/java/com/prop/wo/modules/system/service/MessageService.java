package com.prop.wo.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.prop.wo.common.entity.PageQuery;
import com.prop.wo.common.exception.BizException;
import com.prop.wo.common.result.PageResult;
import com.prop.wo.modules.system.entity.SysMessage;
import com.prop.wo.modules.system.mapper.SysMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 站内信 + WebSocket 实时推送（站内信兜底，不依赖推送在线）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final SysMessageMapper messageMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public void send(Long userId, String msgType, String title, String content, String bizType, Long bizId) {
        if (userId == null) {
            return;
        }
        SysMessage message = new SysMessage();
        message.setUserId(userId);
        message.setMsgType(msgType);
        message.setTitle(title);
        message.setContent(content);
        message.setBizType(bizType);
        message.setBizId(bizId);
        message.setReadFlag(0);
        messageMapper.insert(message);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", message.getId());
        payload.put("msgType", msgType);
        payload.put("title", title);
        payload.put("content", content);
        payload.put("bizType", bizType);
        payload.put("bizId", bizId);
        payload.put("createTime", message.getCreateTime());
        try {
            messagingTemplate.convertAndSend("/topic/user/" + userId, payload);
        } catch (Exception e) {
            log.warn("WebSocket 推送失败 userId={}", userId, e);
        }
    }

    public PageResult<SysMessage> page(Long userId, Integer readFlag, PageQuery query) {
        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getUserId, userId)
                .orderByDesc(SysMessage::getId);
        if (readFlag != null) {
            wrapper.eq(SysMessage::getReadFlag, readFlag);
        }
        Page<SysMessage> page = messageMapper.selectPage(new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        return PageResult.of(page);
    }

    public long unreadCount(Long userId) {
        return messageMapper.selectCount(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getUserId, userId).eq(SysMessage::getReadFlag, 0));
    }

    public void markRead(Long userId, Long id) {
        SysMessage message = messageMapper.selectById(id);
        if (message == null || !message.getUserId().equals(userId)) {
            throw BizException.forbidden("消息不存在或无权操作");
        }
        message.setReadFlag(1);
        message.setReadTime(LocalDateTime.now());
        messageMapper.updateById(message);
    }

    public void markAllRead(Long userId) {
        SysMessage update = new SysMessage();
        update.setReadFlag(1);
        update.setReadTime(LocalDateTime.now());
        messageMapper.update(update, new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getUserId, userId).eq(SysMessage::getReadFlag, 0));
    }
}
