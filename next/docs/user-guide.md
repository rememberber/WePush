# WePush Next 对外使用指南

WePush Next 是由用户自行下载、安装、部署和运维的开源产品，不依赖官方公共 SaaS、账号注册、计费订阅或云 KMS/Secret Manager。数据、密钥和运行环境均由用户掌控；完整定位和后续计划见[《产品目标、边界与路线图》](product-scope-and-roadmap.md)。

本文面向第一次下载和使用 WePush Next 的用户，适用于 `1.1.0` 稳定自部署版。

> `1.1.0` 提供明确的 1.x API、配置、数据库和 Agent 协议兼容承诺。Desktop 与发行包未使用商业代码签名；请只从项目 GitHub Release 下载，并在运行前校验 `SHA256SUMS`。详见[《兼容性策略》](compatibility-policy.md)和 [`UNSIGNED-NOTICE.md`](../UNSIGNED-NOTICE.md)。

## 1. 先选择需要的组件

WePush Next 由多个可以独立使用的组件组成：

| 组件 | 用途 | 第一次体验是否需要 |
| --- | --- | --- |
| Service | 保存配置和运行数据，提供 WebUI、OpenAPI 与 Service API，并可在本机执行任务 | 必需 |
| WebUI | 浏览器中的可视化配置、运行中心、动态 API 文档 | 已包含在 Service 发行包中 |
| Desktop UI | Electron 桌面管理界面，连接并管理已安装的本机 `127.0.0.1:18990` Service | 可选 |
| Agent | 从 Service 接收任务并在独立节点执行，适合远程或分布式部署 | 本地体验不需要 |
| Remote Java SDK | 供其他 Java 应用通过 Service API 集成 WePush | 按需，`1.1.0` 附件已包含 |
| Embedded Java SDK | 在 Java 应用进程内直接装配 Engine 和 Provider，不需要 Service | 按需，`1.1.0` 附件已包含 |
| Core / Engine | Service 与 Agent 内部的执行引擎 | 不需要单独安装 |

发行包的统一脚本负责安装 Standalone Service；Desktop UI 不重复内嵌 Service，但可以检测、启动、停止已安装的本机 Service，读取日志和生成脱敏诊断。

Classic 与 Next 是彼此独立的两条产品线。Classic 继续提供成熟的桌面批量推送能力；Next 当前内置 HTTP、SMTP Email、飞书/钉钉/企微机器人、阿里云短信、微信公众号、小程序和企业微信应用消息。两者没有自动迁移或共享数据要求，可以并行安装和使用。

## 2. 下载与系统要求

官方下载页：

- [WePush Next `1.1.0` Stable](https://github.com/rememberber/WePush/releases/tag/next-v1.1.0)

发行附件：

| 文件 | 内容或平台 |
| --- | --- |
| `wepush-next-1.1.0-linux-<arch>.tar.gz` | Linux 完整包，内含 Java Runtime，推荐首次安装 |
| `wepush-next-1.1.0-macos-<arch>.zip` | macOS 完整包，内含 Java Runtime，推荐首次安装 |
| `wepush-next-1.1.0-windows-<arch>.zip` | Windows 完整包，内含 Java Runtime 和离线 WinSW |
| `wepush-next-1.1.0.tar.gz` / `.zip` | 使用系统 Java 21+ 的精简便携包；内容不含 Java Runtime |
| `wepush-next-desktop-1.1.0-<os>-<arch>.*` | 对应平台的 Desktop 管理界面 |
| `wepush-next-java-sdk-1.1.0.zip` | Remote Java SDK、Embedded Java SDK 及其 POM/JAR 依赖闭包 |
| `wepush-provider-{cmpp,smgp,sgip,smpp}-1.1.0.zip` | 可选运营商短信 Agent 签名插件；各自附带 `.sha256` |
| `wepush-provider-trusted-key-1.1.0.env` | 官方插件签名 Key ID/公钥的 Agent 环境配置；附带 `.sha256` 并列入统一 `SHA256SUMS` |
| `SHA256SUMS` | 全部附件的 SHA-256 |
| `wepush-next-1.1.0-sbom.cdx.json` | CycloneDX SBOM |

完整包运行 Service/Agent 不要求预装 Java。精简包、从源码构建和任一种 Java SDK 需要 Java 21 或更高版本：

```bash
java -version
```

发行工作流按目标 Runner 架构生成附件；文件名中的 `<arch>` 以 Release 实际列出的 `x64` 或 `arm64` 为准。

### 校验下载文件

下载 `SHA256SUMS` 后，计算目标文件的 SHA-256，并与其中同名行比较：

Linux：

```bash
sha256sum wepush-next-1.1.0.tar.gz
grep 'wepush-next-1.1.0.tar.gz$' SHA256SUMS
```

macOS：

```bash
shasum -a 256 wepush-next-1.1.0.tar.gz
grep 'wepush-next-1.1.0.tar.gz$' SHA256SUMS
```

Windows PowerShell：

```powershell
Get-FileHash .\wepush-next-1.1.0.zip -Algorithm SHA256
Select-String -Path .\SHA256SUMS -Pattern 'wepush-next-1.1.0.zip$'
```

两个值必须完全一致。Desktop 包也使用相同方法校验。

## 3. 五分钟安装：Standalone

Standalone 默认只监听 `127.0.0.1:18990`，使用 SQLite、本地 Artifact Store 和内嵌 Engine，不需要 PostgreSQL、对象存储或 Agent。

下载当前操作系统的完整包并解压。统一入口默认只安装 Standalone Service；安装末尾会验证 Readiness、数据库迁移版本和不会访问网络的 Provider Dry Run。

### Linux

```bash
tar -xzf wepush-next-1.1.0-linux-x64.tar.gz
cd wepush-next-1.1.0
sudo ./install/install.sh
```

### macOS

```bash
ditto -x -k wepush-next-1.1.0-macos-arm64.zip .
cd wepush-next-1.1.0
sudo ./install/install.sh
```

### Windows PowerShell

```powershell
Expand-Archive .\wepush-next-1.1.0-windows-x64.zip .\wepush-next
Set-Location .\wepush-next\wepush-next-1.1.0
Set-ExecutionPolicy -Scope Process Bypass
& .\install\install.ps1
```

安装完成后打开：

- WebUI：`http://127.0.0.1:18990/`
- 健康检查：`http://127.0.0.1:18990/actuator/health/readiness`
- OpenAPI：`http://127.0.0.1:18990/openapi.yaml`

也可以在另一个终端验证：

```bash
curl --fail http://127.0.0.1:18990/actuator/health/readiness
curl --fail http://127.0.0.1:18990/api/v1/system/info
```

需要便携运行而不安装系统服务时，Linux/macOS 在解压目录执行 `./bin/wepush-service`；Windows 执行 `install\windows\run-service.ps1`。便携模式应从解压目录启动，使默认 `.local` 数据与该目录保持在一起。完整包优先使用内含 Runtime，精简包自动使用系统 Java。

## 4. 可选：使用 Desktop UI

先按上一节安装 Service，再解压对应平台的 Desktop 附件：

- Linux：运行 `wepush-next-desktop-1.1.0-linux-x64/WePush Next/wepush-next`。
- macOS：打开 `wepush-next-desktop-1.1.0-macos-arm64/WePush Next.app`。
- Windows：运行 `wepush-next-desktop-1.1.0-windows-x64\WePush Next\WePush Next.exe`。

Desktop 会连接本机 `http://127.0.0.1:18990`。设置页可以检测、启动、停止 Service，读取最近日志并生成不包含 Token/Secret 的诊断；涉及系统服务或插件目录的写操作会请求操作系统管理员授权。

Desktop 的 API Token 通过 Electron `safeStorage` 写入操作系统原生安全存储；如果 Linux 桌面没有可用 Keyring，Desktop 会拒绝以弱后端持久化。浏览器 WebUI 默认只写 `sessionStorage`，关闭标签页后即清除，不再使用长期 `localStorage`。

由于发行包没有商业签名，macOS Gatekeeper 或 Windows SmartScreen 可能显示未知开发者/发布者。只有在文件来自项目 Release 且 SHA-256 校验一致时，才使用系统提供的“打开”流程；不要全局关闭系统安全能力。

不想安装 Desktop 时，直接使用浏览器 WebUI 即可，两种 UI 连接的是同一套 Service API 和数据。

### 4.1 `1.1.0` 设置与主题

- 顶部主题选择支持亮色、暗色和跟随系统；选择保存在本地浏览器偏好中，不会上传。
- 系统管理员可在设置页为当前 Workspace 设置 Agent、活动 Run、总发送并发、Artifact 容量和默认保留期。降低上限不会自动删除资源；超过新上限时，新增操作会给出可诊断拒绝。
- “生成诊断包”会由 Service 返回结构化脱敏 ZIP；仍应把它视为内部运维资料，不要公开上传。
- “检查版本”只有点击时才查询 GitHub 的稳定 `next-v*` Release，不会后台检查、自动下载或发送遥测。
- Account 的认证熔断面板显示跨 Run 认证失败和冷却状态。先修正 Secret/凭据，再由管理员复位；网络失败、限流或 Recipient 错误不会计入认证熔断。
- 运营商短信使用 Release 中独立的 CMPP/SMGP/SGIP/SMPP 签名插件。安装前校验 `SHA256SUMS` 并配置发行公钥；这些插件当前处理主动提交和长短信，不处理状态报告或上行短信。

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

进入“受众”，新建 UTF-8 CSV 文件：

```csv
itemId,mobile,name
alice,13000000000,Alice
bob,13100000000,Bob
```

1. 填写受众名称并选择 CSV；TXT 也受支持，每个非空行作为一条 Recipient。
2. 选择 `itemId` 列，把 `mobile`、`name` 映射到同名 Recipient 字段。
3. 点击“上传并预览”，核对总行数、接受数、错误数和重复数。
4. 有错误时先下载错误行 CSV；重复 `itemId` 只保留首次出现的记录。
5. 点击“确认并生成 Snapshot”。

`itemId` 应在同一受众中保持稳定且唯一；字段必须覆盖消息模板引用的变量。更新已有受众时请选择目标受众，提交后会生成新的不可变 Snapshot，不会改变既有 Run。

### 5.4 创建任务并运行

1. 进入“任务与调度”。
2. 选择刚创建的账号、消息和受众。
3. 保持较小的目标并发，例如 `2`。
4. 保存任务。
5. 在右侧任务卡片点击 `Dry Run`。
6. 进入“运行中心”，查看状态、计数、事件和每条 Recipient 的结果。

正常情况下，两条结果会以 Provider Code `DRY_RUN` 成功完成。该过程不会请求 `example.com/messages`。

## 6. 正式 HTTP 发送

完成 Dry Run 后，在任务卡片点击“正式发送”。界面会先展示 Provider/版本、账号、受众及数量、完整策略、目标并发、限速和预计执行规模。只有再次点击“我已核对，开始发送”才会创建真实 Run；确认令牌五分钟有效，并绑定当前资源版本，资源变化后必须重新核对。

> 将 `dryRun` 设为 `false` 会真实访问 HTTP 目标。请先使用专用测试端点、小范围受众和服务端幂等机制验证，不要把上面的 `example.com` 示例用于正式运行。

运行结束后进入“运行中心”。PARTIAL 或 FAILED Run 可以选择 FAILED、UNKNOWN、UNSENT 状态并点击失败重发；Service 会显示匹配 Item 数量，再创建带来源 Run ID 的新 Run。重发使用来源 Run 的冻结 Snapshot，不会混入之后编辑的 Message 或 Audience。

直接调用 API 时同样必须先获取确认上下文：

```bash
curl --request POST \
  http://127.0.0.1:18990/api/v1/workspaces/ws_default/jobs/<JOB_ID>/run-confirmation

curl --request POST \
  --header 'Content-Type: application/json' \
  --header 'Idempotency-Key: replace-with-a-unique-value' \
  --data '{"dryRun":false,"policyOverrides":{},"reason":"manual-live-run","confirmationToken":"<TOKEN_FROM_PREVIOUS_RESPONSE>"}' \
  http://127.0.0.1:18990/api/v1/workspaces/ws_default/jobs/<JOB_ID>/runs
```

开启 API Security 后还需添加：

```text
Authorization: Bearer <TOKEN>
```

HTTP Provider 不跟随重定向，默认阻止私有地址，限制响应体大小，并支持配置成功状态码与幂等 Header。正式接入前应按目标系统的认证、限流、超时和幂等约定完成配置。

SMTP、机器人、短信和微信系渠道使用相同的 Account → Message → Audience → Job → Dry Run → 正式确认流程。每个渠道的最小 Account、Message、Recipient、SecretRef、连接测试边界、限流和 Provider Code 处理见[《内置 Provider 指南》](provider-guide.md)。所有正式发送都使用用户自己的渠道账号与额度，WePush 不提供集中账号、代充值或平台代理发送。

## 7. API 文档与 Java SDKs

WebUI 的“API 文档”会读取 Service 提供的 OpenAPI，支持查看请求 Schema、生成请求和动态调试。写操作发送前会二次确认；权限、审计和同源限制仍由 Service 强制执行。

Remote Java SDK 是 Service API 客户端，不依赖 Core、Engine 或具体 Provider。`beta.1` 发行包和独立 SDK 压缩包都包含它，安装说明见 [Java SDK README](../sdk/README.md)。

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

`beta.1` 发行包和独立 Java SDK 附件同时包含 Embedded SDK 与所需依赖。完整依赖、配置、资源所有权和示例见 [Embedded Java SDK README](../sdk/embedded-java/README.md)。

## 8. 何时需要 Agent

默认内嵌执行模式下，Service 自己调用 Core Engine，本地体验不需要 Agent。以下情况再部署 Agent：

- 任务需要在另一台主机或受控网络中执行。
- 希望 Service 只承担控制面，执行资源独立扩缩容。
- 不同执行节点需要安装不同 Provider 插件。
- 需要借助 Lease、Fencing、心跳和远端 Artifact 回传管理分布式运行。

Agent 使用 Service 的 `19090` gRPC 控制流。跨主机部署必须使用 Enrollment、长期 Credential 和 TLS/mTLS，不应沿用回环开发的明文配置。完整步骤见[部署与运维文档](deployment-and-operations.md)。

## 9. 安装为系统服务

统一 `install/install.sh` 或 `install/install.ps1` 默认安装 Standalone Service。高级部署可直接调用平台目录下的安装器并传入 `service`、`agent` 或 `all`。

### Linux / systemd

```bash
tar -xzf wepush-next-1.1.0-linux-x64.tar.gz
sudo ./wepush-next-1.1.0/install/install.sh
systemctl status wepush-next-service
```

### macOS / launchd

从普通登录用户的终端使用 `sudo`，安装器会将该用户作为非 root 服务用户：

```bash
ditto -x -k wepush-next-1.1.0-macos-arm64.zip .
sudo ./wepush-next-1.1.0/install/install.sh
launchctl print system/com.fangxuele.wepush-next.service
```

无人值守安装需要显式设置已经存在的 `WEPUSH_SERVICE_USER`。

### Windows Service

以管理员 PowerShell 执行：

```powershell
Expand-Archive .\wepush-next-1.1.0-windows-x64.zip .\release
Set-ExecutionPolicy -Scope Process Bypass
& .\release\wepush-next-1.1.0\install\install.ps1
Get-Service WePushNextService
```

Windows 发行包已经携带并再次校验固定版本 WinSW；安装阶段不访问网络。精简包也携带 WinSW，只有 Java 由系统提供。

配置路径、备份、升级、卸载、Server/HA、PostgreSQL、S3 与插件步骤见[部署与运维文档](deployment-and-operations.md)。

## 10. 数据、备份与安全

便携 Standalone 默认保存：

- SQLite：`.local/data/wepush-next.db`
- Secret 主密钥：`.local/secrets/master-key.json`
- Artifact：`.local/artifacts`

系统服务安装后的路径因操作系统而异，具体以部署文档和生成的 `service.env` 为准。正式 `backup` 会停止相关服务，写入格式/版本/内容清单和每个文件的 SHA-256，再调用 Restore 校验；`restore` 会先保留恢复前副本，替换后执行安装健康门，失败则自动恢复原数据。数据库、Secret 主密钥、Artifact、Agent Identity、Journal、Event/Completion Outbox 和插件必须作为一个一致备份恢复。

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
- [内置 Provider 指南](provider-guide.md)
- [架构与概要设计](architecture-and-high-level-design.md)
- [详细设计](detailed-design.md)
- [部署与运维](deployment-and-operations.md)
- [实现状态](implementation-status.md)
- [本版本 Release Notes](releases/1.1.0.md)
- [安全策略](../SECURITY.md)

普通问题和功能建议可提交 GitHub Issue。安全漏洞不要公开披露，请按 `SECURITY.md` 中的方式私下报告。
