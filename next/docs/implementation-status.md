# WePush Next 实现状态

更新时间：2026-08-30

产品范围和后续优先级以[《产品目标、边界与路线图》](product-scope-and-roadmap.md)为准。Next 的长期定位是用户自行下载、安装、部署和运维；不建设官方公共 SaaS、注册计费订阅、云 KMS/Secret Manager 或恶意公共租户物理隔离。

## 1. 里程碑结论

`next/` 的当前稳定基线为 `1.1.0`：在 `1.0.0` 的真实消息渠道、运维闭环和 1.x 兼容承诺上，增加运营商签名插件、Workspace 资源治理、脱敏诊断、跨 Run 认证熔断、PostgreSQL 低延迟唤醒、Agent Presigned Multipart 与 WebUI 主题/可访问性。Classic 源码和构建保持不动；两条产品线不共享源码依赖，允许各自存在相似实现。

```text
React WebUI / Electron Desktop / Remote Java SDK
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

业务 Java 应用 → Embedded Java SDK → Core Engine → Provider SPI
```

Standalone 默认是单 Service + SQLite + Local Artifact + Embedded Engine；Server 默认模型是 PostgreSQL 18 + S3-compatible Store + 多 Service + Remote Agent。PostgreSQL 和 S3 不是使用 Next 的前置条件，只是 Server/HA 形态的共享事实源。

## 2. 组件完成状态

| 组件 | 当前基线 |
|---|---|
| Core | Framework-free API、虚拟线程 Engine、并发/限速/重试/暂停/取消、流式 Result/Event/Artifact 端口 |
| Provider | 独立 SPI；内置 HTTP、SMTP、飞书/钉钉/企微机器人、阿里云短信、微信系渠道；CMPP/SMGP/SGIP/SMPP 独立 Ed25519 签名插件；Schema、SecretRef、Dry Run、错误分类与端点约束 |
| Agent | gRPC 双向流、Sequence/Fence Journal、磁盘 Event/Completion Outbox、重连恢复、Secret Envelope、Presigned Put/Multipart Artifact、Enrollment/轮换、TLS/mTLS |
| Service | Spring Boot 4.1.1、SQLite/PostgreSQL、资源修订/分页、Workspace Policy、认证熔断、脱敏诊断、手动版本检查、Local/S3 Artifact、RBAC/审计/Scheduler、Agent HA Outbox、PostgreSQL 通知与跨实例 SSE 补偿 |
| Remote Java SDK | 只依赖公开 `service-api`；覆盖 System、Provider、Agent、Workspace Policy、资源生命周期/分页、Audience 上传、Run、Artifact、Schedule、Security、诊断、版本检查与认证熔断 |
| Embedded Java SDK | Framework-free 进程内 Engine 门面；显式 Provider、SecretResolver、Result/Event/Artifact Sink，支持列表或流式 Recipient 与完整 RunHandle 控制 |
| WebUI | TypeScript/Vite/React；资源编辑/修订、导入/确认/重发、总览/分页/调度、Security、Workspace Policy、诊断/版本/熔断管理、动态 Schema、API 调试、亮暗主题、低分辨率与可访问性 |
| Desktop | Electron 安全外壳，共用 WebUI；本机 Service 检测/启停/日志/诊断、系统原生 API Token 安全存储、签名插件生命周期；目标系统原生目录打包，不依赖 Core 或 Service 内部实现 |
| Distribution | 系统 Java 精简包 + 三平台 `jlink` Runtime 完整包；统一 Standalone/高级分组件安装、离线 WinSW、正式备份/恢复、Beta 升级、升级健康门、自动回退与安全卸载；容器 Server/HA 拓扑 |

## 3. Core、Provider 与 Agent

- Core API 不依赖 Spring、数据库、HTTP Server、PF4J 或 UI；Service 和 Agent 通过端口适配同一个 Engine。
- 内置标准渠道位于独立 `provider-standard` 模块，通过 Provider SPI 与 ServiceLoader 接入 Service/Agent；Embedded 应用按需要显式声明并注册它，不把渠道依赖强加给 Engine。
- SMTP 使用 Angus Mail；机器人、短信和微信系使用 JDK HttpClient。生产实现固定官方端点或严格校验用户提供的官方 Webhook，本地 mock 入口只对同包测试可见。
- 微信系 Access Token 按 Session 缓存并提前刷新；只有远端明确拒绝 Token 时安全重放一次。提交后超时、I/O 或 `5xx` 均按 `UNKNOWN` 处理，不假定外部渠道 Exactly Once。
- Run Snapshot 固定 Provider ID/实现版本，调度只选择上报精确兼容版本且有容量的 Agent。
- PF4J 只位于 Agent App 边界。每个插件独立 ClassLoader，Provider SPI/Core API/日志 API 由 Parent 提供；这解决依赖冲突，不宣称是恶意代码沙箱。
- 正式插件包必须包含 SHA-256 清单和 Ed25519 签名。未知发布者、清单篡改、Zip Slip、压缩炸弹、捆绑共享 API 或重复 Provider 版本都会失败关闭。
- CMPP、SMGP、SGIP、SMPP 分别以独立插件发布，共享代码只存在 Next 内部的 `provider-carrier-common` 构建模块；插件没有 Classic 源码或构建依赖，Release 流水线在上传前用 Agent 生产校验器重新验签。
- 插件更新采用 Stage → 原子替换 → Supervisor Restart → Health Verify；失败自动恢复旧包。运行中不热卸载 ClassLoader。
- Agent 持久化 Lease Fence、双向 Sequence、Event Batch 与 Run Completion。Service ACK 前数据不会从 Outbox 删除；重连后按 Fence/Sequence 重放。
- 同一权威 Lease Offer 重放只推进 Service Sequence，不重复启动 Run。进程重启发现曾处于 RUNNING 的 Lease 时按 Unknown/Unsent 安全语义生成可恢复 Completion，而不假定外部渠道 Exactly Once。
- Secret 只扫描冻结 Account/Message 中实际引用的 `SecretRef`；Service 使用一次性 X25519 + HKDF-SHA-256 + AES-256-GCM 生成绑定 Agent/Run/Lease/Epoch/Fence/过期时间的 Envelope。Agent 明文只存在运行内存并主动清零。
- Agent 的 Core `ArtifactSink` 先写 owner-only 临时文件并计算大小/SHA-256，再获取 Lease 绑定的上传计划；不超过 1 GiB 使用单次 Put，更大文件使用可重试 Presigned Multipart，Complete/Abort 后再 Commit；READY Artifact 引用进入 `RunCompleted`。
- Enrollment Token 一次性消费并绑定 Workspace。Agent 生成 P-256 私钥，保存长期 Credential、客户端证书和 CA，在到期窗口内自动轮换。匿名 Agent 仅允许回环开发；非回环 HTTP/gRPC 必须认证，gRPC 强制 TLS，生产要求 mTLS。

## 4. Service、HA、安全与数据

- SQLite 开启 Foreign Key、WAL、Busy Timeout 和原子 Flyway 迁移；V10/V11 收口 Enrollment Workspace 和系统管理员，V12/V13 增加资源生命周期、关联重发和流式 Audience Import，并由迁移测试验证实际约束。
- PostgreSQL 使用 Hikari；相同有序迁移由 PostgreSQL 18 CI 实库验证。Server 模式启动校验 PostgreSQL、S3、API Security 和 Agent TLS 均已启用。
- Schedule Scanner 使用 PostgreSQL Session Advisory Lock 单 Leader；触发 Run 仍使用持久幂等键。
- Lease Offer 和 Run Command 先写 `agent_message_outbox`。任意 Service 扫描待发送消息，只有持有 Agent 当前 gRPC 流的实例实际投递，Agent ACK 后关闭消息。
- SSE 历史从 `run_event` 回放；本实例实时发布，同时每个实例轮询有本地订阅者的游标，补偿其他实例提交的事件。
- PostgreSQL Server/HA 对新 Run、Agent Outbox 和 Run Event 使用三个固定 `LISTEN/NOTIFY` Channel 加速唤醒；通知丢失、断线或重复时仍由周期扫描、持久 Outbox 和事件游标保证推进。
- Workspace Policy 限制 Agent 数量、活动 Run、总发送并发、Artifact 总量和默认保留期；账号认证失败在 V16 事实表中跨 Run 累积并熔断，冷却到期或管理员复位后恢复。
- API Token 只保存 SHA-256，明文仅签发时返回一次。VIEWER/OPERATOR/ADMIN 按 Workspace 授权；Bootstrap `SYSTEM_ADMIN` 只承担全局治理。启用安全时按系统管理员是否存在而非“任意 Token 是否存在”初始化 Bootstrap，避免先创建普通 Token 后锁死全局治理。Token 与 Enrollment 管理 API 进入 Workspace 路径，普通 ADMIN 无法跨 Workspace 签发、枚举或吊销；写操作和拒绝结果进入审计日志。
- 无认证开发模式只能绑定回环地址；HTTP 对外监听强制 API Security，Server 模式进一步强制 PostgreSQL、S3 与 Agent gRPC TLS，误配置时失败关闭。
- Workspace 有正式列表/创建/详情 API。Account、Secret、Message、Audience、Job、Schedule、Run、Artifact、API Token 与 Agent Enrollment Binding 均显式带 Workspace 边界；旧的未绑定开发 Agent 只兼容 `ws_default`。
- 默认且正式的 Secret Store 为 Local Envelope：随机 DEK + AES-256-GCM + 主密钥封装。主密钥来自 owner-only 文件或显式注入；密文存在而主密钥丢失/权限不安全时失败关闭。`SecretStore` Port 用于模块隔离和测试，官方产品不规划 OS Keychain、Vault、云 KMS 或 Secret Manager 形式的 Service Adapter。
- Local Artifact 采用受控路径、临时写、fsync、SHA-256 与原子移动。S3 Store 支持 Put/Get/Range/Head/Delete、Presigned Put、服务端与 Agent Multipart、Complete/Abort、原生 AES256 和对象元数据校验；不包含云 KMS 集成。
- Artifact 状态是 `UPLOADING → READY → DELETING → DELETED`，失败进入 `FAILED`；TTL 清理尊重 Pin/Legal Hold。对象存储 Lifecycle 是兜底，不替代数据库事实源。
- Actuator 暴露 Health/Readiness/Prometheus，指标带稳定 application 标签；Server HAProxy 对 Readiness 做后端摘除。

## 5. API、SDK 与 UI

- OpenAPI 3.1 覆盖资源编辑/分页、Audience Import、发送确认/重发/总览、Schedule/Security/Workspace 与 Agent Internal Enrollment/Lease/Artifact API；全局 Bearer Security 和内部自定义认证显式声明。
- Maven 契约测试拒绝 YAML 重复键、重复/缺失 `operationId`、无 Response 和不可解析的本地 `$ref`。
- Remote Java SDK 只有 `service-api` 依赖，绝不依赖 Core/Engine/Provider；支持签名分页、资源编辑、Schedule CRUD、发送确认/重发、总览和使用文件 BodyPublisher 的流式 multipart Audience 上传。
- Embedded Java SDK 依赖 Core API、Provider SPI 和 Engine，但不依赖 Service、Agent、Spring 或具体 Provider；应用显式注册允许的 Provider，并选择共享或按 Run 创建的 Sink。
- TypeScript Client 使用可更新 Bearer Token；SSE 使用自定义 Fetch Parser，因此 Server 安全模式不受原生 `EventSource` 无法设置 Authorization Header 的限制。
- WebUI 接入 Account/Message/Audience/Job/Run/Artifact 全链路、资源编辑/复制/状态、修订历史/Diff、CSV/TXT 导入、Schedule 完整编辑、发送确认/重发、Workspace 选择、API Token、Agent Enrollment、审计与动态 API 调试；Schema Renderer 支持本地 `$ref` 和嵌套 SecretRef 默认示例。
- Desktop 主进程保持 `contextIsolation=true`、`nodeIntegration=false`，只暴露固定 IPC：本机 Service 状态/启停/日志/诊断、签名插件 Stage/Activate/Rollback 和 API Token `safeStorage`。渲染页不能执行任意命令；浏览器 Token 仅使用 `sessionStorage`。

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
| `audience_import_session` / `audience_import_row` | 流式文件导入会话、预览计数、数据库去重和错误行 |
| `run_retry_item` | 关联重发 Run 的精确 Item 集合；来源与状态同时保存在 `run_instance` |
| `workspace_policy` | Workspace 的 Agent、Run、发送并发、Artifact 容量和默认保留期策略 |
| `account_auth_circuit` | 账号跨 Run 的认证失败计数、熔断状态、冷却时间和管理员复位状态 |
| `artifact_multipart_upload` | Agent Presigned Multipart 会话、大小、摘要、Part 规划、到期和状态 |

## 7. 验证与发布门禁

本地/常规门禁：

- `./mvnw verify`：Core、Provider、Agent、Service、SDK、架构和纵向集成测试。
- `pnpm check`：全部 Workspace TypeScript、Vitest、Web Vite 构建与 Desktop TypeScript 构建。
- 标准渠道测试使用本地 SMTP/HTTP mock 验证真实协议、签名、错误映射、Token 缓存/刷新和 Dry Run；Engine 纵向测试逐一执行全部 8 个标准渠道。
- SQLite 空库迁移断言真实列和 Foreign Key，而不只相信 Flyway 版本号。
- 真实 gRPC 纵向链路覆盖 Hello/Welcome、Lease Ack、受保护文档、Secret、Command Ack、Event 去重、Agent Artifact 上传/Commit、Run Completion。
- 插件测试覆盖有效签名、未知签名者、Zip Slip 和空目录。
- 安装脚本通过 POSIX shell 语法、PowerShell AST、launchd plist、WinSW XML 与 Compose 配置校验；三平台自测实际覆盖备份内容摘要、完整恢复和强制升级失败回退。
- 浏览器 Playwright E2E 覆盖 Standalone/Provider/会话 Token；三平台打包后启动 Desktop 冒烟；定时矩阵在 Java 21/25、SQLite/PostgreSQL 上执行长稳健康检查。
- 发行归档生成系统 Java 精简包和含 Runtime 的平台完整包，并检查离线 WinSW、Service/Agent/WebUI/安装/恢复脚本及 Remote/Embedded Java SDK 均存在。

`.github/workflows/next-ci.yml` 另外使用真实 PostgreSQL 18 和固定 MinIO 版本验证：

- PostgreSQL 从 V13 用户数据迁移到 V17，并验证 Workspace Policy、认证熔断、Multipart 表、控制面通知和已有数据；
- S3 Presigned Put、Presigned Multipart Complete/Abort、Range、Head/Checksum、Delete 和 100 MiB Service Multipart；
- Java/UI 门禁通过后才生成发行归档。
- `next-v*` Tag 由独立发行流水线生成三平台未签名 Desktop 包、CycloneDX SBOM 和统一 `SHA256SUMS`；无后缀版本发布为稳定 GitHub Release，带后缀版本才标记为 Pre-release。

## 8. 稳定版完成边界与后续演进

`1.1.0` 已在 `1.0.0` 稳定基线上完成兼容增量：公开 API 只增加端点/字段，V14→V17 为附加迁移，原单次 Artifact 上传和 Agent 协议 Major 1 保持兼容；Provider 插件、资源治理、诊断、可靠性、大 Artifact、UI 与发行资产均进入自动门禁。

以下属于 1.x 内可兼容增加的产品增量，不是稳定版缺口：

- 以独立签名插件继续增加其他短信、推送和运营商协议，不扩大内置核心依赖。
- 在兼容策略范围内增加可选 API、运维诊断和用户自建部署模板。
- 更新始终由用户主动触发；macOS/Windows 应用保持未使用商业代码签名，并继续以 Release、SHA-256、SBOM 和明确告知提供完整性证据。

公共 SaaS、自助注册、计费订阅、公共市场、云 KMS/Secret Manager、恶意公共租户物理隔离、跨区域 Active-Active、强制自动更新和遥测回传属于长期非目标，不进入后续产品增量。

路线图内的增量继续遵守 Classic/Next 双轨独立、Remote Java SDK 不依赖 Core、Embedded Java SDK 不依赖 Service、Workspace 显式隔离、Lease Fencing、Secret 最小暴露和 Artifact 完整性边界。
