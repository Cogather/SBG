---
name: BrowserGateway 功能测试工程
overview: 创建一个独立的 Python 测试工程，包含模拟手机 Web 界面，用于验证 BrowserGateway 的 TCP、WebSocket、REST API 功能，并支持视频流 Mock 传输。
todos: []
---

# BrowserGateway 功能测试工程实施计划

## 1. 项目概述

创建一个独立的 Python 测试工程，用于在外网环境下对 BrowserGateway 服务进行功能自测。测试工程包含：

- 模拟手机 Web 界面（用于可视化测试）
- TCP 客户端（控制流和媒体流）
- WebSocket 客户端（控制流和媒体流）
- REST API 客户端
- 视频流 Mock 生成器

## 2. 项目结构

```
browsergateway-test-client/
├── README.md
├── requirements.txt
├── config/
│   └── config.yaml              # 测试配置（服务器地址、端口等）
├── src/
│   ├── __init__.py
│   ├── client/
│   │   ├── __init__.py
│   │   ├── tcp_client.py        # TCP 客户端（控制流和媒体流）
│   │   ├── websocket_client.py  # WebSocket 客户端（控制流和媒体流）
│   │   └── rest_client.py       # REST API 客户端
│   ├── mock/
│   │   ├── __init__.py
│   │   ├── video_generator.py   # 视频流 Mock 生成器
│   │   └── frame_encoder.py     # 视频帧编码器（H.264/H.265）
│   ├── protocol/
│   │   ├── __init__.py
│   │   ├── tlv.py               # TLV 协议编解码
│   │   └── message.py           # 消息类型定义
│   └── ui/
│       ├── __init__.py
│       ├── app.py               # Flask/FastAPI Web 应用
│       ├── templates/
│       │   └── index.html       # 手机模拟界面
│       └── static/
│           └── style.css        # 样式文件
├── tests/
│   ├── __init__.py
│   ├── test_tcp_control.py      # TCP 控制流测试
│   ├── test_tcp_media.py        # TCP 媒体流测试
│   ├── test_websocket_control.py # WebSocket 控制流测试
│   ├── test_websocket_media.py  # WebSocket 媒体流测试
│   └── test_rest_api.py         # REST API 测试
└── scripts/
    └── run_test.py              # 测试运行脚本
```

## 3. 核心功能模块

### 3.1 TCP 客户端模块 (`src/client/tcp_client.py`)

**功能**：

- 支持 TLS 加密连接
- 实现 TLV 协议编解码
- 支持控制流 TCP（登录、心跳、按键、触摸事件）
- 支持媒体流 TCP（视频帧、音频帧传输）

**关键接口**：

- `connect(host, port, use_tls=True)` - 建立连接
- `login(imei, imsi)` - 发送登录消息
- `send_heartbeat()` - 发送心跳
- `send_key_event(key_code, action)` - 发送按键事件
- `send_touch_event(x, y, action)` - 发送触摸事件
- `send_video_frame(frame_data, frame_type)` - 发送视频帧
- `send_audio_frame(audio_data)` - 发送音频帧

### 3.2 WebSocket 客户端模块 (`src/client/websocket_client.py`)

**功能**：

- 支持控制流 WebSocket (`/control/websocket/{imeiAndImsi}`)
- 支持媒体流 WebSocket (`/browser/websocket/{imeiAndImsi}`)
- 实现 JSON 消息格式
- 支持二进制数据传输

**关键接口**：

- `connect_control(imei_and_imsi)` - 连接控制流
- `connect_media(imei_and_imsi, init_params)` - 连接媒体流并初始化
- `send_control_message(message_type, payload)` - 发送控制消息
- `send_media_frame(frame_data)` - 发送媒体帧
- `receive_message()` - 接收消息

### 3.3 REST API 客户端模块 (`src/client/rest_client.py`)

**功能**：

- 调用浏览器管理 API
- 调用扩展管理 API
- 支持限流测试

**关键接口**：

- `pre_open_browser(params)` - 预开浏览器
- `delete_user_data(imei, imsi)` - 删除用户数据
- `load_extension(params)` - 加载扩展
- `get_plugin_info()` - 获取插件信息

### 3.4 视频流 Mock 生成器 (`src/mock/video_generator.py`)

**功能**：

- 生成合成视频帧（彩色渐变、移动图案）
- 支持 I 帧和 P 帧生成
- 支持不同分辨率和帧率
- 使用 OpenCV 生成测试图案

**关键接口**：

- `generate_frame(width, height, frame_type, frame_number)` - 生成单帧
- `start_streaming(fps, callback)` - 开始流式传输
- `encode_frame(frame, codec='H264')` - 编码帧

### 3.5 Web 界面 (`src/ui/app.py` + `templates/index.html`)

**功能**：

- 模拟手机屏幕界面
- 显示接收到的视频流
- 提供控制按钮（按键、触摸）
- 显示连接状态和日志

**界面元素**：

- 视频显示区域（Canvas）
- 虚拟按键（上下左右、确认、返回等）
- 触摸屏区域
- 连接状态指示
- 日志输出区域
- 配置面板（服务器地址、端口、IMEI/IMSI）

## 4. 测试用例覆盖

基于黑盒测试用例，覆盖以下功能：

### 4.1 TCP 控制流测试

- ✅ TLS 连接建立
- ✅ 登录认证（LOGIN 消息）
- ✅ 心跳保活（HEARTBEATS）
- ✅ 按键事件（KEY_EVENT）
- ✅ 触摸事件（TOUCH_EVENT）
- ✅ 登出（LOGOUT）

### 4.2 TCP 媒体流测试

- ✅ TLS 连接建立
- ✅ 登录认证
- ✅ 心跳保活
- ✅ I 帧传输
- ✅ P 帧传输
- ✅ 音频帧传输

### 4.3 WebSocket 控制流测试

- ✅ 连接建立
- ✅ JSON 消息格式
- ✅ 屏幕点击事件
- ✅ 键盘输入事件
- ✅ 双向消息转发

### 4.4 WebSocket 媒体流测试

- ✅ 连接建立
- ✅ 初始化参数发送
- ✅ 视频帧传输（I 帧、P 帧）
- ✅ 音频帧传输
- ✅ 实时参数调整

### 4.5 REST API 测试

- ✅ 预开浏览器
- ✅ 删除用户数据
- ✅ 加载扩展
- ✅ 获取插件信息
- ✅ 限流测试（`/app-api/control/file/upload`、`/auth/v1/authIMEI`）

## 5. 技术栈

- **后端框架**: FastAPI 或 Flask（轻量级 Web 框架）
- **TCP 客户端**: `ssl` + `socket` 或 `asyncio`
- **WebSocket 客户端**: `websockets` 库
- **HTTP 客户端**: `httpx` 或 `requests`
- **视频处理**: `opencv-python` (cv2) + `numpy`
- **视频编码**: `ffmpeg-python` 或使用 OpenCV 的编码器
- **前端**: HTML5 + JavaScript + Canvas API
- **配置管理**: `pyyaml`

## 6. 配置示例

`config/config.yaml`:

```yaml
server:
  address: "127.0.0.1"  # BrowserGateway 服务器地址
  http_port: 8090
  websocket:
    media_port: 30002
    control_port: 30005
  tcp:
    control_tls_port: <control_tls_port>
    media_tls_port: <media_tls_port>
  
test:
  imei: "123456789012345"
  imsi: "987654321098765"
  video:
    width: 1920
    height: 1080
    fps: 30
    codec: "H264"
    gop_size: 29
```

## 7. 实施步骤

1. **项目初始化**

   - 创建项目结构
   - 配置 Python 虚拟环境
   - 安装依赖包

2. **协议实现**

   - 实现 TLV 编解码
   - 实现消息类型定义
   - 实现协议常量

3. **客户端实现**

   - TCP 客户端（控制流和媒体流）
   - WebSocket 客户端（控制流和媒体流）
   - REST API 客户端

4. **视频流 Mock**

   - 实现视频帧生成器
   - 实现视频编码器
   - 实现流式传输逻辑

5. **Web 界面**

   - 实现后端 API
   - 实现前端界面
   - 实现视频流显示（使用 Canvas）

6. **测试用例**

   - 编写各协议的测试用例
   - 集成测试

7. **文档和部署**

   - 编写 README
   - 编写使用说明
   - 打包部署

## 8. 关键实现细节

### 8.1 TLV 协议实现

- Type: 2 字节（大端序）
- Length: 4 字节（大端序）
- Value: N 字节

### 8.2 视频流格式

- WebSocket: `[4字节: 帧类型][4字节: 时间戳][4字节: 数据长度][N字节: 数据]`
- TCP: TLV 格式，Type=0x000A (VIDEO_FRAME)

### 8.3 视频帧生成策略

- 使用 OpenCV 生成彩色渐变图案
- 每 GOP 大小生成一个 I 帧，其余为 P 帧
- 支持动态移动图案（便于观察视频流是否正常）

## 9. 测试验证点

- ✅ TCP 控制流：登录、心跳、按键、触摸事件传输正常
- ✅ TCP 媒体流：视频帧、音频帧传输正常
- ✅ WebSocket 控制流：JSON 消息双向传输正常
- ✅ WebSocket 媒体流：视频流传输流畅，延迟可接受
- ✅ REST API：所有接口调用成功
- ✅ 视频流显示：Web 界面能正确显示接收到的视频流
- ✅ 控制交互：按键和触摸操作能正确发送到服务器

## 10. 依赖包

```
fastapi==0.104.1
uvicorn==0.24.0
websockets==12.0
httpx==0.25.1
opencv-python==4.8.1.78
numpy==1.24.3
pyyaml==6.0.1
python-socketio==5.10.0
```