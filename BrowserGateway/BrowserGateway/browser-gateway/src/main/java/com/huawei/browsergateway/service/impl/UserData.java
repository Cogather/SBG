package com.huawei.browsergateway.service.impl;

import cn.hutool.core.io.FileUtil;
import com.huawei.browsergateway.service.IFileStorage;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.util.UserdataSlimmer;
import com.huawei.browsergateway.util.ZstdUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Paths;


public class UserData {
    private static final Logger log = LogManager.getLogger(UserData.class);

    private final IFileStorage fileStorageService;

    private final String userdataDir;

    private final String userId;

    private final String selfAddr;

    private final IRemote remote;

    public UserData(IFileStorage fileStorageService, String userdataDir, String userId, String selfAddr, IRemote remote) {
        this.fileStorageService = fileStorageService;
        this.userdataDir = userdataDir;
        this.userId = userId;
        this.selfAddr = selfAddr;
        this.remote = remote;
    }

    /**
     * When the user's browser is closed, the user's data is uploaded to the remote storage.
     */
    public void upload() {
        log.info("Uploading Chrome user data for user {}", userId);
        if (!needUpload()) {
            log.info("userdata not need to upload, userId:{}", userId);
            return;
        }
        File localUserData = getLocalURL();
        if (!localUserData.exists()) {
            log.warn("browser userdata path is not exist: {}", localUserData);
            return;
        }

        UserdataSlimmer.slimInplace(localUserData);
        //压缩
        File localZip = new File(localUserData.getParent(), "userdata.json.zst");
        try {
            ZstdUtil.compressJson(localUserData.getAbsolutePath(), localZip.getAbsolutePath());
        } catch (Exception e) {
            log.error("Compression failed: {}", localUserData.getAbsolutePath(), e);
            return;
        }

        String remoteURL = getRemoteURL();
        long startUpload = System.currentTimeMillis();
        if (fileStorageService.exist(remoteURL)) {
            fileStorageService.deleteFile(remoteURL);
        }
        fileStorageService.uploadFile(localZip.getAbsolutePath(), remoteURL);
        log.info("upload userdata to remote storage end, cost {} ms", System.currentTimeMillis() - startUpload);

        if (localZip.exists()) {
            FileUtil.del(localZip);
        }
    }

    /**
     * When a user starts the browser, the user data is downloaded from the remote storage.
     *
     * @return local data path
     */
    public String download() {
        log.info("Downloading Chrome user data for user {}", userId);
        File localUserData = getLocalURL();
        String remoteURL = getRemoteURL();

        if (!fileStorageService.exist(remoteURL)) {
            log.info("userdata not exist in remote storage, use local path, userId:{}", userId);
            return localUserData.getAbsolutePath();
        }

        if (!localUserData.getParentFile().exists()) {
            FileUtil.mkdir(localUserData.getParentFile());
            FileUtil.touch(localUserData);
        }
        File localZip = new File(localUserData.getParent(), "userdata.json.zst");
        try {
            fileStorageService.downloadFile(localZip.getAbsolutePath(), remoteURL);
            ZstdUtil.decompressJson(localZip.getAbsolutePath(), localUserData.getAbsolutePath());

            if (localZip.exists()) {
                FileUtil.del(localZip);
            }
        } catch (Exception e) {
            log.error("download user data error! userId:{}", userId, e);
        }

        return localUserData.getAbsolutePath();
    }

    /**
     * Delete the cached user data.
     */
    public void delete() {
        log.info("Deleting Chrome user data for user {}", userId);
        File localUserData = getLocalURL();
        FileUtil.del(localUserData);
        log.info("delete local user data success, start to delete remote data");
        String remoteURL = getRemoteURL();
        fileStorageService.deleteFile(remoteURL);
        log.info("delete remote user data success");
    }

    private File getLocalURL() {
        return FileUtil.file(userdataDir, userId, "userdata.json");
    }

    private String getRemoteURL() {
        return Paths.get("userdata", userId, "userdata.json.zst").toString();
    }

    private boolean needUpload() {
        try {
            UserBind userBind = remote.getUserBind(userId);
            return userBind != null && selfAddr.equals(userBind.getBrowserInstance());
        } catch (Exception e) {
            log.error("get user bind error, userId:{}", userId, e);
            return false;
        }
    }
}