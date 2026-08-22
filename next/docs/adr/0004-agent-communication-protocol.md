# ADR-0004：Agent 长期通信协议

- 状态：已接受
- 日期：2026-08-22
- 决策者：WePush 项目维护者

## 背景

Agent 与 Service 之间需要长期传输注册信息、心跳、能力、Lease、运行命令、事件批次和完成结果。该连接是双向、长生命周期、需要顺序和背压的机器间协议，与浏览器运行监控和大文件传输的需求不同。

## 决策

不同类型流量使用不同协议，不建立一个包办所有流量的通道。

### Agent 控制流

- 远程 Agent 的正式长期协议使用 gRPC 双向流，承载于 HTTP/2。
- 使用 Protocol Buffers 定义 `AgentControlService.Connect` 双向流。
- Agent 主动建立出站连接，Service 不主动连接 Agent。
- 同一逻辑流承载 Hello、Capability、Heartbeat、Lease Offer/Ack、Command/Ack、Run Event Batch、Run Summary 和 Drain 状态。
- 每个方向具有独立单调 Sequence；应用层仍进行幂等和断线恢复，不能仅依赖单次 gRPC 流的顺序保证。
- Lease 继续使用 Epoch 和 Fencing Token，gRPC 连接本身不等于 Lease 所有权。
- Agent 断线后指数退避并带抖动重连，可连接任一健康 Service 实例。
- 设置明确 Deadline、Keepalive、最大消息大小和流控上限。

### 注册和管理

- Agent Enrollment、Credential 轮换、插件目录查询等低频管理操作使用 HTTPS REST 或 gRPC Unary；首期优先复用 Service HTTPS REST 管理入口。
- Agent 正式连接使用独立 Agent 身份，目标安全基线为 mTLS；Bootstrap 阶段可以使用一次性 Enrollment Token。

### 浏览器和 SDK

- WebUI、Desktop UI 和远程 SDK 的 Run 实时事件继续使用 SSE。
- 用户命令继续使用 REST API。
- 不使用 WebSocket 作为 Agent 或浏览器的默认协议。

### Artifact

- Audience Snapshot、结果、日志和插件包不通过 gRPC 控制流传输。
- Agent 使用 Service 签发的短期 HTTPS URL 直接上传或下载 Artifact。
- gRPC 消息只携带 Artifact ID、校验值、大小、状态和短期传输描述。

### 版本兼容

- Proto Package 使用主版本，例如 `wepush.agent.v1`。
- 已发布字段编号永不复用；删除字段必须 `reserved`。
- 新字段优先使用 `optional` 或新消息类型，旧 Agent 忽略未知字段。
- Service 维护明确的 Agent 协议兼容窗口。

## 为什么不选择其他方案

- 长轮询适合快速 MVP，但命令、事件、心跳和背压最终会形成多个相互协调的 HTTP 循环。
- SSE 是服务端到客户端单向流，不能独立承担 Agent 双向控制。
- WebSocket 可以双向通信，但缺少 Protobuf/gRPC 的服务契约、生成代码、Deadline 和标准流控能力。
- gRPC 不适合直接服务浏览器交互文档和大 Artifact，因此不替代 REST、SSE 和对象存储。

## 实施策略

Embedded Agent 直接调用 Java 接口，不经过网络。远程 Agent 功能开始实现时直接建设 gRPC，不先发布需要长期兼容的长轮询协议。

## 参考

- [gRPC Core Concepts](https://grpc.io/docs/what-is-grpc/core-concepts/)
- [Protocol Buffers Language Guide](https://protobuf.dev/programming-guides/proto3/)
