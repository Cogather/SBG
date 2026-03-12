package com.huawei.browsergateway.entity;

/**
 * 结果码枚举
 */
public enum ResultCode {
    /** 成功 */
    SUCCESS(200, "success"),
    /** 系统错误 */
    FAIL(500, "system error!"),
    /** 参数校验失败 */
    VALIDATE_ERROR(400, "invalid parameter"),
    /** 资源不存在 */
    NOT_FOUND(404, "not found"),
    /** 权限不足 */
    PERMISSION_DENIED(403, "permission denied");

    /** HTTP状态码 */
    private final int code;
    /** 响应消息 */
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
