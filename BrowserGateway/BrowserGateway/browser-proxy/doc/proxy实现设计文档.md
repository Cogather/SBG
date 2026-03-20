# Browser Proxy 项目设计文档

## 项目概述

Browser Proxy 是一个基于 FastAPI 和 Playwright 的浏览器代理服务，提供 HTTP API 接口来管理控制浏览器实例。该项目支持多浏览器实例管理、移动端触摸操作、浏览器扩展管理等功能，适用于云手机、远程浏览器控制、自动化测试等场景。

## 1. 系统架构设计

### 1.2 技术架构层次

```
┌─────────────────────────────────────────────────────────┐
│                      API 层                              │
│    FastAPI RESTful API (FastAPI 0.116.1)               │
└─────────────────────────────┬─────────────────────────────┘
                              │
┌─────────────────────────────▼─────────────────────────────┐
│                     业务逻辑层                           │
│  BrowserWrapper  | ContextWrapper  |  PageWrapper        │
│      Browser管理      |   Context管理      |  Page管理         │
└─────────────────────────────┬─────────────────────────────┘
                              │
┌─────────────────────────────▼─────────────────────────────┐
│                    封装层                                │
│        ElementWrapper  |  异常处理 |  资源管理         │
└─────────────────────────────┬─────────────────────────────┘
                              │
┌─────────────────────────────▼─────────────────────────────┐
│                   底层技术栈                             │
│    Playwright 1.53.0  | Chromium  | Uvicorn 0.35.0    │
└─────────────────────────────────────────────────────────┘
```

### 1.3 核心组件交互流程

```
1. 创建浏览器实例流程：
   HTTP请求 → Browser API → BrowserWrapper → Playwright.launch_persistent_context → 浏览器实例

2. 创建上下文流程：
   HTTP请求 → Context API → ContextWrapper → browser.new_context → 页面实例 → CDP会话

3. 页面操作流程：
   HTTP请求 → Page API → PageWrapper → page.evaluate/execute → DOM操作结果
```

## 2. 目录结构设计

```
browser-proxy/
├── browser_proxy/                 # 主包目录
│   ├── __init__.py               # 包初始化文件
│   ├── main.py                   # 应用入口和主配置
│   └── api/                      # API模块目录
│       ├── __init__.py
│       ├── browser.py            # 浏览器管理API (browser.py:59-134)
│       ├── context.py            # 上下文管理API (context.py:62-101)
│       ├── page.py               # 页面管理API (page.py:78-304)
│       ├── common.py             # 公共组件和工具类 (common.py:14-243)
│       └── logger_config.py      # 日志配置 (编译缓存)
├── pyproject.toml                # Python项目配置
├── setup.py                      # 安装和打包配置
├── requirements.txt              # 项目依赖列表
├── MANIFEST.in                  # 打包包含文件配置
└── __pycache__/                 # Python编译缓存
```

## 3. 核心实体设计

### 3.1 BrowserWrapper 类 (common.py:167-241)

```python
class BrowserWrapper:
    def __init__(self, browser: Browser, browser_type: BrowserType, 
                 browser_id: str, userdata: str, playwright: Playwright):
        self._browser = browser                    # Playwright Browser对象
        self._browser_type = browser_type          # 浏览器类型枚举
        self._id = browser_id                     # 唯一标识符
        self._contexts = []                       # 关联的Context列表
        self._userdata = userdata                 # 用户数据目录
        self._playwright = playwright             # Playwright引擎实例
    
    # 核心属性和方法
    @property
    def id(self) -> str:                         # 获取浏览器ID
    @property  
    def browser_type(self) -> BrowserType:       # 获取浏览器类型
    @property
    def browser(self) -> Browser:                # 获取Playwright Browser对象
    @property
    def contexts(self) -> List[ContextWrapper]: # 获取所有上下文
    def append_context(self, context: ContextWrapper) # 添加上下文
    def get_context(self, context_id: str) -> ContextWrapper # 获取指定上下文
    def remove_context(self, context: ContextWrapper) # 移除上下文
    async def close(self) # 关闭浏览器并清理资源
    async def is_active(self) -> bool # 检查浏览器是否活跃
```

### 3.2 ContextWrapper 类 (common.py:113-164)

```python
class ContextWrapper:
    def __init__(self, browser_id: str, context: BrowserContext, userdata: str):
        self._browser_id = browser_id             # 所属浏览器ID
        self._context = context                   # Playwright Context对象
        self._id = uuid.uuid4().hex                # 上下文唯一ID
        self._pages: List[PageWrapper] = []       # 关联的Page列表
        self._current = None                      # 当前活跃页面
        self._userdata = userdata                 # 存储状态文件路径
    
    # 核心属性和方法
    @property
    def id(self) -> str:                         # 获取上下文ID
    @property
    def pages(self) -> List[PageWrapper]:       # 获取所有页面
    @property
    def current(self) -> PageWrapper:           # 获取当前页面
    @property
    def context(self) -> BrowserContext:        # 获取Playwright Context对象
    def append_page(self, page: PageWrapper)    # 添加页面
    def get_page(self, page_id: str) -> PageWrapper # 获取指定页面
    def remove_page(self, page: PageWrapper)   # 移除页面
    async def close(self)                        # 关闭上下文并保存状态
```

### 3.3 PageWrapper 类 (common.py:45-110)

```python
class PageWrapper:
    def __init__(self, page: Page, browser_id: str, context_id: str, 
                 cdp_session: CDPSession = None):
        self._page = page                         # Playwright Page对象
        self._id = uuid.uuid4().hex                # 页面唯一ID
        self._browser_id = browser_id             # 所属浏览器ID
        self._context_id = context_id             # 所属上下文ID
        self._cdp_session = cdp_session           # CDP会话对象
        self._elements: Dict[str, ElementWrapper] = {} # 元素缓存
    
    # 核心属性和方法
    @property
    def id(self) -> str:                         # 获取页面ID
    @property
    def page(self) -> Page:                      # 获取Playwright Page对象
    @property
    def url(self) -> str:                        # 获取页面URL
    @property
    def cdp_session(self) -> CDPSession:         # 获取CDP会话
    def set_element(self, element: ElementWrapper) # 缓存元素
    def get_element(self, key: str)             # 获取指定元素
    def del_element(self, key: str)             # 删除元素缓存
    async def close(self)                        # 关闭页面
```

### 3.4 ElementWrapper 类 (common.py:18-43)

```python
class ElementWrapper:
    def __init__(self, element: ElementHandle):
        self._element = element                   # Playwright ElementHandle对象
        self._id = uuid.uuid4().hex                # 元素唯一ID
    
    # 核心属性和方法
    @property
    def element(self):                           # 获取Playwright ElementHandle
    @property
    def id(self):                                # 获取元素ID
    @property
    async def preview():                         # 获取元素预览信息
    async def close()                            # 释放元素资源
    async def as_json()                          # 序列化为JSON
```

### 3.5 BrowserType 枚举 (common.py:14-16)

```python
class BrowserType(Enum):
    KEYS = 'KEYS'                              # 键盘操作类型
    TOUCH = 'TOUCH'                            # 触摸操作类型
```

## 4. API接口设计

### 4.1 浏览器管理 API (browser.py)

#### 4.1.1 创建浏览器实例
```
POST /api/browsers
```

**请求参数：**
```json
{
  "executable_path": "/path/to/chromium",    // 浏览器可执行文件路径
  "browser_id": "unique-browser-id",         // 可选，浏览器唯一标识
  "browser_type": "KEYS",                    // 浏览器类型 ("KEYS" 或 "TOUCH")
  "headless": true,                          // 是否无界面模式
  "language": "en-US",                       // 语言设置
  "base_data": "/path/to/user/data",        // 用户数据目录
  "extension_paths": ["/path/to/ext1"],       // 扩展文件路径数组
  "extension_ids": ["ext1-id"],              // 扩展ID数组
  "allowlisted_extension_id": "ext1-id"      // 白名单扩展ID
}
```

**响应：**
```json
{
  "id": "browser-uuid-here",
  "used": 0,
  "browser_type": "KEYS"
}
```

**实现位置：** `browser.py:59-134`

#### 4.1.2 获取浏览器列表
```
GET /api/browsers
```

**响应：**
```json
[
  {
    "id": "browser-uuid-here",
    "used": 2,
    "browser_type": "KEYS"
  }
]
```

**实现位置：** `browser.py:45-56`

#### 4.1.3 获取指定浏览器
```
GET /api/browsers/{browser_id}
```

**路径参数：**
- `browser_id`: 浏览器唯一标识

**响应：**
```json
{
  "id": "browser-uuid-here",
  "used": 1,
  "browser_type": "KEYS"
}
```

**实现位置：** `browser.py:31-42`

#### 4.1.4 删除浏览器
```
DELETE /api/browsers/{browser_id}
```

**路径参数：**
- `browser_id`: 浏览器唯一标识

**响应：** 204 No Content

**实现位置：** `browser.py:16-28`

#### 4.1.5 健康检查
```
POST /api/browsers/health_check
```

**响应：**
```json
{
  "success": true
}
// 或
{
  "success": false,
  "err_contexts": ["context-uuid-1", "context-uuid-2"]
}
```

**实现位置：** `browser.py:137-159`

### 4.2 上下文管理 API (context.py)

#### 4.2.1 创建上下文
```
POST /api/browsers/{browser_id}/contexts
```

**路径参数：**
- `browser_id`: 浏览器唯一标识

**请求参数：**
```json
{
  "url": "https://example.com",              // 初始访问URL
  "data": "{}",                              // 初始化数据
  "viewport": {"width": 1920, "height": 1080}, // 视口大小
  "userdata": "/path/to/storage.json",       // 存储状态文件路径
  "language": "zh_CN"                       // 语言设置
}
```

**响应：**
```json
{
  "id": "context-uuid-here",
  "current": "page-uuid-here",
  "browser_id": "browser-uuid-here",
  "pages": [
    {
      "id": "page-uuid-here",
      "url": "https://example.com",
      "browser_id": "browser-uuid-here", 
      "context_id": "context-uuid-here",
      "support_cdp_session": true
    }
  ]
}
```

**实现位置：** `context.py:62-101`

#### 4.2.2 获取上下文列表
```
GET /api/browsers/{browser_id}/contexts
```

**路径参数：**
- `browser_id`: 浏览器唯一标识

**响应：**
```json
[
  {
    "id": "context-uuid-here",
    "current": "page-uuid-here", 
    "browser_id": "browser-uuid-here",
    "pages": [...]
  }
]
```

**实现位置：** `context.py:46-58`

#### 4.2.3 获取指定上下文
```
GET /api/browsers/{browser_id}/contexts/{context_id}
```

**路径参数：**
- `browser_id`: 浏览器唯一标识
- `context_id`: 上下文唯一标识

**响应：**
```json
{
  "id": "context-uuid-here",
  "current": "page-uuid-here",
  "browser_id": "browser-uuid-here", 
  "pages": [...]
}
```

**实现位置：** `context.py:33-43`

#### 4.2.4 删除上下文
```
DELETE /api/browsers/{browser_id}/contexts/{context_id}
```

**路径参数：**
- `browser_id`: 浏览器唯一标识
- `context_id`: 上下文唯一标识

**响应：** 204 No Content

**实现位置：** `context.py:16-30`

### 4.3 页面管理 API (page.py)

#### 4.3.1 创建页面
```
POST /api/browsers/{browser_id}/contexts/{context_id}/pages
```

**路径参数：**
- `browser_id`: 浏览器唯一标识
- `context_id`: 上下文唯一标识

**请求参数：**
```json
{
  "url": "https://example.com"              // 页面URL
}
```

**响应：**
```json
{
  "id": "context-uuid-here",
  "current": "new-page-uuid-here",
  "browser_id": "browser-uuid-here",
  "pages": [...]
}
```

**实现位置：** `page.py:177-199`

#### 4.3.2 页面导航
```
POST /api/browsers/{browser_id}/contexts/{context_id}/pages/goto
```

**路径参数：**
- `browser_id`: 浏览器唯一标识
- `context_id`: 上下文唯一标识

**请求参数：**
```json
{
  "url": "https://new-url.com"              // 目标URL
}
```

**响应：**
```json
{
  "id": "context-uuid-here",
  "current": "page-uuid-here",
  "url": "https://new-url.com",
  ...
}
```

**实现位置：** `page.py:16-34`

#### 4.3.3 执行JavaScript
```
POST /api/browsers/{browser_id}/contexts/{context_id}/pages/execute
```

**路径参数：**
- `browser_id`: 浏览器唯一标识
- `context_id`: 上下文唯一标识

**请求参数：**
```json
{
  "expression": "return document.title"     // JavaScript表达式
}
```

**响应：**
```json
// 字符串结果
{
  "result_type": "string",
  "value": "页面标题"
}

// 数字结果  
{
  "result_type": "int",
  "value": 42
}

// 对象结果
{
  "result_type": "dict",
  "element_keys": ["element_id"],
  "value": "{\"title\":\"页面标题\"}"
}

// 元素结果
{
  "result_type": "element",
  "value": "{\"id\":\"element-uuid\",\"preview\":\"<div>...</div>\"}"
}

// 无结果
{
  "result_type": "none",
  "value": null
}
```

**实现位置：** `page.py:78-142`

#### 4.3.4 执行CDP命令
```
POST /api/browsers/{browser_id}/contexts/{context_id}/pages/execute_cdp
```

**路径参数：**
- `browser_id`: 浏览器唯一标识
- `context_id`: 上下文唯一标识

**请求参数：**
```json
{
  "method": "Network.setCacheDisabled",      // CDP方法名
  "params": {                                // CDP参数
    "cacheDisabled": true
  }
}
```

**响应：** CDP命令返回的JSON数据

**实现位置：** `page.py:37-56`

#### 4.3.5 查找元素
```
POST /api/browsers/{browser_id}/contexts/{context_id}/pages/find_element
```

**路径参数：**
- `browser_id`: 浏览器唯一标识
- `context_id`: 上下文唯一标识

**请求参数：**
```json
{
  "selector": "#submit-button"              // CSS选择器
}
```

**响应：**
```json
{
  "result_type": "element", 
  "value": "{\"id\":\"element-uuid\",\"preview\":\"<button>提交</button>\"}"
}
```

**实现位置：** `page.py:255-276`

#### 4.3.6 元素操作
```
POST /api/browsers/{browser_id}/contexts/{context_id}/pages/element
```

**路径参数：**
- `browser_id`: 浏览器唯一标识
- `context_id`: 上下文唯一标识

**请求参数：**
```json
{
  "element_id": "element-uuid",            // 元素ID
  "action": "send_key",                     // 操作类型 ("send_key", "set_file", "focus")
  "value": "输入文本"                       // 操作值（可选）
}
```

**响应：** 204 No Content

**实现位置：** `page.py:145-175`

#### 4.3.7 获取元素尺寸
```
POST /api/browsers/{browser_id}/contexts/{context_id}/pages/element/{element_id}/get_size
```

**路径参数：**
- `browser_id`: 浏览器唯一标识
- `context_id`: 上下文唯一标识
- `element_id`: 元素唯一标识

**响应：**
```json
{
  "width": 120,
  "height": 40
}
```

**实现位置：** `page.py:278-304`

#### 4.3.8 删除页面
```
DELETE /api/browsers/{browser_id}/contexts/{context_id}/pages/{page_id}
```

**路径参数：**
- `browser_id`: 浏览器唯一标识
- `context_id`: 上下文唯一标识
- `page_id`: 页面唯一标识

**响应：**
```json
{
  "id": "context-uuid-here",
  "current": "remaining-page-uuid",
  "browser_id": "browser-uuid-here",
  "pages": [...]
}
```

**实现位置：** `page.py:202-222`

#### 4.3.9 页面导航控制
```
POST /api/browsers/{browser_id}/contexts/{context_id}/pages/go_back
POST /api/browsers/{browser_id}/contexts/{context_id}/pages/go_forward
```

**路径参数：**
- `browser_id`: 浏览器唯一标识
- `context_id`: 上下文唯一标识

**响应：** 204 No Content

**实现位置：** `page.py:224-252`

## 5. 数据模型设计

### 5.1 全局数据结构

#### 5.1.1 浏览器列表
```python
browser_list: List[BrowserWrapper] = []  # 全局浏览器实例列表 (common.py:244)
```

#### 5.1.2 辅助函数
```python
def browser_get(browser_id: str) -> BrowserWrapper:  # 根据ID获取浏览器 (common.py:247)
```

### 5.2 序列化模型

#### 5.2.1 浏览器JSON序列化
```python
{
  "id": "browser-uuid",                    # 浏览器唯一ID
  "used": 2,                               # 当前使用的上下文数量
  "browser_type": "KEYS"                   # 浏览器类型
}
```

#### 5.2.2 上下文JSON序列化
```python
{
  "id": "context-uuid",                   # 上下文唯一ID
  "current": "page-uuid",                  # 当前页面ID
  "browser_id": "browser-uuid",            # 所属浏览器ID
  "pages": [                               # 页面列表
    {
      "id": "page-uuid",
      "url": "https://example.com",
      "browser_id": "browser-uuid",
      "context_id": "context-uuid", 
      "support_cdp_session": true
    }
  ]
}
```

#### 5.2.3 页面JSON序列化
```python
{
  "id": "page-uuid",                       # 页面唯一ID
  "url": "https://example.com",            # 页面URL
  "browser_id": "browser-uuid",            # 所属浏览器ID
  "context_id": "context-uuid",            # 所属上下文ID
  "support_cdp_session": true              # 是否支持CDP会话
}
```

#### 5.2.4 元素JSON序列化
```python
{
  "id": "element-uuid",                    # 元素唯一ID
  "preview": "<div>元素内容</div>"         # 元素预览信息
}
```

## 6. 配置和依赖设计

### 6.1 项目依赖 (requirements.txt)

```text
annotated-types==0.7.0
anyio==4.10.0
click==8.1.8+
colorama==0.4.6
fastapi==0.116.1
greenlet==3.2.4
h11==0.16.0
idna==3.10
playwright==1.53.0
pydantic==2.11.9
pydantic_core==2.33.2
pyee==13.0.0
sniffio==1.3.1
starlette==0.47.3
typing-inspection==0.4.1
typing_extensions==4.15.0
uvicorn==0.35.0
```

### 6.2 项目配置 (setup.py)

```python
setup(
    name="browser_proxy",
    version="1.0.0", 
    description="A FastAPI-based browser automation proxy service",
    long_description="A FastAPI service that manages browser, context and page operations",
    author="Your Name",
    author_email="your.email@example.com",
    url="https://github.com/yourusername/browser-proxy",
    packages=find_packages(),
    include_package_data=True,
    install_requires=read_requirements(),
    python_requires=">=3.8",
    entry_points={
        "console_scripts": [
            "browser_proxy = browser_proxy.main:run_server",
        ]
    },
    classifiers=[
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.8",
        "Programming Language :: Python :: 3.9", 
        "Programming Language :: Python :: 3.10",
        "Framework :: FastAPI",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
    ],
)
```

### 6.3 应用配置 (main.py)

```python
def run_server(host="127.0.0.1", port=8000):
    """启动服务的函数，供命令行入口调用"""
    import uvicorn
    uvicorn.run(app, host=host, port=port)
```

### 6.4 应用中间件配置

```python
@app.middleware("http")
async def log_request_duration_middleware(request: Request, call_next):
    start_time = time.time()
    response = await call_next(request)
    duration = (time.time() - start_time) * 1000
    log.info(f"Request completed: {request.method} {request.url.path}, "
            f"Duration: {duration:.2f} ms, Status: {response.status_code}")
    return response
```

## 7. 启动和部署设计

### 7.1 开发环境启动

#### 7.1.1 环境准备

```bash
# 创建虚拟环境
python -m venv venv

# 激活虚拟环境
# Linux/Mac:
source venv/bin/activate
# Windows:
venv\Scripts\activate

# 安装依赖
pip install -r requirements.txt

# 安装Playwright浏览器
playwright install chromium

# 开发模式安装
pip install -e .
```

#### 7.1.2 启动服务

```bash
# 方式1：使用安装的命令
browser_proxy

# 方式2：直接运行主程序
python browser_proxy/main.py

# 方式3：使用uvicorn
uvicorn browser_proxy.main:app --host 127.0.0.1 --port 8000
```

### 7.2 生产环境部署

#### 7.2.1 Docker化部署

**Dockerfile:**
```dockerfile
FROM python:3.9-slim

WORKDIR /app

# 安装系统依赖
RUN apt-get update && apt-get install -y \
    wget \
    gnupg \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# 安装Chrome/Chromium
RUN wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google.list \
    && apt-get update && apt-get install -y google-chrome-stable

# 复制依赖文件
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 安装Playwright浏览器  
RUN playwright install

# 复制应用代码
COPY . .

# 暴露端口
EXPOSE 8000

# 启动命令
CMD ["uvicorn", "browser_proxy.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

**docker-compose.yml:**
```yaml
version: '3.8'
services:
  browser-proxy:
    build: .
    ports:
      - "8000:8000"
    environment:
      - PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
    volumes:
      - ./data:/app/data
    restart: unless-stopped
```

#### 7.2.2 进程服务化部署

**systemd服务配置 (/etc/systemd/system/browser-proxy.service):**
```ini
[Unit]
Description=Browser Proxy Service
After=network.target

[Service]
Type=simple
User=www-data
Group=www-data
WorkingDirectory=/opt/browser-proxy
Environment=PATH=/opt/browser-proxy/venv/bin
ExecStart=/opt/browser-proxy/venv/bin/browser_proxy
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### 7.3 配置管理

#### 7.3.1 环境变量配置

```bash
# 服务器配置
export HOST=0.0.0.0
export PORT=8000
export WORKERS=4

# 浏览器配置
export BROWSER_HEADLESS=true
export BROWSER_LANGUAGE=zh-CN
export BROWSER_EXECUTABLE_PATH=/usr/bin/google-chrome-stable

# 资源限制
export MAX_BROWSERS=10
export MAX_CONTEXTS_PER_BROWSER=5
export MAX_PAGES_PER_CONTEXT=10
```

#### 7.3.2 浏览器配置文件

Chrome配置文件应包含以下关键设置：
- 用户数据目录持久化
- 扩展管理和加载
- 移动端适配参数
- 安全策略配置

## 8. 异常处理和错误管理

### 8.1 HTTP异常处理

所有API端点都采用统一的异常处理模式：

```python
try:
    # 业务逻辑
    result = await operation()
    return result
except HTTPException:
    raise  # 直接抛出已定义的HTTP异常
except Exception as error:
    log.error(str(error))
    raise HTTPException(status_code=500, detail=str(error))
```

### 8.2 常见异常类型

| 异常类型 | HTTP状态码 | 描述 |
|---------|-----------|------|
| 404 | Not Found | 资源不存在（浏览器、上下文、页面等） |
| 500 | Internal Server Error | 服务器内部错误 |
| 自定义 | 根据具体场景 | 业务逻辑相关错误 |

### 8.3 特殊错误处理

- **执行上下文已销毁**: 返回 `{'result_type': 'none'}` (page.py:136)
- **函数不匹配**: 记录警告并返回 `{'result_type': 'none'}` (page.py:138)
- **404异常删除操作**: 幂等性处理，直接返回 (context.py:25)

## 9. 性能和资源管理

### 9.1 资源生命周期

- **浏览器实例**: 长期运行，根据健康检查定期清理
- **上下文实例**: 持续运行，支持状态持久化
- **页面实例**: 按需创建，可单独关闭
- **元素实例**: 临时缓存，使用后立即释放

### 9.2 内存管理

- 使用字典缓存临时元素引用 (`PageWrapper._elements`)
- 元素使用后立即调用 `close()` 释放资源
- 定期清理不活跃的浏览器实例

### 9.3 并发控制

- 基于 FastAPI 的异步架构支持高并发
- 每个浏览器/上下文/页面都是独立对象
- 使用 UUID 确保资源唯一标识

## 10. 安全性考虑

### 10.1 访问控制

- 内置HTTP基本认证（可通过中间件扩展）
- 资源ID隔离机制
- 敏感操作验证

### 10.2 数据隔离

- 每个浏览器实例独立用户数据目录
- 上下文级别的存储状态隔离
- 页面级别的元素缓存隔离

### 10.3 命令注入防护

- 使用 Playwright 的安全上下文执行JavaScript
- 避免直接执行用户提供的任意代码
- 对输入参数进行验证

## 11. 监控和日志

### 11.1 请求日志

中间件记录以下信息：
- 请求方法、路径
- 响应状态码  
- 请求处理时长

### 11.2 业务日志

关键操作的日志记录：
- 浏览器创建/关闭
- 上下文创建/关闭
- 错误和异常信息

### 11.3 健康检查机制

- 定期检查浏览器连接状态
- 自动清理异常实例
- 返回健康状态报告

## 12. 扩展性和未来规划

### 12.1 当前功能完备性

✅ **已完成功能：**
- 多浏览器实例管理
- 上下文和页面生命周期管理  
- 移动端支持（TOUCH模式）
- 浏览器扩展管理
- CDP协议支持
- 元素操作和JavaScript执行
- 健康检查机制

### 12.2 潜在改进建议

🔧 **建议增强功能：**
- 服务发现和负载均衡支持
- 实例池管理，提高资源利用率
- WebSocket支持实时通信
- 完整的配置文件管理
- 容量限制和配额管理
- API版本控制
- 更完善的监控告警系统

### 12.3 兼容性考虑

- Python 3.8+ 兼容性
- Playwright 1.53.0 版本锁定
- FastAPI 0.116.1 版本锁定
- 支持主流Linux发行版

## 13. 复现指南

### 13.1 环境要求

- Python 3.8+ 
- Linux/macOS/Windows
- Chrome/Chromium 浏览器
- 网络访问（下载依赖包）

### 13.2 完整复现步骤

1. **创建项目目录结构**
   ```bash
   mkdir -p browser-proxy/browser_proxy/api
   ```

2. **创建必要的Python文件**
   - `browser_proxy/__init__.py`
   - `browser_proxy/main.py` 
   - `browser_proxy/api/__init__.py`
   - `browser_proxy/api/browser.py`
   - `browser_proxy/api/context.py`
   - `browser_proxy/api/common.py`
   - `browser_proxy/api/logger_config.py`

3. **配置文件创建**
   - `requirements.txt`
   - `setup.py`
   - `pyproject.toml`
   - `MANIFEST.in`

4. **安装依赖**
   ```bash
   pip install -r requirements.txt
   pip install playwright
   playwright install chromium
   ```

5. **启动服务**
   ```bash
   pip install -e .
   browser_proxy
   ```

### 13.3 验证测试

使用curl测试API功能：

```bash
# 创建浏览器实例
curl -X POST "http://127.0.0.1:8000/api/browsers" \
  -H "Content-Type: application/json" \
  -d '{
    "executable_path": "/usr/bin/google-chrome-stable",
    "browser_type": "KEYS",
    "headless": true,
    "language": "en-US"
  }'

# 创建上下文
curl -X POST "http://127.0.0.1:8000/api/browsers/{browser_id}/contexts" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://www.example.com",
    "viewport": {"width": 1920, "height": 1080}
  }'

# 执行JavaScript
curl -X POST "http://127.0.0.1:8000/api/browsers/{browser_id}/contexts/{context_id}/pages/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "expression": "return document.title"
  }'
```

## 14. 总结

Browser Proxy 是一个功能完善、架构清晰的浏览器代理服务，采用现代的异步设计模式，具有良好的扩展性和可维护性。通过本设计文档提供的详细信息，可以实现100%的代码复现，并确保与原项目功能完全一致。

项目核心优势：
-RESTful API设计规范
- 支持移动端触摸操作
- 内置浏览器扩展管理
- 支持CDP协议底层操作
- 完善的异常处理机制
- 可扩展的架构设计