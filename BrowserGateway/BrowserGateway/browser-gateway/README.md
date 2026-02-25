# BrowserGateway 组件

## 项目简介

BrowserGateway 是云浏览器（SBG）系统的网关层组件，负责用户会话的创建、生命周期管理、数据上报、健康监控、告警推送等核心职责。

## 项目结构

```
browser-gateway/
├── src/main/java/com/huawei/browsergateway/
│   ├── adapter/                    # CSP 适配层
│   │   ├── interfaces/            # 适配器接口
│   │   ├── factory/               # 适配器工厂
│   │   ├── impl/                  # 适配器实现
│   │   │   ├── csp/              # CSP SDK 实现
│   │   │   └── custom/           # 自定义实现
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
│   │   ├── plugin/               # 插件相关实体
│   │   ├── request/              # 请求相关实体
│   │   └── response/              # 响应相关实体
│   ├── exception/                 # 异常处理
│   │   ├── common/               # 通用异常
│   │   └── handler/              # 异常处理器
│   ├── router/                    # 路由模块
│   ├── scheduled/                 # 定时任务
│   └── service/                   # 业务服务层
└── src/main/resources/
    ├── application.yaml           # 主配置
    ├── application-csp.yaml       # 内网配置
    └── application-custom.yaml    # 外网配置
```

## 核心模块

### CSP适配层
- **FrameworkAdapter**: 框架启动适配器
- **AlarmAdapter**: 告警适配器
- **CertificateAdapter**: 证书管理适配器
- **ServiceManagementAdapter**: 服务管理适配器
- **SystemUtilAdapter**: 系统工具适配器
- **ResourceMonitorAdapter**: 资源监控适配器

### 业务模块
- **会话管理**: 浏览器实例的创建、维护、清理
- **插件管理**: 插件的加载、热更新、状态查询
- **媒体转发**: 实时媒体流转发
- **健康监控**: 健康检查、容量上报、告警推送
- **证书管理**: TLS证书动态订阅与更新

## 环境配置

### 内网环境
使用 `application-csp.yaml` 配置，启用CSP SDK实现：
```yaml
adapter:
  provider:
    type: CSP_SDK
```

### 外网环境
使用 `application-custom.yaml` 配置，启用自定义实现：
```yaml
adapter:
  provider:
    type: CUSTOM
```

## 启动方式

```bash
# 内网环境
java -jar browser-gateway.jar --spring.profiles.active=csp

# 外网环境
java -jar browser-gateway.jar --spring.profiles.active=custom
```

## 开发说明

本项目基于组件架构设计文档搭建，各模块按照设计文档中的模块划分组织。详细设计请参考：
- `doc/browserGateway组件架构设计说明书.md`
- `doc/csp适配层设计/CSP-接口适配层架构设计.md`
