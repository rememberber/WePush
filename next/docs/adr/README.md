# WePush Next 架构决策索引

本目录保存 WePush Next 已接受的跨组件架构决策。架构与详细设计描述当前整体状态；ADR 记录选择原因、边界和后果。发生冲突时，应先确认 ADR 是否已被后续决策替代，再同步更新设计文档。

| ADR | 状态 | 决策摘要 |
|---|---|---|
| [ADR-0001](0001-dual-track-development.md) | 已接受 | Classic 与 Next 双轨独立发展，允许按需重复代码 |
| [ADR-0002](0002-technology-baseline.md) | 已接受 | Java 21、Spring Boot 4.1.x、TypeScript/Vite/React、Electron 技术基线 |
| [ADR-0003](0003-default-secret-store.md) | 已接受 | 默认使用本地 AES-256-GCM 信封加密 Secret Store |
| [ADR-0004](0004-agent-communication-protocol.md) | 已接受 | Agent 使用 gRPC 双向流，UI/SDK 事件使用 SSE |
| [ADR-0005](0005-provider-plugin-lifecycle.md) | 已接受 | PF4J、签名插件、ClassLoader 隔离和滚动重启更新 |
| [ADR-0006](0006-postgresql-control-plane-ha.md) | 已接受 | PostgreSQL 18、无状态 Service、Advisory Lock 和 Fencing |
| [ADR-0007](0007-artifact-store-and-retention.md) | 已接受 | Standalone 本地制品、Server S3-compatible 制品及保留策略 |
| [ADR-0008](0008-workspace-multitenancy-scope.md) | 已接受 | Workspace 逻辑多租户进入正式 Server 范围 |

新增或替代决策时使用下一个连续编号，并在状态中明确“提议、已接受、已替代或已拒绝”。替代已有 ADR 时，旧 ADR 保留历史并链接到新 ADR。
