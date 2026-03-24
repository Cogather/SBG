## 1. 引入 Muen SDK biz jar

- [ ] 1.1 将 `browser-module-sdk-biz-0.0.22.jar` 从 `D:/Code/核心网简版插件版本 - v0.0.22/核心网简版插件版本 - v0.0.22/` 复制到 `BrowserGateway/browser-gateway/src/main/resources/lib/`
- [ ] 1.2 在 `pom.xml` 中以 `systemPath` 方式新增 biz jar 依赖（参考已有 api jar 的引入方式）
- [ ] 1.3 验证 BrowserGateway 编译通过，无 `ClassNotFoundException`

## 2. BrowserGateway local profile 配置

- [ ] 2.1 在 `application-local.yaml` 中禁用 TLS TCP 服务（`enable-http: true`，TLS 端口不启动）
- [ ] 2.2 实现 `@Profile("local")` 的 Redis stub Bean，以静态默认值替代 `config:chromeConfigList` 读取（240x320、10fps、1000kbps、48000Hz、recordMode=1）
- [ ] 2.3 禁用 K8s 服务发现和注册相关 Bean（local profile 下跳过或 stub）
- [ ] 2.4 配置 `gids.endpoint=localhost:9090`（指向本地 GIDS Mock）
- [ ] 2.5 配置 `browsergw.chrome.endpoint=http://127.0.0.1:8000`（指向本地 browser-proxy）
- [ ] 2.6 配置 `browsergw.chrome.executable-path` 指向本地 Chrome 可执行文件路径
- [ ] 2.7 配置 `browsergw.chrome.record-extension-id` 和扩展加载路径（指向 `muen-v0.0.22` 扩展目录）
- [ ] 2.8 以 `spring.profiles.active=local` 启动 BrowserGateway，确认端口 30001、30011、8090 监听就绪

## 3. GIDS Mock 接口补全

- [ ] 3.1 确认 `gids_mock_server.py` 已实现 `GET /user-bind/v1/{sessionID}`，返回含 IMEI/IMSI 的绑定信息
- [ ] 3.2 确认或新增 `POST /user-bind/v1/{sessionID}` 接口
- [ ] 3.3 确认或新增 `GET /plugin/v1/current` 接口，返回含 Chrome 启动参数和扩展路径的插件配置
- [ ] 3.4 确认 `POST /stats/v1/traffic/media`、`/stats/v1/traffic/control`、`POST /stats/v1/session` 接口均返回 HTTP 200
- [ ] 3.5 启动 GIDS Mock（端口 9090），确认 `/health` 返回正常

## 4. browser-proxy 本地启动验证

- [ ] 4.1 在 `BrowserGateway/browser-proxy/` 下创建或激活 `.venv` 虚拟环境，安装依赖（`pip install -r requirements.txt`）
- [ ] 4.2 启动 browser-proxy（`python -m browser_proxy.main --port 8000`），确认端口 8000 监听就绪
- [ ] 4.3 手动调用 `POST /api/browsers` 接口（携带 Chrome 路径和扩展参数），确认 Chrome 进程启动且 record 扩展加载成功

## 5. 端到端登录流程验证

- [ ] 5.1 启动全部依赖服务：GIDS Mock（9090）→ browser-proxy（8000）→ BrowserGateway（local profile）
- [ ] 5.2 使用 mobile（或测试客户端）建立 TCP 控制流连接（端口 30001）
- [ ] 5.3 通过 TCP 控制流发送登录请求（携带 IMEI/IMSI/Token），观察 BrowserGateway 日志确认三步 GIDS 鉴权完成
- [ ] 5.4 确认 BrowserGateway 通过 browser-proxy 启动 Chrome 并加载 record 扩展
- [ ] 5.5 确认 record 扩展与 BrowserGateway 建立 WebSocket 连接（`/browser/websocket/{imeiAndImsi}`）
- [ ] 5.6 确认 BrowserGateway 向 mobile 推送 TCP 媒体流地址
- [ ] 5.7 建立 TCP 媒体流连接（端口 30011），确认持续收到视频帧（`[0x01, frameType] + H.264`）和音频帧（`[0x02] + MP3`）

## 6. 端到端操作流程验证

- [ ] 6.1 登录完成后，通过 TCP 控制流发送方向键（上/下/左/右）指令，确认 BrowserGateway 日志记录接收
- [ ] 6.2 确认指令经 SDK 转发至 browser-proxy，Chrome 通过 CDP 执行对应操作
- [ ] 6.3 确认操作结果标记（如 `back_event` 等）经 TCP 控制流回传 mobile
- [ ] 6.4 确认操作后 record 扩展推送更新画面帧，mobile 媒体流收到新画面
- [ ] 6.5 发送确认/返回指令，重复上述验证

## 7. 测试脚本整合

- [ ] 7.1 在 `Test/browsergateway-test-client/start_e2e_test.py` 中补充或调整登录流程自动化验证用例
- [ ] 7.2 在 `Test/browsergateway-test-client/start_e2e_test.py` 中补充操作流程自动化验证用例
- [ ] 7.3 一键执行完整 E2E 测试，输出明确的通过/失败结论和日志摘要
