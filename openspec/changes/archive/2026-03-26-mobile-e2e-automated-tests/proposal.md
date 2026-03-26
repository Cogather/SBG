## Why

当前对 mobile 服务的验证完全依赖人工通过前端 UI 进行登录和操作，缺乏可重复执行的自动化手段。已有黑盒测试用例文档（case.md）覆盖了端到端功能场景，但无对应的自动化实现，导致回归验证效率低、覆盖不稳定。

## What Changes

- 在现有 `Test/browsergateway-test-client` 中新增自动化功能测试框架及用例
- 基于已有 TLV 协议编解码（`protocol/tlv.py`）和 GIDS Mock Server（`mock/gids_mock_server.py`）构建测试客户端
- 实现以下端到端自动化测试场景（对应 case.md 中的功能性用例）：
  - **登录流程**：WebSocket 连接 → TLV LOGIN 消息 → ACK 确认
  - **心跳保活**：登录后定时发送 HEARTBEATS，验证服务端响应
  - **按键控制**：发送上/下/左/右/确认/返回/Home 等 CONTROL 类型 TLV，验证 RETURN_CONTROL 响应
  - **触屏控制**：发送 ACTION_DOWN / ACTION_MOVE / ACTION_UP 触摸事件，验证响应
  - **媒体流接收**：验证服务端推送 VIDEO/AUDIO TLV 帧，检查帧格式和完整性
  - **登出流程**：正常断开连接，验证服务端状态清理
- 新增 pytest 测试入口，支持在 CI 或命令行中独立运行
- 新增 browser-proxy mock（可选），用于隔离对上游依赖的测试

## Capabilities

### New Capabilities

- `mobile-e2e-test-suite`: 针对 mobile 服务的端到端自动化测试套件，覆盖登录、心跳、按键控制、触屏控制、媒体流接收和登出全流程

### Modified Capabilities

<!-- 无现有 spec 需修改 -->

## Impact

- **Test/browsergateway-test-client/**：新增 `tests/` 目录（pytest 用例）、`src/client/` 目录（WebSocket + TCP 测试客户端封装）、`conftest.py`、更新 `requirements.txt`（增加 pytest、pytest-asyncio、websockets）
- **不修改生产代码**：mobile、browser-gateway、browser-proxy 均无变更
- **依赖**：需要 GIDS Mock Server（已有）在本地运行；mobile 服务需可访问（本地 8088/40002 端口）
