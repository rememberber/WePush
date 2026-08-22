# ADR-0005：Provider 插件发现、隔离和更新

- 状态：已接受
- 日期：2026-08-22
- 决策者：WePush 项目维护者

## 背景

WePush Provider 会引入不同厂商 SDK。它们可能依赖不同版本的 HTTP、JSON、加密和日志库。Provider 还需要携带 Descriptor、JSON Schema、UI Schema 和图标。插件机制需要减少依赖冲突，同时保证运行中任务不会被不安全的代码替换破坏。

## 决策

### 插件框架

- 外部 Provider 插件框架采用 PF4J 3.15.x。
- Provider SPI 是 PF4J Extension Point，但 Core Engine 不依赖 PF4J。
- Agent App 的 Provider Catalog Adapter 负责把 PF4J Extension 转换为 Core 的 `ProviderFactory`。
- 内置 Provider 可以作为系统 Extension 参与同一个 Provider Catalog。

### 插件包

```text
wepush-provider-http-1.0.0.zip
├── plugin.jar
├── lib/
├── plugin.json
├── schemas/
├── assets/
├── LICENSES/
└── signature.ed25519
```

`plugin.json` 包含 Plugin ID、Provider ID、版本、SPI 版本、主类、依赖、平台限制、SHA-256 清单和签名 Key ID。

### 依赖隔离

- 每个插件使用独立 PF4J ClassLoader。
- JDK、Provider SPI、Core API 和统一日志 API 由 Parent ClassLoader 提供。
- 插件私有 SDK 和三方依赖由插件 ClassLoader 加载。
- 插件不得捆绑 Provider SPI、Core API 或日志 API 的重复副本。
- 构建阶段检测 Split Package、重复共享类和禁止依赖。

ClassLoader 隔离只解决依赖冲突，不是恶意代码安全沙箱。JVM 内 Provider 被视为受信代码。需要运行不受信第三方插件时，必须增加独立进程 Provider Runner，不能宣称 PF4J 已提供安全隔离。

### 签名和信任

- 正式发行环境默认只加载签名有效且发布者受信的插件。
- 插件包使用 SHA-256 清单和 Ed25519 签名。
- Agent 发行包内置官方公钥，并允许管理员添加受信发布者。
- 未签名插件只允许在显式 Developer Mode 中加载，Service 和 UI 必须显示安全警告。
- ZIP 解包采用路径白名单、大小上限和 Zip Slip 防护。

### 安装和激活

插件安装到版本化目录：

```text
plugins/
└── wepush.http/
    ├── 1.0.0/
    ├── 1.1.0/
    └── active.json
```

- 下载到 Staging 目录后验证大小、哈希、签名、Descriptor 和 SPI 兼容性。
- 验证成功后原子移动到版本目录。
- `active.json` 只记录期望激活版本，不覆盖旧版本文件。
- Service 保存期望插件版本，Agent 上报实际插件清单和状态。

### 更新策略

- 不进行 JVM 内热替换，不在运行中卸载或替换 Provider ClassLoader。
- 更新采用 Stage、Drain、Restart、Verify、Activate 的滚动流程。
- Agent 先停止领取新 Run，等待当前 Run 完成或达到 Drain Timeout，再由 Supervisor 重启。
- 重启后加载新版本并通过 Health 和自检；失败时恢复 `active.json` 指向旧版本并再次重启。
- 多 Agent 环境逐台滚动，确保始终保留可执行旧任务的 Agent。
- Provider 版本由 Run Snapshot 固定；调度新 Run 前必须存在兼容 Agent。

## Provider 删除

- 被 Run Snapshot、Job 或历史诊断引用的版本不能立即物理删除。
- 删除先进入 `DEPRECATED`，禁止新配置使用。
- 没有运行和保留引用后才允许从 Agent 清理文件。
- 历史 Run 仍保存 Provider ID、版本和 Schema 快照，即使代码已经删除。

## 结果

该方案提供清晰的发现和依赖隔离，同时避免热替换引发线程、连接池、静态缓存和 ClassLoader 泄漏。代价是插件升级需要 Agent 重启，但滚动升级可以避免整体停机。

## 参考

- [PF4J](https://github.com/pf4j/pf4j)

