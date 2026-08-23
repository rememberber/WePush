# ADR-0006：PostgreSQL Server 模式控制面高可用

- 状态：已接受
- 日期：2026-08-22
- 决策者：WePush 项目维护者

## 背景

Standalone 模式使用单 Service 和 SQLite，不需要控制面集群。Server 模式需要支持 Service 实例故障、Agent 重连、调度互斥、并发领取 Run 和任意实例提供 API/SSE。方案应尽量减少必须部署的中间件。

## 决策

### 基线拓扑

- Server 模式数据库基线为 PostgreSQL 18.x。
- 正式 HA 至少部署两个无状态 Service 实例，位于支持 HTTP/2 和 gRPC 的负载均衡器后。
- PostgreSQL 是业务状态和协调状态的唯一持久化事实源。
- PostgreSQL 自身的复制、故障转移和备份交给托管数据库或独立数据库运维方案，不由 WePush 安装包自行组建数据库集群。
- Artifact 使用 S3-compatible Store，不使用 Service 本地磁盘共享状态。
- 首期 HA 不强制引入 Redis、Kafka、ZooKeeper 或独立任务队列。

### API 和 Agent 连接

- REST 请求可以落到任意 Service 实例，不要求 Sticky Session。
- SSE 可以连接任意实例，历史游标从 PostgreSQL 读取，新事件通过本地发布加数据库轮询补偿获得。
- Agent gRPC 流连接一个 Service 实例；实例失败时 Agent 重连到负载均衡器并恢复 Sequence、Lease 和事件上报。
- 当前 Agent 连接的 Owner Instance 只作为可丢失的路由提示，不成为持久化真相。

### 调度互斥

- 只有持有 PostgreSQL Session-level Advisory Lock 的 Service 实例运行 Schedule Scanner。
- 使用专用数据库连接持有锁，连接断开时 PostgreSQL 自动释放锁。
- 其他实例持续尝试获取锁，成为新的调度 Leader。
- 每次 Schedule 触发仍使用数据库幂等键，Leader Lock 不是防重复的唯一手段。

### Run 领取和 Lease

- 候选 Run 通过短事务、Run 状态条件更新和“每个 Run 仅一个 Active Lease”的数据库唯一索引领取；并发冲突失败后由下一次扫描重试。批量吞吐需要提高时可以在 PostgreSQL Repository 内加入 `FOR UPDATE SKIP LOCKED`，不改变应用层语义。
- 创建 Lease 时递增 Epoch 并生成随机 Lease Token。
- 同一 Run 同时最多一个 Active Lease。
- 所有 Agent 写入校验 Lease ID、Epoch 和 Token，拒绝旧 Agent 的过期写入。
- Service 实例故障不会自动宣布 Run 失败；由 Lease Timeout 和恢复状态机判断。

### 实例间通知与持久消息

- Service→Agent 的 Lease Offer 和 Run Command 先写 `agent_message_outbox`，所有 Service 周期扫描；只有持有 Agent 当前 gRPC 流的实例发送，ACK 后关闭消息。
- SSE 以 `run_event` 为事实源，每个实例只轮询本机存在订阅者的 Run Cursor，从而观察其他实例提交的事件。
- PostgreSQL `LISTEN/NOTIFY` 可以在后续作为低延迟唤醒优化，但不得替代 outbox、事件表和周期扫描。
- 禁止把 LISTEN/NOTIFY 当成持久消息队列。

### 数据库迁移

- 发布前或滚动启动前只运行一个 Migration Job。
- Service Readiness 在数据库版本不兼容时失败。
- 迁移采用 Expand、Migrate、Contract，保证滚动期间相邻 Service 版本可短期共存。
- 破坏性 Contract 迁移只能在旧版本实例全部退出后执行。

## 故障行为

| 故障 | 预期行为 |
|---|---|
| 一个 Service 实例退出 | REST/SSE 重连；Agent gRPC 重连；其他实例继续服务 |
| 调度 Leader 退出 | Advisory Lock 自动释放，其他实例接任 |
| 一个轮询周期未观察到新事件/消息 | 下一周期继续从持久游标扫描补偿 |
| PostgreSQL 暂时不可用 | Service Readiness 失败，不创建新 Run；Agent 保留有界 Outbox |
| PostgreSQL 主从切换 | Service 重建连接，重新竞选调度 Leader |
| Agent 连接到新实例 | 使用 Sequence 和 Lease Epoch 恢复，不重复接受旧写入 |

## 非目标

- 不在 WePush 内实现 PostgreSQL 选主和数据复制。
- 不承诺跨区域 Active-Active 控制面。
- 不通过分布式锁实现外部渠道 Exactly Once。
- SQLite Standalone 不支持多 Service 实例并发访问同一个数据库文件。

## 参考

- [PostgreSQL Advisory Locks](https://www.postgresql.org/docs/current/explicit-locking.html)
- [PostgreSQL LISTEN](https://www.postgresql.org/docs/current/sql-listen.html)
- [PostgreSQL SELECT Locking](https://www.postgresql.org/docs/current/sql-select.html)
