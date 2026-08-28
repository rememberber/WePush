# WePush Next Changelog

本文件仅记录 `next/` 产品线；Classic 继续独立演进。

## Unreleased

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
