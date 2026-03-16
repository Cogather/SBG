# 全项目代码检视意见（Committer Code Review）

**检视范围**：BrowserGateway / browser-gateway 模块全部主代码（`src/main/java`，约 338 个 Java 文件）  
**规范依据**：`.cursor/rules/java-enterprise-coding-standard.mdc` 企业级 Java 编码规范  
**目的**：供 Committer 检视与开发者自查，督促合入前修复与迭代改进。

---

## 一、总体结论

| 维度           | 结论说明 |
|----------------|----------|
| **命名与格式化** | 整体符合；少量命名/拼写需改进（如 `fs`、`sendMessageToWebscoket`） |
| **注释与 Javadoc** | 公共类与方法多数有 Javadoc，符合规范 |
| **异常处理**   | **普遍** 使用 `catch (Exception e)`；部分 catch 后静默 return，未记录日志 |
| **日志**       | 部分 error 未带异常参数；敏感信息（IMEI/IMSI）存在全量打日志风险 |
| **安全与入参** | API 层已通过 ParamValidator 做入参校验；FileStorageServiceImpl 等仍缺入参校验 |
| **圈复杂度**   | 未发现单方法明显超标；个别方法可拆分 |

**建议**：高优先级项合入前必须修复；中低优先级项本迭代或后续迭代整改，并在 CI 中配合 SonarQube/PMD 等静态检查。

---

## 二、已覆盖的专项检视文档

以下文件已有详细检视结论，请一并执行：

| 文档 | 说明 |
|------|------|
| **CODE_REVIEW.md** | ChromeApi、ExtensionManageService、HWCallbackImpl、FileStorageServiceImpl、FfmpegStreamProcessor、ClientImpl 等按文件与规范条目的审查结果 |
| **CODE_REVIEW_FileStorageServiceImpl.md** | FileStorageServiceImpl 的逐项问题、修复示例与自查清单 |

本报告在以上基础上做**全项目汇总**与**补充检视**，并给出统一的自查清单。

---

## 三、按规范维度汇总的检视意见

### 3.1 命名规范（规范一）

| 位置 | 问题 | 建议 |
|------|------|------|
| **ChromeApi.java** 约 42 行 | 成员变量 `fs` 过短、无业务含义 | 改为 `fileStorage`，与接口 `IFileStorage` 一致 |
| **HWCallbackImpl**（若存在） | 方法名 `sendMessageToWebscoket` 拼写错误 | 改为 `sendMessageToWebSocket`（若为接口实现，需与接口同步） |
| **HWCallbackImpl** | 方法参数名为 `s`（如 uploadFile 第一参数） | 改为 `userId` 或 `userIdentifier` 等有含义命名 |
| **FileStorageServiceImpl** | 接口 Javadoc 为 `localPath`/`remotePath`，实现为 `localFilePath`/`remoteUrl` | 实现类参数名与接口保持一致，便于文档与调用方一致 |

### 3.2 依赖注入一致性（规范 4.2）

| 位置 | 问题 | 建议 |
|------|------|------|
| **ChromeApi.java** 约 38–48 行 | `@Resource` 与 `@Autowired` 混用 | 项目内统一使用一种（建议统一 `@Resource` 或 `@Autowired`） |

### 3.3 异常处理（规范五）

| 规范条目 | 问题分布 | 建议 |
|----------|----------|------|
| **5.1 禁止空 catch** | **FfmpegStreamProcessor**：`readHook`、`writeVideoHook`、`writeAudioHook` 中 `catch (Exception e) { return -1; }` 未记录日志 | 至少 `log.warn("...", e)` 或包装后抛出，避免静默失败、难以排查 |
| **5.2 异常粒度与包装** | 全项目多处 `catch (Exception e)`（ChromeApi、ExtensionManageApi、ExtensionManageService、各 Adapter、FileStorageServiceImpl 等） | 在可区分的情况下按具体类型 catch（如 IOException、JsonException）；对外可包装为业务异常并保留 cause |
| **5.2 保留 cause** | 个别 catch 后 `throw new RuntimeException("msg")` 未传 `e` | 改为 `throw new RuntimeException("msg", e)` 或项目统一业务异常类 |

### 3.4 日志规范（规范六）

| 规范条目 | 问题 | 建议 |
|----------|------|------|
| **6.2 异常参数** | **AudioCodecProcessor** 多处 `log.error("...")` 未带异常参数（如 “Error during decoding”、“Failed to find decoder” 等） | 若在 catch 块或已知异常场景，最后一个参数传 `e`，便于堆栈排查 |
| **6.2 敏感信息** | **ChromeApi**：`log.info/error(..., JSONUtil.toJsonStr(param))`，param 含 IMEI/IMSI | 避免全量序列化；可只打 userId 或脱敏字段（如 IMEI 后四位） |
| **6.2** | **ExtensionManageApi**：info/error 中 `JSONUtil.toJsonStr(param)` | 若 param 含路径等敏感信息，考虑脱敏或只打必要字段 |

### 3.5 安全与入参（规范十三）

| 规范条目 | 问题 | 建议 |
|----------|------|------|
| **13.2 输入校验** | **FileStorageServiceImpl**：`uploadFile`/`downloadFile`/`deleteFile`/`exist` 及 `parseS3Url` 未对 `localFilePath`、`remoteUrl` 做非空与格式校验 | 入口做 `StringUtils.isBlank` 等校验，非法时抛 `IllegalArgumentException`；`parseS3Url` 内校验 url 至少包含两段路径，避免 NPE 或下标越界 |
| **13.2** | API 层（ChromeApi、ExtensionManageApi） | 已使用 ParamValidator，符合规范，保持即可 |
| **13.4 返回值** | 避免集合/重要返回值返回 null 被误用 | 已审文件中未发现明显违规；ExtensionManageService.findJarPath 返回空串已由调用方防护，可维持或改为 Optional/异常 |

### 3.6 魔法值与常量（规范 4.3）

| 位置 | 问题 | 建议 |
|------|------|------|
| **FileStorageServiceImpl** | HTTP 状态码 200、404 字面量散落 | 定义 `HTTP_OK = 200`、`HTTP_NOT_FOUND = 404` 等常量 |
| **FfmpegStreamProcessor** | 120000（毫秒超时）等魔法值 | 提取为命名常量（如 `CONNECT_TIMEOUT_MS`） |

### 3.7 分支完整性（规范 4.7）

| 位置 | 说明 |
|------|------|
| ExtensionManageService 等 | 异常信息字符串拼接时注意空格（如 `"not found package.json file in" + unzipDir` → `"not found package.json file in " + unzipDir`），避免拼接连在一起 |

---

## 四、按优先级列出的修复建议

### 高优先级（合入前建议修复）

1. **FileStorageServiceImpl**
   - 入参校验：对 `localFilePath`、`remoteUrl` 非空及格式校验；`parseS3Url` 内对 url 做合法性校验。
   - 错误响应体解析已改为 `EntityUtils.toString(entity, StandardCharsets.UTF_8)` 的，请确认并保持；`dealFileHttpError` 的 catch 中 `log.error` 最后一个参数为异常 `e`。
2. **ChromeApi / ExtensionManageApi**
   - 日志中避免将含 IMEI/IMSI 的请求体全量 `JSONUtil.toJsonStr(param)` 输出；改为 userId 或脱敏字段。
3. **FfmpegStreamProcessor**
   - `readHook`、`writeVideoHook`、`writeAudioHook` 中 `catch (Exception e) { return -1; }` 至少增加 `log.warn("...", e)`，避免静默失败。

### 中优先级（本迭代或近期迭代）

4. 将 **ChromeApi**、**ExtensionManageService**、各 **Adapter** 等处的 `catch (Exception e)` 在可区分场景下收窄为具体异常类型，并统一包装为业务异常且保留 cause。
5. **AudioCodecProcessor** 等处在 catch 或已知异常场景下的 `log.error` 补充异常参数。
6. **FileStorageServiceImpl**：HTTP 状态码 200/404 提取为常量；实现类参数名与接口 Javadoc 一致（localPath/remotePath）。

### 低优先级（可选优化）

7. **ChromeApi**：`fs` → `fileStorage`；统一 `@Resource` 或 `@Autowired`。
8. **HWCallbackImpl**：`sendMessageToWebscoket` 拼写修正、参数名 `s` → `userId`（若为接口实现需同步接口）。
9. **FileStorageServiceImpl**：上传前校验本地文件存在性；下载前确保目标父目录存在；`dealFileHttpError` 中未读取 entity 的分支显式 `EntityUtils.consume`。

---

## 五、开发者自查清单（提交前逐项确认）

以下清单供**开发者**在提交前自查，Committer 可据此做合入前检查。

### 5.1 异常与日志

- [ ] 无**空 catch**：catch 块中至少打日志或包装后抛出。
- [ ] **FfmpegStreamProcessor** 的 readHook/writeVideoHook/writeAudioHook 中 catch 已增加 `log.warn(..., e)` 或等价处理。
- [ ] 对外接口或关键路径上的 **log.error** 在异常场景下**最后一个参数为异常对象**（如 `log.error("msg", e)`）。
- [ ] 避免将 **IMEI/IMSI** 或其它敏感信息在日志中**全量输出**；使用 userId 或脱敏。

### 5.2 入参与安全

- [ ] **FileStorageServiceImpl** 的 upload/download/delete/exist 及 parseS3Url 已做**入参非空与格式校验**，非法时抛明确异常。
- [ ] 新增或修改的 API 入参已通过 **ParamValidator** 或等价方式校验（非空、格式、长度等）。

### 5.3 命名与常量

- [ ] **ChromeApi** 中 `fs` 已改为 `fileStorage`（若已按建议修改）。
- [ ] **FileStorageServiceImpl** 中 HTTP 状态码 200/404 已提取为常量（若已按建议修改）。
- [ ] 实现类方法参数名与**接口 Javadoc** 一致（如 IFileStorage 的 localPath/remotePath）。

### 5.4 与既有文档一致

- [ ] **FileStorageServiceImpl** 的修改已符合 **CODE_REVIEW_FileStorageServiceImpl.md** 中的高优先级项与自查项。
- [ ] **ChromeApi、ExtensionManageService、HWCallbackImpl** 等已按 **CODE_REVIEW.md** 第二节、第四节完成对应整改（若在本次提交范围内）。

### 5.5 通用

- [ ] 本地单测/联调通过，未引入新的编译或静态检查告警。
- [ ] 变更涉及接口或异常类型时，已评估并兼容**调用方**（如 HWCallbackImpl、UserData、ChromeSetImpl 等）。

---

## 六、使用说明

- **Committer**：合入前可依据本文档第三节（按规范维度）、第四节（按优先级）以及第五节（自查清单）做检视与要求。
- **开发者**：提交前请按 **第五节** 逐项自查，并对照 **CODE_REVIEW.md**、**CODE_REVIEW_FileStorageServiceImpl.md** 中与本次修改相关的条款。
- 建议在 **CI** 中启用 SonarQube/PMD 的圈复杂度与常见违规规则，与本文档配合使用，逐步减少历史债务。

---

**检视人**：Committer（Code Review）  
**文档版本**：全项目代码检视意见 v1，用于督促开发者自查与合入前检查。
