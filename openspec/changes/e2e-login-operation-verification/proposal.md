## Why

BrowserGateway 是核心业务模块，目前缺乏本地端到端验证手段，无法在开发环境中确认登录和操作流程的完整可用性。需要结合 mobile（沐恩终端）、GIDS Mock、browser-proxy、record 扩展插件和本地 Chrome，完成可重复执行的本地端到端验证。

## What Changes

- 将 Muen SDK biz jar（`browser-module-sdk-biz-0.0.22.jar`）加入 BrowserGateway 本地依赖
- 新增 BrowserGateway `local` profile 配置，以静态默认值替代 Redis 中的 `config:chromeConfigList`，禁用 TLS 证书和 K8s 服务发现，支持开发机独立启动
- 补全 GIDS Mock 服务，覆盖 BrowserGateway 在登录流程中回调 GIDS 所需的全部接口
- 验证登录流程：mobile 建立 TCP 控制流 → GIDS 三步鉴权 → BrowserGateway 回调 GIDS Mock 获取实例配置 → browser-proxy 启动带 record 扩展的 Chrome → record 扩展通过 WebSocket 推送录屏到 BrowserGateway → BrowserGateway 通过 TCP 媒体流转发给 mobile
- 验证操作流程：mobile 发送控制指令 → BrowserGateway → SDK → browser-proxy → Chrome 执行，录屏实时更新并经媒体流回传
- 提供可一键执行的端到端测试脚本，输出明确的通过/失败结论

## Capabilities

### New Capabilities

- `local-startup`: BrowserGateway 本地启动能力——引入 `browser-module-sdk-biz-0.0.22.jar`，新增 `application-local.yaml`，以静态配置替代 Redis（`config:chromeConfigList` 默认值：分辨率 240x320、帧率 10fps、码率 1000kbps、采样率 48000Hz、recordMode=1），禁用 TLS 和 K8s 依赖，支持 `spring.profiles.active=local` 独立启动
- `e2e-login-flow`: 端到端登录验证——mobile 建立 TCP 控制流（端口 30001）并完成 GIDS 三步鉴权（gridLoginAuth → gridLoginAuthOpenBrowser → deviceLoginAuth），BrowserGateway 回调 GIDS Mock 获取实例配置，通过 browser-proxy（端口 8000）启动带 record 扩展的 Chrome，record 扩展建立 WebSocket 推送录屏数据到 BrowserGateway，BrowserGateway 向 mobile 推送 TCP 媒体流地址，mobile 建立媒体流（端口 30011）接收录屏
- `e2e-operation-flow`: 端到端操作验证——登录完成后 mobile 通过 TCP 控制流发送方向键/确认/返回等指令，验证 BrowserGateway → SDK → browser-proxy → Chrome 的完整执行链路，以及录屏数据经 record 扩展 WebSocket → BrowserGateway → TCP 媒体流回传 mobile

### Modified Capabilities

（无现有 spec 需变更）

## Impact

- **BrowserGateway**：
  - 新增本地依赖 `browser-module-sdk-biz-0.0.22.jar`（来自 `D:/tmp/muen-build/muen-v0.0.22.zip`）
  - 新增 `application-local.yaml` profile，静态提供 Chrome 配置（替代 Redis `config:chromeConfigList`），禁用证书和服务注册
  - 确认 record 扩展 ID 配置（`record-extension-id: majikpeglnhefidjkmpeipdbikkfbmho`）与本地扩展路径一致
- **GIDS Mock**（`Test/browsergateway-test-client/src/mock/gids_mock_server.py`）：补充 BrowserGateway 回调所需接口（user-bind 绑定查询、plugin 配置下发、stats 流量/会话上报）
- **browser-proxy**：确认本地可正常启动，CDP 接口与 SDK 调用匹配，Chrome 启动时加载 record 扩展
- **record 扩展**（`BrowserGateway/record/`）：WebSocket 推送地址指向本地 BrowserGateway（`/browser/websocket/{imeiAndImsi}`）
- **mobile**：作为终端模拟器，验证 TCP 控制流/媒体流建立及数据收发
- **测试脚本**：`Test/browsergateway-test-client/` 下补充 E2E 测试入口，一键启动全部依赖服务并执行完整验证
