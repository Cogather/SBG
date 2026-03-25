## 已完成任务

## 1. 修复 mobile 前端方向控制 ct/cv 映射

- [x] 1.1 在 `index.html` `sendDirection` 中将 ct=1/2/3/4 改为 ct=0，cv 按 up=12/down=13/left=14/right=15/ok=20 映射
- [x] 1.2 在 `index.html` `sendFunctionKey` 中将 menu/back 改为 ct=0, cv=17/18
- [x] 1.3 在 `index.html` `sendNum` 中改为发送 `{type:'direction', ct:0, cv:num}`
- [x] 1.4 删除数字键盘中的 `*` 和 `#` 按钮（cv 无法序列化为 Integer）

## 2. 修复 ControlChannelHandler ACK 超时

- [x] 2.1 在 `ControlChannelHandler.send()` 中将 `latch.await()` 改为 `latch.await(5, TimeUnit.SECONDS)`
- [x] 2.2 超时时打印 warn 日志，正常 ack 时打印 info 日志

## 3. 修复触屏模式监听器注册

- [x] 3.1 canvas mousedown/mouseup/mousemove 监听器仅在 inputMode=2 时注册
- [x] 3.2 inputMode=1（按键）时不注册任何 canvas 鼠标事件

## 4. 修复 VideoDecoder 配置

- [x] 4.1 codec 改为 `avc1.42E01F`（Level 3.1）
- [x] 4.2 使用 `codedWidth`/`codedHeight` 替代 `width`/`height`
- [x] 4.3 添加 `optimizeForLatency: true`

## 5. 修复音频解码

- [x] 5.1 累积音频 chunk 至 2KB（MIN_BUFFER_SIZE = 2 * 1024）后再调用 decodeAudioData
- [x] 5.2 添加 `isDecoding` 守卫防止并发解码
- [x] 5.3 解码成功后停止旧 AudioBufferSourceNode 再启动新节点

## 6. 补全 GIDS mock 端点

- [x] 6.1 新增 `POST /app-api/center/public/client/sendClientEvent` → `{"code":0,"msg":"success","data":{}}`
- [x] 6.2 新增 `POST /app-api/control/file/upload?fileName=<name>` → `{"code":0,"msg":"success","data":"/tmp/<name>"}`

## 7. 对齐 urlConfigList 顺序

- [x] 7.1 `gids_mock_server.py` urlConfigList 按 appType 升序：1→2→3→5→6→8→10→12→18→920425
- [x] 7.2 `index.html` appType select 选项按相同顺序排列
