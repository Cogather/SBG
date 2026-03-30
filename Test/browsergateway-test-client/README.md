# browsergateway-test-client

Python 自动化测试客户端，用于对 mobile 服务进行端到端功能测试。

## 目录结构

```
browsergateway-test-client/
├── src/
│   ├── client/
│   │   └── mobile_client.py   # WebSocket 测试客户端封装
│   ├── health/
│   │   ├── check_services.py  # 依赖端口探针（run_tests / 看板）
│   │   ├── ws_smoke.py        # 可选 WS 连接+登录 smoke
│   │   └── history.py         # 跑测后追加 history.jsonl
│   ├── mock/
│   │   └── gids_mock_server.py  # GIDS Mock Server
│   └── protocol/
│       ├── tlv.py             # TLV 编解码
│       └── message.py         # 消息类型常量
├── dashboard/
│   ├── index.html             # 评测看板（解析 junit + 探针）
│   └── dashboard-config.json  # mobile 控制台 URL、环境标签等
├── reports/                   # 产物目录（见下表，大文件已 .gitignore）
├── tests/
│   ├── conftest.py            # pytest fixtures；会话中写 reports/e2e-progress.json（看板实时进度）
│   ├── test_login.py          # 登录 / 登出测试
│   ├── test_heartbeat.py      # 心跳保活测试（可配置静默时长 / 长保活）
│   ├── test_control.py        # 按键 + 触屏控制测试
│   ├── test_media.py          # 媒体流接收测试
│   └── test_http_blackbox.py  # HTTP 黑盒：鉴权 / stats / 限流（GIDS Mock）
├── config/
│   └── config.yaml            # 服务地址配置
├── probe_services.py          # 写 reports/probe-result.json
├── pytest.ini
├── run_tests.py
├── dashboard_runner.py        # 看板触发的后台：run_tests.py + 可选 probe_services.py
├── serve_dashboard.py         # 静态站 + 本机 /api/run-tests*
└── requirements.txt
```

### 测试产物路径（SSOT：JUnit）

| 文件 | 说明 |
|------|------|
| `reports/junit.xml` | pytest `--junitxml`，用例状态与耗时（看板与流水线解析） |
| `reports/report.html` | pytest-html 自包含 HTML 人类阅读 |
| `reports/probe-result.json` | `python probe_services.py` 生成的端口探针（可选 `--ws-smoke`） |
| `reports/history.jsonl` | 每次 pytest 会话正常结束由 `conftest` 追加一行摘要（通过率趋势） |
| `reports/run-tests-last.log` | 看板「执行端到端测试」或 API 触发的最近一次完整控制台输出 |
| `reports/e2e-progress.json` | pytest 运行期间由 `conftest` 原子写入；`serve_dashboard` 在任务进行中并入 `GET /api/run-tests/status` 的 `progress` 字段（`.gitignore`） |

约定：CI 将 `reports/` 作为 **artifact** 上传（见仓库 `.github/workflows/browsergateway-test-client.yml`）。完整 E2E 需在已启动 mobile / gateway 的环境执行；默认 workflow 仅保证依赖安装与探针 JSON 上传。

## 前置条件

| 依赖 | 说明 |
|------|------|
| Python 3.9+ | 运行测试客户端 |
| mobile 服务 | 需在 `localhost:40002` 运行（WebSocket） |
| browser-gateway | 需在 `localhost:30001` 运行（TCP 控制通道） |
| GIDS Mock | 由 pytest fixture 自动启动（端口 9090） |

> 一键启动所有服务：在仓库根目录执行 `start-dev.bat`

## 环境安装（首次）

```bat
cd Test\browsergateway-test-client
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
```

## 运行测试

```bat
# 激活虚拟环境
.venv\Scripts\activate

# 运行全部测试
pytest tests/ -v

# 只运行登录测试
pytest tests/test_login.py -v

# 只运行控制测试
pytest tests/test_control.py -v

# 跳过媒体流测试（无推流环境时）
pytest tests/ -v --ignore=tests/test_media.py
```

`pytest.ini` 已默认附带 `--junitxml=reports/junit.xml` 与 `--html=reports/report.html --self-contained-html`；首次运行前会自动创建 `reports/`（见 `conftest.py`）。

### 依赖探针与看板

```bat
# 生成探针 JSON（供 dashboard 读取）
python probe_services.py

# 可选：附带 WebSocket 连接 + 登录 smoke（需 mobile 与 GIDS 已就绪）
python probe_services.py --ws-smoke
```

**一键（推荐）**：先在本仓库根目录用 `start-dev.bat` 起全栈，再执行：

```bat
cd Test\browsergateway-test-client
start-dashboard.bat
```

将新开窗口启动 `http.server 8765`、打开浏览器看板，并在当前窗口运行 `run_tests.py`，结束后自动写 `probe-result.json`。也可传 pytest 参数，例如 `start-dashboard.bat -k login`。

**手动**：在本目录执行 `python serve_dashboard.py`（可加 `--open-browser`）。若出现 **WinError 10013**，多为 Windows 保留端口段或权限问题，脚本会自动尝试其它端口；也可显式指定 `python serve_dashboard.py --port 27654`。勿在仓库根目录用 `python -m http.server`，否则 `/dashboard/` 会 **404**。  

编辑 `dashboard/dashboard-config.json` 中的 `mobile_console_base_url`（默认 `http://127.0.0.1:8088/`），「打开云手机控制台」按钮将新开标签页跳转该地址。

### 看板内「执行端到端测试」

通过 **`serve_dashboard.py` 打开的** 看板页（`http://127.0.0.1:<端口>/dashboard/`）提供 **「执行端到端测试」** 按钮：`dashboard_runner` 在后台线程执行与 `run_tests.py` 等价的流程（`pytest tests/ -v` + 你的附加参数），默认结束后还会跑 **`probe_services.py`** 写探针；控制台输出写入 `reports/run-tests-last.log`。

**HTTP API（仅本机回环）**

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/run-tests` | JSON：`check_services`、`run_probe`、`pytest_args`（字符串数组，与命令行 pytest 附加参数一致） |
| `GET` | `/api/run-tests/status` | `running`、`exit_code`、`error_message` 等；**任务进行中**且存在 `e2e-progress.json` 时附带 **`progress`**（`order` / `current` / `completed` / `pytest_finished`） |
| `GET` | `/api/run-tests/log?tail=…` | 最近一次跑测日志尾部 |

**看板交互（与当前实现对齐）**

- **防误触**：点击后按钮立即 **`disabled`**，任务结束或请求/轮询失败后再恢复；轮询约 **800ms** 一次。
- **用例表实时状态**：点击后先用当前页已加载的 **`junit.xml` 用例列表** 作为乐观清单（**待执行** / **运行中**），避免长时间停在「正在收集」；pytest 收集结束后以服务端 **`order`** 为准（例如使用 `-k` 子集时与上次 junit 不一致会自动纠正）。**运行中**行与 KPI 随 `progress` 更新。
- **阶段提示**：pytest 结束后若仍在跑探针，状态栏会提示 **「pytest 已完成，探针/收尾中…」**，此时表格已可刷新为最新 junit。
- **单列耗时**：亚秒级用例在 junit 里常为 `0.00x` 秒；看板对 **不足 1 秒** 的用例显示 **三位** 小数，避免被误认为「0 秒未执行」。**✓ 通过** 即表示该用例逻辑已执行成功。

- **跳过依赖端口检查**：仅跑 `tests/test_http_blackbox.py` 等不依赖 mobile/gateway 的用例时勾选；等价环境变量 `E2E_SKIP_SERVICE_CHECK=1`（`run_tests.py` / 看板均支持）。

命令行等价调用示例：

```bat
curl -X POST http://127.0.0.1:8765/api/run-tests -H "Content-Type: application/json" -d "{\"check_services\":false,\"pytest_args\":[\"tests/test_http_blackbox.py\"]}"
```

## 测试用例清单

### 1. 登录 / 登出测试 (`test_login.py`)

| 用例 ID | 用例名称 | 描述 | 断言 |
|---------|----------|------|------|
| LOGIN-01 | `test_login_success` | session 级 client 已登录，验证连接可正常使用 | 发送方向消息无异常 |
| LOGIN-02 | `test_logout` | 登录成功后正常断开，使用相同 IMEI/IMSI 重新登录 | 断开重连后仍能正常登录，无错误响应 |

### 2. 心跳保活测试 (`test_heartbeat.py`)

| 用例 ID | 用例名称 | 描述 | 断言 |
|---------|----------|------|------|
| HB-01 | `test_connection_stays_alive` | 登录后静默 `E2E_IDLE_SECONDS`（默认 15s），验证连接未被服务端断开 | 等待后仍可发送方向消息且无异常 |
| HB-LONG-01 | `test_connection_stays_alive_long` | 长静默保活（默认需 `E2E_RUN_LONG_IDLE=1`，`E2E_IDLE_SECONDS` 默认 600） | 标记 `slow`；仅 nightly 建议开启 |

### 3. 按键控制测试 (`test_control.py`)

| 用例 ID | 用例名称 | 描述 | 参数 | 断言 |
|---------|----------|------|------|------|
| CTRL-01~06 | `test_key_event` | 方向键/功能键事件测试 | UP(12), DOWN(13), LEFT(14), RIGHT(15), ENTER(20), BACK(18) | 发送成功且连接不关闭（无异常） |
| CTRL-07 | `test_menu_key` | 菜单键事件测试 | MENU(17) | 发送成功且连接不关闭（无异常） |
| CTRL-08~17 | `test_numeric_keys` | 数字键 0-9 事件测试 | 0-9 | 发送成功且连接不关闭（无异常） |
| CTRL-18 | `test_touch_tap` | 单点触摸点击测试 | ACTION_DOWN(0) → ACTION_UP(1) | 发送成功且连接不关闭（无异常） |
| CTRL-19 | `test_touch_swipe` | 滑动手势测试 | DOWN → 3×MOVE → UP | 发送成功且连接不关闭（无异常） |

### 4. 媒体流测试 (`test_media.py`)

| 用例 ID | 用例名称 | 描述 | 断言 |
|---------|----------|------|------|
| MEDIA-01 | `test_browser_opened_after_login` | 登录后 30s 内验证浏览器成功打开 | 收到至少一帧视频数据（首字节 0x01）；无流时 skip |
| MEDIA-02 | `test_video_frame_format` | 视频帧格式验证 | 首字节 0x01，第二字节为 frameType（0/1/2 均合法），有 H.264 payload；无流时 skip |
| MEDIA-03 | `test_audio_frame_format` | 音频帧格式验证 | 首字节 0x02，其后有 MP3 数据；无流时 skip |

> **注意**：媒体测试依赖真实推流环境；无云手机推流时三个用例均自动 **skip**（不计入失败）。

### 5. HTTP 黑盒防护网 (`test_http_blackbox.py`)

默认请求 **GIDS Mock**（`127.0.0.1:9090`），与 [case.md](BrowserGateway/BrowserGateway/browser-gateway/黑盒测试用例/case.md) 中鉴权 / 限流 / stats 导出等契约对齐（插件侧由 Mock 模拟）。看板「编号」列与下表一致（`PLAN-H-01` … `PLAN-H-13`）。

| 编号 | 说明 |
|------|------|
| PLAN-H-01～H-08 | `importIMEIList` 负面与边界：缺 operation、非法 operation、空文件、超大、非法 CSV 行、成功导入后与 export 一致、缺 file |
| PLAN-H-09 | `exportIMEIList`：白名单为空时 200 空 CSV |
| PLAN-H-10～H-11 | `exportStaticData/{month}`：非法月失败；合法 `YYYY-MM` 返回 CSV 结构 |
| PLAN-H-12～H-13 | 限流 429：`authIMEI` 与 `control/file/upload`（Mock 默认小阈值便于测，可通过 `POST /config` 调整） |

联调真实网关时设置环境变量：`BLACKBOX_HTTP_BASE=http://网关:端口`。

## 测试说明

| 测试文件 | 覆盖场景 | 前置要求 |
|----------|----------|----------|
| `test_login.py` | 登录握手、登出重连 | mobile + GIDS Mock |
| `test_heartbeat.py` | 心跳保活、可选长保活 | mobile + GIDS Mock + browser-gateway |
| `test_control.py` | 方向键、功能键、数字键、触屏点击/滑动 | mobile + GIDS Mock + browser-gateway |
| `test_media.py` | 浏览器打开验证（视频帧）、帧格式校验 | 全栈（含推流） |
| `test_http_blackbox.py` | HTTP 鉴权 / stats / 限流 | **仅 GIDS Mock**（pytest 自动拉起） |

> 媒体测试（`test_media.py`）全部在无推流环境下自动 **skip**，不计入失败。有真实云手机推流时可验证帧格式。

## 配置

编辑 `config/config.yaml` 修改服务地址：

```yaml
server:
  address: 127.0.0.1
  websocket:
    media_port: 40002   # mobile WebSocket 端口
```
