package com.huawei.browsergateway.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.json.JSONUtil;
import com.huawei.browsergateway.common.Constant;
import com.huawei.browsergateway.config.Config;
import com.huawei.browsergateway.entity.plugin.PluginActive;
import com.huawei.browsergateway.entity.request.LoadExtensionRequest;
import com.huawei.browsergateway.sdk.ClientImpl;
import com.huawei.browsergateway.sdk.DriverClient;
import com.huawei.browsergateway.util.GzipUtil;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ExtensionManageService {
    private static final Logger log = LogManager.getLogger(ExtensionManageService.class);

    @Resource
    private IFileStorage fileStorageService;
    @Resource
    private IChromeSet chromeSet;
    @Resource
    private Config config;

    @Autowired
    private IPluginManage pluginManage;

    @Autowired
    private IAlarm alarm;

    /**
     * reload extension for update
     * @param request new extension url from remote
     * @return success?
     */
    public synchronized boolean loadExtension(LoadExtensionRequest request) {
        log.info("reload extension, request params: {}", JSONUtil.toJsonStr(request));
        try {
            ExtensionFilePaths extensionFilePaths = downPlugin(request);
            pluginManage.updatePluginActive(request.getName(), request.getVersion(), request.getType());
            pluginManage.loadPlugin(extensionFilePaths.getKeyDir(), extensionFilePaths.getTouchDir(), extensionFilePaths.jarPath);
            postLoadingPlugin(extensionFilePaths);
            log.info("success to load extension name:{} version:{}", request.getName(), request.getVersion());
            return true;
        } catch (Exception e) {
            log.error("load extension failed", e);
            pluginManage.updateStatus(Constant.FAILED);
            return false;
        }finally {
            chromeSet.reportUsed();
        }
    }

    private ExtensionFilePaths downPlugin(LoadExtensionRequest request) throws IOException {
        Path remoteExtensionPath = Paths.get(request.getBucketName(), request.getExtensionFilePath());

        Path localTmpPath = Paths.get(config.getTmpPath());
        FileUtil.mkdir(localTmpPath);
        Path localExtensionPath = localTmpPath.resolve(remoteExtensionPath.getFileName());
        FileUtil.del(localExtensionPath);

        log.info("clear local extension path success, start download from remote. local :{}, remote: {}"
                , localExtensionPath, remoteExtensionPath);
        fileStorageService.downloadFile(localExtensionPath.toString(),remoteExtensionPath.toString());
        log.info("download extension success, start decompress. local:{}", localExtensionPath);

        String unGzipDir = decompress(localExtensionPath.toString(), config.getTmpPath());
        log.info("decompress success, start get extension file paths. local:{}", unGzipDir);

        ExtensionFilePaths extensionFilePaths = getExtensionFilePaths(unGzipDir, localExtensionPath.toString());
        log.info("get extension file paths success, start close all chrome instance. paths: {}"
                , JSONUtil.toJsonStr(extensionFilePaths));
        return extensionFilePaths;
    }

    private void postLoadingPlugin(ExtensionFilePaths extensionFilePaths){
        FileUtil.del(FileUtil.getParent(extensionFilePaths.getUnGzipDir(), 1) );
        FileUtil.del(extensionFilePaths.getLocalExtensionPath());

        chromeSet.deleteAll();
        DriverClient client = new ClientImpl(config.getChrome().getEndpoint());
        client.browser().list().forEach(browser -> {
            client.browser().delete(browser.getId());
        });
        log.info("close all browsers and reload chrome extension success.");
    }

    public PluginActive getPluginInfo() {
        return pluginManage.getPluginActive();
    }

    private String decompress(String zipFilePath, String unzipDir) throws IOException {
        var zipFileName = FileUtil.getName(zipFilePath);
        var zipFilePrefix = FileUtil.mainName(zipFileName);

        unzipDir = Paths.get(unzipDir, zipFilePrefix).toString();

        FileUtil.del(unzipDir);
        FileUtil.mkdir(unzipDir);
        ZipUtil.unzip(zipFilePath, unzipDir);

        var packageSdfFilepath = Paths.get(unzipDir, "package.json").toString();
        if (!FileUtil.exist(packageSdfFilepath)) {
            log.info("not found package.json file in {}", unzipDir);
            throw new RuntimeException("not found package.json file in" + unzipDir);
        }
        log.info("extension package.json is {}", FileUtil.readUtf8String(packageSdfFilepath));

        var gzipFilePath = Paths.get(unzipDir, zipFilePrefix + ".tar.gz").toString();
        if (!FileUtil.exist(gzipFilePath)) {
            log.info("not found {} gzip file in {}", zipFilePrefix + ".tar.gz", unzipDir);
            throw new RuntimeException("not found " + zipFilePrefix + ".tar.gz gzip file in" + unzipDir);
        }

        var unGzipDir = Paths.get(unzipDir, zipFilePrefix).toString();
        FileUtil.mkdir(unGzipDir);
        GzipUtil.unGzip(gzipFilePath, unGzipDir);
        return unGzipDir;
    }

    private String findJarPath(String unGzipDir) {
        Path path = Paths.get(unGzipDir, "jar");
        try {
            var files = FileUtil.ls(path.toString());
            for (var file : files) {
                if (file.isFile() && file.getName().endsWith(".jar")) {
                    return file.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            log.error("find jar file error, dir:{}", path);
            throw new RuntimeException("find jar file error" + path);
        }

        return StrUtil.EMPTY;
    }

    private ExtensionFilePaths getExtensionFilePaths(String unGzipDir, String localExtensionPath)  {
        var efp = new ExtensionFilePaths();
        var jarPath = findJarPath(unGzipDir);
        if (FileUtil.isFile(jarPath)) {
            efp.setJarPath(jarPath);
        }

        var keyDir = Paths.get(unGzipDir, "keys").toString();
        if (FileUtil.isDirectory(keyDir)) {
            efp.setKeyDir(keyDir);
        }
        efp.setKeyDir(keyDir);

        var touchDir = Paths.get(unGzipDir, "touch").toString();
        if (FileUtil.isDirectory(touchDir)) {
            efp.setTouchDir(touchDir);
        }
        efp.setUnGzipDir(unGzipDir);
        efp.setLocalExtensionPath(localExtensionPath);
        return efp;
    }


    @Data
    private static class ExtensionFilePaths {
        String jarPath;
        String keyDir;
        String touchDir;
        String localExtensionPath;
        String unGzipDir;
    }
}