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

@Data
public class UserChrome {
    private static final Logger log = LogManager.getLogger(UserChrome.class);

    private final String userId;
    private BrowserOptions options;
    private ChromiumDriverProxy chromeDriver;
    private final MuenDriver muenDriver;
    private final UserData userData;
    private final ControlClientSet controlClientSet;
    private final MediaClientSet mediaClientSet;

    //from muen sdk
    private ChromeParams chromeParams;
    private volatile BrowserStatus status;
    private long heartbeats;    // ns,控制流心跳时间，单调时钟

    public UserChrome(InitBrowserRequest request, IFileStorage fs, Config config, MuenDriver muenDriver
            , ControlClientSet controlClientSet, MediaClientSet mediaClientSet, IRemote remote) {
        userId = UserIdUtil.generateUserIdByImeiAndImsi(request.getImei(), request.getImsi());
        userData = new UserData(fs, config.getUserDataPath(), userId, config.getSelfAddr(), remote);
        String userdata = userData.download();
        options = request.buildBrowserOptions(userdata, config);
        options.setUrl(config.getRecordExtensionPage());
        this.controlClientSet = controlClientSet;
        this.mediaClientSet = mediaClientSet;
        this.muenDriver = muenDriver;
        this.status = BrowserStatus.NORMAL;
        this.heartbeats = System.nanoTime();
        log.info("create sel chrome instance success, userId: {}.", userId);
    }

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

    public void closeApp() {
        log.info("close app, userId: {}.", userId);
        controlClientSet.del(userId);
        mediaClientSet.del(userId);
        muenDriver.onControlTcpDisconnected();
        chromeDriver.saveUserdata();
        userData.upload();
    }


    public void closeConnection() {
        controlClientSet.del(userId);
        mediaClientSet.del(userId);
    }

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


