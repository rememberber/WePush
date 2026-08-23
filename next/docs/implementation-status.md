# WePush Next 实现状态

更新时间：2026-08-23

## 1. 里程碑结论

`next/` 的 `0.1.0` 目标架构基线已经形成完整、可独立构建的产品纵向链路。Classic 源码和构建保持不动；两条产品线不共享源码依赖，允许各自存在相似实现。

```text
React WebUI / Electron Desktop / Java SDK
                  │ REST + Bearer RBAC + SSE
                  ▼
       Spring Boot Service API（1..N 实例）
          │             │              │
   SQLite/PostgreSQL  Secret Store   Local/S3 Artifact
          │             │              ▲ Presigned/Commit
          └──── durable control ────────┘
                         │ gRPC 双向流
                         ▼
             Enrolled Remote Agent + PF4J
                         │
                    Core Engine
                         │
                   Provider SPI
```

Standalone 默认是单 Service + SQLite + Local Artifact + Embedded Engine；Server 默认模型是 PostgreSQL 18 + S3-compatible Store + 多 Service + Remote Agent。PostgreSQL 和 S3 不是使用 Next 的前置条件，只是 Server/HA 形态的共享事实源。

## 2. 组件完成状态

| 组件 | 当前基线 |
|---|---|
| Core | Framework-free API、虚拟线程 Engine、并发/限速/重试/暂停/取消、流式 Result/Event/Artifact 端口 |
| Provider | 独立 SPI、HTTP Provider、JSON Schema、SSRF/响应上限；PF4J 外部插件发现、Ed25519 签名、Zip Slip/共享包校验、受控滚动激活与失败回滚 |
| Agent | gRPC 双向流、Sequence/Fence Journal、磁盘 Event/Completion Outbox、重连恢复、Secret Envelope、远端 Artifact 上传、Enrollment/轮换、TLS/mTLS |
| Service | Spring Boot 4.1.1、分层应用服务、SQLite/PostgreSQL、Local/S3 Artifact、RBAC/审计/Scheduler、Agent HA outbox、跨实例 SSE 补偿 |
| Java SDK | 只依赖公开 `service-api`；覆盖 System、Provider、Agent、Workspace、资源、Run、Artifact、Schedule、Security |
| WebUI | TypeScript/Vite/React；可视化配置、任务/调度、运行监控、Bearer SSE、Token/Enrollment/审计、动态 API 调试文档 |
| Desktop | Electron 安全外壳，共用 WebUI；目标系统原生目录打包、相对 Framework 链接、macOS ad-hoc/Developer ID 签名入口，不依赖 Core 或 Service 内部实现 |
| Distribution | tar.gz/zip + 标准 SHA-256 校验；内含 WebUI；Linux systemd、macOS 非 root launchd、Windows LocalService/WinSW 安装/升级/备份/卸载；容器 Server/HA 拓扑 |

## 3. Core、Provider 与 Agent

- Core API 不依赖 Spring、数据库、HTTP Server、PF4J 或 UI；Service 和 Agent 通过端口适配同一个 Engine。
- Run Snapshot 固定 Provider ID/实现版本，调度只选择上报精确兼容版本且有容量的 Agent。
- PF4J 只位于 Agent App 边界。每个插件独立 ClassLoader，Provider SPI/Core API/日志 API 由 Parent 提供；这解决依赖冲突，不宣称是恶意代码沙箱。
- 正式插件包必须包含 SHA-256 清单和 Ed25519 签名。未知发布者、清单篡改、Zip Slip、压缩炸弹、捆绑共享 API 或重复 Provider 版本都会失败关闭。
- 插件更新采用 Stage → 原子替换 → Supervisor Restart → Health Verify；失败自动恢复旧包。运行中不热卸载 ClassLoader。
- Agent 持久化 Lease Fence、双向 Sequence、Event Batch 与 Run Completion。Service ACK 前数据不会从 Outbox 删除；重连后按 Fence/Sequence 重放。
- 同一权威 Lease Offer 重放只推进 Service Sequence，不重复启动 Run。进程重启发现曾处于 RUNNING 的 Lease 时按 Unknown/Unsent 安全语义生成可恢复 Completion，而不假定外部渠道 Exactly Once。
- Secret 只扫描冻结 Account/Message 中实际引用的 `SecretRef`；Service 使用一次性 X25519 + HKDF-SHA-256 + AES-256-GCM 生成绑定 Agent/Run/Lease/Epoch/Fence/过期时间的 Envelope。Agent 明文只存在运行内存并主动清零。
- Agent 的 Core `ArtifactSink` 先写 owner-only 临时文件并计算大小/SHA-256，再获取 Lease 绑定的上传计划，上传到 Service 或 S3 Presigned URL，最后 Commit；READY Artifact 引用进入 `RunCompleted`。
- Enrollment Token 一次性消费并绑定 Workspace。Agent 生成 P-256 私钥，保存长期 Credential、客户端证书和 CA，在到期窗口内自动轮换。匿名 Agent 仅允许回环开发；非回环 HTTP/gRPC 必须认证，gRPC 强制 TLS，生产要求 mTLS。

## 4. Service、HA、安全与数据

- SQLite 开启 Foreign Key、WAL、Busy Timeout 和原子 Flyway 迁移；V10 通过表重建正确增加带外键/非空默认语义的 Enrollment Workspace 列，V11 增加显式系统管理员角色，并由 PRAGMA 测试验证实际列和外键。
- PostgreSQL 使用 Hikari；相同有序迁移由 PostgreSQL 18 CI 实库验证。Server 模式启动校验 PostgreSQL、S3、API Security 和 Agent TLS 均已启用。
- Schedule Scanner 使用 PostgreSQL Session Advisory Lock 单 Leader；触发 Run 仍使用持久幂等键。
- Lease Offer 和 Run Command 先写 `agent_message_outbox`。任意 Service 扫描待发送消息，只有持有 Agent 当前 gRPC 流的实例实际投递，Agent ACK 后关闭消息。
- SSE 历史从 `run_event` 回放；本实例实时发布，同时每个实例轮询有本地订阅者的游标，补偿其他实例提交的事件。
- API Token 只保存 SHA-256，明文仅签发时返回一次。VIEWER/OPERATOR/ADMIN 按 Workspace 授权；Bootstrap `SYSTEM_ADMIN` 只承担全局治理。启用安全时按系统管理员是否存在而非“任意 Token 是否存在”初始化 Bootstrap，避免先创建普通 Token 后锁死全局治理。Token 与 Enrollment 管理 API 进入 Workspace 路径，普通 ADMIN 无法跨 Workspace 签发、枚举或吊销；写操作和拒绝结果进入审计日志。
- 无认证开发模式只能绑定回环地址；HTTP 对外监听强制 API Security，Server 模式进一步强制 PostgreSQL、S3 与 Agent gRPC TLS，误配置时失败关闭。
- Workspace 有正式列表/创建/详情 API。Account、Secret、Message、Audience、Job、Schedule、Run、Artifact、API Token 与 Agent Enrollment Binding 均显式带 Workspace 边界；旧的未绑定开发 Agent 只兼容 `ws_default`。
- 默认 Secret Store 为 Local Envelope：随机 DEK + AES-256-GCM + 主密钥封装。主密钥来自 owner-only 文件或显式注入；密文存在而主密钥丢失/权限不安全时失败关闭。OS Keychain、Vault/KMS 是可替换主密钥适配器，不影响默认方案成立。
- Local Artifact 采用受控路径、临时写、fsync、SHA-256 与原子移动。S3 Store 支持 Put/Get/Range/Head/Delete、Presigned Put、100 MiB Multipart、Abort、可选 AES256/AWS_KMS 和对象元数据校验。
- Artifact 状态是 `UPLOADING → READY → DELETING → DELETED`，失败进入 `FAILED`；TTL 清理尊重 Pin/Legal Hold。对象存储 Lifecycle 是兜底，不替代数据库事实源。
- Actuator 暴露 Health/Readiness/Prometheus，指标带稳定 application 标签；Server HAProxy 对 Readiness 做后端摘除。

## 5. API、SDK 与 UI

- OpenAPI 3.1 覆盖公开控制面、Schedule/Security/Workspace 与 Agent Internal Enrollment/Lease/Artifact API；全局 Bearer Security 和内部自定义认证显式声明。
- Maven 契约测试拒绝 YAML 重复键、重复/缺失 `operationId`、无 Response 和不可解析的本地 `$ref`。
- Java SDK 只有 `service-api` 依赖，绝不依赖 Core/Engine/Provider；新增 `SecurityClient`、`WorkspacesClient`、Schedule CRUD、Token 撤销、Enrollment Token 和通用 PATCH/DELETE Transport。
- TypeScript Client 使用可更新 Bearer Token；SSE 使用自定义 Fetch Parser，因此 Server 安全模式不受原生 `EventSource` 无法设置 Authorization Header 的限制。
- WebUI 接入 Account/Message/Audience/Job/Run/Artifact 全链路、Schedule 创建/启停、API Token 创建/列表/撤销、Agent Enrollment、审计查看与动态 GET/POST/PUT/PATCH/DELETE API 调试。
- Desktop 主进程保持 `contextIsolation=true`、`nodeIntegration=false`，开发加载 Vite，发行加载 `process.resourcesPath` 下的共享 WebUI。布局和视觉 Token 使用接近 Codex 客户端的紧凑侧栏、内容工作区、柔和边界和低噪声状态样式。

## 6. 当前数据库事实源

在原有 Workspace、Account、Message Revision、Audience Snapshot、Job、Run Snapshot/Event/Result/Command、Secret 和 Artifact 表之外，当前迁移还包含：

| 表 | 用途 |
|---|---|
| `agent_registration` | Agent 当前会话、平台、Provider、容量、Sequence、X25519 公钥 |
| `agent_lease` | Run/Agent 归属、Epoch、Fencing Token、Event Cursor 和恢复状态 |
| `agent_message_outbox` | 跨 Service 实例的 Lease Offer/Run Command 持久消息 |
| `agent_enrollment_token` | 一次性 Enrollment、Workspace、过期和消费状态 |
| `agent_credential` | 哈希长期 Credential、证书指纹、轮换/撤销/使用时间 |
| `agent_workspace_binding` | Agent 与可执行 Workspace 的正式绑定 |
| `api_principal` / `api_token` / `role_binding` | API 身份、Token 和 Workspace RBAC |
| `audit_event` | 控制面安全与变更审计 |
| `schedule_definition` | Cron、时区、Misfire、启停和下次触发时间 |

## 7. 验证与发布门禁

本地/常规门禁：

- `./mvnw verify`：Core、Provider、Agent、Service、SDK、架构和纵向集成测试。
- `pnpm check`：全部 Workspace TypeScript、Vitest、Web Vite 构建与 Desktop TypeScript 构建。
- SQLite 空库迁移断言真实列和 Foreign Key，而不只相信 Flyway 版本号。
- 真实 gRPC 纵向链路覆盖 Hello/Welcome、Lease Ack、受保护文档、Secret、Command Ack、Event 去重、Agent Artifact 上传/Commit、Run Completion。
- 插件测试覆盖有效签名、未知签名者、Zip Slip 和空目录。
- 安装脚本通过 POSIX shell 语法、launchd plist、WinSW XML 与 Compose 配置静态校验。
- 发行归档生成 tar.gz、zip 与 SHA-256，并检查 Service/Agent/WebUI/安装脚本均存在。

`.github/workflows/next-ci.yml` 另外使用真实 PostgreSQL 18 和固定 MinIO 版本验证：

- PostgreSQL 全量迁移、默认 Workspace、V10 Workspace 列、V11 系统角色列和 outbox 外键；
- S3 Presigned Put、Range、Head/Checksum、Delete 和 100 MiB Multipart；
- Java/UI 门禁通过后才生成发行归档。

## 8. 本轮完成边界与后续演进

本轮详细设计的阶段 A–D 基线已经实现并进入可验证状态：工程契约、Standalone 纵向闭环、Web/Desktop 产品壳、远程 Agent、安全、Artifact、Scheduler、三平台安装和 Server/HA 参考拓扑均已收口。部署、升级、备份、插件和故障验收见 [`deployment-and-operations.md`](deployment-and-operations.md)。

以下属于后续产品增量，不是本轮目标架构的未完成项：

- 增加邮件、短信、微信等正式 Provider，以及对应模拟服务和 Schema 组件。
- Message/Audience/Job 编辑、修订 Diff、大文件 CSV 导入和正式发送二次确认体验。
- macOS/Windows 商业发行签名、公证、自动更新渠道和品牌资产。
- 外部 Vault/云 KMS/OS Keychain 适配器，以及非受信 Provider 独立进程 Runner。
- 公共 SaaS 的自助注册、计费、订阅、恶意租户物理隔离和跨区域 Active-Active。

这些增量继续遵守 Classic/Next 双轨独立、Java SDK 不依赖 Core、Workspace 显式隔离、Lease Fencing、Secret 最小暴露和 Artifact 完整性边界。
