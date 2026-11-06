package com.prop.wo.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色鉴权注解：标注在 Controller 类或方法上，任一角色匹配即放行
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /** 允许的角色编码数组（如 {"ADMIN","COMPANY"}），任一匹配即放行 */
    String[] value();
}
