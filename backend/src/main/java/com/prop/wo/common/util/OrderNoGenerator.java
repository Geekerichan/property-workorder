package com.prop.wo.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 工单号生成：WO + yyyyMMdd + 6 位当日序列（Redis 自增，数据库唯一索引兜底）
 */
@Component
@RequiredArgsConstructor
public class OrderNoGenerator {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final StringRedisTemplate redisTemplate;

    public String next() {
        String day = LocalDate.now().format(DAY_FMT);
        String key = "wo:seq:" + day;
        Long seq = redisTemplate.opsForValue().increment(key);
        if (seq != null && seq == 1L) {
            redisTemplate.expire(key, Duration.ofDays(2));
        }
        return "WO" + day + String.format("%06d", seq == null ? 1 : seq);
    }

    public String nextTaskNo() {
        String day = LocalDate.now().format(DAY_FMT);
        String key = "task:seq:" + day;
        Long seq = redisTemplate.opsForValue().increment(key);
        if (seq != null && seq == 1L) {
            redisTemplate.expire(key, Duration.ofDays(2));
        }
        return "XJ" + day + String.format("%04d", seq == null ? 1 : seq);
    }

    public String nextContractNo() {
        String day = LocalDate.now().format(DAY_FMT);
        String key = "contract:seq:" + day;
        Long seq = redisTemplate.opsForValue().increment(key);
        if (seq != null && seq == 1L) {
            redisTemplate.expire(key, Duration.ofDays(2));
        }
        return "HT" + day + String.format("%04d", seq == null ? 1 : seq);
    }
}
