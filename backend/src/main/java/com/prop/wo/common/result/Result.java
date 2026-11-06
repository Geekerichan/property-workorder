package com.prop.wo.common.result;

import lombok.Data;

/**
 * 统一响应体：所有接口返回 {code, message, data}，code=0 表示成功。
 * <p>错误码分段见 {@link ResultCode}。</p>
 */
@Data
public class Result<T> {

    /** 状态码：0 成功，其余见 {@link ResultCode} */
    private int code;
    /** 提示信息：成功或失败原因，直接展示给前端 */
    private String message;
    /** 业务数据：可为空 */
    private T data;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 成功且无数据 */
    public static <T> Result<T> ok() {
        return new Result<>(0, "操作成功", null);
    }

    /** 成功并携带数据 */
    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "操作成功", data);
    }

    /** 成功并携带自定义提示 */
    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(0, message, data);
    }

    /** 失败：指定状态码与提示 */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    /** 失败：默认业务错误码（30000） */
    public static <T> Result<T> fail(String message) {
        return new Result<>(ResultCode.BIZ_ERROR, message, null);
    }
}
