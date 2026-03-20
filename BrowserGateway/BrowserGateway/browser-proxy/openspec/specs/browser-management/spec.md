## ADDED Requirements

### Requirement: 创建浏览器实例
系统 SHALL 支持通过 POST /api/browsers 创建 Playwright 浏览器实例（launch_persistent_context）。

**请求体（JSON）：**
- `executable_path`（string，必填）：Chrome 可执行文件路径
- `browser_id`（string，可选，默认自动生成 uuid4().hex）：指定浏览器ID
- `browser_type`（string，必填）：`"KEYS"` 或 `"TOUCH"`
- `headless`（bool，可选，默认 true）：是否无头模式
- `language`（string，可选，默认 `"en-US"`）：浏览器语言，下划线自动转为连字符
- `base_data`（string，必填）：用户数据目录路径（userdata）
- `extension_paths`（list[string]，可选）：扩展路径列表，逗号拼接后传给 --load-extension
- `extension_ids`（list[string]，可选）：扩展ID列表，写入 Preferences 文件
- `allowlisted_extension_id`（string，可选）：白名单扩展ID

**响应：** HTTP 200，返回 `{"id": "<browser_id>", "used": 0, "browser_type": "KEYS"}`

#### Scenario: 创建基本浏览器实例
- **WHEN** 客户端发送 POST /api/browsers，body 包含 executable_path、base_data、browser_type
- **THEN** 系统使用 Playwright launch_persistent_context 创建浏览器，返回 HTTP 200 及包含 id 和 browser_type 的 JSON

#### Scenario: 创建指定ID的浏览器实例
- **WHEN** 客户端发送 POST /api/browsers，body 包含 browser_id="test-browser-001"
- **THEN** 系统使用指定 ID 创建浏览器，响应中 id 字段等于 "test-browser-001"

#### Scenario: 无效executable_path
- **WHEN** 客户端发送 POST /api/browsers，executable_path 指向不存在的路径
- **THEN** 系统返回 HTTP 500，detail 包含错误信息

---

### Requirement: 查询浏览器列表
系统 SHALL 支持通过 GET /api/browsers 获取所有已创建浏览器实例列表。

**请求体：** 无

**响应：** HTTP 200，返回 JSON 数组，每项为 `{"id", "used", "browser_type"}`

#### Scenario: 获取浏览器列表
- **WHEN** 客户端发送 GET /api/browsers
- **THEN** 系统返回 HTTP 200，body 为包含所有浏览器实例信息的 JSON 数组

#### Scenario: 无浏览器时返回空列表
- **WHEN** 没有任何浏览器实例时，客户端发送 GET /api/browsers
- **THEN** 系统返回 HTTP 200，body 为空数组 []

---

### Requirement: 查询指定浏览器
系统 SHALL 支持通过 GET /api/browsers/{browser_id} 获取指定浏览器详情。

**路径参数：**
- `browser_id`（string，必填）

**响应：** HTTP 200，返回 `{"id", "used", "browser_type"}`；浏览器不存在返回 HTTP 404

#### Scenario: 获取存在的浏览器
- **WHEN** 客户端发送 GET /api/browsers/{browser_id}，该浏览器存在
- **THEN** 系统返回 HTTP 200 及该浏览器的详情 JSON

#### Scenario: 获取不存在的浏览器
- **WHEN** 客户端发送 GET /api/browsers/{browser_id}，该浏览器不存在
- **THEN** 系统返回 HTTP 404

---

### Requirement: 删除浏览器实例
系统 SHALL 支持通过 DELETE /api/browsers/{browser_id} 删除指定浏览器实例并释放所有相关资源（关闭 browser、停止 playwright、删除 userdata 目录）。

**路径参数：**
- `browser_id`（string，必填）

**响应：** HTTP 204 No Content；浏览器不存在返回 HTTP 404

#### Scenario: 删除存在的浏览器
- **WHEN** 客户端发送 DELETE /api/browsers/{browser_id}，该浏览器存在
- **THEN** 系统关闭浏览器、停止 Playwright、删除 userdata 目录，从全局列表移除，返回 HTTP 204

#### Scenario: 删除不存在的浏览器
- **WHEN** 客户端发送 DELETE /api/browsers/{browser_id}，该浏览器不存在
- **THEN** 系统返回 HTTP 404

---

### Requirement: 健康检查
系统 SHALL 支持通过 POST /api/browsers/health_check 检查所有浏览器上下文的健康状态。

**请求体：** 无

**响应：** HTTP 200
- 所有上下文正常：`{"success": true}`
- 存在异常上下文：`{"success": false, "err_contexts": ["<context_id>", ...]}`

#### Scenario: 所有上下文正常
- **WHEN** 客户端发送 POST /api/browsers/health_check，所有上下文 current page 可正常调用 title()
- **THEN** 系统返回 HTTP 200，body 为 {"success": true}

#### Scenario: 存在异常上下文
- **WHEN** 某上下文 current page 为 None 或 title() 抛出异常
- **THEN** 系统返回 HTTP 200，body 包含 {"success": false, "err_contexts": [...]}
