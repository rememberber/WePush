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
}
```

## 当前开发基线

- Core API、Provider SPI、虚拟线程 Engine 与 HTTP Provider。
- Agent Protocol、Sequence/Fencing Runtime 与可执行 Agent 包。
- Service 分层、Provider 目录、动态 Schema、Actuator 与 OpenAPI。
- 独立远程 Java SDK。
- React WebUI、Electron 安全外壳和共享前端 packages。
- 架构、单元、契约、Service 冒烟与 Engine→Provider 集成测试。

模块边界和后续迭代以 [`docs/architecture-and-high-level-design.md`](docs/architecture-and-high-level-design.md)、[`docs/detailed-design.md`](docs/detailed-design.md) 及 [`docs/adr/`](docs/adr/) 为准。
