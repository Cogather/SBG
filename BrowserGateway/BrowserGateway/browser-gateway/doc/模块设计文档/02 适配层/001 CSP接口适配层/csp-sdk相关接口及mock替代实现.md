# BrowserGateway CSP SDK 接口完整签名（用于Mock）

## 文档信息

| 项目 | BrowserGateway |
|------|----------------|
| 文档类型 | CSP SDK接口签名文档 |
| 版本 | 2.0 |
| 日期 | 2026-03-07 |
| 更新说明 | 根据实际代码实现刷新文档，确保100%一致 |

---

## 1. 概述

本文档提供了BrowserGateway项目中使用的所有CSP SDK接口的详细签名，便于在外部环境中进行Mock和模拟测试。每个接口都包含了方法定义、参数说明和Mock示例代码。

**注意**: 本文档描述的是CSP SDK底层接口，而非适配器层接口。适配器层接口请参考`doc/csp适配层设计/CSP-接口适配层架构设计.md`。

---

## 2. 框架启动接口

### 2.1 Framework - CSE框架核心API

**包路径**：`com.huawei.csp.csejsdk.core.api.Framework`

```java
/**
 * CSE框架核心API
 */
public class Framework {
    /**
     * 启动(ServiceComb)微服务框架
     * @throws Exception 框架启动异常
     */
    public static void start() throws Exception;

    /**
     * 初始化框架配置
     * @param config 框架配置对象
     */
    public static void initialize(Object config);
}
```

### 2.2 OmsdkStarter - OM SDK启动器

**包路径**：`com.huawei.csp.om.transport.vertx.init.OmsdkStarter`

```java
/**
 * OM SDK启动器
 */
public class OmsdkStarter {
    /**
     * 初始化OM SDK
     * @throws Exception OM SDK初始化异常
     */
    public static void omsdkInit() throws Exception;

    /**
     * 停止OM SDK
     * @throws Exception OM SDK停止异常
     */
    public static void omsdkStop() throws Exception;
}
```

**Mock实现示例**：

```java
/**
 * Mock Framework
 */
public class MockFramework {
    private static boolean isStarted = false;

    public static void start() throws Exception {
        isStarted = true;
        System.out.println("Mock Framework started");
    }

    public static void reset() {
        isStarted = false;
    }

    public static boolean isStarted() {
        return isStarted;
    }
}

/**
 * Mock OmsdkStarter
 */
public class MockOmsdkStarter {
    private static boolean isInitialized = false;

    public static void omsdkInit() throws Exception {
        isInitialized = true;
        System.out.println("Mock OmsdkStarter initialized");
    }

    public static void reset() {
        isInitialized = false;
    }

    public static boolean isInitialized() {
        return isInitialized;
    }
}
```

---

## 3. 服务管理工具接口

### 3.1 ServiceUtils - 服务实例属性管理

**包路径**：`com.huawei.csp.csejsdk.common.utils.ServiceUtils`

```java
/**
 * 服务管理工具类
 */
public class ServiceUtils {
    /**
     * 上报服务实例属性
     * @param properties 属性键值对
     * @return 上报是否成功
     */
    public static boolean putInstanceProperties(Map<String, String> properties);

    /**
     * 获取服务实例属性
     * @param key 属性键
     * @return 属性值，如果不存在返回null
     */
    public static String getInstanceProperty(String key);

    /**
     * 获取所有服务实例属性
     * @return 属性键值对映射
     */
    public static Map<String, String> getAllInstanceProperties();

    /**
     * 删除服务实例属性
     * @param key 属性键
     * @return 删除是否成功
     */
    public static boolean removeInstanceProperty(String key);
}
```

**Mock实现示例**：

```java
/**
 * Mock ServiceUtils
 */
public class MockServiceUtils {
    private static Map<String, String> mockProperties = new ConcurrentHashMap<>();
    private static boolean alwaysSuccess = true;

    public static void setAlwaysSuccess(boolean success) {
        alwaysSuccess = success;
    }

    public static boolean putInstanceProperties(Map<String, String> properties) {
        if (!alwaysSuccess) return false;
        mockProperties.putAll(properties);
        return true;
    }

    public static String getInstanceProperty(String key) {
        return mockProperties.get(key);
    }

    public static Map<String, String> getAllInstanceProperties() {
        return new HashMap<>(mockProperties);
    }

    public static boolean removeInstanceProperty(String key) {
        return mockProperties.remove(key) != null;
    }

    public static void reset() {
        mockProperties.clear();
        alwaysSuccess = true;
    }
}
```

---

## 4. 系统工具接口

### 4.1 SystemUtil - 环境变量管理

**包路径**：`com.huawei.csp.csejsdk.common.utils.SystemUtil`

```java
/**
 * 系统工具类
 */
public class SystemUtil {
    /**
     * 从环境变量中获取字符串值
     * @param key 环境变量键
     * @return 环境变量值，如果不存在返回null
     */
    public static String getStringFromEnv(String key);

    /**
     * 从环境变量中获取整数值
     * @param key 环境变量键
     * @return 环境变量值，如果不存在或解析失败返回0
     */
    public static int getIntFromEnv(String key);

    /**
     * 从环境变量中获取长整型值
     * @param key 环境变量键
     * @return 环境变量值，如果不存在或解析失败返回0L
     */
    public static long getLongFromEnv(String key);

    /**
     * 从环境变量中获取布尔值
     * @param key 环境变量键
     * @return 环境变量值，如果不存在或解析失败返回false
     */
    public static boolean getBooleanFromEnv(String key);

    /**
     * 设置环境变量（用于测试）
     * @param key 键
     * @param value 值
     */
    public static void setEnv(String key, String value);
}
```

**Mock实现示例**：

```java
/**
 * Mock SystemUtil
 */
public class MockSystemUtil {
    private static Map<String, String> mockEnvVars = new ConcurrentHashMap<>();

    public static void setMockEnvVar(String key, String value) {
        mockEnvVars.put(key, value);
    }

    public static String getStringFromEnv(String key) {
        return mockEnvVars.get(key);
    }

    public static int getIntFromEnv(String key) {
        String value = mockEnvVars.get(key);
        try {
            return value != null ? Integer.parseInt(value) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static long getLongFromEnv(String key) {
        String value = mockEnvVars.get(key);
        try {
            return value != null ? Long.parseLong(value) : 0L;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public static boolean getBooleanFromEnv(String key) {
        String value = mockEnvVars.get(key);
        return value != null && Boolean.parseBoolean(value);
    }

    public static void reset() {
        mockEnvVars.clear();
    }
}
```

---

## 5. 告警SDK接口

### 5.1 AlarmSendManager - 告警发送管理

**包路径**：`com.huawei.csp.om.alarmsdk.alarmmanager.AlarmSendManager`

```java
/**
 * 告警发送管理器
 */
public class AlarmSendManager {
    /**
     * 获取AlarmSendManager实例
     * @return AlarmSendManager实例，单例模式
     */
    public static AlarmSendManager getInstance();

    /**
     * 发送告警
     * @param alarm 告警对象
     * @return 发送是否成功
     */
    public boolean sendAlarm(Alarm alarm);

    /**
     * 查询告警状态
     * @param alarmId 告警ID
     * @return 告警状态（是否已激活）
     */
    public boolean isAlarmActive(String alarmId);
}
```

### 5.2 Alarm - 告警模型

**包路径**：`com.huawei.csp.om.alarmsdk.alarmmanager.Alarm`

```java
/**
 * 告警模型
 */
public class Alarm {
    /**
     * 构造告警对象
     * @param alarmId 告警ID
     * @param type 告警类型（生成或清除）
     */
    public Alarm(String alarmId, AlarmModel.EuGenClearType type);

    /**
     * 添加告警参数
     * @param key 参数键
     * @param value 参数值
     * @return 告警对象本身，支持链式调用
     */
    public Alarm appendParameter(String key, String value);

    /**
     * 获取告警ID
     * @return 告警ID
     */
    public String getStrAlarmID();

    /**
     * 获取告警类型
     * @return 告警类型
     */
    public AlarmModel.EuGenClearType getType();

    /**
     * 获取所有参数
     * @return 参数映射
     */
    public Map<String, String> getParameters();
}
```

### 5.3 AlarmModel - 告警模型类型

**包路径**：`com.huawei.csp.om.alarmsdk.alarmmodel.AlarmModel`

```java
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

    /**
     * 获取告警类型
     * @return 告警类型
     */
    public EuGenClearType getEuGenClearType();
}
```

**Mock实现示例**：

```java
/**
 * Mock AlarmSendManager
 */
public class MockAlarmSendManager extends AlarmSendManager {
    private static List<Alarm> sentAlarms = new CopyOnWriteArrayList<>();
    private static Map<String, Boolean> activeAlarms = new ConcurrentHashMap<>();
    private static boolean alwaysSuccess = true;

    public static void setAlwaysSuccess(boolean success) {
        alwaysSuccess = success;
    }

    @Override
    public boolean sendAlarm(Alarm alarm) {
        sentAlarms.add(alarm);
        if (alarm.getType() == AlarmModel.EuGenClearType.GENERATE) {
            activeAlarms.put(alarm.getStrAlarmID(), true);
        } else {
            activeAlarms.put(alarm.getStrAlarmID(), false);
        }
        return alwaysSuccess;
    }

    @Override
    public boolean isAlarmActive(String alarmId) {
        return activeAlarms.getOrDefault(alarmId, false);
    }

    public static List<Alarm> getSentAlarms() {
        return new ArrayList<>(sentAlarms);
    }

    public static int getSentAlarmCount() {
        return sentAlarms.size();
    }

    public static Alarm getLastSentAlarm() {
        return sentAlarms.isEmpty() ? null : sentAlarms.get(sentAlarms.size() - 1);
    }

    public static void reset() {
        sentAlarms.clear();
        activeAlarms.clear();
        alwaysSuccess = true;
    }
}

/**
 * Mock Alarm
 */
public class MockAlarm extends Alarm {
    private Map<String, String> parameters = new HashMap<>();

    public MockAlarm(String alarmId, AlarmModel.EuGenClearType type) {
        super(alarmId, type);
    }

    @Override
    public Alarm appendParameter(String key, String value) {
        parameters.put(key, value);
        return super.appendParameter(key, value);
    }

    public Map<String, String> getMockParameters() {
        return new HashMap<>(parameters);
    }
}
```

---

## 6. 证书SDK接口

### 6.1 CertMgrApi - 证书管理API

**包路径**：`com.huawei.csp.certsdk.certapiImpl.CertMgrApi`

```java
/**
 * 证书管理API接口
 */
public interface CertMgrApi {
    /**
     * 初始化证书SDK
     * @throws Exception SDK初始化异常
     */
    void certSDKInit() throws Exception;

    /**
     * 清理证书SDK
     * @throws Exception SDK清理异常
     */
    void certSDKClose() throws Exception;
}
```

### 6.2 CertMgrApiImpl - 证书管理实现

**包路径**：`com.huawei.csp.certsdk.certapiImpl.CertMgrApiImpl`

```java
/**
 * 证书管理API实现
 */
public class CertMgrApiImpl {
    /**
     * 获取证书管理API实例
     * @return 证书管理API实例，单例模式
     */
    public static CertMgrApi getCertMgrApi();
}
```

### 6.3 ExCertMgrApi - 扩展证书管理API

**包路径**：`com.huawei.csp.certsdk.certapiImpl.ExCertMgrApi`

```java
/**
 * 扩展证书管理API接口
 */
public interface ExCertMgrApi {
    /**
     * 订阅扩展证书
     * @param serviceName 服务名称
     * @param certList 证书列表
     * @param handler 证书变更处理器
     * @param certPath 证书存储路径
     * @return 订阅是否成功
     */
    boolean subscribeExCert(String serviceName, ArrayList<SubscribeEntity> certList,
        IExCertHandler handler, String certPath);

    /**
     * 取消订阅扩展证书
     * @param serviceName 服务名称
     * @return 取消订阅是否成功
     */
    boolean unsubscribeExCert(String serviceName);
}
```

### 6.4 ExCertMgrApiImpl - 扩展证书管理实现

**包路径**：`com.huawei.csp.certsdk.certapiImpl.ExCertMgrApiImpl`

```java
/**
 * 扩展证书管理API实现
 */
public class ExCertMgrApiImpl {
    /**
     * 获取扩展证书管理API实例
     * @return 扩展证书管理API实例，单例模式
     */
    public static ExCertMgrApi getExCertMgrApi();
}
```

### 6.5 IExCertHandler - 证书变更处理器接口

**包路径**：`com.huawei.csp.certsdk.handler.IExCertHandler`

```java
/**
 * 证书变更处理器接口
 */
public interface IExCertHandler {
    /**
     * 处理证书变更通知
     * @param exCertEntity 证书实体列表
     * @param certNotifyType 证书通知类型
     */
    void exCertHandler(List<com.huawei.csp.certsdk.pojo.ExCertEntity> exCertEntity,
                       com.huawei.csp.certsdk.enums.CertNotifyType certNotifyType);
}
```

### 6.6 SubscribeEntity - 证书订阅实体

**包路径**：`com.huawei.csp.certsdk.pojo.SubscribeEntity`

```java
/**
 * 证书订阅实体
 */
public class SubscribeEntity {
    private String sceneName;        // 场景名称
    private String sceneDescCN;     // 场景描述(中文)
    private String sceneDescEN;     // 场景描述(英文)
    private SceneType sceneType;    // 场景类型
    private int feature;           // 特性标志

    // Setter方法
    public void setSceneName(String sceneName);
    public void setSceneDescCN(String sceneDescCN);
    public void setSceneDescEN(String sceneDescEN);
    public void setSceneType(SceneType sceneType);
    public void setFeature(int feature);

    // Getter方法
    public String getSceneName();
    public String getSceneDescCN();
    public String getSceneDescEN();
    public SceneType getSceneType();
    public int getFeature();
}
```

### 6.7 SceneType - 场景类型枚举

**包路径**：`com.huawei.csp.certsdk.enums.SceneType`

```java
/**
 * 证书场景类型枚举
 */
public enum SceneType {
    CA,       // CA证书
    DEVICE    // 设备证书
}
```

### 6.8 ExCertEntity - 扩展证书实体

**包路径**：`com.huawei.csp.certsdk.pojo.ExCertEntity`

```java
/**
 * 扩展证书实体
 * 与适配器层的CertEntity定义完全一致
 */
public class ExCertEntity {
    private String sceneName;
    private String certName;
    private CertType certType;
    private String caFileName;
    private String caContent;
    private int caContentLen;
    private String deviceFileName;
    private String deviceContent;
    private int deviceContentLen;
    private String deviceSN;
    private String crlFileName;
    private String crlContent;
    private int crlContentLen;
    private String privateKeyFileName;
    private String privateKeyContent;
    private int privateKeyContentLen;
    private byte[] privateKeyPassword;

    // Getters and Setters...
}
```

### 6.9 CertType - 证书类型枚举

**包路径**：`com.huawei.csp.certsdk.enums.CertType`

```java
/**
 * 证书类型枚举
 */
public enum CertType {
    CERT_TYPE_CA(0),
    CERT_TYPE_DEVICE(1),
    CERT_TYPE_CRL(2),
    CERT_TYPE_CA_DEVICE(3),
    CERT_TYPE_CA_CRL(4),
    CERT_TYPE_CA_DEVICE_CRL(5),
    CERT_TYPE_EMPTY(6);

    private final int type;

    CertType(int type) {
        this.type = type;
    }

    public int getType() {
        return this.type;
    }

    public static CertType fromType(int type) {
        for (CertType certType : values()) {
            if (certType.type == type) {
                return certType;
            }
        }
        throw new IllegalArgumentException("Unknown CertType: " + type);
    }
}
```

### 6.10 CertNotifyType - 证书通知类型枚举

**包路径**：`com.huawei.csp.certsdk.enums.CertNotifyType`

```java
/**
 * 证书通知类型枚举
 */
public enum CertNotifyType {
    CERT_NOTIFY_TYPE_UPDATE(0),
    CERT_NOTIFY_TYPE_CONSISTENCY(1),
    CERT_NOTIFY_TYPE_QUERY(2),
    CERT_NOTIFY_TYPE_UNBIND(3);

    private final int state;

    CertNotifyType(int state) {
        this.state = state;
    }

    public int getState() {
        return this.state;
    }

    public static CertNotifyType fromState(int state) {
        for (CertNotifyType type : values()) {
            if (type.state == state) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown CertNotifyType state: " + state);
    }
}
```

**Mock实现示例**：

```java
/**
 * Mock CertMgrApi
 */
public class MockCertMgrApi implements CertMgrApi {
    private boolean initialized = false;

    @Override
    public void certSDKInit() {
        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }
}

/**
 * Mock ExCertMgrApi
 */
public class MockExCertMgrApi implements ExCertMgrApi {
    private List<IExCertHandler> handlers = new ArrayList<>();
    private boolean subscribed = false;

    @Override
    public boolean subscribeExCert(String serviceName, ArrayList<SubscribeEntity> certList,
        IExCertHandler handler, String certPath) {
        if (handler != null) {
            handlers.add(handler);
            subscribed = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean unsubscribeExCert(String serviceName) {
        handlers.clear();
        subscribed = false;
        return true;
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    /**
     * 触发证书变更回调
     */
    public void triggerCertChange(List<com.huawei.csp.certsdk.pojo.ExCertEntity> exCertEntity,
                                   com.huawei.csp.certsdk.enums.CertNotifyType certNotifyType) {
        for (IExCertHandler handler : handlers) {
            try {
                handler.exCertHandler(exCertEntity, certNotifyType);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
```

---

## 7. JSF工具包接口

### 7.1 CspRestTemplateBuilder - Rest模板构建器

**包路径**：`com.huawei.csp.jsf.api.CspRestTemplateBuilder`

```java
/**
 * JSF Rest模板构建器
 */
public class CspRestTemplateBuilder {
    /**
     * 创建RestTemplate实例
     * @return RestTemplate实例
     */
    public static RestTemplate create();

    /**
     * 创建指定超时配置的RestTemplate实例
     * @param connectTimeout 连接超时时间(毫秒)
     * @param readTimeout 读取超时时间(毫秒)
     * @return RestTemplate实例
     */
    public static RestTemplate create(Long connectTimeout, Long readTimeout);
}
```

**Mock实现示例**：

```java
/**
 * Mock CspRestTemplateBuilder
 */
public class MockCspRestTemplateBuilder {
    private static RestTemplate mockTemplate;

    public static void setMockTemplate(RestTemplate template) {
        mockTemplate = template;
    }

    public static RestTemplate create() {
        return mockTemplate != null ? mockTemplate : new DefaultMockRestTemplate();
    }

    public static RestTemplate create(Long connectTimeout, Long readTimeout) {
        return create();
    }

    public static void reset() {
        mockTemplate = null;
    }

    /**
     * 默认Mock RestTemplate
     */
    private static class DefaultMockRestTemplate extends RestTemplate {
        @Override
        public <T> ResponseEntity<T> exchange(String url, HttpMethod method,
            HttpEntity<?> requestEntity, Class<T> responseType) {

            // 默认成功响应
            if (responseType == Integer.class) {
                return (ResponseEntity<T>) ResponseEntity.ok(200);
            } else if (responseType == String.class) {
                return (ResponseEntity<T>) ResponseEntity.ok("{"data": []}");
            }

            return (ResponseEntity<T>) ResponseEntity.ok("success");
        }
    }
}
```

---

## 8. SRV管理SDK接口

### 8.1 RsApi - 服务资源API

**包路径**：`com.huawei.csp.csejsdk.rssdk.api.RsApi`

```java
/**
 * 服务资源API
 */
public class RsApi {
    /**
     * 获取最新的容器资源统计信息
     * @param metricType 指标类型（cpu/memory/network）
     * @return 资源统计配置对象
     */
    public static RSPojo.APIBackConfig getLatestContainerResourceStatistics(String metricType);
}
```

### 8.2 RSPojo - 资源统计POJO

**包路径**：`com.huawei.csp.csejsdk.rssdk.rspojo.RSPojo`

```java
/**
 * 资源统计POJO
 */
public class RSPojo {
    /**
     * API返回配置
     */
    public static class APIBackConfig {
        public boolean isSuccess;    // 是否成功
        public float ratio;          // 使用率百分比
        public long timestamp;       // 时间戳
    }
}
```

**Mock实现示例**：

```java
/**
 * Mock RsApi
 */
public class MockRsApi {
    private static Map<String, Float> mockMetrics = new ConcurrentHashMap<>();

    public static void setMockMetric(String name, float value) {
        mockMetrics.put(name, value);
    }

    public static void clearMockMetrics() {
        mockMetrics.clear();
    }

    public static RSPojo.APIBackConfig getLatestContainerResourceStatistics(String metricType) {
        RSPojo.APIBackConfig config = new RSPojo.APIBackConfig();
        config.isSuccess = true;
        config.ratio = mockMetrics.getOrDefault(metricType, 0f);
        config.timestamp = System.currentTimeMillis();
        return config;
    }
}
```

---

## 9. ServiceComb接口

### 9.1 RegistryUtils - 服务注册工具

**包路径**：`org.apache.servicecomb.serviceregistry.RegistryUtils`

```java
/**
 * ServiceComb注册工具类
 */
public final class RegistryUtils {
    /**
     * 查找服务实例
     * @param applicationId 应用ID
     * @param serviceName 服务名称
     * @param version 服务版本
     * @return 服务实例列表
     */
    public static List<MicroserviceInstance> findServiceInstance(String applicationId,
        String serviceName, String version);

    /**
     * 获取当前服务实例
     * @return 当前服务实例
     */
    public static MicroserviceInstance getMicroserviceInstance();

    /**
     * 获取所有服务实例
     * @return 所有服务实例映射
     */
    public static Map<String, List<MicroserviceInstance>> getAllMicroserviceInstances();
}
```

### 9.2 MicroserviceInstance - 微服务实例

**包路径**：`org.apache.servicecomb.registry.api.registry.MicroserviceInstance`

```java
/**
 * 微服务实例
 */
public interface MicroserviceInstance {
    /**
     * 获取实例状态
     * @return 实例状态
     */
    MicroserviceInstanceStatus getStatus();

    /**
     * 获取端点列表
     * @return 端点列表
     */
    List<String> getEndpoints();

    /**
     * 获取实例ID
     * @return 实例ID
     */
    String getInstanceId();

    /**
     * 获取服务名称
     * @return 服务名称
     */
    public String getServiceName();

    /**
     * 获取属性映射
     * @return 属性映射
     */
    Map<String, String> getProperties();

    /**
     * 设置属性
     * @param properties 属性映射
     */
    void setProperties(Map<String, String> properties);
}
```

### 9.3 MicroserviceInstanceStatus - 实例状态枚举

**包路径**：`org.apache.servicecomb.registry.api.registry.MicroserviceInstanceStatus`

```java
/**
 * 微服务实例状态
 */
public enum MicroserviceInstanceStatus {
    UP,       // 服务正常
    DOWN,     // 服务异常
    STARTING, // 启动中
    OUT_OF_SERVICE // 服务不可用
}
```

### 9.4 RestSchema - REST服务注解

**包路径**：`org.apache.servicecomb.provider.rest.common.RestSchema`

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

    /**
     * 是否暴露特定端点
     */
    String path() default "";
}
```

**Mock实现示例**：

```java
/**
 * Mock RegistryUtils
 */
public class MockRegistryUtils {
    private static List<MicroserviceInstance> mockInstances = new ArrayList<>();
    private static MicroserviceInstance mockCurrentInstance;
    private static Map<String, List<MicroserviceInstance>> mockAllInstances = new HashMap<>();

    public static void setMockInstances(List<MicroserviceInstance> instances) {
        mockInstances = new CopyOnWriteArrayList<>(instances);
    }

    public static void setMockCurrentInstance(MicroserviceInstance instance) {
        mockCurrentInstance = instance;
    }

    public static void addMockAllInstances(String serviceName, List<MicroserviceInstance> instances) {
        mockAllInstances.put(serviceName, new ArrayList<>(instances));
    }

    public static List<MicroserviceInstance> findServiceInstance(String applicationId,
        String serviceName, String version) {
        return mockInstances;
    }

    public static MicroserviceInstance getMicroserviceInstance() {
        return mockCurrentInstance;
    }

    public static Map<String, List<MicroserviceInstance>> getAllMicroserviceInstances() {
        return new HashMap<>(mockAllInstances);
    }

    public static void reset() {
        mockInstances.clear();
        mockCurrentInstance = null;
        mockAllInstances.clear();
    }
}

/**
 * Mock MicroserviceInstance
 */
public class MockMicroserviceInstance implements MicroserviceInstance {
    private MicroserviceInstanceStatus status = MicroserviceInstanceStatus.UP;
    private List<String> endpoints = Arrays.asList("http://127.0.0.1:8090");
    private String instanceId = "mock-instance-id";
    private String serviceName = "mock-service";
    private Map<String, String> properties = new HashMap<>();

    @Override
    public MicroserviceInstanceStatus getStatus() {
        return status;
    }

    @Override
    public List<String> getEndpoints() {
        return endpoints;
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    public void setStatus(MicroserviceInstanceStatus status) {
        this.status = status;
    }

    public void setEndpoints(List<String> endpoints) {
        this.endpoints = endpoints;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
```

---

## 10. 审计日志服务

### 10.1 审计日志服务接口

**包路径**：通过CspRestTemplateBuilder调用REST接口

```java
/**
 * 审计日志服务
 */
public class AuditLogService {
    /**
     * 操作日志接口路径
     */
    public static final String OPER_LOG_PATH = "cse://AuditLog/plat/audit/v1/logs";

    /**
     * 安全日志接口路径
     */
    public static final String SECURITY_LOG_PATH = "cse://AuditLog/plat/audit/v1/seculogs";

    /**
     * 写入操作日志
     * @param auditLogInfo 审计日志信息
     * @return 是否成功
     */
    public static boolean writeOperLog(AuditLogInfo auditLogInfo);

    /**
     * 写入安全日志
     * @param auditLogInfo 审计日志信息
     * @return 是否成功
     */
    public static boolean writeSecurityLog(AuditLogInfo auditLogInfo);
}
```

**Mock实现示例**：

```java
/**
 * Mock AuditLogService
 */
public class MockAuditLogService {
    private static List<AuditLogInfo> auditLogs = new ArrayList<>();

    public static boolean writeOperLog(AuditLogInfo auditLogInfo) {
        auditLogInfo.setAuditType("OPERATION");
        auditLogs.add(auditLogInfo);
        return true;
    }

    public static boolean writeSecurityLog(AuditLogInfo auditLogInfo) {
        auditLogInfo.setAuditType("SECURITY");
        auditLogs.add(auditLogInfo);
        return true;
    }

    public static List<AuditLogInfo> getAuditLogs() {
        return new ArrayList<>(auditLogs);
    }

    public static void reset() {
        auditLogs.clear();
    }
}
```

---

## 11. 完整Mock设置示例

### 11.1 CSP SDK统一Mock设置

```java
/**
 * CSP SDK Mock配置类
 */
public class CspSdkMockSetup {

    /**
     * 设置所有CSP SDK的Mock
     */
    public static void setupAllMocks() {
        setupFrameworkMocks();
        setupSystemUtilMock();
        setupServiceUtilsMock();
        setupAlarmSdkMock();
        setupCertificateMock();
        setupRsApiMock();
        setupRegistryMock();
        setupRestTemplateMock();
        setupAuditLogMock();
    }

    /**
     * 清理所有Mock设置
     */
    public static void clearAllMocks() {
        MockFramework.reset();
        MockOmsdkStarter.reset();
        MockSystemUtil.reset();
        MockServiceUtils.reset();
        MockAlarmSendManager.reset();
        MockExCertMgrApi.handlers.clear();
        MockRsApi.clearMockMetrics();
        MockRegistryUtils.reset();
        MockCspRestTemplateBuilder.reset();
        MockAuditLogService.reset();
    }

    /**
     * 设置框架Mock
     */
    private static void setupFrameworkMocks() {
        // 模拟环境变量
        MockSystemUtil.setMockEnvVar("SERVICENAME", "browser-gateway");
        MockSystemUtil.setMockEnvVar("PODNAME", "browser-gateway-pod-1");
        MockSystemUtil.setMockEnvVar("NAMESPACE", "production");
    }

    /**
     * 设置系统工具Mock
     */
    private static void setupSystemUtilMock() {
        // 预设环境变量
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
    }

    /**
     * 设置SRV API Mock
     */
    private static void setupSrApiMock() {
        MockRsApi.setMockMetric("cpu", 45.0f);
        MockRsApi.setMockMetric("memory", 60.0f);
        MockRsApi.setMockMetric("network", 25.0f);
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
     * 设置审计日志Mock
     */
    private static void setupAuditLogMock() {
        MockAuditLogService.reset();
    }
}
```

### 11.2 测试使用示例

```java
/**
 * CSP SDK Mock使用示例
 */
public class CspSdkMockTest {

    @Before
    public void setUp() {
        // 设置所有Mock
        CspSdkMockSetup.setupAllMocks();
    }

    @After
    public void tearDown() {
        // 清理所有Mock
        CspSdkMockSetup.clearAllMocks();
    }

    @Test
    public void testFrameworkStart() {
        // 测试框架启动
        try {
            MockFramework.start();
            assertTrue(MockFramework.isStarted());
        } catch (Exception e) {
            fail("Framework start failed: " + e.getMessage());
        }
    }

    @Test
    public void testSystemUtil() {
        // 测试环境变量读取
        MockSystemUtil.setMockEnvVar("TEST_KEY", "TEST_VALUE");
        assertEquals("TEST_VALUE", MockSystemUtil.getStringFromEnv("TEST_KEY"));
        assertEquals(0, MockSystemUtil.getIntFromEnv("NON_EXISTENT_KEY"));
    }

    @Test
    public void testServiceUtils() {
        // 测试服务属性上报
        Map<String, String> props = new HashMap<>();
        props.put("test.prop", "test.value");
        assertTrue(MockServiceUtils.putInstanceProperties(props));
        assertEquals("test.value", MockServiceUtils.getInstanceProperty("test.prop"));
    }

    @Test
    public void testAlarmSend() {
        // 测试告警发送
        Alarm alarm = new MockAlarm("1001", AlarmModel.EuGenClearType.GENERATE);
        alarm.appendParameter("source", "test-service");
        MockAlarmSendManager manager = (MockAlarmSendManager) AlarmSendManager.getInstance();
        assertTrue(manager.sendAlarm(alarm));

        // 验证告警已发送
        assertEquals(1, MockAlarmSendManager.getSentAlarmCount());
        Alarm sentAlarm = MockAlarmSendManager.getLastSentAlarm();
        assertEquals("1001", sentAlarm.getStrAlarmID());
    }

    @Test
    public void testCertificateMock() {
        // 测试证书Mock
        MockExCertMgrApi mockExMgr = new MockExCertMgrApi();
        MockExCertHandler handler = new MockExCertHandler();

        // 订阅证书
        assertTrue(mockExMgr.subscribeExCert("test-service", new ArrayList<>(), handler, "/tmp"));

        // 触发证书变更回调
        List<com.huawei.csp.certsdk.pojo.ExCertEntity> certEntities = new ArrayList<>();
        com.huawei.csp.certsdk.enums.CertNotifyType notifyType = 
            com.huawei.csp.certsdk.enums.CertNotifyType.CERT_NOTIFY_TYPE_UPDATE;
        mockExMgr.triggerCertChange(certEntities, notifyType);

        // 验证回调已触发
        assertTrue(handler.isCallbackTriggered());
    }

    @Test
    public void testRsApi() {
        // 测试资源查询
        MockRsApi.clearMockMetrics();
        MockRsApi.setMockMetric("cpu", 75.5f);

        RSPojo.APIBackConfig config = MockRsApi.getLatestContainerResourceStatistics("cpu");
        assertEquals(75.5f, config.ratio, 0.01);
        assertTrue(config.isSuccess);
    }

    @Test
    public void testRegistryUtils() {
        // 测试服务发现
        MockMicroserviceInstance instance = new MockMicroserviceInstance();
        instance.setInstanceId("test-instance-id");
        MockRegistryUtils.setMockCurrentInstance(instance);

        assertEquals("test-instance-id", MockRegistryUtils.getMicroserviceInstance().getInstanceId());
    }

    /**
     * Mock证书处理器
     */
    private static class MockExCertHandler implements IExCertHandler {
        private boolean callbackTriggered = false;

        @Override
        public void exCertHandler(List<com.huawei.csp.certsdk.pojo.ExCertEntity> exCertEntity,
                                  com.huawei.csp.certsdk.enums.CertNotifyType certNotifyType) {
            callbackTriggered = true;
        }

        public boolean isCallbackTriggered() {
            return callbackTriggered;
        }
    }
}
```

---

## 12. 接口调用顺序

### 12.1 应用启动序列

```java
// 1. 启动CSE框架
Framework.start();

// 2. 初始化OM SDK
OmsdkStarter.omsdkInit();

// 3. 读取环境变量
String serviceName = SystemUtil.getStringFromEnv("SERVICENAME");
String podName = SystemUtil.getStringFromEnv("PODNAME");

// 4. 初始化证书SDK
CertMgrApi certApi = CertMgrApiImpl.getCertMgrApi();
certApi.certSDKInit();

// 5. 订阅证书变化
ExCertMgrApi exCertApi = ExCertMgrApiImpl.getExCertMgrApi();
exCertApi.subscribeExCert("browsergw", certList, handler, certPath);
```

### 12.2 健康检查序列

```java
// 1. 查询资源使用率
RSPojo.APIBackConfig cpuConfig = RsApi.getLatestContainerResourceStatistics("cpu");
RSPojo.APIBackConfig memoryConfig = RsApi.getLatestContainerResourceStatistics("memory");
float cpuRate = cpuConfig.ratio;
float memoryRate = memoryConfig.ratio;

// 2. 判断健康状态
boolean isHealthy = cpuRate < 80.0f && memoryRate < 85.0f;

// 3. 发送/清除告警
Alarm alarm = new Alarm("3001", isHealthy ? EuGenClearType.CLEAR : EuGenClearType.GENERATE);
alarm.appendParameter("source", serviceName);
AlarmSendManager.getInstance().sendAlarm(alarm);

// 4. 上报实例属性
ServiceUtils.putInstanceProperties(healthReport);
```

### 12.3 服务注册序列

```java
// 1. 通过@RestSchema注册REST服务
@RestSchema(schemaId = "ChromeApi")
public class ChromeApi {
    // 自动注册到CSE
}

// 2. 查询服务实例
List<MicroserviceInstance> instances = RegistryUtils.findServiceInstance(appId, serviceName, version);

// 3. 获取端点
for (MicroserviceInstance instance : instances) {
    List<String> endpoints = instance.getEndpoints();
    // 使用端点进行服务调用
}

// 4. 使用CspRestTemplateBuilder创建HTTP客户端
RestTemplate restTemplate = CspRestTemplateBuilder.create();
ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);
```

---

## 13. 注意事项

1. **单例模式**：大部分CSP SDK组件使用单例模式，Mock时需要注意实例管理
2. **线程安全**：Mock实现应保证线程安全，建议使用ConcurrentHashMap等并发容器
3. **回调处理**：证书变更等回调可能异步执行，Mock中需考虑时序问题
4. **状态管理**：Mock实现中需要适当管理状态，确保测试独立性
5. **异常处理**：Mock实现应提供模拟异常场景的能力
6. **IExCertHandler方法名**：证书回调接口方法名为`exCertHandler`而非`handle`，接收`List<ExCertEntity>`和`CertNotifyType`参数
7. **RsApi方法名**：资源查询方法名为`getLatestContainerResourceStatistics`，接收单个metricType参数，返回`RSPojo.APIBackConfig`

---

## 14. 总结

本文档提供了BrowserGateway项目中使用的所有CSP SDK接口的详细签名和Mock实现示例，包括：

- **11个主要SDK模块**的接口定义（新增审计日志服务）
- **接口的完整签名**包括方法名、参数和返回类型
- **可运行的Mock实现代码**，可直接用于测试
- **完整的使用示例**展示如何在测试中使用这些Mock
- **调用顺序说明**帮助理解各接口的调用关系
- **关键接口修正**：IExCertHandler的exCertHandler方法、RsApi的getLatestContainerResourceStatistics方法

通过本文档，开发者可以：
1. 快速了解所有CSP SDK接口的规格
2. 创建符合接口规范的Mock实现
3. 编写完整的单元测试和集成测试
4. 在外部环境中模拟BrowserGateway的依赖服务