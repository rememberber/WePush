# 用 Codex 等 AI 助手操作 WePush

本功能随当前源码构建交付，尚未包含在已发布的 `next-v1.1.0` 附件中。更新后的 WebUI、Desktop 和完整发行包提供同一个独立安装器 `wepush-ai.mjs`。

MCP 桥接器由 AI 客户端通过 stdio 启动，再调用用户自己的 WePush Next REST API，不需要新开服务端口或部署公共服务。它遵循已有 Workspace RBAC、Provider 校验、发送确认和 Idempotency-Key 语义。

## 桌面端一键接入

1. 启动 WePush Next Service，先在 WePush 中配置渠道账号及 SecretRef。
2. 打开 **设置 → AI 助手接入**，核对 Service URL 和当前 Workspace。
3. 点击 **一键接入 Codex**，安装 MCP 和 Skill；或点击 **仅安装 Skill**。
4. 重新连接 Codex 的 MCP，必要时重启 Codex。可以说：“用 WePush 列出已有任务，并对我选择的任务执行 Dry Run。”

桌面安装使用随 Desktop 提供的 Electron Node Runtime，无需另外安装 Node.js 或 Codex CLI。安装的 MCP 指向该 Desktop 可执行文件，移动或卸载 Desktop 后需要重新安装接入。

安装器会在 `~/.agents/skills/wepush` 写入 Skill、独立工具脚本和连接信息。Codex MCP 配置写入 `$CODEX_HOME/config.toml`，未设置时使用 `~/.codex/config.toml`；修改前保留同目录 `.wepush-<id>.bak` 备份。重复安装仅更新自己的配置块，保留其他 MCP、模型设置及注释。如果已有同名手动配置或自定义 Skill，安装会报告冲突并保留原文件。

“仅安装 Skill”不修改已有 MCP 配置，也不移除此前安装的 MCP；Skill 使用其自己的连接文件。需要同步修改已有 MCP 的连接时，选择“一键接入 Codex”。

## 浏览器或发行包安装

浏览器不能直接写入客户端配置。在 **设置 → AI 助手接入** 下载安装器，然后在下载目录执行界面生成的命令。也可使用新构建发行包的 `ai/wepush-ai.mjs`。这两种方式需要 Node.js 24+：

```bash
node ./wepush-ai.mjs install --target codex --url http://127.0.0.1:18990 --workspace ws_default
```

以上命令也适用于 Windows PowerShell。`--target skill` 只安装 Codex Skill；`--target generic` 把工具和通用 `mcpServers` JSON 写入 `~/.wepush/ai`，不更改 Codex 配置。安装后不需要保留下载的原文件。

Service URL 是 **AI 客户端运行环境** 能访问的地址。容器、WSL、远端主机中的 `127.0.0.1` 不指向桌面所在机器；需要配置实际可达的 HTTPS 地址。桥接器仅允许回环地址使用 HTTP，拒绝 URL 中的密码或查询参数，并拒绝跟随 API 重定向。

## Token 与权限

本地 Service 未启用认证时可直接使用。启用认证后，在启动 AI 客户端的环境中设置 `WEPUSH_API_TOKEN`，使用当前 Workspace 的 VIEWER（查询）或 OPERATOR（创建和发送）Token。已有系统管理员 Token 不应作为日常发送身份。

安装器不读取或复制 WePush 登录 Token，不将 Token 写入 Skill、MCP 配置或导出文件。Codex 配置通过 `env_vars = ["WEPUSH_API_TOKEN"]` 将客户端的环境变量传给 MCP 子进程。GUI 客户端的环境与终端可能不同，设置后应重启客户端；其他客户端按其环境变量设置方式传入该变量。

独立脚本同时支持 `WEPUSH_SERVICE_URL`、`WEPUSH_WORKSPACE_ID` 覆盖连接文件；MCP 安装配置中指定的 URL/Workspace 优先于客户端外部的同名环境变量。修改连接最直接的方式是重新安装。

## 其他 MCP / Skill 客户端

Desktop 的 **导出通用 MCP 配置** 会准备本机运行文件并打开 JSON 保存对话框；取消保存时，配置仍在 `~/.wepush/ai/mcp.json`。将其中 `mcpServers.wepush` 合并到支持 stdio 的客户端（例如 Claude Desktop 或 Cursor）的配置中，保留其他服务器条目。导出包含当前机器的绝对路径；迁移到另一台机器时应在那台机器运行安装器。

其他支持 Agent Skills 的客户端可以使用生成的 `SKILL.md` 与整个 `scripts/` 目录。脚本封装了已检测到的运行时路径，跨机器移动需要重新生成安装文件。此版本不提供远程 HTTP MCP 端点或 OAuth，云端仅支持远程 HTTP MCP 的客户端不能直接使用该 stdio 配置。

## 工具与发送流程

| 工具 | 能力 |
| --- | --- |
| `wepush_system_info` | 测试连接、版本及当前 Workspace |
| `wepush_list_providers` / `wepush_provider_schema` | 发现渠道与账号、消息、收件人 Schema |
| `wepush_list_resources` / `wepush_get_resource` | 分页查询或读取账号、消息、受众、任务及 Run |
| `wepush_create_message` / `wepush_create_audience` / `wepush_create_job` | 创建草稿及任务，不发送 |
| `wepush_dry_run` | 执行测试运行 |
| `wepush_prepare_run` / `wepush_send_run` | 预览正式发送并使用确认令牌创建 Run |
| `wepush_run_results` / `wepush_run_command` | 查询逐条结果、暂停、恢复或取消 |

先读取现有资源及渠道 Schema，准备消息、受众、任务，再执行 Dry Run。大于 1000 条的受众使用 WePush CSV/TXT 导入。账号、密钥、调度和失败重发在本版本仍由 WePush 界面管理。

正式发送需要先取得预览、核对消息内容和受众，然后在用户授权范围内提交 `confirmationToken`、同一个 `idempotencyKey` 和 `userConfirmed: true`。`userConfirmed` 是供助手表达已有授权的参数，不是独立的人工身份验证；实际权限和令牌有效性由 Service 校验。

返回 Run ID 表示已创建运行，不表示收件人已收到消息。应继续查询 Run 状态和结果。网络超时不自动重试；需要重试时复用原幂等键与请求内容。`UNKNOWN` 不等于失败，不自动重发。

Skill 在 MCP 不可用时可以直接调用同一套工具：

```bash
# 在安装后的 Skill 目录中执行；Windows 使用 scripts/wepush-ai.ps1
./scripts/wepush-ai.sh tools
printf '%s' '{}' | ./scripts/wepush-ai.sh call wepush_system_info
./scripts/wepush-ai.sh call wepush_create_message < message-arguments.json
```

`tools` 返回工具描述和完整 JSON Schema。调用参数走 stdin，避免把消息内容拼接成 shell 命令。

## 移除与排错

- 移除 Codex MCP 可运行 `codex mcp remove wepush`，或在客户端中移除该服务器；这不会删除 WePush 的业务数据。
- 不再使用 Skill 时删除安装器管理的 `~/.agents/skills/wepush`。通用安装文件位于 `~/.wepush/ai`。
- `401`：检查 AI 客户端实际继承的 `WEPUSH_API_TOKEN`；`403`：检查 Token 的 Workspace 和角色。
- 安装提示同名冲突：保留原配置，重命名自定义条目或先移除不再使用的条目，再安装。
- 安装中断后若残留 `.install-lock` 目录，确认没有安装进程运行后移除该空目录再重试。

实现接入格式参考 [Codex MCP 文档](https://developers.openai.com/codex/mcp)、[Codex Skills 文档](https://developers.openai.com/codex/skills)和 [MCP stdio 传输规范](https://modelcontextprotocol.io/specification/2025-11-25/basic/transports)。

开发验证：在 `next/ui` 运行 `pnpm check` 和 `pnpm --filter @wepush-next/web e2e`。构建 Service JAR 后，可运行 `pnpm --filter @wepush-next/ai-integration test:service`；该测试创建临时数据库和仅本机可达的 Service，通过 MCP 验证认证、Schema、草稿、发送预览、幂等 Dry Run 与结果，结束后清理，不实际投递消息。
