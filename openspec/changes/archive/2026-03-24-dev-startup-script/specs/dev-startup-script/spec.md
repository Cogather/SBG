## ADDED Requirements

### Requirement: 一键启动所有开发服务
脚本 SHALL 在独立终端窗口中依次启动 gids_mock_server、browser-proxy、mobile、browser-gateway 四个服务。

#### Scenario: 执行脚本启动所有服务
- **WHEN** 开发者双击或在命令行运行启动脚本
- **THEN** 系统为每个服务打开一个独立的 cmd 窗口，并在各自窗口中执行对应的启动命令

### Requirement: Python 服务使用虚拟环境
gids_mock_server 和 browser-proxy 的启动命令 SHALL 先激活各自目录下的 `.venv` 虚拟环境，再执行服务入口。

#### Scenario: gids_mock_server 使用虚拟环境启动
- **WHEN** 脚本启动 gids_mock_server
- **THEN** 脚本在 `Test/browsergateway-test-client/` 目录下激活 `.venv\Scripts\activate.bat`，然后执行 gids_mock_server 启动命令

#### Scenario: browser-proxy 使用虚拟环境启动
- **WHEN** 脚本启动 browser-proxy
- **THEN** 脚本在 `BrowserGateway/BrowserGateway/browser-proxy/` 目录下激活 `.venv\Scripts\activate.bat`，然后执行 FastAPI 服务启动命令

### Requirement: browser-gateway 使用 local profile
browser-gateway 的启动命令 SHALL 包含 `-Dspring.profiles.active=local` 参数，以加载 `application-local.yaml` 配置。

#### Scenario: browser-gateway 以 local profile 启动
- **WHEN** 脚本启动 browser-gateway
- **THEN** 执行 `mvn spring-boot:run -Dspring-boot.run.profiles=local` 或等效命令，Spring Boot 加载 `application-local.yaml`

### Requirement: 服务按依赖顺序启动
脚本 SHALL 按以下顺序启动服务：gids_mock_server → browser-proxy → mobile → browser-gateway。

#### Scenario: 启动顺序正确
- **WHEN** 脚本顺序执行各 start 命令
- **THEN** gids_mock_server 的启动命令在 mobile 和 browser-gateway 之前被触发，确保依赖服务先行

### Requirement: 脚本位于项目根目录
启动脚本文件 SHALL 放置在项目根目录（`SBG/`），文件名为 `start-dev.bat`。

#### Scenario: 从项目根目录执行脚本
- **WHEN** 开发者在项目根目录运行 `start-dev.bat`
- **THEN** 脚本使用相对路径正确定位各模块目录并启动服务
