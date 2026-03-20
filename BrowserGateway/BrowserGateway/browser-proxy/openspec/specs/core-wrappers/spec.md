## ADDED Requirements

### Requirement: ElementWrapper 封装类
系统 SHALL 实现 ElementWrapper 类封装 Playwright ElementHandle 对象。

**属性：**
- `element`（ElementHandle）：Playwright 元素句柄
- `id`（string）：uuid4().hex 生成的唯一ID
- `preview`（async property）：调用 `element.inner_html()` 后返回 `element.__str__().replace('JSHandle@', '')`

**方法：**
- `async close()`：调用 `element.dispose()` 释放资源
- `async as_json()`：返回 `{"id": self.id, "preview": await self.preview}`

#### Scenario: ElementWrapper.as_json() 序列化
- **WHEN** 调用 element_wrapper.as_json()
- **THEN** 返回包含 id 和 preview 字段的字典

---

### Requirement: PageWrapper 封装类
系统 SHALL 实现 PageWrapper 类封装 Playwright Page 对象。

**属性：**
- `page`（Page）：Playwright 页面对象
- `id`（string）：uuid4().hex 生成的唯一ID
- `browser_id`（string）：所属浏览器ID
- `context_id`（string）：所属上下文ID
- `cdp_session`（CDPSession，可选）：若为 None 则访问时抛出 HTTP 500
- `url`（string）：当前页面 URL（`page.url`）
- `_elements`（Dict[str, ElementWrapper]）：元素缓存字典

**方法：**
- `set_element(element: ElementWrapper)`：将元素存入 `_elements[element.id]`
- `get_element(key: str) -> ElementWrapper`：从缓存取元素，不存在时抛出 HTTP 404
- `del_element(key: str)`：从缓存删除元素（不存在时静默忽略）
- `as_json()`：返回 `{"id", "url", "browser_id", "context_id", "support_cdp_session": bool}`
- `async close()`：释放所有缓存元素后关闭页面

#### Scenario: PageWrapper 元素缓存
- **WHEN** 调用 set_element(elem) 后再调用 get_element(elem.id)
- **THEN** 返回同一个 ElementWrapper 实例

#### Scenario: PageWrapper.as_json() 序列化
- **WHEN** 调用 page_wrapper.as_json()
- **THEN** 返回包含 id、url、browser_id、context_id、support_cdp_session 字段的字典

#### Scenario: cdp_session 为 None 时访问
- **WHEN** PageWrapper 创建时未传入 cdp_session，访问 cdp_session 属性
- **THEN** 抛出 HTTP 500，detail 说明该页面不支持 CDP Session

---

### Requirement: ContextWrapper 封装类
系统 SHALL 实现 ContextWrapper 类封装 Playwright BrowserContext 对象。

**属性：**
- `id`（string）：uuid4().hex 生成的唯一ID
- `context`（BrowserContext）：Playwright 上下文对象
- `pages`（List[PageWrapper]）：页面列表
- `current`（PageWrapper）：当前活跃页面（append_page 时更新）
- `userdata`（string）：状态文件路径

**方法：**
- `append_page(page)`：将页面加入列表并设为 current
- `get_page(page_id) -> PageWrapper`：查找页面，不存在时抛出 HTTP 404
- `remove_page(page)`：从列表移除页面；若被移除的是 current，则将列表最后一个页面设为新 current
- `as_json()`：返回 `{"id", "current": page_id or None, "browser_id", "pages": [page.as_json(), ...]}`
- `async close()`：调用 `context.storage_state(path=userdata, indexed_db=False)` 保存状态，再关闭 context

#### Scenario: ContextWrapper 页面管理
- **WHEN** 调用 append_page(page) 后再调用 get_page(page.id)
- **THEN** 返回同一个 PageWrapper 实例

#### Scenario: remove_page 更新 current
- **WHEN** 移除的页面是当前 current
- **THEN** current 自动更新为 pages 列表中最后一个页面

#### Scenario: ContextWrapper.close() 保存状态
- **WHEN** 调用 context_wrapper.close()
- **THEN** 系统先保存 storage_state 到 userdata，再关闭 context

---

### Requirement: BrowserWrapper 封装类
系统 SHALL 实现 BrowserWrapper 类封装 Playwright Browser 对象及其生命周期。

**属性：**
- `id`（string）：指定或自动生成的 uuid4().hex
- `browser`（Browser）：Playwright browser 对象
- `browser_type`（BrowserType）：枚举值 KEYS 或 TOUCH
- `used`（int）：当前上下文数量（`len(contexts)`）
- `userdata`（string）：用户数据目录路径
- `_contexts`（List[ContextWrapper]）：上下文列表

**方法：**
- `append_context(ctx)`：将上下文加入列表
- `get_context(context_id) -> ContextWrapper`：查找上下文，不存在时抛出 HTTP 404
- `remove_context(ctx)`：从列表移除上下文
- `as_json()`：返回 `{"id", "used", "browser_type": browser_type.value}`
- `async close()`：关闭 browser，停止 playwright，删除 userdata 目录（PermissionError 时记录日志，不抛出）
- `async is_active() -> bool`：检查 browser.is_connected() 及 browser.version，异常时返回 False

#### Scenario: BrowserWrapper 正确管理上下文列表
- **WHEN** 调用 append_context(ctx) 后再调用 get_context(ctx.id)
- **THEN** 返回同一个 ContextWrapper 实例

#### Scenario: BrowserWrapper.close() 释放资源
- **WHEN** 调用 browser_wrapper.close()
- **THEN** browser 关闭，playwright 停止，userdata 目录被删除

---

### Requirement: 全局浏览器列表与辅助函数
系统 SHALL 在 common.py 模块级别维护 `browser_list: List[BrowserWrapper] = []` 全局列表，并提供 `browser_get(browser_id: str) -> BrowserWrapper` 辅助函数。

browser_get 遍历 browser_list，找不到指定 ID 时抛出 `HTTPException(status_code=404)`。

#### Scenario: browser_get 找到浏览器
- **WHEN** browser_list 中存在 id="abc" 的 BrowserWrapper，调用 browser_get("abc")
- **THEN** 返回该 BrowserWrapper 实例

#### Scenario: browser_get 找不到浏览器
- **WHEN** browser_list 中不存在指定 id，调用 browser_get("nonexistent")
- **THEN** 抛出 HTTPException(status_code=404)
