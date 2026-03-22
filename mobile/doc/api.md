# Mobile项目 - API接口文档

## 文档概述

本文档详细描述了Mobile项目的所有API接口，包括WebSocket接口、HTTP接口以及TLV协议接口。所有接口均遵循统一的错误处理规范和响应格式。

---

## 1. 公共说明

### 1.1 错误码定义

| 错误码 | 描述           | 说明                         |
|--------|----------------|------------------------------|
| 0      | 成功           | 请求成功处理                 |
| 200    | 成功           | HTTP请求成功                 |
| 400    | 请求参数错误   | 参数格式不正确或缺少必要参数 |
| 401    | 认证失败       | Token无效或已过期            |
| 404    | 资源不存在     | 请求的资源不存在             |
| 500    | 服务器内部错误 | 服务端处理异常               |

### 1.2 通用响应格式

#### WebSocket文本消息响应
```json
{
  "code": 0,
  "data": {},
  "msg": "成功"
}
```

#### HTTP响应
```json
{
  "code": 200,
  "data": {},
  "msg": "success"
}
```

### 1.3 TLV协议标识符

完整的TLV标识符定义请参考 `common/ID.java`，以下是常用标识符：

| 标识符 | 值  | 类型   | 描述           |
|--------|-----|--------|----------------|
| TYPE   | 1   | Integer | 消息类型       |
| FACTORY | 2   | String  | 厂商           |
| DEV_TYPE | 3   | String  | 机型           |
| IMSI    | 4   | String  | IMSI值         |
| IMEI    | 5   | String  | IMEI值         |
| LCD_WIDTH | 6  | Integer | 屏幕宽度       |
| LCD_HEIGHT | 7 | Integer | 屏幕高度       |
| CTRL_TYPE | 12 | Integer | 控制类型       |
| CTRL_VAL  | 13 | Integer | 控制值         |
| CONTENT  | 26   | String  | 传输内容       |
| UPLOAD_TYPE | 27 | Integer | 上传类型       |
| FILE_ADDR  | 36 | String  | 文件地址       |
| TOKEN      | 21 | String  | 登录Token      |
| SESSION_ID | 22 | String  | 会话ID         |

---

## 2. WebSocket接口

### 2.1 WebSocket连接

**接口说明**: 建立浏览器与云手机服务的WebSocket长连接

**接口地址**: `ws://localhost:40002/app/websocket/{imeiAndImsi}`

**连接参数**:
- `imeiAndImsi` (必填): 设备标识，格式为 `{IMEI}_{IMSI}`
- `gids_addr` (可选): GIDS服务地址，默认为 `http://127.0.0.1:9090`

**示例**:
```javascript
const imei = "6258412454025411";
const imsi = "68510155565211";
const ws = new WebSocket(`ws://localhost:40002/app/websocket/${imei}_${imsi}?gids_addr=http://127.0.0.1:9090`);

ws.onopen = function() {
    console.log('WebSocket连接已建立');
};

ws.onmessage = function(event) {
    console.log('收到消息:', event.data);
};
```

---

### 2.2 设备登录

**接口说明**: 设备登录到云手机服务

**消息类型**: `login`

**请求消息格式**:
```json
{
  "type": "login",
  "cs": "240x320",
  "dv": 2,
  "at": 5,
  "ga": "http://127.0.0.1:9090"
}
```

**请求参数说明**:

| 参数名 | 类型   | 必填 | 描述                                                     |
|--------|--------|------|----------------------------------------------------------|
| type   | String | 是   | 消息类型，固定值: `login`                                |
| cs     | String | 是   | 画布尺寸，格式: `宽x高`，可选值: `240x320`, `360x480`, `405x540`, `540x720` |
| dv     | Integer | 是   | 设备类型，默认值: `2`                                     |
| at     | Integer | 是   | 应用类型，可选值: `1`(BBC), `2`(TikTok), `3`(Facebook), `4`(YouTube), `5`(Other) |
| ga     | String | 是   | GIDS服务地址，默认值: `http://127.0.0.1:9090`          |

**响应消息格式**:
```json
{
  "code": 0,
  "data": {
    "token": "1234",
    "expiresTime": "2026-03-20T10:00:00",
    "tcpAddr": "127.0.0.1:30001",
    "timeAxis": 1710921600000,
    "videoMode": 1,
    "shortAddr": "127.0.0.1",
    "nodeGateWayUrl": "http://127.0.0.1:9090"
  },
  "msg": "登录成功"
}
```

**响应参数说明**:

| 参数名         | 类型   | 描述                         |
|----------------|--------|------------------------------|
| code           | Integer | 状态码，`0`表示成功           |
| token          | String  | 登录凭证Token                |
| expiresTime    | String  | Token过期时间（ISO8601格式）  |
| tcpAddr        | String  | 控制通道TCP地址，格式: `IP:PORT` |
| timeAxis       | Long    | 时间轴时间戳                 |
| videoMode      | Integer | 视频模式                     |
| shortAddr      | String  | 短地址                       |
| nodeGateWayUrl | String  | 节点网关URL                  |
| msg            | String  | 响应消息                     |

**示例代码**:
```javascript
let ws;

function login() {
    const imei = "6258412454025411";
    const imsi = "68510155565211";
    const wsUrl = `ws://localhost:40002/app/websocket/${imei}_${imsi}`;

    ws = new WebSocket(wsUrl);
    ws.binaryType = 'arraybuffer';

    ws.onopen = function() {
        const message = {
            type: 'login',
            cs: '240x320',
            dv: 2,
            at: 5,
            ga: 'http://127.0.0.1:9090'
        };
        ws.send(JSON.stringify(message));
    };

    ws.onmessage = function(event) {
        if (event.data instanceof ArrayBuffer) {
            // 处理媒体流数据
            handleMediaData(event.data);
        } else {
            // 处理文本消息
            const response = JSON.parse(event.data);
            console.log('登录响应:', response);
        }
    };
}
```

---

### 2.3 设备登出

**接口说明**: 设备登出云手机服务

**消息类型**: `logout`

**请求消息格式**:
```json
{
  "type": "logout"
}
```

**请求参数说明**:

| 参数名 | 类型   | 必填 | 描述                     |
|--------|--------|------|--------------------------|
| type   | String | 是   | 消息类型，固定值: `logout` |

**响应消息格式**:
```json
{
  "code": 0,
  "msg": "登出成功"
}
```

**示例代码**:
```javascript
function logout() {
    if (ws && ws.readyState === WebSocket.OPEN) {
        const message = { type: 'logout' };
        ws.send(JSON.stringify(message));
        ws.close();
    }
}
```

---

### 2.4 方向控制

**接口说明**: 发送方向控制指令（上下左右、OK等）

**消息类型**: `direction`

**请求消息格式**:
```json
{
  "type": "direction",
  "ct": 1,
  "cv": 1
}
```

**请求参数说明**:

| 参数名 | 类型   | 必填 | 描述                                                     |
|--------|--------|------|----------------------------------------------------------|
| type   | String | 是   | 消息类型，固定值: `direction`                            |
| ct     | Integer | 是   | 控制类型，可选值: `1`(上), `2`(下), `3`(左), `4`(右), `5`(OK), `10`(菜单), `11`(返回) |
| cv     | Integer | 是   | 控制值，一般传 `1`                                       |

**控制类型说明**:

| ct值 | 控制动作 | 方向符号 |
|------|----------|----------|
| 1    | 上       | ↑        |
| 2    | 下       | ↓        |
| 3    | 左       | ←        |
| 4    | 右       | →        |
| 5    | 确定     | OK       |
| 10   | 菜单     | 菜单     |
| 11   | 返回     | 返回     |

**响应消息格式**:
```json
{
  "type": "callback",
  "elm": 12,
  "info": 0,
  "content": "操作成功",
  "wt": 1
}
```

**响应参数说明**:

| 参数名 | 类型    | 描述                     |
|--------|---------|--------------------------|
| type   | String  | 消息类型，固定值: `callback` |
| elm    | Integer | 控制响应元素             |
| info   | Integer | 控制响应信息             |
| content| String  | 控制响应内容             |
| wt     | Integer | 输入框类型               |

**示例代码**:
```javascript
function sendDirection(direction) {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
        alert('请先登录');
        return;
    }

    const ct = getControlType(direction);
    const message = {
        type: 'direction',
        ct: ct,
        cv: 1
    };
    ws.send(JSON.stringify(message));
}

function getControlType(direction) {
    switch (direction) {
        case 'up': return 1;
        case 'down': return 2;
        case 'left': return 3;
        case 'right': return 4;
        case 'ok': return 5;
        case 'menu': return 10;
        case 'back': return 11;
        default: return 1;
    }
}
```

---

### 2.5 内容输入

**接口说明**: 发送文本内容到云手机输入框

**消息类型**: `upload`

**请求消息格式**:
```json
{
  "type": "upload",
  "ut": 1,
  "content": "Hello World"
}
```

**请求参数说明**:

| 参数名 | 类型    | 必填 | 描述                 |
|--------|---------|------|----------------------|
| type   | String  | 是   | 消息类型，固定值: `upload` |
| ut     | Integer | 是   | 上传类型，默认值: `1`     |
| content| String  | 是   | 输入内容             |

**响应消息格式**:
```json
{
  "type": "callback",
  "elm": 26,
  "info": 0,
  "content": "输入成功",
  "wt": 1
}
```

**示例代码**:
```javascript
function sendContent() {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
        alert('请先登录');
        return;
    }

    const content = document.getElementById('contentInput').value;
    const fileType = document.getElementById('fileType').value;

    const message = {
        type: 'upload',
        ut: parseInt(fileType),
        content: content
    };
    ws.send(JSON.stringify(message));
    document.getElementById('contentInput').value = '';
}
```

---

### 2.6 文件上传

**接口说明**: 发送文件上传请求到云手机

**消息类型**: `upload_file`

**请求消息格式**:
```json
{
  "type": "upload_file",
  "fa": "/sdcard/Download/test.png"
}
```

**请求参数说明**:

| 参数名 | 类型   | 必填 | 描述                     |
|--------|--------|------|--------------------------|
| type   | String | 是   | 消息类型，固定值: `upload_file` |
| fa     | String | 是   | 文件地址                     |

**响应消息格式**:
```json
{
  "type": "callback",
  "elm": 35,
  "info": 0,
  "content": "文件上传成功",
  "wt": 0
}
```

**示例代码**:
```javascript
function uploadFile() {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
        alert('请先登录');
        return;
    }

    const fileAddr = document.getElementById('fileAddr').value;
    const message = {
        type: 'upload_file',
        fa: fileAddr
    };
    ws.send(JSON.stringify(message));
}
```

---

### 2.7 埋点上报 - 错误事件

**接口说明**: 上报客户端错误事件埋点

**消息类型**: `send_error`

**请求消息格式**:
```json
{
  "type": "send_error"
}
```

**请求参数说明**:

| 参数名 | 类型   | 必填 | 描述                       |
|--------|--------|------|----------------------------|
| type   | String | 是   | 消息类型，固定值: `send_error` |

**响应消息格式**:
```json
{
  "code": 0,
  "msg": "埋点上报成功"
}
```

**说明**: 该消息会自动创建繁忙(type=1)和错误(type=2)两个事件并上报

**示例代码**:
```javascript
function sendError() {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
        alert('请先登录');
        return;
    }

    const message = { type: 'send_error' };
    ws.send(JSON.stringify(message));
}
```

---

### 2.8 埋点上报 - 使用时长

**接口说明**: 上报客户端使用时长埋点

**消息类型**: `send_time`

**请求消息格式**:
```json
{
  "type": "send_time"
}
```

**请求参数说明**:

| 参数名 | 类型   | 必填 | 描述                       |
|--------|--------|------|----------------------------|
| type   | String | 是   | 消息类型，固定值: `send_time` |

**响应消息格式**:
```json
{
  "code": 0,
  "msg": "埋点上报成功"
}
```

**说明**: 使用时长默认为100000毫秒

**示例代码**:
```javascript
function sendUseTime() {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
        alert('请先登录');
        return;
    }

    const message = { type: 'send_time' };
    ws.send(JSON.stringify(message));
}
```

---

### 2.9 媒体流数据（二进制推送）

**接口说明**: 服务端主动向客户端推送的音视频流数据

**数据格式**: WebSocket二进制消息

**视频帧格式**:
```
+--------+--------+--------+--------+
| 0x01   | frameType |       videoData        |
+--------+--------+--------+--------+
  帧头     帧类型      H.264编码数据
```

**音频帧格式**:
```
+--------+--------+--------+--------+
| 0x02   |       audioData                |
+--------+--------+--------+--------+
  帧头     MP3编码数据
```

**帧类型说明**:

| frameType | 描述       | 值  |
|-----------|------------|-----|
| 1         | 关键帧(I帧)| 1   |
| 2         | 非关键帧(P/B帧)| 2   |

**处理示例代码**:
```javascript
function handleMediaData(data) {
    const frameHeader = new Uint8Array(data, 0, 2);
    const frameType = frameHeader[0];

    if (frameType === 0x01) {
        // 视频帧
        const frameInfo = frameHeader[1];
        const videoData = new Uint8Array(data, 2);

        const chunk = new EncodedVideoChunk({
            type: frameInfo === 1 ? 'key' : 'delta',
            timestamp: performance.now(),
            data: videoData
        });

        videoDecoder.decode(chunk);
    } else if (frameType === 0x02) {
        // 音频帧
        const audioData = new Uint8Array(data, 1);
        playAudio(audioData);
    }
}

function playAudio(audioData) {
    audioContext.decodeAudioData(audioData.buffer.slice(), function(buffer) {
        const source = audioContext.createBufferSource();
        source.buffer = buffer;
        source.connect(audioContext.destination);
        source.start(0);
    }, function(e) {
        console.error('Audio decode error:', e);
    });
}
```

---

## 3. HTTP接口（内部调用）

### 3.1 设备登录认证 - 第一步

**接口说明**: GIDS设备登录认证第一步

**接口地址**: `POST {gidsAddr}/app-api/devicetcp/app/login/v1/gridLoginAuth`

**请求头**:
```
Content-Type: application/json
```

**请求参数**:
```json
{
  "imsi": "68510155565211",
  "imei": "6258412454025411",
  "manufacturer": "default",
  "model": "default",
  "appType": "5",
  "extendModel": "default",
  "country": "default",
  "platform": "1",
  "width": "240",
  "height": "320",
  "mcc": "460",
  "mnc": "00x",
  "lac": "100",
  "ci": "5.21",
  "rxlev": "-72",
  "totalKb": "1424122",
  "freeKb": "1424122",
  "clientLanguage": "en_US",
  "deviceType": "2"
}
```

**请求参数说明**:

| 参数名          | 类型   | 必填 | 描述                 |
|-----------------|--------|------|----------------------|
| imsi            | String | 是   | IMSI值               |
| imei            | String | 是   | IMEI值               |
| manufacturer    | String | 是   | 设备厂商             |
| model           | String | 是   | 设备型号             |
| appType         | String | 是   | 应用类型             |
| extendModel     | String | 是   | 扩展机型             |
| country         | String | 是   | 国家                 |
| platform        | String | 是   | 平台类型，默认值: `1`    |
| width           | String | 是   | 屏幕宽度             |
| height          | String | 是   | 屏幕高度             |
| mcc             | String | 是   | 移动国家码，默认值: `460` |
| mnc             | String | 是   | 移动网络码，默认值: `00x` |
| lac             | String | 是   | 位置区域码，默认值: `100` |
| ci              | String | 是   | 小区ID，默认值: `5.21` |
| rxlev           | String | 是   | 信号强度，默认值: `-72` |
| totalKb         | String | 是   | 总内存(KB)，默认值: `1424122` |
| freeKb          | String | 是   | 可用内存(KB)，默认值: `1424122` |
| clientLanguage  | String | 是   | 客户端语言，默认值: `en_US` |
| deviceType      | String | 是   | 设备类型，默认值: `2` |

**响应参数**:
```json
{
  "code": 200,
  "data": {
    "token": "1234",
    "expiresTime": "2026-03-20T10:00:00",
    "tcpAddr": "127.0.0.1:30001"
  },
  "msg": "success"
}
```

---

### 3.2 设备登录认证 - 第二步

**接口说明**: GIDS设备登录认证第二步（打开浏览器）

**接口地址**: `POST {gidsAddr}/app-api/devicetcp/app/login/v1/gridLoginAuthOpenBrowser`

**请求头**:
```
Content-Type: application/json
```

**请求参数**: 与3.1相同

**响应参数**: 与3.1相同

---

### 3.3 设备登录认证 - 第三步

**接口说明**: GIDS设备登录认证第三步（设备认证）

**接口地址**: `POST {gidsAddr}/app-api/devicetcp/app/login/v1/deviceLoginAuth`

**请求头**:
```
Content-Type: application/json
```

**请求参数**: 与3.1相同

**响应参数**: 与3.1相同

---

### 3.4 上报客户端事件

**接口说明**: 上报客户端事件埋点（繁忙/错误）

**接口地址**: `POST {gidsAddr}/app-api/center/public/client/sendClientEvent`

**请求头**:
```
Content-Type: application/json
```

**请求参数**:
```json
{
  "hsman": "default",
  "hstype": "default",
  "appType": "5",
  "imei": "6258412454025411",
  "imsi": "68510155565211",
  "type": 1
}
```

**请求参数说明**:

| 参数名 | 类型    | 必填 | 描述                                   |
|--------|---------|------|----------------------------------------|
| hsman  | String  | 是   | 厂商                                   |
| hstype | String  | 是   | 机型                                   |
| appType| String  | 是   | 应用类型                               |
| imei   | String  | 是   | IMEI值                                 |
| imsi   | String  | 是   | IMSI值                                 |
| type   | Integer | 是   | 事件类型，`1`=繁忙，`2`=错误（需分别上报） |

**响应参数**:
```json
{
  "code": 0,
  "data": null,
  "msg": "success"
}
```

---

### 3.5 上报使用时长事件

**接口说明**: 上报客户端使用时长埋点

**接口地址**: `POST {gidsAddr}/app-api/center/public/client/sendAppUseTimesEvent`

**请求头**:
```
Content-Type: application/octet-stream
```

**请求参数**:
```json
{
  "useTimes": 100000,
  "hsman": "default",
  "hstype": "default",
  "appType": "5",
  "appId": "5",
  "scheight": 320,
  "scwidth": 240,
  "exttype": "default",
  "imei": "6258412454025411",
  "imsi": "68510155565211",
  "playMode": 1
}
```

**请求参数说明**:

| 参数名    | 类型    | 必填 | 描述                     |
|-----------|---------|------|--------------------------|
| useTimes  | Long    | 是   | 使用时长（毫秒），默认值: `100000` |
| hsman     | String  | 是   | 厂商                     |
| hstype    | String  | 是   | 机型                     |
| appType   | String  | 是   | 应用类型                 |
| appId     | String  | 是   | 应用ID                   |
| scheight  | Integer | 是   | 屏幕高度                 |
| scwidth   | Integer | 是   | 屏幕宽度                 |
| exttype   | String  | 是   | 扩展类型                 |
| imei      | String  | 是   | IMEI值                   |
| imsi      | String  | 是   | IMSI值                   |
| playMode  | Integer | 是   | 播放模式，默认值: `1`   |

**响应参数**:
```json
{
  "code": 0,
  "data": null,
  "msg": "success"
}
```

---

## 4. TLV协议接口（Netty TCP）

### 4.1 TLV协议格式

**包头格式**:
```
+--------+--------+--------+--------+--------+--------+--------+--------+
|                    magic(2B)                  |     count(4B)     |
+--------+--------+--------+--------+--------+--------+--------+--------+
|     dataLen(4B)                                                |
+--------+--------+--------+--------+--------+--------+--------+--------+
|                          TLV数据...                              |
+--------+--------+--------+--------+--------+--------+--------+--------+
```

**TLV数据格式**:
```
+--------+--------+--------+--------+--------+--------+--------+--------+
|                       type(4B)                   |    length(4B)    |
+--------+--------+--------+--------+--------+--------+--------+--------+
|                          value(NB)                               |
+--------+--------+--------+--------+--------+--------+--------+--------+
```

**字段说明**:
- `magic`: 魔数标识，固定值: `"mu"` (0x6D75)
- `count`: TLV字段数量
- `dataLen`: TLV数据总长度
- `type`: 字段标识符（参考common/ID.java定义）
- `length`: value字节长度
- `value`: 字段值

---

### 4.2 控制通道 - 登录

**通道类型**: 控制通道（Control Channel）

**消息类型**: LOGIN (1)

**TLV字段**:

| 标识符       | 类型    | 必填 | 值示例             | 描述           |
|--------------|---------|------|--------------------|----------------|
| TYPE         | Integer | 是   | 1                  | 消息类型       |
| FACTORY      | String  | 是   | "default"          | 厂商           |
| DEV_TYPE     | String  | 是   | "default"          | 机型           |
| IMSI         | String  | 是   | "68510155565211"   | IMSI值         |
| IMEI         | String  | 是   | "6258412454025411" | IMEI值         |
| LCD_WIDTH    | Integer | 是   | 240                | 屏幕宽度       |
| LCD_HEIGHT   | Integer | 是   | 320                | 屏幕高度       |
| APP_TYPE     | Integer | 是   | 5                  | 应用类型       |
| TOKEN        | String  | 是   | "1234"             | 登录Token      |
| APP_ID       | Integer | 是   | 5                  | 应用ID         |
| EXT_TYPE     | String  | 是   | "default"          | 扩展机型       |
| PLAT_TYPE    | Integer | 是   | 1                  | 平台类型       |
| PLAY_MODE    | Integer | 是   | 1                  | 播放模式       |
| ABILITY      | Integer | 是   | 1                  | 能力值         |
| DEVICE_TYPE  | Integer | 是   | 2                  | 设备类型       |
| CLIENT_LANGUAGE | String | 是   | "en_US"           | 客户端语言     |
| AUD_TYPE     | String  | 是   | "mp3"              | 音频类型       |
| AUD_SMPRATE  | Integer | 是   | 46000              | 音频采样率     |
| AUD_CHANNEL  | Integer | 是   | 1                  | 音频通道数     |
| NETWORK_TYPE | Integer | 是   | 1                  | 网络类型       |
| URL_TYPE     | String  | 是   | "1"                | URL类型        |

**示例代码（Java）**:
```java
TlvData<Object> tlvData = new TlvData<>();
tlvData.put(ID.TYPE, Type.LOGIN);
tlvData.put(ID.FACTORY, "default");
tlvData.put(ID.DEV_TYPE, "default");
tlvData.put(ID.IMSI, "68510155565211");
tlvData.put(ID.IMEI, "6258412454025411");
tlvData.put(ID.LCD_WIDTH, 240);
tlvData.put(ID.LCD_HEIGHT, 320);
tlvData.put(ID.APP_TYPE, 5);
tlvData.put(ID.TOKEN, "1234");
tlvData.put(ID.APP_ID, 5);
tlvData.put(ID.EXT_TYPE, "default");
tlvData.put(ID.PLAT_TYPE, 1);
tlvData.put(ID.PLAY_MODE, 1);
tlvData.put(ID.ABILITY, 1);
tlvData.put(ID.DEVICE_TYPE, 2);
tlvData.put(ID.CLIENT_LANGUAGE, "en_US");
tlvData.put(ID.AUD_TYPE, "mp3");
tlvData.put(ID.AUD_SMPRATE, 46000);
tlvData.put(ID.AUD_CHANNEL, 1);
tlvData.put(ID.NETWORK_TYPE, 1);
tlvData.put(ID.URL_TYPE, "1");

channel.writeAndFlush(tlvData);
```

---

### 4.3 控制通道 - 心跳

**通道类型**: 控制通道（Control Channel）

**消息类型**: HEARTBEATS (2)

**TLV字段**:

| 标识符    | 类型    | 必填 | 描述           |
|-----------|---------|------|----------------|
| TYPE      | Integer | 是   | 消息类型       |
| SEQ       | Integer | 是   | 序列号         |

**示例代码（Java）**:
```java
TlvData<Object> tlvData = new TlvData<>();
tlvData.put(ID.TYPE, Type.HEARTBEATS);
tlvData.put(ID.SEQ, System.currentTimeMillis());

channel.writeAndFlush(tlvData);
```

---

### 4.4 控制通道 - 控制指令

**通道类型**: 控制通道（Control Channel）

**消息类型**: CONTROL (4)

**TLV字段**:

| 标识符      | 类型    | 必填 | 描述           |
|-------------|---------|------|----------------|
| TYPE        | Integer | 是   | 消息类型       |
| CTRL_TYPE   | Integer | 是   | 控制类型       |
| CTRL_VAL    | Integer | 是   | 控制值         |
| SESSION_ID  | String  | 是   | 会话ID         |

**控制类型（CTRL_TYPE）值**:

| 值  | 描述   |
|-----|--------|
| 1   | 上     |
| 2   | 下     |
| 3   | 左     |
| 4   | 右     |
| 5   | 确定   |
| 10  | 菜单   |
| 11  | 返回   |

**示例代码（Java）**:
```java
TlvData<Object> tlvData = new TlvData<>();
tlvData.put(ID.TYPE, Type.CONTROL);
tlvData.put(ID.CTRL_TYPE, 1);  // 向上
tlvData.put(ID.CTRL_VAL, 1);
tlvData.put(ID.SESSION_ID, getSessionId());

channel.writeAndFlush(tlvData);
```

---

### 4.5 控制通道 - 消息传输

**通道类型**: 控制通道（Control Channel）

**消息类型**: MESSAGE (13)

**TLV字段**:

| 标识符       | 类型    | 必填 | 描述           |
|--------------|---------|------|----------------|
| TYPE         | Integer | 是   | 消息类型       |
| UPLOAD_TYPE  | Integer | 是   | 上传类型       |
| CONTENT      | String  | 是   | 传输内容       |
| SESSION_ID   | String  | 是   | 会话ID         |

**示例代码（Java）**:
```java
TlvData<Object> tlvData = new TlvData<>();
tlvData.put(ID.TYPE, Type.MESSAGE);
tlvData.put(ID.UPLOAD_TYPE, 1);
tlvData.put(ID.CONTENT, "Hello World");
tlvData.put(ID.SESSION_ID, getSessionId());

channel.writeAndFlush(tlvData);
```

---

### 4.6 控制通道 - 文件上传

**通道类型**: 控制通道（Control Channel）

**消息类型**: UPLOAD_FILE (16)

**TLV字段**:

| 标识符              | 类型    | 必填 | 描述           |
|---------------------|---------|------|----------------|
| TYPE                | Integer | 是   | 消息类型       |
| UPLOAD_FILE_TYPE    | Integer | 是   | 文件类型       |
| UPLOAD_FILE_RESULT  | Integer | 是   | 上传结果，`0`=成功 |
| FILE_ADDR           | String  | 是   | 文件地址       |

**示例代码（Java）**:
```java
TlvData<Object> tlvData = new TlvData<>();
tlvData.put(ID.TYPE, Type.UPLOAD_FILE);
tlvData.put(ID.UPLOAD_FILE_TYPE, 1);
tlvData.put(ID.UPLOAD_FILE_RESULT, 0);  // 成功
tlvData.put(ID.FILE_ADDR, "/sdcard/Download/test.png");

channel.writeAndFlush(tlvData);
```

---

### 4.7 控制通道 - 应答

**通道类型**: 控制通道（Control Channel）

**消息类型**: ACK (7)

**TLV字段**:

| 标识符    | 类型    | 必填 | 描述           |
|-----------|---------|------|----------------|
| TYPE      | Integer | 是   | 消息类型       |
| ACK_TYPE  | Integer | 是   | 应答类型       |
| CODE      | Integer | 否   | 状态码         |

---

### 4.8 控制通道 - 返回媒体地址

**通道类型**: 控制通道（Control Channel）

**消息类型**: RETURN_MEDIA (9)

**TLV字段**:

| 标识符    | 类型    | 必填 | 描述       |
|-----------|---------|------|------------|
| TYPE      | Integer | 是   | 消息类型   |
| TCP_ADDR  | String  | 是   | 媒体TCP地址 |

**说明**: 收到此消息后，客户端应连接到媒体通道

---

### 4.9 控制通道 - 返回控制响应

**通道类型**: 控制通道（Control Channel）

**消息类型**: RETURN_CONTROL (12)

**TLV字段**:

| 标识符          | 类型    | 必填 | 描述           |
|-----------------|---------|------|----------------|
| TYPE            | Integer | 是   | 消息类型       |
| CTRL_RSP_ELM    | Integer | 是   | 控制响应元素   |
| CTRL_RSP_INFO   | Integer | 是   | 控制响应信息   |
| CONTENT         | String  | 否   | 响应内容       |
| WRITE_TYPE      | Integer | 否   | 输入框类型     |

---

### 4.10 媒体通道 - 登录

**通道类型**: 媒体通道（Media Channel）

**消息类型**: LOGIN (1)

**TLV字段**: 与控制通道登录相同

---

### 4.11 媒体通道 - 音频数据

**通道类型**: 媒体通道（Media Channel）

**消息类型**: AUDIO (5)

**TLV字段**:

| 标识符      | 类型    | 必填 | 描述           |
|-------------|---------|------|----------------|
| TYPE        | Integer | 是   | 消息类型       |
| AUDIO_DATA  | byte[]  | 是   | 音频数据(MP3)  |

**说明**: 音频数据添加帧头 `0x02` 后通过WebSocket推送

---

### 4.12 媒体通道 - 视频数据

**通道类型**: 媒体通道（Media Channel）

**消息类型**: VIDEO (6)

**TLV字段**:

| 标识符      | 类型    | 必填 | 描述           |
|-------------|---------|------|----------------|
| TYPE        | Integer | 是   | 消息类型       |
| VIDEO_DATA  | byte[]  | 是   | 视频数据(H.264)|
| FRAME_TYPE  | Integer | 是   | 帧类型，`1`=关键帧，`2`=非关键帧 |

**说明**: 视频数据添加帧头 `0x01 + frameType` 后通过WebSocket推送

---

## 5. 消息类型汇总

### 5.1 WebSocket消息类型

| 消息类型     | 方向       | 描述           |
|--------------|------------|----------------|
| login        | 浏览器→服务端 | 设备登录       |
| logout       | 浏览器→服务端 | 设备登出       |
| direction    | 浏览器→服务端 | 方向控制       |
| upload       | 浏览器→服务端 | 内容输入       |
| upload_file  | 浏览器→服务端 | 文件上传       |
| send_error   | 浏览器→服务端 | 错误埋点上报   |
| send_time    | 浏览器→服务端 | 使用时长埋点上报 |
| callback     | 服务端→浏览器 | 控制响应回调   |
| binary       | 服务端→浏览器 | 媒体流数据     |

### 5.2 TLV消息类型

| 消息类型       | 值  | 方向               | 描述           |
|----------------|-----|--------------------|----------------|
| LOGIN          | 1   | 客户端→服务端       | 登录           |
| HEARTBEATS     | 2   | 双向               | 心跳           |
| CONTROL        | 4   | 客户端→服务端       | 控制指令       |
| AUDIO          | 5   | 服务端→客户端       | 音频数据       |
| VIDEO          | 6   | 服务端→客户端       | 视频数据       |
| ACK            | 7   | 服务端→客户端       | 应答           |
| RETURN_MEDIA   | 9   | 服务端→客户端       | 返回媒体地址   |
| RETURN_CONTROL | 12  | 服务端→客户端       | 返回控制响应   |
| MESSAGE        | 13  | 客户端→服务端       | 消息传输       |
| UPLOAD_FILE    | 16  | 客户端→服务端       | 文件上传       |

---

## 6. 数据对象定义

### 6.1 DeviceLoginRequest

设备登录请求对象

```java
public class DeviceLoginRequest {
    private String imsi;                      // IMSI值
    private String imei;                      // IMEI值
    private String manufacturer;              // 厂商
    private String model;                     // 机型
    private String appType;                   // 应用类型
    private String extendModel;               // 扩展机型
    private String country;                   // 国家
    private String platform;                  // 平台类型
    private String width;                     // 屏幕宽度
    private String height;                    // 屏幕高度
    private String mcc;                       // 移动国家码
    private String mnc;                       // 移动网络码
    private String lac;                       // 位置区域码
    private String ci;                        // 小区ID
    private String rxlev;                     // 信号强度
    private String totalKb;                   // 总内存(KB)
    private String freeKb;                    // 可用内存(KB)
    private String clientLanguage;            // 客户端语言
    private String deviceType;                // 设备类型

    public String getSessionId() {
        return imei + "_" + imsi;
    }
}
```

### 6.2 DeviceLoginResponse

设备登录响应对象

```java
public class DeviceLoginResponse {
    private String token;                     // 登录Token
    private LocalDateTime expiresTime;         // Token过期时间
    private String tcpAddr;                   // 控制通道TCP地址
    private Long timeAxis;                    // 时间轴时间戳
    private Integer videoMode;                // 视频模式
    private String shortAddr;                 // 短地址
    private String nodeGateWayUrl;            // 节点网关URL
}
```

### 6.3 ClientEvent

客户端事件对象

```java
public class ClientEvent {
    private String hsman;                     // 厂商
    private String hstype;                    // 机型
    private String appType;                   // 应用类型
    private String imei;                      // IMEI值
    private String imsi;                      // IMSI值
    private Integer type;                     // 事件类型，1=繁忙，2=错误
}
```

### 6.4 UseTimesEvent

使用时长事件对象

```java
public class UseTimesEvent {
    private Long useTimes;                    // 使用时长（毫秒）
    private String hsman;                     // 厂商
    private String hstype;                    // 机型
    private String appType;                   // 应用类型
    private String appId;                     // 应用ID
    private Integer scheight;                 // 屏幕高度
    private Integer scwidth;                  // 屏幕宽度
    private String exttype;                   // 扩展类型
    private String imei;                      // IMEI值
    private String imsi;                      // IMSI值
    private Integer playMode;                 // 播放模式
}
```

### 6.5 CallbackMessage

控制回调消息对象

```java
public class CallbackMessage {
    private String type;                      // 消息类型
    private Integer elm;                      // 控制响应元素
    private Integer info;                     // 控制响应信息
    private String content;                   // 响应内容
    private Integer wt;                       // 输入框类型
}
```

---

## 7. 完整交互流程示例

### 7.1 登录交互流程

```
1. 浏览器建立WebSocket连接
   └─ ws://localhost:40002/app/websocket/6258412454025411_68510155565211

2. 浏览器发送login消息
   {
     "type": "login",
     "cs": "240x320",
     "dv": 2,
     "at": 5,
     "ga": "http://127.0.0.1:9090"
   }

3. 服务端调用GIDS认证
   ├─ POST /app-api/devicetcp/app/login/v1/gridLoginAuth
   ├─ POST /app-api/devicetcp/app/login/v1/gridLoginAuthOpenBrowser
   └─ POST /app-api/devicetcp/app/login/v1/deviceLoginAuth

4. 服务端返回登录响应
   {
     "code": 0,
     "data": {
       "token": "1234",
       "tcpAddr": "127.0.0.1:30001"
     },
     "msg": "登录成功"
   }

5. 服务端建立控制通道TCP连接
   └─ 连接到 127.0.0.1:30001

6. 服务端发送控制通道登录TLV报文
   TLVData {
     TYPE: 1,
     FACTORY: "default",
     DEV_TYPE: "default",
     IMSI: "68510155565211",
     IMEI: "6258412454025411",
     LCD_WIDTH: 240,
     LCD_HEIGHT: 320,
     APP_TYPE: 5,
     TOKEN: "1234",
     ...
   }

7. 服务端建立媒体通道TCP连接
   └─ 连接到媒体地址

8. 服务端发送媒体通道登录TLV报文
   TLVData {
     TYPE: 1,
     ... (同控制通道)
   }

9. 服务端开始推送媒体流
   ├─ 视频帧: 0x01 + frameType + H.264数据
   └─ 音频帧: 0x02 + MP3数据
```

### 7.2 控制指令交互流程

```
1. 浏览器用户点击"向上"按钮

2. 浏览器发送direction消息
   {
     "type": "direction",
     "ct": 1,
     "cv": 1
   }

3. 服务端通过控制通道发送TLV控制指令
   TLVData {
     TYPE: 4,
     CTRL_TYPE: 1,
     CTRL_VAL: 1,
     SESSION_ID: "6258412454025411_68510155565211"
   }

4. 设备返回控制响应

5. 服务端通过WebSocket推送回调消息
   {
     "type": "callback",
     "elm": 12,
     "info": 0,
     "content": "操作成功",
     "wt": 1
   }
```

### 7.3 内容输入交互流程

```
1. 浏览器用户输入"Hello World"

2. 浏览器发送upload消息
   {
     "type": "upload",
     "ut": 1,
     "content": "Hello World"
   }

3. 服务端通过控制通道发送TLV消息
   TLVData {
     TYPE: 13,
     UPLOAD_TYPE: 1,
     CONTENT: "Hello World",
     SESSION_ID: "6258412454025411_68510155565211"
   }

4. 设备返回输入响应

5. 服务端通过WebSocket推送回调消息
   {
     "type": "callback",
     "elm": 26,
     "info": 0,
     "content": "输入成功",
     "wt": 1
   }
```

---

## 8. 错误处理

### 8.1 WebSocket错误

| 错误类型     | 描述                  | 处理建议                   |
|--------------|-----------------------|----------------------------|
| 连接失败     | WebSocket无法连接     | 检查服务端是否启动         |
| 认证失败     | 登录失败              | 检查设备ID和GIDS地址       |
| 连接断开     | 网络中断              | 尝试重新连接               |
| 消息发送失败 | 消息无法发送          | 检查连接状态和消息格式     |

### 8.2 HTTP错误

| 错误码 | 描述             | 处理建议           |
|--------|------------------|--------------------|
| 400    | 请求参数错误     | 检查请求参数格式   |
| 401    | 认证失败         | 检查Token有效性    |
| 404    | 资源不存在       | 检查请求地址       |
| 500    | 服务器内部错误   | 查看服务器日志     |

### 8.3 TLV错误

| 错误类型     | 描述                | 处理建议                   |
|--------------|---------------------|----------------------------|
| 魔数错误     | 非法TLV报文         | 丢弃报文并记录日志         |
| 长度错误     | 数据长度不匹配      | 丢弃报文并记录日志         |
| 类型错误     | 不支持的字段类型    | 丢弃报文并记录日志         |

---

## 9. 性能指标

### 9.1 连接性能

| 指标名称        | 目标值       | 测量方法               |
|-----------------|--------------|------------------------|
| WebSocket连接时间 | < 100ms      | 从发起连接到收到Open事件 |
| TCP控制通道连接时间 | < 200ms     | 从发起连接到收到ACK      |
| TCP媒体通道连接时间 | < 200ms     | 从发起连接到收到ACK      |
| 总登录时间      | < 1000ms     | 从WebSocket连接到媒体流开始 |

### 9.2 传输性能

| 指标名称        | 目标值       | 测量方法               |
|-----------------|--------------|------------------------|
| 控制指令延迟    | < 50ms       | 从发送指令到收到响应     |
| 音频帧延迟      | < 100ms      | 从收到数据到播放开始     |
| 视频帧延迟      | < 200ms      | 从收到数据到渲染完成     |
| 心跳响应时间    | < 100ms      | 从发送心跳到收到ACK      |

### 9.3 资源占用

| 指标名称        | 目标值       | 测量方法               |
|-----------------|--------------|------------------------|
| 单连接内存占用  | < 10MB       | JVM内存使用情况         |
| 单连接CPU占用   | < 5%         | CPU使用率               |
| 带宽占用        | < 2Mbps      | 网络流量统计            |

---

## 10. 附录

### 10.1 完整TLV标识符映射表

请参考 `common/ID.java` 源代码

### 10.2 完整消息类型映射表

请参考 `common/Type.java` 源代码

### 10.3 术语表

| 术语       | 全称                       | 描述                      |
|------------|----------------------------|---------------------------|
| TLV        | Type-Length-Value          | 类型-长度-值协议          |
| IMEI       | International Mobile Equipment Identity | 国际移动设备识别码 |
| IMSI       | International Mobile Subscriber Identity | 国际移动用户识别码 |
| GIDS       | Global Identity Service       | 全局身份认证服务          |
| TCP        | Transmission Control Protocol | 传输控制协议              |
| WebSocket  | -                          | 全双工通信协议            |
| Netty      | -                          | Java异步事件驱动网络框架  |
| H.264      | -                          | 视频编码标准              |
| MP3        | MPEG Audio Layer-3          | 音频编码标准              |

---