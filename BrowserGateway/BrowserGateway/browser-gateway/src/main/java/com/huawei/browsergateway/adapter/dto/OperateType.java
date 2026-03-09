package com.huawei.browsergateway.adapter.dto;

/**
 * 操作类型枚举
 */
public enum OperateType {
    GET(0),
    ADD(1),
    MOD(2),
    DELETE(3),
    DOWNLOAD(4),
    UPLOAD(5),
    UPHOLD(6);

    private final int codeType;

    OperateType(int code) {this.codeType = code;}

    public int getCodeType() {return this.codeType;}
}
