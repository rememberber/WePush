# WePush Next Changelog

本文件仅记录 `next/` 产品线；Classic 继续独立演进。

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
- 自动更新渠道、品牌化安装器和稳定版运维承诺尚未建立。
