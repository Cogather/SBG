## ADDED Requirements

### Requirement: 创建浏览器上下文
系统 SHALL 支持通过 POST /api/browsers/{browser_id}/contexts 在指定浏览器下创建新的 BrowserContext。

**路径参数：**
- `browser_id`（string，必填）：浏览器ID

**请求体（JSON）：**
- `url`（string，可选）：初始页面导航目标URL
- `data`（string，可选）：传给 START_RECORDING 的参数
- `viewport`（object，可选）：`{"width": int, "height": int}`，视口尺寸
- `userdata`（string，必填）：状态存储文件路径（JSON格式），不存在时自动创建
- `language`（string，可选，默认 `"en_US"`）：语言设置，下划线自动转为连字符

**行为：**
1. 若 userdata 文件不存在则自动创建（写入空 JSON `{}`）
2. 使用 `browser.new_context(storage_state, viewport, locale, is_mobile=True, has_touch=True)` 创建上下文
3. 创建第一个页面并导航到 url
4. 创建第二个页面，对第二个页面建立 CDP Session
5. 在第一个页面执行 `START_RECORDING(data)`
6. 将第二个页面（含 CDP Session）作为 current page

**响应：** HTTP 200，返回上下文 JSON：`{"id", "current": "<page_id>", "browser_id", "pages": [...]}`

#### Scenario: 创建基本上下文
- **WHEN** 客户端发送 POST .../contexts，body 包含 url、viewport、userdata
- **THEN** 系统创建新 BrowserContext，自动创建两个页面，第二个页面含 CDP Session 并设为 current，返回 HTTP 200 及上下文详情

#### Scenario: 浏览器不存在时创建上下文
- **WHEN** 客户端发送 POST .../contexts，browser_id 不存在
- **THEN** 系统返回 HTTP 404

---

### Requirement: 查询上下文列表
系统 SHALL 支持通过 GET /api/browsers/{browser_id}/contexts 获取指定浏览器下所有上下文列表。

**路径参数：**
- `browser_id`（string，必填）

**响应：** HTTP 200，返回 JSON 数组，每项为上下文 JSON

#### Scenario: 获取上下文列表
- **WHEN** 客户端发送 GET .../contexts，浏览器存在
- **THEN** 系统返回 HTTP 200，body 为该浏览器下所有上下文的 JSON 数组

#### Scenario: 无上下文时返回空列表
- **WHEN** 浏览器存在但无上下文
- **THEN** 系统返回 HTTP 200，body 为空数组 []

---

### Requirement: 查询指定上下文
系统 SHALL 支持通过 GET /api/browsers/{browser_id}/contexts/{context_id} 获取指定上下文详情。

**路径参数：**
- `browser_id`（string，必填）
- `context_id`（string，必填）

**响应：** HTTP 200，返回 `{"id", "current": "<page_id>", "browser_id", "pages": [...]}`；不存在返回 HTTP 404

#### Scenario: 获取存在的上下文
- **WHEN** 客户端发送 GET .../contexts/{context_id}，上下文存在
- **THEN** 系统返回 HTTP 200 及上下文详情，包含页面列表

#### Scenario: 获取不存在的上下文
- **WHEN** 客户端发送 GET .../contexts/{context_id}，上下文不存在
- **THEN** 系统返回 HTTP 404

---

### Requirement: 删除上下文
系统 SHALL 支持通过 DELETE /api/browsers/{browser_id}/contexts/{context_id} 删除指定上下文并释放资源。

**路径参数：**
- `browser_id`（string，必填）
- `context_id`（string，必填）

**行为：** 调用 `context.close()`（先保存 storage_state 到 userdata，再关闭 context），从浏览器列表移除。若上下文不存在（HTTP 404）则静默忽略（直接返回）。

**响应：** HTTP 204 No Content

#### Scenario: 删除存在的上下文
- **WHEN** 客户端发送 DELETE .../contexts/{context_id}，上下文存在
- **THEN** 系统保存状态、关闭上下文、从浏览器列表移除，返回 HTTP 204

#### Scenario: 删除不存在的上下文
- **WHEN** 客户端发送 DELETE .../contexts/{context_id}，上下文不存在
- **THEN** 系统静默返回 HTTP 204（不报错）
