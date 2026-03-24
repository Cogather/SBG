## ADDED Requirements

### Requirement: mobile 通过 TCP 控制流发送操作指令
mobile SHALL 在登录完成后通过 TCP 控制流向 BrowserGateway 发送用户操作指令（上/下/左/右/确认/返回等），BrowserGateway 须确认指令接收。

#### Scenario: 方向键指令发送成功
- **WHEN** mobile 通过 TCP 控制流发送方向键（上/下/左/右）操作指令
- **THEN** BrowserGateway 确认接收，日志记录指令类型和内容

#### Scenario: 确认/返回指令发送成功
- **WHEN** mobile 通过 TCP 控制流发送确认或返回操作指令
- **THEN** BrowserGateway 确认接收，日志记录指令类型

### Requirement: BrowserGateway 将操作指令转发至 SDK
BrowserGateway SHALL 将收到的用户操作指令解析后转发给 Muen SDK，SDK 须将指令进一步转发至 browser-proxy。

#### Scenario: 指令经 SDK 转发至 browser-proxy
- **WHEN** BrowserGateway 收到操作指令并转发给 SDK
- **THEN** SDK 回调触发，browser-proxy 收到对应的浏览器控制请求（如鼠标点击、键盘输入、滚动等 CDP 操作）

### Requirement: browser-proxy 驱动 Chrome 执行操作
browser-proxy SHALL 通过 CDP 接口驱动 Chrome 执行对应的用户操作，操作结果须回调给 SDK。

#### Scenario: Chrome 执行操作成功
- **WHEN** browser-proxy 收到操作指令
- **THEN** Chrome 通过 CDP 执行对应操作（模拟触控/点击/滚动），browser-proxy 返回执行成功结果

#### Scenario: 操作结果回调至 BrowserGateway
- **WHEN** Chrome 操作执行完成
- **THEN** SDK 回调 BrowserGateway，BrowserGateway 通过 TCP 控制流向 mobile 发送操作结果标记（如 `file_upload_triggered`、`text_input_required`、`back_event` 等）

### Requirement: 操作后录屏数据实时更新并回传 mobile
操作执行后，record 扩展 SHALL 持续推送更新后的录屏帧，BrowserGateway 须将更新帧经 TCP 媒体流转发给 mobile，mobile 可通过画面变化感知操作生效。

#### Scenario: 操作后画面更新回传
- **WHEN** Chrome 执行操作后页面发生变化
- **THEN** record 扩展推送更新的视频帧，BrowserGateway 经 TCP 媒体流转发，mobile 收到包含操作结果的新画面帧

### Requirement: GIDS Mock 覆盖 BrowserGateway 回调所需全部接口
GIDS Mock SHALL 实现 BrowserGateway 在登录及运行期间回调的全部接口，返回符合协议的响应，确保 BrowserGateway 无因 GIDS 接口缺失导致的报错。

#### Scenario: user-bind 接口响应正确
- **WHEN** BrowserGateway 调用 GIDS Mock `GET /user-bind/v1/{sessionID}`
- **THEN** Mock 返回含 IMEI/IMSI 的用户绑定信息，HTTP 200

#### Scenario: plugin 配置接口响应正确
- **WHEN** BrowserGateway 调用 GIDS Mock `GET /plugin/v1/current`
- **THEN** Mock 返回含 Chrome 启动参数和扩展路径的插件配置，HTTP 200

#### Scenario: stats 上报接口接收成功
- **WHEN** BrowserGateway 向 GIDS Mock 上报流量或会话统计
- **THEN** Mock 返回 HTTP 200，不影响 BrowserGateway 主流程
