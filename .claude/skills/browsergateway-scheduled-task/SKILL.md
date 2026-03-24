---
name: browsergateway-scheduled-task
description: Add a periodic background task in browser-gateway scheduled package using ScheduledExecutorService, PostConstruct, PreDestroy, and browsergw.scheduled YAML keys—matching BrowserCheckTask, TcpChannelMonitor, HealthCheckTask pattern (not Spring @Scheduled).
license: MIT
metadata:
  author: sbg
  version: "1.0"
---

# browser-gateway：`scheduled` 包周期任务范式

网关内**周期性后台任务**统一放在 `com.huawei.browsergateway.scheduled`，使用 **`ScheduledExecutorService` + `@PostConstruct` / `@PreDestroy`**，**不使用** Spring `@Scheduled`。新增任务须与现有类同构，避免线程泄漏与重复调度器风格。

## 何时使用

- 需要按固定间隔执行逻辑（健康检查、TCP 心跳扫描、浏览器清理、状态上报、脚本执行等同类需求）。
- 任务需随 Spring 容器启停：启动时注册调度，销毁时 `shutdown()`。

## 本仓库标准范式（必须对齐）

1. **`@Component`**（少数场景用 `@Service`，如仅监听事件的一次性逻辑——见下文「变体」）。
2. **`private static final Logger log = LogManager.getLogger(X.class)`**。
3. **周期配置**：`@Value("${browsergw.scheduled.<key>:<defaultMs>}")` 注入 `long period`（毫秒）。  
   - 特例：`BrowserProxyLogDump` 使用 `shell.script.period` 等，若新增非 `browsergw.scheduled` 前缀的键，须在 `application.yaml` 中说明并与团队约定一致。
4. **字段**：`private ScheduledExecutorService scheduler;`（按需增加 `@Autowired` 依赖）。
5. **`@PostConstruct`**：`scheduler = Executors.newSingleThreadScheduledExecutor();`  
   `scheduler.scheduleAtFixedRate(this::taskMethod, 0, period, TimeUnit.MILLISECONDS);`  
   首行延迟 `0` 与现有一致；若需 `scheduleWithFixedDelay` 须注明理由。
6. **任务方法**：`public` 或 `private` 均可；**整体 `try/catch`**，异常写 `log.error`，**不向外抛出**以免打断后续调度（与 `TcpChannelMonitor`、`BrowserCheckTask` 等一致）。
7. **`@PreDestroy`**：`if (scheduler != null) { scheduler.shutdown(); }` 并打 info 日志。

## 用户需提供

- 任务职责与**单次执行**方法语义。
- 建议的 **YAML 键名**（放在 `browsergw.scheduled.*` 下）及默认周期（毫秒）。
- 依赖的 Service / Config / Adapter（仅构造与注入，业务另述）。

## 必须遵守

- **单线程调度器**：默认 `newSingleThreadScheduledExecutor()`；若改用多线程线程池须说明并发安全。
- **不在周期任务内阻塞过久**；长耗时考虑异步或拆任务。
- **新增配置**写入 `application.yaml`（或 profile 专用 yaml），键名与 `@Value` 一致，并带合理默认值。
- 不要将同类逻辑散落到随机 `@Component` 自建无关闭钩子的 `Executor`。

## 变体（勿与周期范式混用）

- **仅启动执行一次**、带重试：可参考 `ServiceReporter`（`@EventListener(ContextRefreshedEvent.class)` + 循环/`Thread.sleep`），**不要**为此新建 `scheduleAtFixedRate`。
- **聚合多策略定时健康检查**：参考 `HealthCheckTask`（`List<ICheckStrategy>` + 单次调度入口）；新增检查项优先扩展 `ICheckStrategy`，而非再开一个并行调度器（除非职责完全独立）。

## 参考实现（按需对照）

- `scheduled/BrowserCheckTask.java` — 周期 + 外部 Client + `@PreDestroy`
- `scheduled/TcpChannelMonitor.java` — 周期 + 多集合扫描
- `scheduled/HealthCheckTask.java` — 周期 + 策略列表 + 告警上报
- `scheduled/ServiceStatusRefresherTask.java` — 短周期上报
- `scheduled/BrowserCloserTask.java` — 周期 + 配置 `ttl`
- `scheduled/BrowserProxyLogDump.java` — 周期 + Shell（非 `browsergw.scheduled` 配置前缀示例）

## 实施步骤

1. 在 `scheduled` 包新建 `<Xxx>Task.java`（或语义化类名），按上表补齐字段与注解。
2. 在 `src/main/resources/application.yaml`（或已有片段）增加 `browsergw.scheduled.<key>` 默认值。
3. 实现任务方法；确保 `init` 打周期日志、`destroy` 关闭调度器。
4. 本地启动验证：日志出现 initialized；停止应用无调度器泄漏告警。

## 验收

- 应用正常启停；`@PreDestroy` 被调用后无残留调度（可通过日志确认）。
- Maven 编译通过；周期与配置可通过改 yaml 验证。
