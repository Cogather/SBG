package com.huawei.browsergateway.common.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;

/**
 * 用户ID工具类
 * 基于IMEI+IMSI生成用户ID
 */
public class UserIdUtil {
    
    /**
     * 生成用户ID
     * 基于IMEI和IMSI生成唯一用户标识
     * 
     * @param imei IMEI（国际移动设备识别码）
     * @param imsi IMSI（国际移动用户识别码）
     * @return 用户ID（MD5哈希值）
     */
    public static String generateUserId(String imei, String imsi) {
        if (StrUtil.isBlank(imei) && StrUtil.isBlank(imsi)) {
            throw new IllegalArgumentException("IMEI和IMSI不能同时为空");
        }
        
        // 如果IMEI或IMSI为空，使用空字符串代替
        String imeiValue = StrUtil.isBlank(imei) ? "" : imei.trim();
        String imsiValue = StrUtil.isBlank(imsi) ? "" : imsi.trim();
        
        // 组合IMEI和IMSI，使用冒号分隔
        String combined = imeiValue + ":" + imsiValue;
        
        // 使用MD5生成32位哈希值作为用户ID
        return DigestUtil.md5Hex(combined);
    }
    
    /**
     * 验证用户ID格式
     * 
     * @param userId 用户ID
     * @return 是否为有效的用户ID格式（32位MD5）
     */
    public static boolean isValidUserId(String userId) {
        if (StrUtil.isBlank(userId)) {
            return false;
        }
        // MD5哈希值为32位十六进制字符串
        return userId.matches("^[a-f0-9]{32}$");
    }
}
