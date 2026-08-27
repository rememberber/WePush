# WePush Next

WePush Next 是与 Classic 完全独立的新产品线。Classic 与 Next 可以按需拥有重复代码，彼此不建立共享源码依赖。

> 产品定位：WePush Next 是开源、可下载、可安装、由用户自行部署和运维的消息推送产品。项目不建设官方公共 SaaS、注册计费订阅、公共租户平台或云 KMS/Secret Manager 集成。正式范围和分版本计划见[《产品目标、边界与路线图》](docs/product-scope-and-roadmap.md)。

> 当前版本：`0.1.0-alpha.2` Public Preview。它不是稳定版，Desktop 与安装包未使用商业代码签名。下载、校验和已知边界见 [`PREVIEW-NOTICE.md`](PREVIEW-NOTICE.md) 与 [`docs/releases/0.1.0-alpha.2.md`](docs/releases/0.1.0-alpha.2.md)。

第一次下载和使用请从[《WePush Next 对外使用指南》](docs/user-guide.md)开始。该指南覆盖发行物选择、三平台启动/安装、Desktop 连接、首个 HTTP Provider Dry Run、正式 API 调用、数据备份和常见问题。

## 验证 Java 工程

```bash
cd next
./mvnw verify
```

完整构建、三平台安装、Server/HA、Agent Enrollment、插件升级和恢复步骤见 [`docs/deployment-and-operations.md`](docs/deployment-and-operations.md)。安全问题请按 [`SECURITY.md`](SECURITY.md) 私下报告。

## 启动 Service

```bash
cd next
./mvnw -pl service/service-app -am package -DskipTests
java -jar service/service-app/target/wepush-next-service.jar
```

Service 默认只监听 `127.0.0.1:18990`。启动后可访问：

- `http://127.0.0.1:18990/actuator/health`
- `http://127.0.0.1:18990/api/v1/system/info`
- `http://127.0.0.1:18990/api/v1/providers`
- `http://127.0.0.1:18990/api/v1/agents`
- `http://127.0.0.1:18990/openapi.yaml`

本地无认证模式只允许监听回环地址；将 `WEPUSH_BIND_ADDRESS` 改为非回环地址时必须同时开启 `WEPUSH_SECURITY_ENABLED=true` 并配置足够强度的 `WEPUSH_BOOTSTRAP_TOKEN`，否则 Service 拒绝启动。

Standalone 数据默认保存到 `.local/data/wepush-next.db`，可通过 `WEPUSH_DATABASE_PATH` 指定其他位置。Service 首次启动会运行 Flyway 迁移并创建 `ws_default` 工作区。

Secret 默认使用本地信封加密：密文进入 SQLite，主密钥单独保存到 `.local/secrets/master-key.json`。可通过 `WEPUSH_MASTER_KEY_PATH` 修改路径，或使用 `WEPUSH_MASTER_KEY_BASE64` 注入 32-byte Base64 主密钥。已有密文但主密钥缺失、权限不安全或认证失败时，Service 会失败关闭，不会生成新密钥覆盖。

Artifact 默认保存到 `.local/artifacts`，SQLite 只保存元数据、SHA-256、大小和生命周期状态。可通过 `WEPUSH_ARTIFACT_ROOT` 修改根目录；临时结果导出默认保留 24 小时，可通过 `WEPUSH_EXPORT_RETENTION` 使用 ISO-8601 Duration 调整。Service 会按 `WEPUSH_RETENTION_INTERVAL` 周期回收过期且未 Pin/Legal Hold 的文件。

## 启动 WebUI 开发环境

```bash
cd next/ui
pnpm install
pnpm check
pnpm dev
```

WebUI 默认运行在 `http://127.0.0.1:5173`，开发代理连接本地 Service。界面、API Client、Schema Renderer、设计 Token 与 Electron Desktop 外壳均在同一个 pnpm Workspace 内。

## 启动 Agent

```bash
cd next
./mvnw -pl agent/agent-app -am package -DskipTests
java -jar agent/agent-app/target/wepush-next-agent.jar
```

Agent 默认主动连接 `127.0.0.1:19090` 的 gRPC 双向控制流，发送 Hello 和周期心跳，断线后使用带抖动的指数退避重连；Sequence 与 Lease Fence Journal 默认保存在 `.local/agent/agent-state.properties`。

常用环境变量：

- `WEPUSH_AGENT_ID`：稳定 Agent 身份，默认 `local-agent`。
- `WEPUSH_SERVICE_HOST` / `WEPUSH_AGENT_GRPC_PORT`：Service gRPC 地址，默认 `127.0.0.1:19090`。
- `WEPUSH_AGENT_GRPC_TOKEN`：仅用于回环开发/Bootstrap 的共享 Token；正式 Agent 使用 Enrollment Credential。
- `WEPUSH_AGENT_GRPC_PLAINTEXT`：本地开发默认 `true`；远端部署应关闭并使用 TLS。
- `WEPUSH_AGENT_STATE_PATH`：Agent Journal 文件位置。

Service 的 gRPC 端口默认只绑定回环地址。暴露到非回环地址时强制 TLS，HTTP Lease/Artifact 与 gRPC 都拒绝匿名 Agent；正式生产通过一次性 Enrollment 获取长期 Credential 和客户端证书，并使用 mTLS。共享 Token 只保留为回环开发兼容入口。

要把 Run 交给独立 Agent 执行，Service 使用以下配置启动：

```bash
WEPUSH_EXECUTION_MODE=remote \
WEPUSH_AGENT_PUBLIC_BASE_URL=http://127.0.0.1:18990 \
java -jar service/service-app/target/wepush-next-service.jar
```

远端模式会按 Provider ID/版本和可用容量选择在线 Agent，持久化带 Epoch/Fencing Token 的 Lease。Agent 校验冻结 Execution Spec 与 Audience 的 SHA-256 后 ACK，随后使用同一 Core Engine 执行，并经 gRPC 回传事件、Item Result、命令确认和 Run Summary。

需要 Secret 的远端 Run 会使用 Agent 在 Hello 中发布的会话级 X25519 公钥。Service 只解析冻结配置中实际引用的最小 Secret 集，使用一次性 X25519、HKDF-SHA-256 和 AES-256-GCM 加密，并绑定 Agent、Run、Lease、Epoch、Fencing Token 与过期时间。Agent 仅在内存中解密，运行结束立即清零，不写入 Journal。正式跨主机部署仍应关闭明文 gRPC 并配置 TLS；Secret Envelope 不替代 Agent 身份认证和传输层安全。

## Java SDKs

Next 提供两种独立 SDK。Remote SDK 通过 HTTP 调用 Service，只依赖公开 `service-api`，不依赖 Core、Engine 或具体 Provider：

公开预览发行包在 `sdk/` 中附带可安装到本地 Maven Repository 的 POM/JAR，步骤见 [`sdk/README.md`](sdk/README.md)。

```java
try (var client = WePushClient.builder()
        .endpoint(URI.create("http://127.0.0.1:18990"))
        .build()) {
    var system = client.system().info();
    var providers = client.providers().list();
    var agents = client.agents().list();
    var workspace = client.workspace("ws_default");
    var runs = workspace.runs();
    var artifacts = workspace.runArtifacts(runs.getFirst().id());
}
```

Embedded SDK 则在调用方 JVM 内直接运行 Engine，显式装配所需 Provider，不启动 Service、Agent 或数据库：

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

从源码安装、依赖声明和完整示例见 [`sdk/README.md`](sdk/README.md) 与 [`sdk/embedded-java/README.md`](sdk/embedded-java/README.md)。`alpha.2` 发行包和独立 Java SDK 附件同时提供 Remote SDK 与 Embedded SDK。

## 当前开发基线

- Core API、Provider SPI、虚拟线程 Engine 与 HTTP Provider。
- Agent Protocol、Protobuf/gRPC 双向控制流、Sequence/Fencing Runtime、持久 Journal、加密 Secret Envelope、远端 Core 执行适配与常驻 Agent 包。
- Service 分层、SQLite/Flyway、控制面 CRUD、Agent 注册/心跳/持久 Lease、信封加密 Secret Store、Result/Command/Artifact 持久化、本地 Artifact Store、Run 幂等创建、SSE，以及可切换的内嵌/远端执行器。
- 相互独立的 Remote Java SDK、Embedded Java SDK 和 TypeScript API Client。
- React WebUI、可视化 Account/Message/Audience/Job 创建闭环、可控制运行中心、动态 API 文档、Electron 安全外壳和共享前端 packages。
- 架构、单元、契约、Service 冒烟与 Account→Run→Engine→Provider 纵向测试。

产品范围和迭代优先级以 [`docs/product-scope-and-roadmap.md`](docs/product-scope-and-roadmap.md) 为准；模块边界和实现细节以 [`docs/architecture-and-high-level-design.md`](docs/architecture-and-high-level-design.md)、[`docs/detailed-design.md`](docs/detailed-design.md)、[`docs/implementation-status.md`](docs/implementation-status.md)、[`docs/deployment-and-operations.md`](docs/deployment-and-operations.md) 及 [`docs/adr/`](docs/adr/) 为准。
