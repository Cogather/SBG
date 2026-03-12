package com.huawei.browsergateway.service.impl;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import com.huawei.browsergateway.config.Config;
import com.huawei.browsergateway.entity.alarm.AlarmEvent;
import com.huawei.browsergateway.entity.browser.ChromeRecordConfig;
import com.huawei.browsergateway.entity.enums.AlarmEnum;
import com.huawei.browsergateway.entity.enums.BrowserStatus;
import com.huawei.browsergateway.entity.event.EventInfo;
import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.service.IAlarm;
import com.huawei.browsergateway.service.IChromeSet;
import com.huawei.browsergateway.service.ICse;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.util.HttpUtil;
import com.huawei.browsergateway.util.ReportEventUtil;
import com.huawei.browsergateway.util.UserIdUtil;
import com.moon.cloud.browser.sdk.core.HWContext;
import com.moon.cloud.browser.sdk.model.pojo.ChromeParams;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.http.client.methods.HttpPost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * 远程服务交互实现，封装与 GIDS 服务的所有 HTTP 通信，
 * 并负责浏览器实例的创建流程（加锁、状态检查、登录、事件处理）
 */
@Service
public class RemoteImpl implements IRemote {

    private static final Logger log = LogManager.getLogger(RemoteImpl.class);

    /** 获取用户锁的超时时间（秒） */
    private static final int LOCK_TIMEOUT_SECONDS = 30;

    @Autowired
    private IChromeSet chromeSet;
    @Autowired
    private Config config;
    @Autowired
    private ICse cse;
    @Autowired
    private LockManager lockManager;
    @Autowired
    private IAlarm alarm;

    @Override
    public UserBind getUserBind(String sessionID) {
        try {
            String url = String.format("http://%s/user-bind/v1/%s", cse.getReportEndpoint(), sessionID);
            return HttpUtil.request(url, HttpGet.METHOD_NAME, null, new TypeReference<>() {});
        } catch (RuntimeException e) {
            log.error("get user bind failed: {}.", sessionID, e);
            return null;
        }
    }

    @Override
    public void expiredUserBind(String sessionID) {
        String url = String.format("http://%s/user-bind/v1/%s", cse.getReportEndpoint(), sessionID);
        HttpUtil.request(url, HttpPut.METHOD_NAME, null);
    }

    /**
     * 创建浏览器实例的完整流程：加锁 → 状态检查/重建 → 登录 → 事件处理 → 清除告警。
     * 使用用户级别的 ReentrantLock 防止同一用户并发创建。
     */
    @Override
    public void createChrome(byte[] receivedControlPackets, InitBrowserRequest parsedParams,
                             Consumer<Object> consumer) {
        String userId = UserIdUtil.generateUserIdByImeiAndImsi(parsedParams.getImei(), parsedParams.getImsi());
        log.info("create browser instance, userId:{}, parsed params: {}", userId, JSONUtil.toJsonStr(parsedParams));

        ReentrantLock lock = lockManager.getLock(userId);
        try {
            acquireLock(lock, userId);

            UserChrome userChrome = processUserChrome(userId, receivedControlPackets, parsedParams, consumer);
            if (consumer == null || userChrome == null) {
                return;
            }

            loginAndHandleEvent(userChrome, receivedControlPackets, userId, consumer);
            clearAlarms();
        } catch (Exception e) {
            handleCreateChromeException(e, userId);
        } finally {
            releaseLock(lock, userId);
        }
    }

    @Override
    public void fallback(String sessionID) {
        fallbackInternal(sessionID, false);
    }

    @Override
    public void fallbackByError(String sessionId) {
        fallbackInternal(sessionId, true);
    }

    @Override
    public void handleEvent(byte[] receivedControlPackets, String userId) {
        UserChrome userChromeInfo = chromeSet.get(userId);
        if (userChromeInfo == null) {
            log.error("user browser instance not exists, user: {}", userId);
            return;
        }
        HWContext hwContext = new HWContext();
        hwContext.setChromeDriver(userChromeInfo.getChromeDriver());
        userChromeInfo.getMuenDriver().Handle(hwContext, receivedControlPackets);
    }

    @Override
    public UserBind updateUserBind(String sessionID) {
        UserBind userBind = buildUserBind(sessionID);
        try {
            String url = String.format("http://%s/user-bind/v1/update", cse.getReportEndpoint());
            HttpUtil.request(url, HttpPost.METHOD_NAME, JSONUtil.toJsonStr(userBind), new TypeReference<>() {});
            return userBind;
        } catch (RuntimeException e) {
            log.error("update user bind failed: {}.", sessionID, e);
            return null;
        }
    }

    @Override
    public void sendTrafficMedia(String dataJson) {
        sendTrafficData("/stats/v1/traffic/media", dataJson, "media traffic");
    }

    @Override
    public void sendTrafficControl(String dataJson) {
        sendTrafficData("/stats/v1/traffic/control", dataJson, "control traffic");
    }

    @Override
    public void sendSession(String dataJson) {
        sendTrafficData("/stats/v1/session", dataJson, "session");
    }

    @Override
    public <T> void reportEvent(EventInfo<T> event) {
        ReportEventUtil.reportServerEvent(event, cse.getReportEndpoint());
    }

    // ----------------------------- private methods -----------------------------

    /**
     * 尝试在超时时间内获取锁，超时则抛出异常
     */
    private void acquireLock(ReentrantLock lock, String userId) throws InterruptedException {
        if (!lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            log.error("user:{} get lock timeout.", userId);
            throw new RuntimeException("get lock timeout!");
        }
    }

    /**
     * 检查用户浏览器状态，按需重建实例后返回 UserChrome
     */
    private UserChrome processUserChrome(String userId, byte[] receivedControlPackets,
                                         InitBrowserRequest parsedParams, Consumer<Object> consumer) {
        UserChrome userChrome = chromeSet.get(userId);
        BrowserStatus browserStatus = checkBrowserStatus(userChrome, receivedControlPackets);
        String prefix = consumer == null ? "[pre open]" : "[user connect]";
        log.info("{} user:{} browser status: {}", prefix, userId, browserStatus);

        if (browserStatus != BrowserStatus.NORMAL) {
            chromeSet.deleteForRestart(userId);
            userChrome = createInstance(parsedParams, receivedControlPackets);
        }
        return userChrome;
    }

    /**
     * 执行登录、触发连接回调并处理控制流事件
     */
    private void loginAndHandleEvent(UserChrome userChrome, byte[] receivedControlPackets,
                                     String userId, Consumer<Object> consumer) {
        userChrome.getMuenDriver().Login(receivedControlPackets);
        userChrome.getMuenDriver().onControlTcpConnected();
        consumer.accept(null);
        this.handleEvent(receivedControlPackets, userId);
    }

    /** 清除浏览器创建相关的告警 */
    private void clearAlarms() {
        alarm.clearAlarm(new AlarmEvent(AlarmEnum.ALARM_300033, "the browser has return to normal"));
        alarm.clearAlarm(new AlarmEvent(AlarmEnum.ALARM_300031, "the user interface has returned to normal"));
    }

    /** 处理创建浏览器过程中的异常，按错误类型发送对应告警 */
    private void handleCreateChromeException(Exception e, String userId) {
        if (e.getMessage() != null && e.getMessage().contains("failed to create browsers")) {
            alarm.sendAlarm(new AlarmEvent(AlarmEnum.ALARM_300033, "Failed to create a browser"));
        }
        if (e.getMessage() != null && e.getMessage().contains("failed to create user interface")) {
            alarm.sendAlarm(new AlarmEvent(AlarmEnum.ALARM_300031, "Failed to create user interface"));
        }
        log.error("login and start record failed, user: {}.", userId, e);
        chromeSet.delete(userId);
    }

    /** 释放锁并移除锁记录 */
    private void releaseLock(ReentrantLock lock, String userId) {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
        lockManager.removeLock(userId);
    }

    /**
     * 回退到空白页的内部实现
     *
     * @param byError true 表示异常退出，需标记 PAGE_CONTROL_ERROR 状态
     */
    private void fallbackInternal(String sessionID, boolean byError) {
        UserChrome chrome = chromeSet.get(sessionID);
        if (chrome == null) {
            return;
        }
        chrome.closeApp();
        if (byError) {
            chrome.setStatus(BrowserStatus.PAGE_CONTROL_ERROR);
        }
    }

    /** 统一发送流量/会话统计数据到远端 */
    private void sendTrafficData(String path, String dataJson, String dataType) {
        try {
            String url = String.format("http://%s%s", cse.getReportEndpoint(), path);
            HttpUtil.request(url, HttpPost.METHOD_NAME, dataJson, new TypeReference<>() {});
        } catch (RuntimeException e) {
            log.error("report {} data failed: {}.", dataType, dataJson, e);
        }
    }

    /** 构建用户会话绑定对象，填充本实例的各端点信息 */
    private UserBind buildUserBind(String sessionID) {
        UserBind userBind = new UserBind();
        userBind.setSessionId(sessionID);
        userBind.setBrowserInstance(config.getSelfAddr());
        userBind.setControlEndpoint(config.getReport().getControlEndpoint());
        userBind.setMediaEndpoint(config.getReport().getMediaEndpoint());
        userBind.setControlTlsEndpoint(config.getReport().getControlTlsEndpoint());
        userBind.setMediaTlsEndpoint(config.getReport().getMediaTlsEndpoint());
        userBind.setInnerMediaEndpoint(config.getInnerMediaEndpoint());
        userBind.setInnerBrowserEndpoint(config.getSelfAddr());
        return userBind;
    }

    /**
     * 检查浏览器实例状态：
     * - null → OPEN_ERROR（需重建）
     * - PAGE_CONTROL_ERROR → 需重建
     * - 配置未变更 → NORMAL
     * - 配置变更 → REOPEN（需重建）
     */
    private BrowserStatus checkBrowserStatus(UserChrome userChrome, byte[] encodeParam) {
        if (userChrome == null) {
            return BrowserStatus.OPEN_ERROR;
        }
        if (userChrome.getStatus() == BrowserStatus.PAGE_CONTROL_ERROR) {
            return BrowserStatus.PAGE_CONTROL_ERROR;
        }
        String result = userChrome.getMuenDriver().Login(encodeParam);
        ChromeParams newConfig = JSONUtil.toBean(result, ChromeParams.class);
        if (equalsConfig(newConfig, userChrome.getChromeParams())) {
            return BrowserStatus.NORMAL;
        }
        log.info("user:{} instance already exists, but config changes, reopen. old config:{}, new config:{}.",
                userChrome.getUserId(), JSONUtil.toJsonStr(userChrome.getChromeParams()), JSONUtil.toJsonStr(newConfig));
        return BrowserStatus.REOPEN;
    }

    /** 创建新的浏览器实例并完成初始化（登录、参数解析、创建 ChromiumDriver） */
    private UserChrome createInstance(InitBrowserRequest param, byte[] encodeParam) {
        long start = System.currentTimeMillis();
        UserChrome userChrome = chromeSet.create(param);
        String result = userChrome.getMuenDriver().Login(encodeParam);
        ChromeParams chromeParams = JSONUtil.toBean(result, ChromeParams.class);
        userChrome.setChromeParams(chromeParams);

        // 取沐恩返回的 path 后缀与实际地址前缀拼接
        String extensionPathPrefix = config.getExtensionPath();
        String relativePath = chromeParams.getControlExtentionPath();
        String extensionPath = extensionPathPrefix + relativePath.substring(relativePath.lastIndexOf("/"));

        ChromeRecordConfig recordConfig = ChromeRecordConfig.from(param, chromeParams);
        recordConfig.setCodecMode(config.getCodecMode());
        recordConfig.setLimit(config.getContextLimit());
        recordConfig.setControlExtensionPath(extensionPath);
        userChrome.createBrowser(recordConfig);

        log.info("create browser instance success, user:{}, use time:{} ms.",
                userChrome.getUserId(), System.currentTimeMillis() - start);
        return userChrome;
    }

    /** 比较两个 ChromeParams 的关键字段是否完全一致 */
    private boolean equalsConfig(ChromeParams newConfig, ChromeParams oldConfig) {
        if (newConfig == null || oldConfig == null) {
            return false;
        }
        return Objects.equals(newConfig.getControlExtentionPath(), oldConfig.getControlExtentionPath())
                && Objects.equals(newConfig.getControlExtentionId(), oldConfig.getControlExtentionId())
                && Objects.equals(newConfig.getChromeHeight(), oldConfig.getChromeHeight())
                && Objects.equals(newConfig.getChromeWidth(), oldConfig.getChromeWidth())
                && Objects.equals(newConfig.getFrameRate(), oldConfig.getFrameRate())
                && Objects.equals(newConfig.getSampleRate(), oldConfig.getSampleRate())
                && Objects.equals(newConfig.getChannels(), oldConfig.getChannels())
                && Objects.equals(newConfig.getBitRite(), oldConfig.getBitRite());
    }
}
