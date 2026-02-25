# BrowserGateway CSP SDK 应用模块详细分析报告（修正版）

## 文档信息

| 项目 | BrowserGateway |
|------|----------------|
| 文档类型 | CSP SDK应用模块分析报告 |
| 版本 | 1.0 (修正版) |
| 日期 | 2026-02-13 |
| 分析范围 | CSP SDK各模块在BrowserGateway中的应用详情 |

---

## 1. 概述

### 1.1 分析背景

BrowserGateway项目是基于华为云服务平台的浏览器网关系统，深度集成了多个CSP（Cloud Service Platform）SDK模块。通过对项目源代码的深入分析，本文档详细记录各CSP SDK模块的实际应用场景、依赖关系和实现细节。

### 1.2 实际使用的CSP SDK模块清单

根据源代码中实际导入和使用情况，项目中实际使用的CSP SDK模块如下：

| 模块名称 | 实际导入包 | Maven Artifact | 功能定位 |
|---------|-----------|---------------|----------|
| CSE框架 | com.huawei.csp.csejsdk.core.api | csejsdk-core | 微服务框架 |
| OM管理SDK | com.huawei.csp.om.transport.vertx.init | transport-sdk | 运维管理 |
| SRV管理SDK | com.huawei.csp.csejsdk.common.utils | csejsdk-starter-all | 服务管理工具 |
| 告警SDK | com.huawei.csp.om.alarmsdk.* | alarmsdk | 告警系统 |
| SF工具包 | com.huawei.csp.jsf.api | jsf-api | 接口框架工具 |
| 证书SDK | com.huawei.csp.certsdk.* | certmgr-jsdk | 证书管理 |
| 系统工具 | com.huawei.csp.csejsdk.*.pojo | csejsdk-rssdk | 系统相关POJO |
| CSE服务注解 | org.apache.servicecomb.provider.rest.common | spring-cloud-servicecomb | REST服务注册 |
| CSE注册API | org.apache.servicecomb.* | spring-cloud-servicecomb | 服务注册发现 |

---

## 2. 各模块详细分析（基于实际代码）

### 2.1 CSE框架（com.huawei.csp.csejsdk）

#### 2.1.1 模块说明

**主要导入包**：
- `com.huawei.csp.csejsdk.core.api.Framework`
- `org.apache.servicecomb.foundation.common.utils.BeanUtils`
- `org.apache.servicecomb.provider.rest.common.RestSchema`
- `org.apache.servicecomb.registry.api.registry.MicroserviceInstance`
- `org.apache.servicecomb.registry.api.registry.MicroserviceInstanceStatus`
- `org.apache.servicecomb.serviceregistry.RegistryUtils`

**应用位置**：
- `BrowserGatewayApplication.java` - 框架启动
- `ChromeApi.java` - REST服务注册
- `ExtensionManageApi.java` - REST服务注册
- `CseImpl.java` - 服务发现实现
- `ServerEndpointExporter.java` - WebSocket Bean工具

**实际代码分析**：

```java
// 启动CSE框架
Framework.start();
log.info("Framework.initCsp ==================================");

// 初始化OM SDK
try {
    OmsdkStarter.omsdkInit();
    System.out.println("OmsdkStarter.omsdkInit ==================================");
} catch (Exception e) {
    log.error("CSPRunLog main: init OmsdkStarter exception", e);
}
```

**关键功能**：
- CSE（ServiceComb）微服务框架启动
- OM（运维管理）SDK初始化
- 服务治理基础设施

#### 2.1.2 服务管理工具

**导入包**：
- `com.huawei.csp.csejsdk.common.utils.ServiceUtils`

**应用位置**：
- `ChromeSetImpl.java` - 实例容量上报
- `HealthCheckTask.java` - 健康状态上报

**实际代码**：

```java
// ChromeSetImpl.java 中上报实例容量
Map<String, String> reportMap = new HashMap<>();
reportMap.put(PROPERTY_KEY, jsonStr);
if(!ServiceUtils.putInstanceProperties(reportMap)) {
    log.error("failed to update properties to cse");
}

// HealthCheckTask.java 中上报健康状态
ServiceUtils.putInstanceProperties(healthResult);
```

#### 2.1.3 服务资源SDK

**导入包**：
- `com.huawei.csp.csejsdk.rssdk.api.RsApi`
- `com.huawei.csp.csejsdk.rssdk.rspojo.RSPojo`

**应用位置**：`service/healthCheck/CpuUsageCheck.java`, `MemoryUsageCheck.java`

**实际代码**：

```java
// CPU使用率检查类中导入
import com.huawei.csp.csejsdk.rssdk.api.RsApi;
import com.huawei.csp.csejsdk.rssdk.rspojo.RSPojo;
```

#### 2.1.4 ServiceComb服务注册注解

**导入包**：
- `org.apache.servicecomb.provider.rest.common.RestSchema`

**应用位置**：`ChromeApi.java`, `ExtensionManageApi.java`

**实际代码**：

```java
// ChromeApi.java
@RestSchema(schemaId = "ChromeApi")
@RequestMapping(path = "/browsergw/browser", produces = MediaType.APPLICATION_JSON_VALUE)
public class ChromeApi {
    @Autowired
    private IRemote remoteService;

    @PostMapping(value = "/preOpen")
    public CommonResult<String> preOpenBrowser(@RequestBody InitBrowserRequest request) {
        // 实现逻辑
    }
}

// ExtensionManageApi.java
@RestSchema(schemaId = "ExtensionManageApi")
@RequestMapping(path = "/browsergw/extension", produces = MediaType.APPLICATION_JSON_VALUE)
public class ExtensionManageApi {
    @Autowired
    private ExtensionManageService extensionManageService;

    @PostMapping(value = "/load")
    public CommonResult<LoadExtensionResponse> loadExtension(@RequestBody LoadExtensionRequest request) {
        // 实现逻辑
    }
}
```

#### 2.1.5 ServiceComb服务发现

**导入包**：
- `org.apache.servicecomb.serviceregistry.RegistryUtils`
- `org.apache.servicecomb.registry.api.registry.MicroserviceInstance`
- `org.apache.servicecomb.registry.api.registry.MicroserviceInstanceStatus`

**应用位置**：`CseImpl.java`

**实际代码**：

```java
@Service
public class CseImpl implements ICse {
    private static final Random random = new Random();

    @Override
    public String getReportEndpoint() {
        // 查询服务实例
        List<MicroserviceInstance> instances = RegistryUtils.findServiceInstance("0", "gids", "0+");
        if (CollectionUtil.isEmpty(instances)) {
            return "";
        }
        HashSet<String> endpoints = new HashSet<>();
        for (MicroserviceInstance instance : instances) {
            // 检查实例状态
            if (instance.getStatus() != MicroserviceInstanceStatus.UP) {
                continue;
            }
            // 提取端点
            for (String endpoint : instance.getEndpoints()) {
                String ipPort = extractIPPort(endpoint);
                if (ipPort == null) {
                    continue;
                }
                endpoints.add(ipPort);
            }
        }
        // 随机选择一个端点
        Object[] array = endpoints.toArray();
        return (String) array[random.nextInt(array.length)];
    }

    private String extractIPPort(String endpoint) {
        try {
            URI uri = new URI(endpoint);
            return uri.getHost() + ":" + uri.getPort();
        } catch (URISyntaxException e) {
            log.error("failed to parse endpoint {}, err: {}", endpoint, e.getMessage(), e);
            return null;
        }
    }
}
```

### 2.2 告警SDK（com.huawei.csp.om.alarmsdk）

#### 2.2.1 模块说明

**主要导入包**：
- `com.huawei.csp.om.alarmsdk.alarmmanager.AlarmSendManager`
- `com.huawei.csp.om.alarmsdk.alarmmanager.Alarm`
- `com.huawei.csp.om.alarmsdk.alarmmodel.AlarmModel`
- `com.huawei.csp.om.alarmsdk.util.SystemUtil`

**应用位置**：`AlarmServiceImpl.java`

#### 2.2.2 实际应用分析

```java
@Service
public class AlarmServiceImpl implements IAlarm {
    
    // 防止告警风暴，10分钟内相同告警只发送一次
    private static final Integer ONE_MINUTE = 10 * 60 * 1000;
    public static ConcurrentHashMap<String, Long> alarmMap = new ConcurrentHashMap<>();

    @Override
    public void sendAlarm(AlarmEvent alarmEvent) {
        log.info("enter send alarm");
        
        // 告警去重逻辑
        if (System.currentTimeMillis() - alarmMap.getOrDefault(alarmEvent.getAlarmCodeEnum().getAlarmId(), 0L) < ONE_MINUTE) {
            log.info("An alarm was already reported within 10 minute; skipping this operation.");
            return;
        }
        
        // 生成告警对象
        boolean result = retrySendAlarm(genAlarm(AlarmModel.EuGenClearType.GENERATE, alarmEvent));
        if (result) {
            alarmMap.put(alarmEvent.getAlarmCodeEnum().getAlarmId(), System.currentTimeMillis());
            log.info("send alarm successfully.");
        } else {
            log.info("Failed to send alarm.");
        }
    }

    private Alarm genAlarm(AlarmModel.EuGenClearType type, AlarmEvent alarmEvent) {
        Alarm alarm = new Alarm(alarmEvent.getAlarmCodeEnum().getAlarmId(), type);
        alarm.appendParameter("source", SystemUtil.getStringFromEnv("SERVICENAME"));
        alarm.appendParameter("kind", "service");
        alarm.appendParameter("name", SystemUtil.getStringFromEnv("PODNAME"));
        alarm.appendParameter("namespace", SystemUtil.getStringFromEnv("NAMESPACE"));
        alarm.appendParameter("EventMessage", alarmEvent.getEventMessage());
        alarm.appendParameter("EventSource", "BrowserGW Service");
        alarm.appendParameter("OriginalEventTime", TimeUtil.getCurrentDate());
        
        log.info("Send alarm message: {}", alarm.toString());
        return alarm;
    }
}
```

#### 2.2.3 关键特性

1. **告警去重**：使用`ConcurrentHashMap`记录最近一次告警时间
2. **重试机制**：`retrySendAlarm()`方法最多重试2次
3. **历史告警处理**：启动时获取并清除历史告警
4. **告警参数**：自动添加服务POD信息、命名空间等元数据

#### 2.2.4 告警上报接口

```java
// 查询和处理历史告警
private List<DataParam> getAlarms(String alarmIds) {
    try {
        String jsonParam = String.format("{\"cmd\":\"GET_ACTIVE_ALARMS\",\"language\":\"en-us\",\"data\":{\"appId\":\"%s\",\"alarmIds\":\"%s\"}}", 
                    DeployUtil.getCurrentAppID(), alarmIds);
        
        // 使用CSP RestTemplate
        RestTemplate restTemplate = CspRestTemplateBuilder.create();
        HttpEntity<String> requestEntity = new HttpEntity<>(jsonParam);
        ResponseEntity<String> response = restTemplate.exchange(GETALARMINTERFACE, HttpMethod.POST, requestEntity, String.class);
        
        int statusCode = response.getStatusCodeValue();
        if (HttpStatus.OK.value() != statusCode) {
            log.error("responsestatus = {} ,sn = {}", statusCode, response.getBody());
            return null;
        }
        
        String message = response.getBody();
        log.info("getAlarms response {}", message);
        AlarmResponseParam alarmResponseParam = JSONObject.parseObject(message, AlarmResponseParam.class);
        return alarmResponseParam.getData();
    } catch (Exception e) {
        log.error("getAlarms fail,Exception {}", e.getClass());
    }
    return null;
}
```

### 2.3 JSF工具包（com.huawei.csp.jsf.api）

#### 2.3.1 模块说明

**主要导入包**：
- `com.huawei.csp.jsf.api.CspRestTemplateBuilder`

**应用位置**：
- `AuditLogUtil.java` - 审计日志HTTP调用
- `AlarmServiceImpl.java` - 告警查询

#### 2.3.2 实际应用分析

```java
// AuditLogUtil.java 中的审计日志上报
public static void writeAuditLog(...) {
    // 使用CSP RestTemplateBuilder创建客户端
    RestTemplate restTemplate = CspRestTemplateBuilder.create();
    
    // 审计日志请求体
    JSONObject body = new JSONObject();
    body.put("operation", auditLogInfo.getOperation());
    body.put("level", (level != null) ? level.codeLevel : AuditLevel.MINOR);
    // ... 其他参数
    
    // 发送HTTP请求
    HttpEntity<String> requestEntity = new HttpEntity<>(body.toString());
    ResponseEntity<Integer> response = restTemplate.exchange(
        OPER_LOG_PATH, HttpMethod.POST, requestEntity, Integer.class);
    
    // 处理响应
    int statusCode = response.getStatusCodeValue();
    if (HttpStatus.OK.value() != statusCode) {
        log.error("responsestatus = {} ,sn = {}", statusCode, response.getBody());
        return;
    }
    log.info("reported audit log");
}
```

**审计日志特点**：
1. 使用CSE服务发现地址：`cse://AuditLog/plat/audit/v1/logs`
2. 支持操作日志和安全日志两种类型
3. 自动填充时间戳、应用名等系统信息
4. 错误处理完善

### 2.4 证书SDK（com.huawei.csp.certsdk）

#### 2.4.1 模块说明

**主要导入包**：
- `com.huawei.csp.certsdk.certapiImpl.CertMgrApiImpl`
- `com.huawei.csp.certsdk.certapiImpl.ExCertMgrApiImpl`
- `com.huawei.csp.certsdk.enums.SceneType`
- `com.huawei.csp.certsdk.pojo.SubscribeEntity`
- `com.huawei.csp.certsdk.pojo.ExCertEntity`

**应用位置**：
- `SubscribeCert.java` - 证书订阅
- `CertInfo.java` - 证书信息管理
- `ExCertInfo.java` - 证书变更处理器

#### 2.4.2 证书订阅实现

```java
@Component
public class SubscribeCert {
    private static final Logger log = LogManager.getLogger(SubscribeCert.class);

    @Autowired
    private ExCertInfo exCertHandler;

    @PostConstruct
    public void SubscribeCertInfo() {
        log.info("start subscribe sbg certificate scene");
        
        // 初始化证书SDK
        CertMgrApiImpl.getCertMgrApi().certSDKInit();
        
        // 配置证书订阅列表
        ArrayList<SubscribeEntity> certList = new ArrayList<>();
        
        // CA证书订阅
        SubscribeEntity caEntity = new SubscribeEntity();
        caEntity.setSceneName("sbg_server_ca_certificate");
        caEntity.setSceneDescCN("云浏览器服务端CA证书");
        caEntity.setSceneDescEN("SBG server CA certificate");
        caEntity.setSceneType(SceneType.CA);
        caEntity.setFeature(0);
        certList.add(caEntity);
        
        // 设备证书订阅
        SubscribeEntity certEntity = new SubscribeEntity();
        certEntity.setSceneName("sbg_server_device_certificate");
        certEntity.setSceneDescCN("云浏览器服务端设备证书");
        certEntity.setSceneDescEN("SBG server Device Certificate");
        certEntity.setSceneType(SceneType.DEVICE);
        certEntity.setFeature(0);
        certList.add(certEntity);
        
        // 执行订阅
        ExCertMgrApiImpl exCertMgr = ExCertMgrApiImpl.getExCertMgrApi();
        boolean isSubscriptionSuccessful = exCertMgr.subscribeExCert(
            "browsergw", 
            certList, 
            exCertHandler, 
            "/opt/csp/browsergw"
        );
        
        if (!isSubscriptionSuccessful) {
            log.error("subscribe sbg certificate scene failed");
            return;
        }
        log.info("subscribe sbg certificate scene successful");
    }
}
```

#### 2.4.3 证书信息管理

```java
public class CertInfo {
    private static CertInfo instance;
    private static String caContent = "";
    private static String deviceContent = "";
    private static String keyContent = "";
    private static String keypwd = "";

    // 单例模式
    public static synchronized CertInfo getInstance() {
        if (instance == null) {
            synchronized (CertInfo.class) {
                if (instance == null) {
                    instance = new CertInfo();
                }
            }
        }
        return instance;
    }

    // 设置证书内容
    public static synchronized void SetCaContent(String c) {
        if (c == null) {
            return;
        }
        caContent = c;
    }

    public static synchronized void SetDeviceContent(ExCertEntity cert) {
        if (cert == null) {
            return;
        }
        deviceContent = cert.getDeviceContent();
        keyContent = cert.getPrivateKeyContent();
        keypwd = new String(cert.getPrivateKeyPassword());
    }

    // 获取证书内容流
    public InputStream Ca() {
        return new ByteArrayInputStream(caContent.getBytes());
    }

    public InputStream Device() {
        return new ByteArrayInputStream(deviceContent.getBytes());
    }

    // 私钥处理：从PKCS#1转换为PKCS#8格式
    public InputStream Key() throws IOException {
        PrivateKey privateKey = loadEncryptedPrivateKey(keyContent, keypwd.toCharArray());
        return convertPkcs1ToPkcs8Stream(privateKey);
    }
}
```

#### 2.4.4 证书变更处理

```java
public class ExCertInfo implements IExCertHandler {
    private static final Logger log = LogManager.getLogger(ExCertInfo.class);
    private String sceneName;
    
    public ExCertInfo(String sceneName) {
        this.sceneName = sceneName;
    }
    
    @Override
    public void handle(ExCertInfo certInfo) {
        log.info("cert sceneName:{}, key:{}", sceneName, certInfo.getKey());
        switch (sceneName) {
            case "sbg_server_ca_certificate":
                CertInfo.getInstance().SetCaContent(certInfo.getCaContent());
                log.info("CA certificates content updated");
                break;
            case "sbg_server_device_certificate":
                CertInfo.getInstance().SetDeviceContent(certInfo.getExCertEntity());
                log.info("Device certificates content updated");
                break;
            default:
                log.warn("unknown sceneName:{}", sceneName);
        }
    }
}
```

#### 2.4.5 关键特性

1. **动态更新**：证书变更时自动通过回调机制更新
2. **多证书类型**：同时支持CA证书和设备证书
3. **自动化密钥处理**：私钥格式自动转换
4. **无缝集成到TCP服务器**：证书直接用于Netty TLS配置

### 2.5 系统工具类（com.huawei.csp.csejsdk.common）

#### 2.5.1 模块说明

**主要导入包**：
- `com.huawei.csp.csejsdk.common.utils.SystemUtil`

**应用位置**：`DeployUtil.java`

**实际代码**：

```java
public class DeployUtil {
    private static final String APP_ID_KEY = "APPID";
    private static final String APP_NAME_KEY = "APPNAME";
    
    // 获取当前服务的APPID
    public static String getCurrentAppID() {
        String appId = SystemUtil.getStringFromEnv(APP_ID_KEY);
        if (StringUtils.isBlank(appId)) {
            appId = "0";
            log.error("getCurrentServiceIP return 0");
        }
        return appId;
    }
    
    // 获取当前服务的APPNAME
    public static String getCurrentAppName() {
        String appName = SystemUtil.getStringFromEnv(APP_NAME_KEY);
        if (StringUtils.isBlank(appName)) {
            appName = "csp";
            log.error("getCurrentAppName return csp");
        }
        return appName;
    }
}
```

**功能特点**：
1. **环境变量读取**：系统自动注入的环境变量
2. **容错处理**：获取失败时提供默认值
3. **日志记录**：配置异常时记录错误日志

---

## 3. 模块间依赖关系（基于实际代码）

### 3.1 实际依赖图

```
BrowserGatewayApplication
    |
    +-- Framework.start()              [CSE框架]
    +-- OmsdkStarter.omsdkInit()      [OM管理SDK]
    |
    +-- ChromeApi                      [ServiceComb注册]
    |       +-- @RestSchema注解
    |       +-- 服务端点暴露
    |
    +-- ExtensionManageApi             [ServiceComb注册]
    |       +-- @RestSchema注解
    |       +-- 服务端点暴露
    |
    +-- CseImpl                        [ServiceComb发现]
    |       +-- RegistryUtils.findServiceInstance()
    |       +-- MicroserviceInstance查询
    |
    +-- ServerEndpointExporter         [ServiceComb工具]
    |       +-- BeanUtils
    |       +-- WebSocket Bean注册
    |
    +-- AuditLogUtil                   [JSF工具包]
    |       +-- CspRestTemplateBuilder.create()
    |       +-- 审计日志HTTP请求
    |
    +-- AlarmServiceImpl               [告警SDK]
    |       +-- AlarmSendManager
    |       +-- Alarm
    |       +-- SystemUtil.getStringFromEnv()
    |       +-- CspRestTemplateBuilder.create()
    |
    +-- SubscribeCert                  [证书SDK]
    |       +-- CertMgrApiImpl.getCertMgrApi()
    |       +-- ExCertMgrApiImpl.getExCertMgrApi()
    |
    +-- CertInfo                       [证书SDK]
    |       +-- 单例模式
    |       +-- BouncyCastle私钥处理
    |
    +-- ExCertInfo                     [证书SDK]
    |       +-- IExCertHandler接口实现
    |
    +-- ChromeSetImpl                  [CSE管理工具]
    |       +-- ServiceUtils.putInstanceProperties()
    |
    +-- HealthCheckTask                [CSE管理工具]
    |       +-- ServiceUtils.putInstanceProperties()
    |
    +-- CpuUsageCheck                  [SRV管理SDK]
    |       +-- RsApi
    |       +-- RSPojo
    |
    +-- MemoryUsageCheck               [SRV管理SDK]
    |       +-- RsApi
    |       +-- RSPojo
    |
    +-- DeployUtil                     [系统工具]
            +-- SystemUtil.getStringFromEnv()
```

### 3.2 初始化顺序（按代码执行）

```java
public static void main(String[] args) {
    // 1. 启动CSE框架
    Framework.start();
    log.info("Framework.initCsp ==================================");
    
    // 2. 初始化OM SDK
    try {
        OmsdkStarter.omsdkInit();
        System.out.println("OmsdkStarter.omsdkInit ==================================");
    } catch (Exception e) {
        log.error("CSPRunLog main: init OmsdkStarter exception", e);
    }
    
    // 3. 启动Spring Boot应用
    SpringApplication.run(BrowserGatewayApplication.class, args);
    
    // 4. 审计日志记录（测试日志上报）
    AuditLogUtil.writeAuditLog(...);
}
```

---

## 4. 关键业务流程中的CSP SDK应用

### 4.1 业务启动流程

```java
1. 应用启动
    |
    +-> Framework.start()                              // CSE框架启动
    |   |
    |   +-> [CSE框架] 微服务基础设施初始化
    |
    +-> OmsdkStarter.omsdkInit()                        // OM SDK初始化
    |
    +-> Spring Boot应用启动
        |
        +-> @RestSchema注解自动注册服务
        |   |
        |   +-> ChromeApi注册到CSE                    // REST服务暴露
        |   +-> ExtensionManageApi注册到CSE            // REST服务暴露
        |
        +-> ServiceBean工具类处理
        |   |
        |   +-> ServerEndpointExporter                 // WebSocket Bean注册
        |
        +-> SubscribeCert.SubscribeCertInfo()          // 证书订阅
            |
            +-> CertMgrApiImpl.certSDKInit()            // 证书SDK初始化
            |
            +-> ExCertMgrApiImpl.subscribeExCert()      // 核心证书订阅
```

### 4.2 健康检查与告警流程

```java
1. 健康检查任务
    |
    +-> HealthCheckTask.checkAndReport()               // 定时健康检查
        |
        +-> CpuUsageCheck.check()                      // CPU检查
        |   +-> RsApi查询CPU使用率                      // SRV管理API
        |
        +-> MemoryUsageCheck.check()                   // 内存检查
        |   +-> RsApi查询内存使用率                     // SRV管理API
        |
        +-> NetWorkInterfaceCheck.check()             // 网络检查
        |
        +-> 健康状态检测
            |
            +-> 不健康 -> alarm.sendAlarm()             // 告警SDK
            |
            +-> 健康 -> alarm.clearAlarm()              // 告警SDK
            |
            +-> ServiceUtils.putInstanceProperties()   // CSE属性上报
```

### 4.3 证书动态更新流程

```java
1. 证书推送引擎
    |
    +-> ExCertMgrApiImpl订阅的服务证书变更
        |
        +-> ExCertHandler.handle()                      // 证书回调
            |
            +-> CertInfo.SetCaContent()                 // 更新CA证书
            +-> CertInfo.SetDeviceContent()             // 更新设备证书
            |
            +-> 实际TCP服务器重启逻辑
                |
                +-> ControlTcpServer.stopServer()       // 停止原有TCP服务
                +-> ControlTcpServer.startServer()      // 使用新证书重启
```

### 4.4 审计日志上报流程

```java
1. 关键操作
    |
    +-> AuditLogUtil.writeAuditLog()                    // 记录审计日志
        |
        +-> 审计日志格式化
            |
            +-> CspRestTemplateBuilder.create()         // 创建HTTP客户端
            |
            +-> HTTP请求至审计服务
                |
                +-> 结果处理与日志记录
```

---

## 5. 实际配置与参数

### 5.1 application.yaml中相关配置

```yaml
# 服务配置
spring:
  application:
    name: browser-gateway

# CSE相关配置
servicecomb:
  service:
    name: browser-gateway
    version: 1.0.0
    properties:
      allowCrossApp: true
      environment: production
  service:
    registry:
      address: https://cse.registry.servicecomb.io
      enableSpringRegister: true
  rest:
    address: 0.0.0.0:8090
```

### 5.2 告警相关配置

```java
// 告警配置常量
public class AlarmServiceImpl {
    public static final String GETALARMINTERFACE = "cse://FMService/fmOperation/v1/alarms/get_alarms";
    private static final Integer ONE_MINUTE = 10 * 60 * 1000;  // 10分钟告警去间隔
}
```

### 5.3 证书订阅配置

```java
// 证书场景名称
public class SubscribeCert {
    private static final String CA_CERT_SCENE = "sbg_server_ca_certificate";
    private static final String DEVICE_CERT_SCENE = "sbg_server_device_certificate";
    private static final String CERT_PATH = "/opt/csp/browsergw";
}
```

---

## 6. 总结与建议

### 6.1 实际使用的CSP SDK总结

| SDK模块 | 核心功能 | 使用深度 | 替代难度 |
|---------|---------|---------|---------|
| CSE框架 | 微服务框架 | 高(CSE启动、服务注册、服务发现) | 高 |
| ServiceComb注解 | REST服务注册 | 高(服务端点暴露、WebSocket注册) | 高 |
| ServiceComb注册API | 服务发现查询 | 中(服务实例查询、端点解析) | 中 |
| OM管理SDK | 运维管理 | 中(OM初始化) | 低 |
| 告警SDK | 告警系统 | 高(告警发送、历史处理) | 中 |
| JSF工具包 | HTTP客户端封装 | 中(REST请求封装) | 低 |
| 证书SDK | 证书动态管理 | 高(证书订阅、更新) | 高 |
| SRV管理SDK | 服务资源查询 | 中(CPU/内存查询) | 低 |
| 系统工具类 | 环境变量处理 | 低(配置读取) | 低 |

### 6.2 实际发现的问题与建议

#### 6.2.1 工程实践建议

1. **证书更新**：
   - 证书通过回调机制自动更新，无需重启服务
   - 需确保TCP服务器重启逻辑的可靠性
   - 证书密钥格式转换使用BouncyCastle，需处理异常情况

2. **告警处理**：
   - 10分钟内相同告警只发送一次，避免告警风暴
   - 重试机制最多2次，间隔5秒
   - 启动时清除历史告警，避免重复通知

3. **审计日志**：
   - 使用CSE服务发现地址`cse://AuditLog/`
   - 区分操作日志和安全日志两种类型
   - HTTP请求失败有完善的错误处理


## 7. 附录

### 7.1 关键类文件清单

**CSP SDK核心应用类**：
- `BrowserGatewayApplication.java` - CSE框架启动器
- `ChromeApi.java` - REST服务注册(@RestSchema)
- `ExtensionManageApi.java` - REST服务注册(@RestSchema)
- `CseImpl.java` - 服务发现实现
- `ServerEndpointExporter.java` - ServiceComb工具类
- `AlarmServiceImpl.java` - 告警服务实现
- `SubscribeCert.java` - 证书订阅
- `CertInfo.java` - 证书信息管理
- `ExCertInfo.java` - 证书变更处理器
- `AuditLogUtil.java` - 审计日志实现
- `ChromeSetImpl.java` - CSE服务属性上报
- `HealthCheckTask.java` - 健康检查任务
- `DeployUtil.java` - 系统配置工具

**health check相关类**：
- `service/healthCheck/MemoryUsageCheck.java`
- `service/healthCheck/NetWorkInterfaceCheck.java`

---

## 附录A - CSP接口详细签名（用于Mock）

### A.1 框架启动接口

**接口包**：`com.huawei.csp.csejsdk.core.api.Framework`

**核心接口签名**：

```java
/**
 * CSE框架核心API，用于启动微服务框架
 */
public class Framework {
    /**
     * 启动(ServiceComb)微服务框架
     * 
     * @throws Exception 框架启动异常
     */
    public static void start() throws Exception;
}

/**
 * OM SDK启动器，用于初始化运维管理
 */
public class OmsdkStarter {
    /**
     * 初始化OM SDK
     * 
     * @throws Exception OM SDK初始化异常
     */
    public static void omsdkInit() throws Exception;
}
```

**Mock示例**：

```java
// Mock框架启动
class MockFramework {
    public static void start() {
        // 模拟启动成功
    }
    
    public static void startThrowsException() throws Exception {
        // 模拟启动失败
        throw new RuntimeException("Mock CSE启动失败");
    }
}

// Mock OM SDK启动
class MockOmsdkStarter {
    public static void omsdkInit() {
        // 模拟初始化成功
    }
    
    public static void omsdkInitThrowsException() throws Exception {
        // 模拟初始化失败
        throw new RuntimeException("Mock OM SDK启动失败");
    }
}
```

### A.2 服务管理工具接口

**接口包**：`com.huawei.csp.csejsdk.common.utils.ServiceUtils`

**核心接口签名**：

```java
/**
 * 服务管理工具类，用于上报服务实例属性
 */
public class ServiceUtils {
    /**
     * 上报服务实例属性
     * 
     * @param properties 属性键值对
     * @return 上报是否成功
     */
    public static boolean putInstanceProperties(Map<String, String> properties);
    
    /**
     * 获取服务实例属性
     * 
     * @param key 属性键
     * @return 属性值
     */
    public static String getInstanceProperty(String key);
}
```

**Mock示例**：

```java
// Mock服务属性上报
class MockServiceUtils {
    private static Map<String, String> mockProperties = new HashMap<>();
    
    public static boolean putInstanceProperties(Map<String, String> properties) {
        // 存储属性并返回成功
        mockProperties.putAll(properties);
        return true;
    }
    
    public static boolean putInstancePropertiesFail(Map<String, String> properties) {
        // 模拟上报失败
        return false;
    }
    
    public static String getInstanceProperty(String key) {
        return mockProperties.get(key);
    }
    
    public static void clearMockProperties() {
        mockProperties.clear();
    }
}
```

### A.3 系统工具接口

**接口包**：`com.huawei.csp.csejsdk.common.utils.SystemUtil`

**核心接口签名**：

```java
/**
 * 系统工具类，用于读取环境变量
 */
public class SystemUtil {
    /**
     * 从环境变量中获取字符串值
     * 
     * @param key 环境变量键
     * @return 环境变量值，如果不存在返回null
     */
    public static String getStringFromEnv(String key);
}
```

**Mock示例**：

```java
// Mock系统工具
class MockSystemUtil {
    private static Map<String, String> mockEnvVars = new HashMap<>();
    
    public static void setMockEnvVar(String key, String value) {
        mockEnvVars.put(key, value);
    }
    
    public static void clearMockEnvVars() {
        mockEnvVars.clear();
    }
    
    public static String getStringFromEnv(String key) {
        return mockEnvVars.get(key);
    }
}
```

### A.4 告警SDK接口

**接口包**：
- `com.huawei.csp.om.alarmsdk.alarmmanager.Alarm`
- `com.huawei.csp.om.alarmsdk.alarmmanager.AlarmSendManager`
- `com.huawei.csp.om.alarmsdk.alarmmodel.AlarmModel`
- `com.huawei.csp.om.alarmsdk.util.SystemUtil`

**核心接口签名**：

```java
/**
 * 告警发送管理器
 */
public class AlarmSendManager {
    /**
     * 获取AlarmSendManager实例
     * 
     * @return AlarmSendManager实例
     */
    public static AlarmSendManager getInstance();
    
    /**
     * 发送告警
     * 
     * @param alarm 告警对象
     * @return 发送是否成功
     */
    public boolean sendAlarm(Alarm alarm);
}

/**
 * 告警模型
 */
public class Alarm {
    /**
     * 构造告警对象
     * 
     * @param alarmId 告警ID
     * @param type 告警类型
     */
    public Alarm(String alarmId, AlarmModel.EuGenClearType type);
    
    /**
     * 添加告警参数
     * 
     * @param key 参数键
     * @param value 参数值
     */
    public void appendParameter(String key, String value);
    
    /**
     * 获取告警ID
     * 
     * @return 告警ID
     */
    public String getStrAlarmID();
    
    /**
     * 转换为字符串
     * 
     * @return 告警字符串表示
     */
    @Override
    public String toString();
}

/**
 * 告警模型类型
 */
public class AlarmModel {
    /**
     * 告警类型枚举
     */
    public enum EuGenClearType {
        GENERATE,   // 生成告警
        CLEAR       // 清除告警
    }
}
```

**Mock实现示例**：

```java
// Mock告警管理器
class MockAlarmSendManager {
    private static List<Alarm> sentAlarms = new ArrayList<>();
    private boolean alwaysSuccess = true;
    
    public static AlarmSendManager getInstance() {
        return new MockAlarmSendManagerInstance();
    }
    
    /**
     * 获取已发送的告警
     */
    public static List<Alarm> getSentAlarms() {
        return new ArrayList<>(sentAlarms);
    }
    
    /**
     * 清除Mock告警列表
     */
    public static void clearAlarms() {
        sentAlarms.clear();
    }
    
    /**
     * 检查是否发送了指定ID的告警
     */
    public static boolean hasSentAlarm(String alarmId) {
        return sentAlarms.stream()
            .anyMatch(alarm -> alarm.getStrAlarmID().equals(alarmId));
    }
    
    /**
     * 设置告警发送行为
     *
     * @param success 是否总是成功
     */
    public static void setAlwaysSuccess(boolean success) {
        alwaysSuccess = success;
    }
    
    private static class MockAlarmSendManagerInstance extends AlarmSendManager {
        @Override
        public boolean sendAlarm(Alarm alarm) {
            sentAlarms.add(alarm);
            return alwaysSuccess; // 根据配置返回成功或失败
        }
    }
}
```

### A.5 证书SDK接口

**接口包**：
- `com.huawei.csp.certsdk.certapiImpl.CertMgrApiImpl`
- `com.huawei.csp.certsdk.certapiImpl.ExCertMgrApiImpl`
- `com.huawei.csp.certsdk.enums.SceneType`
- `com.huawei.csp.certsdk.pojo.SubscribeEntity`
- `com.huawei.csp.certsdk.pojo.ExCertEntity`
- `com.huawei.csp.certsdk.enums.CertNotifyType`
- `com.huawei.csp.certsdk.handler.IExCertHandler`

**核心接口签名**：

```java
/**
 * 证书管理API
 */
public interface CertMgrApi {
    /**
     * 初始化证书SDK
     * 
     * @throws Exception SDK初始化异常
     */
    void certSDKInit() throws Exception;
}

/**
 * 扩展证书管理API
 */
public interface ExCertMgrApi {
    /**
     * 订阅扩展证书
     * 
     * @param serviceName 服务名称
     * @param certList 证书列表
     * @param handler 处理器
     * @param certPath 证书路径
     * @return 订阅是否成功
     */
    boolean subscribeExCert(String serviceName, ArrayList<SubscribeEntity> certList, 
        IExCertHandler handler, String certPath);
}

/**
 * 证书订阅接口
 */
public interface IExCertHandler {
    /**
     * 处理证书变更通知
     * 
     * @param certInfo 证书信息
     */
    void handle(ExCertInfo certInfo);
}

/**
 * 订阅实体
 */
public class SubscribeEntity {
    /**
     * 设置场景名称
     * 
     * @param sceneName 场景名称
     */
    public void setSceneName(String sceneName);
    
    /**
     * 设置场景描述(中文)
     * 
     * @param sceneDescCN 场景描述
     */
    public void setSceneDescCN(String sceneDescCN);
    
    /**
     * 设置场景描述(英文)
     * 
     * @param sceneDescEN 场景描述
     */
    public void setSceneDescEN(String sceneDescEN);
    
    /**
     * 设置场景类型
     * 
     * @param sceneType 场景类型
     */
    public void setSceneType(SceneType sceneType);
    
    /**
     * 设置特性标志
     * 
     * @param feature 特性标志
     */
    public void setFeature(int feature);
}

/**
 * 证书场景类型枚举
 */
public enum SceneType {
    CA,       // CA证书
    DEVICE    // 设备证书
}

/**
 * 扩展证书信息
 */
public class ExCertInfo {
    /**
     * 获取CA证书内容
     * 
     * @return CA证书内容
     */
    public String getCaContent();
    
    /**
     * 获取扩展证书实体
     * 
     * @return 扩展证书实体
     */
    public ExCertEntity getExCertEntity();
    
    /**
     * 获取证书键
     * 
     * @return 证书键
     */
    public String getKey();
}

/**
 * 扩展证书实体类
 */
public class ExCertEntity {
    /**
     * 获取设备证书内容
     * 
     * @return 设备证书内容
     */
    public String getDeviceContent();
    
    /**
     * 获取私钥内容
     * 
     * @return 私钥内容
     */
    public String getPrivateKeyContent();
    
    /**
     * 获取私钥密码
     * 
     * @return 私钥密码
     */
    public byte[] getPrivateKeyPassword();
}
```

**Mock实现示例**：

```java
// Mock证书API实现
class MockCertMgrApi implements CertMgrApi {
    @Override
    public void certSDKInit() {
        // 模拟初始化成功
    }
}

class MockExCertMgrApi implements ExCertMgrApi {
    @Override
    public boolean subscribeExCert(String serviceName, ArrayList<SubscribeEntity> certList, 
        IExCertHandler handler, String certPath) {
        // 模拟订阅并触发回调
        if (handler != null) {
            handler.handle(new MockExCertInfo());
        }
        return true;
    }
}

// Mock证书信息
class MockExCertInfo implements ExCertInfo {
    @Override
    public String getCaContent() {
        return "MOCK_CA_CERT_CONTENT";
    }
    
    @Override
    public ExCertEntity getExCertEntity() {
        return new MockExCertEntity();
    }
    
    @Override
    public String getKey() {
        return "mock_key";
    }
}

// Mock扩展证书实体
class MockExCertEntity implements ExCertEntity {
    @Override
    public String getDeviceContent() {
        return "MOCK_DEVICE_CONTENT";
    }
    
    @Override
    public String getPrivateKeyContent() {
        return "MOCK_PRIVATE_KEY_CONTENT";
    }
    
    @Override
    public byte[] getPrivateKeyPassword() {
        return "mock_password".getBytes();
    }
}
```

### A.6 JSF工具包接口

**接口包**：`com.huawei.csp.jsf.api.CspRestTemplateBuilder`

**核心接口签名**：

```java
/**
 * JSF Rest模板构建器
 */
public class CspRestTemplateBuilder {
    /**
     * 创建RestTemplate实例
     * 
     * @return RestTemplate实例
     */
    public static RestTemplate create();
}
```

**Mock示例**：

```java
// Mock JSF Rest模板构建器
class MockCspRestTemplateBuilder {
    private static RestTemplate mockTemplate;
    
    public static void setMockTemplate(RestTemplate template) {
        mockTemplate = template;
    }
    
    public static RestTemplate create() {
        return mockTemplate != null ? mockTemplate : new MockRestTemplate();
    }
    
    private static class MockRestTemplate extends RestTemplate {
        @Override
        public <T> ResponseEntity<T> exchange(String url, HttpMethod method, 
            HttpEntity<?> requestEntity, Class<T> responseType) throws RestClientException {
            
            // 模拟默认成功响应
            if (responseType == Integer.class) {
                return (ResponseEntity<T>) ResponseEntity.ok(200);
            } else if (responseType == String.class) {
                return (ResponseEntity<T>) ResponseEntity.ok("{\"data\": []}");
            }
            
            return (ResponseEntity<T>) ResponseEntity.ok("success");
        }
    }
}
```

### A.7 SRV管理SDK接口

**接口包**：
- `com.huawei.csp.csejsdk.rssdk.api.RsApi`
- `com.huawei.csp.csejsdk.rssdk.rspojo.RSPojo`

#### 核心接口签名

**RsApi接口**：

```java
/**
 * 服务资源API
 */
public interface RsApi {
    /**
     * 查询CPU使用率
     * 
     * @param metric 指标名称
     * @return CPU使用率百分比
     * @deprecated 使用getLatestContainerResourceStatistics替代
     */
    float queryCpuRate();
    
    /**
     * 查询内存使用率
     * 
     * @param metric 指标名称
     * @return 内存使用率百分比
     * @deprecated 使用getLatestContainerResourceStatistics替代
     */
    float queryMemoryRate();
    
    /**
     * 获取最新的容器资源统计信息
     * 
     * @param metric 指标类型 ("cpu" 或 "memory")
     * @return 资源统计信息
     */
    RSPojo.APIBackConfig getLatestContainerResourceStatistics(String metric);
}
```

**RSPojo类**：

```java
/**
 * 服务资源POJO
 */
public class RSPojo {
    /**
     * API返回配置类
     */
    public static class APIBackConfig {
        /** 是否成功 */
        public boolean isSuccess;
        
        /** 使用率百分比 */
        public float ratio;
        
        /** 时间戳 */
        public long timestamp;
        
        /** 可用资源量 */
        public long available;
        
        /** 总资源量 */
        public long capacity;
    }
}
```

**Mock实现示例**：

```java
// Mock服务资源API
class MockRsApi implements RsApi {
    private static Map<String, Float> mockMetrics = new ConcurrentHashMap<>();
    private static Map<String, RSPojo.APIBackConfig> mockStats = new ConcurrentHashMap<>();
    
    public static void setMockMetric(String name, float value) {
        mockMetrics.put(name, value);
    }
    
    public static void setMockStatistics(String metricType, RSPojo.APIBackConfig config) {
        mockStats.put(metricType, config);
    }
    
    public static void clearMockMetrics() {
        mockMetrics.clear();
        mockStats.clear();
    }
    
    @Override
    public RSPojo.APIBackConfig getLatestContainerResourceStatistics(String metric) {
        RSPojo.APIBackConfig config = mockStats.get(metric);
        if (config == null) {
            // 创建默认返回值
            config = new RSPojo.APIBackConfig();
            config.isSuccess = true;
            config.ratio = mockMetrics.getOrDefault(metric, 50.0f);
            config.timestamp = System.currentTimeMillis();
        }
        return config;
    }
    
    @Override
    public float queryCpuRate() {
        RSPojo.APIBackConfig config = getLatestContainerResourceStatistics("cpu");
        return config.ratio;
    }
    
    @Override
    public float queryMemoryRate() {
        RSPojo.APIBackConfig config = getLatestContainerResourceStatistics("memory");
        return config.ratio;
    }
}
```

### A.8 ServiceComb注册发现接口

**接口包**：
- `org.apache.servicecomb.serviceregistry.RegistryUtils`
- `org.apache.servicecomb.registry.api.registry.MicroserviceInstance`

**核心接口签名**：

```java
/**
 * ServiceComb注册工具类
 */
public final class RegistryUtils {
    /**
     * 查找服务实例
     * 
     * @param applicationId 应用ID
     * @param serviceName 服务名称
     * @param version 服务版本
     * @return 服务实例列表
     */
    public static List<MicroserviceInstance> findServiceInstance(String applicationId, String serviceName, String version);
    
    /**
     * 获取当前服务实例
     * 
     * @return 当前服务实例
     */
    public static MicroserviceInstance getMicroserviceInstance();
}
```

### A.9 ServiceComb REST注解

**接口包**：`org.apache.servicecomb.provider.rest.common.RestSchema`

**核心注解签名**：

```java
/**
 * REST服务Schema注解
 */
@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE})
public @interface RestSchema {
    /**
     * Schema ID，用于服务注册
     */
    String schemaId();
    
    /**
     * 是否注册到服务中心
     */
    boolean register() default true;
}
```

### A.10 完整Mock设置示例

```java
/**
 * CSP SDK Mock设置
 */
public class CspSdkMockSettings {
    
    private static Map<String, Object> mockInstances = new ConcurrentHashMap<>();
    
    /**
     * 设置所有CSP SDK的Mock
     */
    public static void setupAllMocks() {
        setupFrameworkMock();
        setupSystemUtilMock();
        setupServiceUtilsMock();
        setupAlarmSdkMock();
        setupCertificateMock();
        setupRsApiMock();
        setupRegistryMock();
        setupRestTemplateMock();
    }
    
    /**
     * 清理所有Mock设置
     */
    public static void clearAllMocks() {
        MockAlarmSendManager.clearAlarms();
        MockServiceUtils.clearMockProperties();
        MockRsApi.clearMockMetrics();
        MockRegistryUtils.clear();
        MockSystemUtil.clearMockEnvVars();
    }
    
    /**
     * 设置框架Mock
     */
    private static void setupFrameworkMock() {
        // 模拟环境变量
        MockSystemUtil.setMockEnvVar("SERVICENAME", "browser-gateway");
        MockSystemUtil.setMockEnvVar("PODNAME", "browser-gateway-pod-1");
        MockSystemUtil.setMockEnvVar("NAMESPACE", "production");
        
        // 预设告警ID
        MockSystemUtil.setMockEnvVar("APPID", "0");
    }
    
    /**
     * 设置系统工具Mock
     */
    private static void setupSystemUtilMock() {
        // 预设CSP应用ID和名称
        MockSystemUtil.setMockEnvVar("APPID", "0");
        MockSystemUtil.setMockEnvVar("APPNAME", "csp");
    }
    
    /**
     * 设置服务工具Mock
     */
    private static void setupServiceUtilsMock() {
        MockServiceUtils.clearMockProperties();
    }
    
    /**
     * 设置告警SDK Mock
     */
    private static void setupAlarmSdkMock() {
        MockAlarmSendManager.setAlwaysSuccess(true);
    }
    
    /**
     * 设置证书SDK Mock
     */
    private static void setupCertificateMock() {
        // 证书Mock设置在测试中动态触发
        // 预设模拟证书内容
        MockRsApi.setMockStatistics("memory", createDefaultMemoryConfig());
    }
    
    /**
     * 设置SRV API Mock
     */
    private static void setupRsApiMock() {
        RSPojo.APIBackConfig cpuConfig = new RSPojo.APIBackConfig();
        cpuConfig.isSuccess = true;
        cpuConfig.ratio = 45.0f;
        cpuConfig.timestamp = System.currentTimeMillis();
        MockRsApi.setMockStatistics("cpu", cpuConfig);
        
        RSPojo.APIBackConfig memoryConfig = createDefaultMemoryConfig();
        MockRsApi.setMockStatistics("memory", memoryConfig);
    }
    
    /**
     * 设置注册发现Mock
     */
    private static void setupRegistryMock() {
        MockMicroserviceInstance instance = new MockMicroserviceInstance();
        instance.setEndpoints(Arrays.asList("http://test:8090"));
        MockRegistryUtils.setMockCurrentInstance(instance);
    }
    
    /**
     * 设置RestTemplate Mock
     */
    private static void setupRestTemplateMock() {
        // 使用默认Mock
    }
    
    /**
     * 创建默认内存配置
     */
    private static RSPojo.APIBackConfig createDefaultMemoryConfig() {
        RSPojo.APIBackConfig config = new RSPojo.APIBackConfig();
        config.isSuccess = true;
        config.ratio = 60.0f;
        config.timestamp = System.currentTimeMillis();
        return config;
    }
    
    /**
     * 获取Mock设置状态
     */
    public static Map<String, Object> getMockStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("alarmCount", MockAlarmSendManager.getSentAlarmCount());
        status.put("rsApiCpuMetric", MockRsApi.getMockMetrics().get("cpu"));
        status.put("rsApiMemoryMetric", MockRsApi.getMockMetrics().get("memory"));
        status.put("serviceProps", MockServiceUtils.getAllInstanceProperties());
        return status;
    }
}
```

---

## 附录B - 完整接口使用示例

### B.1 框架启动完整流程

```java
/**
 * CSP框架启动Mock示例
 */
@Test
public void testFrameworkStartup() throws Exception {
    // 1. 设置环境变量
    MockSystemUtil.setMockEnvVar("SERVICENAME", "browser-gateway");
    MockSystemUtil.setMockEnvVar("PODNAME", "browser-gateway-pod-1");
    MockSystemUtil.setMockEnvVar("NAMESPACE", "production");
    
    // 2. 启动CSE框架
    Framework.start();
    
    // 3. 初始化OM SDK
    OmsdkStarter.omsdkInit();
    
    // 4. 验证启动状态
    assertTrue(MockSystemUtil.getStringFromEnv("SERVICENAME").equals("browser-gateway"));
}
```

### B.2 告警系统完整使用

```java
/**
 * 告警系统Mock示例
 */
@Test
public void testAlarmSystem() {
    // 1. 设置告警Mock行为
    MockAlarmSendManager.setAlwaysSuccess(true);
    
    // 2. 构造告警对象
    AlarmModel alarmModel = new AlarmModel();
    Alarm alarm = new Alarm("1001", AlarmModel.EuGenClearType.GENERATE);
    alarm.appendParameter("source", MockSystemUtil.getStringFromEnv("SERVICENAME"));
    alarm.appendParameter("kind", "service");
    alarm.appendParameter("name", MockSystemUtil.getStringFromEnv("PODNAME"));
    alarm.appendParameter("namespace", MockSystemUtil.getStringFromEnv("NAMESPACE"));
    alarm.appendParameter("EventMessage", "Test alarm message");
    alarm.appendParameter("EventSource", "BrowserGW Service");
    alarm.appendParameter("OriginalEventTime", "2026-02-13 12:00:00");
    
    // 3. 发送告警
    AlarmSendManager manager = MockAlarmSendManager.getInstance();
    boolean success = manager.sendAlarm(alarm);
    
    // 4. 验证告警发送
    assertTrue(success);
    assertEquals(1, MockAlarmSendManager.getSentAlarmCount());
    assertTrue(MockAlarmSendManager.hasSentAlarm("1001"));
    
    // 5. 清除告警
    Alarm clearAlarm = new Alarm("1001", AlarmModel.EuGenClearType.CLEAR);
    manager.sendAlarm(clearAlarm);
}
```

### B.3 证书管理完整流程

```java
/**
 * 证书管理Mock示例
 */
@Test
public void testCertificateManagement() throws Exception {
    // 1. 初始化证书SDK
    MockCertMgrApi mockCertApi = new MockCertMgrApi();
    mockCertApi.certSDKInit();
    
    // 2. 配置证书订阅实体
    ArrayList<SubscribeEntity> certList = new ArrayList<>();
    
    SubscribeEntity caEntity = new SubscribeEntity();
    caEntity.setSceneName("sbg_server_ca_certificate");
    caEntity.setSceneDescCN("云浏览器服务端CA证书");
    caEntity.setSceneDescEN("SBG server CA certificate");
    caEntity.setSceneType(SceneType.CA);
    caEntity.setFeature(0);
    certList.add(caEntity);
    
    SubscribeEntity deviceEntity = new SubscribeEntity();
    deviceEntity.setSceneName("sbg_server_device_certificate");
    deviceEntity.setSceneDescCN("云浏览器服务端设备证书");
    deviceEntity.setSceneDescEN("SBG server Device Certificate");
    deviceEntity.setSceneType(SceneType.DEVICE);
    deviceEntity.setFeature(0);
    certList.add(deviceEntity);
    
    // 3. 订阅证书
    MockExCertMgrApi mockExMgrApi = new MockExCertMgrApi();
    MockExCertHandler handler = new MockExCertHandler();
    boolean success = mockExMgrApi.subscribeExCert("browsergw", certList, handler, "/opt/csp/browsergw");
    assertTrue(success);
    
    // 4. 验证回调触发
    assertTrue(handler.isCallbackTriggered());
    assertNotNull(handler.getCertInfo());
}
```

### B.4 服务资源监控完整流程

```java
/**
 * 服务资源监控Mock示例
 */
@Test
public void testResourceMonitoring() {
    // 1. 设置CPU监控Mock
    RSPojo.APIBackConfig cpuConfig = new RSPojo.APIBackConfig();
    cpuConfig.isSuccess = true;
    cpuConfig.ratio = 75.5f;
    cpuConfig.timestamp = System.currentTimeMillis();
    MockRsApi.setMockStatistics("cpu", cpuConfig);
    
    // 2. 设置内存监控Mock
    RSPojo.APIBackConfig memoryConfig = new RSPojo.APIBackConfig();
    memoryConfig.isSuccess = true;
    memoryConfig.ratio = 85.5f;
    memoryConfig.timestamp = System.currentTimeMillis();
    MockRsApi.setMockStatistics("memory", memoryConfig);
    
    // 3. 查询CPU使用率
    RsApi rsApi = new MockRsApi();
    RSPojo.APIBackConfig cpuStats = rsApi.getLatestContainerResourceStatistics("cpu");
    assertTrue(cpuStats.isSuccess);
    assertEquals(75.5f, cpuStats.ratio, 0.01);
    
    // 4. 查询内存使用率
    RSPojo.APIBackConfig memoryStats = rsApi.getLatestContainerResourceStatistics("memory");
    assertTrue(memoryStats.isSuccess);
    assertEquals(85.5f, memoryStats.ratio, 0.01);
}
```

### B.5 服务注册发现完整流程

```java
/**
 * 服务注册发现Mock示例
 */
@Test
public void testServiceDiscovery() {
    // 1. 创建Mock服务实例
    MockMicroserviceInstance instance1 = new MockMicroserviceInstance();
    instance1.setInstanceId("instance-1");
    instance1.setStatus(MicroserviceInstanceStatus.UP);
    instance1.setEndpoints(Arrays.asList("http://server1:8090", "https://server1:8443"));
    
    MockMicroserviceInstance instance2 = new MockMicroserviceInstance();
    instance2.setInstanceId("instance-2");
    instance2.setStatus(MicroserviceInstanceStatus.UP);
    instance2.setEndpoints(Arrays.asList("http://server2:8090"));
    
    // 2. 设置服务实例
    List<MicroserviceInstance> instances = Arrays.asList(instance1, instance2);
    MockRegistryUtils.setMockInstances(instances);
    
    // 3. 查找服务实例
    List<MicroserviceInstance> foundInstances = MockRegistryUtils.findServiceInstance("0", "gids", "0+");
    assertEquals(2, foundInstances.size());
    
    // 4. 验证实例状态
    for (MicroserviceInstance instance : foundInstances) {
        assertEquals(MicroserviceInstanceStatus.UP, instance.getStatus());
        assertFalse(instance.getEndpoints().isEmpty());
    }
}
```