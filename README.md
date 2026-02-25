# BrowserGateway

BrowserGateway 是云浏览器（SBG）系统的网关层服务，负责用户会话的创建、生命周期管理、数据上报、健康监控、告警推送等核心职责。

## 项目结构

```
browser-gateway/
├── src/
│   ├── main/
│   │   ├── java/com/huawei/browsergateway/
│   │   │   ├── BrowserGatewayApplication.java  # 主应用类
│   │   │   ├── api/                            # REST API 接口
│   │   │   ├── common/                         # 常量定义
│   │   │   ├── config/                         # 配置类
│   │   │   ├── entity/                         # 实体类
│   │   │   ├── exception/                      # 异常处理
│   │   │   ├── scheduled/                      # 定时任务
│   │   │   ├── service/                        # 业务服务
│   │   │   ├── tcpserver/                      # TCP 服务器
│   │   │   ├── util/                           # 工具类
│   │   │   └── websocket/                     # WebSocket 服务
│   │   └── resources/
│   │       ├── application.yaml                # 主配置文件
│   │       └── microservice.yaml               # ServiceComb 配置
│   └── test/
│       └── java/                                # 测试代码
├── pom.xml                                      # Maven 配置
└── README.md                                    # 项目说明
```

## 功能特性

- ✅ REST API 接口（浏览器管理、扩展管理）
- ✅ TCP TLV 服务器（控制流、媒体流）
- ✅ WebSocket 服务（媒体流转发、插件代理）
- ✅ 浏览器实例生命周期管理
- ✅ 用户数据压缩/脱敏/上传下载
- ✅ 插件动态加载与热更新
- ✅ 健康检查与告警推送
- ✅ 服务注册与发现（CSE）
- ✅ 证书动态订阅与管理

## 环境要求

- JDK 12+
- Maven 3.6+
- Chrome/Chromium 80+

## 快速开始

### 1. 编译项目

```bash
mvn clean package
```

### 2. 运行项目

```bash
java -jar target/browser-gateway.jar
```

## 主要接口

### REST API

- `POST /browsergw/browser/preOpen` - 预打开浏览器实例
- `DELETE /browsergw/browser/userdata/delete` - 删除用户数据
- `POST /browsergw/extension/load` - 加载扩展
- `GET /browsergw/extension/pluginInfo` - 获取插件信息

### WebSocket

- `ws://{host}:{port}/browser/websocket/{imeiAndImsi}` - 媒体流转发
- `ws://{host}:{port}/control/websocket/{imeiAndImsi}` - 插件代理

### TCP TLV

- 控制流：`{host}:{control-tls-port}`
- 媒体流：`{host}:{media-tls-port}`

## 文档

- [软件实现设计文档](BrowserGateway/BrowserGateway/browser-gateway/doc/browserGateway组件架构设计说明书.md)

## 许可证

Copyright © Huawei Technologies Co., Ltd.
