## Why

开发环境需要同时启动多个服务（gids_mock_server、browser-proxy、mobile、browser-gateway），目前没有统一的一键启动脚本，每次手动逐个启动繁琐且容易遗漏配置（如 browser-gateway 的 local profile、Python 虚拟环境激活）。

## What Changes

- 新增一个一键启动脚本，按正确顺序启动所有开发依赖服务
- gids_mock_server 通过 Test/browsergateway-test-client 虚拟环境启动
- browser-proxy 通过 BrowserGateway/BrowserGateway/browser-proxy 虚拟环境启动
- mobile 通过 Maven (`mvn spring-boot:run`) 启动
- browser-gateway 通过 Maven 启动，并指定 `application-local.yaml` 配置（`-Dspring.profiles.active=local`）

## Capabilities

### New Capabilities
- `dev-startup-script`: 一键启动所有开发服务的脚本，涵盖服务启动顺序、虚拟环境激活、Spring profile 配置

### Modified Capabilities

## Impact

- 新增启动脚本文件（`.bat` Windows 批处理或 `.sh` Shell 脚本，或两者皆有）
- 不修改任何现有服务代码
- 依赖各模块现有虚拟环境和 Maven 配置已就绪
