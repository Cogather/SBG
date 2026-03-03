# BrowserGateway 功能测试工程

这是一个独立的 Python 测试工程，用于在外网环境下对 BrowserGateway 服务进行功能自测。

## 功能特性

- ✅ 模拟手机 Web 界面（用于可视化测试）
- ✅ TCP 客户端（控制流和媒体流，支持 TLS 和非 TLS）
- ✅ WebSocket 客户端（控制流和媒体流）
- ✅ REST API 客户端
- ✅ 视频流 Mock 生成器（支持 H.264）
- ✅ 前端视频流显示（Canvas 渲染）

## 项目结构

```
browsergateway-test-client/
├── README.md
├── requirements.txt
├── config/
│   └── config.yaml              # 测试配置
├── src/
│   ├── client/                 # 客户端实现
│   ├── mock/                   # Mock 数据生成
│   ├── protocol/               # 协议实现
│   └── ui/                     # Web 界面
├── tests/                      # 测试用例
└── scripts/                    # 运行脚本
```

## 快速开始

### 1. 安装依赖

**推荐使用安装脚本（自动处理 SSL/代理问题）：**

```bash
# Windows
install_dependencies.bat

# Linux/Mac/Git Bash
chmod +x install_dependencies.sh
./install_dependencies.sh
```

**或手动安装：**

```bash
# 创建虚拟环境（推荐）
python -m venv venv
venv\Scripts\activate  # Windows
# source venv/bin/activate  # Linux/Mac

# 使用国内镜像源安装（推荐，解决 SSL 问题）
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple --trusted-host pypi.tuna.tsinghua.edu.cn
```

**如果遇到安装问题，请查看 [INSTALL_TROUBLESHOOTING.md](INSTALL_TROUBLESHOOTING.md)**

### 2. 配置

编辑 `config/config.yaml`，设置 BrowserGateway 服务器地址和端口。

### 3. 运行 Web 界面

```bash
python scripts/run_test.py
```

然后在浏览器中访问 `http://localhost:8000`

### 4. 运行测试用例

**方式一：一键脚本（推荐）**

```bash
# Linux/Mac/Git Bash
./scripts/run_all_tests.sh

# Windows CMD
scripts\run_all_tests.bat

# 启动 Web 测试界面
./scripts/run_all_tests.sh --ui  # Linux/Mac
scripts\run_all_tests.bat --ui   # Windows
```

**方式二：手动运行**

```bash
# 运行所有测试
pytest tests/

# 运行特定测试
pytest tests/test_tcp_control.py -v

# 显示详细输出
pytest tests/ -v -s
```

## 详细测试指南

📖 **完整的测试说明请查看 [TEST_GUIDE.md](TEST_GUIDE.md)**

测试指南包含：
- 详细的测试步骤
- Web 界面使用方法
- 自动化测试用例说明
- 常见问题解决方案

## 使用说明

### Web 界面

1. 打开浏览器访问 `http://localhost:8000`
2. 在配置面板中设置服务器地址、端口、IMEI/IMSI
3. 选择连接类型（TCP/WebSocket）
4. 点击连接按钮建立连接
5. 使用虚拟按键或触摸屏发送控制事件
6. 查看视频流显示和日志输出

## 功能验证能力

### 可以验证的 BrowserGateway 功能

| 功能模块 | 功能项 | 验证状态 | 说明 |
|---------|--------|---------|------|
| **TCP 控制流** | 连接建立（非 TLS） | ✅ 可验证 | 支持端口 30001 非 TLS 连接 |
| | 连接建立（TLS） | ⚠️ 部分验证 | 需要证书配置 |
| | 登录消息（LOGIN） | ⚠️ 已跳过 | 测试时跳过登录，直接发送控制消息 |
| | 心跳消息（HEARTBEATS） | ✅ 可验证 | 支持发送心跳包 |
| | 按键事件（KEY_EVENT） | ✅ 可验证 | 通过 CONTROL 消息发送，ctrlType=1 |
| | 触摸事件（TOUCH_EVENT） | ✅ 可验证 | 通过 CONTROL 消息发送，ctrlType=2 |
| | 控制消息接收确认 | ✅ 可验证 | BrowserGateway 日志显示接收状态 |
| | 连接断开 | ✅ 可验证 | 支持主动断开连接 |
| **TCP 媒体流** | 连接建立（非 TLS） | ✅ 可验证 | 支持端口 30011 非 TLS 连接 |
| | 连接建立（TLS） | ⚠️ 部分验证 | 需要证书配置 |
| | 登录消息（LOGIN） | ⚠️ 已跳过 | 测试时跳过登录 |
| | 视频帧传输（VIDEO） | ✅ 可验证 | 支持 H.264 视频帧发送，包含帧类型和数据 |
| | 视频帧接收确认 | ✅ 可验证 | BrowserGateway 日志显示接收状态 |
| | 前端视频显示 | ✅ 可验证 | Canvas 实时显示视频帧 |
| **WebSocket 控制流** | 连接建立 | ✅ 可验证 | 支持端口 30005 连接 |
| | JSON 消息发送 | ✅ 可验证 | 支持发送 JSON 格式控制消息 |
| | 消息接收 | ✅ 可验证 | 支持接收服务器消息 |
| **WebSocket 媒体流** | 连接建立 | ✅ 可验证 | 支持端口 30002 连接 |
| | 初始化参数发送 | ✅ 可验证 | 支持发送视频参数配置 |
| | 视频流传输 | ✅ 可验证 | 支持二进制视频数据发送 |
| **REST API** | 预打开浏览器 | ✅ 可验证 | `POST /browsergw/browser/preOpen` |
| | 删除用户数据 | ✅ 可验证* | `DELETE /browsergw/browser/userdata/delete`，由于依赖第三方存储系统（S3），只要有响应就视为通过 |
| | 加载扩展 | ✅ 可验证* | `POST /browsergw/extension/load`，由于依赖第三方存储系统（S3），只要有响应就视为通过 |
| | 获取插件信息 | ✅ 可验证 | `GET /browsergw/extension/pluginInfo` |
| **TLV 协议** | TLV 编码格式 | ✅ 可验证 | 支持复合 TLV 格式（Magic + Count + DataLen + Fields） |
| | 消息类型识别 | ✅ 可验证 | 支持 Type.LOGIN, Type.HEARTBEATS, Type.CONTROL, Type.VIDEO 等 |
| | 字段解析 | ✅ 可验证 | 支持 ID.TYPE, ID.CTRL_TYPE, ID.CTRL_VAL, ID.VIDEO_DATA 等字段 |
| **调试功能** | 日志输出 | ✅ 可验证 | 测试客户端和 BrowserGateway 都有详细日志 |
| | 连接状态显示 | ✅ 可验证 | Web 界面实时显示连接状态 |
| | 视频流预览 | ✅ 可验证 | Canvas 实时显示生成的视频帧 |

### 不能验证的 BrowserGateway 功能

| 功能模块 | 功能项 | 原因 |
|---------|--------|------|
| **TCP 控制流** | 登录验证流程 | 测试时跳过登录步骤，无法验证 UserBind 验证、Token 校验等 |
| | 鼠标事件（MOUSE_EVENT） | 测试客户端未实现鼠标事件发送 |
| | 拖拽事件（DRAG_EVENT） | 测试客户端未实现拖拽事件发送 |
| | 文本输入（TEXT_INPUT） | 测试客户端未实现文本输入事件发送 |
| | 剪贴板操作（CLIPBOARD） | 测试客户端未实现剪贴板操作 |
| | 登出消息（LOGOUT） | 测试客户端有实现但未在界面中调用 |
| | 心跳超时检测 | 需要长时间等待，测试客户端未实现超时测试 |
| **TCP 媒体流** | 音频帧传输（AUDIO） | 测试客户端未实现音频帧生成和发送 |
| | 登录验证流程 | 测试时跳过登录步骤 |
| | 媒体流转发到浏览器 | 需要浏览器实例运行，测试客户端无法验证 |
| **WebSocket 控制流** | 与 Muen SDK 代理 | 需要 Muen SDK 运行，测试客户端无法验证 |
| | 浏览器扩展通信 | 需要浏览器扩展运行，测试客户端无法验证 |
| **WebSocket 媒体流** | 音频流传输 | 测试客户端未实现音频流生成 |
| | 媒体流转发到浏览器 | 需要浏览器实例运行，测试客户端无法验证 |
| | WebCodecs 模式 | 需要浏览器环境，测试客户端无法验证 |
| | FFmpeg 编解码 | 需要 FFmpeg 环境，测试客户端无法验证 |
| **REST API** | 浏览器实例管理 | 需要浏览器实例运行，测试客户端无法验证完整流程 |
| | 扩展热更新 | 需要扩展运行，测试客户端无法验证 |
| | 健康检查接口 | BrowserGateway 未实现 `/api/browsers/health_check` 端点 |
| **系统功能** | 用户会话管理 | 需要 UserBind 服务，测试环境未配置 |
| | 数据上报 | 需要 CSE 服务，测试环境未配置 |
| | 告警推送 | 需要告警服务，测试环境未配置 |
| | 证书管理 | 需要证书服务，测试环境未配置 |
| | 浏览器实例创建 | 需要 Chrome/Chromium 运行，测试客户端无法验证 |
| | 用户数据下载 | 需要存储服务，测试环境未配置 |
| **性能测试** | 延迟测试 | 测试客户端未实现延迟测量 |
| | 吞吐量测试 | 测试客户端未实现吞吐量统计 |
| | 并发连接测试 | 测试客户端未实现多连接并发测试 |
| | 压力测试 | 测试客户端未实现压力测试场景 |

### 验证状态说明

- ✅ **可验证**：测试客户端已实现，可以完整验证 BrowserGateway 功能
- ✅ **可验证***：测试客户端已实现，但由于依赖第三方存储系统，测试标准为"只要有响应就视为通过"（适用于删除用户数据和加载扩展 API）
- ⚠️ **部分验证**：测试客户端已实现，但需要额外配置（如证书）才能完整验证
- ❌ **不能验证**：测试客户端未实现或需要外部依赖，无法验证

### 测试限制说明

1. **登录验证**：当前测试时跳过登录步骤，因此无法验证：
   - UserBind 验证流程
   - Token 校验
   - 会话创建和管理

2. **浏览器实例**：测试客户端无法验证需要浏览器实例运行的功能：
   - 浏览器实例创建和管理
   - 媒体流转发到浏览器
   - 浏览器扩展通信

3. **外部服务依赖**：测试环境未配置以下服务，无法验证：
   - CSE 服务（用户绑定、数据上报）
   - 存储服务（用户数据下载）
   - 告警服务（告警推送）

4. **第三方存储系统依赖**：以下 REST API 由于依赖第三方存储系统（S3），测试标准为"只要有响应就视为通过"：
   - **删除用户数据** (`DELETE /browsergw/browser/userdata/delete`)：需要删除本地和 S3 存储的用户数据，如果 S3 服务不可用，只要服务器返回响应（成功或失败）即视为测试通过
   - **加载扩展** (`POST /browsergw/extension/load`)：需要从 S3 下载扩展文件，如果 S3 服务不可用，只要服务器返回响应（成功或失败）即视为测试通过

5. **TLS 加密**：TLS 连接需要证书配置，当前主要测试非 TLS 连接

### 测试用例

测试用例覆盖以下功能：

- TCP 控制流：连接、心跳、按键、触摸事件
- TCP 媒体流：连接、视频帧传输
- WebSocket 控制流：连接、JSON 消息传输
- WebSocket 媒体流：连接、视频流传输
- REST API：浏览器管理、扩展管理等接口

## 配置说明

详见 `config/config.yaml` 文件。

## 技术栈

- FastAPI - Web 框架
- WebSockets - WebSocket 客户端
- OpenCV - 视频处理
- httpx - HTTP 客户端

## 许可证

MIT
