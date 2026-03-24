## ADDED Requirements

### Requirement: mobile 建立 TCP 控制流连接
mobile SHALL 通过 TCP 连接 BrowserGateway 控制端口（30001），连接建立后 BrowserGateway 须确认连接成功。

#### Scenario: TCP 控制流建立成功
- **WHEN** mobile 发起 TCP 连接到 BrowserGateway 端口 30001
- **THEN** 连接建立成功，BrowserGateway 日志记录新客户端连接

### Requirement: GIDS 三步鉴权流程完成
BrowserGateway SHALL 代表 mobile 向 GIDS Mock 完成三步鉴权：gridLoginAuth → gridLoginAuthOpenBrowser → deviceLoginAuth，每步须收到成功响应后再进行下一步。

#### Scenario: 三步鉴权全部成功
- **WHEN** mobile 通过 TCP 控制流发起登录请求（携带 IMEI/IMSI 和 Token）
- **THEN** BrowserGateway 依次调用 GIDS Mock 三个接口均返回成功，BrowserGateway 日志记录每步完成

#### Scenario: GIDS Mock 返回实例配置
- **WHEN** deviceLoginAuth 成功
- **THEN** GIDS Mock 返回含媒体流参数的实例配置（分辨率、帧率、码率、录屏模式等），BrowserGateway 解析配置无报错

### Requirement: browser-proxy 启动 Chrome 并加载 record 扩展
BrowserGateway SHALL 通过 browser-proxy API（`POST /api/browsers`）创建浏览器实例，Chrome 启动时须加载 record 扩展（`muen-v0.0.22`，扩展 ID: `majikpeglnhefidjkmpeipdbikkfbmho`）。

#### Scenario: Chrome 通过 browser-proxy 启动成功
- **WHEN** BrowserGateway 向 browser-proxy 发起创建浏览器请求
- **THEN** browser-proxy 返回浏览器实例 ID，Chrome 进程启动，record 扩展加载完成

### Requirement: record 扩展建立 WebSocket 推送录屏
record 扩展 SHALL 在 Chrome 启动后自动连接 BrowserGateway 媒体 WebSocket 端点（`/browser/websocket/{imeiAndImsi}`），并持续推送录屏数据（视频帧 + 音频帧）。

#### Scenario: 录屏 WebSocket 连接建立
- **WHEN** Chrome 加载 record 扩展后
- **THEN** record 扩展与 BrowserGateway 建立 WebSocket 连接，BrowserGateway 日志记录连接建立

#### Scenario: 录屏数据持续推送
- **WHEN** WebSocket 连接建立后
- **THEN** BrowserGateway 持续收到 record 扩展推送的视频帧（H.264）和音频帧（MP3）数据

### Requirement: BrowserGateway 向 mobile 推送媒体流地址并建立 TCP 媒体流
BrowserGateway SHALL 通过 TCP 控制流向 mobile 推送 TCP 媒体流地址（端口 30011），mobile 建立 TCP 媒体流连接后 BrowserGateway 须持续转发录屏数据。

#### Scenario: 媒体流地址推送和连接建立
- **WHEN** 录屏 WebSocket 连接就绪
- **THEN** BrowserGateway 向 mobile 推送媒体流地址，mobile 建立 TCP 媒体流连接（端口 30011）成功

#### Scenario: 录屏数据经媒体流回传 mobile
- **WHEN** TCP 媒体流连接建立后
- **THEN** mobile 持续收到视频帧（格式：`[0x01, frameType] + H.264`）和音频帧（格式：`[0x02] + MP3`）数据
