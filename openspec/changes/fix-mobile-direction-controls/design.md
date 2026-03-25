## Context

方向控制消息链路：mobile 前端 → WebSocket(40002) → `BrowserContext.handleDirection` → TCP(30001) → `ControlTcpServerHandler.processDefault` → `RemoteImpl.handleEvent` → `UserChrome.muenDriver.Handle`。

调查发现控制事件未到达云浏览器，根因是 mobile 前端 `sendDirection` 发送的 ct 值（1/2/3/4）与 BGW MuenDriver SDK 约定（ct=0，cv 携带方向码）不匹配。此外音视频解码参数、ACK 超时、GIDS mock 端点均存在缺陷。

当前问题点汇总：
1. `sendDirection`：ct=1/2/3/4，BGW 期望 ct=0, cv=12/13/14/15。
2. `sendFunctionKey`：ct 值错误，应为 ct=0, cv=17/18。
3. `sendNum`：包含无法序列化为 Integer 的 `*`/`#` 按键。
4. `ControlChannelHandler.send()`：`latch.await()` 无超时，可能永久阻塞。
5. 触屏监听器在所有 inputMode 下均注册，应仅限 inputMode=2。
6. VideoDecoder 参数：codec level 错误，缺少 `optimizeForLatency`，使用了 `width`/`height` 而非 `codedWidth`/`codedHeight`。
7. 音频解码：无缓冲区积累即解码，无并发守卫，旧节点未停止。
8. GIDS mock 缺少 `sendClientEvent` 和 `file/upload` 端点。
9. urlConfigList 顺序与参考实现不一致。

## Goals / Non-Goals

**Goals:**
- 修复方向控制 ct/cv 映射，使控制消息能被 BGW 正确路由。
- 修复 ACK 超时防止控制通道永久阻塞。
- 修复音视频解码参数，提升播放稳定性。
- 补全 GIDS mock 端点，使 sendError/sendUseTime/文件上传正常工作。
- 统一 urlConfigList 顺序。

**Non-Goals:**
- 不修改 browser-gateway 代码。
- 不修改 TLV 协议或 mobile 后端编码逻辑。
- 不引入 time.html / upload.html（不影响当前场景）。

## Decisions

### 决策 1：ct=0，cv 携带方向码

**选择**：`sendDirection` 统一使用 ct=0，cv 按 MuenDriver SDK 约定映射（up=12, down=13, left=14, right=15, ok=20）。`sendFunctionKey` 同理（menu=17, back=18）。

**理由**：BGW `ControlTcpServerHandler` 按此约定解析 CTRL_TYPE/CTRL_VAL，前端必须对齐。

### 决策 2：删除 `*`/`#` 按键

**选择**：直接从数字键盘删除这两个按钮。

**理由**：`Message.java` 的 `cv` 字段类型为 `Integer`，字符串 `"*"`/`"#"` 无法反序列化，会导致后端解析异常。当前场景不需要这两个按键。

### 决策 3：ACK 超时改为 5 秒

**选择**：`latch.await(5, TimeUnit.SECONDS)`，超时打 warn 日志后返回。

**理由**：无超时的 await 在 BGW 未回 ACK 时会永久阻塞控制通道线程，导致后续所有方向控制失效。

### 决策 4：urlConfigList 按 appType 升序

**选择**：`gids_mock_server.py` 和 `index.html` 均按 appType 数值升序排列。

**理由**：与参考实现保持一致，便于对照和维护。

## Risks / Trade-offs

- **[风险] 音频 2KB 缓冲引入轻微延迟** → 可接受；避免频繁的小块解码失败。
- **[Trade-off] 删除 `*`/`#` 按键** → 当前场景不需要；若未来需要，需先在后端 Message.java 支持字符串 cv。
