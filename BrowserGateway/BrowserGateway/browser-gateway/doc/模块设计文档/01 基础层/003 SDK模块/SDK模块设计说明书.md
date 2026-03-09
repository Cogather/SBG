# BrowserGateway SDK模块设计说明书

## 目录结构

本SDK模块设计说明书涵盖 `src/main/java/com/huawei/browsergateway/sdk` 目录下的所有类，共12个文件：

- `DriverClient.java` - 驱动客户端接口定义
- `ClientImpl.java` - HTTP客户端实现类
- `BrowserDriver.java` - 浏览器驱动实现类
- `ChromiumDriverProxy.java` - Chromium驱动代理类
- `Type.java` - CDP服务数据类型定义
- `Request.java` - CDP服务请求数据模型
- `BrowserOptions.java` - 浏览器配置选项
- `WebElementImpl.java` - Web元素实现类
- `DevToolsProxy.java` - DevTools代理类
- `WindowProxy.java` - 窗口管理代理类
- `NavigationProxy.java` - 导航管理代理类
- `URL.java` - CDP服务URL常量定义

---

## 1. 模块概述

### 1.1 模块定位

SDK模块是BrowserGateway与CDP（Chrome DevTools Protocol）服务通信的客户端封装层，提供以下核心功能：

1. **浏览器管理**：创建、查询、删除浏览器实例
2. **上下文管理**：创建、查询、删除浏览器上下文（会话）
3. **页面管理**：创建、删除页面，执行导航操作
4. **脚本执行**：执行JavaScript脚本和CDP命令
5. **元素操作**：查找元素、执行元素操作（点击、输入等）
6. **Selenium兼容**：提供Selenium WebDriver接口的代理实现

### 1.2 架构设计

```
SDK模块架构：
├── DriverClient接口（顶层接口）
│   ├── Browser接口（浏览器管理）
│   ├── Context接口（上下文管理）
│   └── Page接口（页面管理）
│
├── ClientImpl（HTTP客户端实现）
│   ├── BrowserImpl（浏览器实现）
│   ├── ContextImpl（上下文实现）
│   └── PageImpl（页面实现）
│
├── BrowserDriver（浏览器驱动）
│   └── 封装CDP API调用
│
├── ChromiumDriverProxy（Selenium代理）
│   ├── DevToolsProxy（DevTools代理）
│   ├── WindowProxy（窗口管理代理）
│   └── NavigationProxy（导航管理代理）
│
├── Type（数据类型定义）
├── Request（请求数据模型）
├── BrowserOptions（配置选项）
└── WebElementImpl（Web元素实现）
```

### 1.3 依赖关系

**外部依赖**：
- `org.openqa.selenium` - Selenium WebDriver框架
- `cn.hutool` - Hutool工具库（JSON处理、类型引用等）
- `org.apache.hc.client5.http` - Apache HttpClient 5.x（通过HttpUtil使用）
- `org.jsoup` - JSoup HTML解析库
- `lombok` - Lombok注解库

**内部依赖**：
- `com.huawei.browsergateway.util.HttpUtil` - HTTP请求工具类

---

## 2. 核心接口定义

### 2.1 DriverClient

**文件路径**: `sdk/DriverClient.java`

**作用**: 驱动客户端顶层接口，定义浏览器、上下文、页面的管理接口。

**接口设计**:
- **类型**: 接口
- **包路径**: `com.huawei.browsergateway.sdk`

**嵌套接口定义**:

#### 2.1.1 Browser接口

**功能**: 浏览器管理接口，提供浏览器的创建、查询、删除等操作。

**方法定义**:

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `create(Request.CreateBrowser request)` | `Type.Browser` | 创建浏览器实例 |
| `get(String id)` | `Type.Browser` | 根据ID获取浏览器实例 |
| `list()` | `List<Type.Browser>` | 获取所有浏览器实例列表 |
| `delete(String id)` | `void` | 删除浏览器实例 |
| `healthCheck()` | `Type.HealthCheckResult` | 健康检查 |

**使用场景**: 浏览器实例的生命周期管理。

#### 2.1.2 Context接口

**功能**: 上下文管理接口，提供浏览器上下文的创建、查询、删除等操作。

**方法定义**:

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `create(Request.CreateContext request)` | `Type.Context` | 创建浏览器上下文 |
| `get(String id)` | `Type.Context` | 根据ID获取上下文实例 |
| `list()` | `List<Type.Context>` | 获取所有上下文实例列表 |
| `delete(String id)` | `void` | 删除上下文实例 |
| `saveUserdata(String contextId)` | `void` | 保存用户数据 |
| `page(String contextId)` | `Page` | 获取页面管理接口 |

**使用场景**: 浏览器会话（上下文）的生命周期管理。

#### 2.1.3 Page接口

**功能**: 页面管理接口，提供页面的创建、删除、导航、脚本执行等操作。

**方法定义**:

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `delete(String id)` | `Type.Context` | 删除页面，返回更新后的上下文 |
| `create(String url)` | `Type.Context` | 创建新页面，返回更新后的上下文 |
| `execute(String expression)` | `Request.JSResult` | 执行JavaScript表达式 |
| `executeCdp(String method, Map<String, Object> params)` | `Map<String, Object>` | 执行CDP命令 |
| `gotoUrl(String url)` | `Type.Context` | 导航到指定URL |
| `executeElement(Request.Action action)` | `void` | 执行元素操作 |
| `goBack()` | `void` | 浏览器后退 |
| `goForward()` | `void` | 浏览器前进 |
| `findElement(String selector)` | `Request.JSResult` | 查找元素 |
| `getElementSize(String elementId)` | `Type.Size` | 获取元素尺寸 |

**使用场景**: 页面操作、脚本执行、元素查找等。

#### 2.1.4 DriverClient主接口方法

**方法定义**:

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `browser()` | `Browser` | 获取浏览器管理接口 |
| `context(String browserId)` | `Context` | 获取上下文管理接口 |
| `request(String url, String method, String body, TypeReference<T> typeReference)` | `<T> T` | 发送HTTP请求（带返回类型） |
| `request(String url, String method)` | `void` | 发送HTTP请求（无返回） |

---

## 3. HTTP客户端实现

### 3.1 ClientImpl

**文件路径**: `sdk/ClientImpl.java`

**作用**: HTTP客户端实现类，实现DriverClient接口，提供与CDP服务通信的HTTP客户端功能。

**类设计**:
- **类型**: 实现类
- **实现接口**: `DriverClient`
- **线程安全**: 是（使用HttpUtil连接池）

**核心字段**:

| 字段名 | 类型 | 访问修饰符 | 说明 |
|--------|------|-----------|------|
| `endpoint` | `String` | `private final` | CDP服务端点地址 |
| `browser` | `DriverClient.Browser` | `private final` | 浏览器管理接口实例 |

**构造函数**:

```java
public ClientImpl(String endpoint)
```

**参数**:
- `endpoint` (String): CDP服务端点地址

**实现逻辑**:
1. 保存endpoint地址
2. 创建BrowserImpl实例

**方法定义**:

#### 3.1.1 buildUrl(String url)

**功能**: 构建完整URL。

**参数**:
- `url` (String): 相对路径

**返回值**: `String` - 完整URL（endpoint + url）

**实现逻辑**:
```java
return this.endpoint + url;
```

#### 3.1.2 browser()

**功能**: 获取浏览器管理接口。

**返回值**: `Browser` - 浏览器管理接口实例

#### 3.1.3 context(String browserId)

**功能**: 获取上下文管理接口。

**参数**:
- `browserId` (String): 浏览器ID

**返回值**: `Context` - 上下文管理接口实例

**实现逻辑**:
```java
return new ContextImpl(this, browserId);
```

#### 3.1.4 request(String url, String method)

**功能**: 发送HTTP请求（无返回）。

**参数**:
- `url` (String): 请求URL（相对路径）
- `method` (String): HTTP方法

**实现逻辑**:
```java
HttpUtil.request(buildUrl(url), method, null);
```

#### 3.1.5 request(String url, String method, String body)

**功能**: 发送HTTP请求（无返回类型，带请求体）。

**参数**:
- `url` (String): 请求URL（相对路径）
- `method` (String): HTTP方法
- `body` (String): 请求体

**实现逻辑**:
```java
HttpUtil.request(buildUrl(url), method, body);
```

#### 3.1.6 request(String url, String method, String body, TypeReference<T> typeReference)

**功能**: 发送HTTP请求并返回解析后的对象。

**参数**:
- `url` (String): 请求URL（相对路径）
- `method` (String): HTTP方法
- `body` (String): 请求体
- `typeReference` (TypeReference<T>): 返回类型引用

**返回值**: `<T> T` - 解析后的对象

**实现逻辑**:
```java
return HttpUtil.request(buildUrl(url), method, body, typeReference);
```

#### 3.1.7 convertIntegerToLong(Map<String, Object> map)

**功能**: 将Map中的Integer和Short转换为Long（用于CDP命令执行结果的类型转换）。

**参数**:
- `map` (Map<String, Object>): 待转换的Map

**实现逻辑**:
1. 遍历Map的所有条目
2. 如果值是Map，递归处理
3. 如果值是List，遍历处理每个元素
4. 如果值是Integer，转换为Long
5. 如果值是Short，转换为Long

**使用场景**: CDP命令执行结果的类型统一。

### 3.2 BrowserImpl

**文件路径**: `sdk/ClientImpl.java`（内部类）

**作用**: 浏览器管理接口实现类。

**类设计**:
- **类型**: 内部静态类
- **实现接口**: `DriverClient.Browser`

**核心字段**:

| 字段名 | 类型 | 访问修饰符 | 说明 |
|--------|------|-----------|------|
| `client` | `DriverClient` | `private final` | 驱动客户端实例 |

**构造函数**:

```java
public BrowserImpl(DriverClient client)
```

**方法定义**:

#### 3.2.1 create(Request.CreateBrowser request)

**功能**: 创建浏览器实例。

**参数**:
- `request` (Request.CreateBrowser): 创建浏览器请求对象

**返回值**: `Type.Browser` - 浏览器对象

**异常处理**:
- 如果browser为null，抛出RuntimeException: "failed to create browsers"
- 如果发生异常，抛出RuntimeException并包含原始异常信息

**实现逻辑**:
1. 构建URL: `/api/browsers`
2. 将request对象序列化为JSON字符串
3. 发送POST请求
4. 解析响应为Type.Browser对象
5. 检查结果是否为null，如果为null则抛出异常

#### 3.2.2 get(String id)

**功能**: 根据ID获取浏览器实例。

**参数**:
- `id` (String): 浏览器ID

**返回值**: `Type.Browser` - 浏览器对象

**实现逻辑**:
1. 构建URL: `/api/browsers/{id}`
2. 发送GET请求
3. 解析响应为Type.Browser对象

#### 3.2.3 list()

**功能**: 获取所有浏览器实例列表。

**返回值**: `List<Type.Browser>` - 浏览器列表

**实现逻辑**:
1. 构建URL: `/api/browsers`
2. 发送GET请求
3. 解析响应为List<Type.Browser>对象

#### 3.2.4 delete(String id)

**功能**: 删除浏览器实例。

**参数**:
- `id` (String): 浏览器ID

**实现逻辑**:
1. 构建URL: `/api/browsers/{id}`
2. 发送DELETE请求

#### 3.2.5 healthCheck()

**功能**: 健康检查。

**返回值**: `Type.HealthCheckResult` - 健康检查结果

**实现逻辑**:
1. 构建URL: `/api/browsers/health_check`
2. 发送POST请求
3. 解析响应为Type.HealthCheckResult对象

### 3.3 ContextImpl

**文件路径**: `sdk/ClientImpl.java`（内部类）

**作用**: 上下文管理接口实现类。

**类设计**:
- **类型**: 内部静态类
- **实现接口**: `DriverClient.Context`

**核心字段**:

| 字段名 | 类型 | 访问修饰符 | 说明 |
|--------|------|-----------|------|
| `client` | `ClientImpl` | `private final` | 客户端实例 |
| `browserId` | `String` | `private final` | 浏览器ID |

**构造函数**:

```java
public ContextImpl(ClientImpl client, String browserId)
```

**方法定义**:

#### 3.3.1 create(Request.CreateContext request)

**功能**: 创建浏览器上下文。

**参数**:
- `request` (Request.CreateContext): 创建上下文请求对象

**返回值**: `Type.Context` - 上下文对象

**异常处理**:
- 如果context为null，抛出RuntimeException: "failed to create user interface"
- 如果发生异常，抛出RuntimeException并包含原始异常信息

**实现逻辑**:
1. 构建URL: `/api/browsers/{browserId}/contexts`
2. 将request对象序列化为JSON字符串
3. 发送POST请求
4. 解析响应为Type.Context对象
5. 检查结果是否为null，如果为null则抛出异常

#### 3.3.2 get(String id)

**功能**: 根据ID获取上下文实例。

**参数**:
- `id` (String): 上下文ID

**返回值**: `Type.Context` - 上下文对象

**实现逻辑**:
1. 构建URL: `/api/browsers/{browserId}/contexts/{id}`
2. 发送GET请求
3. 解析响应为Type.Context对象

#### 3.3.3 list()

**功能**: 获取所有上下文实例列表。

**返回值**: `List<Type.Context>` - 上下文列表

**实现逻辑**:
1. 构建URL: `/api/browsers/{browserId}/contexts`
2. 发送GET请求
3. 解析响应为List<Type.Context>对象

#### 3.3.4 delete(String id)

**功能**: 删除上下文实例。

**参数**:
- `id` (String): 上下文ID

**实现逻辑**:
1. 构建URL: `/api/browsers/{browserId}/contexts/{id}`
2. 发送DELETE请求

#### 3.3.5 saveUserdata(String contextId)

**功能**: 保存用户数据。

**参数**:
- `contextId` (String): 上下文ID

**实现逻辑**:
1. 构建URL: `/api/browsers/{browserId}/contexts/{contextId}`
2. 发送PUT请求

#### 3.3.6 page(String contextId)

**功能**: 获取页面管理接口。

**参数**:
- `contextId` (String): 上下文ID

**返回值**: `Page` - 页面管理接口实例

**实现逻辑**:
```java
return new PageImpl(client, browserId, contextId);
```

### 3.4 PageImpl

**文件路径**: `sdk/ClientImpl.java`（内部类）

**作用**: 页面管理接口实现类。

**类设计**:
- **类型**: 内部静态类
- **实现接口**: `DriverClient.Page`

**核心字段**:

| 字段名 | 类型 | 访问修饰符 | 说明 |
|--------|------|-----------|------|
| `client` | `ClientImpl` | `private final` | 客户端实例 |
| `browserId` | `String` | `private final` | 浏览器ID |
| `contextId` | `String` | `private final` | 上下文ID |

**构造函数**:

```java
public PageImpl(ClientImpl client, String browserId, String contextId)
```

**方法定义**:

#### 3.4.1 delete(String id)

**功能**: 删除页面。

**参数**:
- `id` (String): 页面ID

**返回值**: `Type.Context` - 更新后的上下文对象

**实现逻辑**:
1. 构建URL: `/api/browsers/{browserId}/contexts/{contextId}/pages/{id}`
2. 发送DELETE请求
3. 解析响应为Type.Context对象

#### 3.4.2 create(String url)

**功能**: 创建新页面。

**参数**:
- `url` (String): 页面URL

**返回值**: `Type.Context` - 更新后的上下文对象

**实现逻辑**:
1. 构建请求体JSON: `{"url": url}`
2. 构建URL: `/api/browsers/{browserId}/contexts/{contextId}/pages`
3. 发送POST请求
4. 解析响应为Type.Context对象

#### 3.4.3 execute(String expression)

**功能**: 执行JavaScript表达式。

**参数**:
- `expression` (String): JavaScript表达式

**返回值**: `Request.JSResult` - JavaScript执行结果

**实现逻辑**:
1. 构建请求体JSON: `{"expression": expression}`
2. 构建URL: `/api/browsers/{browserId}/contexts/{contextId}/pages/execute`
3. 发送POST请求
4. 解析响应为Request.JSResult对象

#### 3.4.4 executeCdp(String method, Map<String, Object> params)

**功能**: 执行CDP命令。

**参数**:
- `method` (String): CDP方法名
- `params` (Map<String, Object>): CDP参数

**返回值**: `Map<String, Object>` - CDP执行结果

**实现逻辑**:
1. 构建Selenium格式的CDP参数: `{"method": method, "params": params}`
2. 使用Selenium的Json工具序列化为JSON字符串
3. 构建URL: `/api/browsers/{browserId}/contexts/{contextId}/pages/execute_cdp`
4. 发送POST请求
5. 解析响应为Map<String, Object>对象
6. 调用convertIntegerToLong转换结果中的Integer和Short为Long

#### 3.4.5 gotoUrl(String url)

**功能**: 导航到指定URL。

**参数**:
- `url` (String): 目标URL

**返回值**: `Type.Context` - 更新后的上下文对象

**实现逻辑**:
1. 构建请求体JSON: `{"url": url}`
2. 构建URL: `/api/browsers/{browserId}/contexts/{contextId}/pages/goto`
3. 发送POST请求
4. 解析响应为Type.Context对象

#### 3.4.6 executeElement(Request.Action action)

**功能**: 执行元素操作。

**参数**:
- `action` (Request.Action): 元素操作请求对象

**实现逻辑**:
1. 构建URL: `/api/browsers/{browserId}/contexts/{contextId}/pages/element`
2. 将action对象序列化为JSON字符串
3. 发送POST请求

#### 3.4.7 goBack()

**功能**: 浏览器后退。

**实现逻辑**:
1. 构建URL: `/api/browsers/{browserId}/contexts/{contextId}/pages/go_back`
2. 发送POST请求

#### 3.4.8 goForward()

**功能**: 浏览器前进。

**实现逻辑**:
1. 构建URL: `/api/browsers/{browserId}/contexts/{contextId}/pages/go_forward`
2. 发送POST请求

#### 3.4.9 findElement(String selector)

**功能**: 查找元素。

**参数**:
- `selector` (String): 元素选择器

**返回值**: `Request.JSResult` - JavaScript执行结果

**实现逻辑**:
1. 构建请求体JSON: `{"selector": selector}`
2. 构建URL: `/api/browsers/{browserId}/contexts/{contextId}/pages/find_element`
3. 发送POST请求
4. 解析响应为Request.JSResult对象

#### 3.4.10 getElementSize(String elementId)

**功能**: 获取元素尺寸。

**参数**:
- `elementId` (String): 元素ID

**返回值**: `Type.Size` - 元素尺寸对象

**实现逻辑**:
1. 构建URL: `/api/browsers/{browserId}/contexts/{contextId}/pages/element/{elementId}/get_size`
2. 发送POST请求
3. 解析响应为Type.Size对象

---

## 4. 浏览器驱动

### 4.1 BrowserDriver

**文件路径**: `sdk/BrowserDriver.java`

**作用**: 浏览器驱动实现类，封装Chrome API，提供浏览器操作的统一接口，负责管理浏览器实例、上下文和页面的生命周期。

**类设计**:
- **类型**: 数据类（使用Lombok @Data注解）
- **线程安全**: 否（实例级别，非线程安全）

**核心字段**:

| 字段名 | 类型 | 访问修饰符 | 说明 |
|--------|------|-----------|------|
| `context` | `Type.Context` | `private` | 当前上下文对象 |
| `client` | `DriverClient` | `private final` | 驱动客户端实例 |
| `contextClient` | `DriverClient.Context` | `private final` | 上下文客户端实例 |

**构造函数**:

```java
public BrowserDriver(BrowserOptions options)
```

**参数**:
- `options` (BrowserOptions): 浏览器配置选项

**实现逻辑**:
1. 创建ClientImpl实例（使用options.getEndpoint()）
2. 查询是否有可用的浏览器实例：
   - 调用client.browser().list()获取所有浏览器
   - 过滤条件：browserType匹配且used < limit
   - 如果找到可用浏览器，使用它；否则创建新浏览器
3. 创建浏览器（如果需要）：
   - 生成UUID作为浏览器ID
   - 使用Request.CreateBrowser.from(options, uuid)创建请求
   - 调用client.browser().create(request)创建浏览器
4. 获取上下文客户端：client.context(browser.getId())
5. 创建上下文：
   - 使用Request.CreateContext.from(options)创建请求
   - 调用contextClient.create(createContextRequest)创建上下文

**方法定义**:

#### 4.1.1 close()

**功能**: 关闭浏览器上下文。

**实现逻辑**:
```java
client.context(context.getBrowserId()).delete(context.getId());
```

#### 4.1.2 saveUserdata()

**功能**: 保存用户数据。

**实现逻辑**:
```java
client.context(context.getBrowserId()).saveUserdata(context.getId());
```

#### 4.1.3 newPage(String url)

**功能**: 创建新页面。

**参数**:
- `url` (String): 页面URL

**返回值**: `String` - 新页面的ID，如果当前页面为null则返回null

**实现逻辑**:
1. 调用contextClient.page(context.getId()).create(url)创建页面
2. 更新context对象
3. 获取当前页面：context.getCurrentPage()
4. 返回页面ID（如果存在）

#### 4.1.4 gotoUrl(String url)

**功能**: 导航到指定URL。

**参数**:
- `url` (String): 目标URL

**实现逻辑**:
1. 调用contextClient.page(context.getId()).gotoUrl(url)
2. 更新context对象

#### 4.1.5 executeScript(String script)

**功能**: 执行JavaScript脚本。

**参数**:
- `script` (String): JavaScript脚本

**返回值**: `Object` - 执行结果（可能是字符串、数字、元素对象或Map）

**实现逻辑**:
1. 调用contextClient.page(context.getId()).execute(script)执行脚本
2. 获取结果类型：jsResult.getResultType()
3. 根据结果类型进行解析：
   - `"element"`: 使用WebElementImpl.parse(jsResult.getValue(), this)解析为WebElement
   - `"string"`: 直接返回jsResult.getValue()
   - `"int"`: 将jsResult.getValue()转换为Long
   - `"none"`: 返回null
   - `"dict"`: 调用parseDictResult(jsResult)解析为Map
   - 其他: 抛出IllegalArgumentException

#### 4.1.6 parseDictResult(Request.JSResult jsResult)

**功能**: 解析字典类型的结果。

**参数**:
- `jsResult` (Request.JSResult): JavaScript执行结果

**返回值**: `Object` - 解析后的Map对象

**实现逻辑**:
1. 如果elementKeys为空，直接使用JSONUtil解析为Map
2. 否则：
   - 创建结果Map
   - 使用JSONUtil解析为JSONObject
   - 遍历JSONObject的所有键值对
   - 如果键不在elementKeys中，直接添加到结果Map
   - 如果键在elementKeys中，使用WebElementImpl.parse解析为WebElement后添加到结果Map

#### 4.1.7 executeElement(Request.Action action)

**功能**: 执行元素操作。

**参数**:
- `action` (Request.Action): 元素操作请求对象

**实现逻辑**:
```java
contextClient.page(context.getId()).executeElement(action);
```

#### 4.1.8 executeCdp(String method, Map<String, Object> params)

**功能**: 执行CDP命令。

**参数**:
- `method` (String): CDP方法名
- `params` (Map<String, Object>): CDP参数

**返回值**: `Map<String, Object>` - CDP执行结果

**实现逻辑**:
```java
return contextClient.page(context.getId()).executeCdp(method, params);
```

#### 4.1.9 closeCurrentPage()

**功能**: 关闭当前页面。

**实现逻辑**:
1. 获取当前页面：context.getCurrentPage()
2. 如果当前页面不为null，调用contextClient.page(context.getId()).delete(currentPage.getId())
3. 更新context对象

#### 4.1.10 getCurrentUrl()

**功能**: 获取当前页面URL。

**返回值**: `String` - 当前页面URL

**实现逻辑**:
1. 刷新上下文：this.context = contextClient.get(context.getId())
2. 返回context.getCurrentUrl()

#### 4.1.11 back()

**功能**: 浏览器后退。

**实现逻辑**:
```java
contextClient.page(context.getId()).goBack();
```

#### 4.1.12 forward()

**功能**: 浏览器前进。

**实现逻辑**:
```java
contextClient.page(context.getId()).goForward();
```

#### 4.1.13 findElementByTagName(String tagName)

**功能**: 根据标签名查找元素。

**参数**:
- `tagName` (String): 标签名

**返回值**: `WebElement` - WebElement对象

**实现逻辑**:
1. 调用contextClient.page(context.getId()).findElement(tagName)查找元素
2. 使用WebElementImpl.parse(jsResult.getValue(), this)解析为WebElement

#### 4.1.14 getSize(String elementId)

**功能**: 获取元素尺寸。

**参数**:
- `elementId` (String): 元素ID

**返回值**: `Type.Size` - 元素尺寸对象

**实现逻辑**:
```java
return contextClient.page(context.getId()).getElementSize(elementId);
```

---

## 5. Selenium代理层

### 5.1 ChromiumDriverProxy

**文件路径**: `sdk/ChromiumDriverProxy.java`

**作用**: Chromium驱动代理类，继承自Selenium的ChromiumDriver，实现自定义的浏览器驱动功能，提供与CDP服务交互的代理层。

**类设计**:
- **类型**: 代理类
- **继承**: `org.openqa.selenium.chromium.ChromiumDriver`
- **实现**: `WebDriver.TargetLocator`

**核心字段**:

| 字段名 | 类型 | 访问修饰符 | 说明 |
|--------|------|-----------|------|
| `driver` | `BrowserDriver` | `private final` | 浏览器驱动实例 |
| `devTools` | `DevToolsProxy` | `private final` | DevTools代理实例 |
| `webDriver` | `WindowProxy` | `private final` | 窗口代理实例 |

**内部类**:

#### 5.1.1 CommandExecutorProxy

**作用**: 命令执行器代理类，实现CommandExecutor接口。

**方法定义**:

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `execute(Command command)` | `Response` | 执行命令（当前返回null） |

**构造函数**:

```java
public ChromiumDriverProxy(BrowserOptions options)
```

**参数**:
- `options` (BrowserOptions): 浏览器配置选项

**实现逻辑**:
1. 调用父类构造函数：super(new CommandExecutorProxy(), new ChromeOptions(), "goog:chromeOptions")
2. 创建BrowserDriver实例
3. 创建DevToolsProxy实例
4. 创建WindowProxy实例

**方法定义**:

#### 5.1.2 getProxyContextId()

**功能**: 获取代理上下文ID。

**返回值**: `String` - 上下文ID

**实现逻辑**:
```java
return driver.getContext().getId();
```

#### 5.1.3 saveUserdata()

**功能**: 保存用户数据。

**实现逻辑**:
```java
driver.saveUserdata();
```

#### 5.1.4 maybeGetDevTools()

**功能**: 获取DevTools实例（Selenium接口）。

**返回值**: `Optional<DevTools>` - DevTools实例

**实现逻辑**:
```java
return Optional.of(devTools);
```

#### 5.1.5 getWindowHandle()

**功能**: 获取窗口句柄（Selenium接口）。

**返回值**: `String` - 窗口句柄（当前页面ID）

**实现逻辑**:
1. 获取当前页面：driver.getContext().getCurrentPage()
2. 返回页面ID（如果存在），否则返回空字符串

#### 5.1.6 executeScript(String script, Object... args)

**功能**: 执行脚本（Selenium接口）。

**参数**:
- `script` (String): JavaScript脚本
- `args` (Object...): 脚本参数（当前未使用）

**返回值**: `Object` - 执行结果

**特殊处理**:
1. 如果script包含"window.history.go"：
   - 导航到"about:blank"
   - 执行CDP命令"Page.resetNavigationHistory"
   - 返回null
2. 如果script包含"window.history.length"：
   - 返回2L
3. 否则：调用driver.executeScript(script)

#### 5.1.7 get(String url)

**功能**: 导航到URL（Selenium接口）。

**参数**:
- `url` (String): 目标URL

**实现逻辑**:
```java
driver.gotoUrl(url);
```

#### 5.1.8 quit()

**功能**: 退出浏览器（Selenium接口）。

**实现逻辑**:
```java
driver.close();
```

#### 5.1.9 manage()

**功能**: 获取选项管理器（Selenium接口）。

**返回值**: `Options` - 选项管理器（WindowProxy实例）

**实现逻辑**:
```java
return webDriver;
```

#### 5.1.10 executeCdpCommand(String commandName, Map<String, Object> parameters)

**功能**: 执行CDP命令（Selenium接口）。

**参数**:
- `commandName` (String): CDP命令名
- `parameters` (Map<String, Object>): CDP参数

**返回值**: `Map<String, Object>` - CDP执行结果

**实现逻辑**:
```java
return driver.executeCdp(commandName, parameters);
```

#### 5.1.11 getCurrentUrl()

**功能**: 获取当前URL（Selenium接口）。

**返回值**: `String` - 当前页面URL

**实现逻辑**:
```java
return driver.getCurrentUrl();
```

#### 5.1.12 close()

**功能**: 关闭当前窗口（Selenium接口）。

**实现逻辑**:
```java
driver.closeCurrentPage();
```

#### 5.1.13 switchTo()

**功能**: 切换到目标定位器（Selenium接口）。

**返回值**: `TargetLocator` - 目标定位器（自身）

**实现逻辑**:
```java
return this;
```

#### 5.1.14 window(String nameOrHandle)

**功能**: 切换到指定窗口（Selenium接口，当前忽略）。

**参数**:
- `nameOrHandle` (String): 窗口名称或句柄

**返回值**: `WebDriver` - 当前驱动实例

**实现逻辑**:
1. 记录日志：忽略切换窗口操作
2. 返回this

#### 5.1.15 perform(Collection<Sequence> actions)

**功能**: 执行动作序列（Selenium接口，当前忽略）。

**参数**:
- `actions` (Collection<Sequence>): 动作序列

**实现逻辑**:
1. 记录日志：忽略执行动作
2. 空实现

#### 5.1.16 navigate()

**功能**: 获取导航对象（Selenium接口）。

**返回值**: `Navigation` - 导航对象（NavigationProxy实例）

**实现逻辑**:
```java
return new NavigationProxy(driver);
```

#### 5.1.17 findElement(By locator)

**功能**: 查找元素（Selenium接口）。

**参数**:
- `locator` (By): 定位器

**返回值**: `WebElement` - WebElement对象

**支持情况**:
- 支持：`By.ByTagName` - 从toString结果中提取tagName，调用driver.findElementByTagName(tagName)
- 不支持：其他定位器类型，抛出UnsupportedOperationException

**实现逻辑**:
1. 如果locator是By.ByTagName类型：
   - 从toString结果中提取tagName（去除"By.tagName: "前缀）
   - 调用driver.findElementByTagName(tagName)
2. 否则：抛出UnsupportedOperationException

**其他方法**: 大部分Selenium接口方法当前不支持，抛出UnsupportedOperationException，包括：
- frame相关方法
- newWindow
- defaultContent
- activeElement
- alert
- Script相关方法
- 文件检测器相关方法
- 日志事件相关方法
- 认证相关方法
- BiDi相关方法
- Cast相关方法
- 权限相关方法
- 网络条件相关方法
- 会话相关方法
- 截图相关方法
- 打印相关方法
- 页面源码相关方法
- 窗口句柄集合相关方法
- 异步脚本执行相关方法
- 网络相关方法
- 元素转换器相关方法
- 日志级别相关方法
- 命令执行相关方法
- 输入状态重置相关方法
- 虚拟认证器相关方法
- 下载文件相关方法
- 延迟相关方法
- 联合凭据管理对话框相关方法
- 日志记录相关方法
- 文件检测器获取相关方法
- 下载启用要求相关方法

---

### 5.2 DevToolsProxy

**文件路径**: `sdk/DevToolsProxy.java`

**作用**: DevTools代理类，提供Chrome DevTools Protocol命令执行能力，继承自Selenium的DevTools类。

**类设计**:
- **类型**: 代理类
- **继承**: `org.openqa.selenium.devtools.DevTools`

**核心字段**:

| 字段名 | 类型 | 访问修饰符 | 说明 |
|--------|------|-----------|------|
| `driver` | `BrowserDriver` | `private final` | 浏览器驱动实例 |

**构造函数**:

```java
public DevToolsProxy(BrowserDriver driver)
```

**参数**:
- `driver` (BrowserDriver): 浏览器驱动实例

**实现逻辑**:
1. 调用父类构造函数：super((dt) -> null, new ConnectionProxy(new HttpClientProxy(), ""))
2. 保存driver实例

**方法定义**:

#### 5.2.1 send(Command<X> command)

**功能**: 发送CDP命令。

**参数**:
- `command` (Command<X>): CDP命令对象

**返回值**: `<X> X` - 命令执行结果

**实现逻辑**:
1. 获取命令方法名：command.getMethod()
2. 特殊处理"Target.createTarget"命令：
   - 从参数中获取url
   - 调用driver.newPage(url)创建新页面
   - 返回TargetID对象
3. 其他命令：
   - 调用driver.executeCdp(method, command.getParams())
   - 返回Object对象

#### 5.2.2 createSession(String windowHandle)

**功能**: 创建会话。

**参数**:
- `windowHandle` (String): 窗口句柄

**实现逻辑**:
1. 记录日志：忽略创建会话操作
2. 空实现

**内部类**:

#### 5.2.3 HttpClientProxy

**作用**: HTTP客户端代理类，实现HttpClient接口。

**方法定义**:

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `openSocket(HttpRequest request, WebSocket.Listener listener)` | `WebSocket` | 打开WebSocket连接（当前返回null） |
| `execute(HttpRequest request)` | `HttpResponse` | 执行HTTP请求（当前返回null） |

#### 5.2.4 ConnectionProxy

**作用**: 连接代理类，继承自Selenium的Connection类。

**构造函数**:

```java
public ConnectionProxy(HttpClient client, String url)
```

**参数**:
- `client` (HttpClient): HTTP客户端
- `url` (String): 连接URL

---

### 5.3 WindowProxy

**文件路径**: `sdk/WindowProxy.java`

**作用**: 窗口管理代理类，实现WebDriver的Options和Window接口，提供浏览器窗口和选项的管理功能。

**类设计**:
- **类型**: 代理类
- **实现接口**: `WebDriver.Options`, `WebDriver.Window`

**核心字段**:

| 字段名 | 类型 | 访问修饰符 | 说明 |
|--------|------|-----------|------|
| `driver` | `BrowserDriver` | `private final` | 浏览器驱动实例 |

**构造函数**:

```java
public WindowProxy(BrowserDriver driver)
```

**方法定义**:

#### 5.3.1 window()

**功能**: 获取窗口管理器（Selenium接口）。

**返回值**: `WebDriver.Window` - 窗口管理器（自身）

**实现逻辑**:
```java
return this;
```

#### 5.3.2 setSize(Dimension targetSize)

**功能**: 设置窗口大小（Selenium接口）。

**参数**:
- `targetSize` (Dimension): 目标尺寸

**实现逻辑**:
1. 记录日志：设置窗口大小
2. 空实现（当前不支持）

**其他方法**: 大部分Selenium接口方法当前为空实现或返回null，包括：
- addCookie
- deleteCookieNamed
- deleteCookie
- deleteAllCookies
- getCookies
- getCookieNamed
- timeouts
- logs
- getSize
- getPosition
- setPosition
- maximize
- minimize
- fullscreen

---

### 5.4 NavigationProxy

**文件路径**: `sdk/NavigationProxy.java`

**作用**: 导航管理代理类，实现WebDriver的Navigation接口，提供浏览器导航功能。

**类设计**:
- **类型**: 代理类
- **实现接口**: `WebDriver.Navigation`

**核心字段**:

| 字段名 | 类型 | 访问修饰符 | 说明 |
|--------|------|-----------|------|
| `browserDriver` | `BrowserDriver` | `private final` | 浏览器驱动实例 |

**构造函数**:

```java
public NavigationProxy(BrowserDriver browserDriver)
```

**方法定义**:

#### 5.4.1 back()

**功能**: 浏览器后退（Selenium接口）。

**实现逻辑**:
```java
browserDriver.back();
```

#### 5.4.2 forward()

**功能**: 浏览器前进（Selenium接口）。

**实现逻辑**:
```java
browserDriver.forward();
```

#### 5.4.3 to(String url)

**功能**: 导航到URL（Selenium接口）。

**参数**:
- `url` (String): 目标URL

**实现逻辑**:
```java
browserDriver.gotoUrl(url);
```

#### 5.4.4 to(URL url)

**功能**: 导航到URL（Selenium接口，URL对象版本）。

**参数**:
- `url` (URL): 目标URL对象

**实现逻辑**:
```java
browserDriver.gotoUrl(url.toString());
```

#### 5.4.5 refresh()

**功能**: 刷新页面（Selenium接口）。

**实现逻辑**:
1. 获取当前URL：browserDriver.getCurrentUrl()
2. 导航到当前URL：browserDriver.gotoUrl(currentUrl)

---

## 6. 数据类型定义

### 6.1 Type

**文件路径**: `sdk/Type.java`

**作用**: CDP服务数据类型定义，包含Browser、Context、Page等核心数据模型。

**类设计**:
- **类型**: 数据类容器（包含多个静态内部类）

**嵌套类定义**:

#### 6.1.1 BrowserType枚举

**作用**: 浏览器类型枚举。

**枚举值**:

| 枚举值 | ID | 说明 |
|--------|-----|------|
| `KEYS` | 1 | 键盘输入类型 |
| `TOUCH` | 2 | 触摸输入类型 |

**方法定义**:

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `valueOf(int id)` | `BrowserType` | 根据ID获取浏览器类型，如果ID不存在则抛出IllegalArgumentException |

#### 6.1.2 Browser类

**作用**: 浏览器对象。

**字段定义**:

| 字段名 | 类型 | 访问修饰符 | 说明 |
|--------|------|-----------|------|
| `id` | `String` | `private` | 浏览器ID |
| `browserType` | `BrowserType` | `private` | 浏览器类型（JSON字段名：browser_type） |
| `used` | `Integer` | `private` | 已使用数量 |

**注解**: 使用Lombok @Data注解，使用Hutool @Alias注解处理JSON字段映射。

#### 6.1.3 Context类

**作用**: 上下文对象，表示浏览器的一个会话上下文。

**字段定义**:

| 字段名 | 类型 | 访问修饰符 | 说明 |
|--------|------|-----------|------|
| `id` | `String` | `private` | 上下文ID |
| `current` | `String` | `private` | 当前页面ID |
| `browserId` | `String` | `private` | 所属浏览器ID（JSON字段名：browser_id） |
| `pages` | `List<Page>` | `private` | 页面列表 |

**方法定义**:

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `getCurrentPage()` | `Page` | 获取当前页面对象，如果不存在则返回null |
| `getCurrentUrl()` | `String` | 获取当前页面URL，如果当前页面不存在则抛出RuntimeException |

**实现逻辑**:
- `getCurrentPage()`: 从pages列表中查找id等于current的页面
- `getCurrentUrl()`: 调用getCurrentPage()获取当前页面，如果为null则抛出异常，否则返回页面的url

#### 6.1.4 Page类

**作用**: 页面对象，表示浏览器中的一个标签页。

**字段定义**:

| 字段名 | 类型 | 访问修饰符 | 说明 |
|--------|------|-----------|------|
| `id` | `String` | `private` | 页面ID |
| `url` | `String` | `private` | 页面URL |
| `browserId` | `String` | `private` | 所属浏览器ID（JSON字段名：browser_id） |
| `contextId` | `String` | `private` | 所属上下文ID（JSON字段名：context_id） |
| `supportCdpSession` | `Boolean` | `private` | 是否支持CDP会话（JSON字段名：support_cdp_session） |

#### 6.1.5 Size类

**作用**: 尺寸对象，表示元素的宽度和高度。

**字段定义**:

| 字段名 | 类型 | 访问修饰符 | 说明 |
|--------|------|-----------|------|
| `width` | `double` | `private` | 宽度 |
| `height` | `double` | `private` | 高度 |

#### 6.1.6 HealthCheckResult类

**作用**: 健康检查结果对象。

**字段定义**:

| 字段名 | 类型 | 访问修饰符 | 说明 |
|--------|------|-----------|------|
| `success` | `boolean` | `private` | 是否成功 |
| `errContexts` | `List<String>` | `private` | 错误上下文列表（JSON字段名：err_contexts） |

---

### 6.2 Request

**文件路径**: `sdk/Request.java`

**作用**: CDP服务请求数据模型，包含浏览器、上下文、页面操作相关的请求对象定义。

**类设计**:
- **类型**: 数据类容器（包含多个静态内部类）

**嵌套类定义**:

#### 6.2.1 CreateBrowser类

**作用**: 创建浏览器请求对象。

**字段定义**:

| 字段名 | 类型 | 访问修饰符 | 说明 |
|--------|------|-----------|------|
| `executablePath` | `String` | `private` | 浏览器可执行文件路径（JSON字段名：executable_path） |
| `baseData` | `String` | `private` | 浏览器基础数据目录（JSON字段名：base_data） |
| `extensionPaths` | `List<String>` | `private` | 扩展插件路径列表（JSON字段名：extension_paths） |
| `extensionIds` | `List<String>` | `private` | 扩展插件ID列表（JSON字段名：extension_ids） |
| `allowlistedExtensionId` | `String` | `private` | 允许列表中的扩展ID（JSON字段名：allowlisted_extension_id） |
| `browserType` | `BrowserType` | `private` | 浏览器类型（JSON字段名：browser_type） |
| `headless` | `boolean` | `private` | 是否无头模式 |
| `language` | `String` | `private` | 语言设置 |

**静态方法**:

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `from(BrowserOptions options, String id)` | `CreateBrowser` | 从浏览器选项创建请求对象 |

**from方法实现逻辑**:
1. 创建CreateBrowser实例
2. 设置browserType：options.getBrowserType()
3. 设置executablePath：options.getExecutablePath()
4. 设置extensionIds：options.getExtensionIds()
5. 设置extensionPaths：options.getExtensionPaths()
6. 设置allowlistedExtensionId：options.getAllowlistedExtensionId()
7. 设置baseData：FileUtil.file(options.getBaseDataDir(), id).getAbsolutePath()
8. 设置headless：options.isHeadless()
9. 设置language：options.getLanguage()
10. 返回CreateBrowser实例

#### 6.2.2 ViewPort类

**作用**: 视口配置对象，定义浏览器窗口的宽度和高度。

**字段定义**:

| 字段名 | 类型 | 访问修饰符 | 说明 |
|--------|------|-----------|------|
| `width` | `Integer` | `private` | 视口宽度 |
| `height` | `Integer` | `private` | 视口高度 |

**构造函数**:

```java
public ViewPort(Integer width, Integer height)
```

#### 6.2.3 CreateContext类

**作用**: 创建上下文请求对象。

**字段定义**:

| 字段名 | 类型 | 访问修饰符 | 说明 |
|--------|------|-----------|------|
| `url` | `String` | `private` | 初始URL |
| `viewport` | `ViewPort` | `private` | 视口配置 |
| `userdata` | `String` | `private` | 用户数据目录 |
| `data` | `String` | `private` | 录制数据 |
| `language` | `String` | `private` | 语言设置 |

**静态方法**:

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `from(BrowserOptions options)` | `CreateContext` | 从浏览器选项创建上下文请求对象 |

**from方法实现逻辑**:
1. 创建CreateContext实例
2. 设置url：options.getUrl()
3. 设置userdata：options.getUserdata()
4. 设置viewport：options.getViewpoint()
5. 设置data：options.getRecordData()
6. 设置language：options.getLanguage()
7. 返回CreateContext实例

#### 6.2.4 JSResult类

**作用**: JavaScript执行结果对象。

**字段定义**:

| 字段名 | 类型 | 访问修饰符 | 说明 |
|--------|------|-----------|------|
| `resultType` | `String` | `private` | 结果类型（element/string/int/none/dict）（JSON字段名：result_type） |
| `value` | `String` | `private` | 结果值（JSON字段名：value） |
| `elementKeys` | `List<String>` | `private` | 元素键列表（JSON字段名：element_keys，默认值：空列表） |

#### 6.2.5 Element类

**作用**: 页面元素对象。

**字段定义**:

| 字段名 | 类型 | 访问修饰符 | 说明 |
|--------|------|-----------|------|
| `id` | `String` | `private` | 元素ID |
| `preview` | `String` | `private` | 元素预览信息 |

#### 6.2.6 Action类

**作用**: 元素操作请求对象。

**字段定义**:

| 字段名 | 类型 | 访问修饰符 | 说明 |
|--------|------|-----------|------|
| `elementId` | `String` | `private` | 元素ID（JSON字段名：element_id） |
| `action` | `String` | `private` | 操作类型 |
| `value` | `String` | `private` | 操作值 |

**构造函数**:

```java
public Action(String elementId, String action, String value)
```

---

### 6.3 BrowserOptions

**文件路径**: `sdk/BrowserOptions.java`

**作用**: 浏览器配置选项，包含浏览器启动和上下文创建所需的所有配置参数。

**类设计**:
- **类型**: 数据类（使用Lombok @Data注解）

**字段定义**:

| 字段名 | 类型 | 访问修饰符 | 说明 |
|--------|------|-----------|------|
| `endpoint` | `String` | `private` | CDP服务端点地址 |
| `browserType` | `BrowserType` | `private` | 浏览器类型 |
| `baseDataDir` | `String` | `private` | 基础数据目录 |
| `executablePath` | `String` | `private` | 浏览器可执行文件路径 |
| `extensionPaths` | `List<String>` | `private` | 扩展插件路径列表 |
| `extensionIds` | `List<String>` | `private` | 扩展插件ID列表 |
| `allowlistedExtensionId` | `String` | `private` | 允许列表中的扩展ID |
| `headless` | `boolean` | `private` | 是否无头模式 |
| `url` | `String` | `private` | 初始URL |
| `viewpoint` | `Request.ViewPort` | `private` | 视口配置 |
| `userdata` | `String` | `private` | 用户数据目录 |
| `recordData` | `String` | `private` | 录制数据（JSON字段名：data） |
| `language` | `String` | `private` | 语言设置 |
| `limit` | `int` | `private` | 浏览器使用限制数量 |

---

### 6.4 URL

**文件路径**: `sdk/URL.java`

**作用**: CDP服务URL常量定义。

**类设计**:
- **类型**: 接口（常量容器）

**常量定义**:

| 常量名 | 类型 | 值 | 说明 |
|--------|------|-----|------|
| `BROWSER` | `String` | `"/"` | 浏览器根路径 |

---

## 7. Web元素实现

### 7.1 WebElementImpl

**文件路径**: `sdk/WebElementImpl.java`

**作用**: Web元素实现类，实现Selenium的WebElement接口，提供页面元素的操作能力。

**类设计**:
- **类型**: 实现类
- **实现接口**: `org.openqa.selenium.WebElement`

**核心字段**:

| 字段名 | 类型 | 访问修饰符 | 说明 |
|--------|------|-----------|------|
| `DATE_FORMATTERS` | `List<SimpleDateFormat>` | `private static final` | 日期格式化器列表，支持多种日期格式 |
| `id` | `String` | `private final` | 元素ID |
| `preview` | `String` | `private final` | 元素预览HTML |
| `driver` | `BrowserDriver` | `private final` | 浏览器驱动实例 |
| `element` | `Element` | `private` | 解析后的JSoup元素对象 |

**静态初始化块**:

```java
static {
    DATE_FORMATTERS.add(new SimpleDateFormat("yyyy-MM-dd"));
    DATE_FORMATTERS.add(new SimpleDateFormat("MM-dd-yyyy"));
    DATE_FORMATTERS.add(new SimpleDateFormat("dd-MM-yyyy"));
    DATE_FORMATTERS.add(new SimpleDateFormat("yyyy/MM/dd"));
    DATE_FORMATTERS.add(new SimpleDateFormat("dd/MM/yyyy"));
    DATE_FORMATTERS.add(new SimpleDateFormat("MM/dd/yyyy"));
    DATE_FORMATTERS.add(new SimpleDateFormat("yyyy.MM.dd"));
    DATE_FORMATTERS.add(new SimpleDateFormat("dd.MM.yyyy"));
    DATE_FORMATTERS.add(new SimpleDateFormat("MM.dd.yyyy"));
    DATE_FORMATTERS.add(new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH));
    DATE_FORMATTERS.add(new SimpleDateFormat("MMM-dd-yyyy", Locale.ENGLISH));
}
```

**静态方法**:

#### 7.1.1 parse(String json, BrowserDriver driver)

**功能**: 从JSON字符串解析WebElement对象。

**参数**:
- `json` (String): JSON字符串
- `driver` (BrowserDriver): 浏览器驱动实例

**返回值**: `WebElement` - WebElement对象

**实现逻辑**:
1. 使用JSONUtil将json解析为Request.Element对象
2. 创建WebElementImpl实例：new WebElementImpl(element.getId(), element.getPreview(), driver)
3. 返回WebElementImpl实例

**构造函数**:

```java
public WebElementImpl(String id, String preview, BrowserDriver driver)
```

**实现逻辑**:
1. 保存id、preview、driver
2. 如果preview等于"node"，设置element为null并返回
3. 否则：
   - 使用Jsoup.parseBodyFragment(preview)解析HTML
   - 获取body的第一个子元素作为element
   - 如果解析失败，记录错误日志并设置element为null

**方法定义**:

#### 7.1.2 click()

**功能**: 点击元素（Selenium接口）。

**实现逻辑**: 空实现

#### 7.1.3 submit()

**功能**: 提交表单（Selenium接口）。

**实现逻辑**: 空实现

#### 7.1.4 sendKeys(CharSequence... keysToSend)

**功能**: 发送键盘输入（Selenium接口）。

**参数**:
- `keysToSend` (CharSequence...): 要发送的键序列

**实现逻辑**:
1. 如果keysToSend为null或长度为0，直接返回
2. 获取第一个键：keysToSend[0]
3. 如果第一个键是"CONTROL+a"或"DELETE"，直接返回（忽略这些特殊键）
4. 拼接所有键为字符串
5. 去除首尾空格，如果为空则返回
6. 特殊处理日期类型的input元素：
   - 如果tagName是"input"且type是"date"，调用convertDate转换日期格式
7. 确定操作类型action为"send_key"
8. 特殊处理文件类型的input元素：
   - 如果tagName是"input"且type是"file"，检查输入内容是否为文件路径
   - 如果文件存在，将action改为"set_file"
9. 调用driver.executeElement(new Request.Action(id, action, inputContent))
10. 如果是文件上传操作，执行后删除临时文件

#### 7.1.5 clear()

**功能**: 清空元素内容（Selenium接口）。

**实现逻辑**: 空实现

#### 7.1.6 getTagName()

**功能**: 获取标签名（Selenium接口）。

**返回值**: `String` - 标签名，如果element为null则返回空字符串

**实现逻辑**:
```java
return element == null ? "" : element.tagName();
```

#### 7.1.7 getAttribute(String name)

**功能**: 获取元素属性（Selenium接口）。

**参数**:
- `name` (String): 属性名

**返回值**: `String` - 属性值，如果element为null或属性不存在则返回空字符串

**实现逻辑**:
1. 如果element为null，返回空字符串
2. 获取属性：element.attribute(name)
3. 如果属性为null，返回空字符串，否则返回属性值

#### 7.1.8 isSelected()

**功能**: 判断元素是否被选中（Selenium接口）。

**返回值**: `boolean` - 固定返回false

#### 7.1.9 isEnabled()

**功能**: 判断元素是否启用（Selenium接口）。

**返回值**: `boolean` - 固定返回false

#### 7.1.10 getText()

**功能**: 获取元素文本（Selenium接口）。

**返回值**: `String` - 元素文本，如果element为null则返回空字符串

**实现逻辑**:
```java
return element == null ? "" : element.text();
```

#### 7.1.11 findElements(By by)

**功能**: 查找子元素列表（Selenium接口）。

**参数**:
- `by` (By): 定位器

**返回值**: `List<WebElement>` - 固定返回空列表

#### 7.1.12 findElement(By by)

**功能**: 查找子元素（Selenium接口）。

**参数**:
- `by` (By): 定位器

**返回值**: `WebElement` - 固定返回null

#### 7.1.13 isDisplayed()

**功能**: 判断元素是否显示（Selenium接口）。

**返回值**: `boolean` - 固定返回false

#### 7.1.14 getLocation()

**功能**: 获取元素位置（Selenium接口）。

**返回值**: `Point` - 固定返回null

#### 7.1.15 getSize()

**功能**: 获取元素尺寸（Selenium接口）。

**返回值**: `Dimension` - 元素尺寸对象

**实现逻辑**:
1. 调用driver.getSize(id)获取Type.Size对象
2. 将width和height转换为int，创建Dimension对象返回

#### 7.1.16 getRect()

**功能**: 获取元素矩形（Selenium接口）。

**返回值**: `Rectangle` - 固定返回null

#### 7.1.17 getCssValue(String propertyName)

**功能**: 获取CSS属性值（Selenium接口）。

**参数**:
- `propertyName` (String): CSS属性名

**返回值**: `String` - 固定返回空字符串

#### 7.1.18 getScreenshotAs(OutputType<X> target)

**功能**: 获取元素截图（Selenium接口）。

**参数**:
- `target` (OutputType<X>): 输出类型

**返回值**: `<X> X` - 固定返回null

#### 7.1.19 convertDate(String dateStr)

**功能**: 日期格式转换，用于处理日期输入场景。

**参数**:
- `dateStr` (String): 输入的日期字符串（任意格式，selenium风格）

**返回值**: `String` - 转换后的日期字符串（yyyy-MM-dd格式，playwright风格）

**异常处理**:
- 如果输入不是有效的日期格式，抛出RuntimeException: "invalid input: not a valid date format"

**实现逻辑**:
1. 如果dateStr为null或空，返回null
2. 遍历所有日期格式化器：
   - 设置formatter为严格模式（setLenient(false)）
   - 尝试解析dateStr
   - 如果解析成功，使用"yyyy-MM-dd"格式格式化并返回
3. 如果所有格式都解析失败，记录错误日志并抛出异常

**支持的日期格式**:
- yyyy-MM-dd
- MM-dd-yyyy
- dd-MM-yyyy
- yyyy/MM/dd
- dd/MM/yyyy
- MM/dd/yyyy
- yyyy.MM.dd
- dd.MM.yyyy
- MM.dd.yyyy
- dd-MMM-yyyy (英文月份)
- MMM-dd-yyyy (英文月份)

---

## 8. API端点映射

### 8.1 浏览器管理API

| 操作 | HTTP方法 | URL路径 | 请求体 | 响应类型 |
|------|---------|---------|--------|---------|
| 创建浏览器 | POST | `/api/browsers` | `Request.CreateBrowser` | `Type.Browser` |
| 获取浏览器 | GET | `/api/browsers/{id}` | 无 | `Type.Browser` |
| 列出浏览器 | GET | `/api/browsers` | 无 | `List<Type.Browser>` |
| 删除浏览器 | DELETE | `/api/browsers/{id}` | 无 | 无 |
| 健康检查 | POST | `/api/browsers/health_check` | 无 | `Type.HealthCheckResult` |

### 8.2 上下文管理API

| 操作 | HTTP方法 | URL路径 | 请求体 | 响应类型 |
|------|---------|---------|--------|---------|
| 创建上下文 | POST | `/api/browsers/{browserId}/contexts` | `Request.CreateContext` | `Type.Context` |
| 获取上下文 | GET | `/api/browsers/{browserId}/contexts/{id}` | 无 | `Type.Context` |
| 列出上下文 | GET | `/api/browsers/{browserId}/contexts` | 无 | `List<Type.Context>` |
| 删除上下文 | DELETE | `/api/browsers/{browserId}/contexts/{id}` | 无 | 无 |
| 保存用户数据 | PUT | `/api/browsers/{browserId}/contexts/{contextId}` | 无 | 无 |

### 8.3 页面管理API

| 操作 | HTTP方法 | URL路径 | 请求体 | 响应类型 |
|------|---------|---------|--------|---------|
| 创建页面 | POST | `/api/browsers/{browserId}/contexts/{contextId}/pages` | `{"url": "..."}` | `Type.Context` |
| 删除页面 | DELETE | `/api/browsers/{browserId}/contexts/{contextId}/pages/{id}` | 无 | `Type.Context` |
| 执行JavaScript | POST | `/api/browsers/{browserId}/contexts/{contextId}/pages/execute` | `{"expression": "..."}` | `Request.JSResult` |
| 执行CDP命令 | POST | `/api/browsers/{browserId}/contexts/{contextId}/pages/execute_cdp` | `{"method": "...", "params": {...}}` | `Map<String, Object>` |
| 导航到URL | POST | `/api/browsers/{browserId}/contexts/{contextId}/pages/goto` | `{"url": "..."}` | `Type.Context` |
| 执行元素操作 | POST | `/api/browsers/{browserId}/contexts/{contextId}/pages/element` | `Request.Action` | 无 |
| 浏览器后退 | POST | `/api/browsers/{browserId}/contexts/{contextId}/pages/go_back` | 无 | 无 |
| 浏览器前进 | POST | `/api/browsers/{browserId}/contexts/{contextId}/pages/go_forward` | 无 | 无 |
| 查找元素 | POST | `/api/browsers/{browserId}/contexts/{contextId}/pages/find_element` | `{"selector": "..."}` | `Request.JSResult` |
| 获取元素尺寸 | POST | `/api/browsers/{browserId}/contexts/{contextId}/pages/element/{elementId}/get_size` | 无 | `Type.Size` |

---

## 9. 使用示例

### 9.1 基本使用流程

```java
// 1. 创建浏览器配置
BrowserOptions options = new BrowserOptions();
options.setEndpoint("http://cdp-service:8080");
options.setBrowserType(Type.BrowserType.KEYS);
options.setBaseDataDir("/tmp/browser-data");
options.setExecutablePath("/usr/bin/chrome");
options.setUrl("https://example.com");
options.setLimit(10);

// 2. 创建浏览器驱动
BrowserDriver driver = new BrowserDriver(options);

// 3. 导航到URL
driver.gotoUrl("https://www.example.com");

// 4. 执行JavaScript
Object result = driver.executeScript("document.title");

// 5. 查找元素
WebElement element = driver.findElementByTagName("button");

// 6. 执行元素操作
Request.Action action = new Request.Action(elementId, "click", "");
driver.executeElement(action);

// 7. 执行CDP命令
Map<String, Object> params = new HashMap<>();
params.put("url", "https://example.com");
Map<String, Object> result = driver.executeCdp("Page.navigate", params);

// 8. 关闭浏览器
driver.close();
```

### 9.2 Selenium兼容使用

```java
// 1. 创建浏览器配置
BrowserOptions options = new BrowserOptions();
options.setEndpoint("http://cdp-service:8080");
// ... 设置其他配置

// 2. 创建ChromiumDriverProxy（Selenium兼容）
ChromiumDriverProxy driver = new ChromiumDriverProxy(options);

// 3. 使用Selenium标准接口
driver.get("https://www.example.com");
String title = (String) driver.executeScript("return document.title;");
WebElement element = driver.findElement(By.tagName("button"));
element.sendKeys("Hello");

// 4. 使用DevTools
Optional<DevTools> devTools = driver.maybeGetDevTools();
devTools.ifPresent(dt -> {
    // 使用DevTools执行CDP命令
});

// 5. 使用导航
driver.navigate().back();
driver.navigate().forward();
driver.navigate().refresh();

// 6. 退出
driver.quit();
```

### 9.3 直接使用ClientImpl

```java
// 1. 创建客户端
ClientImpl client = new ClientImpl("http://cdp-service:8080");

// 2. 创建浏览器
Request.CreateBrowser createBrowserRequest = new Request.CreateBrowser();
createBrowserRequest.setBrowserType(Type.BrowserType.KEYS);
// ... 设置其他参数
Type.Browser browser = client.browser().create(createBrowserRequest);

// 3. 创建上下文
DriverClient.Context contextClient = client.context(browser.getId());
Request.CreateContext createContextRequest = new Request.CreateContext();
createContextRequest.setUrl("https://example.com");
Type.Context context = contextClient.create(createContextRequest);

// 4. 获取页面管理接口
DriverClient.Page pageClient = contextClient.page(context.getId());

// 5. 执行操作
Request.JSResult result = pageClient.execute("document.title");
Type.Context updatedContext = pageClient.gotoUrl("https://www.example.com");

// 6. 清理
contextClient.delete(context.getId());
client.browser().delete(browser.getId());
```

---

## 10. 设计原则与注意事项

### 10.1 设计原则

1. **分层设计**：
   - 接口层（DriverClient）：定义抽象接口
   - 实现层（ClientImpl）：HTTP通信实现
   - 驱动层（BrowserDriver）：业务逻辑封装
   - 代理层（ChromiumDriverProxy）：Selenium兼容

2. **职责分离**：
   - 每个类只负责一个明确的职责
   - 接口定义与实现分离
   - 数据模型与业务逻辑分离

3. **可扩展性**：
   - 通过接口定义支持多种实现
   - 代理模式支持功能扩展

4. **兼容性**：
   - 提供Selenium WebDriver标准接口
   - 支持CDP命令直接执行

### 10.2 注意事项

1. **线程安全**：
   - BrowserDriver实例不是线程安全的，每个线程应使用独立的实例
   - ClientImpl使用HttpUtil连接池，支持并发请求

2. **错误处理**：
   - HTTP请求失败会抛出RuntimeException
   - CDP命令执行失败会返回错误信息
   - JavaScript执行失败会抛出异常

3. **资源管理**：
   - 使用完BrowserDriver后应调用close()释放资源
   - 浏览器实例和上下文需要显式删除

4. **类型转换**：
   - CDP命令执行结果中的Integer和Short会自动转换为Long
   - JavaScript执行结果根据resultType进行类型转换

5. **日期格式处理**：
   - WebElementImpl支持多种日期格式的自动转换
   - 日期输入会自动转换为yyyy-MM-dd格式

6. **Selenium兼容性**：
   - 仅部分Selenium接口已实现
   - 未实现的接口会抛出UnsupportedOperationException
   - 支持的定位器类型：By.ByTagName

7. **浏览器实例复用**：
   - BrowserDriver会自动查找可用的浏览器实例
   - 如果找到可用实例（used < limit），则复用；否则创建新实例

---

## 11. 依赖关系图

```
DriverClient (接口)
    ↑
    | 实现
ClientImpl
    ├── BrowserImpl (内部类)
    ├── ContextImpl (内部类)
    └── PageImpl (内部类)

BrowserDriver
    ├── 使用 → DriverClient
    └── 使用 → Request, Type

ChromiumDriverProxy
    ├── 继承 → ChromiumDriver (Selenium)
    ├── 使用 → BrowserDriver
    ├── 使用 → DevToolsProxy
    ├── 使用 → WindowProxy
    └── 使用 → NavigationProxy

WebElementImpl
    ├── 实现 → WebElement (Selenium)
    └── 使用 → BrowserDriver

Type (数据类容器)
    ├── BrowserType (枚举)
    ├── Browser (内部类)
    ├── Context (内部类)
    ├── Page (内部类)
    ├── Size (内部类)
    └── HealthCheckResult (内部类)

Request (数据类容器)
    ├── CreateBrowser (内部类)
    ├── CreateContext (内部类)
    ├── ViewPort (内部类)
    ├── JSResult (内部类)
    ├── Element (内部类)
    └── Action (内部类)

BrowserOptions (数据类)
    └── 使用 → Type.BrowserType, Request.ViewPort

HttpUtil (工具类)
    └── 被 ClientImpl 使用
```

---

## 12. 总结

SDK模块是BrowserGateway与CDP服务通信的核心组件，提供了完整的浏览器操作能力。通过分层设计和接口抽象，实现了良好的可扩展性和可维护性。同时，通过Selenium兼容层，支持使用标准的Selenium API进行浏览器自动化操作。

本设计文档详细描述了SDK模块的所有类、接口、方法和实现逻辑，可以根据此文档100%复现当前的SDK模块功能代码。
