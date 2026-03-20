## ADDED Requirements

### Requirement: 创建页面
系统 SHALL 支持通过 POST /api/browsers/{browser_id}/contexts/{context_id}/pages 在指定上下文中创建新页面。

**路径参数：**
- `browser_id`（string，必填）：浏览器ID
- `context_id`（string，必填）：上下文ID

**请求体（JSON）：**
- `url`（string，可选）：新页面要导航的URL

**响应：** HTTP 200，返回更新后的上下文 JSON（`context.as_json()`）

#### Scenario: 创建新页面
- **WHEN** 客户端发送 POST .../pages，body 包含 url
- **THEN** 系统在上下文中创建新页面，导航到指定URL，将新页面设为 current，返回 HTTP 200 及更新后的上下文

---

### Requirement: 页面导航
系统 SHALL 支持通过 POST .../pages/goto 导航当前页面到指定URL。

**路径参数：**
- `browser_id`（string，必填）：浏览器ID
- `context_id`（string，必填）：上下文ID

**请求体（JSON）：**
- `url`（string，可选）：目标URL，通过 `body.get('url')` 获取

**响应：** HTTP 200，返回更新后的上下文 JSON（`context.as_json()`）

#### Scenario: 导航到新URL
- **WHEN** 客户端发送 POST .../pages/goto，body 包含 url="https://example.com"
- **THEN** 系统导航当前页面到该URL，返回 HTTP 200 及更新后的上下文（含新 url）

---

### Requirement: 执行JavaScript
系统 SHALL 支持通过 POST .../pages/execute 在当前页面执行 JavaScript 表达式。

**路径参数：**
- `browser_id`（string，必填）：浏览器ID
- `context_id`（string，必填）：上下文ID

**请求体（JSON）：**
- `expression`（string，必填）：JS 代码字符串。系统将其包装为 `() => {expression}` 执行；含 `await` 时包装为 `(async () => {expression})()`

**响应：** HTTP 200，根据返回类型返回不同结构：
- `{"result_type": "string", "value": "..."}`
- `{"result_type": "int", "value": 123}`
- `{"result_type": "none", "value": null}`
- `{"result_type": "dict", "element_keys": [...], "value": "{...}"}`
- `{"result_type": "element", "value": "{\"id\": \"<uuid>\", \"preview\": \"<html>\"}"}`

当结果为 DOM 元素（`'ref: <Node>'`）时，自动缓存到 PageWrapper._elements 并返回 element_id。页面上下文销毁或函数不匹配时返回 `{"result_type": "none"}`。

#### Scenario: 执行返回字符串的JS
- **WHEN** 客户端发送 POST .../pages/execute，expression="return document.title"
- **THEN** 系统执行JS，返回 {"result_type": "string", "value": "<页面标题>"}

#### Scenario: 执行返回元素的JS
- **WHEN** 客户端发送 POST .../pages/execute，expression 返回 DOM 元素
- **THEN** 系统缓存该元素，返回 {"result_type": "element", "value": "{\"id\": \"<uuid>\", \"preview\": \"<html>\"}"}

#### Scenario: 执行无返回值的JS
- **WHEN** 客户端发送 POST .../pages/execute，expression 无返回值
- **THEN** 系统返回 {"result_type": "none", "value": null}

#### Scenario: 执行上下文销毁或函数不匹配
- **WHEN** JS 执行时页面上下文已销毁，或调用了不存在的函数
- **THEN** 系统返回 {"result_type": "none"}

---

### Requirement: 执行CDP命令
系统 SHALL 支持通过 POST .../pages/execute_cdp 执行 Chrome DevTools Protocol 命令。

**路径参数：**
- `browser_id`（string，必填）：浏览器ID
- `context_id`（string，必填）：上下文ID

**请求体（JSON）：**
- `method`（string，必填）：CDP 方法名，如 `"Network.setCacheDisabled"`
- `params`（object，可选，默认 null）：CDP 参数对象

**响应：** HTTP 200，返回 CDP 命令的 JSON 结果

#### Scenario: 执行CDP命令
- **WHEN** 客户端发送 POST .../pages/execute_cdp，method="Network.setCacheDisabled"，params={"cacheDisabled": true}
- **THEN** 系统通过 CDP Session 执行命令，返回 HTTP 200 及 CDP 响应

#### Scenario: 上下文不支持CDP时
- **WHEN** 客户端发送 POST .../pages/execute_cdp，但当前页面无 CDP Session
- **THEN** 系统返回 HTTP 500，detail 说明不支持 CDP

---

### Requirement: 查找元素
系统 SHALL 支持通过 POST .../pages/find_element 在当前页面查找元素。

**路径参数：**
- `browser_id`（string，必填）：浏览器ID
- `context_id`（string，必填）：上下文ID

**请求体（JSON）：**
- `selector`（string，必填）：CSS 选择器

**响应：** HTTP 200
- 找到元素：`{"result_type": "element", "value": "{\"id\": \"<uuid>\", \"preview\": \"<html>\"}"}`
- 元素缓存在 PageWrapper._elements 中

#### Scenario: 查找存在的元素
- **WHEN** 客户端发送 POST .../pages/find_element，selector 匹配页面中的元素
- **THEN** 系统返回 result_type="element"，value 包含 element_id 和 preview

---

### Requirement: 元素操作
系统 SHALL 支持通过 POST .../pages/element 对缓存的元素执行操作。

**路径参数：**
- `browser_id`（string，必填）：浏览器ID
- `context_id`（string，必填）：上下文ID

**请求体（JSON）：**
- `element_id`（string，必填）：元素UUID，对应 PageWrapper._elements 中的缓存键
- `action`（string，必填）：操作类型，取值为 `"send_key"` / `"set_file"` / `"focus"`
- `value`（string，可选）：操作值，系统会自动去除末尾 `\x00` 空字符

**响应：** HTTP 200，无响应体。操作完成后元素从缓存中删除。操作前自动 focus 元素。

#### Scenario: 向元素发送文本
- **WHEN** 客户端发送 POST .../pages/element，action="send_key"，value="hello"
- **THEN** 系统 focus 元素后调用 fill(value)，返回 HTTP 200

#### Scenario: 设置文件输入
- **WHEN** 客户端发送 POST .../pages/element，action="set_file"，value="/path/to/file"
- **THEN** 系统设置文件输入元素的文件，返回 HTTP 200

#### Scenario: 元素聚焦
- **WHEN** 客户端发送 POST .../pages/element，action="focus"
- **THEN** 系统 focus 指定元素，返回 HTTP 200

---

### Requirement: 获取元素尺寸
系统 SHALL 支持通过 POST .../pages/element/{element_id}/get_size 获取指定元素的尺寸。

**路径参数：**
- `browser_id`（string，必填）：浏览器ID
- `context_id`（string，必填）：上下文ID
- `element_id`（string，必填）：元素UUID

**请求体：** 无

**响应：** HTTP 200，返回 `{"width": float, "height": float}`（像素，浮点数）。操作完成后元素从缓存中删除。

#### Scenario: 获取元素尺寸
- **WHEN** 客户端发送 POST .../pages/element/{element_id}/get_size，元素存在且可见
- **THEN** 系统返回 HTTP 200，body 包含元素的宽高（浮点像素值）

#### Scenario: 元素不可见
- **WHEN** 元素 bounding_box 为 null
- **THEN** 系统返回 HTTP 500，detail="element is not visible"

---

### Requirement: 删除页面
系统 SHALL 支持通过 DELETE .../pages/{page_id} 关闭并删除指定页面。

**路径参数：**
- `browser_id`（string，必填）：浏览器ID
- `context_id`（string，必填）：上下文ID
- `page_id`（string，必填）：要删除的页面ID

**请求体：** 无

**响应：** HTTP 200，返回更新后的上下文 JSON。删除后将 current 页面置于前台（bring_to_front）。仅剩一个页面时拒绝操作（HTTP 500）。

#### Scenario: 删除存在的页面
- **WHEN** 客户端发送 DELETE .../pages/{page_id}，上下文有多个页面
- **THEN** 系统关闭页面，从上下文移除，current 页面 bring_to_front，返回 HTTP 200 及更新后的上下文

#### Scenario: 仅剩一个页面时拒绝删除
- **WHEN** 客户端发送 DELETE .../pages/{page_id}，上下文只有一个页面
- **THEN** 系统返回 HTTP 500，detail="only one page is not supported"

---

### Requirement: 页面前进后退
系统 SHALL 支持通过 POST .../pages/go_back 和 POST .../pages/go_forward 控制当前页面的浏览历史。

**路径参数：**
- `browser_id`（string，必填）：浏览器ID
- `context_id`（string，必填）：上下文ID

**请求体：** 无

**响应：** HTTP 200，无响应体

#### Scenario: 页面后退
- **WHEN** 客户端发送 POST .../pages/go_back
- **THEN** 系统调用当前页面的 go_back()，返回 HTTP 200

#### Scenario: 页面前进
- **WHEN** 客户端发送 POST .../pages/go_forward
- **THEN** 系统调用当前页面的 go_forward()，返回 HTTP 200

---

### Requirement: 页面截图
系统 SHALL 支持通过 POST .../pages/screenshot 对当前页面截图。

**路径参数：**
- `browser_id`（string，必填）：浏览器ID
- `context_id`（string，必填）：上下文ID

**请求体（JSON，可选）：**
- `full_page`（bool，可选，默认 false）：是否截取整个页面

**响应：** HTTP 200，返回 base64 编码的 PNG 字符串

#### Scenario: 页面截图
- **WHEN** 客户端发送 POST .../pages/screenshot
- **THEN** 系统对当前页面截图，返回 HTTP 200，body 为 base64 编码的 PNG 数据

---

### Requirement: 页面滚动
系统 SHALL 支持通过 POST .../pages/scroll 滚动页面。

**路径参数：**
- `browser_id`（string，必填）：浏览器ID
- `context_id`（string，必填）：上下文ID

**请求体（JSON）：**
- `x`（number，必填）：滚动起点 X 坐标
- `y`（number，必填）：滚动起点 Y 坐标
- `delta_x`（number，必填）：X 方向滚动量
- `delta_y`（number，必填）：Y 方向滚动量

**响应：** HTTP 204 No Content

#### Scenario: 滚动页面
- **WHEN** 客户端发送 POST .../pages/scroll，包含坐标和滚动量
- **THEN** 系统执行 mouse.wheel(delta_x, delta_y)，返回 HTTP 204

---

### Requirement: 触摸操作
系统 SHALL 支持通过以下端点执行移动端触摸操作，均通过 CDP 发送 Input 事件实现。

**路径参数（所有触摸接口）：**
- `browser_id`（string，必填）：浏览器ID
- `context_id`（string，必填）：上下文ID

**POST .../pages/tap 请求体（JSON）：**
- `x`（number，必填）：触摸点 X 坐标
- `y`（number，必填）：触摸点 Y 坐标

**POST .../pages/swipe 请求体（JSON）：**
- `start_x`（number，必填）：起始 X 坐标
- `start_y`（number，必填）：起始 Y 坐标
- `end_x`（number，必填）：终止 X 坐标
- `end_y`（number，必填）：终止 Y 坐标
- `duration_ms`（number，可选，默认 300）：滑动持续时间（毫秒）

**POST .../pages/touch_scroll 请求体（JSON）：**
- `x`（number，必填）：触摸起点 X 坐标
- `y`（number，必填）：触摸起点 Y 坐标
- `delta_x`（number，必填）：X 方向滚动量
- `delta_y`（number，必填）：Y 方向滚动量

**响应：** 所有触摸操作均返回 HTTP 204 No Content

#### Scenario: 触摸点击
- **WHEN** 客户端发送 POST .../pages/tap，body 包含 x、y 坐标
- **THEN** 系统通过 CDP 模拟 touchStart + touchEnd，返回 HTTP 204

#### Scenario: 触摸滑动
- **WHEN** 客户端发送 POST .../pages/swipe，body 包含起止坐标和 duration_ms
- **THEN** 系统通过 CDP 分步模拟 touchStart + touchMove × N + touchEnd，返回 HTTP 204

#### Scenario: 触摸滚动
- **WHEN** 客户端发送 POST .../pages/touch_scroll，body 包含坐标和滚动量
- **THEN** 系统通过 CDP 模拟 touchStart + touchMove + touchEnd，返回 HTTP 204

---

### Requirement: Cookie管理
系统 SHALL 支持通过以下端点管理 Cookie。

**路径参数（所有 Cookie 接口）：**
- `browser_id`（string，必填）：浏览器ID
- `context_id`（string，必填）：上下文ID

**GET .../pages/cookies**
- 请求体：无
- 响应：HTTP 200，返回当前上下文的 Cookie 数组

**POST .../pages/cookies**
- 请求体（JSON）：Cookie 对象列表（直接作为请求体，非包装对象）
- 响应：HTTP 204 No Content

**DELETE .../pages/cookies**
- 请求体：无
- 响应：HTTP 204 No Content

#### Scenario: 获取Cookie
- **WHEN** 客户端发送 GET .../pages/cookies
- **THEN** 系统返回 HTTP 200，body 为当前上下文的 Cookie 数组

#### Scenario: 设置Cookie
- **WHEN** 客户端发送 POST .../pages/cookies，body 包含 Cookie 列表
- **THEN** 系统添加指定 Cookie 到当前上下文，返回 HTTP 204

#### Scenario: 清除Cookie
- **WHEN** 客户端发送 DELETE .../pages/cookies
- **THEN** 系统清除当前上下文所有 Cookie，返回 HTTP 204
