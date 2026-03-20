## Why

Browser Proxy 服务需要从头实现：提供一个基于 FastAPI 和 Playwright 的浏览器代理服务，通过 RESTful HTTP API 管理和控制浏览器实例，适用于云手机、远程浏览器控制、自动化测试等场景。

## What Changes

- **新增** `browser_proxy/` Python 包，包含完整的 FastAPI 应用
- **新增** 浏览器生命周期管理 API（创建、查询、删除浏览器实例）
- **新增** 浏览器上下文管理 API（创建、查询、删除 Context）
- **新增** 页面操作 API（JavaScript 执行、元素查找、点击、输入、截图、滚动、触摸等）
- **新增** 公共组件：`BrowserWrapper`、`ContextWrapper`、`PageWrapper`、`ElementWrapper`
- **新增** 异常处理机制和统一错误响应
- **新增** 日志配置模块
- **新增** 项目打包配置（`pyproject.toml`、`setup.py`、`requirements.txt`）

## Capabilities

### New Capabilities

- `browser-management`: 浏览器实例的创建、列表查询、详情查询、删除，支持 KEYS/TOUCH 模式、headless、扩展、用户数据目录等参数
- `context-management`: 浏览器上下文（Context）的创建、列表查询、详情查询、删除，支持 viewport、URL 导航
- `page-operations`: 页面级操作，包括 JavaScript 执行、元素查找（XPath/CSS/文本）、点击、文本输入、截图、页面滚动、触摸操作（tap/swipe/scroll）、Cookie 管理、CDP 原始命令
- `core-wrappers`: 核心封装类 BrowserWrapper/ContextWrapper/PageWrapper/ElementWrapper，管理 Playwright 对象生命周期

### Modified Capabilities

（无已有 Spec 需要修改）

## Impact

- **新建文件**：`browser_proxy/__init__.py`、`browser_proxy/main.py`、`browser_proxy/api/__init__.py`、`browser_proxy/api/browser.py`、`browser_proxy/api/context.py`、`browser_proxy/api/page.py`、`browser_proxy/api/common.py`、`browser_proxy/api/logger_config.py`
- **配置文件**：`requirements.txt`（已存在，需补全）、`setup.py`、`pyproject.toml`、`MANIFEST.in`
- **依赖**：FastAPI 0.116.1、Playwright 1.53.0、Uvicorn 0.35.0、Pillow（截图处理）
- **API 端点**：`/api/browsers`、`/api/browsers/{id}/contexts`、`/api/browsers/{id}/contexts/{ctx_id}/pages/*`
