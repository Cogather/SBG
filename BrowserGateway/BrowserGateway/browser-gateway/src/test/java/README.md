# 浏览器网关测试用例

## 测试模块覆盖

### 1. API 模块

| 测试类 | 覆盖功能 |
|--------|----------|
| **ChromeApiTest** | 删除用户数据（存在/不存在浏览器实例）、预开浏览器成功与异常 |
| **ExtensionManageApiTest** | 加载扩展成功/失败、获取插件信息成功/返回 null |

### 2. Adapter 模块

| 测试类 | 覆盖功能 |
|--------|----------|
| **AlarmAdapterTest** | AlarmType 枚举值、AlarmRequest 创建与批量创建 |

### 3. Config 模块

| 测试类 | 覆盖功能 |
|--------|----------|
| **WebsocketConfigTest** | mediaPort / muenPort / boss / worker / heartbeatTtl 的 set/get、默认值为 null、设置 null 值 |

### 4. SDK 模块

| 测试类 | 覆盖功能 |
|--------|----------|
| **BrowserOptionsTest** | endpoint / browserType / headless / url / extensionPaths / language 的 set/get、默认值 |
| **BrowserDriverTest** | close、saveUserdata、newPage、gotoUrl、executeScript（string/int/none/dict/未知类型）、executeElement、executeCdp、closeCurrentPage、getCurrentUrl、back、forward、findElementByTagName、getSize |
| **ChromiumDriverProxyTest** | getWindowHandle、get、quit、close、getCurrentUrl、executeCdpCommand、executeScript（history.go/history.length/普通脚本）、findElement（ByTagName/不支持定位器）、navigate、manage、saveUserdata、getProxyContextId |
| **ClientImplTest** | buildUrl、browser/context 返回及单例、BrowserImpl/ContextImpl/PageImpl 路径与请求 |
| **DevToolsProxyTest** | send（Target.createTarget/其他命令）、createSession |
| **NavigationProxyTest** | back、forward、to(String)、refresh 委托或空实现 |
| **RequestTest** | CreateBrowser.from、CreateContext.from、ViewPort、Action、JSResult 字段与默认值 |
| **TypeTest** | BrowserType.valueOf(KEYS/TOUCH/非法)、Context.getCurrentPage/getCurrentUrl、Size、HealthCheckResult |
| **WebElementImplTest** | parse、getTagName、getAttribute、getText、sendKeys（普通/Ctrl+A/Delete/空串/日期/文件）、getSize |

### 5. Service 模块

| 测试类 | 覆盖功能 |
|--------|----------|
| **ChromeSetImplTest** | getAllUser 初始为空、getHeartbeats 不存在的用户返回 0、updateHeartbeats 忽略不存在用户、get 不存在的用户返回 null |
| **CseImplTest** | getReportEndpoint 外网返回空串、多次调用一致性 |
| **ServiceModuleBasicTest** | RemoteImpl、FileStorageServiceImpl、ChromeSetImpl、HWCallbackImpl、PluginManageImpl 类存在性 |

### 6. Tcpserver 模块

| 测试类 | 覆盖功能 |
|--------|----------|
| **CertInfoTest** | getInstance、setCaContent/setDeviceContent（含 null）、isCertReady、Ca/Device InputStream |
| **ClientTest** | 创建、set/getStr、set/getInt、getTime、close、send（通道活跃/不活跃）、fromCtx 新建与复用 |
| **ClientSetTest** | setClient、替换旧 client、get 不存在、delByKey/delByClient、allClient、空集合 |
| **ControlClientSetTest** | setClient 无已有 client、替换并回调 fallback、标记旧 client 为 fallback |
| **DataSizeTrackerTest** | addDataSize、按用户区分、sendAllTrafficStatInfo 清 map/带 media 类型/跳过零/批量发送 |
| **FlowRateTrackerTest** | addDataSize、按会话/服务类型区分、移除条目、不存在 key、并发访问 |
| **MediaClientSetTest** | 继承 ClientSet、删除 client |

### 7. Util 模块

| 测试类 | 覆盖功能 |
|--------|----------|
| **DateTimeUtilTest** | millisToDate（正常/0/负数时间戳）、线程安全 |
| **TimeUtilTest** | getCurrentDate 格式、连续调用单调递增 |
| **UserIdUtilTest** | generateUserId（正常参数/null imei/null imsi/全 null/空串） |
| **ZstdUtilTest** | compress/decompress 往返、compressJson 源不存在/非 JSON、decompressJson 源不存在 |
| **TlvCodecTest** | marshal 字段数与长度、marshal/unmarshal 往返、非法类型/ null 目标、magic 字节、Ack 编码 |

### 8. Websocket 模块

| 测试类 | 覆盖功能 |
|--------|----------|
| **SessionSetTest** | 添加/获取/删除会话（正常、替换、不存在）、allSessions（含空集）、并发添加线程安全 |
| **MuenSessionManagerTest** | 继承能力：添加/替换/获取/删除会话、获取所有会话键 |
| **MediaSessionManagerTest** | 处理器添加/替换/获取/删除（存在与不存在）、删除会话同时删处理器与会话、继承的会话添加与 allSessions |
| **SocketKeyConstTest** | USER_ID_KEY 常量值、非 null、非空串 |
| **ServerEndpointExporterTest** | resolveAnnotationValue（null/非字符串）、EndpointClassPathScanner、SmartInitializingSingleton、各方法存在性、类注解 |
| **ServerEndpointExporterFunctionalTest** | buildConfig 参数、resolveAnnotationValue 签名、私有方法/内部类/继承、registerEndpoints 可见性、类字段、buildConfig 返回类型 |
| **FfmpegConstantsTest** | BUFFER_SIZE/CACHE_SIZE/CONTAIN_FORMAT/VIDEO/AUDIO 编码格式/AV 常量、常量修饰符、类 final |
| **CodecProcessorTest** | 接口存在、init/getStreamIndex/streamCodec/close 方法存在、方法数量 |
| **FfmpegCodecServiceTest** | 实现 CodecProcessor、构造函数、init/start/close、私有字段、Video/AudioCodecProcessor 方法与字段 |

---

## 运行测试

### 运行所有测试
```bash
mvn test
```

### 运行特定模块测试
```bash
# API 模块
mvn test -Dtest=com.huawei.browsergateway.api.*Test

# Adapter 模块
mvn test -Dtest=com.huawei.browsergateway.adapter.*Test

# Config 模块
mvn test -Dtest=com.huawei.browsergateway.config.*Test

# SDK 模块
mvn test -Dtest=com.huawei.browsergateway.sdk.*Test

# Service 模块
mvn test -Dtest=com.huawei.browsergateway.service.*Test

# Tcpserver 模块
mvn test -Dtest=com.huawei.browsergateway.tcpserver.*Test

# Util 模块
mvn test -Dtest=com.huawei.browsergateway.util.*Test

# Websocket 模块
mvn test -Dtest=com.huawei.browsergateway.websocket.*Test
```

## 测试覆盖率

生成测试覆盖率报告：
```bash
mvn clean test jacoco:report
```

报告位置：`target/site/jacoco/index.html`

## 测试原则

1. **最小功能集覆盖**：每个模块测试核心功能
2. **边界条件测试**：测试空值、null、边界值
3. **异常处理测试**：验证异常情况的处理
4. **Mock 使用**：对外部依赖使用 Mock 隔离

## 扩展测试

如需添加更多测试用例，请遵循以下规范：
- 测试类命名：`<ClassName>Test`（英文）
- 测试方法命名：`test<MethodName>_<Scenario>`（**必须使用英文**，禁止中文）
- `@DisplayName` 使用英文描述
- 遵循 Given-When-Then 模式
