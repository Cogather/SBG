## ADDED Requirements

### Requirement: 测试环境自动就绪检测
The test suite SHALL verify all required service ports are reachable before running any tests.

#### Scenario: 所有服务端口可达时允许运行测试
- **WHEN** `run_tests.py` 启动时，对以下端口执行 TCP 可达性检查：GIDS Mock (9090)、browser-proxy (8000)、mobile HTTP (8088)、mobile WebSocket (40002)、browser-gateway TCP control (30001)、browser-gateway TCP media (30002)、browser-gateway HTTP (8090)
- **THEN** 所有端口可达时启动 pytest 运行全套测试

#### Scenario: 任一服务端口不可达时拒绝运行
- **WHEN** 任意一个必需端口不可达
- **THEN** 打印具体端口错误信息并以退出码 1 终止，不启动 pytest

### Requirement: GIDS Mock 服务器自动启停
The test suite SHALL start a GIDS mock server as a subprocess for the session and shut it down after all tests complete.

#### Scenario: 会话启动时 GIDS Mock 自动就绪
- **WHEN** pytest 会话开始，`gids_mock` session fixture 被激活
- **THEN** GIDS Mock 在端口 9090 启动，10s 内 `/health` 返回 HTTP 200，测试才开始执行；若超时则终止进程并抛出 RuntimeError

#### Scenario: GIDS Mock 提供完整的三步登录 API
- **WHEN** mobile 服务发起三步 GIDS HTTP 登录
- **THEN** Mock 依次响应：
  - `POST /app-api/devicetcp/app/login/v1/gridLoginAuth` → `{"token": "mock-token-12345", "nodeGateWayUrl": "127.0.0.1:30001"}`
  - `POST /app-api/devicetcp/app/login/v1/gridLoginAuthOpenBrowser` → 同上
  - `POST /app-api/devicetcp/app/login/v1/deviceLoginAuth` → `{"tcpAddr": "127.0.0.1:30001"}` 并在内部注册 `{imei}_{imsi}` session

#### Scenario: 会话结束后 GIDS Mock 自动关闭
- **WHEN** pytest 会话结束
- **THEN** GIDS Mock 子进程被 terminate，5s 内未退出则 kill
### Requirement: WebSocket 登录流程
The test suite SHALL establish a WebSocket connection to the mobile service and complete the JSON login handshake.

#### Scenario: 正常登录成功（无错误响应）
- **WHEN** 客户端连接 `ws://127.0.0.1:40002/app/websocket/{IMEI}_{IMSI}` 并发送 JSON 登录消息：
  `{"type": "login", "cs": "240x320", "dv": 0, "at": 5, "ga": "http://127.0.0.1:9090"}`
- **THEN** 5 秒内未收到任何文本消息（无 `{"code": <非0>}` 错误响应），判定登录成功；session 级连接登录后可正常发送控制消息

#### Scenario: 登录失败时收到错误 JSON
- **WHEN** 登录参数非法或服务端处理失败
- **THEN** 服务端返回 `{"code": <非0>, "msg": "..."}` 文本消息

#### Scenario: 正常登出后可重新登录同一设备
- **WHEN** 登录成功后客户端发送 `{"type": "logout"}` 并关闭 WebSocket 连接，等待 1s
- **THEN** 连接断开无异常；使用同一 IMEI/IMSI 再次连接并发送登录消息，5 秒内无错误响应

### Requirement: 连接保活验证
The test suite SHALL verify the WebSocket connection remains alive after 15 seconds of idle time.

#### Scenario: 空闲 15s 后连接仍可用
- **WHEN** session 级客户端登录成功后，空闲等待 15 秒不发送任何消息
- **THEN** 连接未被服务端关闭；之后发送 `{"type": "direction", "ct": 0, "cv": 12}`（UP 键）不抛出异常

### Requirement: 按键控制
The test suite SHALL send direction/key control JSON messages and verify the server does not close the connection.

#### Scenario: 方向键与功能键发送正常
- **WHEN** 登录成功后发送 `{"type": "direction", "ct": 0, "cv": <cv>}`，cv 取值覆盖：UP=12, DOWN=13, LEFT=14, RIGHT=15, ENTER=20, BACK=18, MENU=17
- **THEN** 每条消息发送成功，连接不关闭，无异常（mobile 不返回 WebSocket 级响应）

#### Scenario: 数字键 0-9 发送正常
- **WHEN** 登录成功后依次发送 `{"type": "direction", "ct": 0, "cv": <0-9>}`，共 10 条
- **THEN** 所有消息发送成功，连接不关闭

### Requirement: 触屏控制
The test suite SHALL send touch event JSON messages and verify the server does not close the connection.

#### Scenario: 单点触摸点击（DOWN → UP）
- **WHEN** 登录成功后依次发送：`{"type": "direction", "ct": 1, "cv": 0}`（ACTION_DOWN），间隔 100ms，再发送 `{"type": "direction", "ct": 1, "cv": 1}`（ACTION_UP）
- **THEN** 两条消息均发送成功，连接不关闭

#### Scenario: 滑动手势（DOWN → MOVE × 3 → UP）
- **WHEN** 登录成功后依次发送 ACTION_DOWN（cv=0）、三次 ACTION_MOVE（cv=2）、ACTION_UP（cv=1），每步间隔 50ms
- **THEN** 所有消息发送成功，连接不关闭

### Requirement: 媒体流帧格式验证
The test suite SHALL receive video and audio binary frames from the server and verify their format.

#### Scenario: 登录后 30s 内浏览器已打开并推送视频帧
- **WHEN** session 级客户端登录成功后，等待最多 30s 接收首字节为 `0x01` 的二进制帧
- **THEN** 收到帧则验证通过；若 30s 内无帧则标记为 `skipped`（推流环境不可用，不计为失败）

#### Scenario: 视频帧格式正确
- **WHEN** 服务端推送视频数据（mobile 从云手机 TLV VIDEO 帧转发）
- **THEN** 收到的 WebSocket 二进制消息满足：首字节 `0x01`，第二字节为帧类型整数（0/1/2 均合法），总长度 > 2 字节（含 H.264 payload）；若 30s 内无帧则 skip

#### Scenario: 音频帧格式正确
- **WHEN** 服务端推送音频数据（mobile 从云手机 TLV AUDIO 帧转发）
- **THEN** 收到的 WebSocket 二进制消息满足：首字节 `0x02`，总长度 > 1 字节（含 MP3 payload）；若 30s 内无帧则 skip

