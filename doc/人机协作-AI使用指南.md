# 人机协作：如何用好 AI，让代码生成越来越准


## 1. 对话修改模式

### 策略

- **先约束、后发散**：一条消息里写清楚改哪里（模块、目录、技术栈）、要达成什么、刻意不做什么。多栈项目（比如 Java + Python + 前端）最好点名栈和入口，少改错层。
- **小步迭代**：每次只做一个能单独验证的改动（例如只动配置和一个 Bean，再看启动日志或单测），避免大重构加联调搅在一起，出了问题不好归因。
- **用事实锚定**：能引用团队已有约定就引用——分层目录、接口前缀、统一返回体、README 里的端口和路径——模型少猜一层。
- **验收标准前置**：怎样算完成要说在前面（编译过、某接口 200、某条用例绿），粒度尽量和任务勾选能对上。
- **发现歧义就升级**：同一需求在对话里跑出多种实现路径时，别靠反复试；改成范式模板（Skill）或大颗粒 Spec，把套路固定下来。

### 正反例描述对比

| 维度 | 正例（推荐写法） | 反例（易跑偏、易幻觉） |
|------|-----------------|------------------------|
| 范围 | 仅改 `application-local.yml` 里的 GIDS base URL，指向本机 Mock（`http://localhost:9090`）；不动 `application.yml`；验收：`start-local.bat` 启动后控制台出现 `BrowserGateway started on port 8080`。 | 本地 GIDS 和网关弄顺就行，能跑起来就好。 |
| 模块与技术栈 | 这是 `browser-gateway`（Java Spring Boot）里的改动，入口在 `BrowserGateway/BrowserGateway/browser-gateway/`，不涉及 `browser-proxy`（Python）和 `mobile`。 | 帮我改一下网关那边的逻辑。 |
| 依赖关系 | 启动顺序：`gids_mock_server.py` → `browser-gateway` → `browser-proxy`；Test Client 的 WebSocket 连 `ws://localhost:40001`；失败时先看 GIDS Mock 日志，再看网关鉴权日志，最后看 TCP 通道建立。 | 把几个服务都起来，做一次端到端联调。 |
| 协议与字段 | 登录 TLV 帧里 `ID.TYPE=1`（LOGIN），必须包含 `IMEI`、`IMSI`、`TOKEN` 等 22 个字段，字段常量在 `common/ID.java`；编码用 `TlvEncoder`，解码用 `TlvDecoder`，不要手写字节偏移。 | 按 TLV 协议格式发登录包。 |
| 接口与数据 | 新增路由路径跟 `browser-proxy` 现有 `/api/browsers`、`/api/contexts` 风格一致；Response 字段 `browserId` 和 `BrowserGateway` 下发的 JSON 键名对齐；参考 `browser/router.py` 的写法。 | 按 REST 最佳实践加接口就行。 |
| 完成定义 | `tasks.md` 里「3.2 GIDS Mock 登录路由」能勾选；或运行 `test_login_workflow.py` 全绿；或控制台打出 `deviceLoginAuth success`。 | 差不多了，我看没问题。 |
| 改动边界 | 只改 `FileStorageServiceImpl.java` 里的上传路径逻辑；不动 `ApplicationConfig.java` 和 `pom.xml`；改完跑一次上传接口手动验证，其他接口不回归。 | 把文件存储那块改一下。 |
| 小步迭代 | 第一步只加 `LocalCseConfig`，让 `mvn spring-boot:run` 能正常启动；第二步再接真实 CSE 注册逻辑；两步分开提交，出了问题好 `git bisect`。 | 把 CSE 本地配置和注册逻辑一起加好，直接联调。 |
| 调试定位 | 登录失败时：先看 GIDS Mock 是否收到请求（Mock 控制台日志）→ 再看 `BrowserContext` 的 `doGridLoginAuth` 返回值 → 最后看 TCP 通道建立日志；不要一上来就改代码。 | 登录一直失败，帮我看看哪里有问题。 |
| 禁止副作用 | 新增 `TestConfig.java` 只在 `test` profile 下生效（加 `@Profile("test")`）；不修改主配置；不引入新的 Maven 依赖。 | 加个测试配置方便调试。 |
| 二进制协议 | `TlvDecoder` 里 `VIDEO_DATA` 和 `AUDIO_DATA` 字段值存为 `byte[]`，不要做任何 String 转换；WS 下发帧头 `[0x01, frameType]`（视频）或 `[0x02]`（音频）拼在 `byte[]` 前面再写到 Channel。 | 音视频数据用 Base64 编码之后当字符串发过去就行，前端再解码。 |
| 不做无关优化 | 只修复 `ControlChannelHandler` 里类型判断用 `data.get(ID.TYPE)` 的逻辑错误；不重构其他 Handler，不调整日志级别，不加新字段。 | 顺便把几个 Handler 的日志和异常处理也规范一下。 |
| 并发与状态 | `BrowserContext` 里 TCP 连接建立后再启动心跳线程（`startHeartbeat()` 在 `connectControlChannel()` 成功回调之后调用）；心跳周期 15 秒，用 `ScheduledExecutorService` 管理，`@PreDestroy` 时 `shutdown()`。 | 直接在 `BrowserContext` 构造函数里起心跳，反正连上之后就要发。 |
| 环境差异 | `start-local.bat` 里通过 `-Dspring.profiles.active=local` 激活本地 profile；本地 profile 只覆盖 GIDS URL 和 CSE 注册开关，其余继承 `application.yml`；说明文档写清哪些环境变量需要手动设置。 | 直接改 `application.yml` 里的地址方便本地跑。 |
| Mock 与真实服务契约 | `gids_mock_server.py` 的 `POST /gids/gridLoginAuth` 返回体里必须包含 `gridLoginToken` 字段，和 `BrowserContext.doGridLoginAuth()` 里 `resp.get("gridLoginToken")` 的键名完全一致；字段缺失或拼写不同会导致后续步骤拿到 null。 | Mock 先随便返回 `{"code":0}` 把流程跑通，字段后面再对齐。 |

---

## 2. 范式需求 Skill 模式

### 策略

某类需求如果重复出现、结构又比较固定，可以把触发条件、必须遵守的约束、验收方式写成一份可复用说明。在 Cursor、Claude Code 里，这类东西常常落在 Skill、Project Rules、团队模板上。按模板填空，比每次从零描述要稳。

常见载体：

- Cursor Project Rules、Claude Code 的 claude.md：叫法不同，作用类似，都是给助手立规矩，相当于项目里的总章程。
- Skill：某几个重复场景里抽出来的技能说明，需要时引用。

### Rule

#### 哪些内容适合放在 Rule / claude.md

（包括但不局限）

1. 长期稳定、不常变的总原则  
   多模块各管什么（例如 browser-gateway、browser-proxy、mobile 的边界）。  
   技术栈默认值：JDK、Python 是否用 venv、包管理器、前端构建工具。  
   全局禁止项：例如禁止提交密钥、禁止某类依赖。

2. 能当检查表一条条执行的约定  
   命名、分层（Controller / Service / Adapter）、包路径习惯。  
   日志、异常、统一返回体、错误码习惯。  
   配置约定：profile、application-local、端口、环境变量名。

规则越像检查表——目录放哪、怎么命名、错误怎么处理、哪些事不许做——输出越容易对齐。

**claude init**：在仓库根目录跑 Claude Code 自带的初始化命令，可以生成一版 claude.md 草稿，后面随项目演进再改。

| 规则类型 | 典型内容 | 常见 globs |
|----------|----------|------------|
| 主语言编码规范 | 命名、注释、复杂度、异常、日志、并发、安全、Clean Code 等，可配正反例 | 如 `**/*.java`、`**/*.ts` |
| 测试命名与展示 | 测试类/方法命名、`@DisplayName` 语言约定、断言消息是否允许中文等 | 如 `**/*Test*.java`、`**/*.spec.ts` |

### 范式需求场景应用

范式 Skill 适合重复出现、结构固定、实现套路清晰的需求。跨模块、长链路、强业务规则更适合配合第 3 节的大颗粒 Spec：“先把设计钉死”，再按任务实现。

#### 场景 1：Mock 外部依赖（HTTP 替身）

```
// Mock 外部依赖 — 规则摘要
// 1. 在现有 Mock 服务上增量，保持同一进程、同一框架，别随便再起一个服务。
// 2. 契约优先：方法、路径、Query/Body、状态码、JSON 字段和真实调用方一致。
// 3. 配置：被测服务把依赖的 base URL、endpoint 指到本机 Mock。
// 4. 风格：跟同仓库已有路由、日志、错误处理一致。
// 5. Mock 只服务联调与测试，不塞真实业务逻辑。
```

```python
# 骨架示意（FastAPI / Flask 等按项目选型）
# @app.get("/api/v1/example/{id}")
# async def example(id: str):
#     return {"field": "与客户端反序列化一致"}
```

**怎么用**：跟助手说清楚按 Mock 外部依赖那套约定来，在现有 Mock 文件里加 GET …，返回字段 a、b，并贴上某某服务里发 HTTP 的代码和配置文件里的 base URL。

---

#### 场景 2：在已有 Web 服务上增加 REST / 类 CRUD

```
// REST 增量 — 规则摘要
// 1. 路由定义和应用入口挂载分开：子模块只写路径片段，全局 prefix 在一处统一。
// 2. 异常和日志走项目统一方式（例如 HTTP 异常类加统一 logger）。
// 3. 有 API 规格或 OpenAPI 的，变更要同步更新。
// 4. 新接口风格对齐现有资源类接口（异步还是同步、分页约定等）。
```

```python
# 示例：FastAPI 风格
from fastapi import APIRouter, HTTPException
router = APIRouter()

@router.get("/items")
async def list_items():
    try:
        return []
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# 应用入口：app.include_router(router, prefix="/api")
```

**怎么用**：说明按 REST 增量约定，新增资源 orders，要 GET 列表和 POST 创建，字段写清楚，数据先放内存列表；入口注册路由，文档一并更新。

---

#### 场景 3：外部系统适配（双实现 + 工厂装配）

适合官方或内网 SDK 一套、外网或简化实现另一套，需要切换的项目。

```
// Adapter — 规则摘要
// 1. 业务只依赖接口，不绑具体实现类。
// 2. 两套实现（VendorA / VendorB 或 Sdk / Stub）成对加，工厂一起改。
// 3. 用配置或环境选工厂分支；别绕过工厂只注册一个实现。
// 4. 日志、超时、重试跟现有适配器对齐。
```

```java
// 示意（Spring）
public interface PaymentAdapter { Result pay(String id); }

// @Bean
// public PaymentAdapter paymentAdapter(AdapterFactory f) {
//     return f.createPaymentAdapter();
// }
```

**怎么用**：说明按 Adapter 范式新增 XxxAdapter，方法签名写清；A 调官方 SDK，B 调内部 HTTP；补工厂和 Bean，业务侧只注入接口。


## 3. 大颗粒 Spec 规范模式（SDD）

### 策略

业务复杂、链路长，或者缺少可参考代码时，光靠对话容易飘。可以先规格后编码：写出能读的动机、范围、做法、验收，再让人或 AI 按任务清单推进。

常见工件名称（团队可自定，逻辑差不多就行）：

- 动机与范围：为什么做、做哪些、不做哪些
- 设计说明：决策、备选方案、非目标
- 按能力拆的规格：可测试的陈述
- 任务清单：步骤、勾选、怎么验证

仓库根目录下的 `openspec/` 用变更（change）把一次交付要写的材料收拢到一个目录里；没有装 OpenSpec CLI 时，也可以自己建同样结构的 Markdown，协作方式一样。

### 何时用大颗粒 Spec（自查表）

| 适用场景 | 建议 |
|----------|------|
| 重构核心模块 | 设计里写清决策和回滚；任务分阶段，每阶段能单独验证。 |
| 新增复杂业务域 | 按能力拆多份规格，别堆成单文件巨篇。 |
| 无参考实现的新功能 | 先固定接口和数据流，再写 Mock 和实现；环境差异写进设计。 |
| 多团队或多仓库 | 在影响面里列依赖方、配置、端口、环境。 |
| 要可追溯验收 | 每条能力对应任务清单里能执行的检查项。 |

实操上：对话里改来改去仍不对时，把共识写回规格或设计，再让 AI 只按清单改实现，相当于换轨道而不是碰运气。

### 场景一：模块设计说明书 + 接口契约文档 驱动代码生成

BrowserGateway 里，每个独立模块，包含 **模块设计文档**：每个模块通常成对出现 **模块设计说明书**（职责、类、流程、配置键）和 **模块接口契约**（对外/依赖哪些 Java 接口、方法语义、调用方、异常约定）。先写或改这两类文档，再按契约去实现或改 `src/main/java`，是典型的规格驱动写法。

**文档放哪、长什么样**

- 模板：`BrowserGateway/BrowserGateway/browser-gateway/doc/模块设计文档/模板/` 下的 `模块设计说明书模板.md`、`模块接口契约文档模板.md`，新模块可以按模板起稿。
- 已填好的模块：同目录下按分层分子目录，例如 `03 核心业务层`、`04 运维层`，每个模块一个文件夹，里面常见两个文件：`xxx模块设计说明书.md`、`xxx模块接口契约.md`。

**以定时任务模块为例（好对照代码）**

| 文档 | 路径（均在 browser-gateway 工程下） | 作用 |
|------|--------------------------------------|------|
| 模块设计说明书 | `doc/模块设计文档/04 运维层/003 定时任务/定时任务模块设计说明书.md` | 写模块定位、有哪些任务（类名、周期、职责）、组件依赖树、各任务执行步骤与配置项（如 `browsergw.scheduled.*`）。 |
| 接口契约 | `doc/模块设计文档/04 运维层/003 定时任务/定时任务接口契约.md` | 写本模块依赖的 `IChromeSet`、`IRemote`、各 Adapter、`DriverClient`、`ClientSet` 等接口方法、谁调用、频率、是否抛异常。 |

实现代码与说明书里的类名一一对应，包在：

`browser-gateway/src/main/java/com/huawei/browsergateway/scheduled/`

例如设计说明书表格里的 `BrowserCheckTask`、`HealthCheckTask`、`TcpChannelMonitor` 等，与仓库中同名 Java 源文件对齐；改定时逻辑或依赖时，应先改设计说明里的流程或周期说明，再改契约里若涉及对外接口语义，最后改实现，避免只改代码、文档悬空。

### 场景二：OpenSpec 驱动的 SDD

OpenSpec 适合**一次变更要动多个模块、多份能力规格、还要留任务清单**的情况：动机和设计先对齐，再按能力拆 spec，最后按 `tasks.md` 勾选项实现，避免只对着聊天改代码。

**本仓库里典型目录结构**（每条变更一个子目录）：

`openspec/changes/<变更名>/`

| 工件 | 常见文件名 | 写什么 |
|------|------------|--------|
| 动机与范围 | `proposal.md` | 为什么做、改哪些能力、影响哪些工程（Java/Python/测试等）。 |
| 设计与决策 | `design.md` | 背景、目标与非目标、关键决策与备选、风险、未决问题、迁移或启动顺序。 |
| 能力规格 | `specs/<能力id>/spec.md` | 按能力拆分，多用 SHALL / WHEN / THEN，便于验收和评审。 |
| 任务清单 | `tasks.md` | 分阶段可勾选步骤，带验证方式，实现时按顺序推进。 |

**和场景一的分工**：场景一侧重**单模块、长期维护**的说明书加接口契约，代码包和类名和文档强绑定。OpenSpec 侧重**跨模块、带交付边界的一次性变更**（例如端到端联调、local 启动能力），变更目录可以引用或对齐 `browser-gateway/doc/模块设计文档` 里的约定，但不替代各模块自己的设计说明书。

**示例**：`openspec/changes/e2e-login-operation-verification/` 里同时包含 local 启动、`e2e-login-flow`、`e2e-operation-flow` 等能力 spec，以及分服务的任务项，适合作为 OpenSpec SDD 的完整样本打开看一眼。

**Claude Code 斜杠命令（本仓库 `.claude/commands/opsx/`）**

在已接入该命令集的对话里，前缀是 **`/opsx:`**（注意不是 `/opsc:`；子命令名是 **propose** 不是 proposal）。与 OpenSpec CLI 配合，用来走「先变更、再实现、再归档」的流程。

| 命令 | 作用 |
|------|------|
| `/opsx:propose` | 新建一条变更（change），并尽量一次性补齐工件：常见为 `proposal.md`（做什么、为什么）、`design.md`（怎么做）、`tasks.md`（实现步骤）；具体以当前 openspec 工作流为准。后面可接 kebab-case 变更名，或一段自然语言描述需求。 |
| `/opsx:apply` | 按指定变更进入**实现**：读取 `openspec status`、`openspec instructions apply` 给出的上下文与任务列表，按 `tasks.md` 逐项推进。可写 `/opsx:apply` 或 `/opsx:apply <变更名>`；若未指名且有多条变更，助手会请你选。全部完成后可再归档。 |
| `/opsx:archive` | 把**已完成**的变更归档：通常移到 `openspec/changes/archive/日期-变更名/`，并可按需与主规格同步；归档前会核对工件是否齐。可写 `/opsx:archive` 或 `/opsx:archive <变更名>`。 |
| `/opsx:explore` | **只思辨、不动手改业务代码**：澄清需求、拆问题、看代码与依赖；适合在 propose 之前想清楚。若要在探索里落笔，一般是记录想法或草稿规格，而不是直接实现功能。 |

补充：`apply` 若提示变更被阻塞（例如还缺某些工件），助手可能让你先补全再继续；底层也会用到 `openspec new change`、`openspec status`、`openspec list` 等 CLI，需要本机已安装 OpenSpec 并能在终端执行。

---

## 小结：如何选用

| 模式 | 适用 |
|------|------|
| 对话修改 | 小改动、范围清楚、验收一句话能说清。 |
| 范式 Skill | 重复形态：Mock、REST 增量、Adapter、周期任务等；适合做成 Skill 或团队模板。 |
| 编码规范 Rule | 横切风格与质量，例如 Cursor 下的 .cursor/rules/*.mdc，和 Skill 一起用；见第 2 章 Rule 小节。 |
| 大颗粒 Spec | 先设计后编码。常见两条：单模块用模块设计说明书 + 接口契约；跨模块交付用 OpenSpec 变更目录（proposal / design / specs / tasks）。 |

