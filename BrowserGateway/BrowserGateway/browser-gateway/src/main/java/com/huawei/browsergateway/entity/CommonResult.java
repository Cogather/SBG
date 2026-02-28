package com.huawei.browsergateway.entity;

public class CommonResult<T> {
    /**
     * 状态码 (默认 SUCCESS = 200)
     */
    private int code;

    /**
     * 响应消息 (默认 SUCCESS_MSG = "成功")
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 默认成功响应构造方法
     */
    private CommonResult() {
        this.code = ResultCode.SUCCESS.getCode();
        this.message = ResultCode.SUCCESS.getMessage();
    }

    /**
     * 成功响应构造方法 (带数据)
     *
     * @param data 返回数据
     */
    private CommonResult(T data) {
        this.code = ResultCode.SUCCESS.getCode();
        this.message = ResultCode.SUCCESS.getMessage();
        this.data = data;
    }

    /**
     * 失败响应构造方法 (带错误代码和消息)
     *
     * @param code    错误码
     * @param message 错误信息
     */
    private CommonResult(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 成功响应
     *
     * @param <T> 泛型
     * @return 公共结果
     */
    public static <T> CommonResult<T> success() {
        return new CommonResult<>();
    }

    /**
     * 成功响应 (带数据)
     *
     * @param data 返回数据
     * @param <T>  泛型
     * @return 公共结果
     */
    public static <T> CommonResult<T> success(T data) {
        return new CommonResult<>(data);
    }

    /**
     * 失败响应
     *
     * @param code    错误码
     * @param message 错误信息
     * @param <T>     泛型
     * @return 公共结果
     */
    public static <T> CommonResult<T> error(int code, String message) {
        return new CommonResult<>(code, message);
    }


    /**
     * 失败响应
     *
     * @param errorCode 错误码枚举
     * @param <T>     泛型
     * @return 公共结果
     */
    public static <T> CommonResult<T> error(ResultCode errorCode) {
        return new CommonResult<>(errorCode.getCode(), errorCode.getMessage());
    }

    // Getter and Setter
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "CommonResult{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}