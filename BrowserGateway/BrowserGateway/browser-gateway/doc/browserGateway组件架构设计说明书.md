# BrowserGateway 组件设计说明书

## 文档信息

| 项目 | BrowserGateway |
|------|----------------|
| 文档类型 | 组件架构设计说明书      |
| 版本 | 2.2            |
| 日期 | 2025-02-14     |
| 状态 | 正式发布           |

---

## 修订历史

| 版本 | 日期 | 修订人 | 修订内容 |
|------|------|--------|----------|
| 1.0 | 2025-02-10 | 初始版本 | 基础架构设计 |
| 2.0 | 2025-02-12 | 系统重构 | 集成CSP适配层设计，明确CSP相关服务 |
| 2.1 | 2025-02-13 | 架构优化 | 重新梳理外部依赖服务，CSP相关服务全部归入适配层 |
| 2.2 | 2025-02-14 | 模块完善 | 新增公共、异常、SDK、路由模块，完善目录结构与实际代码一致性 |

---

## 目录

- [1. 概述](#1-概述)
- [2. 系统架构](#2-系统架构)
- [3. CSP接口适配层设计](#3-csp接口适配层设计)
- [4. 核心业务模块设计](#4-核心业务模块设计)
    - [4.0 公共模块](#40-公共模块)
    - [4.1 异常处理模块](#41-异常处理模块)
    - [4.2 SDK模块](#42-sdk模块)
    - [4.3 路由模块](#43-路由模块)
    - [4.4 会话管理模块](#44-会话管理模块)
    - [4.5 插件管理模块](#45-插件管理模块)
    - [4.6 媒体转发模块](#46-媒体转发模块)
    - [4.7 健康监控模块](#47-健康监控模块)
    - [4.8 证书管理模块](#48-证书管理模块)
    - [4.9 定时任务模块](#49-定时任务模块)
    - [4.10 告警管理模块](#410-告警管理模块)
    - [4.11 数据管理模块](#411-数据管理模块)
- [5. 数据模型设计](#5-数据模型设计)
- [6. 接口设计](#6-接口设计)
- [7. 安全设计](#7-安全设计)
- [8. 性能设计](#8-性能设计)
- [9. 可靠性设计](#9-可靠性设计)
- [10. 部署方案](#10-部署方案)
- [11. 监控与运维](#11-监控与运维)

---

## 1. 概述

### 1.1 系统定位

BrowserGateway 是云浏览器（SBG）系统的网关层组件，负责用户会话的创建、生命周期管理、数据上报、健康监控、告警推送等核心职责。它是浏览器实例（Chrome/Touch）与后端业务（CSE、Redis、S3、告警平台）之间的桥梁。

### 1.2 核心职责

1. **会话管理**：创建、维护、清理浏览器用户会话
2. **数据传输**：提供 REST API、TCP TLV、WebSocket 等多种通信方式
3. **插件管理**：支持插件的加载、热更新、状态查询
4. **媒体转发**：实时媒体流（WebCodecs/FFmpeg）转发
5. **健康管理**：健康检查、容量上报、告警推送
6. **证书管理**：TLS 证书动态订阅与更新

### 1.3 设计目标

1. **功能完整性**：支持内网和外网环境的完整功能
2. **环境适配性**：通过适配层支持内外网差异化部署
3. **高性能**：支持高并发、低延迟的媒体流转发
4. **可扩展性**：模块化设计，便于功能扩展
5. **可靠性**：完善的异常处理、降级、容错机制

---

## 2. 系统架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          外部依赖服务                                          │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │            CSP 相关服务 (通过适配层访问)                                 ││
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐││
│  │  │CSE Registry  │  │   OM Agent   │  │CertMgr Center│  │    FM Service │││
│  │  │(服务注册发现)  │  │  (运维管理)   │  │  (证书中心)   │  │  (告警管理)   │││
│  └──────────────┴──────────────┴──────────────┴──────────────┘            ││
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │           非CSP 相关服务 (直接访问)                                     ││
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                  ││
│  │  │    Redis     │  │  S3/MinIO    │  │  Chrome/Touch│                  ││
│  │  │  (缓存服务)    │  │ (对象存储)     │  │  (浏览器实例)  │                  ││
│  └──────────────┴──────────────┴──────────────┘                          ││
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 外部依赖服务分类

根据服务是否依赖CSP SDK，将外部依赖服务分为两类：

#### 2.2.1 CSP 相关服务（通过适配层访问）

| 服务名称 | CSP SDK 模块 | 适配器接口 | 功能说明 |
|---------|-------------|-----------|---------|
| **CSE Registry** | `csejsdk-core` | `FrameworkAdapter`<br>`ServiceManagementAdapter` | 微服务框架启动、服务注册、发现、属性上报 |
| **OM Agent** | `om-transport` | `FrameworkAdapter` | 运维管理SDK初始化 |
| **CertMgr Center** | `certmgr-jsdk` | `CertificateAdapter` | 证书SDK初始化、证书订阅、证书更新 |
| **FM Service** | `om-alarmsdk` | `AlarmAdapter` | 告警发送、告警清除、历史告警查询 |
| **AuditLog Service** | `jsf-api` | 扩展适配器 | 审计日志上报（通过CspRestTemplate） |

**特点**：
- 内网环境：直接调用 CSP SDK 访问服务
- 外网环境：通过自定义实现替代（Mock 或本地实现）
- 统一通过适配层隔离，业务代码无感知

#### 2.2.2 非 CSP 相关服务（直接访问）

| 服务名称 | 访问方式 | 功能说明 | 是否需要适配 |
|---------|---------|---------|------------|
| **Redis** | 直接使用 Jedis/Lettuce | 会话缓存、心跳、实例元数据 | 否 |
| **S3/MinIO** | 通过 AWS SDK | 用户数据持久化、插件包存储 | 否（已有FileStorageService封装） |
| **Chrome/Touch** | 通过 Selenium/Playwright | 浏览器实例创建和控制 | 否（已有驱动封装） |

**特点**：
- 内网和外网环境访问方式一致
- 不依赖 CSP SDK
- 可通过配置切换不同的服务地址（内网 MinIO vs 公有云 S3）

### 2.2 技术架构

#### 2.2.1 技术栈

| 层级 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 应用框架 | Spring Boot | 2.7.x | 依赖注入、配置管理 |
| Web 框架 | Spring MVC | 5.3.x | REST API |
| 网络 | Netty | 4.1.x | TCP TLV 服务器 |
| WebSocket | Yeauty | latest | WebSocket 框架 |
| 浏览器自动化 | Selenium 4 | 4.x | ChromeDriver 接口 |
| 浏览器自动化 | Playwright | 1.40.x | 预留接口 |
| 压缩 | Zstd-JNI | 1.5.x | 用户数据压缩 |
| 监控 | ServiceComb | latest | 微服务框架 |
| HTTP 客户端 | Apache HttpClient 5 | 5.x | HTTP 请求 |
| 对象存储 | AWS SDK S3 | 2.x | S3/MinIO |
| 媒体编解码 | FFmpeg-JavaCV | latest | 视频转码 |

#### 2.2.2 依赖关系

```xml
<!-- 核心依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- CSP SDK (内网环境) -->
<dependency>
    <groupId>com.huawei.csp</groupId>
    <artifactId>cse-sdk</artifactId>
    <version>2.8.x</version>
    <optional>true</optional>
</dependency>

<!-- 浏览器自动化 -->
<dependency>
    <groupId>org.seleniumhq.selenium</groupId>
    <artifactId>selenium-java</artifactId>
    <version>4.x</version>
</dependency>

<!-- 网络通信 -->
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-all</artifactId>
    <version>4.1.x</version>
</dependency>

<!-- 对象存储 -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.x</version>
</dependency>
```

### 2.3 模块划分

```
browser-gateway/
├── src/main/java/com/huawei/browsergateway/
│   ├── adapter/                    # CSP 适配层
│   │   ├── interfaces/            # 适配器接口
│   │   ├── factory/               # 适配器工厂
│   │   ├── impl/                  # 适配器实现
│   │   │   ├── csp/              # CSP SDK 实现
│   │   │   ├── custom/           # 自定义实现
│   │   │   └── mock/             # Mock 实现
│   │   └── dto/                   # 数据传输对象
│   ├── api/                       # REST API 层
│   ├── common/                    # 公共模块
│   │   ├── constant/             # 常量定义
│   │   ├── enums/                # 枚举类
│   │   ├── utils/                # 通用工具类
│   │   └── response/             # 通用响应类
│   ├── config/                    # 配置类
│   ├── entity/                    # 实体类
│   │   ├── alarm/                # 告警相关实体
│   │   ├── browser/              # 浏览器相关实体
│   │   ├── event/                # 事件相关实体
│   │   ├── operate/              # 操作相关实体
│   │   ├── plugin/               # 插件相关实体
│   │   ├── request/              # 请求相关实体
│   │   └── response/             # 响应相关实体
│   ├── exception/                 # 异常处理
│   │   ├── common/               # 通用异常
│   │   └── handler/              # 异常处理器
│   ├── router/                    # 路由模块
│   ├── scheduled/                 # 定时任务
│   ├── service/                   # 业务服务层
│   │   ├── healthCheck/          # 健康检查策略
│   │   └── impl/                 # 服务实现
│   ├── sdk/                       # SDK 相关代码
│   ├── tcpserver/                 # TCP 服务器
│   │   ├── cert/                 # 证书管理
│   │   ├── control/              # 控制流
│   │   └── media/                # 媒体流
│   ├── util/                      # 工具类
│   │   └── encode/               # 编码相关工具
│   └── websocket/                 # WebSocket 端点
│       ├── extension/            # 插件 WebSocket
│       └── media/                # 媒体 WebSocket
│           ├── ffmpeg/           # FFmpeg 处理器
│           └── webcodecs/        # WebCodecs 处理器
├── src/main/resources/
│   ├── lib/                      # 第三方库
│   ├── log/                      # 日志配置
│   ├── application.yaml           # 主配置
│   ├── application-csp.yaml       # 内网配置
│   └── application-custom.yaml    # 外网配置
└── src/test/java/com/huawei/browsergateway/
    └── util/                     # 测试工具类
```

---

## 3. CSP 接口适配层设计

### 3.1 适配层架构

#### 3.1.1 设计目标

1. **解耦依赖**：隔离 CSP SDK 直接依赖，支持外网环境部署
2. **统一接口**：提供统一的适配器接口，屏蔽实现差异
3. **环境切换**：通过配置切换内外网环境实现
4. **易于测试**：提供 Mock 实现，便于单元测试
5. **可扩展性**：支持新增适配器和实现

#### 3.1.2 核心适配器接口

##### 3.1.2.1 FrameworkAdapter

```java
package com.huawei.browsergateway.adapter;

public interface FrameworkAdapter {
    boolean start();
    boolean stop();
    boolean initializeOmSdK();
    boolean isStarted();
}
```

**职责**：管理 CSE 框架和 OM SDK 的初始化与生命周期

**内网实现**：调用 `Framework.start()` 和 `OmsdkStarter.omsdkInit()`
**外网实现**：空实现，返回 success

##### 3.1.2.2 AlarmAdapter

```java
package com.huawei.browsergateway.adapter;

import java.util.List;
import java.util.Map;

public interface AlarmAdapter {
    boolean sendAlarm(String alarmId, AlarmType type, Map<String, String> parameters);
    boolean clearAlarm(String alarmId);
    int sendAlarmsBatch(List<AlarmRequest> alarms, int maxRetry);
    List<AlarmInfo> queryHistoricalAlarms(List<String> alarmIds);

    enum AlarmType {
        GENERATE, CLEAR
    }
}
```

**职责**：处理告警的发送、清除和历史查询

**内网实现**：使用 `AlarmSendManager` 发送告警
**外网实现**：将告警写入日志文件

##### 3.1.2.3 CertificateAdapter

```java
package com.huawei.browsergateway.adapter;

import java.util.List;

public interface CertificateAdapter {
    boolean subscribeCertificates(String serviceName, List<CertScene> certScenes,
        String certPath, CertUpdateCallback callback);
    String getCaCertificate();
    String getDeviceCertificate();
    String getPrivateKey();
    boolean isCertificateReady();
    boolean initialize();
}
```

**职责**：管理证书的订阅、更新和获取

**内网实现**：从 CSP 证书中心订阅
**外网实现**：从本地文件加载或生成自签名证书

##### 3.1.2.4 ServiceManagementAdapter

```java
package com.huawei.browsergateway.adapter;

import java.util.List;
import java.util.Map;

public interface ServiceManagementAdapter {
    boolean reportInstanceProperties(Map<String, String> properties);
    String getInstanceProperty(String key);
    List<ServiceInstance> findServiceInstances(String serviceName);
    ServiceInstance getCurrentInstance();
    boolean registerRestService(String schemaId, Object serviceInstance);
}
```

**职责**：服务注册、发现、属性上报

**内网实现**：使用 ServiceComb 的服务注册发现
**外网实现**：使用本地内存服务注册表

##### 3.1.2.5 SystemUtilAdapter

```java
package com.huawei.browsergateway.adapter;

public interface SystemUtilAdapter {
    String getEnvString(String key, String defaultValue);
    int getEnvInteger(String key, int defaultValue);
    void setEnv(String key, String value);
}
```

**职责**：环境变量读取、本地配置管理

**内网实现**：调用 `SystemUtil.getStringFromEnv()`
**外网实现**：优先从内存 Map 获取，其次从系统环境变量获取

##### 3.1.2.6 ResourceMonitorAdapter

```java
package com.huawei.browsergateway.adapter;

public interface ResourceMonitorAdapter {
    float getCpuUsage();
    float getMemoryUsage();
    float getNetworkUsage();
    ResourceStatistics getStatistics(String metricType);
}
```

**职责**：CPU、内存等系统资源监控

**内网实现**：调用 `RsApi.getLatestContainerResourceStatistics()`
**外网实现**：使用 JMX 获取系统资源

### 3.2 适配器工厂

#### 3.2.1 工厂类图

```
┌─────────────────────────────────────────────────────────────────┐
│                  EnvironmentAwareAdapterFactory                  │
│  (环境感知工厂：根据配置选择CSP SDK或自定义实现)                  │
└───────┬─────────────────────────────┬───────────────────────────┘
        │                             │
        ├─────────> ┌─────────────────────────────────┐
        │             │    CspSdkAdapterFactory        │
        │             │    (使用CSP SDK的具体实现)       │
        │             └───────┬─────────────┬───────────┘
        │                     │             │
        │      ├────────────────┴───────┐   │
        │      │                        │   │
        │      ▼                        ▼   ▼
        │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
        │  │CspFramework  │  │CspAlarm      │  │CspCertificate│
        │  │Adapter       │  │Adapter       │  │Adapter       │
        │  └──────────────┘  └──────────────┘  └──────────────┘
        │
        └─────────> ┌─────────────────────────────────┐
                      │    CustomAdapterFactory         │
                      │    (自定义实现，用于外网环境)     │
                      └───────┬─────────────┬───────────┘
                              │             │
                  ┌───────────┴───────┐   │
                  │                  │   │
                  ▼                  ▼   ▼
              ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
              │CustomFramework│  │CustomAlarm   │  │CustomCertificate│
              │Adapter        │  │Adapter       │  │Adapter        │
              └──────────────┘  └──────────────┘  └──────────────┘
```

#### 3.2.2 工厂接口定义

```java
package com.huawei.browsergateway.adapter.factory;

public interface AdapterFactory {
    FrameworkAdapter createFrameworkAdapter();
    AlarmAdapter createAlarmAdapter();
    CertificateAdapter createCertificateAdapter();
    ServiceManagementAdapter createServiceManagementAdapter();
    SystemUtilAdapter createSystemUtilAdapter();
    ResourceMonitorAdapter createResourceMonitorAdapter();
}
```

#### 3.2.2 环境感知工厂

```java
package com.huawei.browsergateway.adapter.factory;

@Component
public class EnvironmentAwareAdapterFactory implements AdapterFactory {

    @Value("${adapter.provider.type:default}")
    private AdapterProviderType providerType;

    private final CspSdkAdapterFactory cspSdkAdapterFactory;
    private final CustomAdapterFactory customAdapterFactory;

    @Override
    public FrameworkAdapter createFrameworkAdapter() {
        return getFactory().createFrameworkAdapter();
    }

    @Override
    public AlarmAdapter createAlarmAdapter() {
        return getFactory().createAlarmAdapter();
    }

    private AdapterFactory getFactory() {
        switch (providerType) {
            case CSP_SDK:
                return cspSdkAdapterFactory;
            case CUSTOM:
                return customAdapterFactory;
            default:
                return cspSdkAdapterFactory;
        }
    }

    public enum AdapterProviderType {
        CSP_SDK,  // CSP SDK 实现
        CUSTOM    // 自定义实现
    }
}
```

### 3.3 配置管理

#### 3.3.1 内网环境配置

```yaml
adapter:
  provider:
    type: csp-sdk
    enable-mock: false

cse:
  service:
    registry:
      address: http://127.0.0.1:30100
  rest:
    address: 127.0.0.1:8090

cert:
  cert-path: /opt/csp/browsergw
```

#### 3.3.2 外网环境配置

```yaml
adapter:
  provider:
    type: custom
    enable-mock: false

custom:
  service-name: browser-gateway-external
  pod-name: browser-gateway-pod-1
  namespace: external
  certificate:
    ca-path: /etc/browsergw/ca.crt
    cert-path: /etc/browsergw/device.crt
    key-path: /etc/browsergw/device.key
  alarm:
    log-path: /var/log/browsergw/alarms.log
  service:
    mock-instances: true
```

### 3.6 审计日志适配器（扩展适配器）

#### 3.6.1 接口定义

```java
package com.huawei.browsergateway.adapter;

import java.util.Map;

public interface AuditLogAdapter {
    boolean writeAuditLog(String operation, AuditLevel level, Map<String, Object> params);
    boolean writeSecurityLog(String operation, AuditLevel level, Map<String, Object> params);

    enum AuditLevel {
        MAJOR, MINOR, INFO
    }
}
```

**职责**：审计日志和安全日志上报

**内网实现**：使用 `CspRestTemplateBuilder` 调用 `cse://AuditLog/plat/audit/v1/logs` 服务
**外网实现**：将审计日志写入本地日志文件或直接忽略

#### 3.6.2 使用场景

在关键操作时记录审计日志：

```java
// 在业务代码中使用
@Autowired
private AuditLogAdapter auditLogAdapter;

public void performCriticalOperation() {
    Map<String, Object> params = new HashMap<>();
    params.put("userId", userId);
    params.put("operation", "createBrowser");
    params.put("result", "success");

    auditLogAdapter.writeAuditLog("browser_operation", AuditLogAdapter.AuditLevel.INFO, params);
}
```

---

## 4. 核心业务模块设计

### 4.0 公共模块

#### 4.0.1 模块职责

- 定义系统常量
- 定义通用枚举类
- 提供通用工具类
- 定义统一响应格式
- 公共数据转换和处理

#### 4.0.2 核心组件

```java
// 常量定义
public class Constants {
    public static final String DEFAULT_CHARSET = "UTF-8";
    public static final int DEFAULT_HEARTBEAT_TTL = 60;
    public static final String DATA_PREFIX = "userdata-";
}

// 通用响应类
@Data
public class BaseResponse<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> BaseResponse<T> success(T data) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        return response;
    }
}
```

### 4.1 异常处理模块

#### 4.1.1 模块职责

- 定义业务异常
- 全局异常捕获
- 异常信息封装
- 错误日志记录

#### 4.1.2 核心组件

```java
// 业务异常
@Data
public class BusinessException extends RuntimeException {
    private Integer errorCode;
    private String errorMessage;

    public BusinessException(ErrorCodeEnum errorCodeEnum) {
        this.errorCode = errorCodeEnum.getCode();
        this.errorMessage = errorCodeEnum.getMessage();
    }
}

// 全局异常处理器
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<?>> handleBusinessException(BusinessException e) {
        log.warn("Business exception occurred: {}", e.getErrorMessage());
        return ResponseEntity.status(HttpStatus.OK)
            .body(BaseResponse.fail(e.getErrorCode(), e.getErrorMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<?>> handleException(Exception e) {
        log.error("Unexpected exception occurred", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(BaseResponse.fail(500, "Internal server error"));
    }
}
```

### 4.2 SDK 模块

#### 4.2.1 模块职责

- 封装第三方 SDK
- 提供 SDK 工厂
- 管理 SDK 生命周期
- SDK 配置管理

#### 4.2.2 核心组件

```java
// SDK 工厂接口
public interface SdkFactory<T> {
    T createSdk(SdkConfig config);
    void destroySdk(T sdk);
}

// SDK 配置类
@Data
public class SdkConfig {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private Map<String, Object> properties;
}
```

### 4.3 路由模块

#### 4.3.1 模块职责

- 请求路由分发
- 负载均衡
- 服务发现
- 路由规则管理

#### 4.3.2 核心组件

```java
@Component
public class Router {
    @Autowired
    private ServiceManagementAdapter serviceManagementAdapter;

    public String route(String serviceName, String operation) {
        List<ServiceInstance> instances = serviceManagementAdapter.findServiceInstances(serviceName);
        if (instances == null || instances.isEmpty()) {
            throw new RuntimeException("No available instances for service: " + serviceName);
        }
        return selectInstance(instances, operation);
    }

    private String selectInstance(List<ServiceInstance> instances, String operation) {
        int index = (int) (System.currentTimeMillis() % instances.size());
        return instances.get(index).getEndpoint();
    }
}
```

### 4.4 会话管理模块

#### 4.1.1 模块职责

- 创建和销毁浏览器实例
- 管理用户会话状态
- 处理心跳和超时
- 维护会话元数据

#### 4.1.2 核心类

```java
@Service
public class ChromeSetImpl implements IChromeSet {

    private static ConcurrentMap<String, UserChrome> userChromeMap = new ConcurrentHashMap<>();

    @Autowired
    private IFileStorage fileStorageService;

    @Autowired
    private IRemote remoteService;

    public UserChrome create(InitBrowserRequest request) {
        String userId = UserIdUtil.generateUserIdByImeiAndImsi(
            request.getImei(), request.getImsi());

        if (userChromeMap.containsKey(userId)) {
            throw new BusinessException("User session already exists");
        }

        UserChrome userChrome = new UserChrome(userId, request);
        userChromeMap.put(userId, userChrome);

        return userChrome;
    }

    public UserChrome get(String userId) {
        return userChromeMap.get(userId);
    }

    public void delete(String userId) {
        UserChrome userChrome = userChromeMap.remove(userId);
        if (userChrome != null) {
            userChrome.closeApp();
        }
    }
}
```

#### 4.1.3 关键流程

##### 预打开浏览器

1. 客户端调用 `/browsergw/browser/preOpen` 接口
2. 解析请求参数（imei, imsi, lcdWidth, lcdHeight 等）
3. 生成唯一 userId
4. 检查会话是否已存在
5. 下载用户数据（从 S3）
6. 检查插件状态
7. 创建浏览器驱动
8. 建立 TCP 连接
9. 返回登录凭证

##### 删除用户数据

1. 客户端调用 `/browsergw/browser/userdata/delete` 接口
2. 解析请求参数（imei, imsi）
3. 生成 userId
4. 关闭浏览器实例
5. 压缩用户数据
6. 上传到 S3
7. 删除本地文件
8. 清理会话

### 4.5 插件管理模块

#### 4.5.1 模块职责

- 下载插件 JAR 包
- 解压和加载插件
- 热更新插件
- 查询插件状态

#### 4.2.2 核心类

```java
@Service
public class PluginManageImpl implements IPluginManage {

    @Autowired
    private IFileStorage fileStorageService;

    private MuenPluginClassLoader muenPluginClassLoader;

    public boolean loadPlugin(String bucketName, String extensionFilePath,
        String name, String version, String type) {

        try {
            // 1. 下载插件
            String localPath = "/tmp/plugins/" + name + "-" + version + ".jar";
            fileStorageService.downloadFile(localPath, extensionFilePath);

            // 2. 初始化插件类加载器
            Path pluginPath = Paths.get(localPath);
            muenPluginClassLoader = new MuenPluginClassLoader();
            boolean success = muenPluginClassLoader.init(pluginPath);

            if (!success) {
                alarmAdapter.sendAlarm("plugin-load-fail", AlarmType.GENERATE,
                    Map.of("pluginName", name, "version", version));
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to load plugin", e);
            return false;
        }
    }

    public MuenDriver createDriverInstance(HWCallback callback) {
        if (muenPluginClassLoader == null) {
            return null;
        }
        return muenPluginClassLoader.createDriverInstance(callback);
    }
}
```

#### 4.2.3 关键流程

##### 加载插件

1. 客户端调用 `/browsergw/extension/load` 接口
2. 解析请求参数（bucketName, extensionFilePath, name, version）
3. 从 S3 下载插件 JAR
4. 初始化插件类加载器
5. 加载插件类
6. 创建插件驱动实例
7. 更新插件状态
8. 返回加载结果

### 4.6 媒体转发模块

#### 4.6.1 模块职责

- 建立 WebSocket 连接
- 接收媒体流数据
- 处理媒体流（WebCodecs/FFmpeg）
- 转发媒体流到客户端

#### 4.3.2 核心类

```java
@ServerEndpoint("/browser/websocket/{userId}")
public class MediaStreamSocketServer {

    @Autowired
    private MediaClientSet mediaClientSet;

    @Autowired
    private MediaSessionManager mediaSessionManager;

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId,
        UriInfo uriInfo) throws IOException {
        log.info("Media WebSocket opened for user: {}", userId);

        MediaParam param = parseMediaParam(uriInfo);

        String processorType = param.getProcessorType();
        MediaStreamProcessor processor;

        if ("webcodecs".equals(processorType)) {
            processor = new WebCodecsStreamProcessor();
        } else {
            processor = new FfmpegStreamProcessor();
        }

        processor.init(userId, session, param);
        mediaSessionManager.put(userId, processor);
    }

    @OnBinary
    public void onBinary(Session session, byte[] data) {
        MediaSessionManager manager = getManagerBySession(session);
        if (manager != null) {
            manager.processMediaStream(data);
        }
    }

    @OnClose
    public void onClose(Session session) {
        MediaSessionManager manager = getManagerBySession(session);
        if (manager != null) {
            manager.close();
        }
    }
}
```

#### 4.3.3 媒体处理器

##### WebCodecsStreamProcessor

```java
public class WebCodecsStreamProcessor implements MediaStreamProcessor {

    private Session session;
    private String userId;

    @Override
    public void processMediaStream(byte[] data) {
        try {
            VideoResponse videoResponse = new VideoResponse();
            videoResponse.setData(data);
            session.getAsyncRemote().sendObject(videoResponse);
        } catch (Exception e) {
            log.error("Failed to process media stream", e);
        }
    }
}
```

##### FfmpegStreamProcessor

```java
public class FfmpegStreamProcessor implements MediaStreamProcessor {

    private FfmpegCodecService ffmpegService;

    @Override
    public void init(String userId, Session session, MediaParam param) {
        ffmpegService = new FfmpegCodecService();
        ffmpegService.start(userId, param);
    }

    @Override
    public void processMediaStream(byte[] data) {
        ffmpegService.processFrame(data);
    }
}
```

### 4.7 健康监控模块

#### 4.7.1 模块职责

- 定时检查系统健康状态
- 上报健康指标
- 触发告警
- 自动恢复

#### 4.4.2 核心类

```java
@Component
public class HealthCheckTask {

    @Autowired
    private ResourceMonitorAdapter resourceMonitorAdapter;

    @Autowired
    private AlarmAdapter alarmAdapter;

    @Scheduled(cron = "0 */5 * * * ?")
    public void checkAndReport() {
        checkCpuUsage();
        checkMemoryUsage();
        checkNetworkUsage();
    }

    private void checkCpuUsage() {
        float cpuUsage = resourceMonitorAdapter.getCpuUsage();
        ResourceStatistics stats = resourceMonitorAdapter.getStatistics("cpu");

        if (stats.getRatio() > 80.0) {
            alarmAdapter.sendAlarm("cpu-usage-high", AlarmAdapter.AlarmType.GENERATE,
                Map.of("usage", String.valueOf(cpuUsage)));
        }
    }

    private void checkMemoryUsage() {
        float memoryUsage = resourceMonitorAdapter.getMemoryUsage();
        ResourceStatistics stats = resourceMonitorAdapter.getStatistics("memory");

        if (stats.getRatio() > 85.0) {
            alarmAdapter.sendAlarm("memory-usage-high", AlarmAdapter.AlarmType.GENERATE,
                Map.of("usage", String.valueOf(memoryUsage)));
        }
    }
}
```

#### 4.4.3 健康检查策略

```java
public interface ICheckStrategy {
    HealthCheckResult check();
}

@Component
public class CpuUsageCheck implements ICheckStrategy {

    @Autowired
    private ResourceMonitorAdapter resourceMonitorAdapter;

    @Override
    public HealthCheckResult check() {
        float usage = resourceMonitorAdapter.getCpuUsage();
        boolean healthy = usage < 80.0;

        return HealthCheckResult.builder()
            .metricName("cpu_usage")
            .value(usage)
            .healthy(healthy)
            .message(healthy ? "CPU usage normal" : "CPU usage high")
            .build();
    }
}
```

### 4.8 证书管理模块

#### 4.8.1 模块职责

- 订阅证书更新
- 管理证书生命周期
- 动态更新证书
- 重启 TLS 服务器

#### 4.5.2 核心类

```java
@Component
public class SubscribeCert {

    @Autowired
    private CertificateAdapter certificateAdapter;

    @Autowired
    private ControlTcpServer controlTcpServer;

    @Autowired
    private MediaTcpServer mediaTcpServer;

    @PostConstruct
    public void SubscribeCertInfo() {
        certificateAdapter.initialize();

        List<CertScene> certScenes = Arrays.asList(
            new CertScene("sbg_server_ca_certificate", CertScene.SceneType.CA),
            new CertScene("sbg_server_device_certificate", CertScene.SceneType.DEVICE)
        );

        certificateAdapter.subscribeCertificates("browsergw", certScenes, "/opt/csp/browsergw",
            new CertUpdateCallback() {
                @Override
                public void onCertificateUpdate(String caContent, String deviceContent) {
                    log.info("Certificate updated");
                    CertInfo.getInstance().setCaContent(caContent);
                    CertInfo.getInstance().setDeviceContent(deviceContent);

                    restartTcpServers();
                }
            });
    }

    private void restartTcpServers() {
        controlTcpServer.stopServer();
        mediaTcpServer.stopServer();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        controlTcpServer.startServer(true);
        mediaTcpServer.startServer(true);
    }
}
```

### 4.9 定时任务模块

#### 4.9.1 模块职责

- 定时关闭失效浏览器
- 定时清理 TCP 连接
- 定时上报容量
- 日志轮转

#### 4.6.2 核心类

```java
@Component
public class BrowserCloserTask {

    @Autowired
    private ChromeSetImpl chromeSet;

    @Scheduled(cron = "0 */1 * * * ?")
    public void closeBrowser() {
        long now = System.nanoTime();
        long ttl = Config.getInstance().getTcp().getHeartbeatTtl() * 1_000_000L;

        for (Map.Entry<String, UserChrome> entry : chromeSet.userChromeMap.entrySet()) {
            String userId = entry.getKey();
            UserChrome userChrome = entry.getValue();

            long lastHeartbeat = userChrome.getLastHeartbeat();
            if (now - lastHeartbeat > ttl) {
                log.info("Closing browser for user {} due to heartbeat timeout", userId);
                chromeSet.delete(userId);
            }
        }
    }
}

@Component
public class TcpChannelMonitor {

    @Autowired
    private ControlClientSet controlClientSet;

    @Autowired
    private MediaClientSet mediaClientSet;

    @Scheduled(cron = "0 */2 * * * ?")
    public void tcpClientMonitor() {
        controlClientSet.cleanExpiredClients();
        mediaClientSet.cleanExpiredClients();
    }
}
```

### 4.10 告警管理模块

#### 4.10.1 模块职责

- 发送告警
- 清除告警
- 去重处理
- 告警历史查询

#### 4.7.2 核心类

```java
@Service
public class AlarmServiceImpl implements IAlarm {

    @Autowired
    private AlarmAdapter alarmAdapter;

    @Autowired
    private SystemUtilAdapter systemUtilAdapter;

    private Map<String, Long> lastAlarmTime = new ConcurrentHashMap<>();
    private static final long ALARM_DEDUPE_INTERVAL = 10 * 60 * 1000;

    @Override
    public boolean sendAlarm(AlarmEvent alarmEvent) {
        String alarmId = alarmEvent.getAlarmCodeEnum().getAlarmId();

        if (System.currentTimeMillis() - lastAlarmTime.getOrDefault(alarmId, 0L) < ALARM_DEDUPE_INTERVAL) {
            log.warn("Alarm {} was already reported within 10 minutes; skipping", alarmId);
            return false;
        }

        Map<String, String> parameters = new HashMap<>();
        parameters.put("source", systemUtilAdapter.getEnvString("SERVICENAME", "browser-gateway"));
        parameters.put("kind", "service");
        parameters.put("name", systemUtilAdapter.getEnvString("PODNAME", "unknown"));
        parameters.put("namespace", systemUtilAdapter.getEnvString("NAMESPACE", "default"));
        parameters.put("EventMessage", alarmEvent.getEventMessage());
        parameters.put("EventSource", "BrowserGW Service");
        parameters.put("OriginalEventTime", String.valueOf(System.currentTimeMillis()));

        boolean success = alarmAdapter.sendAlarm(alarmId, AlarmAdapter.AlarmType.GENERATE, parameters);

        if (success) {
            lastAlarmTime.put(alarmId, System.currentTimeMillis());
        }

        return success;
    }

    @Override
    public boolean clearAlarm(AlarmCodeEnum alarmCodeEnum) {
        return alarmAdapter.clearAlarm(alarmCodeEnum.getAlarmId());
    }
}
```

### 4.11 数据管理模块

#### 4.11.1 模块职责

- 用户数据压缩和脱敏
- 用户数据下载
- 用户数据上传
- 用户数据删除

#### 4.8.2 核心类

```java
@Component
public class UserData {

    @Autowired
    private IFileStorage fileStorageService;

    private String userdataDir;
    private String userId;
    private String remote;

    public void upload() {
        if (!needUpload()) {
            return;
        }

        try {
            File localUserData = getLocalURL();

            // 脱敏
            UserdataSlimmer.slimInplace(localUserData);

            // 压缩
            File localZip = new File(localUserData.getParent(), "userdata.json.zst");
            ZstdUtil.compressJson(localUserData.getAbsolutePath(), localZip.getAbsolutePath());

            // 删除旧文件（如果存在）
            if (fileStorageService.exist(remote)) {
                fileStorageService.deleteFile(remote);
            }

            // 上传
            fileStorageService.uploadFile(localZip.getAbsolutePath(), remote);

            // 清理临时文件
            FileUtil.del(localZip);

            log.info("Userdata uploaded successfully for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to upload userdata for user: {}", userId, e);
        }
    }

    public String download() {
        try {
            String localTmpPath = "/tmp/userdata/" + userId + ".json.zst";

            fileStorageService.downloadFile(localTmpPath, remote);

            File localPath = new File(userdataDir, "userdata.json");
            ZstdUtil.decompressJson(localTmpPath, localPath.getAbsolutePath());

            return localPath.getAbsolutePath();
        } catch (Exception e) {
            log.error("Failed to download userdata for user: {}", userId, e);
            return null;
        }
    }

    public void delete() {
        try {
            // 删除远程文件
            if (fileStorageService.exist(remote)) {
                fileStorageService.deleteFile(remote);
            }

            // 删除本地文件
            File localUserData = getLocalURL();
            if (localUserData.exists()) {
                FileUtil.del(localUserData);
            }

            log.info("Userdata deleted successfully for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to delete userdata for user: {}", userId, e);
        }
    }
}
```

---

## 5. 数据模型设计

### 5.1 核心实体

#### 5.1.1 UserChrome

```java
@Data
public class UserChrome {
    private String userId;
    private String imei;
    private String imsi;
    private ChromiumDriverProxy chromeDriver;
    private MuenDriver muenDriver;
    private UserData userData;
    private BrowserStatus status;
    private long lastHeartbeat;

    public UserChrome(String userId, InitBrowserRequest request) {
        this.userId = userId;
        this.imei = request.getImei();
        this.imsi = request.getImsi();
        this.status = BrowserStatus.INITIALIZING;

        initialize(request);
    }

    private void initialize(InitBrowserRequest request) {
        // 初始化用户数据
        this.userData = new UserData(userId);
        userData.download();

        // 初始化浏览器驱动
        this.chromeDriver = new ChromiumDriverProxy(buildChromeOptions(request));

        // 初始化插件驱动
        this.muenDriver = createMuenDriver();

        this.status = BrowserStatus.READY;
    }

    public void createBrowser(ChromeRecordConfig config) {
        chromeDriver.getDriver().newPage(config.getUrl());
        this.status = BrowserStatus.RUNNING;
    }

    public void closeApp() {
        if (chromeDriver != null) {
            chromeDriver.quit();
        }
        if (userData != null) {
            userData.upload();
        }
        this.status = BrowserStatus.CLOSED;
    }
}
```

#### 5.1.2 Session

```java
@Data
public class Session {
    private String sessionId;
    private String apptype;
    private long startedAt;
    private long finishedAt;
    private String tcpUniqueId;
}
```

#### 5.1.3 Traffic

```java
@Data
public class Traffic {
    private String imeiAndImsi;
    private String apptype;
    private long startedAt;
    private long finishedAt;
    private long outBytes;
    private String networkType;
}
```

### 5.2 传输对象

#### 5.2.1 InitBrowserRequest

```java
@Data
public class InitBrowserRequest {
    private String imei;
    private String imsi;
    private Integer lcdWidth;
    private Integer lcdHeight;
    private String appType;
    private String innerMediaEndpoint;
}
```

#### 5.2.2 DeleteUserDataRequest

```java
@Data
public class DeleteUserDataRequest {
    private String imei;
    private String imsi;
}
```

#### 5.2.3 LoadExtensionRequest

```java
@Data
public class LoadExtensionRequest {
    private String bucketName;
    private String extensionFilePath;
    private String name;
    private String version;
    private String type;
}
```

---

## 6. 接口设计

### 6.1 REST API

#### 6.1.1 预打开浏览器

```http
POST /browsergw/browser/preOpen
Content-Type: application/json

{
  "imei": "123456789012345",
  "imsi": "123456789012345",
  "lcdWidth": 1920,
  "lcdHeight": 1080,
  "appType": "mobile",
  "innerMediaEndpoint": "ws://localhost:8080"
}

Response:
{
  "code": 200,
  "message": "success",
  "data": "success"
}
```

#### 6.1.2 删除用户数据

```http
DELETE /browsergw/browser/userdata/delete
Content-Type: application/json

{
  "imei": "123456789012345",
  "imsi": "123456789012345"
}

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "deleted": true
  }
}
```

#### 6.1.3 加载插件

```http
POST /browsergw/extension/load
Content-Type: application/json

{
  "bucketName": "browsergw-plugins",
  "extensionFilePath": "plugins/plugin-1.0.0.jar",
  "name": "example-plugin",
  "version": "1.0.0",
  "type": "jar"
}

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "name": "example-plugin",
    "version": "1.0.0",
    "status": "SUCCESS"
  }
}
```

#### 6.1.4 查询插件状态

```http
GET /browsergw/extension/pluginInfo

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "name": "example-plugin",
    "version": "1.0.0",
    "type": "jar",
    "status": "COMPLETED",
    "bucketName": "browsergw-plugins",
    "packageName": "com.huawei.plugin"
  }
}
```

### 6.2 WebSocket 接口

#### 6.2.1 媒体流 WebSocket

```javascript
// 连接
const ws = new WebSocket('ws://localhost:8080/browser/websocket/imei-123_imsi-456?bitRate=2000&frameRate=30&processorType=webcodecs');

// 接收视频
ws.onmessage = (event) => {
  const videoData = event.data;
  // 处理视频数据
};

// 接收音频
ws.onmessage = (event) => {
  const audioData = event.data;
  // 处理音频数据
};

// 关闭连接
ws.onclose = () => {
  console.log('WebSocket closed');
};
```

#### 6.2.2 控制流 WebSocket

```javascript
// 连接
const ws = new WebSocket('ws://localhost:8080/control/websocket/imei-123_imsi-456');

// 发送控制消息
ws.send(JSON.stringify({
  type: 'click',
  x: 100,
  y: 200
}));

// 接收控制响应
ws.onmessage = (event) => {
  const response = JSON.parse(event.data);
  console.log('Control response:', response);
};
```

### 6.3 TCP TLV 接口

#### 6.3.1 控制流 TLV

```
登录消息:
Type: LOGIN
IMEI: 123456789012345
IMSI: 123456789012345
LCD_WIDTH: 1920
LCD_HEIGHT: 1080
APP_TYPE: mobile

响应消息:
Type: ACK
ACK_TYPE: SUCCESS
CODE: 0

业务消息:
Type: CLICK
X: 100
Y: 200

心跳消息:
Type: HEARTBEATS
```

#### 6.3.2 媒体流 TLV

```
登录消息:
Type: LOGIN
IMEI: 123456789012345
IMSI: 123456789012345

响应消息:
Type: ACK
ACK_TYPE: SUCCESS
CODE: 0

心跳消息:
Type: HEARTBEATS
```

---

## 7. 安全设计

### 7.1 TLS 证书管理

- 证书订阅和动态更新
- 证书有效期检查
- 证书过期预警

### 7.2 数据脱敏

- 用户数据压缩前脱敏
- 敏感信息过滤
- 过期数据清理

### 7.3 访问控制

- 用户身份认证
- 会话管理
- 权限校验

### 7.4 审计日志

- 操作日志记录
- 访问日志记录
- 异常日志记录

---

## 8. 性能设计

### 8.1 并发处理

- Netty 线程池配置
- WebSocket 连接池
- HTTP 连接池

### 8.2 缓存策略

- 会话缓存
- 服务实例缓存
- 证书缓存

### 8.3 压缩算法

- Zstd 用户数据压缩
- 媒体流压缩
- 网络传输压缩

### 8.4 资源限制

- 最大实例数限制
- 最大连接数限制
- 内存使用限制

---

## 9. 可靠性设计

### 9.1 异常处理

- 全局异常捕获
- 业务异常处理
- 网络异常处理

### 9.2 重试机制

- 告警发送重试
- 服务调用重试
- 文件上传重试

### 9.3 降级策略

- 插件加载失败降级
- 告警发送失败降级
- 证书订阅失败降级

### 9.4 容错机制

- 心跳超时处理
- 连接断开重连
- 资源泄漏防护

---

## 10. 部署方案

### 10.1 内网部署

#### 10.1.1 环境要求

- JDK 11+
- CSP SDK 依赖
- CSE 注册中心
- 内网 S3/MinIO
- 告警平台

#### 10.1.2 配置文件

```yaml
adapter:
  provider:
    type: csp-sdk

cse:
  service:
    registry:
      address: http://cse-registry:30100
  rest:
    address: 0.0.0.0:8090

storage:
  s3:
    endpoint: http://minio:9000
    bucket: browsergw-data
```

#### 10.1.3 部署步骤

1. 打包应用：`mvn clean package`
2. 上传到服务器
3. 配置 `application-csp.yaml`
4. 启动应用：`java -jar browser-gateway.jar --spring.profiles.active=csp`
5. 验证服务状态

### 10.2 外网部署

#### 10.2.1 环境要求

- JDK 11+
- 无 CSP SDK 依赖
- 公有云 S3
- 本地日志文件

#### 10.2.2 配置文件

```yaml
adapter:
  provider:
    type: custom

storage:
  s3:
    endpoint: https://s3.amazonaws.com
    bucket: browsergw-data-external

custom:
  certificate:
    ca-path: /etc/browsergw/ca.crt
    cert-path: /etc/browsergw/device.crt
    key-path: /etc/browsergw/device.key

  alarm:
    log-path: /var/log/browsergw/alarms.log
```

#### 10.2.3 部署步骤

1. 打包应用：`mvn clean package -DskipTests`
2. 上传到服务器
3. 配置证书文件
4. 配置 `application-custom.yaml`
5. 启动应用：`java -jar browser-gateway.jar --spring.profiles.active=custom`
6. 验证服务状态

### 10.3 容器化部署

#### 10.3.1 Dockerfile

```dockerfile
FROM openjdk:11-jre-slim

WORKDIR /app

COPY target/browser-gateway.jar app.jar

COPY conf/ /opt/browsergw/conf/

EXPOSE 8090 8080 18601 18602

CMD ["java", "-jar", "app.jar"]
```

#### 10.3.2 Kubernetes 部署

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: browser-gateway
spec:
  replicas: 3
  selector:
    matchLabels:
      app: browser-gateway
  template:
    metadata:
      labels:
        app: browser-gateway
    spec:
      containers:
      - name: browser-gateway
        image: browser-gateway:2.0
        ports:
        - containerPort: 8090
        - containerPort: 8080
        - containerPort: 18601
        - containerPort: 18602
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "csp"
        volumeMounts:
        - name: config
          mountPath: /opt/browsergw/conf
      volumes:
      - name: config
        configMap:
          name: browser-gateway-config
```

---

## 11. 监控与运维

### 11.1 监控指标

#### 11.1.1 系统指标

- CPU 使用率
- 内存使用率
- 网络带宽
- 磁盘使用率

#### 11.1.2 应用指标

- 会话数量
- 并发连接数
- 请求响应时间
- 错误率

#### 11.1.3 业务指标

- 浏览器实例数
- 插件加载次数
- 媒体流量
- 告警数量

### 11.2 日志管理

#### 11.2.1 日志级别

- DEBUG：开发调试
- INFO：业务流程
- WARN：异常但可恢复
- ERROR：严重错误

#### 11.2.2 日志轮转

- 按天轮转
- 保留 30 天
- 压缩归档

### 11.3 告警规则

#### 11.3.1 系统告警

- CPU 使用率 > 80%
- 内存使用率 > 85%
- 磁盘使用率 > 90%

#### 11.3.2 应用告警

- 浏览器实例创建失败
- 插件加载失败
- 证书订阅失败
- 连接数超限

---

## 附录

### A. 术语表

| 术语 | 说明 |
|------|------|
| CSP | Cloud Service Platform，华为云服务平台 |
| CSE | Cloud Service Engine，微服务框架 |
| TLV | Type-Length-Value，一种二进制协议格式 |
| SBG | Smart Browser Gateway，云浏览器网关 |
| SDK | Software Development Kit，软件开发工具包 |
| S3 | Simple Storage Service，对象存储服务 |

### B. 外部依赖服务清单

#### B.1 CSP 相关服务（通过适配层访问）

| 服务名称 | 服务端点 | CSP SDK 模块 | 适配器接口 | 内网实现方式 | 外网实现方式 |
|---------|---------|-------------|-----------|------------|------------|
| **CSE 注册中心** | `cse://` | `csejsdk-core`<br>`csejsdk-starter-all` | `FrameworkAdapter`<br>`ServiceManagementAdapter` | `Framework.start()`<br>`RegistryUtils.findServiceInstance()` | 本地服务注册表（内存Map） |
| **OM 管理服务** | `cse://OM/` | `om-transport` | `FrameworkAdapter` | `OmsdkStarter.omsdkInit()` | 空实现（no-op） |
| **证书管理中心** | `cert://` | `certmgr-jsdk` | `CertificateAdapter` | `CertMgrApi.certSDKInit()`<br>`ExCertMgrApi.subscribeExCert()` | 从配置文件加载或生成自签名证书 |
| **告警管理服务** | `cse://FMService/` | `om-alarmsdk`<br>`om-transport` | `AlarmAdapter` | `AlarmSendManager.getInstance().sendAlarm()` | 写入本地日志文件 |
| **审计日志服务** | `cse://AuditLog/` | `jsf-api` | `AuditLogAdapter`（扩展） | `CspRestTemplateBuilder.create()` | 写入本地审计日志文件 |

**依赖说明**：
- 内网环境：必须引入对应的 CSP SDK Maven 依赖
- 外网环境：不需要引入任何 CSP SDK 依赖
- 所有服务都通过适配层接口访问，业务代码无感知

#### B.2 非 CSP 相关服务（直接访问）

| 服务名称 | 服务协议 | 访问方式 | Maven 依赖 | 备注 |
|---------|---------|---------|-----------|------|
| **Redis** | TCP | Jedis/Lettuce 客户端 | `redis.clients:jedis` | 用于会话缓存、心跳、实例元数据 |
| **S3/MinIO** | HTTP/S3 API | AWS SDK S3 | `software.amazon.awssdk:s3` | 用于用户数据持久化、插件包存储 |
| **Chrome/Touch** | Chrome DevTools Protocol | Selenium/Playwright | `org.seleniumhq.selenium:selenium-java` | 用于浏览器实例创建和控制 |

**依赖说明**：
- 内网和外网环境都使用相同的访问方式
- 只是通过配置切换不同的服务地址
- 不需要适配层

### C. 参考文档

1. `docs/CSP-接口适配层架构设计.md`
2. `docs/CSP-适配层实施指南.md`
3. `docs/CSP-适配层快速参考.md`
4. `docs/CSP-SDK应用模块分析.md`
5. `docs/CSP-SDK接口签名与Mock指南.md`
6. `BrowserGateway/系统架构说明书.md`

### D. 代码结构参考

#### D.1 模块目录映射

| 模块名称 | 目录路径 | 主要职责 |
|---------|---------|---------|
| **公共模块** | `src/main/java/com/huawei/browsergateway/common/` | 常量定义、枚举类、通用工具、统一响应 |
| **异常处理** | `src/main/java/com/huawei/browsergateway/exception/` | 业务异常定义、全局异常处理器 |
| **SDK 模块** | `src/main/java/com/huawei/browsergateway/sdk/` | 第三方 SDK 封装、SDK 工厂、生命周期管理 |
| **路由模块** | `src/main/java/com/huawei/router/` | 请求路由、负载均衡、服务发现 |
| **CSP 适配层** | `src/main/java/com/huawei/browsergateway/adapter/` | CSP 接口适配、内外网环境隔离 |
| **API 层** | `src/main/java/com/huawei/browsergateway/api/` | REST API 接口定义 |
| **配置类** | `src/main/java/com/huawei/browsergateway/config/` | Spring 配置、Bean 定义 |
| **实体类** | `src/main/java/com/huawei/browsergateway/entity/` | 数据模型、DTO、VO |
| **定时任务** | `src/main/java/com/huawei/browsergateway/scheduled/` | 定时任务、调度器 |
| **业务服务** | `src/main/java/com/huawei/browsergateway/service/` | 核心业务逻辑、服务接口与实现 |
| **TCP 服务器** | `src/main/java/com/huawei/browsergateway/tcpserver/` | TCP 服务器实现、控制流、媒体流 |
| **WebSocket** | `src/main/java/com/huawei/browsergateway/websocket/` | WebSocket 端点、媒体流、插件通信 |
| **工具类** | `src/main/java/com/huawei/browsergateway/util/` | 工具类集合、编码、压缩等 |

#### D.2 实体模块详细映射

| 实体模块 | 包路径 | 主要文件/职责 |
|---------|---------|--------------|
| **基础实体** | `entity/` | `BaseResponse.java` - 通用响应封装<br>`CommonResult.java` - 统一返回格式 |
| **告警相关** | `entity/alarm/` | `AlarmEvent.java` - 告警事件<br>`AlarmResponseParam.java` - 告警响应参数<br>`DataParam.java` - 数据参数 |
| **浏览器配置** | `entity/browser/` | `ChromeConfig.java` - Chrome配置<br>`ChromeRecordConfig.java` - 录制配置<br>`RouteAppConfig.java` - 路由应用配置<br>`UrlConfig.java` - URL配置 |
| **事件系统** | `entity/event/` | `BaseEvent.java` - 基础事件<br>`DataDealEvent.java` - 数据处理事件<br>`FlowStatEvent.java` - 流量统计事件<br>`EventInfo.java` - 事件信息<br>`EventTypeEnum.java` - 事件类型枚举 |
| **运行数据** | `entity/operate/` | `Session.java` - 会话信息<br>`Traffic.java` - 流量统计 |
| **插件管理** | `entity/plugin/` | `PluginActive.java` - 插件激活状态 |
| **枚举定义** | `entity/enums/` | `AlarmEnum.java` - 告警类型<br>`BrowserStatus.java` - 浏览器状态<br>`RecordModeEnum.java` - 记录模式 |
| **请求参数** | `entity/request/` | 请求相关的 DTO |
| **响应数据** | `entity/response/` | 响应相关的 DTO |

#### D.2 关键文件说明

```
browser-gateway/src/main/java/com/huawei/browsergateway/
├── common/
│   ├── Response.java                    # 通用响应封装
│   ├── Constants.java                   # 系统常量定义
│   └── ErrorCode.java                   # 错误码定义
├── exception/
│   ├── BusinessException.java           # 业务异常
│   └── GlobalExceptionHandler.java     # 全局异常处理器
├── config/
│   ├── BeanConfig.java                  # Bean 配置
│   └── ApplicationConfig.java           # 应用配置
├── entity/
│   ├── alarm/                           # 告警相关实体
│   ├── browser/                         # 浏览器相关实体
│   ├── request/                         # 请求 DTO
│   └── response/                        # 响应 DTO
├── service/
│   ├── IChromeSet.java                  # 浏览器会话接口
│   ├── IFileStorage.java                # 文件存储接口
│   ├── IPluginManage.java               # 插件管理接口
│   ├── IAlarm.java                      # 告警管理接口
│   ├── impl/                            # 服务实现
│   └── healthCheck/                     # 健康检查策略
├── tcpserver/
│   ├── cert/                            # 证书管理
│   ├── control/                         # 控制流服务器
│   └── media/                           # 媒体流服务器
├── websocket/
│   ├── media/                           # 媒体流 WebSocket
│   │   ├── ffmpeg/                      # FFmpeg 处理器
│   │   └── webcodecs/                   # WebCodecs 处理器
│   └── extension/                       # 插件 WebSocket
├── util/
│   ├── ZstdUtil.java                    # Zstd 压缩工具
│   └── encode/                          # 编码工具
└── scheduled/
    ├── BrowserCloserTask.java           # 浏览器关闭任务
    ├── HealthCheckTask.java             # 健康检查任务
    └── TcpChannelMonitor.java           # TCP 连接监控
```

#### D.3 代码组织原则

1. **模块化设计**：按功能模块划分，模块之间低耦合高内聚
2. **接口优先**：优先定义接口，再提供实现
3. **配置分离**：配置类与业务代码分离
4. **异常统一**：统一异常处理机制
5. **工具复用**：通用工具类抽取到 util 包
6. **实体清晰**：按业务域组织实体类
