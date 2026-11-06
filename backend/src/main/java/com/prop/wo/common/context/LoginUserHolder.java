package com.prop.wo.common.context;

import cn.dev33.satoken.stp.StpUtil;
import com.prop.wo.common.exception.BizException;

/**
 * 登录用户上下文：统一从 Sa-Token Session 读取，避免各处重复解析
 */
public final class LoginUserHolder {

    private static final String KEY = "loginUser";

    private LoginUserHolder() {
    }

    public static void put(LoginUser user) {
        StpUtil.getSession().set(KEY, user);
    }

    /** 当前登录用户，未登录返回 null */
    public static LoginUser get() {
        try {
            Object value = StpUtil.getSession().get(KEY);
            return value instanceof LoginUser user ? user : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** 当前登录用户，未登录抛业务异常 */
    public static LoginUser require() {
        LoginUser user = get();
        if (user == null) {
            throw BizException.unauthorized("登录已失效，请重新登录");
        }
        return user;
    }

    public static Long userId() {
        return require().getId();
    }

    public static String role() {
        LoginUser user = get();
        return user == null ? null : user.getRole();
    }

    public static boolean isAdmin() {
        LoginUser user = get();
        return user != null && user.isAdmin();
    }

    public static Long companyId() {
        LoginUser user = get();
        return user == null ? null : user.getCompanyId();
    }

    public static Long personnelId() {
        LoginUser user = get();
        return user == null ? null : user.getPersonnelId();
    }

    public static String realName() {
        LoginUser user = get();
        return user == null ? "系统" : user.getRealName();
    }
}
