# CSP接口适配层架构设计文档

## 文档信息

| 项目 | BrowserGateway |
|------|----------------|
| 文档类型 | CSP接口适配层架构设计 |
| 版本 | 2.0 |
| 日期 | 2026-03-07 |
| 更新说明 | 根据实际代码实现刷新文档，确保100%一致 |

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
│  ┌──────────┬──────────┬──────────┬──────────┬──────────┐  │
│  │ Framework│  Alarm   │ Certificate│ Service │ System  │  │
│  │  Adapter │ Adapter  │   Adapter  │ Adapter │ Adapter │  │
│  └──────────┴──────────┴──────────┴──────────┴──────────┘  │
│  ┌──────────┬──────────┐                                  │
│  │ Resource │   Audit  │                                  │
│  │  Monitor │    Log   │                                  │
│  │  Adapter │  Adapter │                                  │
│  └──────────┴──────────┘                                  │
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
│  ┌──────────┬──────────┬──────────┬──────────┬──────────┐  │
│  │  CSP SDK │  CSP SDK │  CSP SDK  │  CSP SDK │  CSP SDK │  │
│  │  Framework│  Alarm   │ Certificate│ Service │ System  │  │
│  └──────────┴──────────┴──────────┴──────────┴──────────┘  │
│  ┌──────────┬──────────┐                                  │
│  │  CSP SDK │  CSP SDK │                                  │
│  │ Resource │   Audit  │                                  │
│  │  Monitor │    Log   │                                  │
│  └──────────┴──────────┘                                  │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 分层说明

1. **应用层**：业务逻辑层，直接使用适配层接口
2. **适配层**：统一的接口抽象，屏蔽底层实现差异
3. **提供层**：根据环境选择不同的实现提供者
4. **实现层**：具体的实现（CSP SDK或自定义）

---

## 3. 文件结构

```
browser-gateway/src/main/java/com/huawei/browsergateway/
├── adapter/                                # 适配器层
│   ├── FrameworkAdapter.java               # 框架适配器接口
│   ├── AlarmAdapter.java                   # 告警适配器接口
│   ├── CertificateAdapter.java             # 证书适配器接口
│   ├── ServiceManagementAdapter.java       # 服务管理适配器接口
│   ├── SystemUtilAdapter.java              # 系统工具适配器接口
│   ├── ResourceMonitorAdapter.java         # 资源监控适配器接口
│   ├── AuditLogAdapter.java                # 审计日志适配器接口
│   ├── dto/                                # 数据传输对象
│   │   ├── AlarmInfo.java                  # 告警信息
│   │   ├── AlarmRequest.java               # 告警请求
│   │   ├── AuditLogInfo.java               # 审计日志信息
│   │   ├── AuditLevel.java                 # 审计日志级别枚举
│   │   ├── AuditResult.java                # 审计日志结果枚举
│   │   ├── AuditType.java                  # 审计日志类型枚举
│   │   ├── OperateType.java                # 操作类型枚举
│   │   ├── CertEntity.java                 # 证书实体
│   │   ├── CertScene.java                  # 证书场景
│   │   ├── CertType.java                   # 证书类型枚举
│   │   ├── CertUpdateCallback.java         # 证书更新回调接口
│   │   ├── CertNotifyType.java             # 证书通知类型枚举
│   │   ├── ServiceInstance.java            # 服务实例
│   │   └── ResourceStatistics.java         # 资源统计信息
│   ├── factory/                            # 适配器工厂
│   │   ├── AdapterFactory.java             # 适配器工厂接口
│   │   ├── CspAdapterFactory.java          # CSP SDK工厂
│   │   └── CustomAdapterFactory.java       # 自定义工厂
│   ├── impl/                               # 实现类
│   │   ├── csp/                            # CSP SDK实现（内网）
│   │   │   ├── CspFrameworkAdapter.java
│   │   │   ├── CspAlarmAdapter.java
│   │   │   ├── CspCertificateAdapter.java
│   │   │   ├── CspServiceManagementAdapter.java
│   │   │   ├── CspSystemUtilAdapter.java
│   │   │   ├── CspResourceMonitorAdapter.java
│   │   │   └── CspAuditLogAdapter.java
│   │   └── custom/                         # 自定义实现（外网）
│   │       ├── CustomFrameworkAdapter.java
│   │       ├── CustomAlarmAdapter.java
│   │       ├── CustomCertificateAdapter.java
│   │       ├── CustomServiceManagementAdapter.java
│   │       ├── CustomSystemUtilAdapter.java
│   │       ├── CustomResourceMonitorAdapter.java
│   │       └── CustomAuditLogAdapter.java
│   └── config/                             # 配置类
│       └── AdapterConfig.java              # 适配器配置
```


## 4. 适配器清单

### 4.1 核心适配器汇总

| 序号 | 适配器接口 | CSP SDK实现 | 自定义实现 | 主要职责 |
|------|-----------|------------|-----------|---------|
| 1 | FrameworkAdapter | CspFrameworkAdapter | CustomFrameworkAdapter | CSE框架和OM SDK的生命周期管理 |
| 2 | AlarmAdapter | CspAlarmAdapter | CustomAlarmAdapter | 告警的发送、清除和历史查询 |
| 3 | CertificateAdapter | CspCertificateAdapter | CustomCertificateAdapter | 证书的订阅、更新和获取 |
| 4 | ServiceManagementAdapter | CspServiceManagementAdapter | CustomServiceManagementAdapter | 服务注册、发现、属性上报 |
| 5 | SystemUtilAdapter | CspSystemUtilAdapter | CustomSystemUtilAdapter | 环境变量读取、本地配置管理 |
| 6 | ResourceMonitorAdapter | CspResourceMonitorAdapter | CustomResourceMonitorAdapter | CPU、内存等系统资源监控 |
| 7 | AuditLogAdapter | CspAuditLogAdapter | CustomAuditLogAdapter | 审计日志的写入 |

### 4.2 适配器功能详解

#### 4.2.1 FrameworkAdapter - 框架适配器

**功能说明**:
- 管理CSE框架的启动和停止
- 初始化OM SDK
- 查询框架运行状态

**使用场景**:
- 应用启动时初始化框架
- 应用关闭时清理资源
- 健康检查时查询框架状态

**依赖的CSP SDK**:
- `com.huawei.csp.csejsdk.core.api.Framework`
- `com.huawei.csp.om.transport.vertx.init.OmsdkStarter`

#### 4.2.2 AlarmAdapter - 告警适配器

**功能说明**:
- 发送告警信息
- 清除告警
- 批量发送告警(支持重试)
- 查询历史告警

**使用场景**:
- 系统异常时发送告警
- 故障恢复后清除告警
- 定期健康检查告警
- 告警历史查询和分析

**依赖的CSP SDK**:
- `com.huawei.csp.om.alarmsdk.alarmmanager.AlarmSendManager`
- `com.huawei.csp.om.alarmsdk.alarmmanager.Alarm`
- `com.huawei.csp.jsf.api.CspRestTemplateBuilder`

**告警类型**:
- `GENERATE`: 生成告警
- `CLEAR`: 清除告警

#### 4.2.3 CertificateAdapter - 证书适配器

**功能说明**:
- 订阅证书变更
- 获取CA证书
- 获取设备证书
- 获取私钥(PKCS#8格式)
- 检查证书就绪状态
- 初始化证书SDK

**使用场景**:
- HTTPS服务证书配置
- 双向认证
- 证书自动更新
- 证书到期提醒

**依赖的CSP SDK**:
- `com.huawei.csp.certsdk.certapiImpl.CertMgrApi`
- `com.huawei.csp.certsdk.certapiImpl.ExCertMgrApi`
- `com.huawei.csp.certsdk.handler.IExCertHandler`

**证书场景**:
- `CA`: CA证书
- `DEVICE`: 设备证书

#### 4.2.4 ServiceManagementAdapter - 服务管理适配器

**功能说明**:
- 上报服务实例属性
- 获取服务实例属性
- 查找服务实例
- 获取当前服务实例
- 注册REST服务

**使用场景**:
- 服务注册
- 服务发现
- 负载均衡
- 服务治理

**依赖的CSP SDK**:
- `com.huawei.csp.csejsdk.common.utils.ServiceUtils`
- `org.apache.servicecomb.serviceregistry.RegistryUtils`
- `org.apache.servicecomb.provider.rest.common.RestSchema`

#### 4.2.5 SystemUtilAdapter - 系统工具适配器

**功能说明**:
- 从环境变量获取字符串值
- 从环境变量获取整数值
- 设置环境变量(用于测试)

**使用场景**:
- 读取配置信息
- 获取部署环境参数
- 单元测试中Mock环境变量

**依赖的CSP SDK**:
- `com.huawei.csp.csejsdk.common.utils.SystemUtil`

#### 4.2.6 ResourceMonitorAdapter - 资源监控适配器

**功能说明**:
- 获取CPU使用率
- 获取内存使用率
- 获取网络带宽使用率
- 获取资源统计信息

**使用场景**:
- 系统健康监控
- 资源告警
- 容量规划
- 性能分析

**依赖的CSP SDK**:
- `com.huawei.csp.csejsdk.rssdk.api.RsApi`

#### 4.2.7 AuditLogAdapter - 审计日志适配器

**功能说明**:
- 写入审计日志

**使用场景**:
- 操作审计
- 安全审计
- 合规性检查
- 事件追溯

**依赖的CSP SDK**:
- `com.huawei.csp.jsf.api.CspRestTemplateBuilder`
- 审计日志REST服务

**日志类型**:
- `OPERATION`: 操作日志
- `SECURITY`: 安全日志

### 4.3 DTO类清单

#### 4.3.1 证书相关DTO

| 类名 | 说明 | 主要字段 |
|------|------|---------|
| CertScene | 证书场景 | sceneName, sceneType, feature |
| CertType | 证书类型枚举 | CA, DEVICE, CRL等 |
| CertEntity | 证书实体 | 包含完整的证书信息 |
| CertUpdateCallback | 证书更新回调接口 | onCertificateUpdate方法 |
| CertNotifyType | 证书通知类型枚举 | UPDATE, CONSISTENCY, QUERY, UNBIND |

#### 4.3.2 服务相关DTO

| 类名 | 说明 | 主要字段 |
|------|------|---------|
| ServiceInstance | 服务实例 | instanceId, serviceName, host, port, status |
| ResourceStatistics | 资源统计信息 | success, ratio, timestamp, available, capacity |

#### 4.3.3 告警相关DTO

| 类名 | 说明 | 主要字段 |
|------|------|---------|
| AlarmRequest | 告警请求 | alarmId, type, parameters |
| AlarmInfo | 告警信息 | alarmId, message, source, timestamp |

#### 4.3.4 审计日志相关DTO

| 类名 | 说明 | 主要字段 |
|------|------|---------|
| AuditLogInfo | 审计日志信息 | operation, level, userName, result, detail |
| AuditLevel | 审计日志级别枚举 | WARNING, MINOR, RISK, AUTOQUERY, QUERY |
| AuditResult | 审计日志结果枚举 | SUCCESSFUL, FAILURE, PARTIAL_SUCCESS |
| AuditType | 审计日志类型枚举 | OPERATION, SECURITY |
| OperateType | 操作类型枚举 | GET, ADD, MOD, DELETE, DOWNLOAD, UPLOAD, UPHOLD |

---

## 5. 核心接口定义

### 5.1 框架启动接口

**文件路径**：`com.huawei.browsergateway.adapter.FrameworkAdapter`

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

### 5.2 告警接口

**文件路径**：`com.huawei.browsergateway.adapter.AlarmAdapter`

```java
package com.huawei.browsergateway.adapter;

import com.huawei.browsergateway.adapter.dto.AlarmInfo;
import com.huawei.browsergateway.adapter.dto.AlarmRequest;

import java.util.List;
import java.util.Map;

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

### 5.3 证书管理接口

**文件路径**：`com.huawei.browsergateway.adapter.CertificateAdapter`

```java
package com.huawei.browsergateway.adapter;

import com.huawei.browsergateway.adapter.dto.CertScene;
import com.huawei.browsergateway.adapter.dto.CertUpdateCallback;

import java.io.InputStream;
import java.util.List;

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
     * 获取CA证书输入流
     * @return CA证书输入流
     */
    InputStream getCaCertificateStream();

    /**
     * 获取设备证书输入流
     * @return 设备证书输入流
     */
    InputStream getDeviceCertificateStream();

    /**
     * 获取私钥输入流（已转换为PKCS#8格式）
     * @return 私钥输入流
     */
    InputStream getPrivateKeyStream();

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

### 5.4 审计日志接口

**文件路径**：`com.huawei.browsergateway.adapter.AuditLogAdapter`

```java
package com.huawei.browsergateway.adapter;

import com.huawei.browsergateway.adapter.dto.AuditLogInfo;

/**
 * 审计日志适配器接口
 * 职责：处理审计日志的写入
 */
public interface AuditLogAdapter {

    /**
     * 写入审计日志
     * @param auditLogInfo 审计日志信息
     * @return 写入是否成功
     */
    boolean writeAuditLog(AuditLogInfo auditLogInfo);
}
```

### 5.5 资源监控接口

**文件路径**：`com.huawei.browsergateway.adapter.ResourceMonitorAdapter`

```java
package com.huawei.browsergateway.adapter;

import com.huawei.browsergateway.adapter.dto.ResourceStatistics;

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

### 5.6 服务管理接口

**文件路径**：`com.huawei.browsergateway.adapter.ServiceManagementAdapter`

```java
package com.huawei.browsergateway.adapter;

import com.huawei.browsergateway.adapter.dto.ServiceInstance;

import java.util.List;
import java.util.Map;

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

### 5.7 系统工具接口

**文件路径**：`com.huawei.browsergateway.adapter.SystemUtilAdapter`

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

---

## 6.数据传输对象（DTO）

### 6.1 证书相关DTO

#### 7.1.1 CertScene - 证书场景

**文件路径**：`com.huawei.browsergateway.adapter.dto.CertScene`

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
```

#### 6.1.2 CertType - 证书类型枚举

**文件路径**：`com.huawei.browsergateway.adapter.dto.CertType`

```java
package com.huawei.browsergateway.adapter.dto;

/**
 * 证书类型枚举
 * 用于标识证书的类型
 * 与CSP的证书类型定义保持一致
 */
public enum CertType {
    /**
     * CA证书
     */
    CERT_TYPE_CA(0),

    /**
     * 设备证书
     */
    CERT_TYPE_DEVICE(1),

    /**
     * CRL证书吊销列表
     */
    CERT_TYPE_CRL(2),

    /**
     * CA证书和设备证书组合
     */
    CERT_TYPE_CA_DEVICE(3),

    /**
     * CA证书和CRL组合
     */
    CERT_TYPE_CA_CRL(4),

    /**
     * CA证书、设备证书和CRL组合
     */
    CERT_TYPE_CA_DEVICE_CRL(5),

    /**
     * 空类型
     */
    CERT_TYPE_EMPTY(6);

    private final int type;

    CertType(int type) {
        this.type = type;
    }

    public int getType() {
        return this.type;
    }

    /**
     * 根据类型值获取枚举
     * @param type 类型值
     * @return 对应的枚举类型
     */
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

#### 6.1.3 CertEntity - 证书实体

**文件路径**：`com.huawei.browsergateway.adapter.dto.CertEntity`

```java
package com.huawei.browsergateway.adapter.dto;

import java.util.Arrays;

/**
 * 证书实体类
 * 与CSP的ExCertEntity定义完全保持一致
 * 用于封装证书相关信息
 */
public class CertEntity {
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
    
    public byte[] getPrivateKeyPassword() {
        return this.privateKeyPassword == null ? new byte[0] : Arrays.copyOf(this.privateKeyPassword, this.privateKeyPassword.length);
    }
}
```

#### 6.1.4 CertUpdateCallback - 证书更新回调接口

**文件路径**：`com.huawei.browsergateway.adapter.dto.CertUpdateCallback`

```java
package com.huawei.browsergateway.adapter.dto;

import java.util.List;

/**
 * 证书更新回调接口
 * 使用adapter层的DTO对象,实现与CSP接口的解耦
 */
public interface CertUpdateCallback {
    /**
     * 证书更新时的回调
     * @param certEntities 证书实体列表
     * @param certNotifyType 证书通知类型
     */
    void onCertificateUpdate(List<CertEntity> certEntities, CertNotifyType certNotifyType);
}
```

#### 6.1.5 CertNotifyType - 证书通知类型枚举

**文件路径**：`com.huawei.browsergateway.adapter.dto.CertNotifyType`

```java
package com.huawei.browsergateway.adapter.dto;

/**
 * 证书通知类型枚举
 * 用于标识证书变更的通知类型
 */
public enum CertNotifyType {
    /**
     * 证书更新
     */
    CERT_NOTIFY_TYPE_UPDATE(0),

    /**
     * 证书一致性检查
     */
    CERT_NOTIFY_TYPE_CONSISTENCY(1),

    /**
     * 证书查询
     */
    CERT_NOTIFY_TYPE_QUERY(2),

    /**
     * 证书解绑
     */
    CERT_NOTIFY_TYPE_UNBIND(3);

    private final int state;

    CertNotifyType(int state) {
        this.state = state;
    }

    public int getState() {
        return this.state;
    }

    /**
     * 根据状态值获取枚举
     * @param state 状态值
     * @return 对应的枚举类型
     */
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

### 7.2 服务相关DTO

#### 7.2.1 ServiceInstance - 服务实例

**文件路径**：`com.huawei.browsergateway.adapter.dto.ServiceInstance`

```java
package com.huawei.browsergateway.adapter.dto;

import lombok.Data;

import java.util.Map;

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
```

#### 7.2.2 ResourceStatistics - 资源统计信息

**文件路径**：`com.huawei.browsergateway.adapter.dto.ResourceStatistics`

```java
package com.huawei.browsergateway.adapter.dto;

import lombok.Data;

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

### 6.3 告警相关DTO

#### 6.3.1 AlarmRequest - 告警请求

**文件路径**：`com.huawei.browsergateway.adapter.dto.AlarmRequest`

```java
package com.huawei.browsergateway.adapter.dto;

import lombok.Data;

import java.util.Map;

/**
 * 告警请求
 */
@Data
public class AlarmRequest {
    private String alarmId;
    private com.huawei.browsergateway.adapter.AlarmAdapter.AlarmType type;
    private Map<String, String> parameters;
    private Integer maxRetry;
}
```

#### 6.3.2 AlarmInfo - 告警信息

**文件路径**：`com.huawei.browsergateway.adapter.dto.AlarmInfo`

```java
package com.huawei.browsergateway.adapter.dto;

import lombok.Data;

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

### 5.4 审计日志相关DTO

#### 5.4.1 AuditLogInfo - 审计日志信息

**文件路径**：`com.huawei.browsergateway.adapter.dto.AuditLogInfo`

```java
package com.huawei.browsergateway.adapter.dto;

import lombok.Data;

/**
 * 审计日志信息
 */
@Data
public class AuditLogInfo {
    /**
     * 操作名称
     */
    private String operation;

    /**
     * 日志级别。可以是如下值之一：
     * WARNING：提示
     * MINOR：一般
     * RISK：危险
     */
    private String level;

    /**
     * 操作用户
     */
    private String userName;

    /**
     * 时间戳
     */
    private String dateTime;

    /**
     * 操作来源
     */
    private String appName;

    /**
     * 发起操作的客户端IP地址，可从HTTP Header中获取
     */
    private String terminal;

    /**
     * 操作对象
     */
    private String serviceName;

    /**
     * 操作结果。可以是如下值之一：
     * SUCCESSFUL：成功
     * FAILURE：失败
     * PARTIAL_SUCCESS：部分成功
     */
    private String result;

    /**
     * 详细信息，最多支持800个字符
     */
    private String detail;

    /**
     * 详细信息(中文)，最多支持800个字符
     */
    private String detailZh;

    /**
     * 操作类型（仅操作日志需要）
     */
    private String operateType;

    /**
     * 审计日志类型
     */
    private String auditType;
}
```

#### 5.4.2 AuditLevel - 审计日志级别枚举

**文件路径**：`com.huawei.browsergateway.adapter.dto.AuditLevel`

```java
package com.huawei.browsergateway.adapter.dto;

/**
 * 审计日志级别枚举
 */
public enum AuditLevel {
    /**
     * 提示
     */
    WARNING,

    /**
     * 一般
     */
    MINOR,

    /**
     * 危险
     */
    RISK,

    /**
     * 自动查询
     */
    AUTOQUERY,

    /**
     * 查询
     */
    QUERY
}
```

#### 5.4.3 AuditResult - 审计日志结果枚举

**文件路径**：`com.huawei.browsergateway.adapter.dto.AuditResult`

```java
package com.huawei.browsergateway.adapter.dto;

/**
 * 审计日志结果枚举
 */
public enum AuditResult {
    /**
     * 成功
     */
    SUCCESSFUL,

    /**
     * 失败
     */
    FAILURE,

    /**
     * 部分成功
     */
    PARTIAL_SUCCESS
}
```

#### 5.4.4 AuditType - 审计日志类型枚举

**文件路径**：`com.huawei.browsergateway.adapter.dto.AuditType`

```java
package com.huawei.browsergateway.adapter.dto;

/**
 * 审计日志类型枚举
 */
public enum AuditType {
    /**
     * 操作日志
     */
    OPERATION,

    /**
     * 安全日志
     */
    SECURITY
}
```

#### 5.4.5 OperateType - 操作类型枚举

**文件路径**：`com.huawei.browsergateway.adapter.dto.OperateType`

```java
package com.huawei.browsergateway.adapter.dto;

/**
 * 操作类型枚举
 */
public enum OperateType {
    /**
     * 获取
     */
    GET,

    /**
     * 添加
     */
    ADD,

    /**
     * 修改
     */
    MOD,

    /**
     * 删除
     */
    DELETE,

    /**
     * 下载
     */
    DOWNLOAD,

    /**
     * 上传
     */
    UPLOAD,

    /**
     * 维护
     */
    UPHOLD
}
```

---

## 6. 适配器工厂设计

### 7.1 适配器工厂接口

**文件路径**：`com.huawei.browsergateway.adapter.factory.AdapterFactory`

```java
package com.huawei.browsergateway.adapter.factory;

import com.huawei.browsergateway.adapter.*;

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

    /**
     * 创建审计日志适配器
     * @return 审计日志适配器实例
     */
    AuditLogAdapter createAuditLogAdapter();
}
```

### 7.2 CSP SDK适配器工厂

**文件路径**：`com.huawei.browsergateway.adapter.factory.CspAdapterFactory`

```java
package com.huawei.browsergateway.adapter.factory;

import com.huawei.browsergateway.adapter.*;
import com.huawei.browsergateway.adapter.impl.csp.*;

/**
 * CSP SDK适配器工厂
 * 职责：创建使用CSP SDK的适配器实例（内网环境）
 */
public class CspAdapterFactory implements AdapterFactory {

    @Override
    public FrameworkAdapter createFrameworkAdapter() {
        return new CspFrameworkAdapter();
    }

    @Override
    public AlarmAdapter createAlarmAdapter() {
        return new CspAlarmAdapter();
    }

    @Override
    public CertificateAdapter createCertificateAdapter() {
        return new CspCertificateAdapter();
    }

    @Override
    public ServiceManagementAdapter createServiceManagementAdapter() {
        return new CspServiceManagementAdapter();
    }

    @Override
    public SystemUtilAdapter createSystemUtilAdapter() {
        return new CspSystemUtilAdapter();
    }

    @Override
    public ResourceMonitorAdapter createResourceMonitorAdapter() {
        return new CspResourceMonitorAdapter();
    }

    @Override
    public AuditLogAdapter createAuditLogAdapter() {
        return new CspAuditLogAdapter();
    }
}
```

### 6.3 自定义适配器工厂

**文件路径**：`com.huawei.browsergateway.adapter.factory.CustomAdapterFactory`

```java
package com.huawei.browsergateway.adapter.factory;

import com.huawei.browsergateway.adapter.*;
import com.huawei.browsergateway.adapter.impl.custom.*;

/**
 * 自定义适配器工厂
 * 职责：创建自定义实现的适配器实例（外网环境）
 */
public class CustomAdapterFactory implements AdapterFactory {

    @Override
    public FrameworkAdapter createFrameworkAdapter() {
        return new CustomFrameworkAdapter();
    }

    @Override
    public AlarmAdapter createAlarmAdapter() {
        return new CustomAlarmAdapter();
    }

    @Override
    public CertificateAdapter createCertificateAdapter() {
        return new CustomCertificateAdapter();
    }

    @Override
    public ServiceManagementAdapter createServiceManagementAdapter() {
        return new CustomServiceManagementAdapter();
    }

    @Override
    public SystemUtilAdapter createSystemUtilAdapter() {
        return new CustomSystemUtilAdapter();
    }

    @Override
    public ResourceMonitorAdapter createResourceMonitorAdapter() {
        return new CustomResourceMonitorAdapter();
    }

    @Override
    public AuditLogAdapter createAuditLogAdapter() {
        return new CustomAuditLogAdapter();
    }
}
```

---

## 7. 适配器配置设计

### 7.1 适配器配置类

**文件路径**：`com.huawei.browsergateway.adapter.config.AdapterConfig`

```java
package com.huawei.browsergateway.adapter.config;

import com.huawei.browsergateway.adapter.*;
import com.huawei.browsergateway.adapter.factory.AdapterFactory;
import com.huawei.browsergateway.adapter.factory.CspAdapterFactory;
import com.huawei.browsergateway.adapter.factory.CustomAdapterFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 适配器配置类
 * 根据环境配置自动选择合适的适配器工厂
 */
@Configuration
public class AdapterConfig {
    private static final Logger log = LogManager.getLogger(AdapterConfig.class);

    /**
     * 环境配置：internal(内网) 或 external(外网)
     * 默认为内网环境
     */
    @Value("${csp.adapter.environment:internal}")
    private String environment;

    @Bean
    public AdapterFactory adapterFactory() {
        log.info("Initializing adapter factory for environment: {}", environment);

        if ("external".equalsIgnoreCase(environment)) {
            log.info("Using CustomAdapterFactory for external environment");
            return new CustomAdapterFactory();
        } else {
            log.info("Using CspAdapterFactory for internal environment");
            return new CspAdapterFactory();
        }
    }

    @Bean
    public FrameworkAdapter frameworkAdapter(AdapterFactory factory) {
        return factory.createFrameworkAdapter();
    }

    @Bean
    public AlarmAdapter alarmAdapter(AdapterFactory factory) {
        return factory.createAlarmAdapter();
    }

    @Bean
    public CertificateAdapter certificateAdapter(AdapterFactory factory) {
        return factory.createCertificateAdapter();
    }

    @Bean
    public ServiceManagementAdapter serviceManagementAdapter(AdapterFactory factory) {
        return factory.createServiceManagementAdapter();
    }

    @Bean
    public SystemUtilAdapter systemUtilAdapter(AdapterFactory factory) {
        return factory.createSystemUtilAdapter();
    }

    @Bean
    public ResourceMonitorAdapter resourceMonitorAdapter(AdapterFactory factory) {
        return factory.createResourceMonitorAdapter();
    }

    @Bean
    public AuditLogAdapter auditLogAdapter(AdapterFactory factory) {
        return factory.createAuditLogAdapter();
    }

    @Bean
    public com.huawei.browsergateway.util.DeployUtil deployUtil(SystemUtilAdapter systemUtilAdapter) {
        return new com.huawei.browsergateway.util.DeployUtil(systemUtilAdapter);
    }

    /**
     * 获取适配器环境配置
     * 用于非Spring容器环境（如main方法）
     */
    public static String getAdapterEnvironment() {
        // 从系统属性或环境变量中获取，默认为internal
        String env = System.getProperty("csp.adapter.environment");
        if (env == null) {
            env = System.getenv("CSP_ADAPTER_ENVIRONMENT");
        }
        return env != null ? env : "internal";
    }

    /**
     * 根据环境获取适配器工厂
     * 用于非Spring容器环境（如main方法）
     */
    public static AdapterFactory getAdapterFactory(String environment) {
        if ("external".equalsIgnoreCase(environment)) {
            log.info("Using CustomAdapterFactory for external environment");
            return new CustomAdapterFactory();
        } else {
            log.info("Using CspAdapterFactory for internal environment");
            return new CspAdapterFactory();
        }
    }
}
```

### 7.2 Spring Boot配置

```yaml
# application.yaml

# 适配器环境配置
csp:
  adapter:
    environment: internal  # internal(内网) 或 external(外网)，默认为internal

# 自定义实现配置（仅在外网环境使用）
adapter:
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

## 9. 配置说明

### 9.1 环境配置

#### 9.1.1 配置文件方式

在 `application.yaml` 或 `application.properties` 中配置:

```yaml
# application.yaml
csp:
  adapter:
    environment: internal  # internal(内网) 或 external(外网)，默认为internal
```

```properties
# application.properties
csp.adapter.environment=internal
```

#### 9.1.2 环境变量方式

```bash
# Linux/Mac
export CSP_ADAPTER_ENVIRONMENT=external

# Windows
set CSP_ADAPTER_ENVIRONMENT=external
```

#### 9.1.3 JVM参数方式

```bash
java -Dcsp.adapter.environment=external -jar browser-gateway.jar
```

#### 9.1.4 配置优先级

配置优先级从高到低:
1. JVM参数 (`-Dcsp.adapter.environment`)
2. 环境变量 (`CSP_ADAPTER_ENVIRONMENT`)
3. 配置文件 (`application.yaml` 或 `application.properties`)
4. 默认值 (`internal`)

### 9.2 内网环境配置 (internal)

内网环境使用CSP SDK实现,需要配置CSP SDK相关的参数:

```yaml
# application.yaml

# 适配器环境
csp:
  adapter:
    environment: internal

# CSP框架配置
cse:
  service:
    registry:
      address: https://cse.cn-north-1.myhuaweicloud.com
    name: browser-gateway
    version: 1.0.0
  rest:
    address: 0.0.0.0:8090
  handler:
    chain:
      Provider:
        default: qps-flowcontrol-provider,bizkeeper-provider

# OM SDK配置
om:
  transport:
    vertx:
      enabled: true

# 证书SDK配置
cert:
  sdk:
    enabled: true
    cert-path: /opt/csp/browsergw/cert
```

**内网环境依赖**:
- 需要连接华为云CSE服务
- 需要连接OM服务
- 需要连接证书服务
- 需要连接告警服务
- 需要连接审计日志服务

### 9.3 外网环境配置 (external)

外网环境使用自定义实现,需要配置自定义实现相关的参数:

```yaml
# application.yaml

# 适配器环境
csp:
  adapter:
    environment: external

# 自定义实现配置
adapter:
  custom:
    # 证书配置
    certificate:
      enabled: true
      ca-path: /opt/browsergw/cert/ca.crt
      cert-path: /opt/browsergw/cert/device.crt
      key-path: /opt/browsergw/cert/device.key
      auto-reload: true
      check-interval: 300  # 证书检查间隔(秒)

    # 告警配置
    alarm:
      enabled: true
      log-path: /tmp/browsergw_alarms.log
      max-log-size: 100MB
      retention-days: 30
      notification:
        enabled: false
        email: admin@example.com

    # 资源监控配置
    resource-monitor:
      enabled: true
      cpu-threshold: 80.0  # CPU使用率告警阈值(%)
      memory-threshold: 85.0  # 内存使用率告警阈值(%)
      network-threshold: 90.0  # 网络使用率告警阈值(%)
      check-interval: 60  # 检查间隔(秒)

    # 审计日志配置
    audit-log:
      enabled: true
      log-path: /tmp/browsergw_audit.log
      max-log-size: 200MB
      retention-days: 90

    # 服务发现配置
    service:
      enabled: true
      mock-instances: true
      instances:
        - name: chrome-service
          host: 192.168.1.100
          port: 8080
        - name: chrome-service
          host: 192.168.1.101
          port: 8080

    # 系统工具配置
    system-util:
      enabled: true
      mock-env-vars: false
      env-vars:
        SERVICENAME: browser-gateway
        PODNAME: browser-gateway-pod-1
        NAMESPACE: production
```

**外网环境特点**:
- 不依赖华为云CSE服务
- 使用本地证书文件
- 告警记录到本地日志文件
- 使用本地资源监控
- 审计日志记录到本地文件
- 使用Mock服务实例或静态配置


---


### 9.4 多环境部署

#### 9.4.1 内网部署

```bash
# 启动应用(内网环境)
java -jar browser-gateway.jar --csp.adapter.environment=internal
```

**部署要求**:
- 能够访问华为云CSE服务
- 能够访问OM服务
- 能够访问证书服务
- 能够访问告警服务
- 能够访问审计日志服务

#### 9.4.2 外网部署

```bash
# 启动应用(外网环境)
java -jar browser-gateway.jar --csp.adapter.environment=external
```

**部署要求**:
- 准备好证书文件(CA证书、设备证书、私钥)
- 配置好本地日志目录
- 配置好服务实例信息(如果使用Mock实例)


---

## 11. 总结与建议

### 11.1 架构优势

1. **完全解耦**：业务代码完全不依赖CSP SDK
2. **灵活切换**：通过配置即可在不同实现间切换
3. **易于测试**：可以轻松Mock各个适配器
4. **支持多环境**：内网使用CSP SDK，外网使用自定义实现
5. **代码质量**：符合SOLID原则，易于维护和扩展

### 11.2 核心特性

1. **7个核心接口**：覆盖框架、告警、证书、服务管理、系统工具、资源监控、审计日志
2. **双实现支持**：每个接口都有CSP SDK实现和自定义实现
3. **14个DTO类**：封装所有业务数据结构
4. **工厂模式管理**：通过工厂类统一管理适配器的创建
5. **配置驱动**：通过配置文件控制适配器类型的选择
6. **Spring集成**：与Spring容器深度集成，支持依赖注入

### 11.3 注意事项

1. **确保接口一致性**：自定义实现的行为应与CSP SDK实现保持一致
2. **日志完整性**：确保关键操作都有日志记录
3. **异常处理**：妥善处理所有可能的异常情况
4. **兼容性测试**：在迁移前进行充分的测试
5. **回滚方案**：准备快速回滚到原实现的方案

### 11.4 最佳实践

1. **统一使用适配器接口**：业务代码只依赖适配器接口,不依赖具体实现
2. **配置切换环境**：通过配置文件切换环境,避免修改代码
3. **充分测试**：在内网和外网环境都要进行充分测试
4. **监控适配器状态**：通过健康检查接口监控适配器运行状态
5. **日志记录**：记录适配器的创建、初始化、调用等关键操作