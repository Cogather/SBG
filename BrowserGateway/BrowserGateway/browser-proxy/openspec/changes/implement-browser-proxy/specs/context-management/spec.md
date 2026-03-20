## ADDED Requirements

### Requirement: 创建浏览器上下文
系统 SHALL 支持通过 POST /api/browsers/{browser_id}/contexts 在指定浏览器下创建新的 BrowserContext（页面组）。请求体包含 url（导航目标URL，可选）、viewport（{"width": int, "height": int}，可选）。响应返回上下文 JSON：{"id", "current": "<page_id>", "browser_id", "pages": [...]}，HTTP 200。

#### Scenario: 创建基本上下文
- **WHEN** 客户端发送 POST /api/browsers/{browser_id}/contexts，body 包含 url
- **THEN** 系统创建新 BrowserContext，自动创建一个初始页面并导航到指定URL，返回 HTTP 200 及上下文详情

#### Scenario: 创建带视口的上下文
- **WHEN** 客户端发送 POST /api/browsers/{browser_id}/contexts，body 包含 viewport={"width": 1920, "height": 1080}
- **THEN** 系统创建指定视口尺寸的上下文，返回 HTTP 200

#### Scenario: 浏览器不存在时创建上下文
- **WHEN** 客户端发送 POST /api/browsers/{browser_id}/contexts，browser_id 不存在
- **THEN** 系统返回 HTTP 404

### Requirement: 查询上下文列表
系统 SHALL 支持通过 GET /api/browsers/{browser_id}/contexts 获取指定浏览器下所有上下文列表。响应为 JSON 数组，HTTP 200。

#### Scenario: 获取上下文列表
- **WHEN** 客户端发送 GET /api/browsers/{browser_id}/contexts，浏览器存在
- **THEN** 系统返回 HTTP 200，body 为该浏览器下所有上下文的 JSON 数组

#### Scenario: 无上下文时返回空列表
- **WHEN** 浏览器存在但无上下文时发送 GET /api/browsers/{browser_id}/contexts
- **THEN** 系统返回 HTTP 200，body 为空数组 []

### Requirement: 查询指定上下文
系统 SHALL 支持通过 GET /api/browsers/{browser_id}/contexts/{context_id} 获取指定上下文详情。响应包含 {"id", "current", "browser_id", "pages": [...]}，HTTP 200。若不存在，返回 HTTP 404。

#### Scenario: 获取存在的上下文
- **WHEN** 客户端发送 GET /api/browsers/{browser_id}/contexts/{context_id}，上下文存在
- **THEN** 系统返回 HTTP 200 及上下文详情，包含页面列表

#### Scenario: 获取不存在的上下文
- **WHEN** 客户端发送 GET /api/browsers/{browser_id}/contexts/{context_id}，上下文不存在
- **THEN** 系统返回 HTTP 404

### Requirement: 删除上下文
系统 SHALL 支持通过 DELETE /api/browsers/{browser_id}/contexts/{context_id} 删除指定上下文并释放资源。响应为 HTTP 204 No Content。

#### Scenario: 删除存在的上下文
- **WHEN** 客户端发送 DELETE /api/browsers/{browser_id}/contexts/{context_id}，上下文存在
- **THEN** 系统关闭上下文、保存状态（如有userdata），从浏览器列表移除，返回 HTTP 204

#### Scenario: 删除不存在的上下文
- **WHEN** 客户端发送 DELETE /api/browsers/{browser_id}/contexts/{context_id}，上下文不存在
- **THEN** 系统返回 HTTP 404
