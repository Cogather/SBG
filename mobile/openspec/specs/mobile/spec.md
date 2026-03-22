# Mobile 服务规格说明

## 概述

`mobile` 是云手机控制网关服务，负责将浏览器调试客户端通过 WebSocket 与后端云手机基础设施（GIDS 认证服务 + TCP 控制/媒体通道）相连接。

**技术栈：**
- Spring Boot 2.7.18，HTTP 端口 8088（提供静态前端）
- netty-websocket-spring-boot-starter 0.13.0，WebSocket 端口 40002
- Netty 4.1.109（TCP 通道）
- Hutool 5.8.30（HTTP、JSON、日志、线程）
- Java 17 编译目标，Java 25 运行时

---

## ADDED Requirements

### Requirement: WebSocket 端点连接
服务 SHALL 在 `ws://localhost:40002/app/websocket/{IMEI}_{IMSI}` 提供 WebSocket 端点。连接时 SHALL 从路径变量解析 IMEI 和 IMSI，并从查询参数 `gids_addr` 读取 GIDS 服务地址（默认 `http://127.0.0.1:9090`）。

#### Scenario: 正常建立连接
- **WHEN** 客户端以 `{IMEI}_{IMSI}` 格式连接 WebSocket 端点
- **THEN** 服务解析 IMEI 和 IMSI，初始化 BrowserContext，并将其绑定到 Session

#### Scenario: 携带 gids_addr 参数连接
- **WHEN** 客户端连接时携带查询参数 `gids_addr=http://x.x.x.x:port`
- **THEN** 服务使用该地址作为 GIDS 服务地址，覆盖默认值

#### Scenario: 连接异常或主动断开
- **WHEN** WebSocket 发生错误或客户端断开
- **THEN** 服务调用 `BrowserContext.close()` 释放所有资源（TCP 通道、心跳、Session）

---

### Requirement: 设备登录
收到 `type=login` 的 WebSocket 消息后，服务 SHALL 执行三步 GIDS HTTP 认证，成功后建立 TCP 控制通道和媒体通道，并启动心跳。

登录消息字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| cs | String | 画布尺寸，格式 `宽x高`，可选值：240x320、360x480、405x540、540x720 |
| dv | Integer | 设备类型，默认 2 |
| at | Integer | 应用类型：1=BBC, 2=TikTok, 3=Facebook, 4=YouTube, 5=Other |
| ga | String | GIDS 服务地址（可选，覆盖连接时的值） |

#### Scenario: 登录成功
- **WHEN** 三步 GIDS 认证均返回 code=200 或 code=0
- **THEN** 服务建立 TCP 控制通道，发送 LOGIN TLV，连接媒体通道，启动 15 秒心跳

#### Scenario: 登录失败
- **WHEN** 任意一步 GIDS 认证返回错误码
- **THEN** 服务向 WebSocket 客户端发送包含错误码和错误信息的 JSON 消息，并关闭连接

---

### Requirement: GIDS 三步登录认证
服务 SHALL 按顺序调用以下三个 GIDS HTTP 接口，每步使用相同的 `DeviceLoginRequest` 请求体（Content-Type: application/json）：

1. `POST {gidsAddr}/app-api/devicetcp/app/login/v1/gridLoginAuth`
2. `POST {gidsAddr}/app-api/devicetcp/app/login/v1/gridLoginAuthOpenBrowser`
3. `POST {gidsAddr}/app-api/devicetcp/app/login/v1/deviceLoginAuth`

第三步响应的 `data` 字段包含 `token` 和 `tcpAddr`，供后续 TCP 连接使用。

#### Scenario: 三步全部成功
- **WHEN** 三个接口均返回 code=200 或 code=0
- **THEN** 返回第三步的 data（含 token、tcpAddr），内部 code 置为 0

#### Scenario: 中途某步失败
- **WHEN** 任意一步返回非成功码
- **THEN** 立即停止后续步骤，返回该步的错误码和错误信息

---

### Requirement: TCP 控制通道
服务 SHALL 使用 Netty 建立到 `tcpAddr`（格式 `IP:PORT`）的 TCP 长连接作为控制通道，并通过自定义 TLV 协议通信。

**连接建立后立即发送 LOGIN TLV，包含以下 22 个字段：**

| TLV 字段 | ID | 类型 | 值来源 |
|----------|----|------|--------|
| TYPE | 1 | Integer | 1（LOGIN）|
| FACTORY | 2 | String | manufacturer |
| DEV_TYPE | 3 | String | model |
| IMSI | 4 | String | imsi |
| IMEI | 5 | String | imei |
| LCD_WIDTH | 6 | Integer | width |
| LCD_HEIGHT | 7 | Integer | height |
| APP_TYPE | 19 | Integer | appType |
| TOKEN | 21 | String | token |
| SESSION_ID | 22 | String | imei_imsi |
| APP_ID | 28 | Integer | appType |
| EXT_TYPE | 30 | String | extendModel |
| PLAT_TYPE | 29 | Integer | platform |
| PLAY_MODE | 37 | Integer | 1 |
| ABILITY | 42 | Integer | 1 |
| DEVICE_TYPE | 46 | Integer | deviceType |
| CLIENT_LANGUAGE | 45 | String | clientLanguage |
| AUD_TYPE | 8 | String | mp3 |
| AUD_SMPRATE | 17 | Integer | 46000 |
| AUD_CHANNEL | 18 | Integer | 1 |
| NETWORK_TYPE | 48 | Integer | 1 |
| URL_TYPE | 49 | String | 1 |

**心跳：** 每 15 秒发送 TYPE=2(HEARTBEATS) + SEQ=currentTimeMillis。

**接收消息处理：**
- ACK（type=7）：静默忽略
- RETURN_MEDIA（type=9）：读取 TCP_ADDR 字段，重新连接媒体通道到新地址
- RETURN_CONTROL（type=12）：读取 CTRL_RSP_ELM、CTRL_RSP_INFO、CONTENT、WRITE_TYPE，组装为 callback JSON 推送到 WebSocket 客户端

#### Scenario: 方向控制
- **WHEN** 收到 type=direction 的 WebSocket 消息，含 ct（控制类型）和 cv（控制值）
- **THEN** 向控制通道发送 TYPE=4(CONTROL) + CTRL_TYPE=ct + CTRL_VAL=cv + SESSION_ID TLV

| ct 值 | 动作 |
|-------|------|
| 1 | 上 |
| 2 | 下 |
| 3 | 左 |
| 4 | 右 |
| 5 | 确定(OK) |
| 10 | 菜单 |
| 11 | 返回 |

#### Scenario: 内容输入
- **WHEN** 收到 type=upload 的 WebSocket 消息，含 ut（上传类型）和 content（内容）
- **THEN** 向控制通道发送 TYPE=13(MESSAGE) + UPLOAD_TYPE=ut + CONTENT=content + SESSION_ID TLV

#### Scenario: 文件上传
- **WHEN** 收到 type=upload_file 的 WebSocket 消息，含 fa（文件地址）
- **THEN** 向控制通道发送 TYPE=16(UPLOAD_FILE) + FILE_ADDR=fa + SESSION_ID TLV

#### Scenario: 控制通道未就绪
- **WHEN** 控制通道为 null 或非活跃状态时收到控制消息
- **THEN** 静默丢弃该消息

#### Scenario: 收到 RETURN_CONTROL 响应
- **WHEN** 控制通道收到 type=12(RETURN_CONTROL) 消息
- **THEN** 向 WebSocket 客户端推送 JSON: type=callback，包含 elm、info、content、wt 字段

---

### Requirement: TCP 媒体通道
服务 SHALL 使用 Netty 建立独立的 TCP 媒体通道，初始地址与控制通道相同，可通过 RETURN_MEDIA 消息动态切换。媒体通道连接成功后 SHALL 立即发送与控制通道相同格式的 LOGIN TLV（22 字段）。

**媒体帧推送格式：**

| 类型 | 帧格式 |
|------|--------|
| 视频（type=6）| [0x01, frameType] + H.264字节 |
| 音频（type=5）| [0x02] + MP3字节 |

frameType：1=关键帧（I 帧），2=非关键帧（P/B 帧）。

#### Scenario: 接收视频帧
- **WHEN** 媒体通道收到 type=6(VIDEO) 的 TLV 消息
- **THEN** 组装 [0x01, frameType] + videoBytes 二进制帧，通过 WebSocket sendBinary 推送给客户端

#### Scenario: 接收音频帧
- **WHEN** 媒体通道收到 type=5(AUDIO) 的 TLV 消息
- **THEN** 组装 [0x02] + audioBytes 二进制帧，通过 WebSocket sendBinary 推送给客户端

#### Scenario: 媒体通道地址变更
- **WHEN** 控制通道收到 RETURN_MEDIA 消息，携带新的 TCP_ADDR
- **THEN** 关闭当前媒体通道，重新连接到新地址并重新登录

---

### Requirement: TLV 二进制协议
所有 TCP 通信 SHALL 使用自定义 TLV 二进制协议，格式如下：

```
[magic: "mu" 2字节] [count: int 4字节] [dataLen: int 4字节] [TLV条目 × count]
每条 TLV: [key: int 4字节] [len: int 4字节] [value: len字节]
```

所有整数采用大端字节序。字符串字段以 UTF-8 编码存储。AUDIO_DATA（ID=15）和 VIDEO_DATA（ID=16）字段 SHALL 以原始 byte[] 存储，不得进行 UTF-8 解码。

#### Scenario: 解码完整数据包
- **WHEN** TCP 接收到以 `mu` 开头、长度足够的字节流
- **THEN** 解析 count 和 dataLen，读取对应字节，按 TLV 结构解析所有字段

#### Scenario: 数据包不完整
- **WHEN** 可读字节数小于 header（10字节）或小于声明的 dataLen
- **THEN** 重置读取索引，等待更多数据到达

#### Scenario: 编码发送数据包
- **WHEN** 调用 channel.writeAndFlush(TlvData)
- **THEN** TlvEncoder 将 TlvData 中所有字段按类型（String/Integer/Long/byte[]/ByteBuf）序列化为 TLV 格式，写入 mu 魔数 + count + dataLen + 数据体

---

### Requirement: 埋点事件上报
服务 SHALL 支持向 GIDS 上报两类客户端埋点事件，均通过异步线程执行，不阻塞 WebSocket 消息处理。

**上报繁忙/错误事件（send_error 消息）：**
依次调用 `POST {gidsAddr}/app-api/center/public/client/sendClientEvent`，first type=1（繁忙），then type=2（错误）。

请求体字段：hsman、hstype、appType、imei、imsi、type。Content-Type: application/json。

**上报使用时长（send_time 消息）：**
调用 `POST {gidsAddr}/app-api/center/public/client/sendAppUseTimesEvent`，Content-Type: application/octet-stream。

请求体字段：useTimes=100000、hsman、hstype、appType、appId、scheight、scwidth、exttype、imei、imsi、playMode=1。

#### Scenario: 上报错误事件
- **WHEN** 收到 type=send_error 的 WebSocket 消息
- **THEN** 异步依次发送 type=1 和 type=2 的 sendClientEvent 请求到 GIDS

#### Scenario: 上报使用时长
- **WHEN** 收到 type=send_time 的 WebSocket 消息
- **THEN** 异步发送 useTimes=100000 的 sendAppUseTimesEvent 请求到 GIDS

---

### Requirement: 浏览器调试前端
服务 SHALL 通过 HTTP 8088 端口提供静态 HTML 调试前端（`/index.html`），供本地开发调试使用。

前端 SHALL 包含以下功能：
- GIDS 地址输入框（默认 http://127.0.0.1:9090）、连接/断开按钮
- 会话配置：画布尺寸选择（240x320/360x480/405x540/540x720）、输入模式、应用类型
- Canvas 显示视频帧，使用 WebCodecs VideoDecoder（H.264 avc1.42E01E）解码，分辨率随所选画布尺寸动态调整
- Web Audio API 播放 MP3 音频流
- D-Pad 方向键（上下左右/OK/菜单/返回）
- 数字键盘（0-9、*、#）
- 文本输入框 + 发送按钮，上传类型选择
- 文件地址输入 + 上传文件按钮
- 埋点按钮：上报错误、上报使用时长
- GitHub Dark 配色主题

#### Scenario: 连接并登录
- **WHEN** 用户填写 GIDS 地址并点击连接按钮
- **THEN** 前端建立 WebSocket 连接，连接成功后发送 login 消息，Canvas 尺寸更新为所选分辨率，VideoDecoder 重新初始化

#### Scenario: 接收视频帧并渲染
- **WHEN** WebSocket 收到首字节为 0x01 的二进制消息
- **THEN** 前端使用 VideoDecoder 解码 H.264 数据，将帧绘制到 Canvas

#### Scenario: 接收音频帧并播放
- **WHEN** WebSocket 收到首字节为 0x02 的二进制消息
- **THEN** 前端使用 Web Audio API 解码 MP3 数据并播放

#### Scenario: 收到登录失败响应
- **WHEN** WebSocket 收到 code != 0 的文本消息
- **THEN** 前端弹出错误提示，状态重置为未连接

#### Scenario: 收到 callback 响应
- **WHEN** WebSocket 收到 type=callback 的文本消息
- **THEN** 前端在控制台输出 callback 详情（elm、info、content、wt）
