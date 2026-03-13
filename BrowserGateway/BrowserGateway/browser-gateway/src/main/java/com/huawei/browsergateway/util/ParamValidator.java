package com.huawei.browsergateway.util;

import com.huawei.browsergateway.entity.request.DeleteUserDataRequest;
import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.entity.request.LoadExtensionRequest;

import java.util.regex.Pattern;

/**
 * API 入参校验工具
 * 提供非空、IMEI/IMSI 格式及扩展请求必填字段校验
 */
public final class ParamValidator {

    /** IMEI：14 或 15 位数字 */
    private static final Pattern IMEI_PATTERN = Pattern.compile("^\\d{14,15}$");
    /** IMSI：15 位数字 */
    private static final Pattern IMSI_PATTERN = Pattern.compile("^\\d{15}$");

    private ParamValidator() {}

    /**
     * 校验删除用户数据请求：非空、imei/imsi 非空且格式正确
     *
     * @param param 请求体，不可为 null
     * @return 校验通过返回 null，否则返回错误描述
     */
    public static String validateDeleteUserDataRequest(DeleteUserDataRequest param) {
        if (param == null) {
            return "request body is required";
        }
        if (isBlank(param.getImei())) {
            return "imei is required";
        }
        if (isBlank(param.getImsi())) {
            return "imsi is required";
        }
        if (!IMEI_PATTERN.matcher(param.getImei().trim()).matches()) {
            return "imei must be 14 or 15 digits";
        }
        if (!IMSI_PATTERN.matcher(param.getImsi().trim()).matches()) {
            return "imsi must be 15 digits";
        }
        return null;
    }

    /**
     * 校验初始化浏览器请求：非空、imei/imsi 非空且格式正确
     *
     * @param param 请求体，不可为 null
     * @return 校验通过返回 null，否则返回错误描述
     */
    public static String validateInitBrowserRequest(InitBrowserRequest param) {
        if (param == null) {
            return "request body is required";
        }
        if (isBlank(param.getImei())) {
            return "imei is required";
        }
        if (isBlank(param.getImsi())) {
            return "imsi is required";
        }
        if (!IMEI_PATTERN.matcher(param.getImei().trim()).matches()) {
            return "imei must be 14 or 15 digits";
        }
        if (!IMSI_PATTERN.matcher(param.getImsi().trim()).matches()) {
            return "imsi must be 15 digits";
        }
        return null;
    }

    /**
     * 校验加载扩展请求：非空、bucketName 与 extensionFilePath 非空
     *
     * @param param 请求体，不可为 null
     * @return 校验通过返回 null，否则返回错误描述
     */
    public static String validateLoadExtensionRequest(LoadExtensionRequest param) {
        if (param == null) {
            return "request body is required";
        }
        if (isBlank(param.getBucketName())) {
            return "bucketName is required";
        }
        if (isBlank(param.getExtensionFilePath())) {
            return "extensionFilePath is required";
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
