package com.huawei.browsergateway.service.impl;

import com.huawei.browsergateway.common.utils.UserIdUtil;
import com.huawei.browsergateway.entity.browser.UserChrome.BrowserStatus;
import com.huawei.browsergateway.entity.browser.UserChrome;
import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.service.IFileStorage;
import com.huawei.browsergateway.service.IPluginManage;
import com.huawei.browsergateway.service.IChromeSet;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.sdk.HWCallback;
import com.huawei.browsergateway.sdk.MuenDriver;
import com.huawei.browsergateway.tcpserver.control.ControlClientSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 浏览器管理服务实现类
 */
@Service
public class ChromeSetImpl implements IChromeSet {
    
    private static final Logger log = LoggerFactory.getLogger(ChromeSetImpl.class);
    
    // 用户浏览器实例映射表
    private final ConcurrentMap<String, UserChrome> userChromeMap = new ConcurrentHashMap<>();
    
    @Autowired
    private IFileStorage fileStorageService;
    
    @Autowired
    private IPluginManage pluginManage;
    
    @Autowired(required = false)
    private IRemote remoteService;
    
    @Autowired(required = false)
    private ControlClientSet controlClientSet;
    
    @org.springframework.beans.factory.annotation.Value("${browsergw.server.address:127.0.0.1}")
    private String serverAddress;
    
    @org.springframework.beans.factory.annotation.Value("${browsergw.websocket.media-port:8095}")
    private int websocketPort;
    
    @Override
    public UserChrome create(InitBrowserRequest request) {
        log.info("创建浏览器实例: imei={}, imsi={}", request.getImei(), request.getImsi());
        
        // 1. 生成用户ID
        String userId = UserIdUtil.generateUserId(request.getImei(), request.getImsi());
        
        // 2. 检查现有实例
        UserChrome existingChrome = userChromeMap.get(userId);
        if (existingChrome != null) {
            log.warn("用户浏览器实例已存在，先删除: userId={}", userId);
            delete(userId);
        }
        
        try {
            // 3. 创建MuenDriver驱动实例
            MuenDriver muenDriver = null;
            if (pluginManage instanceof PluginManageImpl) {
                PluginManageImpl pluginManageImpl = (PluginManageImpl) pluginManage;
                HWCallback callback = createHWCallback(userId);
                muenDriver = pluginManageImpl.createDriver(userId, callback);
                if (muenDriver == null) {
                    log.warn("MuenDriver创建失败，继续创建浏览器实例: userId={}", userId);
                }
            }
            
            // 4. 创建UserChrome对象
            UserChrome userChrome = new UserChrome(userId, request);
            userChrome.setMuenDriver(muenDriver);
            userChrome.setStatus(BrowserStatus.READY);
            
            // 5. 保存到映射表
            userChromeMap.put(userId, userChrome);
            
            log.info("浏览器实例创建成功: userId={}, status={}", userId, userChrome.getStatus());
            return userChrome;
            
        } catch (Exception e) {
            log.error("创建浏览器实例失败: userId={}", userId, e);
            throw new RuntimeException("创建浏览器实例失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public UserChrome get(String userId) {
        return userChromeMap.get(userId);
    }
    
    @Override
    public void delete(String userId) {
        log.info("删除浏览器实例: userId={}", userId);
        
        UserChrome userChrome = userChromeMap.remove(userId);
        if (userChrome != null) {
            try {
                userChrome.closeInstance();
            } catch (Exception e) {
                log.error("关闭浏览器实例失败: userId={}", userId, e);
            }
        }
    }
    
    @Override
    public void deleteAll() {
        log.info("删除所有浏览器实例，总数: {}", userChromeMap.size());
        
        Set<String> userIds = Set.copyOf(userChromeMap.keySet());
        for (String userId : userIds) {
            delete(userId);
        }
        
        log.info("所有浏览器实例已删除");
    }
    
    @Override
    public Set<String> getAllUser() {
        return userChromeMap.keySet();
    }
    
    @Override
    public void updateHeartbeats(String userId, long heartbeats) {
        UserChrome userChrome = userChromeMap.get(userId);
        if (userChrome != null) {
            userChrome.setHeartbeats(heartbeats);
        } else {
            log.warn("更新心跳失败，用户实例不存在: userId={}", userId);
        }
    }
    
    @Override
    public long getHeartbeats(String userId) {
        UserChrome userChrome = userChromeMap.get(userId);
        if (userChrome != null) {
            return userChrome.getLastHeartbeat();
        }
        return 0;
    }
    
    /**
     * 创建HWCallback回调接口
     * 根据Moon-SDK文档：使用HWCallbackImpl实现类
     */
    private HWCallback createHWCallback(String userId) {
        return new com.huawei.browsergateway.service.impl.HWCallbackImpl(
                userId, this, fileStorageService, remoteService, 
                controlClientSet, serverAddress, websocketPort);
    }
    
    /**
     * 重启用户浏览器实例
     * 
     * @param userId 用户ID
     * @param request 新的浏览器初始化请求
     * @return 重启的浏览器实例
     */
    public UserChrome restart(String userId, InitBrowserRequest request) {
        log.info("重启浏览器实例: userId={}", userId);
        
        // 先删除现有实例
        delete(userId);
        
        // 重新创建实例
        return create(request);
    }
    
    /**
     * 为重启删除实例（保留部分数据）
     */
    public void deleteForRestart(String userId) {
        log.info("为重启删除实例: userId={}", userId);
        
        UserChrome userChrome = userChromeMap.remove(userId);
        if (userChrome != null) {
            try {
                // 只关闭连接，不删除用户数据
                userChrome.closeApp();
            } catch (Exception e) {
                log.error("关闭浏览器实例失败: userId={}", userId, e);
            }
        }
    }
}
