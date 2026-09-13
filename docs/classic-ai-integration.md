# Classic 的 MCP 与 Skill 接入

此功能随包含本次改动的 Classic 安装包提供；已发布的旧安装包不会自动获得该入口。Classic 的实现独立于 Next，不需要启动 Next Service，也不依赖 Next 的源码、Node.js 或构建产物。

## 一键接入

1. 启动 Classic，先在界面中配置账号、消息、人群和任务。
2. 打开 **应用 → AI 助手接入**。
3. 点击 **一键接入 Codex**，安装器会写入 Codex MCP 配置和 `wepush-classic` Skill，同时开启本机桥接。重新启动 Codex 或新建会话加载接入。
4. 对助手说：“用 WePush Classic 查看任务，空跑任务 1，告诉我校验结果。”

使用期间保持 Classic 运行。桥接默认关闭，首次开启后会在下次启动 Classic 时自动恢复。**关闭本机接入**会停止接受新调用，已启动的任务继续执行；退出 Classic 会中断本机任务。

安装名称均为 `wepush-classic`，与 Next 的 `wepush` 可以共存：

| 内容 | 位置 |
| --- | --- |
| Codex MCP | `$CODEX_HOME/config.toml`，未设置时为 `~/.codex/config.toml`，表名 `[mcp_servers.wepush-classic]` |
| Skill | `~/.agents/skills/wepush-classic/SKILL.md` |
| Skill 启动脚本 | Skill 目录中的 `scripts/wepush-classic.sh` / `scripts/wepush-classic.ps1` |
| 通用 MCP JSON | 安装后的 Skill 目录中的 `mcp.json` |
| 本机连接与防重记录 | `~/.WePush5/ai/` |

安装器保留其他 MCP、模型和 Profile 配置，并在修改已有 Codex 配置前生成 `config.toml.wepush-classic-<UUID>.bak`。遇到同名的手动 MCP 或非本安装器创建的 Skill 时停止，避免覆盖。重复安装会更新自己管理的文件；自定义 Skill 请使用另一个目录名称。

启动配置引用当前 Classic 的 Java 运行时和应用目录，发行版无需另装 Java。移动应用、切换开发目录或升级导致路径变化后，请在新位置再执行一次安装。

## 其他助手与 Skill 单独使用

支持 stdio MCP 的客户端：点击 **复制通用 MCP 配置**，把 JSON 中 `mcpServers.wepush-classic` 合并到客户端配置中，再重新连接。客户端可能有自己的配置格式，保留生成的 `command` 和 `args`。

仅使用 Skill：点击 **仅安装 Skill**。Codex 会从用户 Skill 目录发现它；其他支持 Skill 的助手可加载或复制整个目录。脚本使用与 MCP 相同的工具和本机桥接。使用 `tools` 查询参数，`call <工具名>` 从标准输入读取一个 UTF-8 JSON 对象：

```sh
sh ~/.agents/skills/wepush-classic/scripts/wepush-classic.sh tools
printf '%s' '{"taskId":1}' | sh ~/.agents/skills/wepush-classic/scripts/wepush-classic.sh call wepush_classic_get_task
```

Windows 示例：

```powershell
'{"taskId":1}' | powershell -NoProfile -ExecutionPolicy Bypass -File "$HOME/.agents/skills/wepush-classic/scripts/wepush-classic.ps1" call wepush_classic_get_task
```

## 工具与执行范围

| 工具 | 能力 |
| --- | --- |
| `wepush_classic_system_info` | 检查连接与能力 |
| `wepush_classic_list_tasks` | 分页查询已保存的任务 |
| `wepush_classic_get_task` | 查看消息模板、账号名称、人群名称及数量 |
| `wepush_classic_prepare_run` | 返回发送预览与有效期 10 分钟的确认令牌，不投递消息 |
| `wepush_classic_dry_run` | 使用 Classic 原有渠道逻辑空跑校验 |
| `wepush_classic_send_run` | 执行已授权的正式发送 |
| `wepush_classic_get_run` | 按 `runId` 查询进度与结果 |
| `wepush_classic_list_history` | 分页查询指定任务的 Classic 历史 |

分页参数为 `offset`（默认 0）和 `limit`（默认 30，最大 100）。工具只操作已有资源，账号、消息、人群和任务的创建及编辑仍在 Classic 界面中完成。

当前执行范围为**已保存的手动固定线程任务**：线程数 1～100，已关闭任务的结果邮件提醒，每个 Classic 实例最多同时执行 4 个由 AI 启动的任务。计划、触发、变速任务和结果邮件提醒仍由 Classic 原有界面执行。各渠道的成功语义、限流和空跑行为沿用现有发送器；空跑不证明真实渠道认证或投递一定成功。

正式发送前，助手必须核对 `prepare_run` 的预览，确保已有用户授权覆盖内容、人群及发送时机，再传入 `taskId`、`confirmationToken`、新的标准小写 UUID `idempotencyKey` 和 `userConfirmed: true`。任务、消息、账号或人群在预览后变化时，旧预览失效。接入在同一个数据库锁内核对资源并准备发送器，接受运行后保留该次任务和人群快照。

本次 AI 接入的“发送预览”是只读操作。Classic 原有消息编辑界面中的“预览发送”仍按原行为向预览目标实际发送。

## 防重与结果

`runId` 与调用者提供的 `idempotencyKey` 相同。请求在启动发送前持久化，重复使用相同请求编号和相同参数会返回原运行记录。确认令牌只允许启动一次正式发送，但已接受请求的原样重试不受令牌过期影响。

超时或响应丢失时先查询原 `runId`，必要时原样重试；不要换 UUID 重发。Classic 进程退出或执行异常后可能返回 `UNKNOWN`，必须先核对 Classic 历史和渠道结果，不会自动恢复投递。防重记录长期保留在 `ai/requests`；删除这些记录会失去对应请求的防重保护。

`FINISHED` 只表示执行结束，应继续核对 `totalCount`、`successCount` 和 `failCount`。它不保证所有目标发送成功，也不等同于终端已送达。

## 本机边界

桥接只监听 `127.0.0.1` 随机端口，每次开启生成新的令牌；拒绝带 `Origin` 的浏览器请求。连接文件在支持 POSIX 权限的系统上仅当前用户可读写。MCP 和 Skill 配置只保存连接文件路径，不包含令牌或渠道凭据。不要共享 `~/.WePush5/ai/connection.json`；同一系统用户下的可信本机客户端可以通过此文件调用桥接。

工具不返回账号配置、原始日志、完整人群记录或结果文件。消息模板里的常见凭据字段（如 Header、Cookie、Token）会隐藏；自定义正文中的敏感文本仍属于消息内容，应按实际使用范围处理。

## 开发验证

使用 Java 21+ 和 Maven 执行 `mvn test`。新增测试覆盖 TOML 保留与冲突、重复安装、脚本路径转义、MCP 协议、鉴权与令牌轮换、配置变化、确认过期、持久防重及并发重试。真实链路测试在隔离子进程中使用临时数据库与本机 HTTP 接收器，断言空跑零投递、正式发送使用已接受的消息快照、重复请求不增加投递。

配置依据：[Codex MCP](https://developers.openai.com/codex/mcp)、[Codex Skills](https://developers.openai.com/codex/skills)、[MCP 生命周期](https://modelcontextprotocol.io/specification/2025-11-25/basic/lifecycle)及[工具协议](https://modelcontextprotocol.io/specification/2025-11-25/server/tools)。
