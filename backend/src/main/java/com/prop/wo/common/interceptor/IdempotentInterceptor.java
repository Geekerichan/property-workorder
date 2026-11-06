package com.prop.wo.common.interceptor;

import com.prop.wo.common.annotation.Idempotent;
import com.prop.wo.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * 幂等拦截：请求头 Idempotency-Key 相同且在 5 分钟内重复提交直接拒绝
 */
@RequiredArgsConstructor
public class IdempotentInterceptor implements HandlerInterceptor {

    private static final String HEADER = "Idempotency-Key";
    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method) || method.getMethodAnnotation(Idempotent.class) == null) {
            return true;
        }
        String key = request.getHeader(HEADER);
        if (key == null || key.isBlank()) {
            return true;
        }
        String redisKey = "idem:" + method.getBeanType().getSimpleName() + ":" + key.trim();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", Duration.ofMinutes(5));
        if (Boolean.FALSE.equals(success)) {
            throw BizException.of("操作已提交，请勿重复点击");
        }
        return true;
    }
}
