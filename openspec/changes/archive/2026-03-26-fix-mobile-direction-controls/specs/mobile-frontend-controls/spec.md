## ADDED Requirements

### Requirement: Direction control ct/cv values match BGW MuenDriver SDK
The mobile frontend `sendDirection` function SHALL send CTRL_TYPE=0 with CTRL_VAL carrying the direction code matching the MuenDriver SDK contract: up=12, down=13, left=14, right=15, ok=20.

#### Scenario: Down button triggers browser scroll
- **WHEN** user clicks the down direction button in the mobile UI
- **THEN** a TLV CONTROL message with ct=0, cv=13 is sent over the TCP control channel and the cloud browser page scrolls down

#### Scenario: Up button triggers browser scroll
- **WHEN** user clicks the up direction button in the mobile UI
- **THEN** a TLV CONTROL message with ct=0, cv=12 is sent and the cloud browser page scrolls up

### Requirement: Function keys (menu/back) use correct ct/cv
The mobile `sendFunctionKey` SHALL send ct=0 with cv=17 for menu and cv=18 for back.

#### Scenario: Menu key sends correct TLV
- **WHEN** user clicks the 菜单 button
- **THEN** a TLV CONTROL message with ct=0, cv=17 is sent

#### Scenario: Back key sends correct TLV
- **WHEN** user clicks the 返回 button
- **THEN** a TLV CONTROL message with ct=0, cv=18 is sent

### Requirement: Numeric keypad sends direction TLV
The mobile `sendNum` SHALL send `{type:'direction', ct:0, cv:num}`. The \* and # buttons SHALL NOT be present as they cannot be serialized to Integer cv.

### Requirement: Touch mode only active when inputMode=2
Mouse event listeners (mousedown/mouseup/mousemove) on the canvas SHALL only be attached when the user selects inputMode=2 (触屏). In inputMode=1 (按键), no touch events are registered.

### Requirement: ACK wait in ControlChannelHandler has timeout
The `ControlChannelHandler.send()` method SHALL wait for ACK with a maximum timeout of 5 seconds. If no ACK is received within the timeout, it SHALL log a warning and return without blocking indefinitely.

#### Scenario: ACK not received within timeout
- **WHEN** no ACK is received within 5 seconds
- **THEN** `send()` logs a warning and returns, allowing subsequent control messages to be sent

### Requirement: GIDS mock provides sendClientEvent endpoint
The GIDS mock server SHALL expose `POST /app-api/center/public/client/sendClientEvent` returning `{"code": 0, "msg": "success", "data": {}}` so that mobile `sendError` and `sendUseTime` calls do not fail.

### Requirement: GIDS mock provides file upload endpoint
The GIDS mock server SHALL expose `POST /app-api/control/file/upload?fileName=<name>` returning `{"code": 0, "msg": "success", "data": "/tmp/<name>"}` so that mobile file upload flow works end-to-end.

### Requirement: urlConfigList sorted by appType ascending
The `urlConfigList` in both `gids_mock_server.py` `/config/v1` response and the `appType` select in `index.html` SHALL be ordered by appType ascending: 1(Youtube) → 2(TikTok) → 3(FaceBook) → 5(BBC) → 6(Upload) → 8(Ins) → 10(SNAPCHAT) → 12(Google) → 18(CNN) → 920425(TELE).

### Requirement: VideoDecoder configured with correct codec and parameters
The mobile frontend VideoDecoder SHALL be configured with codec `avc1.42E01F`, `codedWidth`/`codedHeight` (not `width`/`height`), and `optimizeForLatency: true`.

### Requirement: Audio buffered before decoding
Audio chunks SHALL be accumulated until buffer size reaches 2KB (`MIN_BUFFER_SIZE = 2 * 1024`) before calling `decodeAudioData`. An `isDecoding` guard SHALL prevent concurrent decode calls. The previous AudioBufferSourceNode SHALL be stopped before starting a new one.
