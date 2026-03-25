# Mobile 端到端测试指南

## 架构概览

```
手机浏览器
  └─ WebSocket ws://localhost:40002/app/websocket/{IMEI}_{IMSI}
        └─ mobile 服务 (8088/40002)
              ├─ HTTP → GIDS Mock (9090)         登录三步鉴权
              └─ TCP  → browser-gateway (30001)  控制通道
                            └─ browser-proxy (8000)  Chrome 驱动
```

## 前置条件

| 依赖 | 版本要求 |
|------|----------|
| Java | 17+（运行时 Java 21+） |
| Maven | 3.6+ |
| Python | 3.9+ |
| Chrome | 已安装，路径见 browser-gateway 配置 |

### Python 虚拟环境（首次运行需创建）

```bat
# GIDS Mock 虚拟环境
cd Test\mock-servers
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt

# browser-proxy 虚拟环境
cd BrowserGateway\BrowserGateway\browser-proxy
python -m venv .venv
.venv\Scripts\activate
pip install -e .
```

---

## 服务启动说明

### 一键启动（推荐）

```bat
# 在仓库根目录执行，依次启动全部 4 个服务
start-dev.bat
```

启动顺序：GIDS Mock → browser-proxy → mobile → browser-gateway，每个服务在独立 cmd 窗口中运行。

---

### 手动启动各服务

#### 1. GIDS Mock Server（端口 9090）

```bat
cd Test\mock-servers
venv\Scripts\activate
python mock\gids_mock_server.py
```

**提供的接口：**

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/app-api/devicetcp/app/login/v1/gridLoginAuth` | 登录第一步 |
| POST | `/app-api/devicetcp/app/login/v1/gridLoginAuthOpenBrowser` | 登录第二步 |
| POST | `/app-api/devicetcp/app/login/v1/deviceLoginAuth` | 登录第三步，返回 `tcpAddr: 127.0.0.1:30001` |
| GET  | `/config/v1` | 返回 urlConfigList（各 App 类型映射） |
| POST | `/app-api/center/public/client/sendClientEvent` | 接收错误/使用时长埋点 |
| POST | `/app-api/control/file/upload` | 接收文件上传，返回服务器路径 |
| GET  | `/health` | 健康检查 |

**AppType 映射（urlConfigList 顺序）：**

| appType | 名称 | URL |
|---------|------|-----|
| 1 | Youtube | https://m.youtube.com |
| 2 | TikTok | https://www.tiktok.com |
| 3 | FaceBook | https://www.facebook.com |
| 5 | BBC | https://www.bbc.com |
| 6 | Upload | about:blank |
| 8 | Ins | https://www.instagram.com |
| 10 | SNAPCHAT | https://www.snapchat.com |
| 12 | Google | https://www.google.com |
| 18 | CNN | https://www.cnn.com |
| 920425 | TELE | https://web.telegram.org |

---

#### 2. browser-proxy（端口 8000）

```bat
cd BrowserGateway\BrowserGateway\browser-proxy
.venv\Scripts\activate
python -m browser_proxy.main --port 8000
```

或使用快捷脚本：
```bat
BrowserGateway\BrowserGateway\browser-gateway\start-browser-proxy.bat
```

**作用：** 驱动本地 Chrome 浏览器，接收来自 browser-gateway 的操作指令。

---

#### 3. mobile 服务（HTTP 8088 / WebSocket 40002）

```bat
cd mobile
mvn spring-boot:run
```

**配置文件：** `mobile/src/main/resources/application.properties`

```properties
spring.application.name=mobile
server.port=8088
server.address=0.0.0.0
```

**默认 GIDS 地址：** `http://127.0.0.1:9090`（可在前端界面覆盖）

**WebSocket 端点：** `ws://localhost:40002/app/websocket/{IMEI}_{IMSI}`

---

#### 4. browser-gateway（端口 8090 / TCP 30001/30002）

```bat
# 方式一：Maven 启动（开发调试）
cd BrowserGateway\BrowserGateway\browser-gateway
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 方式二：JAR 启动
BrowserGateway\BrowserGateway\browser-gateway\start-local.bat
```

**配置文件：** `application-local.yaml`（local profile）

| 配置项 | 值 | 说明 |
|--------|----|------|
| `server.port` | 8090 | HTTP 管理端口 |
| `browsergw.tcp.control-port` | 30001 | 控制通道 TCP 端口 |
| `browsergw.tcp.media-port` | 30011 | 媒体通道 TCP 端口 |
| `browsergw.chrome.endpoint` | http://127.0.0.1:8000 | browser-proxy 地址 |
| `browsergw.chrome.executable-path` | D:/Program Files/chrome-win64/... | Chrome 可执行文件路径 |
| `browsergw.chrome.headless` | false | 本地调试建议关闭 headless |
| `gids.endpoint` | localhost:9090 | GIDS 地址 |

> **注意：** 如果本地没有 MuenDriver SDK JAR，需在 `application-local.yaml` 中配置 `browsergw.plugin.stub-mode: true` 启用 Stub 模式（仅打印日志，不真正驱动浏览器）。

---

## 使用 mobile 前端测试

### 访问地址

```
http://localhost:8088/index.html
```

### 操作步骤

1. **配置会话参数**（右侧「会话配置」面板）
   - 分辨率：选择 `240×320`（默认）
   - 输入模式：`按键`（keypad）或 `触屏`（touchscreen）
   - 应用类型：选择要测试的网站，如 `BBC`（appType=5）

2. **填写 GIDS 地址**（左下角输入框）
   - 默认值：`http://127.0.0.1:9090`
   - 若 GIDS Mock 在其他端口，修改此处

3. **点击「连接」按钮**
   - mobile 向 GIDS Mock 发起三步登录
   - 获取 `tcpAddr`（`127.0.0.1:30001`）并建立 TCP 控制通道
   - 连接成功后状态点变绿，Canvas 开始显示云浏览器画面

4. **测试方向控制**
   - 点击 ↑↓←→ / OK 按钮，云浏览器中的页面应相应滚动/响应
   - TLV 消息格式：`ct=0`，cv 值：up=12, down=13, left=14, right=15, ok=20

5. **测试功能键**
   - 「菜单」→ ct=0, cv=17
   - 「返回」→ ct=0, cv=18

6. **测试数字键盘**（0-9）
   - 发送 `{type:'direction', ct:0, cv:数字}`

7. **测试触屏模式**
   - 输入模式选「触屏」后重新连接
   - 在 Canvas 上鼠标按下/移动/抬起，发送触摸事件

8. **断开连接**：点击「断开」按钮

---

## 日志排查

### 检查 mobile 日志（控制通道连接）
```
[INFO] success to connect edge control, begin to send login message
[INFO] receive ack :1, code is: 200
[INFO] send control message type:4
[INFO] success to wait ack for type:4
```

若出现 `timeout waiting for ack`，说明 browser-gateway 未回 ACK（5 秒超时）。

### 检查 GIDS Mock 日志（登录流程）
```
POST gridLoginAuth: imei=6258412454025411, imsi=68510155565211
POST gridLoginAuthOpenBrowser: imei=6258412454025411
POST deviceLoginAuth: imei=..., session_id=6258412454025411_68510155565211
```

### 检查 browser-gateway 日志
```
[INFO] processDefault: sessionId=6258412454025411_68510155565211
[INFO] remote.handleEvent called
```

若出现 `user browser instance not exists`，说明插件未加载成功，需检查 MuenDriver SDK 或启用 stub-mode。

---

## 常见问题

| 现象 | 原因 | 解决 |
|------|------|------|
| 连接按钮后状态一直「connecting」 | GIDS Mock 未启动或端口错误 | 确认 9090 端口正常，检查 GIDS 日志 |
| Canvas 无画面 | browser-gateway/proxy 未启动 | 检查 30001/8000 端口 |
| 方向按钮无响应 | browser-gateway 插件未加载 | 启用 stub-mode 或准备 SDK JAR |
| `timeout waiting for ack` | browser-gateway 未回 ACK | 检查控制通道是否正常建立 |
| 音频无声音 | AudioContext 被浏览器挂起 | 先点击页面任意处再连接 |
