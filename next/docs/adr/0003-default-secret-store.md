# ADR-0003：默认 Secret Store

- 状态：已接受
- 日期：2026-08-22
- 决策者：WePush 项目维护者

## 背景

WePush 需要保存短信、微信、邮件、HTTP 等 Provider 的 Token、密码和私钥。默认实现必须同时适用于 Windows、Linux、macOS、Headless Service 和容器环境，且不能依赖 Desktop 进程或某个特定云厂商。

## 决策

默认实现采用本地信封加密 Secret Store，名称为 `LocalEnvelopeSecretStore`。

### 数据加密

- 每次 Secret 写入生成随机 Data Encryption Key（DEK）。
- Secret 使用 AES-256-GCM、随机 Nonce 和认证附加数据加密。
- DEK 使用版本化 Master Key（KEK）加密。
- 数据库只保存 Ciphertext、Nonce、Encrypted DEK、算法、Key Version 和更新时间。
- 认证附加数据绑定 Workspace ID、Secret Set ID、Secret Name 和记录版本，防止密文被移动到其他记录后解密。

### Master Key

- Master Key 与业务数据库分开存放。
- Standalone 初始化时生成 256 位随机 Master Key，保存在独立 Key File。
- Linux 和 macOS Key File 权限必须为 Service 用户独占，目标权限为 `0600`。
- Windows 使用显式 ACL，只允许 Service 身份和受控管理员访问。
- Server 或容器模式优先从只读挂载文件读取 Master Key；允许显式环境变量注入，但不作为推荐方式。
- Server HA 的所有 Service 实例必须挂载同一份 Active/Retained Key Ring；Key Ring 版本变化通过受控部署传播，不能由各实例独立生成。
- Server 模式在未执行显式初始化时不得静默生成新 Master Key，防止重建容器后产生无法解密的数据。

### 密钥轮换

- 每个 Master Key 具有 `keyVersion`。
- 新写入使用当前 Active Key。
- 读取支持仍在保留期内的旧 Key。
- 轮换任务逐批重新封装 DEK，不需要重新加密完整 Secret 内容。
- 删除旧 Key 前必须证明不存在引用，并生成审计记录。

### API 和运行时

- Secret 普通查询只返回 `configured`、`updatedAt` 和版本摘要，不返回明文。
- Secret 不进入日志、Run Event、Artifact、OpenAPI 示例或浏览器持久化存储。
- Agent 只获取当前 Run 所需的最小 Secret Envelope，并绑定 Agent、Run、Lease、Epoch 和过期时间。
- Agent 不将解密 Secret 写入 Local Journal。
- Electron 的 `safeStorage` 不作为 Service Secret Store，因为 Service 必须能在无 Desktop 会话的 Headless 环境运行。

### 扩展

`SecretStore` 保持 Port 接口，未来可以增加 HashiCorp Vault、云 KMS 或操作系统凭据存储适配器，但默认安装不依赖它们。

## 失败策略

- Master Key 缺失、权限不安全、Key Version 未知或认证标签校验失败时，Service Fail Closed。
- 不允许自动创建新 Key 后继续启动已有数据库。
- 解密失败不得把 Ciphertext 或密钥材料写入异常消息。
- Readiness 显示 Secret Store 不可用，但不暴露具体密钥路径和内容。

## 结果

该方案跨平台、可离线、适合安装包和 Headless Service，同时为以后接入外部 Secret Manager 保留接口。代价是项目需要自行实现密钥初始化、备份、权限检查和轮换流程。
