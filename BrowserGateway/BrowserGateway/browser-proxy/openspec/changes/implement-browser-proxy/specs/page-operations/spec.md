## ADDED Requirements

### Requirement: 创建页面
系统 SHALL 支持通过 POST /api/browsers/{browser_id}/contexts/{context_id}/pages 在指定上下文中创建新页面。请求体包含 url（可选）。响应返回更新后的上下文 JSON，HTTP 200。

#### Scenario: 创建新页面
- **WHEN** 客户端发送 POST .../pages，body 包含 url
- **THEN** 系统在上下文中创建新页面，导航到指定URL，将新页面设为 current，返回 HTTP 200 及更新后的上下文

### Requirement: 页面导航
系统 SHALL 支持通过 POST .../pages/goto 导航当前页面到指定URL。请求体包含 url（必填）。响应返回包含 url 字段的上下文 JSON，HTTP 200。

#### Scenario: 导航到新URL
- **WHEN** 客户端发送 POST .../pages/goto，body 包含 url="https://example.com"
- **THEN** 系统导航当前页面到该URL，返回 HTTP 200 及更新后的上下文（含新 url）

### Requirement: 执行JavaScript
系统 SHALL 支持通过 POST .../pages/execute 在当前页面执行 JavaScript 表达式。请求体包含 expression（JS代码字符串）。响应根据返回类型返回不同结构：{"result_type": "string"/"int"/"float"/"bool"/"dict"/"list"/"element"/"none", "value": ...}，HTTP 200。当结果为 DOM 元素时，自动缓存到 PageWrapper._elements 并返回 element_id。

#### Scenario: 执行返回字符串的JS
- **WHEN** 客户端发送 POST .../pages/execute，expression="return document.title"
- **THEN** 系统执行JS，返回 {"result_type": "string", "value": "<页面标题>"}

#### Scenario: 执行返回元素的JS
- **WHEN** 客户端发送 POST .../pages/execute，expression 返回 DOM 元素
- **THEN** 系统缓存该元素，返回 {"result_type": "element", "value": "{\"id\": \"<uuid>\", \"preview\": \"<html>\"}"}

#### Scenario: 执行无返回值的JS
- **WHEN** 客户端发送 POST .../pages/execute，expression 无返回值
- **THEN** 系统返回 {"result_type": "none", "value": null}

### Requirement: 执行CDP命令
系统 SHALL 支持通过 POST .../pages/execute_cdp 执行 Chrome DevTools Protocol 命令。请求体包含 method（CDP方法名）和 params（CDP参数，可选）。响应返回 CDP 命令的 JSON 结果，HTTP 200。

#### Scenario: 执行CDP命令
- **WHEN** 客户端发送 POST .../pages/execute_cdp，method="Network.setCacheDisabled"，params={"cacheDisabled": true}
- **THEN** 系统通过 CDP Session 执行命令，返回 HTTP 200 及 CDP 响应

#### Scenario: 上下文不支持CDP时
- **WHEN** 客户端发送 POST .../pages/execute_cdp，但上下文无 CDP Session
- **THEN** 系统返回 HTTP 500，detail 说明不支持 CDP

### Requirement: 查找元素
系统 SHALL 支持通过 POST .../pages/find_element 在当前页面查找元素。请求体包含 selector（CSS选择器）。响应返回 {"result_type": "element", "value": "{\"id\": \"<uuid>\", \"preview\": \"<html>\"}"} 或 {"result_type": "none", "value": null}，HTTP 200。找到的元素缓存在 PageWrapper._elements 中。

#### Scenario: 查找存在的元素
- **WHEN** 客户端发送 POST .../pages/find_element，selector 匹配页面中的元素
- **THEN** 系统返回 result_type="element"，value 包含 element_id 和 preview

#### Scenario: 查找不存在的元素
- **WHEN** 客户端发送 POST .../pages/find_element，selector 不匹配任何元素
- **THEN** 系统返回 {"result_type": "none", "value": null}

### Requirement: 元素操作
系统 SHALL 支持通过 POST .../pages/element 对缓存的元素执行操作。请求体包含 element_id（元素UUID）、action（"send_key"/"set_file"/"focus"）、value（操作值，可选）。响应为 HTTP 204 No Content。

#### Scenario: 向元素发送文本
- **WHEN** 客户端发送 POST .../pages/element，action="send_key"，value="hello"
- **THEN** 系统向指定元素输入文本，返回 HTTP 204

#### Scenario: 设置文件输入
- **WHEN** 客户端发送 POST .../pages/element，action="set_file"，value="/path/to/file"
- **THEN** 系统设置文件输入元素的文件，返回 HTTP 204

#### Scenario: 元素聚焦
- **WHEN** 客户端发送 POST .../pages/element，action="focus"
- **THEN** 系统聚焦指定元素，返回 HTTP 204

### Requirement: 获取元素尺寸
系统 SHALL 支持通过 POST .../pages/element/{element_id}/get_size 获取指定元素的尺寸。响应返回 {"width": int, "height": int}，HTTP 200。

#### Scenario: 获取元素尺寸
- **WHEN** 客户端发送 POST .../pages/element/{element_id}/get_size，元素存在
- **THEN** 系统返回 HTTP 200，body 包含元素的宽高（像素）

### Requirement: 删除页面
系统 SHALL 支持通过 DELETE .../pages/{page_id} 关闭并删除指定页面。响应返回更新后的上下文 JSON（current 更新为剩余页面之一），HTTP 200。

#### Scenario: 删除存在的页面
- **WHEN** 客户端发送 DELETE .../pages/{page_id}，页面存在
- **THEN** 系统关闭页面，从上下文移除，更新 current 指针，返回 HTTP 200 及更新后的上下文

### Requirement: 页面前进后退
系统 SHALL 支持通过 POST .../pages/go_back 和 POST .../pages/go_forward 控制当前页面的浏览历史。两个端点均返回 HTTP 204 No Content。

#### Scenario: 页面后退
- **WHEN** 客户端发送 POST .../pages/go_back
- **THEN** 系统调用当前页面的 go_back()，返回 HTTP 204

#### Scenario: 页面前进
- **WHEN** 客户端发送 POST .../pages/go_forward
- **THEN** 系统调用当前页面的 go_forward()，返回 HTTP 204

### Requirement: 页面截图
系统 SHALL 支持通过 POST .../pages/screenshot 对当前页面截图。请求体可选 full_page（bool，是否全页截图，默认 false）。响应返回 base64 编码的 PNG 图片字符串，HTTP 200。

#### Scenario: 页面截图
- **WHEN** 客户端发送 POST .../pages/screenshot
- **THEN** 系统对当前页面截图，返回 HTTP 200，body 为 base64 编码的 PNG 数据

### Requirement: 页面滚动
系统 SHALL 支持通过 POST .../pages/scroll 滚动页面。请求体包含 x、y（滚动坐标）和 delta_x、delta_y（滚动量）。响应为 HTTP 204 No Content。

#### Scenario: 滚动页面
- **WHEN** 客户端发送 POST .../pages/scroll，包含坐标和滚动量
- **THEN** 系统在指定坐标执行滚动操作，返回 HTTP 204

### Requirement: 触摸操作
系统 SHALL 支持通过以下端点执行移动端触摸操作：
- POST .../pages/tap：点击操作，请求体含 x、y 坐标
- POST .../pages/swipe：滑动操作，请求体含 start_x、start_y、end_x、end_y、duration_ms
- POST .../pages/touch_scroll：触摸滚动，请求体含 x、y、delta_x、delta_y
所有触摸操作均通过 CDP 发送 Input 事件实现，响应为 HTTP 204 No Content。

#### Scenario: 触摸点击
- **WHEN** 客户端发送 POST .../pages/tap，body 包含 x、y 坐标
- **THEN** 系统通过 CDP 模拟触摸点击，返回 HTTP 204

#### Scenario: 触摸滑动
- **WHEN** 客户端发送 POST .../pages/swipe，body 包含起止坐标和 duration_ms
- **THEN** 系统通过 CDP 模拟触摸滑动动作，返回 HTTP 204

### Requirement: Cookie管理
系统 SHALL 支持通过以下端点管理 Cookie：
- GET .../pages/cookies：获取当前页面的所有 Cookie，响应为 Cookie 数组
- POST .../pages/cookies：设置 Cookie，请求体为 Cookie 对象列表
- DELETE .../pages/cookies：清除所有 Cookie，响应为 HTTP 204

#### Scenario: 获取Cookie
- **WHEN** 客户端发送 GET .../pages/cookies
- **THEN** 系统返回 HTTP 200，body 为当前上下文的 Cookie 数组

#### Scenario: 设置Cookie
- **WHEN** 客户端发送 POST .../pages/cookies，body 包含 Cookie 列表
- **THEN** 系统添加指定 Cookie 到当前上下文，返回 HTTP 204

#### Scenario: 清除Cookie
- **WHEN** 客户端发送 DELETE .../pages/cookies
- **THEN** 系统清除当前上下文所有 Cookie，返回 HTTP 204
