# Security Policy

## Supported versions

| Version | Support status |
| --- | --- |
| `1.0.x` | 当前稳定线，接收安全修复 |
| `0.1.0-beta.1` 及更早版本 | 不再支持，请升级到 `1.0.0` |
| Classic versions | 由 Classic 产品线的发布与维护策略决定 |

`1.x` 安全修复遵守[兼容性策略](docs/compatibility-policy.md)。如修复无法在兼容边界内完成，维护者会记录风险、迁移和临时缓解措施。

## Reporting a vulnerability

请不要为尚未修复的漏洞创建公开 Issue、Discussion 或 Pull Request。优先通过 GitHub 仓库的 **Security → Report a vulnerability** 私下提交报告：

<https://github.com/rememberber/WePush/security/advisories/new>

报告中请包含受影响版本、复现条件、影响范围、最小复现步骤或 PoC，以及可安全联系你的方式。请移除真实 Token、Secret、证书、用户数据和第三方凭据。

维护者会尽力确认报告、评估影响并协调披露时间；开源项目不承诺固定响应 SLA。修复发布前，请给予维护者合理时间完成验证和用户通知。
