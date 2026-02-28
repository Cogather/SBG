package com.huawei.browsergateway.entity;

public enum ResultCode {
    SUCCESS(200, "success"),
    FAIL(500, "system error!"),
    VALIDATE_ERROR(400, "invalid parameter"),
    NOT_FOUND(404, "not found"),
    PERMISSION_DENIED(403, "permission denied");

    private final int code;
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