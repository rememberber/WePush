# WePush Next 产品目标、边界与路线图

- 文档状态：已确认产品方向
- 更新日期：2026-08-27
- 适用范围：`next/`

## 1. 产品定位

WePush Next 是一套开源、可下载、可安装、可由用户自行部署和运维的消息推送产品。项目的目标是让个人、团队或组织在自己控制的计算、网络、数据库和存储环境中运行 WePush，并自行掌控账号、消息、受众、运行结果、Secret 和 Artifact。

WePush 项目不运营承载用户业务数据的官方集中服务，也不把公共云平台化作为商业或技术演进方向。Standalone、用户自建 Server/HA、远程 Agent、Remote Java SDK 和 Embedded Java SDK 都服务于同一个目标：让用户在自己的环境中完成安装、集成、发送、升级、备份和恢复。

本文档是 WePush Next 产品范围和迭代优先级的主文档。架构、详细设计、ADR、实现状态和部署文档中的范围描述必须与本文一致。

## 2. 产品原则

### 2.1 用户掌控

- 用户自行选择部署主机、网络入口、数据库、对象存储和备份位置。
- 用户数据和密钥默认保存在用户控制的环境中。
- 默认安装不依赖 WePush 官方在线控制面、账号系统或托管后端。
- 除用户配置的消息 Provider、远程 Agent、对象存储和数据库连接外，运行时不产生非必要外部网络请求。
- 不采集或上传业务数据、受众数据、Secret、运行结果和使用遥测。

### 2.2 可安装、可升级、可恢复

- Standalone 必须保持低依赖、低门槛和开箱可用。
- Server/HA 必须提供清晰的用户自建参考拓扑，而不是依赖官方托管服务。
- 每次升级必须以可验证备份、版本化迁移、健康检查和明确回滚边界为前提。
- 安装、升级、备份、恢复和卸载都属于正式产品能力，不是文档外的运维责任。

### 2.3 单一管理信任域

- Workspace 用于同一个自建实例内的团队、资源和权限分组。
- Workspace RBAC、审计和数据 Scope 是正式安全边界。
- WePush 不把 Workspace 定义为公共 SaaS 租户，不承诺对互不信任或恶意租户提供进程级、数据库级或基础设施级物理隔离。

### 2.4 可控发布

- 用户决定何时下载和安装新版本，不进行强制自动更新。
- 可以提供手动触发或显式启用的版本检查，但不得成为运行前提。
- Provider 通过发行包或用户本地安装的签名插件交付，不建设官方集中插件市场。

## 3. 正式产品范围

以下能力属于 WePush Next 的长期正式范围：

- Windows、Linux 和 macOS Standalone 安装与便携运行。
- SQLite、本地 Artifact Store 和本地信封加密 Secret Store。
- 用户自建 PostgreSQL、S3-compatible Artifact Store、多 Service 和远程 Agent。
- WebUI、Desktop UI、REST/SSE API、OpenAPI、Remote Java SDK 和 Embedded Java SDK。
- Account、Message、Audience、Job、Schedule、Run、Result、Artifact 和审计管理。
- 邮件、短信、微信、企业 IM、HTTP 等 Provider 及签名插件扩展机制。
- 并发、限流、重试、暂停、恢复、取消、幂等、结果未知和失败重发语义。
- Workspace 逻辑隔离、VIEWER/OPERATOR/ADMIN 权限和 Agent 授权。
- TLS/mTLS、Secret 最小暴露、Artifact 完整性、安装包校验和发行签名。
- 用户可执行的升级、备份、恢复、回滚、日志、指标和故障诊断。

PostgreSQL、S3-compatible Store 和多节点 HA 是可选的用户自建形态，不是使用 WePush 的前置条件。S3-compatible 只表示协议兼容，可以由用户在本地或私有环境中运行。

## 4. 明确且长期不做

以下能力不进入 WePush Next 路线图，也不能以“后续扩展”名义隐式引入：

- WePush 官方公共 SaaS、集中控制面或承载用户业务数据的托管平台。
- SaaS 用户自助注册、组织开户、计费、套餐、订阅、账单和支付。
- 公共租户市场、公共 Provider 市场或依赖官方账号的插件分发。
- 面向互不信任或恶意租户的进程级、数据库级、主机级物理隔离。
- 为每个 Workspace 自动创建独立数据库、集群或云资源。
- 跨区域 Active-Active 公共控制面和全球租户调度。
- AWS KMS、其他云 KMS、云 Secret Manager、HashiCorp Vault 或操作系统凭据库形式的 Service Secret Store 官方适配器。
- 强制自动更新、未经用户同意的远程配置下发和使用遥测回传。

`SecretStore`、数据库和 Artifact Store 保持端口抽象，是为了模块边界、测试和本地实现可替换性，不表示项目计划建设云厂商适配层。Service 的正式 Secret 方案是本地信封加密，Master Key 使用用户控制的受保护文件或显式注入。对象存储建议使用存储端原生 AES256 或由部署方自行管理的存储安全能力；WePush 不负责对接或管理云 KMS。

如果未来有人提出上述能力，应视为改变产品定位，而不是普通功能迭代；在当前产品方向下直接拒绝进入路线图。

## 5. 当前基线

`0.1.0-alpha.2` 已形成独立构建的纵向架构闭环：

- Core API、Provider SPI、虚拟线程 Engine 和 HTTP Provider。
- Standalone Service、SQLite、Local Artifact、Local Envelope Secret Store。
- PostgreSQL/S3-compatible Server/HA 参考拓扑和远程 Agent。
- REST/SSE、OpenAPI、Remote Java SDK 和 Embedded Java SDK。
- React WebUI、Electron Desktop 和三平台安装脚本。
- Agent Enrollment、Lease/Fencing、Secret Envelope、Artifact 上传和断线恢复。

当前差距集中在产品可用性、真实消息渠道、自部署运维和稳定发行，不需要继续扩大公共平台能力。

## 6. 迭代路线图

版本名称用于表达建议发布边界；如果实际版本号调整，里程碑内容和验收条件保持不变。

### 6.1 `0.1.0-alpha.2`：方向收口与当前成果发布

目标：在不扩大功能范围的前提下，修正产品边界、安全问题和发布元数据，发布当前已经完成的 Embedded Java SDK。

计划内容：

- 同步本文档所定义的产品边界到 README、架构、详细设计、ADR、实现状态和部署文档。
- 清理现有 AWS KMS 配置和实现路径，S3-compatible Store 保留原生 AES256 配置。
- 修复安全模式下 Artifact 下载未携带 Bearer Token 的问题，使用认证请求下载文件。
- 将 Embedded Java SDK、Core、Provider SPI、Engine 和 HTTP Provider POM/JAR 纳入正式发行附件。
- 更新版本号、OpenAPI、CHANGELOG、Release Notes 和 Preview Notice。
- 通过 Java、UI、SQLite、PostgreSQL、MinIO、三平台 Desktop 和发行归档门禁。

验收条件：

- 文档不再把云 KMS、外部 Secret Manager 或公共 SaaS 描述为未来产品增量。
- 安全模式下可以完成 Run、生成结果导出并成功下载 Artifact。
- `alpha.2` 发行附件包含 Remote 和 Embedded 两种 Java SDK。
- Standalone 默认不依赖任何官方在线服务。

### 6.2 `0.1.0-alpha.3`：日常使用闭环

目标：让普通用户不编写 JSON、不直接调用 API，也能完成完整发送和结果处理。

计划内容：

- Account 编辑、连接测试、启停和归档。
- Message 编辑并生成新 Revision、历史查看、Diff 和复制。
- Audience CSV/TXT 流式导入、字段映射、预览、去重、错误行下载和新 Snapshot。
- Job 编辑、复制、启停和归档；Schedule 支持完整编辑。
- 正式发送前展示 Provider、账号、受众数量、策略、并发、限速和预计执行规模，并要求二次确认。
- 从 FAILED、UNKNOWN 和 UNSENT Item 创建有来源关联的新 Run。
- Run、资源和审计日志增加游标分页、名称、状态和时间筛选。
- 总览、活动 Run、最近运行和趋势全部接入真实数据。
- Standalone 隐藏不必要的 Workspace 复杂度；Server UI 支持真实 Workspace 选择。

验收条件：

- 用户可通过 UI 完成 CSV 导入、Dry Run、正式确认发送、查看结果和失败重发。
- 修改 Message 或 Audience 不影响已经创建的 Run Snapshot。
- 大量历史 Run 和资源不会通过无界列表一次性载入内存或浏览器。

### 6.3 `0.1.0-alpha.4`：真实消息渠道

目标：在 HTTP Provider 之外建立可实际使用的消息渠道组合。

建议顺序：

1. SMTP Email。
2. 飞书、钉钉和企业微信机器人。
3. 根据用户需求选择首个主流短信 Provider。
4. 微信公众号、小程序和企业微信完整能力。
5. 其他短信、推送和运营商协议作为独立签名插件持续增加。

每个 Provider 必须同时交付：

- Account、Message 和 Recipient Schema。
- SecretRef 定义、字段校验和错误分类。
- Dry Run 行为和真实发送实现。
- 幂等、限流、重试和 Provider Code 映射说明。
- 本地模拟服务、单元测试、契约测试和 Engine 纵向测试。
- WebUI 示例、用户文档和最小可验证配置。

Classic 与 Next 可以复用业务需求、测试数据和验收经验，但不建立共享源码依赖。

### 6.4 `0.1.0-beta.1`：自部署运维成熟

目标：让用户能够长期、可预测地安装和维护 WePush，而不依赖项目维护者远程介入。

计划内容：

- 提供一体化 Standalone 安装体验，并保留便携包和高级分组件安装。
- 提供包含 Java Runtime 的完整发行包，同时保留使用系统 Java 的精简包。
- Windows 发行包离线携带经过校验的 Service Wrapper，不在安装时临时下载。
- Desktop 检测本地 Service，并提供启动、停止、日志和连接诊断入口。
- 增加正式 Restore 工具和备份内容校验。
- 升级后自动执行 Readiness、数据库版本和最小 Dry Run 验证；失败时切回旧版本并给出恢复指引。
- UI 支持本地上传、验证、Stage、Activate 和 Rollback 签名 Provider 插件。
- Desktop 使用本机安全存储保存 UI Token；浏览器默认只在会话范围保存 Token。
- 增加浏览器 E2E、Desktop 冒烟、安装/升级/恢复和长时间运行测试矩阵。
- 清理 JDK 未来兼容警告、Shade 重复资源和发行依赖技术债。

验收条件：

- Windows、Linux 和 macOS 均能在文档支持的离线条件下完成 Standalone 安装。
- 从上一预览版本升级失败时，用户数据不丢失并可恢复旧版本运行。
- 备份恢复后数据库、Master Key、Artifact、Agent Identity、Journal 和 Outbox 一致。

### 6.5 `1.0.0`：稳定发行

目标：对自部署用户提供明确的兼容性、安全和运维承诺。

发布条件：

- 明确 API、配置和数据库迁移兼容策略。
- 至少支持从最近一个 Beta 无损升级，并记录最低可回滚版本。
- 三平台安装、升级、备份、恢复和卸载自动化验收通过。
- 大受众、长时间任务、断网重连、磁盘不足、进程崩溃和 Agent 失联测试通过。
- 核心 Provider 具备稳定的限流、幂等、错误分类和重试语义。
- macOS 和 Windows 发行物完成代码签名；更新仍由用户主动触发。
- 默认运行不存在未声明的外部网络依赖或数据回传。
- 安全、部署、升级、恢复、Provider 开发和用户指南完整且相互一致。

## 7. 优先级规则

后续任务使用以下顺序决策：

1. 数据安全、发送正确性和可恢复性问题优先于新增功能。
2. 安装、升级、备份和恢复优先于公共平台能力。
3. 完整用户闭环优先于一次增加大量 Provider。
4. 每个 Provider 的可验证质量优先于渠道数量。
5. Standalone 简单体验优先，同时保持用户自建 Server/HA 的清晰路径。
6. 默认离线、自包含和无遥测优先于依赖外部控制服务的便利性。

## 8. 完成定义

路线图中的功能只有同时满足以下条件才算完成：

- 有明确的用户场景、权限行为和失败语义。
- API、Schema、SDK 和 UI 同步更新。
- 单元、契约、集成或 E2E 测试覆盖与风险匹配。
- 不泄露 Secret、受众数据和敏感 Provider 响应。
- Standalone 不增加未声明的在线依赖。
- Server/HA 仍可由用户在自己的环境中部署和恢复。
- 安装、升级和数据迁移影响已经记录。
- 用户指南、部署文档、实现状态和 CHANGELOG 同步更新。

## 9. 规划维护

- 每次预览版发布后更新本文件的完成状态和下一里程碑。
- 新功能进入实现前必须确认它属于第 3 节正式范围，且不违反第 4 节长期非目标。
- 产品范围与其他文档冲突时，先按本文纠正文档，再开始实现。
- 任何会把 WePush 变成官方托管公共平台的提案，均视为产品定位变更，不进入普通需求评审。
