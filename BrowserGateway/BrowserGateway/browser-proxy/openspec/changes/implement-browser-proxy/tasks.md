## 1. 项目结构与配置

- [x] 1.1 创建 `browser_proxy/` 包目录及 `__init__.py`
- [x] 1.2 创建 `browser_proxy/api/` 子包目录及 `__init__.py`
- [x] 1.3 编写 `requirements.txt`（fastapi、playwright、uvicorn、pillow 等完整依赖）
- [x] 1.4 编写 `pyproject.toml` 和 `setup.py`（包含 `browser_proxy` 命令行入口点）
- [x] 1.5 编写 `MANIFEST.in`

## 2. 核心封装层（common.py）

- [x] 2.1 实现 `ElementWrapper` 类（id、element、preview 属性，to_json 方法）
- [x] 2.2 实现 `PageWrapper` 类（id、page、url、cdp_session 属性，元素缓存，to_json 方法）
- [x] 2.3 实现 `ContextWrapper` 类（id、pages、current、context 属性，页面管理方法，close 含 storage_state）
- [x] 2.4 实现 `BrowserWrapper` 类（id、browser_type、browser、contexts 属性，上下文管理方法，close、is_active）
- [x] 2.5 定义 `BrowserType` 枚举（KEYS、TOUCH）
- [x] 2.6 实现全局 `browser_list` 列表和 `browser_get(browser_id)` 辅助函数

## 3. 日志配置（logger_config.py）

- [x] 3.1 实现 `logger_config.py`，配置结构化日志输出格式

## 4. 浏览器管理 API（browser.py）

- [x] 4.1 实现 `POST /api/browsers`：解析请求体，调用 `async_playwright().start()`，`launch_persistent_context`，创建 BrowserWrapper，加入 browser_list
- [x] 4.2 实现 `GET /api/browsers`：返回 browser_list 序列化列表
- [x] 4.3 实现 `GET /api/browsers/{browser_id}`：调用 browser_get，返回详情，404处理
- [x] 4.4 实现 `DELETE /api/browsers/{browser_id}`：调用 browser.close()，从 browser_list 移除，返回 204

## 5. 上下文管理 API（context.py）

- [x] 5.1 实现 `POST /api/browsers/{browser_id}/contexts`：创建 new_context，创建初始页面，导航到 url，创建 CDP Session，创建 ContextWrapper
- [x] 5.2 实现 `GET /api/browsers/{browser_id}/contexts`：返回浏览器下所有上下文列表
- [x] 5.3 实现 `GET /api/browsers/{browser_id}/contexts/{context_id}`：返回指定上下文详情，404处理
- [x] 5.4 实现 `DELETE /api/browsers/{browser_id}/contexts/{context_id}`：调用 context.close()，从浏览器移除，返回 204

## 6. 页面操作 API（page.py）

- [x] 6.1 实现 `POST .../pages`：创建新页面并导航，更新 current
- [x] 6.2 实现 `POST .../pages/goto`：导航当前页面到指定 URL
- [x] 6.3 实现 `POST .../pages/execute`：执行 JS 表达式，处理各种返回类型（string/int/float/bool/dict/list/element/none），元素结果缓存
- [x] 6.4 实现 `POST .../pages/execute_cdp`：通过 CDP Session 执行 Chrome DevTools Protocol 命令
- [x] 6.5 实现 `POST .../pages/find_element`：通过 CSS 选择器查找元素，缓存结果
- [x] 6.6 实现 `POST .../pages/element`：执行元素操作（send_key/set_file/focus）
- [x] 6.7 实现 `POST .../pages/element/{element_id}/get_size`：获取元素宽高
- [x] 6.8 实现 `DELETE .../pages/{page_id}`：关闭页面，更新 current 指针
- [x] 6.9 实现 `POST .../pages/go_back` 和 `POST .../pages/go_forward`
- [x] 6.10 实现 `POST .../pages/screenshot`：截图并返回 base64 PNG
- [x] 6.11 实现 `POST .../pages/scroll`：页面滚动（mouse.wheel）
- [x] 6.12 实现触摸操作：`POST .../pages/tap`、`POST .../pages/swipe`、`POST .../pages/touch_scroll`（通过 CDP Input 事件）
- [x] 6.13 实现 Cookie 管理：`GET/POST/DELETE .../pages/cookies`

## 7. 应用入口（main.py）

- [x] 7.1 创建 FastAPI app 实例，挂载 browser/context/page 路由
- [x] 7.2 实现 `browser_proxy` 命令行入口，使用 uvicorn 启动服务（host、port 可配置）
- [x] 7.3 配置全局异常处理中间件（可选，统一 500 错误格式）

## 8. 验证

- [ ] 8.1 安装依赖并启动服务（`pip install -e . && browser_proxy`）
- [ ] 8.2 使用 curl 验证创建浏览器、创建上下文、执行 JS、截图等核心流程
