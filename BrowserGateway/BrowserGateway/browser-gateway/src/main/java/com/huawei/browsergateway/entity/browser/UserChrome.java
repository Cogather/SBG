package com.huawei.browsergateway.entity.browser;

import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.sdk.MuenDriver;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用户浏览器实例
 */
@Data
public class UserChrome {
    private static final Logger log = LoggerFactory.getLogger(UserChrome.class);
    
    private final String userId;
    private String imei;
    private String imsi;
    private BrowserStatus status;
    private long lastHeartbeat;
    
    // 浏览器驱动和SDK驱动
    private Object chromeDriver; // ChromiumDriverProxy，暂时使用Object避免依赖
    private MuenDriver muenDriver;
    
    public UserChrome(String userId, InitBrowserRequest request) {
        this.userId = userId;
        this.imei = request.getImei();
        this.imsi = request.getImsi();
        this.status = BrowserStatus.INITIALIZING;
        this.lastHeartbeat = System.nanoTime();
    }
    
    /**
     * 设置心跳时间
     */
    public void setHeartbeats(long heartbeats) {
        this.lastHeartbeat = heartbeats;
    }
    
    /**
     * 更新浏览器状态
     */
    public void setStatus(BrowserStatus status) {
        BrowserStatus oldStatus = this.status;
        this.status = status;
        if (oldStatus != status) {
            log.info("浏览器状态变更: userId={}, {} -> {}", userId, oldStatus, status);
        }
    }
    
    /**
     * 关闭应用程序连接
     */
    public void closeApp() {
        log.info("关闭应用程序连接: userId={}", userId);
        
        // 通知SDK连接断开
        if (muenDriver != null) {
            try {
                muenDriver.onControlTcpDisconnected();
                muenDriver.onMediaTcpDisconnected();
            } catch (Exception e) {
                log.error("通知SDK连接断开失败: userId={}", userId, e);
            }
        }
        
        // 关闭浏览器驱动
        if (chromeDriver != null) {
            try {
                // 这里需要调用chromeDriver.quit()，暂时使用反射或后续实现
                log.debug("关闭浏览器驱动: userId={}", userId);
            } catch (Exception e) {
                log.error("关闭浏览器驱动失败: userId={}", userId, e);
            }
        }
        
        setStatus(BrowserStatus.CLOSED);
    }
    
    /**
     * 关闭实例并释放资源
     */
    public void closeInstance() {
        log.info("关闭浏览器实例: userId={}", userId);
        closeApp();
    }
    
    public enum BrowserStatus {
        INITIALIZING("初始化中"),
        PRE_OPENING("预开启中"),
        CONNECTING("连接中"),
        READY("就绪"),
        RUNNING("运行中"),
        RECORDING("录制中"),
        CLOSING("关闭中"),
        CLOSED("已关闭"),
        OPEN_ERROR("开启错误"),
        CONNECTION_ERROR("连接错误"),
        PAGE_CONTROL_ERROR("页面控制错误"),
        NETWORK_ERROR("网络错误"),
        MEMORY_ERROR("内存错误");
        
        private final String description;
        
        BrowserStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        /**
         * 判断是否为健康状态
         */
        public boolean isHealthy() {
            return this == READY || this == PRE_OPENING || this == CONNECTING || 
                   this == RUNNING || this == RECORDING;
        }
        
        /**
         * 判断是否为终止状态
         */
        public boolean isTerminal() {
            return this == CLOSED;
        }
    }
}
