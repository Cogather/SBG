# BrowserGateway 测试指南

本文档详细说明如何使用测试工程对 BrowserGateway 进行功能测试。

## 目录

1. [环境准备](#环境准备)
2. [Web 界面测试](#web-界面测试)
3. [自动化测试用例](#自动化测试用例)
4. [测试场景说明](#测试场景说明)
5. [常见问题](#常见问题)

## 环境准备

### 1. 安装依赖

```bash
# 创建虚拟环境（推荐）
python -m venv venv

# Windows 激活
venv\Scripts\activate

# Linux/Mac 激活
source venv/bin/activate

# 安装依赖包
pip install -r requirements.txt
```

### 2. 配置服务器信息

编辑 `config/config.yaml`，设置 BrowserGateway 服务器的实际地址和端口：

```yaml
server:
  address: "127.0.0.1"  # 修改为实际服务器 IP
  http_port: 8090       # HTTP 端口
  websocket:
    media_port: 30002    # WebSocket 媒体流端口
    control_port: 30005  # WebSocket 控制流端口
  tcp:
    control_tls_port: 30001  # TCP 控制流 TLS 端口
    media_tls_port: 30003    # TCP 媒体流 TLS 端口

test:
  imei: "123456789012345"  # 测试用的 IMEI
  imsi: "987654321098765"  # 测试用的 IMSI
  video:
    width: 1920
    height: 1080
    fps: 30
    codec: "H264"
    gop_size: 29
```

### 3. 确保 BrowserGateway 服务运行

在开始测试前，确保 BrowserGateway 服务器已启动并运行在配置的地址和端口上。

## Web 界面测试

### 启动 Web 界面

```bash
python scripts/run_test.py
```

启动后，在浏览器中访问：`http://localhost:8000`

### 测试步骤

#### 1. TCP 控制流测试

**步骤：**
1. 在配置面板中设置服务器地址和端口
2. 输入 IMEI 和 IMSI
3. 选择协议类型为 "TCP 控制流"
4. 点击"连接"按钮
5. 观察连接状态是否变为"已连接"

**测试操作：**
- 点击虚拟按键（上下左右、确认、返回等），观察日志输出
- 在触摸屏区域点击/拖动，观察日志输出
- 查看是否有错误信息

**验证点：**
- ✅ 连接状态显示"已连接"
- ✅ 按键事件发送成功（日志中有记录）
- ✅ 触摸事件发送成功（日志中有记录）
- ✅ 无连接错误

#### 2. TCP 媒体流测试

**步骤：**
1. 选择协议类型为 "TCP 媒体流"
2. 点击"连接"按钮
3. 连接成功后，点击"开始视频流"按钮
4. 观察日志输出，确认视频帧正在发送

**验证点：**
- ✅ 连接成功建立
- ✅ 视频流开始发送（日志中有帧传输记录）
- ✅ 帧类型正确（I 帧和 P 帧交替）
- ✅ 无传输错误

#### 3. WebSocket 控制流测试

**步骤：**
1. 选择协议类型为 "WebSocket 控制流"
2. 点击"连接"按钮
3. 连接成功后，使用虚拟按键或触摸屏发送控制事件

**验证点：**
- ✅ WebSocket 连接成功
- ✅ 控制消息发送成功
- ✅ 可以接收服务器返回的消息（如果有）

#### 4. WebSocket 媒体流测试

**步骤：**
1. 选择协议类型为 "WebSocket 媒体流"
2. 点击"连接"按钮
3. 连接成功后，点击"开始视频流"按钮

**验证点：**
- ✅ WebSocket 连接成功
- ✅ 视频帧通过 WebSocket 传输
- ✅ 帧数据格式正确

#### 5. REST API 测试

**步骤：**
1. 确保 HTTP 端口配置正确
2. 点击相应的 API 测试按钮：
   - "预开浏览器" - 测试浏览器预开接口
   - "删除用户数据" - 测试删除用户数据接口
   - "加载扩展" - 测试扩展加载接口
   - "获取插件信息" - 测试插件信息查询接口
   - "上传文件" - 测试文件上传接口（限流测试）
   - "认证 IMEI" - 测试 IMEI 认证接口（限流测试）

**验证点：**
- ✅ API 调用成功（日志中显示响应）
- ✅ 响应数据格式正确
- ✅ 错误处理正常（如果服务器返回错误）

## 自动化测试用例

### 方式一：一键脚本（推荐）

我们提供了一键启动和测试脚本，可以自动启动 BrowserGateway 服务并执行测试：

**Linux/Mac/Git Bash:**
```bash
# 运行所有测试（自动启动服务）
./scripts/run_all_tests.sh

# 启动 Web 测试界面
./scripts/run_all_tests.sh --ui

# 跳过启动 BrowserGateway 服务（假设服务已运行）
./scripts/run_all_tests.sh --skip-bg

# 显示帮助
./scripts/run_all_tests.sh --help
```

**Windows CMD:**
```cmd
REM 运行所有测试
scripts\run_all_tests.bat

REM 启动 Web 测试界面
scripts\run_all_tests.bat --ui

REM 跳过启动 BrowserGateway 服务
scripts\run_all_tests.bat --skip-bg
```

**脚本功能：**
- ✅ 自动检查环境（Python、依赖等）
- ✅ 自动启动 BrowserGateway 服务（如果未运行）
- ✅ 等待服务启动完成
- ✅ 运行所有测试用例
- ✅ 自动清理资源
- ✅ 可选启动 Web 测试界面

### 方式二：手动运行测试

```bash
# 运行所有测试用例
pytest tests/

# 显示详细输出
pytest tests/ -v

# 显示打印输出
pytest tests/ -v -s
```

### 运行特定测试模块

```bash
# TCP 控制流测试
pytest tests/test_tcp_control.py -v

# TCP 媒体流测试
pytest tests/test_tcp_media.py -v

# WebSocket 控制流测试
pytest tests/test_websocket_control.py -v

# WebSocket 媒体流测试
pytest tests/test_websocket_media.py -v

# REST API 测试
pytest tests/test_rest_api.py -v
```

### 运行特定测试用例

```bash
# 运行单个测试函数
pytest tests/test_tcp_control.py::test_tcp_login -v

# 运行多个测试函数
pytest tests/test_tcp_control.py::test_tcp_login tests/test_tcp_control.py::test_tcp_heartbeat -v
```

### 测试用例说明

#### TCP 控制流测试 (`test_tcp_control.py`)

- `test_tcp_connect` - 测试 TLS 连接建立
- `test_tcp_login` - 测试登录认证
- `test_tcp_heartbeat` - 测试心跳保活
- `test_tcp_key_event` - 测试按键事件
- `test_tcp_touch_event` - 测试触摸事件
- `test_tcp_logout` - 测试登出

#### TCP 媒体流测试 (`test_tcp_media.py`)

- `test_tcp_media_connect` - 测试媒体流连接
- `test_tcp_media_login` - 测试媒体流登录
- `test_tcp_media_video_frame` - 测试视频帧传输（I 帧和 P 帧）
- `test_tcp_media_audio_frame` - 测试音频帧传输

#### WebSocket 控制流测试 (`test_websocket_control.py`)

- `test_websocket_control_connect` - 测试控制流连接
- `test_websocket_control_message` - 测试控制消息发送
- `test_websocket_control_screen_click` - 测试屏幕点击事件
- `test_websocket_control_keyboard_input` - 测试键盘输入事件

#### WebSocket 媒体流测试 (`test_websocket_media.py`)

- `test_websocket_media_connect` - 测试媒体流连接
- `test_websocket_media_video_frame` - 测试视频帧传输

#### REST API 测试 (`test_rest_api.py`)

- `test_pre_open_browser` - 测试预开浏览器
- `test_delete_user_data` - 测试删除用户数据
- `test_get_plugin_info` - 测试获取插件信息
- `test_load_extension` - 测试加载扩展

**注意：** REST API 测试如果服务器未运行会自动跳过，不会导致测试失败。

## 测试场景说明

### 场景 1：完整功能测试流程

1. **启动 BrowserGateway 服务器**
2. **启动测试客户端 Web 界面**
   ```bash
   python scripts/run_test.py
   ```
3. **TCP 控制流测试**
   - 连接 TCP 控制流
   - 发送登录消息
   - 发送心跳消息
   - 发送按键事件
   - 发送触摸事件
   - 登出
4. **TCP 媒体流测试**
   - 连接 TCP 媒体流
   - 发送登录消息
   - 开始视频流传输
   - 观察帧传输情况
5. **WebSocket 控制流测试**
   - 连接 WebSocket 控制流
   - 发送控制消息
   - 发送屏幕点击事件
6. **WebSocket 媒体流测试**
   - 连接 WebSocket 媒体流
   - 发送视频帧
7. **REST API 测试**
   - 依次测试各个 API 接口

### 场景 2：压力测试

使用自动化测试用例进行批量测试：

```bash
# 运行多次测试，检查稳定性
for i in {1..10}; do
    echo "第 $i 次测试"
    pytest tests/test_tcp_control.py -v
done
```


## 常见问题

### 1. 连接失败

**问题：** 无法连接到服务器

**解决方案：**
- 检查服务器地址和端口配置是否正确
- 确认 BrowserGateway 服务已启动
- 检查防火墙设置
- 查看服务器日志确认端口监听状态

### 2. TLS 连接错误

**问题：** TLS 握手失败

**解决方案：**
- 检查服务器 TLS 配置
- 确认端口号正确（TLS 端口）
- 查看错误日志获取详细信息

### 3. 视频流不显示

**问题：** 视频流启动但看不到画面

**解决方案：**
- 当前实现中，视频流是发送到服务器，不在客户端显示
- 检查日志确认帧是否正常发送
- 在服务器端查看是否接收到视频帧

### 4. 测试用例失败

**问题：** pytest 测试失败

**解决方案：**
- 确认服务器正在运行
- 检查配置文件中的服务器地址
- 查看测试输出中的详细错误信息
- 确认网络连接正常

### 5. WebSocket 连接断开

**问题：** WebSocket 连接频繁断开

**解决方案：**
- 检查服务器 WebSocket 配置
- 确认路径格式正确（`/control/websocket/{imeiAndImsi}`）
- 查看服务器日志确认连接状态

### 6. 依赖包安装失败

**问题：** `pip install` 失败

**解决方案：**
- 使用国内镜像源：
  ```bash
  pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple
  ```
- 检查 Python 版本（建议 3.8+）
- 单独安装失败的包查看具体错误

## 测试报告

测试完成后，可以生成测试报告：

```bash
# 生成 HTML 测试报告
pytest tests/ --html=report.html --self-contained-html

# 生成覆盖率报告
pytest tests/ --cov=src --cov-report=html
```

报告文件会保存在项目根目录，可以用浏览器打开查看。

## 注意事项

1. **测试环境隔离**：建议在独立的测试环境中运行，避免影响生产环境
2. **数据清理**：测试完成后，可以使用"删除用户数据"接口清理测试数据
3. **资源释放**：测试完成后记得断开连接，释放资源
4. **日志查看**：测试过程中注意查看日志输出，及时发现问题
5. **性能监控**：长时间测试时注意监控服务器资源使用情况

## 下一步

测试完成后，可以：
- 查看测试报告分析测试结果
- 根据测试结果修复发现的问题
- 扩展测试用例覆盖更多场景
- 集成到 CI/CD 流程中
