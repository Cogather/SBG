## Context

项目包含四个需要同时运行的服务：
- **gids_mock_server** — Python，位于 `Test/browsergateway-test-client/`，模拟 GIDS HTTP 服务（默认端口 9090）
- **browser-proxy** — Python FastAPI，位于 `BrowserGateway/BrowserGateway/browser-proxy/`，浏览器代理服务
- **mobile** — Java Spring Boot，位于 `mobile/`，云手机控制服务（端口 8088，WebSocket 40002）
- **browser-gateway** — Java Spring Boot，位于 `BrowserGateway/BrowserGateway/browser-gateway/`，主网关服务，需使用 `application-local.yaml` 配置

当前无统一启动脚本，开发者需在多个终端中分别手动启动，容易遗漏步骤。

## Goals / Non-Goals

**Goals:**
- 提供一个脚本，在独立终端窗口中按顺序启动全部四个服务
- Python 服务使用各自目录下的虚拟环境（`.venv`）
- browser-gateway 以 `spring.profiles.active=local` 启动，加载 `application-local.yaml`
- 脚本在 Windows 环境下可用（`.bat`），平台为 win32

**Non-Goals:**
- 跨平台 Shell 脚本（Linux/macOS `.sh`）不在本次范围
- 不修改任何服务代码或配置文件
- 不处理服务健康检查或自动重启
- 不管理虚拟环境的创建（假设已存在）

## Decisions

### 1. 脚本格式：Windows `.bat`

项目运行于 Windows 11，Shell 为 bash（git bash），但启动脚本面向开发者日常使用。选择 `.bat` 文件可直接双击运行，无需额外工具。

- 备选方案：PowerShell `.ps1` — 功能更强，但需要执行策略配置，使用门槛略高
- 备选方案：Python 脚本 — 跨平台，但需要确定用哪个 Python 环境运行

### 2. 启动顺序

依赖关系决定启动顺序：
1. `gids_mock_server` — 最先启动，browser-gateway 和 mobile 登录时依赖它
2. `browser-proxy` — 独立服务，早启动
3. `mobile` — 依赖 GIDS，需在 gids_mock_server 启动后启动
4. `browser-gateway` — 主服务，最后启动

### 3. 每个服务独立窗口

使用 `start "title" cmd /k <command>` 为每个服务开启独立 cmd 窗口，保持日志可见且互不干扰。

### 4. 虚拟环境激活方式

Python 服务使用 `.venv\Scripts\activate.bat` 激活虚拟环境后再执行启动命令，确保依赖隔离。

### 5. browser-gateway local profile

通过 Maven 参数 `-Dspring.profiles.active=local` 指定，Spring Boot 会自动加载 `application-local.yaml`。

## Risks / Trade-offs

- [虚拟环境不存在] → 脚本启动失败并报错；缓解：在脚本开头加检查提示，引导用户先创建虚拟环境
- [端口冲突] → 如端口已被占用，服务启动失败；缓解：文档说明各服务默认端口
- [Maven 未在 PATH 中] → Java 服务无法启动；缓解：脚本注释中说明前置条件
- [application-local.yaml 不存在] → browser-gateway 启动失败；缓解：README 或脚本注释说明需手动创建该文件
