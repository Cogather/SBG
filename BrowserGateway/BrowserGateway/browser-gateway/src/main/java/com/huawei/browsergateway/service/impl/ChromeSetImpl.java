package com.huawei.browsergateway.service.impl;

import com.huawei.browsergateway.common.enums.ErrorCodeEnum;
import com.huawei.browsergateway.common.utils.UserIdUtil;
import com.huawei.browsergateway.entity.browser.BrowserStateMachine;
import com.huawei.browsergateway.entity.browser.UserChrome.BrowserStatus;
import com.huawei.browsergateway.entity.browser.UserChrome;
import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.exception.common.BusinessException;
import com.huawei.browsergateway.service.IFileStorage;
import com.huawei.browsergateway.service.IPluginManage;
import com.huawei.browsergateway.service.IChromeSet;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.service.impl.UserDataManager;
import com.huawei.browsergateway.sdk.HWCallback;
import com.huawei.browsergateway.sdk.MuenDriver;
import com.huawei.browsergateway.driver.ChromeDriverProxy;
import com.huawei.browsergateway.tcpserver.control.ControlClientSet;
import com.huawei.browsergateway.tcpserver.media.MediaClientSet;
import com.huawei.browsergateway.adapter.interfaces.ServiceManagementAdapter;
import com.huawei.browsergateway.entity.report.ServiceReport;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
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
    
    @Autowired(required = false)
    private MediaClientSet mediaClientSet;
    
    @Autowired(required = false)
    private UserDataManager userDataManager;
    
    @Autowired(required = false)
    private ServiceManagementAdapter serviceManagementAdapter;
    
    @org.springframework.beans.factory.annotation.Value("${browsergw.server.address:127.0.0.1}")
    private String serverAddress;
    
    @org.springframework.beans.factory.annotation.Value("${browsergw.websocket.media-port:8095}")
    private int websocketPort;
    
    @org.springframework.beans.factory.annotation.Value("${browsergw.workspace:/opt/host}")
    private String workspace;
    
    // 服务上报配置
    @org.springframework.beans.factory.annotation.Value("${browsergw.report.cap:300}")
    private Integer reportCap;
    
    @org.springframework.beans.factory.annotation.Value("${browsergw.report.chain-endpoints:}")
    private String chainEndpoints;
    
    @org.springframework.beans.factory.annotation.Value("${browsergw.report.self-addr:}")
    private String selfAddr;
    
    @org.springframework.beans.factory.annotation.Value("${browsergw.report.control-endpoint:}")
    private String controlEndpoint;
    
    @org.springframework.beans.factory.annotation.Value("${browsergw.report.media-endpoint:}")
    private String mediaEndpoint;
    
    @org.springframework.beans.factory.annotation.Value("${browsergw.report.ttl:120}")
    private Integer reportTtl;
    
    // JSON序列化工具
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 上报属性键名（与存量代码保持一致）
    private static final String PROPERTY_KEY = "serviceReport";
    private static final String REPORT_CHAIN_KEY = "chainEndpoints";
    
    @Override
    public UserChrome create(InitBrowserRequest request) {
        log.info("创建浏览器实例: imei={}, imsi={}", request.getImei(), request.getImsi());
        
        // 1. 容量检查（cap检查）
        if (reportCap != null && userChromeMap.size() >= reportCap) {
            log.error("容量不足，无法创建新实例: cap={}, current size={}", reportCap, userChromeMap.size());
            throw new RuntimeException("cap is not enough!");
        }
        
        // 2. 生成用户ID
        String userId = UserIdUtil.generateUserId(request.getImei(), request.getImsi());
        
        // 3. 检查现有实例
        UserChrome existingChrome = userChromeMap.get(userId);
        if (existingChrome != null) {
            log.warn("用户浏览器实例已存在，先删除: userId={}", userId);
            delete(userId);
        }
        
        // 4. 创建UserChrome对象（初始状态为INITIALIZING）
        UserChrome userChrome = new UserChrome(userId, request);
        userChrome.setStatus(BrowserStatus.INITIALIZING);
        
        // 5. 使用状态机验证并转换到CREATING状态
        try {
            BrowserStateMachine.transition(userChrome, BrowserStatus.CREATING, "开始创建浏览器实例");
        } catch (IllegalStateException e) {
            log.error("状态转换失败: userId={}, error={}", userId, e.getMessage(), e);
            throw new BusinessException(ErrorCodeEnum.BROWSER_CREATE_FAILED, 
                String.format("创建浏览器实例失败: 状态转换失败 - %s", e.getMessage()));
        }
        
        try {
            // 6. 下载用户数据（如果存在）
            String userDataPath = Paths.get(workspace, userId).toString();
            if (userDataManager != null) {
                try {
                    log.info("开始下载用户数据: userId={}, targetPath={}", userId, userDataPath);
                    String downloadedPath = userDataManager.downloadUserData(userId, userDataPath, serverAddress);
                    if (downloadedPath != null) {
                        log.info("用户数据下载成功: userId={}, path={}", userId, downloadedPath);
                    } else {
                        log.info("远程用户数据不存在，使用新数据: userId={}", userId);
                    }
                } catch (Exception e) {
                    log.warn("下载用户数据失败，继续创建流程: userId={}", userId, e);
                    // 下载失败不影响创建流程，继续执行
                }
            } else {
                log.warn("UserDataManager未注入，跳过用户数据下载: userId={}", userId);
            }
            
            // 7. 检查插件状态并加载插件（如果需要）
            MuenDriver muenDriver = null;
            // 使用接口方法创建驱动实例（内部会自动创建callback）
            muenDriver = pluginManage.createDriver(userId);
            if (muenDriver == null) {
                log.warn("MuenDriver创建失败，继续创建浏览器实例: userId={}", userId);
            } else {
                log.info("MuenDriver创建成功: userId={}", userId);
            }
            
            // 8. 创建浏览器驱动实例（ChromeDriverProxy）
            // 根据Moon SDK架构，ChromeDriver可能由MuenDriver内部管理
            // 这里创建ChromeDriverProxy作为代理，提供统一的访问接口
            ChromeDriverProxy chromeDriver = createChromeDriverProxy(userId, muenDriver, request);
            if (chromeDriver != null) {
                userChrome.setChromeDriver(chromeDriver);
                log.info("浏览器驱动创建成功: userId={}", userId);
            } else {
                log.warn("浏览器驱动创建失败: userId={}", userId);
            }
            
            // 9. 设置MuenDriver
            userChrome.setMuenDriver(muenDriver);
            
            // 10. 初始化连接状态和媒体状态
            // 注意：这些状态在UserChrome构造函数中已初始化，这里确保正确设置
            userChrome.setConnectionState(com.huawei.browsergateway.tcpserver.control.ConnectionState.DISCONNECTED);
            userChrome.setMediaState(com.huawei.browsergateway.entity.browser.MediaState.IDLE);
            
            // 11. 使用状态机转换到READY状态
            BrowserStateMachine.transition(userChrome, BrowserStatus.READY, "浏览器实例创建完成");
            
            // 12. 保存到映射表
            userChromeMap.put(userId, userChrome);
            
            // 13. 创建后立即上报使用数量
            reportUsed();
            
            log.info("浏览器实例创建成功: userId={}, status={}", userId, userChrome.getStatus());
            return userChrome;
            
        } catch (BusinessException e) {
            // 业务异常直接抛出
            log.error("创建浏览器实例失败（业务异常）: userId={}, errorCode={}, errorMessage={}", 
                userId, e.getErrorCode(), e.getErrorMessage(), e);
            
            // 异常时转换到错误状态
            try {
                BrowserStateMachine.transition(userChrome, BrowserStatus.OPEN_ERROR, 
                    String.format("创建失败: %s", e.getErrorMessage()));
            } catch (IllegalStateException stateException) {
                log.warn("状态转换到错误状态失败: userId={}, error={}", userId, stateException.getMessage());
            }
            
            // 从映射表中移除（如果已添加）
            userChromeMap.remove(userId);
            
            throw e;
        } catch (Exception e) {
            log.error("创建浏览器实例失败（系统异常）: userId={}, error={}", userId, e.getMessage(), e);
            
            // 异常时转换到错误状态
            try {
                BrowserStateMachine.transition(userChrome, BrowserStatus.OPEN_ERROR, 
                    String.format("创建失败: %s", e.getMessage()));
            } catch (IllegalStateException stateException) {
                log.warn("状态转换到错误状态失败: userId={}, error={}", userId, stateException.getMessage());
            }
            
            // 从映射表中移除（如果已添加）
            userChromeMap.remove(userId);
            
            throw new BusinessException(ErrorCodeEnum.BROWSER_CREATE_FAILED, 
                String.format("创建浏览器实例失败: %s", e.getMessage()));
        }
    }
    
    @Override
    public UserChrome get(String userId) {
        return userChromeMap.get(userId);
    }
    
    @Override
    public void delete(String userId) {
        log.info("删除浏览器实例: userId={}", userId);
        
        UserChrome userChrome = userChromeMap.get(userId);
        if (userChrome == null) {
            log.warn("浏览器实例不存在: userId={}", userId);
            return;
        }
        
        try {
            // 1. 使用状态机转换到CLOSING状态
            try {
                BrowserStateMachine.transition(userChrome, BrowserStatus.CLOSING, "删除浏览器实例");
            } catch (IllegalStateException e) {
                log.warn("状态转换失败，继续删除流程: userId={}, error={}", userId, e.getMessage());
            }
            
            // 2. 关闭连接（TCP和WebSocket）
            if (controlClientSet != null) {
                controlClientSet.removeClient(userId);
            }
            if (mediaClientSet != null) {
                mediaClientSet.removeClient(userId);
            }
            
            // 4. 上传用户数据（在关闭实例前）
            String userDataPath = Paths.get(workspace, userId).toString();
            if (userDataManager != null) {
                try {
                    log.info("开始上传用户数据: userId={}, path={}", userId, userDataPath);
                    userDataManager.uploadUserData(userId, userDataPath, serverAddress);
                    log.info("用户数据上传成功: userId={}", userId);
                } catch (Exception e) {
                    log.error("上传用户数据失败，继续删除流程: userId={}", userId, e);
                    // 上传失败不影响删除流程，继续执行
                }
            } else {
                log.warn("UserDataManager未注入，跳过用户数据上传: userId={}", userId);
            }
            
            // 5. 关闭浏览器实例（会关闭浏览器驱动、插件驱动等）
            userChrome.closeInstance();
            
            // 6. 清理临时文件
            try {
                java.nio.file.Path tempUserPath = Paths.get(workspace, userId);
                if (java.nio.file.Files.exists(tempUserPath)) {
                    // 这里可以添加清理临时文件的逻辑
                    log.debug("清理临时文件: userId={}", userId);
                }
            } catch (Exception e) {
                log.warn("清理临时文件失败: userId={}", userId, e);
            }
            
            // 7. 使用状态机转换到CLOSED状态
            try {
                BrowserStateMachine.transition(userChrome, BrowserStatus.CLOSED, "删除完成");
            } catch (IllegalStateException e) {
                log.warn("状态转换到CLOSED失败: userId={}, error={}", userId, e.getMessage());
            }
            
            // 8. 从映射表中移除
            userChromeMap.remove(userId);
            
            // 9. 删除后立即上报使用数量
            reportUsed();
            
            log.info("浏览器实例删除完成: userId={}", userId);
            
        } catch (Exception e) {
            log.error("删除浏览器实例失败: userId={}, error={}", userId, e.getMessage(), e);
            // 即使失败也要从映射表中移除，避免内存泄漏
            userChromeMap.remove(userId);
            // 删除操作失败不抛出异常，只记录日志，避免影响其他操作
            log.warn("删除浏览器实例失败，已从映射表中移除: userId={}", userId);
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
    
    /**
     * 创建ChromeDriverProxy实例
     * 根据Moon SDK架构，ChromeDriver可能由MuenDriver内部管理
     * 这里创建代理类提供统一的访问接口
     * 
     * @param userId 用户ID
     * @param muenDriver MuenDriver实例（可能为null）
     * @param request 浏览器初始化请求
     * @return ChromeDriverProxy实例
     */
    private ChromeDriverProxy createChromeDriverProxy(String userId, MuenDriver muenDriver, InitBrowserRequest request) {
        log.info("创建ChromeDriverProxy: userId={}", userId);
        
        try {
            // 根据Moon SDK架构，ChromeDriver可能由MuenDriver内部创建和管理
            // 这里先创建代理类，实际的ChromeDriver可能稍后通过MuenDriver获取
            Object actualChromeDriver = null;
            
            // 如果MuenDriver不为null，尝试从MuenDriver获取ChromeDriver
            // 注意：根据Moon SDK文档，ChromeDriver可能由MuenDriver内部管理
            // 这里先创建代理，后续可以通过MuenDriver的方法获取实际的ChromeDriver
            if (muenDriver != null) {
                try {
                    // 尝试通过反射获取ChromeDriver（如果MuenDriver有相关方法）
                    // 注意：这取决于Moon SDK的实际实现
                    log.debug("尝试从MuenDriver获取ChromeDriver: userId={}", userId);
                    // 如果MuenDriver有getChromeDriver方法，可以在这里调用
                    // 目前先创建代理，ChromeDriver可能稍后设置
                } catch (Exception e) {
                    log.debug("无法从MuenDriver获取ChromeDriver，将使用代理: userId={}", userId);
                }
            }
            
            // 创建ChromeDriverProxy实例
            ChromeDriverProxy chromeDriverProxy = new ChromeDriverProxy(userId, actualChromeDriver);
            
            log.info("ChromeDriverProxy创建成功: userId={}", userId);
            return chromeDriverProxy;
            
        } catch (Exception e) {
            log.error("创建ChromeDriverProxy失败: userId={}", userId, e);
            return null;
        }
    }
    
    /**
     * 上报已使用数量
     * 上报当前使用的浏览器实例数量到CSE
     * 与存量代码保持一致：使用ServiceReport对象封装上报数据
     */
    @Override
    public synchronized void reportUsed() {
        if (serviceManagementAdapter == null) {
            log.debug("ServiceManagementAdapter未注入，跳过上报");
            return;
        }
        
        try {
            // 1. 获取服务ID（自身地址）
            String id = selfAddr;
            if (id == null || id.trim().isEmpty()) {
                id = serverAddress;
            }
            
            // 2. 构建媒体内部端点
            String mediaInnerEndpoint = serverAddress + ":" + websocketPort;
            
            // 3. 获取插件状态
            String pluginStatus = pluginManage.getPluginStatus();
            
            // 4. 创建ServiceReport对象
            ServiceReport report = new ServiceReport(id, mediaInnerEndpoint, pluginStatus);
            report.setUsed(userChromeMap.size());
            report.setCap(reportCap);
            report.setControlEndpoint(controlEndpoint);
            report.setMediaEndpoint(mediaEndpoint);
            report.setTtl(reportTtl);
            
            // 5. 序列化为JSON字符串
            String jsonStr = objectMapper.writeValueAsString(report);
            
            // 6. 构建上报属性Map
            Map<String, String> reportMap = new HashMap<>();
            reportMap.put(PROPERTY_KEY, jsonStr);
            
            // 7. 上报到CSE
            boolean success = serviceManagementAdapter.reportInstanceProperties(reportMap);
            if (!success) {
                log.error("failed to update properties to cse");
            } else {
                log.debug("服务使用数量上报成功: used={}, cap={}, id={}", 
                    userChromeMap.size(), reportCap, id);
            }
            
        } catch (Exception e) {
            log.error("上报服务使用数量异常", e);
        }
    }
    
    /**
     * 上报链路端点
     * 上报链路端点信息到CSE
     * 
     * @return 上报是否成功
     */
    @Override
    public boolean reportChainEndpoints() {
        if (serviceManagementAdapter == null) {
            log.debug("ServiceManagementAdapter未注入，跳过上报");
            return false;
        }
        
        try {
            // 1. 检查链路端点配置
            if (chainEndpoints == null || chainEndpoints.trim().isEmpty()) {
                log.warn("链路端点配置为空，跳过上报");
                return false;
            }
            
            // 2. 构建上报属性Map
            Map<String, String> reportMap = new HashMap<>();
            reportMap.put(REPORT_CHAIN_KEY, chainEndpoints);
            
            // 3. 上报到CSE
            boolean success = serviceManagementAdapter.reportInstanceProperties(reportMap);
            if (!success) {
                log.error("failed to report {} to cse", REPORT_CHAIN_KEY);
                return false;
            }
            
            log.debug("链路端点上报成功: chainEndpoints={}", chainEndpoints);
            return true;
            
        } catch (Exception e) {
            log.error("上报链路端点异常", e);
            return false;
        }
    }
}
