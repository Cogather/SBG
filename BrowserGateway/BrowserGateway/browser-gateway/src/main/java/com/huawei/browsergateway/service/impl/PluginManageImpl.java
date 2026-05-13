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

/**
 * 插件管理实现，负责 SDK JAR 的动态加载、JS 扩展文件的部署，
 * 以及插件状态的维护和告警上报
 */
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

    /** 初始化插件状态为未启动 */
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
        // TODO: 实现更新插件激活状态逻辑
    }

    @Override
    public void loadPlugin(String keyPath, String touchPath, String jarPath) {
        // TODO: 实现加载插件逻辑，包括加载SDK和JS扩展
    }

    @Override
    public void updateStatus(String pluginStatus) {
        // TODO: 实现更新插件状态逻辑，包括告警处理
    }

    @Override
    public MuenDriver createDriver(String userId) {
        // TODO: 实现创建驱动实例逻辑
        return null;
    }

    /**
     * 加载 JS 扩展文件（keys 和 touch 目录）到配置的扩展路径
     */
    public boolean loadJSExtension(String keyPath, String touchPath) {
        // TODO: 实现加载JS扩展文件逻辑
        return false;
    }

    /**
     * 加载 SDK JAR 文件，替换旧版本并重新初始化类加载器
     */
    public boolean loadSDK(String jarPath) {
        // TODO: 实现加载SDK JAR文件逻辑
        return false;
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
        if (Constant.COMPLETE.equals(pluginStatus)) {
            alarm.clearAlarm(new AlarmEvent(AlarmEnum.ALARM_300030, "plugin has return to normal"));
        } else {
            alarm.sendAlarm(new AlarmEvent(AlarmEnum.ALARM_300030, "ERROR:Failed to create plugin"));
        }
        pluginActive.setStatus(pluginStatus);
    }

    @Override
    public String getPluginStatus() {
        return pluginActive.getStatus();
    }

    @Override
    public MuenDriver createDriver(String userId) {
        if (muenPluginClassLoader == null) {
            return null;
        }
        String websocketAddr = address + ":" + config.getWebsocket().getMediaPort();
        HWCallbackImpl hwCallback = new HWCallbackImpl(
                cse.getReportEndpoint(), fs, controlClientSet, muenSessionManager,
                userId, websocketAddr, config.getTmpPath());
        return muenPluginClassLoader.createDriverInstance(hwCallback);
    }

    /**
     * 加载 JS 扩展文件（keys 和 touch 目录）到配置的扩展路径
     */
    public boolean loadJSExtension(String keyPath, String touchPath) {
        copyExtensionIfNotBlank(keyPath, config.getKeyExtensionPath());
        copyExtensionIfNotBlank(touchPath, config.getTouchExtensionPath());
        return true;
    }

    /**
     * 加载 SDK JAR 文件，替换旧版本并重新初始化类加载器
     */
    public boolean loadSDK(String jarPath) {
        if (StringUtil.isBlank(jarPath)) {
            return true;
        }
        if (muenPluginClassLoader != null) {
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

    /** 若源路径非空，则删除目标路径后将源文件复制过去 */
    private void copyExtensionIfNotBlank(String sourcePath, String targetPath) {
        if (!StringUtil.isBlank(sourcePath)) {
            FileUtil.del(targetPath);
            FileUtil.copy(sourcePath, FileUtil.getParent(targetPath, 1), true);
        }
    }
}
