package com.prop.wo.common.interceptor;

import com.prop.wo.common.annotation.RequireRole;
import com.prop.wo.common.context.LoginUser;
import com.prop.wo.common.context.LoginUserHolder;
import com.prop.wo.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * 角色注解拦截：ADMIN 拥有全部权限；其余角色需命中注解声明
 */
public class RoleAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        RequireRole requireRole = method.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = method.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole == null) {
            return true;
        }
        LoginUser user = LoginUserHolder.require();
        if (user.isAdmin()) {
            return true;
        }
        boolean matched = Arrays.asList(requireRole.value()).contains(user.getRole());
        if (!matched) {
            throw BizException.forbidden("当前账号无权执行该操作");
        }
        return true;
    }
}
