# ADR-0007：Artifact Store 协议和保留策略

- 状态：已接受
- 日期：2026-08-22
- 修订：2026-08-27，移除云 KMS 产品方向
- 决策者：WePush 项目维护者

## 背景

Audience Snapshot、运行结果、完整响应、日志和插件包可能远大于数据库适合保存的范围。Standalone 需要零依赖本地存储，Server 和 HA 模式需要所有 Service 与 Agent 可访问的共享存储。

## 决策

### 存储实现

- Standalone 默认使用 `LocalFileArtifactStore`。
- Server 默认使用 `S3ArtifactStore`，协议限定为 S3-compatible API 的受控子集。
- 数据库保存 Artifact 元数据和生命周期状态，不把对象存储 List 结果作为事实源。

S3-compatible 最小能力：

- Put Object
- Get Object 和 Range
- Head Object
- Delete Object
- Multipart Upload、Complete 和 Abort
- Presigned Upload/Download URL
- 对象校验和与原生服务端加密配置

### 对象命名

```text
<environment>/<workspaceId>/<artifactType>/<yyyy>/<mm>/<artifactId>
```

- Object Key 不包含用户名、任务名、手机号、邮箱或原始上传文件名。
- Artifact ID 全局唯一且不可变。
- 原始文件名只作为经过清理的下载展示元数据。

### 上传和下载

- Agent 不持有长期对象存储 Credential。
- Service 在权限、Lease 和配额校验后签发短期 Presigned URL。
- URL 绑定方法、Key、Content Length 范围、Checksum 和短期过期时间。
- 上传完成后 Agent 通知 Service，Service通过 Head/Checksum 验证后把 Artifact 置为 `READY`。
- Service 托管流式写入达到 100 MiB 时默认使用 Multipart Upload。Agent 首版直传使用带长度与校验和约束的单次 Presigned Put（当前上限 1 GiB）；后续如提高单 Artifact 上限，可在不改变 Artifact 状态机的前提下扩展 Presigned Multipart Plan。
- 下载支持 Range，用户下载仍先经过 Service 权限检查。

### 完整性和加密

- Artifact 元数据保存 SHA-256、大小和 Content Type。
- 传输使用 TLS。
- Server 模式启用对象存储原生 AES256，或由部署者在其存储层保证等价加密。WePush 不对接或管理云 KMS Key。
- 极敏感 Artifact 可以在客户端加密后再上传，具体类型另行配置。

### 生命周期状态

```text
UPLOADING → READY → DELETING → DELETED
          ↘ FAILED
```

- 删除采用两阶段流程，先标记 `DELETING`，对象删除成功后标记 `DELETED`。
- Object Store 操作幂等，清理任务可以安全重试。
- 数据库引用和 Pin/Legal Hold 阻止进入删除流程。

### 默认保留策略

| Artifact 类型 | 默认保留 |
|---|---|
| Audience Snapshot | 被 Job/Run 引用期间保留，最后引用释放后再保留 30 天 |
| 成功、失败、未知、未发送结果 | Run 结束后 90 天 |
| 结构化运行日志 | Run 结束后 30 天 |
| 完整 Provider 响应体 | Run 结束后 7 天 |
| 导出文件和临时下载 | 创建后 24 小时 |
| 未完成 Multipart Upload | 24 小时后 Abort（对象存储 Lifecycle 兜底） |
| Provider 插件包 | 存在 Agent、Job 或 Run Snapshot 引用期间保留 |

- Workspace 管理员可以在系统允许范围内缩短或延长策略。
- `PINNED` 和 `LEGAL_HOLD` Artifact 不受普通 TTL 删除。
- 对象存储 Lifecycle Rule 可作为兜底，但 Service 数据库清理任务是业务生命周期的主控制器。

## Local Store 等价语义

Local Store 使用相同 Artifact ID、状态、校验和和保留规则。写入先进入临时文件，Flush、fsync 和校验后原子移动到最终路径。路径由 Store 生成，不使用用户输入拼接。

## 结果

该方案使 Standalone 保持自包含，同时让 Server 能使用兼容对象存储。Agent 直传避免 Service 成为大文件带宽瓶颈。代价是需要管理 Presigned URL、Multipart 清理和数据库/对象存储的最终一致性。

## 参考

- [S3 Presigned URLs](https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-presigned-url.html)
- [S3 Multipart Upload](https://docs.aws.amazon.com/AmazonS3/latest/userguide/mpuoverview.html)
- [S3 Object Lifecycle](https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lifecycle-mgmt.html)
