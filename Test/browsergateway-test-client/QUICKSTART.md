# 快速开始指南

## 安装步骤

### 1. 创建虚拟环境（推荐）

```bash
# Windows
python -m venv venv
venv\Scripts\activate

# Linux/Mac
python3 -m venv venv
source venv/bin/activate
```

### 2. 安装依赖

```bash
pip install -r requirements.txt
```

### 3. 配置服务器地址

编辑 `config/config.yaml`，设置 BrowserGateway 服务器的地址和端口：

```yaml
server:
  address: "127.0.0.1"  # 修改为实际服务器地址
  http_port: 8090
  websocket:
    media_port: 30002
    control_port: 30005
  tcp:
    control_tls_port: 30001
    media_tls_port: 30003
```

### 4. 启动 Web 界面

```bash
python scripts/run_test.py
```

然后在浏览器中访问：`http://localhost:8000`

## 使用说明

### Web 界面功能

1. **配置连接**
   - 设置服务器地址、端口
   - 输入 IMEI 和 IMSI
   - 选择协议类型（TCP 控制流/媒体流、WebSocket 控制流/媒体流）

2. **建立连接**
   - 点击"连接"按钮
   - 查看连接状态

3. **发送控制事件**
   - 使用虚拟按键发送按键事件
   - 在触摸屏区域点击/拖动发送触摸事件

4. **视频流测试**
   - 点击"开始视频流"按钮开始发送模拟视频流
   - 点击"停止视频流"按钮停止

5. **REST API 测试**
   - 使用 API 测试按钮测试各种 REST API 接口

### 运行测试用例

```bash
# 运行所有测试
pytest tests/

# 运行特定测试
pytest tests/test_tcp_control.py
pytest tests/test_tcp_media.py
pytest tests/test_websocket_control.py
pytest tests/test_websocket_media.py
pytest tests/test_rest_api.py

# 显示详细输出
pytest tests/ -v
```

## 注意事项

1. **TLS 证书验证**：测试客户端默认不验证 TLS 证书（用于测试环境）

2. **视频编码**：当前使用 JPEG 编码作为占位符，实际项目中应使用真正的 H.264 编码器（如 ffmpeg-python）

3. **服务器要求**：确保 BrowserGateway 服务器已启动并运行在配置的地址和端口上

4. **防火墙**：确保防火墙允许连接到服务器端口

## 故障排除

### 连接失败

- 检查服务器地址和端口是否正确
- 确认 BrowserGateway 服务已启动
- 检查网络连接和防火墙设置

### 视频流不显示

- 检查媒体流连接是否成功建立
- 查看浏览器控制台是否有错误信息
- 确认视频生成器是否正常启动

### 测试用例失败

- 确认服务器正在运行
- 检查配置中的服务器地址和端口
- 查看测试输出中的错误信息
