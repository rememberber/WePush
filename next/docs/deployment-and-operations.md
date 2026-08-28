# WePush Next 部署与运维

本文是 `1.0.0` 稳定自部署版的可执行部署说明。Standalone 使用 SQLite 和本地 Artifact；用户自建 Server 使用 PostgreSQL、S3-compatible Artifact、两个以上 Service 实例以及外部负载均衡器。Classic 不参与 Next 的构建、安装或运行。WePush 不提供官方托管控制面，所有部署、数据、密钥和备份均由用户掌控；产品边界见[《产品目标、边界与路线图》](product-scope-and-roadmap.md)。

Service 的无认证开发模式仅允许绑定回环地址。任何非回环 HTTP 监听都必须启用 API Security；`server` 模式还会强制 PostgreSQL、S3-compatible Artifact Store 和 Agent gRPC TLS，缺少任一项均启动失败。

## 1. 构建与发行物

要求 JDK 21+、Node.js 24 和 pnpm 11.22.0：

```bash
cd next/ui
pnpm install --frozen-lockfile
pnpm check

cd ..
./scripts/prepare-winsw.sh
./mvnw verify
./mvnw -DskipTests package
```

测试必须先独立通过，`-DskipTests` 只用于之后的发行打包阶段。输出位于：

- `distribution/target/wepush-next-1.0.0.tar.gz`
- `distribution/target/wepush-next-1.0.0.zip`
- 对应 `.sha256` 文件

上面两个归档是使用系统 Java 21+ 的精简包。Release 流水线还在 Linux、macOS、Windows Runner 上用 JDK 21 `jlink` 生成对应架构的完整包，目录内多出 `runtime/`；两个变体都携带固定摘要的 WinSW 2.12.0，因此 Windows 用户安装时不联网。`prepare-winsw.sh` 只在源码发行构建阶段从上游下载并验证固定长度与 SHA-256。

归档内包含 Service/Agent Fat JAR、生产 WebUI、统一/分组件安装脚本、正式 Backup/Restore/Upgrade、配置模板和 Provider 插件生命周期工具。Desktop 原生目录包在当前操作系统执行：

```bash
cd next/ui
pnpm --filter @wepush-next/desktop package
```

Desktop 打包器使用锁定版本的 Electron，不引入带 Git 构建依赖的额外打包链；缺少 Electron Runtime 时会运行该锁定版本随附的安装脚本。macOS、Windows、Linux 必须分别在目标操作系统打包。
pnpm 已显式信任固定版本 Electron 的原生运行时下载脚本；无法访问 GitHub Release Asset 的网络可在安装依赖时设置受信 `ELECTRON_MIRROR`。按项目明确边界，`1.0.0` 不使用商业签名：macOS 只做 ad-hoc 签名且不提交 Apple Notarization，Windows 不做 Authenticode 签名。系统可能显示未知开发者警告，请只从项目 GitHub Releases 下载并验证 `SHA256SUMS`；详见 [`UNSIGNED-NOTICE.md`](../UNSIGNED-NOTICE.md)。

## 2. Linux Standalone

解压、校验并安装：

```bash
sha256sum wepush-next-1.0.0-linux-x64.tar.gz
tar -xzf wepush-next-1.0.0-linux-x64.tar.gz
sudo ./wepush-next-1.0.0/install/install.sh
```

安装布局：

- 版本目录：`/opt/wepush-next/releases/<version>`
- 原子当前链接：`/opt/wepush-next/current`
- 配置：`/etc/wepush-next/service.env`、`agent.env`
- 数据：`/var/lib/wepush-next/service`、`agent`
- systemd：默认安装 `wepush-next-service`；高级 `all` 模式另安装 `wepush-next-agent`

环境文件由 root 持有，只向相应服务组开放读取。修改配置后执行：

```bash
sudo systemctl restart wepush-next-service
curl --fail http://127.0.0.1:18990/actuator/health/installation
```

备份会停止两个进程以获得 SQLite、Journal、Outbox 和配置的一致快照，并恢复原先处于运行状态的服务：

```bash
sudo /opt/wepush-next/current/install/linux/backup.sh
sudo /opt/wepush-next/current/install/linux/restore.sh --validate-only "$BACKUP_FILE"
sudo /opt/wepush-next/current/install/linux/restore.sh "$BACKUP_FILE" "$BACKUP_SHA256"
sudo /opt/wepush-next/current/install/linux/upgrade.sh release.tar.gz <sha256>
sudo /opt/wepush-next/current/install/linux/uninstall.sh
sudo /opt/wepush-next/current/install/linux/uninstall.sh --purge  # 明确删除配置与数据
```

## 3. macOS Standalone

```bash
sudo ./wepush-next-1.0.0/install/install.sh
launchctl print system/com.fangxuele.wepush-next.service
curl --fail http://127.0.0.1:18990/actuator/health/installation
```

版本、数据、配置和日志分别位于 `/Library/WePushNext`、`/Library/Preferences/wepush-next` 与 `/Library/Logs/WePushNext`。安装器用 LaunchDaemon 托管 Service/Agent，并拒绝覆盖非符号链接的 `current` 路径。
LaunchDaemon 默认以执行 `sudo` 的非 root 用户运行；无人值守安装必须显式设置已存在的 `WEPUSH_SERVICE_USER`，安装器拒绝回退到 root。

```bash
sudo /Library/WePushNext/current/install/macos/backup.sh
sudo /Library/WePushNext/current/install/macos/restore.sh --validate-only "$BACKUP_FILE"
sudo /Library/WePushNext/current/install/macos/restore.sh "$BACKUP_FILE" "$BACKUP_SHA256"
sudo /Library/WePushNext/current/install/macos/upgrade.sh release.zip <sha256>
sudo /Library/WePushNext/current/install/macos/uninstall.sh
```

## 4. Windows Standalone

以管理员 PowerShell 执行：

```powershell
Expand-Archive .\wepush-next-1.0.0-windows-x64.zip .\release
Set-ExecutionPolicy -Scope Process Bypass
& .\release\wepush-next-1.0.0\install\install.ps1
Get-Service WePushNextService
Invoke-WebRequest http://127.0.0.1:18990/actuator/health/installation
```

安装器只使用发行包内的 WinSW 2.12.0，并在安装前再次校验固定长度和 SHA-256；缺失或被修改时失败关闭，不进行在线下载。Service 以低权限 `LocalService` 运行。版本目录位于 `%ProgramFiles%\WePush Next`，配置和数据位于 `%ProgramData%\WePush Next`；该目录移除继承 ACL，只允许 LocalService、SYSTEM 和 Administrators。备份默认写到独立的 `%ProgramData%\WePush Next Backups`。

```powershell
& "$env:ProgramFiles\WePush Next\current\install\windows\backup.ps1"
& "$env:ProgramFiles\WePush Next\current\install\windows\restore.ps1" -ValidateOnly -Archive '<backup>.zip'
& "$env:ProgramFiles\WePush Next\current\install\windows\restore.ps1" -Archive '<backup>.zip' -ExpectedSha256 '<sha256>'
& "$env:ProgramFiles\WePush Next\current\install\windows\upgrade.ps1" -Archive release.zip -ExpectedSha256 <sha256>
& "$env:ProgramFiles\WePush Next\current\install\windows\uninstall.ps1"
```

只有显式 `-Purge` 才删除持久数据。

## 5. Agent Enrollment 与远程运行

Server 管理员在 WebUI 设置页或 API 创建一次性 Enrollment Token，并绑定 Workspace。Agent 第一次启动设置：

```text
WEPUSH_AGENT_ID=agent-east-1
WEPUSH_AGENT_ENROLLMENT_TOKEN=<one-time-token>
WEPUSH_SERVICE_BASE_URL=https://wepush.example.com
WEPUSH_SERVICE_HOST=wepush.example.com
WEPUSH_AGENT_GRPC_PORT=19090
WEPUSH_AGENT_GRPC_PLAINTEXT=false
```

Agent 生成 P-256 私钥，在 HTTPS Enrollment 后保存长期 Credential、客户端证书与 CA；证书或 Credential 距到期不足 14 天时自动轮换。身份文件、私钥、Event Outbox、Completion Outbox 和 Lease Journal 均必须位于持久卷。非回环部署不允许匿名 Agent；gRPC 必须启用 TLS，生产基线要求 mTLS。

## 6. Provider 插件

HTTP、SMTP Email、飞书/钉钉/企微机器人、阿里云短信、微信公众号、小程序和企业微信应用消息是发行包内置 Provider，不需要放入插件目录。内置渠道的账号、SecretRef、最小消息和验证步骤见[《内置 Provider 指南》](provider-guide.md)。

正式模式必须设置 `WEPUSH_PLUGIN_TRUSTED_KEYS`，格式是 `keyId:Base64Ed25519PublicKey`，多个发布者用逗号分隔。插件 ZIP 包内 `plugin.json` 的 SHA-256 清单与 `signature.ed25519` 必须通过验证；ZIP Slip、共享 API 重复打包、未知签名者和 SPI 不兼容都会导致 Agent 失败关闭。

Linux 应以 Agent 服务账号执行 Stage/Activate，确保文件所有权正确：

```bash
sudo -u wepush-agent /opt/wepush-next/current/plugins/stage.sh provider.zip
sudo -u wepush-agent /opt/wepush-next/current/plugins/activate.sh <pluginId>.zip
sudo -u wepush-agent /opt/wepush-next/current/plugins/rollback.sh <pluginId>.zip
```

Stage 先调用 Agent 的生产校验器读取 `plugin.json`，验证摘要、SPI 和 Ed25519 签名，再以 `<pluginId>.zip` 作为稳定文件名落盘。激活采用版本备份、Supervisor 重启和健康观察；新插件导致 Agent 退出时自动恢复旧包并再次启动。Desktop 的 Providers 页提供相同的本地选择、校验、Stage、Activate 和 Rollback 操作，并通过系统授权写入插件目录。该机制是受控滚动更新，不在 JVM 内热卸载 ClassLoader。

## 7. Server/HA 容器拓扑

`deployment/container/compose.server.yaml` 是本地/验收拓扑：PostgreSQL 18、MinIO、两个无状态 Service 和 HAProxy。HTTP API 通过 `127.0.0.1:18990` 暴露；Agent gRPC 通过 TCP 透传的 `19090` 暴露，因此 mTLS 端到端保持在 Agent 与 Service 之间。

首次运行：

```bash
cd next/deployment/container
./generate-dev-secrets.sh
docker compose --env-file .env.server.local -f compose.server.yaml up --build -d
curl --fail http://127.0.0.1:18990/actuator/health/readiness
curl --fail http://127.0.0.1:18990/actuator/prometheus
```

开发脚本生成的 CA/密钥和 `.env.server.local` 已被 `.gitignore` 排除，且拒绝覆盖已有文件。两个 Service 共享：

- PostgreSQL 业务状态、Lease、Service→Agent Outbox、Schedule 与审计事实源；
- Agent CA、API Bootstrap Token、Secret 主密钥和 Artifact 签名密钥；
- MinIO Artifact 对象。

Schedule Scanner 使用 PostgreSQL Advisory Lock 单 Leader；Agent 命令和 Lease Offer 使用持久 outbox，只有持有当前 gRPC 流的实例发送；SSE 以数据库事件日志和周期轮询跨实例补偿。

该 Compose 为本地协议验收拓扑，明确使用 `WEPUSH_S3_SERVER_SIDE_ENCRYPTION=NONE`。正式自建环境应配置对象存储原生 `AES256`，或由部署者在其存储层保证等价的静态加密；WePush 不对接或管理云 KMS。正式环境还必须：

- 使用由部署者负责的 PostgreSQL HA 和备份恢复；
- 在 HTTP API 前终止受信 TLS，保留 gRPC TLS 透传或等价的受控 mTLS 方案；
- 将 Master Key、Agent CA 和其他密钥放入权限受控的只读挂载文件，不使用提交到源码或共享目录的 Compose env 文件；
- 配置 S3 Lifecycle 作为未完成 Multipart 与误删保护的兜底；
- 至少两个 Service 跨故障域部署并配置 Readiness/Prometheus 告警。

停止本地环境：

```bash
docker compose --env-file .env.server.local -f compose.server.yaml down
```

只有确认不再需要测试数据时才执行带 `--volumes` 的删除。

## 8. 升级、恢复与验收

Standalone 升级顺序：一致性备份 → 校验发行 SHA-256 → 展开新版本目录 → 原子切换 `current` → 重启 → Installation Health（Readiness、Flyway 当前版本、内置 Provider 本地 Dry Run）→ 成功保留备份。任一步失败都会切回旧 `current`，用刚生成的备份恢复配置和数据，再验证旧版本；命令以失败状态退出并保留恢复目录供人工审计。Server 滚动升级先执行单实例 Migration Job，再逐个替换 Service，最后滚动 Drain/Restart Agent。

Backup Archive 包含 `BACKUP-MANIFEST`、逐文件 `SHA256SUMS` 和配置/数据 Payload。Restore 在展开前拒绝路径穿越，并要求实际 Payload 文件集合与摘要清单完全一致；替换前保存 `pre-restore-*`，恢复后执行相同 Installation Health，失败时恢复原目录。不要分别恢复数据库和 Master Key。

最低验收项：

1. Readiness 和 Prometheus 可访问。
2. WebUI 能保存 Account/Message/Audience/Job 并启动 Dry Run。
3. SSE 能重连并从 `Last-Event-ID` 回放。
4. Agent Enrollment、Hello/Welcome、Lease、Event Ack、Artifact Commit 和 Run Completion 成功。
5. 停掉一个 Service 后 API、Schedule、SSE 和 Agent 重连仍可恢复。
6. 备份恢复后 SQLite/PostgreSQL、Artifact、Agent Journal/Outbox 与 Secret 主密钥一致。
