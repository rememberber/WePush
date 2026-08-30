# WePush Next 兼容性策略

- 适用版本：`1.0.0` 及后续 `1.x`（当前稳定版 `1.1.0`）
- 首个稳定兼容基线：`1.0.0`
- 最低直接升级版本：`0.1.0-beta.1`
- `1.0.0` 最低可回滚版本：`0.1.0-beta.1`
- `1.1.0` 推荐回滚版本：`1.0.0`（必须使用升级前完整备份）

## 1. 版本规则

WePush Next 从 `1.0.0` 起使用语义化版本：

- Patch（`1.0.x`）只包含兼容的修复、安全更新和文档调整。
- Minor（`1.x.0`）可以增加可选 API、配置和 Provider 能力，但不能破坏已有用法。
- Major（`2.0.0`）才允许移除或改变已经公开的兼容契约，并必须提供迁移说明。
- 带连字符的版本仍是预览版；不带连字符的版本是稳定版。

## 2. REST、SSE 与 OpenAPI

`/api/v1` 和发行包中的 OpenAPI 3.1 文档是 `1.x` 公共 HTTP 契约。`1.x` 内承诺：

- 不删除已有路径、HTTP 方法、响应状态或必需响应字段。
- 不新增已有请求必须提供的字段，不收窄字段类型、长度或合法取值。
- 可以增加可选请求字段、响应字段、端点和错误码；客户端应忽略未知响应字段。
- 已发布字段或端点至少经过一个 Minor 版本的弃用期后，才可在下一个 Major 移除。
- SSE 事件保持事件 ID 可恢复、事件类型可识别；可以增加新的事件类型和 Payload 字段。

`1.0.0` 发行门禁对 `0.1.0-beta.1` OpenAPI 做忽略版本号后的精确摘要校验，因此 Beta 用户既有 HTTP 调用在稳定版中没有契约漂移。`1.1.0` 只增加 Workspace Policy、系统运维、认证熔断和 Agent Multipart 等端点与可选字段；已有路径继续由兼容性摘要门禁保护。

## 3. Java SDK 与 Provider SPI

- Remote Java SDK 与 Embedded Java SDK 的公开类型在 `1.x` 内保持源码兼容；新增能力优先使用新方法、Builder 或新类型。
- SDK 与 Service 必须使用相同 Major；Service 接受同 Major 的旧 Minor SDK 请求。
- Provider SPI 以 `spiMajorVersion=1` 为稳定边界。`1.x` Agent 可加载 SPI Major 1 且通过清单、摘要和 Ed25519 签名校验的插件。
- Provider ID 与实现版本属于 Run Snapshot。升级不会悄悄把既有 Run 改到另一个 Provider 实现版本。
- Provider 行为发生不可兼容变化时必须发布新的实现版本，不能覆盖旧 Snapshot 语义。

## 4. 配置兼容

- `0.1.0-beta.1` 已公开的 `WEPUSH_*` 配置键在整个 `1.x` 保持可解析；发行门禁逐项验证这些键仍存在。
- Workspace Artifact 保留策略在首次显式更新前继续采用既有 `WEPUSH_EXPORT_RETENTION` 与 `WEPUSH_AGENT_ARTIFACT_RETENTION` 的分类默认值；管理员保存 Workspace Policy 后，由该 Workspace 的统一保留期接管新 Artifact。
- 新配置必须有安全默认值。升级不能默认启用外部网络访问、遥测、公开监听或弱认证。
- 配置弃用必须记录替代键，并在至少一个 Minor 周期内继续接受旧键；非法或冲突配置启动时失败关闭。
- Service/Agent 配置文件由用户控制，安装和升级脚本不能覆盖已存在文件。

## 5. 数据库迁移与回滚

- Flyway 迁移只向前执行，已经发布的迁移文件不可修改、重排或复用版本号。
- SQLite 与 PostgreSQL 使用相同的有序迁移历史，并在 CI 中分别验证。
- `0.1.0-beta.1` 的数据库版本为 V13；`1.0.0` 增加只新增元数据表的 V14，不删除或改写用户数据。
- `1.0.0` 可以直接从 `0.1.0-beta.1` 升级。自动化测试会先创建 V13 数据，再迁移到 V14 并核对原数据。
- V14 是附加迁移，因此 `1.0.0` 最低可回滚到 `0.1.0-beta.1`。仍建议使用升级前自动备份回滚，以同时恢复数据库、Master Key、Artifact 和 Agent 状态。
- `1.1.0` 继续增加附加迁移：V15 `workspace_policy`、V16 `account_auth_circuit`、V17 `artifact_multipart_upload`。这些迁移不删除、重命名或改写 V14 及以前的业务数据；SQLite 与 PostgreSQL 都从 V13 带用户数据迁移到 V17 做自动验证。
- `1.0.0` 不会读取 V15–V17 新表，但 `1.1.0` 人工回滚仍必须恢复升级前完整备份，避免新策略、熔断、上传会话及对象存储状态遗留。不能把“旧程序暂时可启动”等同于受支持的数据回滚。
- 未来版本一旦包含旧版本无法读取的迁移，Release Notes 必须提高最低可回滚版本，并明确只能通过备份恢复。

## 6. Agent 协议

- Agent Hello 声明协议 Major/Minor；`1.x` Service 与 Agent 使用协议 Major 1。
- 同 Major 内允许新增可忽略字段和帧类型，不改变 Lease/Fence、Sequence、ACK 和结果未知语义。
- 建议 Service 与 Agent 使用相同版本；滚动升级期间支持当前稳定版与上一稳定 Minor 的短期混合运行。
- Agent 失联不会直接触发无条件重发。Lease 到期后进入恢复流程；外部发送结果无法确认时记录 `UNKNOWN`。
- `1.1.0` 在 Major 1 内增加 Multipart Plan/Part/Complete/Abort 交互；单次 Presigned Put 继续兼容。超过 1 GiB 的 Agent Artifact 只有在 Service 和 Agent 都升级到 `1.1.0` 后才可用。

## 7. 不属于兼容承诺的内容

- 内部 Java 包、数据库表的直接访问、未记录环境变量和测试端点不是公共接口。
- 第三方消息平台自身的接口变更不受 WePush 控制；Provider 会按错误分类和版本规则处理。
- `0.x` 早期 Alpha 版本不提供直升 `1.0.0` 的保证，必须先升级到 `0.1.0-beta.1` 或导出后重建。
- WePush 不提供公共 SaaS 或官方托管控制面，因此不存在云端租户、计费或订阅数据的兼容承诺。
