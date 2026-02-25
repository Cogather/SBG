package com.huawei.browsergateway.service.impl;

import com.huawei.browsergateway.entity.browser.UserChrome;
import com.huawei.browsergateway.entity.event.EventInfo;
import com.huawei.browsergateway.service.IFileStorage;
import com.huawei.browsergateway.service.IChromeSet;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.sdk.HWCallback;
import com.huawei.browsergateway.tcpserver.control.ControlClientSet;
import com.huawei.browsergateway.websocket.extension.ExtensionSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Moon SDK回调接口实现类
 * 根据Moon-SDK应用模块分析文档实现所有回调方法
 */
@Component
public class HWCallbackImpl implements HWCallback {
    
    private static final Logger log = LoggerFactory.getLogger(HWCallbackImpl.class);
    
    @Autowired
    private IChromeSet chromeSet;
    
    @Autowired
    private IFileStorage fileStorageService;
    
    @Autowired
    private IRemote remoteService;
    
    @Autowired(required = false)
    private ControlClientSet controlClientSet;
    
    @Value("${browsergw.server.address:127.0.0.1}")
    private String serverAddress;
    
    @Value("${browsergw.websocket.media-port:8095}")
    private int websocketPort;
    
    // HTTP客户端（用于配置获取）
    private final HttpClient httpClient;
    
    // 用户ID -> 回调实例映射（用于支持多用户）
    private static final Map<String, HWCallbackImpl> userCallbacks = new ConcurrentHashMap<>();
    
    private final String userId;
    
    /**
     * 创建用户特定的回调实例
     * 
     * @param userId 用户ID
     * @param chromeSet 浏览器管理服务
     * @param fileStorageService 文件存储服务
     * @param remoteService 远程服务
     * @param controlClientSet 控制客户端集合
     * @param serverAddress 服务器地址
     * @param websocketPort WebSocket端口
     */
    public HWCallbackImpl(String userId, IChromeSet chromeSet, IFileStorage fileStorageService,
                          IRemote remoteService, ControlClientSet controlClientSet,
                          String serverAddress, int websocketPort) {
        this.userId = userId;
        this.chromeSet = chromeSet;
        this.fileStorageService = fileStorageService;
        this.remoteService = remoteService;
        this.controlClientSet = controlClientSet;
        this.serverAddress = serverAddress;
        this.websocketPort = websocketPort;
        
        // 创建HTTP客户端
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        // 注册用户回调
        userCallbacks.put(userId, this);
    }
    
    /**
     * 无参构造函数（用于Spring自动注入）
     */
    public HWCallbackImpl() {
        this.userId = null;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    
    /**
     * 获取用户特定的回调实例
     */
    public static HWCallbackImpl getInstance(String userId) {
        return userCallbacks.get(userId);
    }
    
    @Override
    public String getConfig(String configUrl) {
        log.debug("获取配置: userId={}, configUrl={}", userId, configUrl);
        
        try {
            // 根据文档：通过HTTP请求获取远程配置并返回JSON
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(configUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                String configJson = response.body();
                log.debug("配置获取成功: userId={}, configLength={}", userId, 
                        configJson != null ? configJson.length() : 0);
                return configJson;
            } else {
                log.warn("配置获取失败: userId={}, statusCode={}", userId, response.statusCode());
                return "{}";
            }
        } catch (Exception e) {
            log.error("获取配置异常: userId={}, configUrl={}", userId, configUrl, e);
            return "{}";
        }
    }
    
    @Override
    public boolean send(byte[] data) {
        log.debug("发送数据: userId={}, dataSize={}", userId, data != null ? data.length : 0);
        
        try {
            // 根据文档：检查TCP客户端连接并发送二进制数据
            if (controlClientSet != null && userId != null) {
                boolean sent = controlClientSet.sendToClient(userId, data);
                if (sent) {
                    log.debug("数据发送成功: userId={}, size={}", userId, data.length);
                    return true;
                } else {
                    log.warn("数据发送失败，客户端未连接: userId={}", userId);
                    return false;
                }
            } else {
                log.warn("控制客户端集合未初始化或userId为空: userId={}", userId);
                return false;
            }
        } catch (Exception e) {
            log.error("发送数据异常: userId={}", userId, e);
            return false;
        }
    }
    
    @Override
    public String address() {
        // 根据文档：提供WebSocket连接地址
        String wsAddress = String.format("ws://%s:%d/control/websocket", serverAddress, websocketPort);
        log.debug("返回WebSocket地址: userId={}, address={}", userId, wsAddress);
        return wsAddress;
    }
    
    @Override
    public void log(String eventType, Object eventData) {
        log.info("SDK事件日志: userId={}, eventType={}, eventData={}", userId, eventType, eventData);
        
        try {
            // 根据文档：封装事件并通过上报工具提交监控数据
            if (remoteService != null) {
                EventInfo<Object> event = new EventInfo<>();
                event.setEventCode(eventType);
                event.setEventType(eventType);
                event.setEventData(eventData);
                event.setTimestamp(System.currentTimeMillis());
                
                remoteService.reportEvent(event);
                log.debug("事件上报成功: userId={}, eventType={}", userId, eventType);
            }
        } catch (Exception e) {
            log.error("事件上报异常: userId={}, eventType={}", userId, eventType, e);
        }
    }
    
    @Override
    public boolean sendMessageToWebscoket(String message) {
        log.debug("发送WebSocket消息: userId={}, message={}", userId, message);
        
        try {
            // 根据文档：检查WebSocket会话并发送文本消息
            if (userId != null) {
                // 通过ExtensionSocketServer发送消息
                ExtensionSocketServer.sendMessage(userId, message);
                log.debug("WebSocket消息发送成功: userId={}", userId);
                return true;
            } else {
                log.warn("userId为空，无法发送WebSocket消息");
                return false;
            }
        } catch (Exception e) {
            log.error("发送WebSocket消息异常: userId={}", userId, e);
            return false;
        }
    }
    
    @Override
    public boolean getFile(String remotePath, String localPath) {
        log.debug("下载文件: userId={}, remotePath={}, localPath={}", userId, remotePath, localPath);
        
        try {
            // 根据文档：解析文件路径，创建本地目录，下载远程文件
            if (fileStorageService != null) {
                File downloadedFile = fileStorageService.downloadFile(localPath, remotePath);
                if (downloadedFile != null && downloadedFile.exists()) {
                    log.info("文件下载成功: userId={}, localPath={}, remotePath={}", 
                            userId, localPath, remotePath);
                    return true;
                } else {
                    log.warn("文件下载失败: userId={}, localPath={}, remotePath={}", 
                            userId, localPath, remotePath);
                    return false;
                }
            } else {
                log.warn("文件存储服务未初始化: userId={}", userId);
                return false;
            }
        } catch (Exception e) {
            log.error("下载文件异常: userId={}, remotePath={}, localPath={}", 
                    userId, remotePath, localPath, e);
            return false;
        }
    }
    
    @Override
    public boolean uploadFile(String localPath, String remotePath) {
        log.debug("上传文件: userId={}, localPath={}, remotePath={}", userId, localPath, remotePath);
        
        try {
            // 根据文档：生成远程URL，先检查后上传文件到存储服务
            if (fileStorageService != null) {
                // 检查本地文件是否存在
                File localFile = new File(localPath);
                if (!localFile.exists()) {
                    log.warn("本地文件不存在: userId={}, localPath={}", userId, localPath);
                    return false;
                }
                
                // 上传文件
                String uploadedPath = fileStorageService.uploadFile(localPath, remotePath);
                if (uploadedPath != null) {
                    log.info("文件上传成功: userId={}, localPath={}, remotePath={}, uploadedPath={}", 
                            userId, localPath, remotePath, uploadedPath);
                    return true;
                } else {
                    log.warn("文件上传失败: userId={}, localPath={}, remotePath={}", 
                            userId, localPath, remotePath);
                    return false;
                }
            } else {
                log.warn("文件存储服务未初始化: userId={}", userId);
                return false;
            }
        } catch (Exception e) {
            log.error("上传文件异常: userId={}, localPath={}, remotePath={}", 
                    userId, localPath, remotePath, e);
            return false;
        }
    }
    
    /**
     * 清理用户回调实例
     */
    public static void removeInstance(String userId) {
        userCallbacks.remove(userId);
    }
}
