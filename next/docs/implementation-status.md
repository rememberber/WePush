# WePush Next 实现状态

更新时间：2026-08-22

## 1. 当前里程碑

`next/` 已完成第一条可运行的 Standalone 纵向链路：

```text
React / Electron / Java SDK
          ↓ HTTP + SSE
Spring Boot Service API ←── gRPC 双向控制流 ──→ Remote Agent
                  │        Lease / Event / Command        ↓
                  │                                  Core Engine
          ↓ 应用事务
SQLite + Flyway ─── Artifact Metadata ─── Local File Artifact Store
          ↓ Run Snapshot
Embedded Core Engine（embedded）或 Remote Core Engine（remote）
          ↓
HTTP Provider（支持 Dry Run）
```

经典客户端工程未改动。Next 与经典客户端保持双轨、独立演进，允许各自按需实现相似代码。

## 2. 已实现组件

### 2.1 Core 和 Provider

- Framework-free Core API、Provider SPI 与执行引擎。
- 每个 Run 独立状态、虚拟线程执行、动态并发、限速、重试、超时、取消及结果汇总。
- 首个 HTTP Provider，包含 Account、Message、Recipient JSON Schema、SSRF 防护和 Dry Run。

### 2.2 Service 控制面

- Spring Boot 4.1.1 Web 与 Actuator 基线。
- SQLite Standalone 数据库，Hikari 最大连接数 4。
- SQLite `foreign_keys=ON`、WAL、`busy_timeout=5s`、`synchronous=NORMAL`。
- Flyway 显式启动迁移；迁移失败会阻止 Repository 和 Service 启动。
- 默认工作区 `ws_default`。
- Account、Message Revision、Audience Snapshot、Job、Run、Run Snapshot、Run Event 和幂等记录持久化。
- 默认 `LocalEnvelopeSecretStore`：每条 Secret 随机 256-bit DEK、AES-256-GCM、版本化主密钥封装、AAD 身份绑定和失败关闭。
- Standalone 首次启动生成独立主密钥文件；POSIX 强制 owner-only 权限，Windows 使用 owner ACL；也可由环境注入主密钥。
- Item Result 批量、事务和幂等落盘，使用 HMAC 完整性保护的 Cursor Pagination 查询。
- Pause、Resume、Cancel 和 ChangeConcurrency 命令持久化、按命令 ID 幂等并写入审计事件。
- Artifact 元数据以 SQLite 为事实源，采用 `UPLOADING → READY → DELETING → DELETED` 两阶段状态机。
- Standalone 默认 `LocalFileArtifactStore`：受控分区路径、临时文件写入、fsync、SHA-256 校验和原子移动；POSIX 文件权限为 `0600`。
- 定时保留任务回收过期且未 Pin/Legal Hold 的 Artifact；删除幂等，失败记录状态并允许后续重试。
- Message Revision、Audience Snapshot、Run Snapshot 创建后不可变。
- 应用服务显式持有事务边界，Repository 不自行提交。
- Agent 注册表持久化会话、平台、Provider 能力、容量、双向 Sequence 与最后心跳。
- 独立 gRPC Server 默认绑定 `127.0.0.1:19090`；非回环绑定强制 Bootstrap Token，静默超过三个心跳周期自动标记离线。
- 同一 Agent 的新连接会替换旧连接，旧会话后续写入由 Session ID Fence 拒绝。
- SQLite 持久化 Agent Lease、Epoch、Fencing Token、会话归属、Event Cursor 和完成状态；同一 Run 同时只允许一个活跃 Lease。
- `wepush.execution.mode=embedded|remote` 在组合根选择执行路径，默认保持 `embedded`。

### 2.3 公开 API

- Provider 发现及 Schema API。
- Account、Message、Audience、Job 的创建、列表和详情 API。
- Run 幂等创建、列表和详情 API。
- `Idempotency-Key` 相同且请求相同返回原 Run；相同 Key 对应不同请求返回 `409`。
- Run SSE 事件流支持持久化回放及实时推送，事件 ID 是 Run 内单调递增序号。
- Secret 写入/元数据 API；读取接口永不返回明文。
- Run Item Result 分页 API，以及暂停、恢复、取消和动态并发命令 API。
- 终态 Run 的脱敏 Item Result CSV 导出、Artifact 元数据、完整/Range 下载及保留清理 API。
- Agent 列表和详情 API，返回会话、平台、执行容量、Provider 能力和连接状态。
- 统一 `application/problem+json` 错误响应和稳定错误码。
- OpenAPI 3.1 契约以 28 个 Path 覆盖当前公开控制面。

### 2.4 Standalone 执行

- Run 创建提交后由内嵌 Core Engine 异步执行。
- Service 启动时重新发现默认工作区中的 `PENDING` / `RECOVERING` Run。
- Audience Snapshot 转换成 Core RecipientSource。
- Engine 事件持久化后再推送到本地 SSE Hub。
- Engine ResultSink 使用 SQLite 批量 Upsert；相同 Item/attempt 重放不产生重复结果。
- 活跃 RunHandle 保存在 Standalone 执行器内，Service 命令通过稳定 `commandId` 传递给 Core。
- 最终状态和计数原子写回 Run，并追加 `RUN_FINALIZED` 事件。
- 当前 HTTP Provider Dry Run 已通过两条 Recipient 的端到端测试。

### 2.5 SDK 和 UI

- Java SDK 只依赖 Service API DTO 和 HTTP，不依赖 Core/Engine。
- Java SDK 已增加 Workspace 控制面、Secret、结果分页、Artifact 流式下载、幂等 Run 创建和运行命令客户端。
- Java SDK 提供独立 `AgentsClient`，仍不依赖 Core 或 Engine。
- TypeScript API Client 已覆盖 Account、Message、Audience、Job、Run、Secret、结果、Artifact 和命令。
- React WebUI 与 Electron 共享 Feature/UI 包。
- Provider Schema 可视化配置可以直接保存 Account。
- 账号页读取真实持久化数据。
- 消息、受众和 Job 页面均接入真实创建/列表 API，Job 可直接发起 Dry Run。
- 运行中心轮询 Run 状态，使用 SSE 展示持久化/实时事件，并显示 Item Result、实时命令控制条和 CSV Artifact 导出/下载卡片。
- API 文档页加载当前 OpenAPI，并可动态调试主要 GET API。
- Agent 页面轮询真实注册表，以接近 Codex 客户端的紧凑布局展示在线状态、容量、Sequence、平台和 Provider 能力。

### 2.6 Agent 控制面

- `agent_control_v1.proto` 生成 Java Protobuf DTO 和 `AgentControlService.Connect` 双向流 Stub。
- Agent 主动出站连接 Service，Hello 协商协议 v1，Welcome 下发心跳周期、消息上限和 Service Sequence。
- Agent 按 Welcome 周期发送 Heartbeat，断线后以 1 秒至 30 秒指数退避并加入随机抖动重连。
- Agent 的双向 Sequence 和 Lease Fence 使用原子替换的本地文件 Journal 持久化，进程重启后延续。
- gRPC Server 与 Client 均设置消息上限、Keepalive 和超时；Bootstrap Token 通过固定时间比较校验。
- Service 按 Provider 精确版本和可用容量选择在线 Agent，发送带 Epoch、Fencing Token、期限、Snapshot/Audience URL 与 SHA-256 的 Lease Offer。
- Execution Spec 与 Audience 使用受 Agent Token 保护的内部 HTTP API 下载；Agent 只有在下载和哈希校验成功后才发送 Lease Ack。
- Agent 把冻结文档转换成 Core `RunExecutionSpec` / `RecipientSource`，与内嵌模式复用同一 Engine 和 Provider 行为。
- Core Event 和批量 Item Result 编码为连续 Agent Event Batch；Service 原子落库后返回 Event Ack，重放同一批次不会重复结果或事件。
- Pause、Resume、Cancel、ChangeConcurrency 可由 Service 经活跃 Lease 发送到远端 `RunHandle`，Agent 返回 Command Ack。
- Run Summary 经 `RunCompleted` 回传，Service 校验 Fence 后原子完成 Lease 与 Run，并产生 `RUN_FINALIZED`。
- Offer 过期、发送失败或 Agent 断线会使 Lease 进入 `EXPIRED` / `LOST`，Run 进入 `RECOVERING`；运行中断线默认保留 30 秒恢复宽限期，避免立即无条件重发。
- 当前远端 Secret Envelope 尚未实现加密封装；远端执行明确拒绝 Secret 解析，只支持无需 Secret 的配置，不会明文降级。

## 3. 当前数据库表

| 表 | 用途 |
|---|---|
| `workspace` | Workspace 边界和状态 |
| `account_definition` | Provider 账号普通配置和 Secret 引用 |
| `message_definition` / `message_revision` | 消息元数据和不可变修订 |
| `audience_definition` / `audience_snapshot` / `audience_recipient` | 受众及不可变执行快照 |
| `job_definition` | Account、Message、Audience 和策略组合 |
| `run_instance` / `run_snapshot` | Run 状态与冻结输入 |
| `run_event` | Run 内有序、可回放事件 |
| `idempotency_record` | 创建 Run 的 24 小时幂等窗口 |
| `secret_record` | AES-GCM 密文、封装 DEK、主密钥版本和安全元数据 |
| `run_item_result` | Item 最终结果、尝试数、Provider 摘要和完成时间 |
| `run_command` | 幂等运行命令、处理状态和确认结果 |
| `artifact_record` | Artifact 状态、文件定位、校验值、保留期和保护标记 |
| `agent_registration` | Agent 会话、能力、容量、双向 Sequence 和心跳状态 |
| `agent_lease` | Run/Agent 会话归属、Epoch、Fencing Token、Event Cursor 和租约状态 |
| `flyway_schema_history` | 数据库迁移历史 |

## 4. 已验证行为

- Maven 全模块编译、单元测试、架构测试和集成测试。
- Flyway 从空 SQLite 数据库创建所有表和默认工作区。
- HTTP API 创建 Account → Message → Audience → Job → Dry Run。
- Run 首次创建返回 `202`，相同请求幂等重放返回同一 Run，不同请求复用 Key 返回 `409`。
- 内嵌 Engine 把 Dry Run 推进到 `SUCCEEDED`，结果计数满足总量不变量。
- SSE 从 `RUN_CREATED` 开始回放事件。
- Secret 密文不含明文、记录替换递增版本、主密钥文件为 `0600`，丢失主密钥且已有密文时启动失败。
- 两条 Dry Run Item Result 可按 HMAC 游标分页；修改游标返回 `400`。
- REST 命令完成 `RUNNING → PAUSED → RUNNING → SUCCEEDED`，动态并发和命令重放通过验证。
- 终态 Run 结果 CSV 流式生成、重复请求复用、SHA-256、完整/Range 下载以及过期清理通过验证。
- 本地 Artifact 文件路径穿越防护、原子写入、`0600` 权限和幂等删除通过单元测试。
- pnpm 类型检查、Vitest、Vite Web 构建和 Electron TypeScript 构建。
- 真实 gRPC 双向流完成 Token 认证、Hello/Welcome、Heartbeat、HTTP 查询 ONLINE，以及流关闭后 OFFLINE 的端到端验证。
- Protobuf 与领域帧双向映射、Agent 文件 Journal 原子持久化通过单元测试。
- Agent 远端适配器通过 Execution Spec/Audience 下载与哈希校验，使用真实 Core + HTTP Provider 完成两条 Dry Run 并回传结果。
- Service 远端 gRPC 集成覆盖 Lease Ack、受保护文档、命令投递、Event Ack、重复批次去重、Item Result 落库和 Run Summary 终态收敛。

## 5. 下一阶段边界

以下能力仍按详细设计继续推进，不应把当前初版误认为最终实现：

- OS Keychain/KMS 主密钥适配；当前默认实现是独立受保护密钥文件或显式环境注入。
- Server 模式的 S3-compatible Artifact Store、Presigned/Multipart 上传，以及完整 Provider 响应体的 7 天策略。
- Message、Audience 和 Job 的编辑、修订对比、CSV 导入和正式发送确认体验。
- API 文档页面的通用 POST/PUT/PATCH 请求编辑器。
- Agent Event Outbox 的磁盘持久化、断点重传和进程重启后的运行恢复；当前 Event Cursor 在 Service 持久化，Agent 运行期按序发送。
- Agent 加密 Secret Envelope、Artifact Presigned 上传与完整性提交；当前远端模式只允许无需 Secret/Artifact 的执行配置。
- Agent 正式 Enrollment、证书轮换与 mTLS；当前 Token 只作为本地/Bootstrap 安全机制。
- PostgreSQL Server 模式、多实例调度、Outbox 和高可用。
- 身份认证、RBAC、审计事件及正式多 Workspace 管理 API。
- Provider 插件目录监听、子进程隔离、签名校验和热切换。
