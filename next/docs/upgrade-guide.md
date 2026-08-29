# WePush Next 升级与回滚指南

本文适用于 `0.1.0-beta.1` 升级到 `1.0.0`，也定义后续 `1.x` 升级的标准流程。完整兼容承诺见[《兼容性策略》](compatibility-policy.md)。

## 1. 升级前检查

1. 确认当前版本至少为 `0.1.0-beta.1`，Installation Health 为 `UP`。
2. 确认磁盘有足够空间同时容纳当前数据、备份和新发行包；建议可用空间不低于数据目录大小的两倍加发行包大小。
3. 从 GitHub Release 下载正确平台/架构的 `1.0.0` 包和 `SHA256SUMS`，完成 SHA-256 校验。
4. 阅读 [`UNSIGNED-NOTICE.md`](../UNSIGNED-NOTICE.md)。macOS/Windows 发行物没有商业代码签名。
5. 暂停新 Schedule 或选择业务低峰。正在运行的外部发送必须先完成或由操作员确认结果处理方式。

## 2. 自动备份与升级

升级脚本会在切换版本前创建完整备份，安装新版本后检查 Readiness、Flyway 当前版本和内置 HTTP Provider 的无网络 Dry Run。任一步失败都会恢复旧版本链接和升级前数据。

Linux：

```bash
sudo /opt/wepush-next/current/install/linux/upgrade.sh \
  wepush-next-1.0.0-linux-x64.tar.gz <sha256>
```

macOS：

```bash
sudo /Library/WePushNext/current/install/macos/upgrade.sh \
  wepush-next-1.0.0-macos-arm64.zip <sha256>
```

Windows（管理员 PowerShell）：

```powershell
& "$env:ProgramFiles\WePush Next\current\install\windows\upgrade.ps1" `
  -Archive .\wepush-next-1.0.0-windows-x64.zip `
  -ExpectedSha256 <sha256>
```

## 3. 升级后验证

- `GET /actuator/health/readiness` 返回 `UP`。
- `GET /actuator/health/installation` 显示数据库迁移和内置 Provider Dry Run 通过。
- `GET /api/v1/system/info` 返回 `1.0.0`。
- 原 Workspace、Account、Message、Audience、Job、Schedule、历史 Run 和 Artifact 可读取。
- Agent 重新连接，Provider Catalog 完整；先执行 Dry Run，再用自有测试目标执行小规模真实发送。
- 备份文件仍保留在默认备份目录，且可用 Restore 的 `--validate-only` / `-ValidateOnly` 验证。

## 4. 回滚

升级健康检查失败时脚本会自动回滚。升级成功后如需人工回滚，先停止 Service/Agent，再使用升级前备份恢复；不要只替换 JAR 而保留未经确认的新数据库。

`1.0.0` 的 V14 仅新增兼容元数据表，最低可回滚版本是 `0.1.0-beta.1`。推荐仍通过备份恢复，因为备份还覆盖 Master Key、Artifact、Agent Identity、Journal、Event/Completion Outbox 和插件。

Restore 会：

- 拒绝路径穿越、非普通文件、未列入摘要的 Payload 和内容摘要不一致；
- 在替换前保留 `pre-restore-*` 原目录；
- 恢复后运行 Installation Health；
- 健康检查失败时恢复操作前数据。

## 5. 卸载

默认卸载只删除服务注册和程序文件，保留配置与数据。只有显式使用 `--purge`（Linux/macOS）或 `-Purge`（Windows）才删除用户数据。执行 Purge 前必须自行保存需要保留的备份。

三平台默认卸载、数据保留和显式 Purge 都进入发行自动化门禁。
