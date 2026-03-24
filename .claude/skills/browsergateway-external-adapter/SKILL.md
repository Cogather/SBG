---
name: browsergateway-external-adapter
description: Add a new external-system adapter in browser-gateway following the interface + CspAdapter + CustomAdapter + AdapterFactory + AdapterConfig pattern. Use when integrating a new CSP-facing capability with internal (CSP SDK) vs external (custom) implementations.
license: MIT
metadata:
  author: sbg
  version: "1.0"
---

# browser-gateway：新增外部系统适配器（Adapter）

网关通过 **适配器接口** 隔离 CSP / 内网 SDK 与 **外网自定义实现**。新增一类能力时须**成对**补齐两套实现，并挂到工厂与 Spring 配置。

## 何时使用

- 需要对接新的外部系统能力（证书、告警、审计、资源监控、服务管理等同类形态）。
- 行为需随 `csp.adapter.environment`（`internal` / `external`）在 **CSP 实现** 与 **Custom 实现** 间切换。

## 本仓库既有结构（必须对齐）

| 层次 | 路径约定 |
|------|-----------|
| 接口 | `com.huawei.browsergateway.adapter.<Name>Adapter` |
| CSP 实现 | `adapter/impl/csp/Csp<Name>Adapter.java` |
| 外网/自定义实现 | `adapter/impl/custom/Custom<Name>Adapter.java` |
| DTO（如需） | `adapter/dto/` |
| 工厂接口 | `adapter/factory/AdapterFactory.java` — 增加 `create<Name>Adapter()` |
| 工厂实现 | `CspAdapterFactory`、`CustomAdapterFactory` — 分别 `new Csp...` / `new Custom...` |
| Spring 装配 | `adapter/config/AdapterConfig.java` — `@Bean` 注入 `AdapterFactory`，调用 `factory.create<Name>Adapter()` |

环境选择逻辑已在 `AdapterConfig.adapterFactory()`：**external → CustomAdapterFactory**，否则 **CspAdapterFactory**。勿绕开该机制另起全局 Bean。

## 用户需提供

- 适配器**职责**与**接口方法**签名（与调用方 Service 的依赖方式）。
- CSP 侧：依赖的 SDK/API 或占位策略（可先做可编译桩，再填逻辑）。
- Custom 侧：外网/无 SDK 时的行为（空实现、HTTP 调用、本地 JMX 等），与现有 `Custom*Adapter` 风格一致。
- 若构造依赖其它 Adapter（如 `CspAlarmAdapter` 依赖 `DeployUtil(SystemUtilAdapter)`），说明依赖链，在两侧工厂中**对称**构造。

## 必须遵守

1. **接口只定义在** `adapter` 包根下；实现仅在 `impl/csp` 与 `impl/custom`。
2. **两个工厂**必须同时新增 `create...` 方法，且返回类型为同一接口。
3. **`AdapterConfig`** 中为该接口增加 `@Bean`，方法参数为 `AdapterFactory factory`，体内仅 `return factory.createXxxAdapter()`。
4. 日志使用 **Log4j2** `LogManager.getLogger`，与现有 `Csp*Adapter` / `Custom*Adapter` 一致。
5. 调用方只注入 **接口类型**，不注入 `Csp*` / `Custom*` 具体类。

## 实施步骤

1. 在 `adapter` 包新增 `<Name>Adapter` 接口；如需共享数据结构，在 `adapter/dto` 补充类型。
2. 实现 `impl/csp/Csp<Name>Adapter` 与 `impl/custom/Custom<Name>Adapter`。
3. 修改 `AdapterFactory`，增加 `create<Name>Adapter()`；在 `CspAdapterFactory`、`CustomAdapterFactory` 中实现。
4. 在 `AdapterConfig` 中注册 `@Bean`。
5. 在需要使用的 Service / Component 中 `@Autowired` 或构造注入该接口；运行或单测时通过 `application.yaml` 的 `csp.adapter.environment` 验证两种实现均可装配。

## 参考实现（按需对照）

- 工厂与配置：`adapter/factory/CspAdapterFactory.java`、`CustomAdapterFactory.java`、`adapter/config/AdapterConfig.java`
- 完整成对示例：`CertificateAdapter` / `CspCertificateAdapter` / `CustomCertificateAdapter`，或 `ResourceMonitorAdapter` 与对应 Csp/Custom 实现

## 验收

- `internal` / `external` 两种配置下应用均能启动，且对应工厂创建的实现类符合预期。
- 新增模块 **Maven 编译通过**；无循环依赖；调用方仅依赖接口。
