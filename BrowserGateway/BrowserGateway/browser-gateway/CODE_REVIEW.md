# Code Review 报告

基于 `.cursor/rules/java-enterprise-coding-standard.mdc` 企业级 Java 编码规范，对 browser-gateway 主代码的审查结果。

---

## 一、总体结论

| 维度         | 情况说明 |
|--------------|----------|
| 命名与格式化 | 整体符合，少量命名/拼写可改进 |
| 注释与 Javadoc | 公共类与方法大多有 Javadoc，符合规范 |
| 异常处理     | **普遍** 使用 `catch (Exception e)`，建议按规范细化异常类型并保留 cause |
| 日志         | 个别处 error 未带异常参数，或存在潜在敏感信息输出 |
| 安全与入参   | 入参校验可加强，敏感信息避免全量打日志 |
| 圈复杂度/分支 | 未发现明显超标，个别方法可拆分为多方法 |

---

## 二、按文件审查

### 2.1 ChromeApi.java

| 规范条目 | 问题 | 位置 | 建议 |
|----------|------|------|------|
| **5.2 异常粒度** | 多处 `catch (Exception e)` | 71、89、104、128、149 行 | 按可能抛出的类型分别 catch（如 `IOException`、`BusinessException`），或至少包装为业务异常并保留 cause |
| **6.2 日志与敏感信息** | 将 `param` 全量序列化打日志 | 60、70、119、129 行 | 若 Request 中含 IMEI/IMSI 等敏感字段，避免 `JSONUtil.toJsonStr(param)` 全量输出；可只打 userId 或脱敏后字段 |
| **1.4 命名** | 成员变量 `fs` 过短 | 41 行 | 改为 `fileStorage` 等有含义命名 |
| **13.2 输入校验** | 未体现对 RequestBody 的校验 | deleteUserData、preOpenBrowser | 在 Service 或 Api 层对 `param` 做非空、长度、格式校验（如 IMEI/IMSI 格式），避免仅依赖前端 |
| **4.2 依赖注入** | @Resource 与 @Autowired 混用 | 37–47 行 | 建议统一使用一种（如 @Resource 或 @Autowired） |

**符合规范**：类与方法有 Javadoc；大括号与缩进正确；未在 finally 中 return；error 日志多数带异常参数。

---

### 2.2 ExtensionManageService.java

| 规范条目 | 问题 | 位置 | 建议 |
|----------|------|------|------|
| **5.2 异常粒度** | `catch (Exception e)` 过于宽泛 | 62、155 行 | 根据 `downPlugin`、`findJarPath` 等实际抛出类型捕获（如 IOException、IllegalStateException），再包装或记录 |
| **5.1 禁止空 catch** | findJarPath 中 catch 后仅 log 再 throw，未保留 cause | 155–157 行 | `throw new RuntimeException("...", e)` 保留 cause，便于排查 |
| **4.7 分支完整性** | 异常信息字符串拼接缺少空格 | 129、136 行 | `"not found package.json file in" + unzipDir` → `"not found package.json file in " + unzipDir`，`"find jar file error"` → `"find jar file error: " + path` 等，避免拼接连在一起 |
| **13.4 返回值** | findJarPath 未找到时返回 `StrUtil.EMPTY` | 159 行 | 调用方已用 `FileUtil.isFile(jarPath)` 判断，逻辑可行；若希望更明确，可改为 Optional\<String\> 或抛异常，避免空串被误用 |

**符合规范**：类与主要方法有 Javadoc；使用占位符打日志；synchronized 使用合理；私有方法职责清晰。

---

### 2.3 HWCallbackImpl.java

| 规范条目 | 问题 | 位置 | 建议 |
|----------|------|------|------|
| **1.3 方法命名** | 接口实现方法首字母大写：GetConfig、Send、Address、Log | 60、67、79、85 行 | 若为 SDK 接口约束无法修改，在类 Javadoc 中说明“实现外部接口，方法名保持与接口一致”；否则应改为小驼峰 |
| **1.3 拼写** | 方法名拼写错误 | 91 行 | `sendMessageToWebscoket` → `sendMessageToWebSocket`（若为接口方法，需与接口同步修改） |
| **4.2 参数命名** | uploadFile 第一个参数名为 `s` | 124 行 | 改为 `userId` 或 `userIdentifier` 等有含义命名 |
| **5.2 异常包装** | getFile 中 catch IOException 后 throw RuntimeException | 109–112 行 | 已保留 cause，符合规范；若项目有统一业务异常类，建议改为该类型 |

**符合规范**：类与方法有注释；常量命名清晰；未吞异常；日志带占位符与异常。

---

### 2.4 其他文件（共性）

| 文件/位置 | 问题 | 建议 |
|-----------|------|------|
| **FileStorageServiceImpl** 166–167 行 | `catch (Exception e)` 且 `log.error` 未传入 `e` | 在 log.error 最后一个参数传入 `e`，便于堆栈排查 |
| **FfmpegStreamProcessor** 143、173、191 行 | `catch (Exception e) { return -1; }` 吞掉异常 | 至少 log.warn 记录异常，或包装后抛出，避免静默失败 |
| **ClientImpl** 52、121 行 | catch Exception 后 throw new RuntimeException(..., e) | 已保留 cause，可考虑统一为项目业务异常类 |
| **多处** | 广泛使用 `catch (Exception e)` | 在入口或边界处可保留一层兜底，内部尽量按具体异常类型 catch 并处理 |

---

## 三、按规范章节汇总

### 三、命名规范（一）

- 包名、类名、常量与变量整体符合。
- 建议：ChromeApi 中 `fs` → `fileStorage`；HWCallbackImpl 中 `uploadFile(String s, ...)` → 参数名改为 `userId`；修正 `sendMessageToWebscoket` 拼写。

### 四、注释规范（三）

- 公共类与对外方法多数有 Javadoc，符合 3.1。
- 未发现明显“废话注释”，符合 3.2。

### 五、异常处理（五）

- **5.1**：FfmpegStreamProcessor 等存在“仅 return 不记录”的 catch，建议至少打日志或包装后抛出。
- **5.2**：大量 `catch (Exception e)`，建议在可预见的调用链上改为具体异常类型并保留 cause。
- **5.3**：未发现 finally 中 return 的写法。

### 六、日志规范（六）

- 已使用门面与占位符，符合 6.1。
- **6.2**：ChromeApi 中若 param 含 IMEI/IMSI，避免全量 JSON 打日志；FileStorageServiceImpl 的 catch 块中 error 应带上异常参数。

### 七、安全规范（十三）

- **13.1**：注意 Request 序列化到日志时的敏感字段（IMEI/IMSI 等），做脱敏或只打必要字段。
- **13.2**：对 deleteUserData、preOpenBrowser 等入口增加入参校验（非空、格式、长度）。
- **13.4**：集合返回处未发现返回 null，符合；ExtensionManageService.findJarPath 返回空串已由调用方防护，可维持或改为 Optional/异常。

### 八、圈复杂度与分支（四）

- 未发现单方法明显超过 15 的复杂分支；ExtensionManageService.decompress 等分支完整，无缺失 default 的 switch。

---

## 四、优先修复建议（按优先级）

1. **高**：ChromeApi 及 ExtensionManageApi 等对外接口的入参校验（非空、格式、长度），以及日志中避免敏感信息全量输出。
2. **高**：FileStorageServiceImpl 第 167 行 log.error 补充异常参数 `e`。
3. **中**：ChromeApi、ExtensionManageService 等将 `catch (Exception e)` 收窄为具体异常类型（在可区分的情况下），并统一包装为业务异常且保留 cause。
4. **中**：FfmpegStreamProcessor 等 catch 后至少记录日志或包装再抛出，避免静默返回 -1。
5. **低**：ChromeApi 中 `fs` 重命名为 `fileStorage`；HWCallbackImpl 中 `sendMessageToWebscoket` 拼写修正、参数名 `s` 改为 `userId`；ExtensionManageService 异常信息字符串拼接补空格。

---

## 五、使用说明

- 本报告基于当前规范手册生成，可作为迭代修改与 Code Review 检查清单。
- 修复时请兼顾现有测试与调用方，尤其是接口方法名、参数名、异常类型的变更。
- 建议在 CI 中启用 SonarQube/PMD 的圈复杂度与常见违规规则，与本文档配合使用。
