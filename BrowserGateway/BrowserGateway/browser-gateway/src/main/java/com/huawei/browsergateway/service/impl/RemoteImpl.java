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
import com.huawei.browsergateway.service.ICse;
import com.huawei.browsergateway.util.HttpUtil;
import com.huawei.browsergateway.service.IChromeSet;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.util.ReportEventUtil;
import com.huawei.browsergateway.util.UserIdUtil;
import com.moon.cloud.browser.sdk.core.HWContext;
import com.moon.cloud.browser.sdk.model.pojo.ChromeParams;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

@Service
public class RemoteImpl implements IRemote {

    private static final Logger log = LogManager.getLogger(RemoteImpl.class);
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
            return HttpUtil.request(url, HttpGet.METHOD_NAME, null, new TypeReference<>() {
            });
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

    @Override
    public void createChrome(byte[] receivedControlPackets, InitBrowserRequest parsedParams, Consumer<Object> consumer) {
        String userId = UserIdUtil.generateUserIdByImeiAndImsi(parsedParams.getImei(), parsedParams.getImsi());
        log.info("create browser instance, userId:{}, parsed params: {}", userId
                , JSONUtil.toJsonStr(parsedParams));
        ReentrantLock lock = lockManager.getLock(userId);
        try {
            if (!lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                log.error("user:{} get lock timeout.", userId);
                throw new RuntimeException("get lock timeout!");
            }

            UserChrome userChrome = chromeSet.get(userId);
            BrowserStatus browserStatus = checkBrowserStatus(userChrome, receivedControlPackets);
            String prefix = consumer == null ? "[pre open]" : "[user connect]";
            log.info("{} user:{} browser status: {}", prefix, userId, browserStatus);
            if (browserStatus != BrowserStatus.NORMAL) {
                chromeSet.deleteForRestart(userId);
                userChrome = createInstance(parsedParams, receivedControlPackets);
            }

            //null 代表预开，无真正应用传入，到这里创建完实例即可结束
            if (consumer == null) {
                return;
            }

            userChrome.getMuenDriver().Login(receivedControlPackets);
            userChrome.getMuenDriver().onControlTcpConnected();
            consumer.accept(null);
            this.handleEvent(receivedControlPackets, userId);

            alarm.clearAlarm(AlarmEnum.ALARM_300033.getAlarmId());
            alarm.clearAlarm(AlarmEnum.ALARM_300031.getAlarmId());
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("failed to create browsers")) {
                alarm.sendAlarm(new AlarmEvent(AlarmEnum.ALARM_300033, "Failed to create a browser"));
            }
            if (e.getMessage() != null && e.getMessage().contains("failed to create user interface")) {
                alarm.sendAlarm(new AlarmEvent(AlarmEnum.ALARM_300031, "Failed to create user interface"));
            }
            log.error("login and start record failed, user: {}.", userId, e);
            chromeSet.delete(userId);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            lockManager.removeLock(userId);
        }
    }

    @Override
    public void fallback(String sessionID) {
        fallback(sessionID, false);
    }

    @Override
    public void fallbackByError(String sessionId) {
        fallback(sessionId, true);
    }

    /**
     * fall back to blank page
     * @param sessionID user id
     * @param byError true:abnormal exit scenario  false:normal exit scenario
     */
    private void fallback(String sessionID, boolean byError) {
        UserChrome chrome = chromeSet.get(sessionID);
        if (chrome == null) {
            return;
        }

        chrome.closeApp();
        if (byError) {
            chrome.setStatus(BrowserStatus.PAGE_CONTROL_ERROR);
        }
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
        UserBind userBind = new UserBind();
        userBind.setSessionId(sessionID);
        userBind.setBrowserInstance(config.getSelfAddr());
        userBind.setControlEndpoint(config.getReport().getControlEndpoint());
        userBind.setMediaEndpoint(config.getReport().getMediaEndpoint());
        userBind.setControlTlsEndpoint(config.getReport().getControlTlsEndpoint());
        userBind.setMediaTlsEndpoint(config.getReport().getMediaTlsEndpoint());
        userBind.setInnerMediaEndpoint(config.getInnerMediaEndpoint());
        userBind.setInnerBrowserEndpoint(config.getSelfAddr());

        try {
            String url = String.format("http://%s/user-bind/v1/update", cse.getReportEndpoint());
            HttpUtil.request(url, HttpPost.METHOD_NAME
                    , JSONUtil.toJsonStr(userBind), new TypeReference<>() {
                    });
            return userBind;
        } catch (RuntimeException e) {
            log.error("update user bind failed: {}.", sessionID, e);
            return null;
        }
    }

    @Override
    public void sendTrafficMedia(String dataJson) {
        try {
            String url = String.format("http://%s/stats/v1/traffic/media", cse.getReportEndpoint());
            HttpUtil.request(url, HttpPost.METHOD_NAME, dataJson, new TypeReference<>() {});
        } catch (RuntimeException e) {
            log.error("report media traffic data failed: {}.", dataJson, e);
        }
    }

    @Override
    public void sendTrafficControl(String dataJson) {
        try {
            String url = String.format("http://%s/stats/v1/traffic/control", cse.getReportEndpoint());
            HttpUtil.request(url, HttpPost.METHOD_NAME, dataJson, new TypeReference<>() {});
        } catch (RuntimeException e) {
            log.error("report media traffic data failed: {}.", dataJson, e);
        }
    }

    @Override
    public void sendSession(String dataJson) {
        try {
            String url = String.format("http://%s/stats/v1/session", cse.getReportEndpoint());
            HttpUtil.request(url, HttpPost.METHOD_NAME, dataJson, new TypeReference<>() {});
        } catch (RuntimeException e) {
            log.error("report session data failed: {}.", dataJson, e);
        }
    }

    @Override
    public <T> void reportEvent(EventInfo<T> event) {
        ReportEventUtil.reportServerEvent(event, cse.getReportEndpoint());
    }


    private BrowserStatus checkBrowserStatus(UserChrome userChrome, byte[] encodeParam) {
        if (userChrome == null) {
            //暂无实例，按照创建失败处理, 需要重创
            return BrowserStatus.OPEN_ERROR;
        }

        if (userChrome.getStatus() == BrowserStatus.PAGE_CONTROL_ERROR) {
            return BrowserStatus.PAGE_CONTROL_ERROR;
        }

        //判断是否有配置变更
        String result = userChrome.getMuenDriver().Login(encodeParam);
        ChromeParams newConfig = JSONUtil.toBean(result, ChromeParams.class);
        if (this.equalsConfig(newConfig, userChrome.getChromeParams())) {
            return BrowserStatus.NORMAL;
        }
        log.info("user:{} instance already exists, but config changes, reopen. old config:{}, new config:{}."
                , userChrome.getUserId(), JSONUtil.toJsonStr(userChrome.getChromeParams())
                , JSONUtil.toJsonStr(newConfig));
        return BrowserStatus.REOPEN;
    }

    private UserChrome createInstance(InitBrowserRequest param, byte[] encodeParam) {
        long start = System.currentTimeMillis();
        UserChrome userChrome = chromeSet.create(param);
        String result = userChrome.getMuenDriver().Login(encodeParam);
        ChromeParams chromeParams = JSONUtil.toBean(result, ChromeParams.class);
        userChrome.setChromeParams(chromeParams);
        //取沐恩返回的path后缀与我们实际地址前缀拼接
        String extensionPathPrefix = config.getExtensionPath();
        String relativePath = chromeParams.getControlExtentionPath();
        String extensionPath = extensionPathPrefix + relativePath.substring(relativePath.lastIndexOf("/"));
        ChromeRecordConfig recordConfig = ChromeRecordConfig.from(param, chromeParams);
        recordConfig.setCodecMode(config.getCodecMode());
        recordConfig.setLimit(config.getContextLimit());
        recordConfig.setControlExtensionPath(extensionPath);
        userChrome.createBrowser(recordConfig);
        log.info("create browser instance success, user:{}, use time:{} ms.", userChrome.getUserId()
                , System.currentTimeMillis() - start);
        return userChrome;
    }


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