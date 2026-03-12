package com.huawei.browsergateway.service.impl;

import cn.hutool.json.JSONUtil;
import com.huawei.browsergateway.config.Config;
import com.huawei.browsergateway.entity.browser.ChromeRecordConfig;
import com.huawei.browsergateway.entity.enums.BrowserStatus;
import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.sdk.BrowserOptions;
import com.huawei.browsergateway.sdk.ChromiumDriverProxy;
import com.huawei.browsergateway.sdk.Request;
import com.huawei.browsergateway.sdk.Type;
import com.huawei.browsergateway.service.IFileStorage;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.tcpserver.control.ControlClientSet;
import com.huawei.browsergateway.tcpserver.media.MediaClientSet;
import com.huawei.browsergateway.util.UserIdUtil;
import com.moon.cloud.browser.sdk.core.MuenDriver;
import com.moon.cloud.browser.sdk.model.pojo.ChromeParams;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 用户浏览器实例封装，持有 ChromiumDriver、MuenDriver 及用户数据对象，
 * 管理浏览器的创建、关闭和用户数据的上传下载
 */
@Data
public class UserChrome {

    private static final Logger log = LogManager.getLogger(UserChrome.class);

    private final String userId;
    private final MuenDriver muenDriver;
    private final UserData userData;
    private final ControlClientSet controlClientSet;
    private final MediaClientSet mediaClientSet;

    private BrowserOptions options;
    private ChromiumDriverProxy chromeDriver;

    /** 来自 MuenSDK 的浏览器参数（分辨率、帧率等） */
    private ChromeParams chromeParams;

    /** 浏览器当前状态 */
    private volatile BrowserStatus status;

    /** 控制流心跳时间戳（单调时钟，纳秒） */
    private long heartbeats;

    public UserChrome(InitBrowserRequest request, IFileStorage fs, Config config,
                      MuenDriver muenDriver, ControlClientSet controlClientSet,
                      MediaClientSet mediaClientSet, IRemote remote) {
        this.userId = UserIdUtil.generateUserIdByImeiAndImsi(request.getImei(), request.getImsi());
        this.userData = new UserData(fs, config.getUserDataPath(), userId, config.getSelfAddr(), remote);
        this.controlClientSet = controlClientSet;
        this.mediaClientSet = mediaClientSet;
        this.muenDriver = muenDriver;
        this.status = BrowserStatus.NORMAL;
        this.heartbeats = System.nanoTime();

        String userdata = userData.download();
        this.options = request.buildBrowserOptions(userdata, config);
        this.options.setUrl(config.getRecordExtensionPage());

        log.info("create sel chrome instance success, userId: {}.", userId);
    }

    /**
     * 根据录制配置创建 ChromiumDriver，设置分辨率、扩展路径等参数
     */
    public void createBrowser(ChromeRecordConfig params) {
        options.setViewpoint(new Request.ViewPort(params.getWidth(), params.getHeight() + 100));
        options.setRecordData(JSONUtil.toJsonStr(params));

        Type.BrowserType browserType = Type.BrowserType.KEYS;
        if (params.getControlExtensionPath().contains(Type.BrowserType.TOUCH.name().toLowerCase())) {
            browserType = Type.BrowserType.TOUCH;
        }
        options.setBrowserType(browserType);
        options.getExtensionIds().add(params.getControlExtensionId());
        options.getExtensionPaths().add(params.getControlExtensionPath());
        options.setLimit(params.getLimit());

        chromeDriver = new ChromiumDriverProxy(options);
    }

    /**
     * 关闭应用：断开 TCP 连接、保存用户数据并上传
     */
    public void closeApp() {
        log.info("close app, userId: {}.", userId);
        controlClientSet.del(userId);
        mediaClientSet.del(userId);
        muenDriver.onControlTcpDisconnected();
        chromeDriver.saveUserdata();
        userData.upload();
    }

    /**
     * 仅断开 TCP 连接（重启场景使用，不上传数据）
     */
    public void closeConnection() {
        controlClientSet.del(userId);
        mediaClientSet.del(userId);
    }

    /**
     * 关闭浏览器实例并上传用户数据
     */
    public void closeInstance() {
        if (chromeDriver != null) {
            chromeDriver.quit();
        }
        userData.upload();
    }

    public synchronized void setHeartbeats(long heartbeats) {
        this.heartbeats = heartbeats;
    }
}
