## Context

`mobile` 服务通过 WebSocket（端口 40002）接收客户端连接，使用自定义 TLV 二进制协议与云手机设备交互。测试客户端目录 `Test/browsergateway-test-client` 已有：
- TLV 编解码库（`src/protocol/tlv.py`）
- GIDS Mock Server（`src/mock/gids_mock_server.py`）
- 基础配置（`config/config.yaml`）

当前缺少 WebSocket 测试客户端封装和 pytest 测试用例，所有验证依赖人工操作前端 UI。

## Goals / Non-Goals

**Goals:**
- 在现有 test-client 中新增 pytest 自动化测试套件
- 复用已有 TLV 编解码和 GIDS Mock，不重复造轮子
- 覆盖 case.md 中可自动化的功能性黑盒场景：登录、心跳、按键、触屏、媒体流接收、登出
- 测试可在本地一键运行，依赖最小（只需 mobile 服务 + GIDS Mock）

**Non-Goals:**
- 不测试 browser-gateway 或 browser-proxy
- 不实现 H.264 视频解码验证（只验证帧格式和字节头）
- 不覆盖 case.md 中需要真实设备或三方应用的场景（CNN、Facebook 等）
- 不集成 CI/CD 流水线（该工作独立）

## Decisions

### 1. 测试框架：pytest + pytest-asyncio

**选择**：pytest 作为测试框架，pytest-asyncio 支持异步测试。

**理由**：项目已是 Python 栈；pytest 生态成熟，与现有 FastAPI/uvicorn 兼容；pytest-asyncio 天然支持 async/await，适合 WebSocket 长连接场景。

**备选**：unittest（标准库，但异步支持繁琐）；直接脚本（无断言框架，可维护性差）。

### 2. WebSocket 客户端：websockets 库

**选择**：使用 `websockets` 库封装 `MobileTestClient`，提供 `connect()` / `send_tlv()` / `recv_tlv()` / `disconnect()` 方法。

**理由**：纯 Python、异步原生、与 pytest-asyncio 集成简单。现有 config.yaml 已有 WebSocket 端口配置。

**备选**：`aiohttp`（更重）；`httpx` WebSocket（实验性）。

### 3. 测试隔离：每个测试用例独立连接

**选择**：通过 pytest fixture（scope=function）为每个测试用例创建新的 WebSocket 连接，测试结束后断开。

**理由**：避免测试间状态污染；mobile 服务按 IMEI/IMSI 管理会话，重用连接会导致登录状态混乱。

### 4. GIDS Mock 启动方式：conftest.py session-scoped fixture

**选择**：在 `conftest.py` 中用 session-scoped fixture 启动 GIDS Mock Server 子进程，所有测试共享同一 Mock 实例。

**理由**：GIDS Mock 是无状态的（或测试间可重置），启动一次成本低，避免每个测试启动/停止的开销。

### 5. 目录结构

```
Test/browsergateway-test-client/
├── src/
│   ├── client/
│   │   ├── __init__.py
│   │   └── mobile_client.py   # WebSocket 测试客户端封装
│   ├── mock/  (已有)
│   └── protocol/  (已有)
├── tests/
│   ├── conftest.py            # fixtures: gids_mock, mobile_client
│   ├── test_login.py          # 登录 / 登出流程
│   ├── test_heartbeat.py      # 心跳保活
│   ├── test_control.py        # 按键 + 触屏控制
│   └── test_media.py          # 媒体流接收
├── requirements.txt           # 新增 pytest, pytest-asyncio, websockets
└── config/config.yaml  (已有)
```

## Risks / Trade-offs

- **[Risk] mobile 服务需预先运行** → Mitigation: conftest.py 在连接失败时输出明确错误提示，说明需要先启动 mobile 服务
- **[Risk] 媒体流测试依赖云手机设备推流** → Mitigation: media 测试仅验证帧格式（魔数/帧头字节），不依赖实际视频内容；若无推流则 skip
- **[Risk] TLV 字段 ID 与 mobile 服务版本耦合** → Mitigation: 字段 ID 常量集中在 `src/protocol/tlv.py` 和 `src/protocol/message.py`，单点维护
- **[Risk] 异步测试超时难以调试** → Mitigation: 所有 recv 操作设置合理 timeout（默认 5s），超时时抛出明确异常
