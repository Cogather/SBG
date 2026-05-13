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

/**
 * 用户数据管理类，负责浏览器用户数据的本地压缩、远程上传与下载。
 * 用户数据以 Zstd 压缩格式存储在远端，会话结束时上传，会话开始时下载。
 */
public class UserData {

    private static final Logger log = LogManager.getLogger(UserData.class);

    private final IFileStorage fileStorageService;
    private final String userdataDir;
    private final String userId;
    private final String selfAddr;
    private final IRemote remote;

    public UserData(IFileStorage fileStorageService, String userdataDir,
                    String userId, String selfAddr, IRemote remote) {
        this.fileStorageService = fileStorageService;
        this.userdataDir = userdataDir;
        this.userId = userId;
        this.selfAddr = selfAddr;
        this.remote = remote;
    }

    /**
     * 浏览器关闭时将用户数据压缩后上传到远端存储。
     * 仅当本实例是该用户的绑定实例时才执行上传。
     */
    public void upload() {
        // TODO: 实现用户数据上传逻辑，包括压缩、上传到远端存储
    }

    /**
     * 浏览器启动时从远端存储下载并解压用户数据。
     * 若远端不存在则直接使用本地路径。
     *
     * @return 本地用户数据文件路径
     */
    public String download() {
        // TODO: 实现用户数据下载逻辑，从远端存储下载并解压
        return null;
    }

    /**
     * 删除本地及远端的用户数据缓存
     */
    public void delete() {
        log.info("Deleting Chrome user data for user {}", userId);
        FileUtil.del(getLocalURL());
        log.info("delete local user data success, start to delete remote data");
        fileStorageService.deleteFile(getRemoteURL());
        log.info("delete remote user data success");
    }

    /** 本地用户数据文件路径 */
    private File getLocalURL() {
        return FileUtil.file(userdataDir, userId, "userdata.json");
    }

    /** 远端用户数据文件路径 */
    private String getRemoteURL() {
        return Paths.get("userdata", userId, "userdata.json.zst").toString();
    }

    /** 判断是否需要上传：仅当本实例是该用户的绑定实例时才上传 */
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
