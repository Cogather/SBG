---
name: browser-proxy-rest-crud
description: Add FastAPI REST/CRUD routes to browser-proxy consistent with existing browser, context, and page routers. Use when exposing new HTTP resources under the /api prefix on the Python proxy service.
license: MIT
metadata:
  author: sbg
  version: "1.0"
---

# browser-proxy：增加 CRUD / REST 资源

在 **browser-proxy**（FastAPI）上为某一资源增加列表、创建、单条查询、更新、删除中的**一组或全部**路由，与现有路由风格一致。

## 何时使用

- 需要在 `browser_proxy` 暴露新的 HTTP 资源或操作。
- 路径挂在应用统一前缀下（`main.py` 中 `include_router(..., prefix="/api")`）。

## 用户需提供

- 资源名与 URL 片段（例如 `/widgets`）。
- 各操作的 **HTTP 方法** 与语义。
- 请求/响应 JSON 字段或 Pydantic 模型约定。
- 数据落点：进程内结构（类似 `browser_list`）还是外部存储。

## 必须遵守

1. 使用 **`APIRouter()`** 定义路由；在 **`browser_proxy/main.py`** 中 **`include_router`** 注册（勿重复挂载冲突路径）。
2. 异常使用 **`HTTPException`**；日志使用 **`browser_proxy.api.logger_config`** 中的 **`logger`**。
3. 与同目录已有 `*.py`（如 `browser.py`）保持异步风格、`detail` 与状态码约定一致。
4. 若对外契约变化，在 **`BrowserGateway/BrowserGateway/browser-proxy/openspec/specs/`** 下对应能力的 `spec.md` 中增量说明。

## 本仓库参考路径

- 路由与 CRUD 形态：`BrowserGateway/BrowserGateway/browser-proxy/browser_proxy/api/browser.py`
- 挂载入口：`BrowserGateway/BrowserGateway/browser-proxy/browser_proxy/main.py`
- 其它路由参考：`browser_proxy/api/context.py`、`page.py`
- 测试：`BrowserGateway/BrowserGateway/browser-proxy/tests/`
- 能力规格：`BrowserGateway/BrowserGateway/browser-proxy/openspec/specs/`

## 实施步骤

1. 新建或扩展 `browser_proxy/api/<module>.py`，定义 `router = APIRouter()` 及路由函数。
2. 在 `main.py` 中 `from browser_proxy.api import ...` 并 `app.include_router(..., prefix="/api")`。
3. 按需添加 `tests/test_*.py`，风格对齐现有用例。

## 验收

- 服务启动后，文档或 spec 中的方法与路径可用。
- 项目既有测试命令（如 `pytest`）在新增/修改用例上通过。
