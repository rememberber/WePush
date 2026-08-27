# WePush Next Changelog

本文件仅记录 `next/` 产品线；Classic 继续独立演进。

## Unreleased

### Added

- 新增 Embedded Java SDK：Java 应用可在本进程显式装配 Core Engine 和 Provider，无需启动 Service、Agent 或数据库。
- 新增共享/按 Run 创建的 Result、Event、Artifact Sink 生命周期模型，以及测试/小任务使用的 `InMemoryExecutionStore`。
- 发行归档和独立 Java SDK 包开始携带 Embedded SDK、Core、Provider SPI、Engine 与 HTTP Provider 的 POM/JAR。

### Changed

- HTTP Provider 纵向集成测试改为通过 Embedded SDK 公开入口执行，架构测试新增 Embedded SDK 边界约束。
- 明确 WePush Next 以用户自部署为长期产品定位，新增产品边界与分版本路线图，并将公共 SaaS、计费订阅、云 KMS/Secret Manager 和恶意公共租户物理隔离列为长期非目标。

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
