# WePush Next 实现状态

更新时间：2026-08-22

## 1. 当前里程碑

`next/` 已完成第一条可运行的 Standalone 纵向链路：

```text
React / Electron / Java SDK
          ↓ HTTP + SSE
Spring Boot Service API
          ↓ 应用事务
SQLite + Flyway
          ↓ Run Snapshot
Embedded Core Engine
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
- Message Revision、Audience Snapshot、Run Snapshot 创建后不可变。
- 应用服务显式持有事务边界，Repository 不自行提交。

### 2.3 公开 API

- Provider 发现及 Schema API。
- Account、Message、Audience、Job 的创建、列表和详情 API。
- Run 幂等创建、列表和详情 API。
- `Idempotency-Key` 相同且请求相同返回原 Run；相同 Key 对应不同请求返回 `409`。
- Run SSE 事件流支持持久化回放及实时推送，事件 ID 是 Run 内单调递增序号。
- Secret 写入/元数据 API；读取接口永不返回明文。
- Run Item Result 分页 API，以及暂停、恢复、取消和动态并发命令 API。
- 统一 `application/problem+json` 错误响应和稳定错误码。
- OpenAPI 3.1 契约覆盖当前公开控制面。

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
- Java SDK 已增加 Workspace 控制面、Secret、结果分页、幂等 Run 创建和运行命令客户端。
- TypeScript API Client 已覆盖 Account、Message、Audience、Job、Run、Secret、结果和命令。
- React WebUI 与 Electron 共享 Feature/UI 包。
- Provider Schema 可视化配置可以直接保存 Account。
- 账号页读取真实持久化数据。
- 消息、受众和 Job 页面均接入真实创建/列表 API，Job 可直接发起 Dry Run。
- 运行中心轮询 Run 状态，使用 SSE 展示持久化/实时事件，并显示 Item Result 和实时命令控制条。
- API 文档页加载当前 OpenAPI，并可动态调试主要 GET API。

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
- pnpm 类型检查、Vitest、Vite Web 构建和 Electron TypeScript 构建。

## 5. 下一阶段边界

以下能力仍按详细设计继续推进，不应把当前初版误认为最终实现：

- OS Keychain/KMS 主密钥适配；当前默认实现是独立受保护密钥文件或显式环境注入。
- Artifact 元数据、对象/文件存储、完整响应体策略及保留任务。
- Message、Audience 和 Job 的编辑、修订对比、CSV 导入和正式发送确认体验。
- API 文档页面的通用 POST/PUT/PATCH 请求编辑器。
- Agent gRPC 控制流、租约与远端执行；当前执行器是 Standalone Embedded 模式。
- PostgreSQL Server 模式、多实例调度、Outbox 和高可用。
- 身份认证、RBAC、审计事件及正式多 Workspace 管理 API。
- Provider 插件目录监听、子进程隔离、签名校验和热切换。
