## 1. 环境与依赖

- [x] 1.1 在 `Test/browsergateway-test-client/requirements.txt` 中新增 `pytest`、`pytest-asyncio`、`websockets` 依赖
- [x] 1.2 确认 `Test/mock-servers/` 中的 GIDS Mock Server 路径和接口与测试指南一致（三步登录：gridLoginAuth → gridLoginAuthOpenBrowser → deviceLoginAuth）
- [x] 1.3 在 `Test/browsergateway-test-client/` 下创建 `src/client/` 目录及 `__init__.py`

## 2. WebSocket 测试客户端封装

- [x] 2.1 创建 `src/client/mobile_client.py`，封装 `MobileTestClient` 类：`connect(imei, imsi)`、`send_tlv(type, **kwargs)`、`recv_tlv(timeout=5)`、`disconnect()`
- [x] 2.2 `connect()` 内部使用 `websockets` 连接 `ws://localhost:40002/app/websocket/{IMEI}_{IMSI}`，URL 从 `config/config.yaml` 读取
- [x] 2.3 `send_tlv()` 复用 `src/protocol/tlv.py` 中的 `TLVEncoder.encode()` 编码后发送二进制帧
- [x] 2.4 `recv_tlv()` 接收二进制帧并用 `TLVEncoder.decode()` 解码，超时抛出 `TimeoutError`

## 3. pytest fixtures（conftest.py）

- [x] 3.1 创建 `tests/conftest.py`，添加 session-scoped fixture `gids_mock`：以子进程方式启动 `Test/mock-servers/mock/gids_mock_server.py`，等待健康检查（GET /health）通过后 yield，测试结束后终止进程
- [x] 3.2 添加 function-scoped fixture `mobile_client`：依赖 `gids_mock`，创建 `MobileTestClient` 实例，yield 给测试用例，测试结束后调用 `disconnect()`
- [x] 3.3 在 `conftest.py` 中配置 `pytest.ini` 或 `pyproject.toml`：设置 `asyncio_mode = auto`

## 4. 登录 / 登出测试

- [x] 4.1 创建 `tests/test_login.py`
- [x] 4.2 实现 `test_login_success`：`mobile_client.connect(imei, imsi)` → 发送 LOGIN TLV（含 IMEI、IMSI、lcdWidth、lcdHeight、token、appType）→ 断言收到 ACK（type=7）且 code=0（对应测试指南日志 `receive ack :1, code is: 200`）
- [x] 4.3 实现 `test_logout`：登录成功后调用 `disconnect()`，断言无异常，等待 1 秒后用同一 IMEI/IMSI 可再次登录

## 5. 心跳测试

- [x] 5.1 创建 `tests/test_heartbeat.py`
- [x] 5.2 实现 `test_heartbeat_keepalive`：登录成功后发送 HEARTBEATS TLV（type=2，SEQ=当前时间戳毫秒）→ 等待 2 秒 → 发送任意 CONTROL 消息 → 断言连接仍活跃（能正常收到响应）

## 6. 按键控制测试

- [x] 6.1 创建 `tests/test_control.py`
- [x] 6.2 实现参数化测试 `test_key_event[key_code]`，覆盖按键：UP(cv=12)、DOWN(cv=13)、LEFT(cv=14)、RIGHT(cv=15)、ENTER(cv=20)、BACK(cv=18)、HOME（对应测试指南 ct=0 的控制格式）→ 断言收到 RETURN_CONTROL（type=12）响应
- [x] 6.3 实现 `test_menu_key`：发送菜单键（cv=17）→ 断言收到 RETURN_CONTROL 响应
- [x] 6.4 实现 `test_numeric_keys`：发送数字键 0-9（各对应 cv 值）→ 断言每次均收到 RETURN_CONTROL 响应

## 7. 触屏控制测试

- [x] 7.1 实现 `test_touch_tap`（在 `test_control.py` 中）：依次发送 ACTION_DOWN(action=0, x, y) → ACTION_UP(action=1, x, y) CONTROL TLV → 断言每条消息均收到 RETURN_CONTROL 响应
- [x] 7.2 实现 `test_touch_swipe`：依次发送 ACTION_DOWN → 3 次 ACTION_MOVE(action=2) → ACTION_UP → 断言每条均有响应

## 8. 媒体流接收测试

- [x] 8.1 创建 `tests/test_media.py`
- [x] 8.2 实现 `test_browser_opened_after_login`：登录成功（收到 ACK code=0）后，在 timeout=15s 内等待收到第一帧视频数据（WebSocket 二进制消息首字节为 `0x01`），断言收到至少一帧——以此作为浏览器已成功打开并开始推流的轻量验证；若超时则标记为 `pytest.fail`（非 skip，属于功能验收失败）
- [x] 8.3 实现 `test_video_frame_format`：登录后等待接收媒体帧（timeout=5s），若超时则 `pytest.skip()`；收到帧后断言首字节为 `0x01`，第二字节为 1 或 2（I/P 帧），其余为 H.264 数据
- [x] 8.4 实现 `test_audio_frame_format`：同上，断言首字节为 `0x02`，其余为 MP3 数据

## 9. 运行说明文档

- [x] 9.1 更新 `Test/browsergateway-test-client/README.md`（若不存在则创建），说明：前置条件（启动 GIDS Mock + mobile 服务）、虚拟环境激活、运行命令 `pytest tests/ -v`
- [x] 9.2 在测试指南 `doc/Mobile 端到端测试指南.md` 中新增「自动化测试」章节，说明如何运行 pytest 套件