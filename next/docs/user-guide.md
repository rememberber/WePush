# WePush Next 对外使用指南

WePush Next 是由用户自行下载、安装、部署和运维的开源产品，不依赖官方公共 SaaS、账号注册、计费订阅或云 KMS/Secret Manager。数据、密钥和运行环境均由用户掌控；完整定位和后续计划见[《产品目标、边界与路线图》](product-scope-and-roadmap.md)。

本文面向第一次下载和使用 WePush Next 的用户，适用于 `0.1.0-alpha.2` Public Preview。

> WePush Next 仍是公开预览版，API、数据结构和升级路径可能变化，不建议直接承载关键生产业务。Desktop 与发行包未使用商业代码签名；请只从官方 GitHub Release 下载，并在运行前校验 `SHA256SUMS`。

## 1. 先选择需要的组件

WePush Next 由多个可以独立使用的组件组成：

| 组件 | 用途 | 第一次体验是否需要 |
| --- | --- | --- |
| Service | 保存配置和运行数据，提供 WebUI、OpenAPI 与 Service API，并可在本机执行任务 | 必需 |
| WebUI | 浏览器中的可视化配置、运行中心、动态 API 文档 | 已包含在 Service 发行包中 |
| Desktop UI | Electron 桌面管理界面，连接本机 `127.0.0.1:18990` Service | 可选 |
| Agent | 从 Service 接收任务并在独立节点执行，适合远程或分布式部署 | 本地体验不需要 |
| Remote Java SDK | 供其他 Java 应用通过 Service API 集成 WePush | 按需，`alpha.2` 附件已包含 |
| Embedded Java SDK | 在 Java 应用进程内直接装配 Engine 和 Provider，不需要 Service | 按需，`alpha.2` 附件已包含 |
| Core / Engine | Service 与 Agent 内部的执行引擎 | 不需要单独安装 |

Desktop UI 目前不是一体化安装器，也不会自动启动 Service。无论使用浏览器还是 Desktop UI，都应先启动或安装 Service。

Classic 与 Next 是彼此独立的两条产品线。Classic 继续提供成熟的桌面批量推送能力；Next 当前内置 HTTP Provider，重点验证 Service、Agent、SDK、WebUI/Desktop 和新执行架构。两者没有自动迁移或共享数据要求，可以并行安装和使用。

## 2. 下载与系统要求

官方下载页：

- [WePush Next `0.1.0-alpha.2` Public Preview](https://github.com/rememberber/WePush/releases/tag/next-v0.1.0-alpha.2)

发行附件：

| 文件 | 内容或平台 |
| --- | --- |
| `wepush-next-0.1.0-alpha.2.tar.gz` | Service、Agent、WebUI、安装脚本、OpenAPI 与 SDK，适合 Linux/macOS |
| `wepush-next-0.1.0-alpha.2.zip` | 与上面内容相同，适合 Windows |
| `wepush-next-desktop-0.1.0-alpha.2-linux-x64.tar.gz` | Linux x64 Desktop |
| `wepush-next-desktop-0.1.0-alpha.2-macos-arm64.zip` | macOS Apple Silicon Desktop |
| `wepush-next-desktop-0.1.0-alpha.2-windows-x64.zip` | Windows x64 Desktop |
| `wepush-next-java-sdk-0.1.0-alpha.2.zip` | Remote Java SDK、Embedded Java SDK 及其 POM/JAR 依赖闭包 |
| `SHA256SUMS` | 全部附件的 SHA-256 |
| `wepush-next-0.1.0-alpha.2-sbom.cdx.json` | CycloneDX SBOM |

运行 Service、Agent 或任一种 Java SDK 需要 Java 21 或更高版本：

```bash
java -version
```

当前 Desktop 预览包仅提供 Linux x64、Windows x64 和 macOS Apple Silicon。macOS Intel 暂无 Desktop 预览包，但仍可在满足 Java 21 的环境中运行 Service 并使用浏览器 WebUI。

### 校验下载文件

下载 `SHA256SUMS` 后，计算目标文件的 SHA-256，并与其中同名行比较：

Linux：

```bash
sha256sum wepush-next-0.1.0-alpha.2.tar.gz
grep 'wepush-next-0.1.0-alpha.2.tar.gz$' SHA256SUMS
```

macOS：

```bash
shasum -a 256 wepush-next-0.1.0-alpha.2.tar.gz
grep 'wepush-next-0.1.0-alpha.2.tar.gz$' SHA256SUMS
```

Windows PowerShell：

```powershell
Get-FileHash .\wepush-next-0.1.0-alpha.2.zip -Algorithm SHA256
Select-String -Path .\SHA256SUMS -Pattern 'wepush-next-0.1.0-alpha.2.zip$'
```

两个值必须完全一致。Desktop 包也使用相同方法校验。

## 3. 五分钟启动：便携 Standalone

Standalone 默认只监听 `127.0.0.1:18990`，使用 SQLite、本地 Artifact Store 和内嵌 Engine，不需要 PostgreSQL、对象存储或 Agent。

### Linux

```bash
tar -xzf wepush-next-0.1.0-alpha.2.tar.gz
cd wepush-next-0.1.0-alpha.2
./bin/wepush-service
```

### macOS

```bash
tar -xzf wepush-next-0.1.0-alpha.2.tar.gz
cd wepush-next-0.1.0-alpha.2
./bin/wepush-service
```

### Windows PowerShell

```powershell
Expand-Archive .\wepush-next-0.1.0-alpha.2.zip .\wepush-next
Set-Location .\wepush-next\wepush-next-0.1.0-alpha.2
$releaseRoot = (Get-Location).Path.Replace('\', '/')
$env:WEPUSH_WEB_ROOT = "file:$releaseRoot/web/"
java -jar .\lib\wepush-next-service.jar
```

保持终端窗口运行，然后打开：

- WebUI：`http://127.0.0.1:18990/`
- 健康检查：`http://127.0.0.1:18990/actuator/health/readiness`
- OpenAPI：`http://127.0.0.1:18990/openapi.yaml`

也可以在另一个终端验证：

```bash
curl --fail http://127.0.0.1:18990/actuator/health/readiness
curl --fail http://127.0.0.1:18990/api/v1/system/info
```

终端按 `Ctrl+C` 即可停止便携 Service。请从解压后的目录启动，以便默认 `.local` 数据目录位于该发行目录下。

## 4. 可选：使用 Desktop UI

先按上一节启动 Service，再解压对应平台的 Desktop 附件：

- Linux：运行 `wepush-next-desktop-0.1.0-alpha.2-linux-x64/WePush Next/wepush-next`。
- macOS：打开 `wepush-next-desktop-0.1.0-alpha.2-macos-arm64/WePush Next.app`。
- Windows：运行 `wepush-next-desktop-0.1.0-alpha.2-windows-x64\WePush Next\WePush Next.exe`。

Desktop 会连接本机 `http://127.0.0.1:18990`。出现“Service 未连接”时，先确认 Service 终端没有退出，并检查健康地址。

由于当前预览包没有商业签名，macOS Gatekeeper 或 Windows SmartScreen 可能显示未知开发者/发布者。只有在文件来自官方 Release 且 SHA-256 校验一致时，才使用系统提供的“打开”流程；不要全局关闭系统安全能力。

不想安装 Desktop 时，直接使用浏览器 WebUI 即可，两种 UI 连接的是同一套 Service API 和数据。

## 5. 完成第一次安全 Dry Run

下面的流程只验证 Account → Message → Audience → Job → Engine → Provider 全链路。HTTP Provider 的 Dry Run 不会向配置的远端地址发送 HTTP 请求。

### 5.1 创建 HTTP 账号

1. 打开 `Providers`。
2. 选择内置的 `HTTP` Provider。
3. 账号名称填写 `Preview HTTP`。
4. `baseUrl` 填写 `https://example.com`。
5. `connectTimeout` 保持 `PT5S`。
6. `allowPrivateAddresses` 保持关闭，`auth` 保持 `NONE` 或不配置。
7. 点击“保存账号”。

只有明确受信的内部 HTTP 服务才应启用 `allowPrivateAddresses`。默认关闭可避免任务访问本机、局域网和云元数据地址。

### 5.2 创建消息

进入“消息”，选择 HTTP Provider，并填写：

- 名称：`Preview message`
- 方法：`POST`
- 路径：`/messages`
- Headers：`{"Content-Type":"application/json"}`
- Body Template：

```json
{
  "name": "{{name}}",
  "mobile": "{{mobile}}"
}
```

模板变量使用 `{{fieldName}}`，运行时从每个 Recipient 的 `fields` 中取值。保存消息后会形成不可变修订。

### 5.3 创建受众

进入“受众”，保留或粘贴：

```json
[
  {
    "itemId": "alice",
    "fields": {
      "mobile": "13000000000",
      "name": "Alice"
    }
  },
  {
    "itemId": "bob",
    "fields": {
      "mobile": "13100000000",
      "name": "Bob"
    }
  }
]
```

点击“创建受众快照”。`itemId` 应在同一受众中保持稳定且唯一；`fields` 必须包含消息模板引用的所有字段。

### 5.4 创建任务并运行

1. 进入“任务与调度”。
2. 选择刚创建的账号、消息和受众。
3. 保持较小的目标并发，例如 `2`。
4. 保存任务。
5. 在右侧任务卡片点击 `Dry Run`。
6. 进入“运行中心”，查看状态、计数、事件和每条 Recipient 的结果。

正常情况下，两条结果会以 Provider Code `DRY_RUN` 成功完成。该过程不会请求 `example.com/messages`。

## 6. 正式 HTTP 发送

当前 `0.1.0-alpha.2` WebUI 的任务卡片只提供 Dry Run。正式运行可通过“API 文档”页中的 `createRun` 操作、Java SDK 或直接调用 Service API 发起。

> 将 `dryRun` 设为 `false` 会真实访问 HTTP 目标。请先使用专用测试端点、小范围受众和服务端幂等机制验证，不要把上面的 `example.com` 示例用于正式运行。

```bash
curl --request POST \
  --header 'Content-Type: application/json' \
  --header 'Idempotency-Key: replace-with-a-unique-value' \
  --data '{"dryRun":false,"policyOverrides":{},"reason":"manual-live-run"}' \
  http://127.0.0.1:18990/api/v1/workspaces/ws_default/jobs/<JOB_ID>/runs
```

开启 API Security 后还需添加：

```text
Authorization: Bearer <TOKEN>
```

HTTP Provider 不跟随重定向，默认阻止私有地址，限制响应体大小，并支持配置成功状态码与幂等 Header。正式接入前应按目标系统的认证、限流、超时和幂等约定完成配置。

## 7. API 文档与 Java SDKs

WebUI 的“API 文档”会读取 Service 提供的 OpenAPI，支持查看请求 Schema、生成请求和动态调试。写操作发送前会二次确认；权限、审计和同源限制仍由 Service 强制执行。

Remote Java SDK 是 Service API 客户端，不依赖 Core、Engine 或具体 Provider。`alpha.2` 发行包和独立 SDK 压缩包都包含它，安装说明见 [Java SDK README](../sdk/README.md)。

最小示例：

```java
try (var client = WePushClient.builder()
        .endpoint(URI.create("http://127.0.0.1:18990"))
        .build()) {
    var system = client.system().info();
    var providers = client.providers().list();
    var workspace = client.workspace("ws_default");
    var runs = workspace.runs();
}
```

Embedded Java SDK 依赖 Core Engine，但不依赖 Service、Agent、数据库或 Spring；应用显式注册允许的 Provider，并直接传入执行快照和 Recipient：

```java
var store = new InMemoryExecutionStore();
try (var engine = WePushEngine.builder()
        .provider(new HttpProviderFactory())
        .resultSink(store)
        .eventSink(store)
        .build()) {
    var summary = engine.start(spec, recipients)
            .completion().toCompletableFuture().join();
}
```

`alpha.2` 发行包和独立 Java SDK 附件同时包含 Embedded SDK 与所需依赖。完整依赖、配置、资源所有权和示例见 [Embedded Java SDK README](../sdk/embedded-java/README.md)。

## 8. 何时需要 Agent

默认内嵌执行模式下，Service 自己调用 Core Engine，本地体验不需要 Agent。以下情况再部署 Agent：

- 任务需要在另一台主机或受控网络中执行。
- 希望 Service 只承担控制面，执行资源独立扩缩容。
- 不同执行节点需要安装不同 Provider 插件。
- 需要借助 Lease、Fencing、心跳和远端 Artifact 回传管理分布式运行。

Agent 使用 Service 的 `19090` gRPC 控制流。跨主机部署必须使用 Enrollment、长期 Credential 和 TLS/mTLS，不应沿用回环开发的明文配置。完整步骤见[部署与运维文档](deployment-and-operations.md)。

## 9. 安装为系统服务

长期使用时建议安装 Service，而不是保留前台终端。第一次本机体验只需安装 `service`；选择 `all` 才会同时安装 Agent。

### Linux / systemd

```bash
tar -xzf wepush-next-0.1.0-alpha.2.tar.gz
sudo ./wepush-next-0.1.0-alpha.2/install/linux/install.sh service
systemctl status wepush-next-service
```

### macOS / launchd

从普通登录用户的终端使用 `sudo`，安装器会将该用户作为非 root 服务用户：

```bash
tar -xzf wepush-next-0.1.0-alpha.2.tar.gz
sudo ./wepush-next-0.1.0-alpha.2/install/macos/install.sh service
launchctl print system/com.fangxuele.wepush-next.service
```

无人值守安装需要显式设置已经存在的 `WEPUSH_SERVICE_USER`。

### Windows Service

以管理员 PowerShell 执行：

```powershell
Expand-Archive .\wepush-next-0.1.0-alpha.2.zip .\release
Set-ExecutionPolicy -Scope Process Bypass
& .\release\wepush-next-0.1.0-alpha.2\install\windows\install.ps1 -Component service
Get-Service WePushNextService
```

Windows 安装器会下载并校验固定版本的 WinSW，因此安装时需要能够访问对应的 GitHub Release Asset。

配置路径、备份、升级、卸载、Server/HA、PostgreSQL、S3 与插件步骤见[部署与运维文档](deployment-and-operations.md)。

## 10. 数据、备份与安全

便携 Standalone 默认保存：

- SQLite：`.local/data/wepush-next.db`
- Secret 主密钥：`.local/secrets/master-key.json`
- Artifact：`.local/artifacts`

系统服务安装后的路径因操作系统而异，具体以部署文档和生成的 `service.env` 为准。备份时必须同时保留数据库、Secret 主密钥和 Artifact；只恢复数据库而丢失主密钥，会导致已有 Secret 无法解密。

默认无认证模式仅允许 Service 监听回环地址。不要直接把 `127.0.0.1` 改成 `0.0.0.0`。非回环监听必须同时启用 API Security、设置强 Bootstrap Token，并在网络入口配置 TLS；Server 模式还会强制 PostgreSQL、S3-compatible Artifact Store 和 Agent gRPC TLS。

## 11. 常见问题

### UI 显示 Service 未连接

- 确认 `http://127.0.0.1:18990/actuator/health/readiness` 可访问。
- 确认 Java 版本是 21 或更高。
- 检查 `18990` 是否被其他程序占用。
- Desktop 当前只连接本机 `127.0.0.1:18990`。

### Service 启动后看不到 WebUI

- 使用发行包中的 `bin/wepush-service`，或按 Windows 便携命令设置 `WEPUSH_WEB_ROOT`。
- 确认解压目录下存在 `web/index.html`。
- 直接从源码 JAR 启动时需要先构建 WebUI，或单独设置静态资源目录。

### HTTP 运行报告 `SSRF_BLOCKED`

目标解析到了私有、回环或受限地址。只有目标确实是受信内部服务时，才在 Account 中开启 `allowPrivateAddresses`；不要为了绕过未知错误而开启。

### API 返回 `401` 或 `403`

Service 已开启安全模式。把 Bootstrap Token 或已签发 Token 填入 WebUI“设置 → 当前 API Token”；命令行请求使用 `Authorization: Bearer <TOKEN>`。

### 操作系统阻止 Desktop 首次启动

先核对来源和 SHA-256。校验不一致时立即删除文件；校验一致时使用操作系统针对单个应用提供的确认流程，不要关闭 Gatekeeper、SmartScreen 或终端安全软件。

## 12. 反馈与进一步文档

- [Next README](../README.md)
- [产品目标、边界与路线图](product-scope-and-roadmap.md)
- [架构与概要设计](architecture-and-high-level-design.md)
- [详细设计](detailed-design.md)
- [部署与运维](deployment-and-operations.md)
- [实现状态](implementation-status.md)
- [本版本 Release Notes](releases/0.1.0-alpha.2.md)
- [安全策略](../SECURITY.md)

普通问题和功能建议可提交 GitHub Issue。安全漏洞不要公开披露，请按 `SECURITY.md` 中的方式私下报告。
