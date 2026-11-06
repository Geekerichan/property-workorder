package com.prop.wo.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等注解：配合请求头 Idempotency-Key 使用，5 分钟内相同键拒绝重复提交
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** 预留字段：当前判重仅依赖请求头 Idempotency-Key */
    String value() default "";
}
