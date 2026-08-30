# WePush Next Changelog

本文件仅记录 `next/` 产品线；Classic 继续独立演进。

## Unreleased

## 1.1.0 — 2026-08-30

### Added

- 新增 CMPP、SMGP、SGIP、SMPP 四个源码独立、Ed25519 签名的运营商短信 Provider 插件，包含协议编解码、长短信分片、本地模拟网关和连接/发送测试。
- 新增 Workspace 级 Agent 数量、活动 Run、总发送并发、Artifact 容量与默认保留期策略，并同步 REST API、Java SDK、审计和 WebUI 管理入口。
- 新增脱敏诊断包、用户手动触发的版本检查，以及 Nginx、Traefik 和 Kubernetes 自建部署模板；版本检查默认不后台运行且不发送遥测。
- 新增跨 Run 的账号认证失败熔断，支持冷却后自动恢复、管理员查看与复位。
- 新增 PostgreSQL `LISTEN/NOTIFY` 低延迟唤醒，用于待调度 Run、Agent Outbox 和 SSE Run Event；数据库轮询与持久 Outbox 继续作为正确性兜底。
- 新增 Agent Presigned Multipart Artifact 上传计划、分批 Part URL、Complete/Abort、进程失败清理和 S3 完整性校验，单 Artifact 上限扩展至 5 TiB。
- 新增亮色/暗色/跟随系统主题、低分辨率布局、键盘焦点、语义标签、颜色对比和减少动画支持。

### Changed

- 数据库由 V14 前向迁移到 V17：V15 增加 Workspace 资源策略，V16 增加账号认证熔断，V17 增加 Artifact Multipart 会话；迁移均为附加式变更。
- Agent 大文件上传在保留原单次 Presigned Put 契约的同时按大小自动选择 Multipart；Part URL 最多按 100 个一批签发，S3 10,000 Part 上限保持显式校验。
- Public Preview Notice 归档为历史预览版说明；路线图、详细设计、部署、用户、兼容和升级文档同步到 `1.1.0`。

### Fixed

- 运营商插件构建统一固定 Netty `4.1.x` 依赖线，避免 BOM 覆盖造成运行时混用不兼容版本。
- 插件发行包拒绝捆绑 Core API、Provider SPI、PF4J 和日志 API，只携带插件私有实现及运行时协议依赖。

### Security

- 诊断包对 Token、Secret、Recipient、Provider 响应和敏感配置执行结构化脱敏；版本检查必须由用户主动触发。
- 运营商插件发布流水线缺少签名私钥、公钥或 Key ID 时失败关闭，并在上传 Release Asset 前由 Agent 生产校验器重新验签。

## 1.0.0 — 2026-08-29

### Added

- 发布 1.x REST/SSE、Java SDK、Provider SPI、配置、数据库迁移和 Agent 协议兼容策略。
- 新增 V13（`0.1.0-beta.1`）到 V14 的 SQLite/PostgreSQL 无损迁移门禁，以及 Beta 成功升级和最低回滚版本记录。
- 新增 100,000 Recipient 流式执行、三平台默认卸载/显式 Purge 和稳定版发行门禁。

### Changed

- GitHub Tag 流水线对无后缀版本发布正式 Release，不再标记为 prerelease。
- macOS Upgrade 同时接受 Release 使用的 `.zip` 和已有 `.tar.gz` 归档。
- 将 macOS/Windows 不使用商业代码签名确认为长期发行边界，使用 GitHub Release、统一 SHA-256 和 SBOM 验证来源与完整性；Provider 插件仍强制 Ed25519 签名。

### Fixed

- Agent Event/Completion Outbox 在磁盘写入失败时不再提前提交内存变更，避免事件序号跳跃或误删未确认 Completion。
- Linux、macOS、Windows 卸载脚本遵循自定义安装/配置/数据目录；默认保留数据，只有显式 Purge 才删除。
- Linux、macOS、Windows 连续备份使用唯一归档名，避免同一秒内自动升级和人工备份发生文件名碰撞。

## 0.1.0-beta.1 — 2026-08-28

### Added

- 三平台统一 Standalone 安装入口、`jlink` Java Runtime 完整包和系统 Java 精简包；Windows 离线携带固定摘要的 WinSW。
- 正式 Backup/Restore、逐文件内容校验、恢复前副本、Installation Health，以及升级失败自动切回旧版本和数据。
- Desktop 本机 Service 检测/启停/日志/诊断、原生安全 Token 存储和签名 Provider 插件 Stage/Activate/Rollback。
- Playwright 浏览器 E2E、三平台 Desktop 冒烟、安装/恢复/失败升级自测和 Java 21/25 × SQLite/PostgreSQL 长稳矩阵。

### Changed

- 浏览器 Token 从持久 `localStorage` 收口为标签页 `sessionStorage`；Desktop 在系统安全存储不可用时拒绝弱持久化。
- Provider 插件 Stage 先复用 Agent 生产校验器，并按清单 `pluginId` 生成稳定激活文件名。
- Service 新增 Installation Health 组，同时检查 Flyway 当前版本和无网络 Provider Dry Run。

### Fixed

- 清理未来 JDK SQLite Native Access 警告、Agent Shade 重复模块/签名/许可证资源和基础设施测试日志依赖缺口。

## 0.1.0-alpha.4 — 2026-08-28

### Added

- 新增独立 `provider-standard` 模块，内置 SMTP Email、飞书/钉钉/企业微信群机器人、阿里云短信、微信公众号、小程序和企业微信应用消息，共 8 个真实渠道 Provider。
- 全部标准渠道提供 Account/Message/Recipient JSON Schema、SecretRef、Dry Run、实际发送、结构化错误分类和最小 WebUI 示例。
- 新增本地 SMTP/HTTP mock 契约测试，覆盖 SMTP Multipart、机器人签名与限流、阿里云 POP 签名、微信 Token 缓存及失效后单次刷新。
- 新增标准渠道 Engine 纵向测试、Service Provider Catalog 冒烟和 Agent 内置 Provider 发现测试。
- 新增《内置 Provider 指南》，记录账号、消息、Recipient、幂等、限流、重试、未知结果和 Provider Code 语义。

### Changed

- Service 与 Agent 随发行包装载全部 9 个内置 Provider；Embedded SDK 发行附件增加 `provider-standard` POM/JAR，仍由调用方显式注册所需 Provider。
- WebUI Schema Renderer 支持本地 `$ref` 和嵌套 SecretRef 默认值；Message Schema 提供可直接修改的渠道示例。
- 机器人生产请求限制到厂商官方 Webhook，阿里云和微信系 API 固定官方端点；测试端点仅对包内回环 mock 开放。

### Security

- Dry Run 不解析渠道 Secret、不获取 Access Token 且不访问网络；诊断不包含 Secret、完整远端响应或带 Token URL。
- 提交后超时、I/O 中断或远端 `5xx` 统一保守标记为 `UNKNOWN`，不自动重发可能已经送达的消息。

## 0.1.0-alpha.3 — 2026-08-27

### Added

- Account 编辑、连接测试、启停与归档；Message 新 Revision、历史、Diff、复制；Job 编辑、复制、启停与归档；Schedule 完整编辑。
- CSV/TXT 流式 Audience 导入、字段映射、预览、数据库去重、错误行 CSV 下载，以及创建或更新不可变 Snapshot。
- 正式发送影响预览和五分钟确认令牌；FAILED、UNKNOWN、UNSENT Item 可创建保留来源与冻结快照的新 Run。
- Run/资源/Schedule/审计签名游标分页、名称/状态/时间筛选，以及真实 Run 总览、活动列表、最近运行和 14 天趋势。
- Remote Java SDK 与 TypeScript Client 同步覆盖资源生命周期、分页、受众文件上传、发送确认、重发和总览。

### Changed

- WebUI 形成文件导入 → Dry Run → 正式确认 → 结果 → 失败重发闭环；Standalone 隐藏 Workspace 选择，用户自建 Server 可切换真实 Workspace。
- Message 和 Audience 内容更新创建不可变版本；已创建 Run 始终使用冻结 Snapshot。
- 启动恢复改为分批扫描待恢复 Run，避免随历史规模增长而一次性载入。

### Fixed

- 资源与审计列表契约统一为 `{items,page}`，Cursor 使用 HMAC 并绑定当前筛选条件，篡改或跨筛选复用会返回明确错误。

## 0.1.0-alpha.2 — 2026-08-27

### Added

- 新增 Embedded Java SDK：Java 应用可在本进程显式装配 Core Engine 和 Provider，无需启动 Service、Agent 或数据库。
- 新增共享/按 Run 创建的 Result、Event、Artifact Sink 生命周期模型，以及测试/小任务使用的 `InMemoryExecutionStore`。
- 发行归档和独立 Java SDK 包开始携带 Embedded SDK、Core、Provider SPI、Engine 与 HTTP Provider 的 POM/JAR。

### Changed

- HTTP Provider 纵向集成测试改为通过 Embedded SDK 公开入口执行，架构测试新增 Embedded SDK 边界约束。
- 明确 WePush Next 以用户自部署为长期产品定位，新增产品边界与分版本路线图，并将公共 SaaS、计费订阅、云 KMS/Secret Manager 和恶意公共租户物理隔离列为长期非目标。
- S3-compatible Artifact Store 的服务端加密配置收口为 `AUTO`/`AES256`/`NONE`，移除 AWS KMS 配置和请求路径。

### Fixed

- WebUI 下载 Artifact 时改用携带 Bearer Token 的认证请求，安全模式下可正常下载结果导出。
- macOS Desktop 打包会正确重命名主可执文件、保留 Electron Asar 完整性元数据，并写入 WePush 产品版本。三平台发布工作流新增打包后版本校验。

## 0.1.0-alpha.1 — 2026-08-24

首个公开预览版本。

### Added

- 独立 Core API、Provider SPI、虚拟线程执行引擎和 HTTP Provider。
- 常驻 Agent、gRPC 双向控制流、Enrollment/mTLS、Lease/Fencing、断线恢复和 Secret Envelope。
- 可安装 Service、SQLite Standalone、PostgreSQL Server/HA 基线、Local/S3 Artifact Store 和分层控制面 API。
- 独立 Java SDK、OpenAPI 文档与 TypeScript API Client。
- React WebUI 的可视化配置、运行中心、动态 API 文档与 Electron Desktop 外壳。
- Linux、macOS、Windows 安装/升级/卸载脚本以及可复现的服务端分发压缩包。
- CI 中的 SQLite、PostgreSQL、S3、架构、纵向集成、WebUI 和三平台 Desktop 门禁。

### Preview limitations

- Desktop 和安装包未使用商业代码签名或 Apple Notarization。
- 本版本不承诺 API 或数据迁移兼容性，不建议关键生产业务使用。
- 升级仍由用户手动下载并执行；本版本尚未提供品牌化安装器和稳定版运维承诺，不建设强制或托管自动更新渠道。
