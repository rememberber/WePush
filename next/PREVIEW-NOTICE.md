# WePush Next 历史预览版说明

> 本文件仅为 `0.1.0-alpha.*` 和 `0.1.0-beta.1` 历史发行保留。WePush Next 已于 `1.0.0` 进入稳定版，当前稳定版为 `1.1.0`；安装、安全与兼容边界以 [`README.md`](README.md)、[`UNSIGNED-NOTICE.md`](UNSIGNED-NOTICE.md)、[《兼容性策略》](docs/compatibility-policy.md)和对应 Release Notes 为准。

以下内容只适用于历史预览版，不代表稳定版仍处于预览状态。

## 历史预览版的未签名发行说明

本预览版的桌面应用和安装包未使用商业代码签名证书：

- macOS 应用仅进行 ad-hoc 签名，未使用 Apple Developer ID，也未经过 Apple Notarization；Gatekeeper 可能阻止首次启动。
- Windows 应用未使用 Authenticode 签名，SmartScreen 或终端安全软件可能显示未知发布者警告。
- Linux 压缩包未提供发行版仓库签名。

请只从项目的 GitHub Releases 页面下载，并在安装前使用同一 Release 中的 `SHA256SUMS` 校验文件完整性。不要通过关闭系统安全能力来绕过来源不明的副本。

## 历史预览期边界

- API、配置项、数据结构和升级路径仍可能在后续预览版发生不兼容变化。
- `beta.1` 的 SMTP、企业 IM、短信和微信 Provider 会在正式 Run 中调用用户配置的第三方账号，并可能消耗渠道额度或产生第三方费用。务必先用自有测试目标完成 Dry Run 和小范围验证。
- 第三方渠道的模板审核、关注/订阅授权、IP 白名单、账号余额、频率与地区规则由用户和渠道厂商负责；连接测试成功不代表正式消息一定可送达。
- 请先备份数据库、Secret 主密钥、Agent 身份文件、插件与 Artifact，再执行升级或卸载。
- 跨主机部署必须启用认证和 TLS；默认回环地址与明文 gRPC 仅用于本机体验。
- 多节点 Server/HA、外部 PostgreSQL 和 S3 需要运维人员按部署文档配置，不属于开箱即用的桌面体验。
- 发现安全问题时不要提交公开 Issue，请按 `SECURITY.md` 私下报告。

历史预览版按 MIT License 原样提供，不附带任何明示或默示担保。新安装应使用稳定 Release，不应再从预览版开始部署。
