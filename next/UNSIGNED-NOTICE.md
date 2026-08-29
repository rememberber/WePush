# WePush Next 未签名发行说明

WePush Next `1.0.0` 是稳定的开源自部署版本，但 macOS Desktop/应用包未使用 Apple Developer ID 或 Notarization，Windows Desktop/可执行文件未使用 Authenticode。项目不购买或托管商业代码签名证书；未签名是明确的发行边界，不代表包可以跳过来源和完整性校验。

安装前必须：

1. 只从本项目 GitHub Releases 下载发行物。
2. 使用同一 Release 中的 `SHA256SUMS` 校验完整性。
3. 核对 Release 标签、版本号和所需平台/架构。
4. 在生产升级前执行备份，并先在等价环境验证恢复。

操作系统可能显示“未知开发者”或类似警告。是否允许运行由部署者依据组织安全策略决定；项目不会要求关闭系统级安全机制，也不会提供绕过安全策略的自动脚本。

Provider 插件是另一条安全边界：正式模式仍强制校验 SHA-256 清单和 Ed25519 发布者签名。应用发行物不做商业代码签名，不会降低插件签名要求。

WePush 不提供官方公共 SaaS、在线控制面、注册计费订阅、云 KMS 或遥测服务。除用户显式配置的 Provider、远程 Agent、数据库和对象存储连接外，默认 Standalone 不依赖外部网络服务。
