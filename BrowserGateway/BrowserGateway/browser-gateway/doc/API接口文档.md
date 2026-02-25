# BrowserGateway API 接口手册

## 文档说明

本文档详细描述了 BrowserGateway 服务的所有 API 接口，包括 REST API、WebSocket、TCP 服务、内部接口和外部调用接口。

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2026-01-01 | 初始版本 |

---

## 目录

1. [HTTP REST API 接口](#1-http-rest-api-接口)
2. [WebSocket 实时通信接口](#2-websocket-实时通信接口)
3. [TCP 服务接口](#3-tcp-服务接口)
4. [内部服务接口](#4-内部服务接口)
5. [外部 API 接口调用](#5-外部-api-接口调用)
6. [定时任务接口](#6-定时任务接口)
7. [数据格式详解](#7-数据格式详解)
8. [接口调用流程](#8-接口调用流程)

---

## 1. HTTP REST API 接口

### 1.1 ChromeApi - 浏览器管理

#### 1.1.1 删除用户数据

- **URL**: `DELETE /browsergw/browser/userdata/delete`
- **功能说明**: 删除指定用户的浏览器实例和用户数据，如用户存在浏览器实例会先关闭再删除
- **服务ID**: `browser`
- **Content-Type**: `application/json`

**请求参数**:

```json
{
  "imei": "string",      // 用户设备 IMEI 号，必填
  "imsi": "string"       // 用户设备 IMSI 号，必填
}
```

**响应格式**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "imei": "string",
    "imsi": "string"
  }
}
```

**调用场景**: 用户退出登录或管理员清除用户数据时的强制删除操作

**注意事项**:
- 如果用户存在浏览器实例，会先关闭实例再删除数据
- 用户数据包括浏览器配置、缓存、Cookie 等

---

#### 1.1.2 预开浏览器

- **URL**: `POST /browsergw/browser/preOpen`
- **功能说明**: 预创建浏览器实例但不启动，用于提高用户首次连接的响应速度
- **服务ID**: `browser`
- **Content-Type**: `application/json`

**请求参数**:

```json
{
  "factory": "string",        // 设备厂商
  "dev_type": "string",       // 设备型号
  "ext_type": "string",       // 扩展类型
  "plat_type": number,        // 平台类型: 0-Android, 1-iOS
  "lcd_width": number,        // 屏幕宽度
  "lcd_height": number,       // 屏幕高度
  "app_type": number,         // 应用类型
  "appid": number,            // 应用ID
  "imsi": "string",           // 用户设备IMSI号，必填
  "imei": "string",           // 用户设备IMEI号，必填
  "device_type": number,      // 设备类型
  "client_language": "string",// 客户端语言: "en", "zh"
  "play_mode": number,        // 播放模式
  "innerMediaEndpoint": "string" // 内部媒体端点，系统自动填充
}
```

**响应格式**:

```json
{
  "code": 200,
  "message": "success",
  "data": "success"           // 或错误信息
}
```

**调用场景**: 用户登录前预先创建浏览器实例，减少用户实际连接时的等待时间

**注意事项**:
- `innerMediaEndpoint` 由系统自动填充，无需客户端提供
- 预开浏览器后会占用资源，建议在用户即将连接时调用
- 预开的浏览器实例会在一定时间内无操作后自动关闭

---

### 1.2 ExtensionManageApi - 扩展管理

#### 1.2.1 加载扩展

- **URL**: `POST /browsergw/extension/load`
- **功能说明**: 加载或更新浏览器扩展插件，需要删除所有现有浏览器实例并重新加载
- **服务ID**: `extension`
- **Content-Type**: `application/json`

**请求参数**:

```json
{
  "name": "string",           // 插件名称，必填
  "version": "string",        // 插件版本，必填
  "bucketName": "string",     // S3存储桶名称，必填
  "extensionFilePath": "string", // 扩展文件路径，必填
  "type": "string",           // 插件类型，可选: "ChromeExtend"
  "packageName": "string"     // 包名，可选
}
```

**响应格式**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "bucketName": "string",
    "extensionFilePath": "string"
  }
}
```

**调用场景**: 服务启动后更新浏览器扩展，或管理员动态更换扩展版本

**注意事项**:
- 加载新扩展前会关闭所有现有浏览器实例
- 扩展文件从 S3 存储下载
- 支持失败重试机制

---

#### 1.2.2 获取插件信息

- **URL**: `GET /browsergw/extension/pluginInfo`
- **功能说明**: 获取当前激活的浏览器扩展插件的详细信息
- **服务ID**: `extension`

**请求参数**: 无

**响应格式**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "name": "string",              // 插件名称
    "version": "string",           // 插件版本
    "type": "string",              // 插件类型
    "status": "string",            // 插件状态: "ACTIVE", "INACTIVE", "LOADING"
    "bucketName": "string",        // 存储桶名称
    "packageName": "string",       // 包名
    "loadTime": "string"           // 加载时间
  }
}
```

**调用场景**: 查询当前使用的插件版本和状态，或用于问题排查

---

## 2. WebSocket 实时通信接口

### 2.1 Muen 代理 WebSocket

#### 基本信息

- **URL**: `ws://{server.address}:{muen-port}/control/websocket/{imeiAndImsi}`
- **端口**: `${browsergw.websocket.muen-port}` (默认 30005)
- **路径参数**:
  - `imeiAndImsi`: 用户唯一标识 (IMEI + IMSI 组合字符串)

#### 功能说明

作为 Muen SDK 与浏览器扩展之间的代理 WebSocket 服务器，实现双向通信和消息转发

#### 连接配置

| 配置项 | 说明 | 默认值 |
|--------|------|---------|
| boss线程数 | BOSS事件循环线程数 | 8 |
| worker线程数 | Worker事件循环线程数 | 64 |
| 最大帧长度 | 单条消息最大长度 | 655360 字节 |
| 心跳超时 | 无心跳断开连接时间 | 300000ms (5分钟) |

#### 连接流程

1. 客户端建立 WebSocket 连接
2. 服务端接收连接，创建代理会话
3. 建立与 Muen SDK 的双向通信通道
4. 开始双向消息转发
5. 客户端断开连接时清理会话

#### 消息协议

**消息类型**:
- **文本消息**: JSON 格式的控制命令和事件通知
- **二进制消息**: 预留的二进制数据处理（暂未使用）

**消息格式**:
```json
{
  "type": "string",      // 消息类型
  "payload": object      // 消息负载
}
```

#### 使用示例

**客户端连接示例**:
```javascript
// 建立连接
const ws = new WebSocket('ws://127.0.0.1:30005/control/websocket/123456789012345_987654321098765');

// 连接成功
ws.onopen = function(event) {
  console.log('WebSocket连接已建立');
};

// 发送控制命令
ws.send(JSON.stringify({
  type: 'COMMAND',
  payload: {
    action: 'screen_touch',
    x: 100,
    y: 200,
    timestamp: Date.now()
  }
}));

// 接收消息
ws.onmessage = function(event) {
  const message = JSON.parse(event.data);
  console.log('收到消息:', message);

  if (message.type === 'EVENT') {
    // 处理事件消息
    handleEvent(message.payload);
  }
};

// 连接关闭
ws.onclose = function(event) {
  console.log('WebSocket连接已关闭，代码:', event.code);
};

// 连接错误
ws.onerror = function(error) {
  console.error('WebSocket错误:', error);
};
```

---

### 2.2 媒体流 WebSocket

#### 基本信息

- **URL**: `ws://{server.address}:{media-port}/browser/websocket/{imeiAndImsi}`
- **端口**: `${browsergw.websocket.media-port}` (默认 30002)
- **路径参数**:
  - `imeiAndImsi`: 用户唯一标识 (IMEI + IMSI 组合字符串)

#### 功能说明

处理音频和视频媒体的实时传输，支持屏幕共享和远程控制。提供两种编解码模式：
- **WebCodecs 模式**: 使用浏览器原生 WebCodecs API
- **FFmpeg 模式**: 使用 FFmpeg 进行编解码 (默认模式)

#### 连接流程

1. 客户端建立 WebSocket 连接
2. 客户端发送初始化参数配置
3. 服务端启动媒体处理器
4. 开始接收和发送媒体流
5. 支持实时参数调整

#### 初始化参数

客户端连接后必须发送初始化参数:

```json
{
  "width": 1920,              // 视频宽度，默认: 1920
  "height": 1080,             // 视频高度，默认: 1080
  "frameRate": 30,            // 帧率 FPS，默认: 30
  "sampleRate": 44100,        // 采样率 Hz，默认: 44100
  "channels": 2,              // 音频通道数，默认: 2
  "bitRate": 128,             // 音频比特率 kbps，默认: 128
  "gopSize": 29,              // GOP 大小，默认: 29
  "dropFrameMulti": 0.0,      // 丢帧倍数，0-1.0，默认: 0.0
  "codecType": "H264"         // 视频编码格式: H264/H265，默认: H264
}
```

#### 消息处理

**文本消息**:
- 用途: 传输配置参数和控制命令
- 格式: JSON 字符串

**二进制消息**:
- 用途: 传输媒体流数据
- 格式: 视频帧 (H.264/H.265) 或音频数据 (AAC)
- 大小: 不超过 655360 字节

#### 媒体流数据格式

**视频帧**:
```
[4字节: 帧类型][4字节: 时间戳][4字节: 数据长度][N字节: 数据]
```

- 帧类型: 1-I帧, 2-P帧, 3-Audio帧
- 时间戳: Unix时间戳/毫秒
- 数据长度: 实际数据字节数

**音频数据**:
```
[4字节: 帧类型][4字节: 时间戳][4字节: 数据长度][N字节: 数据]
```

#### 使用示例

**客户端连接示例**:
```javascript
// 建立连接
const ws = new WebSocket('ws://127.0.0.1:30002/browser/websocket/imei123_imsi456');

// 连接成功，发送初始化参数
ws.onopen = function(event) {
  ws.send(JSON.stringify({
    width: 1920,
    height: 1080,
    frameRate: 30,
    sampleRate: 44100,
    channels: 2,
    bitRate: 128,
    gopSize: 29,
    dropFrameMulti: 0.0,
    codecType: 'H264'
  }));
};

// 接收二进制媒体数据
ws.onmessage = function(event) {
  if (event.data instanceof Blob) {
    // 处理二进制媒体数据
    const reader = new FileReader();
    reader.onload = function(e) {
      const arrayBuffer = e.target.result;
      processMediaData(arrayBuffer);
    };
    reader.readAsArrayBuffer(event.data);
  } else {
    // 处理文本控制信息
    const control = JSON.parse(event.data);
    handleControlMessage(control);
  }
};

// 发送媒体数据
function sendMediaData(data) {
  ws.send(data);  // 发送二进制数据
};

// 调整视频参数
function adjustVideoConfig(config) {
  ws.send(JSON.stringify(config));
};
```

#### 性能调优建议

1. **帧率设置**: 根据网络带宽调整帧率，推荐 30fps
2. **GOP 大小**: 建议 29-30，在关键帧间隔和延迟间平衡
3. **丢帧策略**: 网络质量差时设置 `dropFrameMulti` > 0 主动丢帧
4. **编码格式**: H264 兼容性好，H265 带宽利用率更高

---

## 3. TCP 服务接口

### 3.1 控制流 TCP 服务

#### 基本信息

- **地址**: `${server.address}`
- **端口**: `${browsergw.tcp.control-tls-port}` (TLS加密)
- **协议**: TLS 加密的 TCP 长连接
- **心跳超时**: `${browsergw.tcp.heartbeat-ttl}` (默认 360000000000 ns)

#### 功能说明

处理控制命令传输，支持低延迟的控制指令下发。适用于以下场景:
- 键盘按键事件
- 触摸屏事件
- 系统控制命令

#### 消息协议 - TLV 格式

所有消息采用 TLV (Type-Length-Value) 编码格式:

```
┌──────────┬──────────┬──────────┐
│ Type(2B) │ Length(4B) │ Value(NB) │
└──────────┴──────────┴──────────┘
```

- **Type**: 2字节消息类型标识
- **Length**: 4字节消息内容长度 (大端序)
- **Value**: N字节消息内容

#### 消息类型定义

| 类型值 | 类型名称 | 十六进制 | 说明 |
|--------|---------|---------|------|
| 1 | LOGIN | 0x0001 | 登录请求，建立控制连接 |
| 2 | HEARTBEATS | 0x0002 | 心跳包，保持连接活跃 |
| 3 | KEY_EVENT | 0x0003 | 按键事件，模拟键盘输入 |
| 4 | TOUCH_EVENT | 0x0004 | 触摸事件，模拟触摸屏输入 |
| 5 | MOUSE_EVENT | 0x0005 | 鼠标事件，模拟鼠标输入 |
| 6 | DRAG_EVENT | 0x0006 | 拖拽事件 |
| 7 | TEXT_INPUT | 0x0007 | 文本输入事件 |
| 8 | CLIPBOARD | 0x0008 | 剪贴板操作 |
| 255 | LOGOUT | 0x00FF | 登出/断开连接 |
| 65280+ | CUSTOM | 0xFF00+ | 自定义消息 |

#### 消息格式示例

**登录消息 (LOGIN)**:
```
Type: 0x0001
Length: 取决于 payload 大小
Value: JSON 格式的登录信息
{
  "userId": "string",
  "timestamp": number,
  "token": "string"
}
```

**心跳消息 (HEARTBEATS)**:
```
Type: 0x0002
Length: 0
Value: 空
```

**按键事件 (KEY_EVENT)**:
```
Type: 0x0003
Length: 取决于 payload 大小
Value: JSON 格式的按键信息
{
  "keyCode": number,
  "action": "DOWN|UP|PRESS",
  "timestamp": number
}
```

**触摸事件 (TOUCH_EVENT)**:
```
Type: 0x0004
Length: 取决于 payload 大小
Value: JSON 格式的触摸信息
{
  "action": "DOWN|MOVE|UP",
  "x": number,
  "y": number,
  "timestamp": number
}
```

#### 连接管理

**建立连接**:
1. 客户端建立 TLS TCP 连接
2. 发送 LOGIN 消息进行认证
3. 服务端验证后建立会话
4. 开始定期发送 HEARTBEATS 保持连接

**心跳保护**:
- 客户端定期发送心跳包
- 服务端检测心跳超时
- 超时后自动断开连接

**断开连接**:
- 主动发送 LOGOUT 消息
- 关闭 TCP 连接
- 服务端清理会话资源

#### 使用示例

**Java 客户端示例**:
```java
public class ControlTcpClient {
    private Socket socket;
    private OutputStream out;
    private InputStream in;

    public void connect(String host, int port) throws IOException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        SSLSocketFactory factory = sslContext.getSocketFactory();
        socket = (SSLSocket) factory.createSocket(host, port);
        out = socket.getOutputStream();
        in = socket.getInputStream();
    }

    public void sendLogin(String userId, String token) throws IOException {
        String payload = "{\"userId\":\"" + userId + "\",\"token\":\"" + token + "\"}";
        sendMessage(0x0001, payload);
    }

    public void sendKeyEvent(int keyCode, String action) throws IOException {
        String payload = "{\"keyCode\":" + keyCode + ",\"action\":\"" + action + "\"}";
        sendMessage(0x0003, payload);
    }

    public void sendTouchEvent(String action, int x, int y) throws IOException {
        String payload = "{\"action\":\"" + action + "\",\"x\":" + x + ",\"y\":" + y + "}";
        sendMessage(0x0004, payload);
    }

    private void sendMessage(int type, String payload) throws IOException {
        byte[] value = payload.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(6 + value.length);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) type);
        buffer.putInt(value.length);
        buffer.put(value);
        out.write(buffer.array());
    }

    public void startHeartbeat() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sendMessage(0x0002, "");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 0, 30, TimeUnit.SECONDS);  // 30秒心跳
    }
}
```

---

### 3.2 媒体流 TCP 服务

#### 基本信息

- **地址**: `${server.address}`
- **端口**: `${browsergw.tcp.media-tls-port}` (TLS加密)
- **协议**: TLS 加密的 TCP 长连接
- **心跳超时**: `${browsergw.tcp.heartbeat-ttl}` (默认 360000000000 ns)

#### 功能说明

传输实时音频和视频数据，保证低延迟和高质量。适用于:
- 屏幕内容实时共享
- 音视频通话
- 多媒体播放控制

#### 消息协议

同样采用 TLV (Type-Length-Value) 格式，消息类型如下:

| 类型值 | 类型名称 | 说明 |
|--------|---------|------|
| 1 | LOGIN | 登录，建立媒体连接 |
| 2 | HEARTBEATS | 心跳包 |
| 10 | VIDEO_FRAME | 视频帧数据 |
| 11 | AUDIO_FRAME | 音频帧数据 |
| 12 | MEDIA_CONFIG | 媒体配置参数 |
| 255 | LOGOUT | 断开连接 |

#### 视频帧格式 (VIDEO_FRAME)

```
Type: 0x000A
Length: 数据长度
Value:
┌────────────┬────────────┬────────────┬────────────┐
│ Type(1B) │ Timestamp(4B) │ Length(4B) │ Data(NB) │
└────────────┴────────────┴────────────┴────────────┘
```

- Type: 1-I帧, 2-P帧, 3-B帧
- Timestamp: 时间戳 (毫秒)
- Length: 帧数据长度
- Data: H.264/H.265 编码的帧数据

#### 音频帧格式 (AUDIO_FRAME)

```
Type: 0x000B
Length: 数据长度
Value:
┌────────────┬────────────┬────────────┬────────────┐
│ Codec(1B) │ Timestamp(4B) │ Length(4B) │ Data(NB) │
└────────────┴────────────┴────────────┴────────────┘
```

- Codec: 1-AAC, 2-OPUS
- Timestamp: 时间戳 (毫秒)
- Length: 帧数据长度
- Data: 音频编码数据

---

## 4. 内部服务接口

### 4.1 远程服务 (IRemote)

#### 4.1.1 用户绑定相关

**获取用户绑定信息**
```
方法: getUserBind(String sessionID)
返回: UserBind
功能: 从远程服务获取用户的绑定信息
参数:
  - sessionID: 用户会话ID
```

**更新用户绑定**
```
方法: updateUserBind(String sessionID)
返回: UserBind
功能: 更新用户的绑定信息，包括端点地址等
参数:
  - sessionID: 用户会话ID
```

**使绑定过期**
```
方法: expiredUserBind(String sessionID)
返回: void
功能: 使用户绑定信息过期，释放资源
参数:
  - sessionID: 用户会话ID
```

#### 4.1.2 浏览器实例管理

**创建浏览器实例**
```
方法: createChrome(byte[] receivedControlPackets,
                  InitBrowserRequest parsedParams,
                  Consumer<Object> consumer)
返回: void
功能: 创建浏览器实例并初始化
参数:
  - receivedControlPackets: 控制包数据 (TLV编码)
  - parsedParams: 解析后的浏览器初始化参数
  - consumer: 回调函数，处理创建结果
```

**回退处理**
```
方法: fallback(String sessionID)
返回: void
功能: 服务降级回退处理
参数:
  - sessionID: 用户会话ID
```

**错误回退**
```
方法: fallbackByError(String sessionId)
返回: void
功能: 出错时的回退处理
参数:
  - sessionId: 用户会话ID
```

#### 4.1.3 数据上报接口

**发送媒体流量统计**
```
方法: sendTrafficMedia(String dataJson)
返回: void
功能: 上报媒体流量统计数据到远程监控服务
参数:
  - dataJson: 媒体流量统计数据的 JSON 字符串
```

**发送控制流量统计**
```
方法: sendTrafficControl(String dataJson)
返回: void
功能: 上报控制流量统计数据到远程监控服务
参数:
  - dataJson: 控制流量统计数据的 JSON 字符串
```

**发送会话信息**
```
方法: sendSession(String dataJson)
返回: void
功能: 上报会话开始和结束信息
参数:
  - dataJson: 会话信息的 JSON 字符串
```

**上报事件**
```
方法: <T> reportEvent(EventInfo<T> event)
返回: void
功能: 上报业务事件
参数:
  - event: 事件信息对象，包含事件类型和数据
```

#### 4.1.4 事件处理

**处理事件**
```
方法: handleEvent(byte[] receivedControlPackets, String userId)
返回: void
功能: 处理接收到的控制事件
参数:
  - receivedControlPackets: 控制包数据
  - userId: 用户ID
```

---

### 4.2 浏览器管理服务 (IChromeSet)

#### 4.2.1 实例 CRUD 操作

**创建用户浏览器实例**
```
方法: create(InitBrowserRequest request)
返回: UserChrome
功能: 创建新的用户浏览器实例
参数:
  - request: 浏览器初始化参数
```

**获取用户浏览器实例**
```
方法: get(String userId)
返回: UserChrome
功能: 获取指定用户的浏览器实例
参数:
  - userId: 用户ID
```

**删除用户浏览器实例**
```
方法: delete(String userId)
返回: void
功能: 删除指定用户的浏览器实例
参数:
  - userId: 用户ID
```

**为重启删除实例**
```
方法: deleteForRestart(String userId)
返回: void
功能: 为重启删除实例，保留部分数据
参数:
  - userId: 用户ID
```

**删除所有实例**
```
方法: deleteAll()
返回: void
功能: 删除所有浏览器实例
```

#### 4.2.2 状态管理

**获取所有用户**
```
方法: getAllUser()
返回: Set<String>
功能: 获取所有活跃用户ID集合
```

**更新心跳**
```
方法: updateHeartbeats(String userId, long heartbeats)
返回: void
功能: 更新指定用户的心跳时间
参数:
  - userId: 用户ID
  - heartbeats: 心跳时间戳
```

**获取心跳**
```
方法: getHeartbeats(String userId)
返回: long
功能: 获取指定用户的心跳时间
参数:
  - userId: 用户ID
```

#### 4.2.3 服务上报

**上报使用情况**
```
方法: reportUsed()
返回: void
功能: 上报浏览器实例使用统计
```

**上报链路端点**
```
方法: reportChainEndpoints()
返回: boolean
功能: 向服务中心上报服务链路端点信息
```

---

### 4.3 扩展管理服务

**加载扩展**
```
方法: loadExtension(LoadExtensionRequest param)
返回: boolean
功能: 加载或更新浏览器扩展
参数:
  - param: 扩展加载参数
```

**获取插件信息**
```
方法: getPluginInfo()
返回: PluginActive
功能: 获取当前激活的插件信息
```

---

### 4.4 文件存储服务

**上传文件**
```
方法: uploadFile(String localFilePath, String remoteUrl)
返回: void
功能: 上传本地文件到远程存储
参数:
  - localFilePath: 本地文件路径
  - remoteUrl: 远程存储URL
```

**下载文件**
```
方法: downloadFile(String localFilePath, String remoteUrl)
返回: void
功能: 从远程存储下载文件到本地
参数:
  - localFilePath: 本地文件路径
  - remoteUrl: 远程存储URL
```

**删除文件**
```
方法: deleteFile(String remoteUrl)
返回: void
功能: 从远程存储删除文件
参数:
  - remoteUrl: 远程存储URL
```

**检查文件存在**
```
方法: exist(String remoteUrl)
返回: boolean
功能: 检查远程存储中是否存在文件
参数:
  - remoteUrl: 远程存储URL
```

---

## 5. 外部 API 接口调用

### 5.1 用户绑定服务

#### 获取用户绑定
- **端点**: `GET {endpoint}/user-bind/v1/{sessionID}`
- **说明**: 查询用户的绑定信息
- **参数**:
  - `endpoint`: 从 CSE 服务发现获取的服务地址
  - `sessionID`: 用户会话ID
- **响应**:
```json
{
  "sessionID": "string",
  "endpoint": "string",
  "timestamp": number,
  "status": "ACTIVE|INACTIVE|EXPIRED"
}
```

#### 更新用户绑定
- **端点**: `PUT {endpoint}/user-bind/v1/update`
- **Content-Type**: `application/json`
- **请求体**:
```json
{
  "sessionID": "string",
  "endpoint": "string",
  "status": "string"
}
```

#### 使绑定过期
- **端点**: `DELETE {endpoint}/user-bind/v1/{sessionID}`
- **参数**:
  - `sessionID`: 用户会话ID

---

### 5.2 统计服务

#### 媒体流量上报
- **端点**: `POST {endpoint}/stats/v1/traffic/media`
- **Content-Type**: `application/json`
- **请求体**:
```json
{
  "sessionId": "string",
  "dataSize": number,
  "startTime": string,
  "endTime": string,
  "tcpUniqueId": "string"
}
```

#### 控制流量上报
- **端点**: `POST {endpoint}/stats/v1/traffic/control`
- **Content-Type**: `application/json`
- **请求体**:
```json
{
  "sessionId": "string",
  "dataSize": number,
  "startTime": string,
  "endTime": string,
  "tcpUniqueId": "string"
}
```

#### 会话信息上报
- **端点**: `POST {endpoint}/stats/v1/session`
- **Content-Type**: `application/json`
- **请求体**:
```json
{
  "sessionId": "string",
  "appType": number,
  "startTime": string,
  "endTime": string,
  "tcpUniqueId": "string"
}
```

---

### 5.3 配置服务

#### 获取浏览器配置
- **端点**: `GET {endpoint}/config/v1`
- **说明**: 获取浏览器配置信息
- **参数**:
  - `endpoint`: 从 CSE 服务发现获取的服务地址
- **响应**:
```json
{
  "chrome_version": "string",
  "platform": "string",
  "capabilities": object
}
```

#### 获取插件信息
- **端点**: `GET {endpoint}/plugin/v1/current`
- **说明**: 获取当前生效的插件信息
- **响应**:
```json
{
  "name": "string",
  "version": "string",
  "status": "string",
  "loadTime": string
}
```

---

### 5.4 文件服务

#### 文件操作接口

所有 S3 兼容接口，支持以下操作:

**上传文件**
- **端点**: `PUT {endpoint}/file/v1/{bucket}/{key}`
- **Content-Type**: `application/octet-stream`
- **参数**:
  - `bucket`: 存储桶名称
  - `key`: 文件键名
- **请求体**: 文件二进制数据

**下载文件**
- **端点**: `GET {endpoint}/file/v1/{bucket}/{key}`
- **参数**:
  - `bucket`: 存储桶名称
  - `key`: 文件键名
- **响应**: 文件二进制数据

**删除文件**
- **端点**: `DELETE {endpoint}/file/v1/{bucket}/{key}`
- **参数**:
  - `bucket`: 存储桶名称
  - `key`: 文件键名

**检查文件存在**
- **端点**: `HEAD {endpoint}/file/v1/{bucket}/{key}`
- **参数**:
  - `bucket`: 存储桶名称
  - `key`: 文件键名
- **响应**: HTTP 200 存在, 404 不存在

---

### 5.5 告警服务

#### 告警查询
- **端点**: `POST cse://FMService/fmOperation/v1/alarms/get_alarms`
- **说明**: 通过华为云告警SDK查询告警
- **Content-Type**: `application/json`
- **请求体**:
```json
{
  "alarmId": "string",
  "startTime": string,
  "endTime": string
}
```

#### 告警发送
- **方式**: 通过华为云 `alarmsdk` SDK
- **方法**:
```java
// 通过 AlarmSendManager 发送告警
AlarmEventManager.dispatchEvent(event);
```

---

### 5.6 事件上报

#### 事件上传
- **端点**: `POST {endpoint}/server/v/event1/uploadEvent`
- **Content-Type**: `application/json`
- **请求体**:
```json
{
  "eventType": "string",
  "eventData": object,
  "timestamp": number,
  "userId": "string"
}
```

---

## 6. 定时任务接口

### 6.1 浏览器检查任务 (BrowserCheckTask)

#### 基本信息
- **类**: `com.huawei.browsergateway.scheduled.BrowserCheckTask`
- **执行周期**: `${browsergw.scheduled.check-browser-period}`
- **默认值**: 1800000ms (30分钟)

#### 功能说明
定期检查所有浏览器实例的健康状态，处理异常情况:
1. 检查每个浏览器实例的运行状态
2. 检查实例心跳时间
3. 处理异常实例并记录日志
4. 上报检查结果

#### 调用流程
```
启动定时任务
  ↓
遍历所有浏览器实例
  ↓
调用 ChromeDriver 进行健康检查
  ↓
获取异常实例列表
  ↓
清理异常实例
  ↓
记录检查日志
```

---

### 6.2 浏览器关闭任务 (BrowserCloserTask)

#### 基本信息
- **类**: `com.huawei.browsergateway.scheduled.BrowserCloserTask`
- **执行周期**: `${browsergw.scheduled.close-browser-period}`
- **默认值**: 600000ms (10分钟)

#### 功能说明
关闭长时间无心跳的浏览器实例，释放系统资源:
1. 检查每个实例的心跳时间
2. 对超过 TTL 无心跳的实例执行关闭操作
3. 清理相关数据和资源
4. 记录关闭动作

#### TTL 配置
```yaml
browsergw:
  chrome:
    ttl: 360000000000  # 纳秒，约 100小时
```

---

### 6.3 连接监控任务 (TcpChannelMonitor)

#### 基本信息
- **类**: `com.huawei.browsergateway.scheduled.TcpChannelMonitor`
- **执行周期**: `${browsergw.scheduled.tcp-heartbeat-period}`
- **默认值**: 600000ms (10分钟)

#### 功能说明
监控 TCP 连接的心跳状态，清理超时连接:
1. 监控控制流 TCP 连接活跃度
2. 监控媒体流 TCP 连接活跃度
3. 统计数据流量
4. 处理超时连接

#### 监控范围
- 控制流连接 (ControlClientSet)
- 媒体流连接 (MediaClientSet)

---

### 6.4 服务上报任务 (ServiceReporter)

#### 基本信息
- **类**: `com.huawei.browsergateway.scheduled.ServiceReporter`
- **执行时机**: 服务启动时 (`ContextRefreshedEvent`)

#### 功能说明
向服务中心上报服务端点和实例信息:
1. 获取服务和实例信息
2. 上报到 CSE 注册中心
3. 失败时自动重试 (最多5次)
4. 重试间隔 30秒

#### 上报内容
- 服务链路端点 (control, media, tls endpoints)
- 服务实例信息
- 服务容量信息

---

### 6.5 健康检查任务 (HealthCheckTask)

#### 基本信息
- **类**: `com.huawei.browsergateway.scheduled.HealthCheckTask`
- **执行周期**: `${browsergw.scheduled.health-check-period}`
- **默认值**: 60000ms (1分钟)

#### 功能说明
检查系统健康状态，包括:
1. CPU 使用率检查
2. 内存使用率检查
3. 网络接口检查
4. 根据检查结果触发告警
5. 更新服务健康状态

#### 健康检查配置
```yaml
browsergw:
  healthCheck:
    cpu-trigger-threshold: 90       # CPU告警触发阈值
    cpu-recover-threshold: 80       # CPU恢复阈值
    memory-trigger-threshold: 90    # 内存告警触发阈值
    memory-recover-threshold: 80    # 内存恢复阈值
```

---

### 6.6 服务状态刷新任务 (ServiceStatusRefresherTask)

#### 基本信息
- **类**: `com.huawei.browsergateway.scheduled.ServiceStatusRefresherTask`
- **执行周期**: 定期执行

#### 功能说明
刷新和更新服务状态信息:
1. 从服务中心获取最新配置
2. 更新服务状态
3. 同步实例信息

---

## 7. 数据格式详解

### 7.1 通用响应格式

所有 HTTP REST API 响应遵循统一格式:

```json
{
  "code": number,            // 业务码，200 成功
  "message": "string",       // 响应消息描述
  "data": object|string|number // 响应数据
}
```

**业务码说明**:
- `200`: 成功
- `400`: 请求参数错误
- `500`: 服务器内部错误
- 其他值: 业务特定错误码

---

### 7.2 用户标识数据

```json
{
  "userId": "string",        // 用户ID: IMEI + IMSI 组合
  "sessionId": "string",     // 会话ID: 临时会话标识
  "imei": "string",          // 15位设备IMEI号
  "imsi": "string",          // 15位设备IMSI号
  "tcpUniqueId": "string",   // TCP连接唯一标识
  "timestamp": number        // 时间戳
}
```

---

### 7.3 浏览器配置数据

```json
{
  "factory": "string",        // 设备厂商: "Huawei", "Xiaomi"
  "dev_type": "string",       // 设备型号: "Mate30", "P40"
  "ext_type": "string",       // 扩展类型
  "plat_type": number,        // 平台类型: 0-Android, 1-iOS
  "lcd_width": number,        // 屏幕宽度
  "lcd_height": number,       // 屏幕高度
  "dpi": number,              // 屏幕密度
  "rotation": number,         // 屏幕旋转角度: 0/90/180/270
  "fps": number,              // 刷新率: 30/60
  "chrome_version": "string", // Chrome版本号
  "record_mode": number,      // 记录模式
  "headless": boolean,        // 无头模式: true/false
  "user_agent": "string"      // 用户代理字符串
}
```

---

### 7.4 媒体流配置

```json
{
  "video": {
    "width": number,         // 视频宽度: 1920
    "height": number,        // 视频高度: 1080
    "fps": number,           // 帧率: 30
    "bitrate": number,       // 视频比特率 kbps
    "codec": "string",       // 编码格式: "H264"|"H265"
    "gop": number            // GOP大小: 29
  },
  "audio": {
    "sample_rate": number,   // 采样率 Hz: 44100/48000
    "channels": number,      // 通道数: 1/2
    "bitrate": number,       // 音频比特率 kbps: 128
    "codec": "string"        // 编码格式: "AAC"|"OPUS"
  }
}
```

---

### 7.5 会话信息

```json
{
  "sessionId": "string",      // 会话ID
  "userId": "string",         // 用户ID
  "appType": number,          // 应用类型
  "startTime": string,        // 开始时间: "2026-01-01 12:00:00"
  "endTime": string,          // 结束时间
  "duration": number,         // 持续时间 (秒)
  "status": "string",         // 状态: "ACTIVE"|"FINISHED"
  "tcpUniqueId": "string"     // TCP连接ID
}
```

---

### 7.6 流量统计信息

```json
{
  "sessionId": "string",      // 用户会话ID
  "serviceType": "string",    // 服务类型: "CONTROL_SERVICE"|"MEDIA_SERVICE"
  "dataSize": number,         // 数据量 (字节)
  "startTime": string,        // 开始时间
  "endTime": string,          // 结束时间
  "tcpUniqueId": "string"     // TCP连接ID
}
```

---

### 7.7 事件数据

```json
{
  "eventType": "string",      // 事件类型
  "eventData": object,        // 事件数据
  "timestamp": number,        // 事件时间戳 (毫秒)
  "userId": "string",         // 用户ID
  "sessionId": "string"       // 会话ID
}
```

**事件类型枚举**:
- `USER_LOGIN`: 用户登录
- `USER_LOGOUT`: 用户登出
- `BROWSER_CREATE`: 浏览器创建
- `BROWSER_CLOSE`: 浏览器关闭
- `ERROR`: 错误事件
- `ALARM`: 告警事件

---

### 7.8 告警事件

```json
{
  "alarmId": "string",        // 告警ID
  "alarmName": "string",      // 告警名称
  "alarmLevel": string,       // 告警级别: "CRITICAL"|"MAJOR"|"MINOR"
  "alarmType": string,        // 告警类型
  "alarmContent": "string",   // 告警内容
  "occurTime": string,        // 发生时间
  "clearTime": string,        // 清除时间
  "resourceId": "string",     // 资源ID
  "location": "string"        // 位置信息
}
```

---

## 8. 接口调用流程

### 8.1 用户登录完整流程

```
┌─────────┐
│Clienr   │
└────┬────┘
     │ 1. preOpen - 预创建浏览器
     ▼
┌──────────────┐
│ ChromeApi    │
└──────┬───────┘
       │ 2. createChrome - 创建浏览器实例
       ▼
┌──────────────┐
│ RemoteImpl   │
└──────┬───────┘
       │ 3 getUserBind - 获取绑定信息
       ▼
┌──────────────┐
│ 外部绑定服务  │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ ChromeSetImpl│ ◄── 4. 浏览器实例创建完成
└──────┬───────┘
       │
       ├───────► 5. 建立控制 TCP 连接
       │
       ├───────► 6. 建立 TCP 心跳保活
       │
       ├───────► 7. 建立媒体 WebSocket 连接
       │
       └───────► 8. 开始音视频传输
```

**详细步骤**:

1. **预创建浏览器** (可选)
   - 调用 `POST /browsergw/browser/preOpen`
   - 系统预创建浏览器实例但不启动
   - 提高首次连接响应速度

2. **建立控制连接**
   - 客户端建立 TLS TCP 连接
   - 发送 LOGIN 消息进行认证
   - 系统创建用户会话

3. **创建浏览器实例**
   - 接收控制包数据
   - 创建 ChromeDriver 实例
   - 启动浏览器和扩展

4. **配置媒体参数**
   - 建立媒体 WebSocket 连接
   - 发送初始化参数
   - 启动媒体处理器

5. **开始媒体传输**
   - 接收视频帧数据
   - 接收音频数据
   - 转发到客户端

6. **持续心跳保活**
   - TCP 连接定期发送心跳
   - WebSocket 心跳包
   - 浏览器实例心跳更新

---

### 8.2 浏览器实例生命周期

```
┌─────────────┐
│  preOpen    │  预创建实例 (可选)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ createChrome│  创建实例
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Active    │  活跃状态
│  Heartbeat  │  ├─ 定期更新心跳
│             │  ├─ 处理控制命令
│             │  └─ 传输媒体流
└──────┬──────┘
       │
       ├─▶ 超时 ───┐
       │           ▼
       │    ┌─────────────┐
       └───▶│   Close     │  关闭实例
            └──────┬──────┘
                   │
                   ▼
            ┌─────────────┐
            │   Delete    │  删除实例
            │  Cleanup    │  清理资源
            └─────────────┘
```

**生命周期说明**:

1. **preOpen 阶段**
   - 预创建浏览器实例
   - 不启动浏览器进程
   - 等待用户连接

2. **createChrome 阶段**
   - 正式创建浏览器实例
   - 启动 ChromeDriver
   - 加载浏览器扩展

3. **Active 阶段**
   - 定期更新心跳时间
   - 处理客户端控制命令
   - 转发媒体流数据

4. **Close 阶段**
   - 无心跳或用户登出触发
   - 关闭浏览器进程
   - 释放连接资源

5. **Delete 阶段**
   - 删除浏览器实例对象
   - 清理用户数据
   - 移除会话记录

---

### 8.3 扩展更新流程

```
┌──────────────┐
│ Update API   │  发送加载扩展请求
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ Plugsh manage │ 下载扩展文件
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ Delete All   │ 删除所有浏览器实例
│   Browser    │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ Remove Old   │ 删除旧扩展
│  Extension   │
└──────┬───────┘
       │
       ▼
┌──────────────┐
 │ Load New    │ 加载新扩展
 │  Extension  │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  Restart     │ 重启受影响用户会话
│   Session    │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  Verification│ 验证扩展加载成功
└──────────────┘
```

**详细步骤**:

1. **触发更新**
   - 管理员调用 `POST /browsergw/extension/load`
   - 指定新扩展的版本和存储位置

2. **下载扩展**
   - 从 S3 存储下载扩展文件
   - 解压到本地目录
   - 验证文件完整性

3. **关闭所有浏览器**
   - 调用 `ChromeSetImpl.deleteAll()`
   - 优雅关闭所有活跃实例
   - 断开所有连接

4. **清理旧扩展**
   - 卸载旧版本 Chrome 扩展
   - 删除旧的 jar 文件
   - 清理临时文件

5. **加载新扩展**
   - 安装新的 Chrome 扩展
   - 加载新的 MuenSDK 驱动
   - 初始化扩展功能

6. **重启会话**
   - 通知受影响用户
   - 重新创建浏览器实例
   - 恢复用户会话

7. **验证**
   - 检查扩展是否正常工作
   - 测试关键功能
   - 记录更新日志

---

### 8.4 数据上报流程

```
┌──────────────────┐
│  业务触发点       │  会话开始/结束、流量统计、事件发生
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ 收集统计数据      │  收集: 会话、流量、事件数据
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ 数据格式化        │  转换为 JSON 格式
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│  Query Endpoint  │  从 CSE 获取上报端点
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│  Send Request    │  HTTP POST 上报数据
└──────┬───────────┘
       │
       ├─ 成功 ──▶ 记录日志
       │
       └─ 失败 ──▶ 重试/记录错误
```

**上报类型**:

1. **会话上报**
   - 会话开始事件
   - 会话结束事件
   - 会话统计数据

2. **流量上报**
   - 媒体流量统计
   - 控制流量统计
   - 定期上报 (可配置周期)

3. **事件上报**
   - 用户登录/登出事件
   - 浏览器创建/关闭事件
   - 错误事件
   - 告警事件

4. **服务信息上报**
   - 服务端点信息
   - 服务容量信息
   - 服务健康状态

---

### 8.5 错误处理流程

```
┌─────────────────┐
│ Exception       │  发生异常
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│ GlobalException │  全局异常处理器
│    Handle       │
└──────┬──────────┘
       │
       ├─ 业务异常 ──▧
       │             ▼
       │     ┌────────────┐
       │     │ 错误日志    │ 记录错误信息
       │     └─────┬──────┘
       │           │
       │           ▼
       │     ┌────────────┐
       │     │ 返回错误    │ 统一格式响应
       │     └────────────┘
       │
       └─ 系统异常 ──▧
                     ▼
              ┌────────────┐
              │ 告警触发    │ 发送系统告警
              └─────┬──────┘
                    │
                    ▼
              ┌────────────┐
              │ 回滚处理    │ 降级/回滚
              └────────────┘
```

**错误分类**:

1. **业务错误** (400)
   - 参数验证错误
   - 业务逻辑错误
   - 资源不可用

2. **系统错误** (500)
   - 服务器内部错误
   - 外部服务不可用
   - 资源不足

3. **网络错误**
   - 连接超时
   - 连接拒绝
   - 网络中断

---

### 8.6 监控和告警流程

```
┌──────────────────┐
│  定时监控任务     │  HealthCheck / Monitor
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ 收集监控指标      │  CPU、内存、网络、实例数
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ 触发阈值判断      │  是否超过告警阈值
└──────┬───────────┘
       │
       ├─ 正常 ──▶ 更新健康状态
       │
       └─ 超阈值 ──▧
                     ▼
              ┌────────────┐
              │ 发送告警    │ 通过 alarmsdk
              └─────┬──────┘
                    │
                    ▼
              ┌────────────┐
              │ 记录日志    │
              └─────┬──────┘
                    │
                    ▼
              ┌────────────┐
              │ 通知运维    │
              └────────────┘
```

**监控指标**:

1. **CPU 使用率**
   - 触发阈值: 90%
   - 恢复阈值: 80%

2. **内存使用率**
   - 触发阈值: 90%
   - 恢复阈值: 80%

3. **网络接口**
   - 连接数监控
   - 流量监控
   - 丢包率监控

4. **浏览器实例**
   - 活跃实例数
   - 异常实例数
   - 创建/关闭速率

5. **连接状态**
   - TCP 连接数
   - WebSocket 连接数
   - 连接超时数

---

## 附录

### A. 配置参数说明

#### application.yaml
```yaml
server:
  address: {mbase_ip}          # 服务器地址
  port: 8090                    # HTTP端口

browsergw:
  workspace: /opt/host          # 工作目录

  chrome:
    headless: true              # 无头模式
    record-mode: 1              # 记录模式
    endpoint: http://127.0.0.1:8000  # Chrome驱动端点
    executable-path: /usr/bin/chromium  # Chrome路径
    ttl: 360000000000           # 浏览器实例TTL (纳秒)

  report:
    cap: 300                    # 服务容量
    control-endpoint: ...       # 控制端点
    media-endpoint: ...         # 媒体端点
    ttl: 120                    # 信息TTL (秒)

  websocket:
    media-port: 30002           # 媒体端口
    muen-port: 30005            # 控制端口
    boss: 8                     # 线程数
    worker: 64
    heartbeat-ttl: 300000       # 心跳超时 (毫秒)

  scheduled:
    check-browser-period: 1800000   # 浏览器检查周期
    close-browser-period: 600000    # 关闭周期
    tcp-heartbeat-period: 600000    # 心跳周期
    health-check-period: 60000      # 健康检查周期

  tcp:
    control-port: ...           # 控制端口
    media-port: ...             # 媒体端口
    heartbeat-ttl: 360000000000 # 心跳超时 (纳秒)

  healthCheck:
    cpu-trigger-threshold: 90  # CPU告警阈值
    cpu-recover-threshold: 80  # CPU恢复阈值
    memory-trigger-threshold: 90 # 内存告警阈值
    memory-recover-threshold: 80 # 内存恢复阈值
```

---

### B. 错误码说明

| 错误码 | 说明 | 处理建议 |
|--------|------|---------|
| 200 | 成功 | - |
| 400 | 请求参数错误 | 检查请求参数格式 |
| 401 | 未授权 | 检查认证信息 |
| 404 | 资源不存在 | 检查资源路径 |
| 500 | 服务器内部错误 | 联系管理员 |
| 503 | 服务不可用 | 稍后重试 |
| 1001 | 浏览器创建失败 | 重试或检查资源 |
| 1002 | 扩展加载失败 | 检查扩展配置 |

---

### C. 支持的浏览器

| 浏览器 | 版本要求 | 特性支持 |
|--------|---------|---------|
| Chromium | 80+ | 完整支持 |
| Chrome | 80+ | 完整支持 |
| Edge | 80+ | 完整支持 |

---

### D. 系统要求

| 组件 | 最低要求 | 推荐配置 |
|------|---------|---------|
| CPU | 4核 | 8核+ |
| 内存 | 8GB | 16GB+ |
| 磁盘 | 50GB | 100GB+ |
| 网络 | 1Gbps | 10Gbps |
| 操作系统 | Linux | Linux (CentOS/Ubuntu) |

---

## 修订历史

| 版本 | 日期 | 作者 | 修改内容 |
|------|------|------|---------|
| 1.0 | 2026-01-01 | Initial | 初始版本 |

---

**文档结束**