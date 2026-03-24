## Context

本次变更涉及多个模块的协同：BrowserGateway（Java/Spring Boot，核心业务）、Muen SDK（`browser-module-sdk-biz-0.0.22.jar`）、browser-proxy（Python/FastAPI，CDP 代理）、record 扩展（Chrome MV3 扩展，录屏）、GIDS Mock（Python，鉴权服务模拟）、mobile（沐恩终端，TCP 客户端）。

当前状态：
- BrowserGateway 仅有生产配置（依赖 Redis、TLS 证书、K8s 服务发现），无法在开发机独立启动
- Muen SDK biz jar（`browser-module-sdk-biz-0.0.22.jar`）未引入项目，导致 SDK 初始化无法运行
- 完整插件包位于 `D:/Code/核心网简版插件版本 - v0.0.22/核心网简版插件版本 - v0.0.22/`，含 jar、Chrome extension（muen-v0.0.22）及 keys
- GIDS Mock 已有基础实现，但缺少 BrowserGateway 回调所需的部分接口
- browser-proxy、record 扩展代码已实现，待与 BrowserGateway 联调

## Goals / Non-Goals

**Goals:**
- BrowserGateway 可在开发机以 `local` profile 独立启动（无 Redis、无 TLS、无 K8s）
- mobile 与 BrowserGateway 完成完整登录流程（TCP 控制流建立 → GIDS 三步鉴权 → browser-proxy 启动 Chrome + record 扩展 → 录屏 WebSocket → TCP 媒体流回传）
- mobile 与 BrowserGateway 完成操作流程（控制指令 → browser-proxy → Chrome → 录屏更新回传）
- 测试可一键启动、结果明确（通过/失败）

**Non-Goals:**
- 不涉及生产环境部署或 K8s 配置变更
- 不修改 Muen SDK 源码
- 不实现完整的 TLS/mTLS 链路验证
- 不覆盖性能压测场景

## Decisions

### 决策 1：SDK biz jar 引入方式

**决策**：将 `browser-module-sdk-biz-0.0.22.jar` 复制到 `BrowserGateway/browser-gateway/src/main/resources/lib/`，以 `systemPath` 方式在 pom.xml 中引入，与已有 api jar 保持一致。

**备选**：安装到本地 Maven 仓库（`mvn install:install-file`）。

**理由**：systemPath 方式无需额外构建步骤，与现有 api jar 引入方式统一，适合本地验证场景。

### 决策 2：local profile 的 Redis 替代方案

**决策**：新增 `application-local.yaml`，将 `config:chromeConfigList` 等 Redis 读取逻辑通过 `@Profile("local")` 的 Bean 以硬编码默认值替代（分辨率 240x320、帧率 10fps、码率 1000kbps、采样率 48000Hz、recordMode=1）。

**备选**：嵌入 Redis（如 embedded-redis）。

**理由**：硬编码默认值最简单，不引入新依赖，本地验证阶段配置固定即可。

### 决策 3：Chrome extension 来源

**决策**：使用 `D:/Code/核心网简版插件版本 - v0.0.22/核心网简版插件版本 - v0.0.22/extension/extension/muen/muen-v0.0.22/` 下的完整扩展包，在 BrowserGateway 配置中指向该路径启动 Chrome。

**理由**：该版本为最新完整插件版本（v0.0.22），触控策略已优化（SimpleTouchStrategy），与 SDK 版本匹配。

### 决策 4：GIDS Mock 补全策略

**决策**：在现有 `gids_mock_server.py` 基础上补充 BrowserGateway 回调所需接口，不新建独立服务。

**接口清单**（需补全）：
- `GET /user-bind/v1/{sessionID}` — 返回用户绑定信息（含 IMEI/IMSI）
- `POST /user-bind/v1/{sessionID}` — 创建/更新绑定
- `GET /plugin/v1/current` — 返回插件配置（含 Chrome 启动参数、扩展路径）
- `POST /stats/v1/traffic/media` / `/stats/v1/traffic/control` — 接收流量上报
- `POST /stats/v1/session` — 接收会话上报

### 决策 5：端到端测试入口

**决策**：扩展现有 `Test/browsergateway-test-client/start_e2e_test.py`，新增登录流程和操作流程的验证用例，依赖 mobile 模块中已有的 WebSocket/TCP 客户端实现。

## Risks / Trade-offs

- **[风险] SDK biz jar 与 BrowserGateway 接口不匹配** → 先通过日志确认 SDK 初始化回调是否正常触发，逐步排查接口签名
- **[风险] record 扩展 WebSocket 地址配置错误导致录屏数据无法到达** → 在 BrowserGateway 媒体 WebSocket 端点加日志确认连接建立
- **[风险] browser-proxy CDP 调用与 SDK 期望的接口不一致** → 对照 SDK 回调日志与 browser-proxy API 文档逐一核对
- **[风险] local profile 遗漏某些 Redis 依赖 Bean 导致启动失败** → 逐步启动，按报错补全 stub Bean
- **[Trade-off] systemPath 引入 jar 不利于 CI 构建** → 本地验证阶段可接受，后续可迁移到私服

## Migration Plan

1. 复制 SDK jar 到 lib 目录，更新 pom.xml
2. 新增 `application-local.yaml`，实现 local profile Bean
3. 补全 GIDS Mock 接口
4. 配置 Chrome 扩展路径（指向完整插件包）
5. 启动顺序：GIDS Mock → browser-proxy → BrowserGateway（local profile）→ mobile 发起连接
6. 执行 E2E 测试脚本，验证登录和操作流程

## Open Questions

- BrowserGateway 启动 Chrome 时传给 browser-proxy 的扩展路径参数格式是否需要绝对路径？
- SDK 初始化回调中的 `chrome启动参数` 具体格式需与 browser-proxy 的 `POST /api/browsers` 接口对齐，需确认字段映射
- mobile 使用哪个具体入口（`websocket_browser_client.py` 还是 Spring Boot 的 `WebsocketServer`）作为 TCP 控制流客户端？
