# ADR-0008：Workspace 多租户范围

- 状态：已接受
- 日期：2026-08-22
- 决策者：WePush 项目维护者

## 背景

Standalone 通常只有一个用户，但 Server 模式会出现多个团队、账号、Agent 和权限边界。如果完全按单租户实现，后续增加隔离需要修改所有表、唯一索引、Repository、API、Artifact 和 Secret。另一方面，首期直接建设公共 SaaS 的计费、租户自助和物理隔离会显著扩大范围。

## 决策

Workspace 逻辑多租户进入 WePush Next 正式产品范围，但公共多租户 SaaS 不进入首期范围。

### Standalone

- 初始化时自动创建一个 Default Workspace。
- UI 默认隐藏 Workspace 切换和管理复杂度。
- 数据、API 和 Repository 内部仍显式携带 Workspace ID。

### Server

- 支持多个 Workspace。
- 用户通过 Role Binding 获得某个 Workspace 的 ADMIN、OPERATOR 或 VIEWER 权限。
- Account、Secret、Message、Audience、Job、Schedule、Run、Artifact、Agent Pool 和 API Token 都归属 Workspace。
- Agent 可以加入一个或多个明确授权的 Agent Pool，Lease 只能分配给 Run 所属 Workspace 可用的 Pool。

### API 范围

Workspace 业务资源使用显式路径：

```text
/api/v1/workspaces/{workspaceId}/accounts
/api/v1/workspaces/{workspaceId}/messages
/api/v1/workspaces/{workspaceId}/audiences
/api/v1/workspaces/{workspaceId}/jobs
/api/v1/workspaces/{workspaceId}/schedules
/api/v1/workspaces/{workspaceId}/runs
/api/v1/workspaces/{workspaceId}/agents
```

身份、公开 Provider Catalog、系统 Health 和 Agent Internal API 不使用该业务路径，但仍按自身规则校验 Scope。

Java SDK 创建 `WorkspaceClient` 后执行资源操作，避免每个方法重复传 Workspace ID：

```java
WorkspaceClient workspace = client.workspace(workspaceId);
workspace.jobs().startRun(jobId, request);
```

### 数据隔离

- 所有 Workspace 资源表包含非空 `workspace_id`。
- 唯一索引包含 `workspace_id`，例如 `(workspace_id, provider_id, name)`。
- Repository 方法必须接收 `WorkspaceId`，禁止提供无 Scope 的通用 `findById(id)` 给业务用例。
- API ID 即使全局唯一，也不能替代 Workspace 权限检查。
- 跨 Workspace 管理操作集中在独立 System Administration 用例中。
- PostgreSQL Row Level Security 可以作为未来防御层，但不作为首期正确性的唯一保障。

### Secret、Artifact 和指标

- Secret 认证附加数据绑定 Workspace ID。
- Artifact Object Key 以 Workspace ID 分区。
- Presigned URL 在签发前校验 Workspace 权限。
- 审计事件记录 Workspace ID。
- 指标默认不把 Workspace ID 作为高基数 Label；按 Workspace 的统计通过查询模型提供。

### 配额

Server 支持 Workspace 级策略入口，包括最大 Agent、并发 Run、总并发、Artifact 容量和保留期。首期可以只实现默认策略，但模型和校验入口必须存在。

## 明确不做

- 首期不提供计费、订阅套餐、租户自助注册和公共市场。
- 不承诺恶意租户之间的进程级或数据库级物理隔离。
- 不为每个 Workspace 自动创建独立数据库。
- 不允许 Provider 插件执行任意跨 Workspace 查询。

## 结果

Workspace 从第一张表、第一条 API 和第一个 Artifact 开始成为正式边界，可以避免未来代价高昂的数据改造。Standalone 通过 Default Workspace 保持简单体验。代价是每个用例、Repository 和测试都必须验证 Workspace Scope。
