# WePush Next

WePush Next 是与 Classic 完全独立的新产品线。Classic 与 Next 可以按需拥有重复代码，彼此不建立共享源码依赖。

## 验证 Java 工程

```bash
cd next
./mvnw verify
```

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
- `http://127.0.0.1:18990/openapi.yaml`

Standalone 数据默认保存到 `.local/data/wepush-next.db`，可通过 `WEPUSH_DATABASE_PATH` 指定其他位置。Service 首次启动会运行 Flyway 迁移并创建 `ws_default` 工作区。

Secret 默认使用本地信封加密：密文进入 SQLite，主密钥单独保存到 `.local/secrets/master-key.json`。可通过 `WEPUSH_MASTER_KEY_PATH` 修改路径，或使用 `WEPUSH_MASTER_KEY_BASE64` 注入 32-byte Base64 主密钥。已有密文但主密钥缺失、权限不安全或认证失败时，Service 会失败关闭，不会生成新密钥覆盖。

## 启动 WebUI 开发环境

```bash
cd next/ui
pnpm install
pnpm check
pnpm dev
```

WebUI 默认运行在 `http://127.0.0.1:5173`，开发代理连接本地 Service。界面、API Client、Schema Renderer、设计 Token 与 Electron Desktop 外壳均在同一个 pnpm Workspace 内。

## 验证 Agent 可执行包

```bash
cd next
./mvnw -pl agent/agent-app -am package -DskipTests
java -jar agent/agent-app/target/wepush-next-agent.jar
```

当前 Agent 包会验证 Provider 发现、能力清单和 Hello/Sequence Journal 基线；正式 gRPC Transport 将在远程 Agent 迭代中接入同一 Runtime。

## Java SDK

远程 SDK 只依赖公开 `service-api`，不依赖 Core、Engine 或具体 Provider：

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

## 当前开发基线

- Core API、Provider SPI、虚拟线程 Engine 与 HTTP Provider。
- Agent Protocol、Sequence/Fencing Runtime 与可执行 Agent 包。
- Service 分层、SQLite/Flyway、控制面 CRUD、信封加密 Secret Store、Result/Command 持久化、Run 幂等创建、SSE 与内嵌执行器。
- 独立远程 Java SDK 和 TypeScript API Client。
- React WebUI、可视化 Account/Message/Audience/Job 创建闭环、可控制运行中心、动态 API 文档、Electron 安全外壳和共享前端 packages。
- 架构、单元、契约、Service 冒烟与 Account→Run→Engine→Provider 纵向测试。

模块边界和后续迭代以 [`docs/architecture-and-high-level-design.md`](docs/architecture-and-high-level-design.md)、[`docs/detailed-design.md`](docs/detailed-design.md)、[`docs/implementation-status.md`](docs/implementation-status.md) 及 [`docs/adr/`](docs/adr/) 为准。
