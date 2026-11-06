package com.prop.wo.modules.system.service;

import com.prop.wo.common.context.LoginUserHolder;
import com.prop.wo.modules.system.entity.SysLogLogin;
import com.prop.wo.modules.system.entity.SysLogOper;
import com.prop.wo.modules.system.mapper.SysLogLoginMapper;
import com.prop.wo.modules.system.mapper.SysLogOperMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 审计日志：登录日志与关键操作日志统一写入口
 */
@Service
@RequiredArgsConstructor
public class OperLogService {

    private final SysLogOperMapper operMapper;
    private final SysLogLoginMapper loginMapper;

    public void record(String module, String action, Long bizId, String content) {
        SysLogOper log = new SysLogOper();
        log.setModule(module);
        log.setAction(action);
        log.setBizId(bizId);
        log.setContent(content);
        log.setIp(currentIp());
        log.setOperatorId(LoginUserHolder.get() == null ? null : LoginUserHolder.get().getId());
        log.setOperatorName(LoginUserHolder.realName());
        log.setOperTime(LocalDateTime.now());
        operMapper.insert(log);
    }

    public void recordLogin(String username, boolean success, String message) {
        SysLogLogin log = new SysLogLogin();
        log.setUsername(username);
        log.setIp(currentIp());
        log.setUserAgent(currentUserAgent());
        log.setSuccess(success ? 1 : 0);
        log.setMessage(message);
        log.setLoginTime(LocalDateTime.now());
        loginMapper.insert(log);
    }

    private String currentIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String currentUserAgent() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String ua = request.getHeader("User-Agent");
        return ua != null && ua.length() > 250 ? ua.substring(0, 250) : ua;
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
