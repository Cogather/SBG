## Why

点击 mobile 界面的方向按钮（上/下/左/右）后，云浏览器中的网页没有任何反应。根因调查表明，mobile 前端发送的 ct 值与 BGW MuenDriver SDK 约定不匹配，且控制事件链路存在多处缺陷（ACK 无超时保护、GIDS mock 缺少必要端点、音视频解码参数不正确）。

## What Changes

- **修复 `sendDirection` ct/cv 映射**：前端原来发送 ct=1/2/3/4，BGW MuenDriver SDK 要求 ct=0，cv 携带方向码（up=12, down=13, left=14, right=15, ok=20）。
- **修复 `sendFunctionKey`**：menu/back 改为 ct=0, cv=17/18。
- **修复 `sendNum`**：发送 `{type:'direction', ct:0, cv:num}`，删除无法序列化为 Integer 的 `*` 和 `#` 按钮。
- **修复 `ControlChannelHandler.send()` ACK 超时**：将无限期 `latch.await()` 改为 `latch.await(5, TimeUnit.SECONDS)`，超时后打 warn 日志。
- **修复触屏模式监听器注册**：仅在 inputMode=2 时注册 canvas 鼠标事件，inputMode=1 下不注册。
- **修复 VideoDecoder 配置**：codec 改为 `avc1.42E01F`，使用 `codedWidth`/`codedHeight`，添加 `optimizeForLatency: true`。
- **修复音频解码**：累积 2KB 后再解码，添加 `isDecoding` 守卫，停止旧 AudioBufferSourceNode 再播新的。
- **补充 GIDS mock 端点**：新增 `POST /app-api/center/public/client/sendClientEvent` 和 `POST /app-api/control/file/upload`。
- **对齐 urlConfigList 顺序**：`gids_mock_server.py` 和 `index.html` appType 下拉均按 appType 升序排列。

## Capabilities

### Modified Capabilities
- `local-plugin-stub`: mobile 前端方向控制、功能键、触屏、音视频解码逻辑与 BGW SDK 约定对齐；GIDS mock 补全缺失端点。

## Impact

- **mobile/src/main/resources/static/index.html**
  - `sendDirection`：ct=0, cv=12/13/14/15/20
  - `sendFunctionKey`：ct=0, cv=17/18
  - `sendNum`：ct=0, cv=num；删除 `*`/`#` 按钮
  - 触屏监听器仅 inputMode=2 时注册
  - VideoDecoder：avc1.42E01F + codedWidth/codedHeight + optimizeForLatency
  - 音频：2KB buffer + isDecoding guard + stop previous source
  - appType 选项按升序排列
- **mobile/src/main/java/com/huawei/mobile/ControlChannelHandler.java**
  - `send()`：`latch.await(5, TimeUnit.SECONDS)` + warn log
- **Test/browsergateway-test-client/src/mock/gids_mock_server.py**
  - 新增 `POST /app-api/center/public/client/sendClientEvent`
  - 新增 `POST /app-api/control/file/upload`
  - urlConfigList 按 appType 升序排列
