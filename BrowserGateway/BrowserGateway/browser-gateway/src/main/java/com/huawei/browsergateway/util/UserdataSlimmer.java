package com.huawei.browsergateway.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 浏览器用户数据精简工具类
 *
 * <p>对 Playwright/Chromium 导出的 storageState JSON 文件进行裁剪，
 * 仅保留登录凭证相关的 Cookie 和 localStorage 条目，减少文件体积。
 */
public final class UserdataSlimmer {

    private static final Logger log = LogManager.getLogger(UserdataSlimmer.class);

    /** 沐恩自研应用域名标识，命中时跳过裁剪 */
    private static final String MUEN_APP_FLAG = "tmofamily.com";

    /** 登录凭证相关关键词（Cookie name 匹配） */
    private static final List<String> LOGIN_KEYWORDS = Collections.unmodifiableList(Arrays.asList(
            "session", "token", "auth", "login", "user", "sid", "uid", "account",
            "access", "refresh", "credential", "ticket", "sign", "key"
    ));

    /** 人机验证相关关键词（localStorage name 匹配） */
    private static final List<String> VERIFY_KEYWORDS = Collections.unmodifiableList(Arrays.asList(
            "verify", "captcha", "check", "pass", "valid", "certify", "robot",
            "human", "anti", "shield", "audit"
    ));

    /** 验证通过的 value 值集合 */
    private static final List<String> VERIFY_PASSED_VALUES = Collections.unmodifiableList(Arrays.asList(
            "passed", "true", "success", "valid", "1", "yes", "allowed"
    ));

    /** 登录 + 验证关键词合集 */
    private static final List<String> ALL_KEYWORDS;

    static {
        String[] combined = new String[LOGIN_KEYWORDS.size() + VERIFY_KEYWORDS.size()];
        LOGIN_KEYWORDS.toArray(combined);
        for (int i = 0; i < VERIFY_KEYWORDS.size(); i++) {
            combined[LOGIN_KEYWORDS.size() + i] = VERIFY_KEYWORDS.get(i);
        }
        ALL_KEYWORDS = Collections.unmodifiableList(Arrays.asList(combined));
    }

    private UserdataSlimmer() {}

    /**
     * 原地精简用户数据文件（覆盖写入）
     *
     * @param file 待精简的 storageState JSON 文件
     * @throws RuntimeException 文件不存在时抛出
     */
    public static void slimInplace(File file) {
        if (!file.exists()) {
            String msg = "File not found: " + file.getAbsolutePath();
            log.error(msg);
            throw new RuntimeException(msg);
        }

        long startTime = System.currentTimeMillis();
        String originalContent = FileUtil.readString(file, StandardCharsets.UTF_8);
        JSONObject state = JSONUtil.parseObj(originalContent);
        long originalSize = originalContent.getBytes(StandardCharsets.UTF_8).length;

        JSONArray originalCookies = nullSafe(state.getJSONArray("cookies"));
        JSONArray originalOrigins = nullSafe(state.getJSONArray("origins"));

        JSONArray slimCookies = slimCookies(originalCookies);
        JSONArray slimOrigins = slimOrigins(originalOrigins);

        JSONObject slimState = new JSONObject();
        slimState.set("cookies", slimCookies);
        slimState.set("origins", slimOrigins);
        String output = JSONUtil.toJsonStr(slimState).replaceAll("\\s+", "");
        long slimSize = output.getBytes(StandardCharsets.UTF_8).length;

        FileUtil.writeString(output, file, StandardCharsets.UTF_8);

        log.info("slim user data success, cost:{}ms. cookies:{}->{} origins:{}->{} size(KB):{}->{}"
                , System.currentTimeMillis() - startTime
                , size(originalCookies), size(slimCookies)
                , size(originalOrigins), size(slimOrigins)
                , originalSize / 1024.0, slimSize / 1024.0);
    }

    // ---- Cookie 裁剪 ----

    private static JSONArray slimCookies(JSONArray cookies) {
        if (cookies == null || cookies.isEmpty()) return new JSONArray();

        long now = DateUtil.currentSeconds();
        JSONArray result = new JSONArray();
        for (Object obj : cookies) {
            JSONObject cookie = JSONUtil.parseObj(obj);
            // 沐恩自研应用不裁剪
            if (cookie.getStr("domain", "").contains(MUEN_APP_FLAG)) {
                result.add(cookie);
                continue;
            }
            if (!isNotExpiredCookie(cookie, now)) continue;

            String value = cookie.getStr("value", "").trim();
            if (StrUtil.isBlank(value) || value.length() <= 1) continue;

            String name = cookie.getStr("name", "").toLowerCase();
            boolean isLoginKey = LOGIN_KEYWORDS.stream().anyMatch(name::contains);
            boolean isHttpOnly = cookie.getBool("httpOnly", false);
            if (isLoginKey || isHttpOnly) {
                result.add(cookie);
            }
        }
        return result;
    }

    /** 判断 Cookie 是否未过期 */
    private static boolean isNotExpiredCookie(JSONObject cookie, long now) {
        if (!cookie.containsKey("expires") || cookie.isNull("expires")) return true;
        try {
            return cookie.getDouble("expires") > now;
        } catch (Exception e) {
            return true;
        }
    }

    // ---- Origin / localStorage 裁剪 ----

    private static JSONArray slimOrigins(JSONArray origins) {
        if (origins == null || origins.isEmpty()) return new JSONArray();

        long now = DateUtil.currentSeconds();
        JSONArray result = new JSONArray();
        for (Object obj : origins) {
            JSONObject originItem = JSONUtil.parseObj(obj);
            String origin = originItem.getStr("origin", "");
            // 沐恩自研应用不裁剪
            if (origin.contains(MUEN_APP_FLAG)) {
                result.add(originItem);
                continue;
            }
            JSONArray slimLocal = slimLocalStorage(origin, nullSafe(originItem.getJSONArray("localStorage")), now);
            if (!slimLocal.isEmpty()) {
                JSONObject slimItem = new JSONObject();
                slimItem.set("origin", origin);
                slimItem.set("localStorage", slimLocal);
                result.add(slimItem);
            }
        }
        return result;
    }

    private static JSONArray slimLocalStorage(String origin, JSONArray local, long now) {
        if (local == null || local.isEmpty()) return new JSONArray();

        JSONArray result = new JSONArray();
        for (Object obj : local) {
            JSONObject item = JSONUtil.parseObj(obj);
            String name = item.getStr("name", "").toLowerCase();
            String value = item.getStr("value", "").toLowerCase();

            if (StrUtil.isBlank(value) || value.length() <= 1) continue;

            boolean hasKey = ALL_KEYWORDS.stream().anyMatch(name::contains);
            // TikTok 特殊处理：排除 text.* 格式的无效条目
            if (origin.contains(".tiktok.com")) {
                hasKey = hasKey && !name.contains("text");
            }

            boolean isVerifyPassed = VERIFY_PASSED_VALUES.stream().anyMatch(value::equalsIgnoreCase);
            if ((hasKey || isVerifyPassed) && isNotExpiredLocalStorage(value, now)) {
                result.add(item);
            }
        }
        return result;
    }

    /** 判断 localStorage 条目是否未过期（value 为 JSON 且含 expire/expires 字段时检查） */
    private static boolean isNotExpiredLocalStorage(String value, long now) {
        try {
            if (!JSONUtil.isJson(value)) return true;
            JSONObject valJson = JSONUtil.parseObj(value);
            double expire = 0.0;
            if (valJson.containsKey("expire")) {
                expire = valJson.getDouble("expire");
            } else if (valJson.containsKey("expires")) {
                expire = valJson.getDouble("expires");
            }
            return expire <= 0 || expire > now;
        } catch (Exception e) {
            return false;
        }
    }

    private static JSONArray nullSafe(JSONArray array) {
        return array != null ? array : new JSONArray();
    }

    private static int size(JSONArray array) {
        return array == null ? 0 : array.size();
    }
}
