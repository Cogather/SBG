package com.huawei.browsergateway.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.browsergateway.adapter.interfaces.ServiceManagementAdapter;
import com.huawei.browsergateway.common.utils.UserIdUtil;
import com.huawei.browsergateway.entity.browser.UserChrome;
import com.huawei.browsergateway.entity.event.EventInfo;
import com.huawei.browsergateway.entity.remote.UserBind;
import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.service.IChromeSet;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.sdk.MuenDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

/**
 * 远程通信服务实现类
 */
@Service
public class RemoteImpl implements IRemote {
    
    private static final Logger log = LoggerFactory.getLogger(RemoteImpl.class);
    
    @Autowired
    private IChromeSet chromeSet;
    
    @Autowired
    private ServiceManagementAdapter serviceManagementAdapter;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${browsergw.server.address:127.0.0.1}")
    private String serverAddress;
    
    @Value("${browsergw.tcp.control-port:18601}")
    private int controlPort;
    
    @Value("${browsergw.tcp.media-port:18602}")
    private int mediaPort;
    
    @Value("${browsergw.tcp.control-tls-port:18603}")
    private int controlTlsPort;
    
    @Value("${browsergw.tcp.media-tls-port:18604}")
    private int mediaTlsPort;
    
    // 用户绑定信息缓存
    private final ConcurrentMap<String, UserBind> userBindCache = new ConcurrentHashMap<>();
    
    @Override
    public void createChrome(byte[] receivedControlPackets, InitBrowserRequest parsedParams, Consumer<Object> consumer) {
        log.info("创建浏览器实例并建立连接: imei={}, imsi={}", 
                parsedParams.getImei(), parsedParams.getImsi());
        
        try {
            // 1. 生成用户ID
            String userId = UserIdUtil.generateUserId(parsedParams.getImei(), parsedParams.getImsi());
            
            // 2. 创建浏览器实例
            UserChrome userChrome = chromeSet.create(parsedParams);
            
            // 3. 调用MuenDriver.Login()进行用户认证并获取配置
            // 根据Moon-SDK文档：Login方法接受二进制认证数据，返回JSON格式配置信息
            MuenDriver muenDriver = userChrome.getMuenDriver();
            if (muenDriver != null && receivedControlPackets != null && receivedControlPackets.length > 0) {
                try {
                    String configJson = muenDriver.login(receivedControlPackets);
                    log.info("用户认证成功，获取配置: userId={}, configLength={}", 
                            userId, configJson != null ? configJson.length() : 0);
                    
                    // 根据文档：将JSON反序列化为ChromeParams对象并保存到userChrome
                    // 注意：ChromeParams是SDK中的类，需要通过反射或JSON解析来处理
                    if (configJson != null && !configJson.isEmpty() && !configJson.equals("{}")) {
                        // 保存配置JSON字符串到UserChrome（后续可以通过反射解析）
                        // 由于ChromeParams是SDK中的类，这里先保存JSON字符串
                        // 实际使用时可以通过反射加载SDK中的ChromeParams类来解析
                        log.debug("保存配置JSON: userId={}, config={}", userId, configJson);
                        // TODO: 如果UserChrome有chromeParams字段，可以在这里设置
                    }
                } catch (Exception e) {
                    log.error("用户认证失败: userId={}", userId, e);
                    // 继续执行，不阻断流程
                }
            }
            
            // 4. 通知连接建立
            if (muenDriver != null) {
                try {
                    muenDriver.onControlTcpConnected();
                } catch (Exception e) {
                    log.warn("通知控制TCP连接建立失败: userId={}", userId, e);
                }
            }
            
            // 5. 更新用户绑定信息
            String sessionId = userId; // 简化实现，使用userId作为sessionId
            updateUserBind(sessionId);
            
            // 6. 执行回调（如果有）
            if (consumer != null) {
                consumer.accept(userChrome);
            }
            
            log.info("浏览器实例创建并连接成功: userId={}", userId);
            
        } catch (Exception e) {
            log.error("创建浏览器实例失败", e);
            if (consumer != null) {
                consumer.accept(null);
            }
            throw new RuntimeException("创建浏览器实例失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public UserBind getUserBind(String sessionID) {
        // 先从缓存获取
        UserBind cached = userBindCache.get(sessionID);
        if (cached != null && !cached.isExpired(300000)) { // 5分钟过期
            return cached;
        }
        
        // 从CSE服务获取（简化实现，直接创建）
        UserBind userBind = createUserBind(sessionID);
        userBindCache.put(sessionID, userBind);
        return userBind;
    }
    
    @Override
    public UserBind updateUserBind(String sessionID) {
        log.info("更新用户绑定信息: sessionID={}", sessionID);
        
        UserBind userBind = createUserBind(sessionID);
        userBind.updateTime();
        userBindCache.put(sessionID, userBind);
        
        // 同步更新到CSE服务（简化实现）
        // serviceManagementAdapter.reportInstanceProperties(...);
        
        return userBind;
    }
    
    @Override
    public void expiredUserBind(String sessionID) {
        log.info("过期用户绑定信息: sessionID={}", sessionID);
        
        UserBind userBind = userBindCache.remove(sessionID);
        if (userBind != null) {
            // 通知CSE服务绑定已过期（简化实现）
            log.debug("用户绑定已过期: sessionID={}", sessionID);
        }
    }
    
    @Override
    public void handleEvent(byte[] receivedControlPackets, String userId) {
        log.debug("处理浏览器事件: userId={}, packetSize={}", userId, 
                receivedControlPackets != null ? receivedControlPackets.length : 0);
        
        try {
            // 1. 获取浏览器实例
            UserChrome userChrome = chromeSet.get(userId);
            if (userChrome == null) {
                log.warn("浏览器实例不存在: userId={}", userId);
                return;
            }
            
            // 2. 创建HWContext上下文对象
            // 根据Moon-SDK文档：需要创建HWContext并绑定ChromeDriver
            com.huawei.browsergateway.sdk.HWContext hwContext = new com.huawei.browsergateway.sdk.HWContext();
            hwContext.setChromeDriver(userChrome.getChromeDriver());
            
            // 3. 获取MuenDriver并处理事件
            // 根据Moon-SDK文档：Handle方法需要HWContext和packets参数
            MuenDriver muenDriver = userChrome.getMuenDriver();
            if (muenDriver != null) {
                muenDriver.handle(hwContext, receivedControlPackets);
            } else {
                log.warn("MuenDriver不存在，无法处理事件: userId={}", userId);
            }
            
        } catch (Exception e) {
            log.error("处理浏览器事件失败: userId={}", userId, e);
        }
    }
    
    @Override
    public void fallback(String sessionID) {
        log.info("回退页面: sessionID={}", sessionID);
        
        // 实现回退逻辑
        // 例如：关闭当前浏览器实例，重新创建等
        try {
            String userId = sessionID; // 简化实现
            chromeSet.delete(userId);
        } catch (Exception e) {
            log.error("回退页面失败: sessionID={}", sessionID, e);
        }
    }
    
    @Override
    public void fallbackByError(String sessionId) {
        log.info("错误回退: sessionId={}", sessionId);
        
        // 实现错误回退逻辑
        fallback(sessionId);
    }
    
    @Override
    public void sendTrafficMedia(String dataJson) {
        log.debug("上报媒体流量统计: data={}", dataJson);
        
        // 异步上报，避免阻塞主流程
        CompletableFuture.runAsync(() -> {
            try {
                // 构建上报属性
                Map<String, String> properties = new HashMap<>();
                properties.put("statisticsType", "trafficMedia");
                properties.put("data", dataJson != null ? dataJson : "{}");
                properties.put("timestamp", String.valueOf(System.currentTimeMillis()));
                
                // 如果dataJson是有效的JSON，尝试解析并添加更多属性
                if (dataJson != null && !dataJson.trim().isEmpty()) {
                    try {
                        Map<String, Object> dataMap = objectMapper.readValue(dataJson, Map.class);
                        // 将JSON数据转换为字符串属性
                        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
                            String key = "trafficMedia." + entry.getKey();
                            String value = entry.getValue() != null ? entry.getValue().toString() : "";
                            properties.put(key, value);
                        }
                    } catch (Exception e) {
                        log.debug("无法解析媒体流量统计JSON，使用原始字符串: {}", e.getMessage());
                    }
                }
                
                // 调用ServiceManagementAdapter上报
                boolean success = serviceManagementAdapter.reportInstanceProperties(properties);
                if (success) {
                    log.debug("媒体流量统计上报成功");
                } else {
                    log.warn("媒体流量统计上报失败");
                }
            } catch (Exception e) {
                log.error("媒体流量统计上报异常", e);
            }
        });
    }
    
    @Override
    public void sendTrafficControl(String dataJson) {
        log.debug("上报控制流量统计: data={}", dataJson);
        
        // 异步上报
        CompletableFuture.runAsync(() -> {
            try {
                // 构建上报属性
                Map<String, String> properties = new HashMap<>();
                properties.put("statisticsType", "trafficControl");
                properties.put("data", dataJson != null ? dataJson : "{}");
                properties.put("timestamp", String.valueOf(System.currentTimeMillis()));
                
                // 如果dataJson是有效的JSON，尝试解析并添加更多属性
                if (dataJson != null && !dataJson.trim().isEmpty()) {
                    try {
                        Map<String, Object> dataMap = objectMapper.readValue(dataJson, Map.class);
                        // 将JSON数据转换为字符串属性
                        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
                            String key = "trafficControl." + entry.getKey();
                            String value = entry.getValue() != null ? entry.getValue().toString() : "";
                            properties.put(key, value);
                        }
                    } catch (Exception e) {
                        log.debug("无法解析控制流量统计JSON，使用原始字符串: {}", e.getMessage());
                    }
                }
                
                // 调用ServiceManagementAdapter上报
                boolean success = serviceManagementAdapter.reportInstanceProperties(properties);
                if (success) {
                    log.debug("控制流量统计上报成功");
                } else {
                    log.warn("控制流量统计上报失败");
                }
            } catch (Exception e) {
                log.error("控制流量统计上报异常", e);
            }
        });
    }
    
    @Override
    public void sendSession(String dataJson) {
        log.debug("上报会话数据: data={}", dataJson);
        
        // 异步上报
        CompletableFuture.runAsync(() -> {
            try {
                // 构建上报属性
                Map<String, String> properties = new HashMap<>();
                properties.put("statisticsType", "session");
                properties.put("data", dataJson != null ? dataJson : "{}");
                properties.put("timestamp", String.valueOf(System.currentTimeMillis()));
                
                // 如果dataJson是有效的JSON，尝试解析并添加更多属性
                if (dataJson != null && !dataJson.trim().isEmpty()) {
                    try {
                        Map<String, Object> dataMap = objectMapper.readValue(dataJson, Map.class);
                        // 将JSON数据转换为字符串属性
                        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
                            String key = "session." + entry.getKey();
                            String value = entry.getValue() != null ? entry.getValue().toString() : "";
                            properties.put(key, value);
                        }
                    } catch (Exception e) {
                        log.debug("无法解析会话数据JSON，使用原始字符串: {}", e.getMessage());
                    }
                }
                
                // 调用ServiceManagementAdapter上报
                boolean success = serviceManagementAdapter.reportInstanceProperties(properties);
                if (success) {
                    log.debug("会话数据上报成功");
                } else {
                    log.warn("会话数据上报失败");
                }
            } catch (Exception e) {
                log.error("会话数据上报异常", e);
            }
        });
    }
    
    @Override
    public <T> void reportEvent(EventInfo<T> event) {
        log.debug("上报事件: eventCode={}, eventType={}", event.getEventCode(), event.getEventType());
        
        // 异步上报
        CompletableFuture.runAsync(() -> {
            try {
                // 构建上报属性
                Map<String, String> properties = new HashMap<>();
                properties.put("statisticsType", "event");
                properties.put("eventCode", event.getEventCode() != null ? event.getEventCode() : "");
                properties.put("eventType", event.getEventType() != null ? event.getEventType() : "");
                properties.put("userId", event.getUserId() != null ? event.getUserId() : "");
                properties.put("sessionId", event.getSessionId() != null ? event.getSessionId() : "");
                properties.put("timestamp", String.valueOf(event.getTimestamp()));
                properties.put("level", event.getLevel() != null ? event.getLevel() : "INFO");
                properties.put("source", event.getSource() != null ? event.getSource() : "BrowserGateway");
                
                // 添加事件数据（如果存在）
                if (event.getEventData() != null) {
                    try {
                        String eventDataJson = objectMapper.writeValueAsString(event.getEventData());
                        properties.put("eventData", eventDataJson);
                    } catch (Exception e) {
                        log.debug("无法序列化事件数据，使用toString: {}", e.getMessage());
                        properties.put("eventData", event.getEventData().toString());
                    }
                }
                
                // 添加标签（如果存在）
                if (event.getTags() != null && !event.getTags().isEmpty()) {
                    try {
                        String tagsJson = objectMapper.writeValueAsString(event.getTags());
                        properties.put("tags", tagsJson);
                    } catch (Exception e) {
                        log.debug("无法序列化事件标签: {}", e.getMessage());
                    }
                }
                
                // 调用ServiceManagementAdapter上报
                boolean success = serviceManagementAdapter.reportInstanceProperties(properties);
                if (success) {
                    log.debug("事件上报成功: eventCode={}, eventType={}", event.getEventCode(), event.getEventType());
                } else {
                    log.warn("事件上报失败: eventCode={}, eventType={}", event.getEventCode(), event.getEventType());
                }
            } catch (Exception e) {
                log.error("事件上报异常", e);
            }
        });
    }
    
    /**
     * 创建用户绑定信息
     */
    private UserBind createUserBind(String sessionID) {
        UserBind userBind = new UserBind();
        userBind.setSessionId(sessionID);
        userBind.setBrowserInstance(sessionID);
        
        // 设置端点信息
        userBind.setControlEndpoint(String.format("tcp://%s:%d", serverAddress, controlPort));
        userBind.setMediaEndpoint(String.format("tcp://%s:%d", serverAddress, mediaPort));
        userBind.setControlTlsEndpoint(String.format("tls://%s:%d", serverAddress, controlTlsPort));
        userBind.setMediaTlsEndpoint(String.format("tls://%s:%d", serverAddress, mediaTlsPort));
        userBind.setInnerMediaEndpoint(String.format("ws://%s:30002", serverAddress));
        userBind.setInnerBrowserEndpoint(String.format("ws://%s:30005", serverAddress));
        
        userBind.setCreateTime(System.currentTimeMillis());
        userBind.setUpdateTime(System.currentTimeMillis());
        
        return userBind;
    }
}
