## Context

Browser Proxy 是一个全新的 Python 服务，从零开始实现。当前仓库中只有设计文档（`doc/`）和测试用例文档，没有任何源代码。需要根据设计文档完整实现该服务。

技术栈固定：FastAPI 0.116.1 + Playwright 1.53.0 + Uvicorn 0.35.0，异步编程模型（async/await）。

## Goals / Non-Goals

**Goals:**
- 实现完整的 `browser_proxy` Python 包，包含所有 API 端点
- 实现核心封装类：BrowserWrapper、ContextWrapper、PageWrapper、ElementWrapper
- 支持浏览器生命周期管理（创建、查询、删除）
- 支持上下文管理（创建、查询、删除）
- 支持页面操作（JS执行、元素操作、截图、触摸、CDP命令等）
- 提供完整的打包配置（pyproject.toml、setup.py）

**Non-Goals:**
- 不实现认证/鉴权机制
- 不实现数据库持久化（状态仅在内存中，支持文件系统状态保存）
- 不实现集群/分布式部署
- 不实现 WebSocket 实时推送

## Decisions

### 决策1：使用 `launch_persistent_context` 而非 `launch`

设计文档明确要求用 Playwright 的 `launch_persistent_context` 创建浏览器，这样可以直接获得一个持久化的 BrowserContext（同时也是 Browser），支持用户数据目录持久化。替代方案（`launch` + `new_context`）不支持持久化扩展安装。

### 决策2：全局 `browser_list` 列表管理实例

使用模块级全局列表 `browser_list: List[BrowserWrapper] = []` 存储所有浏览器实例，提供 `browser_get(browser_id)` 辅助函数。简单直接，无需引入数据库或缓存层。并发安全性通过 FastAPI 异步单线程模型保证。

### 决策3：元素缓存在 PageWrapper 中

元素查找结果缓存在 `PageWrapper._elements` 字典中，通过 UUID 引用。客户端持有 element_id，后续操作（click、send_key、get_size）直接通过 ID 从缓存中取元素，避免重复查找。这是设计文档规定的模式。

### 决策4：包结构为可安装 Python 包

通过 `pyproject.toml` + `setup.py` 提供 `browser_proxy` 命令行入口点，便于在容器和 systemd 服务中使用 `pip install -e .` 安装后直接运行。

### 决策5：统一异常处理模式

所有 API 端点使用 `try/except Exception as e` + `raise HTTPException(status_code=500, detail=str(e))`，对于资源不存在返回 404。不定义自定义异常类，保持简洁。

## Risks / Trade-offs

- **并发安全** → 全局列表非线程安全，但 Uvicorn 默认单 worker 异步模型下风险可控；多 worker 模式下需注意
- **内存泄漏** → 浏览器实例未正确关闭会占用资源；通过 `close()` 方法和 DELETE API 缓解
- **元素缓存失效** → 页面导航后缓存元素可能失效，客户端需重新查找；当前设计不做自动失效
- **Playwright 异步初始化** → `async_playwright().start()` 需在异步上下文中调用，通过 FastAPI lifespan 或按需初始化处理
