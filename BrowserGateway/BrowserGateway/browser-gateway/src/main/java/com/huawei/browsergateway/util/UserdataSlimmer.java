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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserdataSlimmer {
    private static final Logger log = LogManager.getLogger(UserdataSlimmer.class);
    // Core keywords (strict filtering rules)
    private static final List<String> LOGIN_KEYWORDS = Arrays.asList(
            "session", "token", "auth", "login", "user", "sid", "uid", "account",
            "access", "refresh", "credential", "ticket", "sign", "key"
    );
    private static final List<String> VERIFY_KEYWORDS = Arrays.asList(
            "verify", "captcha", "check", "pass", "valid", "certify", "robot",
            "human", "anti", "shield", "audit"
    );
    private static final List<String> VERIFY_PASSED_VALUES = Arrays.asList(
            "passed", "true", "success", "valid", "1", "yes", "allowed"
    );
    private static final String MUEN_APP_FLAG = "tmofamily.com";
    private static final List<String> ALL_KEYWORDS = new ArrayList<>();

    static {
        ALL_KEYWORDS.addAll(LOGIN_KEYWORDS);
        ALL_KEYWORDS.addAll(VERIFY_KEYWORDS);
    }

    /**
     * Deep slim and modify the original file in-place
     */
    public static void slimInplace(File file)  {
        long startTime = System.currentTimeMillis();
        if (!file.exists()) {
            String errorMsg = "File not found: " + file.getAbsolutePath();
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        String originalContent = FileUtil.readString(file, StandardCharsets.UTF_8);
        JSONObject originalState = JSONUtil.parseObj(originalContent);
        long originalSize = originalContent.getBytes(StandardCharsets.UTF_8).length;

        JSONArray originalCookies = originalState.getJSONArray("cookies");
        if (originalCookies == null) {
            originalCookies = new JSONArray();
        }
        JSONArray originalOrigins = originalState.getJSONArray("origins");
        if (originalOrigins == null) {
            originalOrigins = new JSONArray();
        }

        JSONArray slimCookies = slimCookies(originalCookies);
        JSONArray slimOrigins = slimOrigins(originalOrigins);

        JSONObject slimState = new JSONObject();
        slimState.set("cookies", slimCookies);
        slimState.set("origins", slimOrigins);
        String compressedJson = JSONUtil.toJsonStr(slimState).replaceAll("\\s+", "");
        long slimSize = compressedJson.getBytes(StandardCharsets.UTF_8).length;

        FileUtil.writeString(compressedJson, file, StandardCharsets.UTF_8);

        log.info("slim user data success, cost:{}ms. cookies: {} -> {}; origins: {} -> {}; size(KB): {} -> {}"
                , System.currentTimeMillis() - startTime, getArraySize(originalCookies)
                , getArraySize(slimCookies), getArraySize(originalOrigins), getArraySize(slimOrigins)
                ,originalSize/1024.0, slimSize/1024.0);
    }

    /**
     *  slim Cookies
     */
    private static JSONArray slimCookies(JSONArray originalCookies) {
        if (originalCookies == null || originalCookies.isEmpty()) {
            return new JSONArray();
        }

        long currentTimestamp = DateUtil.currentSeconds();
        JSONArray slimCookies = new JSONArray();

        for (Object obj : originalCookies) {
            JSONObject cookie = JSONUtil.parseObj(obj);
            //沐恩自研应用不进行裁剪
            if (cookie.getStr("domain", "").contains(MUEN_APP_FLAG)) {
                slimCookies.add(cookie);
                continue;
            }
            //  Must not be expired
            boolean isNotExpired = true;
            if (cookie.containsKey("expires") && !cookie.isNull("expires")) {
                try {
                    double expires = cookie.getDouble("expires");
                    isNotExpired = expires > currentTimestamp;
                } catch (Exception e) {
                    isNotExpired = true;
                }
            }
            if (!isNotExpired) continue;

            String value = cookie.getStr("value", "").trim();
            if (StrUtil.isBlank(value) || value.length() <= 1) continue;

            // Core login credential or HttpOnly
            String name = cookie.getStr("name", "").toLowerCase();
            boolean isHttpOnly = cookie.getBool("httpOnly", false);

            boolean hasLoginKey = LOGIN_KEYWORDS.stream()
                    .anyMatch(name::contains);

            if (hasLoginKey || isHttpOnly) {
                slimCookies.add(cookie);
            }
        }
        return slimCookies;
    }

    /**
     * slim Origins
     */
    private static JSONArray slimOrigins(JSONArray originalOrigins) {
        if (originalOrigins == null || originalOrigins.isEmpty()) {
            return new JSONArray();
        }

        JSONArray slimOrigins = new JSONArray();
        long currentTimestamp = DateUtil.currentSeconds();

        for (Object obj : originalOrigins) {
            JSONObject originItem = JSONUtil.parseObj(obj);
            String origin = originItem.getStr("origin", "");
            //沐恩自研应用不进行裁剪
            if (origin.contains(MUEN_APP_FLAG)) {
                slimOrigins.add(originItem);
                continue;
            }
            JSONArray originalLocal = originItem.getJSONArray("localStorage");
            if (originalLocal == null) {
                originalLocal = new JSONArray();
            }
            JSONArray slimLocal = slimLocalStorage(origin, originalLocal, currentTimestamp);

            if (!slimLocal.isEmpty()) {
                JSONObject slimOriginItem = new JSONObject();
                slimOriginItem.set("origin", origin);
                slimOriginItem.set("localStorage", slimLocal);
                slimOrigins.add(slimOriginItem);
            }
        }
        return slimOrigins;
    }

    /**
     * slim localStorage
     */
    private static JSONArray slimLocalStorage(String origin, JSONArray originalLocal, long currentTimestamp) {
        if (originalLocal == null || originalLocal.isEmpty()) {
            return new JSONArray();
        }

        JSONArray slimLocal = new JSONArray();

        for (Object obj : originalLocal) {
            JSONObject item = JSONUtil.parseObj(obj);
            String name = item.getStr("name", "").toLowerCase();
            String value = item.getStr("value", "").toLowerCase();

            if (StrUtil.isBlank(value) || value.length() <= 1) continue;

            // Core verify keyword or explicit passed status
            boolean hasKey = ALL_KEYWORDS.stream().anyMatch(name::contains);

            //tiktok无用localStorage: example:"name": "text.5dc26cf008d511e9b571e1bc0c9e23b5.WebApp_Login.en-GB"
            if (origin.contains(".tiktok.com")) {
                hasKey = hasKey && !name.contains("text");
            }
            boolean isVerifyPassed = VERIFY_PASSED_VALUES.stream().anyMatch(value::equalsIgnoreCase);

            // Not expired
            boolean isNotExpired = true;
            try {
                if (JSONUtil.isJson(value)) {
                    JSONObject valJson = JSONUtil.parseObj(value);
                    double expire = 0.0;
                    if (valJson.containsKey("expire")) {
                        expire = valJson.getDouble("expire");
                    } else if (valJson.containsKey("expires")) {
                        expire = valJson.getDouble("expires");
                    }
                    if (expire > 0) {
                        isNotExpired = expire > currentTimestamp;
                    }
                }
            } catch (Exception e) {
                isNotExpired = false;
            }

            if ((hasKey || isVerifyPassed) && isNotExpired) {
                slimLocal.add(item);
            }
        }
        return slimLocal;
    }

    private static int getArraySize(JSONArray array) {
        return array == null ? 0 : array.size();
    }
}
