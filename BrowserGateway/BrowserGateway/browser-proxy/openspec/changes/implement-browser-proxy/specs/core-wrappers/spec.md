## ADDED Requirements

### Requirement: BrowserWrapper 封装类
系统 SHALL 实现 BrowserWrapper 类封装 Playwright Browser 对象及其生命周期。BrowserWrapper 持有：browser（Playwright Browser/BrowserContext 对象）、browser_type（BrowserType 枚举：KEYS/TOUCH）、id（唯一字符串标识）、contexts（List[ContextWrapper]）、userdata（用户数据目录路径）、playwright（Playwright 引擎实例）。提供 id、browser_type、browser、contexts 属性，以及 append_context、get_context、remove_context、close、is_active 方法。

#### Scenario: BrowserWrapper 正确管理上下文列表
- **WHEN** 调用 append_context(ctx) 后再调用 get_context(ctx.id)
- **THEN** 返回同一个 ContextWrapper 实例

#### Scenario: BrowserWrapper.close() 释放资源
- **WHEN** 调用 browser_wrapper.close()
- **THEN** Playwright browser 被关闭，playwright 引擎被停止，contexts 列表被清空

#### Scenario: BrowserWrapper.is_active() 检查状态
- **WHEN** 浏览器正常运行时调用 is_active()
- **THEN** 返回 True；浏览器已关闭时返回 False

### Requirement: ContextWrapper 封装类
系统 SHALL 实现 ContextWrapper 类封装 Playwright BrowserContext 对象。ContextWrapper 持有：browser_id（所属浏览器ID）、context（Playwright BrowserContext）、id（uuid4().hex 生成的唯一ID）、pages（List[PageWrapper]）、current（当前活跃的 PageWrapper）、userdata（状态文件路径）。提供 id、pages、current、context 属性，以及 append_page、get_page、remove_page、close 方法。close() 时保存浏览器状态到 userdata（如有）。

#### Scenario: ContextWrapper 正确管理页面列表
- **WHEN** 调用 append_page(page) 后再调用 get_page(page.id)
- **THEN** 返回同一个 PageWrapper 实例

#### Scenario: ContextWrapper.close() 保存状态
- **WHEN** ContextWrapper 有 userdata 路径时调用 close()
- **THEN** 系统调用 context.storage_state(path=userdata) 保存状态，再关闭 context

### Requirement: PageWrapper 封装类
系统 SHALL 实现 PageWrapper 类封装 Playwright Page 对象。PageWrapper 持有：page（Playwright Page）、id（uuid4().hex）、browser_id、context_id、cdp_session（CDPSession，可选）、elements（Dict[str, ElementWrapper] 元素缓存）。提供 id、page、url、cdp_session 属性，以及 set_element、get_element、to_json 方法。to_json() 返回 {"id", "url", "browser_id", "context_id", "support_cdp_session": bool}。

#### Scenario: PageWrapper 元素缓存
- **WHEN** 调用 set_element(elem) 后再调用 get_element(elem.id)
- **THEN** 返回同一个 ElementWrapper 实例

#### Scenario: PageWrapper.to_json() 序列化
- **WHEN** 调用 page_wrapper.to_json()
- **THEN** 返回包含 id、url、browser_id、context_id、support_cdp_session 字段的字典

### Requirement: ElementWrapper 封装类
系统 SHALL 实现 ElementWrapper 类封装 Playwright Locator/ElementHandle 对象。ElementWrapper 持有：element（Playwright element 对象）、id（uuid4().hex）、preview（元素的 outer HTML 预览字符串）。提供 id、element、preview 属性，以及 to_json() 方法返回 {"id", "preview"}。

#### Scenario: ElementWrapper.to_json() 序列化
- **WHEN** 调用 element_wrapper.to_json()
- **THEN** 返回包含 id 和 preview 字段的字典

### Requirement: 全局浏览器列表与辅助函数
系统 SHALL 在 common.py 模块级别维护 browser_list: List[BrowserWrapper] = [] 全局列表，并提供 browser_get(browser_id: str) -> BrowserWrapper 辅助函数。browser_get 若找不到指定 ID 的浏览器，SHALL 抛出 ValueError 或 KeyError。

#### Scenario: browser_get 找到浏览器
- **WHEN** browser_list 中存在 id="abc" 的 BrowserWrapper，调用 browser_get("abc")
- **THEN** 返回该 BrowserWrapper 实例

#### Scenario: browser_get 找不到浏览器
- **WHEN** browser_list 中不存在指定 id，调用 browser_get("nonexistent")
- **THEN** 抛出异常，API 层捕获后返回 HTTP 404
