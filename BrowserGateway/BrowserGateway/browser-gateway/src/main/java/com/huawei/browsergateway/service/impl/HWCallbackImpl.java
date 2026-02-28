package com.huawei.browsergateway.service.impl;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import com.huawei.browsergateway.service.IFileStorage;
import com.huawei.browsergateway.service.MuenConfig;
import com.huawei.browsergateway.tcpserver.Client;
import com.huawei.browsergateway.tcpserver.control.ControlClientSet;
import com.huawei.browsergateway.util.HttpUtil;
import com.huawei.browsergateway.util.ReportEventUtil;
import com.huawei.browsergateway.websocket.extension.MuenSessionManager;
import com.moon.cloud.browser.sdk.core.HWCallback;
import com.moon.cloud.browser.sdk.core.MuenContext;
import com.moon.cloud.browser.sdk.model.pojo.ReportEvent;
import lombok.Data;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yeauty.pojo.Session;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.io.IOException;

@Data
public class HWCallbackImpl implements HWCallback {
    private static final Logger log = LogManager.getLogger(HWCallbackImpl.class);

    private final ControlClientSet controlClientSet;
    private final MuenSessionManager muenSessionManager;
    private final String userId;
    private final String websocketAddr;
    private final IFileStorage fileStorage;
    private final String localTmp;
    private final String endpoint;


    public HWCallbackImpl(String endpoint, IFileStorage fileStorage, ControlClientSet controlClientSet, MuenSessionManager muenSessionManager
            , String userId, String websocketAddr, String localTmp) {
        this.endpoint = endpoint;
        this.fileStorage = fileStorage;
        this.controlClientSet = controlClientSet;
        this.muenSessionManager = muenSessionManager;
        this.userId = userId;
        this.websocketAddr = websocketAddr;
        this.localTmp = localTmp;
    }

    @Override
    public String GetConfig() {
        String url = String.format("http://%s/config/v1", endpoint);
        MuenConfig cfg = HttpUtil.request(url, HttpGet.METHOD_NAME, null, new TypeReference<>() {
        });

        return JSONUtil.toJsonStr(cfg);
    }

    @Override
    public void Send(MuenContext muenContext, byte[] bytes) {
        Client cli = controlClientSet.get(userId);
        if (cli == null) {
            log.error("channel client has close");
            return;
        }
        cli.send(bytes);
    }

    @Override
    public String Address() {
        return websocketAddr;
    }

    @Override
    public void Log(ReportEvent<Object> event) {
        ReportEventUtil.reportSdkEvent(event, endpoint);
    }

    @Override
    public void sendMessageToWebscoket(String imeiAndImsi, String message) {
        Session session = muenSessionManager.getSession(imeiAndImsi);
        if (session != null) {
            session.sendText(message);
        }
    }

    @Override
    public File getFile(String remoteUrl) {
        log.info("sdk download file from remote:{}", remoteUrl);
        String fileName = remoteUrl.substring(remoteUrl.lastIndexOf("/") + 1);
        Path localFilePath = Paths.get(localTmp, userId, fileName);

        Path parentDir = localFilePath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            try {
                Files.createDirectories(parentDir);
            } catch (IOException e) {
                log.error("Failed to create directory: " + parentDir, e);
                throw new RuntimeException("Failed to create directory: " + parentDir, e);
            }
        }
        FileUtil.del(localFilePath.toFile());
        fileStorage.downloadFile(localFilePath.toString(), remoteUrl);
        log.info("sdk download file from remote:{} to local:{}", remoteUrl, localFilePath.toAbsolutePath());
        return localFilePath.toFile();
    }

    @Override
    public String uploadFile(String s, File file) {
        log.info("sdk upload file to remote, userId:{}, file addr:{}", s, file.getAbsolutePath());
        String remoteUrl = Paths.get("clientDownload", userId, file.getName()).toString();
        if (fileStorage.exist(remoteUrl)) {
            fileStorage.deleteFile(remoteUrl);
        }
        fileStorage.uploadFile(file.getAbsolutePath(), remoteUrl);
        return remoteUrl;
    }
}