## ADDED Requirements

### Requirement: 创建浏览器实例
系统 SHALL 支持通过 POST /api/browsers 创建 Playwright 浏览器实例。请求体包含 executable_path（可选）、browser_id（可选，不传则自动生成UUID）、browser_type（"KEYS" 或 "TOUCH"，默认 "KEYS"）、headless（bool，默认 true）、language（字符串，默认 "zh-CN"）、base_data（用户数据目录，可选）、extension_paths（扩展路径列表，可选）、extension_ids（扩展ID列表，可选）、allowlisted_extension_id（白名单扩展ID，可选）。响应返回 {"id": "<browser_id>", "used": 0, "browser_type": "KEYS"}，HTTP 200。

#### Scenario: 创建基本浏览器实例
- **WHEN** 客户端发送 POST /api/browsers，body 包含 executable_path 和 browser_type="KEYS"
- **THEN** 系统使用 Playwright launch_persistent_context 创建浏览器，返回 HTTP 200 及包含 id 和 browser_type 的 JSON

#### Scenario: 创建指定ID的浏览器实例
- **WHEN** 客户端发送 POST /api/browsers，body 包含 browser_id="test-browser-001"
- **THEN** 系统使用指定 ID 创建浏览器，响应中 id 字段等于 "test-browser-001"

#### Scenario: 创建TOUCH模式浏览器实例
- **WHEN** 客户端发送 POST /api/browsers，body 包含 browser_type="TOUCH"
- **THEN** 系统创建支持触摸操作的浏览器，响应中 browser_type 为 "TOUCH"

#### Scenario: 无效executable_path
- **WHEN** 客户端发送 POST /api/browsers，executable_path 指向不存在的路径
- **THEN** 系统返回 HTTP 500，detail 包含错误信息

### Requirement: 查询浏览器列表
系统 SHALL 支持通过 GET /api/browsers 获取所有已创建浏览器实例列表。响应为 JSON 数组，每项包含 {"id", "used", "browser_type"}，HTTP 200。

#### Scenario: 获取浏览器列表
- **WHEN** 客户端发送 GET /api/browsers
- **THEN** 系统返回 HTTP 200，body 为包含所有浏览器实例信息的 JSON 数组

#### Scenario: 无浏览器时返回空列表
- **WHEN** 没有任何浏览器实例时，客户端发送 GET /api/browsers
- **THEN** 系统返回 HTTP 200，body 为空数组 []

### Requirement: 查询指定浏览器
系统 SHALL 支持通过 GET /api/browsers/{browser_id} 获取指定浏览器详情。响应包含 {"id", "used", "browser_type"}，HTTP 200。若浏览器不存在，返回 HTTP 404。

#### Scenario: 获取存在的浏览器
- **WHEN** 客户端发送 GET /api/browsers/{browser_id}，该浏览器存在
- **THEN** 系统返回 HTTP 200 及该浏览器的详情 JSON

#### Scenario: 获取不存在的浏览器
- **WHEN** 客户端发送 GET /api/browsers/{browser_id}，该浏览器不存在
- **THEN** 系统返回 HTTP 404

### Requirement: 删除浏览器实例
系统 SHALL 支持通过 DELETE /api/browsers/{browser_id} 删除指定浏览器实例并释放所有相关资源。响应为 HTTP 204 No Content。若浏览器不存在，返回 HTTP 404。

#### Scenario: 删除存在的浏览器
- **WHEN** 客户端发送 DELETE /api/browsers/{browser_id}，该浏览器存在
- **THEN** 系统关闭浏览器、释放 Playwright 资源，从全局列表移除，返回 HTTP 204

#### Scenario: 删除不存在的浏览器
- **WHEN** 客户端发送 DELETE /api/browsers/{browser_id}，该浏览器不存在
- **THEN** 系统返回 HTTP 404
