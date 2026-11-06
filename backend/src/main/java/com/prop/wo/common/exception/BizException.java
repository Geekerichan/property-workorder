package com.prop.wo.common.exception;

import com.prop.wo.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常：message 直接展示给前端，禁止携带堆栈
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.BIZ_ERROR;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException of(String message) {
        return new BizException(message);
    }

    public static BizException forbidden(String message) {
        return new BizException(ResultCode.FORBIDDEN, message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(ResultCode.UNAUTHORIZED, message);
    }
}
