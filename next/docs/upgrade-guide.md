# WePush Next 升级与回滚指南

本文以 `1.1.0` 升级到 `1.2.0` 为当前路径，也适用于 `0.1.0-beta.1` 或 `1.0.0` 经稳定版升级和后续 `1.x` Minor/Patch 升级。完整兼容承诺见[《兼容性策略》](compatibility-policy.md)。

`1.2.0` 将 Java 基线升级至 25，且不新增数据库迁移。精简包部署与 Java SDK 使用方须先将系统 Java 升级至 25+；完整包使用随包提供的 Java 25 运行时。旧版发布说明中的 Java 21 要求仅适用于 `1.1.0` 及更早版本。

## 1. 升级前检查

1. 确认当前版本至少为 `0.1.0-beta.1`，推荐先处于 `1.1.0`，且 Installation Health 为 `UP`。使用精简包或 Java SDK 时，先安装 Java 25+。
2. 确认磁盘有足够空间同时容纳当前数据、备份和新发行包；建议可用空间不低于数据目录大小的两倍加发行包大小。
3. 从 GitHub Release 下载正确平台/架构的 `1.2.0` 包和 `SHA256SUMS`，完成 SHA-256 校验。
4. 阅读 [`UNSIGNED-NOTICE.md`](../UNSIGNED-NOTICE.md)。macOS/Windows 发行物没有商业代码签名。
5. 暂停新 Schedule 或选择业务低峰。正在运行的外部发送必须先完成或由操作员确认结果处理方式。

## 2. 自动备份与升级

升级脚本会在切换版本前创建完整备份，安装新版本后检查 Readiness、Flyway 当前版本和内置 HTTP Provider 的无网络 Dry Run。任一步失败都会恢复旧版本链接和升级前数据。

Linux：

```bash
sudo /opt/wepush-next/current/install/linux/upgrade.sh \
  wepush-next-1.2.0-linux-x64.tar.gz <sha256>
```

macOS：

```bash
sudo /Library/WePushNext/current/install/macos/upgrade.sh \
  wepush-next-1.2.0-macos-arm64.zip <sha256>
```

Windows（管理员 PowerShell）：

```powershell
& "$env:ProgramFiles\WePush Next\current\install\windows\upgrade.ps1" `
  -Archive .\wepush-next-1.2.0-windows-x64.zip `
  -ExpectedSha256 <sha256>
```

## 3. 升级后验证

- `GET /actuator/health/readiness` 返回 `UP`。
- `GET /actuator/health/installation` 显示数据库迁移和内置 Provider Dry Run 通过。
- `GET /api/v1/system/info` 返回 `1.2.0`。
- Installation Health 显示 Flyway 当前版本为 V17；`workspace_policy`、`account_auth_circuit` 和 `artifact_multipart_upload` 已创建。`1.2.0` 不新增迁移。
- 原 Workspace、Account、Message、Audience、Job、Schedule、历史 Run 和 Artifact 可读取。
- Agent 重新连接，Provider Catalog 完整；如使用超过 1 GiB Artifact 或运营商插件，确认 Agent 也为 `1.2.0` 并已配置本版本发行签名公钥。先执行 Dry Run，再用自有测试目标执行小规模真实发送。
- 需要 AI 助手时，在 Desktop **设置 → AI 助手接入** 重新安装 MCP/Skill，或使用发行包中的 `ai/wepush-ai.mjs`。旧的 `1.1.0` 安装包没有该安装器。
- Server/HA 确认周期扫描持续工作，并在 PostgreSQL 日志/指标中检查通知链路；`LISTEN/NOTIFY` 失效不得阻断 Run、Agent 命令或 SSE 推进。
- 备份文件仍保留在默认备份目录，且可用 Restore 的 `--validate-only` / `-ValidateOnly` 验证。

## 4. 回滚

升级健康检查失败时脚本会自动回滚。升级成功后如需人工回滚，先停止 Service/Agent，再使用升级前备份恢复；不要只替换 JAR 而保留未经确认的新数据库。

`1.2.0` 不新增表。从 `1.1.0` 升级时，受支持的回滚目标是 `1.1.0`；若数据库曾经过 `1.1.0` 的 V15–V17，不能把只替换 JAR 当成回滚到 `1.0.0`。无论目标版本如何，都必须通过升级前完整备份恢复。备份同时覆盖数据库、Master Key、Artifact、Agent Identity、Journal、Event/Completion Outbox 和插件。

Restore 会：

- 拒绝路径穿越、非普通文件、未列入摘要的 Payload 和内容摘要不一致；
- 在替换前保留 `pre-restore-*` 原目录；
- 恢复后运行 Installation Health；
- 健康检查失败时恢复操作前数据。

## 5. 卸载

默认卸载只删除服务注册和程序文件，保留配置与数据。只有显式使用 `--purge`（Linux/macOS）或 `-Purge`（Windows）才删除用户数据。执行 Purge 前必须自行保存需要保留的备份。

三平台默认卸载、数据保留和显式 Purge 都进入发行自动化门禁。
