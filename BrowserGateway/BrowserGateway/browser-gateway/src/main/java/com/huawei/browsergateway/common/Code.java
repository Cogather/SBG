package com.huawei.browsergateway.common;

/**
 * TCP 通信层通用状态码常量，用于控制通道和媒体通道的 ACK 应答。
 * HTTP API 层请使用 {@link com.huawei.browsergateway.entity.ResultCode}。
 *
 * @see com.huawei.browsergateway.util.encode.Ack
 * @see com.huawei.browsergateway.util.encode.LoginResponse
 */
public class Code {

    private Code() {
        // 工具类，禁止实例化
    }

    // ---- 成功 ----

    /** 操作成功 */
    public static final int OK = 200;

    // ---- 客户端错误 ----

    /** 请求参数无效 */
    public static final int BAD_REQUEST = 400;

    /** 未授权，认证失败 */
    public static final int UNAUTHORIZED = 401;

    /** 权限不足，拒绝访问 */
    public static final int FORBIDDEN = 403;

    /** 资源未找到 */
    public static final int NOT_FOUND = 404;

    // ---- 服务端错误 ----

    /** 服务器内部错误 */
    public static final int FAILED = 500;

    /** 服务不可用 */
    public static final int SERVICE_UNAVAILABLE = 503;

    // ---- 自定义业务码 ----

    /** 会话超时或已过期 */
    public static final int SESSION_EXPIRED = 4001;

    /** Chrome 实例创建失败 */
    public static final int CHROME_CREATE_FAILED = 5001;

    /** Chrome 实例未找到 */
    public static final int CHROME_NOT_FOUND = 5002;

    /** 媒体流连接失败 */
    public static final int MEDIA_CONNECT_FAILED = 5003;

    /** 判断状态码是否表示成功 */
    public static boolean isSuccess(int code) {
        return code == OK;
    }

    /** 判断状态码是否表示失败 */
    public static boolean isError(int code) {
        return code != OK;
    }
}
