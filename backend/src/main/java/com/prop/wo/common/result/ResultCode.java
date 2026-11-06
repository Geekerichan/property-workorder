package com.prop.wo.common.result;

/**
 * 错误码分段：10000 参数 / 20000 认证 / 30000 业务 / 40000 权限 / 50000 系统
 */
public interface ResultCode {

    int SUCCESS = 0;
    int PARAM_ERROR = 10000;
    int UNAUTHORIZED = 20000;
    int BIZ_ERROR = 30000;
    int FORBIDDEN = 40000;
    int SYSTEM_ERROR = 50000;
}
