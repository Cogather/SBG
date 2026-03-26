# browsergateway-test-client

Python 自动化测试客户端，用于对 mobile 服务进行端到端功能测试。

## 目录结构

```
browsergateway-test-client/
├── src/
│   ├── client/
│   │   └── mobile_client.py   # WebSocket 测试客户端封装
│   ├── mock/
│   │   └── gids_mock_server.py  # GIDS Mock Server
│   └── protocol/
│       ├── tlv.py             # TLV 编解码
│       └── message.py         # 消息类型常量
├── tests/
│   ├── conftest.py            # pytest fixtures
│   ├── test_login.py          # 登录 / 登出测试
│   ├── test_heartbeat.py      # 心跳保活测试
│   ├── test_control.py        # 按键 + 触屏控制测试
│   └── test_media.py          # 媒体流接收测试
├── config/
│   └── config.yaml            # 服务地址配置
├── pytest.ini
└── requirements.txt
```

## 前置条件

| 依赖 | 说明 |
|------|------|
| Python 3.9+ | 运行测试客户端 |
| mobile 服务 | 需在 `localhost:40002` 运行（WebSocket） |
| browser-gateway | 需在 `localhost:30001` 运行（TCP 控制通道） |
| GIDS Mock | 由 pytest fixture 自动启动（端口 9090） |

> 一键启动所有服务：在仓库根目录执行 `start-dev.bat`

## 环境安装（首次）

```bat
cd Test\browsergateway-test-client
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
```

## 运行测试

```bat
# 激活虚拟环境
.venv\Scripts\activate

# 运行全部测试
pytest tests/ -v

# 只运行登录测试
pytest tests/test_login.py -v

# 只运行控制测试
pytest tests/test_control.py -v

# 跳过媒体流测试（无推流环境时）
pytest tests/ -v --ignore=tests/test_media.py
```

## 测试用例清单

### 1. 登录 / 登出测试 (`test_login.py`)

| 用例 ID | 用例名称 | 描述 | 断言 |
|---------|----------|------|------|
| LOGIN-01 | `test_login_success` | session 级 client 已登录，验证连接可正常使用 | 发送方向消息无异常 |
| LOGIN-02 | `test_logout` | 登录成功后正常断开，使用相同 IMEI/IMSI 重新登录 | 断开重连后仍能正常登录，无错误响应 |

### 2. 心跳保活测试 (`test_heartbeat.py`)

| 用例 ID | 用例名称 | 描述 | 断言 |
|---------|----------|------|------|
| HB-01 | `test_connection_stays_alive` | 登录后静默等待 15s，验证连接未被服务端断开 | 等待后仍可发送方向消息且无异常 |

### 3. 按键控制测试 (`test_control.py`)

| 用例 ID | 用例名称 | 描述 | 参数 | 断言 |
|---------|----------|------|------|------|
| CTRL-01~06 | `test_key_event` | 方向键/功能键事件测试 | UP(12), DOWN(13), LEFT(14), RIGHT(15), ENTER(20), BACK(18) | 发送成功且连接不关闭（无异常） |
| CTRL-07 | `test_menu_key` | 菜单键事件测试 | MENU(17) | 发送成功且连接不关闭（无异常） |
| CTRL-08~17 | `test_numeric_keys` | 数字键 0-9 事件测试 | 0-9 | 发送成功且连接不关闭（无异常） |
| CTRL-18 | `test_touch_tap` | 单点触摸点击测试 | ACTION_DOWN(0) → ACTION_UP(1) | 发送成功且连接不关闭（无异常） |
| CTRL-19 | `test_touch_swipe` | 滑动手势测试 | DOWN → 3×MOVE → UP | 发送成功且连接不关闭（无异常） |

### 4. 媒体流测试 (`test_media.py`)

| 用例 ID | 用例名称 | 描述 | 断言 |
|---------|----------|------|------|
| MEDIA-01 | `test_browser_opened_after_login` | 登录后 30s 内验证浏览器成功打开 | 收到至少一帧视频数据（首字节 0x01）；无流时 skip |
| MEDIA-02 | `test_video_frame_format` | 视频帧格式验证 | 首字节 0x01，第二字节为 frameType（0/1/2 均合法），有 H.264 payload；无流时 skip |
| MEDIA-03 | `test_audio_frame_format` | 音频帧格式验证 | 首字节 0x02，其后有 MP3 数据；无流时 skip |

> **注意**：媒体测试依赖真实推流环境；无云手机推流时三个用例均自动 **skip**（不计入失败）。

---

## 测试说明

| 测试文件 | 覆盖场景 | 前置要求 |
|----------|----------|----------|
| `test_login.py` | 登录握手、登出重连 | mobile + GIDS Mock |
| `test_heartbeat.py` | 心跳保活 | mobile + GIDS Mock + browser-gateway |
| `test_control.py` | 方向键、功能键、数字键、触屏点击/滑动 | mobile + GIDS Mock + browser-gateway |
| `test_media.py` | 浏览器打开验证（视频帧）、帧格式校验 | 全栈（含推流） |

> 媒体测试（`test_media.py`）全部在无推流环境下自动 **skip**，不计入失败。有真实云手机推流时可验证帧格式。

## 配置

编辑 `config/config.yaml` 修改服务地址：

```yaml
server:
  address: 127.0.0.1
  websocket:
    media_port: 40002   # mobile WebSocket 端口
```
