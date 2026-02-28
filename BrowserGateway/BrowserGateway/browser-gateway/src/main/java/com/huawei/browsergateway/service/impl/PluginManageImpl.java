package com.huawei.browsergateway.service.impl;

import cn.hutool.core.io.FileUtil;
import com.huawei.browsergateway.common.Constant;
import com.huawei.browsergateway.config.Config;
import com.huawei.browsergateway.entity.alarm.AlarmEvent;
import com.huawei.browsergateway.entity.enums.AlarmEnum;
import com.huawei.browsergateway.entity.plugin.PluginActive;
import com.huawei.browsergateway.service.IAlarm;
import com.huawei.browsergateway.service.ICse;
import com.huawei.browsergateway.service.IFileStorage;
import com.huawei.browsergateway.service.IPluginManage;
import com.huawei.browsergateway.service.MuenPluginClassLoader;
import com.huawei.browsergateway.tcpserver.control.ControlClientSet;
import com.huawei.browsergateway.websocket.extension.MuenSessionManager;
import com.moon.cloud.browser.sdk.core.MuenDriver;
import org.jsoup.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.file.Paths;

@Component
public class PluginManageImpl implements IPluginManage {

    @Autowired
    private IFileStorage fs;

    @Autowired
    private Config config;

    @Autowired
    private ICse cse;

    @Autowired
    private ControlClientSet controlClientSet;

    @Autowired
    private MuenSessionManager muenSessionManager;

    @Autowired
    private IAlarm alarm;

    @Value("${server.address}")
    private String address;

    private PluginActive pluginActive = new PluginActive();
    private MuenPluginClassLoader muenPluginClassLoader;

    @PostConstruct
    void initPluginActive() {
        pluginActive = new PluginActive();
        pluginActive.setStatus(Constant.NOTSTART);
        pluginActive.setType("ChromeExtend");
    }

    @Override
    public PluginActive getPluginActive() {
        return pluginActive;
    }

    @Override
    public void updatePluginActive(String name, String version, String type) {
        this.pluginActive.setName(name);
        this.pluginActive.setVersion(version);
        this.pluginActive.setType(type);
    }

    @Override
    public void loadPlugin(String keyPath, String touchPath, String jarPath) {
        if (loadSDK(jarPath) && loadJSExtension(keyPath, touchPath)) {
            updateStatus(Constant.COMPLETE);
        } else {
            updateStatus(Constant.FAILED);
        }
    }

    @Override
    public void updateStatus(String pluginStatus) {
        // 上报插件创建失败告警
        if (Constant.COMPLETE.equals(pluginStatus)) {
            alarm.clearAlarm(AlarmEnum.ALARM_300030.getAlarmId());
        } else {
            alarm.sendAlarm(new AlarmEvent(AlarmEnum.ALARM_300030, "ERROR:Failed to create plugin"));
        }

        this.pluginActive.setStatus(pluginStatus);
    }

    @Override
    public String getPluginStatus() {
        return pluginActive.getStatus();
    }

    @Override
    public MuenDriver createDriver(String userId) {
        if (muenPluginClassLoader != null) {
            String websocketAddr = address + ":" + config.getWebsocket().getMediaPort();
            String localTmp = config.getTmpPath();
            HWCallbackImpl hwCallback = new HWCallbackImpl(cse.getReportEndpoint(), fs, controlClientSet, muenSessionManager, userId
                    , websocketAddr, localTmp);
            return muenPluginClassLoader.createDriverInstance(hwCallback);
        }
        return null;
    }


    public boolean loadJSExtension(String keyPath, String touchPath) {
        if (!StringUtil.isBlank(keyPath)) {
            FileUtil.del(config.getKeyExtensionPath());
            FileUtil.copy(keyPath, FileUtil.getParent(config.getKeyExtensionPath(), 1), true);
        }
        if (!StringUtil.isBlank(touchPath)) {
            FileUtil.del(config.getTouchExtensionPath());
            FileUtil.copy(touchPath, FileUtil.getParent(config.getTouchExtensionPath(), 1), true);
        }

        return true;
    }


    public boolean loadSDK(String jarPath) {
        if (StringUtil.isBlank(jarPath)) {
            return true;
        }
        if (muenPluginClassLoader !=null) {
            muenPluginClassLoader.close();
        }
        FileUtil.mkdir(config.getJarDirPath());
        FileUtil.clean(config.getJarDirPath());
        FileUtil.copy(jarPath, config.getJarDirPath(), true);
        String name = FileUtil.getName(jarPath);
        String path = FileUtil.file(config.getJarDirPath(), name).getAbsolutePath();

        muenPluginClassLoader = new MuenPluginClassLoader();
        return muenPluginClassLoader.init(Paths.get(path));
    }
}