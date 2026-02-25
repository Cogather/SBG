# CSP接口适配层架构设计文档

## 文档信息

| 项目 | BrowserGateway |
|------|----------------|
| 文档类型 | CSP接口适配层架构设计 |
| 版本 | 1.0 |
| 日期 | 2026-02-13 |

---

## 1. 概述

### 1.1 设计目标

1. **解耦CSP接口依赖**：通过适配层隔离CSP SDK的直接依赖
2. **提高扩展性**：支持在外网环境中重写核心业务逻辑
3. **增强可维护性**：统一的接口抽象，便于后续升级和维护
4. **支持多环境部署**：内网使用CSP SDK，外网使用自定义实现
5. **降低测试复杂度**：Mock接口更容易实现

### 1.2 设计原则

- **依赖倒置原则**：依赖抽象而非具体实现
- **开闭原则**：对扩展开放，对修改关闭
- **单一职责原则**：每个适配器只负责一个CSP模块
- **接口隔离原则**：使用专用接口而非通用接口

---

## 2. 架构设计

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    BrowserGateway 应用层                        │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                CSP接口适配层 (CSP Adapter Layer)            │
│  ┌──────────┬──────────┬──────────┬──────────┬─────────┐   │
│  │ Framework│  Alarm   │ Certificate│ Service │ System  │   │
│  │  Adapter │ Adapter  │   Adapter  │ Adapter │ Adapter │   │
│  └──────────┴──────────┴──────────┴──────────┴─────────┘   │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                  适配器提供层 (Provider Layer)               │
│  ┌────────────────────┬─────────────────────────────────┐  │
│  │  CSP SDK Provider  │      Custom Provider             │  │
│  │  (内网环境使用)      │       (外网环境使用)              │  │
│  └────────────────────┴─────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                 具体实现层 (Implementation Layer)           │
│  ┌──────────┬──────────┬──────────┬──────────┬─────────┐   │
│  │  CSP SDK │  CSP SDK │  CSP SDK  │  Spring  │  Local  │   │
│  │  Framework│  Alarm   │ Certificate│  Cloud   │  Impl   │   │
│  └──────────┴──────────┴──────────┴──────────┴─────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 分层说明

1. **应用层**：业务逻辑层，直接使用适配层接口
2. **适配层**：统一的接口抽象，屏蔽底层实现差异
3. **提供层**：根据环境选择不同的实现提供者
4. **实现层**：具体的实现（CSP SDK或自定义）

---

## 3. 核心接口定义

### 3.1 框架启动接口

```java
package com.huawei.browsergateway.adapter;

/**
 * 框架适配器接口
 * 职责：管理CSE框架和OM SDK的初始化与生命周期
 */
public interface FrameworkAdapter {
    
    /**
     * 启动框架
     * @return 启动是否成功
     */
    boolean start();
    
    /**
     * 停止框架
     * @return 停止是否成功
     */
    boolean stop();
    
    /**
     * 初始化OM SDK
     * @return 初始化是否成功
     */
    boolean initializeOmSdK();
    
    /**
     * 检查框架是否已启动
     * @return 框架是否已启动
     */
    boolean isStarted();
}
```

### 3.2 告警接口

```java
package com.huawei.browsergateway.adapter;

/**
 * 告警适配器接口
 * 职责：处理告警的发送、清除和历史查询
 */
public interface AlarmAdapter {
    
    /**
     * 发送告警
     * @param alarmId 告警ID
     * @param type 告警类型
     * @param parameters 告警参数
     * @return 发送是否成功
     */
    boolean sendAlarm(String alarmId, AlarmType type, Map<String, String> parameters);
    
    /**
     * 清除告警
     * @param alarmId 告警ID
     * @return 清除是否成功
     */
    boolean clearAlarm(String alarmId);
    
    /**
     * 批量发送告警（支持重试）
     * @param alarms 告警列表
     * @param maxRetry 最大重试次数
     * @return 成功发送的告警数量
     */
    int sendAlarmsBatch(List<AlarmRequest> alarms, int maxRetry);
    
    /**
     * 查询历史告警
     * @param alarmIds 告警ID列表
     * @return 历史告警信息
     */
    List<AlarmInfo> queryHistoricalAlarms(List<String> alarmIds);
    
    /**
     * 告警类型枚举
     */
    enum AlarmType {
        GENERATE,
        CLEAR
    }
}
```

### 3.3 证书管理接口

```java
package com.huawei.browsergateway.adapter;

/**
 * 证书适配器接口
 * 职责：管理证书的订阅、更新和获取
 */
public interface CertificateAdapter {
    
    /**
     * 订阅证书
     * @param serviceName 服务名称
     * @param certScenes 证书场景列表
     * @param certPath 证书存储路径
     * @param callback 证书更新回调
     * @return 订阅是否成功
     */
    boolean subscribeCertificates(String serviceName, List<CertScene> certScenes, 
        String certPath, CertUpdateCallback callback);
    
    /**
     * 获取CA证书内容
     * @return CA证书内容
     */
    String getCaCertificate();
    
    /**
     * 获取设备证书内容
     * @return 设备证书内容
     */
    String getDeviceCertificate();
    
    /**
     * 获取私钥内容（已转换为PKCS#8格式）
     * @return 私钥内容
     */
    String getPrivateKey();
    
    /**
     * 检查证书是否就绪
     * @return 证书是否就绪
     */
    boolean isCertificateReady();
    
    /**
     * 初始化证书SDK
     * @return 初始化是否成功
     */
    boolean initialize();
}
```

### 3.4 服务管理接口

```java
package com.huawei.browsergateway.adapter;

/**
 * 服务管理适配器接口
 * 职责：服务注册、发现、属性上报
 */
public interface ServiceManagementAdapter {
    
    /**
     * 上报服务实例属性
     * @param properties 属性键值对
     * @return 上报是否成功
     */
    boolean reportInstanceProperties(Map<String, String> properties);
    
    /**
     * 获取服务实例属性
     * @param key 属性键
     * @return 属性值
     */
    String getInstanceProperty(String key);
    
    /**
     * 查找服务实例
     * @param serviceName 服务名称
     * @return 服务实例列表
     */
    List<ServiceInstance> findServiceInstances(String serviceName);
    
    /**
     * 获取当前服务实例信息
     * @return 当前服务实例
     */
    ServiceInstance getCurrentInstance();
    
    /**
     * 注册REST服务
     * @param schemaId Schema ID
     * @param serviceInstance 服务实例
     * @return 注册是否成功
     */
    boolean registerRestService(String schemaId, Object serviceInstance);
}
```

### 3.5 系统工具接口

```java
package com.huawei.browsergateway.adapter;

/**
 * 系统工具适配器接口
 * 职责：环境变量读取、本地配置管理
 */
public interface SystemUtilAdapter {
    
    /**
     * 从环境变量获取字符串值
     * @param key 环境变量键
     * @param defaultValue 默认值
     * @return 环境变量值
     */
    String getEnvString(String key, String defaultValue);
    
    /**
     * 从环境变量获取整数值
     * @param key 环境变量键
     * @param defaultValue 默认值
     * @return 环境变量值
     */
    int getEnvInteger(String key, int defaultValue);
    
    /**
     * 设置环境变量（用于测试）
     * @param key 键
     * @param value 值
     */
    void setEnv(String key, String value);
}
```

### 3.6 资源监控接口

```java
package com.huawei.browsergateway.adapter;

/**
 * 资源监控适配器接口
 * 职责：CPU、内存等系统资源监控
 */
public interface ResourceMonitorAdapter {
    
    /**
     * 获取CPU使用率
     * @return CPU使用率百分比（0-100）
     */
    float getCpuUsage();
    
    /**
     * 获取内存使用率
     * @return 内存使用率百分比（0-100）
     */
    float getMemoryUsage();
    
    /**
     * 获取网络带宽使用率
     * @return 带宽使用率百分比（0-100）
     */
    float getNetworkUsage();
    
    /**
     * 获取资源统计信息
     * @param metricType 指标类型（cpu、memory、network）
     * @return 资源统计信息
     */
    ResourceStatistics getStatistics(String metricType);
}
```

---

## 4. 数据传输对象（DTO）

### 4.1 证书相关DTO

```java
package com.huawei.browsergateway.adapter.dto;

import lombok.Data;

/**
 * 证书场景
 */
@Data
public class CertScene {
    private String sceneName;
    private String sceneDescCN;
    private String sceneDescEN;
    private SceneType sceneType;
    private int feature;
    
    public enum SceneType {
        CA,
        DEVICE
    }
}

/**
 * 证书更新回调接口
 */
public interface CertUpdateCallback {
    /**
     * 证书更新时的回调
     * @param caContent CA证书内容
     * @param deviceContent 设备证书内容
     */
    void onCertificateUpdate(String caContent, String deviceContent);
}
```

### 4.2 服务相关DTO

```java
package com.huawei.browsergateway.adapter.dto;

import lombok.Data;

import java.util.List;

/**
 * 服务实例
 */
@Data
public class ServiceInstance {
    private String instanceId;
    private String serviceName;
    private String host;
    private int port;
    private String protocol;
    private InstanceStatus status;
    private Map<String, String> properties;
    
    public enum InstanceStatus {
        UP, DOWN, STARTING, OUT_OF_SERVICE
    }
}

/**
 * 资源统计信息
 */
@Data
public class ResourceStatistics {
    private boolean success;
    private float ratio;
    private long timestamp;
    private long available;
    private long capacity;
}
```

### 4.3 告警相关DTO

```java
package com.huawei.browsergateway.adapter.dto;

import lombok.Data;

/**
 * 告警请求
 */
@Data
public class AlarmRequest {
    private String alarmId;
    private AlarmAdapter.AlarmType type;
    private Map<String, String> parameters;
    private Integer maxRetry;
}

/**
 * 告警信息
 */
@Data
public class AlarmInfo {
    private String alarmId;
    private String message;
    private String source;
    private String kind;
    private String name;
    private String namespace;
    private long timestamp;
}
```

---

## 5. 适配器工厂设计

### 5.1 适配器工厂接口

```java
package com.huawei.browsergateway.adapter.factory;

/**
 * 适配器工厂接口
 * 职责：根据环境创建合适的适配器实例
 */
public interface AdapterFactory {
    
    /**
     * 创建框架适配器
     * @return 框架适配器实例
     */
    FrameworkAdapter createFrameworkAdapter();
    
    /**
     * 创建告警适配器
     * @return 告警适配器实例
     */
    AlarmAdapter createAlarmAdapter();
    
    /**
     * 创建证书适配器
     * @return 证书适配器实例
     */
    CertificateAdapter createCertificateAdapter();
    
    /**
     * 创建服务管理适配器
     * @return 服务管理适配器实例
     */
    ServiceManagementAdapter createServiceManagementAdapter();
    
    /**
     * 创建系统工具适配器
     * @return 系统工具适配器实例
     */
    SystemUtilAdapter createSystemUtilAdapter();
    
    /**
     * 创建资源监控适配器
     * @return 资源监控适配器实例
     */
    ResourceMonitorAdapter createResourceMonitorAdapter();
}
```

### 5.2 环境感知工厂实现

```java
package com.huawei.browsergateway.adapter.factory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 环境感知适配器工厂
 * 根据配置自动选择CSP SDK实现或自定义实现
 */
@Component
public class EnvironmentAwareAdapterFactory implements AdapterFactory {
    
    @Value("${adapter.provider.type:default}")
    private AdapterProviderType providerType;
    
    private final CspSdkAdapterFactory cspSdkAdapterFactory;
    private final CustomAdapterFactory customAdapterFactory;
    
    public EnvironmentAwareAdapterFactory(
        CspSdkAdapterFactory cspSdkAdapterFactory,
        CustomAdapterFactory customAdapterFactory) {
        this.cspSdkAdapterFactory = cspSdkAdapterFactory;
        this.customAdapterFactory = customAdapterFactory;
    }
    
    @Override
    public FrameworkAdapter createFrameworkAdapter() {
        return getFactory().createFrameworkAdapter();
    }
    
    @Override
    public AlarmAdapter createAlarmAdapter() {
        return getFactory().createAlarmAdapter();
    }
    
    @Override
    public CertificateAdapter createCertificateAdapter() {
        return getFactory().createCertificateAdapter();
    }
    
    @Override
    public ServiceManagementAdapter createServiceManagementAdapter() {
        return getFactory().createServiceManagementAdapter();
    }
    
    @Override
    public SystemUtilAdapter createSystemUtilAdapter() {
        return getFactory().createSystemUtilAdapter();
    }
    
    @Override
    public ResourceMonitorAdapter createResourceMonitorAdapter() {
        return getFactory().createResourceMonitorAdapter();
    }
    
    private AdapterFactory getFactory() {
        switch (providerType) {
            case CSP_SDK:
                return cspSdkAdapterFactory;
            case CUSTOM:
                return customAdapterFactory;
            default:
                // 默认使用CSP SDK
                return cspSdkAdapterFactory;
        }
    }
    
    public enum AdapterProviderType {
        CSP_SDK,  // CSP SDK实现
        CUSTOM   // 自定义实现
    }
}
```

---

## 6. CSP SDK适配器实现

### 6.1 框架适配器CSP SDK实现

```java
package com.huawei.browsergateway.adapter.impl;

import com.huawei.adapter.FrameworkAdapter;
import com.huawei.csp.csejsdk.core.api.Framework;
import com.huawei.csp.om.transport.vertx.init.OmsdkStarter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 框架适配器 - CSP SDK实现
 * 适用场景：内网环境，使用华为CSP SDK
 */
@Component("cspFrameworkAdapter")
public class CspFrameworkAdapter implements FrameworkAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CspFrameworkAdapter.class);
    
    private boolean isStarted = false;
    
    @Override
    public boolean start() {
        try {
            Framework.start();
            isStarted = true;
            logger.info("CSE Framework started successfully");
            return true;
        } catch (Exception e) {
            logger.error("Failed to start CSE Framework", e);
            return false;
        }
    }
    
    @Override
    public boolean stop() {
        // CSE框架通常不需要显式停止
        isStarted = false;
        logger.info("CSE Framework stopped");
        return true;
    }
    
    @Override
    public boolean initializeOmSdK() {
        try {
            OmsdkStarter.omsdkInit();
            logger.info("OM SDK initialized successfully");
            return true;
        } catch (Exception e) {
            logger.error("Failed to initialize OM SDK", e);
            return false;
        }
    }
    
    @Override
    public boolean isStarted() {
        return isStarted;
    }
}
```

### 6.2 告警适配器CSP SDK实现

```java
package com.huawei.browsergateway.adapter.impl;

import com.huawei.adapter.AlarmAdapter;
import com.huawei.adapter.dto.AlarmInfo;
import com.huawei.adapter.dto.AlarmRequest;
import com.huawei.csp.om.alarmsdk.alarmmanager.Alarm;
import com.huawei.csp.om.alarmsdk.alarmmanager.AlarmSendManager;
import com.huawei.csp.om.alarmsdk.alarmmodel.AlarmModel;
import com.huawei.csp.om.alarmsdk.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警适配器 - CSP SDK实现
 */
@Component("cspAlarmAdapter")
public class CspAlarmAdapter implements AlarmAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CspAlarmAdapter.class);
    
    private static final String GET_ALARM_INTERFACE = "cse://FMService/fmOperation/v1/alarms/get_alarms";
    
    private final RestTemplate restTemplate;
    private final Map<String, Long> lastAlarmTime = new ConcurrentHashMap<>();
    
    private static final long ALARM_DEDUPE_INTERVAL = 10 * 60 * 1000; // 10分钟
    
    @Autowired
    public CspAlarmAdapter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    @Override
    public boolean sendAlarm(String alarmId, AlarmType type, Map<String, String> parameters) {
        // 告警去重检查
        if (System.currentTimeMillis() - lastAlarmTime.getOrDefault(alarmId, 0L) < ALARM_DEDUPE_INTERVAL) {
            logger.warn("Alarm {} was already reported within 10 minutes; skipping", alarmId);
            return false;
        }
        
        try {
            AlarmModel.EuGenClearType alarmType = convertAlarmType(type);
            Alarm alarm = new Alarm(alarmId, alarmType);
            
            // 设置默认参数
            alarm.appendParameter("source", StringUtil.getStringFromEnv("SERVICENAME", "browser-gateway"));
            alarm.appendParameter("kind", "service");
            alarm.appendParameter("name", StringUtil.getStringFromEnv("PODNAME", "unknown"));
            alarm.appendParameter("namespace", StringUtil.getStringFromEnv("NAMESPACE", "default"));
            alarm.appendParameter("EventSource", "BrowserGateway Service");
            alarm.appendParameter("OriginalEventTime", DateUtil.getCurrentDateTime());
            
            // 设置自定义参数
            if (parameters != null) {
                parameters.forEach(alarm::appendParameter);
            }
            
            boolean success = AlarmSendManager.getInstance().sendAlarm(alarm);
            if (success) {
                lastAlarmTime.put(alarmId, System.currentTimeMillis());
            }
            return success;
        } catch (Exception e) {
            logger.error("Failed to send alarm {}", alarmId, e);
            return false;
        }
    }
    
    @Override
    public boolean clearAlarm(String alarmId) {
        if (!lastAlarmTime.containsKey(alarmId)) {
            return false;
        }
        
        try {
            Alarm alarm = new Alarm(alarmId, AlarmModel.EuGenClearType.CLEAR);
            boolean success = AlarmSendManager.getInstance().sendAlarm(alarm);
            if (success) {
                lastAlarmTime.remove(alarmId);
            }
            return success;
        } catch (Exception e) {
            logger.error("Failed to clear alarm {}", alarmId, e);
            return false;
        }
    }
    
    @Override
    public int sendAlarmsBatch(List<AlarmRequest> alarms, int maxRetry) {
        // 默认按顺序发送，可以优化为并行发送
        int successCount = 0;
        for (AlarmRequest alarm : alarms) {
            boolean success = sendAlarmWithRetry(alarm, maxRetry);
            if (success) {
                successCount++;
            }
        }
        return successCount;
    }
    
    @Override
    public List<AlarmInfo> queryHistoricalAlarms(List<String> alarmIds) {
        try {
            String jsonParam = String.format("{\"cmd\":\"GET_ACTIVE_ALARMS\",\"language\":\"en-us\",\"data\":{\"appId\":\"%s\",\"alarmIds\":\"%s\"}}",
                StringUtil.getStringFromEnv("APPID", "0"), String.join(",", alarmIds));
            
            // 使用HTTP调用查询历史告警
            // ... 实现省略
            
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Failed to query historical alarms", e);
            return new ArrayList<>();
        }
    }
    
    private boolean sendAlarmWithRetry(AlarmRequest alarm, int maxRetry) {
        int maxRetryCount = maxRetry != null ? maxRetry : 2;
        int retryCount = 0;
        long retryDelay = 5000L; // 5秒
        
        while (retryCount < maxRetryCount) {
            try {
                if (sendAlarm(alarm.getAlarmId(), alarm.getType(), alarm.getParameters())) {
                    return true;
                }
            } catch (Exception e) {
                logger.error("Failed to send alarm {} on attempt {}/{}", 
                    alarm.getAlarmId(), retryCount + 1, maxRetryCount, e);
            }
            
            retryCount++;
            if (retryCount < maxRetryCount) {
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return false;
    }
    
    private AlarmModel.EuGenClearType convertAlarmType(AlarmType type) {
        switch (type) {
            case GENERATE: return AlarmModel.EuGenClearType.GENERATE;
            case CLEAR: return AlarmModel.EuGenClearType.CLEAR;
            default: return AlarmModel.EuGenClearType.GENERATE;
        }
    }
}
```

---

## 7. 自定义适配器实现（外网环境）

### 7.1 框架适配器自定义实现

```java
package com.huawei.browsergateway.adapter.impl;

import com.huawei.adapter.FrameworkAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 框架适配器 - 自定义实现
 * 适用场景：外网环境，使用本地逻辑替代CSP SDK
 */
@Component("customFrameworkAdapter")
public class CustomFrameworkAdapter implements FrameworkAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomFrameworkAdapter.class);
    
    private boolean isStarted = false;
    
    @Override
    public boolean start() {
        // 本地环境无需启动CSE框架
        isStarted = true;
        logger.info(" Custom Framework started (no-op for external environment)");
        return true;
    }
    
    @Override
    public boolean stop() {
        isStarted = false;
        logger.info("Custom Framework stopped");
        return true;
    }
    
    @Override
    public boolean initializeOmSdK() {
        // 本地环境无需初始化OM SDK
        logger.info("Custom OM SDK initialized (no-op for external environment)");
        return true;
    }
    
    @Override
    public boolean isStarted() {
        return isStarted;
    }
}
```

### 7.2 告警适配器自定义实现

```java
package com.huawei.browsergateway.adapter.impl;

import com.huawei.adapter.AlarmAdapter;
import com.huawei.adapter.dto.AlarmInfo;
import com.huawei.adapter.dto.AlarmRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 告警适配器 - 自定义实现
 * 适用场景：外网环境，将告警写入本地日志文件或发送到监控系统
 */
@Component("customAlarmAdapter")
public class CustomAlarmAdapter implements AlarmAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomAlarmAdapter.class);
    
    private static final long ALARM_DEDUPE_INTERVAL = 10 * 60 * 1000;
    
    private final Map<String, Long> lastAlarmTime = new ConcurrentHashMap<>();
    private final AtomicInteger alarmCounter = new AtomicInteger(0);
    private final String logFilePath = "/tmp/browsergw_alarms.log";
    
    @Override
    public boolean sendAlarm(String alarmId, AlarmType type, Map<String, String> parameters) {
        // 去重检查
        if (System.currentTimeMillis() - lastAlarmTime.getOrDefault(alarmId, 0L) < ALARM_DEDUPE_INTERVAL) {
            logger.warn("Alarm {} was already reported within 10 minutes; skipping", alarmId);
            return false;
        }
        
        try {
            // 记录告警到日志文件
            String alarmLog = formatAlarmLog(alarmId, type, parameters);
            writeAlarmLog(alarmLog);
            
            // 同时记录到应用日志
            logger.warn("ALARM TRIGGERED: {}", alarmLog);
            
            // 更新最后发送时间
            lastAlarmTime.put(alarmId, System.currentTimeMillis());
            alarmCounter.incrementAndGet();
            
            return true;
        } catch (Exception e) {
            logger.error("Failed to send alarm {}", alarmId, e);
            return false;
        }
    }
    
    @Override
    public boolean clearAlarm(String alarmId) {
        if (!lastAlarmTime.containsKey(alarmId)) {
            return false;
        }
        
        try {
            String clearLog = formatClearLog(alarmId);
            writeAlarmLog(clearLog);
            logger.info("Alarm cleared: {}", alarmId);
            
            lastAlarmTime.remove(alarmId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to clear alarm {}", alarmId, e);
            return false;
        }
    }
    
    @Override
    public int sendAlarmsBatch(List<AlarmRequest> alarms, int maxRetry) {
        int successCount = 0;
        for (AlarmRequest alarm : alarms) {
            boolean success = sendAlarm(alarm.getAlarmId(), alarm.getType(), alarm.getParameters());
            if (success) {
                successCount++;
            }
        }
        return successCount;
    }
    
    @Override
    public List<AlarmInfo> queryHistoricalAlarms(List<String> alarmIds) {
        // 外网环境不支持历史告警查询，返回空列表
        return new ArrayList<>();
    }
    
    private String formatAlarmLog(String alarmId, AlarmType type, Map<String, String> parameters) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(LocalDateTime.now().format(formatter)).append("] ");
        sb.append("ALARM_ID=").append(alarmId).append(" ");
        sb.append("TYPE=").append(type).append(" ");
        
        if (parameters != null) {
            parameters.forEach((k, v) -> sb.append(k).append("=").append(v).append(" "));
        }
        
        return sb.toString();
    }
    
    private String formatClearLog(String alarmId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format("[%s] ALARM_CLEARED: ALARM_ID=%s", 
            LocalDateTime.now().format(formatter), alarmId);
    }
    
    private void writeAlarmLog(String logMessage) throws IOException {
        try (FileWriter writer = new FileWriter(logFilePath, true)) {
            writer.write(logMessage + "\n");
        }
    }
}
```

### 7.3 证书适配器自定义实现

```java
package com.huawei.browsergateway.adapter.impl;

import com.huawei.adapter.CertificateAdapter;
import com.huawei.adapter.dto.CertScene;
import com.huawei.adapter.dto.CertUpdateCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 证书适配器 - 自定义实现
 * 适用场景：外网环境，从本地文件加载证书或生成自签名证书
 */
@Component("customCertificateAdapter")
public class CustomCertificateAdapter implements CertificateAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomCertificateAdapter.class);
    
    private String caContent = "";
    private String deviceContent = "";
    private String privateKey = "";
    
    private final List<CertUpdateCallback> callbacks = new CopyOnWriteArrayList<>();
    
    @Value("${adapter.custom.certificate.ca-path:}")
    private String caCertPath;
    
    @Value("${adapter.custom.certificate.cert-path:}")
    private String deviceCertPath;
    
    @Value("${adapter.custom.certificate.key-path:}")
    private String privateKeyPath;
    
    @PostConstruct
    public void initializeCustomCertificates() {
        // 尝试从配置的路径加载证书文件
        loadCertificatesFromFile();
    }
    
    @Override
    public boolean subscribeCertificates(String serviceName, List<CertScene> certScenes, 
        String certPath, CertUpdateCallback callback) {
        // 外网环境：生成自签名证书或使用本地证书
        logger.info("Certificate subscription for external environment (using local certificates)");
        
        if (callback != null) {
            callbacks.add(callback);
            // 立即触发一次回调
            callback.onCertificateUpdate(caContent, deviceContent);
        }
        return true;
    }
    
    @Override
    public String getCaCertificate() {
        return caContent;
    }
    
    @Override
    public String getDeviceCertificate() {
        return deviceContent;
    }
    
    @Override
    public String getPrivateKey() {
        return privateKey;
    }
    
    @Override
    public boolean isCertificateReady() {
        return caContent != null && !caContent.isEmpty() && 
               deviceContent != null && !deviceContent.isEmpty();
    }
    
    @Override
    public boolean initialize() {
        // 生成自签名证书或加载本地证书
        try {
            if (loadCertificatesFromFile() || generateSelfSignedCertificates()) {
                logger.info("Certificates initialized successfully");
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Failed to initialize certificates", e);
            return false;
        }
    }
    
    private boolean loadCertificatesFromFile() {
        try {
            if (caCertPath != null && !caCertPath.isEmpty()) {
                caContent = new String(Files.readAllBytes(Paths.get(caCertPath)));
            }
            if (deviceCertPath != null && !deviceCertPath.isEmpty()) {
                deviceContent = new String(Files.readAllBytes(Paths.get(deviceCertPath)));
            }
            if (privateKeyPath != null && !privateKeyPath.isEmpty()) {
                privateKey = new String(Files.readAllBytes(Paths.get(privateKeyPath)));
            }
            return isCertificateReady();
        } catch (IOException e) {
            logger.warn("Failed to load certificates from file", e);
            return false;
        }
    }
    
    private boolean generateSelfSignedCertificates() {
        // 生成自签名证书的逻辑
        // 可以使用Java的KeyTool或BouncyCastle生成
        // ... 实现省略
        logger.info("Generated self-signed certificates");
        return true;
    }
}
```

---

## 8. 配置类设计

### 8.1 适配器配置

```java
package com.huawei.browsergateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 适配器配置类
 */
@Configuration
@ConfigurationProperties(prefix = "adapter")
public class AdapterConfig {
    
    private Provider provider = new Provider();
    
    public static class Provider {
        private Type type = Type.CSP_SDK;
        private boolean enableMock = false;
        
        public enum Type {
            CSP_SDK,
            CUSTOM
        }
    }
    
    public Provider getProvider() {
        return provider;
    }
}
```

### 8.2 Spring Boot配置

```yaml
# application.yaml

# 适配器提供者配置
adapter:
  provider:
    type: csp-sdk  # 或 custom
    enable-mock: false
  
  # 自定义实现配置
  custom:
    certificate:
      ca-path: /path/to/ca.crt
      cert-path: /path/to/device.crt
      key-path: /path/to/device.key
    alarm:
      log-path: /tmp/browsergw_alarms.log
    service:
      mock-instances: true
```

---

## 9. 使用示例

### 9.1 在业务代码中使用适配器

```java
package com.huawei.browsergateway.service;

import com.huawei.adapter.AlarmAdapter;
import com.huawei.adapter.FrameworkAdapter;
import com.huawei.adapter.dto.CertScene;
import com.huawei.adapter.dto.CertUpdateCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 示例服务：展示如何在业务代码中使用适配器
 * 无需直接依赖CSP SDK
 */
@Service
public class ExampleBusinessService {
    
    @Autowired
    private FrameworkAdapter frameworkAdapter;
    
    @Autowired
    private AlarmAdapter alarmAdapter;
    
    @Autowired
    private com.huawei.adapter.CertificateAdapter certificateAdapter;
    
    public void startService() {
        // 启动框架
        boolean frameworkStarted = frameworkAdapter.start();
        if (!frameworkStarted) {
            throw new RuntimeException("Failed to start framework");
        }
        
        // 初始化证书SDK
        certificateAdapter.initialize();
        
        // 订阅证书更新
        List<CertScene> certScenes = List.of(
            new CertScene("sbg_server_ca_certificate", CertScene.SceneType.CA),
            new CertScene("sbg_server_device_certificate", CertScene.SceneType.DEVICE)
        );
        
        certificateAdapter.subscribeCertificates("browsergw", certScenes, "/opt/csp/browsergw", 
            new CertUpdateCallback() {
                @Override
                public void onCertificateUpdate(String caContent, String deviceContent) {
                    System.out.println("证书已更新");
                    // 处理证书更新逻辑
                }
            });
    }
    
    public void sendAlarmExample(String alarmId, String message) {
        // 发送告警
        Map<String, String> params = new HashMap<>();
        params.put("EventMessage", message);
        params.put("Severity", "MAJOR");
        
        boolean success = alarmAdapter.sendAlarm(alarmId, AlarmAdapter.AlarmType.GENERATE, params);
        
        if (!success) {
            throw new RuntimeException("Failed to send alarm");
        }
    }
}
```

---

## 10. 测试策略

### 10.1 单元测试 - 使用Mock实现

```java
package com.huawei.browsergateway.adapter;

import com.huawei.adapter.dto.CertScene;
import com.huawei.adapter.impl.MockFrameworkAdapter;
import com.huawei.adapter.impl.MockAlarmAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 适配器单元测试
 */
public class AdapterTest {
    
    private MockFrameworkAdapter frameworkAdapter;
    private MockAlarmAdapter alarmAdapter;
    
    @BeforeEach
    public void setUp() {
        frameworkAdapter = new MockFrameworkAdapter();
        alarmAdapter = new MockAlarmAdapter();
    }
    
    @Test
    public void testFrameworkStart() {
        boolean started = frameworkAdapter.start();
        assertTrue(started);
        assertTrue(frameworkAdapter.isStarted());
    }
    
    @Test
    public void testAlarmSend() {
        boolean sent = alarmAdapter.sendAlarm("1001", AlarmAdapter.AlarmType.GENERATE, 
            Collections.singletonMap("message", "Test alarm"));
        
        assertTrue(sent);
        assertEquals(1, alarmAdapter.getSentAlarmCount());
        assertTrue(alarmAdapter.hasSentAlarm("1001"));
    }
    
    @Test
    public void testAlarmDedup() {
        // 发送两次相同告警
        alarmAdapter.sendAlarm("1002", AlarmAdapter.AlarmType.GENERATE, 
            Collections.singletonMap("message", "First alarm"));
        
        boolean secondSent = alarmAdapter.sendAlarm("1002", AlarmAdapter.AlarmType.GENERATE, 
            Collections.singletonMap("message", "Second alarm"));
        
        assertFalse(secondSent); // 第二次应该被去重
    }
}
```

### 10.2 集成测试 - 使用自定义实现

```java
package com.huawei.browsergateway.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 自定义适配器集成测试
 */
@SpringBootTest
@ActiveProfiles("custom")
public class CustomAdapterTest {
    
    @Autowired
    private FrameworkAdapter frameworkAdapter;
    
    @Autowired
    private AlarmAdapter alarmAdapter;
    
    @Test
    public void testCustomFramework() {
        boolean started = frameworkAdapter.start();
        assertTrue(started);
        assertTrue(frameworkAdapter.isStarted());
    }
    
    @Test
    public void testCustomAlarm() {
        boolean sent = alarmAdapter.sendAlarm("1001", AlarmAdapter.AlarmType.GENERATE,
            java.util.Collections.singletonMap("message", "Test"));
        
        assertTrue(sent);
        // 检查自定义实现行为（如日志文件）
    }
}
```

---

## 11. 迁移指南

### 11.1 从直接使用CSP SDK迁移到适配器

#### 迁移前（直接使用CSP SDK）：

```java
// 原代码直接依赖CSP SDK
import com.huawei.csp.csejsdk.core.api.Framework;
import com.huawei.csp.om.alarmsdk.alarmmanager.*;

public class OldService {
    public void start() {
        Framework.start();
        OmsdkStarter.omsdkInit();
    }
    
    public void sendAlarm(String id) {
        Alarm alarm = new Alarm(id, AlarmModel.EuGenClearType.GENERATE);
        AlarmSendManager.getInstance().sendAlarm(alarm);
    }
}
```

#### 迁移后（使用适配器）：

```java
// 新代码使用适配器，不依赖CSP SDK
import com.huawei.browsergateway.adapter.FrameworkAdapter;
import com.huawei.browsergateway.adapter.AlarmAdapter;

public class NewService {
    @Autowired
    private FrameworkAdapter frameworkAdapter;
    
    @Autowired
    private AlarmAdapter alarmAdapter;
    
    public void start() {
        frameworkAdapter.start();
        frameworkAdapter.initializeOmSdK();
    }
    
    public void sendAlarm(String id) {
        alarmAdapter.sendAlarm(id, AlarmAdapter.AlarmType.GENERATE, null);
    }
}
```

### 11.2 迁移步骤

1. **添加适配器依赖**：将适配器模块的jar包添加到项目
2. **修改Spring配置**：配置adapter.provider.type
3. **替换导入语句**：将CSP SDK的导入替换为适配器导入
4. **修改代码**：替换直接API调用为适配器方法调用
5. **运行测试**：确保功能正常
6. **部署验证**：在生产环境验证部署
7. **移除CSP依赖**：确认功能正常后，移除CSP SDK依赖（可选）

---

## 12. 总结与建议

### 12.1 架构优势

1. **完全解耦**：业务代码完全不依赖CSP SDK
2. **灵活切换**：通过配置即可在不同实现间切换
3. **易于测试**：可以轻松Mock各个适配器
4. **支持多环境**：内网使用CSP SDK，外网使用自定义实现
5. **代码质量**：符合SOLID原则，易于维护和扩展

### 12.2 后续优化建议

1. **性能优化**：对于高频调用的接口，考虑添加缓存
2. **监控完善**：为适配器添加详细的监控指标
3. **文档补充**：为每个适配器添加详细的API文档
4. **错误处理**：完善错误处理和重试机制
5. **配置中心**：支持从配置中心动态加载配置

### 12.3 注意事项

1. **确保接口一致性**：自定义实现的行为应与CSP SDK实现保持一致
2. **日志完整性**：确保关键操作都有日志记录
3. **异常处理**：妥善处理所有可能的异常情况
4. **兼容性测试**：在迁移前进行充分的测试
5. **回滚方案**：准备快速回滚到原实现的方案

---

## 13. 附录

### 13.1 文件结构

```
browser-gateway/src/main/java/com/huawei/browsergateway/
├── adapter/                          # 适配器层
│   ├── FrameworkAdapter.java         # 框架适配器接口
│   ├── AlarmAdapter.java             # 告警适配器接口
│   ├── CertificateAdapter.java        # 证书适配器接口
│   ├── ServiceManagementAdapter.java  # 服务管理适配器接口
│   ├── SystemUtilAdapter.java        # 系统工具适配器接口
│   ├── ResourceMonitorAdapter.java    # 资源监控适配器接口
│   ├── dto/                          # 数据传输对象
│   ├── factory/                      # 适配器工厂
│   │   ├── AdapterFactory.java
│   │   ├── EnvironmentAwareAdapterFactory.java
│   │   ├── CspSdkAdapterFactory.java
│   │   └── CustomAdapterFactory.java
│   └── impl/                         # 实现类
│       ├── csp/                      # CSP SDK实现
│       │   ├── CspFrameworkAdapter.java
│       │   ├── CspAlarmAdapter.java
│       │   └── ...
│       └── custom/                   # 自定义实现
│           ├── CustomFrameworkAdapter.java
│           ├── CustomAlarmAdapter.java
│           └── ...
├── config/                           # 配置类
│   └── AdapterConfig.java
└── service/                          # 业务服务（使用适配器）
    └── ExampleBusinessService.java