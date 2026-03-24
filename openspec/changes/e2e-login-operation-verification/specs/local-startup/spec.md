## ADDED Requirements

### Requirement: BrowserGateway 支持 local profile 独立启动
BrowserGateway SHALL 在 `spring.profiles.active=local` 时无需 Redis、TLS 证书或 K8s 服务发现即可正常启动，所有生产依赖 Bean 须在 local profile 下被 stub 或静态默认值替代。

#### Scenario: local profile 启动成功
- **WHEN** 以 `spring.profiles.active=local` 启动 BrowserGateway
- **THEN** 应用正常启动，TCP 控制端口（30001）和媒体端口（30011）监听就绪，HTTP 端口（8090）可访问 `/actuator/health`

#### Scenario: Redis 依赖被静态配置替代
- **WHEN** local profile 启动且无 Redis 服务
- **THEN** `config:chromeConfigList` 使用静态默认值（分辨率 240x320、帧率 10fps、码率 1000kbps、采样率 48000Hz mono、recordMode=1），启动不报错

#### Scenario: TLS 依赖被禁用
- **WHEN** local profile 启动且无 TLS 证书文件
- **THEN** TLS TCP 服务器不启动，非 TLS 服务（`enable-http: true`）正常工作，无异常日志

### Requirement: Muen SDK biz jar 正确引入
BrowserGateway pom.xml SHALL 以 `systemPath` 方式引入 `browser-module-sdk-biz-0.0.22.jar`，与已有 api jar 保持一致。

#### Scenario: SDK 初始化成功
- **WHEN** BrowserGateway 启动并触发 SDK 初始化流程
- **THEN** SDK 回调正常执行，日志中无 `ClassNotFoundException` 或 `NoClassDefFoundError`
